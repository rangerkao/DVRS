package program;


import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

public class suspendGPRS {
	
	private static Connection conn;
	private static Connection conn2; //mBoss
	private static Logger logger;
	public suspendGPRS(Connection con,Connection con2,Logger log){
		conn=con;
		conn2=con2;
		logger=log;
	}
	
	// TWN_IMSI、TWN_MSISDN先代空值，sMNOSubCode不重要，sCount忽略
	private String cRCode,Process_Code,sCMHKLOGID,cMSISDNOLD,cM205OT,cMVLN,cGPRS,csta,bb;
	private String cReqStatus,dReqDate,cTicketNumber,cS2TIMSI,cS2TMSISDN,sFORWARD_TO_HOME_NO,sS_FORWARD_TO_HOME_NO;
	//private String cTWNLDIMSI,cTWNLDMSISDN;
	//TWNLDIMSI=>HOME IMSI,TWNLDMSISDN=>PARTNER MSISDN
	private String sHOMEIMSI,PARTNERMSISDN;
	private String sMNOSubCode,sMNOName,sWSFStatus,sWSFDStatus,cServiceOrderNBR;
	private SimpleDateFormat dFormat1=new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat dFormat2=new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private SimpleDateFormat dFormat3=new SimpleDateFormat("yyMMddHHmm");
	private SimpleDateFormat dFormat4=new SimpleDateFormat("yyMMddHHmmss");
	private String cFileID,cFileName,c910SEQ,sCount,cWorkOrderNBR,sDATE,Sdate,sDataType,sValue,sSubCode,sStepNo,sTypeCode,sMap,sM_CTYPE,cGPRSStatus;
	private String sFMTH,sFMTHa,sSFMTH,sSFMTHa;
	static Vector<String> vln=new Vector<String>();
	private ResultSet Temprs;
	private String sSql;	
	
	public Map<String,String> ReqStatus_17_Act(String imsi,String msisdn) throws SQLException,
			IOException, ClassNotFoundException, Exception {
		logger.debug("ReqStatus_17_Act");
		
		cS2TIMSI=imsi;
		cS2TMSISDN=msisdn;
		
		//20141104 add
		setPartnerCode();
		
		cReqStatus="17";
		dReqDate=dFormat3.format(new Date());
		//20141103 set as TWNLD
		sMNOSubCode="950";
		
		//20141117 add 必須指定將GPRS變更的狀態 0-Disabled,1 – Enabled
		cGPRSStatus="0";
		
		//設定sCount 
		Temprs = null;
		sSql = "select DVRS_SUSPEND_COUNT.NEXTVAL as ab from dual";
		Temprs = conn.createStatement().executeQuery(sSql);
		while (Temprs.next()) {
			sCount = Temprs.getString("ab");
		}
		for (int i = sCount.length(); i < 3; i++) {
			sCount = "0" + sCount;
		}
		
		cTicketNumber="D"+dReqDate+sCount;
		

		/*
		 * logger.debug("ReqStatus_17_Act");
		 */
		
		//20141117 新增 必須的檢查與賦予值
		Check_Type_Code_87_MAP_VALUE(cS2TMSISDN); 
		sWSFStatus = "V";
		sWSFDStatus = "V";
		Process_SyncFile(sWSFStatus);
		Process_SyncFileDtl(sWSFDStatus);
		Process_ServiceOrder();
		// Process_WorkSubcode();
		Process_WorkSubcode_05_17(cS2TIMSI, sHOMEIMSI, cReqStatus,
				PARTNERMSISDN);
		sSql = "update S2T_TB_SERVICE_ORDER set STATUS='N' where "
				+ "SERVICE_ORDER_NBR='" + cServiceOrderNBR + "'";
		conn.createStatement().executeUpdate(sSql);
		logger.debug("update SERVICE_ORDER:" + sSql);
		/* Query_PreProcessResult(out17, "000"); */
		Query_GPRSStatus();
		// 待實做Log紀錄停止GPRS 回傳結果 desc
		
		//20141118 add 確認狀態，實做在Main後面持續監測
		//System.out.println("rcode : "+Query_ServiceOrderStatus());
		/*sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set s2t_operationdate="+
	              "to_date('"+dFormat4.format(new Date())+
	              "','YYYYMMDDHH24MISS')"+
	              " where WORK_ORDER_NBR='"+cWorkOrderNBR+"'";
	         
	         logger.debug("update S2T_TB_TYPB_WO_SYNC_FILE_DTL:"+sSql);
	         conn.createStatement().executeUpdate(sSql);
	         
	         sSql="update S2T_TB_SERVICE_ORDER_ITEM set timestamp="+
	              "to_date('"+dFormat4.format(new Date())+
	              "','YYYYMMDDHH24MISS')"+
	              " where Service_Order_NBR='"+cServiceOrderNBR+"'";
	                
	         logger.debug("Update S2T_TB_SERVICE_ORDER_ITEM:"+sSql);
	         conn.createStatement().executeUpdate(sSql);
	                
	         sSql="update S2T_TB_SERVICE_ORDER set timestamp="+
	              "to_date('"+dFormat4.format(new Date())+
	              "','YYYYMMDDHH24MISS')"+
	              " where SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
	                
	         logger.debug("Update S2T_TB_SERVICE_ORDER:"+sSql);
	         conn.createStatement().executeUpdate(sSql);*/
		
		Map<String,String> map =new HashMap<String,String>();
		map.put("cServiceOrderNBR", cServiceOrderNBR);
		map.put("cWorkOrderNBR", cWorkOrderNBR);
		map.put("imsi", imsi);
		map.put("msisdn", msisdn);
		return map;
	}
	
	//20141104 add
		public void setPartnerCode() throws Exception{
			logger.debug("setPartnerCode");
			
			
			//From AVAILABLEMSISDN
			sSql=
					"SELECT HOMEIMSI , PARTNERMSISDN "
					+ "FROM AVAILABLEMSISDN A, IMSI B, SERVICE C "
					+ "WHERE A.S2TMSISDN=C.SERVICECODE AND B.SERVICEID=C.SERVICEID "
					+ "AND B.IMSI='"+cS2TIMSI+"'";
					
			logger.info("Get HOMEIMSI,PARTNERMSISDN from AVAILABLEMSISDN :" + sSql);
			Temprs = null;
			Temprs = conn.createStatement().executeQuery(sSql);
			while(Temprs.next()){
				sHOMEIMSI = Temprs.getString("HOMEIMSI");
				PARTNERMSISDN = Temprs.getString("PARTNERMSISDN");
			}
			
			if(sHOMEIMSI!=null && !"".equals(sHOMEIMSI) && PARTNERMSISDN!=null && !"".equals(PARTNERMSISDN))
				return;
			
			// mBOSS From TWNLD record
			sSql=
					"SELECT HOMEIMSI, PARTNERMSISDN "
					+ "FROM ( 	SELECT '"+cS2TIMSI+"' IMSI,VALUE PARTNERMSISDN "
					+ "			FROM NEWSERVICEORDERPARAMETERVALUE "
					+ "			WHERE PARAMETERVALUEID=3792 AND SERVICEID= (SELECT MAX(SERVICEID) "
					+ "														FROM NEWSERVICEORDERINFO "
					+ "														WHERE FIELDVALUE='"+cS2TIMSI+"') ) A, "
					+ "IMSI B "
					+ "WHERE A.IMSI=B.IMSI";
			
			logger.info("Get HOMEIMSI,PARTNERMSISDN mBOSS From TWNLD record :" + sSql);
			Temprs = null;
			Temprs = conn2.createStatement().executeQuery(sSql);
			while(Temprs.next()){
				sHOMEIMSI = Temprs.getString("HOMEIMSI");
				PARTNERMSISDN = Temprs.getString("PARTNERMSISDN");
			}
			
			if(sHOMEIMSI!=null && !"".equals(sHOMEIMSI) && PARTNERMSISDN!=null && !"".equals(PARTNERMSISDN))
				return;
			
			//mBOSS From change sim card record  
			
			sSql=
					"SELECT HOMEIMSI , PARTNERMSISDN "
					+ "FROM ( 	SELECT '"+cS2TIMSI+"' IMSI, VALUE PARTNERMSISDN "
					+ "			FROM NEWSERVICEORDERPARAMETERVALUE A,(	SELECT MAX(A.ORDERID), B.SERVICEID "
					+ "													FROM SERVICEINFOCHANGEORDER A, SERVICEORDER B "
					+ "													WHERE A.ORDERID=B.ORDERID AND A.FIELDID=3713 AND A.OLDVALUE<>A.NEWVALUE AND A.NEWVALUE='"+cS2TIMSI+"' "
					+ "													GROUP BY B.SERVICEID) B "
					+ "			WHERE PARAMETERVALUEID=3792 AND A.SERVICEID=B.SERVICEID  ) A,IMSI B "
					+ "WHERE A.IMSI=B.IMSI ";

			logger.info("Get HOMEIMSI,PARTNERMSISDN mBOSS From change sim card record  :" + sSql);
			Temprs = null;
			Temprs = conn2.createStatement().executeQuery(sSql);
			while(Temprs.next()){
				sHOMEIMSI = Temprs.getString("HOMEIMSI");
				PARTNERMSISDN = Temprs.getString("PARTNERMSISDN");
			}
			if(sHOMEIMSI==null || "".equals(sHOMEIMSI) || PARTNERMSISDN==null || "".equals(PARTNERMSISDN))
				throw new Exception("Can't find HOMEIMSI,PARTNERMSISDN");
			
		}
	
	public void Check_Type_Code_87_MAP_VALUE(String sServiceCode)
			throws SQLException {
		logger.info("Check_Type_Code_87_MAP_VALUE...");
		Temprs = null;
		sSql = "select CUSTOMERTYPE from service where servicecode='"
				+ sServiceCode + "'";
		logger.info("Check_Type_Code_87_MAP_VALUE:" + sSql);
		Temprs = conn.createStatement().executeQuery(sSql);
		while (Temprs.next()) {
			sM_CTYPE = Temprs.getString("CUSTOMERTYPE");
		}
		if (sM_CTYPE.equals("1")) {
			sM_CTYPE = "3";
		}
	}


	public void Process_SyncFile(String sSFStatus) throws SQLException,
			Exception {
		logger.info("Process_SyncFile...");
		
		String sMNOSubCode="950";
		
		// 格式為YYYYMMDDXXX
		sDATE = dFormat1.format(new Date());
		c910SEQ = sDATE + sCount;
		cFileName = "S2TDI" + c910SEQ + "."+sMNOSubCode;
		cFileID = "";
		Temprs = null;
		sSql = "select S2T_SQ_FILE_CNTRL.NEXTVAL as ab from dual";
		Temprs = conn.createStatement().executeQuery(sSql);
		while (Temprs.next()) {
			cFileID = Temprs.getString("ab");
		}
		//dReqDate 要求時間，訂為即時
		dReqDate = dFormat1.format(new Date());
		sSql = "INSERT INTO S2T_TB_TYPEB_WO_SYNC_FILE "
				+ "(FILE_ID,FILE_NAME,FILE_SEND_DATE,FILE_SEQ,CMCC_BRANCH_ID,FILE_CREATE_DATE,STATUS) "
				+ "VALUES "
				+ "(" + cFileID + ",'"
				+ cFileName + "','" + dReqDate.substring(0, 8) + "','"
				+ c910SEQ.substring(8, 11) + "','"+sMNOSubCode+"',sysdate,'" + sSFStatus
				+ "')";
		logger.debug("Process_SyncFile:" + sSql);
		conn.createStatement().executeUpdate(sSql);
	}


	public void Process_SyncFileDtl(String sSFDStatus) throws SQLException,
			IOException {
		logger.info("Process_SyncFileDtl...");
		Sdate = dFormat2.format(new Date());

		cWorkOrderNBR = "";
		Temprs = conn.createStatement().executeQuery(
				"select S2T_SQ_WORK_ORDER.nextval as ab from dual");
		while (Temprs.next()) {
			cWorkOrderNBR = Temprs.getString("ab");
		}
		Temprs = null;
		cServiceOrderNBR = "";
		Temprs = conn.createStatement().executeQuery(
				"select S2T_SQ_SERVICE_ORDER.nextval as ab from dual");
		while (Temprs.next()) {
			cServiceOrderNBR = Temprs.getString("ab");
		}
		sSql = "INSERT INTO S2T_TB_TYPB_WO_SYNC_FILE_DTL (WORK_ORDER_NBR,"
				+ "WORK_TYPE, FILE_ID, SEQ_NO, CMCC_OPERATIONDATE, ORIGINAL_CMCC_IMSI,"
				+ "ORIGINAL_CMCC_MSISDN, S2T_IMSI, S2T_MSISDN, FORWARD_TO_HOME_NO, "
				+ "FORWARD_TO_S2T_NO_1, IMSI_FLAG, STATUS, SERVICE_ORDER_NBR, SUBSCR_ID)"
				+ " VALUES ("+ cWorkOrderNBR+ ",'"+ cReqStatus+ "',"+ cFileID+ ",'"+ c910SEQ
				+ "',to_date('"+ Sdate+ "','MM/dd/yyyy HH24:mi:ss'),'"+ sHOMEIMSI+ "','+"+ PARTNERMSISDN
				+ "','"+ cS2TIMSI+ "','"+ cS2TMSISDN+ "','+"+ PARTNERMSISDN+ "','"+ PARTNERMSISDN+ "', '2', '"
				+ sSFDStatus+ "','"+ cServiceOrderNBR+ "','"+ cTicketNumber+ "')";
		logger.debug("Process_SyncFileDtl:" + sSql);
		conn.createStatement().executeUpdate(sSql);
      
	}

	public void Process_ServiceOrder() throws SQLException, IOException {
		logger.info("Process_ServiceOrder...");
		sSql = "INSERT INTO S2T_TB_SERVICE_ORDER (SERVICE_ORDER_NBR, "
				+ "WORK_TYPE, S2T_MSISDN, SOURCE_TYPE, SOURCE_ID, STATUS, "
				+ "CREATE_DATE) " + "VALUES ('" + cServiceOrderNBR + "','"
				+ cReqStatus + "','" + cS2TMSISDN + "'," + "'B_TYPE',"
				+ cWorkOrderNBR + ", '', sysdate)";

		logger.info("Process_ServiceOrder[1]:" + sSql);

		conn.createStatement().executeUpdate(sSql);
		Temprs = null;

		sSql = "Select MNO_NAME from S2T_TB_MNO_COMPANY "
				+ "Where MNO_SUB_CODE='" + sMNOSubCode + "'";

		logger.debug("Process_ServiceOrder[2]:" + sSql);
		Temprs = conn.createStatement().executeQuery(sSql);

		while (Temprs.next()) {
			sMNOName = Temprs.getString("MNO_NAME");
		}
	}


	public void Process_WorkSubcode_05_17(String S2TImsiB, String TWNImsiB,
			String sReqStatus, String sTWNLDMSISDN) throws SQLException,
			IOException {
		logger.info("Process_WorkSubcode_05_17...");
		Temprs = null;
		String cMd = "", Ssvrid = "";
		sSql = "select nvl(serviceid,'0') as ab from imsi " + " where imsi = '"
		//20141103 decide need not to check homeimsi is TWNimsi , or not.
				+ S2TImsiB + "'";// +" and homeimsi='" + TWNImsiB + "'";
		
		 logger.info("Get_Serviceid:"+sSql);
		 Temprs = conn.createStatement().executeQuery(sSql);
		while (Temprs.next()) {
			Ssvrid = Temprs.getString("ab");
		}
		if (!Ssvrid.equals("0")) {
			Temprs = null;
			sSql = "select count(serviceid) as ab from serviceparameter where "
					+ "parameterid=3792 and serviceid='" + Ssvrid + "'";
			
			 logger.info("Check_Follow_Me_To_Home(有1表示有申請, 0表示未申請):"+sSql);
			 Temprs = conn.createStatement().executeQuery(sSql);
			while (Temprs.next()) { // (有1表示有申請, 0表示未申請)
				sFMTH = Temprs.getString("ab");
			}

			if (sFMTH.equals("1")) {
				Temprs = null;
				sSql = "select nvl(value,'2') as ab From parametervalue where "
						+ "parametervalueid=3793 and serviceid='" + Ssvrid
						+ "'";
				
				 logger.info(
				 "Check_Follow_Me_To_Home_Status(Value=1: active, Value=0: inactive, 若未申請, 則2):"
				 +sSql);
				 Temprs = conn.createStatement().executeQuery(sSql);
				while (Temprs.next()) { // (Value=1: active, Value=0: inactive,
										// 若未申請, 則NULL)
					sFMTHa = Temprs.getString("ab");
				}
			}
			Temprs = null;
			sSql = "select count(serviceid) as ab from serviceparameter where "
					+ "parameterid=3748 and serviceid='" + Ssvrid + "'";
			
			 logger.info("Check_SMS_Follow_Me_To_Home(有1表示有申請, 0表示未申請):"+sSql);
			 Temprs = conn.createStatement().executeQuery(sSql);
			while (Temprs.next()) { // (有1表示有申請, 0表示未申請)
				sSFMTH = Temprs.getString("ab");
			}

			if (sSFMTH.equals("1")) {
				Temprs = null;
				sSql = "select nvl(value,'2') as ab From parametervalue where "
						+ "parametervalueid=3752 and serviceid='" + Ssvrid
						+ "'";
				
				 logger.info(
				 "Check_SMS_Follow_Me_To_Home_Status(Value=1: active, Value=0: inactive, 若未申請, 則2):"
				 +sSql);
				 Temprs = conn.createStatement().executeQuery(sSql);
				while (Temprs.next()) { // (Value=1: active, Value=0: inactive,
										// 若未申請, 則NULL)
					sSFMTHa = Temprs.getString("ab");
				}
			}
			if (sReqStatus.equals("17")) {
				Temprs = null;
				sSql = "select nvl(value,'0') as ab from parametervalue where parametervalueid=3792 "
						+ "and serviceid='" + Ssvrid + "'";
				
				 logger.info("Check_FORWARD_TO_HOME_NO:"+sSql);
				 Temprs = conn.createStatement().executeQuery(sSql);
				while (Temprs.next()) {
					sFORWARD_TO_HOME_NO = Temprs.getString("ab");
				}
				if (sFORWARD_TO_HOME_NO != null && sFORWARD_TO_HOME_NO.equals('0')) {
					sFORWARD_TO_HOME_NO = null;
				}
				Temprs = null;
				sSql = "select nvl(value,'0') as ab from parametervalue where parametervalueid=3748 "
						+ "and serviceid='" + Ssvrid + "'";
				
				 logger.info("Check_S_FORWARD_TO_HOME_NO:"+sSql);
				 Temprs = conn.createStatement().executeQuery(sSql);
				while (Temprs.next()) {
					sS_FORWARD_TO_HOME_NO = Temprs.getString("ab");
				}
				if (sS_FORWARD_TO_HOME_NO != null && sS_FORWARD_TO_HOME_NO.equals('0')) {
					sS_FORWARD_TO_HOME_NO = null;
				}
			} else {
				sFORWARD_TO_HOME_NO = sTWNLDMSISDN;
				sS_FORWARD_TO_HOME_NO = sTWNLDMSISDN;
				sTWNLDMSISDN = null;
			}
		}

		sSql = "Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"
				+ sMNOName
				+ "' And work_type='"
				+ cReqStatus
				+ "' Order by step_no";
		
		 logger.debug("Process_WorkSubcode_05_17:"+sSql);
		 Temprs = conn.createStatement().executeQuery(sSql);
		while (Temprs.next()) {
			sSubCode = Temprs.getString("subcode");
			sStepNo = Temprs.getString("step_no");
			Process_ServiceOrderItem();
			Process_DefValue();
			Process_MapValue();
		}

		// 不需要更新provLog
		/*
		 * sSql="update PROVLOG " + "set STEP='"+sStepNo+"' "+
		 * " where LOGID="+sCMHKLOGID;
		 * conn.createStatement().executeUpdate(sSql);
		 */
	}

	public void Process_ServiceOrderItem() throws SQLException, IOException {
		logger.info("Process_ServiceOrderItem...");
		sSql = "Insert into S2T_TB_SERVICE_ORDER_ITEM (SERVICE_ORDER_NBR,"
				+ "STEP_NO, SUB_CODE, IDENTIFIER, STATUS, SEND_DATE) "
				+ "Values (" + cServiceOrderNBR + "," + sStepNo + ",'"
				+ sSubCode + "',"
				+ " S2T_SQ_SERVICE_ORDER_ITEM.nextval, 'N', sysdate)";
		logger.debug("Process_ServiceOrderItem:" + sSql);
		conn.createStatement().executeUpdate(sSql);
	}

	public void Process_ServiceOrderItemDtl() throws SQLException, IOException {
		logger.info("Process_ServiceOrderItemDtl...");
		sSql = "Insert into S2T_TB_SERVICE_ORDER_ITEM_DTL "
				+ "(SERVICE_ORDER_NBR, STEP_NO, TYPE_CODE, DATA_TYPE, VALUE) "
				+ "VALUES (" + cServiceOrderNBR + "," + sStepNo + ","
				+ sTypeCode + "," + sDataType + ",'" + sValue + "')";
		logger.debug("Process_ServiceOrderItemDtl:" + sSql);
		conn.createStatement().executeUpdate(sSql);
	}

	public void Process_DefValue() throws SQLException, IOException {
		logger.info("Process_DefValue...");
		ResultSet TeRt = null;
		sSql = "Select TYPE_CODE, DATA_TYPE, DEF_VALUE "
				+ "From S2T_TB_SUBCODE_TYPECODE " + "Where subcode='"
				+ sSubCode + "' And work_type='" + cReqStatus
				+ "' And MNO_NAME='" + sMNOName + "' And DEF_VALUE is not null";
		logger.debug("Process_DefValue:" + sSql);
		TeRt = conn.createStatement().executeQuery(sSql);
		while (TeRt.next()) {
			sTypeCode = TeRt.getString("TYPE_CODE");
			sDataType = TeRt.getString("DATA_TYPE");
			sValue = TeRt.getString("DEF_VALUE");
			Process_ServiceOrderItemDtl();
		}
	}


	public void Process_MapValue() throws SQLException, IOException {
		logger.info("Process_MapValue...");
		ResultSet TeRtA = null;
		sSql = "Select TYPE_CODE, DATA_TYPE, MAP_VALUE "
				+ "From S2T_TB_SUBCODE_TYPECODE " + "Where subcode='"
				+ sSubCode + "' And work_type='" + cReqStatus
				+ "' And MNO_NAME='" + sMNOName + "' And MAP_VALUE is not null";
		logger.debug("Process_MapValue:" + sSql);
		TeRtA = conn.createStatement().executeQuery(sSql);
		while (TeRtA.next()) {
			sTypeCode = TeRtA.getString("TYPE_CODE");
			sDataType = TeRtA.getString("DATA_TYPE");
			sMap = "";
			sMap = TeRtA.getString("MAP_VALUE");
			if ("S2T_MSISDN".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cS2TMSISDN;
			} else if ("S2T_IMSI".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cS2TIMSI;
				/*
				 * } else if
				 * ("TWNLD_MSISDN".equals(TeRtA.getString("MAP_VALUE"))) {
				 * sValue = cTWNLDMSISDN;
				 */
				/*
				 * } else if ("TWNLD_IMSI".equals(TeRtA.getString("MAP_VALUE")))
				 * { sValue = cTWNLDIMSI;
				 */
				/*
				 * } else if
				 * ("S2T_MSISDN_OLD".equals(TeRtA.getString("MAP_VALUE"))) {
				 * sValue = cMSISDNOLD;
				 */
				/*
				 * } else if ("M_205_OT".equals(TeRtA.getString("MAP_VALUE"))) {
				 * sValue = cM205OT;
				 */
				/*
				 * } else if ("M_VLN".equals(TeRtA.getString("MAP_VALUE"))) {
				 * sValue = cMVLN;
				 */
			} else if ("M_GPRS".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cGPRSStatus;
			} else if ("M_CTYPE".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sM_CTYPE;
			} else if ("FMTH".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sFMTH;
			} else if ("FMTH_A".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sFMTHa;
			} else if ("SFMTH".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sSFMTH;
			} else if ("SFMTH_A".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sSFMTHa;
			} else if ("FORWARD_TO_HOME_NO"
					.equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sFORWARD_TO_HOME_NO;
			} else if ("S_FORWARD_TO_HOME_NO".equals(TeRtA
					.getString("MAP_VALUE"))) {
				sValue = sS_FORWARD_TO_HOME_NO;
			}
			logger.debug("MAP_VALUE:" + sMap + "=" + sValue + ",StepNo:"
					+ sStepNo + ",DataType:" + sDataType + ",TypeCode:"
					+ sTypeCode);

			if (sTypeCode.equals("1909") && ("0".equals(sValue))) {
				logger.debug("Follow Me To Home did not work");
			} else if (sTypeCode.equals("1911") && ("".equals(sValue))) {
				logger.debug("Follow Me To Home did not Active");
			} else if (sTypeCode.equals("1942") && ("0".equals(sValue))) {
				logger.debug("SMS Follow Me To Home did not work");
			} else if (sTypeCode.equals("1944") && ("".equals(sValue))) {
				logger.debug("SMS Follow Me To Home did not Active");
			} else {
				Process_ServiceOrderItemDtl();
			}
		}
	}


/*	public void Query_PreProcessResult(String rcode)
			throws SQLException, InterruptedException, Exception {
		logger.info("Query_PreProcessResult...");
		cRCode = "";

		// rcode = "000"; //****************************************

		sSql = "update S2T_TB_TYPB_WO_SYNC_FILE_DTL set s2t_operationdate="
				+ "to_date('" + dateFormat2.format(new Date())
				+ "','YYYYMMDDHH24MISS')" + " where WORK_ORDER_NBR='"
				+ cWorkOrderNBR + "'";

		logger.debug("update S2T_TB_TYPB_WO_SYNC_FILE_DTL:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();

		sSql = "update S2T_TB_SERVICE_ORDER_ITEM set timestamp=" + "to_date('"
				+ dateFormat2.format(new Date()) + "','YYYYMMDDHH24MISS')"
				+ " where Service_Order_NBR='" + cServiceOrderNBR + "'";

		logger.debug("Update S2T_TB_SERVICE_ORDER_ITEM:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();

		sSql = "update S2T_TB_SERVICE_ORDER set timestamp=" + "to_date('"
				+ dateFormat2.format(new Date()) + "','YYYYMMDDHH24MISS')"
				+ " where SERVICE_ORDER_NBR='" + cServiceOrderNBR + "'";

		logger.debug("Update S2T_TB_SERVICE_ORDER:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();

		desc = Load_ResultDescription(rcode);
	}*/

	public String Load_ResultDescription(String sDecs) throws SQLException {
		logger.info("Load_ResultDescription...");
		sDecs = "000";

		String sD = "";
		Temprs = null;

		sSql = "Select describe from S2T_TB_RESULT" + " where RESULT_FLAG ='"
				+ sDecs + "'";

		Temprs = conn.createStatement().executeQuery(sSql);

		while (Temprs.next()) {
			sD = Temprs.getString("describe");
		}

		return sD;
	}

	public void Query_GPRSStatus() throws IOException, SQLException {
		logger.info("Query_GPRSStatus...");
		String sG = "";
		cGPRS = "";
		Temprs = null;
		sSql = "SELECT nvl(PDPSUBSID,0) as ab FROM basicprofile WHERE msisdn = '"
				+ cS2TMSISDN + "'";
		logger.debug("Query_GPRSStatus:" + sSql);
		Temprs = conn.createStatement().executeQuery(sSql);
		Temprs = conn.createStatement().executeQuery(sSql);
		while (Temprs.next()) {
			sG = Temprs.getString("ab");
		}
		logger.debug("GPRS_Values:" + sG);
		if ((sG.equals("0")) || (sG.equals(""))) {
			cGPRS = "0";
		} else {
			cGPRS = "1";
		}
	}
	public String Query_ServiceOrderStatus() throws SQLException, InterruptedException, IOException {
	      String cMesg = "";
	      
	      for(int i = 0; i < 15; i++) {
	         Thread.sleep(2000);
	         
	         Temprs = null;
	         sSql="select STATUS from S2T_TB_SERVICE_ORDER Where SERVICE_ORDER_NBR ='"+
	              cServiceOrderNBR+"'";
	         
	         logger.info(sSql);
	         Temprs = conn.createStatement().executeQuery(sSql);
	         
	        while(Temprs.next()) {
	           cMesg = Temprs.getString("STATUS");
	        }
	        
	        logger.info("Query_ServiceOrderStatus:"+Integer.toString(i)+" Times "+ cMesg);
	        
	        if(cMesg.equals("Y") || cMesg.equals("F")){
	        	break;
	        }
	       }
	      
	      if(cMesg.equals("Y") || cMesg.equals("F")) {
	            cMesg = Query_SyncFileDtlStatus();
	            if (cMesg.equals("")) {
	              cMesg="501";
	            }
	       }
	     else {cMesg="501";
	        }
	      return cMesg;
	  }
	 public String Query_SyncFileDtlStatus() throws SQLException, InterruptedException, IOException{
	      String cSt="";
	     for (int i=0;i<5;i++){
	        Thread.sleep(1000);
	        Temprs=null;
	        sSql="select result_flag from S2T_TB_TYPB_WO_SYNC_FILE_DTL Where " +
	                "SERVICE_ORDER_NBR ='"+cServiceOrderNBR+"'";
	        Temprs=conn.createStatement().executeQuery(sSql);
	        while (Temprs.next()){
	                cSt=Temprs.getString("result_flag");
	        }
	        logger.info("Query_SyncFileDtlStatus:"+Integer.toString(i)+" Times "+cSt);
	            }

	     return cSt;
	    }

}

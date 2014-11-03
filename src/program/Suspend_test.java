package program;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class Suspend_test {

	
	private String cS2TIMSI;
	private String csta;
	private boolean bb;
	private int iError;
	private String iErrorMsg;
	private ResultSet Temprs;
	private String cTicketNumber;
	private String sSql;
	Connection conn =null;
	private String cRCode;
	private String cReqStatus;
	private String Process_Code;
	private String sMNOSubCode;
	private String cTWNLDIMSI;
	private String cS2TMSISDN;
	private String sWSFStatus;
	private String sWSFDStatus;
	private String cTWNLDMSISDN;
	private String cServiceOrderNBR;
	private String sDATE;
	private SimpleDateFormat dFormat1=new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat dFormat2=new SimpleDateFormat("MM/dd/yyyy HH24:mi:ss");
	private String c910SEQ;
	private String sCount;
	private String cFileName;
	private String cFileID;
	private String dReqDate;
	private String cWorkOrderNBR;
	private String Sdate;
	private String sMNOName;
	private String sSubCode;
	private String sStepNo;
	private String sFORWARD_TO_HOME_NO;
	private String sSFMTH;
	private String sFMTHa;
	private String sFMTH;
	private String sSFMTHa;
	private String sS_FORWARD_TO_HOME_NO;
	private String sCMHKLOGID;
	private String sDataType;
	private String sValue;
	private String sTypeCode;
	private String sMap;
	private String cMSISDNOLD;
	private String cM205OT;
	private String cMVLN;
	private String sM_CTYPE;
	private String cGPRSStatus;
	private String cGPRS;
	static Vector<String> vln=new Vector<String>();


	public void ReqStatus_17(PrintWriter out17) throws SQLException,IOException, ClassNotFoundException, Exception {
		// else if (cReqStatus.equals("17")){
/*		logger.debug("ReqStatus_17");
*/
		// 確認配對，移除
		/*csta = Check_Pair_IMSI(cTWNLDIMSI, cS2TIMSI);
		if (csta.equals("1")) {*/
			/*csta = "";*/
			//確認中華門號狀態，移除
			/*csta = Check_TWN_Msisdn_Status(cTWNLDIMSI, cS2TIMSI);
			if (!"0".equals(csta)) {*/
				csta = "";
				csta = Update_GPRSStatus();
				switch (Integer.parseInt(csta)) {
				case 0: {// Check S2T IMSI
					bb = Validate_IMSIRange(cS2TIMSI);
					if (bb == true) {
						// Check CHT MSISDN
						//確認中華門號範圍,移除
						/*bc = Validate_PartnerMSISDNRange(cTWNLDMSISDN);
						if (bc == true) {*/
							//確認中華門號是否存在
							/*Get_GurrentS2TMSISDN();
							if (!cS2TMSISDN.equals("")) {*/
								ReqStatus_17_Act(out17);
							/*} else {
								iError = 1;
								Query_PreProcessResult(out17, "108");
								if ("".endsWith(iErrorMsg))
									iErrorMsg += ",";
								iErrorMsg += "Error Code 108!";
							}*/
						/*} else {
							iError = 1;
							Query_PreProcessResult(out17, "109");
							if ("".endsWith(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";
						}*/
					} else {
						iError = 1;
						/*Query_PreProcessResult(out17, "101");*/
						if ("".endsWith(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";
					}
				}
					break;
				case 107:
					iError = 1;
					/*Query_PreProcessResult(out17, "107");*/
					if ("".endsWith(iErrorMsg))
						iErrorMsg += ",";
					iErrorMsg += "Error Code 107!";
					break;
				default:
					break;
				}
			/*} else {
				Query_PreProcessResult(out17, "211");
			}*/
		/*} else {
			Query_PreProcessResult(out17, "111");
		}*/
	}
	
	
	public void ReqStatus_17_Act(PrintWriter out17) throws SQLException,
			IOException, ClassNotFoundException, Exception {
/*		logger.debug("ReqStatus_17_Act");
*/
		//確認台灣門號的 MAP VALUE，移除
		/*Check_Type_Code_87_MAP_VALUE(cS2TMSISDN);*/
		sWSFStatus = "V";
		sWSFDStatus = "V";
		Process_SyncFile(sWSFStatus);
		Process_SyncFileDtl(sWSFDStatus);
		Process_ServiceOrder();
		// Process_WorkSubcode();
		Process_WorkSubcode_05_17(cS2TIMSI, cTWNLDIMSI, cReqStatus,
				cTWNLDMSISDN);
		sSql = "update S2T_TB_SERVICE_ORDER set STATUS='N' where "
				+ "SERVICE_ORDER_NBR='" + cServiceOrderNBR + "'";
		conn.createStatement().executeUpdate(sSql);
/*		logger.debug("update SERVICE_ORDER:" + sSql);
*/		/*Query_PreProcessResult(out17, "000");*/
		Query_GPRSStatus();
	}
	
	
	public String Update_GPRSStatus() throws SQLException, IOException {
		String sCoun = "";
		Temprs = null;

		sSql = "Select count(subscr_id) as ab from S2T_TB_TYPB_WO_SYNC_FILE_DTL "
				+ "where subscr_id ='" + cTicketNumber + "'";

/*		logger.debug("Update_GPRSStatus:" + sSql);
*/		Temprs = conn.createStatement().executeQuery(sSql);

		while (Temprs.next()) {
			sCoun = Temprs.getString("ab");
		}

		if (Integer.parseInt(sCoun) > 0) {
			return "107";
		} else {
			return "0";
		}
	}
	
	public boolean Validate_IMSIRange(String s2timsi) throws SQLException, ClassNotFoundException, IOException{
	    String minvalue="",maxvalue="",TmpSql="",sR="",sR1="";
	    Temprs=null;
	    int iL=0;
	    sSql="SELECT minvalue, maxvalue FROM numbervalidation WHERE mnosubcode='"+
	            sMNOSubCode +"' AND checktype='I'";
	    Temprs=conn.createStatement().executeQuery(sSql);
	    while (Temprs.next()){
	      minvalue=Temprs.getString("minvalue");
	      maxvalue=Temprs.getString("maxvalue");
	      TmpSql=TmpSql+" SELECT 'OK' result FROM dual WHERE '"+cTWNLDIMSI+
	              "' BETWEEN '"+minvalue+"' AND '"+maxvalue+"' UNION ";
	    }
	    Temprs=null;
	    if (TmpSql.length()>0){
	    iL=TmpSql.lastIndexOf("UNION");
	    TmpSql=TmpSql.substring(0, iL);
/*	            logger.info("Validate_IMSIRange:"+sSql);
*/	           Temprs=conn.createStatement().executeQuery(sSql);
	        Temprs=conn.createStatement().executeQuery(sSql);
	    while (Temprs.next()){
	      sR=Temprs.getString("result");
	      }
	    }
	        Temprs=null;
	    if (!sR.equals("OK")){return false;}
	    else {TmpSql="";
	    sSql="SELECT minvalue, maxvalue FROM numbervalidation WHERE mnosubcode='000'" +
	            " AND checktype='I'";
	    Temprs=conn.createStatement().executeQuery(sSql);
	    while (Temprs.next()){
	      minvalue=Temprs.getString("minvalue");
	      maxvalue=Temprs.getString("maxvalue");
	      TmpSql=TmpSql+" SELECT 'OK' result FROM dual WHERE '"+s2timsi+
	              "' BETWEEN '"+minvalue+"' AND '"+maxvalue+"' UNION ";
	    }
	    Temprs=null;
	    if (TmpSql.length()>0){
	    iL=TmpSql.lastIndexOf("UNION");
	    TmpSql=TmpSql.substring(0, iL);
/*	    logger.debug("Check_s2timsi:"+TmpSql);
*/	        Temprs=conn.createStatement().executeQuery(TmpSql);
	    while (Temprs.next()){
	      sR1=Temprs.getString("result");
	      }
	    }
	    if (sR1.equals("OK")){return true;}
	    else {return false;}}

	}
	
	public void Process_SyncFile(String sSFStatus) throws SQLException,
			Exception {
		// 格式為YYYYMMDDXXX
		sDATE = dFormat1.format(new Date());
		c910SEQ = sDATE + sCount;
		cFileName = "S2TCI" + c910SEQ + ".950";
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
				+ c910SEQ.substring(8, 11) + "','950',sysdate,'" + sSFStatus
				+ "')";
/*		logger.debug("Process_SyncFile:" + sSql);
*/		conn.createStatement().executeUpdate(sSql);
	}
	
	public void Process_SyncFileDtl(String sSFDStatus) throws SQLException, IOException{
        int iv,ix=0;
        String sVl="",sC,sH;
		Sdate = dFormat2.format(new Date());

        cWorkOrderNBR="";
            Temprs=conn.createStatement().executeQuery("select S2T_SQ_WORK_ORDER.nextval as ab from dual");
            while (Temprs.next()){
                cWorkOrderNBR=Temprs.getString("ab");
            }
        Temprs=null;
            cServiceOrderNBR="";
            Temprs=conn.createStatement().executeQuery("select S2T_SQ_SERVICE_ORDER.nextval as ab from dual");
            while (Temprs.next()){
                cServiceOrderNBR=Temprs.getString("ab");
            }
      sSql="INSERT INTO S2T_TB_TYPB_WO_SYNC_FILE_DTL (WORK_ORDER_NBR,"+
           "WORK_TYPE, FILE_ID, SEQ_NO, CMCC_OPERATIONDATE, ORIGINAL_CMCC_IMSI,"+
           "ORIGINAL_CMCC_MSISDN, S2T_IMSI, S2T_MSISDN, FORWARD_TO_HOME_NO, "+
           "FORWARD_TO_S2T_NO_1, IMSI_FLAG, STATUS, SERVICE_ORDER_NBR, SUBSCR_ID)"+
           " VALUES ("+cWorkOrderNBR+",'"+ cReqStatus+"',"+ cFileID+",'"+
           c910SEQ+"',to_date('"+Sdate+"','MM/dd/yyyy HH24:mi:ss'),'"+
           cTWNLDIMSI+"','+"+ cTWNLDMSISDN+"','"+ cS2TIMSI+"','"+ cS2TMSISDN+"','+"+
           cTWNLDMSISDN+"','"+cTWNLDMSISDN+"', '2', '"+sSFDStatus+"','"+
           cServiceOrderNBR+"','"+cTicketNumber+"')";
/*      logger.debug("Process_SyncFileDtl:"+sSql);
*/      
      conn.createStatement().executeUpdate(sSql);
      
      //vln 17 不需設定 ，移除
      /*if (vln.size()>0){
         vln.firstElement();
          for (iv=0; iv<vln.size();iv++){
            sVl=(String) vln.get(iv);
             ix=sVl.indexOf(",");
             sC=sVl.substring(0, ix);
             sVl=sVl.substring(ix+1, sVl.length());
             ix=sVl.indexOf(",");
             sH=sVl.substring(0, ix);
             sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set VLN_"+sC+"='"+sH+
             "' where WORK_ORDER_NBR="+cWorkOrderNBR+" and SERVICE_ORDER_NBR='"+
             cServiceOrderNBR+"'";
             conn.createStatement().executeUpdate(sSql); }}*/
    }
	
	public void Process_ServiceOrder() throws SQLException, IOException {
		sSql = "INSERT INTO S2T_TB_SERVICE_ORDER (SERVICE_ORDER_NBR, "
				+ "WORK_TYPE, S2T_MSISDN, SOURCE_TYPE, SOURCE_ID, STATUS, "
				+ "CREATE_DATE) " + "VALUES ('" + cServiceOrderNBR + "','"
				+ cReqStatus + "','" + cS2TMSISDN + "'," + "'B_TYPE',"
				+ cWorkOrderNBR + ", '', sysdate)";

/*		logger.info("Process_ServiceOrder[1]:" + sSql);
*/
		conn.createStatement().executeUpdate(sSql);
		Temprs = null;

		sSql = "Select MNO_NAME from S2T_TB_MNO_COMPANY "
				+ "Where MNO_SUB_CODE='" + sMNOSubCode + "'";

/*		logger.debug("Process_ServiceOrder[2]:" + sSql);
*/		Temprs = conn.createStatement().executeQuery(sSql);

		while (Temprs.next()) {
			sMNOName = Temprs.getString("MNO_NAME");
		}
	}
	
    public void Process_WorkSubcode_05_17(String S2TImsiB,String TWNImsiB,String sReqStatus,String sTWNLDMSISDN) throws SQLException, IOException{
        Temprs=null;
        String cMd="",Ssvrid="";
        sSql="select nvl(serviceid,'0') as ab from imsi "+
              " where imsi = '"+S2TImsiB+"' and homeimsi='"+TWNImsiB+
              "'";
/*        logger.info("Get_Serviceid:"+sSql);
*/        Temprs=conn.createStatement().executeQuery(sSql);
        while (Temprs.next()){
          Ssvrid=Temprs.getString("ab");
        }
        if (!Ssvrid.equals("0")){
          Temprs=null;
              sSql="select count(serviceid) as ab from serviceparameter where "+
                   "parameterid=3792 and serviceid='"+Ssvrid+"'";
/*              logger.info("Check_Follow_Me_To_Home(有1表示有申請, 0表示未申請):"+sSql);
*/              Temprs=conn.createStatement().executeQuery(sSql);
              while (Temprs.next()){ //(有1表示有申請, 0表示未申請)
                sFMTH=Temprs.getString("ab");
              }

              if (sFMTH.equals("1")){
                  Temprs=null;
                  sSql="select nvl(value,'2') as ab From parametervalue where "+
                       "parametervalueid=3793 and serviceid='"+Ssvrid+"'";
/*                  logger.info("Check_Follow_Me_To_Home_Status(Value=1: active, Value=0: inactive, 若未申請, 則2):"+sSql);
*/                  Temprs=conn.createStatement().executeQuery(sSql);
                  while (Temprs.next()){ //(Value=1: active, Value=0: inactive, 若未申請, 則NULL)
                    sFMTHa=Temprs.getString("ab");
                  }
              }
              Temprs=null;
              sSql="select count(serviceid) as ab from serviceparameter where "+
                   "parameterid=3748 and serviceid='"+Ssvrid+"'";
/*              logger.info("Check_SMS_Follow_Me_To_Home(有1表示有申請, 0表示未申請):"+sSql);
*/              Temprs=conn.createStatement().executeQuery(sSql);
              while (Temprs.next()){ //(有1表示有申請, 0表示未申請)
                sSFMTH=Temprs.getString("ab");
              }

              if (sSFMTH.equals("1")){
                  Temprs=null;
                  sSql="select nvl(value,'2') as ab From parametervalue where "+
                       "parametervalueid=3752 and serviceid='"+Ssvrid+"'";
/*                  logger.info("Check_SMS_Follow_Me_To_Home_Status(Value=1: active, Value=0: inactive, 若未申請, 則2):"+sSql);
*/                  Temprs=conn.createStatement().executeQuery(sSql);
                  while (Temprs.next()){ //(Value=1: active, Value=0: inactive, 若未申請, 則NULL)
                    sSFMTHa=Temprs.getString("ab");
                  }
              }
              if (sReqStatus.equals("17")){
              Temprs=null;
              sSql="select nvl(value,'0') as ab from parametervalue where parametervalueid=3792 "+
                   "and serviceid='"+Ssvrid+"'";
/*              logger.info("Check_FORWARD_TO_HOME_NO:"+sSql);
*/                  Temprs=conn.createStatement().executeQuery(sSql);
                  while (Temprs.next()){
                    sFORWARD_TO_HOME_NO=Temprs.getString("ab");
                  }
              if (sFORWARD_TO_HOME_NO.equals('0')){sFORWARD_TO_HOME_NO=null;}
               Temprs=null;
              sSql="select nvl(value,'0') as ab from parametervalue where parametervalueid=3748 "+
                   "and serviceid='"+Ssvrid+"'";
/*              logger.info("Check_S_FORWARD_TO_HOME_NO:"+sSql);
*/                  Temprs=conn.createStatement().executeQuery(sSql);
                  while (Temprs.next()){
                    sS_FORWARD_TO_HOME_NO=Temprs.getString("ab");
                  }
                  if (sS_FORWARD_TO_HOME_NO.equals('0')){sS_FORWARD_TO_HOME_NO=null;}
            }
              else{
                sFORWARD_TO_HOME_NO=sTWNLDMSISDN;
                sS_FORWARD_TO_HOME_NO=sTWNLDMSISDN;
                sTWNLDMSISDN=null;
              }
        }

               sSql="Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"+
                       sMNOName+"' And work_type='"+cReqStatus+"' Order by step_no";
/*                logger.debug("Process_WorkSubcode_05_17:"+sSql);
*/                Temprs=conn.createStatement().executeQuery(sSql);
                 while (Temprs.next()){
                   sSubCode=Temprs.getString("subcode");
                   sStepNo=Temprs.getString("step_no");
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();
                 }
                 
                 //不需要更新provLog
                /* sSql="update PROVLOG " +
                 "set STEP='"+sStepNo+"' "+
                 " where LOGID="+sCMHKLOGID;
                 conn.createStatement().executeUpdate(sSql);*/
     }

	public void Process_ServiceOrderItem() throws SQLException, IOException {
		sSql = "Insert into S2T_TB_SERVICE_ORDER_ITEM (SERVICE_ORDER_NBR,"
				+ "STEP_NO, SUB_CODE, IDENTIFIER, STATUS, SEND_DATE) "
				+ "Values (" + cServiceOrderNBR + "," + sStepNo + ",'"
				+ sSubCode + "',"
				+ " S2T_SQ_SERVICE_ORDER_ITEM.nextval, 'N', sysdate)";
/*		logger.debug("Process_ServiceOrderItem:" + sSql);
*/		conn.createStatement().executeUpdate(sSql);
	}
	
	public void Process_DefValue() throws SQLException, IOException {
		ResultSet TeRt = null;
		sSql = "Select TYPE_CODE, DATA_TYPE, DEF_VALUE "
				+ "From S2T_TB_SUBCODE_TYPECODE " + "Where subcode='"
				+ sSubCode + "' And work_type='" + cReqStatus
				+ "' And MNO_NAME='" + sMNOName + "' And DEF_VALUE is not null";
/*		logger.debug("Process_DefValue:" + sSql);
*/		TeRt = conn.createStatement().executeQuery(sSql);
		while (TeRt.next()) {
			sTypeCode = TeRt.getString("TYPE_CODE");
			sDataType = TeRt.getString("DATA_TYPE");
			sValue = TeRt.getString("DEF_VALUE");
			Process_ServiceOrderItemDtl();
		}
	}

	public void Process_MapValue() throws SQLException, IOException {
		ResultSet TeRtA = null;
		sSql = "Select TYPE_CODE, DATA_TYPE, MAP_VALUE "
				+ "From S2T_TB_SUBCODE_TYPECODE " + "Where subcode='"
				+ sSubCode + "' And work_type='" + cReqStatus
				+ "' And MNO_NAME='" + sMNOName + "' And MAP_VALUE is not null";
/*		logger.debug("Process_MapValue:" + sSql);
*/		TeRtA = conn.createStatement().executeQuery(sSql);
		while (TeRtA.next()) {
			sTypeCode = TeRtA.getString("TYPE_CODE");
			sDataType = TeRtA.getString("DATA_TYPE");
			sMap = "";
			sMap = TeRtA.getString("MAP_VALUE");
			if ("S2T_MSISDN".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cS2TMSISDN;
			} else if ("S2T_IMSI".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cS2TIMSI;
			} else if ("TWNLD_MSISDN".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cTWNLDMSISDN;
			} else if ("TWNLD_IMSI".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cTWNLDIMSI;
			} else if ("S2T_MSISDN_OLD".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cMSISDNOLD;
			} else if ("M_205_OT".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cM205OT;
			} else if ("M_VLN".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cMVLN;
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
			/*logger.debug("MAP_VALUE:" + sMap + "=" + sValue + ",StepNo:"
					+ sStepNo + ",DataType:" + sDataType + ",TypeCode:"
					+ sTypeCode);*/
			if (sTypeCode.equals("1909") && (sValue.equals("0"))) {
/*				logger.debug("Follow Me To Home did not work");
*/			} else if (sTypeCode.equals("1911") && (sValue.equals(""))) {
/*				logger.debug("Follow Me To Home did not Active");
*/			} else if (sTypeCode.equals("1942") && (sValue.equals("0"))) {
/*				logger.debug("SMS Follow Me To Home did not work");
*/			} else if (sTypeCode.equals("1944") && (sValue.equals(""))) {
/*				logger.debug("SMS Follow Me To Home did not Active");
*/			} else {
				Process_ServiceOrderItemDtl();
			}
		}
	}
	
	public void Process_ServiceOrderItemDtl() throws SQLException, IOException {
		sSql = "Insert into S2T_TB_SERVICE_ORDER_ITEM_DTL "
				+ "(SERVICE_ORDER_NBR, STEP_NO, TYPE_CODE, DATA_TYPE, VALUE) "
				+ "VALUES (" + cServiceOrderNBR + "," + sStepNo + ","
				+ sTypeCode + "," + sDataType + ",'" + sValue + "')";
/*		logger.debug("Process_ServiceOrderItemDtl:" + sSql);
*/		conn.createStatement().executeUpdate(sSql);
	}
	
	public void Query_GPRSStatus() throws IOException, SQLException {
		String sG = "";
		cGPRS = "";
		Temprs = null;
		sSql = "SELECT nvl(PDPSUBSID,0) as ab FROM basicprofile WHERE msisdn = '"
				+ cS2TMSISDN + "'";
/*		logger.debug("Query_GPRSStatus:" + sSql);
*/		Temprs = conn.createStatement().executeQuery(sSql);
		Temprs = conn.createStatement().executeQuery(sSql);
		while (Temprs.next()) {
			sG = Temprs.getString("ab");
		}
/*		logger.debug("GPRS_Values:" + sG);
*/		if ((sG.equals("0")) || (sG.equals(""))) {
			cGPRS = "0";
		} else {
			cGPRS = "1";
		}
	}
	
}

/*
在Process_SyncFile中
寫入TABLE S2T_TB_TYPEB_WO_SYNC_FILE 
參數
sDATE = 今天日期 format yyyyMMdd
sCount = 從 seqrec table 撈出 
	"select currentseq,count(currentseq) as ab from seqrec where "+
            "MNOSUBCODE='"+sMNOSubCode+"' and currentdate='"+sDATE+
                    "' group by currentseq";
	如果ab>0，則sCount=currentseq+1，並更新回去
	否則sCount=1，刪除"MNOSUBCODE='"+sMNOSubCode+" 資料
		更新回去

	數字補0成3碼


在Process_SyncFileDtl
insert S2T_TB_TYPB_WO_SYNC_FILE_DTL
ORIGINAL_CMCC_IMSI=cTWNLDIMSI
ORIGINAL_CMCC_MSISDN=cTWNLDMSISDN
FORWARD_TO_HOME_NO=cTWNLDMSISDN
FORWARD_TO_S2T_NO_1=cTWNLDMSISDN

在Process_ServiceOrder
insert S2T_TB_SERVICE_ORDER

依照sMNOSubCode，select 出 MNO_NAME

在Process_WorkSubcode_05_17
select from imsi where imsi = '"+S2TImsiB+"' and homeimsi='"+TWNImsiB
homeimsi=cTWNLDIMSI
放入到Ssvrid

不需要更新provLog
確認sFMTH、sFMTHa、sSFMTH、sSFMTHa是不是需要MAP_VALUE設定

 */

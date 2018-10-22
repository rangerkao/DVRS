
/** Program History
 * 
 * 20141008 開始CODING，第一版
 * 20141009 將功能集合成工具java檔，完成mail寄送功能
 * 20141009 完成批價功能，修正批次Query語法錯誤(將rownum以minus取代)
 * 20141209 修改日簡訊內容帶出當月累計金額
 * 20141013 完成警示簡訊功能
 * 20141013 決定CDR無MCCMNC時的處理方式
 * 20141014 完成GPRS中止功能
 * 20141014 新增對應表，修正日期比對方式a.before(b)
 * 20141015 調整mail發送
 * 20141015 新增定時執行排程功能
 * 20141016 UI端建立，完成權限驗證
 * 20141021 完成UI執行外部程式功能
 * 20141029 修改SMS寄送方式，從交由smpp發送改為使用http post方式
 * 20141029 修改簡訊Table，將設定與內容分開，以msg ID 對應
 * 20141029 完成由UI可進行操作Proccess功能
 * 20141103 測試並已確認GPRS中止功能可運作
 * 20141104 UI新增警示上限頁面，menu內容由後端控制
 * 20141113 修改VIP客戶不進行斷網
 * 20141113 新增1.5G、2.0G流量警示功能
 * 20141118 修改VIP客戶以每5000塊進行警示
 * 20141118 考慮之後也許有客制上限功能，修改table Schema，目前以0表示
 * 20141118 新增追蹤中斷GPRS要求的狀態
 * 20141118 新增華人上網包不批價計費
 * 20141125 UI 套用BootStrap樣式
 * 20141125 修改Daycap判斷，如果為負值，表示不參考
 * 20141201 新增舊資料判斷Set集合
 * 20141204 UI新增日、月累計頁面
 * 20141204 新增menu小工具，以ID查詢簡訊，以門號查詢VIP
 * 20141209 新增每日500塊警示
 * 20141209 將取出日累計由未限制改為只取近兩個月
 * 20141211 新增由IP對應到MCCMNC功能
 * 20141215 二版(未上線)，依資費套用簡訊設定，批價不轉換幣別，部分設定改以table取出
 * 20141216 修改，因每日500簡訊會帶出當月金額，修改當客戶已經斷網後不發送每日500警示
 * 20150115 將累計由IMSI改為SERVICE ID為單位累計(避免換卡換號造成無法累計)
 * 20150115 修改華人上網包檢查，取消檢查門號
 * 20150309 NTT要求以mail通知流量警示
 * 20150316 修改Jtool檔案，取消發送帳號與寄件者相同的限制(所以之前發送給ntt不成功)
 * 20150317 新增判斷，如果GPRS中斷要求結果不成功(000)，發送警示amil
 * 20150324 修改日累計Mccmnc到新key值(國碼+業者名稱)
 * 
 */





package program;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class DVRSmain extends TimerTask{

	
	//DB config
/*	private  final String DriverClass="oracle.jdbc.driver.OracleDriver";
	private  final String Host="10.42.1.101";
	private  final String Port="1521";
	private  final String ServiceName=":S2TBSDEV";
	private  final String UserName="foyadev";
	private  final String PassWord="foyadev";

	private  final String URL = "jdbc:oracle:thin:@"+ Host + ":"+Port+ServiceName; */
	
	//HUR
	static Connection conn = null;
	//MBOSS
	static Connection conn2 = null;
	
	private static  Logger logger ;
	static Properties props=new Properties();

	static boolean resume = false;
	static boolean resumeSpeed = false;
	
	//mail conf
	static String mailSender="";
	//static String mailReceiver="";
	static String mailSubject="mail test";
	static String mailContent="mail content text";
	

	
	private static String sql="";
	private static String errorMsg="";
	
	//Hur Data conf
	private static Integer dataThreshold=null;//CDR資料一批次取出數量
	//private static Integer lastfileID=null;//最後批價檔案號
	private static Double exchangeRate=null; //港幣對台幣匯率，暫訂為4
	private static Double kByte=null;//RATE單位KB，USAGE單位B
	
	//日期設定
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	SimpleDateFormat year_month_day_sdf = new SimpleDateFormat("yyyyMMdd");
	SimpleDateFormat year_month_sdf = new SimpleDateFormat("yyyyMM");
	SimpleDateFormat day_hour_sdf = new SimpleDateFormat("ddHH");
	SimpleDateFormat day_time_sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	//系統時間，誤差一小時，系統資料處理時間為當時時間提前一小時
	private String programeDateTime="";
	private String programeTime="";
	private String sYearmonth="";
	private String sYearmonthday="";
	//上個月
	private String sYearmonth2="";
	
	//20180201 add 執行時間
	//private String executeDateTime = "";
	
	
	//預設值
	private static int RUN_INTERVAL=3600;//單位秒
	private static String DEFAULT_MCCMNC=null;//預設mssmnc
	//private static Double DEFAULT_THRESHOLD=null;//預設月警示量
	//private static Double DEFAULT_DAY_THRESHOLD=null;//預設日警示量
	//private static Double DEFAULT_DAYCAP=null;
	//private static Double DEFAULT_VOLUME_THRESHOLD=null;//預設流量警示(降速)，1.5GB;
	//private static Double DEFAULT_VOLUME_THRESHOLD2=null;//預設流量警示(降速)，15GB;
	private static String DEFAULT_PHONE=null;
	private static Boolean TEST_MODE=true;
	private static String HKNetReceiver;
	private static String AlertReceiver;
	
	//多排程處理
	private static boolean executing =false;
	private static boolean hasWaiting = false;
	
	static //20160706 add
	long subStartTime = 0;

	
	static Set<Map<String,String>> serviceOrderNBR = new HashSet<Map<String,String>>();
	
	
	static Map <String,Double> cdrChargeMap = new HashMap<String,Double>();
	
	
	//20150505 add
	static Map <String,Set<String>> updateMap = new HashMap<String,Set<String>>();
	static Map <String,Map <String,Set<String>>> updateMapD = new HashMap<String,Map <String,Set<String>>>();
	static Map <String,Map <String,Set<String>>> updateMapDD = new HashMap<String,Map <String,Set<String>>>();
	//20151230 add 對華人上網包客戶在所申請區域時，不進行斷網
	//20151231 cancel
	/*static Set <String> addonMark = new HashSet<String>();
	static Set <String> notAddonMark = new HashSet<String>();*/
	
	
	
	
	
	
	//double pocketLimit = 500*1024;//KB
	
	/*************************************************************************
	 *************************************************************************
	 *                                程式參數設定
	 *************************************************************************
	 *************************************************************************/
	
	//20180223
	Set<String> userDataAnnounceSet = new HashSet<String>();
	
	public boolean setUserDataAnnounceSet() {
		boolean result = false;
		logger.info("setUserDataAnnounceSet...");
		userDataAnnounceSet.clear();
		
		sql = ""
				+ "select A.SEND_NUMBER "
				+ "from HUR_SMS_LOG A "
				+ "where A.CREATE_DATE>= sysdate-7 "
				+ "AND A.TYPE = 'DATA'";
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				userDataAnnounceSet.add(rs.getString("SEND_NUMBER"));
			}
			
			result = true;

		} catch (SQLException e) {
			ErrorHandle("At set setUserDataAnnounceSet Got a SQLException", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));

		return result;
	}
	
	// TODO
	//Map<String,Double> qosResetVolumeMap = new HashMap<String,Double>();
	Map<Integer,Map<String,List<Map<String,Object>>>> qosResetSettingMap = new HashMap<Integer,Map<String,List<Map<String,Object>>>>();
	//priceplanid-> type -> List (mccmnsc,volume,times
	
	public boolean setQosResetSettingMap() {
		boolean result = false;
		logger.info("setQosResetSettingMap...");
		//qosResetVolumeMap.clear();
		qosResetSettingMap.clear();
		
		sql = ""
				+ "select A.PRICEPLANID,A.RESET_CONDITION,A.MCCNET,A.MAX_TIMES,A.TYPE "
				+ "from HUR_QOSRESET_SETTING A "
				+ "where 1 = 1 "
				//+ "and A.TYPE = 0 "
				+ "and to_date(A.START_TIME,'yyyyMMddhh24miss')<=to_date('"+programeDateTime+"','yyyyMMddhh24miss') "
				+ "and (A.END_TIME is null OR to_date(A.END_TIME,'yyyyMMddhh24miss')>=to_date('"+programeDateTime+"','yyyyMMddhh24miss'))";
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				Map<String,Object> m = new HashMap<String,Object>();
				
				Integer type = (rs.getString("TYPE")!=null ? rs.getInt("TYPE"):null);
				m.put("MCCNET", rs.getString("MCCNET"));
				m.put("VOLUME", rs.getDouble("RESET_CONDITION"));
				m.put("TIMES", rs.getString("MAX_TIMES")==null?null:Integer.parseInt(rs.getString("MAX_TIMES")));
				
				
				for(String priceplanid : rs.getString("PRICEPLANID").trim().split(",")) {
					List<Map<String,Object>> l = null;
					Map<String,List<Map<String,Object>>>  typeMap = null;
					
					if(qosResetSettingMap.containsKey(type)) {
						typeMap = qosResetSettingMap.get(type);
						
						if(typeMap.containsKey(priceplanid)) {
							l = typeMap.get(priceplanid);
						}else {
							l = new ArrayList<Map<String,Object>>();
						}
					}else {
						typeMap = new HashMap<String,List<Map<String,Object>>>();
						l = new ArrayList<Map<String,Object>>();
					}
					l.add(m);
					typeMap.put(priceplanid, l);
					qosResetSettingMap.put(type, typeMap);
				}
			}
			
			result = true;

		} catch (SQLException e) {
			ErrorHandle("At set setQosResetSettingMap Got a SQLException", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));

		return result;
	}
	
	Map<String,Map<String,Integer>> qosD_ResetLogMap = new HashMap<String,Map<String,Integer>>();
	//目前上網包都為互斥，重置流量相同，簡單紀錄
	Map<String,List<Map<String,String>>> qosM_ResetLogMap = new HashMap<String,List<Map<String,String>>>();
	
	
	
	public boolean setqosResetLogMap() {
		boolean result = false;
		logger.info("setqosD_ResetLogMap...");
		qosD_ResetLogMap.clear();
		qosM_ResetLogMap.clear();
		
		sql = ""
				+ "select MSISDN,MCCMNC,COUNT(1) TIMES "
				+ "from HUR_QOSRESET_LOG A "
				+ "where to_char(A.CREATETIME,'yyyyMMdd') = '"+sYearmonthday+"' "
				+ "and TYPE = 'D_TYPE' "
				+ "group by MSISDN,MCCMNC ";
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				Map<String,Integer> m = new HashMap<String,Integer>();
				m.put(rs.getString("MCCMNC"), rs.getInt("TIMES"));
				qosD_ResetLogMap.put(rs.getString("MSISDN"), m);
			}
			
			rs = null;
			
			/*sql = ""
					+ "select MSISDN,MCCMNC,COUNT(1) TIMES "
					+ "from HUR_QOSRESET_LOG A "
					+ "where to_char(A.CREATETIME,'yyyyMM') = '"+sYearmonth+"' "
					+ "and TYPE = 'M_RESET' "
					+ "group by MSISDN,MCCMNC ";*/
			
			sql = ""
					+ "select MSISDN,MCCMNC,to_char(A.CREATETIME,'yyyyMMdd') TIME "
					+ "from HUR_QOSRESET_LOG A  "
					+ "where to_char(A.CREATETIME,'yyyyMM') = '"+sYearmonth+"' "
					+ "and TYPE = 'M_RESET' ";
			
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				
				String msisdn = rs.getString("MSISDN");
				Map<String,String> m = new HashMap<String,String>();
				//m.put("TIMES", rs.getString("TIMES"));
				m.put("MCCMNC", rs.getString("MCCMNC"));
				m.put("TIME", rs.getString("TIME"));
				
				List<Map<String,String>> l = null;
				if(qosM_ResetLogMap.containsKey(msisdn)) {
					l = qosM_ResetLogMap.get(msisdn);
				}else {
					l = new ArrayList<Map<String,String>>();
				}
				l.add(m);
				qosM_ResetLogMap.put(msisdn, l);
			}
			
			
			result = true;

		} catch (SQLException e) {
			ErrorHandle("At set setqosD_ResetLogMap Got a SQLException", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));

		return result;
	}
	
	Map<String,Date> qosResetLogTimeMap = new HashMap<String,Date>();
	
	public boolean setQosResetLogTimeMap() {
		boolean result = false;
		logger.info("setQosResetLogTimeMap...");
		qosResetLogTimeMap.clear();
		
		sql = ""
				+ "select A.MSISDN,TO_CHAR(MAX(A.CREATETIME),'yyyyMMddhh24miss') CREATETIME "
				+ "	from HUR_QOSRESET_LOG A "
				+ " where to_char(A.CREATETIME,'yyyyMMdd') = '"+sYearmonthday+"' "
				+ "	group by A.MSISDN  ";
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				qosResetLogTimeMap.put(rs.getString("MSISDN"), day_time_sdf.parse(rs.getString("CREATETIME")));
			}
			
			result = true;

		} catch (SQLException e) {
			ErrorHandle("At set setQosResetLogTimeMap Got a SQLException", e);
		} catch (ParseException e) {
			ErrorHandle("At set setQosResetLogTimeMap Got a ParseException", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));

		return result;
	}
	
	//20180223
	//Set<String> dataAnnounceSet = new HashSet<String>();
	
	Map<String,Map<String,String>> dataAnnounceMap = new HashMap<String,Map<String,String>>();
	
	public boolean setDataAnnounceMap() {
		boolean result = false;
		logger.info("setDataAnnounceMap...");
		dataAnnounceMap.clear();
		
		sql = ""
				+ "select A.PRICEPLANID,A.MCC,A.COUNTRY_CH "
				+ "from HUR_DATA_ANNOUCE A ";
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				String priceplanid = rs.getString("PRICEPLANID");
				Map<String,String> m = null;
				if(dataAnnounceMap.containsKey(priceplanid)) {
					m = dataAnnounceMap.get(priceplanid);
				}else {
					m = new HashMap<String,String>();
				}
				m.put(rs.getString("MCC"), convertString(rs.getString("COUNTRY_CH"),"ISO-8859-1","Big5"));
				dataAnnounceMap.put(priceplanid, m);
			}
			
			result = true;

		} catch (SQLException e) {
			ErrorHandle("At set setDataAnnounceMap Got a SQLException", e);
		} catch (UnsupportedEncodingException e) {
			ErrorHandle("At set setDataAnnounceMap Got a SQLException", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));

		return result;
	}
	
	public boolean sendDataUsageAnnounce(String serviceid,String phone,String country,Double dayCap) {
		boolean result = false;
		
		//取得簡訊內容
		String msgids = null ;
		
		if(dayCap!=null && dayCap>0)
			msgids = getSystemConfigParam("0", "DATA_USAGE_ANNOUNCE_DAYCAP");
		else
			msgids = getSystemConfigParam("0", "DATA_USAGE_ANNOUNCE_NO_DAYCAP");

		
		
		if(msgids != null){
			
			String [] msgid = msgids.split(",");
			
			sendSMS(serviceid, msgid, phone,
					new String[] {"{{date_start}}","{{country}}","{{amount}}","{{customerService}}"},
					//yyyy-MM-dd
					new String[] {sYearmonthday.substring(0, 4)+"-"+sYearmonthday.substring(4, 6)+"-"+sYearmonthday.substring(6, 8),
							country,
							dayCap!=null?dayCap.toString().replaceAll("\\.\\d+", ""):"",
									queryCustomerServicePhone(serviceid, false)},
					"DATA");
			
		}else {
			sql="";
			ErrorHandle("For number "+phone+" occured ERROR AT GET DATA_USAGE_ANNOUNCE.");
		}
		
		return result;
	}
	
	
	
	//20160719 add
	/**
	 * 設定流量包
	 */
	
	//20160715 add	
	//key:ServiceID,subMap key:MCC,Map start_date,end_date,currency,
	static Map<String,List<Map<String,String>>> volumePocketMap = new HashMap<String,List<Map<String,String>>>();//流量包對應

	private boolean setVolumePocketMap(){
		logger.info("setVolumePocketMap...");
		volumePocketMap.clear();
		volumeList.clear();
		/*sql = ""
				+ "SELECT A.SERVICEID,A.MCC,A.START_DATE,A.END_DATE,A.CURRENCY,A.ALERTED,A.TYPE,A.LIMIT "
				+ "FROM HUR_VOLUME_POCKET A WHERE A.CANCEL_TIME IS NULL ";*/
		
		sql = ""
				+ "select distinct A.SERVICEID,A.MCC,A.CURRENCY,A.TYPE,A.LIMIT,A.ALERTED,B.PID,B.START_DATE,B.END_DATE,A.TERMINATE,A.IS_RESUME "
				+ "from HUR_VOLUME_POCKET A,(	select A.PID,MIN(A.START_DATE) START_DATE,MAX(A.END_DATE) END_DATE "
				+ "								from HUR_VOLUME_POCKET A WHERE A.CANCEL_TIME IS NULL group by A.PID ) B "
				//+ "WHERE A.PID = B.PID and A.TERMINATE = 0 AND A.TYPE = 1 ";
				//+ "WHERE A.PID = B.PID AND A.TYPE = 1 AND A.END_DATE>= to_char(sysdate-2,'yyyyMMdd') ";
				//20170626 mod
				+ "WHERE A.PID = B.PID AND A.END_DATE>= to_char(sysdate-2,'yyyyMMdd') ";
	
		boolean result = false;
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				Map<String, String> m = new HashMap<String,String>();
				m.put("START_DATE", rs.getString("START_DATE"));
				m.put("END_DATE", rs.getString("END_DATE"));
				m.put("CURRENCY", rs.getString("CURRENCY"));
				m.put("ALERTED", rs.getString("ALERTED"));
				m.put("TYPE", rs.getString("TYPE"));
				m.put("LIMIT", rs.getString("LIMIT"));
				m.put("PID", rs.getString("PID"));
				m.put("MCC", rs.getString("MCC"));				
				m.put("TERMINATE", rs.getString("TERMINATE"));
				m.put("IS_RESUME", rs.getString("IS_RESUME"));
				
				String serviceid = rs.getString("SERVICEID");
				List<Map<String, String>> volumePocketList = volumePocketMap.get(serviceid);
				if(volumePocketList == null) volumePocketList = new ArrayList<Map<String, String>>();
				
				volumePocketList.add(m);
				volumePocketMap.put(serviceid, volumePocketList);
			}
			
			result = true;
			
			//20160913 add
			result = setVolumeList();
		} catch (SQLException e) {
			ErrorHandle("At set setVolumePocketMap Got a SQLException", e);
		} catch (Exception e) {
			sql="";
			ErrorHandle("At setVolumePocketMap occur Exception error!", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		return result;
	}
	
	//20160721 add
	/**
	 * 當美國流量包開始計算時，寄出簡訊告知
	 * @return
	 */
	/*private boolean sendStartPocketDateSMS(){
		logger.info("sendStartPocketDateSMS...");
		boolean result =false;
		for(String serviceid : volumePocketMap.keySet()){
			for(String mcc : volumePocketMap.get(serviceid).keySet()){
				for(Map<String,String> m : volumePocketMap.get(serviceid).get(mcc)){
					if("0".equals(m.get("TYPE")) && sYearmonthday.equals(m.get("START_DATE"))){
						//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
						String phone = getMSISDN(serviceid);

						if(phone==null ||"".equals(phone)){
							sql="";
							ErrorHandle("At sendStartPocketDateSMS sendAlertSMS occur error! The serviceid:"+serviceid+",MCC:"+mcc+" can't find msisdn to send! ");
							continue;
						}
						String startDate = m.get("START_DATE");
						String endDate = m.get("END_DATE");
						String msgID = getSystemConfigParam("0", "VOLUME_POCKET_START_MSG");
						//發送簡訊
						sendSMS(serviceid,msgID.split(","),phone,
								new String[]{"{{date_start}}","{{date_end}}"},
								new String[]{startDate.substring(4,6)+"/"+startDate.substring(6,8),endDate.substring(4,6)+"/"+endDate.substring(6,8)});
					}
				}			
			}
		}		
		
		checkPocketStart = false;
		return result;
	}*/

	//20160824 add
	/**
	 * 當美國流量包開始結束時，寄出簡訊告知
	 * @return
	 */
	/*private boolean sendEndPocketDateSMS(){
		logger.info("sendEndPocketDateSMS...");
		boolean result =false;
		for(String serviceid : volumePocketMap.keySet()){
			for(String mcc : volumePocketMap.get(serviceid).keySet()){
				for(Map<String,String> m : volumePocketMap.get(serviceid).get(mcc)){
					if("0".equals(m.get("TYPE")) && sYearmonthday.equals(m.get("END_DATE"))){
						//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
						String phone=getMSISDN(serviceid);
	
						if(phone==null ||"".equals(phone)){
							sql="";
							ErrorHandle("At checkVolumePocket sendAlertSMS occur error! The serviceid:"+serviceid+",MCC:"+mcc+" can't find msisdn to send! ");
							continue;
						}
						String startDate = m.get("START_DATE");
						String endDate = m.get("END_DATE");
						String msgID = getSystemConfigParam("0", "VOLUME_POCKET_END_MSG");
						//發送簡訊
						sendSMS(serviceid,msgID.split(","),phone,
								new String[]{"{{date_start}}","{{date_end}}"},
								new String[]{startDate.substring(4,6)+"/"+startDate.substring(6,8),endDate.substring(4,6)+"/"+endDate.substring(6,8)});
					}
				}
			}
		}		
		
		checkPocketEnd = false;
		return result;
	}*/
	
	private boolean doEndPocket(){
		logger.info("doEndPocket...");
		boolean result =false;
		
		try {
			for(String serviceid : volumePocketMap.keySet()){
				for(Map<String,String> m : volumePocketMap.get(serviceid)){
					String endDateS = m.get("END_DATE");
					String type = m.get("TYPE");
					String pid = (String)m.get("PID");
					
					Date endDate = year_month_day_sdf.parse(endDateS);
					String sMNOSubCode = null;
					/*if("1".equals(type)){
						//1:Joy
						//20161123 發現因parse後為末天0點，當0點後則會終止，不符合
						//應該在最末天整天還可使用
						endDate = new Date(endDate.getTime()+1000*60*60*24-1000*60*31);
						sMNOSubCode="950";
					}*/
						
					//Annex 因時差問題，到最末天23點即終止服務	
					/*if("2".equals(type)){
						endDate = new Date(endDate.getTime()+1000*60*60*23-1000*60*31);
						sMNOSubCode="982";
					}*/
					
					//GO2PLAY 用到最後的24時
					if("3".equals(type)){
						endDate = new Date(endDate.getTime()+1000*60*60*24-1000*60*31);
						sMNOSubCode="983";
					}
					
					//YunYoBo 用到最後的24時
					if("4".equals(type)){
						endDate = new Date(endDate.getTime()+1000*60*60*24-1000*60*31);
						sMNOSubCode="984";
					}
					
					//FanTravel 用到最後的24時
					if("5".equals(type)){
						endDate = new Date(endDate.getTime()+1000*60*60*24-1000*60*31);
						sMNOSubCode="987";
					}
					
					//還未被終止，已過期，進行終止
					if("0".equals(m.get("TERMINATE")) && day_time_sdf.parse(programeDateTime).after(endDate)){
						//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
						String msisdn = getMSISDN(serviceid);
						if(msisdn==null ||"".equals(msisdn)){
							sql="";
							ErrorHandle("At doEndPocket, The serviceid:"+serviceid+" can't find msisdn!");
							continue;
						}
						
						String imsi = msisdnMap.get(serviceid).get("IMSI");
						if(imsi==null ||"".equals(imsi)){
							sql="";
							ErrorHandle("At doEndPocket, The serviceid:"+serviceid+" can't find imsi!");
							continue;
						}
						
						suspendGPRS sus = new suspendGPRS(conn,conn2,logger);

						PreparedStatement pst = null;
						try {
							//退租並recycle
							//20141118 add 傳回suspend排程的 service order nbr
							//Map<String,String> orderNBR = sus.doTerminate(imsi, msisdn, pricplanID, "1");
							//20160920 輸入0取消直接recycle
							//20170620 因加入Annex所以必須將MNO CODE變為參數
							Map<String,String> orderNBR = sus.doTerminate(imsi, msisdn, "0",sMNOSubCode);
							
							serviceOrderNBR.add(orderNBR);
							
							sql=
									"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
									+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS,IS_SLOWDOWN,PROCESS_DAY) "
									+ "VALUES(?,?,SYSDATE,?,?,?,'"+sYearmonthday+"' )";
							
							pst=conn.prepareStatement(sql);
							pst.setString(1,orderNBR.get("cServiceOrderNBR") );
							pst.setString(2,imsi );
							pst.setString(3,msisdn );
							pst.setString(4,"Terminate" );
							pst.setString(5,"3" );
							logger.info("Execute SQL : "+sql);
							pst.executeUpdate();
							//更新Volume終止flag
							Map<String,String> mm = new HashMap<String,String>();
							mm.put("PID", pid);
							mm.put("TERMINATE", "1");
							updateVolumePocketMap.add(mm);
							
							
						} catch (SQLException e) {
							ErrorHandle("At doEndPocket occur SQLException error!", e);
						} catch (Exception e) {
							sql="";
							ErrorHandle("At doEndPocket occur Exception error!", e);
						}finally{
							try {
								if(pst!=null) pst.close();
								if(sus.Temprs!=null) sus.Temprs.close();
							} catch (SQLException e) {
							}
						}
					}
				}
			}
			result = true;
		} catch (Exception e) {
			sql="";
			ErrorHandle("At doEndPocket occur Exception error!", e);
		}
		endPocket = false;
		return result;
	}
	
//	private String markName(String name){
//		String s = null;
//		if(name!=null && name.length()>0){
//			s = name.substring(0,1);
//			for(int i=1 ; i<name.length();i++){
//				s+="*";
//			}
//		}
//			
//		return s;
//	}
	
	//已轉移至每日報表
	public void sendVolumeReport(){
		logger.info("sendVolumeReport...");
		
		
		
		
		Statement st = null;
		ResultSet rs = null;
		
		Map<String,Map<String,Map<String,String>>> totalCount = new HashMap<String,Map<String,Map<String,String>>>();
		
		try{
			st = conn.createStatement();
			
			
			//統計每月有在美國使用總天數與總量，MO日期、CD天數、SU量
			sql=
					"select A.SERVICEID,substr(A.day,0,6) MO,count(1) CD,sum(A.volume) SU "
					+ "from HUR_CURRENT_DAY A "
					+ "where A.MCCMNC like '310%' "
					+ "group by A.Serviceid,substr(A.day,0,6)";
			
			
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");

			while(rs.next()){
				Map<String,Map<String,String>> m1 = new HashMap<String,Map<String,String>>();
				Map<String,String> m2 = new HashMap<String,String>();
				String serviceid = rs.getString("SERVICEID");
				String month = rs.getString("MO");

				if(totalCount.containsKey(serviceid)){
					m1 = totalCount.get(serviceid);
				}
				
				m2.put("DAY", String.valueOf(rs.getInt("CD")));
				m2.put("VOLUME", String.valueOf(rs.getDouble("SU")/1024/1024));
				
				m1.put(month, m2);
				totalCount.put(serviceid, m1);
			}
			
			rs = null;
			
			//統計每月在美國流量包中使用天數與總量，PID、CD天數、SU量
			Map<String,Map<String,String>> subCount = new HashMap<String,Map<String,String>>();
			
			sql=
					"select A.PID,count(1) CD ,sum(B.s) SU "
					+ "from HUR_VOLUME_POCKET A,(	select B.Serviceid,B.day,sum(B.volume) s "
					+ "								from HUR_CURRENT_DAY B "
					+ "								where B.MCCMNC like '310%' "
					+ "								group by B.Serviceid,B.day) B "
					+ "where A.SERVICEID = B.Serviceid AND A.START_DATE<=B.day AND A.END_DATE>=B.day "
					+ "group by A.PID "
					+ "order by A.pid ";
			
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");

			while(rs.next()){
				Map<String,String> m1 = new HashMap<String,String>();
				m1.put("DAY", String.valueOf(rs.getInt("CD")));
				m1.put("VOLUME", String.valueOf(FormatDouble(rs.getDouble("SU")/1024/1024, "0.0000")));
				subCount.put(rs.getString("PID"), m1);
			}
			
			rs = null;
			
			
			//建立report
			String report = "";
			report+="<html><head></head><body><table>";

			String [] v = new String[]{
					"中華門號",
					"起始時間",
					"結束時間",
					//"Email",
					"已警示",
					"建立時間",
					"取消時間",
					"客戶姓名",
					//"進線者姓名",
					"手機型號",
					"期間內流量(MB)",
					"期間外流量(MB)",
					"期間內使用天數",
					"時間外使用天數",
					};
			report += pfString(v);
			
			sql=
					"SELECT A.SERVICEID,A.PID,B.FOLLOWMENUMBER CHTMSISDN,A.SERVICEID,A.MCC,A.ALERTED,A.ID,A.CALLER_NAME,A.CUSTOMER_NAME,A.PHONE_TYPE,A.EMAIL,A.CANCEL_REASON, "
					+ "A.START_DATE,A.END_DATE,"
					+ "TO_CHAR(A.CREATE_TIME,'yyyy/MM/dd hh24:mi:ss') CREATE_TIME,TO_CHAR(A.CANCEL_TIME,'yyyy/MM/dd hh24:mi:ss') CANCEL_TIME "
					+ "from HUR_VOLUME_POCKET A,FOLLOWMEDATA B "
					+ "WHERE A.SERVICEID = B.SERVICEID(+) AND A.TYPE=0 AND B.FOLLOWMENUMBER like '886%' "
					+ "ORDER BY A.START_DATE DESC ";
			
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");

			while(rs.next()){
				String serviceid = rs.getString("SERVICEID");
				String pid = rs.getString("PID");
				String id = convertString(rs.getString("ID"),"ISO-8859-1","Big5");
				String cusName = convertString(rs.getString("CUSTOMER_NAME"),"ISO-8859-1","Big5");
				//String calName = convertString(rs.getString("CALLER_NAME"),"ISO-8859-1","Big5");
				String startDate = rs.getString("START_DATE");
				String endDate = rs.getString("END_DATE");
				if(!id.matches("^\\d+$")){
					//cusName = markName(cusName);
					//calName = markName(calName);
				}
				int totalday = 0;
				double totleVolume = 0d;
				if(totalCount.containsKey(serviceid)){
					String startMonth = startDate.substring(0,6);
					String endMonth = endDate.substring(0,6);
					String d;
					if(totalCount.get(serviceid).containsKey(startMonth)){
						d = totalCount.get(serviceid).get(startMonth).get("DAY");
						totalday += (d==null?0:Integer.parseInt(d));
						d = totalCount.get(serviceid).get(startMonth).get("VOLUME");
						totleVolume += (d==null?0d:Double.parseDouble(d));
					}
					if(!startMonth.endsWith(endMonth)&&totalCount.get(serviceid).containsKey(endMonth)){
						d = totalCount.get(serviceid).get(endMonth).get("DAY");
						totalday += (d==null?0:Integer.parseInt(d));
						d = totalCount.get(serviceid).get(endMonth).get("VOLUME");
						totleVolume += (d==null?0d:Double.parseDouble(d));
					}
				}
				
				int inDay = (subCount.get(pid)!=null?Integer.parseInt(subCount.get(pid).get("DAY")):0);
				double inVolume = (subCount.get(pid)!=null?Double.parseDouble(subCount.get(pid).get("VOLUME")):0.d);
				
				report += pfString(new String[]{
						rs.getString("CHTMSISDN"),
						startDate,
						endDate,
						//convertString(rs.getString("EMAIL"),"ISO-8859-1","Big5"),
						rs.getString("ALERTED"),
						rs.getString("CREATE_TIME"),
						nvl(rs.getString("CANCEL_TIME")," "),
						cusName,
						//calName,
						convertString(rs.getString("PHONE_TYPE"),"ISO-8859-1","Big5"),
						//String.valueOf(volumeList.get(pid)==null?FormatDouble(0d, "0.0000"):FormatDouble((Double) volumeList.get(pid)/1024/1024, "0.0000")),
						String.valueOf(inVolume),
						String.valueOf(FormatDouble(Math.abs(totleVolume-inVolume), "0.0000")),
						String.valueOf(inDay),
						String.valueOf(totalday-inDay),
						});
			}
			report+="</table></body></html>";
		
			//sendMail("美國流量包Report", report, "DVRS Report", "Galen.Kao@sim2travel.com,douglas.chuang@sim2travel.com,yvonne.lin@sim2travel.com,ranger.kao@sim2travel.com");
			sendHTMLMail("美國流量包Report", report, "DVRS Report", "Galen.Kao@sim2travel.com,douglas.chuang@sim2travel.com,yvonne.lin@sim2travel.com,ranger.kao@sim2travel.com");
			//sendHTMLMail("美國流量包Report", report, "DVRS Report", "ranger.kao@sim2travel.com");
			

				

		} catch (SQLException e) {
			ErrorHandle("At set sendVolumeReport Got a SQLException", e);
		} catch (UnsupportedEncodingException e) {
			ErrorHandle("At set sendVolumeReport Got a UnsupportedEncodingException", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		volumeReport = false ;

	}
	
	public static String convertString(String msg,String sCharset,String dCharset) throws UnsupportedEncodingException{
		
		if(msg==null)
			msg=" ";
		
		return sCharset==null? new String(msg.getBytes(),dCharset):new String(msg.getBytes(sCharset),dCharset);
	}
	public static String nvl(String msg,String s){
		if(msg==null)
			msg = s;
		return msg;
	}
	public static String pfString(String[] value){
		String r ="";
		r+="<tr>";
		
		for(int i = 0;i<value.length;i++){
			if(i==value.length-1)
				r+="<td align='right'>";
			else 
				r+="<td>";
			
			r+= value[i]+"</td>";
		}
		r+="</tr>";
		
		return r;
	}
	
	
	
	public static boolean isChinese(char c) {  
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);     
        if (   ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS  
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS  
            //|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A  
            //|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B  
            //|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION  
            //|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS  
            )  
            //|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION)   
        {  
                return true;  
        }  
        return false;  
    }  
	
		
	/**
	 * 設定計費週期
	 * 取特定日期那個月的，前面加上calendar.setTime(date);設定date日期
	 */
	private boolean setDayDate(){
		logger.info("setDayDate...");
		subStartTime = System.currentTimeMillis();

		//目前時間
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		
		//TODO test
		//calendar.set(2018,0, 25, 1, 30, 0);
		System.out.println(calendar.getTime());
		
		calendar.setTimeInMillis(calendar.getTimeInMillis()-1000*60*60);	//系統時間提前一小時,當00：30執行時，所處理的資料為前一天的23：30
		//20170214 add
		programeDateTime = day_time_sdf.format(calendar.getTime());
		programeTime = day_hour_sdf.format(calendar.getTime());
		sYearmonth = year_month_sdf.format(calendar.getTime());
		sYearmonthday = year_month_day_sdf.format(calendar.getTime());
		
		//20180201 add 執行時間
		//executeDateTime = day_time_sdf.format(new Date());
		
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));	//上個月時間，減掉Month會-30天，採取到1號向前，確定跨月
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-1);
		sYearmonth2 = year_month_sdf.format(calendar.getTime());
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		//當執行時間為1日0點時進行復網
		if("0100".equals(programeTime)){
			resume = true;
			NTTalerted = false;
			NTT75alerted = false;
			updateSystemConfig("NTT_ALERTED","0"); //將月份為單位的NTT警示Flag歸零
			
			//20160901 月初時重新撈取Month day 資料
			reloadMonth = true;
			reloadDay = true;
			
		}
		//201607 add
		//當執行時間為0點時(實際為01:30)進行速度恢復
		if("00".equals(programeTime.substring(2))){
			resumeSpeed = true;
			//checkPocketStart = true;
			volumeReport = true;
		}
		//希望在22:30時執行
		/*if("21".equals(programeTime.substring(2))){
			checkPocketEnd = true;
		}*/
		//希望在00:30時執行
		if("23".equals(programeTime.substring(2))){
			endPocket = true;
		}
		
		//中午Check mail
		/*if("11".equals(programeTime.substring(2))){
			logger.info("Send resumeReport result.");
			String mailReceiver = "k1988242001@gmail.com";
			if(!TEST_MODE){
				mailReceiver+=",Yvonne.lin@sim2travel.com";
			}
			
			sendMail("DVRS noon checkMail.","DVRS noon checkMail.","DVRS Report",mailReceiver);
		}*/
		
		sendMail("DVRS checkMail.","DVRS checkMail.","DVRS Report",props.getProperty("mail.Receiver"));
		
		return true;
	}
	
	

	/**
	 * 設定華人上網包MCCMNC資料 20141118 排除華人上網包
	 */
	static Set<String> sSX001 = new HashSet<String>();
	static Set<String> sSX002 = new HashSet<String>();
	static Set<String> sSX004 = new HashSet<String>();
	static Set<String> sSX005 = new HashSet<String>();
	static Set<String> sSX006 = new HashSet<String>();
	
	static private boolean setQosData(){
		
		logger.info("setQosData...");
		sSX001.clear();
		sSX002.clear();
		sSX004.clear();
		sSX005.clear();
		sSX006.clear();
		
		subStartTime = System.currentTimeMillis();
		//sSX001 香港
		/*sSX001.add("45412");
		sSX001.add("454CMHK");*/
		sSX001.add("454");
		
		//sSX002 香港+大陸
		/*sSX002.add("46001");
		sSX002.add("46007");
		sSX002.add("46002");
		sSX002.add("460000");
		sSX002.add("46000");
		sSX002.add("45412");
		
		sSX002.add("460China Unicom");
		sSX002.add("460CMCC");
		sSX002.add("454CMHK");*/
		sSX002.add("460");
		sSX002.add("454");
		
		//sSX004 多國上網包  (中、港、泰、美、加、俄、澳洲、德、英、法、巴西)
		sSX004.add("460");//中
		sSX004.add("454");//港
		sSX004.add("520");//泰
		sSX004.add("310");//美
		sSX004.add("316");//美
		sSX004.add("302");//加
		sSX004.add("250");//俄
		sSX004.add("505");//澳洲
		sSX004.add("262");//德
		sSX004.add("234");//英
		sSX004.add("208");//法
		sSX004.add("724");//巴西
		
		//sS005高量華人上網包
		sSX005.add("460");
		sSX005.add("454");
		
		//sSX006 美國流量包
		sSX006.add("310");//美
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		
		return true;
	}
	

	/*************************************************************************
	 *************************************************************************
	 *                                表格資料設定
	 *************************************************************************
	 *************************************************************************/
	
	//20160624 add
	/**
	 * 設定降速參數
	 * Map<Type,List<Map<String,Value>>>
	 */
	//20160624 add
		//List<Map<String,String>> slowDownList = new ArrayList<Map<String,String>>();
		static Map<String,List<Map<String,String>>> slowDownMap = new HashMap<String,List<Map<String,String>>>();
		private boolean setSlowDownList(){
			
			logger.info("setSlowDownList...");
			slowDownMap.clear();
			boolean result = false;
			
			subStartTime = System.currentTimeMillis();
			
			List<Map<String,String>> l1 = new ArrayList<Map<String,String>>();
			List<Map<String,String>> l0 = new ArrayList<Map<String,String>>();
			
			Statement st = null;
			ResultSet rs = null;
			try {
				st = conn.createStatement();
		
				//撈出日期適用區間在今天
				//201706__  添加Type以應更多變化
				sql = "SELECT A.MCCMNC,A.PRICEPLANID,A.LIMIT,A.START_DATE,A.END_DATE,A.ISDAYLY,A.GPRS_NAME,A.TYPE "
						+ "from HUR_SLOWDOWN_LIST A "
						+ "WHERE sysdate > =to_date(A.START_DATE,'yyyyMMddhh24miss') AND (A.END_DATE is null or sysdate<=to_date(A.END_DATE,'yyyyMMddhh24miss')+1)";
				
				logger.debug("Query SlowDownList SQL : "+sql);
				rs = st.executeQuery(sql);
				logger.info("Query end!");
				
				while(rs.next()){
					Map<String,String> m = new HashMap<String,String>();
					m.put("MNO", rs.getString("MCCMNC"));
					m.put("PRICEPLAN", rs.getString("PRICEPLANID"));
					m.put("LIMIT", rs.getString("LIMIT"));//KB轉byte
					m.put("TIMESTART", rs.getString("START_DATE"));
					m.put("TIMEEND", rs.getString("END_DATE"));
					m.put("GPRS_NAME", rs.getString("GPRS_NAME"));
					//20170603 add
					m.put("TYPE", rs.getString("TYPE"));
					
					//簡單區分每日降速與非每日降速
					if("1".equals(rs.getString("ISDAYLY"))){
						l1.add(m);
					}else{
						l0.add(m);
					}
					
				}
				slowDownMap.put("1",l1);
				slowDownMap.put("0",l0);

				result = true;
			} catch (SQLException e) {
				ErrorHandle("At set setSlowDownList Got a SQLException", e);
			}finally{
				try {
					if(st!=null)
						st.close();
					
					if(rs!=null)
						rs.close();
				} catch (SQLException e) {
				}
			}
			logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));			
			return result;
		}
		
	//new Version
	/**
	 * NTD_MONTH_LIMIT
	 * NTD_DAY_LIMIT
	 * HKD_MONTH_LIMIT
	 * HKD_DAY_LIMIT
	 * VOLUME_LIMIT1
	 * VOLUME_LIMIT2
	 */
	static Map<String,Map<String,String>> systemConfig = new HashMap<String,Map<String,String>>();
	public boolean setSystemConfig(){
		
		logger.info("setSystemConfig...");
		boolean result = false;
		subStartTime = System.currentTimeMillis();
		systemConfig.clear();
		Statement st = null;
		ResultSet rs = null;
		try {
			sql="SELECT A.NAME,A.VALUE,A.DESCR,A.PRICE_PLAN_ID FROM HUR_DVRS_CONFIG A";
			st = conn.createStatement();
			logger.debug("Query SystemConfig SQL : "+sql);
			rs = st.executeQuery(sql);
			while(rs.next()){

				//20160704 add
				//針對NTT的警示處理
				if("NTT_ALERTED".equals(rs.getString("NAME"))){
					if("100".equals(rs.getString("VALUE"))){
						NTTalerted = true;
					}else if("75".equals(rs.getString("VALUE"))){
						NTT75alerted = true;
					}
				}else{
					String pricePlanId = rs.getString("PRICE_PLAN_ID");
					if(pricePlanId==null) pricePlanId = "0"; //global parameter
					
					for(String id : pricePlanId.split(",")){
						Map<String,String> m = new HashMap<String,String>();
						if(systemConfig.containsKey(id)){
							m = systemConfig.get(id);
						}
						m.put(rs.getString("NAME"), rs.getString("VALUE"));
						systemConfig.put(id, m);
					}
				}
			}
			result= true;
			
			
			//必須資料Check
			Set<String> checkList = new HashSet<String>();
			checkList.add("NTD_MONTH_LIMIT");
			checkList.add("HKD_MONTH_LIMIT");
			checkList.add("NTD_DAY_LIMIT");
			checkList.add("HKD_DAY_LIMIT");
			checkList.add("VOLUME_LIMIT1");//1.5GB
			checkList.add("VOLUME_LIMIT2");//2.0GB
			
			for(String s: checkList){
				if(systemConfig.get("0").get(s)==null){
					sql="";
					ErrorHandle("Can' found set parameter "+s);
					result = false;
					break;
				}
			}

		} catch (SQLException e) {
			ErrorHandle("At set SystemConfig Got a SQLException", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	//new version end

	/**
	 * 尋找最後一次更改的fileID，以及目標處理的最終ID
	 */
	/*private boolean setLastFileID(){
		logger.info("setLastFileID...");
		subStartTime = System.currentTimeMillis();
		boolean result = false;
		
		Statement st = null;
		ResultSet rs = null;
		
		try {
			sql="SELECT MAX(A.LAST_FILEID) id FROM HUR_CURRENT A";
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				lastfileID=rs.getString("id");
			}
			logger.info("Last process file ID :"+lastfileID);
			
			result = true;
		} catch (SQLException e) {
			logger.error("At setLastFileID Got a SQLException", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setLastFileID occur SQLException error!");
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}*/
	
	/**
	 * 取出 HUR_CURRENT table資料
	 * 建立成
	 * Map 
	 * Key:MONTH,Value:Map(serviceid,Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_VOLUME)))
	 */
	static Map<String,Map<String,Map<String,String>>> currentMap = new HashMap<String,Map<String,Map<String,String>>>();
	static Map <String,Set<String>> existMap = new HashMap<String,Set<String>>();
	private boolean setCurrentMap(){
		
		logger.info("setCurrentMap...");
		currentMap.clear();
		existMap.clear();
		subStartTime = System.currentTimeMillis();
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		
		//設定取出此月與上月的資料
		try {
			sql=
					"SELECT A.SERVICEID,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,"
					+ "to_char(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,"
					+ "A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				Map<String,String> map=new HashMap<String,String>();
				Map<String,Map<String,String>> map2=new HashMap<String,Map<String,String>>();

				String serviceid =rs.getString("SERVICEID");
				String month=rs.getString("MONTH");
				
				if(currentMap.containsKey(month)){
					map2=currentMap.get(month);
				}
				
				map.put("LAST_FILEID", rs.getString("LAST_FILEID"));
				map.put("SMS_TIMES", rs.getString("SMS_TIMES"));
				map.put("LAST_DATA_TIME", rs.getString("LAST_DATA_TIME"));
				map.put("CHARGE", rs.getString("CHARGE"));
				map.put("VOLUME", rs.getString("VOLUME")==null?"0":rs.getString("VOLUME"));
				map.put("EVER_SUSPEND", rs.getString("EVER_SUSPEND"));
				map.put("LAST_ALERN_THRESHOLD", rs.getString("LAST_ALERN_THRESHOLD"));
				map.put("LAST_ALERN_VOLUME", rs.getString("LAST_ALERN_VOLUME"));

				map2.put(serviceid, map);
				currentMap.put(month,map2);
				
				
				
				//20141201 add 設定存在資料，避免重複Insert
				Set<String> set =new HashSet<String>();
				if(existMap.containsKey(month)){
					set=existMap.get(month);
				}
				set.add(serviceid);
				existMap.put(month, set);
			}
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setCurrentMap occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		reloadMonth = false;
		return result;
	}
	
	/**
	 * 保留這個月舊資料，作為預測費用使用
	 */
	static Map<String,Double> oldChargeMap = new HashMap<String,Double>();
	private boolean setoldChargeMap(){
		logger.info("setoldChargeMap...");
		oldChargeMap.clear();
		
		subStartTime = System.currentTimeMillis();
		if(currentMap.containsKey(sYearmonth)){
			for(String serviceid : currentMap.get(sYearmonth).keySet()){
				oldChargeMap.put(serviceid, parseDouble((String)currentMap.get(sYearmonth).get(serviceid).get("CHARGE")));
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		return true;
	}
	
	//20160913 add
	//key:PID,Volume
	static Map<String,Double> volumeList = new HashMap<String,Double>();//流量統計
	private boolean setVolumeList(){
		boolean result = false;
		
		logger.info("setVolumeList...");
		volumeList.clear();
		
		try {
			for(String serviceid : volumePocketMap.keySet()){
				for(Map<String,String> m : volumePocketMap.get(serviceid)){
					double pidv = 0d;
					
					String startDate = (String) m.get("START_DATE");
					String endDate = (String) m.get("END_DATE");
					String pid = (String) m.get("PID");
					String type = m.get("TYPE");
					
					Date start = year_month_day_sdf.parse(startDate);
					Date end = year_month_day_sdf.parse(endDate);
					
					//從日累計計算流量
					for(String day : currentDayMap.keySet()){
						Date cday =  year_month_day_sdf.parse(day);
						//在期間內，而且有此用戶的資料
						if((cday.getTime()-start.getTime())>=0 && (cday.getTime()-end.getTime())<=0 && currentDayMap.get(day).containsKey(serviceid)){
							//如果是3 go2play 或 4 yunyobo，不分區域累計流量
							if("3".equals(type)||"4".equals(type)) {
								for(String mccmnc: currentDayMap.get(day).get(serviceid).keySet()) {
									pidv += parseDouble((String) currentDayMap.get(day).get(serviceid).get(mccmnc).get("VOLUME"));	
								}
							}
						}
					}
					volumeList.put(pid, pidv);
				}
			}
			result = true;
		} catch (ParseException e) {
			ErrorHandle("At setVolumeList got ParseException.",e);
		}catch (Exception e) {
			ErrorHandle("At setVolumeList got Exception.",e);
		}

		/*
		try {
			for(String day : currentDayMap.keySet()){
				for(String serviceid : currentDayMap.get(day).keySet()){
					if(volumePocketMap.containsKey(serviceid)){

						for(String mccmnc : currentDayMap.get(day).get(serviceid).keySet()){
							String mcc = mccmnc.substring(0, 3);
							if(volumePocketMap.get(serviceid).containsKey(mccmnc.substring(0, 3))){
								for(Map<String,String> m:volumePocketMap.get(serviceid).get(mcc)){
									
									if("3".equals(m.get("TYPE"))||"4".equals(m.get("TYPE"))) {
										//GO2PLAY不需要累計
										//yunyobo 另外處理
									}else //yunyobo 不分國 
										if(){
										String startDate = (String) m.get("START_DATE");
										String endDate = (String) m.get("END_DATE");
										String pid = (String) m.get("PID");
										
										if(day.equals(startDate)||
												(	year_month_day_sdf.parse(day).after(year_month_day_sdf.parse(startDate))&& 
													year_month_day_sdf.parse(day).before(year_month_day_sdf.parse(endDate)))||
												day.equals(endDate)){
												Double v = volumeList.get(pid);
												for(String kmcc : currentDayMap.get(day).get(serviceid).keySet()) {
													v += parseDouble((String) currentDayMap.get(day).get(serviceid).get(kmcc).get("VOLUME"));
													volumeList.put(pid, v);
												}
		
										}
										
										
									}else {
										String startDate = (String) m.get("START_DATE");
										String endDate = (String) m.get("END_DATE");
										String pid = (String) m.get("PID");
										
										if(day.equals(startDate)||
												(	year_month_day_sdf.parse(day).after(year_month_day_sdf.parse(startDate))&& 
													year_month_day_sdf.parse(day).before(year_month_day_sdf.parse(endDate)))||
												day.equals(endDate)){
												Double v = volumeList.get(pid);
												v += parseDouble((String) currentDayMap.get(day).get(serviceid).get(mccmnc).get("VOLUME"));
												volumeList.put(pid, v);
										}
									}
								}
							}
						}		
					}
				}
			}
			result = true;
		} catch (ParseException e) {
			ErrorHandle("At setVolumeList got ParseException.");
		}*/
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));

		return result;
	}
	
	/**
	 * 取出 HUR_CURRENTE_DAY table資料
	 * 建立成
	 * Map 
	 * Key:day , value:Map(SERVICEID,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME,ALERT)))
	 * 設定HUR_CURRENT_DAY計費,目前不做刪除動作，之後考慮是否留2個月資料
	 * 20141209 修改取出近兩個月
	 */
	static Map<String,Map<String,Map<String,Map<String,String>>>> currentDayMap = new HashMap<String,Map<String,Map<String,Map<String,String>>>>();
	static Map <String,Map <String,Set<String>>> existMapD = new HashMap<String,Map <String,Set<String>>>();
	private boolean setCurrentMapDay(){
		
		logger.info("setCurrentMapDay...");
		currentDayMap.clear();
		existMapD.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result = false; 
		try {
			sql=
					"SELECT A.SERVICEID,A.CHARGE,A.LAST_FILEID,to_char(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,A.VOLUME,A.UPDATE_DATE,A.CREATE_DATE,A.MCCMNC,A.DAY,A.ALERT,A.IS_SLOWDOWN,A.RESET_VOLUME "
					+ "FROM HUR_CURRENT_DAY A "
					+ "WHERE SUBSTR(A.DAY,0,6) IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs =st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				Map<String,String> map=new HashMap<String,String>();
				Map<String,Map<String,String>> map2=new HashMap<String,Map<String,String>>();			
				Map<String,Map<String,Map<String,String>>> map3=new HashMap<String,Map<String,Map<String,String>>>();
				
				String day =rs.getString("DAY");
				String serviceid =rs.getString("SERVICEID");
				String mccmnc=rs.getString("MCCMNC");
				
				if(day!=null && !"".equals(day) && serviceid!=null && !"".equals(serviceid) &&	mccmnc!=null && !"".equals(mccmnc)){
					if(currentDayMap.containsKey(day)){
						map3=currentDayMap.get(day);
						if(map3.containsKey(serviceid)){
							map2=map3.get(serviceid);
						}
					}
							
					map.put("LAST_FILEID", rs.getString("LAST_FILEID"));
					map.put("LAST_DATA_TIME", rs.getString("LAST_DATA_TIME"));
					map.put("CHARGE", rs.getString("CHARGE"));
					map.put("VOLUME", (rs.getString("VOLUME")==null?"0":rs.getString("VOLUME")));
					map.put("ALERT", rs.getString("ALERT"));
					map.put("IS_SLOWDOWN", rs.getString("IS_SLOWDOWN"));
					map.put("RESET_VOLUME", (rs.getString("RESET_VOLUME")==null?"0":rs.getString("RESET_VOLUME")));
					map2.put(mccmnc, map);
					
					map3.put(serviceid, map2);
					currentDayMap.put(day,map3);

					//20141201 add 設定存在資料
					Map<String,Set<String>> map5 = new HashMap<String,Set<String>>();
					Set<String> set =new HashSet<String>();
					if(existMapD.containsKey(day)){
						map5=existMapD.get(day);
						if(map5.containsKey(serviceid)){
							set=map5.get(serviceid);
						}
					}
					set.add(mccmnc);
					map5.put(serviceid, set);
					existMapD.put(day, map5);
				}
			}
			
			result = true;

		} catch (SQLException e) {
			ErrorHandle("At setCurrentMapDay occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		reloadDay = false;
		return result;
	}
	
	
	
	/**
	 * 20180828 add
	 * 取出 HUR_CURRENTE_DAY_DETAIL table資料
	 * 建立成
	 * Map 
	 * Key:day , value:Map(SERVICEID,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME,ALERT)))
	 * 設定HUR_CURRENT_DAY計費,目前不做刪除動作，之後考慮是否留2個月資料
	 * 20141209 修改取出近兩個月
	 */
	static Map<String,Map<String,Map<String,Map<String,String>>>> currentDayDetailMap = new HashMap<String,Map<String,Map<String,Map<String,String>>>>();
	static Map <String,Map <String,Set<String>>> existMapDD = new HashMap<String,Map <String,Set<String>>>();
	private boolean setCurrentDayDetailMap(){
		
		logger.info("setCurrentDayDetailMap...");
		currentDayDetailMap.clear();
		existMapDD.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result = false; 
		try {
			sql = "select A.SERVICEID,A.DAY,A.MCCNET,A.MCCMNC,A.VOLUME,A.CHARGE "
					+ "from HUR_CURRENT_DAY_DETAIL A "
					+ "WHERE SUBSTR(A.DAY,0,6) IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs =st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				Map<String,String> map=new HashMap<String,String>();
				Map<String,Map<String,String>> map2=new HashMap<String,Map<String,String>>();			
				Map<String,Map<String,Map<String,String>>> map3=new HashMap<String,Map<String,Map<String,String>>>();
				
				String day =rs.getString("DAY");
				String serviceid =rs.getString("SERVICEID");
				String mccmnc=rs.getString("MCCMNC");
				
				if(day!=null && !"".equals(day) && serviceid!=null && !"".equals(serviceid) &&	mccmnc!=null && !"".equals(mccmnc)){
					if(currentDayDetailMap.containsKey(day)){
						map3=currentDayDetailMap.get(day);
						if(map3.containsKey(serviceid)){
							map2=map3.get(serviceid);
						}
					}
							
					map.put("MCCNET", rs.getString("MCCNET"));
					map.put("CHARGE", rs.getString("CHARGE"));
					map.put("VOLUME", (rs.getString("VOLUME")==null?"0":rs.getString("VOLUME")));
					map2.put(mccmnc, map);
					
					map3.put(serviceid, map2);
					currentDayDetailMap.put(day,map3);

					//20141201 add 設定存在資料
					Map<String,Set<String>> map5 = new HashMap<String,Set<String>>();
					Set<String> set =new HashSet<String>();
					if(existMapDD.containsKey(day)){
						map5=existMapDD.get(day);
						if(map5.containsKey(serviceid)){
							set=map5.get(serviceid);
						}
					}
					set.add(mccmnc);
					map5.put(serviceid, set);
					existMapDD.put(day, map5);
				}
			}
			
			result = true;

		} catch (SQLException e) {
			ErrorHandle("At setCurrentDayDetailMap occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		reloadDayDetail = false;
		return result;
	}
	
	/**
	 * 取出 HUR_DATA_RATE
	 * 建立成MAP Key:PRICEPLANID,Value:Map(MCCMNC,MAP(CURRENCY,CHARGEUNIT,RATE,NETWORK))
	 * 找出符合的Priceplanid、MCCMNC，接著從List比對有效資料
	 */
	//static Map<String,Map<String,List<Map<String,String>>>> dataRate = new HashMap<String,Map<String,List<Map<String,String>>>>();
	static Map<String,Map<String,Map<String,String>>> dataRate = new HashMap<String,Map<String,Map<String,String>>>();
	static Map<String,String> pricePlanIdtoCurrency = new HashMap<String,String>();
	//20150324 modify add network info
	private boolean setDataRate(){
		
		logger.info("setDataRate...");
		subStartTime = System.currentTimeMillis();
		boolean result = false;
		dataRate.clear();
		pricePlanIdtoCurrency.clear();
		
		Statement st = null;
		ResultSet rs = null;
																		 
		sql=""
				/*+ "SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY,A.PRICEPLANID,A.DAYCAP,B.NETWORK,A.START_TIME,A.END_TIME "
				+ "FROM HUR_DATA_RATE A,HUR_MCCMNC B "
				+ "where A.MCCMNC=B.MCCMNC";*/
				+"SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY,A.PRICEPLANID,A.DAYCAP,B.NETWORK,A.START_TIME,A.END_TIME,B.COUNTRY "
				+ "FROM HUR_DATA_RATE A,HUR_MCCMNC B "
				+ "where A.MCCMNC=B.MCCMNC "
				+ "and to_date(A.START_TIME,'yyyyMMddhh24miss')<=to_date('"+programeDateTime+"','yyyyMMddhh24miss') "
				+ "and (A.END_TIME is null OR to_date(A.END_TIME,'yyyyMMddhh24miss')>=to_date('"+programeDateTime+"','yyyyMMddhh24miss'))";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);

			while(rs.next()){

				String mccmnc =rs.getString("MCCMNC");
				String priceplanID =rs.getString("PRICEPLANID");
				
				Map<String,String> map=new HashMap<String,String>();
				Map<String,Map<String,String>> map2=new HashMap<String,Map<String,String>>();
				//20150427  因增加有效日期，修改結構
				//List<Map<String,String>> list = new ArrayList<Map<String,String>>();
				
				map.put("RATE", rs.getString("RATE"));
				map.put("CHARGEUNIT", rs.getString("CHARGEUNIT"));
				map.put("CURRENCY", rs.getString("CURRENCY"));
				map.put("DAYCAP", rs.getString("DAYCAP"));
				map.put("NETWORK", rs.getString("NETWORK"));
				map.put("STARTTIME", rs.getString("START_TIME").replace("/", ""));
				map.put("ENDTIME", rs.getString("END_TIME")!=null?rs.getString("END_TIME").replace("/", ""):null);
				//20180223
				map.put("COUNTRY", rs.getString("COUNTRY"));
				
				
				if(dataRate.containsKey(priceplanID)){
					map2=dataRate.get(priceplanID);
					/*if(map2.containsKey(mccmnc)){
						list=map2.get(mccmnc);
					}*/
				}
				
				//list.add(map);
				//map2.put(mccmnc, list);
				map2.put(mccmnc, map);
				dataRate.put(priceplanID, map2);
				
				pricePlanIdtoCurrency.put(priceplanID, rs.getString("CURRENCY"));
			}
			result =true;
		} catch (SQLException e) {			
			ErrorHandle("At setDataRate occur SQLException error", e);
		} finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	/**
	 * 取出以Priceplan對應到的幣別
	 */
	/*Map<String,String> pricePlanIdtoCurrency = new HashMap<String,String>();
	public boolean setCurrencyMap(){
		
		pricePlanIdtoCurrency.clear();
		Statement st = null;
		ResultSet rs = null;
		try {
			sql=
					"SELECT  A.PRICEPLANID,A.CURRENCY "
					+ "FROM HUR_DATA_RATE A "
					+ "GROUP BY A.PRICEPLANID,A.CURRENCY ";
			st = conn.createStatement();
			logger.debug("Query Currency And IMSI SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				String id = rs.getString("PRICEPLANID");
				String currency = rs.getString("CURRENCY");
				
				if(id!=null && !"".equals(id)&& 
						currency!=null && !"".equals(currency))
					pricePlanIdtoCurrency.put(id, currency);
			}
			return true;
		} catch (SQLException e) {
			ErrorHandle("At setCurrencyMap occur SQLException error", e);
			return false;
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}			
	}*/
	
	/**
	 * 取出HUR_THRESHOLD
	 * 建立MAP Key:SERVICEID,VALUE:THRESHOLD
	 * 可以變更成使用者自定義上限，目前不使用全填上null，在此清單內進行每5000累進警示
	 * @return 
	 */
	static Map<String,String> thresholdMap = new HashMap<String,String>();
	private boolean setThreshold(){
		logger.info("setThreshold...");
		subStartTime = System.currentTimeMillis();
		thresholdMap.clear();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.SERVICEID,A.THRESHOLD  FROM HUR_GPRS_THRESHOLD A  WHERE A.CANCEL_DATE IS NULL ";
		
		try {
			st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				thresholdMap.put(rs.getString("SERVICEID"), rs.getString("THRESHOLD"));
			}
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setThreshold occur SQLException error!",e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	/**
	 * 設定 SERVICEID 至 VLN 的對應表
	 * Map Key:IMSI,VALUE:VLN
	 */
	static Map<String,String> SERVICEIDtoVLN =new HashMap<String,String>();
	private boolean setSERVICEIDtoVLN(){
		
		logger.info("setSERVICEIDtoVLN...");
		SERVICEIDtoVLN.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.VLR_NUMBER,A.SERVICEID  FROM UTCN.BASICPROFILE A WHERE A.VLR_NUMBER IS NOT NULL ";
		
		try {
			
			st = conn2.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				SERVICEIDtoVLN.put(rs.getString("SERVICEID"), rs.getString("VLR_NUMBER"));
			}
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setSERVICEIDtoVLN occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	/**
	 * 
	 * 建立 VLN至TADIG對應表
	 * 
	 * 從SERVICEIDtoVLN取得VALUE必須縮位匹配
	 * 
	 * MAP KEY：VLN,VALUE:TADIG
	 */
	static Map<String,String> VLNtoTADIG =new HashMap<String,String>();
	private boolean setVLNtoTADIG(){
		
		logger.info("setVLNtoTADIG...");
		VLNtoTADIG.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result =false;
		sql=
				"SELECT B.REALMNAME TADIG, A.CHARGEAREACODE VLR  FROM CHARGEAREACONFIG A, REALM B  WHERE A.AREAREFERENCE=B.AREAREFERENCE ";
		
		try {
			
			st = conn2.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				VLNtoTADIG.put(rs.getString("VLR"), rs.getString("TADIG"));
			}
			
			result =true;
		} catch (SQLException e) {			
			ErrorHandle("At setVLNtoTADIG occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	/**
	 * 
	 * 建立 TADIG至MCCMNC對應表
	 * 
	 * MAP KEY：TADIG,VALUE:MCCMNC
	 */
	static Map<String,String> TADIGtoMCCMNC =new HashMap<String,String>();
	private boolean setTADIGtoMCCMNC(){
		
		logger.info("setTADIGtoMCCMNC...");
		TADIGtoMCCMNC.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result = false;
		sql=
				"SELECT A.TADIG,A.MCCMNC  FROM HUR_MCCMNC A ";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				TADIGtoMCCMNC.put(rs.getString("TADIG"), rs.getString("MCCMNC"));
			}
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setTADIGtoMCCMNC occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}

	/**
	 * 
	 * 建立 國碼對客服電話，國家 對應表
	 * 
	 * MAP KEY：CODE,VALUE:(PHONE,NAME)
	 */
	static Map<String,Map<String,String>> codeMap = new HashMap<String,Map<String,String>>();
	private boolean setCostomerNumber(){
		
		logger.info("setCostomerNumber...");
		codeMap.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.CODE,A.CHT_PHONE,A.S2T_PHONE,A.NAME  FROM HUR_CUSTOMER_SERVICE_PHONE A ";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				Map<String,String> map = new HashMap<String,String>();
				map.put("S2T_PHONE", rs.getString("S2T_PHONE"));
				map.put("CHT_PHONE", rs.getString("CHT_PHONE"));
				map.put("NAME", rs.getString("NAME"));
				codeMap.put(rs.getString("CODE"), map);
			}
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At setTADIGtoMCCMNC occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	/**
	 * 取得資料筆數
	 * @return
	 */
	/*private int getDataCount(int maxUsageId){
		logger.info("dataCount...");
		subStartTime = System.currentTimeMillis();
		Statement st = null;
		ResultSet rs = null;
		sql="SELECT COUNT(1) count  FROM HUR_DATA_USAGE A WHERE A.CHARGE is null and usageid<= "+maxUsageId+" ";
		int count=0;
		//找出總量
		
		try {
			st = conn.prepareStatement(sql);

			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			
			
			while(rs.next()){
				count=rs.getInt("count");
			}
			logger.info("usage count : " +count);

		} catch (SQLException e) {
			ErrorHandle("At dataCount occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return count;
	}*/
	
	/**
	 * 取得預設計費比率（0.011），對MCCNOC有卻無法對應資料計費
	 * @return
	 */
	//20151229 del
	/*private double defaultRate(){
		logger.info("defaultRate...");
		subStartTime = System.currentTimeMillis();
		
		double defaultRate=0.011;
		logger.info("defaultRate : " +defaultRate+" TWD ");
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return defaultRate;
	}*/
	
	/**
	 * 建立華人上網包對應資料
	 * 
	 * List Map KEY:MSISDN,VALUE(IMSI,MSISDN,SERVICEID,SERVICECODE,STARTDATE,ENDDATE)>
	 */
	static List<Map<String,String>> addonDataList = new ArrayList<Map<String,String>>();
	private boolean setAddonData(){
		
		logger.info("setAddonData...");
		addonDataList.clear();
		
		subStartTime = System.currentTimeMillis();
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		
		sql=
				"SELECT A.SEQ,A.S2TIMSI IMSI,A.S2TMSISDN MSISDN,A.SERVICEID,A.SERVICECODE,To_char(A.STARTDATE,'yyyyMMdd') STARTDATE,To_char(A.ENDDATE,'yyyyMMdd') ENDDATE "
				+ "FROM ADDONSERVICE_N A "
				+ "where A.ENDDATE is null or A.ENDDATE >= (to_date('"+sYearmonthday+"','yyyyMMdd') -3) ";
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				Map<String,String> map = new HashMap<String,String>();
				map.put("SEQ", rs.getString("SEQ"));
				map.put("IMSI", rs.getString("IMSI"));
				map.put("MSISDN", rs.getString("MSISDN"));
				map.put("SERVICEID", rs.getString("SERVICEID"));
				map.put("SERVICECODE", rs.getString("SERVICECODE"));
				map.put("STARTDATE", rs.getString("STARTDATE"));
				map.put("ENDDATE", rs.getString("ENDDATE"));
				addonDataList.add(map);
			}
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setTADIGtoMCCMNC occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	/**
	 * IP number to Mccmnc  20141211 add
	 * Map Value:START_NUM,END_NUM,MCCMNC
	 */
	static  List<Map<String,String>> IPtoMccmncList = new ArrayList<Map<String,String>>();
	private boolean setIPtoMccmncList(){
		
		logger.info("setIPtoMccmncList...");
		IPtoMccmncList.clear();
		subStartTime = System.currentTimeMillis();
		boolean result =false;
		
		Statement st = null;
		ResultSet rs = null;
		
		
		try {
			st = conn.createStatement();
			
			sql=
					"SELECT A.START_NUM,A.END_NUM,A.MCCMNC  FROM HUR_IP_RANGE A  ORDER BY A.START_NUM ";
			
			logger.debug("Query AddonData SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				Map<String,String> map = new HashMap<String,String>();
				map.put("START_NUM", rs.getString("START_NUM"));
				map.put("END_NUM", rs.getString("END_NUM"));
				map.put("MCCMNC", rs.getString("MCCMNC"));
				IPtoMccmncList.add(map);
			}
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setIPtoMccmncList occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	/**
	 * 取出msisdn
	 * 建立msisdnMap
	 * Key:imsi,Value:Map(MSISDN,PRICEPLANID,SUBSIDIARYID,NCODE,SERVICEID)
	 * 增加serviceid to map，做以serviceid反查
	 */
	static Map<String,Map<String,String>> msisdnMap = new HashMap<String,Map<String,String>>();
	private boolean setMsisdnMap(){
		
		logger.info("setMsisdnMap...");
		msisdnMap.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		
		try {
			
			st = conn.createStatement();
			
			
			sql=
					"SELECT da.IMSI,da.SERVICECODE,da.PRICEPLANID,da.SUBSIDIARYID,da.SERVICEID,da.ICCID, da.NCODE "
					+ "from ("
					+ "        SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,B.ICCID, "
					+ "				(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE "
					+ "			FROM SERVICE A,IMSI B,PARAMETERVALUE C "
					+ "			WHERE A.SERVICEID=B.SERVICEID(+) AND A.SERVICECODE IS NOT NULL "
					+ "			AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 ) da, "
					+ "			(	select serviceid from hur_current where month in ('"+sYearmonth+"','"+sYearmonth2+"') group by serviceid "
					+ "				 union "
					+ "				select serviceid from hur_volume_pocket where cancel_time is null group by serviceid  ) cho "
					+ "        WHERE da.serviceid = cho.serviceid ";
			
			logger.info("Execute SQL :"+sql);
			rs=st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("MSISDN", rs.getString("SERVICECODE"));
				map.put("PRICEPLANID", rs.getString("PRICEPLANID"));
				map.put("SUBSIDIARYID", rs.getString("SUBSIDIARYID"));
				map.put("NCODE", rs.getString("NCODE"));
				map.put("SERVICEID", rs.getString("SERVICEID"));
				map.put("IMSI", rs.getString("IMSI"));
				map.put("ICCID", rs.getString("ICCID"));
				msisdnMap.put(rs.getString("IMSI"), map);
				msisdnMap.put(rs.getString("SERVICEID"), map);
			}
			
			
			
			sql= 
					"SELECT da.IMSI,da.SERVICECODE,da.PRICEPLANID,da.SUBSIDIARYID,da.SERVICEID,da.ICCID, da.NCODE "
					+ "from ("
					+ "        SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,B.ICCID, "
					+ "				(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE "
					+ "			FROM SERVICE A,IMSI B,PARAMETERVALUE C "
					+ "			WHERE A.SERVICEID=B.SERVICEID(+) AND A.SERVICECODE IS NOT NULL "
					+ "			AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 ) da, (select imsi from hur_data_usage where charge is null group by imsi ) cho "
					+ "        WHERE da.imsi = cho.imsi ";
			
			logger.info("Execute SQL :"+sql);
			rs=st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("MSISDN", rs.getString("SERVICECODE"));
				map.put("PRICEPLANID", rs.getString("PRICEPLANID"));
				map.put("SUBSIDIARYID", rs.getString("SUBSIDIARYID"));
				map.put("NCODE", rs.getString("NCODE"));
				map.put("SERVICEID", rs.getString("SERVICEID"));
				map.put("IMSI", rs.getString("IMSI"));
				map.put("ICCID", rs.getString("ICCID"));
				msisdnMap.put(rs.getString("IMSI"), map);
				msisdnMap.put(rs.getString("SERVICEID"), map);
			}
			
			
			//temp 4G
			
			sql= 
					"SELECT da.IMSI,da.SERVICECODE,da.PRICEPLANID,da.SUBSIDIARYID,da.SERVICEID,da.ICCID, da.NCODE " + 
					"					from (" + 
					"					        SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,B.ICCID, " + 
					"									(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE " + 
					"								FROM SERVICE A,IMSI B,PARAMETERVALUE C " + 
					"								WHERE A.SERVICEID=B.SERVICEID_4G(+) AND A.SERVICECODE IS NOT NULL " + 
					"                and b.serviceid is null and b.serviceid_4g is not null" + 
					"								AND A.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 ) da ";
			
			rs = null;
			
			logger.info("Execute SQL :"+sql);
			rs=st.executeQuery(sql);
			logger.info("Query end!");
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("MSISDN", rs.getString("SERVICECODE"));
				map.put("PRICEPLANID", rs.getString("PRICEPLANID"));
				map.put("SUBSIDIARYID", rs.getString("SUBSIDIARYID"));
				map.put("NCODE", rs.getString("NCODE"));
				map.put("SERVICEID", rs.getString("SERVICEID"));
				map.put("IMSI", rs.getString("IMSI"));
				map.put("ICCID", rs.getString("ICCID"));
				msisdnMap.put(rs.getString("IMSI"), map);
				msisdnMap.put(rs.getString("SERVICEID"), map);
			}
			
			
			
			
			
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At setMsisdnMap occur SQLException error!", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	
	/**
	 * 設定IMSI對應到ServiceID Map 20150115 add
	 * 20160914 調整成專給CDR用
	 * 
	 * 先從MSISDN MAP(IMSI table 資料找尋 SERVICEID)
	 * 找不到再從此Table(換卡記錄)找尋
	 */
	static Map<String,String> IMSItoServiceIdMap = new HashMap<String,String>();
	//Map<String,String> ServiceIdtoIMSIMap = new HashMap<String,String>();
	private boolean setIMSItoServiceIDMap(){
		boolean result =false;
		logger.info("setIMSItoServiceIDMap...");
		//logger.info("setServiceIDtoImsiMap...");
		IMSItoServiceIdMap.clear();
		//ServiceIdtoIMSIMap.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;

		if(TEST_MODE){
			sql = "select imsi,serviceid " + 
					"from imsi " + 
					"where imsi in (select imsi from hur_data_usage where CHARGE is null) " + 
					"union all " + 
					"select imsi,serviceid " + 
					"from imsi " + 
					"where serviceid in (select serviceid from HUR_CURRENT where month in ('"+sYearmonth+"','"+sYearmonth2+"'))";
		}else{
			
			/*sql=  ""
					+ "select da.serviceid,da.imsi "
					+ "from "
					+ "(	SELECT A.SERVICEID,A.IMSI "
					+ "		FROM( "
					+ "				SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE "
					+ "				FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
					+ "				WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
					+ "				UNION "
					+ "				SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE "
					+ "				FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
					+ "				WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A, "
					+ "			(	SELECT IMSI,MAX(COMPLETEDATE) COMPLETEDATE "
					+ "				from(	SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE "
					+ "						FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
					+ "						WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
					+ "						UNION "
					+ "						SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE "
					+ "						FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
					+ "						WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713) "
					+ "				GROUP BY IMSI )B "
					+ "		WHERE A.IMSI=B.IMSI AND A.COMPLETEDATE =B.COMPLETEDATE ) da , "
					+ "(	select serviceid from hur_current where month in ('"+sYearmonth+"','"+sYearmonth2+"') group by serviceid )  cho "
					+ "		WHERE da.serviceid = cho.serviceid ";*/
			
			sql = ""
					+ "select da.serviceid,da.imsi "
					+ "from "
					+ "(	SELECT A.SERVICEID,A.IMSI "
					+ "		FROM("
					+ "				SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE "
					+ "				FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
					+ "				WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
					+ "				UNION "
					+ "				SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE "
					+ "				FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
					+ "				WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A, "
					+ "			(	SELECT IMSI,MAX(COMPLETEDATE) COMPLETEDATE "
					+ "				from(	SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE "
					+ "						FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
					+ "						WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
					+ "						UNION "
					+ "						SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE "
					+ "						FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
					+ "						WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713) "
					+ "				GROUP BY IMSI )B "
					+ "		WHERE A.IMSI=B.IMSI AND A.COMPLETEDATE =B.COMPLETEDATE ) da , "
					+ "(	select imsi from hur_data_usage where charge is null and usageid <= "+maxId+" group by imsi )  cho "
					+ "		WHERE da.imsi = cho.imsi ";

		}
		
		
		
		try {
			/*if(TEST_MODE){
				st = conn2.createStatement();
			}else{
				st = conn.createStatement();
			}*/
			st = conn.createStatement();
			
			logger.info("Execute SQL :"+sql);
			rs=st.executeQuery(sql);
			logger.info("Query1 end!");
			while(rs.next()){
				IMSItoServiceIdMap.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
				//ServiceIdtoIMSIMap.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
			}
			/*if(!TEST_MODE){
				sql = ""
						+ "select da.serviceid,da.imsi "
						+ "from "
						+ "(	SELECT A.SERVICEID,A.IMSI "
						+ "		FROM("
						+ "				SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE "
						+ "				FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
						+ "				WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
						+ "				UNION "
						+ "				SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE "
						+ "				FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
						+ "				WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A, "
						+ "			(	SELECT IMSI,MAX(COMPLETEDATE) COMPLETEDATE "
						+ "				from(	SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE "
						+ "						FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
						+ "						WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
						+ "						UNION "
						+ "						SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE "
						+ "						FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
						+ "						WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713) "
						+ "				GROUP BY IMSI )B "
						+ "		WHERE A.IMSI=B.IMSI AND A.COMPLETEDATE =B.COMPLETEDATE ) da , "
						+ "(	select imsi from hur_data_usage where charge is null group by imsi )  cho "
						+ "		WHERE da.imsi = cho.imsi ";
				logger.info("Execute SQL :"+sql);
				rs=st.executeQuery(sql);
				logger.info("Query2 end!");
				while(rs.next()){
					IMSItoServiceIdMap.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
					ServiceIdtoIMSIMap.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
				}
			}*/
			
			
			rs = null;
			
			//temp 4G
			sql = "select a.imsi IMSI ,a.serviceid_4G SERVICEID " + 
					"from imsi a " + 
					"where a.serviceid_4G is not null and serviceid is null ";
			
			
			logger.info("Execute SQL :"+sql);
			rs=st.executeQuery(sql);
			logger.info("Query1 end!");
			while(rs.next()){
				IMSItoServiceIdMap.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
				//ServiceIdtoIMSIMap.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
			}
			
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setIMSItoServiceIDMap occur SQLException error!", e);
		}finally{
				try {
					if(st!=null)
						st.close();
					if(rs!=null)
						rs.close();
				} catch (SQLException e) {
	
				}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}


	/*private boolean setServiceIDtoImsiMap(){
		logger.info("setServiceIDtoImsiMap...");
		ServiceIdtoIMSIMap.clear();
		subStartTime = System.currentTimeMillis();
		Statement st = null;
		ResultSet rs = null;
		boolean result =false;
		
		sql = ""
				+ " SELECT A.SERVICEID,A.IMSI "
				+ "FROM( "
				+ "		SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE "
				+ "		FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
				+ "		WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
				+ "		UNION "
				+ "		SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE "
				+ "		FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
				+ "		WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A, "
				+ "		(	SELECT SERVICEID,MAX(COMPLETEDATE) COMPLETEDATE "
				+ "			from(	SELECT A.SERVICEID,B.NEWVALUE,A.COMPLETEDATE "
				+ "					FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
				+ "					WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
				+ "					UNION "
				+ "					SELECT A.SERVICEID,B.FIELDVALUE,A.COMPLETEDATE "
				+ "					FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
				+ "					WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713) "
				+ "			GROUP BY SERVICEID )B "
				+ "		WHERE A.SERVICEID=B.SERVICEID AND A.COMPLETEDATE =B.COMPLETEDATE ";
		
		
		try {
			logger.info("Execute SQL :"+sql);
			st = conn2.createStatement();
			rs=st.executeQuery(sql);
			
			while(rs.next()){
				ServiceIdtoIMSIMap.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
			}
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setIMSItoServiceIDMap occur SQLException error!", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}*/

	/*************************************************************************
	 *************************************************************************
	 *                                Function
	 *************************************************************************
	 *************************************************************************/
	
	/**
	 * 程式初始化
	 */
	private static void IniProgram(){
		// 初始化log
		// iniLog4j();
		loadProperties();
		//checkPocketStart = false;
		//checkPocketEnd = false;
		reloadMonth = true;
		reloadDay = true;
		reloadDayDetail = true;
	}

	/**
	 * 初始化
	 * 載入Log4j Properties
	 */
	@SuppressWarnings("unused")
	private  void iniLog4j(){
		System.out.println("initial Log4g, property at "+DVRSmain.class.getResource(""));
		PropertyConfigurator.configure(DVRSmain.class.getResource("").toString().replace("file:/", "")+"Log4j.properties");
		logger =Logger.getLogger(DVRSmain.class);
	}
	
	/**
	 * 初始化
	 * 載入Log4j Properties
	 * 同時載入參數porps
	 */
	private static  void loadProperties(){
		System.out.println("initial Log4j, property !");
		String path=DVRSmain.class.getResource("").toString().replace("file:", "")+"Log4j.properties";
		System.out.println("path:"+path);
		try {
			props.load(new   FileInputStream(path));
			PropertyConfigurator.configure(props);
			logger =Logger.getLogger(DVRSmain.class);
			logger.info("Logger Load Success!");

			DEFAULT_MCCMNC=props.getProperty("progrma.DEFAULT_MCCMNC");//預設mssmnc
			//DEFAULT_THRESHOLD=(props.getProperty("progrma.DEFAULT_THRESHOLD")!=null?parseDouble(props.getProperty("progrma.DEFAULT_THRESHOLD")):5000D);//預設月警示量
			//DEFAULT_DAY_THRESHOLD=(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")!=null?parseDouble(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")):500D);//預設日警示量
			//DEFAULT_DAYCAP=(props.getProperty("progrma.DEFAULT_DAYCAP")!=null?parseDouble(props.getProperty("progrma.DEFAULT_DAYCAP")):500D);
			//DEFAULT_VOLUME_THRESHOLD=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")!=null?parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")):1.5*1024*1024*1024D);//預設流量警示(降速)，1.5GB;
			//DEFAULT_VOLUME_THRESHOLD2=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")!=null?parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")):1.5*1024*1024*1024D);//預設流量警示(降速)，15GB;
			DEFAULT_PHONE=props.getProperty("progrma.DEFAULT_PHONE");
			RUN_INTERVAL=(props.getProperty("progrma.RUN_INTERVAL")!=null?Integer.parseInt(props.getProperty("progrma.RUN_INTERVAL")):3600);
			HKNetReceiver = props.getProperty("program.HKNetReceiver");
			AlertReceiver = props.getProperty("program.AlertReceiver");
			TEST_MODE=("true".equalsIgnoreCase(props.getProperty("progrma.TEST_MODE"))?true:false);
			
			dataThreshold=(props.getProperty("progrma.dataThreshold")!=null?Integer.parseInt(props.getProperty("progrma.dataThreshold")):500);//CDR資料一批次取出數量
			//lastfileID=(props.getProperty("progrma.lastfileID")!=null?parseInt(props.getProperty("progrma.lastfileID")):0);//最後批價檔案號
			exchangeRate=(props.getProperty("progrma.exchangeRate")!=null?Double.parseDouble(props.getProperty("progrma.exchangeRate")):0.25); //港幣對台幣匯率，暫訂為4
			kByte=(props.getProperty("progrma.kByte")!=null?Double.parseDouble(props.getProperty("progrma.kByte")):1/1024D);//RATE單位KB，USAGE單位B
			
			logger.info(
					"DEFAULT_MCCMNC : "+DEFAULT_MCCMNC+"\n"
					//+ "DEFAULT_THRESHOLD : "+DEFAULT_THRESHOLD+"\n"
					//+ "DEFAULT_DAY_THRESHOLD : "+DEFAULT_DAY_THRESHOLD+"\n"
					//+ "DEFAULT_DAYCAP : "+DEFAULT_DAYCAP+"\n"
					//+ "DEFAULT_VOLUME_THRESHOLD : "+DEFAULT_VOLUME_THRESHOLD+"\n"
					//+ "DEFAULT_VOLUME_THRESHOLD2 : "+DEFAULT_VOLUME_THRESHOLD2+"\n"
					+ "DEFAULT_PHONE : "+DEFAULT_PHONE+"\n"
					+ "RUN_INTERVAL : "+RUN_INTERVAL+"\n"
					+ "HKNetReceiver : "+HKNetReceiver +"\n"
					+ "TEST_MODE : "+TEST_MODE+"\n"
					+ "dataThreshold : "+dataThreshold+"\n"
					//+ "lastfileID : "+lastfileID+"\n"
					+ "exchangeRate : "+exchangeRate+"\n"
					+ "kByte : "+kByte+"\n");
			
		} catch (FileNotFoundException e) {
			sql="";
			ErrorHandle("At loadProperties occur file not found error \n <br> file path="+path);
		} catch (IOException e) {
			sql="";
			ErrorHandle("At loadProperties occur IOException error !\n <br> file path="+path);
		}
	}
	
	/**
	 * 連線至DB1
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	private void connectDB() throws ClassNotFoundException, SQLException{
		//conn=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		String url=props.getProperty("Oracle.URL")
				.replace("{{Host}}", props.getProperty("Oracle.Host"))
				.replace("{{Port}}", props.getProperty("Oracle.Port"))
				.replace("{{ServiceName}}", (props.getProperty("Oracle.ServiceName")!=null?props.getProperty("Oracle.ServiceName"):""))
				.replace("{{SID}}", (props.getProperty("Oracle.SID")!=null?props.getProperty("Oracle.SID"):""));
		
		conn=connDB(props.getProperty("Oracle.DriverClass"), url, 
				props.getProperty("Oracle.UserName"), 
				props.getProperty("Oracle.PassWord")
				);
		logger.info("Connect to "+url);
		connectionTime1 = System.currentTimeMillis();
	}
	
	/**
	 * 連線至DB2
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	private void connectDB2() throws ClassNotFoundException, SQLException{
		// 進行DB連線
		//conn2=tool.connDB(logger, DriverClass, URL, UserName, PassWord);

		String url=props.getProperty("mBOSS.URL")
				.replace("{{Host}}", props.getProperty("mBOSS.Host"))
				.replace("{{Port}}", props.getProperty("mBOSS.Port"))
				.replace("{{ServiceName}}", (props.getProperty("mBOSS.ServiceName")!=null?props.getProperty("mBOSS.ServiceName"):""))
				.replace("{{SID}}", (props.getProperty("mBOSS.SID")!=null?props.getProperty("mBOSS.SID"):""));
		
		conn2=connDB(props.getProperty("mBOSS.DriverClass"),url, 
				props.getProperty("mBOSS.UserName"), 
				props.getProperty("mBOSS.PassWord"));
		
		logger.info("Connrct to "+url);
		connectionTime2 = System.currentTimeMillis();
	}
	
	public Connection connDB(String DriverClass, String URL,
			String UserName, String PassWord) throws ClassNotFoundException, SQLException {
		Connection conn = null;

			Class.forName(DriverClass);
			conn = DriverManager.getConnection(URL, UserName, PassWord);
		return conn;
	}
	
	public Connection connDB(String DriverClass,
			String DBType,String ip,String port,String DBNameorSID,String charset,
			String UserName,String PassWord) throws ClassNotFoundException, SQLException{

			Class.forName(DriverClass);
			String url="jdbc:{{DBType}}:thin:@{{Host}}:{{Port}}:{{ServiceName}}{{charset}}"
					.replace("{{DBType}}", DBType)
					.replace("{{Host}}", ip)
					.replace("{{Port}}", port)
					.replace("{{ServiceName}}", DBNameorSID)
					.replace("{{charset}}", (charset!=null?"?charset="+charset:""));
			
		return connDB(DriverClass, url, UserName, PassWord);
	}
	
	/**
	 * 關閉連線
	 */
	/*private void closeConnect() {
		
		if (conn != null) {

			try {
				logger.info("closeConnect1...");
				conn.close();
			} catch (SQLException e) {
			}

		}
		
		if (conn2 != null) {

			try {
				logger.info("closeConnect2...");
				conn2.close();
			} catch (SQLException e) {
			}

		}
	}*/
	
	public Double FormatDouble(Double value,String form){
		if(value == null)
			value = 0d;
		
		if(form==null || "".equals(form)){
			form="0.00";
		}
			
		/*DecimalFormat df = new DecimalFormat(form);   
		String str=df.format(value);*/
		
		return Double.valueOf(new DecimalFormat(form).format(value));
	}
	
	static String iniform= "yyyy/MM/dd HH:mm:ss";
	public static String DateFormat(){
		DateFormat dateFormat = new SimpleDateFormat(iniform);
		return dateFormat.format(new Date());
	}
	public Date DateFormat(String dateString, String form) throws ParseException {
		Date result=new Date();
		
		if(dateString==null) return result;

		if(form==null ||"".equals(form)) form=iniform;
		DateFormat dateFormat = new SimpleDateFormat(form);
		result=dateFormat.parse(dateString);
		
		return result;
	}
	public String DateFormat(Date date, String form) {
		
		if(date==null) date=new Date();
		if(form==null ||"".equals(form)) form=iniform;
		
		DateFormat dateFormat = new SimpleDateFormat(form);
		return dateFormat.format(date);
	}
	public String FormatNumString(Double value,String form){
		if(form==null || "".equals(form)){
			form="#,##0.00";
		}
			
		DecimalFormat df = new DecimalFormat(form);   
		String str=df.format(value);
		
		return str;
	}
	public String HttpPost(String url,String param,String charset) throws IOException{
		URL obj = new URL(url);
		
		if(charset!=null && !"".equals(charset))
			param=URLEncoder.encode(param, charset);
		
		
		HttpURLConnection con =  (HttpURLConnection) obj.openConnection();
 
		//add reuqest header
		/*con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");*/
 
		// Send post request
		con.setDoOutput(true);
		
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(param);
		wr.flush();
		wr.close();
 
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + new String(param.getBytes("ISO8859-1")));
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
 
		//print result
		return(response.toString());
	}
	
	/**
	 * 取消connection的Auto commit
	 */
	private void cancelAutoCommit(){
		logger.info("cancelAutoCommit...");
		try {
			logger.info("set AutoCommit false!");
			conn.setAutoCommit(false);
			
		} catch (SQLException e) {
			ErrorHandle("At cancelAutoCommit occur SQLException error", e);
		}
	}
	
	
	/**
	 * 從imsi找尋目前的mccmnc
	 */
	public String searchMccmncBySERVICEID(String serviceid){
		String mccmnc=null;
		
		String vln=SERVICEIDtoVLN.get(serviceid);
		if(vln!=null && !"".equals(vln)){
			for(int k = vln.length();k>0;k--){
				String tadig = VLNtoTADIG.get(vln.subSequence(0, k));
				if(tadig!=null && !"".equals(tadig)){
					mccmnc=TADIGtoMCCMNC.get(tadig);
					break;
				}
			}
		}
		return mccmnc;
	}

	
	/**
	 *確認華人上網包 
	 *20150115 ALTER 取消檢查門號
	 * @throws Exception 
	 */
	private boolean  checkQosAddon(String serviceID,String mccmnc,Date callTime,Double volumn) throws Exception{
		//logger.info("checkQosAddon...");

		//20150623 search by serviceid 
		if(serviceID!=null && !"".equals(serviceID)){
			
			//if(volumn!=0){//是否為新版的美國流量包，Volume 0 時只是確認，不需要執行累計美國流量包
			//已建立確認用checkQosAddon2取代
				
				//檢查是不是在美國流量包的區間
				//在區間=>不批價
				//不在區間=>是否為SX003申請期間，是=>新增不批價，否=>正常批價
				//是否已在流量包新增資料
				/*if(checkInPackage(serviceID,mccmnc,callTime)){
					//需要統計流量包期間總量
					for(Map<String,String> m2:volumePocketMap.get(serviceID).get(mccmnc.substring(0,3))){
						String pid = m2.get("PID");
						Double v = volumeList.get(pid);
						v+=volumn;
						volumeList.put(pid, v);
						return true;
					}
				}*/
			//}

			for(Map<String,String> m : addonDataList){
				Calendar startTime = null,endTime = null;
				//取得開始時間
				startTime = Calendar.getInstance();
				startTime.setTime(year_month_day_sdf.parse(m.get("STARTDATE")));
				startTime.set(Calendar.HOUR_OF_DAY, 0);
				startTime.set(Calendar.MINUTE, 0);
				startTime.set(Calendar.SECOND, 0);
				
				//取得到期時間
				if(m.get("ENDDATE")!=null){
					endTime = Calendar.getInstance();
					endTime.setTime(year_month_day_sdf.parse(m.get("ENDDATE")));
					endTime.set(Calendar.DAY_OF_YEAR, endTime.get(Calendar.DAY_OF_YEAR)+1);
					endTime.set(Calendar.HOUR_OF_DAY, 0);
					endTime.set(Calendar.MINUTE, 0);
					endTime.set(Calendar.SECOND, 0);
				}
				
				if(serviceID.equals(m.get("SERVICEID"))){
					//是否在所申請的服務地區
					if(
							(
							("SX001".equals(m.get("SERVICECODE"))&& sSX001.contains(mccmnc.substring(0, 3)))||
							("SX002".equals(m.get("SERVICECODE"))&& sSX002.contains(mccmnc.substring(0, 3)))||
							("SX004".equals(m.get("SERVICECODE"))&& sSX004.contains(mccmnc.substring(0, 3)))||
							("SX005".equals(m.get("SERVICECODE"))&& sSX005.contains(mccmnc.substring(0, 3)))||
							//20180718 add
							("SX006".equals(m.get("SERVICECODE"))&& sSX006.contains(mccmnc.substring(0, 3)))
							)&&
							//是否符合申請的時間區段
							(callTime.after(startTime.getTime())||callTime.equals(startTime.getTime()))&&	(endTime==null ||callTime.before(endTime.getTime()))){

						if(0!=volumn)
							logger.info(m.get("SERVICECODE"));

						return true;
						
						/*}else if(	0!=volumn && 
							"SX003".equals(m.get("SERVICECODE"))&&
							//在美國
							mccmnc.startsWith("310")&&
							//是否符合申請的時間區段
							(callTime.after(startTime.getTime())||callTime.equals(startTime.getTime()))&&	(endTime==null ||callTime.before(endTime.getTime()))){
							logger.info("Is SX003...");
						
							String sDate = year_month_day_sdf.format(callTime);
							//不含當天的15天封包
							String eDate = year_month_day_sdf.format(new Date(callTime.getTime()+15*24*60*60*1000));
							String type = "0";
							//1.5 GB，達9成時降速
							String limit = "1415577";	
							
							//流量包表無資料的話，新增並處理
							//取得PID
							String pid = getVolumePocketPID();
							
							if(pid == null ){
								throw new Exception("Can't get pid.");
							}
							logger.info("Start Package count.");
							String mcc = mccmnc.substring(0,3);
							
							//設定Insert Map
							Map<String,String> iMap = new HashMap<String,String>();
							iMap.put("SERVICEID", serviceID);
							iMap.put("MCC", mcc);
							iMap.put("SDATE", sDate);
							iMap.put("EDATE", eDate);
							iMap.put("TYPE", type);
							iMap.put("LIMIT",  limit);
							iMap.put("PID", pid);
							iMap.put("IMSI", msisdnMap.get(serviceID).get("IMSI"));
							insertVolumeList.add(iMap);

							//放入快取表格資料
							Map<String, String> m3 = new HashMap<String,String>();
							m3.put("START_DATE", sDate);
							m3.put("END_DATE", eDate);
							m3.put("CURRENCY", "NTD");
							m3.put("ALERTED", "0");
							m3.put("TYPE", type);
							m3.put("LIMIT", limit);
							m3.put("PID", pid);
							Map<String, List<Map<String, String>>> m2 = new HashMap<String,List<Map<String,String>>>();
							List<Map<String, String>> l3 = new ArrayList<Map<String,String>>();
							if(volumePocketMap.containsKey(serviceID)){
								m2=volumePocketMap.get(serviceID);
								if(m2.containsKey(mcc)){
									l3 = m2.get(mcc);
								}
							}
							l3.add(m3);
							m2.put(mcc, l3);
							volumePocketMap.put(serviceID, m2);
							
							//初始VolumeList，流量統計
							volumeList.put(pid, volumn);				
							
							
							logger.info("Sneding start SMS.");
							//取得簡訊內容
							String msgids = getSystemConfigParam("0", "VOLUME_POCKET_START_MSG");
							 if(msgids == null){
								sql="";
								ErrorHandle("For ServiceID:"+serviceID+" PID:"+pid+" cannot get VOLUME_POCKET_START_MSG! ");
								continue;
							}
							//取得門號
							String msisdn = getMSISDN(serviceID);
							if(msisdn==null ||"".equals(msisdn)){
								sql="";
								ErrorHandle("At checkVolumePocket start notice , The serviceid:"+serviceID+" can't find msisdn!");
								continue;
							}
							
							String[] contentid = msgids.split(",");
							
							sendSMS(serviceID,contentid,msisdn,
									new String[]{"{{date_start}}","{{date_end}}"},
									new String[]{sDate.substring(0,4)+"/"+sDate.substring(4,6)+"/"+sDate.substring(6),(eDate==null?"":eDate.substring(0,4)+"/"+eDate.substring(4,6)+"/"+eDate.substring(6))});
							return true;*/
					}
				}
			}
		}
		return false;
	}
	
	/**
	 *確認華人上網包 ，在日累計累計流量使用SX001、SX002、SX003
	 *20150115 ALTER 取消檢查門號
	 * @throws Exception 
	 */
	private String  checkQosAddon2(String serviceID,String mccmnc,Date callTime) throws Exception{
		if(serviceID!=null && !"".equals(serviceID)){
			for(Map<String,String> m : addonDataList){
				Calendar startTime = null,endTime = null;
				//取得開始時間
				startTime = Calendar.getInstance();
				startTime.setTime(year_month_day_sdf.parse(m.get("STARTDATE")));
				startTime.set(Calendar.HOUR_OF_DAY, 0);
				startTime.set(Calendar.MINUTE, 0);
				startTime.set(Calendar.SECOND, 0);
				
				//取得到期時間
				if(m.get("ENDDATE")!=null){
					endTime = Calendar.getInstance();
					endTime.setTime(year_month_day_sdf.parse(m.get("ENDDATE")));
					endTime.set(Calendar.DAY_OF_YEAR, endTime.get(Calendar.DAY_OF_YEAR)+1);
					endTime.set(Calendar.HOUR_OF_DAY, 0);
					endTime.set(Calendar.MINUTE, 0);
					endTime.set(Calendar.SECOND, 0);
				}
				
				if(serviceID.equals(m.get("SERVICEID"))&&
						//是否符合申請的時間區段	
						(callTime.after(startTime.getTime())||callTime.equals(startTime.getTime()))&&
						(endTime==null ||callTime.before(endTime.getTime()))){
					if("SX001".equals(m.get("SERVICECODE"))&& sSX001.contains(mccmnc.substring(0, 3))){
						return "SX001";
					}else if("SX002".equals(m.get("SERVICECODE"))&& sSX002.contains(mccmnc.substring(0, 3))) {
						return "SX002";
					}else if("SX004".equals(m.get("SERVICECODE"))&& sSX004.contains(mccmnc.substring(0, 3))) {
						return "SX004";
					}else if("SX005".equals(m.get("SERVICECODE"))&& sSX005.contains(mccmnc.substring(0, 3))) {
						return "SX005";
					}
					//20180718 add
					else if("SX006".equals(m.get("SERVICECODE"))&& sSX006.contains(mccmnc.substring(0, 3))) {
						return "SX006";
					}
				}
			}
		}
		return null;
	}
	//20180316 add 確認用戶是否在上網包使用期間內
	private String  checkQosAddon3(String serviceID,Date callTime) throws Exception{
		if(serviceID!=null && !"".equals(serviceID)){
			for(Map<String,String> m : addonDataList){
				Calendar startTime = null,endTime = null;
				//取得開始時間
				startTime = Calendar.getInstance();
				startTime.setTime(year_month_day_sdf.parse(m.get("STARTDATE")));
				startTime.set(Calendar.HOUR_OF_DAY, 0);
				startTime.set(Calendar.MINUTE, 0);
				startTime.set(Calendar.SECOND, 0);
				
				//取得到期時間
				if(m.get("ENDDATE")!=null){
					endTime = Calendar.getInstance();
					endTime.setTime(year_month_day_sdf.parse(m.get("ENDDATE")));
					endTime.set(Calendar.DAY_OF_YEAR, endTime.get(Calendar.DAY_OF_YEAR)+1);
					endTime.set(Calendar.HOUR_OF_DAY, 0);
					endTime.set(Calendar.MINUTE, 0);
					endTime.set(Calendar.SECOND, 0);
				}
				
				if(serviceID.equals(m.get("SERVICEID"))&&
						//是否符合申請的時間區段	
						(callTime.after(startTime.getTime())||callTime.equals(startTime.getTime()))&&
						(endTime==null ||callTime.before(endTime.getTime()))){
					
					if(!"SX003".equalsIgnoreCase(m.get("SERVICECODE")))
						return m.get("SERVICECODE"); //SX001、SX002、SX004、SX005、SX006
					
					/*if("SX001".equals(m.get("SERVICECODE"))){
						return "SX001";
					}else if("SX002".equals(m.get("SERVICECODE"))) {
						return "SX002";
					}else if("SX004".equals(m.get("SERVICECODE"))) {
						return "SX004";
					}else if("SX005".equals(m.get("SERVICECODE"))) {
						return "SX005";
					}*/
				}
			}
		}
		return null;
	}
	
	/**
	 * 20180608，針對重複退申華人上網包用戶，累計流量需改以SEQ獨立包累計，否則會2包合計
	 *確認華人上網包 ，在日累計累計流量使用SX001、SX002、SX003
	 *20150115 ALTER 取消檢查門號
	 * @throws Exception 
	 */
	private String  checkQosAddon4(String serviceID,String mccmnc,Date callTime) throws Exception{
		if(serviceID!=null && !"".equals(serviceID)){
			for(Map<String,String> m : addonDataList){
				Calendar startTime = null,endTime = null;
				//取得開始時間
				startTime = Calendar.getInstance();
				startTime.setTime(year_month_day_sdf.parse(m.get("STARTDATE")));
				startTime.set(Calendar.HOUR_OF_DAY, 0);
				startTime.set(Calendar.MINUTE, 0);
				startTime.set(Calendar.SECOND, 0);
				
				//取得到期時間
				if(m.get("ENDDATE")!=null){
					endTime = Calendar.getInstance();
					endTime.setTime(year_month_day_sdf.parse(m.get("ENDDATE")));
					endTime.set(Calendar.DAY_OF_YEAR, endTime.get(Calendar.DAY_OF_YEAR)+1);
					endTime.set(Calendar.HOUR_OF_DAY, 0);
					endTime.set(Calendar.MINUTE, 0);
					endTime.set(Calendar.SECOND, 0);
				}
				
				String seq = m.get("SEQ");
				
				if(serviceID.equals(m.get("SERVICEID"))&&
						//是否符合申請的時間區段	
						(callTime.after(startTime.getTime())||callTime.equals(startTime.getTime()))&&
						(endTime==null ||callTime.before(endTime.getTime()))){
					if("SX001".equals(m.get("SERVICECODE"))&& sSX001.contains(mccmnc.substring(0, 3))){
						return "SX001"+","+seq;
					}else if("SX002".equals(m.get("SERVICECODE"))&& sSX002.contains(mccmnc.substring(0, 3))) {
						return "SX002"+","+seq;
					}else if("SX004".equals(m.get("SERVICECODE"))&& sSX004.contains(mccmnc.substring(0, 3))) {
						return "SX004"+","+seq;
					}else if("SX005".equals(m.get("SERVICECODE"))&& sSX005.contains(mccmnc.substring(0, 3))) {
						return "SX005"+","+seq;
					}
					//20180718 add
					else if("SX006".equals(m.get("SERVICECODE"))&& sSX006.contains(mccmnc.substring(0, 3))) {
						return "SX006"+","+seq;
					}
				}
			}
		}
		return null;
	}
	
	
	//20170630 add
	
	/*private boolean checkInPackage(String serviceID,String mccmnc,Date callTime) throws ParseException{
		boolean inPeriod = false;
		if(volumePocketMap.containsKey(serviceID)&&volumePocketMap.get(serviceID).containsKey(mccmnc.substring(0,3))){
			for(Map<String,String> um : volumePocketMap.get(serviceID).get(mccmnc.substring(0,3))){
				
				String Type = um.get("TYPE");
				Date uSdate = year_month_day_sdf.parse(um.get("START_DATE"));
				//含最後一天所以+24HR
				Date uEdate = new Date(year_month_day_sdf.parse(um.get("END_DATE")).getTime()+24*60*60*1000);
				//美國流量包
				//相等 0
				//日期在參數之前 < 0
				//日期在參數之後 > 0
				if("0".equals(Type) && callTime.compareTo(uSdate)>=0 && callTime.compareTo(uEdate)<=0 ){
					logger.info("inPeriod.");
					inPeriod = true;
					break;
				}			
			}
		}
		
		return inPeriod;
	}*/
	
	/**
	 *確認是否為數據預付包 客戶
	 *20150115 ALTER 取消檢查門號
	 */
	List<Map<String,String>> insertVolumeList = new ArrayList<Map<String,String>> ();
	private boolean checkDataPrepay(String serviceID,String nccNet,Date callTime,String cPriceplan,Double volumn) throws Exception{
		//logger.info("checkDataPrepay...");
		boolean isFined = false;
		boolean isNeedInsert = false;
		
		String sDate = null;
		String eDate = null;
		
		String findingType = null;
		String limit = null;
		
		String imsi =  msisdnMap.get(serviceID).get("IMSI");

		//20180112 GO2PLAY
		if(!isFined) {
			//20181002 add 9020-GO2PLAY-7D，制訂時間，結束日期為含當天為第一天+7天，共8天 2GB
			if("182".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+7*24*60*60*1000));
					isFined = true;
			}else // 20181002 add '9021-GO2PLAY-15D'，制訂時間，結束日期為含當天為第一天+15天，共16天 4GB
				if("183".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+15*24*60*60*1000));
					isFined = true;
			}else //6500 門 3天，制訂時間，結束日期為含當天為第一天+3天，共4天
				if(imsi!= null && 
					Long.parseLong(imsi)>=Long.parseLong("454120290050007") && Long.parseLong(imsi)<=Long.parseLong("454120290056506")) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+3*24*60*60*1000));
					isFined = true;
			}else //500門 7天、1000門 7 天、1000門 7天 20180816，結束日期為含當天為第一天+7天，共8天
				if(imsi!= null && 
					(Long.parseLong(imsi)>=Long.parseLong("454120290056507") && Long.parseLong(imsi)<=Long.parseLong("454120290057006")) ||
					(Long.parseLong(imsi)>=Long.parseLong("454120290066022") && Long.parseLong(imsi)<=Long.parseLong("454120290067021")) ||
					(Long.parseLong(imsi)>=Long.parseLong("454120290075022") && Long.parseLong(imsi)<=Long.parseLong("454120290076021"))) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+7*24*60*60*1000));
					isFined = true;
			}		
			
			if(isFined) {
				limit = "1024000"; //隨意給，因不累計，不會達到
				isNeedInsert = true; //要記錄
				findingType = "3";
				nccNet = "default";
			}
		}
		
		//20180612 Yunyobo
		if(!isFined) {
			//1400門 1+3 day/1GB
			if("175".equals(cPriceplan)) {
				sDate = year_month_day_sdf.format(callTime);
				eDate = year_month_day_sdf.format(new Date(callTime.getTime()+3*24*60*60*1000));
				isFined = true;
			}else 
				//1600門 1+7 day/2GB
				if("176".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+7*24*60*60*1000));
					isFined = true;
			}else 
				//YYB3 1+7 day/2GB
				if("178".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+7*24*60*60*1000));
					isFined = true;
			}else 
				//YYB4 1+15 day/4GB
				if("179".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+15*24*60*60*1000));
					isFined = true;
			}else 
				//YYB3 1+15 day/4GB
				if("181".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+15*24*60*60*1000));
					isFined = true;
			}else 
				//D-Yunyobo-C-07D-2GB 1+7 day/4GB
				if("186".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+7*24*60*60*1000));
					isFined = true;			
			}else 
				// D-Yunyobo-C-15D-4GB 1+15 day/4GB
				if("187".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+15*24*60*60*1000));
					isFined = true;
			}else 
				// D-Yunyobo-C-15D-8GB 1+15 day/4GB
				if("188".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+15*24*60*60*1000));
					isFined = true;
				}
			
			if(isFined) {
				limit = "1024000"; //隨意給，因不累計，不會達到
				isNeedInsert = true; //要記錄
				findingType = "4";
				nccNet = "default";
			}	
		}
		
		//20180612 FanTravel
		if(!isFined) {
			//9023-FANTRAVEL-7D 1000門 1+7 day
			if("185".equals(cPriceplan)) {
				sDate = year_month_day_sdf.format(callTime);
				eDate = year_month_day_sdf.format(new Date(callTime.getTime()+7*24*60*60*1000));
				isFined = true;
			}else 
				//9022-FANTRAVEL-4D 1+4 day
				if("184".equals(cPriceplan)) {
					sDate = year_month_day_sdf.format(callTime);
					eDate = year_month_day_sdf.format(new Date(callTime.getTime()+4*24*60*60*1000));
					isFined = true;
			}
			if(isFined) {
				limit = "1024000"; //隨意給，因不累計，不會達到
				isNeedInsert = true; //要記錄
				findingType = "5";
				nccNet = "default";
			}	
		}
		
		
		//從slowDownMap 非每日降速清單尋找
		//泛用規則中華在台灣使用數據用戶，PricePlan 167，TYPE 1 JOY
		/*if(!isFined) {
			for(Map<String,String> map :slowDownMap.get("0")){
				String type = map.get("TYPE");

				//Joy 資料
				if("1".equals(type)){
					//MNO清單
					String mccmnc = (String) map.get("MNO");
					Set<String> mccmncs = new HashSet<String>();
					for(String s:mccmnc.split(","))
						mccmncs.add(s);
					
					//PricePlanID清單
					String priceplan = (String) map.get("PRICEPLAN");
					Set<String> priceplans = new HashSet<String>();
					for(String s:priceplan.split(","))
						priceplans.add(s);
					
					//PricePlan符合
					if(priceplans.contains(cPriceplan)&&
							//MccMnc業者或國家符合
							(mccmncs.contains(nccNet)||mccmncs.contains(nccNet.substring(0,3)))){
						
						logger.info("Joy Data...");
						//制訂時間，結束日期為起始+7天，共8天
						sDate = year_month_day_sdf.format(callTime);
						eDate = year_month_day_sdf.format(new Date(callTime.getTime()+7*24*60*60*1000));
						
						//符合的Type資料
						findingType = type;
						limit = map.get("LIMIT");
						//以預付，不批價
						isPrePayDate = true;
						//插入流量包表
						isNeedInsert = true;
						//需要統計流量
						isNeedCount = true;
						
						isFined = true;
					}
				}
			}
		}*/
		
		
		//從slowDownMap 每日降速清單尋找
		//泛用規則，PricePlan 168、169，TYPE 2 Annex
		/*if(!isFined) {
			for(Map<String,String> map :slowDownMap.get("1")){
				String type = map.get("TYPE");
				
				//Annex 資料
				if("2".equals(type)){
					//MNO清單
					String mccmnc = (String) map.get("MNO");
					Set<String> mccmncs = new HashSet<String>();
					for(String s:mccmnc.split(","))
						mccmncs.add(s);
					
					//PricePlanID清單
					String priceplan = (String) map.get("PRICEPLAN");
					Set<String> priceplans = new HashSet<String>();
					for(String s:priceplan.split(","))
						priceplans.add(s);
					
					//PricePlan符合
					if(priceplans.contains(cPriceplan)&&
							//MccMnc業者或國家符合
							(mccmncs.contains(nccNet)||mccmncs.contains(nccNet.substring(0,3)))){
						
						logger.info("Annex Data...");
						//取得使用者天數
						int days = queryAnnexSettingDays(serviceID);
						
						if(days == -1){
							ErrorHandle("Annex customer "+serviceID+" can't find setting days.");
							break;
						}
						
						//制訂時間，結束日期為含當天為第一天+6天，共7天
						sDate = year_month_day_sdf.format(callTime);
						eDate = year_month_day_sdf.format(new Date(callTime.getTime()+(days-1)*24*60*60*1000));
						
						//符合的Type資料
						findingType = type;
						limit = map.get("LIMIT");
						//需批價，事後請款
						isPrePayDate = false;
						//插入流量包表
						isNeedInsert = true;
						//不需統計流量，為計算當日流量降速
						isNeedCount = false;
						
						isFined = true;
					}	
				}
			}
		}*/

		//如果有確認類型
		if(isFined) {
			boolean exist = false;
			//是否已在流量包新增資料
			if(volumePocketMap.containsKey(serviceID)){
				for(Map<String,String> m:volumePocketMap.get(serviceID)){
					String type = m.get("TYPE");
					String pid = m.get("PID");
					if(type.equals(findingType)) {
						//已存在
						exist = true;
						//累計含這次流量
						Double v = volumeList.get(pid);
						v+=volumn;
						volumeList.put(pid, v);
					}
				}
			}
			//流量包表無資料的話，需要新增的話，新增並處理
			if(isNeedInsert && !exist) {
				//取得PID
				String pid = getVolumePocketPID();
				
				if(pid == null ) throw new Exception("Can't get pid.");
					
				String mcc = nccNet.substring(0,3);
				
				//設定Insert Map
				Map<String,String> iMap = new HashMap<String,String>();
				iMap.put("SERVICEID", serviceID);
				iMap.put("MCC", mcc);
				iMap.put("SDATE", sDate);
				iMap.put("EDATE", eDate);
				iMap.put("TYPE", findingType);
				iMap.put("LIMIT",  limit);
				iMap.put("PID", pid);
				iMap.put("IMSI", msisdnMap.get(serviceID).get("IMSI"));
				insertVolumeList.add(iMap);
				
				//放入快取表格資料
				Map<String, String> m = new HashMap<String,String>();
				m.put("START_DATE", sDate);
				m.put("END_DATE", eDate);
				m.put("CURRENCY", "NTD");
				m.put("ALERTED", "0");
				m.put("TYPE", findingType);
				m.put("LIMIT", limit);
				m.put("PID", pid);
				m.put("MCC", mcc);				
				m.put("TERMINATE", "0");
				m.put("IS_RESUME", "0");
				
				List<Map<String, String>> volumePocketList = volumePocketMap.get(serviceID);
				if(volumePocketList == null) volumePocketList = new ArrayList<Map<String,String>>();
						
				volumePocketList.add(m);		
				volumePocketMap.put(serviceID, volumePocketList);
		
				//初始VolumeList，流量統計
				volumeList.put(pid, volumn);
			}
		}
		return isFined;
	}
	
	
	private int queryAnnexSettingDays(String serviceID){
		int result = -1;

		Statement st = null;
		ResultSet rs = null;
		try {
			sql =
					"SELECT DAYS From HUR_PACKAGE_SETTING where serviceid = '"+serviceID+"' ";
			
			st = conn.createStatement();
			logger.debug("Query SMSSetting SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				result =rs.getInt("DAYS");
			}
		} catch (SQLException e) {
			ErrorHandle("At setTADIGtoMCCMNC occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}

		return result;

	}
	
	
	/**
	 * 由IP找尋MCCMNC  20141211 add
	 * @param ipaddr
	 * @return
	 */
	private String searchMccmncByIP(String ipaddr){
		String mccmnc=null;
		//20141211 add 藉由網域資料判定,Map Value:START_NUM,END_NUM,MCCMNC
		if(ipaddr.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")){
			String [] ips = ipaddr.split("\\.");
			long ipNumber =0L;
			for(int j=0;j<ips.length;j++){
				ipNumber+=parseInt(ips[j])*Math.pow(256, 3-j);
			}
			System.out.println("ipNumber="+ipNumber);
			
			for(Map<String,String> m : IPtoMccmncList){
				long startNum = Long.parseLong(m.get("START_NUM"));
				long EndNum = Long.parseLong(m.get("END_NUM"));
				
				if(startNum <= ipNumber && ipNumber <= EndNum){
					mccmnc = (String) m.get("MCCMNC");
					break;
				}
			}
		}
		return mccmnc;
	}
	
	Map<String,Map<String,String>> userLocation = new HashMap<String,Map<String,String>>();
	/**
	 * 開始批價
	 */
	private void charge(){
		logger.info("charge...");
		subStartTime = System.currentTimeMillis();
		
		//20151230 add
		//20151231 cancel
		//addonMark.clear();
		//notAddonMark.clear();
		updateMap.clear();
		updateMapD.clear();
		updateMapDD.clear();
		cdrChargeMap.clear();
		
		userLocation.clear();
		
		Statement st = null;
		ResultSet rs = null;
		Statement st2 = null;
		ResultSet rs2 = null;
		
		Double defaultRate=0.011;
		try {
			//count=getDataCount();
			//20151229 del
			//defaultRate=defaultRate();
			//setQosData();

			//批次Query 避免ram空間不足
			for(int i=1; !minId.equals(maxId) && i<=5;i++){
				/*sql=
						"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM HUR_DATA_USAGE A WHERE ROWNUM <= "+(i*dataThreshold)+" AND A.CHARGE is null and usageid<= "+maxId+" "
						//+ "AND A.FILEID>= "+lastfileID+" "
						+ "ORDER BY A.USAGEID,A.FILEID) "
						+ "MINUS "
						+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM HUR_DATA_USAGE A WHERE ROWNUM <= "+((i-1)*dataThreshold)+" AND A.CHARGE is null and usageid<= "+maxId+" "
						//+ "AND A.FILEID= "+lastfileID+" "
						+ "ORDER BY A.USAGEID,A.FILEID) ";*/
				
				sql = ""
						+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS,CHARGE "
						+ "FROM ( "
						+ "		SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS,CHARGE,"
						+ "						ROW_NUMBER() OVER(ORDER BY A.USAGEID,A.FILEID DESC) ROW_NUM "
						+ "		FROM HUR_DATA_USAGE A "
						+ "		WHERE  A.CHARGE is null and usageid<= "+maxId+" ) "
						+ "where ROW_NUM<"+(i*dataThreshold)+" "
						+ "MINUS "
						+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS,CHARGE "
						+ "FROM ( "
						+ "		SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS,CHARGE,"
						+ "						ROW_NUMBER() OVER(ORDER BY A.USAGEID,A.FILEID DESC) ROW_NUM "
						+ "		FROM HUR_DATA_USAGE A "
						+ "		WHERE  A.CHARGE is null and usageid<= "+maxId+" ) "
						+ "where ROW_NUM<"+((i-1)*dataThreshold)+" ";
				
				
				
				logger.debug("Execute SQL : "+sql);
				
				logger.debug("Round "+i+" Procsess ...");
				st = conn.createStatement();
				rs = st.executeQuery(sql);

				while(rs.next()){			
					String logMsg="";
					
					
					//取出CDR資料
					String imsi= rs.getString("IMSI");
					String mccmnc=rs.getString("MCCMNC");
					String usageId=rs.getString("USAGEID");
					Double volume=rs.getDouble("DATAVOLUME");
					Double oVolume = volume;//因volume在美國流量包過程中會被修改，新增參數記錄原始流量
					String sCallTime=rs.getString("CALLTIME");
					Date callTime=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(sCallTime);
					Integer fileID=rs.getInt("FILEID");	
					
					//20141211 add
					String ipaddr = rs.getString("SGSNADDRESS");
					//logger.debug("For CDR usageId="+usageId+" get IP Address "+ipaddr);
					
					minId = usageId;
					//logger.info("minId:"+minId);
					
					//抓到對應的Serviceid
					//20150115 add
					String serviceid = null;
			
					//從IMSI找尋ServiceID
					//從換卡記錄找IMSI最後的ServiceID
					serviceid = IMSItoServiceIdMap.get(imsi);
					
					/*if(serviceid==null || "".equals(serviceid)){
						sql="";
						ErrorHandle("For CDR usageId="+usageId+" which can't find  ServceID." );
						continue;
					}*/
					
					
					//20160906 add
					if(serviceid==null || "".equals(serviceid)){
						String sql2= ""
								+ "SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,B.ICCID, "
								+ "		(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE "
								+ "FROM SERVICE A,(Select IMSI,SERVICEID,ICCID from IMSI where IMSI = '"+imsi+"' )B,PARAMETERVALUE C "
								+ "WHERE A.SERVICEID=B.SERVICEID AND A.SERVICECODE IS NOT NULL "
								+ "AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 ";
						st2 = conn.createStatement();
						logger.info("Execute SQL :"+sql2);
						rs2=st2.executeQuery(sql2);
						logger.info("Query end!");
						
						while(rs2.next()){
							serviceid = rs2.getString("SERVICEID");
							
							Map<String,String> map =new HashMap<String,String>();
							map.put("MSISDN", rs2.getString("SERVICECODE"));
							map.put("PRICEPLANID", rs2.getString("PRICEPLANID"));
							map.put("SUBSIDIARYID", rs2.getString("SUBSIDIARYID"));
							map.put("NCODE", rs2.getString("NCODE"));
							map.put("SERVICEID", rs2.getString("SERVICEID"));
							map.put("IMSI", rs.getString("IMSI"));
							map.put("ICCID", rs.getString("ICCID"));
							msisdnMap.put(rs2.getString("IMSI"), map);
							msisdnMap.put(rs2.getString("SERVICEID"), map);
						}
						
						if(serviceid==null || "".equals(serviceid)){
							sql="";
							ErrorHandle("For CDR usageId="+usageId+" which can't find  ServceID." );
							continue;
						}else{
							IMSItoServiceIdMap.put(imsi, serviceid);
							//ServiceIdtoIMSIMap.put(serviceid, imsi);
						}
					}

					//從ServiceId找出PricePlan
					//抓到對應的PricePlanid
					String pricplanID=null;
					if(msisdnMap.containsKey(serviceid))
						pricplanID=msisdnMap.get(serviceid).get("PRICEPLANID");
					else{
						sql="";
						ErrorHandle("For CDR usageId="+usageId+" which can't find  PricePlanID." );
						continue;
					}
						
					
					Double charge=0D;
					Double dayCap=null;
					
					/*if(dataRate.containsKey(pricplanID)){
						//20141210 add
						currency=pricePlanIdtoCurrency.get(pricplanID);
						if(currency== null)
							ErrorHandle("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+"find currency="+currency);

						if(mccmnc==null || "".equals(mccmnc)){
							mccmnc=searchMccmncBySERVICEID(serviceid);
						}
					}else{
						sql="";
						ErrorHandle("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
					}*/
					
					
					//假設無MCCMNC嘗試取得Mccmnc
					//20141211 add
					if(mccmnc==null || "".equals(mccmnc)){
						logger.debug("Findding MCCMNC by IP range");
						mccmnc=searchMccmncByIP(ipaddr);
						if(mccmnc!=null && !"".equals(mccmnc))
							logger.debug("For CDR usageId="+usageId+" which is without mccmnc. Found mccmnc="+mccmnc+" by IP range.");
					}
					
					if(mccmnc==null || "".equals(mccmnc)){
						logger.debug("Findding MCCMNC by serviceid");
						mccmnc=searchMccmncBySERVICEID(serviceid);
						if(mccmnc!=null && !"".equals(mccmnc))
							logger.debug("For CDR usageId="+usageId+" which is without mccmnc. Found mccmnc="+mccmnc+" by serviceid.");
					}

					//還是找不到，給予預設，必須有
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc= DEFAULT_MCCMNC;
						logger.debug("usageId:"+usageId+" set mccmnc to default!");
					}
					
					//察看是否有以存在的資料，有的話取出做累加
					//20150324 modify mccmnc to mcc + network
					
					
					//以國碼+業者代碼作為累計的key值，不同業者不一起累計
					String mccNet;
					if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
							dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
						//System.out.println(mccmnc);
						mccNet=mccmnc.substring(0,3);
						mccNet+=dataRate.get(pricplanID).get(mccmnc).get("NETWORK");
					}else{
						mccNet=DEFAULT_MCCMNC;
					}
					
					
					//20180307 add
					String msisdn = (String) getMSISDN(serviceid);
					//更新 使用者最後位置
					if((userLocation.get(serviceid)==null || 
							callTime.after(day_time_sdf.parse(userLocation.get(serviceid).get("TIME"))))&&
							callTime.after(year_month_day_sdf.parse(year_month_day_sdf.format(new Date())) //只記錄今天的位置
									) 
							) {
						Map<String,String> m = new HashMap<String,String>();
						m.put("MCCNET", mccNet);
						m.put("TIME", day_time_sdf.format(callTime));
						userLocation.put(serviceid, m);
					}
					

					//String mcc = mccmnc.substring(0,3);
					
					double resetVolume = 0d;
					boolean isAddedUser = checkQosAddon(serviceid, mccmnc, callTime,volume);
					boolean isPrepayUser = checkDataPrepay(serviceid, mccNet, callTime, pricplanID,volume);

					if(isAddedUser){
						//是華人上網包，不批價設定為0
					}else if(isPrepayUser){
						//確認是否為預付卡資料(例如Joy)
					}else{
						//20151230 add
						//20151231 cancel
						//notAddonMark.add(serviceid);
						
						volume=volume!=null?Math.ceil(volume*kByte):0;//配合費率表單位變成KB
						
						//取出幣別
						String currency= null;
						if(dataRate.containsKey(pricplanID)){
							currency = pricePlanIdtoCurrency.get(pricplanID);
						}else{
							ErrorHandle("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+"find currency="+currency);
						}

						//判斷是否可以找到對應的費率表，並計算此筆CDR的價格(charge)
						if(currency!=null && dataRate.get(pricplanID).containsKey(mccmnc)){
							
							//20160719 add
							//美國流量包客戶計費，期間內上限內免費，上限外打折，其餘正常計算
							//boolean inPacketRange=false;
							
							/*if(volumePocketMap.containsKey(serviceid)&&volumePocketMap.get(serviceid).containsKey(mcc)){
								
								for(Map<String,String> m : volumePocketMap.get(serviceid).get(mcc)){
									String startDate = (String) m.get("START_DATE");
									String endDate = (String) m.get("END_DATE");
									int type = parseInt((String) m.get("TYPE"));
									double pocketLimit = parseDouble((String) m.get("LIMIT"));
									String pid = (String) m.get("PID");
								
									if(type == 0 &&
											callTime.after(year_month_day_sdf.parse(startDate))&& 
											callTime.before(new Date(year_month_day_sdf.parse(endDate).getTime()+1000*60*60*24))){
										
										inPacketRange = true;
										
										double v = volumeList.get(pid);
										//累計(因每次都會重新計算，所以用不到)
										//volumeList.put(pid, v+oVolume);
										
										
										v=Math.ceil(v*kByte);//變成KB
										
										if(v>pocketLimit){
											v = pocketLimit;
										}
										
										v += volume;
	
										if(v<pocketLimit){
											volume = 0d;
										}else{
											volume = v- pocketLimit;
											v = pocketLimit;
										}
									}
								}
							}*/
							double ec=1;
							//boolean haveRate = false;
							
							Map<String,String> m = dataRate.get(pricplanID).get(mccmnc);
							
							//假設在日期範圍內找無適合的費率   
							if(m==null){
								sql="";
								ErrorHandle("usageId:"+usageId+",CALLTIME:"+callTime.toString()+" can't find datarate in date range ! ");
								continue;
							}			
							
							//取消幣別轉換，直接以原幣計價
							/*if("HKD".equalsIgnoreCase(currency))
								ec=exchangeRate;*/
								
							Double rate=parseDouble((String) m.get("RATE"));
							Double unit=parseDouble((String) m.get("CHARGEUNIT"));
							charge=volume*rate*ec/unit;
							
							//在數據包期間內超過流量金額為95折
							//if(inPacketRange) charge*=0.95;
							
							
							dayCap=parseDouble((String) m.get("DAYCAP"));
							
							String country = (String) m.get("COUNTRY");
							
							//數據通知警示
							try {
								//20180724 add 當單筆超過一塊時才發出警示通知
								if(charge>=1d && dataAnnounceMap.containsKey(pricplanID) && 
										dataAnnounceMap.get(pricplanID).containsKey(mccmnc.substring(0,3))&& 
										!userDataAnnounceSet.contains(msisdn)){
									
									logger.info("send data annouce SMS to "+msisdn);
									sendDataUsageAnnounce(serviceid,msisdn,dataAnnounceMap.get(pricplanID).get(mccmnc.substring(0,3)),dayCap);
									userDataAnnounceSet.add(msisdn);
								}
							} catch (Exception e) {
								sendErrorMail("send announce Error.");
							}
							
							
							/*haveRate=true;
							break;*/
							
							/*for(Map<String,String> m : dataRate.get(pricplanID).get(mccmnc)){
								
								Date sdate = year_month_day_sdf.parse(m.get("STARTTIME"));
								Date edate = m.get("ENDTIME")!=null?year_month_day_sdf.parse(m.get("ENDTIME")):null;
								
								if(sdate.equals(callTime)||sdate.before(callTime)&&(edate==null || edate.after(callTime))){
									
									
								}
							}	*/	
											
						}else{
							ErrorHandle("usageId:"+usageId+",IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");
							
							//沒有PRICEPLANID(月租方案)，MCCMNC，無法判斷區域業者，作法：統計流量，
							//沒有對應的PRICEPLANID(月租方案)，MCCMNC，無法判斷區域業者
							//以預設費率計費
							
							//20141210 假設幣值為港幣，將平均台幣換算成平均港幣
							double ec=1;					
							if("HKD".equalsIgnoreCase(currency))
								ec=exchangeRate;
							
							
							//System.out.println("New Version check Point defaultRate.");
							//預設費率
							if("NTD".equals(pricePlanIdtoCurrency.get(pricplanID)))
								defaultRate = getSystemConfigDoubleParam(pricplanID,"NTD_DEFAULT_RATE");
							if("HKD".equals(pricePlanIdtoCurrency.get(pricplanID)))
								defaultRate = getSystemConfigDoubleParam(pricplanID,"HKD_DEFAULT_RATE");
							
							//取不到任何預設值 跳過
							if(defaultRate == null){
								sql="";
								ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricplanID+" cannot get defaultRate! ");
								continue;
							}
							System.out.println("Default Rate:"+defaultRate);
							
							charge=volume*defaultRate/ec;
						}
						
						
					}
					
					//20180316 add 只要在華人上網包申請期間，就不計算重置累積量
					//不是上網包用戶，也不是預付用戶
					if( checkQosAddon3(serviceid,callTime)==null && !isPrepayUser) {
						//20180314 add
						//20180307 add 在reset之後才累計
						if(qosResetLogTimeMap.get(msisdn) == null || callTime.after(qosResetLogTimeMap.get(msisdn))) {
							resetVolume = oVolume;
						}
					}
					
					
					//20151230 add
					//20151231 cancel
					//addonMark.removeAll(notAddonMark);
					
					//格式化至小數點後四位
					charge=FormatDouble(charge, "0.0000");

					//將此筆CDR結果記錄，稍後回寫到USAGE TABLE
					cdrChargeMap.put(usageId, charge);
					logMsg+="UsageId "+usageId+" ,IMSI "+imsi+" ,MCCMNC "+mccmnc+" charge result is "+cdrChargeMap.get(usageId)+". ";

					
					//日累計處理
					String cDay=year_month_day_sdf.format(callTime);
					Double oldCharge=0D;
					Double oldvolume=0D;
					Double oldResetVolume = 0D;
					String alert="0";
					Map<String, Map<String, Map<String, String>>> map=new HashMap<String,Map<String,Map<String,String>>>();
					Map<String,Map<String,String>> map2=new HashMap<String,Map<String,String>>();
					Map<String,String> map3=new HashMap<String,String>();
					
					//取出舊值
					if(currentDayMap.containsKey(cDay)){
						map=currentDayMap.get(cDay);
						if(map.containsKey(serviceid)){
							map2=map.get(serviceid);
							if( map2.containsKey(mccNet)){
								map3=map2.get(mccNet);

								oldCharge=parseDouble((String) map3.get("CHARGE"));
								
								logMsg+="The old Daily charge is "+oldCharge+". ";
								
								//summary charge
								charge=oldCharge+charge;
								charge=FormatDouble(charge, "0.0000");
								alert=(String)map3.get("ALERT");
								oldvolume=parseDouble((String) map3.get("VOLUME"));
								//20180307 add
								oldResetVolume = parseDouble((String) map3.get("RESET_VOLUME"));
							
								if(fileID<parseInt((String) map3.get("LAST_FILEID")))
									fileID=parseInt((String) map3.get("LAST_FILEID"));
							}
						}
					}

					//如果有計費上線，限制最大值  20141125 取消預設Daycap，如果值為負，表示沒有
					//if(dayCap==null || dayCap==0) dayCap= DEFAULT_DAYCAP;
					if(dayCap!=null && dayCap>=0 && charge>dayCap) charge=dayCap;
					
					//將結果記錄到currentDayMap
					map3.put("CHARGE", String.valueOf(charge));
					logMsg+="The final Daily charge is "+map3.get("CHARGE")+". ";
					map3.put("LAST_FILEID",String.valueOf(fileID));
					map3.put("LAST_DATA_TIME",sCallTime);
					map3.put("VOLUME",String.valueOf(oVolume+oldvolume));
					//20180307 add 
					map3.put("RESET_VOLUME",String.valueOf(resetVolume+oldResetVolume));
					
					map3.put("ALERT",alert);
					map2.put(mccNet, map3);
					map.put(serviceid, map2);
					currentDayMap.put(cDay, map);
					
					//20150505 add
					//標記需要更新的資料
					Map <String,Set<String>> map6 = new HashMap<String,Set<String>>();
					Set<String> set1 = new HashSet<String>();					
					//取出舊值
					if(updateMapD.containsKey(cDay)){	
						map6 = updateMapD.get(cDay);
						if(map6.containsKey(serviceid)){
							set1 = map6.get(serviceid);
						}
					}
					set1.add(mccNet);
					map6.put(serviceid, set1);
					updateMapD.put(cDay, map6);
					

					//20180828 add
					//日累計細節處理
					//日累計處理
					//String cDay=year_month_day_sdf.format(callTime);
					//oldCharge=0D;
					//oldvolume=0D;
					Map<String, Map<String, Map<String, String>>> mapd=new HashMap<String,Map<String,Map<String,String>>>();
					Map<String,Map<String,String>> mapd2=new HashMap<String,Map<String,String>>();
					Map<String,String> mapd3=new HashMap<String,String>();
					
					Double oldDetailCharge = 0D;
					Double oldDetailVolume = 0D;
					Double difCharge = charge - oldCharge;
					
					//取出舊值
					if(currentDayDetailMap.containsKey(cDay)){
						mapd=currentDayDetailMap.get(cDay);
						if(mapd.containsKey(serviceid)){
							mapd2=mapd.get(serviceid);
							if( mapd2.containsKey(mccmnc)){
								mapd3=mapd2.get(mccmnc);

								oldDetailCharge=parseDouble((String) mapd3.get("CHARGE"));
								
								//summary charge
								//charge=oldCharge+charge;
								//charge=FormatDouble(charge, "0.0000");
								//alert=(String)map3.get("ALERT");
								oldDetailVolume=parseDouble((String) mapd3.get("VOLUME"));
		
							}
						}
					}

					//將結果記錄到currentDayMap
					mapd3.put("CHARGE", String.valueOf(FormatDouble(oldDetailCharge+difCharge, "0.0000")));
					mapd3.put("VOLUME",String.valueOf(oVolume+oldDetailVolume));
					mapd3.put("MCCNET",mccNet);
					mapd2.put(mccmnc, mapd3);
					mapd.put(serviceid, mapd2);
					currentDayDetailMap.put(cDay, mapd);
					
					//20180828 add
					//標記需要更新的資料
					Map <String,Set<String>> mapd6 = new HashMap<String,Set<String>>();
					Set<String> setd1 = new HashSet<String>();					
					//取出舊值
					if(updateMapDD.containsKey(cDay)){	
						mapd6 = updateMapDD.get(cDay);
						if(mapd6.containsKey(serviceid)){
							setd1 = mapd6.get(serviceid);
						}
					}
					setd1.add(mccmnc);
					mapd6.put(serviceid, setd1);
					updateMapDD.put(cDay, mapd6);
					
					
					
					//月累計處理
					Double preCharge=0D;
					Integer smsTimes=0;
					String suspend="0";
					String cMonth=cDay.substring(0,6);
					Double lastAlertThreshold=0D;
					Double volumeAlert=0D;
					
					Map<String,String> map4=new HashMap<String,String>();
					Map<String,Map<String,String>> map5=new HashMap<String,Map<String,String>>();
					
					if(currentMap.containsKey(cMonth)){
						map5=currentMap.get(cMonth);
					}
					//取出舊值
					if(map5.containsKey(serviceid)){
						map4=map5.get(serviceid);
						
						//當月扣除此日
						preCharge=parseDouble((String)map4.get("CHARGE"))-oldCharge;
						
						logMsg+="The old month charge is "+parseDouble((String) map4.get("CHARGE"))+". ";
						smsTimes=parseInt((String) map4.get("SMS_TIMES"));
						suspend=(String) map4.get("EVER_SUSPEND");
						oVolume=parseDouble((String) map4.get("VOLUME"))+oVolume;
						lastAlertThreshold=parseDouble((String) map4.get("LAST_ALERN_THRESHOLD"));
						volumeAlert=parseDouble((String) map4.get("LAST_ALERN_VOLUME"));
						
						if(fileID<parseInt((String) map4.get("LAST_FILEID")))
							fileID=parseInt((String) map4.get("LAST_FILEID"));
					}
					
					
					map4.put("LAST_FILEID", fileID.toString());
					map4.put("SMS_TIMES", smsTimes.toString());
					map4.put("LAST_DATA_TIME", sCallTime);
					//最後累記為扣除此日+新的此日
					charge=preCharge+charge;
					charge=FormatDouble(charge, "0.0000");
					map4.put("CHARGE", charge.toString());
					logMsg+="The final month charge is "+map4.get("CHARGE")+". ";
					
					map4.put("VOLUME", oVolume.toString());
					map4.put("EVER_SUSPEND", suspend);
					map4.put("LAST_ALERN_THRESHOLD", lastAlertThreshold.toString());
					map4.put("LAST_ALERN_VOLUME", volumeAlert.toString());
					map5.put(serviceid, map4);
					currentMap.put(cMonth, map5);
					
					//20150505
					Set<String> set2 = new HashSet<String>();
					
					if(updateMap.containsKey(cMonth)){	
						set2 = updateMap.get(cMonth);
					}
					set2.add(serviceid);
					updateMap.put(cMonth, set2);

					logger.debug(logMsg);
				}
			}
		} catch (SQLException e) {
			ErrorHandle("At charge occur SQLException error", e);
		} catch (ParseException e) {
			sql="";
			ErrorHandle("At charge occur ParseException error", e);
		} catch (Exception e) {
			sql="";
			ErrorHandle("At charge occur Exception error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(st2!=null)
					st2.close();
				if(rs!=null)
					rs.close();
				if(rs2!=null)
					rs2.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	/***
	 * 回寫CDR的CHARGE
	 */
	private boolean updateCdr(){
		logger.info("updateCdr...");
		subStartTime = System.currentTimeMillis();
		
		boolean result =false;
		int count=0;
		
		Statement st = null;

		try {
			st = conn.createStatement();
			for(String s: cdrChargeMap.keySet()){
				sql=
						"UPDATE HUR_DATA_USAGE A SET A.CHARGE="+cdrChargeMap.get(s)+" WHERE A.USAGEID='"+s+"' ";
				st.addBatch(sql);
				count++;

				if(count==dataThreshold){
					logger.info("Execute updateCdr Batch");
					st.executeBatch();
					count=0;
				}
			}
			if(count!=0){
				st.executeBatch();
			}
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At updateCdr occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At updateCdr occur Exception error", e);
		}finally{
			cdrChargeMap.clear();
			try {
				if(st!=null)
					st.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}

	/**
	 * 計算完畢後寫回資料庫-更新
	 * @throws SQLException 
	 */
	private boolean updateCurrentMap(){
		logger.info("updateCurrentMap...");
		subStartTime = System.currentTimeMillis();

		boolean result = false;
		int count=0;
		
		sql=
				"UPDATE HUR_CURRENT A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.SMS_TIMES=?,A.LAST_DATA_TIME=TO_DATE(?,'yyyy/MM/dd hh24:mi:ss'),A.VOLUME=?,A.EVER_SUSPEND=?,A.LAST_ALERN_THRESHOLD=?,A.LAST_ALERN_VOLUME=?,A.UPDATE_DATE=SYSDATE "
				+ "WHERE A.MONTH=? AND A.SERVICEID=? ";
		
		logger.info("Execute SQL :"+sql);

		PreparedStatement pst = null;
		try {
			pst = conn.prepareStatement(sql);
			//20150505 add only update are modified
			for(String mon : updateMap.keySet()){
				for(String serviceid : updateMap.get(mon)){
					pst.setDouble(1,parseDouble((String) currentMap.get(mon).get(serviceid).get("CHARGE")));
					pst.setInt(2, parseInt((String) currentMap.get(mon).get(serviceid).get("LAST_FILEID")));
					pst.setInt(3, parseInt((String) currentMap.get(mon).get(serviceid).get("SMS_TIMES")));
					pst.setString(4, (String) currentMap.get(mon).get(serviceid).get("LAST_DATA_TIME"));
					pst.setDouble(5,parseDouble((String) currentMap.get(mon).get(serviceid).get("VOLUME")));
					pst.setString(6,(String) currentMap.get(mon).get(serviceid).get("EVER_SUSPEND"));
					pst.setDouble(7,parseDouble((String) currentMap.get(mon).get(serviceid).get("LAST_ALERN_THRESHOLD")));
					pst.setDouble(8,parseDouble((String) currentMap.get(mon).get(serviceid).get("LAST_ALERN_VOLUME")));
					pst.setString(9, mon);
					pst.setString(10, serviceid);//具有mccmnc
					pst.addBatch();
					count++;
					if(count==dataThreshold){
						logger.info("Execute updateCurrentMap Batch");
						pst.executeBatch();
						count=0;
					}	
				}
			}
			if(count!=0){
				logger.info("Execute updateCurrentMap Batch");
				pst.executeBatch();
			}
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At updateCurrentMap occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At updateCurrentMap occur Exception error", e);
		}finally{
			try {
				if(pst!=null)
					pst.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	private boolean updateCurrentMapDay(){
		logger.info("updateCurrentMapDay...");
		subStartTime = System.currentTimeMillis();

		int count=0;
		boolean result= false;
		sql=
				"UPDATE HUR_CURRENT_DAY A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.LAST_DATA_TIME=TO_DATE(?,'yyyy/MM/dd hh24:mi:ss'),A.VOLUME=?,A.ALERT=?,A.IS_SLOWDOWN=?,A.UPDATE_DATE=SYSDATE,A.RESET_VOLUME = ? "
				+ "WHERE A.DAY=? AND A.SERVICEID=? AND A.MCCMNC=? ";
		
		logger.info("Execute SQL :"+sql);
		
		PreparedStatement pst = null;
		try {
			pst = conn.prepareStatement(sql);
			
			//20141201 add change another method to distinguish insert and update
			for(String day : updateMapD.keySet()){
				for(String serviceid : updateMapD.get(day).keySet()){
					for(String nccNet : updateMapD.get(day).get(serviceid)){
						pst.setDouble(1,parseDouble((String) currentDayMap.get(day).get(serviceid).get(nccNet).get("CHARGE")));
						pst.setString(2, (String) currentDayMap.get(day).get(serviceid).get(nccNet).get("LAST_FILEID"));
						pst.setString(3, (String) currentDayMap.get(day).get(serviceid).get(nccNet).get("LAST_DATA_TIME"));
						pst.setDouble(4,parseDouble((String) currentDayMap.get(day).get(serviceid).get(nccNet).get("VOLUME")));
						pst.setString(5,(String) currentDayMap.get(day).get(serviceid).get(nccNet).get("ALERT"));
						pst.setString(6,"1".equals((String) currentDayMap.get(day).get(serviceid).get(nccNet).get("IS_SLOWDOWN"))?"1":"0");
						pst.setString(7, (String) currentDayMap.get(day).get(serviceid).get(nccNet).get("RESET_VOLUME"));
						pst.setString(8, day);
						pst.setString(9, serviceid);
						pst.setString(10, nccNet);
						
						pst.addBatch();
						count++;
						if(count==dataThreshold){
							logger.info("Execute updateCurrentMapU Batch");
							pst.executeBatch();
							count=0;
						}
					}
				}
			}
			if(count!=0){
				logger.info("Execute updateCurrentMapU Batch");
				pst.executeBatch();
			}
			
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At updateCurrentMapU occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At updateCurrentMapU occur Exception error", e);
		}finally{
			try {
				if(pst!=null)
					pst.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	private boolean updateCurrentDayDetailMap(){
		logger.info("updateCurrentDayDetailMap...");
		subStartTime = System.currentTimeMillis();

		int count=0;
		boolean result= false;
		
		sql = "update HUR_CURRENT_DAY_DETAIL "
				+ "set MCCNET = ?,VOLUME=?,CHARGE=?,UPDATETIME=sysdate "
				+ "where DAY=? and SERVICEID = ? and MCCMNC = ? ";
		
		logger.info("Execute SQL :"+sql);
		
		PreparedStatement pst = null;
		try {
			pst = conn.prepareStatement(sql);
			
			//20141201 add change another method to distinguish insert and update
			for(String day : updateMapDD.keySet()){
				for(String serviceid : updateMapDD.get(day).keySet()){
					for(String mccmnc : updateMapDD.get(day).get(serviceid)){
						pst.setString(1,(String) currentDayDetailMap.get(day).get(serviceid).get(mccmnc).get("MCCNET"));
						pst.setDouble(2,parseDouble((String) currentDayDetailMap.get(day).get(serviceid).get(mccmnc).get("VOLUME")));
						pst.setDouble(3,parseDouble((String) currentDayDetailMap.get(day).get(serviceid).get(mccmnc).get("CHARGE")));
						pst.setString(4, day);
						pst.setString(5, serviceid);
						pst.setString(6, mccmnc);
						
						pst.addBatch();
						count++;
						if(count==dataThreshold){
							logger.info("Execute updateCurrentDayDetailMap Batch");
							pst.executeBatch();
							count=0;
						}
					}
				}
			}
			if(count!=0){
				logger.info("Execute updateCurrentDayDetailMap Batch");
				pst.executeBatch();
			}
			
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At updateCurrentDayDetailMap occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At updateCurrentDayDetailMap occur Exception error", e);
		}finally{
			try {
				if(pst!=null)
					pst.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	/**
	 * 計算完畢後寫回資料庫-新增
	 * @throws SQLException 
	 */
	
	private boolean insertCurrentMap(){
		logger.info("insertCurrentMap...");
		subStartTime = System.currentTimeMillis();
		
		boolean result = false;
		
		Statement st = null;
		
		try {
			st = conn.createStatement();
		
			/*for(String mon : insertMap.keySet()){
				for(String imsi : insertMap.get(mon)){*/
			//20141201 add change another method to distinguish insert and update
			for(String mon : currentMap.keySet()){
				for(String serviceid : currentMap.get(mon).keySet()){
					if(!existMap.containsKey(mon) || !existMap.get(mon).contains(serviceid)){
						sql=
								"INSERT INTO HUR_CURRENT (SERVICEID,MONTH,CREATE_DATE) VALUES('"+serviceid+"','"+mon+"',SYSDATE)";
						logger.info("Execute SQL:"+sql);
						st.executeUpdate(sql);
						
						//20141229 alter insert data before update
						Set<String> set=new HashSet<String>();
						if(existMap.containsKey(mon))
							set=existMap.get(mon);
						
						set.add(serviceid);
						existMap.put(mon, set);
					}
				}
			}
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At insertCurrent occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At insertCurrent occur Exception error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
			} catch (SQLException e) {
			}
		}

		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	private boolean insertCurrentMapDay(){
		logger.info("insertCurrentMapDay...");
		subStartTime = System.currentTimeMillis();

		boolean result = false;
		
		Statement st = null;
		
		try {
			st = conn.createStatement();
			//20141201 add change another method to distinguish insert and update
			for(String day : currentDayMap.keySet()){
				for(String serviceid : currentDayMap.get(day).keySet()){
					for(String nccNet : currentDayMap.get(day).get(serviceid).keySet()){
						if(!existMapD.containsKey(day)||!existMapD.get(day).containsKey(serviceid)||!existMapD.get(day).get(serviceid).contains(nccNet)){
							
							sql=
									"INSERT INTO HUR_CURRENT_DAY "
									+ "(SERVICEID,CREATE_DATE,MCCMNC,DAY) "
									+ "VALUES('"+serviceid+"',SYSDATE,'"+nccNet+"','"+day+"')";
							
							logger.info("Execute SQL :"+sql);
							st.executeUpdate(sql);
							//20141229 alter insert data before update
							Set<String> set=new HashSet<String>();
							Map<String,Set<String>> map = new HashMap<String,Set<String>>();
							if(existMapD.containsKey(day)){
								map=existMapD.get(day);
								if(map.containsKey(serviceid)){
									set=map.get(serviceid);
								}							
							}
							set.add(nccNet);
							map.put(serviceid, set);
							existMapD.put(day,map);
						}
					}
				}
			}
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At insertCurrentDay occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At insertCurrentDay occur Exception error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	private boolean insertCurrentDayDetailMap(){
		logger.info("insertCurrentDayDetailMap...");
		subStartTime = System.currentTimeMillis();
		boolean result = false;
		
		Statement st = null;
		
		try {
			st = conn.createStatement();
			//20141201 add change another method to distinguish insert and update
			for(String day : currentDayDetailMap.keySet()){
				for(String serviceid : currentDayDetailMap.get(day).keySet()){
					for(String mccmnc : currentDayDetailMap.get(day).get(serviceid).keySet()){
						if(!existMapDD.containsKey(day)||!existMapDD.get(day).containsKey(serviceid)||!existMapDD.get(day).get(serviceid).contains(mccmnc)){
							
							sql = "insert into HUR_CURRENT_DAY_DETAIL"
									+ "(SERVICEID,MCCMNC,DAY) "
									+ "values("+serviceid+",'"+mccmnc+"','"+day+"')";
							
							logger.info("Execute SQL :"+sql);
							st.executeUpdate(sql);
							//20141229 alter insert data before update
							Set<String> set=new HashSet<String>();
							Map<String,Set<String>> map = new HashMap<String,Set<String>>();
							if(existMapDD.containsKey(day)){
								map=existMapDD.get(day);
								if(map.containsKey(serviceid)){
									set=map.get(serviceid);
								}							
							}
							set.add(mccmnc);
							map.put(serviceid, set);
							existMapDD.put(day,map);
						}
					}
				}
			}
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At insertCurrentDayDetailMap occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At insertCurrentDayDetailMap occur Exception error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}

	/*
	List<Integer> times=new ArrayList<Integer>();
	List<Double> bracket=new ArrayList<Double>();
	List<String> msg=new ArrayList<String>();
	List<String> suspend=new ArrayList<String>();
	
	public Boolean getSMSsetting(){
		//載入簡訊設定
		//times 設定編號，無作用
		//bracket 警示額度，以0~1的比率
		//msg 要發送的id，如果多個id以","號分開
		//suspend 1表示需要中斷，0表示沒有
		Statement st = null;
		ResultSet rs = null;
		try {
			sql="SELECT A.ID,A.BRACKET,A.MEGID,A.SUSPEND FROM HUR_SMS_SETTING A ORDER BY ID DESC";	
			
			
			st = conn.createStatement();
			logger.debug("Execute SQL:"+sql);
			rs=st.executeQuery(sql);
			
			while(rs.next()){
				times.add(rs.getString("ID"));
				bracket.add(rs.getString("BRACKET"));
				msg.add(rs.getString("MEGID"));
				suspend.add(rs.getString("SUSPEND"));
				logger.info("times:"+times.get(times.size()-1)+",bracket:"+bracket.get(bracket.size()-1)+",msg:"+msg.get(msg.size()-1)+",suspend:"+suspend.get(suspend.size()-1));
			}
		} catch (SQLException e) {
			ErrorHandle("At sendAlertSMS:Load SMSSetting occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		if(times.size()==0){
			sql="";
			ErrorHandle("Can't found SMS Setting!");
			return false;
		}else{
			return true;
		}
		
	}*/
	/**
	 * 設定簡訊設定
	 * Map Key priceplanID，Value: ID,BRACKET,MEGID,SUSPEND,PRICEPLANID< List>
	 */
	static Map<String,Map<String,List<String>>> smsSettingMap = new HashMap<String,Map<String,List<String>>>();
	
	private Boolean getSMSsetting(){
		logger.info("getSMSsetting...");
		subStartTime = System.currentTimeMillis();
		smsSettingMap.clear();
		Statement st = null;
		ResultSet rs = null;
		try {
			sql =
					"SELECT A.ID,A.BRACKET,A.MEGID,A.SUSPEND,A.PRICEPLANID "
					+ "FROM HUR_SMS_SETTING A "
					+ "ORDER BY PRICEPLANID,ID DESC";
			
			st = conn.createStatement();
			logger.debug("Query SMSSetting SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				String pId=rs.getString("PRICEPLANID");
				if(pId == null){
					pId = "0";
				}
				
				for(String id : pId.split(",")){
					Map<String,List<String>> map = new HashMap<String,List<String>>();
					
					List<String> l1=new ArrayList<String>(); //ID
					List<String> l2=new ArrayList<String>();
					List<String> l3=new ArrayList<String>();
					List<String> l4=new ArrayList<String>();
					if(smsSettingMap.containsKey(id)){
						map=smsSettingMap.get(id);
						if(map.containsKey("ID")) 
							l1=map.get("ID");
						if(map.containsKey("BRACKET")) 
							l2=map.get("BRACKET");
						if(map.containsKey("MEGID")) 
							l3=map.get("MEGID");
						if(map.containsKey("SUSPEND")) 
							l4=map.get("SUSPEND");
					}
					
					l1.add(rs.getString("ID"));
					l2.add(rs.getString("BRACKET"));
					l3.add(rs.getString("MEGID"));
					l4.add(rs.getString("SUSPEND"));
					
					map.put("ID", l1);
					map.put("BRACKET", l2);
					map.put("MEGID", l3);
					map.put("SUSPEND", l4);
					smsSettingMap.put(id, map);
				}	
			}
		} catch (SQLException e) {
			ErrorHandle("At setTADIGtoMCCMNC occur SQLException error", e);
			return false;
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		if(smsSettingMap.size()==0){
			sql="";
			ErrorHandle("Can't found SMS Setting!");
			return false;
		}else{
			return true;
		}
		
	}

	static Map<String,Map<String,String>> content=new HashMap<String,Map<String,String>>();
	public Boolean getSMSContents(){
		logger.info("getSMSContents...");
		subStartTime = System.currentTimeMillis();
		content.clear();
		//載入簡訊內容
		Statement st = null;
		ResultSet rs = null;
		try {
			sql=
					"SELECT A.ID,A.CONTENT,A.CHARSET "
					+ "FROM HUR_SMS_CONTENT A "
					+ "WHERE A.START_DATE<= '"+sYearmonthday+"' AND (A.END_DATE IS NULL OR A.END_DATE>'"+sYearmonthday+"') "
					+ "order by A.ID ";	

			st = conn.createStatement();
			logger.debug("Execute SQL:"+sql); 
			rs=st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("CONTENT", rs.getString("CONTENT"));
				map.put("CHARSET", rs.getString("CHARSET"));
				content.put(rs.getString("ID"), map);
			}

		} catch (SQLException e) {
			ErrorHandle("At sendAlertSMS:Load SMSContent occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At sendAlertSMS:Load SMSContent occur Exception error", e);
		} finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		if(content.size()==0){
			ErrorHandle("Can't found SMS Content sentting!");
			return false;
		}else{
			return true;
		}
	}
	public static void ErrorHandle(String cont){
		ErrorHandle(cont,null);
	}
	public static void ErrorHandle(String cont,Exception e){
		if(e!=null){
			logger.error(cont, e);
			
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			//send mail
			errorMsg=s.toString();
		}else{
			logger.error(cont);
			errorMsg="";
		}
		
		sendErrorMail(cont);
	}
	public void ckeckMonthAlert(){
		logger.info("ckeckMonthAlert...");
		//開始檢查是否發送警示簡訊
		//月金額警示*************************************
		//沒有當月資料，不檢查
		if(currentMap.containsKey(sYearmonth)){

			int smsCount=0;
	
			//取出此月資料
			for(String serviceid: currentMap.get(sYearmonth).keySet()){
				
				//確認資料
				if(!msisdnMap.containsKey(serviceid)){
					ErrorHandle("MsisdnMap without serviceid:"+serviceid+"'s data!");
					continue;
				}
				
				//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
				String phone =(String) getMSISDN(serviceid);

				if(phone==null ||"".equals(phone)){
					sql="";
					ErrorHandle("At ckeckMonthAlert sendAlertSMS occur error! The serviceid:"+serviceid+" can't find msisdn to send !");
					continue;
				}
				
				//取得PricePlanID
				String priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID");
				if(priceplanid==null ||"".equals(priceplanid)){
					sql="";
					ErrorHandle("At ckeckMonthAlert sendAlertSMS occur error! The serviceid:"+serviceid+" can't find priceplanid!");
					continue;
				}
				
				//取得警示設定
				if(smsSettingMap.get(priceplanid)==null){
					sql="";
					ErrorHandle("At ckeckMonthAlert sendAlertSMS occur error! Can't find priceplanid="+priceplanid+" setting in smsSetting!");
					continue;
				}
		
				List<String> ids = smsSettingMap.get(priceplanid).get("ID");
				List<String> brackets = smsSettingMap.get(priceplanid).get("BRACKET");
				List<String> msgids = smsSettingMap.get(priceplanid).get("MEGID");
				List<String> suspends = smsSettingMap.get(priceplanid).get("SUSPEND");
	
				
				//目前累計費用
				Double charge=parseDouble((String) currentMap.get(sYearmonth).get(serviceid).get("CHARGE"));
				//與舊費用差，運用在預估推測
				Double differenceCharge=charge-(oldChargeMap.get(serviceid)==null?0d:oldChargeMap.get(serviceid));
				
				int smsTimes=parseInt((String) currentMap.get(sYearmonth).get(serviceid).get("SMS_TIMES"));
				//String everSuspend =(String) currentMap.get(sYearmonth).get(serviceid).get("EVER_SUSPEND");
				Double lastAlernThreshold=parseDouble((String) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_THRESHOLD"));
			
				String[] contentid=null;
				Double DEFAULT_THRESHOLD = null;
				String param = null;
				//抓取不同幣別月上限
				if("NTD".equals(pricePlanIdtoCurrency.get(priceplanid))) {
					param = "NTD_MONTH_LIMIT";
				}else if("HKD".equals(pricePlanIdtoCurrency.get(priceplanid))) {
					param = "HKD_MONTH_LIMIT";
				}
				
				//取不到任何上限值 跳過
				if(param == null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get Month Limit parameter! ");
					continue;
				}
				
				DEFAULT_THRESHOLD = getSystemConfigDoubleParam(priceplanid,param);
				
				//取不到任何上限值 跳過
				if(DEFAULT_THRESHOLD == null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get Month Limit! ");
					continue;
				}
				
				
				//20141118 修改 約定客戶訂為每5000提醒一次不斷網
				Double threshold=null;
				
				//20160112針對非139客戶走VIP不斷網的方式
				if("139".equals(priceplanid)){
					threshold = thresholdMap.get(serviceid)==null?null:parseDouble(thresholdMap.get(serviceid));
				}else{
					threshold = 0D;
				}
				
				

				//判斷客戶是不是VIP
				boolean isCustomized=false;
				//目前不設計自訂上限，皆為0，取有表示客戶為VIP，取無則是非VIP
				if(threshold==null){
					threshold=DEFAULT_THRESHOLD;
				}else{
					isCustomized=true;
				}
				
				if(lastAlernThreshold==null)
					lastAlernThreshold=0D;

				boolean sendSMS=false;
				boolean needSuspend=false;
				Double alertBracket=0D;
				
				int msgSettingID=0;
				
				//20141118 修改 約定客戶訂為每5000提醒一次不斷網，規則客制訂為0進行5000持續累積
				if(threshold!=0D){
					//20151230 add 如果使用者此次只在華人上網包區域使用，不去判斷他的警示
					//
					/*if(addonMark.contains(serviceid))
						continue;*/
					
					//檢查月用量，從最大值上限開始向下檢查
					for(;msgSettingID<ids.size();msgSettingID++){
						Double bracket = parseDouble((String) brackets.get(msgSettingID));
						//費用>=最大警示*警示門檻   and 最後警示量<最大警示*警示門檻
						if(((charge>=bracket*threshold))&&lastAlernThreshold<bracket*threshold){
							sendSMS=true;
							alertBracket=bracket*threshold;
							contentid=((String)msgids.get(msgSettingID)).split(",");

							if("1".equals((String)suspends.get(msgSettingID))){
								needSuspend=true;
							}
							break;
						}
					}	
					
					
					//檢查預測用量，如果之前判斷不用發簡訊，或不是發最上限簡訊
					//20151201 為了重新開通數據用戶，不檢查是否發過，只要滿足最大上限就發
					//20151218避免有設定上限VIP 依照60、80、100上限通知但不會斷網，會在預估中重複警告
					//所以預估排除VIP將可避免這個問題
					if((!sendSMS||(sendSMS && msgSettingID!=0))&&!isCustomized){
						Double bracket = parseDouble((String) brackets.get(0));
						//判斷包含預估是否超過最大限度
						if(charge+differenceCharge>=bracket*threshold){
							//20151201 add
							String gprsStatus = Query_GPRSStatus(phone);
							//已超過最大限度則繼續檢查GPRS狀態，如為開啟狀態則再次進行關閉
							if(gprsStatus!=null && !"".equals(gprsStatus) && !"0".equals(gprsStatus)){
							//if(charge+differenceCharge>=bracket*threshold&&lastAlernThreshold<bracket*threshold){
								logger.info("For "+serviceid+" add charge "+differenceCharge+" in this hour ,System forecast the next hour will over charge limit");
								sendSMS=true;
								msgSettingID=0;
								
								alertBracket=bracket*threshold;
							
								contentid=((String)msgids.get(0)).split(",");
								
								if("1".equals((String)suspends.get(0))){
									needSuspend=true;
								}
							}
						}
					}
				}else{
					//以5000的整數進行警示
					int temp=(int) ((int)(charge/DEFAULT_THRESHOLD)*DEFAULT_THRESHOLD);
					
					if(temp>lastAlernThreshold){
						alertBracket=(double) temp;
						sendSMS=true;
						//如果為VIP客戶預設發3號簡訊
						contentid=new String[]{"3"};
						
						
						contentid = null;
						String vipSMS = null;
						//抓取不同幣別VIP簡訊
						if("NTD".equals(pricePlanIdtoCurrency.get(priceplanid))){
							vipSMS = getSystemConfigParam(priceplanid,"NTD_VIP_MSG_ID");
						}
						if("HKD".equals(pricePlanIdtoCurrency.get(priceplanid))){
							vipSMS = getSystemConfigParam(priceplanid,"HKD_VIP_MSG_ID");
						}
						
						//取不到任何上限值 跳過
						if(vipSMS == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VIP SMS number! ");
							continue;
						}
						contentid = vipSMS.split(",");
						
					}
				}				
				//20151001 cancel if have been suspend then not do
				//if(needSuspend &&"0".equals(everSuspend)&&!isCustomized){
				if(needSuspend &&!isCustomized){
					doSuspend(serviceid,phone);	
				}
				
				
				if(sendSMS){
					String currency = "";
					
					currency = pricePlanIdtoCurrency.get(priceplanid);
					if(currency== null){
						ErrorHandle("The PRICEPLANID:"+priceplanid+"find currency="+currency);
						continue;
					}
					
					//smsCount += sendSMS(serviceid,contentid,alertBracket,phone,currency);
					smsCount += sendSMS(serviceid,contentid,phone,
							param,
							new String[]{"{{bracket}}","{{customerService}}","{{month}}"},
							new String[]{currency+FormatNumString(alertBracket,"#,##0.00").toString(),
									queryCustomerServicePhone(serviceid,false),
									sYearmonth.substring(4,6)});
					currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_THRESHOLD", alertBracket.toString());				
					currentMap.get(sYearmonth).get(serviceid).put("SMS_TIMES", String.valueOf((smsTimes+1)));
					
					//20150629 add
					Set<String> set2 = new HashSet<String>();

					if(updateMap.containsKey(sYearmonth)){	
						set2 = updateMap.get(sYearmonth);
					}
					set2.add(serviceid);
					updateMap.put(sYearmonth, set2);
				}
				
			}
			logger.debug("Total send month alert "+smsCount+" ...");
			logger.debug("Log to table...executeBatch");
		}
	}
	public Double parseDouble(String value){
		
		return value==null?null:Double.parseDouble(value);
	}
	public Integer parseInt(String value){
		
		return value==null?null:Integer.parseInt(value);
	}
	//new Version
	
	public Double getSystemConfigDoubleParam(String pricePlanid,String paramName){
		//System.out.println("New Version check Point 4.");
		String result = getSystemConfigParam(pricePlanid,paramName);
		return (result!=null? parseDouble(result):null);
	}
	//new version end
	
	//new Version
	public String getSystemConfigParam(String pricePlanid,String paramName){
		//System.out.println("New Version check Point 5.");
		String result = null;
		
		if(systemConfig.get(pricePlanid)!=null)
			result = (String) systemConfig.get(pricePlanid).get(paramName);
			 
		if(result == null)
			result = (String) systemConfig.get("0").get(paramName);
		
		return result;
	}
	//new version end
	
	public void doSuspend(String serviceid,String phone) {
		//中斷GPRS服務
		//20141113 新增客制定上限不執行斷網
		//20150529 將中斷的部分從發送簡訊中獨立出來
		
		String imsi = msisdnMap.get(serviceid).get("IMSI");
		if(imsi==null || "".equals(imsi))
			imsi = msisdnMap.get(serviceid).get("IMSI");
		
		if(imsi==null || "".equals(imsi)){
			logger.debug("Suspend GPRS fail because without imsi for serviceid is "+serviceid);
			return;
		}
		
		logger.debug("Suspend GPRS ... ");		
		PreparedStatement pst = null;
		try {
			suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
			Map<String,String> orderNBR = sus.doChangeGPRSStatus(0,imsi,phone,"0","CHT-GPRS");
			serviceOrderNBR.add(orderNBR);
			currentMap.get(sYearmonth).get(serviceid).put("EVER_SUSPEND", "1");

			sql=
					"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
					+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS,PROCESS_DAY) "
					+ "VALUES(?,?,SYSDATE,?,?,'"+sYearmonthday+"' )";
			
			pst=conn.prepareStatement(sql);
			pst.setString(1,orderNBR.get("cServiceOrderNBR") );
			pst.setString(2,imsi );
			pst.setString(3,phone );
			pst.setString(4,"0" );
			logger.info("Execute SQL : "+sql);
			pst.executeUpdate();
		} catch (SQLException e) {
			ErrorHandle("At doSuspend got SQLException",e);
		} catch (Exception e) {
			ErrorHandle("At doSuspend got Exception",e);
		}finally{
			try {
				if(pst!=null)
					pst.close();
			} catch (SQLException e) {}
		}
		
	}
	
	
	
	public void doResume(String serviceid,String phone) {
		//中斷GPRS服務
		//20141113 新增客制定上限不執行斷網
		//20150529 將中斷的部分從發送簡訊中獨立出來

		
		String imsi = msisdnMap.get(serviceid).get("IMSI");
		if(imsi==null || "".equals(imsi))
			imsi = msisdnMap.get(serviceid).get("IMSI");
		
		if(imsi==null || "".equals(imsi)){
			logger.debug("Resume GPRS fail because without imsi for serviceid is "+serviceid);
			return;
		}

		logger.debug("Resume GPRS ... ");		
		PreparedStatement pst = null;
		try {
			suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
			Map<String,String> orderNBR = sus.doChangeGPRSStatus(0,imsi,phone,"1","CHT-GPRS");
			serviceOrderNBR.add(orderNBR);
			
			sql=
					"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
					+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS,PROCESS_DAY) "
					+ "VALUES(?,?,SYSDATE,?,?,'"+sYearmonthday+"')";
			
			pst=conn.prepareStatement(sql);
			pst.setString(1,orderNBR.get("cServiceOrderNBR") );
			pst.setString(2,imsi );
			pst.setString(3,phone );
			pst.setString(4,"1" );
			
			logger.info("Execute SQL : "+sql);
			pst.executeUpdate();
		} catch (SQLException e) {
			ErrorHandle("At doResume got SQLException",e);
		} catch (Exception e) {
			ErrorHandle("At doResume got Exception",e);
		}finally{
			try {
				if(pst!=null)
					pst.close();
			} catch (SQLException e) {}
		}
	}
	
	

	/**
	 * 對因5000塊斷網客戶，進行復網，排除VIP，排除主動提出斷網需求客戶
	 */
	public void checkMonthlyResume(){
		logger.info("checkMonthlyResume...");
		String resumeReport="";
		List<Map<String,String>> beenSuspended = new ArrayList<Map<String,String>>();
		Map<String,String> CHTSuspended = new HashMap<String,String>();
		
		Statement st = null;
		ResultSet rs = null;

		try {
			
			//撈出上個月被斷網的客戶	
			String sql ="SELECT A.SERVICEID ,B.SERVICECODE,C.CREATE_DATE "
					+ "FROM HUR_CURRENT A,SERVICE B ,"
					+ "		(	select msisdn,max(create_date) create_date "
					+ "			from HUR_SUSPEND_GPRS_LOG "
					+ "			where  TO_CHAR(CREATE_DATE,'yyyyMM')= '"+sYearmonth2+"' group by msisdn )C "
					+ "WHERE A.SERVICEID=B.SERVICEID AND B.SERVICECODE = C.MSISDN(+) "
					+ "AND TO_CHAR(C.CREATE_DATE,'yyyyMM')= '"+sYearmonth2+"' "
					+ "AND A.MONTH='"+sYearmonth2+"'  AND A.EVER_SUSPEND = 1 ";
			
			/*String sql = "SELECT A.SERVICEID "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH='"+sYearmonth2+"'  AND A.EVER_SUSPEND = 1 ";*/
			
			
			st = conn.createStatement();
			logger.info("Query Customers had been suspended last month:"+sql);
			rs = st.executeQuery(sql);
			
			resumeReport += "Customers had been suspended last month:";
			while(rs.next()){
				Map<String,String> m = new HashMap<String,String>();
				m.put("serviceid", rs.getString("SERVICEID"));
				m.put("msisdn", rs.getString("SERVICECODE"));
				m.put("date", rs.getString("CREATE_DATE"));
				beenSuspended.add(m);
				resumeReport += rs.getString("SERVICEID") + ",";
			}
			
			resumeReport = resumeReport.substring(0,resumeReport.length()-1)+"\n";
			logger.info(resumeReport);
			rs.close();
			
			//取得上個月中華最後一筆供裝17的資料，是0
			sql = "SELECT B.IMSI,B.REQTIME "
					+ "FROM( "
					+ "		SELECT A.IMSI,MAX(A.REQTIME) REQTIME "
					+ "		FROM(	SELECT SUBSTR(CONTENT,INSTR(CONTENT, 'S2T_IMSI=')+LENGTH('S2T_IMSI='),INSTR(CONTENT, 'Req_Status')-INSTR(CONTENT, 'S2T_IMSI')-LENGTH('S2T_IMSI=')-1) IMSI "
					+ "						,REQTIME,LOGID "
					+ "				FROM PROVLOG "
					+ "				WHERE CONTENT LIKE '%Req_Status=17%' AND TO_CHAR(REQTIME,'yyyyMM') = '"+sYearmonth2+"'"
					+ "			) A "
					+ "		GROUP BY A.IMSI "
					+ "		) B ,("
					+ "		SELECT A.IMSI,A.REQTIME,CONTENT "
					+ "		FROM(   SELECT SUBSTR(CONTENT,INSTR(CONTENT, 'S2T_IMSI=')+LENGTH('S2T_IMSI='),INSTR(CONTENT, 'Req_Status')-INSTR(CONTENT, 'S2T_IMSI')-LENGTH('S2T_IMSI=')-1) IMSI,CONTENT "
					+ " 	               ,REQTIME,LOGID "
					+ "				FROM PROVLOG "
					+ "				WHERE CONTENT LIKE '%Req_Status=17%' AND TO_CHAR(REQTIME,'yyyyMM') = '"+sYearmonth2+"' AND  CONTENT LIKE '%GPRS_Status=0%' "
					+ "			) A "
					+ "		)C "
					+ "where B.IMSI = C. IMSI AND B.REQTIME = C.REQTIME ";
			
			logger.info("Query TWNLD request is 17 and status is 0 in last month:"+sql);
			rs = st.executeQuery(sql);
			while(rs.next()){
				//CHTSuspended.add(rs.getString("IMSI"));
				CHTSuspended.put(rs.getString("IMSI"), rs.getString("REQTIME"));
			}
		} catch (SQLException e) {
			ErrorHandle("At checkMonthlyResume got SQLException",e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		
		for(Map<String,String> m: beenSuspended){
			String serviceid = m.get("serviceid");
			String s2tmsisdn = getMSISDN(serviceid);
			String imsi = msisdnMap.get(serviceid).get("IMSI");
			
			
			
			if(s2tmsisdn == null){
				ErrorHandle("For service id = "+serviceid+" can't find msisdn!");
				continue;
			}
			if(imsi == null){
				ErrorHandle("For service id = "+serviceid+" can't find imsi!");
				continue;
			}
			//如果有中華供裝指定要關閉網路，則跳過
			if(CHTSuspended.keySet().contains(imsi)){
				logger.info(s2tmsisdn+" had required to suspend at "+CHTSuspended.get(imsi)+"!");
				resumeReport+= s2tmsisdn+" had been disable GPRS by CHT at "+CHTSuspended.get(imsi)+"!\n<br>";
				continue;
			}
			String gprsSatatus = Query_GPRSStatus(s2tmsisdn);
			//如果是已斷網狀態才進行復網
			if("0".equals(gprsSatatus)){
				logger.info("resume "+s2tmsisdn+"(suspended at "+m.get("date")+") GPRS");
				resumeReport+= s2tmsisdn+"(suspended at "+m.get("date")+") GPRS is resumed\n<br>";
				doResume(serviceid,s2tmsisdn);
			}else{
				logger.info(s2tmsisdn+"(suspended at "+m.get("date")+") GPRS is active");
				resumeReport+= s2tmsisdn+"(suspended at "+m.get("date")+") GPRS is active\n<br>";
			}
				
		}
		//mailReceiver=props.getProperty("mail.Receiver");
		logger.info("Send resumeReport result.");
		String mailReceiver = "k1988242001@gmail.com";
		if(!TEST_MODE){
			mailReceiver+=",Yvonne.lin@sim2travel.com";
		}
		sendMail("DVRS Resume GPRS Report.",resumeReport,"DVRS Report",mailReceiver);
		resume = false;
		
	}
	public int sendSMS(String serviceid,String[] contentid,String phone,
			String[] paramName,String[] paramValue){
		return sendSMS(serviceid, contentid, phone, paramName, paramValue, null,null);
	}
	public int sendSMS(String serviceid,String[] contentid,String phone,String alertParam,
			String[] paramName,String[] paramValue){
		return sendSMS(serviceid, contentid, phone, paramName, paramValue, null,alertParam);
	}
	public int sendSMS(String serviceid,String[] contentid,String phone,
			String[] paramName,String[] paramValue,String type){
		return sendSMS(serviceid, contentid, phone, paramName, paramValue, type,null);
	}
	
	public int sendSMS(String serviceid,String[] contentid,String phone,
			String[] paramName,String[] paramValue,String type,String alertParam){
	//public int sendSMS(String serviceid,String[] contentid,Double alertBracket,String phone,String currency){
		Statement st =null;
		ResultSet rs = null;
		int smsCount=0;
		String res;
		
		String oPhone = null;
		/*// TODO 測試用，發至特定手機
		if(type!=null && type.equalsIgnoreCase("DATA")) {
			oPhone = phone;
			phone = "886989235253";
		}*/

		try {
			st = conn.createStatement();
			for(String s:contentid){
				if(s!=null){
					//寄送簡訊
					
					if(content.get(s)==null){
						throw new Exception("Can't find sms content id:"+s);
					}
					if(content.get(s).get("CONTENT")==null){
						throw new Exception("id:"+s+" sms content is null.");
					}
					String cont = content.get(s).get("CONTENT");

					cont = new String(cont.getBytes("ISO8859-1"),"big5");
					
					cont = processMag(cont,paramName,paramValue);
					
					//WSDL方式呼叫 WebServer
					//result=tool.callWSDLServer(setSMSXmlParam(cont,phone));
					String nMccmnc=searchMccmncBySERVICEID(serviceid);
					//如果判斷客戶在印尼，則分段簡訊進行發送
					if(nMccmnc!=null&&"510".equals(nMccmnc.substring(0,3))){
						/*int number = 68;
						
						int length = cont.length();
						byte[] b =cont.getBytes();
						length = b.length;
						int msgN = length/number;
						if(length%number>0)
							msgN += 1;
						String [] sub =new String[msgN];
						
						for(int j=0;j<msgN;j++){
							int last = (j+1)*number;
							if(last>length)
								last=length;
								
							byte [] c = new byte[number];
							System.arraycopy(b, j*number , c, 0, last-j*number);
							sub[j]=new String(c);
							sub[j]=cont.substring(j*number,last);
						}*/

						String [] sub =cont.split("\\{\\{bp\\}\\}");
						String sRes="";
						for(String sCont : sub){
							sRes += setSMSPostParam(new String(sCont.getBytes("big5"),"ISO8859-1"),phone);
							Thread.sleep(3000);
						}
						res = sRes.substring(0,sRes.length()-1);
					}else{
						cont =cont.replaceAll("\\{\\{bp\\}\\}","");
						res=setSMSPostParam(new String(cont.getBytes("big5"),"ISO8859-1"),phone);
					}
					logger.debug("send message result : "+res);		

					//假如需要回寫正確的電話
					if(oPhone!=null)
						phone = oPhone;
					
					smsCount++;
					sql="INSERT INTO HUR_SMS_LOG"
							+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE"+(type!=null?",type":"")+") "
							+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+new String(cont.getBytes("big5"),"ISO8859-1")+"',TO_DATE('"+sdf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE"+(type!=null?",'"+type+"'":"")+")";
					//寫入資料庫
					logger.debug("execute SQL : "+sql);
					st.addBatch(sql);
					
					if(alertParam!=null) {
						//20180528 add
						sendMail("DVRS "+alertParam+" "+sdf.format(new Date()), "To "+phone+"\n\n"+cont, "DVRS_ALERT", AlertReceiver);
					}
				}
			}
			st.executeBatch();
		} catch (SQLException e) {
			ErrorHandle("At send alert SMS occur SQLException error!", e);
		} catch (UnsupportedEncodingException e) {
			sql="";
			ErrorHandle("At send alert SMS occur UnsupportedEncodingException error!", e);
		} catch (IOException e) {
			sql="";
			ErrorHandle("At send alert SMS occur IOException error!", e);
		} catch (InterruptedException e) {
			sql="";
			ErrorHandle("At send alert SMS occur InterruptedException error!", e);
		} catch (Exception e) {
			sql="";
			ErrorHandle("At send alert SMS occur Exception error!", e);
		}finally {
			try {
				if(st!=null) {
					st.close();
				}
			} catch (SQLException e) {}
			
			try {
				if(rs!=null) {
					rs.close();
				}
			} catch (SQLException e) {}
		}
		return smsCount;
	}
	
	

	
	public void checkDailyAlert(){
		logger.info("checkDailyAlert...");
		//取出今日資料
		if(currentDayMap.containsKey(sYearmonthday)){
			int smsCount=0;

			for(String serviceid:currentDayMap.get(sYearmonthday).keySet()){

				//20141216 add 斷網過後，不發送每日簡訊，避免預估斷網後，每日帶出實際累計引發爭議
				if(currentMap.containsKey(sYearmonth) && currentMap.get(sYearmonth).containsKey(serviceid)){
					String everSuspend =(String) currentMap.get(sYearmonth).get(serviceid).get("EVER_SUSPEND");
					if("1".equals(everSuspend)){
						continue;
					}
				}
				
				//確認資料
				if(!msisdnMap.containsKey(serviceid)){
					ErrorHandle("MsisdnMap without serviceid:"+serviceid+"'s data!");
					continue;
				}
				
				
				//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
	
				String phone = (String) getMSISDN(serviceid);
				if(phone==null ||"".equals(phone)){
					sql="";
					ErrorHandle("At checkDailyAlert sendAlertSMS occur error! The serviceid:"+serviceid+" can't find msisdn to send! ");
					continue;
				}
				
				//取得PricePlanId
				String pricePlanID = msisdnMap.get(serviceid).get("PRICEPLANID");
				if(pricePlanID==null ||"".equals(pricePlanID)){
					sql="";
					ErrorHandle("At checkDailyAlert sendAlertSMS occur error! The ServiceID:"+serviceid+" can't find pricePlanID!");
					continue;
				}

				Double daycharge=0D;
				String alerted ="0";
				Double DEFAULT_DAY_THRESHOLD = null;
				String[]  contentid = null;
				String currency = "";
				
				String param = null;
				String contentidParam = null;
				String contentids = null;
				
				//抓取不同幣別日上限
				if("NTD".equals(pricePlanIdtoCurrency.get(pricePlanID))){
					param = "NTD_DAY_LIMIT";
					contentidParam = "NTD_DAY_LIMIT_MSG_ID";
					currency = "NTD";
				}
				if("HKD".equals(pricePlanIdtoCurrency.get(pricePlanID))){
					param = "HKD_DAY_LIMIT";
					contentidParam = "HKD_DAY_LIMIT_MSG_ID";
					currency = "HKD";
				}
				
				//取不到每日上限參數 跳過
				if(param== null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricePlanID+" cannot get Daily Limit param! ");
					continue;
				}
				DEFAULT_DAY_THRESHOLD = getSystemConfigDoubleParam(pricePlanID,param);
				
				
				//取不到每日上限 跳過
				if(DEFAULT_DAY_THRESHOLD== null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricePlanID+" cannot get Daily Limit! ");
					continue;
				}
				contentids = getSystemConfigParam(pricePlanID,contentidParam);

				//取不到每日上限 簡訊內容跳過
				if(contentids == null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricePlanID+" cannot get Daily Limit SMS content! ");
					continue;
				}

				contentid = contentids.split(",");
				
				//一日累計
				for(String nccNet : currentDayMap.get(sYearmonthday).get(serviceid).keySet()){
					daycharge=daycharge+parseDouble((String) currentDayMap.get(sYearmonthday).get(serviceid).get(nccNet).get("CHARGE"));
					String a=(String) currentDayMap.get(sYearmonthday).get(serviceid).get(nccNet).get("ALERT");
					if("1".equals(a)) alerted="1";
				}

				//確認是否達到每日上限
				if(daycharge>=DEFAULT_DAY_THRESHOLD && "0".equalsIgnoreCase(alerted)){
					//為了每日警示內容中帶入月累計費用才需要
					Double charge=parseDouble((String) currentMap.get(sYearmonth).get(serviceid).get("CHARGE"));
					
					//smsCount+=sendSMS(serviceid,contentid,charge,phone,pricePlanIdtoCurrency.get(pricePlanID));	
					smsCount+=sendSMS(serviceid,contentid,phone,param,
							new String[]{"{{bracket}}","{{customerService}}","{{month}}"},
							new String[]{currency+FormatNumString(charge,"#,##0.00"),
									queryCustomerServicePhone(serviceid,false),
									sYearmonth.substring(4,6)});
					//回寫註記，因為有區分Mccmnc，全部紀錄避免之後取不到
					for(String nccNet : currentDayMap.get(sYearmonthday).get(serviceid).keySet()){
						currentDayMap.get(sYearmonthday).get(serviceid).get(nccNet).put("ALERT", "1");
						
						//20150505 add
						Map <String,Set<String>> map6 = new HashMap<String,Set<String>>();
						Set<String> set1 = new HashSet<String>();					

						if(updateMapD.containsKey(sYearmonthday)){	
							map6 = updateMapD.get(sYearmonthday);
							if(map6.containsKey(serviceid)){
								set1 = map6.get(serviceid);
							}
						}
						set1.add(nccNet);
						map6.put(serviceid, set1);
						updateMapD.put(sYearmonthday, map6);
					}
				}		
			}
			logger.debug("Total send day alert "+smsCount+" ...");
			logger.debug("Log to table...executeBatch");
		}
	}
	
	//20170414 add processor   false:S2T true:CHT
	public String queryCustomerServicePhone(String serviceid,Boolean processor){
		//查詢所在國家的客服電話
		String cPhone = null;
		String nMccmnc=searchMccmncBySERVICEID(serviceid);
		Map<String,String> map=null;
		
		if(nMccmnc!=null && !"".equals(nMccmnc))
			map = codeMap.get(nMccmnc.substring(0,3));
		if(map!=null){	
			cPhone=processor?map.get("CHT_PHONE"):map.get("S2T_PHONE");
		}
			
		
		if(cPhone == null)
			cPhone = "";
		
		return cPhone;
	}
	
	
	static boolean NTTalerted = false;
	static boolean NTT75alerted = false;
	//20160701 restart by other case
	public void checkNTTVolumeAlert(){
		
		if(NTTalerted)
			return;
		
		//NTT
		logger.info("checkNTTVolumeAlert...");
		subStartTime = System.currentTimeMillis();
		double volume = 0L;
		double limit = 0;
		double limit2 = 0;
		
		limit = getSystemConfigDoubleParam("0", "NTT_ALERT_LIMIT");
		limit2 = FormatDouble(limit*0.75, "0.00");
		
		if(limit ==0){
			ErrorHandle("Can't get NTT volume limit!");
			return;
		}
			
		logger.info("get limit 75%/100%("+limit2+"/"+limit+")");
		
		if(currentMap.containsKey(sYearmonth)){
			//檢查這個月的資料作警示通知
			for(String serviceid: currentMap.get(sYearmonth).keySet()){
				String priceplanid = null;
				String subsidiaryid = null;
				if(msisdnMap.containsKey(serviceid)){
					priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID");
					subsidiaryid = msisdnMap.get(serviceid).get("SUBSIDIARYID");
				}
				if("160".equals(priceplanid) && "72".equals(subsidiaryid)){
					if("160".equals(priceplanid) && "72".equals(subsidiaryid)){
						volume=volume+parseDouble((String) currentMap.get(sYearmonth).get(serviceid).get("VOLUME"));
					}
				}
			}
		}
		
		
		volume = volume/1024/1024/1024; //變成GB
		
		boolean sendMail = false ; 
		String mail_subject="";
		String mail_content="";
		String mail_sender="HKNet@sim2travel.com";
		String mail_receiver=HKNetReceiver;
		if(TEST_MODE){
			mail_receiver = "k1988242001@gmail.com.tw,ranger.kao@sim2travel.com";
		}
		
		
		if(limit<volume){
			sendMail=true;
			NTTalerted = true;
			mail_subject = "Notice-"+sYearmonth+": total pooled usage in HK reaches 100%";
			mail_content=""
					+ "Dear Sir,\n\n\n"
					+ "Please be informed that the total pooled usage of HKNet Oneness SIM subscribers ("+limit+") has reached 100% ("+limit+"GB) of "+limit+"GB: "+FormatDouble(volume, "0.00")+"GB.\n\n"
					+ "Thank you for your attention and please let me know if any queries.\n\n"
					+ "Sim2travel.\n";
		}else if(limit2<volume&&!NTT75alerted){
			sendMail=true;
			NTT75alerted = true;
			mail_subject = "Notice-"+sYearmonth+": total pooled usage in HK reaches 75%";
			mail_content=""
					+ "Dear Sir,\n\n\n"
					+ "Please be informed that the total pooled usage of HKNet Oneness SIM subscribers ("+limit+") has reached 75% ("+limit2+"GB) of "+limit+"GB: "+FormatDouble(volume, "0.00")+"GB.\n\n"
					+ "Thank you for your attention and please let me know if any queries.\n\n"
					+ "Sim2travel.\n";
		}
		
		if(sendMail){

			Statement st = null;
			
			try {
				
				sql="INSERT INTO HUR_SMS_LOG"
						+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
						+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+mail_receiver+"','"+new String(mail_content.getBytes("BIG5"),"ISO8859-1")+"',TO_DATE('"+sdf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'success',SYSDATE)";
				//寫入資料庫
				
				sendMail(mail_subject,mail_content,mail_sender ,mail_receiver );
				
				st = conn.createStatement();
				logger.debug("execute SQL : "+sql); 
				st.executeUpdate(sql);
				
				if(NTTalerted){
					updateSystemConfig("NTT_ALERTED","100");
				}else if(NTT75alerted){
					updateSystemConfig("NTT_ALERTED","75");
				}
				
			} catch (SQLException e) {
				ErrorHandle("At checkNTTVolumeAlert occur SQLException!");
			} catch (UnsupportedEncodingException e) {
				ErrorHandle("At checkNTTVolumeAlert occur UnsupportedEncodingException!");
			} catch (Exception e) {
				ErrorHandle("At checkNTTVolumeAlert occur Exception!");
			}finally{
				if(st!=null){
					try {
						st.close();
					} catch (SQLException e) {

					}
				}
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	List<Map<String,String>> updateVolumePocketMap = new ArrayList<Map<String,String>>();

	public void checkVolumePocket(){
		logger.info("checkVolumePocket...");
		subStartTime = System.currentTimeMillis();
		int smsCount = 0;
		
		//從流量包Table取得資料
		for(String serviceid:volumePocketMap.keySet()){
			String pricplanID = msisdnMap.get(serviceid).get("PRICEPLANID"); 
			String msisdn = getMSISDN(serviceid);
			if(msisdn==null ||"".equals(msisdn)){
				sql="";
				ErrorHandle("At checkVolumePocket, The serviceid:"+serviceid+" can't find msisdn!");
				continue;
			}
			
			for(Map<String,String> m : volumePocketMap.get(serviceid)){
				String pid = (String)m.get("PID");
				int alerted = parseInt((String)m.get("ALERTED"));
				String type = m.get("TYPE");
				
				double v = volumeList.get(pid);
				v = v*kByte;//轉換為由B轉換為KB 
				
				
				
				//Go2Play
				if("3".equals(type)) {
					//4GB方案Qos方案為每月2GB降速，於是每1GB降速，2次後使用戶達到4GB
					if(("183".equals(pricplanID)) && 
							alerted<100 && v>=1048576d*(alerted/50+1) ) {
						//insert Reset Table
						Map<String,String> qosReset = new HashMap<String,String>();
						qosReset.put("IMSI", (String) msisdnMap.get(serviceid).get("IMSI"));
						qosReset.put("MSISDN", msisdn);
						qosReset.put("MCCNET", m.get("MCCNET"));
						qosReset.put("TYPE", "Go2Play");
						insertQosResetList.add(qosReset);
	
						alerted += 50; //0、50、100
						
						//更新Volume已警示部分
						Map<String,String> mm = new HashMap<String,String>();
						mm.put("PID", pid);
						mm.put("ALERTED", ""+alerted);
						updateVolumePocketMap.add(mm);
					}
					continue;
				}else //YunyoBo
					if("4".equals(type)) {
						//4GB方案Qos方案為每月2GB降速，於是每1GB降速，2次後使用戶達到4GB
						if(("179".equals(pricplanID) || "181".equals(pricplanID)|| "187".equals(pricplanID)) && 
								alerted<100 && v>=1048576d*(alerted/50+1) ) {
							//insert Reset Table
							Map<String,String> qosReset = new HashMap<String,String>();
							qosReset.put("IMSI", (String) msisdnMap.get(serviceid).get("IMSI"));
							qosReset.put("MSISDN", msisdn);
							qosReset.put("MCCNET", m.get("MCCNET"));
							qosReset.put("TYPE", "YunYoBo");
							insertQosResetList.add(qosReset);
		
							alerted += 50; //0、50、100
							
							//更新Volume已警示部分
							Map<String,String> mm = new HashMap<String,String>();
							mm.put("PID", pid);
							mm.put("ALERTED", ""+alerted);
							updateVolumePocketMap.add(mm);
						}else //20181019 188 8GB方案 
							if(("188".equals(pricplanID))&& alerted<100 && v>=1048576d*(alerted/17+1)) {
							//insert Reset Table
							Map<String,String> qosReset = new HashMap<String,String>();
							qosReset.put("IMSI", (String) msisdnMap.get(serviceid).get("IMSI"));
							qosReset.put("MSISDN", msisdn);
							qosReset.put("MCCNET", m.get("MCCNET"));
							qosReset.put("TYPE", "YunYoBo");
							insertQosResetList.add(qosReset);
		
							alerted += 17; //0、17、34、51、68、85
							
							//更新Volume已警示部分
							Map<String,String> mm = new HashMap<String,String>();
							mm.put("PID", pid);
							mm.put("ALERTED", ""+alerted);
							updateVolumePocketMap.add(mm);
						}
						continue;
				}
			}
		}		
		logger.info("Total send pocket alert:"+smsCount);
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	List<Map<String,String>> insertQosResetList = new ArrayList<Map<String,String>>();
	
	private void checkDailyQosReset() {
		logger.info("checkDailyQosReset...");
		for( String serviceid : userLocation.keySet()) {
			Map<String,String> m = userLocation.get(serviceid);
			String msisdn = getMSISDN(serviceid);
			String pricePlanid = msisdnMap.get(serviceid).get("PRICEPLANID"); 
			
			if(!qosResetSettingMap.get(0).containsKey(pricePlanid))
				continue;

			for(Map<String,Object> qosResetSetting : qosResetSettingMap.get(0).get(pricePlanid)) {
				String nccNets = (String) qosResetSetting.get("MCCNET");
				if(nccNets.contains(m.get("MCCNET"))) {
					double volume = 0d;
					//累計RESET流量
					for(String mccNet:nccNets.split(",")) {
						if(currentDayMap.get(sYearmonthday).get(serviceid).get(mccNet)!=null) {
							String v = currentDayMap.get(sYearmonthday).get(serviceid).get(mccNet).get("RESET_VOLUME");
							volume += v==null?0:Double.parseDouble(v);
						}
					}
					//超過流量，且未達限制次數
					if((Double)qosResetSetting.get("VOLUME")<=volume) {
						logger.info("USer serviceid:"+serviceid+"'s volume is "+volume+" reached volume limit "+qosResetSetting.get("VOLUME")+".");
						Integer maxTimes = (Integer)qosResetSetting.get("TIMES");
						
						boolean needReset = false;
						//次數null 無限次
						if(maxTimes == null) {
							needReset = true;
						}else {
							int times = 0;
							//統計次數
							if(qosD_ResetLogMap.containsKey(msisdn)) {
								for(String mccNet:nccNets.split(",")) {
									Integer i = qosD_ResetLogMap.get(msisdn).get(mccNet);
									times += i==null? 0 : i;
								}	
							}
							//次數未達上限
							if(maxTimes>times) {
								needReset = true;
							}else {
								logger.info("User serviceid:"+serviceid+" reached max reset times.");
							}
						}				
						
						if(needReset) {
							//進行Reset
							Map<String,String> qosReset = new HashMap<String,String>();
							qosReset.put("IMSI", (String) msisdnMap.get(serviceid).get("IMSI"));
							qosReset.put("MSISDN", msisdn);
							qosReset.put("MCCNET", m.get("MCCNET"));
							qosReset.put("TYPE", "D_RESET");
							insertQosResetList.add(qosReset);
							
							//清空REST累積流量
							Map<String,Map<String,Map<String,String>>> currentDay = currentDayMap.get(sYearmonthday);
							Map<String, Map<String, String>> serviceidMap = currentDay.get(serviceid);
							for(String mccNet:nccNets.split(",")) {
								if(serviceidMap.containsKey(mccNet)) {
									Map<String, String> mccMap = serviceidMap.get(mccNet);
									mccMap.put("RESET_VOLUME", "0");
									serviceidMap.put(mccNet, mccMap);
								}
							}
							currentDay.put(serviceid, serviceidMap);
							currentDayMap.put(sYearmonthday, currentDay);
						}else {
							logger.info("USer serviceid:"+serviceid+" reached max times, no reset.");
						}
					}
				}
			}
		}
	}
	
	
	
	private void queryCCT() {
		logger.info("queryCCT...");
		
		subStartTime = System.currentTimeMillis();
		Statement st = null;
		ResultSet rs = null;
		
		Long singleLimit = Long.valueOf(getSystemConfigParam("0", "CCT_SINGLE_LIMIT"));
		
		if(singleLimit == null ) {
			ErrorHandle("At queryCCT can't get singleLimit");
			return;
		}
		
		Double totalLimit = getSystemConfigDoubleParam("0", "CCT_TOTAL_LIMIT");
		
		if(totalLimit == null ) {
			ErrorHandle("At queryCCT can't get totalLimit");
			return;
		}
 
		sql=
				""
				+ "select distinct a.serviceid,b.servicecode,c.imsi,b.status,sum(a.VOLUME) over (partition by b.serviceid) vo,sum(a.CHARGE) over (partition by b.priceplanid) ch "
				+ "from HUR_CURRENT a ,service b,imsi c "
				+ "where a.serviceid = b.serviceid and b.serviceid = c.serviceid "
				+ "and b.subsidiaryid = 81 ";
		
		
		List<Map<String,String>> suspendList = new ArrayList<Map<String,String>>();
		
		try {
			
			suspendList.clear();
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			Double charge = null;
			while(rs.next()){
				charge = rs.getDouble("ch");
				Long volume = rs.getLong("vo");
				int status = rs.getInt("status");
				
				if(status == 1 && volume >= singleLimit ) {
					
					Map<String,String> m = new HashMap<String,String>();
					m.put("IMSI", rs.getString("imsi"));
					m.put("MSISDN", rs.getString("servicecode"));
					suspendList.add(m);
				}
			}
			
			suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
			
			
			for(Map<String,String> m : suspendList) {
				
				Map<String,String> orderNBR = sus.doSuspend(m.get("IMSI"),m.get("MSISDN") , "985");
				serviceOrderNBR.add(orderNBR);

				sql=
						"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
						+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS,IS_SLOWDOWN,PROCESS_DAY) "
						+ "VALUES('"+orderNBR.get("cServiceOrderNBR") +"','"+m.get("IMSI")+"',SYSDATE,'"+m.get("MSISDN")+"','1','4','"+sYearmonthday+"')";
				
				logger.info("Execute SQL : "+sql);
				st.executeUpdate(sql);
			}
			//台幣轉港幣
			charge = charge*exchangeRate;
			
			if(charge>=totalLimit) {
				ErrorHandle("CCT had exceed over "+totalLimit+" HKD.");
			}else if(charge>=(totalLimit*0.75)) {
				ErrorHandle("CCT had exceed over 0.75 of "+totalLimit+" HKD.");
			}

		} catch (SQLException e) {
			ErrorHandle("At queryCCT occur SQLException error", e);
		} catch (Exception e) {
			ErrorHandle("At queryCCT occur Exception error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	private String getOriginGPRSName(String msisdn){
		
		String result = null;
		
		String sql = 
				"SELECT B.SERVICEID, B.SERVICECODE, C.PDPSUBSID, C.PDPSUBSNAME "
				+ "FROM PARAMETERVALUE A, SERVICE B, GPRSSUBSCRIPTION C "
				+ "WHERE A.PARAMETERVALUEID=3749 AND A.SERVICEID=B.SERVICEID "
				+ "AND B.STATUS IN (1,3) "
				+ "AND A.VALUE=C.PDPSUBSID AND B.SERVICECODE = '"+msisdn+"' ";
		
		
		Statement st = null;
		ResultSet rs = null;
		try {
		st = conn2.createStatement();
		logger.info("Query PDPSUBSNAME SQL:"+sql);
		rs = st.executeQuery(sql);
		
		if(rs.next()){
			result = rs.getString("PDPSUBSNAME");
		}
		
		}catch (SQLException e) {
			ErrorHandle("At updateVolumePocket occur SQLException!",e);
		} catch (Exception e) {
			ErrorHandle("At updateVolumePocket occur Exception!",e);
		}finally{
			
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {

			}
			
		
		}
		
		return result;
		
	}
	
	
	//20160721 add
	private boolean updateVolumePocket(){
		logger.info("updateVolumePocket...");
		subStartTime = System.currentTimeMillis();

		boolean result = false;
		
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			//新增資料
			for(Map<String,String> m : insertVolumeList){

				sql = "insert into HUR_VOLUME_POCKET(SERVICEID,START_DATE,END_DATE,CREATE_TIME,MCC,PID,TYPE,LIMIT,IMSI)"
						+ "VALUES('"+m.get("SERVICEID")+"','"+m.get("SDATE")+"','"+m.get("EDATE")+"',sysdate,'"+m.get("MCC")+"',"+m.get("PID")+","+m.get("TYPE")+","+m.get("LIMIT")+","
								+ (m.get("IMSI")!=null?"'"+m.get("IMSI")+"'":"null")	+ ")";
				logger.debug("insertVolumePocket SQL : "+sql); 
				st.executeUpdate(sql);
			}
			//20161116 add 新增玩後清空此List
			insertVolumeList.clear();
			//更新資料
			for(Map<String,String> m : updateVolumePocketMap){
				sql = "update HUR_VOLUME_POCKET A set "
					+ (m.get("ALERTED")!=null?"A.ALERTED = '"+m.get("ALERTED")+"' ":"")
					+ (m.get("TERMINATE")!=null?"A.TERMINATE = "+m.get("TERMINATE")+" ":"")
					+ (m.get("IS_RESUME")!=null?"A.IS_RESUME = "+m.get("IS_RESUME")+" ":"")
					+ " where A.PID="+m.get("PID")+" ";
				logger.debug("updateVolumePocket SQL : "+sql); 
				st.executeUpdate(sql);
			}
			//20170104 add 更新完後清空此List
			updateVolumePocketMap.clear();
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At updateVolumePocket occur SQLException!",e);
		} catch (Exception e) {
			ErrorHandle("At updateVolumePocket occur Exception!",e);
		}finally{
			
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {

			}
			
		
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	//20180308 add
	private boolean insertQosResetLog(){
		logger.info("insertQosResetLog...");
		subStartTime = System.currentTimeMillis();

		boolean result = false;
		
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			//新增資料
			for(Map<String,String> m : insertQosResetList){

				sql = "insert into HUR_QOSRESET_LOG(IMSI,MSISDN,MCCMNC,TYPE)"
						+ "VALUES('"+m.get("IMSI")+"','"+m.get("MSISDN")+"','"+m.get("MCCNET")+"','"+m.get("TYPE")+"')";
				logger.debug("insertQosResetLog SQL : "+sql); 
				st.addBatch(sql);
			}
			st.executeBatch();

			insertQosResetList.clear();
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At insertQosResetLog occur SQLException!",e);
		} catch (Exception e) {
			ErrorHandle("At insertQosResetLog occur Exception!",e);
		}finally{
			
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {

			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	
	public String getVolumePocketPID(){
		int pid = 0;
		/*if(pid == 0){
			sql = "select nvl(MAX(PID),0)+1 PID from HUR_VOLUME_POCKET ";
			Statement st = null;
			ResultSet rs = null;
			try {
				st = conn.createStatement();
				logger.debug("select pid : "+sql); 
				rs=st.executeQuery(sql);
				while(rs.next()){
					pid = rs.getInt("PID");
				}
			} catch (SQLException e) {
				ErrorHandle("At updateVolumePocket occur SQLException!");
			} catch (Exception e) {
				ErrorHandle("At updateVolumePocket occur Exception!");
			}finally{
				
				try {
					if(st!=null)
						st.close();
					if(rs!=null)
						rs.close();
				} catch (SQLException e) {
				}
			}
		}else{
			pid++;
		}*/
		
		sql = "select HUR_VOLUME_POCKET_SEQ.nextval PID  from dual ";
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			logger.debug("select pid : "+sql); 
			rs=st.executeQuery(sql);
			while(rs.next()){
				pid = rs.getInt("PID");
			}
		} catch (SQLException e) {
			ErrorHandle("At updateVolumePocket occur SQLException!");
		} catch (Exception e) {
			ErrorHandle("At updateVolumePocket occur Exception!");
		}finally{
			
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
					 
			} catch (SQLException e) {
			}
		
		 
		}
		
		return String.valueOf(pid);
	}
	
	//20160704 add
	private void updateSystemConfig(String name,String value){
		Statement st = null;
		try {
			st = conn.createStatement();
			sql = "update HUR_DVRS_CONFIG A set A.VALUE = '"+value+"' where A.NAME = '"+name+"'";
			logger.debug("updateSystemConfig SQL : "+sql); 
			st.executeUpdate(sql);
			
			
		} catch (SQLException e) {
			ErrorHandle("At updateSystemConfig occur SQLException!");
		} catch (Exception e) {
			ErrorHandle("At updateSystemConfig occur Exception!");
		}finally{
			if(st!=null){
				try {
					st.close();
				} catch (SQLException e) {

				}
			}
		}
	}
	

	/**
	 * 華人上網包警示功能
	 * 當用戶在華人上網包達上限時，發送警示簡訊
	 * @throws Exception
	 */
	public void addonVolumeAlert() throws Exception{
		logger.info("addonVolumeAlert...");
		subStartTime = System.currentTimeMillis();
		//20150623 新增華人上網包
		//暫存數據用量資料 Key:SERVICEID,Value:Volume、addonCode
		Map<String,Map<String,Double>> tempMap = new HashMap<String,Map<String,Double>>();
		
		for(String day : currentDayMap.keySet()){
			//這個月的月資料
			if(sYearmonth.equalsIgnoreCase(day.substring(0, 6))){
				Date dayTime = null;
				try {
					dayTime = year_month_day_sdf.parse(day);
				} catch (ParseException e) {
					continue;
				}
				for(String serviceid:currentDayMap.get(day).keySet()){
					for(String mccNet:currentDayMap.get(day).get(serviceid).keySet()){
						/*if(checkQosAddon(serviceid, mccNet, dayTime,(double) 0)){
							//進行累計
							Double oldVolume=tempMap.get(serviceid)==null?0D:tempMap.get(serviceid);
							Double volume=parseDouble((String) currentDayMap.get(day).get(serviceid).get(mccNet).get("VOLUME"));
							tempMap.put(serviceid, oldVolume+volume);
						}*/
						String addonCodeSeq = checkQosAddon4(serviceid, mccNet, dayTime);
						
						if(addonCodeSeq!= null) {
							Map<String,Double> m = new HashMap<String,Double>();
							if(tempMap.containsKey(serviceid)) {
								m = tempMap.get(serviceid);
							}
							Double oldVolume=m.get(addonCodeSeq)==null?0D:m.get(addonCodeSeq);
							Double volume=parseDouble((String) currentDayMap.get(day).get(serviceid).get(mccNet).get("VOLUME"));
							m.put(addonCodeSeq, oldVolume+volume);
							tempMap.put(serviceid, m);
						}
					}
				}
			}
		}
		//SX001、SX002
		Double DEFAULT_VOLUME_THRESHOLD1 = getSystemConfigDoubleParam("0", "VOLUME_LIMIT1");
		Double DEFAULT_VOLUME_THRESHOLD2 = getSystemConfigDoubleParam("0", "VOLUME_LIMIT2");
		
		//SX004
		Double DEFAULT_VOLUME_THRESHOLD3 = getSystemConfigDoubleParam("0", "VOLUME_LIMIT3");
		Double DEFAULT_VOLUME_THRESHOLD4 = getSystemConfigDoubleParam("0", "VOLUME_LIMIT4");
		
		//SX005
		Double DEFAULT_VOLUME_THRESHOLD5 = getSystemConfigDoubleParam("0", "VOLUME_LIMIT5");
		Double DEFAULT_VOLUME_THRESHOLD6 = getSystemConfigDoubleParam("0", "VOLUME_LIMIT6");
		
		//SX006
		Double DEFAULT_VOLUME_THRESHOLD7 = getSystemConfigDoubleParam("0", "VOLUME_LIMIT7");
		Double DEFAULT_VOLUME_THRESHOLD8 = getSystemConfigDoubleParam("0", "VOLUME_LIMIT8");
		
		
		if(		DEFAULT_VOLUME_THRESHOLD1 == null || 
				DEFAULT_VOLUME_THRESHOLD2 == null ||
				DEFAULT_VOLUME_THRESHOLD3 == null || 
				DEFAULT_VOLUME_THRESHOLD4 == null ||
				DEFAULT_VOLUME_THRESHOLD5 == null || 
				DEFAULT_VOLUME_THRESHOLD6 == null ||
				DEFAULT_VOLUME_THRESHOLD7 == null || 
				DEFAULT_VOLUME_THRESHOLD8 == null){
			sql="";
			ErrorHandle("At addonVolumeAlert can't find DEFAULT_VOLUME_THRESHOLD!");
			return;
		}
		
		int smsCount=0;
		
		for(String serviceid:tempMap.keySet()){
			Map<String,Double> m = tempMap.get(serviceid);
			Double everAlertVolume = parseDouble((String) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_VOLUME"));
			//確認資料
			if(!msisdnMap.containsKey(serviceid)){
				ErrorHandle("MsisdnMap without serviceid:"+serviceid+"'s data!");
				continue;
			}
			
			//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
			String phone = (String) getMSISDN(serviceid);
			if(phone==null ||"".equals(phone)){
				sql="";
				ErrorHandle("At addonVolumeAlert occur error! The serviceid:"+serviceid+" can't find msisdn to send !");
				continue;
			}
			
			String priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID"); 
			
			if(priceplanid==null){
				sql="";
				ErrorHandle("At addonVolumeAlert sendAlertSMS occur error! The serviceid:"+serviceid+" can't find priceplanid!");
				continue;
			}

			//確認方案與流量限度
			for(String addonCodeSeq : m.keySet()) {
				Double volume = m.get(addonCodeSeq);
				
				boolean sendmsg = false;
				String [] contentid = null;
				
				//華人上網包
				if(addonCodeSeq.startsWith("SX001") || addonCodeSeq.startsWith("SX002")) {
					if(volume>=DEFAULT_VOLUME_THRESHOLD2 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD2){
						String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT2_MSG_ID");
						if(msgids == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT2_MSG_ID! ");
							continue;
						}
						contentid = msgids.split(",");
						sendmsg=true;
					}else if(volume>=DEFAULT_VOLUME_THRESHOLD1 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD1){
						String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT1_MSG_ID");
						 if(msgids == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT1_MSG_ID! ");
							continue;
						}
						contentid = msgids.split(",");
						sendmsg=true;
					}
					
					//20180425 add Qosreset for SX001,SX002
					Map<String,Object> resetSettingMap = qosResetSettingMap.get(1).get("139").get(0);
					Integer limitTimes = (Integer) resetSettingMap.get("TIMES");
					double limitVolume = (Double) resetSettingMap.get("VOLUME");
	
					int alertTimes = 0;
					if(qosM_ResetLogMap.containsKey(phone)){
						//alertTimes = Integer.parseInt(qosM_ResetLogMap.get(phone).get("TIMES"));
						for(Map<String,String> addonMap : addonDataList){
							Calendar startTime = null,endTime = null;
							//取得開始時間
							startTime = Calendar.getInstance();
							startTime.setTime(year_month_day_sdf.parse(addonMap.get("STARTDATE")));
							startTime.set(Calendar.HOUR_OF_DAY, 0);
							startTime.set(Calendar.MINUTE, 0);
							startTime.set(Calendar.SECOND, 0);
							
							//取得到期時間
							if(m.get("ENDDATE")!=null){
								endTime = Calendar.getInstance();
								endTime.setTime(year_month_day_sdf.parse(addonMap.get("ENDDATE")));
								endTime.set(Calendar.DAY_OF_YEAR, endTime.get(Calendar.DAY_OF_YEAR)+1);
								endTime.set(Calendar.HOUR_OF_DAY, 0);
								endTime.set(Calendar.MINUTE, 0);
								endTime.set(Calendar.SECOND, 0);
							}
							Date now = year_month_day_sdf.parse(sYearmonthday);
							
							if(serviceid.equals(addonMap.get("SERVICEID"))&& 
									("SX001".equalsIgnoreCase(addonMap.get("SERVICECODE")) ||"SX002".equalsIgnoreCase(addonMap.get("SERVICECODE"))) &&
									startTime.getTime().before(now) && (endTime==null || endTime.getTime().after(now))) {
								for(Map<String,String> resetMap : qosM_ResetLogMap.get(phone)) {
									String mccmnc = resetMap.get("MCCMNC");
									Date time = year_month_day_sdf.parse(resetMap.get("TIME"));
									
									if((mccmnc.indexOf("SX001")!=-1 || mccmnc.indexOf("SX002")!=-1)
											&& startTime.getTime().before(time) && (endTime==null || endTime.getTime().after(time))) {
										alertTimes++;
									}
								}
							}
						}
					}
					
					int rTime = (int)(volume / limitVolume);
					//當需重置次數<已重置次數，重置
					//但是當需重置次數>最大重置次數，表示已經太晚，不重置
					logger.info(serviceid+",volume="+volume+" and limit volume="+limitVolume+", calculated number is "+rTime+" and alertTimes="+alertTimes+",limitTimes="+limitTimes);
					
					if(rTime <= limitTimes && rTime > alertTimes) {
						logger.info("do reset");
						//進行Reset
						Map<String,String> qosReset = new HashMap<String,String>();
						qosReset.put("IMSI", (String) msisdnMap.get(serviceid).get("IMSI"));
						qosReset.put("MSISDN", phone);
						qosReset.put("MCCNET", (String) resetSettingMap.get("MCCNET"));
						qosReset.put("TYPE", "M_RESET");
						insertQosResetList.add(qosReset);
					}
					
					
				//多國流量包
				}else if(addonCodeSeq.startsWith("SX004")) {
					if(volume>=DEFAULT_VOLUME_THRESHOLD4 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD4){
						//2.0 GB 
						String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT4_MSG_ID");
						if(msgids == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT4_MSG_ID! ");
							continue;
						}
						contentid = msgids.split(",");
						sendmsg=true;
					}else if(volume>=DEFAULT_VOLUME_THRESHOLD3 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD3){
						//1.5 GB 
						String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT3_MSG_ID");
						 if(msgids == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT3_MSG_ID! ");
							continue;
						}
						contentid = msgids.split(",");
						sendmsg=true;
					}
				//高量華人上網包
				}else if(addonCodeSeq.startsWith("SX005")) {
					if(volume>=DEFAULT_VOLUME_THRESHOLD6 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD6){
						String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT6_MSG_ID");
						if(msgids == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT6_MSG_ID! ");
							continue;
						}
						contentid = msgids.split(",");
						sendmsg=true;
					}else if(volume>=DEFAULT_VOLUME_THRESHOLD5 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD5){
						String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT5_MSG_ID");
						 if(msgids == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT5_MSG_ID! ");
							continue;
						}
						contentid = msgids.split(",");
						sendmsg=true;
					}
					
					//20180425 add Qosreset for SX005
					Map<String,Object> resetSettingMap = qosResetSettingMap.get(2).get("139").get(0);
					Integer limitTimes = (Integer) resetSettingMap.get("TIMES");
					double limitVolume = (Double) resetSettingMap.get("VOLUME");
	
					int alertTimes = 0;
					if(qosM_ResetLogMap.containsKey(phone)){
						//alertTimes = Integer.parseInt(qosM_ResetLogMap.get(phone).get("TIMES"));
						for(Map<String,String> addonMap : addonDataList){
							Calendar startTime = null,endTime = null;
							//取得開始時間
							startTime = Calendar.getInstance();
							startTime.setTime(year_month_day_sdf.parse(addonMap.get("STARTDATE")));
							startTime.set(Calendar.HOUR_OF_DAY, 0);
							startTime.set(Calendar.MINUTE, 0);
							startTime.set(Calendar.SECOND, 0);
							
							//取得到期時間
							if(m.get("ENDDATE")!=null){
								endTime = Calendar.getInstance();
								endTime.setTime(year_month_day_sdf.parse(addonMap.get("ENDDATE")));
								endTime.set(Calendar.DAY_OF_YEAR, endTime.get(Calendar.DAY_OF_YEAR)+1);
								endTime.set(Calendar.HOUR_OF_DAY, 0);
								endTime.set(Calendar.MINUTE, 0);
								endTime.set(Calendar.SECOND, 0);
							}
							
							Date now = year_month_day_sdf.parse(sYearmonthday);
							
							if(serviceid.equals(addonMap.get("SERVICEID"))&& 
									"SX005".equalsIgnoreCase(addonMap.get("SERVICECODE")) &&
									startTime.getTime().before(now) && (endTime==null || endTime.getTime().after(now))) {
								for(Map<String,String> resetMap : qosM_ResetLogMap.get(phone)) {
									String mccmnc = resetMap.get("MCCMNC");
									Date time = year_month_day_sdf.parse(resetMap.get("TIME"));
									
									if(mccmnc.indexOf("SX005")!=-1
											&& startTime.getTime().before(time) && (endTime==null || endTime.getTime().after(time))) {
										alertTimes++;
									}
								}
							}
						}
					}
					int rTime = (int)(volume / limitVolume);
					//當需重置次數<已重置次數，重置
					//但是當需重置次數>最大重置次數，表示已經太晚，不重置
					if(rTime <= limitTimes && rTime > alertTimes) {
						//進行Reset
						Map<String,String> qosReset = new HashMap<String,String>();
						qosReset.put("IMSI", (String) msisdnMap.get(serviceid).get("IMSI"));
						qosReset.put("MSISDN", phone);
						qosReset.put("MCCNET", (String) resetSettingMap.get("MCCNET"));
						qosReset.put("TYPE", "M_RESET");
						insertQosResetList.add(qosReset);
					}
				}
				
				//20180718
				
				else if(addonCodeSeq.startsWith("SX006")) {
					if(volume>=DEFAULT_VOLUME_THRESHOLD8 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD8){
						String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT8_MSG_ID");
						if(msgids == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT8_MSG_ID! ");
							continue;
						}
						contentid = msgids.split(",");
						sendmsg=true;
					}else if(volume>=DEFAULT_VOLUME_THRESHOLD7 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD7){
						String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT7_MSG_ID");
						 if(msgids == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT7_MSG_ID! ");
							continue;
						}
						contentid = msgids.split(",");
						sendmsg=true;
					}
					
					//20180425 add Qosreset for SX006
					//20180720  因與華人上網包衝突暫不供裝PLAN
					/*Map<String,Object> resetSettingMap = qosResetSettingMap.get(3).get("139").get(0);
					Integer limitTimes = (Integer) resetSettingMap.get("TIMES");
					double limitVolume = (Double) resetSettingMap.get("VOLUME");
	
					int alertTimes = 0;
					if(qosM_ResetLogMap.containsKey(phone)){
						//alertTimes = Integer.parseInt(qosM_ResetLogMap.get(phone).get("TIMES"));
						for(Map<String,String> addonMap : addonDataList){
							Calendar startTime = null,endTime = null;
							//取得開始時間
							startTime = Calendar.getInstance();
							startTime.setTime(year_month_day_sdf.parse(addonMap.get("STARTDATE")));
							startTime.set(Calendar.HOUR_OF_DAY, 0);
							startTime.set(Calendar.MINUTE, 0);
							startTime.set(Calendar.SECOND, 0);
							
							//取得到期時間
							if(m.get("ENDDATE")!=null){
								endTime = Calendar.getInstance();
								endTime.setTime(year_month_day_sdf.parse(addonMap.get("ENDDATE")));
								endTime.set(Calendar.DAY_OF_YEAR, endTime.get(Calendar.DAY_OF_YEAR)+1);
								endTime.set(Calendar.HOUR_OF_DAY, 0);
								endTime.set(Calendar.MINUTE, 0);
								endTime.set(Calendar.SECOND, 0);
							}
							
							Date now = year_month_day_sdf.parse(sYearmonthday);
							
							if(serviceid.equals(addonMap.get("SERVICEID"))&& 
									"SX006".equalsIgnoreCase(addonMap.get("SERVICECODE")) &&
									startTime.getTime().before(now) && (endTime==null || endTime.getTime().after(now))) {
								for(Map<String,String> resetMap : qosM_ResetLogMap.get(phone)) {
									String mccmnc = resetMap.get("MCCMNC");
									Date time = year_month_day_sdf.parse(resetMap.get("TIME"));
									
									if(mccmnc.indexOf("SX006")!=-1
											&& startTime.getTime().before(time) && (endTime==null || endTime.getTime().after(time))) {
										//新美國流量包，4GB重置一次，之後使用滿6GB，達到10GB服務，找到一次後脫離迴圈
										alertTimes++;
										break;
									}
								}
							}
						}
					}
					int rTime = (int)(volume / limitVolume);
					//當需重置次數<已重置次數，重置
					//但是當需重置次數>最大重置次數，表示已經太晚，不重置
					if(rTime <= limitTimes && rTime > alertTimes) {
						//進行Reset
						Map<String,String> qosReset = new HashMap<String,String>();
						qosReset.put("IMSI", (String) msisdnMap.get(serviceid).get("IMSI"));
						qosReset.put("MSISDN", phone);
						qosReset.put("MCCNET", (String) resetSettingMap.get("MCCNET"));
						qosReset.put("TYPE", "M_RESET");
						insertQosResetList.add(qosReset);
					}*/
				}
				
				if(sendmsg){
					smsCount+=sendSMS(serviceid,contentid,phone,new String[]{"{{month}}"},new String[] {sYearmonth.substring(4,6)});
					
					//更新CurrentMap
					currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_VOLUME",volume.toString());
					
					//20150629 add
					Set<String> set2 = new HashSet<String>();

					if(updateMap.containsKey(sYearmonth)){	
						set2 = updateMap.get(sYearmonth);
					}
					set2.add(serviceid);
					updateMap.put(sYearmonth, set2);
				}	
			}
		}
		
		
		/*for(String serviceid:tempMap.keySet()){
			Double volume=tempMap.get(serviceid);
			Double everAlertVolume = parseDouble((String) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_VOLUME"));
			
			
			//確認資料
			if(!msisdnMap.containsKey(serviceid)){
				ErrorHandle("MsisdnMap without serviceid:"+serviceid+"'s data!");
				continue;
			}
			
			//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
			String phone = (String) getMSISDN(serviceid);
			if(phone==null ||"".equals(phone)){
				sql="";
				ErrorHandle("At addonVolumeAlert occur error! The serviceid:"+serviceid+" can't find msisdn to send !");
				continue;
			}
			
			String priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID"); 
			
			if(priceplanid==null){
				sql="";
				ErrorHandle("At addonVolumeAlert sendAlertSMS occur error! The serviceid:"+serviceid+" can't find priceplanid!");
				continue;
			}
			boolean sendmsg = false;
			String [] contentid = null;
			//華人上網包簡訊內容
			if(volume>=DEFAULT_VOLUME_THRESHOLD2 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD2){
				//2.0 GB 
				String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT2_MSG_ID");
				if(msgids == null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT2_MSG_ID! ");
					continue;
				}
				contentid = msgids.split(",");
				sendmsg=true;
			}else if(volume>=DEFAULT_VOLUME_THRESHOLD && everAlertVolume<DEFAULT_VOLUME_THRESHOLD){
				//1.5 GB 
				String msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT1_MSG_ID");
				 if(msgids == null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT1_MSG_ID! ");
					continue;
				}
				contentid = msgids.split(",");
				sendmsg=true;
			}
			
			if(sendmsg){
				smsCount+=sendSMS(serviceid,contentid,phone,null,null);
				
				//更新CurrentMap
				currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_VOLUME",volume.toString());
				
				//20150629 add
				Set<String> set2 = new HashSet<String>();

				if(updateMap.containsKey(sYearmonth)){	
					set2 = updateMap.get(sYearmonth);
				}
				set2.add(serviceid);
				updateMap.put(sYearmonth, set2);
			}	
		}*/
		logger.debug("Total send 華人上網包 volume alert SMS "+smsCount+" ...");
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	
	
	private void checkDailySlowDown(){
		if(currentDayMap.get(sYearmonthday)==null){
			logger.info("current Day without today.");
			return;
		}
		
		logger.info("checkSlowDown...");
		subStartTime = System.currentTimeMillis();		
		//取得每日降速清單
		for(Map<String,String> m: slowDownMap.get("1")){
			//MNO，允許全碼或國碼
			String mccmnc = (String) m.get("MNO");
			Set<String> mccmncs = new HashSet<String>();
			for(String s:mccmnc.split(","))
				mccmncs.add(s);
			//取得適用的PricePlanID
			String priceplan = (String) m.get("PRICEPLAN");
			Set<String> priceplans = new HashSet<String>();
			for(String s:priceplan.split(","))
				priceplans.add(s);
			
			
			int type = Integer.parseInt(m.get("TYPE"));
			
			Integer limit = parseInt((String) m.get("LIMIT"));	
			String nGPRSName = (String) m.get("GPRS_NAME");			
			
			//從今日的累計流量判定是否降速
			for(String serviceid : currentDayMap.get(sYearmonthday).keySet()){	
				
				//確認資料
				if(!msisdnMap.containsKey(serviceid)){
					ErrorHandle("MsisdnMap without serviceid:"+serviceid+"'s data!");
					continue;
				}
				
				
				//取的Priceplan Id
				String priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID"); 			
				if(priceplanid==null){
					sql="";
					ErrorHandle("At checkSlowDown ,The serviceid:"+serviceid+" can't find priceplanid!");
					continue;
				}
				
				if(priceplans.contains(priceplanid)){
					Set<String> mccs = new HashSet<String>();
					double total = 0d;
					
					for(String cMccmnc : currentDayMap.get(sYearmonthday).get(serviceid).keySet()){
						//因為沒有DayCap需每日降速國家包含美國，多國上網包區域，所以必須設例外不降速
						try {
							/*//20170630 在流量包其間，不另外降速
							if(checkInPackage(serviceid, mccmnc, year_month_day_sdf.parse(sYearmonthday))){
								continue;
							}else { //20180115 在多國上網包其間，不另外降速，20180718 add 美國上網包期間不另外降速
								
							}*/
							
							//如果在多國或美國包的使用區間，不含在降速累計
							String addonCode = checkQosAddon2(serviceid, cMccmnc, year_month_day_sdf.parse(sYearmonthday));
							if("SX004".equalsIgnoreCase(addonCode) || "SX006".equalsIgnoreCase(addonCode)) {
								continue;
							}
						} catch (ParseException e) {
							ErrorHandle("At checkSlowDown occured error, can't check "+serviceid+ "if in package period.", e);
						} catch (Exception e) {
							ErrorHandle("At checkSlowDown occured error, can't check "+serviceid+ "if in package period.", e);
						}
						
						//如果國家區域在降速區域中 
						if(mccmncs.contains(cMccmnc) || mccmncs.contains(cMccmnc.substring(0,3))){
							//如果已經降速，歸0，並跳出
							if("1".equals(currentDayMap.get(sYearmonthday).get(serviceid).get(cMccmnc).get("IS_SLOWDOWN"))){
								total = 0d;
								break;
							}
							//統計流量
							double volume =  parseDouble((String) currentDayMap.get(sYearmonthday).get(serviceid).get(cMccmnc).get("VOLUME"));
							total += volume;
							mccs.add(cMccmnc);
						}
					}
					
					//如果流量超過
					if(limit<=total/1024){//limit存的是KB，current存放的是原始B
						//20160725 add 執行降速
						doSlowDown(type,serviceid,nGPRSName,priceplanid);
						
						for(String cMccmnc : mccs){
							currentDayMap.get(sYearmonthday).get(serviceid).get(cMccmnc).put("IS_SLOWDOWN", "1");
							
							//標記Update
							Map <String,Set<String>> map6 = new HashMap<String,Set<String>>();
							Set<String> set1 = new HashSet<String>();					

							if(updateMapD.containsKey(sYearmonthday)){	
								map6 = updateMapD.get(sYearmonthday);
								if(map6.containsKey(serviceid)){
									set1 = map6.get(serviceid);
								}
							}
							set1.add(cMccmnc);
							map6.put(serviceid, set1);
							updateMapD.put(sYearmonthday, map6);
							
						}
					}
				}
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	/**
	 * 執行每日降速，在Log中 Type為1
	 * @param type
	 * @param serviceid
	 * @param nGPRSName
	 * @param pricplanID
	 */
	private void doSlowDown(int type,String serviceid,String nGPRSName,String pricplanID){
		logger.info("doSlowDown..."+serviceid+"...");
		
		suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
		
		String msisdn = getMSISDN(serviceid);
		if(msisdn==null ||"".equals(msisdn)){
			sql="";
			ErrorHandle("At checkVolumePocket, The serviceid:"+serviceid+" can't find msisdn!");
			return;
		}
	
		String imsi = msisdnMap.get(serviceid).get("IMSI");
		if(imsi==null ||"".equals(imsi)){
			sql="";
			ErrorHandle("At checkVolumePocket, The serviceid:"+serviceid+" can't find imsi!");
			return;
		}
		
		String oGPRSName = null;
		
		Statement st = null;
		//20170426 add
		Statement st2 = null;
		ResultSet rs = null;
		try {
			//如果用戶已經退租，不降速
			int userStatus = 0;
			//20170426 add
			st2 = conn.createStatement();
			//20170426 add
			String sql3 = "select nvl(count(1),0) cd from service where serviceid = "+serviceid+" and datecanceled is null ";
			
			logger.info("Query user status SQL:"+sql3);
			rs = st2.executeQuery(sql3);
			if(rs.next())
				userStatus = rs.getInt("cd");
			
			if(userStatus ==0){
				logger.info("Cause  the serviceid="+serviceid+" had been terminated, need not slowdown.");
				return;
			}
				
		} catch (SQLException e) {
			ErrorHandle("At doSlowDown occur Exception error!", e);
		}finally{
			try {
				if(st!=null) st.close();
				if(rs!=null) rs.close();
			} catch (SQLException e) {
			}
		}
		
		
		oGPRSName = getOriginGPRSName(msisdn);
		
		if(oGPRSName == null){
			ErrorHandle("At doSlowDown occur error! Can't find serviceid="+serviceid+"'s PDPSUBSNAME.");
			return ;
		}
		

		PreparedStatement pst = null;
		try {
			
			sql = "";
			//降速
			//20141118 add 傳回suspend排程的 service order nbr
			Map<String,String> orderNBR = sus.doChangeGPRSStatus(type,imsi, msisdn, "1", nGPRSName);
			serviceOrderNBR.add(orderNBR);
			
			//建立Log 並紀錄原始的GPRS Name
			sql=
					"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
					+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS,IS_SLOWDOWN,PROCESS_DAY) "
					+ "VALUES(?,?,SYSDATE,?,?,?,'"+sYearmonthday+"')";
			
			pst=conn.prepareStatement(sql);
			pst.setString(1,orderNBR.get("cServiceOrderNBR") );
			pst.setString(2,imsi );
			pst.setString(3,msisdn );
			pst.setString(4,oGPRSName );
			pst.setString(5,"1" );
			logger.info("Execute SQL : "+sql);
			
			pst.executeUpdate();
		} catch (SQLException e) {
			ErrorHandle("At doSlowDown occur SQLException error!", e);
		/*} catch (IOException e) {
			sql="";
			ErrorHandle("At doSlowDown occur IOException error!", e);
		} catch (ClassNotFoundException e) {
			sql="";
			ErrorHandle("At doSlowDown occur ClassNotFoundException error!", e);*/
		} catch (Exception e) {
			sql="";
			ErrorHandle("At doSlowDown occur Exception error!", e);
		}finally{
			try {
				if(pst!=null) pst.close();
				if(sus.Temprs!=null) sus.Temprs.close();
			} catch (SQLException e) {
			}
		}
	}
	
	
	/**
	 * 對因每日超額被降速客戶，在一日的開始進行復網，LOG標記IS_SLOWDOWN為1，使用原本的GPRSNAME恢復
	 */
	private void doResumeSpeed(){
		logger.info("doResumeSpeed...");
		
		String resumeSpeedReport = "";
		//每日恢復速度
		sql = "select A.MSISDN,A.GPRS_STATUS,B.SERVICEID,C.IMSI,TO_CHAR(A.CREATE_DATE,'yyyy-MM-dd hh24:mi:ss') CREATE_DATE,A.IS_SLOWDOWN "
				+ "from HUR_SUSPEND_GPRS_LOG A,SERVICE B,IMSI C "
				+ "WHERE A.MSISDN=B.SERVICECODE AND B.SERVICEID = C.SERVICEID "
				+ "AND A.IS_SLOWDOWN = '1' AND RESULT = '000' AND A.PROCESS_DAY= to_char(sysdate-1,'yyyyMMdd') ";
		Statement st = null;
		ResultSet rs = null;
		
		List<Map<String,String>> resumeSpeedList = new ArrayList<Map<String,String>>();
		try {
			st = conn.createStatement();
			
			logger.info("Query slow up SQL:"+sql);
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> m = new HashMap<String,String>();
				m.put("SERVICEID", rs.getString("SERVICEID"));
				m.put("IMSI", rs.getString("IMSI"));
				m.put("MSISDN", rs.getString("MSISDN"));
				m.put("GPRS_STATUS", rs.getString("GPRS_STATUS"));
				m.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				m.put("TYPE", rs.getString("IS_SLOWDOWN"));
				resumeSpeedList.add(m);
			}
			
		} catch (SQLException e) {
			ErrorHandle("At doResumeSpeed occur Exception error!", e);
		}finally{
			try {
				if(st!=null) st.close();
				if(rs!=null) rs.close();
			} catch (SQLException e) {
			}
		}
		
		
		//20170629 美國流量包到期後恢復速度
		sql = "select A.serviceid,A.PID,C.GPRS_STATUS,B.servicecode,D.imsi,TO_CHAR(C.CREATE_DATE,'yyyy-MM-dd hh24:mi:ss') CREATE_DATE,C.IS_SLOWDOWN "
				+ "from HUR_VOLUME_POCKET A,service B, HUR_SUSPEND_GPRS_LOG C,IMSI D "
				+ "where A.SERVICEID = B.serviceid and b.servicecode = C.MSISDN and A.serviceid = D.serviceid "
				+ "and A.TYPE = 0 "	//美國流量包
				+ "and to_date(A.END_DATE,'yyyyMMdd') < to_date('"+sYearmonthday+"','yyyyMMdd') and A.IS_RESUME = 0 "
				+ "and C.IS_SLOWDOWN = '2' " //是額外服務的GPRS變更
				+ "and to_date(A.START_DATE,'yyyyMMdd')<C.CREATE_DATE and C.CREATE_DATE< to_date(A.END_DATE,'yyyyMMdd') ";
		try {
			st = conn.createStatement();
			
			logger.info("Query slow up SQL:"+sql);
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> m = new HashMap<String,String>();
				m.put("SERVICEID", rs.getString("serviceid"));
				m.put("IMSI", rs.getString("IMSI"));
				m.put("MSISDN", rs.getString("servicecode"));
				m.put("GPRS_STATUS", rs.getString("GPRS_STATUS"));
				m.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				m.put("TYPE", rs.getString("IS_SLOWDOWN"));
				m.put("PID", rs.getString("PID"));
				resumeSpeedList.add(m);
			}
			
		} catch (SQLException e) {
			ErrorHandle("At doResumeSpeed occur Exception error!", e);
		}finally{
			try {
				if(st!=null) st.close();
				if(rs!=null) rs.close();
			} catch (SQLException e) {
			}
		}
		
		PreparedStatement pst = null;
		suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
		try {
			for(Map<String,String> m : resumeSpeedList){
				
				String imsi = m.get("IMSI");
				String msisdn = m.get("MSISDN");
				String GPRS_STATUS = m.get("GPRS_STATUS");
				String time = m.get("CREATE_DATE");
				String type = m.get("TYPE");
				//20141118 add 傳回suspend排程的 service order nbr
				Map<String,String> orderNBR = sus.doChangeGPRSStatus(0,imsi, msisdn, "1", GPRS_STATUS);
				
				serviceOrderNBR.add(orderNBR);
				sql=
						"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
						+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS) "
						+ "VALUES(?,?,SYSDATE,?,?)";
				
				pst=conn.prepareStatement(sql);
				pst.setString(1,orderNBR.get("cServiceOrderNBR") );
				pst.setString(2,imsi );
				pst.setString(3,msisdn );
				pst.setString(4,GPRS_STATUS );
				logger.info("Execute SQL : "+sql);
				pst.executeUpdate();
				
				if("1".equals(type)){
					resumeSpeedReport += msisdn+" had been resume speed!( slowDown at "+time+")\n";
				}
				else if("2".equals(type)){
					Map<String,String> mm = new HashMap<String,String>();
					mm.put("PID",m.get("PID"));
					mm.put("IS_RESUME", "1");
					updateVolumePocketMap.add(mm);
				}
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
				
			}
			
		} catch (SQLException e) {
			ErrorHandle("At doResumeSpeed occur SQLException error!", e);
		} catch (Exception e) {
			sql="";
			ErrorHandle("At doResumeSpeed occur Exception error!", e);
		}finally{
			try {
				if(pst!=null) pst.close();
				if(sus.Temprs!=null) sus.Temprs.close();
			} catch (SQLException e) {
			}
		}
		String mailReceiver = "k1988242001@gmail.com";
		if(!TEST_MODE){
			mailReceiver+=",Yvonne.lin@sim2travel.com";
		}
		
		logger.info("Send resumeSpeedReport result.");
		sendMail("DVRS Resume speed Report.",resumeSpeedReport,"DVRS Alert",mailReceiver);
		resumeSpeed = false;
		
	}

	/**
	 * 處理替代字串
	 * {{bracket}} 額度
	 * @param msg
	 * @param bracket
	 * @return
	 */
	
	private String processMag(String msg,String[] paramName,String[] paramValue){
		
		if(paramName!=null){
			for(int i = 0;i<paramName.length;i++){
				msg = msg.replace(paramName[i], paramValue[i]);
			}
		}
		
		return msg;
	}
	
	
	/*private String processMag(String msg,Double bracket,String cPhone,String currency){
		return processMag(msg,bracket,cPhone,null,currency);
	}
	
	private String processMag(String msg,Double bracket,String cPhone,String ICCID,String currency){
		
		//金額
		if(bracket==null){
			msg=msg.replace("{{bracket}}", "");
		}else{
			
			msg=msg.replace("{{bracket}}",FormatNumString(bracket,currency+"#,##0.00"));
		}
		
		//客服電話
		if(cPhone==null)
			cPhone="";
		msg=msg.replace("{{customerService}}",cPhone);
		
		//ICCID
		if(ICCID==null)
			ICCID="";
		msg=msg.replace("{{ICCID}}",ICCID);
		
		return msg;
	}*/
	
	/**
	 * 發送簡訊功能
	 * 處理post到 WebServer的xml字串
	 * @param msg
	 * @param phone
	 * @return
	 */
	@SuppressWarnings("unused")
	private String setSMSXmlParam(String msg,String phone){
		StringBuffer sb=new StringBuffer ();
		
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<SMSREQUEST>"
				+ "	<USERNAME>smppadmin</USERNAME>"
				+ "	<PASSWORD>QYrTciMQR</PASSWORD>"
				+ "	<ORGCODE>代發組織分類</ORGCODE>"
				+ "	<DATA>"
				+ "		<ITEM>"
				+ "			<SCHEDULE>0</SCHEDULE>"
				+ "			<MULTIPLE>0</MULTIPLE>"
				+ "			<MSG>"+msg+"</MSG>"
				+ "			<PHONE>"+phone+"</PHONE>"
				+ "		</ITEM>"
				+ "	</DATA>"
				+ "	<REMARK></REMARK>"
				+ "</SMSREQUEST>");
		
		return sb.toString();
	}
	
	/**
	 * 發送簡訊功能
	 * 處理post到 http網頁的Url並傳送
	 * @param msg
	 * @param phone
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private String setSMSPostParam(String msg,String phone) throws IOException{
		StringBuffer sb=new StringBuffer ();
		if(TEST_MODE){
			phone=DEFAULT_PHONE;
		}
		
		String PhoneNumber=phone,Text=msg,charset="big5",InfoCharCounter=null,PID=null,DCS=null;
		String param =
				"PhoneNumber=+{{PhoneNumber}}&"
				+ "Text={{Text}}&"
				+ "charset={{charset}}&"
				+ "InfoCharCounter={{InfoCharCounter}}&"
				+ "PID={{PID}}&"
				+ "DCS={{DCS}}&"
				+ "Submit=Submit";
		
		if(PhoneNumber==null)PhoneNumber="";
		if(Text==null)Text="";
		if(charset==null)charset="";
		if(InfoCharCounter==null)InfoCharCounter="";
		if(PID==null)PID="";
		if(DCS==null)DCS="";
		param=param.replace("{{PhoneNumber}}",PhoneNumber );
		param=param.replace("{{Text}}",Text.replaceAll("\\+", "%2b") );
		param=param.replace("{{charset}}",charset );
		param=param.replace("{{InfoCharCounter}}",InfoCharCounter );
		param=param.replace("{{PID}}",PID );
		param=param.replace("{{DCS}}",DCS );
		
		
		//20151022 change ip from 192.168.10.125 to 10.42.200.100
		return HttpPost("http://10.42.200.100:8800/Send%20Text%20Message.htm", param,"");
	}
	
	/**
	 * 變更GPRS //20160115 change
	 * 
	 * @param imsi
	 * @param msisdn
	 */
	/*private void changeGPRSStatus(String imsi,String msisdn,String GPRSStatus){
		logger.info("changeGPRSStatus...");
		
		suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
		PreparedStatement pst = null;
		try {
			
			sql = "";
			//20141118 add 傳回suspend排程的 service order nbr
			Map<String,String> orderNBR = sus.doChangeGPRSStatus(0,imsi, msisdn, GPRSStatus, "CHT-GPRS");
			
			serviceOrderNBR.add(orderNBR);
			sql=
					"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
					+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS) "
					+ "VALUES(?,?,SYSDATE,?,?)";
			
			pst=conn.prepareStatement(sql);
			pst.setString(1,orderNBR.get("cServiceOrderNBR") );
			pst.setString(2,imsi );
			pst.setString(3,msisdn );
			pst.setString(4,GPRSStatus );
			logger.info("Execute SQL : "+sql);
			
			pst.executeUpdate();
		} catch (SQLException e) {
			ErrorHandle("At changeGPRSStatus occur SQLException error!", e);
		} catch (IOException e) {
			sql="";
			ErrorHandle("At changeGPRSStatus occur IOException error!", e);
		} catch (ClassNotFoundException e) {
			sql="";
			ErrorHandle("At changeGPRSStatus occur ClassNotFoundException error!", e);
		} catch (Exception e) {
			sql="";
			ErrorHandle("At changeGPRSStatus occur Exception error!", e);
		}finally{
			try {
				if(pst!=null) pst.close();
				if(sus.Temprs!=null) sus.Temprs.close();
			} catch (SQLException e) {
			}
		}
	}*/
	
	//20151201 add
	 public String Query_GPRSStatus(String s2tmsisdn){
		 logger.info("check_GPRSStatus...");
		 String sG="";
	     String cGPRS="";
	     
	     sql="SELECT nvl(PDPSUBSID,0) as ab FROM basicprofile WHERE msisdn = '"+s2tmsisdn+"'";
	     logger.debug("Query_GPRSStatus:"+sql);
	          
	     Statement st = null;
	     ResultSet rs = null;
	     try {
	    	 st = conn.createStatement();
			 rs = st.executeQuery(sql);
			 while(rs.next()){
				 sG=rs.getString("ab");
			 }
			 logger.debug("GPRS_Values:"+sG);
			 if ((sG.equals("0"))||(sG.equals(""))){
				 cGPRS="0";
			 }else {
				 cGPRS="1";
			 }
		} catch (SQLException e) {
			ErrorHandle("At Query_GPRSStatus occur SQLException error!", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
	     return cGPRS;
	}
	
	/**
	 * 發送Error mail
	 * 
	 * @param content
	 */
	//20150616 change send from linux mail server
	/*private static void sendErrorMail(String content){
		mailReceiver=props.getProperty("mail.Receiver");
		mailSubject="DVRS Warnning Mail";
		mailContent="Error :"+content+"<br>\n"
				+ "Error occurr time: "+tool.DateFormat()+"<br>\n"
				+ "SQL : "+sql+"<br>\n"
				+ "Error Msg : "+errorMsg;

		try {
			if(mailReceiver==null ||"".equals(mailReceiver)){
				logger.error("Can't send email without receiver!");
			}else{
				tool.sendMail(logger, props, mailSender, mailReceiver, mailSubject, mailContent);
			}
			
		} catch (AddressException e) {
			logger.error("Error at sendErrorMail",e);
		} catch (MessagingException e) {
			logger.error("Error at sendErrorMail",e);
		} catch (IOException e) {
			logger.error("Error at sendErrorMail",e);
		} catch (Exception e) {
			logger.error("Error at sendErrorMail",e);
		}
	}
	
	*//**
	 * 發送mail
	 * 
	 * @param content
	 * @throws Exception 
	 */ /*
	private static void sendMail(String mailSubject,String mailContent,String mailSender,String mailReceiver) throws Exception{

		if(mailReceiver==null ||"".equals(mailReceiver)){
			logger.error("Can't send email without receiver!");
			throw new Exception("Can't send email without receiver!");
		}else{
			tool.sendMail(logger, props, mailSender, mailReceiver, mailSubject, mailContent);
		}

	}*/
	
	static void sendErrorMail(String msg){

		String mailReceiver=props.getProperty("mail.Receiver");
		mailSubject="DVRS Warnning Mail";
		mailContent="Error :"+msg+"<br>\n"
				+ "Error occurr time: "+DateFormat()+"<br>\n"
				+ "SQL : "+sql+"<br>\n"
				+ "Error Msg : "+errorMsg;	
		
		String [] cmd=new String[3];
		cmd[0]="/bin/bash";
		cmd[1]="-c";
		cmd[2]= "/bin/echo \""+mailContent+"\" | /bin/mail -s \""+mailSubject+"\" -r DVRS_ALERT "+mailReceiver;

		try{
			Process p = Runtime.getRuntime().exec (cmd);
			p.waitFor();
			if(logger!=null)
				logger.info("send mail cmd:"+cmd[2]);
			System.out.println("send mail cmd:"+cmd[2]);
		}catch (Exception e){
			if(logger!=null)
				logger.info("send mail fail:"+cmd[2]);
			System.out.println("send mail fail:"+cmd[2]);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}
	
	void sendMail(String mailSubject,String mailContent,String mailSender,String mailReceiver){
		String [] cmd=new String[3];
		cmd[0]="/bin/bash";
		cmd[1]="-c";
		cmd[2]= "/bin/echo \""+mailContent+"\" | /bin/mail -s \""+mailSubject+"\" -r "+mailSender+" "+mailReceiver;

		try{
			Process p = Runtime.getRuntime().exec (cmd);
			p.waitFor();
			if(logger!=null)
				logger.info("send mail cmd:"+cmd[2]);
			System.out.println("send mail cmd:"+cmd[2]);
		}catch (Exception e){
			if(logger!=null)
				logger.info("send mail fail:"+cmd[2]);
			System.out.println("send mail fail:"+cmd[2]);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}
	
	void sendHTMLMail(String mailSubject,String mailContent,String mailSender,String mailReceiver){
		String [] cmd=new String[3];
		cmd[0]="/bin/bash";
		cmd[1]="-c";
		cmd[2]= "echo \""+mailContent+"\" | mutt -s \""+mailSubject+"\"  -e \"set content_type=text/html\" "+mailReceiver+" -e 'my_hdr From:"+mailSender+"<local@localhost.com>'";

		try{
			Process p = Runtime.getRuntime().exec (cmd);
			p.waitFor();
			if(logger!=null)
				logger.info("send mail cmd:"+cmd[2]);
			System.out.println("send mail cmd:"+cmd[2]);
		}catch (Exception e){
			if(logger!=null)
				logger.info("send mail fail:"+cmd[2]);
			System.out.println("send mail fail:"+cmd[2]);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * 中斷數據後續追蹤
	 */
	private void processSuspendNBR() {
		for (Map<String,String> NBR : serviceOrderNBR) {
			String cMesg = "";
			try {
				for (int i = 0; i < 15; i++) {

					Thread.sleep(2000);

					sql = "select STATUS from S2T_TB_SERVICE_ORDER Where SERVICE_ORDER_NBR ='"
							+ NBR.get("cServiceOrderNBR") + "'";
					logger.info(sql);
					Statement st = conn.createStatement();
					ResultSet rs = st.executeQuery(sql);

					while (rs.next()) {
						cMesg = rs.getString("STATUS");
					}
					
					rs.close();
					st.close();

					logger.info("Query_ServiceOrderStatus:"
							+ Integer.toString(i) + " Times " + cMesg);

					if (cMesg.equals("Y") || cMesg.equals("F")) {
						break;
					}
				}

				if (cMesg.equals("Y") || cMesg.equals("F")) {
					cMesg = Query_SyncFileDtlStatus(NBR.get("cServiceOrderNBR"));
					if (cMesg.equals("")) {
						cMesg = "501";
					}
				} else {
					cMesg = "501";
				}
				
				//如果狀態更新失敗，沒有動作發送錯誤Email
				//20150317 change ,if result_flag is not equal to "000" then send alert mail.
				if(!"000".equalsIgnoreCase(cMesg)){
					ErrorHandle("Suspend does not work for"+"<br>"
							+ "IMSI : "+NBR.get("imsi")+"<br>"
							+ "MSISDN : "+NBR.get("msisdn")+"<br>"
							+ "WorkOrderNBR : "+NBR.get("cWorkOrderNBR")+"<br>"
							+ "ServiceOrderNBR : "+NBR.get("cServiceOrderNBR")+"<br>");
				}

				sql=
						"UPDATE HUR_SUSPEND_GPRS_LOG A "
						+ "SET A.RESULT='"+cMesg+"' "
						+ "WHERE A.SERVICE_ORDER_NBR='"+NBR.get("cServiceOrderNBR")+"'";
				
				Statement st2 = conn.createStatement();
				logger.info("Update HUR_SUSPEND_GPRS_LOG:"+sql);
				st2.executeUpdate(sql);
				st2.close();
				//conn.createStatement().executeUpdate(sql);
				
				
				//更新回Table
				SimpleDateFormat dFormat4=new SimpleDateFormat("yyyyMMddHHmmss");
				String dString=dFormat4.format(new Date());
				//PROVLOG 不需要
				/*sql = "update PROVLOG set replytime=sysdate where LOGID="
						+ sCMHKLOGID;

				logger.debug("Update PROVLOG:" + sSql);
				conn.createStatement().executeUpdateUpdate(sSql);*/

				sql = "update S2T_TB_TYPB_WO_SYNC_FILE_DTL set s2t_operationdate="
						+ "to_date('"+dString+"','YYYYMMDDHH24MISS')"
						+ " where WORK_ORDER_NBR='" + NBR.get("cWorkOrderNBR") + "'";

				logger.debug("update S2T_TB_TYPB_WO_SYNC_FILE_DTL:" + sql);
				//conn.createStatement().executeUpdate(sql);
				Statement st3 = conn.createStatement();
				st3.executeUpdate(sql);
				st3.close();

				sql = "update S2T_TB_SERVICE_ORDER_ITEM set timestamp="
						+ "to_date('"+dString+"','YYYYMMDDHH24MISS')"
						+ " where Service_Order_NBR='" + NBR.get("cServiceOrderNBR") + "'";

				logger.debug("Update S2T_TB_SERVICE_ORDER_ITEM:" + sql);
				//conn.createStatement().executeUpdate(sql);
				Statement st4 = conn.createStatement();
				st4.executeUpdate(sql);
				st4.close();

				sql = "update S2T_TB_SERVICE_ORDER set timestamp="
						+ "to_date('"+dString+"','YYYYMMDDHH24MISS')"
						+ " where SERVICE_ORDER_NBR='" + NBR.get("cServiceOrderNBR") + "'";

				logger.debug("Update S2T_TB_SERVICE_ORDER:" + sql);
				//conn.createStatement().executeUpdate(sql);
				Statement st5 = conn.createStatement();
				st5.executeUpdate(sql);
				st5.close();
				
			} catch (InterruptedException e) {
				sql="";
				ErrorHandle("At processSuspendNBR occur InterruptedException error!", e);
			} catch (SQLException e) {
				ErrorHandle("At processSuspendNBR occur SQLException error!", e);
			} catch (IOException e) {
				sql="";
				ErrorHandle("At processSuspendNBR occur IOException error!", e);
			}
		}
		serviceOrderNBR.clear();
	}
	
	/**
	 * 查詢中斷處理狀態
	 * @param cServiceOrderNBR
	 * @return
	 * @throws SQLException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public String Query_SyncFileDtlStatus(String cServiceOrderNBR) throws SQLException, InterruptedException, IOException{
		String cSt = "";
		for (int i = 0; i < 5; i++) {
			Thread.sleep(1000);
			sql = "select result_flag from S2T_TB_TYPB_WO_SYNC_FILE_DTL Where "
					+ "SERVICE_ORDER_NBR ='" + cServiceOrderNBR + "'";
			logger.info(sql);
			ResultSet rs = conn.createStatement().executeQuery(sql);
			while (rs.next()) {
				cSt = rs.getString("result_flag");
			}
			logger.info("Query_SyncFileDtlStatus:" + Integer.toString(i)
					+ " Times " + cSt);
			
			/*if("000".equals(cSt))
				break;*/
			if(cSt!=null && !"".equals(cSt))
				break;
		}
		return cSt;
    }
	boolean threadCheck = false;
	int errorTime = 30*60*1000;
	class ThreadWatcher implements Runnable {

		@Override
		public void run() {
			while(threadCheck){
				logger.info("threadCheck running...");
				try {
					Thread.sleep(5*60*1000);	
				} catch (InterruptedException e) {
				}
				
				if(System.currentTimeMillis()-subStartTime>=errorTime){
					ErrorHandle("Subprocess had ececuted longger than 30min.\n\nPlease check if the program is in normal.");
					// 進行DB連線
					try {

						try {
							conn.close();
						} catch (Exception e) {	
						}
						
						try {
							conn2.close();
						} catch (Exception e) {
						}
						
						
						connectDB();
						connectDB2();
						logger.info("reconnect success!");
					} catch (ClassNotFoundException e) {
						ErrorHandle("At reconnDB occur ClassNotFoundException error", e);
					} catch (SQLException e) {
						ErrorHandle("At reconnDB occur SQLException error", e);
					}
				}
			}
		}
		
	}
	
	public String getMSISDN(String serviceid){
		String result = null;
		
		if(msisdnMap.containsKey(serviceid))
			result = (String) msisdnMap.get(serviceid).get("MSISDN");;
		
		if(result!=null) return result;
		
		sql = ""
				+ "select SERVICECODE from service where serviceid = '"+serviceid+"'";

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			logger.debug("SQL : "+sql);
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				result = rs.getString("SERVICECODE");
			}
		} catch (SQLException e) {
			ErrorHandle("At set getMSISDN Got a SQLException", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}

		return result;
	}

	
	//*******************************Debug 工具*************************************//

	

	/*************************************************************************
	 *************************************************************************
	 *                                主程式
	 *************************************************************************
	 *************************************************************************/
	
	public static void regilarHandle(){
		

		Calendar c = Calendar.getInstance();
		
		if(c.get(Calendar.MINUTE)>=30)
			c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY)+1);
		
		c.set(Calendar.MINUTE, 30);
		c.set(Calendar.SECOND, 0);
		
		logger.info("First run Time:"+c.getTime());
		
		long runPeriod = 1000*60*60;
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new DVRSmain(), c.getTime(), runPeriod);
		//timer.scheduleAtFixedRate(new DVRSmain(), 0, runPeriod);
		
	}
	
	public void run(){
		//如果已有等待的thread，結束自己
		if(hasWaiting) {
			logger.debug("****************************      Found wating thread!");
			return;
		}
		//如果已經在進行中，暫停
		if(executing) {
			logger.debug("****************************      New Thread Wating... ");
			hasWaiting=true;
			while(executing){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					sql="";
					ErrorHandle("At waiting thread occur error", e);
				}
			}
			//離開等待狀態
			hasWaiting=false;
			logger.debug("****************************      Thread Leave Wating... ");
		}

		//開始執行程式
		executing=true;
		try {
			process();
		} catch (Exception e) {
			sql="";
			ErrorHandle("process error!",e);
		}finally{
			executing=false;
		}
	}
	
	String maxId = null,minId = null;
	
	public void getMaxAndMinUsageId() throws SQLException{
		
		//20170603 找出未處理的最大UsageID
		sql = "select max(usageId)  usageId from hur_data_usage where charge is null ";
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery(sql);
		if(rs.next()){
			maxId = rs.getString("usageId");
		}
		
		rs = null;
		
		//20170603 找出已處理的UsageId
		sql = "select max(usageId) usageId from hur_data_usage where charge is  not null ";
		rs = st.executeQuery(sql);
		if(rs.next()){
			minId = rs.getString("usageId");
		}
		
		logger.info("Max UsageId = "+maxId+",Min UsageId = "+minId);
	}
	
	long connectionTime1,connectionTime2;
	
	
	static boolean reloadMonth = true;
	static boolean reloadDay = true;
	static boolean reloadAddonData = true;
	static boolean reloadDayDetail = true;
	
	//static boolean checkPocketStart = false;
	//static boolean checkPocketEnd = false;
	static boolean endPocket = false;

	static boolean volumeReport = false;
	
	private void process(){
		/*int maxError = 1;
		int errorTimes = 0;*/
		
		long startTime = System.currentTimeMillis();//程式時間記錄開始
		logger.info("RFP Program Start! "+new Date());
		//20160706 add
		Thread subCheckThread = new Thread(new ThreadWatcher());
		threadCheck = true;
		errorTime = 30*60*1000;	//程式執行超過30分鐘時，提出警示
		subCheckThread.start();	//監控開始
		
		
		
		// 進行DB連線
		conn = null;
		conn2 = null;
		try {
			connectDB();
			connectDB2();
			logger.info("connect success!");
			
			
			setDayDate(); //設定日期
			
			getMaxAndMinUsageId();
			
			setQosResetSettingMap();
			setqosResetLogMap();
			setQosResetLogTimeMap();
			
					if(		setDataAnnounceMap()&& //20180305 add 數據通知設定			
							setUserDataAnnounceSet()&& //20180305 add 數據使用通知歷史紀錄設定
							setIMSItoServiceIDMap()&&//設定IMSI至ServiecId的對應，因時間較長，提到首個執行
							setSystemConfig()&&//系統Comfig設定
							setThreshold()&&//取出HUR_THRESHOLD
							setDataRate()&&//取出HUR_DATARATE
							setMsisdnMap()&&//取出msisdn資訊
							setIPtoMccmncList()&&//20141211 add IP 對應到 MCCMNC
							//簡訊
							getSMSsetting()&&
							getSMSContents()&&
							setCostomerNumber()&&//國碼對應表(客服,國名)
							//以SERVICEID 從登記之VLN找出用戶目前的MCCMNC
							setSERVICEIDtoVLN()&&//IMSI 對應到 vln
							setVLNtoTADIG()&&//vln 對應到 TADIG
							setTADIGtoMCCMNC()&&//TADIG 對應到 MCCMNC
							
							setAddonData()&&//華人上網包申請資料
							(!reloadAddonData ||setQosData())&&//設定SX001,SX002資料
							//setCurrencyMap()&&//設定PricePlanID對應幣別
							
							//20160624 add
							setSlowDownList()&&
							
							(!reloadMonth || setCurrentMap())&&//取出HUR_CURRENT，因為每次取造成資料有些許差異，於是決定只取第一次
							(!reloadDay || setCurrentMapDay())&&//取出HUR_CURRENT_DAY，因為每次取造成資料有些許差異，於是決定只取第一次
							(!reloadDayDetail || setCurrentDayDetailMap())&&//取出HUR_CURRENT_DAY_DETATIL
							setoldChargeMap()&&//設定old 20151027 modified update old Map every times
							//20160721 add
							//20170216 mod 美國流量包下架，鎖定Joy用戶(type=1)
							setVolumePocketMap()&& 
							true
							
							){
						
						//每月恢復速度
						if(resume) checkMonthlyResume();
						
						//每日降速恢復速度
						if(resumeSpeed)doResumeSpeed();

						//if(checkPocketStart)sendStartPocketDateSMS();
						
						//if(checkPocketEnd)sendEndPocketDateSMS();
						
						//流量包到期終止
						if(endPocket) doEndPocket();
						//20161115 cancel
						//if(volumeReport) sendVolumeReport();

						do{

							if(maxId!=null)
								charge();	//開始批價

							try {
								//警示提示
								ckeckMonthAlert(); //月警示
								checkDailyAlert(); //日警示
								addonVolumeAlert();//華人上網包降速提醒簡訊
								checkNTTVolumeAlert();//20160701 mod ，重新啟用NTT用量告警
								checkDailySlowDown();//20160624 對特定條件客戶進行降速
								checkVolumePocket();//20160721 add 美國流量包、JOY ，20170216，美國流量包下架
								
								//20180308 add
								checkDailyQosReset();

								/*if(TEST_MODE) {
									return;
								}*/
								//重新連線DB
								logger.info("reconnecting!");
								conn.close();
								conn2.close();
								connectDB();
								connectDB2();
								logger.info("connect success!");
								
								logger.info("set auto commit false!");
								//取消自動Commit
								cancelAutoCommit();
								
								
								if(
									updateCdr() &&//回寫批價結果
									insertCurrentMap() &&
									updateCurrentMap() &&
									insertCurrentMapDay()&&
									updateCurrentMapDay() &&
									insertCurrentDayDetailMap()&&
									updateCurrentDayDetailMap() &&
									updateVolumePocket() 
								){
									//20180308 add
									insertQosResetLog();
									//避免資料異常，完全處理完之後在commit
									conn.commit();
									
									//20180730 add
									queryCCT();
								
								}else {
									conn.rollback();
								}
								//suspend的後續追蹤處理
								processSuspendNBR();
								
							/*} catch (ParseException e) {
								//errorTimes++;
								ErrorHandle("At check occur ParseException error", e);
								maxId = null;*/
							}catch (Exception e) {
								ErrorHandle("At check occur ParseException error", e);
								maxId = null;
								//errorTimes++;
							}
						}while(maxId!=null && !minId.equals(maxId));
					}
		} catch (ClassNotFoundException e) {
			ErrorHandle("At connDB occur ClassNotFoundException error", e);
		} catch (SQLException e) {
			ErrorHandle("At connDB occur SQLException error", e);
		}finally{
			try {
				if(conn!=null) conn.close();
				if(conn2!=null) conn2.close();
			} catch (SQLException e) {}
			
			// 程式執行完成
			logger.info("Program execute time :" + (System.currentTimeMillis() - startTime));
			threadCheck = false;
		}
	}
	
	
	
	/*private void process(){
		
		long startTime = System.currentTimeMillis();//程式時間記錄開始
		
		logger.info("RFP Program Start! "+new Date());
		
		// 進行DB連線
		conn = null;
		conn2 = null;
		try {
			connectDB();
			connectDB2();
			logger.info("connect success!");
		} catch (ClassNotFoundException e) {
			ErrorHandle("At connDB occur ClassNotFoundException error", e);
		} catch (SQLException e) {
			ErrorHandle("At connDB occur SQLException error", e);
		}
			
		
		if (conn != null && conn2!=null) {
			//20160706 add
			Thread subCheckThread = new Thread(new ThreadWatcher());
			threadCheck = true;
			errorTime = 30*60*1000;	//程式執行超過30分鐘時，提出警示
			subCheckThread.start();	//監控開始
			
			try {
				if(
						
						setIMSItoServiceIDMap()&&//設定IMSI至ServiecId的對應，因時間較長，提到首個執行
						
						setDayDate() && //設定日期
						
						setQosData()&&//設定SX001,SX002資料
						
						setSystemConfig()&&//系統Comfig設定
						setThreshold()&&//取出HUR_THRESHOLD
						setDataRate()&&//取出HUR_DATARATE
						setMsisdnMap()&&//取出msisdn資訊
						setIPtoMccmncList()&&//20141211 add IP 對應到 MCCMNC
						getSMSsetting()&&
						getSMSContents()&&

						setCostomerNumber()&&//國碼對應表(客服,國名)
						
						//以SERVICEID 從登記之VLN找出用戶目前的MCCMNC
						setSERVICEIDtoVLN()&&//IMSI 對應到 vln
						setVLNtoTADIG()&&//vln 對應到 TADIG
						setTADIGtoMCCMNC()&&//TADIG 對應到 MCCMNC
						
						setAddonData()&&//華人上網包申請資料
						//setCurrencyMap()&&//設定PricePlanID對應幣別
						
						//20160624 add
						setSlowDownList()&&
						
						(!reloadMonth||setCurrentMap())&&//取出HUR_CURRENT，因為每次取造成資料有些許差異，於是決定只取第一次
						(!reloadDay||setCurrentMapDay())&&//取出HUR_CURRENT_DAY，因為每次取造成資料有些許差異，於是決定只取第一次
						setoldChargeMap()&&//設定old 20151027 modified update old Map every times
						//20160721 add
						//20170216 mod 美國流量包下架，鎖定Joy用戶(type=1)
						setVolumePocketMap()&& 
						true
						
						){

					

					//發送警示
					try {
						ckeckMonthAlert(); //月警示
						checkDailyAlert(); //日警示
						addonVolumeAlert();//華人上網包降速提醒簡訊
						checkNTTVolumeAlert();//20160701 mod ，重新啟用NTT用量告警
						checkDailySlowDown();//20160624 對特定條件客戶進行降速
						checkVolumePocket();//20160721 add 美國流量包、JOY ，20170216，美國流量包下架
						
						//取消自動Commit
						cancelAutoCommit();
						if(
								updateCdr()&&//回寫批價結果
								insertCurrentMap()&&
								updateCurrentMap()&&
								insertCurrentMapDay()&&
								updateCurrentMapDay()&&
								updateVolumePocket()){
							//避免資料異常，完全處理完之後在commit
							try {
								conn.commit();
							} catch (SQLException e) {
								try {
									conn.rollback();
								} catch (SQLException e1) {
								}
								ErrorHandle("At commit occur SQLException error!",e);
							}
						}else{
							//20160901
							conn.rollback();
						}
						//suspend的後續追蹤處理
						processSuspendNBR();
					} catch (Exception e) {
						sql="";
						ErrorHandle("At sendAlertSMS got Exception!",e);
					}
				}
			} catch (Exception e) {
				ErrorHandle("Main Process Error!!",e);
			}finally{
				// 程式執行完成
				logger.info("Program execute time :" + (System.currentTimeMillis() - startTime));
				closeConnect();
				threadCheck = false;
			}
		} else {
			sql="";
			ErrorHandle("Cannot connect to DB(connect is null)!!");
		}
	}	*/
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		IniProgram();
		 /*if(TEST_MODE){
			DVRSmain rf =new DVRSmain();			
			rf.process();
		}else{
			regilarHandle();
		}*/
		
		DVRSmain rf =new DVRSmain();			
		rf.process();

	}
}



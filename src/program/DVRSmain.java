
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


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class DVRSmain implements Job{

	
	//DB config
/*	private  final String DriverClass="oracle.jdbc.driver.OracleDriver";
	private  final String Host="10.42.1.101";
	private  final String Port="1521";
	private  final String ServiceName=":S2TBSDEV";
	private  final String UserName="foyadev";
	private  final String PassWord="foyadev";

	private  final String URL = "jdbc:oracle:thin:@"+ Host + ":"+Port+ServiceName; */
	
	//HUR
	Connection conn = null;
	//MBOSS
	Connection conn2 = null;
	
	private static  Logger logger ;
	static Properties props=new Properties();

	
	//mail conf
	static String mailSender="";
	static String mailReceiver="";
	static String mailSubject="mail test";
	static String mailContent="mail content text";
	
	static IJatool tool=new Jatool();
	SimpleDateFormat spf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	private static String sql="";
	private static String errorMsg="";
	
	//Hur Data conf
	private static Integer dataThreshold=null;//CDR資料一批次取出數量
	private static Integer lastfileID=null;//最後批價檔案號
	private static Double exchangeRate=null; //港幣對台幣匯率，暫訂為4
	private static Double kByte=null;//RATE單位KB，USAGE單位B
	
	//日期設定
	private String MONTH_FORMATE="yyyyMM";
	//系統時間，誤差一小時，系統資料處理時間為當時時間提前一小時
	private String sYearmonth="";
	private String sYearmonthday="";
	//上個月
	private String sYearmonth2="";
	private String DAY_FORMATE="yyyyMMdd";	
	
	//預設值
	private static int RUN_INTERVAL=3600;//單位秒
	private static String DEFAULT_MCCMNC=null;//預設mssmnc
	private static Double DEFAULT_THRESHOLD=null;//預設月警示量
	private static Double DEFAULT_DAY_THRESHOLD=null;//預設日警示量
	private static Double DEFAULT_DAYCAP=null;
	private static Double DEFAULT_VOLUME_THRESHOLD=null;//預設流量警示(降速)，1.5GB;
	private static Double DEFAULT_VOLUME_THRESHOLD2=null;//預設流量警示(降速)，15GB;
	private static String DEFAULT_PHONE=null;
	private static Boolean TEST_MODE=true;
	private static String HKNetReceiver;
	
	//多排程處理
	private static boolean executing =false;
	private static boolean hasWaiting = false;
	
	
	static Map<String,Map<String,Map<String,Object>>> currentMap = new HashMap<String,Map<String,Map<String,Object>>>();
	static Map<String,Map<String,Map<String,Map<String,Object>>>> currentDayMap = new HashMap<String,Map<String,Map<String,Map<String,Object>>>>();
	Map <String,Double> cdrChargeMap = new HashMap<String,Double>();
	static Map <String,Set<String>> existMap = new HashMap<String,Set<String>>();
	static Map <String,Map <String,Set<String>>> existMapD = new HashMap<String,Map <String,Set<String>>>();
	//20150505 add
	static Map <String,Set<String>> updateMap = new HashMap<String,Set<String>>();
	static Map <String,Map <String,Set<String>>> updateMapD = new HashMap<String,Map <String,Set<String>>>();
	
	
	Map<String,Map<String,List<Map<String,Object>>>> dataRate = new HashMap<String,Map<String,List<Map<String,Object>>>>();
	Map<String,Map<String,String>> msisdnMap = new HashMap<String,Map<String,String>>();
	Map<String,Double> thresholdMap = new HashMap<String,Double>();
	Map<String,String> SERVICEIDtoVLN =new HashMap<String,String>();
	Map<String,String> VLNtoTADIG =new HashMap<String,String>();
	Map<String,String> TADIGtoMCCMNC =new HashMap<String,String>();
	Map<String,Double> oldChargeMap = new HashMap<String,Double>();
	Map<String,Map<String,String>> codeMap = new HashMap<String,Map<String,String>>();
	List<Map<String,Object>> addonDataList = new ArrayList<Map<String,Object>>();
	Set<Map<String,String>> serviceOrderNBR = new HashSet<Map<String,String>>();
	Map<String,String> pricePlanIdtoCurrency = new HashMap<String,String>();
	List<Map<String,Object>> IPtoMccmncList = new ArrayList<Map<String,Object>>();
	Set<String> sSX001 = new HashSet<String>();
	Set<String> sSX002 = new HashSet<String>();
		
	/*************************************************************************
	 *************************************************************************
	 *                                程式參數設定
	 *************************************************************************
	 *************************************************************************/
	
	/**
	 * 設定計費週期
	 * 取特定日期那個月的，前面加上calendar.setTime(date);設定date日期
	 */
	private boolean setDayDate(){
		logger.info("setMonthDate...");
		long subStartTime = System.currentTimeMillis();

		//目前時間
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		
		//系統時間提前一小時
		calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY)-1);
		sYearmonth=tool.DateFormat(calendar.getTime(), MONTH_FORMATE);
		sYearmonthday=tool.DateFormat(calendar.getTime(),DAY_FORMATE);
		//上個月時間，減掉Month會-30天，採取到1號向前，確定跨月
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)-1);
		sYearmonth2=tool.DateFormat(calendar.getTime(), MONTH_FORMATE);
		
		calendar.clear();
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		return true;
	}
	
	/**
	 * 設定華人上網包MCCMNC資料 20141118 排除華人上網包
	 */
	private boolean setQosData(){
		logger.info("setQosData...");
		long subStartTime = System.currentTimeMillis();
		//sSX001
		sSX001.add("45412");
		
		//sSX002
		sSX002.add("46001");
		sSX002.add("46007");
		sSX002.add("46002");
		sSX002.add("460000");
		sSX002.add("46000");
		sSX002.add("45412");
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return true;
	}
	

	/*************************************************************************
	 *************************************************************************
	 *                                表格資料設定
	 *************************************************************************
	 *************************************************************************/

	/**
	 * 尋找最後一次更改的fileID，以及目標處理的最終ID
	 */
	private boolean setLastFileID(){
		logger.info("setLastFileID...");
		long subStartTime = System.currentTimeMillis();
		boolean result = false;
		
		Statement st = null;
		ResultSet rs = null;
		
		try {
			sql="SELECT MAX(A.LAST_FILEID) id FROM HUR_CURRENT A";
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				lastfileID=rs.getInt("id");
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
	}
	
	/**
	 * 取出 HUR_CURRENTE table資料
	 * 建立成
	 * Map 
	 * Key:MONTH,Value:Map(serviceid,Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_VOLUME)))
	 */
	private boolean setCurrentMap(){
		logger.info("setCurrentMap...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		
		//設定HUR_CURRENT計費，抓出這個月與下個月
		try {
			sql=
					"SELECT A.SERVICEID,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.LAST_DATA_TIME,A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,Object> map=new HashMap<String,Object>();
				Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();

				String serviceid =rs.getString("SERVICEID");
				String month=rs.getString("MONTH");
				
				if(currentMap.containsKey(month)){
					map2=currentMap.get(month);
				}
				
				map.put("LAST_FILEID", rs.getInt("LAST_FILEID"));
				map.put("SMS_TIMES", rs.getInt("SMS_TIMES"));
				map.put("LAST_DATA_TIME", (rs.getDate("LAST_DATA_TIME")!=null?rs.getDate("LAST_DATA_TIME"):new Date()));
				map.put("CHARGE", rs.getDouble("CHARGE"));
				map.put("VOLUME", rs.getDouble("VOLUME"));
				map.put("EVER_SUSPEND", rs.getString("EVER_SUSPEND"));
				map.put("LAST_ALERN_THRESHOLD", rs.getDouble("LAST_ALERN_THRESHOLD"));
				map.put("LAST_ALERN_VOLUME", rs.getDouble("LAST_ALERN_VOLUME"));

				map2.put(serviceid, map);
				currentMap.put(month,map2);
				
				
				
				//20141201 add 設定存在資料
				Set<String> set =new HashSet<String>();
				if(existMap.containsKey(month)){
					set=existMap.get(month);
				}
				set.add(serviceid);
				existMap.put(month, set);
			}
			result =true;
		} catch (SQLException e) {
			logger.error("At setCurrentMap occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setCurrentMap occur SQLException error!");
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
	 * 保留這個月舊資料，作為預測費用使用
	 */
	private boolean setoldChargeMap(){
		logger.info("setoldChargeMap...");
		long subStartTime = System.currentTimeMillis();
		
		if(currentMap.containsKey(sYearmonth)){
			for(String serviceid : currentMap.get(sYearmonth).keySet()){
				oldChargeMap.put(serviceid, (Double) currentMap.get(sYearmonth).get(serviceid).get("CHARGE"));
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		return true;
	}
	
	/**
	 * 取出 HUR_CURRENTE_DAY table資料
	 * 建立成
	 * Map 
	 * Key:day , value:Map(SERVICEID,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME,ALERT)))
	 * 設定HUR_CURRENT_DAY計費,目前不做刪除動作，之後考慮是否留2個月資料
	 * 20141209 修改取出近兩個月
	 */
	private boolean setCurrentMapDay(){
		logger.info("setCurrentMapDay...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result = false; 
		try {
			sql=
					"SELECT A.SERVICEID,A.CHARGE,A.LAST_FILEID,A.LAST_DATA_TIME,A.VOLUME,A.UPDATE_DATE,A.CREATE_DATE,A.MCCMNC,A.DAY,A.ALERT "
					+ "FROM HUR_CURRENT_DAY A "
					+ "WHERE SUBSTR(A.DAY,0,6) IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs =st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,Object> map=new HashMap<String,Object>();
				Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();			
				Map<String,Map<String,Map<String,Object>>> map3=new HashMap<String,Map<String,Map<String,Object>>>();
				
				String day =rs.getString("DAY");
				String serviceid =rs.getString("SERVICEID");
				String mccmnc=rs.getString("MCCMNC");
				
				if(day!=null && !"".equals(day) && 
						serviceid!=null && !"".equals(serviceid) &&
								mccmnc!=null && !"".equals(mccmnc)){
					if(currentDayMap.containsKey(day)){
						map3=currentDayMap.get(day);
						if(map.containsKey(serviceid)){
							map2=map3.get(serviceid);
						}
					}
							
					map.put("LAST_FILEID", rs.getInt("LAST_FILEID"));
					map.put("LAST_DATA_TIME", (rs.getDate("LAST_DATA_TIME")!=null?rs.getDate("LAST_DATA_TIME"):new Date()));
					map.put("CHARGE", rs.getDouble("CHARGE"));
					map.put("VOLUME", rs.getDouble("VOLUME"));
					map.put("ALERT", rs.getString("ALERT"));

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
			logger.error("At setCurrentMapDay occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setCurrentMapDay occur SQLException error!");
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
	 * 取出 HUR_DATA_RATE
	 * 建立成MAP Key:PRICEPLANID,Value:Map(MCCMNC,MAP(CURRENCY,CHARGEUNIT,RATE,NETWORK))
	 */
	//20150324 modify add network info
	private boolean setDataRate(){
		logger.info("setDataRate...");
		long subStartTime = System.currentTimeMillis();
		boolean result = false;
		Statement st = null;
		ResultSet rs = null;
		sql=""
				+ "SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY,A.PRICEPLANID,A.DAYCAP,B.NETWORK,"
				+ "       to_Date(A.START_TIME,'yyyy/MM/dd') START_TIME,"
				+ "       Case when A.END_TIME is null then null else to_date(A.END_TIME,'yyyy/MM/dd') END END_TIME "
				+ "FROM HUR_DATA_RATE A,HUR_MCCMNC B "
				+ "where A.MCCMNC=B.MCCMNC";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);

			while(rs.next()){

				String mccmnc =rs.getString("MCCMNC");
				String priceplanID =rs.getString("PRICEPLANID");
				
				Map<String,Object> map=new HashMap<String,Object>();
				Map<String,List<Map<String,Object>>> map2=new HashMap<String,List<Map<String,Object>>>();
				//20150427 Because of adding date data ,changing structure;
				List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
				
				map.put("RATE", rs.getDouble("RATE"));
				map.put("CHARGEUNIT", rs.getDouble("CHARGEUNIT"));
				map.put("CURRENCY", rs.getString("CURRENCY"));
				map.put("DAYCAP", rs.getDouble("DAYCAP"));
				map.put("NETWORK", rs.getString("NETWORK"));
				map.put("STARTTIME", rs.getDate("START_TIME"));
				if(rs.getString("END_TIME")!=null && !"".equals(rs.getDate("END_TIME")))
					map.put("ENDTIME", rs.getDate("END_TIME"));

				if(dataRate.containsKey(priceplanID)){
					map2=dataRate.get(priceplanID);
					if(map2.containsKey(mccmnc)){
						list=map2.get(mccmnc);
					}
				}
				
				list.add(map);
				map2.put(mccmnc, list);
				dataRate.put(priceplanID, map2);
			}
			result =true;
		} catch (SQLException e) {			
			logger.error("At setDataRate occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setDataRate occur SQLException errorr!");
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
	 * 取出HUR_THRESHOLD
	 * 建立MAP Key:IMSI,VALUE:THRESHOLD
	 * @return 
	 */
	private boolean setThreshold(){
		logger.info("setThreshold...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.SERVICEID,A.THRESHOLD "
				+"FROM HUR_GPRS_THRESHOLD A ";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);

			while(rs.next()){
				thresholdMap.put(rs.getString("SERVICEID"), rs.getDouble("THRESHOLD"));
			}
			result =true;
		} catch (SQLException e) {
			logger.error("At setThreshold occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setThreshold occur SQLException error!");
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
	 * 設定 IMSI 至 VLN 的對應表
	 * Map Key:IMSI,VALUE:VLN
	 */
	private boolean setSERVICEIDtoVLN(){
		logger.info("setSERVICEIDtoVLN...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.VLR_NUMBER,A.SERVICEID "
				+ "FROM UTCN.BASICPROFILE A "
				+ "WHERE A.VLR_NUMBER is not null ";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn2.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				SERVICEIDtoVLN.put(rs.getString("SERVICEID"), rs.getString("VLR_NUMBER"));
			}
			result =true;
		} catch (SQLException e) {
			logger.error("At setSERVICEIDtoVLN occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setSERVICEIDtoVLN occur SQLException error!");
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
	private boolean setVLNtoTADIG(){
		logger.info("setVLNtoTADIG...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result =false;
		sql=
				"SELECT B.REALMNAME TADIG, A.CHARGEAREACODE VLR "
				+ "FROM CHARGEAREACONFIG A, REALM B "
				+ "WHERE A.AREAREFERENCE=B.AREAREFERENCE";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn2.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				VLNtoTADIG.put(rs.getString("VLR"), rs.getString("TADIG"));
			}
			
			result =true;
		} catch (SQLException e) {			
			logger.error("At setVLNtoTADIG occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setVLNtoTADIG occur SQLException error!");
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
	private boolean setTADIGtoMCCMNC(){
		logger.info("setTADIGtoMCCMNC...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result = false;
		sql=
				"SELECT A.TADIG,A.MCCMNC "
				+ "FROM HUR_MCCMNC A ";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				TADIGtoMCCMNC.put(rs.getString("TADIG"), rs.getString("MCCMNC"));
			}
			result =true;
		} catch (SQLException e) {
			logger.error("At setTADIGtoMCCMNC occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setTADIGtoMCCMNC occur SQLException error!");
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
	private boolean setCostomerNumber(){
		logger.info("setCostomerNumber...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.CODE,A.PHONE,A.NAME "
				+ "FROM HUR_CUSTOMER_SERVICE_PHONE A";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> map = new HashMap<String,String>();
				map.put("PHONE", rs.getString("PHONE"));
				map.put("NAME", rs.getString("NAME"));
				codeMap.put(rs.getString("CODE"), map);
			}
			result = true;
		} catch (SQLException e) {
			logger.error("At setTADIGtoMCCMNC occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setTADIGtoMCCMNC occur SQLException error!");
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
	private int dataCount(){
		logger.info("dataCount...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		sql="SELECT COUNT(1) count  FROM HUR_DATA_USAGE A WHERE A.CHARGE is null ";
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
			logger.error("At dataCount occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At dataCount occur SQLException error!");
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
	}
	
	/**
	 * 取得預設計費比率（總費率平均），對MCCNOC有卻無法對應資料計費
	 * @return
	 */
	private double defaultRate(){
		logger.info("defaultRate...");
		long subStartTime = System.currentTimeMillis();
		
		double defaultRate=0.011;
		logger.info("defaultRate : " +defaultRate+" TWD ");
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return defaultRate;
	}
	
	/**
	 * 建立華人上網包對應資料
	 * 
	 * List Map KEY:MSISDN,VALUE(IMSI,MSISDN,SERVICECODE,STARTDATE,ENDDATE)>
	 */
	private boolean setAddonData(){
		logger.info("setAddonData...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		
		sql=
				"SELECT A.S2TIMSI IMSI,A.S2TMSISDN MSISDN,A.SERVICECODE,A.STARTDATE,A.ENDDATE "
				+ "FROM ADDONSERVICE_N A ";
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("IMSI", rs.getString("IMSI"));
				map.put("MSISDN", rs.getString("MSISDN"));
				map.put("SERVICECODE", rs.getString("SERVICECODE"));
				map.put("STARTDATE", rs.getDate("STARTDATE"));
				map.put("ENDDATE", rs.getDate("ENDDATE"));
				addonDataList.add(map);
			}
			result =true;
		} catch (SQLException e) {
			logger.error("At setTADIGtoMCCMNC occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setTADIGtoMCCMNC occur SQLException error!");
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
	private boolean setIPtoMccmncList(){
		logger.info("setIPtoMccmncList...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		try {
			sql=
					"SELECT A.START_NUM,A.END_NUM,A.MCCMNC "
					+ "FROM HUR_IP_RANGE A "
					+ "ORDER BY A.START_NUM ";
			logger.debug("Query AddonData SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);

			while(rs.next()){
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("START_NUM", rs.getLong("START_NUM"));
				map.put("END_NUM", rs.getLong("END_NUM"));
				map.put("MCCMNC", rs.getString("MCCMNC"));
				IPtoMccmncList.add(map);
			}
			result =true;
		} catch (SQLException e) {
			logger.error("At setIPtoMccmncList occur SQLException error", e);
			//send mail
			sendErrorMail("At setIPtoMccmncList occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
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
	private boolean setMsisdnMap(){
		logger.info("setMsisdnMap...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		sql=
				"SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,B.ICCID, "
				+ "(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE "
				+ "FROM SERVICE A,IMSI B,PARAMETERVALUE C "
				+ "WHERE A.SERVICEID=B.SERVICEID AND A.SERVICECODE IS NOT NULL "
				+ "AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748";
		
		try {
			logger.info("Execute SQL :"+sql);
			st = conn2.createStatement();
			rs=st.executeQuery(sql);
			
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
			logger.error("At setMsisdnMap occur SQLException error!", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At setMsisdnMap occur SQLException error!");
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
	
	Map<String,String> IMSItoServiceIdMap = new HashMap<String,String>();
	Map<String,String> ServiceIdtoIMSIMap = new HashMap<String,String>();
	/**
	 * 設定IMSI對應到ServiceID Map 20150115 add
	 * 
	 * 先從MSISDN MAP(IMSI table 資料找尋 SERVICEID)
	 * 找不到再從此Table(換卡記錄)找尋
	 */
	private boolean setIMSItoServiceIDMap(){
		logger.info("setIMSItoServiceIDMap...");
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result =false;
		sql=  ""
				+ "SELECT A.SERVICEID,A.IMSI"
				+ "		FROM("
				+ "			SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE"
				+ "			FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B"
				+ "		    WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713"
				+ "		    UNION"
				+ "		    SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE"
				+ "		    FROM SERVICEORDER A,NEWSERVICEORDERINFO B"
				+ "		    WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A,"
				+ "		    	(SELECT IMSI,MAX(COMPLETEDATE) COMPLETEDATE"
				+ "				from(SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE"
				+ "             		FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B"
				+ "                     WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713"
				+ "                     UNION"
				+ "                     SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE"
				+ "                     FROM SERVICEORDER A,NEWSERVICEORDERINFO B"
				+ "                     WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713)"
				+ "                     GROUP BY IMSI )B"
				+ "		WHERE A.IMSI=B.IMSI AND A.COMPLETEDATE =B.COMPLETEDATE";
		
		
		try {
			logger.info("Execute SQL :"+sql);
			st = conn2.createStatement();
			rs=st.executeQuery(sql);
			
			while(rs.next()){
				IMSItoServiceIdMap.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
			}
			result =true;
		} catch (SQLException e) {
			logger.error("At setIMSItoServiceIDMap occur SQLException error!", e);
			//send mail
			sendErrorMail("At setIMSItoServiceIDMap occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
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
	
	private boolean setServiceIDtoImsiMap(){
		logger.info("setServiceIDtoImsiMap...");
		long subStartTime = System.currentTimeMillis();
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
			logger.error("At setIMSItoServiceIDMap occur SQLException error!", e);
			//send mail
			sendErrorMail("At setIMSItoServiceIDMap occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
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
		String path=DVRSmain.class.getResource("").toString().replace("file:", "")+"/Log4j.properties";
		try {
			props.load(new   FileInputStream(path));
			PropertyConfigurator.configure(props);
			logger =Logger.getLogger(DVRSmain.class);
			logger.info("Logger Load Success!");

			DEFAULT_MCCMNC=props.getProperty("progrma.DEFAULT_MCCMNC");//預設mssmnc
			DEFAULT_THRESHOLD=(props.getProperty("progrma.DEFAULT_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_THRESHOLD")):5000D);//預設月警示量
			DEFAULT_DAY_THRESHOLD=(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")):500D);//預設日警示量
			DEFAULT_DAYCAP=(props.getProperty("progrma.DEFAULT_DAYCAP")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_DAYCAP")):500D);
			DEFAULT_VOLUME_THRESHOLD=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")):1.5*1024*1024D);//預設流量警示(降速)，1.5GB;
			DEFAULT_VOLUME_THRESHOLD2=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")):1.5*1024*1024D);//預設流量警示(降速)，15GB;
			DEFAULT_PHONE=props.getProperty("progrma.DEFAULT_PHONE");
			RUN_INTERVAL=(props.getProperty("progrma.RUN_INTERVAL")!=null?Integer.parseInt(props.getProperty("progrma.RUN_INTERVAL")):3600);
			HKNetReceiver = props.getProperty("program.HKNetReceiver");
			TEST_MODE=("true".equalsIgnoreCase(props.getProperty("progrma.TEST_MODE"))?true:false);
			
			dataThreshold=(props.getProperty("progrma.dataThreshold")!=null?Integer.parseInt(props.getProperty("progrma.dataThreshold")):500);//CDR資料一批次取出數量
			//lastfileID=(props.getProperty("progrma.lastfileID")!=null?Integer.parseInt(props.getProperty("progrma.lastfileID")):0);//最後批價檔案號
			exchangeRate=(props.getProperty("progrma.exchangeRate")!=null?Double.parseDouble(props.getProperty("progrma.exchangeRate")):4); //港幣對台幣匯率，暫訂為4
			kByte=(props.getProperty("progrma.kByte")!=null?Double.parseDouble(props.getProperty("progrma.kByte")):1/1024D);//RATE單位KB，USAGE單位B
			
			logger.info(
					"DEFAULT_MCCMNC : "+DEFAULT_MCCMNC+"\n"
					+ "DEFAULT_THRESHOLD : "+DEFAULT_THRESHOLD+"\n"
					+ "DEFAULT_DAY_THRESHOLD : "+DEFAULT_DAY_THRESHOLD+"\n"
					+ "DEFAULT_DAYCAP : "+DEFAULT_DAYCAP+"\n"
					+ "DEFAULT_VOLUME_THRESHOLD : "+DEFAULT_VOLUME_THRESHOLD+"\n"
					+ "DEFAULT_VOLUME_THRESHOLD2 : "+DEFAULT_VOLUME_THRESHOLD2+"\n"
					+ "DEFAULT_PHONE : "+DEFAULT_PHONE+"\n"
					+ "RUN_INTERVAL : "+RUN_INTERVAL+"\n"
					+ "HKNetReceiver : "+HKNetReceiver +"\n"
					+ "TEST_MODE : "+TEST_MODE+"\n"
					+ "dataThreshold : "+dataThreshold+"\n"
					//+ "lastfileID : "+lastfileID+"\n"
					+ "exchangeRate : "+exchangeRate+"\n"
					+ "kByte : "+kByte+"\n");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("File Path : "+path);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At loadProperties occur file not found error \n <br> file path="+path);
		} catch (IOException e) {
			e.printStackTrace();
			//send mail	
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At loadProperties occur IOException error !\n <br> file path="+path);
		}
	}
	
	/**
	 * 連線至DB1
	 */
	private void connectDB(){
		//conn=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		try {
			String url=props.getProperty("Oracle.URL")
					.replace("{{Host}}", props.getProperty("Oracle.Host"))
					.replace("{{Port}}", props.getProperty("Oracle.Port"))
					.replace("{{ServiceName}}", (props.getProperty("Oracle.ServiceName")!=null?props.getProperty("Oracle.ServiceName"):""))
					.replace("{{SID}}", (props.getProperty("Oracle.SID")!=null?props.getProperty("Oracle.SID"):""));
			
			conn=tool.connDB(logger, props.getProperty("Oracle.DriverClass"), url, 
					props.getProperty("Oracle.UserName"), 
					props.getProperty("Oracle.PassWord")
					);
			logger.info("Connect to "+url);
			
			
		} catch (ClassNotFoundException e) {
			sql="";
			logger.error("At connDB occur ClassNotFoundException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At connDB occur ClassNotFoundException error!");
		} catch (SQLException e) {
			sql="";
			logger.error("At connDB occur SQLException error", e);
			//send mail	
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At connDB occur SQLException error!");
		}
	}
	
	/**
	 * 連線至DB2
	 */
	private void connectDB2(){
		// 進行DB連線
		//conn2=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		try {
			String url=props.getProperty("mBOSS.URL")
					.replace("{{Host}}", props.getProperty("mBOSS.Host"))
					.replace("{{Port}}", props.getProperty("mBOSS.Port"))
					.replace("{{ServiceName}}", (props.getProperty("mBOSS.ServiceName")!=null?props.getProperty("mBOSS.ServiceName"):""))
					.replace("{{SID}}", (props.getProperty("mBOSS.SID")!=null?props.getProperty("mBOSS.SID"):""));
			
			conn2=tool.connDB(logger, props.getProperty("mBOSS.DriverClass"),url, 
					props.getProperty("mBOSS.UserName"), 
					props.getProperty("mBOSS.PassWord"));
			
			logger.info("Connrct to "+url);
		} catch (ClassNotFoundException e) {
			sql="";
			logger.error("At connDB2 occur ClassNotFoundException error", e);
			//send mail
			sendErrorMail("At connDB2 occur ClassNotFoundException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}	
		} catch (SQLException e) {
			sql="";
			logger.error("At connDB2 occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At connDB2 occur SQLException error!");
		}
	}
	
	/**
	 * 關閉連線
	 */
	private void closeConnect() {
		
		if (conn != null) {

			try {
				logger.info("closeConnect1...");
				conn.close();
			} catch (SQLException e) {
				logger.error("close Connect Error", e);
				//send mail
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
				sendErrorMail("At closeConnect occur SQLException error!");
			}

		}
		
		if (conn2 != null) {

			try {
				logger.info("closeConnect2...");
				conn2.close();
			} catch (SQLException e) {
				logger.error("close Connect2 Error", e);
				//send mail
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
				sendErrorMail("At closeConnect2 occur SQLException error!");
			}

		}
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
			logger.error("At cancelAutoCommit occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At cancelAutoCommit occur SQLException error!");
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
	 */
	private int checkQosAddon(String imsi,String mccmnc,Date callTime){
		

		int cd=0;
		
		//String msisdn=null;
		if(msisdnMap.containsKey(imsi)) {
			//msisdn=msisdnMap.get(imsi).get("MSISDN");
			
			//只有香港
			if(cd==0 && sSX001.contains(mccmnc)){
				for(Map<String,Object> m : addonDataList){
					if(imsi.equals(m.get("IMSI"))&&"SX001".equals(m.get("SERVICECODE"))&&
							(callTime.after((Date) m.get("STARTDATE"))||callTime.equals((Date) m.get("STARTDATE")))&&
							(m.get("ENDDATE")==null ||callTime.before((Date) m.get("ENDDATE")))){
						cd=1;
						break;
					}
				}
				
			}
			
			//香港加中國大陸
			if(cd==0 && sSX002.contains(mccmnc)){
				for(Map<String,Object> m : addonDataList){
					if(imsi.equals(m.get("IMSI"))&&"SX002".equals(m.get("SERVICECODE"))&&
							(callTime.after((Date) m.get("STARTDATE"))||callTime.equals((Date) m.get("STARTDATE")))&&(m.get("ENDDATE")==null ||callTime.before((Date) m.get("ENDDATE")))){
						cd=1;
						break;
					}
				}
				
			}
		}
		
		return cd;
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
				ipNumber+=Integer.parseInt(ips[j])*Math.pow(256, 3-j);
			}
			System.out.println("ipNumber="+ipNumber);
			
			for(Map<String,Object> m : IPtoMccmncList){
				long startNum = (Long) m.get("START_NUM");
				long EndNum = (Long) m.get("END_NUM");
				
				if(startNum <= ipNumber && ipNumber <= EndNum){
					mccmnc = (String) m.get("MCCMNC");
					break;
				}
			}
		}
		return mccmnc;
	}
	
	/**
	 * 開始批價
	 */
	private void charge(){
		logger.info("charge...");
		long subStartTime = System.currentTimeMillis();
		
		updateMap.clear();
		updateMapD.clear();
		
		Statement st = null;
		ResultSet rs = null;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		int count=0;
		double defaultRate=0;
		try {
			count=dataCount();
			defaultRate=defaultRate();
			setQosData();

			//批次Query 避免ram空間不足
			for(int i=1;(i-1)*dataThreshold+1<=count ;i++){
				sql=
						"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME,SGSNADDRESS "
						+ "FROM HUR_DATA_USAGE A WHERE ROWNUM <= "+(i*dataThreshold)+" AND A.CHARGE is null "
						//+ "AND A.FILEID>= "+lastfileID+" "
						+ "ORDER BY A.USAGEID,A.FILEID) "
						+ "MINUS "
						+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME,SGSNADDRESS "
						+ "FROM HUR_DATA_USAGE A WHERE ROWNUM <= "+((i-1)*dataThreshold)+" AND A.CHARGE is null "
						//+ "AND A.FILEID>= "+lastfileID+" "
						+ "ORDER BY A.USAGEID,A.FILEID) ";
				
				logger.debug("Execute SQL : "+sql);
				
				logger.debug("Round "+i+" Procsess ...");
				st = conn.createStatement();
				rs = st.executeQuery(sql);

				while(rs.next()){			
					String logMsg="";
					
					String imsi= rs.getString("IMSI");
					String mccmnc=rs.getString("MCCMNC");
					String usageId=rs.getString("USAGEID");
					Double volume=rs.getDouble("DATAVOLUME");
					Date callTime=rs.getDate("CALLTIME");
					Integer fileID=rs.getInt("FILEID");	
					Double charge=0D;
					Double dayCap=null;
										
					
					//20141211 add
					String ipaddr = rs.getString("SGSNADDRESS");
					
					//20150115 add
					String serviceid = null;
					if(msisdnMap.get(imsi)!=null)
						serviceid = msisdnMap.get(imsi).get("SERVICEID");
					
					if(serviceid==null || "".equals(serviceid))
						serviceid = IMSItoServiceIdMap.get(imsi);
					
					if(serviceid==null || "".equals(serviceid)){
						sendErrorMail("For CDR usageId="+usageId+" which can't find  ServceID." );
						continue;
					}
					
					
					String pricplanID=null;
					if(msisdnMap.containsKey(imsi))
						pricplanID=msisdnMap.get(imsi).get("PRICEPLANID");
					
					
					if(!dataRate.containsKey(pricplanID)){
						sql="";errorMsg="";
						logger.debug("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
						sendErrorMail("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
						
					}
					
					
					//20141211 add
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc=searchMccmncByIP(ipaddr);
						if(mccmnc!=null && !"".equals(mccmnc))
							logger.debug("For CDR usageId="+usageId+" which is without mccmnc. Found mccmnc="+mccmnc+" by IP range.");
					}
					
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc=searchMccmncBySERVICEID(serviceid);
						if(mccmnc!=null && !"".equals(mccmnc))
							logger.debug("For CDR usageId="+usageId+" which is without mccmnc. Found mccmnc="+mccmnc+" by serviceid.");
					}

					//還是找不到，給予預設，必須有
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc= DEFAULT_MCCMNC;
						logger.debug("usageId:"+usageId+" set mccmnc to default!");
					}
					
					int cd=checkQosAddon(imsi, mccmnc, callTime);
					if(cd==0){
						//判斷是否可以找到對應的費率表，並計算此筆CDR的價格(charge)
						if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
								dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
							
							double ec=1;
							
							boolean haveRate = false;
							
							List<Map<String,Object>> rateList = new ArrayList<Map<String,Object>>();
							rateList=dataRate.get(pricplanID).get(mccmnc);
							for(Map<String,Object> m : rateList){
								Date sdate = (Date) m.get("STARTTIME");
								Date edate = (Date) m.get("ENDTIME");
								if(sdate.equals(callTime)||sdate.before(callTime)&&(edate==null || edate.after(callTime))){
									String currency=(String) m.get("CURRENCY");
									if("HKD".equalsIgnoreCase(currency))
										ec=exchangeRate;
										
									Double rate=(Double)m.get("RATE");
									Double unit=(Double)m.get("CHARGEUNIT");
									charge=Math.ceil(volume*kByte)*rate*ec/unit;
									dayCap=(Double)m.get("DAYCAP");
									haveRate=true;
									break;
								}
							}		
							if(!haveRate){
								sql="";errorMsg="";
								logger.error("usageId:"+usageId+",CALLTIME:"+callTime.toString()+" can't charge correctly without Rate table ! ");
								sendErrorMail("usageId:"+usageId+",CALLTIME:"+callTime.toString()+" can't charge correctly without Rate table ! ");
								continue;
							}
							/*
							String currency=(String) dataRate.get(pricplanID).get(mccmnc).get("CURRENCY");
							if("HKD".equalsIgnoreCase(currency))
								ec=exchangeRate;
								
							Double rate=(Double)dataRate.get(pricplanID).get(mccmnc).get("RATE");
							Double unit=(Double)dataRate.get(pricplanID).get(mccmnc).get("CHARGEUNIT");
							charge=Math.ceil(volume*kByte)*rate*ec/unit;
							dayCap=(Double)dataRate.get(pricplanID).get(mccmnc).get("DAYCAP");*/
							
						}else{
							
							//沒有PRICEPLANID(月租方案)，MCCMNC，無法判斷區域業者，作法：統計流量，
							//沒有對應的PRICEPLANID(月租方案)，MCCMNC，無法判斷區域業者
							//以預設費率計費
							sql="";errorMsg="";
							logger.error("usageId:"+usageId+",IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");
							sendErrorMail("usageId:"+usageId+",IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");
							charge=Math.ceil(volume*kByte)*defaultRate;
						}
					}
		
					//XXX
					/*if("454120260226967".equals(imsi))
						System.out.println("check point");
					*/
					
					//格式化至小數點後四位
					charge=tool.FormatDouble(charge, "0.0000");

					//將此筆CDR結果記錄，稍後回寫到USAGE TABLE
					cdrChargeMap.put(usageId, charge);
					logMsg+="UsageId "+usageId+" ,IMSI "+imsi+" ,MCCMNC "+mccmnc+" charge result is "+cdrChargeMap.get(usageId)+". ";

					
					//察看是否有以存在的資料，有的話取出做累加
					//20150324 modify mccmnc to mcc + network
					
					
					String nccNet;
					if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
							dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
						System.out.println(mccmnc);
						nccNet=mccmnc.substring(0,3);
						nccNet+=dataRate.get(pricplanID).get(mccmnc).get(0).get("NETWORK");
					}else{
						nccNet=DEFAULT_MCCMNC;
					}
					
					String cDay=tool.DateFormat(callTime, DAY_FORMATE);
					Double oldCharge=0D;
					Double oldvolume=0D;
					String alert="0";
					Map<String,Map<String,Map<String,Object>>> map=new HashMap<String,Map<String,Map<String,Object>>>();
					Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();
					Map<String,Object> map3=new HashMap<String,Object>();
					
					if(currentDayMap.containsKey(cDay)){
						map=currentDayMap.get(cDay);
						if(map.containsKey(serviceid)){
							map2=map.get(serviceid);
							if( map2.containsKey(nccNet)){
								map3=map2.get(nccNet);

								oldCharge=(Double)map3.get("CHARGE");
								
								logMsg+="The old Daily charge is "+oldCharge+". ";
								
								//summary charge
								charge=oldCharge+charge;
								charge=tool.FormatDouble(charge, "0.0000");
								if(serviceid.equals("26234"))
									System.out.println();
								alert=(String)map3.get("ALERT");
								oldvolume=(Double)map3.get("VOLUME");
							
								if(fileID<(Integer) map3.get("LAST_FILEID"))
									fileID=(Integer) map3.get("LAST_FILEID");
							}
						}
					}

					//如果有計費上線，限制最大值  20141125 取消預設Daycap，如果值為負，表示沒有
					//if(dayCap==null || dayCap==0) dayCap= DEFAULT_DAYCAP;
					if(dayCap!=null && dayCap>=0 && charge>dayCap) charge=dayCap;
					
					//將結果記錄到currentDayMap
					map3.put("CHARGE", charge);
					logMsg+="The final Daily charge is "+map3.get("CHARGE")+". ";
					map3.put("LAST_FILEID",fileID);
					map3.put("LAST_DATA_TIME",callTime);
					map3.put("VOLUME",volume+oldvolume);
					map3.put("ALERT",alert);
					map2.put(nccNet, map3);
					map.put(serviceid, map2);
					currentDayMap.put(cDay, map);
					
					//20150505 add
					Map <String,Set<String>> map6 = new HashMap<String,Set<String>>();
					Set<String> set1 = new HashSet<String>();					
					
					if(updateMapD.containsKey(cDay)){	
						map6 = updateMapD.get(cDay);
						if(map6.containsKey(serviceid)){
							set1 = map6.get(serviceid);
						}
					}
					set1.add(nccNet);
					map6.put(serviceid, set1);
					updateMapD.put(cDay, map6);
					

					//更新currentMap，如果此筆CDR記錄時間的月紀錄存在
					Double preCharge=0D;
					Integer smsTimes=0;
					String suspend="0";
					String cMonth=cDay.substring(0,6);
					Double lastAlertThreshold=0D;
					Double volumeAlert=0D;
					
					Map<String,Object> map4=new HashMap<String,Object>();
					Map<String,Map<String,Object>> map5=new HashMap<String,Map<String,Object>>();
					
					if(currentMap.containsKey(cMonth)){
						map5=currentMap.get(cMonth);
					}
					
					if(map5.containsKey(serviceid)){
						map4=map5.get(serviceid);
						
						preCharge=(Double)map4.get("CHARGE")-oldCharge;
						
						logMsg+="The old month charge is "+(Double)map4.get("CHARGE")+". ";
						smsTimes=(Integer) map4.get("SMS_TIMES");
						suspend=(String) map4.get("EVER_SUSPEND");
						volume=(Double)map4.get("VOLUME")+volume;
						lastAlertThreshold=(Double) map4.get("LAST_ALERN_THRESHOLD");
						volumeAlert=(Double) map4.get("LAST_ALERN_VOLUME");
						
						if(fileID<(Integer) map4.get("LAST_FILEID"))
							fileID=(Integer) map4.get("LAST_FILEID");
					}
					
					
					map4.put("LAST_FILEID", fileID);
					map4.put("SMS_TIMES", smsTimes);
					map4.put("LAST_DATA_TIME", callTime);
					
					charge=preCharge+charge;
					charge=tool.FormatDouble(charge, "0.0000");
					map4.put("CHARGE", charge);
					logMsg+="The final month charge is "+map4.get("CHARGE")+". ";
					
					map4.put("VOLUME", volume);
					map4.put("EVER_SUSPEND", suspend);
					map4.put("LAST_ALERN_THRESHOLD", lastAlertThreshold);
					map4.put("LAST_ALERN_VOLUME", volumeAlert);
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
			logger.error("At charge occur SQLException error", e);
			//send mail
			sendErrorMail("At charge occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
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
	
	/***
	 * 回寫CDR的CHARGE
	 */
	private boolean updateCdr(){
		logger.info("updateCdr...");
		long subStartTime = System.currentTimeMillis();
		
		boolean result =false;
		int count=0;
		
		Statement st = null;

		try {
			st = conn.createStatement();
			for(String s: cdrChargeMap.keySet()){
				sql=
						"UPDATE HUR_DATA_USAGE A "
						+ "SET A.CHARGE="+cdrChargeMap.get(s)+" "
						+ "WHERE A.USAGEID='"+s+"' ";
				
				//logger.info("Execute Sql: "+sql);
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
			logger.error("At updateCdr occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At updateCdr occur SQLException error!");
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

	/**
	 * 計算完畢後寫回資料庫-更新
	 * @throws SQLException 
	 */
	private boolean updateCurrentMap(){
		logger.info("updateCurrentMap...");
		long subStartTime = System.currentTimeMillis();

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
			
			//20141201 add change another method to distinguish insert and update
			/*for(String mon : currentMap.keySet()){
				for(String serviceid : currentMap.get(mon).keySet()){
					pst.setDouble(1,(Double) currentMap.get(mon).get(serviceid).get("CHARGE"));
					pst.setInt(2, (Integer) currentMap.get(mon).get(serviceid).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMap.get(mon).get(serviceid).get("SMS_TIMES"));
					pst.setString(4, spf.format((Date) currentMap.get(mon).get(serviceid).get("LAST_DATA_TIME")));
					pst.setDouble(5,(Double) currentMap.get(mon).get(serviceid).get("VOLUME"));
					pst.setString(6,(String) currentMap.get(mon).get(serviceid).get("EVER_SUSPEND"));
					pst.setDouble(7,(Double) currentMap.get(mon).get(serviceid).get("LAST_ALERN_THRESHOLD"));
					pst.setDouble(8,(Double) currentMap.get(mon).get(serviceid).get("LAST_ALERN_VOLUME"));
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
			}*/
			//20150505 add only update are modified
			for(String mon : updateMap.keySet()){
				for(String serviceid : updateMap.get(mon)){
					pst.setDouble(1,(Double) currentMap.get(mon).get(serviceid).get("CHARGE"));
					pst.setInt(2, (Integer) currentMap.get(mon).get(serviceid).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMap.get(mon).get(serviceid).get("SMS_TIMES"));
					pst.setString(4, spf.format((Date) currentMap.get(mon).get(serviceid).get("LAST_DATA_TIME")));
					pst.setDouble(5,(Double) currentMap.get(mon).get(serviceid).get("VOLUME"));
					pst.setString(6,(String) currentMap.get(mon).get(serviceid).get("EVER_SUSPEND"));
					pst.setDouble(7,(Double) currentMap.get(mon).get(serviceid).get("LAST_ALERN_THRESHOLD"));
					pst.setDouble(8,(Double) currentMap.get(mon).get(serviceid).get("LAST_ALERN_VOLUME"));
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
			logger.error("At updateCurrentMap occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At updateCurrentMap occur SQLException error!");
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
		long subStartTime = System.currentTimeMillis();

		int count=0;
		boolean result= false;
		sql=
				"UPDATE HUR_CURRENT_DAY A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.LAST_DATA_TIME=TO_DATE(?,'yyyy/MM/dd hh24:mi:ss'),A.VOLUME=?,A.ALERT=?,A.UPDATE_DATE=SYSDATE "
				+ "WHERE A.DAY=? AND A.SERVICEID=? AND A.MCCMNC=? ";
		
		logger.info("Execute SQL :"+sql);
		
		PreparedStatement pst = null;
		try {
			pst = conn.prepareStatement(sql);
			
			//20141201 add change another method to distinguish insert and update
			for(String day : updateMapD.keySet()){
				for(String serviceid : updateMapD.get(day).keySet()){
					for(String nccNet : updateMapD.get(day).get(serviceid)){
						pst.setDouble(1,(Double) currentDayMap.get(day).get(serviceid).get(nccNet).get("CHARGE"));
						pst.setInt(2, (Integer) currentDayMap.get(day).get(serviceid).get(nccNet).get("LAST_FILEID"));
						pst.setString(3, spf.format((Date) currentDayMap.get(day).get(serviceid).get(nccNet).get("LAST_DATA_TIME")));
						pst.setDouble(4,(Double) currentDayMap.get(day).get(serviceid).get(nccNet).get("VOLUME"));
						pst.setString(5,(String) currentDayMap.get(day).get(serviceid).get(nccNet).get("ALERT"));
						pst.setString(6, day);
						pst.setString(7, serviceid);
						pst.setString(8, nccNet);
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
			logger.error("At updateCurrentMapU occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At updateCurrentMapU occur SQLException error!");
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
		long subStartTime = System.currentTimeMillis();
		
		boolean result = false;
		int count = 0 ;
		sql=
				"INSERT INTO HUR_CURRENT "
				+ "(SERVICEID,MONTH,CREATE_DATE) "
				+ "VALUES(?,?,SYSDATE)";
		
		logger.info("Execute SQL :"+sql);
		
		PreparedStatement pst = null;
		
		try {
			pst = conn.prepareStatement(sql);
		
			/*for(String mon : insertMap.keySet()){
				for(String imsi : insertMap.get(mon)){*/
			//20141201 add change another method to distinguish insert and update
			for(String mon : currentMap.keySet()){
				for(String serviceid : currentMap.get(mon).keySet()){
					if(!existMap.containsKey(mon) || !existMap.get(mon).contains(serviceid)){
						pst.setString(1, serviceid);		
						pst.setString(2, mon);
						pst.addBatch();
					
						//20141229 alter insert data before update
						Set<String> set=new HashSet<String>();
						if(existMap.containsKey(mon))
							set=existMap.get(mon);
						
						set.add(serviceid);
						existMap.put(mon, set);
						
						count++;
						if(count==dataThreshold){
							logger.info("Execute insertCurrentMap Batch");
							pst.executeBatch();
							count=0;
						}
					}
				}
			}
			if(count!=0){
				logger.info("Execute insertCurrentMap Batch");
				pst.executeBatch();
			}
			result = true;
		} catch (SQLException e) {
			logger.error("At insertCurrent occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At insertCurrent occur SQLException error!");
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
	
	private boolean insertCurrentMapDay(){
		logger.info("insertCurrentMapDay...");
		long subStartTime = System.currentTimeMillis();

		boolean result = false;
		int count = 0;
		sql=
				"INSERT INTO HUR_CURRENT_DAY "
				+ "(SERVICEID,CREATE_DATE,MCCMNC,DAY) "
				+ "VALUES(?,SYSDATE,?,?)";
		
		logger.info("Execute SQL :"+sql);

		PreparedStatement pst = null;
		
		try {
			pst = conn.prepareStatement(sql);
			//20141201 add change another method to distinguish insert and update
			for(String day : currentDayMap.keySet()){
				for(String serviceid : currentDayMap.get(day).keySet()){
					for(String nccNet : currentDayMap.get(day).get(serviceid).keySet()){
						if(!existMapD.containsKey(day)||!existMapD.get(day).containsKey(serviceid)||!existMapD.get(day).get(serviceid).contains(nccNet)){
							pst.setString(1, serviceid);
							pst.setString(2, nccNet);
							pst.setString(3, day);
							pst.addBatch();
						
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
							
							count++;
							if(count==dataThreshold){
								logger.info("Execute insertCurrentMapDay Batch");
								pst.executeBatch();
								count=0;
							}
						}
					}
				}
			}
			if(count!=0){
				logger.info("Execute insertCurrentMapDay Batch");
				pst.executeBatch();
			}
			result = true;
		} catch (SQLException e) {
			logger.error("At insertCurrentDay occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At insertCurrentDay occur SQLException error!");
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
	 * 依照額度需求發送警示簡訊
	 * 第一次，額度一，訊息一
	 * 
	 * 
	 */
	private void sendAlertSMS(){
		logger.info("sendAlertSMS...");
		long subStartTime = System.currentTimeMillis();
		
		List<Integer> times=new ArrayList<Integer>();
		List<Double> bracket=new ArrayList<Double>();
		List<String> msg=new ArrayList<String>();
		List<String> suspend=new ArrayList<String>();
		
		int smsCount=0;
		String phone=null;
		
		Statement st = null;
		ResultSet rs = null;
		//載入簡訊設定
		
		try {
			sql="SELECT A.ID,A.BRACKET,A.MEGID,A.SUSPEND FROM HUR_SMS_SETTING A ORDER BY ID DESC";	
			
			
			st = conn.createStatement();
			logger.debug("Execute SQL:"+sql);
			rs=st.executeQuery(sql);
			
			while(rs.next()){
				times.add(rs.getInt("ID"));
				bracket.add(rs.getDouble("BRACKET"));
				msg.add(rs.getString("MEGID"));
				suspend.add(rs.getString("SUSPEND"));
				logger.info("times:"+times.get(times.size()-1)+",bracket:"+bracket.get(bracket.size()-1)+",msg:"+msg.get(msg.size()-1)+",suspend:"+suspend.get(suspend.size()-1));
			}
		} catch (SQLException e) {
			logger.error("At sendAlertSMS:Load SMSSetting occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At sendAlertSMS:Load SMSSetting occur SQLException error!");
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
			logger.error("No SMS Setting!");
			//sendErrorMail
			sql="";errorMsg="";
			sendErrorMail("Can't found SMS Setting!");
			return;
		}
		
		//載入簡訊內容
		
		Map<String,Map<String,String>> content=new HashMap<String,Map<String,String>>();
		
		try {
			sql=
					"SELECT A.ID,A.CONTENT,A.CHARSET "
					+ "FROM HUR_SMS_CONTENT A ";	

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
			logger.error("At sendAlertSMS:Load SMSContent occur SQLException error", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At sendAlertSMS:Load SMSContent occur SQLException error!");
		} finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		
		if(content.size()==0){
			logger.error("No SMS content!");
			sql="";errorMsg="";
			sendErrorMail("Can't found SMS Content sentting!");
			return;
		}
		
		
		//開始檢查是否發送警示簡訊
		//月金額警示*************************************
		//沒有當月資料，不檢查
		if(currentMap.containsKey(sYearmonth)){
			smsCount=0;
			try {
				st = conn.createStatement();
				//檢查這個月的資料作警示通知
				for(String serviceid: currentMap.get(sYearmonth).keySet()){
					
					//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
					if(msisdnMap.containsKey(serviceid))
						phone=(String) msisdnMap.get(serviceid).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendErrorMail
						sendErrorMail("At sendAlertSMS occur error! The serviceid:"+serviceid+" can't find msisdn to send !");
						logger.error("The serviceid:"+serviceid+" can't find msisdn to send! ");
						continue;
					}
					
					String res="";
					
					Double charge=(Double) currentMap.get(sYearmonth).get(serviceid).get("CHARGE");
					Double oldCharge=(Double) oldChargeMap.get(serviceid);
					if(oldCharge==null)	oldCharge=0D;
	
					Double differenceCharge=charge-oldCharge;
					int smsTimes=(Integer) currentMap.get(sYearmonth).get(serviceid).get("SMS_TIMES");
					String everSuspend =(String) currentMap.get(sYearmonth).get(serviceid).get("EVER_SUSPEND");
					Double lastAlernThreshold=(Double) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_THRESHOLD");
					boolean isCustomized=false;
					
					//20141118 修改 約定客戶訂為每5000提醒一次不斷網
					Double threshold=thresholdMap.get(serviceid);

					if(threshold==null){
						threshold=DEFAULT_THRESHOLD;
					}else{
						isCustomized=true;
					}
					if(lastAlernThreshold==null)
						lastAlernThreshold=0D;
					
					int i=0;
					boolean sendSMS=false;
					boolean needSuspend=false;
					Double alertBracket=0D;
					String[] contentid=null;
					
					//20141118 修改 約定客戶訂為每5000提醒一次不斷網，規則客制訂為0進行5000持續累積
					if(threshold!=0D){
						//檢查月用量
						for(;i<times.size();i++){
							if(((charge>=bracket.get(i)*threshold))&&lastAlernThreshold<bracket.get(i)*threshold){
								sendSMS=true;
								alertBracket=bracket.get(i)*threshold;
								contentid=msg.get(i).split(",");
								if("1".equals(suspend.get(i)))
									needSuspend=true;
								break;
							}
						}	
						
						//檢查預測用量，如果之前判斷不用發簡訊，或不是發最上限簡訊
						if(!sendSMS||(sendSMS && i!=0)){
							if(charge+differenceCharge>=bracket.get(0)*threshold&&lastAlernThreshold<bracket.get(0)*threshold){
								logger.info("For "+serviceid+" add charge "+differenceCharge+" in this hour ,System forecast the next hour will over charge limit");
								sendSMS=true;
								alertBracket=bracket.get(0)*threshold;
								contentid=msg.get(0).split(",");
								if("1".equals(suspend.get(0))){
									needSuspend=true;
								}else{
									needSuspend=false;
								}
								i=0;
							}
						}
					}else{
						int temp=(int) ((int)(charge/DEFAULT_THRESHOLD)*DEFAULT_THRESHOLD);
						
						if(temp>lastAlernThreshold){
							alertBracket=(double) temp;
							sendSMS=true;
							contentid=new String[]{"3"};
						}
					}
					

					//寄送簡訊
					if(sendSMS){
						//查詢所在國家的客服電話
						String cPhone = null;
						String nMccmnc=searchMccmncBySERVICEID(serviceid);
						Map<String,String> map=null;
						
						if(nMccmnc!=null && !"".equals(nMccmnc))
							map = codeMap.get(nMccmnc.substring(0,3));
						if(map!=null)
							cPhone=map.get("PHONE");
						
						for(String s:contentid){
							if(s!=null){
								//寄送簡訊
								lastAlernThreshold=alertBracket;
								smsTimes++;
								logger.info("For "+serviceid+" send "+smsTimes+"th message:"+msg.get(i));
								String cont =processMag(content.get(s).get("CONTENT"),alertBracket,cPhone);
								//WSDL方式呼叫 WebServer
								//result=tool.callWSDLServer(setSMSXmlParam(cont,phone));

								res=setSMSPostParam(cont,phone);
								currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_THRESHOLD", lastAlernThreshold);
								logger.debug("send message result : "+res);						
								currentMap.get(sYearmonth).get(serviceid).put("SMS_TIMES", smsTimes);
								smsCount++;
								
								sql="INSERT INTO HUR_SMS_LOG"
										+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
										+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+cont+"',TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
								//寫入資料庫
								logger.debug("execute SQL : "+sql);
								st.addBatch(sql);
								
								
								//中斷GPRS服務
								//20141113 新增客制定上限不執行斷網

								if(needSuspend &&"0".equals(everSuspend)&&!isCustomized){
									String imsi = msisdnMap.get(serviceid).get("IMSI");
									if(imsi==null || "".equals(imsi))
										imsi = ServiceIdtoIMSIMap.get(serviceid);
									
									if(imsi==null || "".equals(imsi)){
										logger.debug("Suspend GPRS fail because without mimsi for serviceid is "+serviceid);
										continue;
									}
									logger.debug("Suspend GPRS ... ");		
									suspend(imsi,phone);
									currentMap.get(sYearmonth).get(serviceid).put("EVER_SUSPEND", "1");
								}

							}
							
						}
					}
				}
				logger.debug("Total send month alert"+smsCount+" ...");
				logger.debug("Log to table...executeBatch");
				st.executeBatch();
			} catch (SQLException e) {
				logger.error("At sendMonthAlertSMS occur SQLException error!", e);
				//send mail
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
				sendErrorMail("At sendMonthAlertSMS occur SQLException error!");
			}catch(Exception e){
				logger.error("At sendMonthAlertSMS occur Exception error!", e);
				//send mail
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
				sendErrorMail("At sendMonthAlertSMS occur Exception error!");
			}finally{
				try {
					if(st!=null)
						st.close();
					if(rs!=null)
						rs.close();
				} catch (SQLException e) {
				}
			}
		}
		
		
		//實做日警示部分，有今日資料才警示*************************************
		if(currentDayMap.containsKey(sYearmonthday)){
			smsCount=0;
			try {
				st = conn.createStatement();
				for(String serviceid:currentDayMap.get(sYearmonthday).keySet()){
					
					//20141216 add 斷網過後，不發送每日簡訊，避免預估斷網後，每日帶出實際累計引發爭議
					if(currentMap.containsKey(sYearmonth) && currentMap.get(sYearmonth).containsKey(serviceid)){
						String everSuspend =(String) currentMap.get(sYearmonth).get(serviceid).get("EVER_SUSPEND");
						if("1".equals(everSuspend)){
							continue;
						}
					}
					
					
					
					//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
					if(msisdnMap.containsKey(serviceid))
						phone=(String) msisdnMap.get(serviceid).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendErrorMail
						sendErrorMail("At sendAlertSMS occur error! The serviceid:"+serviceid+" can't find msisdn to send! ");
						logger.debug("The serviceid:"+serviceid+" can't find msisdn to send! ");
						continue;
					}

					Double daycharge=0D;
					String alerted ="0";
					
					//累計
					for(String nccNet : currentDayMap.get(sYearmonthday).get(serviceid).keySet()){
						daycharge=daycharge+(Double)currentDayMap.get(sYearmonthday).get(serviceid).get(nccNet).get("CHARGE");
						String a=(String) currentDayMap.get(sYearmonthday).get(serviceid).get(nccNet).get("ALERT");
						if("1".equals(a)) alerted="1";
					}
					
					if(daycharge>=DEFAULT_DAY_THRESHOLD && "0".equalsIgnoreCase(alerted)){
						//查詢所在國家的客服電話
						String cPhone = null;
						String nMccmnc=searchMccmncBySERVICEID(serviceid);
						Map<String,String> map=null;
						
						if(nMccmnc!=null && !"".equals(nMccmnc))
							map = codeMap.get(nMccmnc.substring(0,3));
						if(map!=null)
							cPhone=map.get("PHONE");
						
						//處理字串，日警示內容ID設定為99
						//20141209 修改為帶出當月累積金額
						String cont =processMag(content.get("99").get("CONTENT"),(Double)currentMap.get(sYearmonth).get(serviceid).get("CHARGE"),cPhone);
						//發送簡訊
						logger.info("For "+serviceid+" send daily allert message:99");
						String res = setSMSPostParam(cont,phone);
						logger.debug("send message result : "+res);	
						smsCount++;
						//回寫註記，因為有區分Mccmnc，全部紀錄避免之後取不到
						for(String nccNet : currentDayMap.get(sYearmonthday).get(serviceid).keySet()){
							currentDayMap.get(sYearmonthday).get(serviceid).get(nccNet).put("ALERT", "1");
						}
						sql="INSERT INTO HUR_SMS_LOG"
								+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
								+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+cont+"',TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
						//寫入資料庫
						st.addBatch(sql);
						logger.debug("execute SQL : "+sql);
					}
				}
				logger.debug("Total send day alert "+smsCount+" ...");
				logger.debug("Log to table...executeBatch");
				st.executeBatch();			
			} catch (SQLException e) {
				logger.error("At sendDayAlertSMS occur SQLException error!", e);
				//send mail
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
				sendErrorMail("At sendDayAlertSMS occur SQLException error!");
			} catch (IOException e) {
				logger.error("At sendDayAlertSMS occur SQLException error!", e);
				//send mail
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
				sendErrorMail("At sendDayAlertSMS occur SQLException error!");
			} finally{
				try {
					if(st!=null)
						st.close();
					if(rs!=null)
						rs.close();
				} catch (SQLException e) {
				}
			}	
		}
		
		
		//降速提醒簡訊判斷*************************************
		
		//暫存數據用量資料 Key:SERVICEID,Value:Volume
		Map<String,Double> tempMap = new HashMap<String,Double>();
		//是否需要計算的pricePlanid
		Set<String> checkedPriceplanid =new HashSet<String>();
		checkedPriceplanid.add("155");
		checkedPriceplanid.add("156");
		checkedPriceplanid.add("157");
		checkedPriceplanid.add("158");
		checkedPriceplanid.add("159");
		checkedPriceplanid.add("160");
		
		Set<String> checkedMCCNET =new HashSet<String>();
		checkedMCCNET.add("460China Unicom");//46001
		checkedMCCNET.add("460CMCC");//46007.46002,460000,46000
		checkedMCCNET.add("454CMHK");//45412
		
		//是否為Data Only 客戶 NTT
		Set<String> checkedPriceplanid2 =new HashSet<String>();
		checkedPriceplanid2.add("158");
		checkedPriceplanid2.add("159");
		checkedPriceplanid2.add("160");
		
		for(String day : currentDayMap.keySet()){
			//這個月的月資料
			if(sYearmonth.equalsIgnoreCase(day.substring(0, 6))){
				for(String serviceid:currentDayMap.get(day).keySet()){
					//確認priceplanid 與 subsidiaryid
					
					String priceplanid = null;
					String subsidiaryid = null;
					if(msisdnMap.containsKey(serviceid)){
						priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID");
						subsidiaryid = msisdnMap.get(serviceid).get("SUBSIDIARYID");
					}
					
					if(checkedPriceplanid.contains(priceplanid)&&"72".equalsIgnoreCase(subsidiaryid)){
						for(String mccNet:currentDayMap.get(day).get(serviceid).keySet()){
							//確認Mccmnc
							if(checkedMCCNET.contains(mccNet)){
								//進行累計
								Double oldVolume=0D;
								Double volume=(Double) currentDayMap.get(day).get(serviceid).get(mccNet).get("VOLUME");
								if(tempMap.containsKey(serviceid)){
									oldVolume=tempMap.get(serviceid);
								}
								tempMap.put(serviceid, oldVolume+volume);
							}
						}
					}	
				}
			}
		}
		
		try {
			smsCount=0;
			st = conn.createStatement();
			for(String serviceid:tempMap.keySet()){
				Double volume=tempMap.get(serviceid);
				Double everAlertVolume = (Double) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_VOLUME");
				//超過發簡訊，另外確認是否已通知過
				boolean sendmsg=false;
				Integer msgid=0;
				
				if(volume>=DEFAULT_VOLUME_THRESHOLD2 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD2){
					//2.0 GB 簡訊中文102，英文103
					msgid=102;
					sendmsg=true;
				}
				if(!sendmsg && volume>=DEFAULT_VOLUME_THRESHOLD && everAlertVolume<DEFAULT_VOLUME_THRESHOLD){
					//1.5 GB 簡訊中文100，英文101
					msgid=100;
					sendmsg=true;
				}
				
				if(sendmsg){
					/*
					String priceplanid = null;
					if(msisdnMap.containsKey(serviceid)){
						priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID");
						//是否為Data only 方案
						if(checkedPriceplanid2.contains(priceplanid)){
							//以設定通知號通知
							phone=msisdnMap.get(serviceid).get("NCODE");
						}else{
							//以門號通知
							phone=msisdnMap.get(serviceid).get("MSISDN");
						}
					}
					
					//確認號碼
					if(phone==null ||"".equals(phone)){
						//sendErrorMail
						sendErrorMail("At sendAlertSMS occur error! The serviceid:"+serviceid+" can't find msisdn to send "+msgid+"! ");
						logger.debug("The serviceid:"+serviceid+" can't find msisdn to send "+msgid+"! ");
						continue;
					}*/
					
					//查詢所在國家的客服電話
					String cPhone = null;
					String nMccmnc=searchMccmncBySERVICEID(serviceid);
					Map<String,String> map=null;
					
					if(nMccmnc!=null && !"".equals(nMccmnc))
						map = codeMap.get(nMccmnc.substring(0,3));
					if(map!=null)
						cPhone=map.get("PHONE");

					/*
					//中文
					//處理字串
					String cont =processMag(content.get(msgid.toString()).get("CONTENT"),null,cPhone);
					mail_content += cont +"<br><br>";
					//20150309 mod temprater setting only send mail
					//發送簡訊
					//String res = setSMSPostParam(cont,phone);
					//logger.debug("send chinese message result : "+res);	

					smsCount++;
					
					sql="INSERT INTO HUR_SMS_LOG"
							+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
							+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+cont+"',TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
					//寫入資料庫
					st.addBatch(sql);
					logger.debug("execute SQL : "+sql); 
					
					
					//英文
					msgid+=1;
					//處理字串
					cont =processMag(content.get(msgid.toString()).get("CONTENT"),null,cPhone);
					mail_content += cont +"<br><br>";
					//20150309 mod temprater setting only send mail
					//發送簡訊
					//res = setSMSPostParam(cont,phone);
					//logger.debug("send english message result : "+res);	
					smsCount++;
					
					sql="INSERT INTO HUR_SMS_LOG"
							+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
							+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+cont+"',TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
					//寫入資料庫
					st.addBatch(sql);
					logger.debug("execute SQL : "+sql); */

					String mail_subject="";
					String mail_content="";
					String mail_sender="HKNet@sim2travel.com";
					String mail_receiver=HKNetReceiver;
					//發送簡訊
					if(msgid==100){
						mail_subject = "Notification on FUP 75% 1.5GB";
						mail_content = content.get("104").get("CONTENT");
						logger.info("For "+serviceid+" send 1.5GB decrease speed  message !");
					}
					if(msgid==102){
						mail_subject = "Notification on FUP 100% 2GB";
						mail_content = content.get("105").get("CONTENT");
						logger.info("For "+serviceid+" send 2.0GB decrease speed  message !");
					}
					
					mail_content = processMag(mail_content,null,cPhone,msisdnMap.get(serviceid).get("ICCID"));
					
					sendMail(new String(mail_subject.getBytes("ISO8859-1"),"BIG5"), 
							new String(mail_content.getBytes("ISO8859-1"),"BIG5").replaceAll("%2b", "+"),mail_sender ,mail_receiver );
					
					
					sql="INSERT INTO HUR_SMS_LOG"
							+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
							+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+mail_receiver+"','"+mail_content+"',TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'success',SYSDATE)";
					//寫入資料庫
					//st.addBatch(sql);
					st.executeUpdate(sql);
					logger.debug("execute SQL : "+sql); 
					
					//更新CurrentMap
					currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_VOLUME",volume);

				}
				
			}
			logger.debug("Total send volume alert SMS "+smsCount+" ...");
			//st.executeBatch();			
		} catch (SQLException e) {
			logger.error("At sendDayAlertSMS occur SQLException error!", e);
			//send mail
			
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At sendDayAlertSMS occur SQLException error!");
		} catch (UnsupportedEncodingException e) {
			logger.error("At sendDayAlertSMS occur UnsupportedEncodingException error!", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At sendDayAlertSMS occur UnsupportedEncodingException error!");
		} catch (Exception e) {
			logger.error("At sendDayAlertSMS occur Exception error!", e);
			//send mail
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
			sendErrorMail("At sendDayAlertSMS occur Exception error!");
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
	}
	
	/**
	 * 處理替代字串
	 * {{bracket}} 額度
	 * @param msg
	 * @param bracket
	 * @return
	 */
	
	private String processMag(String msg,Double bracket,String cPhone){
		return processMag(msg,bracket,cPhone,null);
	}
	
	private String processMag(String msg,Double bracket,String cPhone,String ICCID){
		
		//金額
		if(bracket==null)
			bracket=0D;
		msg=msg.replace("{{bracket}}", tool.FormatNumString(bracket,"NT#,##0.00"));
		
		//客服電話
		if(cPhone==null)
			cPhone="";
		msg=msg.replace("{{customerService}}",cPhone);
		
		//ICCID
		if(ICCID==null)
			ICCID="";
		msg=msg.replace("{{ICCID}}",ICCID);
		
		return msg;
	}
	
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
		param=param.replace("{{Text}}",Text );
		param=param.replace("{{charset}}",charset );
		param=param.replace("{{InfoCharCounter}}",InfoCharCounter );
		param=param.replace("{{PID}}",PID );
		param=param.replace("{{DCS}}",DCS );
		
		
		
		return tool.HttpPost("http://192.168.10.125:8800/Send%20Text%20Message.htm", param,"");
	}
	
	/**
	 * 中斷GPRS
	 * @param imsi
	 * @param msisdn
	 */
	private void suspend(String imsi,String msisdn){
		logger.info("suspend...");
		
		try {
			
			suspendGPRS sus=new suspendGPRS(conn,conn2,logger);
			
			//20141118 add 傳回suspend排程的 service order nbr
			Map<String,String> orderNBR=sus.ReqStatus_17_Act(imsi, msisdn);
			serviceOrderNBR.add(orderNBR);
			
			sql=
					"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
					+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN) "
					+ "VALUES(?,?,SYSDATE,?)";
			
			PreparedStatement pst=conn.prepareStatement(sql);
			pst.setString(1,orderNBR.get("cServiceOrderNBR") );
			pst.setString(2,imsi );
			pst.setString(3,msisdn );
			logger.info("Execute SQL : "+sql);
			
			pst.executeUpdate();
			
			if(pst!=null) pst.close();
			if(sus.Temprs!=null) sus.Temprs.close();
			
		} catch (SQLException e) {
			logger.error("At suspend occur SQLException error!", e);
			//send mail
			sendErrorMail("At suspend occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (IOException e) {
			logger.error("At suspend occur IOException error!", e);
			//send mail
			sendErrorMail("At suspend occur IOException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (ClassNotFoundException e) {
			logger.error("At suspend occur ClassNotFoundException error!", e);
			//send mail
			sendErrorMail("At suspend occur ClassNotFoundException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (Exception e) {
			logger.error("At suspend occur Exception error!", e);
			//send mail
			sendErrorMail("At suspend occur Exception error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * 發送Error mail
	 * 
	 * @param content
	 */
	private static void sendErrorMail(String content){
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
	
	/**
	 * 發送mail
	 * 
	 * @param content
	 * @throws Exception 
	 */
	private static void sendMail(String mailSubject,String mailContent,String mailSender,String mailReceiver) throws Exception{

		if(mailReceiver==null ||"".equals(mailReceiver)){
			logger.error("Can't send email without receiver!");
			throw new Exception("Can't send email without receiver!");
		}else{
			tool.sendMail(logger, props, mailSender, mailReceiver, mailSubject, mailContent);
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
					sendErrorMail("Suspend does not work for"+"<br>"
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
				logger.error("At processSuspendNBR occur InterruptedException error!", e);
				//send mail
				sendErrorMail("At processSuspendNBR occur InterruptedException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			} catch (SQLException e) {
				logger.error("At processSuspendNBR occur SQLException error!", e);
				//send mail
				sendErrorMail("At processSuspendNBR occur SQLException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			} catch (IOException e) {
				logger.error("At processSuspendNBR occur IOException error!", e);
				//send mail
				sendErrorMail("At processSuspendNBR occur IOException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			}
		}
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
		String cSt="";
		for (int i=0;i<5;i++){
			Thread.sleep(1000);
			sql="select result_flag from S2T_TB_TYPB_WO_SYNC_FILE_DTL Where " +
                "SERVICE_ORDER_NBR ='"+cServiceOrderNBR+"'";
			 logger.info(sql);
        ResultSet rs=conn.createStatement().executeQuery(sql);
        while (rs.next()){
                cSt=rs.getString("result_flag");
        }
        logger.info("Query_SyncFileDtlStatus:"+Integer.toString(i)+" Times "+cSt);
            }
     return cSt;
    }
	
	
	//*******************************Debug 工具*************************************//

	

	/*************************************************************************
	 *************************************************************************
	 *                                主程式
	 *************************************************************************
	 *************************************************************************/
	
	public static void main(String[] args) {
		

		IniProgram();

		//DVRSmain rf =new DVRSmain();
		//rf.process();
		
		regularTimeRun();

	}
	
	public static void regularTimeRun(){
		try {
			// Grab the Scheduler instance from the Factory
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

			// define the job and tie it to our HelloJob class ->
			// JobBuilder.newJob()
			JobDetail job = JobBuilder.newJob(DVRSmain.class)
					.withIdentity("job1", "group1").build();

			// Trigger the job to run now, and then repeat every 40 seconds ->
			// TriggerBuilder.newTrigger()
			Trigger trigger = TriggerBuilder
					.newTrigger()
					.withIdentity("trigger1", "group1")
					.startNow()
					.withSchedule(
							SimpleScheduleBuilder.simpleSchedule()
									.withIntervalInSeconds(RUN_INTERVAL)
									.repeatForever()).build();

			// Tell quartz to schedule the job using our trigger
			scheduler.scheduleJob(job, trigger);
		
			// and start it off
			scheduler.start();
			
			// 使程式暫停，Job持續運作
			//pause();// 以sleep的方式暫停
			//keyin();// 以等待使用者keyin的方式暫停

		} catch (SchedulerException e) {
			e.printStackTrace();
			sendErrorMail("at regularTimeRun occure error!");
		}
	}
	
	@Override
	public void execute(JobExecutionContext arg0){
		
		//如果已有等待的thread，結束自己
		if(hasWaiting) {
			logger.debug("****************************      Found had wating thread doesn't execute!");
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
					logger.error("At waiting thread occur error", e);
					//send mail
					sendErrorMail("At waiting thread occur error!");
					errorMsg="";
					for(StackTraceElement s :e.getStackTrace()){
						errorMsg+=s.toString()+"<br>\n";
					}
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
			e.printStackTrace();
			sendErrorMail("process error!");
		}finally{
			//將動作交給下個thread
			executing=false;
		}

		//sendErrorMail("test mail " + new Date());
	}
	
	private void process() {
		// 程式開始時間
		long startTime;
	
		logger.info("RFP Program Start! "+new Date());
		
		// 進行DB連線
			connectDB();
			connectDB2();
		
		if (conn != null && conn2!=null) {
			
			logger.debug("connect success!");
			
			startTime = System.currentTimeMillis();
			
			//取消自動Commit
			cancelAutoCommit();
			
			if(
					setDayDate() && //設定日期
					setIMSItoServiceIDMap()&&//設定IMSI至ServiecId的對應
					setServiceIDtoImsiMap()&&//設定ServiecId至IMSI的對應
					//setLastFileID()&&//取得最後更新的FileID
					setThreshold()&&//取出HUR_THRESHOLD
					setDataRate()&&//取出HUR_DATARATE
					setMsisdnMap()&&//取出msisdn資訊
					setIPtoMccmncList()&&//20141211 add IP 對應到 MCCMNC
					setSERVICEIDtoVLN()&&//IMSI 對應到 vln
					setVLNtoTADIG()&&//vln 對應到 TADIG
					setTADIGtoMCCMNC()&&//TADIG 對應到 MCCMNC
					setCostomerNumber()&&//國碼對應表(客服,國名)
					setAddonData()&&//華人上網包申請資料
					(currentMap.size()!=0||setCurrentMap())&&//取出HUR_CURRENT
					(oldChargeMap.size()!=0||setoldChargeMap())&&//設定old
					(currentDayMap.size()!=0||setCurrentMapDay())){//取出HUR_CURRENT
				
				
				//開始批價 
				charge();

				//發送警示簡訊
				sendAlertSMS();
				
				//回寫批價結果
				if(
						updateCdr()&&
						insertCurrentMap()&&
						insertCurrentMapDay()&&
						updateCurrentMap()&&
						updateCurrentMapDay()){
					//避免資料異常，完全處理完之後在commit
					try {
						conn.commit();
					} catch (SQLException e) {
						try {
							conn.rollback();
						} catch (SQLException e1) {
						}
						logger.error("At commit occur SQLException error!", e);
						//send mail
						errorMsg="";
						for(StackTraceElement s :e.getStackTrace()){
							errorMsg+=s.toString()+"<br>\n";
						}
						sendErrorMail("At commit occur SQLException error!");
					}

					//suspend的後續追蹤處理
					processSuspendNBR();
				}
			}

			// 程式執行完成
			logger.info("Program execute time :" + (System.currentTimeMillis() - startTime));
			closeConnect();

		} else {
			logger.error("connect is null!");
			sendErrorMail("Cannot connect to DB!");
		}
	}	
}



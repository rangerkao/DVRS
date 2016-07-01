
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
	Connection conn = null;
	//MBOSS
	Connection conn2 = null;
	
	private static  Logger logger ;
	static Properties props=new Properties();

	static boolean resume = false;
	static boolean resumeSpeed = false;
	
	//mail conf
	static String mailSender="";
	static String mailReceiver="";
	static String mailSubject="mail test";
	static String mailContent="mail content text";
	
	SimpleDateFormat spf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	private static String sql="";
	private static String errorMsg="";
	
	//Hur Data conf
	private static Integer dataThreshold=null;//CDR資料一批次取出數量
	//private static Integer lastfileID=null;//最後批價檔案號
	private static Double exchangeRate=null; //港幣對台幣匯率，暫訂為4
	private static Double kByte=null;//RATE單位KB，USAGE單位B
	
	//日期設定
	private String MONTH_FORMATE="yyyyMM";
	private String DAY_FORMATE="yyyyMMdd";	
	private String TIME_FORMATE="ddHH";	
	//系統時間，誤差一小時，系統資料處理時間為當時時間提前一小時
	private String programeTime="";
	private String sYearmonth="";
	private String sYearmonthday="";
	//上個月
	private String sYearmonth2="";
	
	
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
	
	//20151230 add 對華人上網包客戶在所申請區域時，不進行斷網
	//20151231 cancel
	/*static Set <String> addonMark = new HashSet<String>();
	static Set <String> notAddonMark = new HashSet<String>();*/
	
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
	
	Map<String,String> IMSItoServiceIdMap = new HashMap<String,String>();
	Map<String,String> ServiceIdtoIMSIMap = new HashMap<String,String>();
	
	//new Version
	Map<String,Map<String,Object>> systemConfig = new HashMap<String,Map<String,Object>>();
	//new version end
	
	//20160624 add
	List<Map<String,Object>> slowDownList = new ArrayList<Map<String,Object>>();
	
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
		
		//TODO test
		//calendar.set(2016,6, 2, 1, 30, 0);
		System.out.println(calendar.getTime());
		
		
		//系統時間提前一小時
		//因為警示發送只計當日，當00：30執行時，所處理的資料為前一天的23：00
		calendar.setTimeInMillis(calendar.getTimeInMillis()-1000*60*60);
		programeTime = DateFormat(calendar.getTime(),TIME_FORMATE);		
		sYearmonth=DateFormat(calendar.getTime(), MONTH_FORMATE);
		sYearmonthday=DateFormat(calendar.getTime(),DAY_FORMATE);
		//上個月時間，減掉Month會-30天，採取到1號向前，確定跨月
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)-1);
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-1);
		sYearmonth2=DateFormat(calendar.getTime(), MONTH_FORMATE);
		calendar.clear();
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		//當執行時間為1日0點時進行復網
		if("0100".equals(programeTime)){
			resume = true;
			NTTalerted = false;
			NTT75alerted = false;
		}
		if("00".equals(programeTime.substring(2))){
			resumeSpeed = true;
		}
		
		return true;
	}

	/**
	 * 設定華人上網包MCCMNC資料 20141118 排除華人上網包
	 */
	private boolean setQosData(){
		checkConnection();
		logger.info("setQosData...");
		sSX001.clear();
		sSX002.clear();
		
		long subStartTime = System.currentTimeMillis();
		//sSX001
		sSX001.add("45412");
		
		sSX001.add("454CMHK");
		
		//sSX002
		sSX002.add("46001");
		sSX002.add("46007");
		sSX002.add("46002");
		sSX002.add("460000");
		sSX002.add("46000");
		sSX002.add("45412");
		
		sSX002.add("460China Unicom");
		sSX002.add("460CMCC");
		sSX002.add("454CMHK");
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return true;
	}
	

	/*************************************************************************
	 *************************************************************************
	 *                                表格資料設定
	 *************************************************************************
	 *************************************************************************/
	SimpleDateFormat year_month_day_sdf = new SimpleDateFormat("yyyyMMdd");
	//20160624 add
		/**
		 * 設定降速參數
		 */
		private boolean setSlowDownList(){
			checkConnection();
			logger.info("setSlowDownList...");
			
			
			
			sql = ""
					+ "SELECT A.MCCMNC,A.PRICEPLANID,A.LIMIT,A.START_DATE,A.END_DATE,A.ISDAYLY,A.GPRS_NAME "
					+ "from HUR_SLOWDOWN_LIST A ";
		
			/*String sql2 = ""
					+ "SELECT A.MCCMNC,A.PRICEPLANID,A.LIMIT,To_DATE(A.START_DATE,'yyyyMMdd') START_DATE,TO_DATE(A.END_DATE,'yyyyMMdd') END_DATE,A.ISDAYLY "
					+ "from HUR_SLOWDOWN_LIST A "
					+ "where A.START_DATE < sysdate and A.END_DATE> sysdate ";*/
			
			boolean result = false;
			Statement st = null;
			ResultSet rs = null;
			try {
				st = conn.createStatement();
				logger.debug("Query SlowDownList SQL : "+sql);
				rs = st.executeQuery(sql);
				logger.info("Query end!");
				
				while(rs.next()){
					
					boolean isdayly = "1".equals(rs.getString("ISDAYLY"))?true:false;
					String sDate = null,eDate = null;
					if(isdayly){
						Calendar c = Calendar.getInstance();
						c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
						sDate = year_month_day_sdf.format(c.getTime());
						
						c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH)+1);
						eDate = year_month_day_sdf.format(c.getTime());
					}else{
						sDate = rs.getString("START_DATE");
						eDate = rs.getString("END_DATE");
					}
					System.out.println(sDate+"~"+eDate);
					
					Map<String,Object> m = new HashMap<String,Object>();
					m.put("MNO", rs.getString("MCCMNC"));
					m.put("PRICEPLAN", rs.getString("PRICEPLANID"));
					m.put("LIMIT", rs.getInt("LIMIT")*1024);//500MB
					m.put("TIMESTART", sDate);
					m.put("TIMEEND", eDate);
					m.put("GPRS_NAME", rs.getString("GPRS_NAME"));
					slowDownList.add(m);
				}
				
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
	public boolean setSystemConfig(){
		checkConnection();
		//System.out.println("New Version check Point 11.");
		logger.info("setSystemConfig...");
		boolean result = false;
		Statement st = null;
		ResultSet rs = null;
		try {
			sql="SELECT A.NAME,A.VALUE,A.DESCR,A.PRICE_PLAN_ID FROM HUR_DVRS_CONFIG A";
			st = conn.createStatement();
			logger.debug("Query SystemConfig SQL : "+sql);
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				String pricePlanId = rs.getString("PRICE_PLAN_ID");
				if(pricePlanId==null)
					pricePlanId = "0"; //global parameter
				
				for(String id : pricePlanId.split(",")){
					Map<String,Object> m = new HashMap<String,Object>();
					if(systemConfig.containsKey(id)){
						m = systemConfig.get(id);
					}
					m.put(rs.getString("NAME"), rs.getObject("VALUE"));
					systemConfig.put(id, m);
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
		
		return result;
	}
	//new version end

	/**
	 * 尋找最後一次更改的fileID，以及目標處理的最終ID
	 */
	/*private boolean setLastFileID(){
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
	}*/
	
	/**
	 * 取出 HUR_CURRENTE table資料
	 * 建立成
	 * Map 
	 * Key:MONTH,Value:Map(serviceid,Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_VOLUME)))
	 */
	private boolean setCurrentMap(){
		checkConnection();
		logger.info("setCurrentMap...");
		currentMap.clear();
		
		long subStartTime = System.currentTimeMillis();
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		
		//設定HUR_CURRENT計費，抓出這個月與下個月
		try {
			sql=
					"SELECT A.SERVICEID,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,"
					+ "to_char(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,"
					+ "A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			logger.info("Query end!");
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
				map.put("LAST_DATA_TIME", rs.getString("LAST_DATA_TIME"));
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
		return result;
	}
	
	/**
	 * 保留這個月舊資料，作為預測費用使用
	 */
	private boolean setoldChargeMap(){
		logger.info("setoldChargeMap...");
		oldChargeMap.clear();
		
		long subStartTime = System.currentTimeMillis();
		if(currentMap.containsKey(sYearmonth)){
			for(String serviceid : currentMap.get(sYearmonth).keySet()){
				oldChargeMap.put(serviceid, (Double)currentMap.get(sYearmonth).get(serviceid).get("CHARGE"));
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
		checkConnection();
		logger.info("setCurrentMapDay...");
		currentDayMap.clear();
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result = false; 
		try {
			sql=
					"SELECT A.SERVICEID,A.CHARGE,A.LAST_FILEID,to_char(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,A.VOLUME,A.UPDATE_DATE,A.CREATE_DATE,A.MCCMNC,A.DAY,A.ALERT,A.IS_SLOWDOWN "
					+ "FROM HUR_CURRENT_DAY A "
					+ "WHERE SUBSTR(A.DAY,0,6) IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs =st.executeQuery(sql);
			logger.info("Query end!");
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
						if(map3.containsKey(serviceid)){
							map2=map3.get(serviceid);
						}
					}
							
					map.put("LAST_FILEID", rs.getInt("LAST_FILEID"));
					map.put("LAST_DATA_TIME", rs.getString("LAST_DATA_TIME"));
					map.put("CHARGE", rs.getDouble("CHARGE"));
					map.put("VOLUME", rs.getDouble("VOLUME"));
					map.put("ALERT", rs.getString("ALERT"));
					map.put("IS_SLOWDOWN", rs.getString("IS_SLOWDOWN"));
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
		
		return result;
	}
	
	/**
	 * 取出 HUR_DATA_RATE
	 * 建立成MAP Key:PRICEPLANID,Value:Map(MCCMNC,MAP(CURRENCY,CHARGEUNIT,RATE,NETWORK))
	 */
	//20150324 modify add network info
	private boolean setDataRate(){
		checkConnection();
		logger.info("setDataRate...");
		dataRate.clear();
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
	public boolean setCurrencyMap(){
		checkConnection();
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
	}
	
	/**
	 * 取出HUR_THRESHOLD
	 * 建立MAP Key:IMSI,VALUE:THRESHOLD
	 * 可以變更成使用者自定義上限，目前不使用全填上null
	 * @return 
	 */
	private boolean setThreshold(){
		checkConnection();
		logger.info("setThreshold...");
		thresholdMap.clear();
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.SERVICEID,A.THRESHOLD "
				+ "FROM HUR_GPRS_THRESHOLD A "
				+ "WHERE A.CANCEL_DATE IS NULL ";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);

			while(rs.next()){
				thresholdMap.put(rs.getString("SERVICEID"), rs.getDouble("THRESHOLD"));
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
	 * 設定 IMSI 至 VLN 的對應表
	 * Map Key:IMSI,VALUE:VLN
	 */
	private boolean setSERVICEIDtoVLN(){
		checkConnection();
		logger.info("setSERVICEIDtoVLN...");
		SERVICEIDtoVLN.clear();
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.VLR_NUMBER,A.SERVICEID "
				+ "FROM UTCN.BASICPROFILE A ";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn2.createStatement();
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				//20160701 mod
				String vlrNumber = rs.getString("VLR_NUMBER");
				if(vlrNumber!=null)
					SERVICEIDtoVLN.put(rs.getString("SERVICEID"), vlrNumber);
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
	private boolean setVLNtoTADIG(){
		checkConnection();
		logger.info("setVLNtoTADIG...");
		VLNtoTADIG.clear();
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
	private boolean setTADIGtoMCCMNC(){
		checkConnection();
		logger.info("setTADIGtoMCCMNC...");
		TADIGtoMCCMNC.clear();
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
	private boolean setCostomerNumber(){
		checkConnection();
		logger.info("setCostomerNumber...");
		codeMap.clear();
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
			logger.info("Query end!");
			while(rs.next()){
				Map<String,String> map = new HashMap<String,String>();
				map.put("PHONE", rs.getString("PHONE"));
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
	}
	
	/**
	 * 取得預設計費比率（0.011），對MCCNOC有卻無法對應資料計費
	 * @return
	 */
	//20151229 del
	/*private double defaultRate(){
		logger.info("defaultRate...");
		long subStartTime = System.currentTimeMillis();
		
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
	private boolean setAddonData(){
		checkConnection();
		logger.info("setAddonData...");
		addonDataList.clear();
		
		long subStartTime = System.currentTimeMillis();
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		
		sql=
				"SELECT A.S2TIMSI IMSI,A.S2TMSISDN MSISDN,A.SERVICEID,A.SERVICECODE,A.STARTDATE,A.ENDDATE "
				+ "FROM ADDONSERVICE_N A ";
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("IMSI", rs.getString("IMSI"));
				map.put("MSISDN", rs.getString("MSISDN"));
				map.put("SERVICEID", rs.getString("SERVICEID"));
				map.put("SERVICECODE", rs.getString("SERVICECODE"));
				map.put("STARTDATE", rs.getDate("STARTDATE"));
				map.put("ENDDATE", rs.getDate("ENDDATE"));
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
	private boolean setIPtoMccmncList(){
		checkConnection();
		logger.info("setIPtoMccmncList...");
		IPtoMccmncList.clear();
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
			logger.info("Query end!");
			while(rs.next()){
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("START_NUM", rs.getLong("START_NUM"));
				map.put("END_NUM", rs.getLong("END_NUM"));
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
	private boolean setMsisdnMap(){
		checkConnection();
		logger.info("setMsisdnMap...");
		msisdnMap.clear();
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		sql=
				"SELECT da.IMSI,da.SERVICECODE,da.PRICEPLANID,da.SUBSIDIARYID,da.SERVICEID,da.ICCID, da.NCODE "
				+ "from ("
				+ "        SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,B.ICCID, "
				+ "				(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE "
				+ "			FROM SERVICE A,IMSI B,PARAMETERVALUE C "
				+ "			WHERE A.SERVICEID=B.SERVICEID(+) AND A.SERVICECODE IS NOT NULL "
				+ "			AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 ) da, (select serviceid from hur_current where month in ('"+sYearmonth+"','"+sYearmonth2+"')) cho "
				+ "        WHERE da.serviceid = cho.serviceid ";
		
		String sql2= 
				"SELECT da.IMSI,da.SERVICECODE,da.PRICEPLANID,da.SUBSIDIARYID,da.SERVICEID,da.ICCID, da.NCODE "
				+ "from ("
				+ "        SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,B.ICCID, "
				+ "				(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE "
				+ "			FROM SERVICE A,IMSI B,PARAMETERVALUE C "
				+ "			WHERE A.SERVICEID=B.SERVICEID(+) AND A.SERVICECODE IS NOT NULL "
				+ "			AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 ) da, (select imsi from hur_data_usage where charge is null) cho "
				+ "        WHERE da.imsi = cho.imsi ";
		
		try {
			
			st = conn.createStatement();
			
			logger.info("Execute SQL :"+sql);
			rs=st.executeQuery(sql);
			logger.info("Query1 end!");
			
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
			
			
			logger.info("Execute SQL :"+sql2);
			rs=st.executeQuery(sql2);
			logger.info("Query2 end!");
			
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
	 * 
	 * 先從MSISDN MAP(IMSI table 資料找尋 SERVICEID)
	 * 找不到再從此Table(換卡記錄)找尋
	 */

	private boolean setIMSItoServiceIDMap(){
		checkConnection();
		logger.info("setIMSItoServiceIDMap...");
		logger.info("setServiceIDtoImsiMap...");
		IMSItoServiceIdMap.clear();
		ServiceIdtoIMSIMap.clear();
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result =false;
		String sql2 = null;
		
		if(TEST_MODE){
			sql = "SELECT A.SERVICEID,A.IMSI "
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
				+ "		WHERE A.IMSI=B.IMSI AND A.COMPLETEDATE =B.COMPLETEDATE ";
		}else{
			
			sql=  ""
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
					+ "(	select serviceid from hur_current where month in ('"+sYearmonth+"','"+sYearmonth2+"'))  cho "
					+ "		WHERE da.serviceid = cho.serviceid ";
			
			sql2 = ""
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
					+ "(	select imsi from hur_data_usage where charge is null)  cho "
					+ "		WHERE da.imsi = cho.imsi ";
		}
		
		
		
		try {
			if(TEST_MODE){
				st = conn2.createStatement();
			}else{
				st = conn.createStatement();
			}
			logger.info("Execute SQL :"+sql);
			rs=st.executeQuery(sql);
			logger.info("Query1 end!");
			while(rs.next()){
				IMSItoServiceIdMap.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
				ServiceIdtoIMSIMap.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
			}
			if(!TEST_MODE){
				logger.info("Execute SQL :"+sql2);
				rs=st.executeQuery(sql2);
				logger.info("Query2 end!");
				while(rs.next()){
					IMSItoServiceIdMap.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
					ServiceIdtoIMSIMap.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
				}
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
			//DEFAULT_THRESHOLD=(props.getProperty("progrma.DEFAULT_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_THRESHOLD")):5000D);//預設月警示量
			//DEFAULT_DAY_THRESHOLD=(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")):500D);//預設日警示量
			//DEFAULT_DAYCAP=(props.getProperty("progrma.DEFAULT_DAYCAP")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_DAYCAP")):500D);
			//DEFAULT_VOLUME_THRESHOLD=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")):1.5*1024*1024*1024D);//預設流量警示(降速)，1.5GB;
			//DEFAULT_VOLUME_THRESHOLD2=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")):1.5*1024*1024*1024D);//預設流量警示(降速)，15GB;
			DEFAULT_PHONE=props.getProperty("progrma.DEFAULT_PHONE");
			RUN_INTERVAL=(props.getProperty("progrma.RUN_INTERVAL")!=null?Integer.parseInt(props.getProperty("progrma.RUN_INTERVAL")):3600);
			HKNetReceiver = props.getProperty("program.HKNetReceiver");
			TEST_MODE=("true".equalsIgnoreCase(props.getProperty("progrma.TEST_MODE"))?true:false);
			
			dataThreshold=(props.getProperty("progrma.dataThreshold")!=null?Integer.parseInt(props.getProperty("progrma.dataThreshold")):500);//CDR資料一批次取出數量
			//lastfileID=(props.getProperty("progrma.lastfileID")!=null?Integer.parseInt(props.getProperty("progrma.lastfileID")):0);//最後批價檔案號
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
	 */
	private void connectDB(){
		//conn=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		try {
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
			
			
		} catch (ClassNotFoundException e) {
			sql="";
			ErrorHandle("At connDB occur ClassNotFoundException error", e);
		} catch (SQLException e) {
			ErrorHandle("At connDB occur SQLException error", e);
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
			
			conn2=connDB(props.getProperty("mBOSS.DriverClass"),url, 
					props.getProperty("mBOSS.UserName"), 
					props.getProperty("mBOSS.PassWord"));
			
			logger.info("Connrct to "+url);
		} catch (ClassNotFoundException e) {
			sql="";
			ErrorHandle("At connDB2 occur ClassNotFoundException error", e);
		} catch (SQLException e) {
			sql="";
			ErrorHandle("At connDB2 occur SQLException error", e);;
		}
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
	private void closeConnect() {
		
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
	}
	
	public Double FormatDouble(Double value,String form){
		
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
	 */
	private int checkQosAddon(String serviceID,String mccmnc,Date callTime){
		

		int cd=0;
				
		//20150623 search by serviceid 
		if(serviceID!=null && !"".equals(serviceID)){
			for(Map<String,Object> m : addonDataList){
				Calendar startTime = Calendar.getInstance(),endTime = Calendar.getInstance();
				startTime.setTime((Date) m.get("STARTDATE"));
				startTime.set(Calendar.HOUR_OF_DAY, 0);
				startTime.set(Calendar.MINUTE, 0);
				startTime.set(Calendar.SECOND, 0);
				
				
				if(m.get("ENDDATE")!=null){
					endTime.setTime((Date) m.get("ENDDATE"));
					endTime.set(Calendar.DAY_OF_YEAR, endTime.get(Calendar.DAY_OF_YEAR)+1);
					endTime.set(Calendar.HOUR_OF_DAY, 0);
					endTime.set(Calendar.MINUTE, 0);
					endTime.set(Calendar.SECOND, 0);
				}
				
				if(serviceID.equals(m.get("SERVICEID"))&&
						//是否在所申請的服務地區
						(("SX001".equals(m.get("SERVICECODE"))&& sSX001.contains(mccmnc))||
						("SX002".equals(m.get("SERVICECODE"))&& sSX002.contains(mccmnc)))&&
						//是否符合申請的時間區段
						(callTime.after(startTime.getTime())||callTime.equals(startTime.getTime()))&&
						(endTime==null ||callTime.before(endTime.getTime()))
						){
					cd=1;
					break;
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
		
		//20151230 add
		//20151231 cancel
		/*addonMark.clear();
		notAddonMark.clear();*/
		updateMap.clear();
		updateMapD.clear();
		cdrChargeMap.clear();
		
		Statement st = null;
		ResultSet rs = null;
		
		int count=0;
		Double defaultRate=0.011;
		try {
			count=dataCount();
			//20151229 del
			//defaultRate=defaultRate();
			setQosData();

			//批次Query 避免ram空間不足
			for(int i=1;(i-1)*dataThreshold+1<=count ;i++){
				sql=
						"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM HUR_DATA_USAGE A WHERE ROWNUM <= "+(i*dataThreshold)+" AND A.CHARGE is null "
						//+ "AND A.FILEID>= "+lastfileID+" "
						+ "ORDER BY A.USAGEID,A.FILEID) "
						+ "MINUS "
						+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM HUR_DATA_USAGE A WHERE ROWNUM <= "+((i-1)*dataThreshold)+" AND A.CHARGE is null "
						//+ "AND A.FILEID= "+lastfileID+" "
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
					String sCallTime=rs.getString("CALLTIME");
					Date callTime=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(sCallTime);
					Integer fileID=rs.getInt("FILEID");	
					Double charge=0D;
					Double dayCap=null;
					//20141211 add
					String ipaddr = rs.getString("SGSNADDRESS");
					//logger.debug("For CDR usageId="+usageId+" get IP Address "+ipaddr);
					
					//抓到對應的Serviceid
					//20150115 add
					String serviceid = null;
			
					//從換卡記錄找IMSI最後的ServiceID
					if(IMSItoServiceIdMap.get(imsi)!=null)
						serviceid = IMSItoServiceIdMap.get(imsi);
					
					if(serviceid==null || "".equals(serviceid)){
						sql="";
						ErrorHandle("For CDR usageId="+usageId+" which can't find  ServceID." );
						continue;
					}
					
					//抓到對應的PricePlanid
					String pricplanID=null;
					if(msisdnMap.containsKey(serviceid))
						pricplanID=msisdnMap.get(serviceid).get("PRICEPLANID");
					
			
					String currency = null;
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
					
					
					//取的資料所在的Mccmnc
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
					
					if(dataRate.containsKey(pricplanID)){
						
						currency=pricePlanIdtoCurrency.get(pricplanID);
						if(currency== null)
							ErrorHandle("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+"find currency="+currency);
						
						//判斷Mccmnc是否在Datarate中
						if(!dataRate.get(pricplanID).containsKey(mccmnc)){
							sql="";
							ErrorHandle("usageId:"+usageId+",IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");
						}
					}else{
						sql="";
						ErrorHandle("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
					}
					
					

					
					//如果為華人上網包的客戶，不批價設定為0
					int cd=checkQosAddon(serviceid, mccmnc, callTime);
					if(cd==0){
						
						//20151230 add
						//20151231 cancel
						//notAddonMark.add(serviceid);
						
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
									
									//取消幣別轉換，直接以原幣計價
									/*if("HKD".equalsIgnoreCase(currency))
										ec=exchangeRate;*/
										
									Double rate=(Double) m.get("RATE");
									Double unit=(Double) m.get("CHARGEUNIT");
									charge=Math.ceil(volume*kByte)*rate*ec/unit;
									dayCap=(Double) m.get("DAYCAP");
									haveRate=true;
									break;
								}
							}		
							if(!haveRate){
								sql="";
								ErrorHandle("usageId:"+usageId+",CALLTIME:"+callTime.toString()+" can't charge correctly without Rate table ! ");
								continue;
							}							
						}else{
							
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
							
							//取不到任何上限值 跳過
							if(defaultRate == null){
								sql="";
								ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricplanID+" cannot get defaultRate! ");
								continue;
							}
							System.out.println("Default Rate:"+defaultRate);
							
							charge=Math.ceil(volume*kByte)*defaultRate/ec;
						}
					}else{
						//20151230 add
						//20151231 cancel
						//addonMark.add(serviceid);
					}
					//20151230 add
					//20151231 cancel
					//addonMark.removeAll(notAddonMark);
					
					//格式化至小數點後四位
					charge=FormatDouble(charge, "0.0000");

					//將此筆CDR結果記錄，稍後回寫到USAGE TABLE
					cdrChargeMap.put(usageId, charge);
					logMsg+="UsageId "+usageId+" ,IMSI "+imsi+" ,MCCMNC "+mccmnc+" charge result is "+cdrChargeMap.get(usageId)+". ";

					
					//察看是否有以存在的資料，有的話取出做累加
					//20150324 modify mccmnc to mcc + network
					
					
					//以國碼+業者代碼作為累計的key值，不同業者不一起累計
					String nccNet;
					if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
							dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
						System.out.println(mccmnc);
						nccNet=mccmnc.substring(0,3);
						nccNet+=dataRate.get(pricplanID).get(mccmnc).get(0).get("NETWORK");
					}else{
						nccNet=DEFAULT_MCCMNC;
					}
					
					String cDay=DateFormat(callTime, DAY_FORMATE);
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

								oldCharge=(Double) map3.get("CHARGE");
								
								logMsg+="The old Daily charge is "+oldCharge+". ";
								
								//summary charge
								charge=oldCharge+charge;
								charge=FormatDouble(charge, "0.0000");
								alert=(String)map3.get("ALERT");
								oldvolume=(Double) map3.get("VOLUME");
							
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
					map3.put("LAST_DATA_TIME",sCallTime);
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
						
						logMsg+="The old month charge is "+(Double) map4.get("CHARGE")+". ";
						smsTimes=(Integer) map4.get("SMS_TIMES");
						suspend=(String) map4.get("EVER_SUSPEND");
						volume=(Double) map4.get("VOLUME")+volume;
						lastAlertThreshold=(Double) map4.get("LAST_ALERN_THRESHOLD");
						volumeAlert=(Double) map4.get("LAST_ALERN_VOLUME");
						
						if(fileID<(Integer) map4.get("LAST_FILEID"))
							fileID=(Integer) map4.get("LAST_FILEID");
					}
					
					
					map4.put("LAST_FILEID", fileID);
					map4.put("SMS_TIMES", smsTimes);
					map4.put("LAST_DATA_TIME", sCallTime);
					
					charge=preCharge+charge;
					charge=FormatDouble(charge, "0.0000");
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
			ErrorHandle("At updateCdr occur SQLException error", e);
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
					pst.setDouble(1,Double.parseDouble((String)  currentMap.get(mon).get(serviceid).get("CHARGE"));
					pst.setInt(2, (Integer) currentMap.get(mon).get(serviceid).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMap.get(mon).get(serviceid).get("SMS_TIMES"));
					pst.setString(4, spf.format((Date) currentMap.get(mon).get(serviceid).get("LAST_DATA_TIME")));
					pst.setDouble(5,Double.parseDouble((String)  currentMap.get(mon).get(serviceid).get("VOLUME"));
					pst.setString(6,(String) currentMap.get(mon).get(serviceid).get("EVER_SUSPEND"));
					pst.setDouble(7,Double.parseDouble((String)  currentMap.get(mon).get(serviceid).get("LAST_ALERN_THRESHOLD"));
					pst.setDouble(8,Double.parseDouble((String)  currentMap.get(mon).get(serviceid).get("LAST_ALERN_VOLUME"));
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
					pst.setString(4, (String) currentMap.get(mon).get(serviceid).get("LAST_DATA_TIME"));
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
			ErrorHandle("At updateCurrentMap occur SQLException error", e);
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
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.LAST_DATA_TIME=TO_DATE(?,'yyyy/MM/dd hh24:mi:ss'),A.VOLUME=?,A.ALERT=?,A.IS_SLOWDOWN=?,A.UPDATE_DATE=SYSDATE "
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
						pst.setString(3, (String) currentDayMap.get(day).get(serviceid).get(nccNet).get("LAST_DATA_TIME"));
						pst.setDouble(4,(Double) currentDayMap.get(day).get(serviceid).get(nccNet).get("VOLUME"));
						pst.setString(5,(String) currentDayMap.get(day).get(serviceid).get(nccNet).get("ALERT"));
						pst.setString(6,(String) currentDayMap.get(day).get(serviceid).get(nccNet).get("IS_SLOWDOWN"));
						pst.setString(7, day);
						pst.setString(8, serviceid);
						pst.setString(9, nccNet);
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
			ErrorHandle("At insertCurrent occur SQLException error", e);
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
			ErrorHandle("At insertCurrentDay occur SQLException error", e);
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
				times.add(rs.getInt("ID"));
				bracket.add(rs.getDouble("BRACKET"));
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
	Map<String,Map<String,List<Object>>> smsSettingMap = new HashMap<String,Map<String,List<Object>>>();
	
	private Boolean getSMSsetting(){
		
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
					Map<String,List<Object>> map = new HashMap<String,List<Object>>();
					
					List<Object> l1=new ArrayList<Object>(); //ID
					List<Object> l2=new ArrayList<Object>();
					List<Object> l3=new ArrayList<Object>();
					List<Object> l4=new ArrayList<Object>();
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
					
					l1.add(rs.getInt("ID"));
					l2.add(rs.getDouble("BRACKET"));
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
		
		if(smsSettingMap.size()==0){
			sql="";
			ErrorHandle("Can't found SMS Setting!");
			return false;
		}else{
			return true;
		}
	}

	Map<String,Map<String,String>> content=new HashMap<String,Map<String,String>>();
	public Boolean getSMSContents(){
		//載入簡訊內容
		Statement st = null;
		ResultSet rs = null;
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
		//開始檢查是否發送警示簡訊
		//月金額警示*************************************
		//沒有當月資料，不檢查
		if(currentMap.containsKey(sYearmonth)){
			
			String phone = null;
			int smsCount=0;
	
			//檢查這個月的資料作警示通知
			for(String serviceid: currentMap.get(sYearmonth).keySet()){
				//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
				if(msisdnMap.containsKey(serviceid))
					phone=(String) msisdnMap.get(serviceid).get("MSISDN");
				if(phone==null ||"".equals(phone)){
					sql="";
					ErrorHandle("At sendAlertSMS occur error! The serviceid:"+serviceid+" can't find msisdn to send !");
					continue;
				}
				//取得Priceplanid舊版本使用0
				String priceplanid = "0";
	
				//System.out.println("New Version check Point 1.");
				if(msisdnMap.get(serviceid)==null){
					sql="";
					ErrorHandle("At sendAlertSMS occur error! The serviceid:"+serviceid+" without in msisdnMap and can't find priceplanid!");
					continue;
				}
					
				priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID");
				if(priceplanid==null ||"".equals(priceplanid)){
					sql="";
					ErrorHandle("At sendAlertSMS occur error! The serviceid:"+serviceid+" can't find priceplanid!");
					continue;
				}
				
				//
				if(smsSettingMap.get(priceplanid)==null){
					sql="";
					ErrorHandle("At sendAlertSMS occur error! Can't find priceplanid="+priceplanid+" setting in smsSetting!");
					continue;
				}
		
				List<Object> ids = smsSettingMap.get(priceplanid).get("ID");
				List<Object> brackets = smsSettingMap.get(priceplanid).get("BRACKET");
				List<Object> msgids = smsSettingMap.get(priceplanid).get("MEGID");
				List<Object> suspends = smsSettingMap.get(priceplanid).get("SUSPEND");
	
				//取得此次批價前費用
				Double oldCharge=(Double) oldChargeMap.get(serviceid);
				if(oldCharge==null)	oldCharge=0D;
				//目前累計費用
				Double charge=(Double) currentMap.get(sYearmonth).get(serviceid).get("CHARGE");
				//兩者的費用差，運用在預估推測
				Double differenceCharge=charge-oldCharge;
				
				int smsTimes=(Integer) currentMap.get(sYearmonth).get(serviceid).get("SMS_TIMES");
				//String everSuspend =(String) currentMap.get(sYearmonth).get(serviceid).get("EVER_SUSPEND");
				Double lastAlernThreshold=(Double) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_THRESHOLD");
				Double DEFAULT_THRESHOLD = 5000D;
				String[] contentid=null;
				
				
				//System.out.println("New Version check Point 2.");
				DEFAULT_THRESHOLD = null;
				contentid=null;
				//抓取不同幣別月上限
				if("NTD".equals(pricePlanIdtoCurrency.get(priceplanid)))
					DEFAULT_THRESHOLD = getSystemConfigDoubleParam(priceplanid,"NTD_MONTH_LIMIT");
				if("HKD".equals(pricePlanIdtoCurrency.get(priceplanid)))
					DEFAULT_THRESHOLD = getSystemConfigDoubleParam(priceplanid,"HKD_MONTH_LIMIT");
				
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
					threshold = thresholdMap.get(serviceid);
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
						Double bracket = (Double) brackets.get(msgSettingID);
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
						Double bracket = (Double) brackets.get(0);
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
						if("NTD".equals(pricePlanIdtoCurrency.get(priceplanid)))
							vipSMS = getSystemConfigParam(priceplanid,"NTD_VIP_MSG_ID");
						if("HKD".equals(pricePlanIdtoCurrency.get(priceplanid)))
							vipSMS = getSystemConfigParam(priceplanid,"HKD_VIP_MSG_ID");
						
						//取不到任何上限值 跳過
						if(vipSMS == null){
							sql="";
							ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VIP SMS number! ");
							continue;
						}
						contentid = vipSMS.split(",");
						
					}
				}				
				
				if(sendSMS){
					String currency = "";
					
					currency = pricePlanIdtoCurrency.get(priceplanid);
					if(currency== null){
						ErrorHandle("The PRICEPLANID:"+priceplanid+"find currency="+currency);
						continue;
					}
					
					
					smsCount += sendSMS(serviceid,contentid,alertBracket,phone,currency);
					currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_THRESHOLD", alertBracket);				
					currentMap.get(sYearmonth).get(serviceid).put("SMS_TIMES", (smsTimes+1));
					
					//20150629 add
					Set<String> set2 = new HashSet<String>();

					if(updateMap.containsKey(sYearmonth)){	
						set2 = updateMap.get(sYearmonth);
					}
					set2.add(serviceid);
					updateMap.put(sYearmonth, set2);
				}
				//20151001 cancel if have been suspend then not do
				//if(needSuspend &&"0".equals(everSuspend)&&!isCustomized){
				if(needSuspend &&!isCustomized){
					doSuspend(serviceid,phone);
				}
			}
			logger.debug("Total send month alert "+smsCount+" ...");
			logger.debug("Log to table...executeBatch");
		}
	}
	
	//new Version
	
	public Double getSystemConfigDoubleParam(String pricePlanid,String paramName){
		//System.out.println("New Version check Point 4.");
		String result = getSystemConfigParam(pricePlanid,paramName);
		return (result!=null? Double.parseDouble(result):null);
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
	
	public void doSuspend(String serviceid,String phone){
		//中斷GPRS服務
		//20141113 新增客制定上限不執行斷網
		//20150529 將中斷的部分從發送簡訊中獨立出來

		
		String imsi = msisdnMap.get(serviceid).get("IMSI");
		if(imsi==null || "".equals(imsi))
			imsi = ServiceIdtoIMSIMap.get(serviceid);
		
		if(imsi==null || "".equals(imsi)){
			logger.debug("Suspend GPRS fail because without imsi for serviceid is "+serviceid);
			return;
		}
		
		logger.debug("Suspend GPRS ... ");		
		changeGPRSStatus(imsi,phone,"0");
		currentMap.get(sYearmonth).get(serviceid).put("EVER_SUSPEND", "1");
	}
	
	public void doResume(String serviceid,String phone){
		//中斷GPRS服務
		//20141113 新增客制定上限不執行斷網
		//20150529 將中斷的部分從發送簡訊中獨立出來

		
		String imsi = msisdnMap.get(serviceid).get("IMSI");
		if(imsi==null || "".equals(imsi))
			imsi = ServiceIdtoIMSIMap.get(serviceid);
		
		if(imsi==null || "".equals(imsi)){
			logger.debug("Resume GPRS fail because without imsi for serviceid is "+serviceid);
			return;
		}
		
		logger.debug("Resume GPRS ... ");		
		changeGPRSStatus(imsi,phone,"1");
	}
	
	
	
	public void checkResume(){
		logger.info("checkResume...");
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
			ErrorHandle("At checkResume got SQLException",e);
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
			String s2tmsisdn = msisdnMap.get(serviceid).get("MSISDN");
			String imsi = ServiceIdtoIMSIMap.get(serviceid);
			
			
			
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
		sendMail("DVRS Resume GPRS Report.",resumeReport,"DVRS Alert",mailReceiver);
		resume = false;
		
	}
	
	public int sendSMS(String serviceid,String[] contentid,Double alertBracket,String phone,String currency){
		Statement st =null;
		int smsCount=0;
		String res;

		try {
			st = conn.createStatement();
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
					
					if(content.get(s)==null){
						throw new Exception("Can't find sms content id:"+s);
					}
					String cont = content.get(s).get("CONTENT");

					cont = new String(cont.getBytes("ISO8859-1"),"big5");
					
					cont =processMag(cont,alertBracket,cPhone,currency);
					
					//WSDL方式呼叫 WebServer
					//result=tool.callWSDLServer(setSMSXmlParam(cont,phone));
					
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
					smsCount++;
					sql="INSERT INTO HUR_SMS_LOG"
							+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
							+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+new String(cont.getBytes("big5"),"ISO8859-1")+"',TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
					//寫入資料庫
					logger.debug("execute SQL : "+sql);
					st.addBatch(sql);
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
		}
		return smsCount;
	}
	
	
	

	
	public void checkDailyAlert(){
		//實做日警示部分，有今日資料才警示*************************************
		if(currentDayMap.containsKey(sYearmonthday)){
			int smsCount=0;
			String phone = null;
			for(String serviceid:currentDayMap.get(sYearmonthday).keySet()){
				
				//為了每日警示內容中帶入月累計費用才需要
				Double charge=(Double) currentMap.get(sYearmonth).get(serviceid).get("CHARGE");
				
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
					sql="";
					ErrorHandle("At sendAlertSMS occur error! The serviceid:"+serviceid+" can't find msisdn to send! ");
					continue;
				}
				
				String pricePlanID = msisdnMap.get(serviceid).get("PRICEPLANID");
				if(pricePlanID==null ||"".equals(pricePlanID)){
					sql="";
					ErrorHandle("At sendAlertSMS occur error! The ServiceID:"+serviceid+" can't find pricePlanID!");
					continue;
				}

				Double daycharge=0D;
				String alerted ="0";
				Double DEFAULT_DAY_THRESHOLD =  500D;
				String[]  contentid = {"99"};
				
				DEFAULT_DAY_THRESHOLD = null;
				contentid=null;
				//抓取不同幣別日上限
				if("NTD".equals(pricePlanIdtoCurrency.get(pricePlanID))){
					DEFAULT_DAY_THRESHOLD = getSystemConfigDoubleParam(pricePlanID,"NTD_DAY_LIMIT");
					String contentids = getSystemConfigParam(pricePlanID,"NTD_DAY_LIMIT_MSG_ID");
					if(contentids != null )
						contentid = contentids.split(",");
				}
				if("HKD".equals(pricePlanIdtoCurrency.get(pricePlanID))){
					DEFAULT_DAY_THRESHOLD = getSystemConfigDoubleParam(pricePlanID,"HKD_DAY_LIMIT");
					String contentids = getSystemConfigParam(pricePlanID,"HKD_DAY_LIMIT_MSG_ID");
					if(contentids != null )
						contentid = contentids.split(",");
				}
				
				//取不到每日上限 跳過
				if(DEFAULT_DAY_THRESHOLD== null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricePlanID+" cannot get Daily Limit! ");
					continue;
				}
				//取不到每日上限 簡訊內容跳過
				if(contentid == null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricePlanID+" cannot get Daily Limit SMS content! ");
					continue;
				}
				
				
				//累計
				for(String nccNet : currentDayMap.get(sYearmonthday).get(serviceid).keySet()){
					daycharge=daycharge+(Double) currentDayMap.get(sYearmonthday).get(serviceid).get(nccNet).get("CHARGE");
					String a=(String) currentDayMap.get(sYearmonthday).get(serviceid).get(nccNet).get("ALERT");
					if("1".equals(a)) alerted="1";
				}

				//確認是否達到每日上限
				if(daycharge>=DEFAULT_DAY_THRESHOLD && "0".equalsIgnoreCase(alerted)){
					smsCount+=sendSMS(serviceid,contentid,charge,phone,pricePlanIdtoCurrency.get(pricePlanID));	
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
	static boolean NTTalerted = false;
	static boolean NTT75alerted = false;
	//canceled
	public void checkNTTVolumeAlert(){
		
		if(NTTalerted)
			return;
		
		//NTT
		logger.info("checkNTTVolumeAlert...");
		long subStartTime = System.currentTimeMillis();
		double volumn = 0L;
		double limit = 85;
		double limit2 = 63.75;
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
						volumn=volumn+(Double) currentMap.get(sYearmonth).get(serviceid).get("VOLUME");
					}
				}
			}
		}
		boolean sendMail = false ; 
		String mail_subject="";
		String mail_content=""
				+ "Dear Sir,\n\n\n"
				+ "Please be informed that the total pooled usage of HKNet Oneness SIM subscribers (85) has reached 75%/100% (63.75GB/85.00GB) of 85GB: nn.nnGB.\n\n"
				+ "Thank you for your attention and please let me know if any queries.\n\n"
				+ "Sim2travel\n\n"
				+ "感謝！\n\n\n"
				+ "BR/Yvonne "+new SimpleDateFormat("yyyy/MM/dd").format(new Date())+"";

		String mail_sender="HKNet@sim2travel.com";
		String mail_receiver=HKNetReceiver;
		if(TEST_MODE){
			mail_receiver = "k1988242001@gmail.com.tw,ranger.kao@sim2travel.com";
		}
		volumn = volumn/1024/1024/1024;
		
		if(limit<volumn){
			sendMail=true;
			NTTalerted = true;
			mail_subject = "Notice-"+sYearmonth+": total pooled usage in HK reaches 100%";
		}else if(limit2<volumn&&!NTT75alerted){
			sendMail=true;
			NTT75alerted = true;
			mail_subject = "Notice-"+sYearmonth+": total pooled usage in HK reaches 75%";
		}
		
		if(sendMail){

			sql="INSERT INTO HUR_SMS_LOG"
					+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
					+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+mail_receiver+"','"+mail_content+"',TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'success',SYSDATE)";
			//寫入資料庫
			
			Statement st = null;
			
			try {
				sendMail(new String(mail_subject.getBytes("ISO8859-1"),"BIG5"), 
						new String(mail_content.getBytes("ISO8859-1"),"BIG5").replaceAll("%2b", "+"),mail_sender ,mail_receiver );
				
				st = conn.createStatement();
				logger.debug("execute SQL : "+sql); 
				st.executeUpdate(sql);
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
	

	
	public void addonVolumeAlert(){
		logger.info("addonVolumeAlert...");
		long subStartTime = System.currentTimeMillis();
		//20150623 新增華人上網包
		//暫存數據用量資料 Key:SERVICEID,Value:Volume
		Map<String,Double> tempMap = new HashMap<String,Double>();
		for(String day : currentDayMap.keySet()){
			//這個月的月資料
			if(sYearmonth.equalsIgnoreCase(day.substring(0, 6))){
				Date dayTime = null;
				try {
					dayTime = new SimpleDateFormat("yyyyMMdd").parse(day);
				} catch (ParseException e) {
					continue;
				}
				for(String serviceid:currentDayMap.get(day).keySet()){
					for(String mccNet:currentDayMap.get(day).get(serviceid).keySet()){
						int check=checkQosAddon(serviceid, mccNet, dayTime);
						if(check==1){
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
		//1.5 GB
		Double DEFAULT_VOLUME_THRESHOLD = 1.5*1024*1024*1024D;
		
		DEFAULT_VOLUME_THRESHOLD = getSystemConfigDoubleParam("0", "VOLUME_LIMIT1");
	
		//2.0 GB
		Double DEFAULT_VOLUME_THRESHOLD2 = 2.0*1024*1024*1024D;
		
		DEFAULT_VOLUME_THRESHOLD = getSystemConfigDoubleParam("0","VOLUME_LIMIT2");
		
		
		if(DEFAULT_VOLUME_THRESHOLD == null || DEFAULT_VOLUME_THRESHOLD2 == null){
			sql="";
			ErrorHandle("At addonVolumeAlert can't find DEFAULT_VOLUME_THRESHOLD!");
			return;
		}
		
		int smsCount=0;
		String phone = null;
		for(String serviceid:tempMap.keySet()){
			Double volume=tempMap.get(serviceid);
			Double everAlertVolume = (Double) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_VOLUME");
			
			//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
			if(msisdnMap.containsKey(serviceid))
				phone=(String) msisdnMap.get(serviceid).get("MSISDN");
			if(phone==null ||"".equals(phone)){
				sql="";
				ErrorHandle("At addonVolumeAlert occur error! The serviceid:"+serviceid+" can't find msisdn to send !");
				continue;
			}
			
			//超過發簡訊，另外確認是否已通知過
			boolean sendmsg=false;
			String [] contentid = null;
			
			if(msisdnMap.get(serviceid)==null){
				sql="";
				ErrorHandle("At sendAlertSMS occur error! The serviceid:"+serviceid+" without in msisdnMap and can't find priceplanid!");
				continue;
			}
			
			String priceplanid = msisdnMap.get(serviceid).get("PRICEPLANID"); 
			
			if(priceplanid==null){
				sql="";
				ErrorHandle("At sendAlertSMS occur error! The serviceid:"+serviceid+" can't find priceplanid!");
				continue;
			}
			
			
			//華人上網包簡訊內容
			if(volume>=DEFAULT_VOLUME_THRESHOLD2 && everAlertVolume<DEFAULT_VOLUME_THRESHOLD2){
				//2.0 GB 
				String msgids = "107";
				
				msgids = null;
				
				
				msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT2_MSG_ID");
				if(msgids == null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT2_MSG_ID! ");
					continue;
				}
				
				//new version end
				contentid = msgids.split(",");
				sendmsg=true;
	
			}
			if(!sendmsg && volume>=DEFAULT_VOLUME_THRESHOLD && everAlertVolume<DEFAULT_VOLUME_THRESHOLD){
				//1.5 GB 
				String msgids = "106";
				
				msgids = null;

				msgids = getSystemConfigParam(priceplanid, "VOLUME_LIMIT1_MSG_ID");
				 if(msgids == null){
					sql="";
					ErrorHandle("For ServiceID:"+serviceid+" PricePlanId:"+priceplanid+" cannot get VOLUME_LIMIT1_MSG_ID! ");
					continue;
				}
				
				contentid = msgids.split(",");
				sendmsg=true;
			}
			
			if(sendmsg){
				smsCount+=sendSMS(serviceid,contentid,null,phone,null);
				
				//更新CurrentMap
				currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_VOLUME",volume);
				
				//20150629 add
				Set<String> set2 = new HashSet<String>();

				if(updateMap.containsKey(sYearmonth)){	
					set2 = updateMap.get(sYearmonth);
				}
				set2.add(serviceid);
				updateMap.put(sYearmonth, set2);
			}	
		}
		logger.debug("Total send 華人上網包 volume alert SMS "+smsCount+" ...");
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	private void checkSlowDown(){
		logger.info("checkSlowDown...");
		long subStartTime = System.currentTimeMillis();
		//Key:day , value:Map(SERVICEID,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME,ALERT)))
		//重建不同的current day 結構
		Map<String,Map<String,Map<String,Map<String,Object>>>> slowTempList = new HashMap<String,Map<String,Map<String,Map<String,Object>>>>();
		for(String day : currentDayMap.keySet()){
			Map<String,Map<String,Map<String,Object>>> oneDayMap = currentDayMap.get(day);
			for(String serviceid: oneDayMap.keySet() ){
				Map<String,Map<String,Object>> serviceidMap = oneDayMap.get(serviceid);
				for(String mccmnc:serviceidMap.keySet()){
					
					Map<String,Object> mccmncMap = serviceidMap.get(mccmnc);
					
					if("1".equals(mccmncMap.get("IS_SLOWDOWN"))){
						//降速過則跳過
						continue;
					}
					
					Map<String,Object> map;
					Map<String,Map<String,Object>> map2; 
					Map<String,Map<String,Map<String,Object>>> map3;
					
					if(slowTempList.containsKey(serviceid)){
						map3=slowTempList.get(serviceid);
						if(map3.containsKey(mccmnc)){
							map2 = map3.get(mccmnc);
						}else{
							map2= new HashMap<String,Map<String,Object>>();
						}
					}else{
						map3 = new HashMap<String,Map<String,Map<String,Object>>>();
						map2= new HashMap<String,Map<String,Object>>();
					}
					
					map = new HashMap<String,Object>();
					map.put("LAST_FILEID", mccmncMap.get("LAST_FILEID"));
					map.put("LAST_DATA_TIME", mccmncMap.get("LAST_DATA_TIME"));
					map.put("CHARGE", mccmncMap.get("CHARGE"));
					map.put("VOLUME", mccmncMap.get("VOLUME"));
					map.put("ALERT", mccmncMap.get("ALERT"));
					
					map2.put(day, map);
					map3.put(mccmnc, map2);
					
					slowTempList.put(serviceid, map3);
					
				}
			}
		}
		
		if(slowTempList.isEmpty()){
			logger.info("need not slowdown");
			return;
		}
		
		//確認是否需要降速
		for(Map<String,Object> m: slowDownList){
			String mccmnc = (String) m.get("MNO");
			Set<String> mccmncs = new HashSet<String>();
			for(String s:mccmnc.split(","))
				mccmncs.add(s);
			
			String priceplan = (String) m.get("PRICEPLAN");
			Set<String> priceplans = new HashSet<String>();
			for(String s:priceplan.split(","))
				priceplans.add(s);
			Integer limit = (Integer) m.get("LIMIT");	
			String nGPRSName = (String) m.get("GPRS_NAME");
			Date sDate,eDate;
			try {
				sDate = year_month_day_sdf.parse((String) m.get("TIMESTART"));
				eDate = year_month_day_sdf.parse((String) m.get("TIMEEND"));
			} catch (ParseException e1) {
				ErrorHandle("For slowdown date mcc="+mccmncs+" ,priceplan="+priceplan+" parse date error.");
				continue;
			}
			
			Set<String> days = new HashSet<String>();
			Set<String> mccs = new HashSet<String>();

			for(String serviceid:slowTempList.keySet()){
				double total = 0D;
				days.clear();
				mccs.clear();
				String pricplanID = msisdnMap.get(serviceid).get("PRICEPLANID");
				if (!priceplans.contains(pricplanID)) {
					continue;
				}
				Map<String,Map<String,Map<String,Object>>> mccMncMap = slowTempList.get(serviceid);
				
				
				try {
					for(String mcc:mccmncs){
						Map<String, Map<String, Object>> dayMap = mccMncMap.get(mcc);
						
						if(dayMap!=null){
							for (String day : dayMap.keySet()) {
								Date d = null;
								
									d = year_month_day_sdf.parse(day);
	
								if ((d.after(sDate) || d.equals(sDate))	&& d.before(eDate)) {
									total += (Double) dayMap.get(day).get(
											"VOLUME");
									days.add(day);
									mccs.add(mcc);
								}
							}
						}	
					}
				} catch (ParseException e) {
					ErrorHandle("For slowdown when parse customer date error! serviceid="+serviceid+".");
					continue;
				}
				if(limit<=total){
					for(String mcc:mccs){
						for (String day : days) {
							currentDayMap.get(day).get(serviceid).get(mcc).put("IS_SLOWDOWN", "1");
							
							//標記Update
							Map <String,Set<String>> map6 = new HashMap<String,Set<String>>();
							Set<String> set1 = new HashSet<String>();					

							if(updateMapD.containsKey(day)){	
								map6 = updateMapD.get(day);
								if(map6.containsKey(serviceid)){
									set1 = map6.get(serviceid);
								}
							}
							set1.add(mcc);
							map6.put(serviceid, set1);
							updateMapD.put(day, map6);
						}
					}
					doSlowDown(serviceid,nGPRSName);
				}		
			}		
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	private void doSlowDown(String serviceid,String nGPRSName){
		logger.info("doSlowDown...");
		
		suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
		String imsi = msisdnMap.get(serviceid).get("IMSI");
		String msisdn = msisdnMap.get(serviceid).get("MSISDN");
		String oGPRSName = null;
		
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn2.createStatement();
			
			String sql2 = 
					"SELECT B.SERVICEID, B.SERVICECODE, C.PDPSUBSID, C.PDPSUBSNAME "
					+ "FROM PARAMETERVALUE A, SERVICE B, GPRSSUBSCRIPTION C "
					+ "WHERE A.PARAMETERVALUEID=3749 AND A.SERVICEID=B.SERVICEID "
					+ "AND B.PRICEPLANID=139 AND B.STATUS IN (1,3) "
					+ "AND A.VALUE=C.PDPSUBSID AND B.SERVICECODE = '"+msisdn+"' ";
			
			logger.info("Query PDPSUBSNAME SQL:"+sql2);
			rs = st.executeQuery(sql2);
			
			while(rs.next()){
				oGPRSName = rs.getString("PDPSUBSNAME");
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
		
		
		if(oGPRSName == null){
			ErrorHandle("At doSlowDown occur error! Can't find serviceid="+serviceid+"'s PDPSUBSNAME.");
			return ;
		}
		

		PreparedStatement pst = null;
		try {
			
			sql = "";
			//20141118 add 傳回suspend排程的 service order nbr
			Map<String,String> orderNBR = null;
			orderNBR = sus.ReqStatus_16_Act(imsi, msisdn, "1", nGPRSName);
			serviceOrderNBR.add(orderNBR);
			sql=
					"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
					+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS,IS_SLOWDOWN) "
					+ "VALUES(?,?,SYSDATE,?,?,?)";
			
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
	
	private void doResumeSpeed(){
		logger.info("doResumeSpeed...");
		
		String resumeSpeedReport = "";
		
		sql = "select A.MSISDN,A.GPRS_STATUS,B.SERVICEID,C.IMSI,TO_CHAR(A.CREATE_DATE,'yyyy-MM-dd hh24:mi:ss') CREATE_DATE "
				+ "from HUR_SUSPEND_GPRS_LOG A,SERVICE B,IMSI C "
				+ "WHERE A.MSISDN=B.SERVICECODE AND B.SERVICEID = C.SERVICEID "
				+ "AND A.IS_SLOWDOWN = '1' AND RESULT = '000' AND A.CREATE_DATE>=sysdate-1 AND A.CREATE_DATE<=sysdate ";
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
				//20141118 add 傳回suspend排程的 service order nbr
				Map<String,String> orderNBR = null;
				orderNBR = sus.ReqStatus_16_Act(imsi, msisdn , "1", GPRS_STATUS);
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
				
				resumeSpeedReport += msisdn+" had been resume speed!( slowDown at "+time+")\n";
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
				
			}
			
		} catch (SQLException e) {
			ErrorHandle("At doSlowDown occur SQLException error!", e);
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
		String mailReceiver = "k1988242001@gmail.com";
		if(!TEST_MODE){
			mailReceiver+=",Yvonne.lin@sim2travel.com";
		}
		
		logger.info("Send resumeSpeedReport result.");
		sendMail("DVRS Resume speed Report.",resumeSpeedReport,"DVRS Alert",mailReceiver);
		resumeSpeed = false;
		
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

		if(getSMSsetting()&&getSMSContents()){
			try {
				ckeckMonthAlert();
				checkDailyAlert();
				//華人上網包降速提醒簡訊
				addonVolumeAlert();
				//20160624 對特定條件客戶進行降速
				checkSlowDown();
				//20160701 mod ，重新啟用NTT用量告警
				checkNTTVolumeAlert();
				
			} catch (Exception e) {
				sql="";
				ErrorHandle("At sendAlertSMS got Exception!",e);
			}
		}else{
			return;
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
	
	private String processMag(String msg,Double bracket,String cPhone,String currency){
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
		param=param.replace("{{Text}}",Text.replaceAll("/+", "%2b") );
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
	private void changeGPRSStatus(String imsi,String msisdn,String GPRSStatus){
		logger.info("changeGPRSStatus...");
		
		suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
		PreparedStatement pst = null;
		try {
			
			sql = "";
			//20141118 add 傳回suspend排程的 service order nbr
			Map<String,String> orderNBR=sus.ReqStatus_16_Act(imsi, msisdn,GPRSStatus);
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
	}
	
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

		mailReceiver=props.getProperty("mail.Receiver");
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
			
			if("000".equals(cSt))
				break;
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
		
		if(TEST_MODE){
			DVRSmain rf =new DVRSmain();
			while(true){
				rf.process();
				try {
					Thread.sleep(10*60*1000);
				} catch (InterruptedException e) {
				}
			}
		}else{
			regilarHandle();
		}
	}
	
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
		
	}
	
	public void run(){
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
			ErrorHandle("process error!");
		}finally{
			//將動作交給下個thread
			executing=false;
		}
	}
	long connectionTime1,connectionTime2;
	
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
			connectionTime1 = startTime;
			connectionTime2 = startTime;
			
			if(
					setDayDate() && //設定日期
					setIMSItoServiceIDMap()&&//設定IMSI至ServiecId的對應
					//20160624 add
					setSlowDownList()&&
	
					setThreshold()&&//取出HUR_THRESHOLD
					setDataRate()&&//取出HUR_DATARATE
					setMsisdnMap()&&//取出msisdn資訊
					setIPtoMccmncList()&&//20141211 add IP 對應到 MCCMNC
					setSERVICEIDtoVLN()&&//IMSI 對應到 vln
					setVLNtoTADIG()&&//vln 對應到 TADIG
					setTADIGtoMCCMNC()&&//TADIG 對應到 MCCMNC
					setCostomerNumber()&&//國碼對應表(客服,國名)
					setAddonData()&&//華人上網包申請資料
					setQosData()&&//設定SX001,SX002資料
					setCurrencyMap()&&//設定PricePlanID對應幣別
					setSystemConfig()&&//系統Comfig設定
					(currentMap.size()!=0||setCurrentMap())&&//取出HUR_CURRENT
					setoldChargeMap()&&//設定old 20151027 modified update old Map every times
					(currentDayMap.size()!=0||setCurrentMapDay())){//取出HUR_CURRENT

				if(resume){
					checkConnection();
					checkResume();
				}
				if(resumeSpeed){
					checkConnection();
					doResumeSpeed();
				}
				

				//開始批價 
				checkConnection();
				charge();

				//發送警示簡訊
				checkConnection();
				sendAlertSMS();
				
				//回寫批價結果
				checkConnection();
				//取消自動Commit
				cancelAutoCommit();
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
						ErrorHandle("At commit occur SQLException error!",e);
					}
				}
				//suspend的後續追蹤處理
				processSuspendNBR();
			}

			// 程式執行完成
			logger.info("Program execute time :" + (System.currentTimeMillis() - startTime));
			closeConnect();

		} else {
			sql="";
			ErrorHandle("Cannot connect to DB(connect is null)!!");
		}
	}	
	
	
	private void checkConnection(){
		if(System.currentTimeMillis()-connectionTime1>30*60*1000){
			if(conn!=null){
				try {
					conn.close();
					
				} catch (SQLException e1) {
				}
			}
			connectDB();
			connectionTime1 = System.currentTimeMillis();
		}
		if(System.currentTimeMillis()-connectionTime2>30*60*1000){
			if(conn2!=null){
				try {
					conn2.close();
					
				} catch (SQLException e1) {
				}
			}
			connectDB2();
			connectionTime2 = System.currentTimeMillis();
		}
	}
}



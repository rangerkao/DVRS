
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
	

	
	private static String sql="";
	private static String errorMsg="";
	
	//Hur Data conf
	private static Integer dataThreshold=null;//CDR資料一批次取出數量
	//private static Integer lastfileID=null;//最後批價檔案號
	private static Double exchangeRate=null; //港幣對台幣匯率，暫訂為4
	private static Double kByte=null;//RATE單位KB，USAGE單位B
	
	//日期設定
	SimpleDateFormat spf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	SimpleDateFormat year_month_day_sdf = new SimpleDateFormat("yyyyMMdd");
	SimpleDateFormat year_month_sdf = new SimpleDateFormat("yyyyMM");
	SimpleDateFormat day_hour_sdf = new SimpleDateFormat("ddHH");
	
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
	
	//20160706 add
	long subStartTime = 0;

	
	Set<Map<String,String>> serviceOrderNBR = new HashSet<Map<String,String>>();
	
	
	Map <String,Double> cdrChargeMap = new HashMap<String,Double>();
	
	
	//20150505 add
	static Map <String,Set<String>> updateMap = new HashMap<String,Set<String>>();
	static Map <String,Map <String,Set<String>>> updateMapD = new HashMap<String,Map <String,Set<String>>>();
	
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
	
	//20160719 add
	/**
	 * 設定流量包
	 */
	
	//20160715 add
	//key:ServiceID,subMap MCC,Volume
	Map<String,Map<String,Double>> volumeList = new HashMap<String,Map<String,Double>>();//流量統計
	//key:ServiceID,subMap key:MCC,Map start_date,end_date,currency,
	Map<String,Map<String,Map<String,String>>> volumePocketMap = new HashMap<String,Map<String,Map<String,String>>>();//流量包對應
	//TODO
	private boolean setVolumePocketMap(){
		logger.info("setVolumePocketMap...");
		volumePocketMap.clear();
		volumeList.clear();	
		sql = ""
				+ "SELECT A.SERVICEID,A.MCC,A.START_DATE,A.END_DATE,A.CURRENCY,A.ALERTED,A.TYPE,A.LIMIT "
				+ "FROM HUR_VOLUME_POCKET A WHERE A.CANCEL_TIME IS NULL ";
	
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
				
				Map<String, Map<String, String>> m2 = new HashMap<String,Map<String,String>>();
				if(volumePocketMap.containsKey(rs.getString("SERVICEID")))
					m2=volumePocketMap.get(rs.getString("SERVICEID"));
				m2.put(rs.getString("MCC"), m);
				volumePocketMap.put(rs.getString("SERVICEID"), m2);
				
				//volumeList歸零
				Map<String,Double> m3 = new HashMap<String,Double>();
				if(volumeList.containsKey(rs.getString("SERVICEID")))
					m3=volumeList.get(rs.getString("SERVICEID"));
				m3.put(rs.getString("MCC"), 0d);
				volumeList.put(rs.getString("SERVICEID"), m3);
			}
			
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At set setVolumePocketMap Got a SQLException", e);
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
	
	//TODO
	//20160721 add
	/**
	 * 當美國流量包開始計算時，寄出簡訊告知
	 * @return
	 */
	private boolean sendStartPocketDateSMS(){
		boolean result =false;
		for(String serviceid : volumePocketMap.keySet()){
			for(String mcc : volumePocketMap.get(serviceid).keySet()){
				if("0".equals(volumePocketMap.get(serviceid).get(mcc).get("TYPE")) && sYearmonthday.equals(volumePocketMap.get(serviceid).get(mcc).get("START_DATE"))){
					//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
					String phone = null;
					if(msisdnMap.containsKey(serviceid))
						phone=(String) msisdnMap.get(serviceid).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						sql="";
						ErrorHandle("At checkVolumePocket sendAlertSMS occur error! The serviceid:"+serviceid+",MCC:"+mcc+" can't find msisdn to send! ");
						continue;
					}
					String msgID = getSystemConfigParam("0", "VOLUME_POCKET_START_MSG");
					//發送簡訊
					sendSMS(serviceid,msgID.split(","),null,phone,null);
				}
			}
		}		
		
		checkPocketStart = false;
		return result;
	}

	/**
	 * 設定計費週期
	 * 取特定日期那個月的，前面加上calendar.setTime(date);設定date日期
	 */
	private boolean setDayDate(){
		logger.info("setMonthDate...");
		subStartTime = System.currentTimeMillis();

		//目前時間
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		
		//TODO test
		//calendar.set(2016,6, 28, 1, 30, 0);
		System.out.println(calendar.getTime());
		
		calendar.setTimeInMillis(calendar.getTimeInMillis()-1000*60*60);	//系統時間提前一小時,當00：30執行時，所處理的資料為前一天的23：00
		programeTime = day_hour_sdf.format(calendar.getTime());
		sYearmonth = year_month_sdf.format(calendar.getTime());
		sYearmonthday = year_month_day_sdf.format(calendar.getTime());
		
		
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)-1);	//上個月時間，減掉Month會-30天，採取到1號向前，確定跨月
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-1);
		sYearmonth2 = year_month_sdf.format(calendar.getTime());
		
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		//當執行時間為1日0點時進行復網
		if("0100".equals(programeTime)){
			resume = true;
			NTTalerted = false;
			NTT75alerted = false;
			updateSystemConfig("NTT_ALERTED","0"); //將月份為單位的NTT警示Flag歸零
		}
		//201607 add
		//當執行時間為0點時進行速度恢復
		if("00".equals(programeTime.substring(2))){
			resumeSpeed = true;
			checkPocketStart = true;
		}
		
		//20160816
		//為了長期運作，必須清除不需要資料
		for(String s : currentMap.keySet()){
			if(!sYearmonth.equalsIgnoreCase(s)&&!sYearmonth2.equalsIgnoreCase(s)){
				currentMap.remove(s);
			}
		}
		for(String s : existMap.keySet()){
			if(!sYearmonth.equalsIgnoreCase(s)&&!sYearmonth2.equalsIgnoreCase(s)){
				currentMap.remove(s);
			}
		}
		for(String s : currentDayMap.keySet()){
			if(!sYearmonth.equalsIgnoreCase(s.substring(0,6))&&!sYearmonth2.equalsIgnoreCase(s.substring(0,6))){
				currentDayMap.remove(s);
			}
		}
		for(String s : existMapD.keySet()){
			if(!sYearmonth.equalsIgnoreCase(s.substring(0,6))&&!sYearmonth2.equalsIgnoreCase(s.substring(0,6))){
				currentDayMap.remove(s);
			}
		}
		
		return true;
	}
	
	

	/**
	 * 設定華人上網包MCCMNC資料 20141118 排除華人上網包
	 */
	Set<String> sSX001 = new HashSet<String>();
	Set<String> sSX002 = new HashSet<String>();
	private boolean setQosData(){
		
		logger.info("setQosData...");
		sSX001.clear();
		sSX002.clear();
		
		subStartTime = System.currentTimeMillis();
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
	
	//20160624 add
	/**
	 * 設定降速參數
	 * Map<Type,List<Map<String,Value>>>
	 */
	//20160624 add
		//List<Map<String,String>> slowDownList = new ArrayList<Map<String,String>>();
		Map<String,List<Map<String,String>>> slowDownMap = new HashMap<String,List<Map<String,String>>>();
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
				sql = "SELECT A.MCCMNC,A.PRICEPLANID,A.LIMIT,A.START_DATE,A.END_DATE,A.ISDAYLY,A.GPRS_NAME "
						+ "from HUR_SLOWDOWN_LIST A "
						+ "WHERE sysdate > =to_date(A.START_DATE,'yyyyMMdd') AND (A.END_DATE is null or sysdate<=to_date(A.START_DATE,'yyyyMMdd')+1)";
				
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
	Map<String,Map<String,String>> systemConfig = new HashMap<String,Map<String,String>>();
	public boolean setSystemConfig(){
		
		logger.info("setSystemConfig...");
		boolean result = false;
		subStartTime = System.currentTimeMillis();
		
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
	 * 取出 HUR_CURRENTE table資料
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
				map.put("VOLUME", rs.getString("VOLUME"));
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
		return result;
	}
	
	/**
	 * 保留這個月舊資料，作為預測費用使用
	 */
	Map<String,Double> oldChargeMap = new HashMap<String,Double>();
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
					"SELECT A.SERVICEID,A.CHARGE,A.LAST_FILEID,to_char(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,A.VOLUME,A.UPDATE_DATE,A.CREATE_DATE,A.MCCMNC,A.DAY,A.ALERT,A.IS_SLOWDOWN "
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
				
				//TODO
				//20160719 set volumeList
				if(volumePocketMap.containsKey(serviceid)){
					
					if(volumePocketMap.get(serviceid).containsKey(mccmnc.substring(0, 3))){
						String mcc = mccmnc.substring(0, 3);
						String startDate = (String) volumePocketMap.get(serviceid).get(mcc).get("START_DATE");
						String endDate = (String) volumePocketMap.get(serviceid).get(mcc).get("END_DATE");
						
						if(day.equals(startDate)||
								(	year_month_day_sdf.parse(day).after(year_month_day_sdf.parse(startDate))&& 
									year_month_day_sdf.parse(day).before(year_month_day_sdf.parse(endDate)))||
								day.equals(endDate)){
							
								Double v = 0d;
								
								if(volumeList.containsKey(serviceid)&&volumeList.get(serviceid).containsKey(mcc)){
									v = volumeList.get(serviceid).get(mcc);
								}
								v += parseDouble((String) map.get("VOLUME"));
								
								Map<String,Double> m = new HashMap<String,Double>();
								m.put(mcc, v);
								volumeList.put(serviceid, m);
						}
					}
				}
			}
			
			result = true;

		} catch (SQLException e) {
			ErrorHandle("At setCurrentMapDay occur SQLException error", e);
		} catch (ParseException e) {
			ErrorHandle("At setCurrentMapDay occur ParseException error", e);
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
	 * 找出符合的Priceplanid、MCCMNC，接著從List比對有效資料
	 */
	Map<String,Map<String,List<Map<String,String>>>> dataRate = new HashMap<String,Map<String,List<Map<String,String>>>>();
	Map<String,String> pricePlanIdtoCurrency = new HashMap<String,String>();
	//20150324 modify add network info
	private boolean setDataRate(){
		
		logger.info("setDataRate...");
		subStartTime = System.currentTimeMillis();
		boolean result = false;
		dataRate.clear();
		
		Statement st = null;
		ResultSet rs = null;
		sql=""
				+ "SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY,A.PRICEPLANID,A.DAYCAP,B.NETWORK,A.START_TIME,A.END_TIME "
				+ "FROM HUR_DATA_RATE A,HUR_MCCMNC B "
				+ "where A.MCCMNC=B.MCCMNC";
		
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);

			while(rs.next()){

				String mccmnc =rs.getString("MCCMNC");
				String priceplanID =rs.getString("PRICEPLANID");
				
				Map<String,String> map=new HashMap<String,String>();
				Map<String,List<Map<String,String>>> map2=new HashMap<String,List<Map<String,String>>>();
				//20150427  因增加有效日期，修改結構
				List<Map<String,String>> list = new ArrayList<Map<String,String>>();
				
				map.put("RATE", rs.getString("RATE"));
				map.put("CHARGEUNIT", rs.getString("CHARGEUNIT"));
				map.put("CURRENCY", rs.getString("CURRENCY"));
				map.put("DAYCAP", rs.getString("DAYCAP"));
				map.put("NETWORK", rs.getString("NETWORK"));
				map.put("STARTTIME", rs.getString("START_TIME").replace("/", ""));
				map.put("ENDTIME", rs.getString("END_TIME")!=null?rs.getString("END_TIME").replace("/", ""):null);

				if(dataRate.containsKey(priceplanID)){
					map2=dataRate.get(priceplanID);
					if(map2.containsKey(mccmnc)){
						list=map2.get(mccmnc);
					}
				}
				
				list.add(map);
				map2.put(mccmnc, list);
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
	Map<String,String> thresholdMap = new HashMap<String,String>();
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
	Map<String,String> SERVICEIDtoVLN =new HashMap<String,String>();
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
	Map<String,String> VLNtoTADIG =new HashMap<String,String>();
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
	Map<String,String> TADIGtoMCCMNC =new HashMap<String,String>();
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
	Map<String,Map<String,String>> codeMap = new HashMap<String,Map<String,String>>();
	private boolean setCostomerNumber(){
		
		logger.info("setCostomerNumber...");
		codeMap.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		
		boolean result =false;
		sql=
				"SELECT A.CODE,A.PHONE,A.NAME  FROM HUR_CUSTOMER_SERVICE_PHONE A ";
		
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
		subStartTime = System.currentTimeMillis();
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
	List<Map<String,String>> addonDataList = new ArrayList<Map<String,String>>();
	private boolean setAddonData(){
		
		logger.info("setAddonData...");
		addonDataList.clear();
		
		subStartTime = System.currentTimeMillis();
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		
		sql=
				"SELECT A.S2TIMSI IMSI,A.S2TMSISDN MSISDN,A.SERVICEID,A.SERVICECODE,To_char(A.STARTDATE,'yyyyMMdd') STARTDATE,To_char(A.ENDDATE,'yyyyMMdd') ENDDATE FROM ADDONSERVICE_N A ";
		try {
			logger.debug("Execute SQL : "+sql);
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			logger.info("Query end!");
			while(rs.next()){
				Map<String,String> map = new HashMap<String,String>();
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
	List<Map<String,String>> IPtoMccmncList = new ArrayList<Map<String,String>>();
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
	Map<String,Map<String,String>> msisdnMap = new HashMap<String,Map<String,String>>();
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
					+ "			AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 ) da, (select serviceid from hur_current where month in ('"+sYearmonth+"','"+sYearmonth2+"')) cho "
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
					+ "			AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 ) da, (select imsi from hur_data_usage where charge is null) cho "
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
	Map<String,String> IMSItoServiceIdMap = new HashMap<String,String>();
	Map<String,String> ServiceIdtoIMSIMap = new HashMap<String,String>();
	private boolean setIMSItoServiceIDMap(){
		boolean result =false;
		logger.info("setIMSItoServiceIDMap...");
		logger.info("setServiceIDtoImsiMap...");
		IMSItoServiceIdMap.clear();
		ServiceIdtoIMSIMap.clear();
		subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;

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
						+ "(	select imsi from hur_data_usage where charge is null)  cho "
						+ "		WHERE da.imsi = cho.imsi ";
				logger.info("Execute SQL :"+sql);
				rs=st.executeQuery(sql);
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
			//DEFAULT_THRESHOLD=(props.getProperty("progrma.DEFAULT_THRESHOLD")!=null?parseDouble(props.getProperty("progrma.DEFAULT_THRESHOLD")):5000D);//預設月警示量
			//DEFAULT_DAY_THRESHOLD=(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")!=null?parseDouble(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")):500D);//預設日警示量
			//DEFAULT_DAYCAP=(props.getProperty("progrma.DEFAULT_DAYCAP")!=null?parseDouble(props.getProperty("progrma.DEFAULT_DAYCAP")):500D);
			//DEFAULT_VOLUME_THRESHOLD=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")!=null?parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")):1.5*1024*1024*1024D);//預設流量警示(降速)，1.5GB;
			//DEFAULT_VOLUME_THRESHOLD2=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")!=null?parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")):1.5*1024*1024*1024D);//預設流量警示(降速)，15GB;
			DEFAULT_PHONE=props.getProperty("progrma.DEFAULT_PHONE");
			RUN_INTERVAL=(props.getProperty("progrma.RUN_INTERVAL")!=null?Integer.parseInt(props.getProperty("progrma.RUN_INTERVAL")):3600);
			HKNetReceiver = props.getProperty("program.HKNetReceiver");
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
	 * @throws ParseException 
	 */
	private int checkQosAddon(String serviceID,String mccmnc,Date callTime) throws ParseException{
		

		int cd=0;
				
		//20150623 search by serviceid 
		if(serviceID!=null && !"".equals(serviceID)){
			for(Map<String,String> m : addonDataList){
				Calendar startTime = Calendar.getInstance(),endTime = Calendar.getInstance();
				startTime.setTime(year_month_day_sdf.parse(m.get("STARTDATE")));
				startTime.set(Calendar.HOUR_OF_DAY, 0);
				startTime.set(Calendar.MINUTE, 0);
				startTime.set(Calendar.SECOND, 0);
				
				
				if(m.get("ENDDATE")!=null){
					endTime.setTime(year_month_day_sdf.parse(m.get("ENDDATE")));
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
	 *確認是否為數據預付包 客戶
	 *20150115 ALTER 取消檢查門號
	 */
	List<Map<String,String>> insertVolumeList = new ArrayList<Map<String,String>> ();
	private int checkDataPrepay(String serviceID,String cMccmnc,Date callTime,String cPriceplan){
		int cd=0;
		
		for(Map<String,String> map :slowDownMap.get("0")){
			String mccmnc = (String) map.get("MNO");
			Set<String> mccmncs = new HashSet<String>();
			for(String s:mccmnc.split(","))
				mccmncs.add(s);
			
			String priceplan = (String) map.get("PRICEPLAN");
			Set<String> priceplans = new HashSet<String>();
			for(String s:priceplan.split(","))
				priceplans.add(s);
			
			if(priceplans.contains(cPriceplan)&&(mccmncs.contains(cMccmnc)||mccmncs.contains(cMccmnc.substring(0,3)))){
				//屬於降速非每日，暫時只有數據包符合
				cd = 1;
				//XXX
				if(volumePocketMap.containsKey(serviceID)&&volumePocketMap.get(serviceID).containsKey(cMccmnc.substring(0,3))){
					Double volume = volumeList.get(serviceID).get(cMccmnc.substring(0,3));
					//使用超過1G過量警示
					if(volume/1024/1024>1024){
						ErrorHandle("The customer(serviceid="+serviceID+") had been exceed 1G.");
					}
				}else{
					//如果是非每日降速包，流量統計無資料的話，新增
					String mcc = cMccmnc.substring(0,3);
					String sDate = year_month_day_sdf.format(callTime);
					String eDate = year_month_day_sdf.format(new Date(callTime.getTime()+7*24*60*60*1000));//結束日期為起始+7天，供8天
					Map<String,String> iMap = new HashMap<String,String>();
					iMap.put("SERVICEID", serviceID);
					iMap.put("MCC", mcc);
					iMap.put("SDATE", sDate);
					iMap.put("EDATE", eDate);
					iMap.put("TYPE", "1");
					iMap.put("LIMIT", (String) map.get("LIMIT"));
					insertVolumeList.add(iMap);
					
					//加入Map供之後檢查
					Map<String, String> m = new HashMap<String,String>();
					m.put("START_DATE", sDate);
					m.put("END_DATE", eDate);
					m.put("CURRENCY", "NTD");
					m.put("ALERTED", "0");
					m.put("TYPE", "1");
					m.put("LIMIT", (String)map.get("LIMIT"));
					
					Map<String, Map<String, String>> m2 = new HashMap<String,Map<String,String>>();
					if(volumePocketMap.containsKey(serviceID))
						m2=volumePocketMap.get(serviceID);
					m2.put(mcc, m);
					volumePocketMap.put(serviceID, m2);
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
	/**
	 * 開始批價
	 */
	private void charge(){
		logger.info("charge...");
		subStartTime = System.currentTimeMillis();
		
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
					
					//抓到對應的Serviceid
					//20150115 add
					String serviceid = null;
			
					//從IMSI找尋ServiceID
					//從換卡記錄找IMSI最後的ServiceID
					if(IMSItoServiceIdMap.get(imsi)!=null)
						serviceid = IMSItoServiceIdMap.get(imsi);
					
					if(serviceid==null || "".equals(serviceid)){
						sql="";
						ErrorHandle("For CDR usageId="+usageId+" which can't find  ServceID." );
						continue;
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
					

					String mcc = mccmnc.substring(0,3);

					if(checkQosAddon(serviceid, mccmnc, callTime)==1){
						//是華人上網包，不批價設定為0
					}else if(checkDataPrepay(serviceid, mccmnc, callTime, pricplanID)==1){
						//是台灣數據預付卡，不批價設定為0
						//將流量加起來
						Map<String,Double> m = new HashMap<String,Double>();
						double v = 0d;
						if(volumeList.containsKey(serviceid)){
							m = volumeList.get(serviceid);
							v = m.get(mcc);
						}
						v += volume;
						m.put(mcc, v);
						volumeList.put(serviceid, m);
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
							
							//TODO
							//20160719 add
							//美國流量包客戶計費
							boolean inPacketRange=false;
							if(volumeList.containsKey(serviceid)&&volumeList.get(serviceid).containsKey(mcc)){
								String startDate = (String) volumePocketMap.get(serviceid).get(mcc).get("START_DATE");
								String endDate = (String) volumePocketMap.get(serviceid).get(mcc).get("END_DATE");
								int type = parseInt((String) volumePocketMap.get(serviceid).get(mcc).get("TYPE"));
								double pocketLimit = parseDouble((String) volumePocketMap.get(serviceid).get(mcc).get("LIMIT"));
							
								if(type == 0 &&
										callTime.after(year_month_day_sdf.parse(startDate))&& 
										callTime.before(year_month_day_sdf.parse(endDate))){
									inPacketRange = true;
									double v = volumeList.get(serviceid).get(mcc);
									v=v*kByte;//從Byte換成KB
									v += volume;
			
									if(v<pocketLimit){
										volume = 0d;
									}else{
										volume = v- pocketLimit;
										v = pocketLimit;
									}
									Map<String,Double> m = volumeList.get(serviceid);
									m.put(mcc, v/kByte);//還原成B
									volumeList.put(serviceid, m);
								}
							}
							double ec=1;
							boolean haveRate = false;
							
							for(Map<String,String> m : dataRate.get(pricplanID).get(mccmnc)){
								Date sdate = year_month_day_sdf.parse(m.get("STARTTIME"));
								Date edate = m.get("ENDTIME")!=null?year_month_day_sdf.parse(m.get("ENDTIME")):null;
								if(sdate.equals(callTime)||sdate.before(callTime)&&(edate==null || edate.after(callTime))){
									
									//取消幣別轉換，直接以原幣計價
									/*if("HKD".equalsIgnoreCase(currency))
										ec=exchangeRate;*/
										
									Double rate=parseDouble((String) m.get("RATE"));
									Double unit=parseDouble((String) m.get("CHARGEUNIT"));
									charge=volume*rate*ec/unit;
									
									//在數據包期間內超過流量金額為95折
									if(inPacketRange) charge*=0.95;
									
									
									dayCap=parseDouble((String) m.get("DAYCAP"));
									haveRate=true;
									break;
								}
							}		
							//假設在日期範圍內找無適合的費率
							if(!haveRate){
								sql="";
								ErrorHandle("usageId:"+usageId+",CALLTIME:"+callTime.toString()+" can't find datarate in date range ! ");
								continue;
							}							
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
					
					
					//日累計處理
					String cDay=year_month_day_sdf.format(callTime);
					Double oldCharge=0D;
					Double oldvolume=0D;
					String alert="0";
					Map<String, Map<String, Map<String, String>>> map=new HashMap<String,Map<String,Map<String,String>>>();
					Map<String,Map<String,String>> map2=new HashMap<String,Map<String,String>>();
					Map<String,String> map3=new HashMap<String,String>();
					
					//取出舊值
					if(currentDayMap.containsKey(cDay)){
						map=currentDayMap.get(cDay);
						if(map.containsKey(serviceid)){
							map2=map.get(serviceid);
							if( map2.containsKey(nccNet)){
								map3=map2.get(nccNet);

								oldCharge=parseDouble((String) map3.get("CHARGE"));
								
								logMsg+="The old Daily charge is "+oldCharge+". ";
								
								//summary charge
								charge=oldCharge+charge;
								charge=FormatDouble(charge, "0.0000");
								alert=(String)map3.get("ALERT");
								oldvolume=parseDouble((String) map3.get("VOLUME"));
							
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
					map3.put("ALERT",alert);
					map2.put(nccNet, map3);
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
					set1.add(nccNet);
					map6.put(serviceid, set1);
					updateMapD.put(cDay, map6);
					

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
						pst.setDouble(1,parseDouble((String) currentDayMap.get(day).get(serviceid).get(nccNet).get("CHARGE")));
						pst.setInt(2, parseInt((String) currentDayMap.get(day).get(serviceid).get(nccNet).get("LAST_FILEID")));
						pst.setString(3, (String) currentDayMap.get(day).get(serviceid).get(nccNet).get("LAST_DATA_TIME"));
						pst.setDouble(4,parseDouble((String) currentDayMap.get(day).get(serviceid).get(nccNet).get("VOLUME")));
						pst.setString(5,(String) currentDayMap.get(day).get(serviceid).get(nccNet).get("ALERT"));
						pst.setString(6,"1".equals((String) currentDayMap.get(day).get(serviceid).get(nccNet).get("IS_SLOWDOWN"))?"1":"0");
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
		subStartTime = System.currentTimeMillis();
		
		boolean result = false;
		int count = 0 ;
		sql=
				"INSERT INTO HUR_CURRENT (SERVICEID,MONTH,CREATE_DATE) VALUES(?,?,SYSDATE)";
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
							logger.info("Execute insertCurrentMap Batch."+serviceid);
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
		subStartTime = System.currentTimeMillis();

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
	Map<String,Map<String,List<String>>> smsSettingMap = new HashMap<String,Map<String,List<String>>>();
	
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
				String phone =(String) msisdnMap.get(serviceid).get("MSISDN");

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
					
					
					smsCount += sendSMS(serviceid,contentid,alertBracket,phone,currency);
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
	
	
	/**
	 * 對因5000塊斷網客戶，進行復網，排除VIP，排除主動提出斷網需求客戶
	 */
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
	
				String phone = (String) msisdnMap.get(serviceid).get("MSISDN");
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
		String mail_content=""
				+ "Dear Sir,\n\n\n"
				+ "Please be informed that the total pooled usage of HKNet Oneness SIM subscribers (85) has reached 75%/100% ("+limit2+"GB/"+limit+"GB) of "+limit+"GB: "+FormatDouble(volume, "0.00")+"GB.\n\n"
				+ "Thank you for your attention and please let me know if any queries.\n\n"
				+ "Sim2travel.\n";

		String mail_sender="HKNet@sim2travel.com";
		String mail_receiver=HKNetReceiver;
		if(TEST_MODE){
			mail_receiver = "k1988242001@gmail.com.tw,ranger.kao@sim2travel.com";
		}
		
		
		if(limit<volume){
			sendMail=true;
			NTTalerted = true;
			mail_subject = "Notice-"+sYearmonth+": total pooled usage in HK reaches 100%";
		}else if(limit2<volume&&!NTT75alerted){
			sendMail=true;
			NTT75alerted = true;
			mail_subject = "Notice-"+sYearmonth+": total pooled usage in HK reaches 75%";
		}
		
		if(sendMail){

			Statement st = null;
			
			try {
				
				sql="INSERT INTO HUR_SMS_LOG"
						+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
						+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+mail_receiver+"','"+new String(mail_content.getBytes("BIG5"),"ISO8859-1")+"',TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss'),'success',SYSDATE)";
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
	//TODO
	public void checkVolumePocket(){
		logger.info("checkVolumePocket...");
		subStartTime = System.currentTimeMillis();
		int count = 0;

		for(String serviceid:volumeList.keySet()){
			Map<String,Double> sMap =  volumeList.get(serviceid);
			for(String mcc : sMap.keySet()){
				double v = sMap.get(mcc);
				v = v*kByte;//轉換為由B轉換為KB 
				int alerted = parseInt((String)volumePocketMap.get(serviceid).get(mcc).get("ALERTED"));
				int type = parseInt((String)volumePocketMap.get(serviceid).get(mcc).get("TYPE"));
				double pocketLimit = parseDouble((String)volumePocketMap.get(serviceid).get(mcc).get("LIMIT"));
				
				if(type==0){ //美國流量包
					String msgID = null;
					boolean sendSMS = false;
					
					//檢查額度
					if(v>=pocketLimit && alerted < 100){//達到500M的100%
						alerted = 100;
						msgID = getSystemConfigParam("0", "VOLUME_POCKET_MSG_ID3");
						sendSMS = true;
					}else if(v>=pocketLimit*0.7 && alerted < 70){//達到500M的70%
						alerted = 70;
						msgID = getSystemConfigParam("0", "VOLUME_POCKET_MSG_ID2");
						sendSMS = true;
					}/*else if(v>=pocketLimit*0.5 && alerted < 50){//達到500M的50%
						alerted = 50;
						msgID = getSystemConfigParam("0", "VOLUME_POCKET_MSG_ID1");
						sendSMS = true;
					}*/
					
					
					if(sendSMS){
						//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
						String phone = null;
						if(msisdnMap.containsKey(serviceid))
							phone=(String) msisdnMap.get(serviceid).get("MSISDN");
						if(phone==null ||"".equals(phone)){
							sql="";
							ErrorHandle("At checkVolumePocket sendAlertSMS occur error! The serviceid:"+serviceid+",MCC:"+mcc+" can't find msisdn to send! ");
							continue;
						}
						//發送簡訊
						count += sendSMS(serviceid,msgID.split(","),null,phone,null);	
						//更新Volume已警示部分
						Map<String,String> m = new HashMap<String,String>();
						m.put("SERVICEID", serviceid);
						m.put("MCC", mcc);
						m.put("ALERTED", ""+alerted);
						updateVolumePocketMap.add(m);
					}
				}else if(type==1){//數據預付包
					if(v>=pocketLimit&& alerted < 100){
						alerted = 100;
						//進行降速
						for(Map<String,String> map : slowDownMap.get("0")){
							String pricplanID = msisdnMap.get(serviceid).get("PRICEPLANID"); 
							String pricePlanIds = (String) map.get("PRICEPLAN");
							String mccs = (String) map.get("MNO");
							
							if(pricePlanIds.indexOf(pricplanID)!=-1 &&(mccs.indexOf(mcc)!=-1||mccs.indexOf(mcc.substring(0,3))!=-1)){
								
								String gprsName = (String) slowDownMap.get("0").get(0).get("GPRS_NAME");
								String imsi = msisdnMap.get(serviceid).get("IMSI");
								String msisdn = msisdnMap.get(serviceid).get("MSISDN");
								
								
								suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
								sus.setPriceplanID(pricplanID);
								
								PreparedStatement pst = null;
								try {
									//20141118 add 傳回suspend排程的 service order nbr
									Map<String,String> orderNBR = sus.ReqStatus_16_Act(imsi, msisdn, "1", gprsName);
									serviceOrderNBR.add(orderNBR);
									
									sql=
											"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
											+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS,IS_SLOWDOWN) "
											+ "VALUES(?,?,SYSDATE,?,?,?)";
									
									pst=conn.prepareStatement(sql);
									pst.setString(1,orderNBR.get("cServiceOrderNBR") );
									pst.setString(2,imsi );
									pst.setString(3,msisdn );
									pst.setString(4,gprsName );
									pst.setString(5,"2" );
									logger.info("Execute SQL : "+sql);
									pst.executeUpdate();
									//更新Volume已警示部分
									Map<String,String> m = new HashMap<String,String>();
									m.put("SERVICEID", serviceid);
									m.put("MCC", mcc);
									m.put("ALERTED", ""+alerted);
									updateVolumePocketMap.add(m);
									
									
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
							}
						}
						
					}
				}
			}
		}
		
		logger.info("Total send pocket alert:"+count);
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
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
				
				sql = "select nvl(MAX(PID),0)+1 PID from HUR_VOLUME_POCKET ";
				logger.debug("select pid : "+sql); 
				rs=st.executeQuery(sql);
				String pid = null;
				while(rs.next()){
					pid = rs.getString("PID");
				}
				
				
				sql = "insert into HUR_VOLUME_POCKET(SERVICEID,START_DATE,END_DATE,CREATE_TIME,MCC,PID,TYPE,LIMIT)"
						+ "VALUES('"+m.get("SERVICEID")+"','"+m.get("SDATE")+"','"+m.get("EDATE")+"',sysdate,'"+m.get("MCC")+"',"+pid+","+m.get("TYPE")+","+m.get("LIMIT")+")";
				logger.debug("insertVolumePocket SQL : "+sql); 
				st.executeUpdate(sql);
			}
			//更新資料
			for(Map<String,String> m : updateVolumePocketMap){
				sql = "update HUR_VOLUME_POCKET A set A.ALERTED = '"+m.get("ALERTED")+"' where A.SERVICEID='"+m.get("SERVICEID")+"' AND A.MCC = '"+m.get("MCC")+"' ";
				logger.debug("updateVolumePocket SQL : "+sql); 
				st.executeUpdate(sql);
			}
			
			
			result = true;
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
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
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
	

	
	public void addonVolumeAlert() throws ParseException{
		logger.info("addonVolumeAlert...");
		subStartTime = System.currentTimeMillis();
		//20150623 新增華人上網包
		//暫存數據用量資料 Key:SERVICEID,Value:Volume
		Map<String,Double> tempMap = new HashMap<String,Double>();
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
						int check=checkQosAddon(serviceid, mccNet, dayTime);
						if(check==1){
							//進行累計
							Double oldVolume=tempMap.get(serviceid)==null?0D:tempMap.get(serviceid);
							Double volume=parseDouble((String) currentDayMap.get(day).get(serviceid).get(mccNet).get("VOLUME"));
							tempMap.put(serviceid, oldVolume+volume);
						}
					}
				}
			}
		}
		//1.5 GB
		Double DEFAULT_VOLUME_THRESHOLD = getSystemConfigDoubleParam("0", "VOLUME_LIMIT1");
	
		//2.0 GB
		Double DEFAULT_VOLUME_THRESHOLD2 = getSystemConfigDoubleParam("0","VOLUME_LIMIT2");
		
		
		if(DEFAULT_VOLUME_THRESHOLD == null || DEFAULT_VOLUME_THRESHOLD2 == null){
			sql="";
			ErrorHandle("At addonVolumeAlert can't find DEFAULT_VOLUME_THRESHOLD!");
			return;
		}
		
		int smsCount=0;
		for(String serviceid:tempMap.keySet()){
			Double volume=tempMap.get(serviceid);
			Double everAlertVolume = parseDouble((String) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_VOLUME"));
			
			
			//確認資料
			if(!msisdnMap.containsKey(serviceid)){
				ErrorHandle("MsisdnMap without serviceid:"+serviceid+"'s data!");
				continue;
			}
			
			//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
			String phone=(String) msisdnMap.get(serviceid).get("MSISDN");
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
				smsCount+=sendSMS(serviceid,contentid,null,phone,null);
				
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
		logger.debug("Total send 華人上網包 volume alert SMS "+smsCount+" ...");
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
	}
	
	private void checkDailySlowDown(){
		if(currentDayMap.get(sYearmonthday)==null)
			return;
		
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
			
			
			Integer limit = parseInt((String) m.get("LIMIT"));	
			String nGPRSName = (String) m.get("GPRS_NAME");			
			
			for(String serviceid : currentDayMap.get(sYearmonthday).keySet()){
				//確認資料
				if(!msisdnMap.containsKey(serviceid)){
					ErrorHandle("MsisdnMap without serviceid:"+serviceid+"'s data!");
					continue;
				}
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
						if(mccmncs.contains(cMccmnc) || mccmncs.contains(cMccmnc.substring(0,3))){
							if("1".equals(currentDayMap.get(sYearmonthday).get(serviceid).get(cMccmnc).get("IS_SLOWDOWN"))){
								total = 0d;
								break;
							}
							double volume =  parseDouble((String) currentDayMap.get(sYearmonthday).get(serviceid).get(cMccmnc).get("VOLUME"));
							total += volume;
							mccs.add(cMccmnc);
						}
					}
					if(limit<=total/1024){//limit存的是KB，current存放的是原始B
						//20160725 add
						doSlowDown(serviceid,nGPRSName,priceplanid);
						
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
	
	private void doSlowDown(String serviceid,String nGPRSName,String pricplanID){
		logger.info("doSlowDown..."+serviceid+"...");
		
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
					+ "AND B.STATUS IN (1,3) "
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
			sus.setPriceplanID(pricplanID);
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
	/**
	 * 對因每日超額被降速客戶，在一日的開始進行復網，LOG標記IS_SLOWDOWN為1，使用原本的GPRSNAME恢復
	 */
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
	
	boolean checkPocketStart = false;
	
	private void process(){
		
		long startTime = System.currentTimeMillis();//程式時間記錄開始
		
		logger.info("RFP Program Start! "+new Date());
		
		// 進行DB連線
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

			
			if(
					
					setIMSItoServiceIDMap()&&//設定IMSI至ServiecId的對應，因時間較長，提到首個執行
					
					setDayDate() && //設定日期
					
					setQosData()&&//設定SX001,SX002資料
					
					setSystemConfig()&&//系統Comfig設定
					setThreshold()&&//取出HUR_THRESHOLD
					setDataRate()&&//取出HUR_DATARATE
					setMsisdnMap()&&//取出msisdn資訊
					setIPtoMccmncList()&&//20141211 add IP 對應到 MCCMNC
					
					//以SERVICEID 從登記之VLN找出用戶目前的MCCMNC
					setSERVICEIDtoVLN()&&//IMSI 對應到 vln
					setVLNtoTADIG()&&//vln 對應到 TADIG
					setTADIGtoMCCMNC()&&//TADIG 對應到 MCCMNC
					
					setCostomerNumber()&&//國碼對應表(客服,國名)
					setAddonData()&&//華人上網包申請資料
					//setCurrencyMap()&&//設定PricePlanID對應幣別
					
					//20160624 add
					setSlowDownList()&&
	
					//20160721 add
					setVolumePocketMap()&& 
					
					(currentMap.size()!=0||setCurrentMap())&&//取出HUR_CURRENT，因為每次取造成資料有些許差異，於是決定只取第一次
					(currentDayMap.size()!=0||setCurrentMapDay())&&//取出HUR_CURRENT_DAY，因為每次取造成資料有些許差異，於是決定只取第一次
					setoldChargeMap()&&//設定old 20151027 modified update old Map every times
					true
					
					){
				
				if(resume) checkResume();
				
				if(resumeSpeed)doResumeSpeed();

				if(checkPocketStart)sendStartPocketDateSMS();

				charge();	//開始批價 

				//發送警示
				if(getSMSsetting()&&getSMSContents()){
					try {
						ckeckMonthAlert(); //月警示
						checkDailyAlert(); //日警示
						addonVolumeAlert();//華人上網包降速提醒簡訊
						checkNTTVolumeAlert();//20160701 mod ，重新啟用NTT用量告警
						checkDailySlowDown();//20160624 對特定條件客戶進行降速
						checkVolumePocket();//20160721 add 美國流量包
						
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
						}
						//suspend的後續追蹤處理
						processSuspendNBR();
					} catch (Exception e) {
						sql="";
						ErrorHandle("At sendAlertSMS got Exception!",e);
					}
				}
			}

			// 程式執行完成
			logger.info("Program execute time :" + (System.currentTimeMillis() - startTime));
			closeConnect();
			threadCheck = false;

		} else {
			sql="";
			ErrorHandle("Cannot connect to DB(connect is null)!!");
		}
	}	
}



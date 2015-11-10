package program;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class DVRSmaintest implements Job{

	
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
	
	//多排程處理
	private static boolean executing =false;
	private static boolean hasWaiting = false;
	
	
	static Map<String,Map<String,Map<String,Object>>> currentMap = new HashMap<String,Map<String,Map<String,Object>>>();
	static Map<String,Map<String,Map<String,Map<String,Object>>>> currentDayMap = new HashMap<String,Map<String,Map<String,Map<String,Object>>>>();
	Map <String,Double> cdrChargeMap = new HashMap<String,Double>();
	static Map <String,Set<String>> existMap = new HashMap<String,Set<String>>();
	static Map <String,Map <String,Set<String>>> existMapD = new HashMap<String,Map <String,Set<String>>>();
	
	Map<String,Map<String,Map<String,Object>>> dataRate = new HashMap<String,Map<String,Map<String,Object>>>();
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
	private void setDayDate(){
		logger.info("setMonthDate...");

		//目前時間
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		
		//系統時間提前一小時
		calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY)-1);
		sYearmonth=DateFormat(calendar.getTime(), MONTH_FORMATE);
		sYearmonthday=DateFormat(calendar.getTime(),DAY_FORMATE);
		//上個月時間，減掉Month會-30天，採取到1號向前，確定跨月
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)-1);
		sYearmonth2=DateFormat(calendar.getTime(), MONTH_FORMATE);
		
		calendar.clear();
	}
	
	/**
	 * 設定華人上網包MCCMNC資料 20141118 排除華人上網包
	 */
	private void setQosData(){
		//sSX001

		sSX001.add("45412");
		
		//sSX002

		sSX002.add("46001");
		sSX002.add("46007");
		sSX002.add("46002");
		sSX002.add("460000");
		sSX002.add("46000");
		sSX002.add("45412");
	}
	

	/*************************************************************************
	 *************************************************************************
	 *                                表格資料設定
	 *************************************************************************
	 *************************************************************************/

	/**
	 * NTD_MONTH_LIMIT
	 * NTD_DAY_LIMIT
	 * HKD_MONTH_LIMIT
	 * HKD_DAY_LIMIT
	 * VOLUME_LIMIT1
	 * VOLUME_LIMIT2
	 */
	Map<String,Object> systemConfig = new HashMap<String,Object>();
	private void setSystemConfig(){
		Statement st = null;
		ResultSet rs = null;
		try {
			sql="SELECT A.NAME,A.VALUE,A.DESCR id FROM HUR_DVRS_CONFIG A";
			st = conn.createStatement();
			logger.debug("Query SystemConfig SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				systemConfig.put(rs.getString("NAME"), rs.getObject("VALUE"));
			}

		} catch (SQLException e) {
			exceptionHandle("At set SystemConfig Got a SQLException", e);
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
	 * 尋找最後一次更改的fileID，以及目標處理的最終ID
	 */
	private void setLastFileID(){
		Statement st = null;
		ResultSet rs = null;
		try {
			sql="SELECT MAX(A.LAST_FILEID) id FROM HUR_CURRENT A";
			st = conn.createStatement();
			logger.debug("Query LastFileID SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				lastfileID=rs.getInt("id");
			}
			logger.info("Last process file ID :"+lastfileID);

		} catch (SQLException e) {
			exceptionHandle("At set LastFileID occur SQLException error", e);
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
	 * 取出 HUR_CURRENTE table資料
	 * 建立成
	 * Map 
	 * Key:MONTH,Value:Map(serviceid,Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_VOLUME)))
	 */
	private void setCurrentMap(){
		Statement st = null;
		ResultSet rs = null;
		try {
			//設定HUR_CURRENT計費，抓出這個月與下個月
			sql=
					"SELECT A.SERVICEID,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.LAST_DATA_TIME,A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
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

		} catch (SQLException e) {
			exceptionHandle("At set LastFileID occur SQLException error", e);
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
	 * 保留這個月舊資料，作為預測費用使用
	 */
	private void setoldChargeMap(){
		if(currentMap.containsKey(sYearmonth)){
			for(String serviceid : currentMap.get(sYearmonth).keySet()){
				oldChargeMap.put(serviceid, (Double) currentMap.get(sYearmonth).get(serviceid).get("CHARGE"));
			}
		}
		/*
		
		Statement st = null;
		ResultSet rs = null;
		try {
			//設定HUR_CURRENT計費，抓出這個月與下個月
			sql=
					"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.LAST_DATA_TIME,A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN ('"+sYearmonth+"') ";
			st = conn.createStatement();
			logger.debug("For set oldChargeMap SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				String imsi =rs.getString("IMSI");
				//保留舊資料
				oldChargeMap.put(imsi, rs.getDouble("CHARGE"));
			}

		} catch (SQLException e) {
			exceptionHandle("At set oldChargeMap occur SQLException error", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}*/
	}
	
	/**
	 * 取出 HUR_CURRENTE_DAY table資料
	 * 建立成
	 * Map 
	 * Key:day , value:Map(IMSI,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME,ALERT)))
	 * 設定HUR_CURRENT_DAY計費,目前不做刪除動作，之後考慮是否留2個月資料
	 * 20141209 修改取出近兩個月
	 */
	private void setCurrentMapDay(){
		Statement st = null;
		ResultSet rs = null;
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
		}catch (SQLException e) {
			exceptionHandle("At setCurrentMapDay occur SQLException error", e);
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
	 * 取出 HUR_DATA_RATE
	 * 建立成MAP Key:PRICEPLANID,Value:Map(MCCMNC,MAP(CURRENCY,CHARGEUNIT,RATE))
	 */
	private void setDataRate(){
		Statement st = null;
		ResultSet rs = null;
		
		sql=
				"SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY,A.PRICEPLANID,A.DAYCAP "
				+ "FROM HUR_DATA_RATE A ";
		
		try {
			st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			while(rs.next()){

				String mccmnc =rs.getString("MCCMNC");
				String priceplanID =rs.getString("PRICEPLANID");
				
				Map<String,Object> map=new HashMap<String,Object>();
				Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();
				
				map.put("RATE", rs.getDouble("RATE"));
				map.put("CHARGEUNIT", rs.getDouble("CHARGEUNIT"));
				map.put("CURRENCY", rs.getString("CURRENCY"));
				map.put("DAYCAP", rs.getDouble("DAYCAP"));
				
				if(dataRate.containsKey(priceplanID)){
					map2=dataRate.get(priceplanID);
				}

				map2.put(mccmnc, map);
				
				dataRate.put(priceplanID, map2);
			}
		} catch (SQLException e) {
			exceptionHandle("At setDataRate occur SQLException error", e);
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
	 * 取出以Priceplan對應到的幣別
	 */
	private void setCurrencyMap(){
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

			while(rs.next()){
				String id = rs.getString("PRICEPLANID");
				String currency = rs.getString("CURRENCY");
				
				if(id!=null && !"".equals(id)&& 
						currency!=null && !"".equals(currency))
					pricePlanIdtoCurrency.put(id, currency);
			}

		} catch (SQLException e) {
			exceptionHandle("At setCurrencyMap occur SQLException error", e);
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
	 */
	private void setThreshold(){
		Statement st = null;
		ResultSet rs = null;
		sql=
				"SELECT A.SERVICEID,A.THRESHOLD "
				+"FROM HUR_GPRS_THRESHOLD A ";
		
		try {
			st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			while(rs.next()){
				thresholdMap.put(rs.getString("SERVICEID"), rs.getDouble("THRESHOLD"));
			}
			
		} catch (SQLException e) {
			exceptionHandle("At setThreshold occur SQLException error", e);
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
	 * 設定 IMSI 至 VLN 的對應表
	 * Map Key:IMSI,VALUE:VLN
	 */
	private void setSERVICEIDtoVLN(){
		Statement st = null;
		ResultSet rs = null;
		sql=
				"SELECT A.VLR_NUMBER,A.SERVICEID "
				+ "FROM UTCN.BASICPROFILE A "
				+ "WHERE A.VLR_NUMBER is not null ";
		
		try {
			st = conn2.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				SERVICEIDtoVLN.put(rs.getString("SERVICEID"), rs.getString("VLR_NUMBER"));
			}
		} catch (SQLException e) {
			exceptionHandle("At setIMSItoVLN occur SQLException error", e);
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
	 * 
	 * 建立 VLN至TADIG對應表
	 * 
	 * 從SERVICEIDtoVLN取得VALUE必須縮位匹配
	 * 
	 * MAP KEY：VLN,VALUE:TADIG
	 */
	private void setVLNtoTADIG(){
		Statement st = null;
		ResultSet rs = null;
		sql=
				"SELECT B.REALMNAME TADIG, A.CHARGEAREACODE VLR "
				+ "FROM CHARGEAREACONFIG A, REALM B "
				+ "WHERE A.AREAREFERENCE=B.AREAREFERENCE";
		
		try {
			st = conn2.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				VLNtoTADIG.put(rs.getString("VLR"), rs.getString("TADIG"));
			}

		} catch (SQLException e) {
			exceptionHandle("At setVLNtoTADIG occur SQLException error", e);
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
	 * 
	 * 建立 TADIG至MCCMNC對應表
	 * 
	 * MAP KEY：TADIG,VALUE:MCCMNC
	 */
	private void setTADIGtoMCCMNC(){
		Statement st = null;
		ResultSet rs = null;
		sql=
				"SELECT A.TADIG,A.MCCMNC "
				+ "FROM HUR_MCCMNC A ";
		
		try {
			st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				TADIGtoMCCMNC.put(rs.getString("TADIG"), rs.getString("MCCMNC"));
			}
		} catch (SQLException e) {
			exceptionHandle("At setTADIGtoMCCMNC occur SQLException error", e);
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
	 * 
	 * 建立 國碼對客服電話，國家 對應表
	 * 
	 * MAP KEY：CODE,VALUE:(PHONE,NAME)
	 */
	private void setCostomerNumber(){
		Statement st = null;
		ResultSet rs = null;
		sql=
				"SELECT A.CODE,A.PHONE,A.NAME "
				+ "FROM HUR_CUSTOMER_SERVICE_PHONE A";
		
		try {
			st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> map = new HashMap<String,String>();
				map.put("PHONE", rs.getString("PHONE"));
				map.put("NAME", rs.getString("NAME"));
				codeMap.put(rs.getString("CODE"), map);
			}
		} catch (SQLException e) {
			exceptionHandle("At setTADIGtoMCCMNC occur SQLException error", e);
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
	 * 取得資料筆數
	 * @return
	 */
	private int dataCount(){
		Statement st = null;
		ResultSet rs = null;
		int count=0;
		try {
			sql=
					"SELECT COUNT(1) count  FROM HUR_DATA_USAGE A WHERE A.FILEID>= "+lastfileID+" AND A.CHARGE is null ";
			st = conn.createStatement();
			logger.debug("Query dataCount SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				count=rs.getInt("count");
			}

		} catch (SQLException e) {
			exceptionHandle("At dataCount occur SQLException error", e);
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
		return count;
	}
	
	/**
	 * 取得預設計費比率（總費率平均），對MCCNOC有卻無法對應資料計費
	 * @return
	 */
	private double defaultRate(){
		double defaultRate=0.011;
		/*double exchangeRate = Double.valueOf((String) systemConfig.get("EXCHANGE_RATE"));
		Statement st = null;
		ResultSet rs = null;
		try {
			sql=
					"SELECT AVG(CASE WHEN A.CURRENCY = 'HKD' THEN A.RATE/A.CHARGEUNIT*"+exchangeRate+" ELSE  A.RATE/A.CHARGEUNIT END)  AVG "
					+ "FROM HUR_DATA_RATE A ";
			st = conn.createStatement();
			logger.debug("Query dataCount SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				defaultRate=rs.getDouble("AVG");
			}

		} catch (SQLException e) {
			exceptionHandle("At defaultRate occur SQLException error!", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}*/
		logger.info("defaultRate : " +defaultRate+" TWD ");
		return defaultRate;
	}
	
	
	/**
	 * 建立華人上網包對應資料
	 * 
	 * List Map KEY:MSISDN,VALUE(IMSI,MSISDN,SERVICECODE,STARTDATE,ENDDATE)>
	 */
	private void setAddonData(){
		Statement st = null;
		ResultSet rs = null;
		sql=
				"SELECT A.S2TIMSI IMSI,A.S2TMSISDN MSISDN,A.SERVICECODE,A.STARTDATE,A.ENDDATE "
				+ "FROM ADDONSERVICE_N A ";
		try {
			st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
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
		} catch (SQLException e) {
			exceptionHandle("At setTADIGtoMCCMNC occur SQLException error", e);
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
	 * IP number to Mccmnc  20141211 add
	 * Map Value:START_NUM,END_NUM,MCCMNC
	 */
	private void setIPtoMccmncList(){
		Statement st = null;
		ResultSet rs = null;
		try {
			sql=
					"SELECT A.START_NUM,A.END_NUM,A.MCCMNC "
					+ "FROM HUR_IP_RANGE A "
					+ "ORDER BY A.START_NUM ";
			st = conn.createStatement();
			logger.debug("Query AddonData SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("START_NUM", rs.getLong("START_NUM"));
				map.put("END_NUM", rs.getLong("END_NUM"));
				map.put("MCCMNC", rs.getString("MCCMNC"));
				IPtoMccmncList.add(map);
			}

		} catch (SQLException e) {
			logger.error("At setIPtoMccmncList occur SQLException error", e);
			//send mail
			sendMail("At setIPtoMccmncList occur SQLException error!");
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
	}
	
	/**
	 * 取出msisdn
	 * 建立msisdnMap
	 * Key:imsi,Value:Map(MSISDN,PRICEPLANID,SUBSIDIARYID,NCODE,SERVICEID)
	 * 增加serviceid to map，做以serviceid反查
	 */
	private void setMsisdnMap(){
		logger.info("setMsisdnMap...");
		sql=
				/*"SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID "
				+ "FROM SERVICE A,IMSI B "
				+ "WHERE A.SERVICEID=B.SERVICEID AND A.SERVICECODE IS NOT NULL ";*/
				"SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,"
				+ "(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE "
				+ "FROM SERVICE A,IMSI B,PARAMETERVALUE C "
				+ "WHERE A.SERVICEID=B.SERVICEID AND A.SERVICECODE IS NOT NULL "
				+ "AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748";
		
		try {
			/*String a="(";
			for(String imsi: currentMap.keySet()){
				a+=imsi+",";
			}			
			a+=")";
			a=a.replace(",)", ")");
			
			sql=sql.replace("?", a);*/
			
			//logger.info("a:"+a);
			Statement st = conn2.createStatement();
			logger.info("Execute SQL :"+sql);
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("MSISDN", rs.getString("SERVICECODE"));
				map.put("PRICEPLANID", rs.getString("PRICEPLANID"));
				map.put("SUBSIDIARYID", rs.getString("SUBSIDIARYID"));
				map.put("NCODE", rs.getString("NCODE"));
				map.put("SERVICEID", rs.getString("SERVICEID"));
				map.put("IMSI", rs.getString("IMSI"));
				msisdnMap.put(rs.getString("IMSI"), map);
				msisdnMap.put(rs.getString("SERVICEID"), map);
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			logger.error("At setMsisdnMap occur SQLException error!", e);
			//send mail
			sendMail("At setMsisdnMap occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	Map<String,String> IMSItoServiceIdMap = new HashMap<String,String>();
	Map<String,String> ServiceIdtoIMSIMap = new HashMap<String,String>();
	/**
	 * 設定IMSI對應到ServiceID Map 20150115 add
	 * 
	 * 先從MSISDN MAP(IMSI table 資料找尋 SERVICEID)
	 * 找不到再從此Table(換卡記錄)找尋
	 */
	private void setIMSItoServiceIDMap(){
		logger.info("setIMSItoServiceIDMap...");
		sql=  
				/*"SELECT A.IMSI,A.SERVICEID "
				+ "FROM IMSI A "
				+ "WHERE A.SERVICEID IS NOT NULL "
				+ "UNION "
				+ */
				"SELECT A.NEWVALUE IMSI, C.SERVICEID "
				+ "FROM SERVICEINFOCHANGEORDER A, SERVICEORDER B, SERVICE C ,("
				+ "        SELECT max(A.COMPLETEDATE) COMPLETEDATE, A.NEWVALUE,count(1) "
				+ "        FROM SERVICEINFOCHANGEORDER A "
				+ "        WHERE A.FIELDID=3713  AND A.COMPLETEDATE IS NOT NULL "
				+ "        AND A.OLDVALUE <> A.NEWVALUE "
				+ "        GROUP BY A.NEWVALUE ) D "
				+ "WHERE A.FIELDID=3713 AND A.ORDERID=B.ORDERID "
				+ "AND B.SERVICEID=C.SERVICEID "
				+ "AND A.OLDVALUE <> A.NEWVALUE "
				+ "AND D.COMPLETEDATE=A.COMPLETEDATE "
				+ "AND A.NEWVALUE=D.NEWVALUE "
				+ "AND A.NEWVALUE IN ( SELECT A.IMSI FROM IMSI A WHERE A.SERVICEID IS NULL)";
		
		try {
			
			Statement st = conn.createStatement();
			logger.info("Execute SQL :"+sql);
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				IMSItoServiceIdMap.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
				ServiceIdtoIMSIMap.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			logger.error("At setIMSItoServiceIDMap occur SQLException error!", e);
			//send mail
			sendMail("At setIMSItoServiceIDMap occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	
	
	
	
	
	/**
	 * 設定簡訊設定
	 * Map Key priceplanID，Value: ID,BRACKET,MEGID,SUSPEND,PRICEPLANID< List>
	 */
	Map<String,Map<String,List<Object>>> smsSettingMap = new HashMap<String,Map<String,List<Object>>>();
	
	private void setSMSSettingMap(){
		
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
				Map<String,List<Object>> map = new HashMap<String,List<Object>>();
				
				List<Object> l1=new ArrayList<Object>(); //ID
				List<Object> l2=new ArrayList<Object>();
				List<Object> l3=new ArrayList<Object>();
				List<Object> l4=new ArrayList<Object>();
				if(smsSettingMap.containsKey(pId)){
					map=smsSettingMap.get(pId);
					if(map.containsKey("ID")) l1=map.get("ID");
					if(map.containsKey("BRACKET")) l2=map.get("BRACKET");
					if(map.containsKey("MEGID")) l3=map.get("MEGID");
					if(map.containsKey("SUSPEND")) l4=map.get("SUSPEND");
				}
				
				l1.add(rs.getInt("ID"));
				l2.add(rs.getDouble("BRACKET"));
				l3.add(rs.getString("MEGID"));
				l4.add(rs.getString("SUSPEND"));
				
				map.put("ID", l1);
				map.put("BRACKET", l2);
				map.put("MEGID", l3);
				map.put("SUSPEND", l4);
				
				smsSettingMap.put(pId, map);
			}

		} catch (SQLException e) {
			exceptionHandle("At setTADIGtoMCCMNC occur SQLException error", e);
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
	 * 簡訊內容設定
	 */
	Map<String,Map<String,String>> smsContent=new HashMap<String,Map<String,String>>();
	private void setSMSContent(){
		
		Statement st = null;
		ResultSet rs = null;
		try {
			sql=
					"SELECT A.ID,A.CONTENT,A.CHARSET "
					+ "FROM HUR_SMS_CONTENT A ";
			st = conn.createStatement();
			logger.debug("Query SMSContent SQL : "+sql);
			rs = st.executeQuery(sql);

			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("CONTENT", rs.getString("CONTENT"));
				map.put("CHARSET", rs.getString("CHARSET"));
				smsContent.put(rs.getString("ID"), map);
			}

		} catch (SQLException e) {
			exceptionHandle("At setTADIGtoMCCMNC occur SQLException error", e);
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
					+ "TEST_MODE : "+TEST_MODE+"\n"
					+ "dataThreshold : "+dataThreshold+"\n"
					//+ "lastfileID : "+lastfileID+"\n"
					+ "exchangeRate : "+exchangeRate+"\n"
					+ "kByte : "+kByte+"\n");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("File Path : "+path);
			//send mail
			sendMail("At loadProperties occur file not found error \n <br>"
					+ "file path="+path);
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (IOException e) {
			e.printStackTrace();
			//send mail
			sendMail("At loadProperties occur IOException error !\n <br>"
					+ "file path="+path);
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
		
	}
	
	/**
	 * 連線至DB1
	 */
	private void connectDB(){
		//conn=connDB(logger, DriverClass, URL, UserName, PassWord);
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
			logger.error("At connDB occur ClassNotFoundException error", e);
			//send mail
			sendMail("At connDB occur ClassNotFoundException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (SQLException e) {
			sql="";
			logger.error("At connDB occur SQLException error", e);
			//send mail
			sendMail("At connDB occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * 連線至DB2
	 */
	private void connectDB2(){
		// 進行DB連線
		//conn2=connDB(logger, DriverClass, URL, UserName, PassWord);
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
			logger.error("At connDB2 occur ClassNotFoundException error", e);
			//send mail
			sendMail("At connDB2 occur ClassNotFoundException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}	
		} catch (SQLException e) {
			sql="";
			logger.error("At connDB2 occur SQLException error", e);
			//send mail
			sendMail("At connDB2 occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
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
				sendMail("At closeConnect occur SQLException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			}

		}
		
		if (conn2 != null) {

			try {
				logger.info("closeConnect2...");
				conn2.close();
			} catch (SQLException e) {
				logger.error("close Connect2 Error", e);
				//send mail
				sendMail("At closeConnect2 occur SQLException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
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
			sendMail("At cancelAutoCommit occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
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
							callTime.after((Date) m.get("STARTDATE"))&&(m.get("ENDDATE")==null ||callTime.before((Date) m.get("ENDDATE")))){
						cd=1;
						break;
					}
				}
				
			}
			
			//香港加中國大陸
			if(cd==0 && sSX002.contains(mccmnc)){
				for(Map<String,Object> m : addonDataList){
					if(imsi.equals(m.get("IMSI"))&&"SX002".equals(m.get("SERVICECODE"))&&
							callTime.after((Date) m.get("STARTDATE"))&&(m.get("ENDDATE")==null ||callTime.before((Date) m.get("ENDDATE")))){
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
	
	private void logSMS(String phone,String cont,String res){
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			sql="INSERT INTO HUR_SMS_LOG"
					+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
					+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+cont+"'"
					+ ",TO_DATE('"+spf.format(new Date())+"','yyyy/MM/dd HH24:mi:ss')"
					+ ",'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
			logger.debug("Insert SMSLog SQL : "+sql);
			rs = st.executeQuery(sql);
		} catch (SQLException e) {
			exceptionHandle("At logSMS occur SQLException error", e);
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
	 * 開始批價
	 */
	private void charge(){
		logger.info("charge...");
		
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
						+ "FROM HUR_DATA_USAGE A WHERE A.FILEID>= "+lastfileID+" AND ROWNUM <= "+(i*dataThreshold)+" AND A.CHARGE is null "
						+ "ORDER BY A.USAGEID,A.FILEID) "
						+ "MINUS "
						+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME,SGSNADDRESS "
						+ "FROM HUR_DATA_USAGE A WHERE A.FILEID>= "+lastfileID+" AND ROWNUM <= "+((i-1)*dataThreshold)+" AND A.CHARGE is null "
						+ "ORDER BY A.USAGEID,A.FILEID) ";
				
				logger.debug("Execute SQL : "+sql);
				
				logger.debug("Round "+i+" Procsess ...");
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql);

				while(rs.next()){			
					String logMsg="";
					
					String imsi= rs.getString("IMSI");
					String mccmnc=rs.getString("MCCMNC");
					String usageId=rs.getString("USAGEID");
					Double volume=rs.getDouble("DATAVOLUME");
					Date callTime=rs.getDate("CALLTIME");
					Integer fileID=rs.getInt("FILEID");	
					String cDay = DateFormat(callTime, DAY_FORMATE);
					Double charge=0D;
					Double dayCap=null;
										
					
					//20141211 add
					String ipaddr = rs.getString("SGSNADDRESS");
					
					//20141210 add
					String currency=null;

					//20150115 add
					String serviceid = msisdnMap.get(imsi).get("SERVICEID");
					
					if(serviceid==null || "".equals(serviceid))
						serviceid = IMSItoServiceIdMap.get("IMSI");
					
					if(serviceid==null || "".equals(serviceid)){
						sendMail("For CDR usageId="+usageId+" which can't find  ServceID." );
						continue;
					}
					
					
					String pricplanID=null;
					if(msisdnMap.containsKey(imsi))
						pricplanID=msisdnMap.get(imsi).get("PRICEPLANID");
					//20141211 add
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc=searchMccmncByIP(ipaddr);
						if(mccmnc!=null && !"".equals(mccmnc))
							logger.debug("For CDR usageId="+usageId+" which is without mccmnc. Found mccmnc="+mccmnc+" by IP range.");
					}
	
					if(dataRate.containsKey(pricplanID)){
						//20141210 add
						currency=pricePlanIdtoCurrency.get(pricplanID);
						
						if(mccmnc==null || "".equals(mccmnc)){
							mccmnc=searchMccmncBySERVICEID(serviceid);
						}
					}else{
						sql="";errorMsg="";
						logger.debug("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
						sendMail("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
						
					}

					//還是找不到，給予預設，必須有
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc= DEFAULT_MCCMNC;
					}
					
					int cd=checkQosAddon(imsi, mccmnc, callTime);
					if(cd==0){
						//判斷是否可以找到對應的費率表，並計算此筆CDR的價格(charge)
						if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
								dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
							
							double ec=1;
							//20141210 取消港幣全部換成台幣批價
							/*
							if("HKD".equalsIgnoreCase(currency))
								ec=exchangeRate;*/
							
							Double rate=(Double)dataRate.get(pricplanID).get(mccmnc).get("RATE");
							Double unit=(Double)dataRate.get(pricplanID).get(mccmnc).get("CHARGEUNIT");
							charge=Math.ceil(volume*kByte)*rate*ec/unit;
							dayCap=(Double)dataRate.get(pricplanID).get(mccmnc).get("DAYCAP");
							
						}else{
							//沒有PRICEPLANID(月租方案)，MCCMNC，無法判斷區域業者，作法：統計流量，
							//沒有對應的PRICEPLANID(月租方案)，MCCMNC，無法判斷區域業者
							//以預設費率計費
							
							//20141210 假設幣值為港幣，將平均台幣換算成平均港幣
							double ec=1;					
							if("HKD".equalsIgnoreCase(currency))
								ec=exchangeRate;
							charge=Math.ceil(volume*kByte)*defaultRate/ec;
							
							sql="";errorMsg="";
							sendMail("IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");
							charge=Math.ceil(volume*kByte)*defaultRate;
						}
					}
		
					//XXX
					/*if("454120260226967".equals(imsi))
						System.out.println("check point");
					*/
					
					//格式化至小數點後四位
					charge=FormatDouble(charge, "0.0000");

					//將此筆CDR結果記錄，稍後回寫到USAGE TABLE
					cdrChargeMap.put(usageId, charge);
					logMsg+="UsageId "+usageId+" ,IMSI "+imsi+" ,MCCMNC "+mccmnc+" charge result is "+cdrChargeMap.get(usageId)+". ";

					//察看是否有以存在的資料，有的話取出做累加
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
							if( map2.containsKey(mccmnc)){
								map3=map2.get(mccmnc);

								oldCharge=(Double)map3.get("CHARGE");
								
								logMsg+="The old Daily charge is "+oldCharge+". ";
								
								//summary charge
								charge=oldCharge+charge;
								charge=FormatDouble(charge, "0.0000");
								
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
					map2.put(mccmnc, map3);
					map.put(serviceid, map2);
					currentDayMap.put(cDay, map);

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
					charge=FormatDouble(charge, "0.0000");
					map4.put("CHARGE", charge);
					logMsg+="The final month charge is "+map4.get("CHARGE")+". ";
					
					map4.put("VOLUME", volume);
					map4.put("EVER_SUSPEND", suspend);
					map4.put("LAST_ALERN_THRESHOLD", lastAlertThreshold);
					map4.put("LAST_ALERN_VOLUME", volumeAlert);
					map5.put(serviceid, map4);
					currentMap.put(cMonth, map5);

					logger.debug(logMsg);
				}
				if(st!=null)st.close();
				if(rs!=null)rs.close();
			}
		} catch (SQLException e) {
			logger.error("At charge occur SQLException error", e);
			//send mail
			sendMail("At charge occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}	
	}
	
	/***
	 * 回寫CDR的CHARGE
	 */
	private void updateCdr(){
		int count=0;

		try {
			Statement st = conn.createStatement();
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
			st.close();
		} catch (SQLException e) {
			logger.error("At updateCdr occur SQLException error", e);
			//send mail
			sendMail("At updateCdr occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}

	/**
	 * 計算完畢後寫回資料庫-更新
	 * @throws SQLException 
	 */
	private void updateCurrentMap() throws SQLException{
		logger.info("updateCurrentMap...");

		@SuppressWarnings("unused")
		int[] result;
		int count=0;
		
		sql=
				"UPDATE HUR_CURRENT A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.SMS_TIMES=?,A.LAST_DATA_TIME=TO_DATE(?,'yyyy/MM/dd hh24:mi:ss'),A.VOLUME=?,A.EVER_SUSPEND=?,A.LAST_ALERN_THRESHOLD=?,A.LAST_ALERN_VOLUME=?,A.UPDATE_DATE=SYSDATE "
				+ "WHERE A.MONTH=? AND A.SERVICEID=? ";
		
		logger.info("Execute SQL :"+sql);

		PreparedStatement pst = conn.prepareStatement(sql);

		//20141201 add change another method to distinguish insert and update
		for(String mon : currentMap.keySet()){
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
					result=pst.executeBatch();
					count=0;
				}	
			}
		}
		if(count!=0){
			logger.info("Execute updateCurrentMap Batch");
			result=pst.executeBatch();
		}
		pst.close();
	}
	private void updateCurrentMapDay() throws SQLException{
		logger.info("updateCurrentMapDay...");
		@SuppressWarnings("unused")
		int[] result;
		int count=0;
		
		sql=
				"UPDATE HUR_CURRENT_DAY A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.LAST_DATA_TIME=TO_DATE(?,'yyyy/MM/dd hh24:mi:ss'),A.VOLUME=?,A.ALERT=?,A.UPDATE_DATE=SYSDATE "
				+ "WHERE A.DAY=? AND A.SERVICEID=? AND A.MCCMNC=? ";
		
		logger.info("Execute SQL :"+sql);
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		//20141201 add change another method to distinguish insert and update
		for(String day : currentDayMap.keySet()){
			for(String serviceid : currentDayMap.get(day).keySet()){
				for(String mccmnc : currentDayMap.get(day).get(serviceid).keySet()){
					pst.setDouble(1,(Double) currentDayMap.get(day).get(serviceid).get(mccmnc).get("CHARGE"));
					pst.setInt(2, (Integer) currentDayMap.get(day).get(serviceid).get(mccmnc).get("LAST_FILEID"));
					pst.setString(3, spf.format((Date) currentDayMap.get(day).get(serviceid).get(mccmnc).get("LAST_DATA_TIME")));
					pst.setDouble(4,(Double) currentDayMap.get(day).get(serviceid).get(mccmnc).get("VOLUME"));
					pst.setString(5,(String) currentDayMap.get(day).get(serviceid).get(mccmnc).get("ALERT"));
					pst.setString(6, day);
					pst.setString(7, serviceid);
					pst.setString(8, mccmnc);
					pst.addBatch();
					count++;
					if(count==dataThreshold){
						logger.info("Execute updateCurrentMapU Batch");
						result=pst.executeBatch();
						count=0;
					}
				}
			}
		}
		if(count!=0){
			logger.info("Execute updateCurrentMapU Batch");
			result=pst.executeBatch();
		}
		pst.close();

	}
	
	/**
	 * 計算完畢後寫回資料庫-新增
	 * @throws SQLException 
	 */
	
	private void insertCurrentMap(){
		logger.info("insertCurrentMap...");
		@SuppressWarnings("unused")
		int[] result;
		
		sql=
				"INSERT INTO HUR_CURRENT "
				+ "(SERVICEID,MONTH,CREATE_DATE) "
				+ "VALUES(?,?,SYSDATE)";
		
		logger.info("Execute SQL :"+sql);
		
		/*for(String mon : insertMap.keySet()){
			for(String imsi : insertMap.get(mon)){*/
		//20141201 add change another method to distinguish insert and update
		for(String mon : currentMap.keySet()){
			for(String serviceid : currentMap.get(mon).keySet()){
				if(!existMap.containsKey(mon) || !existMap.get(mon).contains(serviceid)){
					try {
						PreparedStatement pst = conn.prepareStatement(sql);
						pst.setString(1, serviceid);		
						pst.setString(2, mon);
						pst.executeUpdate();
						pst.close();
					} catch (SQLException e) {
						logger.error("At insertCurrent "+mon+":"+serviceid+" occur SQLException error", e);
						//send mail
						sendMail("At insertCurrent "+mon+":"+serviceid+" occur SQLException error!");
						errorMsg="";
						for(StackTraceElement s :e.getStackTrace()){
							errorMsg+=s.toString()+"<br>\n";
						}
					}finally{
						//20141229 alter insert data before update
						Set<String> set=new HashSet<String>();
						if(existMap.containsKey(mon))
							set=existMap.get(mon);
						
						set.add(serviceid);
						existMap.put(mon, set);
					}
				}
			}
		}

		
	}
	private void insertCurrentMapDay(){
		logger.info("insertCurrentMapDay...");
		@SuppressWarnings("unused")
		int[] result;
		sql=
				"INSERT INTO HUR_CURRENT_DAY "
				+ "(SERVICEID,CREATE_DATE,MCCMNC,DAY) "
				+ "VALUES(?,SYSDATE,?,?)";
		
		logger.info("Execute SQL :"+sql);

		//20141201 add change another method to distinguish insert and update
		for(String day : currentDayMap.keySet()){
			for(String serviceid : currentDayMap.get(day).keySet()){
				for(String mccmnc : currentDayMap.get(day).get(serviceid).keySet()){
					if(!existMapD.containsKey(day)||!existMapD.get(day).containsKey(serviceid)||!existMapD.get(day).get(serviceid).contains(mccmnc)){
						try {
							PreparedStatement pst = conn.prepareStatement(sql);
							pst.setString(1, serviceid);
							pst.setString(2, mccmnc);
							pst.setString(3, day);
							pst.executeUpdate();
							pst.close();
						} catch (SQLException e) {
							logger.error("At insertCurrentDay "+day+":"+":"+serviceid+":"+mccmnc+" occur SQLException error", e);
							//send mail
							sendMail("At insertCurrentDay "+day+":"+":"+serviceid+":"+mccmnc+" occur SQLException error!");
							errorMsg="";
							for(StackTraceElement s :e.getStackTrace()){
								errorMsg+=s.toString()+"<br>\n";
							}
						}finally{
							//20141229 alter insert data before update
							Set<String> set=new HashSet<String>();
							Map<String,Set<String>> map = new HashMap<String,Set<String>>();
							if(existMapD.containsKey(day)){
								map=existMapD.get(day);
								if(map.containsKey(serviceid)){
									set=map.get(serviceid);
								}							
							}
							set.add(mccmnc);
							map.put(serviceid, set);
							existMapD.put(day,map);
						}
						
					}
				}
			}
		}
	}

	
	/**
	 * 依照額度需求發送警示簡訊
	 * 第一次，額度一，訊息一
	 * 
	 */
	private void sendAlertSMS(){
		//檢查是否有讀取到簡訊內容
		if(smsContent.size()==0){
			sql="";
			exceptionHandle("Can't found SMS Content sentting!", null);
			return;
		}
		
		//開始檢查是否發送警示簡訊
		//月金額警示*************************************
		//沒有當月資料，不檢查
		if(currentMap.containsKey(sYearmonth)){
			int smsCount=0;
			try {
				//檢查這個月的資料作警示通知
				for(String serviceid: currentMap.get(sYearmonth).keySet()){
					//20141210 add 
					//沒有priceplanID 跳過
					String pricePlanID = msisdnMap.get(serviceid).get("PRICEPLANID");
					String phone=null;
					if(pricePlanID==null ||"".equals(pricePlanID)){
						exceptionHandle("At sendAlertSMS occur error! The ServiceID:"+serviceid+" can't find pricePlanID!");
						continue;
					}
					
					//沒有讀取到設定 跳過
					if(smsSettingMap.get(pricePlanID)==null){
						exceptionHandle("At sendAlertSMS occur error! The PricePlanID:"+pricePlanID+" can't find setting!");
						continue;
					}
					List<Object> ids = smsSettingMap.get(pricePlanID).get("ID");
					List<Object> brackets = smsSettingMap.get(pricePlanID).get("BRACKET");
					List<Object> msgids = smsSettingMap.get(pricePlanID).get("MEGID");
					List<Object> suspends = smsSettingMap.get(pricePlanID).get("SUSPEND");
					
					//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
					if(msisdnMap.containsKey(serviceid)){
						phone=(String) msisdnMap.get(serviceid).get("MSISDN");
						if(phone==null ||"".equals(phone)){
							exceptionHandle("At sendAlertSMS occur error! The ServiceID:"+serviceid+" can't find msisdn to send !");
							continue;
						}
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
					
					//抓取不同幣別月上限
					if("NTD".equals(pricePlanIdtoCurrency.get(pricePlanID)))
						DEFAULT_THRESHOLD = Double.valueOf((String) systemConfig.get("NTD_MONTH_LIMIT"));
					if("HKD".equals(pricePlanIdtoCurrency.get(pricePlanID)))
						DEFAULT_THRESHOLD = Double.valueOf((String) systemConfig.get("HKD_MONTH_LIMIT"));
					
					//20141118 修改 約定客戶訂為每5000提醒一次不斷網
					Double threshold=thresholdMap.get(serviceid);

					//目前不設計自訂上限，取有表示客戶為VIP，取無則是非VIP
					if(threshold==null){
						threshold=DEFAULT_THRESHOLD;
					}else{
						isCustomized=true;
					}
					
					//取不到任何上限值 跳過
					if(threshold==null){
						exceptionHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricePlanID+" cannot get Month Limit! ");
						continue;
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
						for(;i<ids.size();i++){
							if(((charge>=(Double)brackets.get(i)*threshold))&&lastAlernThreshold<(Double)brackets.get(i)*threshold){
								sendSMS=true;
								alertBracket=(Double)brackets.get(i)*threshold;
								contentid=((String)msgids.get(i)).split(",");
								if("1".equals(suspends.get(i)))
									needSuspend=true;
								break;
							}
						}	
						
						//檢查預測用量，如果之前判斷不用發簡訊，或不是發最上限簡訊
						if(!sendSMS||(sendSMS && i!=0)){
							if(charge+differenceCharge>=(Double)brackets.get(0)*threshold&&lastAlernThreshold<(Double)brackets.get(0)*threshold){
								logger.info("For "+serviceid+" add charge "+differenceCharge+" in this hour ,System forecast the next hour will over charge limit");
								sendSMS=true;
								alertBracket=(Double)brackets.get(0)*threshold;
								contentid=((String)msgids.get(0)).split(",");
								if("1".equals(suspends.get(0))){
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
								String cont =processMag(smsContent.get(s).get("CONTENT"),alertBracket,cPhone);
								res=setSMSPostParam(cont,phone);
								logger.info("For "+serviceid+" send "+smsTimes+"th message:"+cont+" result:"+res);
								currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_THRESHOLD", lastAlernThreshold);				
								currentMap.get(sYearmonth).get(serviceid).put("SMS_TIMES", smsTimes);
								smsCount++;
								
								//Log SMS
								logSMS(phone,cont,res);
								
								
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
									suspend(serviceid,phone);
									currentMap.get(sYearmonth).get(serviceid).put("EVER_SUSPEND", "1");
								}
							}
						}
					}
				}
			} catch (IOException e) {
				exceptionHandle("At sendDayAlertSMS occur SQLException error!", e);
			}
			logger.debug("Total send month alert"+smsCount+" ...");
		}
		
		
		//實做日警示部分，有今日資料才警示*************************************
		if(currentDayMap.containsKey(sYearmonthday)){
			int smsCount=0;
			try {
				for(String serviceid:currentDayMap.get(sYearmonthday).keySet()){
					
					//20141216 add 斷網過後，不發送每日簡訊，避免預估斷網後，每日帶出實際累計引發爭議
					if(currentMap.containsKey(sYearmonth) && currentMap.get(sYearmonth).containsKey(serviceid)){
						String everSuspend =(String) currentMap.get(sYearmonth).get(serviceid).get("EVER_SUSPEND");
						if("1".equals(everSuspend)){
							continue;
						}
					}
					
					String pricePlanID = msisdnMap.get(serviceid).get("PRICEPLANID");
					if(pricePlanID==null ||"".equals(pricePlanID)){
						exceptionHandle("At sendAlertSMS occur error! The ServiceID:"+serviceid+" can't find pricePlanID!");
						continue;
					}
					String phone = null;
					//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
					if(msisdnMap.containsKey(serviceid))
						phone=(String) msisdnMap.get(serviceid).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error! The serviceid:"+serviceid+" can't find msisdn to send! ");
						logger.debug("The serviceid:"+serviceid+" can't find msisdn to send! ");
						continue;
					}

					Double daycharge=0D;
					Double DEFAULT_DAY_THRESHOLD = null;
					String alerted ="0";

					//抓取不同幣別日上限
					if("NTD".equals(pricePlanIdtoCurrency.get(pricePlanID)))
						DEFAULT_DAY_THRESHOLD = Double.valueOf((String) systemConfig.get("NTD_DAY_LIMIT"));
					if("HKD".equals(pricePlanIdtoCurrency.get(pricePlanID)))
						DEFAULT_DAY_THRESHOLD = Double.valueOf((String) systemConfig.get("HKD_DAY_LIMIT"));
					
					//取不到每日上限 跳過
					if(DEFAULT_DAY_THRESHOLD== null){
						exceptionHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricePlanID+" cannot get Daily Limit! ");
						continue;
					}
					
					//累計
					for(String mccmnc : currentDayMap.get(sYearmonthday).get(serviceid).keySet()){
						daycharge=daycharge+(Double)currentDayMap.get(sYearmonthday).get(serviceid).get(mccmnc).get("CHARGE");
						String a=(String) currentDayMap.get(sYearmonthday).get(serviceid).get(mccmnc).get("ALERT");
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
						
						//處理字串，日警示內容ID
						//20141209 修改為帶出當月累積金額
						String cont =null;
						if("NTD".equals(pricePlanIdtoCurrency.get(pricePlanID)) && systemConfig.get("NTD_DAY_LIMIT_MSG_ID")!=null)
							cont = processMag(smsContent.get(systemConfig.get("NTD_DAY_LIMIT_MSG_ID")).get("CONTENT"),(Double)currentMap.get(sYearmonth).get(serviceid).get("CHARGE"),cPhone);
						if("HKD".equals(pricePlanIdtoCurrency.get(pricePlanID)) && systemConfig.get("HKD_DAY_LIMIT_MSG_ID")!=null)
							cont = processMag(smsContent.get(systemConfig.get("HKD_DAY_LIMIT_MSG_ID")).get("CONTENT"),(Double)currentMap.get(sYearmonth).get(serviceid).get("CHARGE"),cPhone);
						
						//取不到簡訊內容 跳過
						if(cont== null){
							exceptionHandle("For ServiceID:"+serviceid+" PricePlanId:"+pricePlanID+" cannot get Daily SMS content! ");
							continue;
						}
						//發送簡訊
						logger.info("For "+serviceid+" send daily allert message:99");
						String res = setSMSPostParam(cont,phone);
						logger.debug("send message result : "+res);	
						smsCount++;
						//回寫註記，因為有區分Mccmnc，全部紀錄避免之後取不到
						for(String mccmnc : currentDayMap.get(sYearmonthday).get(serviceid).keySet()){
							currentDayMap.get(sYearmonthday).get(serviceid).get(mccmnc).put("ALERT", "1");
						}
						//Log SMS
						logSMS(phone,cont,res);
					}
				}
			} catch (IOException e) {
				exceptionHandle("At sendDayAlertSMS occur SQLException error!", e);
			}	
			logger.debug("Total send day alert "+smsCount+" ...");
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
		
		Set<String> checkedMCCMNC =new HashSet<String>();
		checkedMCCMNC.add("46001");
		checkedMCCMNC.add("46007");
		checkedMCCMNC.add("46002");
		checkedMCCMNC.add("460000");
		checkedMCCMNC.add("46000");
		checkedMCCMNC.add("45412");
		
		//是否為Data Only 客戶
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
						for(String mccmnc:currentDayMap.get(day).get(serviceid).keySet()){
							//確認Mccmnc
							if(checkedMCCMNC.contains(mccmnc)){
								//進行累計
								Double oldVolume=0D;
								Double volume=(Double) currentDayMap.get(day).get(serviceid).get(mccmnc).get("VOLUME");
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
		
		Double DEFAULT_VOLUME_THRESHOLD = Double.valueOf((String) systemConfig.get("VOLUME_LIMIT1"));
		Double DEFAULT_VOLUME_THRESHOLD2 = Double.valueOf((String) systemConfig.get("VOLUME_LIMIT2"));
		
		try {
			int smsCount=0;
			for(String serviceid:tempMap.keySet()){
				Double volume=tempMap.get(serviceid);
				Double everAlertVolume = (Double) currentMap.get(sYearmonth).get(serviceid).get("LAST_ALERN_VOLUME");
				//超過發簡訊，另外確認是否已通知過
				boolean sendmsg=false;
				Integer msgid=0;
				
				if(volume>=DEFAULT_VOLUME_THRESHOLD2 && everAlertVolume<volume){
					//2.0 GB 簡訊中文102，英文103
					msgid=102;
					sendmsg=true;
				}
				if(!sendmsg && volume>=DEFAULT_VOLUME_THRESHOLD && everAlertVolume<volume){
					//2.0 GB 簡訊中文100，英文101
					msgid=100;
					sendmsg=true;
				}
				
				if(sendmsg){
					String priceplanid = null;
					String phone = null;
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
					
					// 確認號碼
					if (phone == null || "".equals(phone)) {
						exceptionHandle("At sendAlertSMS occur error! The ServiceID:"+ serviceid + " can't find msisdn to send! ");
						continue;
					}
					
					//查詢所在國家的客服電話
					String cPhone = null;
					String nMccmnc=searchMccmncBySERVICEID(serviceid);
					Map<String,String> map=null;
					
					if(nMccmnc!=null && !"".equals(nMccmnc))
						map = codeMap.get(nMccmnc.substring(0,3));
					if(map!=null)
						cPhone=map.get("PHONE");
					
					//發送簡訊
					if(msgid==100)
						logger.info("For "+serviceid+" send 1.5GB decrease speed  message !");
					if(msgid==102)
						logger.info("For "+serviceid+" send 2.0GB decrease speed  message !");
					// 中文
					// 處理字串
					String cont = processMag(smsContent.get(msgid.toString()).get("CONTENT"),null, cPhone);
					// 發送簡訊
					String res = setSMSPostParam(cont, phone);
					logger.info("For "+serviceid+" send message:"+cont+" result:"+res);
					smsCount++;
					//Log SMS
					logSMS(phone,cont,res);

					// 英文
					msgid += 1;
					// 處理字串
					cont = processMag(smsContent.get(msgid.toString()).get("CONTENT"),null, cPhone);
					// 發送簡訊
					res = setSMSPostParam(cont, phone);
					logger.info("For "+serviceid+" send message:"+cont+" result:"+res);
					smsCount++;
					//Log SMS
					logSMS(phone,cont,res);

					//更新CurrentMap
					currentMap.get(sYearmonth).get(serviceid).put("LAST_ALERN_VOLUME",volume);

				}
				
			}
			logger.debug("Total send volume alert SMS " + smsCount + " ...");
		} catch (IOException e) {
			sql="";
			exceptionHandle("At sendDayAlertSMS occur SQLException error!", e);
		}
	}
	
	/**
	 * 處理替代字串
	 * {{bracket}} 額度
	 * @param msg
	 * @param bracket
	 * @return
	 */
	private String processMag(String msg,Double bracket,String cPhone){
		
		//金額
		if(bracket==null)
			bracket=0D;
		msg=msg.replace("{{bracket}}", FormatNumString(bracket,"NT#,##0.00"));
		
		//客服電話
		if(cPhone==null)
			cPhone="";
		msg=msg.replace("{{customerService}}",cPhone);
		
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
		
		
		
		return HttpPost("http://192.168.10.125:8800/Send%20Text%20Message.htm", param,"");
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
			sendMail("At suspend occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (IOException e) {
			logger.error("At suspend occur IOException error!", e);
			//send mail
			sendMail("At suspend occur IOException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (ClassNotFoundException e) {
			logger.error("At suspend occur ClassNotFoundException error!", e);
			//send mail
			sendMail("At suspend occur ClassNotFoundException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (Exception e) {
			logger.error("At suspend occur Exception error!", e);
			//send mail
			sendMail("At suspend occur Exception error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * 發送mail
	 * 
	 * @param content
	 */
	private static void sendMail(String content){
		mailReceiver=props.getProperty("mail.Receiver");
		mailSubject="DVRS Warnning Mail";
		mailContent="Error :"+content+"<br>\n"
				+ "Error occurr time: "+DateFormat()+"<br>\n"
				+ "SQL : "+sql+"<br>\n"
				+ "Error Msg : "+errorMsg;

		try {
			if(mailReceiver==null ||"".equals(mailReceiver)){
				logger.error("Can't send email without receiver!");
			}else{
				sendMail(mailSubject, mailContent, mailSender, mailReceiver);
			}
			
		} catch (Exception e) {
			logger.error("Error at sendMail",e);
		}
	}
	static void sendMail(String mailSubject,String mailContent,String mailSender,String mailReceiver){
		String [] cmd=new String[3];
		cmd[0]="/bin/bash";
		cmd[1]="-c";
		cmd[2]= "/bin/echo \""+mailContent+"\" | /bin/mail -s \""+mailSubject+"\" -r "+mailSender+" "+mailReceiver;

		try{
			Process p = Runtime.getRuntime().exec (cmd);
			p.waitFor();
			System.out.println("send mail cmd:"+cmd);
		}catch (Exception e){
			System.out.println("send mail fail:"+mailContent);
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
				if("501".equalsIgnoreCase(cMesg)){
					sendMail("Suspend does not work for"+"<br>"
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
				sendMail("At processSuspendNBR occur InterruptedException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			} catch (SQLException e) {
				logger.error("At processSuspendNBR occur SQLException error!", e);
				//send mail
				sendMail("At processSuspendNBR occur SQLException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			} catch (IOException e) {
				logger.error("At processSuspendNBR occur IOException error!", e);
				//send mail
				sendMail("At processSuspendNBR occur IOException error!");
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
	@SuppressWarnings("unused")
	private void showCurrent() {
		if(currentMap==null)
			return;
		for(String mon : currentMap.keySet()){
			for(String imsi : currentMap.get(mon).keySet()){
				System.out.print(" mon"+" : "+mon);
				System.out.print(", imsi"+" : "+imsi);
				System.out.print(", CHARGE"+" : "+currentMap.get(mon).get(imsi).get("CHARGE"));
				System.out.print(", VOLUME"+" : "+currentMap.get(mon).get(imsi).get("VOLUME"));
				System.out.print(", LAST_FILEID"+" : "+currentMap.get(mon).get(imsi).get("LAST_FILEID"));
				System.out.print(", SMS_TIMES"+" : "+currentMap.get(mon).get(imsi).get("SMS_TIMES"));
				System.out.print(", LAST_DATA_TIME"+" : "+currentMap.get(mon).get(imsi).get("LAST_DATA_TIME"));
				System.out.println();
			}
		}
	}
	@SuppressWarnings("unused")
	private void showCurrentDay() {
		for(String day : currentDayMap.keySet()){
			for(String imsi : currentDayMap.get(day).keySet()){
				for(String mccmnc : currentDayMap.get(day).get(imsi).keySet()){
					System.out.print(" day"+" : "+day);
					System.out.print(", imsi"+" : "+imsi);
					System.out.print(", mccmnc"+" : "+mccmnc);
					System.out.print(", CHARGE"+" : "+currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE"));
					System.out.print(", VOLUME"+" : "+currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME"));
					System.out.print(", LAST_FILEID"+" : "+currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID"));
					System.out.print(", LAST_DATA_TIME"+" : "+currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME"));
					System.out.println();
				}
			}
		}
	}
	@SuppressWarnings("unused")
	private void showDataRate(){
		for(String priceplanid : dataRate.keySet()){
			for(String mccmnc:dataRate.get(priceplanid).keySet() ){
				System.out.print(" priceplanid"+" : "+priceplanid);
				System.out.print(", mccmnc"+" : "+mccmnc);
				System.out.print(", RATE"+" : "+dataRate.get(priceplanid).get(mccmnc).get("RATE"));
				System.out.print(", CHARGEUNIT"+" : "+dataRate.get(priceplanid).get(mccmnc).get("CHARGEUNIT"));
				System.out.print(", CURRENCY"+" : "+dataRate.get(priceplanid).get(mccmnc).get("CURRENCY"));
				System.out.print(", DAYCAP"+" : "+dataRate.get(priceplanid).get(mccmnc).get("DAYCAP"));
				System.out.println();
			}
		}
	}
	@SuppressWarnings("unused")
	private void showSERVICEIDtoVLN(){
		for(String imsi : SERVICEIDtoVLN.keySet()){
			System.out.print("IMSI"+" : "+imsi);
			System.out.print(", VLN"+" : "+SERVICEIDtoVLN.get(imsi));
			System.out.println();
		}
	}
	@SuppressWarnings("unused")
	private void showVLNtoTADIG(){
		for(String vln : VLNtoTADIG.keySet()){
			System.out.print("VLN"+" : "+vln);
			System.out.print(", TADIG"+" : "+VLNtoTADIG.get(vln));
			System.out.println();
		}
	}
	@SuppressWarnings("unused")
	private void showTADIGtoMCCMNC(){
		for(String tadig : TADIGtoMCCMNC.keySet()){
			System.out.print("TADIG"+" : "+tadig);
			System.out.print(", MCCMNC"+" : "+TADIGtoMCCMNC.get(tadig));
			System.out.println();
		}
	}
	@SuppressWarnings("unused")
	private void showThresHold(){
		for(String imsi : thresholdMap.keySet()){
			System.out.print("IMSI"+" : "+imsi);
			System.out.print(", threshold"+" : "+thresholdMap.get(imsi));
			System.out.println();
		}
	}
	@SuppressWarnings("unused")
	private void showMsisdnMap(){
		for(String imsi : msisdnMap.keySet()){
			System.out.print("IMSI"+" : "+imsi);
			System.out.print(", MSISDN"+" : "+msisdnMap.get(imsi).get("MSISDN"));
			System.out.print(", PRICEPLANID"+" : "+msisdnMap.get(imsi).get("PRICEPLANID"));
			System.out.println();
		}
	}
	private void show(){
		//System.out.println("Show lastfileID"+" : ");
		//System.out.println("lastfileID"+" : "+lastfileID);
		//System.out.println("Show Current"+" : ");
		//showCurrent();
		//System.out.println("Show CurrentOld"+" : ");
		//showCurrentOld();
		//System.out.println("Show CurrentDay"+" : ");
		//showCurrentDay();
		//System.out.println("Show MsisdnMap"+" : ");
		//showMsisdnMap();
		//System.out.println("Show SERVICEIDtoVLN"+" : ");
		//showSERVICEIDtoVLN();
		//System.out.println("Show TADIGtoMCCMNC"+" : ");
		//showTADIGtoMCCMNC();
		//System.out.println("Show VLNtoTADIG"+" : ");
		//showVLNtoTADIG();
		//System.out.println("Show ThresHold"+" : ");
		//showThresHold();
		//System.out.println("Show DataRate"+" : ");
		//showDataRate();
		
	}
	
	private void exceptionHandle(String msg){
		exceptionHandle(msg,null);
	}
	
	private void exceptionHandle(String msg,Exception e){
		errorMsg="";
		if(logger!=null){
			if(e != null){
				logger.error(msg, e);
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			}
			else{
				logger.error(msg);
			}
		}
		//send mail
		sendMail(msg);			
	}

	/*************************************************************************
	 *************************************************************************
	 *                                主程式
	 *************************************************************************
	 *************************************************************************/
	
	public static void main(String[] args) {
		

		IniProgram();

		DVRSmaintest rf =new DVRSmaintest();
		rf.process();
		
		//regularTimeRun();

	}
	
	public static void regularTimeRun(){
		try {
			// Grab the Scheduler instance from the Factory
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

			// define the job and tie it to our HelloJob class ->
			// JobBuilder.newJob()
			JobDetail job = JobBuilder.newJob(DVRSmaintest.class)
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
			sendMail("at regularTimeRun occure error!");
		}
	}
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		
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
					sendMail("At waiting thread occur error!");
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
		process();
		
		//將動作交給下個thread
		executing=false;
		
		//sendMail("test mail " + new Date());
	}
	
	private void process() {
		// 程式開始時間
		long startTime;
		// 程式結束時間
		long endTime;
		// 副程式開始時間
		long subStartTime;

		logger.info("RFP Program Start! "+new Date());
		
		// 進行DB連線
			connectDB();
			connectDB2();
		
		if (conn != null && conn2!=null) {
			
			logger.debug("connect success!");
			
			startTime = System.currentTimeMillis();
			
			//取消自動Commit
			cancelAutoCommit();
			
			//設定日期
			setDayDate();
			
			//取得系統設定
			subStartTime = System.currentTimeMillis();
			setSystemConfig();
			logger.info("setSystemConfig execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//設定Priceplanid 對應幣別
			subStartTime = System.currentTimeMillis();
			setCurrencyMap();
			logger.info("setCurrencyMap execute time :"+(System.currentTimeMillis()-subStartTime));
			
			
			//設定IMSI至ServiecId的對應
			subStartTime = System.currentTimeMillis();
			setIMSItoServiceIDMap();
			logger.info("setIMSItoServiceIDMap execute time :"+(System.currentTimeMillis()-subStartTime));

			//取得最後更新的FileID
			subStartTime = System.currentTimeMillis();
			setLastFileID();
			logger.info("setLastFileID execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//取出HUR_CURRENT
			subStartTime = System.currentTimeMillis();
			if(currentMap.size()==0) setCurrentMap();
			if(oldChargeMap.size()==0) setoldChargeMap();
			if(currentDayMap.size()==0) setCurrentMapDay();
			logger.info("setCurrentMap execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//取出HUR_THRESHOLD
			subStartTime = System.currentTimeMillis();
			setThreshold();
			logger.info("setThreshold execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//取出HUR_DATARATE
			subStartTime = System.currentTimeMillis();
			setDataRate();
			logger.info("setDataRate execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//取出msisdn資訊
			subStartTime = System.currentTimeMillis();
			setMsisdnMap();
			logger.info("setMsisdnMap execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//20141211 add
			//IP 對應到 MCCMNC
			subStartTime = System.currentTimeMillis();
			setIPtoMccmncList();
			logger.info("setIPtoMccmncList execute time :"+(System.currentTimeMillis()-subStartTime));
	
			//IMSI 對應到 vln
			subStartTime = System.currentTimeMillis();
			setSERVICEIDtoVLN();
			logger.info("setSERVICEIDtoVLN execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//vln 對應到 TADIG
			subStartTime = System.currentTimeMillis();
			setVLNtoTADIG();
			logger.info("setVLNtoTADIG execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//TADIG 對應到 MCCMNC
			subStartTime = System.currentTimeMillis();
			setTADIGtoMCCMNC();
			logger.info("setTADIGtoMCCMNC execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//國碼對應表(客服,國名)
			subStartTime = System.currentTimeMillis();
			setCostomerNumber();
			logger.info("setCostomerNumber execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//華人上網包申請資料
			subStartTime = System.currentTimeMillis();
			setAddonData();
			logger.info("setAddonData execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//網域對應MCCMNC
			subStartTime = System.currentTimeMillis();
			setIPtoMccmncList();
			logger.info("setIPtoMccmncList execute time :"+(System.currentTimeMillis()-subStartTime));
			
			
			//開始批價 
			subStartTime = System.currentTimeMillis();
			charge();
			logger.info("charge execute time :"+(System.currentTimeMillis()-subStartTime));

			//取的簡訊設定
			subStartTime = System.currentTimeMillis();
			setSMSSettingMap();
			logger.info("charge setSMSSettingMap time :"+(System.currentTimeMillis()-subStartTime));
			
			//取得簡訊內容
			subStartTime = System.currentTimeMillis();
			setSMSContent();
			logger.info("charge setSMSContent time :"+(System.currentTimeMillis()-subStartTime));
			
			
			//發送警示簡訊
			subStartTime = System.currentTimeMillis();
			sendAlertSMS();
			logger.info("sendAlertSMS execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//回寫批價結果
			subStartTime = System.currentTimeMillis();
			//避免資料異常，完全處理完之後在commit
			try {
				updateCdr();
				insertCurrentMap();
				insertCurrentMapDay();
				updateCurrentMap();
				updateCurrentMapDay();
				conn.commit();
			} catch (SQLException e) {
				try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				logger.error("At updateTable occur SQLException error!", e);
				//send mail
				sendMail("At updateTable occur SQLException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			}
			logger.info("insert＆update execute time :"+(System.currentTimeMillis()-subStartTime));

			//suspend的後續追蹤處理
			subStartTime = System.currentTimeMillis();
			processSuspendNBR();
			logger.info("processSuspendNBR execute time :"+(System.currentTimeMillis()-subStartTime));
			
			
			// 程式執行完成
			endTime = System.currentTimeMillis();
			logger.info("Program execute time :" + (endTime - startTime));
			show();
			closeConnect();

		} else {
			logger.error("connect is null!");
			sendMail("Cannot connect to DB!");
		}
	}	
}



package program;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
		
		static IJatool tool=new Jatool();
		SimpleDateFormat spf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		private static String sql="";
		private static String errorMsg="";
		
		//Hur Data conf
		/*private static Integer dataThreshold=null;//CDR資料一批次取出數量
		 * */
		Integer lastfileID=0;//最後批價檔案號
		/*
		private static Double exchangeRate=null; //港幣對台幣匯率，暫訂為4
		private static Double kByte=null;//RATE單位KB，USAGE單位B
*/		
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
	/*	private static String DEFAULT_MCCMNC=null;//預設mssmnc
		private static Double DEFAULT_THRESHOLD=null;//預設月警示量
		private static Double DEFAULT_DAY_THRESHOLD=null;//預設日警示量
		private static Double DEFAULT_DAYCAP=null;
		private static Double DEFAULT_VOLUME_THRESHOLD=null;//預設流量警示(降速)，1.5GB;
		private static Double DEFAULT_VOLUME_THRESHOLD2=null;//預設流量警示(降速)，15GB;*/
		private static String DEFAULT_PHONE=null;
		private static Boolean TEST_MODE=true;
		
		//多排程處理
		private static boolean executing =false;
		private static boolean hasWaiting = false;
		
		
		Map<String,Map<String,Map<String,Object>>> currentMap = new HashMap<String,Map<String,Map<String,Object>>>();
		Map<String,Map<String,Map<String,Map<String,Object>>>> currentDayMap = new HashMap<String,Map<String,Map<String,Map<String,Object>>>>();
		Map <String,Double> cdrChargeMap = new HashMap<String,Double>();
		Map <String,Set<String>> existMap = new HashMap<String,Set<String>>();
		Map <String,Map <String,Set<String>>> existMapD = new HashMap<String,Map <String,Set<String>>>();
		
		Map<String,Map<String,Map<String,Object>>> dataRate = new HashMap<String,Map<String,Map<String,Object>>>();
		Map<String,Map<String,String>> msisdnMap = new HashMap<String,Map<String,String>>();
		Map<String,Double> thresholdMap = new HashMap<String,Double>();
		Map<String,String> IMSItoVLN =new HashMap<String,String>();
		Map<String,String> VLNtoTADIG =new HashMap<String,String>();
		Map<String,String> TADIGtoMCCMNC =new HashMap<String,String>();
		Map<String,Double> oldChargeMap = new HashMap<String,Double>();
		Map<String,Map<String,String>> codeMap = new HashMap<String,Map<String,String>>();
		List<Map<String,Object>> addonDataList = new ArrayList<Map<String,Object>>();
		Set<Map<String,String>> serviceOrderNBR = new HashSet<Map<String,String>>();
		Map<String,String> pricePlanIdtoCurrency = new HashMap<String,String>();
		
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

				/*DEFAULT_MCCMNC=props.getProperty("progrma.DEFAULT_MCCMNC");//預設mssmnc
				DEFAULT_THRESHOLD=(props.getProperty("progrma.DEFAULT_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_THRESHOLD")):5000D);//預設月警示量
				DEFAULT_DAY_THRESHOLD=(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")):500D);//預設日警示量
				DEFAULT_DAYCAP=(props.getProperty("progrma.DEFAULT_DAYCAP")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_DAYCAP")):500D);
				DEFAULT_VOLUME_THRESHOLD=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")):1.5*1024*1024D);//預設流量警示(降速)，1.5GB;
				DEFAULT_VOLUME_THRESHOLD2=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")):1.5*1024*1024D);//預設流量警示(降速)，15GB;
				*/
				DEFAULT_PHONE=props.getProperty("progrma.DEFAULT_PHONE");
				RUN_INTERVAL=(props.getProperty("progrma.RUN_INTERVAL")!=null?Integer.parseInt(props.getProperty("progrma.RUN_INTERVAL")):3600);
				TEST_MODE=("true".equalsIgnoreCase(props.getProperty("progrma.TEST_MODE"))?true:false);
				
/*				dataThreshold=(props.getProperty("progrma.dataThreshold")!=null?Integer.parseInt(props.getProperty("progrma.dataThreshold")):500);//CDR資料一批次取出數量
				//lastfileID=(props.getProperty("progrma.lastfileID")!=null?Integer.parseInt(props.getProperty("progrma.lastfileID")):0);//最後批價檔案號
				exchangeRate=(props.getProperty("progrma.exchangeRate")!=null?Double.parseDouble(props.getProperty("progrma.exchangeRate")):4); //港幣對台幣匯率，暫訂為4
				kByte=(props.getProperty("progrma.kByte")!=null?Double.parseDouble(props.getProperty("progrma.kByte")):1/1024D);//RATE單位KB，USAGE單位B
*/				
				logger.info(
						""
						/*+ "DEFAULT_MCCMNC : "+DEFAULT_MCCMNC+"\n"
						+ "DEFAULT_THRESHOLD : "+DEFAULT_THRESHOLD+"\n"
						+ "DEFAULT_DAY_THRESHOLD : "+DEFAULT_DAY_THRESHOLD+"\n"
						+ "DEFAULT_DAYCAP : "+DEFAULT_DAYCAP+"\n"
						+ "DEFAULT_VOLUME_THRESHOLD : "+DEFAULT_VOLUME_THRESHOLD+"\n"
						+ "DEFAULT_VOLUME_THRESHOLD2 : "+DEFAULT_VOLUME_THRESHOLD2+"\n"*/
						+ "DEFAULT_PHONE : "+DEFAULT_PHONE+"\n"
						+ "RUN_INTERVAL : "+RUN_INTERVAL+"\n"
						+ "TEST_MODE : "+TEST_MODE+"\n"
						/*+ "dataThreshold : "+dataThreshold+"\n"
						//+ "lastfileID : "+lastfileID+"\n"
						+ "exchangeRate : "+exchangeRate+"\n"
						+ "kByte : "+kByte+"\n"*/
						);
				
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

		/**
		 * 關閉連線
		 */
		private void closeConnect() {
			if (conn != null) {
				try {
					logger.info("closeConnect1...");
					conn.close();
				} catch (SQLException e) {
					exceptionHandle("close Connect Error", e);
				}
			}
			
			if (conn2 != null) {
				try {
					logger.info("closeConnect2...");
					conn2.close();
				} catch (SQLException e) {
					exceptionHandle("close Connect2 Error", e);
				}
			}
		}
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
		 * 設定計費週期
		 * 取特定日期那個月的，前面加上calendar.setTime(date);設定date日期
		 */
		private void setDayDate(){
			logger.info("setMonthDate...");

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
		 * Key:MONTH,Value:Map(IMSI,Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_VOLUME)))
		 */
		private void setCurrentMap(){
			Statement st = null;
			ResultSet rs = null;
			try {
				//設定HUR_CURRENT計費，抓出這個月與下個月
				sql=
						"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.LAST_DATA_TIME,A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
						+ "FROM HUR_CURRENT A "
						+ "WHERE A.MONTH IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
				st = conn.createStatement();
				logger.debug("Query CurrentMap SQL : "+sql);
				rs = st.executeQuery(sql);
	
				while(rs.next()){
					Map<String,Object> map=new HashMap<String,Object>();
					Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();

					String imsi =rs.getString("IMSI");
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

					map2.put(imsi, map);
					currentMap.put(month,map2);

					//20141201 add 設定存在資料
					Set<String> set =new HashSet<String>();
					if(existMap.containsKey(month)){
						set=existMap.get(month);
					}
					set.add(imsi);
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
						"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.LAST_DATA_TIME,A.VOLUME,A.UPDATE_DATE,A.CREATE_DATE,A.MCCMNC,A.DAY,A.ALERT "
						+ "FROM HUR_CURRENT_DAY A "
						+ "WHERE SUBSTR(A.DAY,0,6) IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
				st = conn.createStatement();
				logger.debug("Query CurrentDay SQL : "+sql);
				rs = st.executeQuery(sql);
	
				while(rs.next()){
					Map<String,Object> map=new HashMap<String,Object>();
					Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();			
					Map<String,Map<String,Map<String,Object>>> map3=new HashMap<String,Map<String,Map<String,Object>>>();
					
					String day =rs.getString("DAY");
					String imsi =rs.getString("IMSI");
					String mccmnc=rs.getString("MCCMNC");
					
					if(day!=null && !"".equals(day) && 
							imsi!=null && !"".equals(imsi) &&
									mccmnc!=null && !"".equals(mccmnc)){
						if(currentDayMap.containsKey(day)){
							map3=currentDayMap.get(day);
							if(map.containsKey(imsi)){
								map2=map3.get(imsi);
							}
						}
								
						map.put("LAST_FILEID", rs.getInt("LAST_FILEID"));
						map.put("LAST_DATA_TIME", (rs.getDate("LAST_DATA_TIME")!=null?rs.getDate("LAST_DATA_TIME"):new Date()));
						map.put("CHARGE", rs.getDouble("CHARGE"));
						map.put("VOLUME", rs.getDouble("VOLUME"));
						map.put("ALERT", rs.getString("ALERT"));

						map2.put(mccmnc, map);
						map3.put(imsi, map2);
						currentDayMap.put(day,map3);

						//20141201 add 設定存在資料
						Map<String,Set<String>> map5 = new HashMap<String,Set<String>>();
						Set<String> set =new HashSet<String>();
						if(existMapD.containsKey(day)){
							map5=existMapD.get(day);
							if(map5.containsKey(imsi)){
								set=map5.get(imsi);
							}
						}
						set.add(mccmnc);
						map5.put(imsi, set);
						existMapD.put(day, map5);
					}
				}

			} catch (SQLException e) {
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
			try {
				sql=
						"SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY,A.PRICEPLANID,A.DAYCAP "
						+ "FROM HUR_DATA_RATE A ";
				st = conn.createStatement();
				logger.debug("Query dataRate SQL : "+sql);
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
		 * 取出HUR_THRESHOLD
		 * 建立MAP Key:IMSI,VALUE:THRESHOLD
		 */
		private void setThreshold(){
			
			Statement st = null;
			ResultSet rs = null;
			try {
				sql=
						"SELECT A.IMSI,A.THRESHOLD "
						+"FROM HUR_GPRS_THRESHOLD A ";
				st = conn.createStatement();
				logger.debug("Query GPRS threshold SQL : "+sql);
				rs = st.executeQuery(sql);
	
				while(rs.next()){
					thresholdMap.put(rs.getString("IMSI"), rs.getDouble("THRESHOLD"));
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
		private void setIMSItoVLN(){
			Statement st = null;
			ResultSet rs = null;
			try {
				sql=
						"SELECT A.VLR_NUMBER,A.IMSI "
						+ "FROM UTCN.BASICPROFILE A "
						+ "WHERE A.VLR_NUMBER is not null ";
				st = conn2.createStatement();
				logger.debug("Query IMSItoVLN threshold SQL : "+sql);
				rs = st.executeQuery(sql);
	
				while(rs.next()){
					IMSItoVLN.put(rs.getString("IMSI"), rs.getString("VLR_NUMBER"));
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
		 * 從IMSItoVLN取得VALUE必須縮位匹配
		 * 
		 * MAP KEY：VLN,VALUE:TADIG
		 */
		private void setVLNtoTADIG(){
			Statement st = null;
			ResultSet rs = null;
			try {
				sql=
						"SELECT B.REALMNAME TADIG, A.CHARGEAREACODE VLR "
						+ "FROM CHARGEAREACONFIG A, REALM B "
						+ "WHERE A.AREAREFERENCE=B.AREAREFERENCE";
				st = conn2.createStatement();
				logger.debug("Query VLNtoTADIG threshold SQL : "+sql);
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
			try {
				sql=
						"SELECT A.TADIG,A.MCCMNC "
						+ "FROM HUR_MCCMNC A ";
				st = conn.createStatement();
				logger.debug("Query TADIGtoMCCMNC threshold SQL : "+sql);
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
			try {
				sql=
						"SELECT A.CODE,A.PHONE,A.NAME "
						+ "FROM HUR_CUSTOMER_SERVICE_PHONE A";
				st = conn.createStatement();
				logger.debug("Query Costomer Service Number threshold SQL : "+sql);
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
		 * 從imsi找尋目前的mccmnc
		 */
		public String searchMccmncByIMSI(String imsi){
			String mccmnc=null;
			
			String vln=IMSItoVLN.get(imsi);
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
		 * 建立華人上網包對應資料
		 * 
		 * List Map KEY:MSISDN,VALUE(IMSI,MSISDN,SERVICECODE,STARTDATE,ENDDATE)>
		 */
		private void setAddonData(){
			Statement st = null;
			ResultSet rs = null;
			try {
				sql=
						"SELECT A.S2TIMSI IMSI,A.S2TMSISDN MSISDN,A.SERVICECODE,A.STARTDATE,A.ENDDATE "
						+ "FROM ADDONSERVICE_N A ";
				st = conn.createStatement();
				logger.debug("Query AddonData SQL : "+sql);
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
		
		
		private int findAddonData(String imsi,String mccmnc,Date callTime){
			//20141118 排除華人上網包
			Set<String> sSX001 = new HashSet<String>();
			sSX001.add("45412");
			
			Set<String> sSX002 = new HashSet<String>();
			sSX002.add("46001");
			sSX002.add("46007");
			sSX002.add("46002");
			sSX002.add("460000");
			sSX002.add("46000");
			sSX002.add("45412");

			int cd=0;
			
			String msisdn=null;
			if(msisdnMap.containsKey(imsi)) {
				msisdn=msisdnMap.get(imsi).get("MSISDN");
				
				//只有香港
				if(cd==0 && sSX001.contains(mccmnc)){
					for(Map<String,Object> m : addonDataList){
						if(imsi.equals(m.get("IMSI"))&&msisdn.equals(m.get("MSISDN"))&&"SX001".equals(m.get("SERVICECODE"))&&
								callTime.after((Date) m.get("STARTDATE"))&&(m.get("ENDDATE")==null ||callTime.before((Date) m.get("ENDDATE")))){
							cd=1;
							break;
						}
					}
				}
				
				//香港加中國大陸
				if(cd==0 && sSX002.contains(mccmnc)){
					for(Map<String,Object> m : addonDataList){
						if(imsi.equals(m.get("IMSI"))&&msisdn.equals(m.get("MSISDN"))&&"SX002".equals(m.get("SERVICECODE"))&&
								callTime.after((Date) m.get("STARTDATE"))&&(m.get("ENDDATE")==null ||callTime.before((Date) m.get("ENDDATE")))){
							cd=1;
							break;
						}
					}
				}
			}
			return cd;
		}

		private void updateCurrentDayMap(String cDay,String imsi,String mccmnc,Double charge,
				Double dayCap,Integer fileID,Double volume,Date callTime){
			Double oldCharge=0D;
			Double oldvolume=0D;
			String alert="0";	
			
			//更新currentDayMap，如果此筆CDR記錄時間的月紀錄存在
			//察看是否有以存在的資料，有的話取出做累加
			Map<String,Map<String,Map<String,Object>>> map=new HashMap<String,Map<String,Map<String,Object>>>();
			Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();
			Map<String,Object> map3=new HashMap<String,Object>();
			
			if(currentDayMap.containsKey(cDay)){
				map=currentDayMap.get(cDay);
				if(map.containsKey(imsi)){
					map2=map.get(imsi);
				}
			}
			
			if( map2.containsKey(mccmnc)){
				map3=map2.get(mccmnc);

				oldCharge=(Double)map3.get("CHARGE");
				charge=oldCharge+charge;
				alert=(String)map3.get("ALERT");
				oldvolume=(Double)map3.get("VOLUME");
			
				if(fileID<(Integer) map3.get("LAST_FILEID"))
					fileID=(Integer) map3.get("LAST_FILEID");
			}
			
			//如果有計費上線，限制最大值  20141125 取消預設Daycap，如果值為負，表示沒有
			//if(dayCap==null || dayCap==0) dayCap= DEFAULT_DAYCAP;
			if(dayCap!=null && dayCap>=0 && charge>dayCap) charge=dayCap;
			
			//將結果記錄到currentDayMap
			map3.put("CHARGE", charge);
			map3.put("LAST_FILEID",fileID);
			map3.put("LAST_DATA_TIME",callTime);
			map3.put("VOLUME",volume+oldvolume);
			map3.put("ALERT",alert);
			map2.put(mccmnc, map3);
			map.put(imsi, map2);
			currentDayMap.put(cDay, map);
			
			//更新currentMap
			updateCurrentMap(cDay.substring(0,6),imsi,oldCharge,volume,fileID
					,callTime,charge);
		}
		
		private void updateCurrentMap(String cMonth,String imsi,Double oldCharge,Double volume,Integer fileID
				,Date callTime,Double charge){
			//更新currentMap，如果此筆CDR記錄時間的月紀錄存在
			Double preCharge=0D;
			Integer smsTimes=0;
			String suspend="0";
			Double lastAlertThreshold=0D;
			Double volumeAlert=0D;
			
			Map<String,Object> map4=new HashMap<String,Object>();
			Map<String,Map<String,Object>> map5=new HashMap<String,Map<String,Object>>();
			
			if(currentMap.containsKey(cMonth)){
				map5=currentMap.get(cMonth);
			}
			
			if(map5.containsKey(imsi)){
				map4=map5.get(imsi);
				
				preCharge=(Double)map4.get("CHARGE")-oldCharge;
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
			map4.put("CHARGE", preCharge+charge);
			map4.put("VOLUME", volume);
			map4.put("EVER_SUSPEND", suspend);
			map4.put("LAST_ALERN_THRESHOLD", lastAlertThreshold);
			map4.put("LAST_ALERN_VOLUME", volumeAlert);
			map5.put(imsi, map4);
			currentMap.put(cMonth, map5);
		}
		
		/**
		 * IP number to Mccmnc
		 * Map Value:START_NUM,END_NUM,MCCMNC
		 */
		List<Map<String,Object>> IPtoMccmncList = new ArrayList<Map<String,Object>>();
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
		 * 開始批價
		 */
		private void charge(){
			Integer dataThreshold = Integer.valueOf((String) systemConfig.get("DATA_THRESHOLD"));
			int count = dataCount();
			Double defaultRate = defaultRate();
			String DEFAULT_MCCMNC = (String) systemConfig.get("DEFAULT_MCCMNC");
			Double exchangeRate = Double.valueOf((String) systemConfig.get("EXCHANGE_RATE"));
			Double kByte = Double.valueOf((String) systemConfig.get("KBYTE"));
			//批次Query 避免ram空間不足
			for(int i=1;(i-1)*dataThreshold+1<=count ;i++){
				logger.debug("Round "+i+" Procsess ...");
				Statement st = null;
				ResultSet rs = null;
				
				try {
					sql=
							"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
							+ "FROM ( "
							+ "			SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME,A.SGSNADDRESS "
							+ "			FROM HUR_DATA_USAGE A "
							+ "			WHERE A.FILEID>= "+lastfileID+" AND ROWNUM <= "+(i*dataThreshold)+" AND A.CHARGE is null "
							+ "			ORDER BY A.USAGEID,A.FILEID) "
							+ "MINUS "
							+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME,SGSNADDRESS "
							+ "FROM ( "
							+ "			SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME,A.SGSNADDRESS "
							+ "			FROM HUR_DATA_USAGE A "
							+ "			WHERE A.FILEID>= "+lastfileID+" AND ROWNUM <= "+((i-1)*dataThreshold)+" AND A.CHARGE is null "
							+ "			ORDER BY A.USAGEID,A.FILEID) ";
					st = conn.createStatement();
					rs = st.executeQuery(sql);

					while(rs.next()){
						String imsi = rs.getString("IMSI");
						String mccmnc = rs.getString("MCCMNC");
						String usageId = rs.getString("USAGEID");
						Double volume = rs.getDouble("DATAVOLUME");
						Date callTime = rs.getDate("CALLTIME");
						Integer fileID = rs.getInt("FILEID");
						String cDay = tool.DateFormat(callTime, DAY_FORMATE);
						Double dayCap = null;
						Double charge = 0D;
						
						//20141211 add
						String ipaddr = rs.getString("SGSNADDRESS");
						
						//20141210 add
						String currency=null;
						
						String pricplanID=null;
						if(msisdnMap.containsKey(imsi))
							pricplanID=msisdnMap.get(imsi).get("PRICEPLANID");
		
						if(dataRate.containsKey(pricplanID)){
							//20141210 add
							currency=pricePlanIdtoCurrency.get(pricplanID);
							
							if(mccmnc==null || "".equals(mccmnc)){
								mccmnc=searchMccmncByIP(ipaddr);
								if(mccmnc!=null && !"".equals(mccmnc))
									logger.debug("For CDR usageId="+usageId+" which is without mccmnc. Found mccmnc="+mccmnc+" by IP range.");
							}
							
							if(mccmnc==null || "".equals(mccmnc)){
								//藉由vln的方法去推估MCCMNC
								mccmnc=searchMccmncByIMSI(imsi);
								if(mccmnc!=null && !"".equals(mccmnc))
									logger.debug("For CDR usageId="+usageId+" which is without mccmnc. Found mccmnc="+mccmnc+" by VLN record.");
							}
							
						}else{
							sql="";
							exceptionHandle("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
							//20141210 add 沒有PircePlanID或找不到幣別， 跳過、不批
							continue;
						}

						//還是找不到，給予預設，必須有
						if(mccmnc==null || "".equals(mccmnc)){
							mccmnc= DEFAULT_MCCMNC;
						}

						//處理華人上網包部分
						int cd=findAddonData(imsi,mccmnc,callTime);
						//如果在華人上網包，不批價，此筆為0
						if(cd==0){
							//判斷是否可以找到對應的費率表，並計算此筆CDR的價格(charge)
							if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
									dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
								double ec=1;
								//20141210 取消港幣全部換成台幣批價
								/*String currency=(String) dataRate.get(pricplanID).get(mccmnc).get("CURRENCY");
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
								
								
								sql="";
								exceptionHandle("IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");

								
							}
							
							//格式化至小數點後四位
							charge=tool.FormatDouble(charge, "0.0000");
						}
						
						//將此筆CDR結果記錄，稍後回寫到USAGE TABLE
						cdrChargeMap.put(usageId, charge);
						
						//更新Map
						updateCurrentDayMap(cDay,imsi,mccmnc,charge,dayCap,fileID,volume,callTime);
					
					}
					
				} catch (SQLException e) {
					exceptionHandle("At charge occur SQLException error", e);
					//批價出現Exception，離開批價
					break;
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
		}
		
		
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
		
		/***
		 * 回寫CDR的CHARGE
		 */
		private void updateCdr(){
			int count = 0;
			int dataThreshold = Integer.valueOf((String) systemConfig.get("DATA_THRESHOLD"));
			Statement st = null;
			ResultSet rs = null;
			
			try {
				st = conn.createStatement();
				for(String s: cdrChargeMap.keySet()){
					sql=
							"UPDATE HUR_DATA_USAGE A "
							+ "SET A.CHARGE="+cdrChargeMap.get(s)+" "
							+ "WHERE A.USAGEID='"+s+"' ";
					logger.debug("Update CDR SQL : "+sql);
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
			} catch (SQLException e) {
				exceptionHandle("At updateCdr occur SQLException error", e);
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
		 * 取消connection的Auto commit
		 */
		private void cancelAutoCommit(){
			try {
				logger.info("set AutoCommit false!");
				conn.setAutoCommit(false);
				
			} catch (SQLException e) {
				exceptionHandle("At cancelAutoCommit occur SQLException error", e);
			}
		}
		
		private static void IniProgram(){
			// 初始化log
			// iniLog4j();
			loadProperties();

		}
		
		private void connectDB(){
			// 進行DB連線
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
				logger.info("Connrct to "+url);
			} catch (ClassNotFoundException e) {
				exceptionHandle("At connDB occur ClassNotFoundException error", e);
			} catch (SQLException e) {
				sql = "";
				exceptionHandle("At connDB occur SQLException error", e);
			}
		}
		
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
				exceptionHandle("At connDB2 occur ClassNotFoundException error", e);
			} catch (SQLException e) {
				sql="";
				exceptionHandle("At connDB2 occur SQLException error", e);
			}
		}

		/**
		 * 計算完畢後寫回資料庫-更新
		 * @throws SQLException 
		 */

		private void updateCurrent(){
			int count = 0;
			int dataThreshold = Integer.valueOf((String) systemConfig.get("DATA_THRESHOLD"));
			Statement st = null;
			ResultSet rs = null;
			try {
				st = conn.createStatement();
				//20141201 add change another method to distinguish insert and update
				for(String mon : currentMap.keySet()){
					for(String imsi : currentMap.get(mon).keySet()){
						if(existMap.containsKey(mon) && existMap.get(mon).contains(imsi)){
							sql=
									"UPDATE HUR_CURRENT A "
									+ "SET A.CHARGE="+currentMap.get(mon).get(imsi).get("CHARGE")
									+ ",A.LAST_FILEID="+currentMap.get(mon).get(imsi).get("LAST_FILEID")
									+ ",A.SMS_TIMES="+currentMap.get(mon).get(imsi).get("LAST_FILEID")
									+ ",A.LAST_DATA_TIME=TO_DATE('"+spf.format((Date) currentMap.get(mon).get(imsi).get("LAST_DATA_TIME"))+"','yyyy/MM/dd hh24:mi:ss')"
									+ ",A.VOLUME="+currentMap.get(mon).get(imsi).get("VOLUME")
									+ ",A.EVER_SUSPEND='"+currentMap.get(mon).get(imsi).get("EVER_SUSPEND")+"'"
									+ ",A.LAST_ALERN_THRESHOLD="+currentMap.get(mon).get(imsi).get("LAST_ALERN_THRESHOLD")
									+ ",A.LAST_ALERN_VOLUME="+currentMap.get(mon).get(imsi).get("LAST_ALERN_THRESHOLD")
									+ ",A.UPDATE_DATE=SYSDATE "
									+ "WHERE A.MONTH='"+mon+"' AND A.IMSI='"+imsi+"' ";
							logger.info("Update Current SQL : "+sql);
							st.addBatch(sql);
							count++;
							
							if(count==dataThreshold){
								st.executeBatch();
								count=0;
							}
						}	
					}
				}
				if(count!=0){
					st.executeBatch();
				}
			} catch (SQLException e) {
				exceptionHandle("At update Current occur SQLException error", e);
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
		private void updateCurrentDay(){
			int count = 0;
			int dataThreshold = Integer.valueOf((String) systemConfig.get("DATA_THRESHOLD"));
			Statement st = null;
			ResultSet rs = null;
			try {
				st = conn.createStatement();
				//20141201 add change another method to distinguish insert and update
				for(String day : currentDayMap.keySet()){
					for(String imsi : currentDayMap.get(day).keySet()){
						for(String mccmnc : currentDayMap.get(day).get(imsi).keySet()){
							if(existMapD.containsKey(day) && existMapD.get(day).containsKey(imsi) && existMapD.get(day).get(imsi).contains(mccmnc)){
								sql=
										"UPDATE HUR_CURRENT_DAY A "
										+ "SET A.CHARGE="+currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE")
										+ ",A.LAST_FILEID="+currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID")
										+ ",A.LAST_DATA_TIME=TO_DATE('"+spf.format((Date) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME"))+"','yyyy/MM/dd hh24:mi:ss')"
										+ ",A.VOLUME="+currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME")
										+ ",A.ALERT='"+currentDayMap.get(day).get(imsi).get(mccmnc).get("ALERT")
										+ "',A.UPDATE_DATE=SYSDATE "
										+ "WHERE A.DAY='"+day+"' AND A.IMSI='"+imsi+"' AND A.MCCMNC='"+mccmnc+"' ";
								logger.info("Update CurrentDay SQL : "+sql);
								st.addBatch(sql);
								count++;
								if(count==dataThreshold){
									st.executeBatch();
									count=0;
								}
							}
						}
					}
				}
				if(count!=0){
					st.executeBatch();
				}
			} catch (SQLException e) {
				exceptionHandle("At update CurrentDay occur SQLException error", e);
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
		 * 計算完畢後寫回資料庫-新增
		 * @throws SQLException 
		 */
		
		private void insertCurrent(){
			//20141201 add change another method to distinguish insert and update
			for(String mon : currentMap.keySet()){
				for(String imsi : currentMap.get(mon).keySet()){
					if(!existMap.containsKey(mon) || !existMap.get(mon).contains(imsi)){
						Statement st = null;
						ResultSet rs = null;
						try {
							st = conn.createStatement();
							sql=
									"INSERT INTO HUR_CURRENT "
									+ "(IMSI,CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_THRESHOLD,LAST_ALERN_VOLUME,MONTH,CREATE_DATE) "
									+ "VALUES('"+imsi+"',"+currentMap.get(mon).get(imsi).get("CHARGE")+","+currentMap.get(mon).get(imsi).get("LAST_FILEID")
									+ ","+currentMap.get(mon).get(imsi).get("SMS_TIMES")
									+ ",TO_DATE('"+spf.format((Date) currentMap.get(mon).get(imsi).get("LAST_DATA_TIME"))+"','yyyy/MM/dd hh24:mi:ss')"
									+ ","+currentMap.get(mon).get(imsi).get("VOLUME")+",'"+currentMap.get(mon).get(imsi).get("EVER_SUSPEND")+"'"
									+ ","+currentMap.get(mon).get(imsi).get("LAST_ALERN_THRESHOLD")+","+currentMap.get(mon).get(imsi).get("LAST_ALERN_VOLUME")
									+ ",'"+mon+"',SYSDATE)";
							
							logger.info("Insert Current SQL : "+sql);
							st.executeUpdate(sql);

						} catch (SQLException e) {
							exceptionHandle("At Insert Current occur SQLException error", e);
							//新增失敗後嘗試update更新
							Set<String> set=new HashSet<String>();
							if(existMap.containsKey(mon))
								set=existMap.get(mon);
							
							set.add(imsi);
							existMap.put(mon, set);
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
				}
			}

			
		}
		private void insertCurrentDay(){
			//20141201 add change another method to distinguish insert and update
			for(String day : currentDayMap.keySet()){
				for(String imsi : currentDayMap.get(day).keySet()){
					for(String mccmnc : currentDayMap.get(day).get(imsi).keySet()){
						if(!existMapD.containsKey(day)||!existMapD.get(day).containsKey(imsi)||!existMapD.get(day).get(imsi).contains(mccmnc)){
							Statement st = null;
							ResultSet rs = null;
							try {
								st = conn.createStatement();
								sql=
										"INSERT INTO HUR_CURRENT_DAY "
										+ "(IMSI,CHARGE,LAST_FILEID,LAST_DATA_TIME,VOLUME,CREATE_DATE,MCCMNC,DAY,ALERT) "
										+ "VALUES('"+imsi+"',"+currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE")
										+ ","+currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID")
										+ ",TO_DATE('"+spf.format((Date) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME"))+"','yyyy/MM/dd hh24:mi:ss')"
										+ ","+currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME")+",SYSDATE,'"+mccmnc+"','"+day+"'"
										+ ",'"+currentDayMap.get(day).get(imsi).get(mccmnc).get("ALERT")+"')";
								logger.info("Insert CurrentDay SQL : "+sql);
								st.executeUpdate(sql);
								
							} catch (SQLException e) {
								exceptionHandle("At Insert CurrentDay occur SQLException error", e);
								
								//新增失敗後嘗試update更新
								Set<String> set=new HashSet<String>();
								Map<String,Set<String>> map = new HashMap<String,Set<String>>();
								if(existMapD.containsKey(day)){
									map=existMapD.get(day);
									if(map.containsKey(imsi)){
										set=map.get(imsi);
									}							
								}
								set.add(mccmnc);
								map.put(imsi, set);
								existMapD.put(day,map);
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
					}
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
		
		private void sendAlertSMS(){
			logger.info("sendAlertSMS...");
		
			

			if(smsContent.size()==0){
				sql="";
				exceptionHandle("Can't found SMS Content sentting!", null);
				return;
			}

			//月警示，只檢查當月資料*************************************
			if(currentMap.containsKey(sYearmonth)){
				int smsCount=0;
				
				for(String imsi: currentMap.get(sYearmonth).keySet()){
					try{
						//20141210 add 
						//沒有priceplanID 跳過
						String pricePlanID = msisdnMap.get(imsi).get("PRICEPLANID");
						String phone=null;
						if(pricePlanID==null ||"".equals(pricePlanID)){
							exceptionHandle("At sendAlertSMS occur error! The IMSI:"+imsi+" can't find pricePlanID!");
							continue;
						}
						List<Object> ids = smsSettingMap.get(pricePlanID).get("ID");
						List<Object> brackets = smsSettingMap.get(pricePlanID).get("BRACKET");
						List<Object> msgids = smsSettingMap.get(pricePlanID).get("MEGID");
						List<Object> suspends = smsSettingMap.get(pricePlanID).get("SUSPEND");
						//沒有讀取到設定 跳過
						if(ids.size()==0){
							exceptionHandle("At sendAlertSMS occur error! The PricePlanID:"+pricePlanID+" can't find setting!");
							continue;
						}
	
						//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
						if(msisdnMap.containsKey(imsi)){
							phone=(String) msisdnMap.get(imsi).get("MSISDN");
							if(phone==null ||"".equals(phone)){
								exceptionHandle("At sendAlertSMS occur error! The IMSI:"+imsi+" can't find msisdn to send !");
								continue;
							}
						}
							
						Double charge = (Double) currentMap.get(sYearmonth).get(imsi).get("CHARGE");
						Double oldCharge = (Double) oldChargeMap.get(imsi);
						String everSuspend =(String) currentMap.get(sYearmonth).get(imsi).get("EVER_SUSPEND");
						Double lastAlernThreshold=(Double) currentMap.get(sYearmonth).get(imsi).get("LAST_ALERN_THRESHOLD");
						int smsTimes=(Integer) currentMap.get(sYearmonth).get(imsi).get("SMS_TIMES");
						
						int i=0;
						boolean sendSMS=false;
						boolean needSuspend=false;
						boolean isCustomized=false;
						Double alertBracket=0D;
						Double differenceCharge = null;
						Double DEFAULT_THRESHOLD = null;
						Double threshold=null;
						String[] contentid=null;
						String res="";
						
						//抓取不同幣別月上限
						if("NTD".equals(pricePlanIdtoCurrency.get(pricePlanID)))
							DEFAULT_THRESHOLD = Double.valueOf((String) systemConfig.get("NTD_MONTH_LIMIT"));
						if("HKD".equals(pricePlanIdtoCurrency.get(pricePlanID)))
							DEFAULT_THRESHOLD = Double.valueOf((String) systemConfig.get("HKD_MONTH_LIMIT"));
						
						//20141118 修改 約定客戶不斷網
						threshold = thresholdMap.get(imsi);
						if(threshold==null){
							threshold=DEFAULT_THRESHOLD;
						}else{
							isCustomized=true;
						}
						
						//取不到任何上限值 跳過
						if(threshold==null){
							exceptionHandle("For IMSI:"+imsi+" PricePlanId:"+pricePlanID+" cannot get Month Limit! ");
							continue;
						}
						
						if(lastAlernThreshold==null)
							lastAlernThreshold=0D;
						
						if(oldCharge==null)	oldCharge=0D;
						differenceCharge = charge-oldCharge;
	
						//20141118 修改 約定客戶訂為每5000提醒一次，規則客制訂為0進行5000持續累積
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
									logger.info("For "+imsi+" add charge "+differenceCharge+" in this hour ,System forecast the next hour will over charge limit");
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
							String nMccmnc=searchMccmncByIMSI(imsi);
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
									logger.info("For "+imsi+" send "+smsTimes+"th message:"+cont+" result:"+res);
									currentMap.get(sYearmonth).get(imsi).put("LAST_ALERN_THRESHOLD", lastAlernThreshold);					
									currentMap.get(sYearmonth).get(imsi).put("SMS_TIMES", smsTimes);
									smsCount++;
									//Log SMS
									logSMS(phone,cont,res);
									
									//中斷GPRS服務
									//20141113 新增客制定上限不執行斷網
									if(needSuspend &&"0".equals(everSuspend)&&!isCustomized){	
										suspend(imsi,phone);
										currentMap.get(sYearmonth).get(imsi).put("EVER_SUSPEND", "1");
									}
								}								
							}
						}
					} catch (IOException e) {
						exceptionHandle("At sendDayAlertSMS occur SQLException error!", e);
					}
				}
				logger.debug("Total send month alert "+smsCount+" ...");
			}
			
			
			//實做日警示部分，有今日資料才警示*************************************
			if(currentDayMap.containsKey(sYearmonthday)){
				int smsCount=0;
				
				for(String imsi:currentDayMap.get(sYearmonthday).keySet()){
					
					//20141216 add 斷網過後，不發送每日簡訊，避免預估斷網後，每日帶出實際累計引發爭議
					if(currentMap.containsKey(sYearmonth) && currentMap.get(sYearmonth).containsKey(imsi)){
						String everSuspend =(String) currentMap.get(sYearmonth).get(imsi).get("EVER_SUSPEND");
						if("1".equals(everSuspend)){
							continue;
						}
					}
					
					try {
						String pricePlanID = msisdnMap.get(imsi).get("PRICEPLANID");
						String phone=null;
						if(pricePlanID==null ||"".equals(pricePlanID)){
							exceptionHandle("At sendAlertSMS occur error! The IMSI:"+imsi+" can't find pricePlanID!");
							continue;
						}
						
						//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
						if(msisdnMap.containsKey(imsi))
							phone=(String) msisdnMap.get(imsi).get("MSISDN");
						if(phone==null ||"".equals(phone)){
							exceptionHandle("At sendAlertSMS occur error! The IMSI:"+imsi+" can't find msisdn to send! ");
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
							exceptionHandle("For IMSI:"+imsi+" PricePlanId:"+pricePlanID+" cannot get Daily Limit! ");
							continue;
						}
	
						//累計
						for(String mccmnc : currentDayMap.get(sYearmonthday).get(imsi).keySet()){
							daycharge=daycharge+(Double)currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).get("CHARGE");
							String a=(String) currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).get("ALERT");
							if("1".equals(a)) alerted="1";
						}
						
						if(daycharge>=DEFAULT_DAY_THRESHOLD && "0".equalsIgnoreCase(alerted)){
							//查詢所在國家的客服電話
							String cPhone = null;
							String nMccmnc=searchMccmncByIMSI(imsi);
							Map<String,String> map=null;
							
							if(nMccmnc!=null && !"".equals(nMccmnc))
								map = codeMap.get(nMccmnc.substring(0,3));
							if(map!=null)
								cPhone=map.get("PHONE");
							
							//處理字串，日警示內容ID
							//20141209 修改為帶出當月累積金額
							String cont =null;
							if("NTD".equals(pricePlanIdtoCurrency.get(pricePlanID)) && systemConfig.get("NTD_DAY_LIMIT_MSG_ID")!=null)
								cont = processMag(smsContent.get(systemConfig.get("NTD_DAY_LIMIT_MSG_ID")).get("CONTENT"),(Double)currentMap.get(sYearmonth).get(imsi).get("CHARGE"),cPhone);
							if("HKD".equals(pricePlanIdtoCurrency.get(pricePlanID)) && systemConfig.get("HKD_DAY_LIMIT_MSG_ID")!=null)
								cont = processMag(smsContent.get(systemConfig.get("HKD_DAY_LIMIT_MSG_ID")).get("CONTENT"),(Double)currentMap.get(sYearmonth).get(imsi).get("CHARGE"),cPhone);
							
							//取不到簡訊內容 跳過
							if(cont== null){
								exceptionHandle("For IMSI:"+imsi+" PricePlanId:"+pricePlanID+" cannot get Daily SMS content! ");
								continue;
							}
							
							//發送簡訊
							String res = setSMSPostParam(cont,phone);
							logger.info("For "+imsi+" send message:"+cont+" result:"+res);
							smsCount++;
							//回寫註記，因為有區分Mccmnc，全部紀錄避免之後取不到
							for(String mccmnc : currentDayMap.get(sYearmonthday).get(imsi).keySet()){
								currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).put("ALERT", "1");
							}
							//Log SMS
							logSMS(phone,cont,res);
						}
					} catch (IOException e) {
						exceptionHandle("At sendDayAlertSMS occur SQLException error!", e);
					}	
				}
				logger.debug("Total send day alert "+smsCount+" ...");	
			}
			
			
			//降速提醒簡訊判斷*************************************
			
			//暫存數據用量資料 Key:IMSI,Value:Volume
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
					for(String imsi:currentDayMap.get(day).keySet()){
						//確認priceplanid 與 subsidiaryid
						
						String priceplanid = null;
						String subsidiaryid = null;
						if(msisdnMap.containsKey(imsi)){
							priceplanid = msisdnMap.get(imsi).get("PRICEPLANID");
							subsidiaryid = msisdnMap.get(imsi).get("SUBSIDIARYID");
						}
						
						if(checkedPriceplanid.contains(priceplanid)&&"72".equalsIgnoreCase(subsidiaryid)){
							for(String mccmnc:currentDayMap.get(day).get(imsi).keySet()){
								//確認Mccmnc
								if(checkedMCCMNC.contains(mccmnc)){
									//進行累計
									Double oldVolume=0D;
									Double volume=(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME");
									if(tempMap.containsKey(imsi)){
										oldVolume=tempMap.get(imsi);
									}
									tempMap.put(imsi, oldVolume+volume);
								}
							}
						}	
					}
				}
			}
			
			Double DEFAULT_VOLUME_THRESHOLD = Double.valueOf((String) systemConfig.get("VOLUME_LIMIT1"));
			Double DEFAULT_VOLUME_THRESHOLD2 = Double.valueOf((String) systemConfig.get("VOLUME_LIMIT2"));
			
			if (DEFAULT_VOLUME_THRESHOLD != null && DEFAULT_VOLUME_THRESHOLD2 != null) {
				try {
					int smsCount = 0;
	
					for (String imsi : tempMap.keySet()) {
						Double volume = tempMap.get(imsi);
						Double everAlertVolume = (Double) currentMap.get(sYearmonth).get(imsi).get("LAST_ALERN_VOLUME");
						
						// 超過發簡訊，另外確認是否已通知過
						boolean sendmsg = false;
						Integer msgid = 0;
						String phone=null;
	
						if (volume >= DEFAULT_VOLUME_THRESHOLD2 && everAlertVolume!=null && everAlertVolume < volume) {
							// 2.0 GB 簡訊中文102，英文103
							msgid = 102;
							sendmsg = true;
						}
						if (!sendmsg && volume >= DEFAULT_VOLUME_THRESHOLD && everAlertVolume != null && everAlertVolume < volume) {
							// 1.5 GB 簡訊中文100，英文101
							msgid = 100;
							sendmsg = true;
						}
	
						if (sendmsg) {
							String priceplanid = null;
							if (msisdnMap.containsKey(imsi)) {
								priceplanid = msisdnMap.get(imsi)
										.get("PRICEPLANID");
								// 是否為Data only 方案
								if (checkedPriceplanid2.contains(priceplanid)) {
									// 以設定通知號通知
									phone = msisdnMap.get(imsi).get("NCODE");
								} else {
									// 以門號通知
									phone = msisdnMap.get(imsi).get("MSISDN");
								}
							}
	
							// 確認號碼
							if (phone == null || "".equals(phone)) {
								exceptionHandle("At sendAlertSMS occur error! The IMSI:"+ imsi + " can't find msisdn to send! ");
								continue;
							}
	
							// 查詢所在國家的客服電話
							String cPhone = null;
							String nMccmnc = searchMccmncByIMSI(imsi);
							Map<String, String> map = null;
	
							if (nMccmnc != null && !"".equals(nMccmnc))
								map = codeMap.get(nMccmnc.substring(0, 3));
							if (map != null)
								cPhone = map.get("PHONE");

							// 中文
							// 處理字串
							String cont = processMag(smsContent.get(msgid.toString()).get("CONTENT"),null, cPhone);
							// 發送簡訊
							String res = setSMSPostParam(cont, phone);
							logger.info("For "+imsi+" send message:"+cont+" result:"+res);
							smsCount++;
							//Log SMS
							logSMS(phone,cont,res);
	
							// 英文
							msgid += 1;
							// 處理字串
							cont = processMag(smsContent.get(msgid.toString()).get("CONTENT"),null, cPhone);
							// 發送簡訊
							res = setSMSPostParam(cont, phone);
							logger.info("For "+imsi+" send message:"+cont+" result:"+res);
							smsCount++;
							//Log SMS
							logSMS(phone,cont,res);
	
							// 更新CurrentMap
							currentMap.get(sYearmonth).get(imsi).put("LAST_ALERN_VOLUME", volume);
						}
					}
					logger.debug("Total send volume alert SMS " + smsCount + " ...");
				} catch (IOException e) {
					sql="";
					exceptionHandle("At sendDayAlertSMS occur SQLException error!", e);
				}
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
			msg=msg.replace("{{bracket}}", tool.FormatNumString(bracket,"NT#,##0.00"));
			
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
			
			
			
			return tool.HttpPost("http://192.168.10.125:8800/Send%20Text%20Message.htm", param,"");
		}
		
		
		
		
		/**
		 * 取出msisdn
		 * 建立msisdnMap
		 * Key:imsi,Value:Map(MSISDN,PRICEPLANID,SUBSIDIARYID,NCODE)
		 */
		private void setMsisdnMap(){
			logger.info("setMsisdnMap...");
			sql=
					/*"SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID "
					+ "FROM SERVICE A,IMSI B "
					+ "WHERE A.SERVICEID=B.SERVICEID AND A.SERVICECODE IS NOT NULL ";*/
					"SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,"
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
				Statement st = conn.createStatement();
				logger.info("Execute SQL :"+sql);
				ResultSet rs=st.executeQuery(sql);
				
				while(rs.next()){
					Map<String,String> map =new HashMap<String,String>();
					map.put("MSISDN", rs.getString("SERVICECODE"));
					map.put("PRICEPLANID", rs.getString("PRICEPLANID"));
					map.put("SUBSIDIARYID", rs.getString("SUBSIDIARYID"));
					map.put("NCODE", rs.getString("NCODE"));
					msisdnMap.put(rs.getString("IMSI"), map);
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
		
		private void suspend(String imsi,String msisdn){
			Statement st = null;
			ResultSet rs = null;
			suspendGPRS sus = null;
			try {
				sus = new suspendGPRS(conn,conn2,logger);
				//20141118 add 傳回suspend排程的 service order nbr
				Map<String,String> orderNBR=sus.ReqStatus_17_Act(imsi, msisdn);
				serviceOrderNBR.add(orderNBR);
				
				st = conn.createStatement();
				sql=
						"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
						+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN) "
						+ "VALUES('"+orderNBR.get("cServiceOrderNBR")+"','"+imsi+"',SYSDATE,'"+msisdn+"')";
				
				logger.debug("Insert Suspend Log SQL : "+sql);
				rs = st.executeQuery(sql);
			} catch (SQLException e) {
				exceptionHandle("Insert Suspend Log occur SQLException error", e);
			} catch (IOException e) {
				sql="";
				exceptionHandle("Insert Suspend Log occur IOException error", e);
			} catch (ClassNotFoundException e) {
				sql="";
				exceptionHandle("Insert Suspend Log occur ClassNotFoundException error", e);
			} catch (Exception e) {
				sql="";
				exceptionHandle("Insert Suspend Log occur Exception error", e);
			}finally{
				try {
					if(st!=null)
						st.close();
					if(rs!=null)
						rs.close();
					if(sus!=null && sus.Temprs!=null) 
						sus.Temprs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
		private static void sendMail(String content){
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
				logger.error("Error at sendMail",e);
			} catch (MessagingException e) {
				logger.error("Error at sendMail",e);
			} catch (IOException e) {
				logger.error("Error at sendMail",e);
			}
		}

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
					sql="";
					exceptionHandle("At processSuspendNBR occur InterruptedException error!", e);
				} catch (SQLException e) {
					exceptionHandle("At processSuspendNBR occur SQLException error!", e);
				} catch (IOException e) {
					sql="";
					exceptionHandle("At processSuspendNBR occur IOException error!", e);
				}
			}
		}
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
		private void showIMSItoVLN(){
			for(String imsi : IMSItoVLN.keySet()){
				System.out.print("IMSI"+" : "+imsi);
				System.out.print(", VLN"+" : "+IMSItoVLN.get(imsi));
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
			//System.out.println("Show IMSItoVLN"+" : ");
			//showIMSItoVLN();
			//System.out.println("Show TADIGtoMCCMNC"+" : ");
			//showTADIGtoMCCMNC();
			//System.out.println("Show VLNtoTADIG"+" : ");
			//showVLNtoTADIG();
			//System.out.println("Show ThresHold"+" : ");
			//showThresHold();
			//System.out.println("Show DataRate"+" : ");
			//showDataRate();
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
				
				//取得最後更新的FileID
				subStartTime = System.currentTimeMillis();
				setLastFileID();
				logger.info("setLastFileID execute time :"+(System.currentTimeMillis()-subStartTime));
				
				//取出HUR_CURRENT
				subStartTime = System.currentTimeMillis();
				setCurrentMap();
				setoldChargeMap();
				setCurrentMapDay();
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
		
				//IMSI 對應到 vln
				subStartTime = System.currentTimeMillis();
				setIMSItoVLN();
				logger.info("setIMSItoVLN execute time :"+(System.currentTimeMillis()-subStartTime));
				
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
					insertCurrent();
					insertCurrentDay();
					updateCurrent();
					updateCurrentDay();
					conn.commit();
				} catch (SQLException e) {
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

				//scheduler.shutdown();

				//JobDetail job = new JobDetail("job1", "group1", SayHelloJob.class);
				//// 由於quartz support多group到多job. 這裡我們只有一個job. 我們自己我隨意把它命名.
				//// 但相同group裡如果出現相同的job名,會被overrride.
				//CronTrigger cTrigger = new CronTrigger("trigg1", "group1", "job1",
				//"group1", "1/10 * * * * ?");
				//// 這裡指定trigger執行那個group的job.
				//// "1/10 * * * * ?" 與 在unix like裡的crontab job的設定類似. 這裡表示每天裡的每10秒執行一次
				//// Seconds Minutes Hours Day-of-Month Month Day-of-Week Year(optional field)

			} catch (SchedulerException e) {
				e.printStackTrace();
				sendMail("at regularTimeRun occure error!");
			}
		}
		
		public static void main(String[] args) {
			IniProgram();
			
			DVRSmaintest rf =new DVRSmaintest();
			rf.process();
			
			//regularTimeRun();	
			//closeConnect();
		}

}

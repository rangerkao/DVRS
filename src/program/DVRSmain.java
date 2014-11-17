package program;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
	
	static int runInterval=1*60*60;//單位秒
	
	private  Logger logger ;
	Properties props=new Properties();

	
	//mail conf
	String mailSender="";
	String mailReceiver="";
	String mailSubject="mail test";
	String mailContent="mail content text";
	
	IJatool tool=new Jatool();
	
	
	private String sql="";
	private String errorMsg="";
	
	//Hur Data conf
	private Integer dataThreshold=null;//CDR資料一批次取出數量
	private Integer lastfileID=null;//最後批價檔案號
	private Double exchangeRate=null; //港幣對台幣匯率，暫訂為4
	private Double kByte=null;//RATE單位KB，USAGE單位B

	//日期設定
	private String MONTH_FORMATE="yyyyMM";
	//系統時間，誤差一小時，系統資料處理時間為當時時間提前一小時
	private String sYearmonth="";
	private String sYearmonthday="";
	//上個月
	private String sYearmonth2="";
	private String DAY_FORMATE="yyyyMMdd";	
	
	//預設值
	private String DEFAULT_MCCMNC=null;//預設mssmnc
	private Double DEFAULT_THRESHOLD=null;//預設月警示量
	private Double DEFAULT_DAY_THRESHOLD=null;//預設日警示量
	private Double DEFAULT_DAYCAP=null;
	private Double DEFAULT_VOLUME_THRESHOLD=null;//預設流量警示(降速)，1.5GB;
	private Double DEFAULT_VOLUME_THRESHOLD2=null;//預設流量警示(降速)，15GB;
	private String DEFAULT_PHONE=null;
	private Boolean TEST_MODE=true;
	
	Map<String,Map<String,Map<String,Object>>> currentMap = new HashMap<String,Map<String,Map<String,Object>>>();
	Map<String,Map<String,Map<String,Map<String,Object>>>> currentDayMap = new HashMap<String,Map<String,Map<String,Map<String,Object>>>>();
	Map<String,Map<String,Map<String,Object>>> dataRate = new HashMap<String,Map<String,Map<String,Object>>>();
	Map<String,Map<String,String>> msisdnMap = new HashMap<String,Map<String,String>>();
	Map<String,Double> thresholdMap = new HashMap<String,Double>();
	Map<String,String> IMSItoVLN =new HashMap<String,String>();
	Map<String,String> VLNtoTADIG =new HashMap<String,String>();
	Map<String,String> TADIGtoMCCMNC =new HashMap<String,String>();
	Map<String,Double> oldChargeMap = new HashMap<String,Double>();
	Map<String,Map<String,String>> codeMap = new HashMap<String,Map<String,String>>();

	Map <String,Set<String>> updateMap = new HashMap<String,Set<String>>();
	Map <String,Map <String,Set<String>>> updateMapD = new HashMap<String,Map <String,Set<String>>>();
	Map <String,Set<String>> insertMap = new HashMap<String,Set<String>>();
	Map <String,Map <String,Set<String>>> insertMapD = new HashMap<String,Map <String,Set<String>>>();
	Map <String,Double> cdrChargeMap = new HashMap<String,Double>();
	
	
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
	private  void loadProperties(){
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
			TEST_MODE=("true".equalsIgnoreCase(props.getProperty("progrma.TEST_MODE"))?true:false);
			
			dataThreshold=(props.getProperty("progrma.dataThreshold")!=null?Integer.parseInt(props.getProperty("progrma.dataThreshold")):500);//CDR資料一批次取出數量
			lastfileID=(props.getProperty("progrma.lastfileID")!=null?Integer.parseInt(props.getProperty("progrma.lastfileID")):0);//最後批價檔案號
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
					+ "TEST_MODE : "+TEST_MODE+"\n"
					+ "dataThreshold : "+dataThreshold+"\n"
					+ "lastfileID : "+lastfileID+"\n"
					+ "exchangeRate : "+exchangeRate+"\n"
					+ "kByte : "+kByte+"\n");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("File Not Found : "+e.getMessage());
			System.out.println("File Path : "+path);
			//send mail
			sendMail("At loadProperties occur file not found error \n <br>"
					+ "file path="+path);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IOException : "+e.getMessage());
			//send mail
			sendMail("At loadProperties occur IOException error !\n <br>"
					+ "file path="+path);
		}
		
	}

	/**
	 * 關閉連線
	 */
	private void closeConnect() {
		if (conn != null) {

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
				logger.debug("close Connect Error : "+e.getMessage());
				//send mail
				sendMail("At closeConnect occur SQLException error!");
			}

		}
		
		if (conn2 != null) {

			try {
				conn2.close();
			} catch (SQLException e) {
				e.printStackTrace();
				logger.debug("close Connect2 Error : "+e.getMessage());
				//send mail
				sendMail("At closeConnect occur SQLException error!");
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
		calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR)-1);
		sYearmonth2=tool.DateFormat(calendar.getTime(), MONTH_FORMATE);
		
		calendar.clear();
	}
	
	/**
	 * 尋找最後一次更改的fileID
	 */
	private void setLastFileID(){
		logger.info("setLastFileID...");
		sql="SELECT MAX(A.LAST_FILEID) id FROM HUR_CURRENT A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			
			while(rs.next()){
				lastfileID=rs.getInt("id");
			}
			logger.info("Last process file ID :"+lastfileID);
			
			st.close();
			rs.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setLastFileID occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	/**
	 * 取出 HUR_CURRENTE table資料
	 * 建立成
	 * Map 
	 * Key:MONTH,Value:Map(IMSI,Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_VOLUME)))
	 */
	private void setCurrentMap(){
		logger.info("setCurrentMap...");
		try {
			//設定HUR_CURRENT計費，抓出這個月與下個月
			sql=
					"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.LAST_DATA_TIME,A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN (?,?) ";
			
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, sYearmonth);
			pst.setString(2, sYearmonth2);
			
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = pst.executeQuery();
			logger.debug("Set current map...");
			
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
				
				
				//保留舊資料
				oldChargeMap.put(imsi, rs.getDouble("CHARGE"));
				
			}
			pst.close();
			rs.close();

		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setCurrentMap occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	
	/**
	 * 取出 HUR_CURRENTE_DAY table資料
	 * 建立成
	 * Map 
	 * Key:day , value:Map(IMSI,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME,ALERT)))
	 */
	private void setCurrentMapDay(){
		//設定HUR_CURRENT_DAY計費,目前不做刪除動作，之後考慮是否留2個月資料
		logger.info("setCurrentMapDay...");
		try {
			sql=
					"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.LAST_DATA_TIME,A.VOLUME,A.UPDATE_DATE,A.CREATE_DATE,A.MCCMNC,A.DAY,A.ALERT "
					+ "FROM HUR_CURRENT_DAY A ";
			logger.debug("Execute SQL : "+sql);
			Statement st = conn.createStatement();
			logger.debug("Set current day map...");
			ResultSet rs2 =st.executeQuery(sql);
			
			while(rs2.next()){
				Map<String,Object> map=new HashMap<String,Object>();
				Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();			
				Map<String,Map<String,Map<String,Object>>> map3=new HashMap<String,Map<String,Map<String,Object>>>();
				
				String mccmnc=rs2.getString("MCCMNC");
				if(mccmnc==null || "".equals(mccmnc)) mccmnc=DEFAULT_MCCMNC;
				String imsi =rs2.getString("IMSI");
				String day =rs2.getString("DAY");
				//System.out.println("imsi : "+imsi);
				
				if(currentDayMap.containsKey(day)){
					map3=currentDayMap.get(day);
					if(map.containsKey(imsi)){
						map2=map3.get(imsi);
					}
				}
						
				map.put("LAST_FILEID", rs2.getInt("LAST_FILEID"));
				map.put("LAST_DATA_TIME", (rs2.getDate("LAST_DATA_TIME")!=null?rs2.getDate("LAST_DATA_TIME"):new Date()));
				map.put("CHARGE", rs2.getDouble("CHARGE"));
				map.put("VOLUME", rs2.getDouble("VOLUME"));
				map.put("ALERT", rs2.getString("ALERT"));

				map2.put(mccmnc, map);
				
				map3.put(imsi, map2);
				currentDayMap.put(day,map3);
			}
			st.close();
			rs2.close();
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setCurrentMapDay occur SQLException error!");
			errorMsg=e.getMessage();
		}
		
		
	}
	/**
	 * 取出 HUR_DATA_RATE
	 * 建立成MAP Key:PRICEPLANID,Value:Map(MCCMNC,MAP(CURRENCY,CHARGEUNIT,RATE))
	 */
	private void setDataRate(){
		logger.info("setDataRate...");
		sql=
				"SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY,A.PRICEPLANID,A.DAYCAP "
				+ "FROM HUR_DATA_RATE A ";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);

			logger.debug("Set dataRate map...");
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
			
			st.close();
			rs.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setDataRate occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	/**
	 * 取出HUR_THRESHOLD
	 * 建立MAP Key:IMSI,VALUE:THRESHOLD
	 */
	private void setThreshold(){
		logger.info("setThreshold...");
		sql=
				"SELECT A.IMSI,A.THRESHOLD "
				+"FROM HUR_GPRS_THRESHOLD A ";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);

			logger.debug("Set dataRate map...");
			while(rs.next()){
				thresholdMap.put(rs.getString("IMSI"), rs.getDouble("THRESHOLD"));
			}
			
			st.close();
			rs.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setThreshold occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	/**
	 * 設定 IMSI 至 VLN 的對應表
	 * Map Key:IMSI,VALUE:VLN
	 */
	private void setIMSItoVLN(){
		logger.info("setIMSItoVLN...");
		sql=
				"SELECT A.VLR_NUMBER,A.IMSI "
				+ "FROM UTCN.BASICPROFILE A "
				+ "WHERE A.VLR_NUMBER is not null ";
		
		try {
			Statement st = conn2.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			
			while(rs.next()){
				IMSItoVLN.put(rs.getString("IMSI"), rs.getString("VLR_NUMBER"));
			}
			st.close();
			rs.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setIMSItoVLN occur SQLException error!");
			errorMsg=e.getMessage();
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
		logger.info("setVLNtoTADIG...");
		sql=
				"SELECT B.REALMNAME TADIG, A.CHARGEAREACODE VLR "
				+ "FROM CHARGEAREACONFIG A, REALM B "
				+ "WHERE A.AREAREFERENCE=B.AREAREFERENCE";
		
		try {
			Statement st = conn2.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			
			while(rs.next()){
				VLNtoTADIG.put(rs.getString("VLR"), rs.getString("TADIG"));
			}
			st.close();
			rs.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setVLNtoTADIG occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	/**
	 * 
	 * 建立 TADIG至MCCMNC對應表
	 * 
	 * MAP KEY：TADIG,VALUE:MCCMNC
	 */
	private void setTADIGtoMCCMNC(){
		logger.info("setTADIGtoMCCMNC...");
		sql=
				"SELECT A.TADIG,A.MCCMNC "
				+ "FROM HUR_MCCMNC A ";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			
			while(rs.next()){
				TADIGtoMCCMNC.put(rs.getString("TADIG"), rs.getString("MCCMNC"));
			}
			st.close();
			rs.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setTADIGtoMCCMNC occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	
	
	/**
	 * 
	 * 建立 國碼對客服電話，國家 對應表
	 * 
	 * MAP KEY：CODE,VALUE:(PHONE,NAME)
	 */
	private void setCostomerNumber(){
		logger.info("setCostomerNumber...");
		sql=
				"SELECT A.CODE,A.PHONE,A.NAME "
				+ "FROM HUR_CUSTOMER_SERVICE_PHONE A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> map = new HashMap<String,String>();
				map.put("PHONE", rs.getString("PHONE"));
				map.put("NAME", rs.getString("NAME"));
				codeMap.put(rs.getString("CODE"), map);
			}
			st.close();
			rs.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setTADIGtoMCCMNC occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	/**
	 * 取得資料筆數
	 * @return
	 */
	private int dataCount(){
		logger.info("dataCount...");
		sql="SELECT COUNT(1) count  FROM HUR_DATA_USAGE A WHERE A.FILEID>= ? ";
		int count=0;
		//找出總量
		PreparedStatement pst;
		try {
			pst = conn.prepareStatement(sql);
			pst.setInt(1, lastfileID+1);
			
			ResultSet rs = pst.executeQuery();
			logger.debug("Execute SQL : "+sql);
			
			while(rs.next()){
				count=rs.getInt("count");
			}
			logger.info("usage count : " +count);
			pst.close();
			rs.close();
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At dataCount occur SQLException error!");
			errorMsg=e.getMessage();
		}
		return count;
	}
	
	/**
	 * 取得預設計費比率（總費率平均），對MCCNOC有卻無法對應資料計費
	 * @return
	 */
	private double defaultRate(){
		logger.info("defaultRate...");
		double avg=0;
		sql=
				"SELECT AVG(CASE WHEN A.CURRENCY = 'HKD' THEN A.RATE/A.CHARGEUNIT*"+exchangeRate+" ELSE  A.RATE/A.CHARGEUNIT END)  AVG "
				+ "FROM HUR_DATA_RATE A ";
		//找出最貴價格
		try {
			Statement st = conn.createStatement();
			
			ResultSet rs = st.executeQuery(sql);
			logger.debug("Execute SQL : "+sql);
			
			while(rs.next()){
				avg=rs.getDouble("AVG");
			}
			logger.info("AVG Rate : " +avg+" TWD ");
			st.close();
			rs.close();
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At defaultRate occur SQLException error!");
			errorMsg=e.getMessage();
		}
		
		return avg;
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
	 * 開始批價
	 */
	private void charge(){
		logger.info("charge...");
		
		int count=0;
		double defaultRate=0;
		try {
			count=dataCount();
			defaultRate=defaultRate();
			
			sql=
					"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME "
					+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME "
					+ "FROM HUR_DATA_USAGE A WHERE A.FILEID>= ? AND ROWNUM <= ?  ORDER BY A.USAGEID,A.FILEID) "
					+ "MINUS "
					+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME "
					+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME "
					+ "FROM HUR_DATA_USAGE A WHERE A.FILEID>= ? AND ROWNUM <= ?  ORDER BY A.USAGEID,A.FILEID)";
			
			logger.debug("Execute SQL : "+sql);
			
			//批次Query 避免ram空間不足
			for(int i=1;(i-1)*dataThreshold+1<=count ;i++){
				logger.debug("Round "+i+" Procsess ...");
				PreparedStatement pst = conn.prepareStatement(sql);
				pst.setInt(1, lastfileID+1);
				pst.setInt(2, i*dataThreshold);
				pst.setInt(3, lastfileID+1);
				pst.setInt(4, (i-1)*dataThreshold);
			
				ResultSet rs = pst.executeQuery();

				while(rs.next()){					
					String imsi= rs.getString("IMSI");
					String mccmnc=rs.getString("MCCMNC");
					String usageId=rs.getString("USAGEID");
					Double volume=rs.getDouble("DATAVOLUME");
					Date callTime=rs.getDate("CALLTIME");
					Integer fileID=rs.getInt("FILEID");
					String cDay=tool.DateFormat(callTime, DAY_FORMATE);
					Double charge=0D;
					Double oldCharge=0D;
					Double oldvolume=0D;
					Double dayCap=null;
					String alert="0";					
					
					String pricplanID=null;
					if(msisdnMap.containsKey(imsi))
						pricplanID=msisdnMap.get(imsi).get("PRICEPLANID");
					
					if(dataRate.containsKey(pricplanID)){
						//假如沒有mccmnc給予預設字樣，必須要有
						if(mccmnc==null || "".equals(mccmnc)){
							mccmnc=searchMccmncByIMSI(imsi);
						}
					}else{
						logger.debug("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
						sendMail("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
					}
					
					//還是找不到，給予預設，必須有
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc= DEFAULT_MCCMNC;
					}
					
					//判斷是否可以找到對應的費率表，並計算此筆CDR的價格(charge)
					if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
							dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
						
						double ec=1;
						if("HKD".equalsIgnoreCase((String) dataRate.get(pricplanID).get(mccmnc).get("CURRENCY")))
							ec=exchangeRate;
						charge=volume*kByte*(Double)dataRate.get(pricplanID).get(mccmnc).get("RATE")*ec;
						dayCap=(Double)dataRate.get(pricplanID).get(mccmnc).get("DAYCAP");
						
					}else{
						//沒有PRICEPLANID(月租方案)，MCCMNC，無法判斷區域業者，作法：統計流量，
						//沒有對應的PRICEPLANID(月租方案)，MCCMNC，無法判斷區域業者
						//以最大費率計費
						sendMail("IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");
						charge=volume*kByte*defaultRate;
					}
					
					//格式化至小數點後四位
					charge=tool.FormatDouble(charge, "0.0000");
					
					//將此筆CDR結果記錄，稍後回寫到USAGE TABLE
					cdrChargeMap.put(usageId, charge);

					//察看是否有以存在的資料，有的話取出做累加
					
					Map<String,Map<String,Map<String,Object>>> map=new HashMap<String,Map<String,Map<String,Object>>>();
					Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();
					Map<String,Object> map3=new HashMap<String,Object>();
					
					Map<String,Set<String>> smd= new HashMap<String,Set<String>>();
					Set<String> sed=new HashSet<String>();
					
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
						if(updateMapD.containsKey(cDay)){
							smd= updateMapD.get(cDay);
							if(smd.containsKey(imsi)){
								sed=smd.get(imsi);
							}
						}
						sed.add(mccmnc);
						smd.put(imsi, sed);
						updateMapD.put(cDay, smd);
					}else{
						if(insertMapD.containsKey(cDay)){
							smd= insertMapD.get(cDay);
							if(smd.containsKey(imsi)){
								sed=smd.get(imsi);
							}
						}
						sed.add(mccmnc);
						smd.put(imsi, sed);
						insertMapD.put(cDay, smd);
					}
					
					//如果有計費上線，限制最大值
					if(dayCap==null || dayCap==0) dayCap= DEFAULT_DAYCAP;
					if(dayCap!=null && charge>dayCap) charge=dayCap;
					
					//將結果記錄到currentDayMap
					map3.put("CHARGE", charge);
					map3.put("LAST_FILEID",fileID);
					map3.put("LAST_DATA_TIME",callTime);
					map3.put("VOLUME",volume+oldvolume);
					map3.put("ALERT",alert);
					map2.put(mccmnc, map3);
					map.put(imsi, map2);
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
					
					Set<String> se=new HashSet<String>();
					if(map5.containsKey(imsi)){
						map4=map5.get(imsi);
						
						preCharge=(Double)map4.get("CHARGE")-oldCharge;
						smsTimes=(Integer) map4.get("SMS_TIMES");
						suspend=(String) map4.get("EVER_SUSPEND");
						volume=(Double)map4.get("VOLUME")+volume;
						lastAlertThreshold=(Double) map4.get("LAST_ALERN_THRESHOLD");
						volumeAlert=(Double) map4.get("LAST_ALERN_VOLUME");
						if(updateMap.containsKey(cMonth)){
							se=updateMap.get(cMonth);
						}
						se.add(imsi);
						updateMap.put(cMonth, se);

					}else{
						if(insertMap.containsKey(cMonth)){
							se=insertMap.get(cMonth);
						}
						se.add(imsi);
						insertMap.put(cMonth, se);
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
				pst.close();
				rs.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At charge occur SQLException error!");
			errorMsg=e.getMessage();
		}	
	}
	
	/***
	 * 回寫CDR的CHARGE
	 */
	private void updateCdr(){
		String sql=
				"UPDATE HUR_DATA_USAGE A "
				+ "SET A.CHARGE=? "
				+ "WHERE A.USAGEID=? ";
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			int count=0;
			@SuppressWarnings("unused")
			int [] result;
			for(String s: cdrChargeMap.keySet()){
				pst.setDouble(1, cdrChargeMap.get(s));
				pst.setString(2, s);
				pst.addBatch();
				count++;

				if(count==dataThreshold){
					logger.info("Execute updateCdr Batch");
					result=pst.executeBatch();
					count=0;
				}
			}
			if(count!=0){
				logger.info("Execute updateCdr Batch");
				result=pst.executeBatch();
			}
			//conn.commit();
			pst.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Error at updateCdr : "+e.getMessage());
			//sendMail
			sendMail("At updateCdr occur SQLException error!");
			errorMsg=e.getMessage();
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
			e.printStackTrace();
			logger.error("Error Occur at setAutoCommit !");
			//sendMail
			sendMail("At cancelAutoCommit occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	private void IniProgram(){
		// 初始化log
		// iniLog4j();
		loadProperties();
		
		// 進行DB連線
		connectDB();
		connectDB2();
		
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
			e.printStackTrace();
			logger.error("Error at connDB : "+e.getMessage());
			//sendMail
			sendMail("At connDB occur ClassNotFoundException error!");
			errorMsg=e.getMessage();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error at connDB : "+e.getMessage());
			//sendMail
			sendMail("At connDB occur SQLException error!");
			errorMsg=e.getMessage();
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
			e.printStackTrace();
			logger.error("Error at connDB2 : "+e.getMessage());
			//sendMail
			sendMail("At connDB2 occur ClassNotFoundException error!");
			errorMsg=e.getMessage();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error at connDB2 : "+e.getMessage());
			//sendMail
			sendMail("At connDB2 occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}

	/**
	 * 計算完畢後寫回資料庫-更新
	 */

	private void updateCurrentMap(){
		logger.info("updateCurrentMap...");

		@SuppressWarnings("unused")
		int[] result;
		int count=0;
		
		sql=
				"UPDATE HUR_CURRENT A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.SMS_TIMES=?,A.LAST_DATA_TIME=?,A.VOLUME=?,A.EVER_SUSPEND=?,A.LAST_ALERN_THRESHOLD=?,A.LAST_ALERN_VOLUME=?,A.UPDATE_DATE=SYSDATE "
				+ "WHERE A.MONTH=? AND A.IMSI=? ";
		
		logger.info("Execute SQL :"+sql);
		
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String mon : updateMap.keySet()){
				for(String imsi : updateMap.get(mon)){
					
						pst.setDouble(1,(Double) currentMap.get(mon).get(imsi).get("CHARGE"));
						pst.setInt(2, (Integer) currentMap.get(mon).get(imsi).get("LAST_FILEID"));
						pst.setInt(3, (Integer) currentMap.get(mon).get(imsi).get("SMS_TIMES"));
						pst.setDate(4, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentMap.get(mon).get(imsi).get("LAST_DATA_TIME")));
						pst.setDouble(5,(Double) currentMap.get(mon).get(imsi).get("VOLUME"));
						pst.setString(6,(String) currentMap.get(mon).get(imsi).get("EVER_SUSPEND"));
						pst.setDouble(7,(Double) currentMap.get(mon).get(imsi).get("LAST_ALERN_THRESHOLD"));
						pst.setDouble(8,(Double) currentMap.get(mon).get(imsi).get("LAST_ALERN_VOLUME"));
						pst.setString(9, mon);
						pst.setString(10, imsi);//具有mccmnc
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
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Error at updateCurrentMap : "+e.getMessage());
			//sendMail
			sendMail("At updateCurrentMap occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	private void updateCurrentMapDay(){
		logger.info("updateCurrentMapDay...");
		@SuppressWarnings("unused")
		int[] result;
		int count=0;
		
		sql=
				"UPDATE HUR_CURRENT_DAY A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.LAST_DATA_TIME=?,A.VOLUME=?,A.ALERT=?,A.UPDATE_DATE=SYSDATE "
				+ "WHERE A.DAY=? AND A.IMSI=? AND A.MCCMNC=? ";
		
		logger.info("Execute SQL :"+sql);
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String day : updateMapD.keySet()){
				for(String imsi : updateMapD.get(day).keySet()){
					for(String mccmnc : updateMapD.get(day).get(imsi)){
						
						if(currentDayMap.get(day).get(imsi).get(mccmnc)==null){
							System.out.println(day+" for imsi:"+imsi+" mccmnc:"+mccmnc+" in updateMapD can't get in current day map !");
							continue;
						}
						
						
							pst.setDouble(1,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE"));
							pst.setInt(2, (Integer) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID"));
							pst.setDate(3, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME")));
							pst.setDouble(4,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME"));
							pst.setString(5,(String) currentDayMap.get(day).get(imsi).get(mccmnc).get("ALERT"));
							pst.setString(6, day);
							pst.setString(7, imsi);
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
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Error at updateCurrentMapU : "+e.getMessage());
			//sendMail
			sendMail("At updateCurrentMapU occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	/**
	 * 計算完畢後寫回資料庫-新增
	 */
	
	private void insertCurrentMap(){
		logger.info("insertCurrentMap...");
		@SuppressWarnings("unused")
		int[] result;
		int count=0;
		
		sql=
				"INSERT INTO HUR_CURRENT "
				+ "(IMSI,CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_THRESHOLD,LAST_ALERN_VOLUME,MONTH,CREATE_DATE) "
				+ "VALUES(?,?,?,?,?,?,?,?,?,?,SYSDATE)";
		
		logger.info("Execute SQL :"+sql);
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			for(String mon : insertMap.keySet()){
				for(String imsi : insertMap.get(mon)){
					
						pst.setString(1, imsi);		
						pst.setDouble(2,(Double) currentMap.get(mon).get(imsi).get("CHARGE"));
						pst.setInt(3,(Integer) currentMap.get(mon).get(imsi).get("LAST_FILEID"));
						pst.setInt(4, (Integer) currentMap.get(mon).get(imsi).get("SMS_TIMES"));
						pst.setDate(5, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentMap.get(mon).get(imsi).get("LAST_DATA_TIME")));
						pst.setDouble(6,(Double) currentMap.get(mon).get(imsi).get("VOLUME"));
						pst.setString(7,(String) currentMap.get(mon).get(imsi).get("EVER_SUSPEND"));
						pst.setDouble(8,(Double) currentMap.get(mon).get(imsi).get("LAST_ALERN_THRESHOLD"));
						pst.setDouble(9,(Double) currentMap.get(mon).get(imsi).get("LAST_ALERN_VOLUME"));
						pst.setString(10, mon);
						pst.addBatch();
						count++;
					if(count==dataThreshold){
						logger.info("Execute insertCurrentMap Batch");
						result=pst.executeBatch();
						count=0;
					}
				}
			}
			if(count!=0){
				logger.info("Execute insertCurrentMap Batch");
				result=pst.executeBatch();
			}
			pst.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Error at insertCurrentMap : "+e.getMessage());
			//sendMail
			sendMail("At insertCurrentMap occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	private void insertCurrentMapDay(){
		logger.info("insertCurrentMapDay...");
		@SuppressWarnings("unused")
		int[] result;
		int count=0;
		
		sql=
				"INSERT INTO HUR_CURRENT_DAY "
				+ "(IMSI,CHARGE,LAST_FILEID,LAST_DATA_TIME,VOLUME,CREATE_DATE,MCCMNC,DAY,ALERT) "
				+ "VALUES(?,?,?,?,?,SYSDATE,?,?,?)";
		
		logger.info("Execute SQL :"+sql);
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			for(String day : insertMapD.keySet()){
				for(String imsi : insertMapD.get(day).keySet()){
					for(String mccmnc : insertMapD.get(day).get(imsi)){
						
						if(currentDayMap.get(day).get(imsi).get(mccmnc)==null){
							System.out.println(day+" for imsi:"+imsi+" mccmnc:"+mccmnc+" in insertMap can't get in current day map !");
							continue;
						}
						
							pst.setString(1, imsi);
							pst.setDouble(2,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE"));
							pst.setInt(3, (Integer) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID"));
							pst.setDate(4, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME")));
							pst.setDouble(5,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME"));
							pst.setString(6, mccmnc);
							pst.setString(7, day);
							pst.setString(8, (String) currentDayMap.get(day).get(imsi).get(mccmnc).get("ALERT"));
							pst.addBatch();
							count++;
						if(count==dataThreshold){
							result=pst.executeBatch();
							logger.info("Execute insertCurrentMapU Batch");
							count=0;
						}
					}
				}
			}
			
			if(count!=0){
				result=pst.executeBatch();
				logger.info("Execute insertCurrentMapU Batch");
			}
			pst.close();
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Error at insertCurrentMapU : "+e.getMessage());
			//sendMail
			sendMail("At insertCurrentMapU occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}

	
	/**
	 * 依照額度需求發送警示簡訊
	 * 第一次，額度一，訊息一
	 * 
	 */
	private void sendAlertSMS(){
		logger.info("sendAlertSMS...");

		List<Integer> times=new ArrayList<Integer>();
		List<Double> bracket=new ArrayList<Double>();
		List<String> msg=new ArrayList<String>();
		List<String> suspend=new ArrayList<String>();
		
		int smsCount=0;
		String phone=null;
		
		//載入簡訊設定
		
		try {
			sql="SELECT A.ID,A.BRACKET,A.MEGID,A.SUSPEND FROM HUR_SMS_SETTING A ORDER BY ID DESC";	
			
			Statement st;
			st = conn.createStatement();
			logger.debug("Execute SQL:"+sql);
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				times.add(rs.getInt("ID"));
				bracket.add(rs.getDouble("BRACKET"));
				msg.add(rs.getString("MEGID"));
				suspend.add(rs.getString("SUSPEND"));
				logger.info("times:"+times.get(times.size()-1)+",bracket:"+bracket.get(bracket.size()-1)+",msg:"+msg.get(msg.size()-1)+",suspend:"+suspend.get(suspend.size()-1));
			}
			
			rs.close();
			st.close();

		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error("Error at Load SMSSetting : "+e1.getMessage());
			//sendMail
			sendMail("At sendAlertSMS:Load SMSSetting occur SQLException error!");
			errorMsg=e1.getMessage();
		}
		
		
		if(times.size()==0){
			logger.error("No SMS Setting!");
			return;
		}
		
		//載入簡訊內容
		
		Map<String,Map<String,String>> content=new HashMap<String,Map<String,String>>();
		
		try {
			sql=
					"SELECT A.ID,A.COMTENT,A.CHARSET "
					+ "FROM HUR_SMS_COMTENT A ";	
			
			Statement st;
			st = conn.createStatement();
			logger.debug("Execute SQL:"+sql);
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("COMTENT", rs.getString("COMTENT"));
				map.put("CHARSET", rs.getString("CHARSET"));
				content.put(rs.getString("ID"), map);
			}

		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error("Error at Load SMSContent : "+e1.getMessage());
			//sendMail
			sendMail("At sendAlertSMS:Load SMSContent occur SQLException error!");
			errorMsg=e1.getMessage();
		}
		
		if(content.size()==0){
			logger.error("No SMS content!");
			return;
		}
		
		
		//開始檢查是否發送警示簡訊
		
		sql="INSERT INTO HUR_SMS_LOG"
				+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
				+ "VALUES(DVRS_SMS_ID.NEXTVAL,?,?,?,?,SYSDATE)";
		
		//月金額警示*************************************
		//沒有當月資料，不檢查
		if(currentMap.containsKey(sYearmonth)){
			smsCount=0;
			try {
				PreparedStatement pst = conn.prepareStatement(sql);
				logger.info("Execute SQL :"+sql);
				//檢查這個月的資料作警示通知
				for(String imsi: currentMap.get(sYearmonth).keySet()){
					
					//如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
					if(msisdnMap.containsKey(imsi))
						phone=(String) msisdnMap.get(imsi).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error!<br>\n "
								+ "The IMSI:"+imsi+" can't find msisdn to send! ");
						logger.debug("The IMSI:"+imsi+" can't find msisdn to send! ");
						continue;
					}
					
					//查詢所在國家的客服電話
					String cPhone = null;
					String nMccmnc=searchMccmncByIMSI(imsi);
					Map<String,String> map=null;
					
					if(nMccmnc!=null && !"".equals(nMccmnc))
						map = codeMap.get(nMccmnc.substring(0,3));
					if(map!=null)
						cPhone=map.get("PHONE");
					
					//logger.info("For imsi="+imsi+" get phone number="+phone);
					String res="";
					
					Double charge=(Double) currentMap.get(sYearmonth).get(imsi).get("CHARGE");
					Double oldCharge=(Double) oldChargeMap.get(imsi);
					if(oldCharge==null) oldCharge=0D;
					Double differenceCharge=charge-oldCharge;
					int smsTimes=(Integer) currentMap.get(sYearmonth).get(imsi).get("SMS_TIMES");
					String everSuspend =(String) currentMap.get(sYearmonth).get(imsi).get("EVER_SUSPEND");
					Double lastAlernThreshold=(Double) currentMap.get(sYearmonth).get(imsi).get("LAST_ALERN_THRESHOLD");
					boolean isCustomized=false;
					Double threshold=thresholdMap.get(imsi);
					
					
					if(threshold==null || threshold==0D){
						threshold=DEFAULT_THRESHOLD;
					}else{
						isCustomized=true;
					}
					if(lastAlernThreshold==null)
						lastAlernThreshold=0D;
					
					int i=0;
					boolean sendSMS=false;
					//檢查月用量
					for(;i<times.size();i++){
						if(((charge>=bracket.get(i)*threshold))&&lastAlernThreshold<bracket.get(i)*threshold){
							sendSMS=true;
							break;
						}
					}	
					
					//檢查預測用量，如果之前判斷不用發簡訊，或是非發最上限簡訊
					if(!sendSMS||(sendSMS && i!=0)){
						if(charge+differenceCharge>=bracket.get(0)*threshold&&lastAlernThreshold<bracket.get(0)*threshold){
							logger.info("For "+imsi+" ,System forecast the next hour will over charge limit");
							sendSMS=true;
							i=0;
						}
					}

					//寄送簡訊
					if(sendSMS){
						for(String s:msg.get(i).split(",")){
							if(s!=null){
								//寄送簡訊
								lastAlernThreshold=bracket.get(i)*threshold;
								smsTimes++;
								logger.info("For "+imsi+" send "+smsTimes+"th message:"+msg.get(i));
								String cont =processMag(content.get(s).get("COMTENT"),bracket.get(i)*threshold,cPhone);
								//TODO
								//WSDL方式呼叫 WebServer
								//result=tool.callWSDLServer(setSMSXmlParam(cont,phone));
								//WSDL方式呼叫 WebServer
								res=setSMSPostParam(cont,phone);
								currentMap.get(sYearmonth).get(imsi).put("LAST_ALERN_THRESHOLD", lastAlernThreshold);
								logger.debug("send message result : "+res);						
								currentMap.get(sYearmonth).get(imsi).put("SMS_TIMES", smsTimes);
								smsCount++;
								//中斷GPRS服務
								//20141113 新增客制定上限不執行斷網
								if("1".equals(suspend.get(i))&&"0".equals(everSuspend)&&!isCustomized){
									logger.debug("Suspend GPRS ... ");		
									suspend(imsi,phone);
									currentMap.get(sYearmonth).get(imsi).put("EVER_SUSPEND", "1");
								}
								//寫入資料庫
								pst.setString(1, phone);
								pst.setString(2, msg.get(i));
								pst.setDate(3,tool.convertJaveUtilDate_To_JavaSqlDate(new Date()));
								pst.setString(4, res);
								pst.addBatch();
								
								//HUR_Current 需更新
								//如果是新資料，insertList會已有資料，直接註記update
								if(!updateMap.containsKey(sYearmonth)){
									Set<String> se=new HashSet<String>();
									se.add(imsi);
									updateMap.put(sYearmonth, se);
								}else{
									updateMap.get(sYearmonth).add(imsi);
								}
							}
							
						}
					}
				}
				logger.debug("Total send month alert"+smsCount+" ...");
				logger.debug("Log to table...executeBatch");
				pst.executeBatch();
				pst.close();

			} catch (SQLException e) {
				e.printStackTrace();
				logger.error("Error at sendMonthAlertSMS : "+e.getMessage());
				//sendMail
				sendMail("At sendMonthAlertSMS occur SQLException error!");
				errorMsg=e.getMessage();
			}catch(Exception e){
				e.printStackTrace();
				logger.error("Error at sendMonthAlertSMS : "+e.getMessage());
				//sendMail
				sendMail("At sendMonthAlertSMS occur Exception error!");
				errorMsg=e.getMessage();
			}
		}
		
		
		//實做日警示部分，有今日資料才警示*************************************
		if(currentDayMap.containsKey(sYearmonthday)){
			smsCount=0;
			try {
				PreparedStatement pst = conn.prepareStatement(sql);
				for(String imsi:currentDayMap.get(sYearmonthday).keySet()){
					//檢查門號是否存在，如果沒有門號資料，因為無法發送簡訊，寄送警告mail後跳過
					if(msisdnMap.containsKey(imsi))
						phone=(String) msisdnMap.get(imsi).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error!<br>\n "
								+ "The IMSI:"+imsi+" can't find msisdn to send! ");
						logger.debug("The IMSI:"+imsi+" can't find msisdn to send! ");
						continue;
					}
					
					//查詢所在國家的客服電話
					String cPhone = null;
					String nMccmnc=searchMccmncByIMSI(imsi);
					Map<String,String> map=null;
					
					if(nMccmnc!=null && !"".equals(nMccmnc))
						map = codeMap.get(nMccmnc.substring(0,3));
					if(map!=null)
						cPhone=map.get("PHONE");
					
					Double daycharge=0D;
					String alerted =null;
					
					//累計
					for(String mccmnc : currentDayMap.get(sYearmonthday).get(imsi).keySet()){
						daycharge=daycharge+(Double)currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).get("CHARGE");
						alerted=(String) currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).get("ALERT");
					}
					
					if(daycharge>=DEFAULT_DAY_THRESHOLD && "0".equalsIgnoreCase(alerted)){
						//處理字串，日警示內容ID設定為99
						String cont =processMag(content.get("99").get("COMTENT"),DEFAULT_DAY_THRESHOLD,cPhone);
						//發送簡訊
						String res = setSMSPostParam(cont,phone);
						logger.debug("send message result : "+res);	
						smsCount++;
						//回寫註記，因為有區分Mccmnc，全部紀錄避免之後取不到
						for(String mccmnc : currentDayMap.get(sYearmonthday).get(imsi).keySet()){
							currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).put("ALERT", "1");
							
							//HUR_Current_day 需更新，如果是新資料，insertList會已有資料，直接註記update
							Map<String, Set<String>> smd=new HashMap<String, Set<String>>();
							Set<String> sed = new HashSet<String>();
							if(updateMapD.containsKey(sYearmonthday)){
								smd= updateMapD.get(sYearmonthday);
								if(smd.containsKey(imsi)){
									sed=smd.get(imsi);
								}
							}
							sed.add(mccmnc);
							smd.put(imsi, sed);
							updateMapD.put(sYearmonthday, smd);
						}
						
						//寫入資料庫
						pst.setString(1, phone);
						pst.setString(2, "99");
						pst.setDate(3,tool.convertJaveUtilDate_To_JavaSqlDate(new Date()));
						pst.setString(4, res);
						pst.addBatch();
					}
				}
				logger.debug("Total send day alert"+smsCount+" ...");
				logger.debug("Log to table...executeBatch");
				pst.executeBatch();
				pst.close();
			
			} catch (SQLException e1) {
				e1.printStackTrace();
				logger.error("Error at sendDayAlertSMS : "+e1.getMessage());
				//sendMail
				sendMail("At sendDayAlertSMS occur SQLException error!");
				errorMsg=e1.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("Error at sendDayAlertSMS : "+e.getMessage());
				//sendMail
				sendMail("At sendDayAlertSMS occur SQLException error!");
				errorMsg=e.getMessage();
			}	
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
					String priceplanid = msisdnMap.get(imsi).get("PRICEPLANID");
					String subsidiaryid = msisdnMap.get(imsi).get("SUBSIDIARYID");
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
		
		try {
			smsCount=0;
			PreparedStatement pst = conn.prepareStatement(sql);
			for(String imsi:tempMap.keySet()){
				Double volume=tempMap.get(imsi);
				Double everAlertVolume = (Double) currentMap.get(sYearmonth).get(imsi).get("LAST_ALERN_VOLUME");
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
					String priceplanid = msisdnMap.get(imsi).get("PRICEPLANID");
					//是否為Data only 方案
					if(checkedPriceplanid2.contains(priceplanid)){
						//以設定通知號通知
						phone=msisdnMap.get(imsi).get("NCODE");
					}else{
						//以門號通知
						phone=msisdnMap.get(imsi).get("MSISDN");
					}
					//確認號碼
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error!<br>\n "
								+ "The IMSI:"+imsi+" can't find msisdn to send! ");
						logger.debug("The IMSI:"+imsi+" can't find msisdn to send! ");
						continue;
					}
					
					//查詢所在國家的客服電話
					String cPhone = null;
					String nMccmnc=searchMccmncByIMSI(imsi);
					Map<String,String> map=null;
					
					if(nMccmnc!=null && !"".equals(nMccmnc))
						map = codeMap.get(nMccmnc.substring(0,3));
					if(map!=null)
						cPhone=map.get("PHONE");
					
					//發送簡訊
					if(msgid==100)
						logger.info("For "+imsi+" send 1.5GB decrease speed  message !");
					if(msgid==102)
						logger.info("For "+imsi+" send 2.0GB decrease speed  message !");
					
					//中文
					//處理字串
					String cont =processMag(content.get(msgid.toString()).get("COMTENT"),null,cPhone);
					//發送簡訊
					String res = setSMSPostParam(cont,phone);
					logger.debug("send chinese message result : "+res);	
					smsCount++;
					
					//英文
					msgid+=1;
					//處理字串
					cont =processMag(content.get(msgid.toString()).get("COMTENT"),null,cPhone);
					//發送簡訊
					res = setSMSPostParam(cont,phone);
					logger.debug("send english message result : "+res);	
					smsCount++;
					
					//寫入資料庫
					pst.setString(1, phone);
					pst.setString(2, msgid.toString());
					pst.setDate(3,tool.convertJaveUtilDate_To_JavaSqlDate(new Date()));
					pst.setString(4, res);
					pst.addBatch();
					
					//更新CurrentMap
					currentMap.get(sYearmonth).get(imsi).put("LAST_ALERN_VOLUME",volume);
					
					//如果是新資料，insertList會已有資料，直接註記update
					if(!updateMap.containsKey(sYearmonth)){
						Set<String> se=new HashSet<String>();
						se.add(imsi);
						updateMap.put(sYearmonth, se);
					}else{
						updateMap.get(sYearmonth).add(imsi);
					}
				}
				
			}
			logger.debug("Total send volume alert SMS "+smsCount+" ...");
			pst.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error at sendDayAlertSMS : "+e.getMessage());
			//sendMail
			sendMail("At sendDayAlertSMS occur SQLException error!");
			errorMsg=e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error at sendDayAlertSMS : "+e.getMessage());
			//sendMail
			sendMail("At sendDayAlertSMS occur SQLException error!");
			errorMsg=e.getMessage();
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
		msg=msg.replace("{{customerService}}", "+"+cPhone);
		
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
			e.printStackTrace();
			logger.error("Error at setMsisdnMap : "+e.getMessage());
			//sendMail
			sendMail("At setMsisdnMap occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	private void suspend(String imsi,String msisdn){
		suspendGPRS sus=new suspendGPRS(conn,conn2,logger);
		try {
			sus.ReqStatus_17_Act(imsi, msisdn);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error at suspend : "+e.getMessage());
			//sendMail
			sendMail("At suspend occur SQLException error!");
			errorMsg=e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error at suspend : "+e.getMessage());
			//sendMail
			sendMail("At suspend occur IOException error!");
			errorMsg=e.getMessage();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("Error at suspend : "+e.getMessage());
			//sendMail
			sendMail("At suspend occur ClassNotFoundException error!");
			errorMsg=e.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error at suspend : "+e.getMessage());
			//sendMail
			sendMail("At suspend occur Exception error!");
			errorMsg=e.getMessage();
		}
	}
	
	private void sendMail(String content){
		mailReceiver=props.getProperty("mail.Receiver");
		mailSubject="DVRS Warnning Mail";
		mailContent="Error :"+content+"<br>\n"
				+ "Error occurr time: "+tool.DateFormat()+"<br>\n"
				+ "SQL : "+sql+"<br>\n"
				+ "Error Msg : "+errorMsg;

		try {
			tool.sendMail(logger, props, mailSender, mailReceiver, mailSubject, mailContent);
		} catch (AddressException e) {
			e.printStackTrace();
			logger.error("Error at sendMail : "+e.getMessage());
			//sendMail
			//sendMail("At sendMail occur AddressException error!");
			//errorMsg=e.getMessage();
		} catch (MessagingException e) {
			e.printStackTrace();
			logger.error("Error at sendMail : "+e.getMessage());
			//sendMail
			//sendMail("At sendMail occur MessagingException error!");
			//errorMsg=e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error at sendMail : "+e.getMessage());
			//sendMail
			//sendMail("At sendMail occur IOException error!");
			//errorMsg=e.getMessage();
		}
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
	
	private void showIMSItoVLN(){
		for(String imsi : IMSItoVLN.keySet()){
			System.out.print("IMSI"+" : "+imsi);
			System.out.print(", VLN"+" : "+IMSItoVLN.get(imsi));
			System.out.println();
		}
	}
	
	private void showVLNtoTADIG(){
		for(String vln : VLNtoTADIG.keySet()){
			System.out.print("VLN"+" : "+vln);
			System.out.print(", TADIG"+" : "+VLNtoTADIG.get(vln));
			System.out.println();
		}
	}
	private void showTADIGtoMCCMNC(){
		for(String tadig : TADIGtoMCCMNC.keySet()){
			System.out.print("TADIG"+" : "+tadig);
			System.out.print(", MCCMNC"+" : "+TADIGtoMCCMNC.get(tadig));
			System.out.println();
		}
	}
	private void showThresHold(){
		for(String imsi : thresholdMap.keySet()){
			System.out.print("IMSI"+" : "+imsi);
			System.out.print(", threshold"+" : "+thresholdMap.get(imsi));
			System.out.println();
		}
	}
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
	
	//TODO
		private void process() {
			// 程式開始時間
			long startTime;
			// 程式結束時間
			long endTime;
			// 副程式開始時間
			long subStartTime;

			IniProgram();
			
			logger.info("RFP Program Start! "+new Date());
			
			if (conn != null && conn2!=null) {
				
				logger.debug("connect success!");
				
				startTime = System.currentTimeMillis();
				
				//取消自動Commit
				cancelAutoCommit();
				
				//設定日期
				setDayDate();
				
				//取得最後更新的FileID
				subStartTime = System.currentTimeMillis();
				setLastFileID();
				logger.info("setLastFileID execute time :"+(System.currentTimeMillis()-subStartTime));
				
				//取出HUR_CURRENT
				subStartTime = System.currentTimeMillis();
				setCurrentMap();
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
				
				//開始批價 
				subStartTime = System.currentTimeMillis();
				charge();
				logger.info("charge execute time :"+(System.currentTimeMillis()-subStartTime));

				//發送警示簡訊
				subStartTime = System.currentTimeMillis();
				sendAlertSMS();
				logger.info("sendAlertSMS execute time :"+(System.currentTimeMillis()-subStartTime));
				
				//回寫批價結果
				subStartTime = System.currentTimeMillis();
				updateCdr();
				
				//避免資料異常，完全處理完之後在commit
				try {
					insertCurrentMap();
					insertCurrentMapDay();
					updateCurrentMap();
					updateCurrentMapDay();
					conn.commit();
				} catch (SQLException e) {
					e.printStackTrace();
					logger.error("Error at commit : "+e.getMessage());
					//sendMail
					sendMail("At updateCurrentMapU occur SQLException error!");
					errorMsg=e.getMessage();
				}
				logger.info("insert＆update execute time :"+(System.currentTimeMillis()-subStartTime));

				
				// 程式執行完成
				endTime = System.currentTimeMillis();
				logger.info("Program execute time :" + (endTime - startTime));
				show();
				closeConnect();

			} else {
				logger.error("connect is null!");
			}
		}
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		process();
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
									.withIntervalInSeconds(runInterval)
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		/*DVRSmain rf =new DVRSmain();
		rf.process();*/

		regularTimeRun();

	}
	
	
}



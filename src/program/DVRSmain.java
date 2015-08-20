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
	private static Integer dataThreshold=null;//CDR��Ƥ@�妸���X�ƶq
	private static Integer lastfileID=null;//�̫����ɮ׸�
	private static Double exchangeRate=null; //�����x���ײv�A�ȭq��4
	private static Double kByte=null;//RATE���KB�AUSAGE���B
	
	//����]�w
	private String MONTH_FORMATE="yyyyMM";
	//�t�ήɶ��A�~�t�@�p�ɡA�t�θ�ƳB�z�ɶ�����ɮɶ����e�@�p��
	private String sYearmonth="";
	private String sYearmonthday="";
	//�W�Ӥ�
	private String sYearmonth2="";
	private String DAY_FORMATE="yyyyMMdd";	
	
	//�w�]��
	private static int RUN_INTERVAL=3600;//����
	private static String DEFAULT_MCCMNC=null;//�w�]mssmnc
	private static Double DEFAULT_THRESHOLD=null;//�w�]��ĵ�ܶq
	private static Double DEFAULT_DAY_THRESHOLD=null;//�w�]��ĵ�ܶq
	private static Double DEFAULT_DAYCAP=null;
	private static Double DEFAULT_VOLUME_THRESHOLD=null;//�w�]�y�qĵ��(���t)�A1.5GB;
	private static Double DEFAULT_VOLUME_THRESHOLD2=null;//�w�]�y�qĵ��(���t)�A15GB;
	private static String DEFAULT_PHONE=null;
	private static Boolean TEST_MODE=true;
	
	//�h�Ƶ{�B�z
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
	 * ��l��
	 * ���JLog4j Properties
	 */
	@SuppressWarnings("unused")
	private  void iniLog4j(){
		System.out.println("initial Log4g, property at "+DVRSmain.class.getResource(""));
		PropertyConfigurator.configure(DVRSmain.class.getResource("").toString().replace("file:/", "")+"Log4j.properties");
		logger =Logger.getLogger(DVRSmain.class);
	}
	
	/**
	 * ��l��
	 * ���JLog4j Properties
	 * �P�ɸ��J�Ѽ�porps
	 */
	private static  void loadProperties(){
		System.out.println("initial Log4j, property !");
		String path=DVRSmain.class.getResource("").toString().replace("file:", "")+"/Log4j.properties";
		try {
			props.load(new   FileInputStream(path));
			PropertyConfigurator.configure(props);
			logger =Logger.getLogger(DVRSmain.class);
			logger.info("Logger Load Success!");

			DEFAULT_MCCMNC=props.getProperty("progrma.DEFAULT_MCCMNC");//�w�]mssmnc
			DEFAULT_THRESHOLD=(props.getProperty("progrma.DEFAULT_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_THRESHOLD")):5000D);//�w�]��ĵ�ܶq
			DEFAULT_DAY_THRESHOLD=(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_DAY_THRESHOLD")):500D);//�w�]��ĵ�ܶq
			DEFAULT_DAYCAP=(props.getProperty("progrma.DEFAULT_DAYCAP")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_DAYCAP")):500D);
			DEFAULT_VOLUME_THRESHOLD=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD")):1.5*1024*1024D);//�w�]�y�qĵ��(���t)�A1.5GB;
			DEFAULT_VOLUME_THRESHOLD2=(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")!=null?Double.parseDouble(props.getProperty("progrma.DEFAULT_VOLUME_THRESHOLD2")):1.5*1024*1024D);//�w�]�y�qĵ��(���t)�A15GB;
			DEFAULT_PHONE=props.getProperty("progrma.DEFAULT_PHONE");
			RUN_INTERVAL=(props.getProperty("progrma.RUN_INTERVAL")!=null?Integer.parseInt(props.getProperty("progrma.RUN_INTERVAL")):3600);
			TEST_MODE=("true".equalsIgnoreCase(props.getProperty("progrma.TEST_MODE"))?true:false);
			
			dataThreshold=(props.getProperty("progrma.dataThreshold")!=null?Integer.parseInt(props.getProperty("progrma.dataThreshold")):500);//CDR��Ƥ@�妸���X�ƶq
			//lastfileID=(props.getProperty("progrma.lastfileID")!=null?Integer.parseInt(props.getProperty("progrma.lastfileID")):0);//�̫����ɮ׸�
			exchangeRate=(props.getProperty("progrma.exchangeRate")!=null?Double.parseDouble(props.getProperty("progrma.exchangeRate")):4); //�����x���ײv�A�ȭq��4
			kByte=(props.getProperty("progrma.kByte")!=null?Double.parseDouble(props.getProperty("progrma.kByte")):1/1024D);//RATE���KB�AUSAGE���B
			
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
	 * �����s�u
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
	 * �]�w�p�O�g��
	 * ���S�w������Ӥ몺�A�e���[�Wcalendar.setTime(date);�]�wdate���
	 */
	private void setDayDate(){
		logger.info("setMonthDate...");

		//�ثe�ɶ�
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		
		//�t�ήɶ����e�@�p��
		calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY)-1);
		sYearmonth=tool.DateFormat(calendar.getTime(), MONTH_FORMATE);
		sYearmonthday=tool.DateFormat(calendar.getTime(),DAY_FORMATE);
		//�W�Ӥ�ɶ��A�Month�|-30�ѡA�Ĩ���1���V�e�A�T�w���
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)-1);
		sYearmonth2=tool.DateFormat(calendar.getTime(), MONTH_FORMATE);
		
		calendar.clear();
	}
	
	/**
	 * �M��̫�@����諸fileID�A�H�ΥؼгB�z���̲�ID
	 */
	private void setLastFileID(){
		logger.info("setLastFileID...");
		
		
		try {
			sql="SELECT MAX(A.LAST_FILEID) id FROM HUR_CURRENT A";
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
			logger.error("At setLastFileID Got a SQLException", e);
			//send mail
			sendMail("At setLastFileID occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * ���X HUR_CURRENTE table���
	 * �إߦ�
	 * Map 
	 * Key:MONTH,Value:Map(IMSI,Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_VOLUME)))
	 */
	private void setCurrentMap(){
		logger.info("setCurrentMap...");
		try {
			//�]�wHUR_CURRENT�p�O�A��X�o�Ӥ�P�U�Ӥ�
			sql=
					"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.LAST_DATA_TIME,A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
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
				
				
				
				//20141201 add �]�w�s�b���
				Set<String> set =new HashSet<String>();
				if(existMap.containsKey(month)){
					set=existMap.get(month);
				}
				set.add(imsi);
				existMap.put(month, set);
			}
			st.close();
			rs.close();

		} catch (SQLException e) {
			logger.error("At setCurrentMap occur SQLException error", e);
			//send mail
			sendMail("At setCurrentMap occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	/**
	 * �O�d�o�Ӥ��¸�ơA�@���w���O�Ψϥ�
	 */
	private void setoldChargeMap(){
		logger.info("setoldChargeMap...");
		try {
			//�]�wHUR_CURRENT�p�O�A��X�o�Ӥ�P�U�Ӥ�
			sql=
					"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.LAST_DATA_TIME,A.VOLUME,A.MONTH,A.EVER_SUSPEND,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN ('"+sYearmonth+"') ";
			
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			logger.debug("Set current map...");
			
			while(rs.next()){
				String imsi =rs.getString("IMSI");
				//�O�d�¸��
				oldChargeMap.put(imsi, rs.getDouble("CHARGE"));
			}
			st.close();
			rs.close();

		} catch (SQLException e) {
			logger.error("At setoldChargeMap occur SQLException error", e);
			//send mail
			sendMail("At setoldChargeMap occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * ���X�HPriceplan�����쪺���O
	 */
	private void setCurrencyMap(){
		logger.info("setCurrencyMap...");
		try {
			sql=
					"SELECT  A.PRICEPLANID,A.CURRENCY "
					+ "FROM HUR_DATA_RATE A "
					+ "GROUP BY A.PRICEPLANID,A.CURRENCY ";
			
			logger.debug("Execute SQL : "+sql);
			Statement st = conn.createStatement();
			ResultSet rs =st.executeQuery(sql);
			
			while(rs.next()){
				String id = rs.getString("PRICEPLANID");
				String currency = rs.getString("CURRENCY");
				
				if(id!=null && !"".equals(id)&& 
						currency!=null && !"".equals(currency))
					pricePlanIdtoCurrency.put(id, currency);
			}
			st.close();
			rs.close();
			
		} catch (SQLException e) {
			logger.error("At setCurrencyMap occur SQLException error", e);
			//send mail
			sendMail("At setCurrencyMap occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
		
	}
	
	/**
	 * ���X HUR_CURRENTE_DAY table���
	 * �إߦ�
	 * Map 
	 * Key:day , value:Map(IMSI,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME,ALERT)))
	 * �]�wHUR_CURRENT_DAY�p�O,�ثe�����R���ʧ@�A����Ҽ{�O�_�d2�Ӥ���
	 * 20141209 �ק���X���Ӥ�
	 */
	private void setCurrentMapDay(){
		logger.info("setCurrentMapDay...");
		try {
			sql=
					"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.LAST_DATA_TIME,A.VOLUME,A.UPDATE_DATE,A.CREATE_DATE,A.MCCMNC,A.DAY,A.ALERT "
					+ "FROM HUR_CURRENT_DAY A "
					+ "WHERE SUBSTR(A.DAY,0,6) IN ('"+sYearmonth+"','"+sYearmonth2+"') ";
			logger.debug("Execute SQL : "+sql);
			Statement st = conn.createStatement();
			ResultSet rs =st.executeQuery(sql);
			
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

					//20141201 add �]�w�s�b���
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
			st.close();
			rs.close();
			
		} catch (SQLException e) {
			logger.error("At setCurrentMapDay occur SQLException error", e);
			//send mail
			sendMail("At setCurrentMapDay occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
		
		
	}
	/**
	 * ���X HUR_DATA_RATE
	 * �إߦ�MAP Key:PRICEPLANID,Value:Map(MCCMNC,MAP(CURRENCY,CHARGEUNIT,RATE))
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
			logger.error("At setDataRate occur SQLException error", e);
			//send mail
			sendMail("At setDataRate occur SQLException errorr!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	/**
	 * ���XHUR_THRESHOLD
	 * �إ�MAP Key:IMSI,VALUE:THRESHOLD
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
			logger.error("At setThreshold occur SQLException error", e);
			//send mail
			sendMail("At setThreshold occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * �]�w IMSI �� VLN ��������
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
			logger.error("At setIMSItoVLN occur SQLException error", e);
			//send mail
			sendMail("At setIMSItoVLN occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * 
	 * �إ� VLN��TADIG������
	 * 
	 * �qIMSItoVLN���oVALUE�����Y��ǰt
	 * 
	 * MAP KEY�GVLN,VALUE:TADIG
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
			logger.error("At setVLNtoTADIG occur SQLException error", e);
			//send mail
			sendMail("At setVLNtoTADIG occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	/**
	 * 
	 * �إ� TADIG��MCCMNC������
	 * 
	 * MAP KEY�GTADIG,VALUE:MCCMNC
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
			logger.error("At setTADIGtoMCCMNC occur SQLException error", e);
			//send mail
			sendMail("At setTADIGtoMCCMNC occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	
	
	/**
	 * 
	 * �إ� ��X��ȪA�q�ܡA��a ������
	 * 
	 * MAP KEY�GCODE,VALUE:(PHONE,NAME)
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
			logger.error("At setTADIGtoMCCMNC occur SQLException error", e);
			//send mail
			sendMail("At setTADIGtoMCCMNC occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * ���o��Ƶ���
	 * @return
	 */
	private int dataCount(){
		logger.info("dataCount...");
		sql="SELECT COUNT(1) count  FROM HUR_DATA_USAGE A WHERE A.FILEID>= ? AND A.CHARGE is null ";
		int count=0;
		//��X�`�q
		PreparedStatement pst;
		try {
			pst = conn.prepareStatement(sql);
			pst.setInt(1, lastfileID);
			
			ResultSet rs = pst.executeQuery();
			logger.debug("Execute SQL : "+sql);
			
			while(rs.next()){
				count=rs.getInt("count");
			}
			logger.info("usage count : " +count);
			pst.close();
			rs.close();
			
		} catch (SQLException e) {
			logger.error("At dataCount occur SQLException error", e);
			//send mail
			sendMail("At dataCount occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
		return count;
	}
	
	/**
	 * ���o�w�]�p�O��v�]�`�O�v�����^�A��MCCNOC���o�L�k������ƭp�O
	 * @return
	 */
	private double defaultRate(){
		logger.info("defaultRate...");
		double defaultRate=0.011;
		
	/*	double avg=0;
		sql=
				"SELECT AVG(CASE WHEN A.CURRENCY = 'HKD' THEN A.RATE/A.CHARGEUNIT*"+exchangeRate+" ELSE  A.RATE/A.CHARGEUNIT END)  AVG "
				+ "FROM HUR_DATA_RATE A ";
		//��X�̶Q����
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
			logger.error("Got a SQLException : "+e.getStackTrace());
			//sendMail
			sendMail("At defaultRate occur SQLException error!");
			errorMsg=e.getStackTrace().toString();
		}*/
		logger.info("defaultRate : " +defaultRate+" TWD ");
		return defaultRate;
		//return avg;
	}
	
	/**
	 * �qimsi��M�ثe��mccmnc
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
	 * �إߵؤH�W���]�������
	 * 
	 * List Map KEY:MSISDN,VALUE(IMSI,MSISDN,SERVICECODE,STARTDATE,ENDDATE)>
	 */
	private void setAddonData(){
		logger.info("setAddonData...");
		sql=
				"SELECT A.S2TIMSI IMSI,A.S2TMSISDN MSISDN,A.SERVICECODE,A.STARTDATE,A.ENDDATE "
				+ "FROM ADDONSERVICE_N A ";
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("IMSI", rs.getString("IMSI"));
				map.put("MSISDN", rs.getString("MSISDN"));
				map.put("SERVICECODE", rs.getString("SERVICECODE"));
				map.put("STARTDATE", rs.getDate("STARTDATE"));
				map.put("ENDDATE", rs.getDate("ENDDATE"));
				addonDataList.add(map);
			}
			st.close();
			rs.close();
		} catch (SQLException e) {
			logger.error("At setTADIGtoMCCMNC occur SQLException error", e);
			//send mail
			sendMail("At setTADIGtoMCCMNC occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}

	
	/**
	 * �}�l���
	 */
	private void charge(){
		logger.info("charge...");
		
		int count=0;
		double defaultRate=0;
		try {
			count=dataCount();
			defaultRate=defaultRate();

			//�妸Query �קKram�Ŷ�����
			for(int i=1;(i-1)*dataThreshold+1<=count ;i++){
				sql=
						"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME "
						+ "FROM HUR_DATA_USAGE A WHERE A.FILEID>= "+lastfileID+" AND ROWNUM <= "+(i*dataThreshold)+" AND A.CHARGE is null "
						+ "ORDER BY A.USAGEID,A.FILEID) "
						+ "MINUS "
						+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME "
						+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME "
						+ "FROM HUR_DATA_USAGE A WHERE A.FILEID>= "+lastfileID+" AND ROWNUM <= "+((i-1)*dataThreshold)+" AND A.CHARGE is null "
						+ "ORDER BY A.USAGEID,A.FILEID) ";
				
				logger.debug("Execute SQL : "+sql);
				
				logger.debug("Round "+i+" Procsess ...");
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql);

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
						if(mccmnc==null || "".equals(mccmnc)){
							mccmnc=searchMccmncByIMSI(imsi);
						}
					}else{
						logger.debug("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
						sendMail("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
						sql="";errorMsg="";
					}

					//�٬O�䤣��A�����w�]�A������
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc= DEFAULT_MCCMNC;
					}
					
					//20141118 �ư��ؤH�W���]
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
						
						//�u������
						if(cd==0 && sSX001.contains(mccmnc)){
							for(Map<String,Object> m : addonDataList){
								if(imsi.equals(m.get("IMSI"))&&msisdn.equals(m.get("MSISDN"))&&"SX001".equals(m.get("SERVICECODE"))&&
										callTime.after((Date) m.get("STARTDATE"))&&(m.get("ENDDATE")==null ||callTime.before((Date) m.get("ENDDATE")))){
									cd=1;
									break;
								}
							}
							
						}
						
						//����[����j��
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

					logger.info("cd="+cd);
					if(cd==0){
						//�P�_�O�_�i�H���������O�v��A�íp�⦹��CDR������(charge)
						if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
								dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
							
							double ec=1;
							
							String currency=(String) dataRate.get(pricplanID).get(mccmnc).get("CURRENCY");
							
							if("HKD".equalsIgnoreCase(currency))
								ec=exchangeRate;
							Double rate=(Double)dataRate.get(pricplanID).get(mccmnc).get("RATE");
							Double unit=(Double)dataRate.get(pricplanID).get(mccmnc).get("CHARGEUNIT");
							charge=Math.ceil(volume*kByte)*rate*ec/unit;
							dayCap=(Double)dataRate.get(pricplanID).get(mccmnc).get("DAYCAP");
							
						}else{
							//�S��PRICEPLANID(�믲���)�AMCCMNC�A�L�k�P�_�ϰ�~�̡A�@�k�G�έp�y�q�A
							//�S��������PRICEPLANID(�믲���)�AMCCMNC�A�L�k�P�_�ϰ�~��
							//�H�w�]�O�v�p�O
							sendMail("IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");
							charge=Math.ceil(volume*kByte)*defaultRate;
							sql="";errorMsg="";
						}
					}
		
					
					//�榡�Ʀܤp���I��|��
					charge=tool.FormatDouble(charge, "0.0000");
					logger.info("charge="+charge);
					
					//�N����CDR���G�O���A�y��^�g��USAGE TABLE
					cdrChargeMap.put(usageId, charge);

					//��ݬO�_���H�s�b����ơA�����ܨ��X���֥[
					
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
					
					//�p�G���p�O�W�u�A����̤j��  20141125 �����w�]Daycap�A�p�G�Ȭ��t�A��ܨS��
					//if(dayCap==null || dayCap==0) dayCap= DEFAULT_DAYCAP;
					if(dayCap!=null && dayCap>=0 && charge>dayCap) charge=dayCap;
					
					//�N���G�O����currentDayMap
					map3.put("CHARGE", charge);
					map3.put("LAST_FILEID",fileID);
					map3.put("LAST_DATA_TIME",callTime);
					map3.put("VOLUME",volume+oldvolume);
					map3.put("ALERT",alert);
					map2.put(mccmnc, map3);
					map.put(imsi, map2);
					currentDayMap.put(cDay, map);
					
					//��scurrentMap�A�p�G����CDR�O���ɶ���������s�b
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
	 * �^�gCDR��CHARGE
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
				
				logger.info("Execute Sql: "+sql);
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
	 * ����connection��Auto commit
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
	
	private static void IniProgram(){
		// ��l��log
		// iniLog4j();
		loadProperties();

	}
	
	private void connectDB(){
		// �i��DB�s�u
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
	
	private void connectDB2(){
		// �i��DB�s�u
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
	 * �p�⧹����g�^��Ʈw-��s
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
				+ "WHERE A.MONTH=? AND A.IMSI=? ";
		
		logger.info("Execute SQL :"+sql);

		PreparedStatement pst = conn.prepareStatement(sql);

		//20141201 add change another method to distinguish insert and update
		for(String mon : currentMap.keySet()){
			for(String imsi : currentMap.get(mon).keySet()){
				if(existMap.containsKey(mon) && existMap.get(mon).contains(imsi)){
					pst.setDouble(1,(Double) currentMap.get(mon).get(imsi).get("CHARGE"));
					pst.setInt(2, (Integer) currentMap.get(mon).get(imsi).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMap.get(mon).get(imsi).get("SMS_TIMES"));
					pst.setString(4, spf.format((Date) currentMap.get(mon).get(imsi).get("LAST_DATA_TIME")));
					pst.setDouble(5,(Double) currentMap.get(mon).get(imsi).get("VOLUME"));
					pst.setString(6,(String) currentMap.get(mon).get(imsi).get("EVER_SUSPEND"));
					pst.setDouble(7,(Double) currentMap.get(mon).get(imsi).get("LAST_ALERN_THRESHOLD"));
					pst.setDouble(8,(Double) currentMap.get(mon).get(imsi).get("LAST_ALERN_VOLUME"));
					pst.setString(9, mon);
					pst.setString(10, imsi);//�㦳mccmnc
					pst.addBatch();
					count++;
					
					if(count==dataThreshold){
						logger.info("Execute updateCurrentMap Batch");
						result=pst.executeBatch();
						count=0;
					}
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
				+ "WHERE A.DAY=? AND A.IMSI=? AND A.MCCMNC=? ";
		
		logger.info("Execute SQL :"+sql);
		
		PreparedStatement pst = conn.prepareStatement(sql);

		//20141201 add change another method to distinguish insert and update
		for(String day : currentDayMap.keySet()){
			for(String imsi : currentDayMap.get(day).keySet()){
				for(String mccmnc : currentDayMap.get(day).get(imsi).keySet()){
					if(existMapD.containsKey(day) && existMapD.get(day).containsKey(imsi) && existMapD.get(day).get(imsi).contains(mccmnc)){

						pst.setDouble(1,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE"));
						pst.setInt(2, (Integer) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID"));
						pst.setString(3, spf.format((Date) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME")));
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
		}
		if(count!=0){
			logger.info("Execute updateCurrentMapU Batch");
			result=pst.executeBatch();
		}
		pst.close();

	}
	
	/**
	 * �p�⧹����g�^��Ʈw-�s�W
	 * @throws SQLException 
	 */
	
	private void insertCurrentMap(){
		logger.info("insertCurrentMap...");
		@SuppressWarnings("unused")
		int[] result;
		
		sql=
				"INSERT INTO HUR_CURRENT "
				+ "(IMSI,CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,EVER_SUSPEND,LAST_ALERN_THRESHOLD,LAST_ALERN_VOLUME,MONTH,CREATE_DATE) "
				+ "VALUES(?,?,?,?,TO_DATE(?,'yyyy/MM/dd hh24:mi:ss'),?,?,?,?,?,SYSDATE)";
		
		logger.info("Execute SQL :"+sql);
		
		/*for(String mon : insertMap.keySet()){
			for(String imsi : insertMap.get(mon)){*/
		//20141201 add change another method to distinguish insert and update
		for(String mon : currentMap.keySet()){
			for(String imsi : currentMap.get(mon).keySet()){
				if(!existMap.containsKey(mon) || !existMap.get(mon).contains(imsi)){
					
					try {
						PreparedStatement pst = conn.prepareStatement(sql);
						pst.setString(1, imsi);		
						pst.setDouble(2,(Double) currentMap.get(mon).get(imsi).get("CHARGE"));
						pst.setInt(3,(Integer) currentMap.get(mon).get(imsi).get("LAST_FILEID"));
						pst.setInt(4, (Integer) currentMap.get(mon).get(imsi).get("SMS_TIMES"));
						pst.setString(5, spf.format((Date) currentMap.get(mon).get(imsi).get("LAST_DATA_TIME")));
						pst.setDouble(6,(Double) currentMap.get(mon).get(imsi).get("VOLUME"));
						pst.setString(7,(String) currentMap.get(mon).get(imsi).get("EVER_SUSPEND"));
						pst.setDouble(8,(Double) currentMap.get(mon).get(imsi).get("LAST_ALERN_THRESHOLD"));
						pst.setDouble(9,(Double) currentMap.get(mon).get(imsi).get("LAST_ALERN_VOLUME"));
						pst.setString(10, mon);
						pst.executeUpdate();
						pst.close();
					} catch (SQLException e) {
						logger.error("At insertCurrent "+mon+":"+imsi+" occur SQLException error", e);
						//send mail
						sendMail("At insertCurrent "+mon+":"+imsi+" occur SQLException error!");
						errorMsg="";
						for(StackTraceElement s :e.getStackTrace()){
							errorMsg+=s.toString()+"<br>\n";
						}
						//�s�W���ѫ����update��s
						Set<String> set=new HashSet<String>();
						if(existMap.containsKey(mon))
							set=existMap.get(mon);
						
						set.add(imsi);
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
				+ "(IMSI,CHARGE,LAST_FILEID,LAST_DATA_TIME,VOLUME,CREATE_DATE,MCCMNC,DAY,ALERT) "
				+ "VALUES(?,?,?,TO_DATE(?,'yyyy/MM/dd hh24:mi:ss'),?,SYSDATE,?,?,?)";
		
		logger.info("Execute SQL :"+sql);

		//20141201 add change another method to distinguish insert and update
		for(String day : currentDayMap.keySet()){
			for(String imsi : currentDayMap.get(day).keySet()){
				for(String mccmnc : currentDayMap.get(day).get(imsi).keySet()){
					if(!existMapD.containsKey(day)||!existMapD.get(day).containsKey(imsi)||!existMapD.get(day).get(imsi).contains(mccmnc)){
						try {
							PreparedStatement pst = conn.prepareStatement(sql);
							pst.setString(1, imsi);
							pst.setDouble(2,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE"));
							pst.setInt(3, (Integer) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID"));
							pst.setString(4, spf.format((Date) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME")));
							pst.setDouble(5,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME"));
							pst.setString(6, mccmnc);
							pst.setString(7, day);
							pst.setString(8, (String) currentDayMap.get(day).get(imsi).get(mccmnc).get("ALERT"));
							pst.executeUpdate();
							pst.close();
						} catch (SQLException e) {
							logger.error("At insertCurrentDay "+day+":"+":"+imsi+":"+mccmnc+" occur SQLException error", e);
							//send mail
							sendMail("At insertCurrentDay "+day+":"+":"+imsi+":"+mccmnc+" occur SQLException error!");
							errorMsg="";
							for(StackTraceElement s :e.getStackTrace()){
								errorMsg+=s.toString()+"<br>\n";
							}
							
							//�s�W���ѫ����update��s
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
						}	
						
					}
				}
			}
		}
	}

	
	/**
	 * �̷��B�׻ݨD�o�eĵ��²�T
	 * �Ĥ@���A�B�פ@�A�T���@
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
		
		//���J²�T�]�w
		
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

		} catch (SQLException e) {
			logger.error("At sendAlertSMS:Load SMSSetting occur SQLException error", e);
			//send mail
			sendMail("At sendAlertSMS:Load SMSSetting occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
		
		
		if(times.size()==0){
			logger.error("No SMS Setting!");
			//sendMail
			sql="";
			errorMsg="";
			sendMail("Can't found SMS Setting!");
			return;
		}
		
		//���J²�T���e
		
		Map<String,Map<String,String>> content=new HashMap<String,Map<String,String>>();
		
		try {
			sql=
					"SELECT A.ID,A.CONTENT,A.CHARSET "
					+ "FROM HUR_SMS_CONTENT A ";	
			
			Statement st;
			st = conn.createStatement();
			logger.debug("Execute SQL:"+sql); 
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("CONTENT", rs.getString("CONTENT"));
				map.put("CHARSET", rs.getString("CHARSET"));
				content.put(rs.getString("ID"), map);
			}

		} catch (SQLException e) {
			logger.error("At sendAlertSMS:Load SMSContent occur SQLException error", e);
			//send mail
			sendMail("At sendAlertSMS:Load SMSContent occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} 
		
		if(content.size()==0){
			logger.error("No SMS content!");
			sql="";
			errorMsg="";
			sendMail("Can't found SMS Content sentting!");
			return;
		}
		
		
		//�}�l�ˬd�O�_�o�eĵ��²�T
		
		
		
		//����Bĵ��*************************************
		//�S������ơA���ˬd
		if(currentMap.containsKey(sYearmonth)){
			smsCount=0;
			try {
				Statement st = conn.createStatement();
				//�ˬd�o�Ӥ몺��Ƨ@ĵ�ܳq��
				for(String imsi: currentMap.get(sYearmonth).keySet()){
					
					//�p�G�S��������ơA�]���L�k�o�e²�T�A�H�eĵ�imail����L
					if(msisdnMap.containsKey(imsi))
						phone=(String) msisdnMap.get(imsi).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error! The IMSI:"+imsi+" can't find msisdn to send !");
						logger.error("The IMSI:"+imsi+" can't find msisdn to send! ");
						continue;
					}
					
					String res="";
					
					Double charge=(Double) currentMap.get(sYearmonth).get(imsi).get("CHARGE");
					Double oldCharge=(Double) oldChargeMap.get(imsi);
					if(oldCharge==null)	oldCharge=0D;
	
					Double differenceCharge=charge-oldCharge;
					int smsTimes=(Integer) currentMap.get(sYearmonth).get(imsi).get("SMS_TIMES");
					String everSuspend =(String) currentMap.get(sYearmonth).get(imsi).get("EVER_SUSPEND");
					Double lastAlernThreshold=(Double) currentMap.get(sYearmonth).get(imsi).get("LAST_ALERN_THRESHOLD");
					boolean isCustomized=false;
					
					//20141118 �ק� ���w�Ȥ�q���C5000�����@�����_��
					Double threshold=thresholdMap.get(imsi);

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
					
					//20141118 �ק� ���w�Ȥ�q���C5000�����@�����_���A�W�h�Ȩ�q��0�i��5000����ֿn
					if(threshold!=0D){
						//�ˬd��ζq
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
						
						//�ˬd�w���ζq�A�p�G���e�P�_���εo²�T�A�Τ��O�o�̤W��²�T
						if(!sendSMS||(sendSMS && i!=0)){
							if(charge+differenceCharge>=bracket.get(0)*threshold&&lastAlernThreshold<bracket.get(0)*threshold){
								logger.info("For "+imsi+" add charge "+differenceCharge+" in this hour ,System forecast the next hour will over charge limit");
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
					

					//�H�e²�T
					if(sendSMS){
						//�d�ߩҦb��a���ȪA�q��
						String cPhone = null;
						String nMccmnc=searchMccmncByIMSI(imsi);
						Map<String,String> map=null;
						
						if(nMccmnc!=null && !"".equals(nMccmnc))
							map = codeMap.get(nMccmnc.substring(0,3));
						if(map!=null)
							cPhone=map.get("PHONE");
						
						for(String s:contentid){
							if(s!=null){
								//�H�e²�T
								lastAlernThreshold=alertBracket;
								smsTimes++;
								logger.info("For "+imsi+" send "+smsTimes+"th message:"+msg.get(i));
								String cont =processMag(content.get(s).get("CONTENT"),alertBracket,cPhone);
								//WSDL�覡�I�s WebServer
								//result=tool.callWSDLServer(setSMSXmlParam(cont,phone));

								res=setSMSPostParam(cont,phone);
								currentMap.get(sYearmonth).get(imsi).put("LAST_ALERN_THRESHOLD", lastAlernThreshold);
								logger.debug("send message result : "+res);						
								currentMap.get(sYearmonth).get(imsi).put("SMS_TIMES", smsTimes);
								smsCount++;
								//���_GPRS�A��
								//20141113 �s�W�Ȩ�w�W���������_��
								if(needSuspend &&"0".equals(everSuspend)&&!isCustomized){
									logger.debug("Suspend GPRS ... ");		
									suspend(imsi,phone);
									currentMap.get(sYearmonth).get(imsi).put("EVER_SUSPEND", "1");
								}
								
								sql="INSERT INTO HUR_SMS_LOG"
										+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
										+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+cont+"',TO_DATE('"+spf.format(new Date())+"','yyyy-MM-dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
								//�g�J��Ʈw
								logger.debug("execute SQL : "+sql);
								st.addBatch(sql);
								
							}
							
						}
					}
				}
				logger.debug("Total send month alert"+smsCount+" ...");
				logger.debug("Log to table...executeBatch");
				st.executeBatch();
				st.close();

			} catch (SQLException e) {
				logger.error("At sendMonthAlertSMS occur SQLException error!", e);
				//send mail
				sendMail("At sendMonthAlertSMS occur SQLException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
				
			}catch(Exception e){
				logger.error("At sendMonthAlertSMS occur Exception error!", e);
				//send mail
				sendMail("At sendMonthAlertSMS occur Exception error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			}
		}
		
		
		//�갵��ĵ�ܳ����A�������Ƥ~ĵ��*************************************
		if(currentDayMap.containsKey(sYearmonthday)){
			smsCount=0;
			try {
				Statement st = conn.createStatement();
				for(String imsi:currentDayMap.get(sYearmonthday).keySet()){
					//�ˬd�����O�_�s�b�A�p�G�S��������ơA�]���L�k�o�e²�T�A�H�eĵ�imail����L
					if(msisdnMap.containsKey(imsi))
						phone=(String) msisdnMap.get(imsi).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error! The IMSI:"+imsi+" can't find msisdn to send! ");
						logger.debug("The IMSI:"+imsi+" can't find msisdn to send! ");
						continue;
					}

					Double daycharge=0D;
					String alerted ="0";
					
					//�֭p
					for(String mccmnc : currentDayMap.get(sYearmonthday).get(imsi).keySet()){
						daycharge=daycharge+(Double)currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).get("CHARGE");
						String a=(String) currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).get("ALERT");
						if("1".equals(a)) alerted="1";
					}
					
					if(daycharge>=DEFAULT_DAY_THRESHOLD && "0".equalsIgnoreCase(alerted)){
						//�d�ߩҦb��a���ȪA�q��
						String cPhone = null;
						String nMccmnc=searchMccmncByIMSI(imsi);
						Map<String,String> map=null;
						
						if(nMccmnc!=null && !"".equals(nMccmnc))
							map = codeMap.get(nMccmnc.substring(0,3));
						if(map!=null)
							cPhone=map.get("PHONE");
						
						//�B�z�r��A��ĵ�ܤ��eID�]�w��99
						//20141209 �קאּ�a�X���ֿn���B
						String cont =processMag(content.get("99").get("CONTENT"),(Double)currentMap.get(sYearmonth).get(imsi).get("CHARGE"),cPhone);
						//�o�e²�T
						logger.info("For "+imsi+" send daily allert message:99");
						String res = setSMSPostParam(cont,phone);
						logger.debug("send message result : "+res);	
						smsCount++;
						//�^�g���O�A�]�����Ϥ�Mccmnc�A���������קK���������
						for(String mccmnc : currentDayMap.get(sYearmonthday).get(imsi).keySet()){
							currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).put("ALERT", "1");
						}
						sql="INSERT INTO HUR_SMS_LOG"
								+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
								+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+cont+"',TO_DATE('"+spf.format(new Date())+"','yyyy-MM-dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
						//�g�J��Ʈw
						st.addBatch(sql);
						logger.debug("execute SQL : "+sql);
					}
				}
				logger.debug("Total send day alert "+smsCount+" ...");
				logger.debug("Log to table...executeBatch");
				st.executeBatch();
				st.close();
			
			} catch (SQLException e) {
				logger.error("At sendDayAlertSMS occur SQLException error!", e);
				//send mail
				sendMail("At sendDayAlertSMS occur SQLException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			} catch (IOException e) {
				logger.error("At sendDayAlertSMS occur SQLException error!", e);
				//send mail
				sendMail("At sendDayAlertSMS occur SQLException error!");
				errorMsg="";
				for(StackTraceElement s :e.getStackTrace()){
					errorMsg+=s.toString()+"<br>\n";
				}
			}	
		}
		
		
		//���t����²�T�P�_*************************************
		
		//�Ȧs�ƾڥζq��� Key:IMSI,Value:Volume
		Map<String,Double> tempMap = new HashMap<String,Double>();
		//�O�_�ݭn�p�⪺pricePlanid
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
		
		//�O�_��Data Only �Ȥ�
		Set<String> checkedPriceplanid2 =new HashSet<String>();
		checkedPriceplanid2.add("158");
		checkedPriceplanid2.add("159");
		checkedPriceplanid2.add("160");
		
		for(String day : currentDayMap.keySet()){
			//�o�Ӥ몺����
			if(sYearmonth.equalsIgnoreCase(day.substring(0, 6))){
				for(String imsi:currentDayMap.get(day).keySet()){
					//�T�{priceplanid �P subsidiaryid
					
					String priceplanid = null;
					String subsidiaryid = null;
					if(msisdnMap.containsKey(imsi)){
						priceplanid = msisdnMap.get(imsi).get("PRICEPLANID");
						subsidiaryid = msisdnMap.get(imsi).get("SUBSIDIARYID");
					}
					
					if(checkedPriceplanid.contains(priceplanid)&&"72".equalsIgnoreCase(subsidiaryid)){
						for(String mccmnc:currentDayMap.get(day).get(imsi).keySet()){
							//�T�{Mccmnc
							if(checkedMCCMNC.contains(mccmnc)){
								//�i��֭p
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
			Statement st = conn.createStatement();
			for(String imsi:tempMap.keySet()){
				Double volume=tempMap.get(imsi);
				Double everAlertVolume = (Double) currentMap.get(sYearmonth).get(imsi).get("LAST_ALERN_VOLUME");
				//�W�L�o²�T�A�t�~�T�{�O�_�w�q���L
				boolean sendmsg=false;
				Integer msgid=0;
				
				if(volume>=DEFAULT_VOLUME_THRESHOLD2 && everAlertVolume<volume){
					//2.0 GB ²�T����102�A�^��103
					msgid=102;
					sendmsg=true;
				}
				if(!sendmsg && volume>=DEFAULT_VOLUME_THRESHOLD && everAlertVolume<volume){
					//2.0 GB ²�T����100�A�^��101
					msgid=100;
					sendmsg=true;
				}
				
				if(sendmsg){
					String priceplanid = null;
					if(msisdnMap.containsKey(imsi)){
						priceplanid = msisdnMap.get(imsi).get("PRICEPLANID");
						//�O�_��Data only ���
						if(checkedPriceplanid2.contains(priceplanid)){
							//�H�]�w�q�����q��
							phone=msisdnMap.get(imsi).get("NCODE");
						}else{
							//�H�����q��
							phone=msisdnMap.get(imsi).get("MSISDN");
						}
					}
					
					//�T�{���X
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error! The IMSI:"+imsi+" can't find msisdn to send! ");
						logger.debug("The IMSI:"+imsi+" can't find msisdn to send! ");
						continue;
					}
					
					//�d�ߩҦb��a���ȪA�q��
					String cPhone = null;
					String nMccmnc=searchMccmncByIMSI(imsi);
					Map<String,String> map=null;
					
					if(nMccmnc!=null && !"".equals(nMccmnc))
						map = codeMap.get(nMccmnc.substring(0,3));
					if(map!=null)
						cPhone=map.get("PHONE");
					
					//�o�e²�T
					if(msgid==100)
						logger.info("For "+imsi+" send 1.5GB decrease speed  message !");
					if(msgid==102)
						logger.info("For "+imsi+" send 2.0GB decrease speed  message !");
					
					//����
					//�B�z�r��
					String cont =processMag(content.get(msgid.toString()).get("CONTENT"),null,cPhone);
					//�o�e²�T
					String res = setSMSPostParam(cont,phone);
					logger.debug("send chinese message result : "+res);	
					smsCount++;
					
					sql="INSERT INTO HUR_SMS_LOG"
							+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
							+ "VALUES(DVRS_SMS_ID.NEXTVAL,'"+phone+"','"+cont+"',TO_DATE('"+spf.format(new Date())+"','yyyy-MM-dd HH24:mi:ss'),'"+(res.contains("Message Submitted")?"Success":"failed")+"',SYSDATE)";
					//�g�J��Ʈw
					st.addBatch(sql);
					logger.debug("execute SQL : "+sql); 
					
					
					//�^��
					msgid+=1;
					//�B�z�r��
					cont =processMag(content.get(msgid.toString()).get("CONTENT"),null,cPhone);
					//�o�e²�T
					res = setSMSPostParam(cont,phone);
					logger.debug("send english message result : "+res);	
					smsCount++;

					//��sCurrentMap
					currentMap.get(sYearmonth).get(imsi).put("LAST_ALERN_VOLUME",volume);

				}
				
			}
			logger.debug("Total send volume alert SMS "+smsCount+" ...");
			st.executeBatch();
			st.close();
			
		} catch (SQLException e) {
			logger.error("At sendDayAlertSMS occur SQLException error!", e);
			//send mail
			sendMail("At sendDayAlertSMS occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		} catch (IOException e) {
			logger.error("At sendDayAlertSMS occur SQLException error!", e);
			//send mail
			sendMail("At sendDayAlertSMS occur SQLException error!");
			errorMsg="";
			for(StackTraceElement s :e.getStackTrace()){
				errorMsg+=s.toString()+"<br>\n";
			}
		}
	}
	
	/**
	 * �B�z���N�r��
	 * {{bracket}} �B��
	 * @param msg
	 * @param bracket
	 * @return
	 */
	private String processMag(String msg,Double bracket,String cPhone){
		
		//���B
		if(bracket==null)
			bracket=0D;
		msg=msg.replace("{{bracket}}", tool.FormatNumString(bracket,"NT#,##0.00"));
		
		//�ȪA�q��
		if(cPhone==null)
			cPhone="";
		msg=msg.replace("{{customerService}}",cPhone);
		
		return msg;
	}
	/**
	 * �o�e²�T�\��
	 * �B�zpost�� WebServer��xml�r��
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
				+ "	<ORGCODE>�N�o��´����</ORGCODE>"
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
	 * �o�e²�T�\��
	 * �B�zpost�� http������Url�öǰe
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
	 * ���Xmsisdn
	 * �إ�msisdnMap
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
		logger.info("suspend...");
		
		try {
			
			suspendGPRS sus=new suspendGPRS(conn,conn2,logger);
			
			//20141118 add �Ǧ^suspend�Ƶ{�� service order nbr
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
				
				//�p�G���A��s���ѡA�S���ʧ@�o�e���~Email
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
				
				
				//��s�^Table
				SimpleDateFormat dFormat4=new SimpleDateFormat("yyyyMMddHHmmss");
				String dString=dFormat4.format(new Date());
				//PROVLOG ���ݭn
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
		// �{���}�l�ɶ�
		long startTime;
		// �{�������ɶ�
		long endTime;
		// �Ƶ{���}�l�ɶ�
		long subStartTime;

		logger.info("RFP Program Start! "+new Date());
		
		// �i��DB�s�u
			connectDB();
			connectDB2();
		
		if (conn != null && conn2!=null) {
			
			logger.debug("connect success!");
			
			startTime = System.currentTimeMillis();
			
			//�����۰�Commit
			cancelAutoCommit();
			
			//�]�w���
			setDayDate();
			
			//���o�̫��s��FileID
			subStartTime = System.currentTimeMillis();
			setLastFileID();
			logger.info("setLastFileID execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//���XHUR_CURRENT
			subStartTime = System.currentTimeMillis();
			setCurrentMap();
			setoldChargeMap();
			setCurrentMapDay();
			logger.info("setCurrentMap execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//���XHUR_THRESHOLD
			subStartTime = System.currentTimeMillis();
			setThreshold();
			logger.info("setThreshold execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//���XHUR_DATARATE
			subStartTime = System.currentTimeMillis();
			setDataRate();
			logger.info("setDataRate execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//���Xmsisdn��T
			subStartTime = System.currentTimeMillis();
			setMsisdnMap();
			logger.info("setMsisdnMap execute time :"+(System.currentTimeMillis()-subStartTime));
	
			//IMSI ������ vln
			subStartTime = System.currentTimeMillis();
			setIMSItoVLN();
			logger.info("setIMSItoVLN execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//vln ������ TADIG
			subStartTime = System.currentTimeMillis();
			setVLNtoTADIG();
			logger.info("setVLNtoTADIG execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//TADIG ������ MCCMNC
			subStartTime = System.currentTimeMillis();
			setTADIGtoMCCMNC();
			logger.info("setTADIGtoMCCMNC execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//��X������(�ȪA,��W)
			subStartTime = System.currentTimeMillis();
			setCostomerNumber();
			logger.info("setCostomerNumber execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//�ؤH�W���]�ӽи��
			subStartTime = System.currentTimeMillis();
			setAddonData();
			logger.info("setAddonData execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//�}�l��� 
			subStartTime = System.currentTimeMillis();
			charge();
			logger.info("charge execute time :"+(System.currentTimeMillis()-subStartTime));

			//�o�eĵ��²�T
			subStartTime = System.currentTimeMillis();
			sendAlertSMS();
			logger.info("sendAlertSMS execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//�^�g������G
			subStartTime = System.currentTimeMillis();
			//�קK��Ʋ��`�A�����B�z������bcommit
			try {
				updateCdr();
				insertCurrentMap();
				insertCurrentMapDay();
				updateCurrentMap();
				updateCurrentMapDay();
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
			logger.info("insert��update execute time :"+(System.currentTimeMillis()-subStartTime));

			//suspend������l�ܳB�z
			subStartTime = System.currentTimeMillis();
			processSuspendNBR();
			logger.info("processSuspendNBR execute time :"+(System.currentTimeMillis()-subStartTime));
			
			
			// �{�����槹��
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

		
		
		
		
		//�p�G�w�����ݪ�thread�A�����ۤv
		if(hasWaiting) {
			logger.debug("****************************      Found had wating thread doesn't execute!");
			return;
		}
		//�p�G�w�g�b�i�椤�A�Ȱ�
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
			//���}���ݪ��A
			hasWaiting=false;
			logger.debug("****************************      Thread Leave Wating... ");
		}

		
		//�}�l����{��
		executing=true;
		process();
		
		//�N�ʧ@�浹�U��thread
		executing=false;
		
		//sendMail("test mail " + new Date());
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
			
			// �ϵ{���Ȱ��AJob����B�@
			//pause();// �Hsleep���覡�Ȱ�
			//keyin();// �H���ݨϥΪ�keyin���覡�Ȱ�

			//scheduler.shutdown();

			//JobDetail job = new JobDetail("job1", "group1", SayHelloJob.class);
			//// �ѩ�quartz support�hgroup��hjob. �o�̧ڭ̥u���@��job. �ڭ̦ۤv���H�N�⥦�R�W.
			//// ���ۦPgroup�̦p�G�X�{�ۦP��job�W,�|�Qoverrride.
			//CronTrigger cTrigger = new CronTrigger("trigg1", "group1", "job1",
			//"group1", "1/10 * * * * ?");
			//// �o�̫��wtrigger���樺��group��job.
			//// "1/10 * * * * ?" �P �bunix like�̪�crontab job���]�w����. �o�̪�ܨC�Ѹ̪��C10�����@��
			//// Seconds Minutes Hours Day-of-Month Month Day-of-Week Year(optional field)
		
			
		} catch (SchedulerException e) {
			e.printStackTrace();
			sendMail("at regularTimeRun occure error!");
		}
	}
	
	public static void main(String[] args) {
		//DVRSmain rf =new DVRSmain();
		//rf.process();

		IniProgram();

		regularTimeRun();
		
		//closeConnect();

	}
	
	
}



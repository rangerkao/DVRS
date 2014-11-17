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
	
	static int runInterval=1*60*60;//����
	
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
	private Integer dataThreshold=null;//CDR��Ƥ@�妸���X�ƶq
	private Integer lastfileID=null;//�̫����ɮ׸�
	private Double exchangeRate=null; //�����x���ײv�A�ȭq��4
	private Double kByte=null;//RATE���KB�AUSAGE���B

	//����]�w
	private String MONTH_FORMATE="yyyyMM";
	//�t�ήɶ��A�~�t�@�p�ɡA�t�θ�ƳB�z�ɶ�����ɮɶ����e�@�p��
	private String sYearmonth="";
	private String sYearmonthday="";
	//�W�Ӥ�
	private String sYearmonth2="";
	private String DAY_FORMATE="yyyyMMdd";	
	
	//�w�]��
	private String DEFAULT_MCCMNC=null;//�w�]mssmnc
	private Double DEFAULT_THRESHOLD=null;//�w�]��ĵ�ܶq
	private Double DEFAULT_DAY_THRESHOLD=null;//�w�]��ĵ�ܶq
	private Double DEFAULT_DAYCAP=null;
	private Double DEFAULT_VOLUME_THRESHOLD=null;//�w�]�y�qĵ��(���t)�A1.5GB;
	private Double DEFAULT_VOLUME_THRESHOLD2=null;//�w�]�y�qĵ��(���t)�A15GB;
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
	private  void loadProperties(){
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
			TEST_MODE=("true".equalsIgnoreCase(props.getProperty("progrma.TEST_MODE"))?true:false);
			
			dataThreshold=(props.getProperty("progrma.dataThreshold")!=null?Integer.parseInt(props.getProperty("progrma.dataThreshold")):500);//CDR��Ƥ@�妸���X�ƶq
			lastfileID=(props.getProperty("progrma.lastfileID")!=null?Integer.parseInt(props.getProperty("progrma.lastfileID")):0);//�̫����ɮ׸�
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
	 * �����s�u
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
		calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR)-1);
		sYearmonth2=tool.DateFormat(calendar.getTime(), MONTH_FORMATE);
		
		calendar.clear();
	}
	
	/**
	 * �M��̫�@����諸fileID
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
				
				
				//�O�d�¸��
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
	 * ���X HUR_CURRENTE_DAY table���
	 * �إߦ�
	 * Map 
	 * Key:day , value:Map(IMSI,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME,ALERT)))
	 */
	private void setCurrentMapDay(){
		//�]�wHUR_CURRENT_DAY�p�O,�ثe�����R���ʧ@�A����Ҽ{�O�_�d2�Ӥ���
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
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setDataRate occur SQLException error!");
			errorMsg=e.getMessage();
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
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setThreshold occur SQLException error!");
			errorMsg=e.getMessage();
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
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setIMSItoVLN occur SQLException error!");
			errorMsg=e.getMessage();
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
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setVLNtoTADIG occur SQLException error!");
			errorMsg=e.getMessage();
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
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setTADIGtoMCCMNC occur SQLException error!");
			errorMsg=e.getMessage();
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
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At setTADIGtoMCCMNC occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	
	/**
	 * ���o��Ƶ���
	 * @return
	 */
	private int dataCount(){
		logger.info("dataCount...");
		sql="SELECT COUNT(1) count  FROM HUR_DATA_USAGE A WHERE A.FILEID>= ? ";
		int count=0;
		//��X�`�q
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
	 * ���o�w�]�p�O��v�]�`�O�v�����^�A��MCCNOC���o�L�k������ƭp�O
	 * @return
	 */
	private double defaultRate(){
		logger.info("defaultRate...");
		double avg=0;
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
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At defaultRate occur SQLException error!");
			errorMsg=e.getMessage();
		}
		
		return avg;
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
	 * �}�l���
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
			
			//�妸Query �קKram�Ŷ�����
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
						//���p�S��mccmnc�����w�]�r�ˡA�����n��
						if(mccmnc==null || "".equals(mccmnc)){
							mccmnc=searchMccmncByIMSI(imsi);
						}
					}else{
						logger.debug("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
						sendMail("FOR IMSI:"+imsi+",the PRICEPLANID:"+pricplanID+" NOT EXIST in HUR_DATA_RATE!");
					}
					
					//�٬O�䤣��A�����w�]�A������
					if(mccmnc==null || "".equals(mccmnc)){
						mccmnc= DEFAULT_MCCMNC;
					}
					
					//�P�_�O�_�i�H���������O�v��A�íp�⦹��CDR������(charge)
					if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
							dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
						
						double ec=1;
						if("HKD".equalsIgnoreCase((String) dataRate.get(pricplanID).get(mccmnc).get("CURRENCY")))
							ec=exchangeRate;
						charge=volume*kByte*(Double)dataRate.get(pricplanID).get(mccmnc).get("RATE")*ec;
						dayCap=(Double)dataRate.get(pricplanID).get(mccmnc).get("DAYCAP");
						
					}else{
						//�S��PRICEPLANID(�믲���)�AMCCMNC�A�L�k�P�_�ϰ�~�̡A�@�k�G�έp�y�q�A
						//�S��������PRICEPLANID(�믲���)�AMCCMNC�A�L�k�P�_�ϰ�~��
						//�H�̤j�O�v�p�O
						sendMail("IMSI:"+imsi+" can't charge correctly without mccmnc or mccmnc is not in Data_Rate table ! ");
						charge=volume*kByte*defaultRate;
					}
					
					//�榡�Ʀܤp���I��|��
					charge=tool.FormatDouble(charge, "0.0000");
					
					//�N����CDR���G�O���A�y��^�g��USAGE TABLE
					cdrChargeMap.put(usageId, charge);

					//��ݬO�_���H�s�b����ơA�����ܨ��X���֥[
					
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
					
					//�p�G���p�O�W�u�A����̤j��
					if(dayCap==null || dayCap==0) dayCap= DEFAULT_DAYCAP;
					if(dayCap!=null && charge>dayCap) charge=dayCap;
					
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
	 * �^�gCDR��CHARGE
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
	 * ����connection��Auto commit
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
		// ��l��log
		// iniLog4j();
		loadProperties();
		
		// �i��DB�s�u
		connectDB();
		connectDB2();
		
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
	 * �p�⧹����g�^��Ʈw-��s
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
	 * �p�⧹����g�^��Ʈw-�s�W
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
		
		//���J²�T���e
		
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
		
		
		//�}�l�ˬd�O�_�o�eĵ��²�T
		
		sql="INSERT INTO HUR_SMS_LOG"
				+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
				+ "VALUES(DVRS_SMS_ID.NEXTVAL,?,?,?,?,SYSDATE)";
		
		//����Bĵ��*************************************
		//�S������ơA���ˬd
		if(currentMap.containsKey(sYearmonth)){
			smsCount=0;
			try {
				PreparedStatement pst = conn.prepareStatement(sql);
				logger.info("Execute SQL :"+sql);
				//�ˬd�o�Ӥ몺��Ƨ@ĵ�ܳq��
				for(String imsi: currentMap.get(sYearmonth).keySet()){
					
					//�p�G�S��������ơA�]���L�k�o�e²�T�A�H�eĵ�imail����L
					if(msisdnMap.containsKey(imsi))
						phone=(String) msisdnMap.get(imsi).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error!<br>\n "
								+ "The IMSI:"+imsi+" can't find msisdn to send! ");
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
					//�ˬd��ζq
					for(;i<times.size();i++){
						if(((charge>=bracket.get(i)*threshold))&&lastAlernThreshold<bracket.get(i)*threshold){
							sendSMS=true;
							break;
						}
					}	
					
					//�ˬd�w���ζq�A�p�G���e�P�_���εo²�T�A�άO�D�o�̤W��²�T
					if(!sendSMS||(sendSMS && i!=0)){
						if(charge+differenceCharge>=bracket.get(0)*threshold&&lastAlernThreshold<bracket.get(0)*threshold){
							logger.info("For "+imsi+" ,System forecast the next hour will over charge limit");
							sendSMS=true;
							i=0;
						}
					}

					//�H�e²�T
					if(sendSMS){
						for(String s:msg.get(i).split(",")){
							if(s!=null){
								//�H�e²�T
								lastAlernThreshold=bracket.get(i)*threshold;
								smsTimes++;
								logger.info("For "+imsi+" send "+smsTimes+"th message:"+msg.get(i));
								String cont =processMag(content.get(s).get("COMTENT"),bracket.get(i)*threshold,cPhone);
								//TODO
								//WSDL�覡�I�s WebServer
								//result=tool.callWSDLServer(setSMSXmlParam(cont,phone));
								//WSDL�覡�I�s WebServer
								res=setSMSPostParam(cont,phone);
								currentMap.get(sYearmonth).get(imsi).put("LAST_ALERN_THRESHOLD", lastAlernThreshold);
								logger.debug("send message result : "+res);						
								currentMap.get(sYearmonth).get(imsi).put("SMS_TIMES", smsTimes);
								smsCount++;
								//���_GPRS�A��
								//20141113 �s�W�Ȩ�w�W���������_��
								if("1".equals(suspend.get(i))&&"0".equals(everSuspend)&&!isCustomized){
									logger.debug("Suspend GPRS ... ");		
									suspend(imsi,phone);
									currentMap.get(sYearmonth).get(imsi).put("EVER_SUSPEND", "1");
								}
								//�g�J��Ʈw
								pst.setString(1, phone);
								pst.setString(2, msg.get(i));
								pst.setDate(3,tool.convertJaveUtilDate_To_JavaSqlDate(new Date()));
								pst.setString(4, res);
								pst.addBatch();
								
								//HUR_Current �ݧ�s
								//�p�G�O�s��ơAinsertList�|�w����ơA�������Oupdate
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
		
		
		//�갵��ĵ�ܳ����A�������Ƥ~ĵ��*************************************
		if(currentDayMap.containsKey(sYearmonthday)){
			smsCount=0;
			try {
				PreparedStatement pst = conn.prepareStatement(sql);
				for(String imsi:currentDayMap.get(sYearmonthday).keySet()){
					//�ˬd�����O�_�s�b�A�p�G�S��������ơA�]���L�k�o�e²�T�A�H�eĵ�imail����L
					if(msisdnMap.containsKey(imsi))
						phone=(String) msisdnMap.get(imsi).get("MSISDN");
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error!<br>\n "
								+ "The IMSI:"+imsi+" can't find msisdn to send! ");
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
					
					Double daycharge=0D;
					String alerted =null;
					
					//�֭p
					for(String mccmnc : currentDayMap.get(sYearmonthday).get(imsi).keySet()){
						daycharge=daycharge+(Double)currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).get("CHARGE");
						alerted=(String) currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).get("ALERT");
					}
					
					if(daycharge>=DEFAULT_DAY_THRESHOLD && "0".equalsIgnoreCase(alerted)){
						//�B�z�r��A��ĵ�ܤ��eID�]�w��99
						String cont =processMag(content.get("99").get("COMTENT"),DEFAULT_DAY_THRESHOLD,cPhone);
						//�o�e²�T
						String res = setSMSPostParam(cont,phone);
						logger.debug("send message result : "+res);	
						smsCount++;
						//�^�g���O�A�]�����Ϥ�Mccmnc�A���������קK���������
						for(String mccmnc : currentDayMap.get(sYearmonthday).get(imsi).keySet()){
							currentDayMap.get(sYearmonthday).get(imsi).get(mccmnc).put("ALERT", "1");
							
							//HUR_Current_day �ݧ�s�A�p�G�O�s��ơAinsertList�|�w����ơA�������Oupdate
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
						
						//�g�J��Ʈw
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
					String priceplanid = msisdnMap.get(imsi).get("PRICEPLANID");
					String subsidiaryid = msisdnMap.get(imsi).get("SUBSIDIARYID");
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
			PreparedStatement pst = conn.prepareStatement(sql);
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
					String priceplanid = msisdnMap.get(imsi).get("PRICEPLANID");
					//�O�_��Data only ���
					if(checkedPriceplanid2.contains(priceplanid)){
						//�H�]�w�q�����q��
						phone=msisdnMap.get(imsi).get("NCODE");
					}else{
						//�H�����q��
						phone=msisdnMap.get(imsi).get("MSISDN");
					}
					//�T�{���X
					if(phone==null ||"".equals(phone)){
						//sendMail
						sendMail("At sendAlertSMS occur error!<br>\n "
								+ "The IMSI:"+imsi+" can't find msisdn to send! ");
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
					String cont =processMag(content.get(msgid.toString()).get("COMTENT"),null,cPhone);
					//�o�e²�T
					String res = setSMSPostParam(cont,phone);
					logger.debug("send chinese message result : "+res);	
					smsCount++;
					
					//�^��
					msgid+=1;
					//�B�z�r��
					cont =processMag(content.get(msgid.toString()).get("COMTENT"),null,cPhone);
					//�o�e²�T
					res = setSMSPostParam(cont,phone);
					logger.debug("send english message result : "+res);	
					smsCount++;
					
					//�g�J��Ʈw
					pst.setString(1, phone);
					pst.setString(2, msgid.toString());
					pst.setDate(3,tool.convertJaveUtilDate_To_JavaSqlDate(new Date()));
					pst.setString(4, res);
					pst.addBatch();
					
					//��sCurrentMap
					currentMap.get(sYearmonth).get(imsi).put("LAST_ALERN_VOLUME",volume);
					
					//�p�G�O�s��ơAinsertList�|�w����ơA�������Oupdate
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
		msg=msg.replace("{{customerService}}", "+"+cPhone);
		
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
			// �{���}�l�ɶ�
			long startTime;
			// �{�������ɶ�
			long endTime;
			// �Ƶ{���}�l�ɶ�
			long subStartTime;

			IniProgram();
			
			logger.info("RFP Program Start! "+new Date());
			
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
				updateCdr();
				
				//�קK��Ʋ��`�A�����B�z������bcommit
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
				logger.info("insert��update execute time :"+(System.currentTimeMillis()-subStartTime));

				
				// �{�����槹��
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



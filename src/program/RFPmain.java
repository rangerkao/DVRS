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

public class RFPmain implements Job{

	
	//DB config
/*	private  final String DriverClass="oracle.jdbc.driver.OracleDriver";
	private  final String Host="10.42.1.101";
	private  final String Port="1521";
	private  final String ServiceName=":S2TBSDEV";
	private  final String UserName="foyadev";
	private  final String PassWord="foyadev";

	private  final String URL = "jdbc:oracle:thin:@"+ Host + ":"+Port+ServiceName; */
	
	Connection conn = null;
	
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
	private  int dataThreshold=1000;
	private  int lastfileID=0;
	private final int exchangeRate=4; //�����x���ײv�A�ȭq��4
	private final double kByte=1/1024D;//RATE���KB�AUSAGE���B
	
	/*private Date monthFirstDate=null;
	private Date monthLastDate=null;*/
	private String MONTH_FORMATE="yyyyMM";
	private String sYearmonth="";
	private String sYearmonth2="";
	
	private String DAY_FORMATE="yyyyMMdd";
	private String sDay="";
	private String sDay2="";
	
	
	private String DEFAULT_MCCMNC="default";
	/*private Date dayFirstDate=null;
	private Date dayLastDate=null;*/
	
	Map<String,Map<String,Map<String,Object>>> currentMap = new HashMap<String,Map<String,Map<String,Object>>>();
	Map<String,Map<String,Map<String,Map<String,Object>>>> currentDayMap = new HashMap<String,Map<String,Map<String,Map<String,Object>>>>();
	Map<String,Map<String,Map<String,Object>>> dataRate = new HashMap<String,Map<String,Map<String,Object>>>();
	Map<String,Map<String,String>> msisdnMap = new HashMap<String,Map<String,String>>();
	
	Map <String,Set<String>> updateMap = new HashMap<String,Set<String>>();
	Map <String,Map <String,Set<String>>> updateMapD = new HashMap<String,Map <String,Set<String>>>();
	Map <String,Set<String>> insertMap = new HashMap<String,Set<String>>();
	Map <String,Map <String,Set<String>>> insertMapD = new HashMap<String,Map <String,Set<String>>>();
	
	List<String> updateList = new ArrayList<String>();
	List<String> updateListU = new ArrayList<String>();
	List<String> inseretList = new ArrayList<String>();
	List<String> inseretListU = new ArrayList<String>();
	
	/**
	 * ��l��
	 * ���JLog4j Properties
	 */
	private  void iniLog4j(){
		System.out.println("initial Log4g, property at "+RFPmain.class.getResource(""));
		PropertyConfigurator.configure(RFPmain.class.getResource("").toString().replace("file:/", "")+"Log4j.properties");
		logger =Logger.getLogger(RFPmain.class);
	}
	
	/**
	 * ��l��
	 * ���JLog4j Properties
	 * �P�ɸ��J�Ѽ�porps
	 */
	private  void loadProperties(){
		System.out.println("initial Log4g, property !");
		String path=RFPmain.class.getResource("").toString().replace("file:/", "")+"/log4j.properties";
		try {
			props.load(new   FileInputStream(path));
			PropertyConfigurator.configure(props);
			logger =Logger.getLogger(RFPmain.class);
			logger.info("Logger Load Success!");
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
				logger.debug("close ResultSet Error : "+e.getMessage());
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

		/*//���o���Ѷ}�l�ɶ�
		dayFirstDate=tool.getDayFirstDate(new Date());
		//���o���ѳ̫�ɶ�
		dayLastDate=tool.getDayLastDate(new Date());*/
		
		//�ثe�ɶ�
		sYearmonth=tool.DateFormat(new Date(), MONTH_FORMATE);
		sDay=tool.DateFormat(new Date(), DAY_FORMATE);
		logger.debug("set Current Time...");
		//�W�Ӥ�
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));//�q�{����e�ɶ�
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)-1);
		sYearmonth2=tool.DateFormat(calendar.getTime(), MONTH_FORMATE);
		calendar.clear();
		//�Q��
		calendar = Calendar.getInstance();//�q�{����e�ɶ�
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-1);
		sDay2=tool.DateFormat(calendar.getTime(), DAY_FORMATE);
		logger.debug("set One Hour Before Time...");
		
		/*//�]�w�C�Ӥ�Ĥ@��
		monthFirstDate=tool.getMonthFirstDate(new Date());
		logger.debug("set monthFirstDate:"+monthFirstDate);
		
		//�]�w�C�Ӥ�̫�@��
		monthLastDate= tool.getMonthLastDate(new Date());
		logger.debug("set monthLastDate:"+monthLastDate);*/
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
	 * Key:MONTH,Value:Map(IMSI,Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME)))
	 */
	private void setCurrentMap(){
		logger.info("setCurrentMap...");
		try {
			//�]�wHUR_CURRENT�p�O�A��X�o�Ӥ�P�U�Ӥ�
			sql=
					"SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.LAST_DATA_TIME,A.VOLUME,A.MONTH "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH IN (?,?) ";
			
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, sYearmonth);
			pst.setString(2, sYearmonth2);
			
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = pst.executeQuery(sql);
			logger.debug("Set current map...");
			
			while(rs.next()){
				Map<String,Object> map=new HashMap<String,Object>();
				String imsi =rs.getString("IMSI");
				String month=rs.getString("MONTH");
				map.put("LAST_FILEID", rs.getInt("LAST_FILEID"));
				map.put("SMS_TIMES", rs.getInt("SMS_TIMES"));
				map.put("LAST_DATA_TIME", (rs.getDate("LAST_DATA_TIME")!=null?rs.getDate("LAST_DATA_TIME"):new Date()));
				map.put("CHARGE", rs.getDouble("CHARGE"));
				map.put("VOLUME", rs.getDouble("VOLUME"));
				Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();
				map2.put(imsi, map);
				currentMap.put(month,map2);
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
	 * Key:day , value:Map(IMSI,Map(MCCMNC,Map(LAST_FILEID,LAST_DATA_TIME,CHARGE,VOLUME)))
	 */
	private void setCurrentMapDay(){
		//�]�wHUR_CURRENT_DAY�p�O
		logger.info("setCurrentMapDay...");
		try {
			sql=
					"SELECT SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.LAST_DATA_TIME,A.VOLUME,A.UPDATE_DATE,A.CREATE_DATE,A.MCCMNC,A.DAY "
					+ "FROM HUR_CURRENT_DAY A ";
			logger.debug("Execute SQL : "+sql);
			Statement st = conn.createStatement();
			logger.debug("Set current day map...");
			ResultSet rs2 =st.executeQuery(sql);
			
			while(rs2.next()){
				Map<String,Object> map=new HashMap<String,Object>();
				String mccmnc=rs2.getString("MCCMNC");
				if(mccmnc==null || "".equals(mccmnc)) mccmnc=DEFAULT_MCCMNC;
				
				
				String imsi =rs2.getString("IMSI");
				String day =rs2.getString("DAY");
				//System.out.println("imsi : "+imsi);
				
				map.put("LAST_FILEID", rs2.getInt("LAST_FILEID"));
				map.put("LAST_DATA_TIME", (rs2.getDate("LAST_DATA_TIME")!=null?rs2.getDate("LAST_DATA_TIME"):new Date()));
				map.put("CHARGE", rs2.getDouble("CHARGE"));
				map.put("VOLUME", rs2.getDouble("VOLUME"));
	
				Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();
				map2.put(mccmnc, map);
				Map<String,Map<String,Map<String,Object>>> map3=new HashMap<String,Map<String,Map<String,Object>>>();
				map3.put(imsi, map2);
				currentDayMap.put(day,map3);
			}
			st.close();
			rs2.close();
			closeConnect();
			
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
				map.put("RATE", rs.getDouble("RATE"));
				map.put("CHARGEUNIT", rs.getDouble("CHARGEUNIT"));
				map.put("CURRENCY", rs.getString("CURRENCY"));
				map.put("DAYCAP", rs.getString("DAYCAP"));
				
				Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();
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
	 * ���o�̤j�p�O��v�A��MCCNOC���o�L�k������ƭp�O
	 * @return
	 */
	private double maxRate(){
		logger.info("maxRate...");
		double max=0;
		
		sql=
				"SELECT MAX(CASE WHEN A.CURRENCY = 'HKD' THEN A.RATE/A.CHARGEUNIT*"+exchangeRate+" ELSE  A.RATE/A.CHARGEUNIT END)  max "
				+ "FROM HUR_DATA_RATE A ";
		//��X�̶Q����
		try {
			Statement st = conn.createStatement();
			
			ResultSet rs = st.executeQuery(sql);
			logger.debug("Execute SQL : "+sql);
			
			while(rs.next()){
				max=rs.getDouble("max");
			}
			logger.info("Max Rate : " +max+" TWD ");
			st.close();
			rs.close();
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At maxRate occur SQLException error!");
			errorMsg=e.getMessage();
		}
		
		return max;
	}
	
	/**
	 * �}�l���
	 */
	private void charge(){
		logger.info("charge...");
		sql=
				"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME "
				+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME "
				+ "			FROM HUR_DATA_USAGE A WHERE A.FILEID>=? AND ROWNUM <= ?  ORDER BY A.USAGEID,A.FILEID) "
				+ "MINUS "
				+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME "
				+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME "
				+ "			FROM HUR_DATA_USAGE A WHERE A.FILEID>=? AND ROWNUM <= ?  ORDER BY A.USAGEID,A.FILEID)";

		String sql2=
				"UPDATE HUR_DATA_USAGE A "
				+ "SET A.CHARGE=? "
				+ "WHERE A.USAGEID=? ";

		int count=0;
		double maxRate=0;
		int j=1;
		try {
			count=dataCount();
			maxRate=maxRate();
			//�妸Query �קKram�Ŷ�����
			logger.debug("Execute SQL : "+sql);

			for(int i=1;(i-1)*dataThreshold+1<count ;i++){	
				PreparedStatement pst = conn.prepareStatement(sql);
				
				pst.setInt(1, lastfileID+1);
				pst.setInt(2, i*dataThreshold);
				pst.setInt(3, lastfileID+1);
				pst.setInt(4, (i-1)*dataThreshold);
				
				ResultSet rs = pst.executeQuery();

				PreparedStatement pst2 = conn.prepareStatement(sql2);
				
				while(rs.next()){
					/*System.out.println(j+"\t:\t"+rs.getString("USAGEID"));
					j++;*/
					
					String imsi= rs.getString("IMSI");
					String mccmnc=rs.getString("MCCMNC");
					//���p�S��mccmnc�����w�]�r�ˡA�����n��
					if(mccmnc==null || "".equals(mccmnc)) mccmnc= DEFAULT_MCCMNC;
					
					Double volume=rs.getDouble("DATAVOLUME");
					Date callTime=rs.getDate("CALLTIME");
					Integer fileID=rs.getInt("FILEID");
					String cDay=tool.DateFormat(callTime, DAY_FORMATE);
					Double charge=0D;
					Double oldCharge=0D;
					Double dayCap=null;
					
					String pricplanID=msisdnMap.get(imsi).get("PRICEPLANID");
					
					Map<String,Map<String,Map<String,Object>>> map=new HashMap<String,Map<String,Map<String,Object>>>();
					Map<String,Map<String,Object>> map2=new HashMap<String,Map<String,Object>>();
					Map<String,Object> map3=new HashMap<String,Object>();
					
					
					//�P�_�O�_�i�H���������O�v��A�íp�⦹��CDR������(charge)
					if(pricplanID!=null && !"".equals(pricplanID) && !DEFAULT_MCCMNC.equals(mccmnc) &&
							dataRate.containsKey(pricplanID)&&dataRate.get(pricplanID).containsKey(mccmnc)){
						charge=volume*kByte*(Double)dataRate.get(pricplanID).get(mccmnc).get("RATE");
						dayCap=(Double)dataRate.get(pricplanID).get(mccmnc).get("DAYCAP");
					}else{
						//�S��PRICEPLANID(�믲���)�AMCCMNC�A�L�k�P�_�ϰ�~�̡A�@�k�G�έp�y�q�A
						//�S��������PRICEPLANID(�믲���)�AMCCMNC�A�L�k�P�_�ϰ�~��
						//�H�̤j�O�v�p�O
						charge=volume*kByte*maxRate;
					}
					
					//�N����CDR���G�A�^�g��USAGE TABLE
					pst2.setDouble(1, charge);
					pst2.setString(2, rs.getString("USAGEID"));
					pst2.addBatch();
					
					//��ݬO�_���H�s�b����ơA�����ܨ��X���֥[
					if(currentDayMap.containsKey(cDay)&& map.containsKey(imsi)&& map2.containsKey(mccmnc)){
						oldCharge=(Double)currentDayMap.get(cDay).get(imsi).get(mccmnc).get("CHARGE");
						charge=oldCharge+charge;
						
						if(!updateMapD.containsKey(cDay)){
							Set<String> se=new HashSet<String>();
							se.add(mccmnc);
							Map<String,Set<String>> sm= new HashMap<String,Set<String>>();
							sm.put(imsi, se);
							updateMapD.put(cDay, sm);
						}else{
							updateMapD.get(cDay).get(imsi).add(mccmnc);
						}
					}else{
						if(!insertMapD.containsKey(cDay)){
							Set<String> se=new HashSet<String>();
							se.add(mccmnc);
							Map<String,Set<String>> sm= new HashMap<String,Set<String>>();
							sm.put(imsi, se);
							insertMapD.put(cDay, sm);
						}else{
							insertMapD.get(cDay).get(imsi).add(mccmnc);
						}
					}
					
					
					//�p�G���p�O�W�u�A����̤j��
					if(dayCap!=null && charge>dayCap) charge=dayCap;
					
					//�N���G�O����currentDayMap
					map3.put("CHARGE", oldCharge+charge);
					map3.put("LAST_FILEID",fileID);
					map3.put("LAST_DATA_TIME",callTime);
					map3.put("VOLUME",volume);
					map2.put(mccmnc, map3);
					map.put(imsi, map2);
					currentDayMap.put(cDay, map);
					
					//��scurrentMap�A�p�G����CDR�O���ɶ���������s�b
					Double preCharge=0D;
					Integer smsTimes=0;
					String cMonth=cDay.substring(0,4);
					if(currentMap.containsKey(cMonth) && currentMap.get(cMonth).containsKey(imsi)){
						preCharge=(Double)currentMap.get(cMonth).get(imsi).get("CHARGE")-oldCharge;
						smsTimes=(Integer) currentMap.get(cMonth).get(imsi).get("SMS_TIMES");
						volume=(Double)currentMap.get(cMonth).get(imsi).get("VOLUME")+volume;
						
						if(!updateMap.containsKey(cMonth)){
							Set<String> se=new HashSet<String>();
							se.add(imsi);
							updateMap.put(cMonth, se);
						}else{
							updateMap.get(cMonth).add(imsi);
						}
					}else{
						if(!insertMap.containsKey(cMonth)){
							Set<String> se=new HashSet<String>();
							se.add(imsi);
							insertMap.put(cMonth, se);
						}else{
							insertMap.get(cMonth).add(imsi);
						}
					}
					
					Map<String,Object> map4=new HashMap<String,Object>();
					map4.put("LAST_FILEID", lastfileID);
					map4.put("SMS_TIMES", smsTimes);
					map4.put("LAST_DATA_TIME", callTime);
					map4.put("CHARGE", preCharge+charge);
					map4.put("VOLUME", volume);
					
					Map<String,Map<String,Object>> map5=new HashMap<String,Map<String,Object>>();
					map5.put(imsi, map4);
					currentMap.put(cMonth, map5);
				}
				pst2.executeUpdate();
				pst2.close();
				pst.close();
				rs.close();
			}
			closeConnect();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
			sendMail("At charge occur SQLException error!");
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
		
	}
	
	private void connectDB(){
		// �i��DB�s�u
		//conn=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		try {
			conn=tool.connDB(logger, props.getProperty("Oracle.DriverClass"), 
					props.getProperty("Oracle.URL")
					.replace("{{Host}}", props.getProperty("Oracle.Host"))
					.replace("{{Port}}", props.getProperty("Oracle.Port"))
					.replace("{{ServiceName}}", props.getProperty("Oracle.ServiceName")), 
					props.getProperty("Oracle.UserName"), 
					props.getProperty("Oracle.PassWord"));
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
		
		if (conn != null) {
			logger.debug("connect success!");
			startTime = System.currentTimeMillis();
			//�����۰�Commit
			cancelAutoCommit();
			//�]�w����϶�
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
			
			/*//���XHUR_DATARATE
			subStartTime = System.currentTimeMillis();
			setDataRate();
			logger.info("setDataRate execute time :"+(System.currentTimeMillis()-subStartTime));*/
			
			/*//���Xmsisdn��T
			subStartTime = System.currentTimeMillis();
			setMsisdnMap();
			logger.info("setMsisdnMap execute time :"+(System.currentTimeMillis()-subStartTime));*/
	
			/*//�}�l��� 
			subStartTime = System.currentTimeMillis();
			charge();
			logger.info("charge execute time :"+(System.currentTimeMillis()-subStartTime));
			//showCurrent();
*/
			/*//�o�eĵ��²�T
			subStartTime = System.currentTimeMillis();
			sendAlertSMS();
			logger.info("sendAlertSMS execute time :"+(System.currentTimeMillis()-subStartTime));
			*/
			/*//�^�g������G
			subStartTime = System.currentTimeMillis();
			insert();
			update();
			logger.info("insert��update execute time :"+(System.currentTimeMillis()-subStartTime));
*/
			
			show();
			// �{�����槹��
			endTime = System.currentTimeMillis();
			logger.info("Program execute time :" + (endTime - startTime));
			
			closeConnect();

		} else {
			logger.error("connect is null!");
		}
	}
	
	private void show(){
		//System.out.println("Show lastfileID"+" : ");
		System.out.println("lastfileID"+" : "+lastfileID);
		System.out.println("Show Current"+" : ");
		showCurrent();
		System.out.println("Show CurrentDay"+" : ");
		showCurrentDay();
		
	}
	
	private void showCurrent() {
		for(String mon : currentMap.keySet()){
			for(String imsi : currentMap.get(mon).keySet()){
				System.out.print(" mon"+" : "+mon);
				System.out.println(", imsi"+" : "+imsi);
				System.out.println(", CHARGE"+" : "+currentMap.get(mon).get(imsi).get("CHARGE"));
				System.out.println(", VOLUME"+" : "+currentMap.get(mon).get(imsi).get("VOLUME"));
				System.out.println(", LAST_FILEID"+" : "+currentMap.get(mon).get(imsi).get("LAST_FILEID"));
				System.out.println(", SMS_TIMES"+" : "+currentMap.get(mon).get(imsi).get("SMS_TIMES"));
				System.out.println(", LAST_DATA_TIME"+" : "+currentMap.get(mon).get(imsi).get("LAST_DATA_TIME"));
			}
		}
	}
	private void showCurrentDay() {
		for(String day : currentDayMap.keySet()){
			for(String imsi : currentDayMap.get(day).keySet()){
				for(String mccmnc : currentDayMap.get(day).get(imsi).keySet()){
					System.out.print(" day"+" : "+day);
					System.out.println(", imsi"+" : "+imsi);
					System.out.println(", mccmnc"+" : "+mccmnc);
					System.out.println(", CHARGE"+" : "+currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE"));
					System.out.println(", VOLUME"+" : "+currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME"));
					System.out.println(", LAST_FILEID"+" : "+currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID"));
					System.out.println(", LAST_DATA_TIME"+" : "+currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME"));
				}
			}
		}
	}
	
	
	
	/**
	 * �p�⧹����g�^��Ʈw-��s
	 */

	private void updateCurrentMap(String sql){
		logger.info("updateCurrentMap...");

		int[] result;
		int count=0;
		
		sql=
				"UPDATE HUR_CURRENT A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.SMS_TIMES=?,A.LAST_DATA_TIME=?,A.VOLUME=?,A.UPDATE_DATE=SYSDATE "
				+ "WHERE A.MONTH=? AND A.IMSI=? ";
		
		logger.info("Execute SQL :"+sql);
		
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String mon : updateMap.keySet()){
				for(String imsi : updateMap.get(mon)){
					if(count<dataThreshold){
						pst.setDouble(1,(Double) currentMap.get(mon).get(imsi).get("CHARGE"));
						pst.setInt(2, (Integer) currentMap.get(mon).get(imsi).get("LAST_FILEID"));
						pst.setInt(3, (Integer) currentMap.get(mon).get(imsi).get("SMS_TIMES"));
						pst.setDate(4, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentMap.get(mon).get(imsi).get("LAST_DATA_TIME")));
						pst.setDouble(5,(Double) currentMap.get(mon).get(imsi).get("VOLUME"));
						pst.setString(6, mon);
						pst.setString(7, imsi);//�㦳mccmnc
						pst.addBatch();
						count++;
					}else{
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

			conn.commit();
			pst.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Error at updateCurrentMap : "+e.getMessage());
			//sendMail
			sendMail("At updateCurrentMap occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	private void updateCurrentMapDay(String sql){
		logger.info("updateCurrentMapDay...");
		int[] result;
		int count=0;
		
		sql=
				"UPDATE HUR_CURRENT_DAY A "
				+ "SET A.CHARGE=?,A.LAST_FILEID=?,A.LAST_DATA_TIME=?,A.VOLUME=?,A.UPDATE_DATE=SYSDATE "
				+ "WHERE A.DAY=? AND A.IMSI=? AND A.MCCMNC=? ";
		
		logger.info("Execute SQL :"+sql);
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String day : updateMapD.keySet()){
				for(String imsi : updateMapD.get(day).keySet()){
					for(String mccmnc : updateMapD.get(day).get(imsi)){
						if(count<dataThreshold){
							pst.setDouble(1,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE"));
							pst.setInt(2, (Integer) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID"));
							pst.setDate(4, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME")));
							pst.setDouble(5,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME"));
							pst.setString(6, day);
							pst.setString(7, imsi);
							pst.setString(8, mccmnc);
							pst.addBatch();
							count++;
						}else{
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

			conn.commit();
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
	
	private void insertCurrentMap(String sql){
		logger.info("insertCurrentMap...");
		int[] result;
		int count=0;
		
		sql=
				"INSERT INTO HUR_CURRENT "
				+ "(IMSI,CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,VOLUME,MONTH,CREATE_DATE) "
				+ "VALUES(?,?,?,?,?,?,?,SYSDATE)";
		
		logger.info("Execute SQL :"+sql);
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			for(String mon : insertMap.keySet()){
				for(String imsi : insertMap.get(mon)){
					if(count<dataThreshold){
						pst.setString(1, imsi);		
						pst.setDouble(2,(Double) currentMap.get(mon).get(imsi).get("CHARGE"));
						pst.setInt(3,(Integer) currentMap.get(mon).get(imsi).get("LAST_FILEID"));
						pst.setInt(4, (Integer) currentMap.get(mon).get(imsi).get("SMS_TIMES"));
						pst.setDate(5, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentMap.get(mon).get(imsi).get("LAST_DATA_TIME")));
						pst.setDouble(6,(Double) currentMap.get(mon).get(imsi).get("VOLUME"));
						pst.setString(7, mon);
						pst.addBatch();
						count++;
					}else{
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
			conn.commit();
			pst.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Error at insertCurrentMap : "+e.getMessage());
			//sendMail
			sendMail("At insertCurrentMap occur SQLException error!");
			errorMsg=e.getMessage();
		}
	}
	private void insertCurrentMapDay(String sql){
		logger.info("insertCurrentMapDay...");
		int[] result;
		int count=0;
		
		sql=
				"INSERT INTO HUR_CURRENT_DAY "
				+ "(IMSI,CHARGE,LAST_FILEID,LAST_DATA_TIME,VOLUME,CREATE_DATE,MCCMNC,DAY) "
				+ "VALUES(?,?,?,?,?,SYSDATE,?,?)";
		
		logger.info("Execute SQL :"+sql);
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			for(String day : insertMapD.keySet()){
				for(String imsi : insertMapD.get(day).keySet()){
					for(String mccmnc : insertMapD.get(day).get(imsi)){
						if(count<dataThreshold){
							pst.setString(1, imsi);
							pst.setDouble(2,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("CHARGE"));
							pst.setInt(3, (Integer) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_FILEID"));
							pst.setDate(4, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentDayMap.get(day).get(imsi).get(mccmnc).get("LAST_DATA_TIME")));
							pst.setDouble(5,(Double) currentDayMap.get(day).get(imsi).get(mccmnc).get("VOLUME"));
							pst.setString(6, mccmnc);
							pst.setString(7, day);
							pst.addBatch();
							count++;
						}else{
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

			conn.commit();
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
			sql="SELECT A.ID,A.BRACKET,A.MEG,A.SUSPEND FROM HUR_SMS_SETTING A ORDER BY ID";	
			
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
		
		
		
		

		try {
			sql="INSERT INTO HUR_SMS_LOG"
					+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
					+ "VALUES(SMS_ID.NEXTVAL,?,?,?,?,SYSDATE)";
			
			 PreparedStatement pst = conn.prepareStatement(sql);
			 logger.info("Execute SQL :"+sql);
			 //�ˬd�o�Ӥ몺��Ƨ@ĵ�ܳq��
			for(String imsi: currentMap.get(sYearmonth).keySet()){
				
				//�p�G�S��������ơA�]���L�k�o�e²�T�A�H�eĵ�imail����L
				phone=(String) msisdnMap.get(imsi).get("SERVICECODE");
				if(phone==null ||"".equals(phone)){
					//sendMail
					sendMail("At sendAlertSMS occur error!<br>\n "
							+ "The IMSI:"+imsi+" can't find msisdn to send! ");
					continue;
				}
				
				//logger.info("For imsi="+imsi+" get phone number="+phone);
				String res="";
				
				Double charge=(Double) currentMap.get(sYearmonth).get(imsi).get("CHARGE");
				int smsTimes=(Integer) currentMap.get(sYearmonth).get(imsi).get("SMS_TIMES");
				
				for(int i=0;i<times.size();i++){
					if((smsTimes==times.get(i)-1)&&(charge>=bracket.get(i))){
						smsTimes++;
						//�H�e²�T
						logger.info("For "+imsi+" send "+smsTimes+"th message:"+msg.get(i));
						for(String s:msg.get(i).split(",")){
							String cont =processMag(content.get(s).get("COMTENT"),bracket.get(i));
							//TODO
							//WSDL�覡�I�s WebServer
							//result=tool.callWSDLServer(setSMSXmlParam(cont,phone));
							//WSDL�覡�I�s WebServer
							res=setSMSPostParam(cont,phone);
							
							logger.debug("send message result : "+res);						
							currentMap.get(sYearmonth).get(imsi).put("SMS_TIMES", smsTimes);
							smsCount++;
							if("1".equals(suspend.get(i))){
								suspend(imsi,phone);
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
							break;
						}
					}
				}	
			}
			logger.debug("Total send "+smsCount+" ...");
			logger.debug("Log to table...executeBatch");
			pst.executeBatch();
			pst.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error at sendAlertSMS : "+e.getMessage());
			//sendMail
			sendMail("At sendAlertSMS occur SQLException error!");
			errorMsg=e.getMessage();
		}catch(Exception e){
			e.printStackTrace();
			logger.error("Error at sendAlertSMS : "+e.getMessage());
			//sendMail
			sendMail("At sendAlertSMS occur Exception error!");
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
	private String processMag(String msg,Double bracket){
		
		msg=msg.replace("{{bracket}}", bracket.toString());
		
		return msg;
	}
	
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
	
	private String setSMSPostParam(String msg,String phone) throws IOException{
		StringBuffer sb=new StringBuffer ();
		
		String PhoneNumber=phone,Text=msg,charset="big5",InfoCharCounter=null,PID=null,DCS=null;
		PhoneNumber="886235253";
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
	
	private String getMsisdn(String imsi){
		logger.info("getMsisdn");
		sql=
				"SELECT A.SERVICECODE "
				+ "FROM SERVICE A,IMSI B "
				+ "WHERE A.SERVICEID=B.SERVICEID AND B.IMSI=?";
		String msisdn=null;
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, imsi);
			ResultSet rs=pst.executeQuery();
			
			while(rs.next()){
				msisdn=rs.getString("SERVICECODE");
			}
			rs.close();
			pst.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error at getMsisdn : "+e.getMessage());
			//sendMail
			sendMail("At getMsisdn occur SQLException error!");
			errorMsg=e.getMessage();
		}
		return msisdn;
	}
	/**
	 * ���X�i��Ψ쪺msisdn
	 * �إ�msisdnMap
	 * Key:imsi,Value:Map(MSISDN,PRICEPLANID)
	 */
	private void setMsisdnMap(){
		logger.info("setMsisdnMap...");
		sql=
				"SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID "
				+ "FROM SERVICE A,IMSI B "
				+ "WHERE A.SERVICEID=B.SERVICEID AND B.IMSI IN ?";
		String a="(";
		try {
			for(String imsi: currentMap.keySet()){
				a+=imsi+",";
			}			
			
			a+=")";
			a=a.replace(",)", ")");
			//logger.info("a:"+a);
			Statement st = conn.createStatement();
			
			ResultSet rs=st.executeQuery(sql.replace("?", a));
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("MSISDN", rs.getString("SERVICECODE"));
				map.put("PRICEPLANID", rs.getString("PRICEPLANID"));
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
		suspendGPRS sus=new suspendGPRS(conn,logger);
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
		mailSubject="RFP Warnning Mail";
		mailContent="Error :"+content+"<br>\n"
				+ "Error occurr time: "+tool.DateFormat()+"<br>\n"
				+ "SQL : "+sql+"<br>\n"
				+ "Error Msg : "+errorMsg;

		/*try {
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
		}*/
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		process();
	}
	
	public static void main(String[] args) {
		
		try {
			// Grab the Scheduler instance from the Factory
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

			// define the job and tie it to our HelloJob class ->
			// JobBuilder.newJob()
			JobDetail job = JobBuilder.newJob(RFPmain.class)
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
			
			
			
			
			/*JobDetail job = new JobDetail("job1", "group1", SayHelloJob.class);
			// �ѩ�quartz support�hgroup��hjob. �o�̧ڭ̥u���@��job. �ڭ̦ۤv���H�N�⥦�R�W.
			// ���ۦPgroup�̦p�G�X�{�ۦP��job�W,�|�Qoverrride.
			CronTrigger cTrigger = new CronTrigger("trigg1", "group1", "job1",
			"group1", "1/10 * * * * ?");
			// �o�̫��wtrigger���樺��group��job.
			// "1/10 * * * * ?" �P �bunix like�̪�crontab job���]�w����. �o�̪�ܨC�Ѹ̪��C10�����@��
			// Seconds Minutes Hours Day-of-Month Month Day-of-Week Year(optional field)
*/			
			
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}



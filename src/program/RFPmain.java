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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
	
	static int runInterval=1*60;//����
	
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
	
	private Date monthFirstDate=null;
	private Date monthLastDate=null;
	
	Map<String,Map<String,Object>> currentMap = new HashMap<String,Map<String,Object>>();
	Map<String,Map<String,Object>> currentMapU = new HashMap<String,Map<String,Object>>();
	Map<String,Map<String,Object>> dataRate = new HashMap<String,Map<String,Object>>();
	Map<String,String> msisdnMap = new HashMap<String,String>();
	
	List<String> updatList = new ArrayList<String>();
	List<String> updatListU = new ArrayList<String>();
	List<String> insertList = new ArrayList<String>();
	List<String> insertListU = new ArrayList<String>();
	
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
	private void setMonthDate(){
		logger.info("setMonthDate...");

		//�]�w�C�Ӥ�Ĥ@��
		monthFirstDate=tool.getMonthFirstDate(new Date());
		logger.debug("set monthFirstDate:"+monthFirstDate);
		
		//�]�w�C�Ӥ�̫�@��
		monthLastDate= tool.getMonthLastDate(new Date());
		logger.debug("set monthLastDate:"+monthLastDate);
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
	 * �إߦ�Map Key:IMSI,Value:Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME)
	 */
	private void setCurrentMap(){
		logger.info("setCurrentMap...");
		sql="SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.IS_UNKNOWN,A.LAST_DATA_TIME FROM HUR_CURRENT A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			logger.debug("Set current map...");
			while(rs.next()){
				Map<String,Object> map=new HashMap<String,Object>();
				String imsi =rs.getString("IMSI");
				//System.out.println("imsi : "+imsi);
				
				map.put("LAST_FILEID", rs.getInt("LAST_FILEID"));
				map.put("SMS_TIMES", rs.getInt("SMS_TIMES"));
				map.put("LAST_DATA_TIME", (rs.getDate("LAST_DATA_TIME")!=null?rs.getDate("LAST_DATA_TIME"):new Date()));
				//logger.debug("map.get('LAST_DATA_TIME'):"+map.get("LAST_DATA_TIME"));
	
				//�p�G�p�O�g���w�L�A�k�s
				if(monthFirstDate.before((Date) map.get("LAST_DATA_TIME")) && monthLastDate.after((Date) map.get("LAST_DATA_TIME"))){
					map.put("CHARGE", rs.getDouble("CHARGE"));
				}else{
					map.put("CHARGE", 0D);
					//�[�J�ݧ�s�C��
					if("0".equals(rs.getString("IS_UNKNOWN"))){
						updatList.add(imsi);
					}else{
						updatListU.add(imsi);
					}
				}
				//�N��Ƥ�����MCCMNC�P�LMCCMNC���
				if("0".equals(rs.getString("IS_UNKNOWN"))){
					currentMap.put(imsi,map);
				}else{
					currentMapU.put(imsi,map);
				}
			}
			st.close();
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
	 * ���X HUR_DATA_RATE
	 * �إߦ�MAP Key:MCCMNC,Value:Map(RATE,CHARGEUNIT,CURRENCY)
	 */
	private void setDataRate(){
		logger.info("setDataRate...");
		sql="SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY FROM HUR_DATA_RATE A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+sql);
			ResultSet rs = st.executeQuery(sql);
			
			
			
			logger.debug("Set dataRate map...");
			while(rs.next()){
				Map<String,Object> map=new HashMap<String,Object>();
				String mccmnc =rs.getString("MCCMNC");
				//System.out.println("mccmnc : "+mccmnc);
				map.put("RATE", rs.getDouble("RATE"));
				map.put("CHARGEUNIT", rs.getDouble("CHARGEUNIT"));
				map.put("CURRENCY", rs.getString("CURRENCY"));
				dataRate.put(mccmnc, map);
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
				
				while(rs.next()){
					/*System.out.println(j+"\t:\t"+rs.getString("USAGEID"));
					j++;*/
					
					String imsi= rs.getString("IMSI");
					String mccmnc=rs.getString("MCCMNC");
					Double volume=rs.getDouble("DATAVOLUME");
					Date callTime=rs.getDate("CALLTIME");
					Integer fileID=rs.getInt("FILEID");
					
					if(mccmnc==null||"".equals(mccmnc)){
						//mccmnc ���šACharge�@��Volume�ϥ�
						//logger.info("setting IMSI="+imsi+" MCCMNC is NULL ...");
						if(currentMapU.containsKey(imsi)){
							
							currentMapU.get(imsi).put("CHARGE", (Double)currentMapU.get(imsi).get("CHARGE")+volume);
							currentMapU.get(imsi).put("LAST_FILEID",fileID);
							currentMapU.get(imsi).put("LAST_DATA_TIME",callTime);
							if(!updatListU.contains(imsi))updatListU.add(imsi);
						}else{
							Map<String,Object> map=new HashMap<String,Object>();
							map.put("CHARGE", volume);
							map.put("LAST_FILEID", fileID);
							map.put("LAST_DATA_TIME",callTime);
							map.put("SMS_TIMES", 0);
							currentMapU.put(imsi, map);
							if(!insertListU.contains(imsi)) insertListU.add(imsi);
						}
						//20141013 �M�w���ť�MCCMNC�B�z�覡�P�����L�k�����ۦP
						//logger.info("setting IMSI="+imsi+" MCCMNC CANNOT COMPARE ...");
						Double charge=volume*kByte*maxRate;
						
						if(currentMap.containsKey(imsi)){
							currentMap.get(imsi).put("CHARGE", (Double)currentMap.get(imsi).get("CHARGE")+charge);
							currentMap.get(imsi).put("LAST_FILEID",fileID);
							currentMap.get(imsi).put("LAST_DATA_TIME",callTime);
							if(!updatList.contains(imsi))updatList.add(imsi);
						}else{
							Map<String,Object> map=new HashMap<String,Object>();
							map.put("CHARGE", charge);
							map.put("LAST_FILEID", fileID);
							map.put("LAST_DATA_TIME",callTime);
							map.put("SMS_TIMES", 0);
							currentMap.put(imsi, map);
							if(!insertList.contains(imsi)) insertList.add(imsi);
						}

					}else if(!dataRate.containsKey(mccmnc)){
						//mccmnc�L�k�t��A�ϥγ̰�������(maxRate)�p��
						//logger.info("setting IMSI="+imsi+" MCCMNC CANNOT COMPARE ...");
						Double charge=volume*kByte*maxRate;
						
						if(currentMap.containsKey(imsi)){
							currentMap.get(imsi).put("CHARGE", (Double)currentMap.get(imsi).get("CHARGE")+charge);
							logger.info("get current charge:"+currentMap.get(imsi).get("CHARGE"));
							currentMap.get(imsi).put("LAST_FILEID",fileID);
							currentMap.get(imsi).put("LAST_DATA_TIME",callTime);
							if(!updatList.contains(imsi))updatList.add(imsi);
						}else{
							Map<String,Object> map=new HashMap<String,Object>();
							map.put("CHARGE", charge);
							map.put("LAST_FILEID", fileID);
							map.put("LAST_DATA_TIME",callTime);
							map.put("SMS_TIMES", 0);
							currentMap.put(imsi, map);
							if(!insertList.contains(imsi)) insertList.add(imsi);
						}
					}else{
						//mccmnc�i�H�t��A
						//logger.info("setting IMSI="+imsi+"...");
	
						Double rate=(Double) dataRate.get(mccmnc).get("RATE");
						Double chargeunit=(Double) dataRate.get(mccmnc).get("CHARGEUNIT");
						String currency=(String) dataRate.get(mccmnc).get("CURRENCY");
						
						Double charge=volume*kByte*(rate/chargeunit);
						
						if("HKD".equals(currency)) charge*=exchangeRate;
						
						if(currentMap.containsKey(imsi)){
							currentMap.get(imsi).put("CHARGE", (Double)currentMap.get(imsi).get("CHARGE")+charge);
							logger.info("get current charge:"+currentMap.get(imsi).get("CHARGE"));
							currentMap.get(imsi).put("LAST_FILEID",fileID);
							currentMap.get(imsi).put("LAST_DATA_TIME",callTime);
							if(!updatList.contains(imsi))updatList.add(imsi);
						}else{
							Map<String,Object> map=new HashMap<String,Object>();
							map.put("CHARGE", charge);
							map.put("LAST_FILEID", fileID);
							map.put("LAST_DATA_TIME",callTime);
							map.put("SMS_TIMES", 0);
							currentMap.put(imsi, map);
							if(!insertList.contains(imsi)) insertList.add(imsi);
						}
					}
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
	
	//TODO
	private void process() {
		// �{���}�l�ɶ�
		long startTime;
		// �{�������ɶ�
		long endTime;
		// �Ƶ{���}�l�ɶ�
		long subStartTime;
		// ��l��log
		// iniLog4j();
		loadProperties();

		logger.info("RFP Program Start! "+new Date());
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
		
		
		if (conn != null) {
			logger.debug("connect success!");
			startTime = System.currentTimeMillis();
			//�����۰�Commit
			cancelAutoCommit();
			//�]�w����϶�
			setMonthDate();
			//���o�̫��s��FileID
			subStartTime = System.currentTimeMillis();
			setLastFileID();
			logger.info("setLastFileID execute time :"+(System.currentTimeMillis()-subStartTime));
			//���XHUR_CURRENT
			subStartTime = System.currentTimeMillis();
			setCurrentMap();
			logger.info("setCurrentMap execute time :"+(System.currentTimeMillis()-subStartTime));
			//���XHUR_DATARATE
			subStartTime = System.currentTimeMillis();
			setDataRate();
			logger.info("setDataRate execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//�}�l��� 
			subStartTime = System.currentTimeMillis();
			charge();
			logger.info("charge execute time :"+(System.currentTimeMillis()-subStartTime));

			showCurrent();
			
			
			//���Xmsisdn��T
			subStartTime = System.currentTimeMillis();
			setMsisdnMap();
			logger.info("setMsisdnMap execute time :"+(System.currentTimeMillis()-subStartTime));
			//�o�eĵ��²�T
			subStartTime = System.currentTimeMillis();
			sendAlertSMS();
			logger.info("sendAlertSMS execute time :"+(System.currentTimeMillis()-subStartTime));
			
			//�^�g������G
			subStartTime = System.currentTimeMillis();
			insert();
			update();
			logger.info("insert��update execute time :"+(System.currentTimeMillis()-subStartTime));

			// �{�����槹��
			endTime = System.currentTimeMillis();
			logger.info("Program execute time :" + (endTime - startTime));
			
			closeConnect();

		} else {
			logger.error("connect is null!");
		}
	}
	
	private void showCurrent() {
		for (String s : currentMap.keySet()) {
			System.out.println("IMSI:" + s + ",\t\tCHARGE:"
					+ currentMap.get(s).get("CHARGE") + ",\t\tLAST_FILEID:"
					+ currentMap.get(s).get("LAST_FILEID")+",\t\tTimes:"
					+ currentMap.get(s).get("SMS_TIMES"));
		}
		for (String s : currentMapU.keySet()) {
			System.out.println("IMSI:" + s + ",\t\tVOLUME:"
					+ currentMapU.get(s).get("CHARGE") + ",\t\tLAST_FILEID:"
					+ currentMapU.get(s).get("LAST_FILEID")+",\t\tTimes:"
					+ currentMapU.get(s).get("SMS_TIMES"));
		}
	}
	
	/**
	 * �p�⧹����g�^��Ʈw-��s
	 */
	private void update(){
		logger.info("Update...");
		sql=
				"UPDATE HUR_CURRENT "
				+ "SET CHARGE=?,LAST_FILEID=?,SMS_TIMES=?,LAST_DATA_TIME=?,UPDATE_DATE=SYSDATE "
				+ "WHERE IMSI=? AND IS_UNKNOWN=? ";
		
		logger.info("Execute SQL :"+sql);
		updateCurrentMap(sql);
		updateCurrentMapU(sql);
	}

	private void updateCurrentMap(String sql){
		logger.info("updateCurrentMap...");

		int[] result;
		int count=1;
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String imsi : updatList){
				if(count<=dataThreshold){
					pst.setDouble(1,(Double) currentMap.get(imsi).get("CHARGE"));
					pst.setInt(2, (Integer) currentMap.get(imsi).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMap.get(imsi).get("SMS_TIMES"));
					pst.setDate(4, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentMap.get(imsi).get("LAST_DATA_TIME")));
					pst.setString(5, imsi);
					pst.setInt(6, 0);//�㦳mccmnc
					pst.addBatch();
					count++;
				}else{
					logger.info("Execute updateCurrentMap Batch");
					result=pst.executeBatch();
					count=1;
				}
			}
			if(count!=1){
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
	private void updateCurrentMapU(String sql){
	
		int[] result;
		int count=1;
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String imsi : updatListU){
				if(count<=dataThreshold){
					pst.setDouble(1,(Double) currentMapU.get(imsi).get("CHARGE"));
					pst.setInt(2, (Integer) currentMapU.get(imsi).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMapU.get(imsi).get("SMS_TIMES"));
					pst.setDate(4, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentMapU.get(imsi).get("LAST_DATA_TIME")));
					pst.setString(5, imsi);
					pst.setInt(6, 1);//���㦳mccmnc
					pst.addBatch();
					count++;
				}else{
					logger.info("Execute updateCurrentMapU Batch");
					result=pst.executeBatch();
					count=1;
				}
			}
			if(count!=1){
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
	private void insert(){
		logger.info("insert...");
		sql=
				"INSERT INTO HUR_CURRENT"
				+ "(IMSI,CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,IS_UNKNOWN,CREATE_DATE) "
				+ "VALUES(?,?,?,?,?,?,SYSDATE)";
		logger.info("Execute SQL :"+sql);
		insertCurrentMap(sql);
		insertCurrentMapU(sql);
	}
	
	private void insertCurrentMap(String sql){
		logger.info("insertCurrentMap...");
		int[] result;
		int count=1;
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String imsi : insertList){
				if(count<=dataThreshold){
					pst.setString(1, imsi);		
					pst.setDouble(2,(Double) currentMap.get(imsi).get("CHARGE"));
					pst.setInt(3,(Integer) currentMap.get(imsi).get("LAST_FILEID"));
					pst.setInt(4, (Integer) currentMap.get(imsi).get("SMS_TIMES"));
					pst.setDate(5, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentMap.get(imsi).get("LAST_DATA_TIME")));
					pst.setInt(6, 0);//�㦳mccmnc
					pst.addBatch();
					count++;
				}else{
					logger.info("Execute insertCurrentMap Batch");
					result=pst.executeBatch();
					count=1;
				}
			}
			if(count!=1){
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
	private void insertCurrentMapU(String sql){
		logger.info("insertCurrentMapU...");
		int[] result;
		int count=1;
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String imsi : insertListU){
				if(count<=dataThreshold){
					pst.setString(1, imsi);		
					pst.setDouble(2,(Double) currentMapU.get(imsi).get("CHARGE"));
					pst.setInt(3, (Integer) currentMapU.get(imsi).get("LAST_FILEID"));
					pst.setInt(4, (Integer) currentMapU.get(imsi).get("SMS_TIMES"));
					pst.setDate(5, tool.convertJaveUtilDate_To_JavaSqlDate((Date) currentMapU.get(imsi).get("LAST_DATA_TIME")));
					pst.setInt(6, 1);//�㦳mccmnc
					pst.addBatch();
					count++;
				}else{
					result=pst.executeBatch();
					logger.info("Execute insertCurrentMapU Batch");
					count=1;
				}
			}
			if(count!=1){
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
	/**
	 * �̷��B�׻ݨD�o�eĵ��²�T
	 * �Ĥ@���A�B�פ@�A�T���@
	 * 
	 */
	private void sendAlertSMS(){
		logger.info("sendAlertSMS...");
		
		sql="SELECT A.ID,A.BRACKET,A.MEG,A.SUSPEND FROM HUR_SMS_SETTING A ORDER BY ID";	
		
		List<Integer> times=new ArrayList<Integer>();
		List<Double> bracket=new ArrayList<Double>();
		List<String> msg=new ArrayList<String>();
		List<String> suspend=new ArrayList<String>();
		
		int smsCount=0;
		String phone=null;
		try {
			Statement st =conn.createStatement();
			logger.debug("Execute SQL:"+sql);
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				times.add(rs.getInt("ID"));
				bracket.add(rs.getDouble("BRACKET"));
				msg.add(processMag(rs.getString("MEG"),rs.getDouble("BRACKET")));
				suspend.add(rs.getString("SUSPEND"));
				logger.info("times:"+times.get(times.size()-1)+",bracket:"+bracket.get(bracket.size()-1)+",msg:"+msg.get(msg.size()-1)+",suspend:"+suspend.get(suspend.size()-1));
			}
			
			if(times.size()==0){
				logger.error("No SMS Setting!");
				return;
			}
			sql="INSERT INTO HUR_SMS_LOG"
					+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
					+ "VALUES(SMS_ID.NEXTVAL,?,?,?,?,SYSDATE)";
			
			 PreparedStatement pst = conn.prepareStatement(sql);
			 logger.info("Execute SQL :"+sql);
			
			for(String imsi: currentMap.keySet()){
				
				phone=msisdnMap.get(imsi);
				if(phone==null ||"".equals(phone)){
					//sendMail
					sendMail("At sendAlertSMS occur error!<br>\n "
							+ "The IMSI:"+imsi+" can't find msisdn to send! ");
					continue;
				}
				
				//logger.info("For imsi="+imsi+" get phone number="+phone);
				String result="";
				
				Double charge=(Double) currentMap.get(imsi).get("CHARGE");
				int smsTimes=(Integer) currentMap.get(imsi).get("SMS_TIMES");
				
				for(int i=0;i<times.size();i++){
					if((smsTimes==times.get(i)-1)&&(charge>=bracket.get(i))){
						smsTimes++;
						//�H�e²�T
						logger.info("For "+imsi+" send "+smsTimes+"th message:"+msg.get(i));
						//TODO
						//result=tool.callWSDLServer(setParam(msg.get(i),phone));
						logger.debug("send message result : "+result);						
						currentMap.get(imsi).put("SMS_TIMES", smsTimes);
						smsCount++;
						if("1".equals(suspend.get(i))){
							suspend(imsi,phone);
						}

						//�g�J��Ʈw
						pst.setString(1, phone);
						pst.setString(2, msg.get(i));
						pst.setDate(3,tool.convertJaveUtilDate_To_JavaSqlDate(new Date()));
						pst.setString(4, result);
						pst.addBatch();
						
						//HUR_Current �ݧ�s
						//�p�G�O�s��ơAinsertList�|�w����ơA�S����ܬ��¸�ơA����T�{�O�_�w���Oupdate
						if(!insertList.contains(imsi)&&!updatList.contains(imsi))updatList.add(imsi);
						break;
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
	
	private String setParam(String msg,String phone){
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
	 * Key:imsi,Value:msisdn
	 */
	private void setMsisdnMap(){
		logger.info("setMsisdnMap...");
		sql=
				"SELECT B.IMSI,A.SERVICECODE "
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
				msisdnMap.put(rs.getString("IMSI"), rs.getString("SERVICECODE"));
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
	
	
}



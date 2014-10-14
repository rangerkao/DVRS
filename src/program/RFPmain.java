package program;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class RFPmain {

	
	//DB config
	private  final String DriverClass="oracle.jdbc.driver.OracleDriver";
	private  final String Host="10.42.1.101";
	private  final String Port="1521";
	private  final String ServiceName=":S2TBSDEV";
	private  final String UserName="foyadev";
	private  final String PassWord="foyadev";

	private  final String URL = "jdbc:oracle:thin:@"+ Host + ":"+Port+ServiceName; 
	
	Connection conn = null;
	 
	private  Logger logger ;
	Properties props=new Properties();

	
	//mail conf
	String mailSender="";
	String mailReceiver="k1988242001@gmail.com";
	String mailSubject="mail test";
	String mailContent="mail content text";
	
	IJatool tool=new Jatool();
	
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
		try {
			props.load(new   FileInputStream(RFPmain.class.getResource("").toString().replace("file:/", "")+"/log4j.properties"));
			PropertyConfigurator.configure(props);
			logger =Logger.getLogger(RFPmain.class);
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
			System.out.println("File Not Found : "+e.getMessage());
			System.out.println("File Path : "+RFPmain.class.getResource("").toString().replace("file:/", "")+"/log4j.properties");
		} catch (IOException e) {
			
			e.printStackTrace();
			System.out.println("IOException : "+e.getMessage());
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
			}

		}
	}
	
	/**
	 * �]�w�p�O�g��
	 * ���S�w������Ӥ몺�A�e���[�Wcalendar.setTime(date);�]�wdate���
	 */
	private void setMonthDate(){
		logger.info("setMonthDate...");
		Calendar calendar = Calendar.getInstance();
		//�]�w�C�Ӥ�Ĥ@��
		calendar.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DATE));
		monthFirstDate=calendar.getTime();
		//calendar.clear();
		logger.debug("set monthFirstDate:"+monthFirstDate);
		
		//�]�w�C�Ӥ�̫�@��
		calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
		monthLastDate= calendar.getTime();
		calendar.clear();
		logger.debug("set monthLastDate:"+monthLastDate);
	}
	
	/**
	 * �M��̫�@����諸fileID
	 */
	private void setLastFileID(){
		logger.info("setLastFileID...");
		String searchLastID="SELECT MAX(A.LAST_FILEID) id FROM HUR_CURRENT A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+searchLastID);
			ResultSet rs = st.executeQuery(searchLastID);
			
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
		}
	}
	
	/**
	 * ���X HUR_CURRENTE table���
	 * �إߦ�Map Key:IMSI,Value:Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME)
	 */
	private void setCurrentMap(){
		logger.info("setCurrentMap...");
		String queryCurrent="SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.IS_UNKNOWN,A.LAST_DATA_TIME FROM HUR_CURRENT A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+queryCurrent);
			ResultSet rs = st.executeQuery(queryCurrent);
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
		}
	}
	/**
	 * ���X HUR_DATA_RATE
	 * �إߦ�MAP Key:MCCMNC,Value:Map(RATE,CHARGEUNIT,CURRENCY)
	 */
	private void setDataRate(){
		logger.info("setDataRate...");
		String queryDataRate="SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY FROM HUR_DATA_RATE A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+queryDataRate);
			ResultSet rs = st.executeQuery(queryDataRate);
			
			
			
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
		}
	}
	
	/**
	 * ���o��Ƶ���
	 * @return
	 */
	private int dataCount(){
		logger.info("dataCount...");
		String queryCount="SELECT COUNT(1) count  FROM HUR_DATA_USAGE A WHERE A.FILEID>= ? ";
		int count=0;
		//��X�`�q
		PreparedStatement pst;
		try {
			pst = conn.prepareStatement(queryCount);
			pst.setInt(1, lastfileID+1);
			
			ResultSet rs = pst.executeQuery();
			logger.debug("Execute SQL : "+queryCount);
			
			while(rs.next()){
				count=rs.getInt("count");
			}
			logger.info("usage count : " +count);
			pst.close();
			rs.close();
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
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
		
		String queryMaxRate=
				"SELECT MAX(CASE WHEN A.CURRENCY = 'HKD' THEN A.RATE/A.CHARGEUNIT*"+exchangeRate+" ELSE  A.RATE/A.CHARGEUNIT END)  max "
				+ "FROM HUR_DATA_RATE A ";
		//��X�̶Q����
		try {
			Statement st = conn.createStatement();
			
			ResultSet rs = st.executeQuery(queryMaxRate);
			logger.debug("Execute SQL : "+queryMaxRate);
			
			while(rs.next()){
				max=rs.getDouble("max");
			}
			logger.info("Max Rate : " +max+" TWD ");
			st.close();
			rs.close();
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
		}
		
		return max;
	}
	
	/**
	 * �}�l���
	 */
	private void charge(){
		logger.info("charge...");
		String queryUsage=
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
			logger.debug("Execute SQL : "+queryUsage);
			
			for(int i=1;(i-1)*dataThreshold+1<count ;i++){
				PreparedStatement pst = conn.prepareStatement(queryUsage);
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
			//send email
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

		logger.info("RFP Program Start!");
		
		// �i��DB�s�u
		conn=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		
		
		
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

			/*for(String s : currentMap.keySet()){
				System.out.println(
						"IMSI:"+s+",\t\tCHARGE:"+currentMap.get(s).get("CHARGE")+",\t\tLAST_FILEID:"+currentMap.get(s).get("LAST_FILEID"));
			}
			for(String s : currentMapU.keySet()){
				System.out.println(
						"IMSI:"+s+",\t\tVOLUME:"+currentMapU.get(s).get("CHARGE")+",\t\tLAST_FILEID:"+currentMapU.get(s).get("LAST_FILEID"));
			}*/
			//���Xmsisdn��T
			setMsisdnMap();
			//�o�eĵ��²�T
			sendAlertSMS();
			
			//�^�g������G
			insert();
			update();

			// �{�����槹��
			endTime = System.currentTimeMillis();
			logger.info("Program execute time :" + (endTime - startTime));
			
			closeConnect();

		} else {
			logger.error("connect is null!");
		}
	}
	
	/**
	 * �p�⧹����g�^��Ʈw-��s
	 */
	private void update(){
		logger.info("Update...");
		String updateCurrent=
				"UPDATE HUR_CURRENT "
				+ "SET CHARGE=?,LAST_FILEID=?,SMS_TIMES=?,LAST_DATA_TIME=?,UPDATE_DATE=SYSDATE "
				+ "WHERE IMSI=? AND IS_UNKNOWN=? ";
		
		logger.info("Execute SQL :"+updateCurrent);
		updateCurrentMap(updateCurrent);
		updateCurrentMapU(updateCurrent);
	}

	private void updateCurrentMap(String sql){
		logger.info("updateCurrentMap...");

		int[] result;
		int count=1;
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			
			for(String imsi : updatList){
				if(count<=dataThreshold){
					Date date=(Date) currentMap.get(imsi).get("LAST_DATA_TIME") ;
					pst.setDouble(1,(Double) currentMap.get(imsi).get("CHARGE"));
					pst.setInt(2, (Integer) currentMap.get(imsi).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMap.get(imsi).get("SMS_TIMES"));
					pst.setDate(4, new java.sql.Date(date.getTime()));
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
			//send mail
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
					pst.setDate(4, (java.sql.Date) currentMapU.get(imsi).get("LAST_DATA_TIME"));
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
			//send mail
		}
	}
	
	/**
	 * �p�⧹����g�^��Ʈw-�s�W
	 */
	private void insert(){
		logger.info("insert...");
		String insertCurrent=
				"INSERT INTO HUR_CURRENT"
				+ "(IMSI,CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME,IS_UNKNOWN,CREATE_DATE) "
				+ "VALUES(?,?,?,?,?,?,SYSDATE)";
		logger.info("Execute SQL :"+insertCurrent);
		insertCurrentMap(insertCurrent);
		insertCurrentMapU(insertCurrent);
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
					pst.setDate(5, (java.sql.Date) currentMap.get(imsi).get("LAST_DATA_TIME"));
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
			//send mail
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
					pst.setDate(5, (java.sql.Date) currentMapU.get(imsi).get("LAST_DATA_TIME"));
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
			//send mail
		}
	}

	public static void main(String[] args) {

		RFPmain rf = new RFPmain();
		rf.process();
	}
	/**
	 * �̷��B�׻ݨD�o�eĵ��²�T
	 * �Ĥ@���A�B�פ@�A�T���@
	 * 
	 */
	private void sendAlertSMS(){
		logger.info("sendAlertSMS...");
		
		String querySMSSetting="SELECT A.ID,A.BRACKET,A.MEG,A.SUSPEND FROM HUR_SMS_SETTING A";
		String insertSMSLog=
				"INSERT INTO HUR_SMS_LOG"
				+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
				+ "VALUES(SMS_ID.NEXTVAL,?,?,?,?,SYSDATE)";
		
		List<Integer> times=new ArrayList<Integer>();
		List<Double> bracket=new ArrayList<Double>();
		List<String> msg=new ArrayList<String>();
		List<String> suspend=new ArrayList<String>();
		
		int smsCount=0;
		String phone=null;
		try {
			Statement st =conn.createStatement();
			logger.debug("Execute SQL:"+querySMSSetting);
			ResultSet rs=st.executeQuery(querySMSSetting);
			
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
			
			 PreparedStatement pst = conn.prepareStatement(insertSMSLog);
			 logger.info("Execute SQL :"+insertSMSLog);
			
			for(String imsi: currentMap.keySet()){
				
				phone=msisdnMap.get(imsi);
				if(phone==null ||"".equals(phone)){
					//send mail
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
						pst.setDate(3,new java.sql.Date(new Date().getTime()) );
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
		}catch(Exception e){
			e.printStackTrace();
			logger.error("Error at sendAlertSMS : "+e.getMessage());
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
				+ "	<REMARK>�Ƶ����</REMARK>"
				+ "</SMSREQUEST>");
		
		return sb.toString();
	}
	
	private String getMsisdn(String imsi){
		logger.info("getMsisdn");
		String queryimsi=
				"SELECT A.SERVICECODE "
				+ "FROM SERVICE A,IMSI B "
				+ "WHERE A.SERVICEID=B.SERVICEID AND B.IMSI=?";
		String msisdn=null;
		try {
			PreparedStatement pst = conn.prepareStatement(queryimsi);
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
			//send mail
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
		String queryimsi=
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
			
			ResultSet rs=st.executeQuery(queryimsi.replace("?", a));
			
			while(rs.next()){
				msisdnMap.put(rs.getString("IMSI"), rs.getString("SERVICECODE"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error at getMsisdn : "+e.getMessage());
			//send mail
		}
	}
	
	private void suspend(String imsi,String msisdn){
		suspendGPRS sus=new suspendGPRS(conn,logger);
		try {
			sus.ReqStatus_17_Act(imsi, msisdn);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error at suspend : "+e.getMessage());
			//send mail
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error at suspend : "+e.getMessage());
			//send mail
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("Error at suspend : "+e.getMessage());
			//send mail
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error at suspend : "+e.getMessage());
			//send mail
		}
	}
	
	
}



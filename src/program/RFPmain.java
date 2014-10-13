package program;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

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
	private final int exchangeRate=4; //港幣對台幣匯率，暫訂為4
	private final int kByte=1/1024;//RATE單位KB，USAGE單位B
	
	private Date monthFirstDate=null;
	private Date monthLastDate=null;
	
	Map<String,Map<String,Object>> currentMap = new HashMap<String,Map<String,Object>>();
	Map<String,Map<String,Object>> currentMapU = new HashMap<String,Map<String,Object>>();
	Map<String,Map<String,Object>> dataRate = new HashMap<String,Map<String,Object>>();
	List<String> updatList = new ArrayList<String>();
	List<String> updatListU = new ArrayList<String>();
	List<String> insertList = new ArrayList<String>();
	List<String> insertListU = new ArrayList<String>();
	
	/**
	 * 初始化
	 * 載入Log4j Properties
	 */
	private  void iniLog4j(){
		System.out.println("initial Log4g, property at "+RFPmain.class.getResource(""));
		PropertyConfigurator.configure(RFPmain.class.getResource("").toString().replace("file:/", "")+"Log4j.properties");
		logger =Logger.getLogger(RFPmain.class);
	}
	
	/**
	 * 初始化
	 * 載入Log4j Properties
	 * 同時載入參數porps
	 */
	private  void loadProperties(){
		System.out.println("initial Log4g, property !");
		try {
			props.load(new   FileInputStream(RFPmain.class.getResource("").toString().replace("file:/", "")+"/log4j.properties"));
			PropertyConfigurator.configure(props);
			logger =Logger.getLogger(RFPmain.class);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("File Not Found : "+e.getMessage());
			System.out.println("File Path : "+RFPmain.class.getResource("").toString().replace("file:/", "")+"/log4j.properties");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("IOException : "+e.getMessage());
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
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.debug("close ResultSet Error : "+e.getMessage());
			}

		}
	}
	
	/**
	 * 設定計費週期
	 * 取特定日期那個月的，前面加上calendar.setTime(date);設定date日期
	 */
	private void setMonthDate(){
		Calendar calendar = Calendar.getInstance();
		//設定每個月第一天
		calendar.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DATE));
		monthFirstDate=calendar.getTime();
		calendar.clear();
		
		//設定每個月最後一天
		calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
		monthLastDate= calendar.getTime();
		calendar.clear();
	}
	
	/**
	 * 尋找最後一次更改的fileID
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
		}
	}
	
	/**
	 * 取出 HUR_CURRENTE table資料
	 * 建立成Map Key:IMSI,Value:Map(CHARGE,LAST_FILEID,SMS_TIMES,LAST_DATA_TIME)
	 */
	private void setCurrentMap(){
		logger.info("setCurrentMap...");
		String queryCurrent="SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.IS_UNKNOWN,A.LAST_DATA_TIME FROM HUR_CURRENT A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+queryCurrent);
			ResultSet rs = st.executeQuery(queryCurrent);
			
			Map<String,Object> map=new HashMap<String,Object>();
			
			logger.debug("Set current map...");
			while(rs.next()){
				String imsi =rs.getString("IMSI");
				//System.out.println("imsi : "+imsi);
				
				map.put("LAST_FILEID", rs.getInt("LAST_FILEID"));
				map.put("SMS_TIMES", rs.getInt("SMS_TIMES"));
				map.put("LAST_DATA_TIME", rs.getDate("LAST_DATA_TIME"));
				
				//如果計費週期已過，歸零
				if(monthFirstDate.after((Date) map.get("LAST_DATA_TIME")) && monthLastDate.before((Date) map.get("LAST_DATA_TIME"))){
					map.put("CHARGE", rs.getDouble("CHARGE"));
				}else{
					map.put("CHARGE", 0D);
					//加入需更新列表中
					if("0".equals(rs.getString("IS_UNKNOWN"))){
						updatList.add(imsi);
					}else{
						updatListU.add(imsi);
					}
				}

				//將資料分為有MCCMNC與無MCCMNC兩種
				if("0".equals(rs.getString("IS_UNKNOWN"))){
					currentMap.put(imsi,map);
				}else{
					currentMapU.put(imsi,map);
				}
			}
			st.close();
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
		}
	}
	/**
	 * 取出 HUR_DATA_RATE
	 * 建立成MAP Key:MCCMNC,Value:Map(RATE,CHARGEUNIT,CURRENCY)
	 */
	private void setDataRate(){
		logger.info("setDataRate...");
		String queryDataRate="SELECT A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY FROM HUR_DATA_RATE A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+queryDataRate);
			ResultSet rs = st.executeQuery(queryDataRate);
			
			Map<String,Object> map=new HashMap<String,Object>();
			
			logger.debug("Set dataRate map...");
			while(rs.next()){
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
		}
	}
	
	/**
	 * 取得資料筆數
	 * @return
	 */
	private int dataCount(){
		logger.info("dataCount...");
		String queryCount="SELECT COUNT(1) count  FROM HUR_DATA_USAGE A WHERE A.FILEID>= ? ";
		int count=0;
		//找出總量
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
		}
		return count;
	}
	
	/**
	 * 取得最大計費比率，對MCCNOC有卻無法對應資料計費
	 * @return
	 */
	private double maxRate(){
		logger.info("maxRate...");
		double max=0;
		
		String queryMaxRate=
				"SELECT MAX(CASE WHEN A.CURRENCY = 'HKD' THEN A.RATE/A.CHARGEUNIT*"+exchangeRate+" ELSE  A.RATE/A.CHARGEUNIT END)  max "
				+ "FROM HUR_DATA_RATE A ";
		//找出最貴價格
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
		}
		
		return max;
	}
	
	/**
	 * 開始批價
	 */
	private void charge(){
		logger.info("charge...");
		String queryUsage=
				"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,CALLTIME "
				+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME "
				+ "			FROM HUR_DATA_USAGE A WHERE A.FILEID>=? AND ROWNUM <= ?  ORDER BY A.USAGEID,A.FILEID) "
				+ "MINUS "
				+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID "
				+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID,to_date(CALLTIME,'yyyy/MM/dd hh24:mi:ss') CALLTIME "
				+ "			FROM HUR_DATA_USAGE A WHERE A.FILEID>=? AND ROWNUM <= ?  ORDER BY A.USAGEID,A.FILEID)";


		int count=0;
		double maxRate=0;
		int j=1;
		try {
			count=dataCount();
			maxRate=maxRate();
			//批次Query 避免ram空間不足
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
						//mccmnc 為空，Charge作為Volume使用
						logger.info("setting IMSI="+imsi+" MCCMNC is NULL ...");
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
						//20141013 決定對於空白MCCMNC處理方式與有但無法對應相同
						logger.info("setting IMSI="+imsi+" MCCMNC CANNOT COMPARE ...");
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
						//mccmnc無法配對，使用最高的價位(maxRate)計算
						logger.info("setting IMSI="+imsi+" MCCMNC CANNOT COMPARE ...");
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
					}else{
						//mccmnc可以配對，
						logger.info("setting IMSI="+imsi+"...");
	
						Double rate=(Double) dataRate.get(mccmnc).get("RATE");
						Double chargeunit=(Double) dataRate.get(mccmnc).get("CHARGEUNIT");
						String currency=(String) dataRate.get(mccmnc).get("CURRENCY");
						
						Double charge=volume*kByte*(rate/chargeunit);
						
						if("HKD".equals(currency)) charge*=exchangeRate;
						
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
					}
				}
				pst.close();
				rs.close();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	private void process() {
		// 程式開始時間
		long startTime;
		// 程式結束時間
		long endTime;
		// 副程式開始時間
		long subStartTime;
		// 初始化log
		// iniLog4j();
		loadProperties();

		logger.info("RFP Program Start!");
		
		// 進行DB連線
		conn=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		
		
		
		if (conn != null) {
			logger.debug("connect success!");
			
			try {
				logger.info("set AutoCommit false!");
				conn.setAutoCommit(false);
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("Error Occur at setAutoCommit !");
				//send email
			}
			
			
			startTime = System.currentTimeMillis();

			//開始批價 
			subStartTime = System.currentTimeMillis();
			setLastFileID();
			logger.info("setLastFileID execute time :"+(System.currentTimeMillis()-subStartTime));
			
			subStartTime = System.currentTimeMillis();
			setCurrentMap();
			logger.info("setCurrentMap execute time :"+(System.currentTimeMillis()-subStartTime));
			
			subStartTime = System.currentTimeMillis();
			setDataRate();
			logger.info("setDataRate execute time :"+(System.currentTimeMillis()-subStartTime));
			
			subStartTime = System.currentTimeMillis();
			charge();
			logger.info("charge execute time :"+(System.currentTimeMillis()-subStartTime));
			

			for(String s : currentMap.keySet()){
				System.out.println(
						"IMSI:"+s+",\t\tCHARGE:"+currentMap.get(s).get("CHARGE")+",\t\tLAST_FILEID:"+currentMap.get(s).get("LAST_FILEID"));
			}
			for(String s : currentMapU.keySet()){
				System.out.println(
						"IMSI:"+s+",\t\tVOLUME:"+currentMapU.get(s).get("CHARGE")+",\t\tLAST_FILEID:"+currentMapU.get(s).get("LAST_FILEID"));
			}
			
			//tool.sendMail(logger, props, mailSender, mailReceiver, mailSubject, mailContent);

			// 程式執行完成
			endTime = System.currentTimeMillis();
			logger.info("Program execute time :" + (endTime - startTime));
			
			closeConnect();

		} else {
			logger.error("connect is null!");
		}
	}
	
	/**
	 * 計算完畢後寫回資料庫-更新
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
					pst.setDouble(1,(Double) currentMap.get(imsi).get("CHARGE"));
					pst.setString(2,(String) currentMap.get(imsi).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMap.get(imsi).get("SMS_TIMES"));
					pst.setDate(4, (java.sql.Date) currentMap.get(imsi).get("LAST_DATA_TIME"));
					pst.setString(5, imsi);
					pst.setInt(6, 0);//具有mccmnc
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
			// TODO Auto-generated catch block
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
					pst.setString(2,(String) currentMapU.get(imsi).get("LAST_FILEID"));
					pst.setInt(3, (Integer) currentMapU.get(imsi).get("SMS_TIMES"));
					pst.setDate(4, (java.sql.Date) currentMapU.get(imsi).get("LAST_DATA_TIME"));
					pst.setString(5, imsi);
					pst.setInt(6, 1);//不具有mccmnc
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Error at updateCurrentMapU : "+e.getMessage());
			//send mail
		}
	}
	
	/**
	 * 計算完畢後寫回資料庫-新增
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
					pst.setString(3,(String) currentMap.get(imsi).get("LAST_FILEID"));
					pst.setInt(4, (Integer) currentMap.get(imsi).get("SMS_TIMES"));
					pst.setDate(5, (java.sql.Date) currentMap.get(imsi).get("LAST_DATA_TIME"));
					pst.setInt(6, 0);//具有mccmnc
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
			// TODO Auto-generated catch block
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
			
			for(String imsi : insertList){
				if(count<=dataThreshold){
					pst.setString(1, imsi);		
					pst.setDouble(2,(Double) currentMapU.get(imsi).get("CHARGE"));
					pst.setString(3,(String) currentMapU.get(imsi).get("LAST_FILEID"));
					pst.setInt(4, (Integer) currentMapU.get(imsi).get("SMS_TIMES"));
					pst.setDate(5, (java.sql.Date) currentMapU.get(imsi).get("LAST_DATA_TIME"));
					pst.setInt(6, 1);//具有mccmnc
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
			// TODO Auto-generated catch block
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
	 * 依照額度需求發送警示簡訊
	 * 第一次，額度一，訊息一
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
		
		
		try {
			Statement st =conn.createStatement();
			logger.debug("Execute SQL:"+querySMSSetting);
			ResultSet rs=st.executeQuery(querySMSSetting);
			
			while(rs.next()){
				times.add(rs.getInt("ID"));
				bracket.add(rs.getDouble("BRACKET"));
				msg.add(processMag(rs.getString("MEG"),rs.getDouble("BRACKET")));
				suspend.add(rs.getString("SUSPEND"));
			}
			
			if(times.size()==0){
				logger.error("No SMS Setting!");
				return;
			}
			
			 PreparedStatement pst = conn.prepareStatement(insertSMSLog);
			 logger.info("Execute SQL :"+insertSMSLog);
			
			for(String imsi: currentMap.keySet()){
				
				String phone="";
				String result="";
				
				Double charge=(Double) currentMap.get(imsi).get("CHARGE");
				int smsTimes=(Integer) currentMap.get(imsi).get("SMS_TIMES");
				
				for(int i=0;i<=times.size();i++){
					if((smsTimes==times.get(i)-1)&&(charge>=bracket.get(i))){
						smsTimes++;
						
						//寄送簡訊
						logger.info("send "+smsTimes+"th message:"+msg.get(i));
						result=tool.callWSDLServer(setParam(msg.get(i),phone));
						logger.debug("send message result : "+result);						
						currentMap.get(imsi).put("SMS_TIMES", smsTimes);
						
						//寫入資料庫
						pst.setInt(1, Integer.valueOf(phone));
						pst.setString(2, msg.get(i));
						pst.setDate(3,(java.sql.Date) new Date());
						pst.setString(4, result);
						pst.addBatch();
						break;
					}
				}
				logger.debug("Log to table...executeBatch");
				pst.executeBatch();
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Error at sendAlertSMS : "+e.getMessage());
		}
	}
	
	/**
	 * 處理替代字串
	 * {{bracket}} 額度
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
				+ "	<ORGCODE>代發組織分類</ORGCODE>"
				+ "	<DATA>"
				+ "		<ITEM>"
				+ "			<SCHEDULE>0</SCHEDULE>"
				+ "			<MULTIPLE>0</MULTIPLE>"
				+ "			<MSG>"+msg+"</MSG>"
				+ "			<PHONE>"+phone+"</PHONE>"
				+ "		</ITEM>"
				+ "	</DATA>"
				+ "	<REMARK>備註資料</REMARK>"
				+ "</SMSREQUEST>");
		
		return sb.toString();
	}

	private void suspend(){
		
	}
	
	//缺少IMSI、MSISDN
	String cS2TMSISDN="",cS2TIMSI="",cTWNLDMSISDN="",cTWNLDIMSI="",sWSFStatus="",sWSFDStatus="",
			cReqStatus="17",cServiceOrderNBR="",sSql="";
	
    public void ReqStatus_17_Act(PrintWriter out17) throws SQLException, IOException, ClassNotFoundException, Exception{
    	logger.debug("ReqStatus_17_Act");
		Check_Type_Code_87_MAP_VALUE(cS2TMSISDN);
        sWSFStatus="V";
        sWSFDStatus="V";
        Process_SyncFile(sWSFStatus);
        Process_SyncFileDtl(sWSFDStatus);
        Process_ServiceOrder();
        //Process_WorkSubcode();
        Process_WorkSubcode_05_17(cS2TIMSI,cTWNLDIMSI,cReqStatus,cTWNLDMSISDN);
        sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
        		"SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
        conn.createStatement().executeUpdate(sSql);
        conn.commit();
        logger.debug("update SERVICE_ORDER:"+sSql);
        Query_PreProcessResult(out17,"000");
        Query_GPRSStatus();
	}
    String sM_CTYPE="";
    public void Check_Type_Code_87_MAP_VALUE(String sServiceCode)throws SQLException{
        Temprs=null;
        sSql="select CUSTOMERTYPE from service where servicecode='"+sServiceCode+"'";
        logger.info("Check_Type_Code_87_MAP_VALUE:"+sSql);
        Temprs=conn.createStatement().executeQuery(sSql);
        while (Temprs.next()){
             sM_CTYPE=Temprs.getString("CUSTOMERTYPE");
        }
        if (sM_CTYPE.equals("1")){sM_CTYPE="3";}
      }
    
    String  sDATE="",sCount="",sCMHKLOGID="",sMNOSubCode="950",cTicketNumber="";
    ResultSet Temprs;
    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    DateFormat dateFormat2 = new SimpleDateFormat("yyyyMMddhhmiss");
    String c910SEQ="",cFileName="",cFileID="";

	public void Process_SyncFile(String sSFStatus) throws SQLException,
			Exception {
		
		// 格式為YYYYMMDDXXX
		sDATE = dateFormat.format(new Date());
		c910SEQ = sDATE + sCount;
		cFileName = "S2TCI" + c910SEQ + ".950";
		cFileID = "";
		Temprs = null;
		sSql = "select S2T_SQ_FILE_CNTRL.NEXTVAL as ab from dual";
		Temprs = conn.createStatement().executeQuery(sSql);
		while (Temprs.next()) {
			cFileID = Temprs.getString("ab");
		}
		sSql = "INSERT INTO S2T_TB_TYPEB_WO_SYNC_FILE (FILE_ID,"
				+ "FILE_NAME,FILE_SEND_DATE,FILE_SEQ,CMCC_BRANCH_ID,"
				+ "FILE_CREATE_DATE,STATUS) VALUES (" + cFileID + ",'"
				//+ cFileName + "','" + dReqDate.substring(0, 8) + "','"
				+ cFileName + "','" + "" + "','"
				+ c910SEQ.substring(8, 11) + "','950',sysdate,'" + sSFStatus
				+ "')";
		logger.debug("Process_SyncFile:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();
	}
	
	String cWorkOrderNBR="",Sdate="";
	static Vector<String> vln=new Vector<String>();
	
	public void Process_SyncFileDtl(String sSFDStatus) throws SQLException, IOException{
        int iv,ix=0;
        String sVl="",sC,sH;
        cWorkOrderNBR="";
            Temprs=conn.createStatement().executeQuery("select S2T_SQ_WORK_ORDER.nextval as ab from dual");
            while (Temprs.next()){
                cWorkOrderNBR=Temprs.getString("ab");
            }
        Temprs=null;
            cServiceOrderNBR="";
            Temprs=conn.createStatement().executeQuery("select S2T_SQ_SERVICE_ORDER.nextval as ab from dual");
            while (Temprs.next()){
                cServiceOrderNBR=Temprs.getString("ab");
            }
      sSql="INSERT INTO S2T_TB_TYPB_WO_SYNC_FILE_DTL (WORK_ORDER_NBR,"+
           "WORK_TYPE, FILE_ID, SEQ_NO, CMCC_OPERATIONDATE, ORIGINAL_CMCC_IMSI,"+
           "ORIGINAL_CMCC_MSISDN, S2T_IMSI, S2T_MSISDN, FORWARD_TO_HOME_NO, "+
           "FORWARD_TO_S2T_NO_1, IMSI_FLAG, STATUS, SERVICE_ORDER_NBR, SUBSCR_ID)"+
           " VALUES ("+cWorkOrderNBR+",'"+ cReqStatus+"',"+ cFileID+",'"+
           c910SEQ+"',to_date('"+Sdate+"','MM/dd/yyyy HH24:mi:ss'),'"+
           cTWNLDIMSI+"','+"+ cTWNLDMSISDN+"','"+ cS2TIMSI+"','"+ cS2TMSISDN+"','+"+
           cTWNLDMSISDN+"','"+cTWNLDMSISDN+"', '2', '"+sSFDStatus+"','"+
           cServiceOrderNBR+"','"+cTicketNumber+"')";
      logger.debug("Process_SyncFileDtl:"+sSql);
      conn.createStatement().executeUpdate(sSql);
      conn.commit();
      if (vln.size()>0){
         vln.firstElement();
          for (iv=0; iv<vln.size();iv++){
            sVl=vln.get(iv);
             ix=sVl.indexOf(",");
             sC=sVl.substring(0, ix);
             sVl=sVl.substring(ix+1, sVl.length());
             ix=sVl.indexOf(",");
             sH=sVl.substring(0, ix);
             sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set VLN_"+sC+"='"+sH+
             "' where WORK_ORDER_NBR="+cWorkOrderNBR+" and SERVICE_ORDER_NBR='"+
             cServiceOrderNBR+"'";
             conn.createStatement().executeUpdate(sSql); 
             conn.commit();}}
    }

	String sMNOName="TWNLD";
	public void Process_ServiceOrder() throws SQLException, IOException {
		sSql = "INSERT INTO S2T_TB_SERVICE_ORDER (SERVICE_ORDER_NBR, "
				+ "WORK_TYPE, S2T_MSISDN, SOURCE_TYPE, SOURCE_ID, STATUS, "
				+ "CREATE_DATE) " + "VALUES ('" + cServiceOrderNBR + "','"
				+ cReqStatus + "','" + cS2TMSISDN + "'," + "'B_TYPE',"
				+ cWorkOrderNBR + ", '', sysdate)";

		logger.info("Process_ServiceOrder[1]:" + sSql);
		Temprs =  conn.createStatement().executeQuery(sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();
		Temprs = null;

		sSql = "Select MNO_NAME from S2T_TB_MNO_COMPANY "
				+ "Where MNO_SUB_CODE='" + sMNOSubCode + "'";

		logger.debug("Process_ServiceOrder[2]:" + sSql);
		Temprs = conn.createStatement().executeQuery(sSql);

		while (Temprs.next()) {
			sMNOName = Temprs.getString("MNO_NAME");
		}
	}
	String sFMTH="",sFMTHa="",sSFMTH="",sSFMTHa="",sFORWARD_TO_HOME_NO="",sS_FORWARD_TO_HOME_NO="",sSubCode="",sStepNo="";
    public void Process_WorkSubcode_05_17(String S2TImsiB,String TWNImsiB,String sReqStatus,String sTWNLDMSISDN) throws SQLException, IOException{
        Temprs=null;
        String cMd="",Ssvrid="";
        sSql="select nvl(serviceid,'0') as ab from imsi "+
              " where imsi = '"+S2TImsiB+"' and homeimsi='"+TWNImsiB+
              "'";
        logger.info("Get_Serviceid:"+sSql);
        Temprs=conn.createStatement().executeQuery(sSql);
        while (Temprs.next()){
          Ssvrid=Temprs.getString("ab");
        }
        if (!Ssvrid.equals("0")){
          Temprs=null;
              sSql="select count(serviceid) as ab from serviceparameter where "+
                   "parameterid=3792 and serviceid='"+Ssvrid+"'";
              logger.info("Check_Follow_Me_To_Home(有1表示有申請, 0表示未申請):"+sSql);
              Temprs=conn.createStatement().executeQuery(sSql);
              while (Temprs.next()){ //(有1表示有申請, 0表示未申請)
                sFMTH=Temprs.getString("ab");
              }

              if (sFMTH.equals("1")){
                  Temprs=null;
                  sSql="select nvl(value,'2') as ab From parametervalue where "+
                       "parametervalueid=3793 and serviceid='"+Ssvrid+"'";
                  logger.info("Check_Follow_Me_To_Home_Status(Value=1: active, Value=0: inactive, 若未申請, 則2):"+sSql);
                  Temprs=conn.createStatement().executeQuery(sSql);
                  while (Temprs.next()){ //(Value=1: active, Value=0: inactive, 若未申請, 則NULL)
                    sFMTHa=Temprs.getString("ab");
                  }
              }
              Temprs=null;
              sSql="select count(serviceid) as ab from serviceparameter where "+
                   "parameterid=3748 and serviceid='"+Ssvrid+"'";
              logger.info("Check_SMS_Follow_Me_To_Home(有1表示有申請, 0表示未申請):"+sSql);
              Temprs=conn.createStatement().executeQuery(sSql);
              while (Temprs.next()){ //(有1表示有申請, 0表示未申請)
                sSFMTH=Temprs.getString("ab");
              }

              if (sSFMTH.equals("1")){
                  Temprs=null;
                  sSql="select nvl(value,'2') as ab From parametervalue where "+
                       "parametervalueid=3752 and serviceid='"+Ssvrid+"'";
                  logger.info("Check_SMS_Follow_Me_To_Home_Status(Value=1: active, Value=0: inactive, 若未申請, 則2):"+sSql);
                  Temprs=conn.createStatement().executeQuery(sSql);
                  while (Temprs.next()){ //(Value=1: active, Value=0: inactive, 若未申請, 則NULL)
                    sSFMTHa=Temprs.getString("ab");
                  }
              }
              if (sReqStatus.equals("17")){
              Temprs=null;
              sSql="select nvl(value,'0') as ab from parametervalue where parametervalueid=3792 "+
                   "and serviceid='"+Ssvrid+"'";
              logger.info("Check_FORWARD_TO_HOME_NO:"+sSql);
                  Temprs=conn.createStatement().executeQuery(sSql);
                  while (Temprs.next()){
                    sFORWARD_TO_HOME_NO=Temprs.getString("ab");
                  }
              if (sFORWARD_TO_HOME_NO.equals('0')){sFORWARD_TO_HOME_NO=null;}
               Temprs=null;
              sSql="select nvl(value,'0') as ab from parametervalue where parametervalueid=3748 "+
                   "and serviceid='"+Ssvrid+"'";
              logger.info("Check_S_FORWARD_TO_HOME_NO:"+sSql);
                  Temprs=conn.createStatement().executeQuery(sSql);
                  while (Temprs.next()){
                    sS_FORWARD_TO_HOME_NO=Temprs.getString("ab");
                  }
                  if (sS_FORWARD_TO_HOME_NO.equals('0')){sS_FORWARD_TO_HOME_NO=null;}
            }
              else{
                sFORWARD_TO_HOME_NO=sTWNLDMSISDN;
                sS_FORWARD_TO_HOME_NO=sTWNLDMSISDN;
                sTWNLDMSISDN=null;
              }
        }

               sSql="Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"+
                       sMNOName+"' And work_type='"+cReqStatus+"' Order by step_no";
                logger.debug("Process_WorkSubcode_05_17:"+sSql);
                Temprs=conn.createStatement().executeQuery(sSql);
                 while (Temprs.next()){
                   sSubCode=Temprs.getString("subcode");
                   sStepNo=Temprs.getString("step_no");
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();
                 }
                 sSql="update PROVLOG " +
                 "set STEP='"+sStepNo+"' "+
                 " where LOGID="+sCMHKLOGID;
                 conn.createStatement().executeUpdate(sSql);
                 conn.commit();
     }

	public void Process_ServiceOrderItem() throws SQLException, IOException {
		sSql = "Insert into S2T_TB_SERVICE_ORDER_ITEM (SERVICE_ORDER_NBR,"
				+ "STEP_NO, SUB_CODE, IDENTIFIER, STATUS, SEND_DATE) "
				+ "Values (" + cServiceOrderNBR + "," + sStepNo + ",'"
				+ sSubCode + "',"
				+ " S2T_SQ_SERVICE_ORDER_ITEM.nextval, 'N', sysdate)";
		logger.debug("Process_ServiceOrderItem:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();
	}
	String sTypeCode="",sDataType="",sValue="";

	public void Process_ServiceOrderItemDtl() throws SQLException, IOException {
		sSql = "Insert into S2T_TB_SERVICE_ORDER_ITEM_DTL "
				+ "(SERVICE_ORDER_NBR, STEP_NO, TYPE_CODE, DATA_TYPE, VALUE) "
				+ "VALUES (" + cServiceOrderNBR + "," + sStepNo + ","
				+ sTypeCode + "," + sDataType + ",'" + sValue + "')";
		logger.debug("Process_ServiceOrderItemDtl:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();
	}

	public void Process_DefValue() throws SQLException, IOException {
		ResultSet TeRt = null;
		sSql = "Select TYPE_CODE, DATA_TYPE, DEF_VALUE "
				+ "From S2T_TB_SUBCODE_TYPECODE " + "Where subcode='"
				+ sSubCode + "' And work_type='" + cReqStatus
				+ "' And MNO_NAME='" + sMNOName + "' And DEF_VALUE is not null";
		logger.debug("Process_DefValue:" + sSql);
		TeRt = conn.createStatement().executeQuery(sSql);
		while (TeRt.next()) {
			sTypeCode = TeRt.getString("TYPE_CODE");
			sDataType = TeRt.getString("DATA_TYPE");
			sValue = TeRt.getString("DEF_VALUE");
			Process_ServiceOrderItemDtl();
		}
	}

	String sMap="",cGPRSStatus="0";
	public void Process_MapValue() throws SQLException, IOException {
		ResultSet TeRtA = null;
		sSql = "Select TYPE_CODE, DATA_TYPE, MAP_VALUE "
				+ "From S2T_TB_SUBCODE_TYPECODE " + "Where subcode='"
				+ sSubCode + "' And work_type='" + cReqStatus
				+ "' And MNO_NAME='" + sMNOName + "' And MAP_VALUE is not null";
		logger.debug("Process_MapValue:" + sSql);
		TeRtA = conn.createStatement().executeQuery(sSql);
		while (TeRtA.next()) {
			sTypeCode = TeRtA.getString("TYPE_CODE");
			sDataType = TeRtA.getString("DATA_TYPE");
			sMap = "";
			sMap = TeRtA.getString("MAP_VALUE");
			if ("S2T_MSISDN".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cS2TMSISDN;
			} else if ("S2T_IMSI".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cS2TIMSI;
			/*} else if ("TWNLD_MSISDN".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cTWNLDMSISDN;*/
			/*} else if ("TWNLD_IMSI".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cTWNLDIMSI;*/
			/*} else if ("S2T_MSISDN_OLD".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cMSISDNOLD;*/
			/*} else if ("M_205_OT".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cM205OT;*/
			/*} else if ("M_VLN".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cMVLN;*/
			} else if ("M_GPRS".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = cGPRSStatus;
			} else if ("M_CTYPE".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sM_CTYPE;
			} else if ("FMTH".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sFMTH;
			} else if ("FMTH_A".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sFMTHa;
			} else if ("SFMTH".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sSFMTH;
			} else if ("SFMTH_A".equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sSFMTHa;
			} else if ("FORWARD_TO_HOME_NO"
					.equals(TeRtA.getString("MAP_VALUE"))) {
				sValue = sFORWARD_TO_HOME_NO;
			} else if ("S_FORWARD_TO_HOME_NO".equals(TeRtA
					.getString("MAP_VALUE"))) {
				sValue = sS_FORWARD_TO_HOME_NO;
			}
			logger.debug("MAP_VALUE:" + sMap + "=" + sValue + ",StepNo:"
					+ sStepNo + ",DataType:" + sDataType + ",TypeCode:"
					+ sTypeCode);
			
			if (sTypeCode.equals("1909") && (sValue.equals("0"))) {
				logger.debug("Follow Me To Home did not work");
			} else if (sTypeCode.equals("1911") && (sValue.equals(""))) {
				logger.debug("Follow Me To Home did not Active");
			} else if (sTypeCode.equals("1942") && (sValue.equals("0"))) {
				logger.debug("SMS Follow Me To Home did not work");
			} else if (sTypeCode.equals("1944") && (sValue.equals(""))) {
				logger.debug("SMS Follow Me To Home did not Active");
			} else {
				Process_ServiceOrderItemDtl();
			}
		}
	}

	String cRCode="",Process_Code="",desc="";
	public void Query_PreProcessResult(PrintWriter outA, String rcode)
			throws SQLException, InterruptedException, Exception {
		cRCode = "";

		// rcode = "000"; //****************************************

		sSql = "update S2T_TB_TYPB_WO_SYNC_FILE_DTL set s2t_operationdate="
				+ "to_date('" + dateFormat2.format(new Date())
				+ "','YYYYMMDDHH24MISS')" + " where WORK_ORDER_NBR='"
				+ cWorkOrderNBR + "'";

		logger.debug("update S2T_TB_TYPB_WO_SYNC_FILE_DTL:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();

		sSql = "update S2T_TB_SERVICE_ORDER_ITEM set timestamp=" + "to_date('"
				+ dateFormat2.format(new Date())
				+ "','YYYYMMDDHH24MISS')" + " where Service_Order_NBR='"
				+ cServiceOrderNBR + "'";

		logger.debug("Update S2T_TB_SERVICE_ORDER_ITEM:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();

		sSql = "update S2T_TB_SERVICE_ORDER set timestamp=" + "to_date('"
				+ dateFormat2.format(new Date())
				+ "','YYYYMMDDHH24MISS')" + " where SERVICE_ORDER_NBR='"
				+ cServiceOrderNBR + "'";

		logger.debug("Update S2T_TB_SERVICE_ORDER:" + sSql);
		conn.createStatement().executeUpdate(sSql);
		conn.commit();

		desc = Load_ResultDescription(rcode);
	}
	public String Load_ResultDescription(String sDecs) throws SQLException {
		sDecs = "000";
		
		
	   String sD="";
	   Temprs = null;
	           
	   sSql="Select describe from S2T_TB_RESULT"+
	        " where RESULT_FLAG ='"+sDecs+"'";
	   
	   Temprs = conn.createStatement().executeQuery(sSql);
	    
	   while (Temprs.next()) {
	     sD = Temprs.getString("describe");
	   }
	     
	   return sD;
	}
}



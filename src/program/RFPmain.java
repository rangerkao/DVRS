package program;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
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

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


//190
/*import com.infotech.smpp.SMPPServicesStub;
import com.infotech.smpp.SMPPServicesStub.SendSMPP;
import com.infotech.smpp.SMPPServicesStub.SendSMPPResponse;*/








//199
import com.iglomo.SMPPServicesStub;
import com.iglomo.SMPPServicesStub.SendSMPP;
import com.iglomo.SMPPServicesStub.SendSMPPResponse;

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
	private final int kByte=1;//1024
	
	
	Map<String,Map> currentMap = new HashMap<String,Map>();
	Map<String,Map> currentMapU = new HashMap<String,Map>();
	Map<String,Map> dataRate = new HashMap<String,Map>();
	List<String> updatList = new ArrayList<String>();
	List<String> updatListU = new ArrayList<String>();
	List<String> insertList = new ArrayList<String>();
	List<String> insertListU = new ArrayList<String>();
	
	private  void iniLog4j(){
		System.out.println("initial Log4g, property at "+RFPmain.class.getResource(""));
		PropertyConfigurator.configure(RFPmain.class.getResource("").toString().replace("file:/", "")+"Log4j.properties");
		logger =Logger.getLogger(RFPmain.class);
	}
	
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
	
	
	

	
	private  void chargeCompare(){
		//對MCCNC可配對做批價,IMSI、總價、單位
				String chargeCompare=
						"SELECT IMSI,CHARGE,CURRENCY "
						+ "FROM( "
						+ "		SELECT C.IMSI,SUM(C.CHARGE) CHARGE,C.CURRENCY CURRENCY "
						+ "		FROM ( "
						+ "				SELECT A.IMSI,A.MCCMNC,B.CURRENCY,ROUND((A.DATAVOLUME/B.CHARGEUNIT)*B.RATE*1024,2) CHARGE "
						+ "				FROM HUR_DATA_USAGE A, HUR_DATA_RATE B "
						+ "				WHERE A.MCCMNC = B.MCCMNC AND A.MCCMNC is NOT NULL ) C "
						+ "		GROUP BY C.IMSI,C.CURRENCY)"
						+ "	WHERE ROWNUM>? AND ROWNUM<?";
				
				try {
					PreparedStatement pst=conn.prepareStatement(chargeCompare);
					/*pst.setInt(1, dataStart);
					pst.setInt(2, dataEnd);*/
					logger.debug("Execute SQL : "+chargeCompare);
					ResultSet rs = pst.executeQuery();

					while(rs.next()){
						System.out.print(rs.getString("IMSI"));
						System.out.println("");
					}
					pst.close();
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.debug("Execute SQL Error : "+e.getMessage());
				}
				
	}
	
	private  void cahrgeNonCompare(){
		//對MCCNC不可配對做批價(以最大計算)，IMSI、總價、單位
		String cahrgeNonCompare=
				"SELECT IMSI,CHARGE,CURRENCY "
				+ "FROM("
				+ "		SELECT C.IMSI,SUM(C.CHARGE) CHARGE,'HKD' CURRENCY "
				+ "		FROM ("
				+ "				SELECT A.IMSI,A.MCCMNC,ROUND(A.DATAVOLUME*(SELECT MAX(RATE/CHARGEUNIT) FROM HUR_DATA_RATE WHERE  ROWNUM=1)*1024,2) CHARGE "
				+ "				FROM HUR_DATA_USAGE A "
				+ "				WHERE A.MCCMNC not in (SELECT MCCMNC FROM HUR_DATA_RATE ) AND A.MCCMNC is NOT NULL ) C "
				+ "		GROUP BY C.IMSI, 'HKD' )"
				+ "WHERE  ROWNUM>? AND ROWNUM<?";
		
		try {
			PreparedStatement pst = conn.prepareStatement(cahrgeNonCompare);
/*			pst.setInt(1, dataStart);
			pst.setInt(2, dataEnd);*/
			logger.debug("Execute SQL : "+cahrgeNonCompare);
			ResultSet rs = pst.executeQuery();

			while(rs.next()){
				System.out.print(rs.getString("IMSI"));
				System.out.println("");
			}
			
			pst.close();
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug("Execute SQL Error : "+e.getMessage());
		}
	}
	
	
	
	private  void cahrgeUnknown(){
		//對MCCNC沒有資料做批價
		String cahrgeUnknown=				
				"SELECT IMSI,VOLUME "
				+ "FROM( "
				+ "SELECT A.IMSI,SUM(A.DATAVOLUME) VOLUME FROM HUR_DATA_USAGE A WHERE A.MCCMNC IS NULL GROUP BY A.IMSI "
				+ ")WHERE ROWNUM>? AND ROWNUM<? ";
		
		try {
			PreparedStatement pst = conn.prepareStatement(cahrgeUnknown);
/*			pst.setInt(1, dataStart);
			pst.setInt(2, dataEnd);*/
			logger.debug("Execute SQL : "+cahrgeUnknown);
			ResultSet rs = pst.executeQuery();
			while(rs.next()){
				System.out.print(rs.getString("VOLUME"));
				System.out.println("");
			}
			pst.close();
			rs.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug("Execute SQL Error : "+e.getMessage());
		}
	}
	

	
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
			
			logger.info("Got last process file ID :"+lastfileID);
			
			st.close();
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Got a SQLException : "+e.getMessage());
			//sendMail
		}
	}
	
	private void setCurrentMap(){
		logger.info("setCurrentMap...");
		String queryCurrent="SELECT A.IMSI,A.CHARGE,A.LAST_FILEID,A.SMS_TIMES,A.IS_UNKNOWN FROM HUR_CURRENT A";
		
		try {
			Statement st = conn.createStatement();
			logger.debug("Execute SQL : "+queryCurrent);
			ResultSet rs = st.executeQuery(queryCurrent);
			
			Map<String,Object> map=new HashMap<String,Object>();
			
			logger.debug("Set current map...");
			while(rs.next()){
				String imsi =rs.getString("IMSI");
				//System.out.println("imsi : "+imsi);
				map.put("CHARGE", rs.getDouble("CHARGE"));
				map.put("LAST_FILEID", rs.getInt("LAST_FILEID"));
				map.put("SMS_TIMES", rs.getInt("SMS_TIMES"));
				
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
	
	
	private void charge(){
		logger.info("charge...");
		String queryUsage=
				"SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID "
				+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID "
				+ "			FROM HUR_DATA_USAGE A WHERE A.FILEID>=? AND ROWNUM <= ?  ORDER BY A.USAGEID,A.FILEID) "
				+ "MINUS "
				+ "SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID "
				+ "FROM ( SELECT USAGEID,IMSI,MCCMNC,DATAVOLUME,FILEID "
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
					Integer fileID=rs.getInt("FILEID");
					
					if(mccmnc==null||"".equals(mccmnc)){
						//mccmnc 為空
						logger.info("setting IMSI="+imsi+" MCCMNC is NULL ...");
						if(currentMapU.containsKey(imsi)){
							currentMapU.get(imsi).put("CHARGE", (Double)currentMapU.get(imsi).get("CHARGE")+volume);
							currentMapU.get(imsi).put("LAST_FILEID",fileID);
						}else{
							Map map=new HashMap();
							map.put("CHARGE", volume);
							map.put("LAST_FILEID", fileID);
							map.put("SMS_TIMES", 0);
							currentMapU.put(imsi, map);
						}

					}else if(!dataRate.containsKey(mccmnc)){
						//mccmnc無法配對
						logger.info("setting IMSI="+imsi+" MCCMNC CANNOT COMPARE ...");
						Double charge=volume*kByte*maxRate;
						
						if(currentMap.containsKey(imsi)){
							currentMap.get(imsi).put("CHARGE", (Double)currentMap.get(imsi).get("CHARGE")+charge);
							currentMap.get(imsi).put("LAST_FILEID",fileID);
						}else{
							Map map=new HashMap();
							map.put("CHARGE", charge);
							map.put("LAST_FILEID", fileID);
							map.put("SMS_TIMES", 0);
							currentMap.put(imsi, map);
						}
					}else{
						logger.info("setting IMSI="+imsi+"...");
	
						Double rate=(Double) dataRate.get(mccmnc).get("RATE");
						Double chargeunit=(Double) dataRate.get(mccmnc).get("CHARGEUNIT");
						String currency=(String) dataRate.get(mccmnc).get("CURRENCY");
						
						Double charge=volume*kByte*(rate/chargeunit);
						
						if("HKD".equals(currency)) charge*=exchangeRate;
						
						if(currentMap.containsKey(imsi)){
							currentMap.get(imsi).put("CHARGE", (Double)currentMap.get(imsi).get("CHARGE")+charge);
							currentMap.get(imsi).put("LAST_FILEID",fileID);
						}else{
							Map map=new HashMap();
							map.put("CHARGE", charge);
							map.put("LAST_FILEID", fileID);
							map.put("SMS_TIMES", 0);
							currentMap.put(imsi, map);
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

	public static void main(String[] args) {

		RFPmain rf = new RFPmain();
		rf.process();
		
		

	}
	
	private void sendSMS(){
		String result=tool.callWSDLServer(setParam());
	}
	
	private String setParam(){
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
				+ "			<MSG>要發送的訊息</MSG>"
				+ "			<PHONE>886989235253</PHONE>"
				+ "		</ITEM>"
				+ "	</DATA>"
				+ "	<REMARK>備註資料</REMARK>"
				+ "</SMSREQUEST>");
		
		return sb.toString();
	}

}



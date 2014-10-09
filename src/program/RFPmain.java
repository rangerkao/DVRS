package program;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
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
	PreparedStatement st=null;
	ResultSet rs=null;
	 
	 
	private  Logger logger ;
	Properties props=new Properties();

	
	//mail conf
	String mailSender="";
	String mailReceiver="k1988242001@gmail.com";
	String mailSubject="mail test";
	String mailContent="mail content text";
	
	IJatool tool=new Jatool();
	
	
	private  final int dataStart=0;
	private  final int dataEnd=20;

	
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
					st=conn.prepareStatement(chargeCompare);
					st.setInt(1, dataStart);
					st.setInt(2, dataEnd);
					logger.debug("Execute SQL : "+chargeCompare);
					rs=st.executeQuery();

					while(rs.next()){
						System.out.print(rs.getString("IMSI"));
						System.out.println("");
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.debug("Execute SQL Error : "+e.getMessage());
				}finally{
					closeStatement();
					closeResultSet();
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
			st=conn.prepareStatement(cahrgeNonCompare);
			st.setInt(1, dataStart);
			st.setInt(2, dataEnd);
			logger.debug("Execute SQL : "+cahrgeNonCompare);
			rs=st.executeQuery();

			while(rs.next()){
				System.out.print(rs.getString("IMSI"));
				System.out.println("");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug("Execute SQL Error : "+e.getMessage());
		}finally{
			closeStatement();
			closeResultSet();
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
			st = conn.prepareStatement(cahrgeUnknown);
			st.setInt(1, dataStart);
			st.setInt(2, dataEnd);
			logger.debug("Execute SQL : "+cahrgeUnknown);
			rs=st.executeQuery();
			while(rs.next()){
				System.out.print(rs.getString("VOLUME"));
				System.out.println("");
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug("Execute SQL Error : "+e.getMessage());
		}finally{
			closeStatement();
			closeResultSet();
		}
	}
	
	private  void closeStatement(){
		if(st!=null){
			try {
				st.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.debug("close Statement Error : "+e.getMessage());
			}
		}
	}
	private  void closeResultSet(){
		if(rs!=null){
			try {
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.debug("close ResultSet Error : "+e.getMessage());
			}
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

			/*
			 * //開始批價 subStartTime = System.currentTimeMillis();
			 * chargeCompare();
			 * logger.info("chargeCompare execute time :"+(System
			 * .currentTimeMillis()-subStartTime));
			 * 
			 * subStartTime = System.currentTimeMillis(); cahrgeNonCompare();
			 * logger
			 * .info("cahrgeNonCompare execute time :"+(System.currentTimeMillis
			 * ()-subStartTime));
			 * 
			 * subStartTime = System.currentTimeMillis(); cahrgeUnknown();
			 * logger
			 * .info("cahrgeUnknown execute time :"+(System.currentTimeMillis
			 * ()-subStartTime));
			 */

			tool.sendMail(logger, props, mailSender, mailReceiver, mailSubject, mailContent);

			// 程式執行完成
			endTime = System.currentTimeMillis();
			logger.info("Program execute time :" + (endTime - startTime));

		} else {
			logger.error("connect is null!");
		}
	}

	public static void main(String[] args) {

		RFPmain rf = new RFPmain();
		rf.iniLog4j();
		//rf.process();
		
		

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



package program;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class RFPmain {

	
	//DB config
	private static final String DriverClass="oracle.jdbc.driver.OracleDriver";
	private static final String Host="10.42.1.101";
	private static final String Port="1521";
	private static final String ServiceName=":S2TBSDEV";
	private static final String UserName="foyadev";
	private static final String PassWord="foyadev";

	private static final String URL = "jdbc:oracle:thin:@"+ Host + ":"+Port+ServiceName; 
	
	
	private static Logger logger ;
	static Properties prop=new Properties();

	private static final int dataStart=0;
	private static final int dataEnd=20;
	static Connection conn = null;
	static PreparedStatement st=null;
	static ResultSet rs=null;
	
	private static void iniLog4j(){
		System.out.println("initial Log4g, property at "+RFPmain.class.getResource(""));
		PropertyConfigurator.configure(RFPmain.class.getResource("").toString().replace("file:/", "")+"Log4j.properties");
		logger =Logger.getLogger(RFPmain.class);
	}
	
	private static void loadProperties(){
		System.out.println("initial Log4g, property !");
		try {
			prop.load(new   FileInputStream(RFPmain.class.getResource("").toString().replace("file:/", "")+"/log4j.properties"));
			PropertyConfigurator.configure(prop);
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
	
	
	
	private static void connDB(){
		logger.info("Start to connect DB ");
		

			try {
				Class.forName(DriverClass);
				conn = DriverManager.getConnection(URL, UserName, PassWord);    
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("connecting DB error : " +e.getMessage());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("connecting DB error : " +e.getMessage());
			}    
			        
		logger.info("Finished to connect DB ");
	}
	
	private static void chargeCompare(){
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
	
	private static void cahrgeNonCompare(){
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
	
	
	
	private static void cahrgeUnknown(){
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
	
	private static void closeStatement(){
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
	private static void closeResultSet(){
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
	
	
	private static void sendMail(){
		final String host=prop.getProperty("mail.smtp.host");
		System.out.println("host\t:\t"+host);
		final String username=prop.getProperty("mail.username");
		System.out.println("username\t:\t"+username);
		final String passwd=prop.getProperty("mail.password");
		//System.out.println("passwd\t:\t"+passwd);
		/*final int port=Integer.parseInt(prop.getProperty("mail.smtp.port"));
		System.out.println("port\t:\t"+port);*/
		
		
		boolean authFlag = true; 
		boolean sessionDebug = false;
		boolean singleBody=true;
		String sender=username;
		String receiver="k1988242001@gmail.com";
		InternetAddress[] address = null; 
		String ccList="";
		String Subject="mail text";
		
		
		StringBuilder messageText = new StringBuilder(); 
		messageText.append("<html><body>"); 
		messageText.append("content"); 
		messageText.append("</body></html>"); 
		
		// construct a mail session 
		javax.mail.Session mailSession = javax.mail.Session.getInstance(prop,new javax.mail.Authenticator() {
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication(username, passwd);
		    }
		}); 
		
		
		mailSession.setDebug(sessionDebug); 
		
		
		try {
			Message msg = new MimeMessage(mailSession); 
			// mail sender 
			msg.setFrom(new InternetAddress(sender));
			// mail recievers 
			address = InternetAddress.parse(receiver, false); 
			msg.setRecipients(Message.RecipientType.TO, address); 
			// mail cc 
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccList)); 
			// mail's subject 
			msg.setSubject(Subject); 
			// mail's sending time 
			msg.setSentDate(new Date());
			
			if(singleBody){
				//msg.setText(messageText.toString());
			    msg.setContent(messageText.toString(), "text/html;charset=UTF-8");
			}else{
				MimeBodyPart mbp = new MimeBodyPart();// mail's charset
				mbp.setContent(messageText.toString(), "text/html; charset=utf8"); 
				Multipart mp = new MimeMultipart(); 
				mp.addBodyPart(mbp); 
				msg.setContent(mp); 
			}
			
			Transport.send(msg);
			
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		  
	}
	
	
	public static void main(String[] args){
		//程式開始時間
		long startTime;
		//程式結束時間
		long endTime;
		//副程式開始時間
		long subStartTime;
		
		//初始化log
		//iniLog4j();
		loadProperties();
		
		logger.info("RFP Program Start!");
		//進行DB連線
		connDB();
		if(conn!=null){
			logger.debug("connect success!");
			startTime = System.currentTimeMillis();
			
			/*//開始批價
			subStartTime = System.currentTimeMillis();
			chargeCompare();
			logger.info("chargeCompare execute time :"+(System.currentTimeMillis()-subStartTime));
			
			subStartTime = System.currentTimeMillis();
			cahrgeNonCompare();
			logger.info("cahrgeNonCompare execute time :"+(System.currentTimeMillis()-subStartTime));
			
			subStartTime = System.currentTimeMillis();
			cahrgeUnknown();
			logger.info("cahrgeUnknown execute time :"+(System.currentTimeMillis()-subStartTime));*/
			
			sendMail();
			
			//程式執行完成
			endTime = System.currentTimeMillis();
			logger.info("Program execute time :"+(endTime-startTime));
		}else{
			logger.error("connect is null!");
		}
	}
	
}



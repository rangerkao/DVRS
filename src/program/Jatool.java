package program;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

//190
/*import com.infotech.smpp.SMPPServicesStub;
import com.infotech.smpp.SMPPServicesStub.SendSMPP;
import com.infotech.smpp.SMPPServicesStub.SendSMPPResponse;*/







//199
import com.iglomo.SMPPServicesStub;
import com.iglomo.SMPPServicesStub.SendSMPP;
import com.iglomo.SMPPServicesStub.SendSMPPResponse;

public class Jatool implements IJatool{

	
	private void logControl(Logger logger,String type,String message){
		if(logger==null){
			System.out.println(message);
		}else{
			if("info".equalsIgnoreCase(type)){
				logger.info(message);
			}else if("debug".equalsIgnoreCase(type)){
				logger.debug(message);
			}else if("error".equalsIgnoreCase(type)){
				logger.error(message);
			}
		}
	}
	
	@Override
	public void sendMail(Logger logger,String sender, String receiver, String subject,String content) throws AddressException, MessagingException, IOException {
		sendMail(logger,null,sender,receiver,subject,content);
	}
	

	@Override
	public void sendMail(Logger logger,Properties props,String sender,String receiver,String subject,String content) throws AddressException, MessagingException, IOException {

		if(props==null){
			props=getProperties();
		}
		logControl(logger,"info","get Properites!");			
		
		final String host=props.getProperty("mail.smtp.host");
		logControl(logger,"info","Connect to Host : "+ host);
		
		String p=props.getProperty("mail.smtp.port");
		final Integer port=((p==null||"".equals(p))?null:Integer.valueOf(p));
		logControl(logger,"info","port : "+port);
		
		final String username=props.getProperty("mail.username");
		final String passwd=props.getProperty("mail.password");		
		
		String auth = props.getProperty("mail.smtp.auth");
		boolean authFlag = true;
		if(auth==null||"".equals(auth)||"false".equals(auth)){
			authFlag=false;
		}
		logControl(logger,"info","use authority : "+authFlag);
		
		boolean sessionDebug = false;
		boolean singleBody=true;
		
		if(sender==null || "".equals(sender)){
			if(username==null){
				logControl(logger,"error","No sender and No UserName Set!");
				return;
			}
			sender=username;			
		}else{
			if(username!=null && !"".equals(username) &&!sender.equalsIgnoreCase(username)){
				logControl(logger,"error","sender is not equals to UserName !");
				return;
			}
		}
		
		InternetAddress[] address = null; 
		String ccList="";
		
		
		StringBuilder messageText = new StringBuilder(); 
		messageText.append("<html><body>"); 
		messageText.append(content); 
		messageText.append("</body></html>"); 
		
		javax.mail.Session mailSession=null;
		logControl(logger,"debug","Creat mail Session!");
		if(authFlag){
			// construct a mail session 
			mailSession = javax.mail.Session.getInstance(props,new javax.mail.Authenticator() {
			    protected PasswordAuthentication getPasswordAuthentication() {
			        return new PasswordAuthentication(username, passwd);
			    }
			}); 
		}else{
			mailSession = javax.mail.Session.getDefaultInstance(props);
		}
		
		mailSession.setDebug(sessionDebug); 
		
			Message msg = new MimeMessage(mailSession); 
			msg.setFrom(new InternetAddress(sender));			// mail sender 
			
			address = InternetAddress.parse(receiver, false); // mail recievers 
			msg.setRecipients(Message.RecipientType.TO, address); 
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccList)); // mail cc 
			
			msg.setSubject(subject); // mail's subject 
			msg.setSentDate(new Date());// mail's sending time 
			logControl(logger,"debug","set mail content!");
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
			
			logControl(logger,"info","sending mail from "+sender+" to "+receiver+"\n<br>"+
										"Subject : "+msg.getSubject()+"\n<br>"+
										"Content : "+msg.getContent()+"\n<br>"+
										"SendDate: "+msg.getSentDate());			
	}
	
	private Properties getProperties(){
		Properties result=new Properties();
		
		result.setProperty("mail.smtp.host", "202.133.250.242");
		result.setProperty("mail.transport.protocol", "smtp");
		//result.setProperty("mail.smtp.port", "");//未設定預設為25
		
		result.setProperty("mail.smtp.auth", "true");
		
		//TLS authentication 
		//result.setProperty("mail.smtp.starttls.enable", "true");

		//SSL authentication 
		//result.setProperty("mail.smtp.socketFactory.port", "465");
		//result.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");		
		
		
		//自訂參數
		result.setProperty("mail.username", "");
		result.setProperty("mail.password", "");
		
		return result;
	}

	
	@Override
	public Connection connDB(Logger logger, String DriverClass, String URL,
			String UserName, String PassWord) throws ClassNotFoundException, SQLException {
		logControl(logger, "debug", "Start to connect DB ");

		Connection conn = null;

			Class.forName(DriverClass);
			conn = DriverManager.getConnection(URL, UserName, PassWord);

		logControl(logger, "debug", "Finished to connect DB ");

		return conn;
	}

	@Override
	public String callWSDLServer(String param) throws RemoteException {

		String result = null;
		SMPPServicesStub stub = new SMPPServicesStub();

		SendSMPP smpp = new SendSMPP();
		smpp.setArgs0(param);
		SendSMPPResponse res = stub.sendSMPP(smpp);

		result = res.get_return();

		return result;

	}

	@Override
	public Date getMonthFirstDate(Date date) {
		
		Calendar calendar = Calendar.getInstance();//默認為當前時間
		Date monthFirstDate=null;

		calendar.setTime(date);
		calendar.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DATE));
		monthFirstDate=calendar.getTime();
		calendar.clear();

		return monthFirstDate;
	}

	@Override
	public Date getMonthLastDate(Date date) {
		Calendar calendar = Calendar.getInstance();//默認為當前時間
		Date monthLastDate=null;
		
		calendar.setTime(date);
		calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
		monthLastDate= calendar.getTime();
		calendar.clear();
		return monthLastDate;
	}

	@Override
	public java.sql.Date convertJaveUtilDate_To_JavaSqlDate(java.util.Date date) {
		
		return new java.sql.Date(date.getTime());
	}

	@Override
	public java.util.Date convertJaveSqlDate_To_JavaUtilDate(java.sql.Date date) {
		return new java.util.Date(date.getTime());
	}

	String iniform="yyyy/MM/dd hh24:mm:ss";
	@Override
	public String DateFormat(){
		DateFormat dateFormat = new SimpleDateFormat(iniform);
		return dateFormat.format(new Date());
	}
	
	@Override
	public String DateFormat(Date date, String form) {
		
		if(date==null) date=new Date();
		if(form==null ||"".equals(form)) form=iniform;
		
		DateFormat dateFormat = new SimpleDateFormat(form);
		return dateFormat.format(date);
	}

	@Override
	public Date DateFormat(String dateString, String form) throws ParseException {
		Date result=new Date();
		
		if(dateString==null) return result;
		
		if(form==null ||"".equals(form)) form=iniform;
		DateFormat dateFormat = new SimpleDateFormat(form);
		dateFormat.parse(dateString);
		
		return result;
	}
	
	
	
}

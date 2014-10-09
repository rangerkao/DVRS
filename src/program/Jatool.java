package program;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
	public void sendMail(Logger logger,String sender, String receiver, String subject,String content) {
		sendMail(logger,null,sender,receiver,subject,content);
	}
	

	@Override
	public void sendMail(Logger logger,Properties props,String sender,String receiver,String subject,String content) {

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
		
		
		try {
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
			

			
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logControl(logger,"error","Got AddressException : " + e.getMessage());
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logControl(logger,"error","Got MessagingException : "+ e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logControl(logger,"error","Got IOException : "+ e.getMessage());
		} 

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
			String UserName, String PassWord) {
		logControl(logger, "debug", "Start to connect DB ");

		Connection conn = null;
		try {
			Class.forName(DriverClass);
			conn = DriverManager.getConnection(URL, UserName, PassWord);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logControl(logger, "error",
					"connecting DB error : " + e.getMessage());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logControl(logger, "error",
					"connecting DB error : " + e.getMessage());
		}

		logControl(logger, "debug", "Finished to connect DB ");

		return conn;
	}

	@Override
	public String callWSDLServer(String param) {

		String result = null;
		try {
			SMPPServicesStub stub = new SMPPServicesStub();

			SendSMPP smpp = new SendSMPP();
			smpp.setArgs0(param);
			SendSMPPResponse res = stub.sendSMPP(smpp);

			result = res.get_return();
		} catch (AxisFault e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;

	}
	
	
	
}

package program;


import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

public interface IJatool {

	/**
	 *	�o�e�l��
	 * 
	 * @param logger
	 * @param sender
	 * @param receiver
	 * @param subject
	 * @param content
	 * @throws MessagingException 
	 * @throws AddressException 
	 * @throws IOException 
	 * @throws Exception
	 */
	void sendMail(Logger logger,String sender,String receiver,String subject,String content) throws AddressException, MessagingException, IOException;
	
	/**
	 * �o�e�l��
	 * 
	 * @param logger
	 * @param props
	 * @param sender ���񪽱��q�]�w��Ū��
	 * @param receiver �h�����̤��\�H","�j�}
	 * @param subject
	 * @param content
	 * @throws MessagingException 
	 * @throws AddressException 
	 * @throws IOException 
	 * @throws Exception
	 */
	void sendMail(Logger logger,Properties props,String sender,String receiver,String subject,String content) throws AddressException, MessagingException, IOException;
	
	/**
	 * DB�s�u
	 * 
	 * @param logger
	 * @param DriverClass
	 * @param URL
	 * @param UserName
	 * @param PassWord
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 */
	Connection connDB(Logger logger,String DriverClass,String URL,String UserName,String PassWord) throws ClassNotFoundException, SQLException;
	
	
	/**
	 * �ǰe��ƨ�WSDL Server
	 * �ݭn�إ�WSDL Client�����ɮ�
	 * new Web Client
	 * 
	 * @param param
	 * @return
	 * @throws AxisFault 
	 * @throws RemoteException 
	 */
	String callWSDLServer(String param) throws AxisFault, RemoteException;
	
	/**
	 * ���o�ѼƮɶ���몺�Ĥ@��
	 * @param date
	 * @return
	 */
	Date getMonthFirstDate(Date date);
	
	/**
	 * ���o�ѼƮɶ���몺�̫�@��
	 * @param date
	 * @return
	 */
	Date getMonthLastDate(Date date);
	
	/**
	 * ���o�ѼƮɶ���骺�s�I
	 * @param date
	 * @return
	 */
	Date getDayFirstDate(Date date);
	
	/**
	 * ���o�ѼƮɶ���몺23�I59��
	 * @param date
	 * @return
	 */
	Date getDayLastDate(Date date);
	
	/**
	 * �Nutil Date �ഫ��sql Date
	 * @param date
	 * @return
	 */
	java.sql.Date convertJaveUtilDate_To_JavaSqlDate(java.util.Date date);
	
	/**
	 * �Nsql Date �ഫ��util Date
	 * @param date
	 * @return
	 */
	java.util.Date convertJaveSqlDate_To_JavaUtilDate(java.sql.Date date);
	
	/**
	 * �N����ഫ���r��
	 * @param date
	 * @param form
	 * @return
	 */
	String DateFormat();
	String DateFormat(Date date,String form);
	
	/**
	 * �N�r���ഫ�����
	 * @param dateString
	 * @param form
	 * @return
	 * @throws ParseException 
	 */
	Date DateFormat(String dateString,String form) throws ParseException;
	
	/**
	 * �HHttp Post �覡�ǰe�Ѽ�
	 * @param url "https://selfsolve.apple.com/wcResults.do"
	 * @param param "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345"
	 * @return
	 * @throws IOException 
	 */
	String HttpPost(String url,String param,String charset) throws IOException;
}

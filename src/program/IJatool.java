package program;


import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

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
	void sendMail(Logger logger,String sender,String receiver,String subject,String content) throws AddressException, MessagingException, IOException, Exception;
	
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
	void sendMail(Logger logger,Properties props,String sender,String receiver,String subject,String content) throws AddressException, MessagingException, IOException, Exception;
	
	/**
	 * DB�s�u
	 * 
	 * @param logger
	 * @param DriverClass
	 * 		oracle:oracle.jdbc.driver.OracleDriver
	 * 		postgresql:org.postgresql.Driver
	 * 		mySQL: com.mysql.jdbc.Driver
	 * 		MsSQL:com.microsoft.jdbc.sqlserver.SQLServerDriver
	 * @param URL
	 * @param UserName
	 * @param PassWord
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 */
	Connection connDB(Logger logger,String DriverClass,String URL,String UserName,String PassWord) throws ClassNotFoundException, SQLException;
	
	
	/**
	 * 
	 * @param DBType
	 * 		oracle:oracle:thin
	 * 		postgresql:postgresql
	 * 		mySQL: mysql
	 * 		MsSQL:microsoft:sqlserver
	 * @param ip
	 * @param port
	 * @param DB
	 * @param charSet
	 * @return
	 */
	String parseDBURL(String DBType,String ip,String port,String DB,String charSet);
	
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
	 * �ɶ�HH:Mi:SS �|�Q����
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

	/**
	 * ��DecimalFormat �� Double�榡��
	 * @param value
	 * @param form
	 * @return
	 */
	Double FormatDouble(Double value, String form);

	/**
	 * ��DecimalFormat �� �ƭȮ榡�Ʀ��r��
	 * @param value
	 * @param form
	 * @return
	 */
	String FormatNumString(Double value);
	String FormatNumString(Double value, String form);
	
	/**
	 * read File example
	 * @param filePath
	 */
	void readtxt(String filePath);
	
	/**
	 * write File example
	 * @param content
	 */
	void writetxt(String content);

	/**
	 * �Q�Υ��W��ܪk���Ҧr����
	 * 
	 * "^\\d+$"  �¼Ʀr
	 * "^\\d+(.\\d+)?"  ��ƩΥN���p�Ʀ�
	 * 
	 * @param content
	 * @param regex
	 * @return
	 */
	boolean regularMatch(String content,String regex);
	
	
	/**
	 * ����ƪ�ܪk
	 * �Ǧ^Pattern
	 * �H pattern.matcher(testString)�ϥ�
	 * 
	 * @param regex
	 * @return
	 */
	
	Pattern regularMatch(String regex);
	
	
	/**
	 * �Q�Υ��W��ܪk
	 * ��X���e�ŦX��ܪk������
	 * 
	 * "\\d{4}-\\d{6}"
	 * 4��Ʀr-6��Ʀr ex:1111-555663
	 * 
	 * 
	 * @param content
	 * @param regex
	 * @return
	 */
	List<String> regularFind(String content,String regex);
	
	
	
	
	/**
	 * 
	 * @param value
	 * @param method
	 * 
	 * 			ROUND_CEILING 	�ƭȥ��� 	ROUND_UP
	 * 							�ƭȭt��	
	 * 
	 * 			ROUND_HALF_EVEN �¦V���ƪ��
	 * 
	 * 			ROUND_HALF_UP	�����Ƥj��5 ROUND_UP
	 * 			ROUND_HALF_DOWN	�����Ƥj��6 ROUND_UP
	 * 
	 * @param digit �p�ƫ���
	 * @return
	 */
	Double roundUpOrDdown(Double value,String method,int digit);
	
}

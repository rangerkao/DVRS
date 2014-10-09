package program;

import java.sql.Connection;
import java.util.Properties;

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
	 * @throws Exception
	 */
	void sendMail(Logger logger,String sender,String receiver,String subject,String content);
	
	/**
	 * �o�e�l��
	 * 
	 * @param logger
	 * @param props
	 * @param sender ���񪽱��q�]�w��Ū��
	 * @param receiver �h�����̤��\�H","�j�}
	 * @param subject
	 * @param content
	 * @throws Exception
	 */
	void sendMail(Logger logger,Properties props,String sender,String receiver,String subject,String content);
	
	/**
	 * DB�s�u
	 * 
	 * @param logger
	 * @param DriverClass
	 * @param URL
	 * @param UserName
	 * @param PassWord
	 * @return
	 */
	Connection connDB(Logger logger,String DriverClass,String URL,String UserName,String PassWord);
	
	
	/**
	 * �ǰe��ƨ�WSDL Server
	 * �ݭn�إ�WSDL Client�����ɮ�
	 * new Web Client
	 * 
	 * @param param
	 * @return
	 */
	String callWSDLServer(String param);
}

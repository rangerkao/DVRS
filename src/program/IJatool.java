package program;

import java.sql.Connection;
import java.util.Properties;

import org.apache.log4j.Logger;

public interface IJatool {

	/**
	 *	發送郵件
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
	 * 發送郵件
	 * 
	 * @param logger
	 * @param props
	 * @param sender 未填直接從設定檔讀取
	 * @param receiver 多接收者允許以","隔開
	 * @param subject
	 * @param content
	 * @throws Exception
	 */
	void sendMail(Logger logger,Properties props,String sender,String receiver,String subject,String content);
	
	/**
	 * DB連線
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
	 * 傳送資料到WSDL Server
	 * 需要建立WSDL Client必須檔案
	 * new Web Client
	 * 
	 * @param param
	 * @return
	 */
	String callWSDLServer(String param);
}

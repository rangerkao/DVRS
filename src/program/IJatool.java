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
	 *	發送郵件
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
	 * 發送郵件
	 * 
	 * @param logger
	 * @param props
	 * @param sender 未填直接從設定檔讀取
	 * @param receiver 多接收者允許以","隔開
	 * @param subject
	 * @param content
	 * @throws MessagingException 
	 * @throws AddressException 
	 * @throws IOException 
	 * @throws Exception
	 */
	void sendMail(Logger logger,Properties props,String sender,String receiver,String subject,String content) throws AddressException, MessagingException, IOException, Exception;
	
	/**
	 * DB連線
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
	 * 傳送資料到WSDL Server
	 * 需要建立WSDL Client必須檔案
	 * new Web Client
	 * 
	 * @param param
	 * @return
	 * @throws AxisFault 
	 * @throws RemoteException 
	 */
	String callWSDLServer(String param) throws AxisFault, RemoteException;
	
	/**
	 * 取得參數時間當月的第一天
	 * @param date
	 * @return
	 */
	Date getMonthFirstDate(Date date);
	
	/**
	 * 取得參數時間當月的最後一天
	 * @param date
	 * @return
	 */
	Date getMonthLastDate(Date date);
	
	/**
	 * 取得參數時間當日的零點
	 * @param date
	 * @return
	 */
	Date getDayFirstDate(Date date);
	
	/**
	 * 取得參數時間當月的23點59分
	 * @param date
	 * @return
	 */
	Date getDayLastDate(Date date);
	
	/**
	 * 將util Date 轉換至sql Date
	 * 時間HH:Mi:SS 會被裁掉
	 * @param date
	 * @return
	 */
	java.sql.Date convertJaveUtilDate_To_JavaSqlDate(java.util.Date date);
	
	/**
	 * 將sql Date 轉換至util Date
	 * @param date
	 * @return
	 */
	java.util.Date convertJaveSqlDate_To_JavaUtilDate(java.sql.Date date);
	
	/**
	 * 將日期轉換成字串
	 * @param date
	 * @param form
	 * @return
	 */
	String DateFormat();
	String DateFormat(Date date,String form);
	
	/**
	 * 將字串轉換成日期
	 * @param dateString
	 * @param form
	 * @return
	 * @throws ParseException 
	 */
	Date DateFormat(String dateString,String form) throws ParseException;
	
	/**
	 * 以Http Post 方式傳送參數
	 * @param url "https://selfsolve.apple.com/wcResults.do"
	 * @param param "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345"
	 * @return
	 * @throws IOException 
	 */
	String HttpPost(String url,String param,String charset) throws IOException;

	/**
	 * 用DecimalFormat 對 Double格式化
	 * @param value
	 * @param form
	 * @return
	 */
	Double FormatDouble(Double value, String form);

	/**
	 * 用DecimalFormat 對 數值格式化成字串
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
	 * 利用正規表示法驗證字串資料
	 * 
	 * "^\\d+$"  純數字
	 * "^\\d+(.\\d+)?"  整數或代有小數位
	 * 
	 * @param content
	 * @param regex
	 * @return
	 */
	boolean regularMatch(String content,String regex);
	
	
	/**
	 * 持續化表示法
	 * 傳回Pattern
	 * 以 pattern.matcher(testString)使用
	 * 
	 * @param regex
	 * @return
	 */
	
	Pattern regularMatch(String regex);
	
	
	/**
	 * 利用正規表示法
	 * 找出內容符合表示法的部分
	 * 
	 * "\\d{4}-\\d{6}"
	 * 4位數字-6位數字 ex:1111-555663
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
	 * 			ROUND_CEILING 	數值正值 	ROUND_UP
	 * 							數值負值	
	 * 
	 * 			ROUND_HALF_EVEN 朝向偶數近似
	 * 
	 * 			ROUND_HALF_UP	近似位數大於5 ROUND_UP
	 * 			ROUND_HALF_DOWN	近似位數大於6 ROUND_UP
	 * 
	 * @param digit 小數後位數
	 * @return
	 */
	Double roundUpOrDdown(Double value,String method,int digit);
	
}

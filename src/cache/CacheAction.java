package cache;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import program.IJatool;
import program.Jatool;
import dao.CurrentDao;
import action.BaseAction;

public class CacheAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Properties props =null;
	static Logger logger ;

	//IMSI SERVICEID 對應表
	static Map<String,String> serviceIDtoIMSI = new HashMap<String,String>();
	static Map<String,String> imsitoServiceID = new HashMap<String,String>();
	
	//Instance 化 DB連結
	//instance 會造成程式無法持續運作
	static Connection conn = null;
	static Connection conn2 = null;
	
	//Batch thread 時間
	int ThreadPeriod = 24;
	static boolean batchExcute = false;
	
	CurrentDao currentDao = new CurrentDao();
	protected static IJatool tool= new Jatool();
	static String classPath = "12312313";
	static String sql="";
	
	
	static {
		classPath=CacheAction.class.getClassLoader().getResource("").toString().replace("file:", "").replace("%20", " ");
		try {
			loadProperties();
			/*connectDB();
			connectDB2();*/
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public CacheAction() throws Exception{
		super();
	}
	
	//---------------建立  Properties---------------------
	private static void loadProperties() throws FileNotFoundException, IOException {	
		String path=classPath+ "/program/Log4j.properties";
		props =new Properties();
		props.load(new FileInputStream(path));
		PropertyConfigurator.configure(props);
		System.out.println("loadProperties success!");
	}
	
	public static Properties getProperties() {
		return props;
	}
	
	public String reloadProperties(){
		try {
			loadProperties();
		} catch (FileNotFoundException e) {
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = s.toString();
		} catch (IOException e) {
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = s.toString();
		}
		System.out.println("reload End!");
		return SUCCESS;
	}
	
	//---------------建立DB 連結 conn1 主資料庫、conn2 Mboss----
	public static Connection getConnect1() throws Exception{
		return connectDB();
	}
	
	public static Connection getConnect2() throws Exception{
		return connectDB2();
	}
	
	protected static Connection connectDB() throws Exception{
		Connection conn = null;
		System.out.println(classPath);
		String url=props.getProperty("Oracle.URL");
		System.out.println(url);
		url=url.replace("{{Host}}", props.getProperty("Oracle.Host"));
		System.out.println(url);
		url=url.replace("{{Port}}", props.getProperty("Oracle.Port"));
		System.out.println(url);
		url=url.replace("{{ServiceName}}", (props.getProperty("Oracle.ServiceName")!=null?props.getProperty("Oracle.ServiceName"):""));
		System.out.println(url);
		url=url.replace("{{SID}}", (props.getProperty("Oracle.SID")!=null?props.getProperty("Oracle.SID"):""));
		conn=tool.connDB(props.getProperty("Oracle.DriverClass"), url, 
				props.getProperty("Oracle.UserName"), 
				props.getProperty("Oracle.PassWord")
				);
			if(conn==null){
				throw new Exception("DB Connect null !");
			}
		return conn;
	}
	protected static Connection connectDB2() throws Exception {
		Connection conn2=null;
		String url=props.getProperty("mBOSS.URL")
				.replace("{{Host}}", props.getProperty("mBOSS.Host"))
				.replace("{{Port}}", props.getProperty("mBOSS.Port"))
				.replace("{{ServiceName}}", (props.getProperty("mBOSS.ServiceName")!=null?props.getProperty("mBOSS.ServiceName"):""))
				.replace("{{SID}}", (props.getProperty("mBOSS.SID")!=null?props.getProperty("mBOSS.SID"):""));
		conn2=tool.connDB(props.getProperty("mBOSS.DriverClass"), url, 
				props.getProperty("mBOSS.UserName"), 
				props.getProperty("mBOSS.PassWord")
				);
			if(conn2==null){
				throw new Exception("DB Connect2 null !");
			}
		return conn2;
	}
	/**
	 * 建立連線
	 * @throws Exception 
	 */
	
	protected static void createConnect() throws Exception{
		conn=getConnect1();
		conn2=getConnect2();
	}
	/**
	 * 關閉連線
	 */
	protected static void closeConnect() {
		if (conn != null) {

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
				//logger.debug("close ResultSet Error : "+e.getMessage());
				//send mail
				//sendMail("At closeConnect occur SQLException error!");
			}
		}
		if (conn2 != null) {

			try {
				conn2.close();
			} catch (SQLException e) {
				e.printStackTrace();
				//logger.debug("close ResultSet Error : "+e.getMessage());
				//send mail
				//sendMail("At closeConnect occur SQLException error!");
			}
		}
	}
	
	//-------------清除Cache---------------------
	
	public String flushCache(){
		flushServiceIDwithIMSIMappingCache();
		
		return SUCCESS;
	}
	
	public static String flushServiceIDwithIMSIMappingCache(){
		serviceIDtoIMSI.clear();
		imsitoServiceID.clear();
		
		return SUCCESS;
	}
	
	//-------------重載Cache--------------------
	
	public String reloadCache(){
		reloadServiceIDwithIMSIMappingCache();

		System.out.println("reload End!");
		return SUCCESS;
	}
	
	
	
	public static String reloadServiceIDwithIMSIMappingCache(){
		flushServiceIDwithIMSIMappingCache();
		try {
			createConnect();
			setIMSItoServiceID();
			setServiceIDtoIMSI();
			sendMail("k1988242001@gmail.com","DVRS Cache Reload!","DVRS Cache Reload! At "+new Date());
		} catch (SQLException e) {
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = s.toString();
			sendMail("k1988242001@gmail.com","DVRS Cache Reload Error!","DVRS Cache Reload Error! At "+new Date()+"\n"+s);
		} catch (Exception e) {
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = s.toString();
			sendMail("k1988242001@gmail.com","DVRS Cache Reload Error!","DVRS Cache Reload Error! At "+new Date()+"\n"+s);
		}finally{
			closeConnect();
		}
		System.out.println("reload End!");
		return SUCCESS;
	}
	
	//----------------啟動Batch reload------------------
	public String batchReloadCache(){
		if(!batchExcute){
			Thread reloadThread = new BatchThread(ThreadPeriod);
			reloadThread.setDaemon(true);
			reloadThread.start();
			batchExcute =true;
		}else{
			result="Batch had been Started!";
		}
		
		return SUCCESS;
	}
	
	public class BatchThread extends Thread implements Runnable {
    	int ThreadPeriod =0;
    	public BatchThread(int ThreadPeriod){
    		this.ThreadPeriod=ThreadPeriod;
    	}
        public void run() { // implements Runnable run()
        	
        	Timer timer = new Timer();
        	if(ThreadPeriod==0){
        		timer.schedule(new taskClass(), 0);
        	}else{
        		Calendar calendar = Calendar.getInstance();
        		//啟動時間為下次的0點
        		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)+1,0,0,0);
        		timer.schedule(new taskClass(), calendar.getTime(), ThreadPeriod*60*60*1000);
        	}
        }
    }
	public class taskClass extends TimerTask{

		public void run(){
			reloadCache();     
		}
	}

	
	
	/**
	 * 發送Error mail
	 * 
	 * @param content
	 */
	protected static void sendMail(String Receiver,String Subject,String Content){
	
		try {
			tool.sendMail(null, props, "DVRS UI", Receiver, Subject, Content);
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private static void setIMSItoServiceID() throws Exception{	
		
		System.out.println("setIMSItoServiceID...");
				sql = " SELECT A.SERVICEID,A.IMSI          "
						+ "FROM("
						+ "		SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE"
						+ "		FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B"
						+ "		WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713"
						+ "		UNION"
						+ "		SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE"
						+ "		FROM SERVICEORDER A,NEWSERVICEORDERINFO B"
						+ "		WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A,"
						+ "     	(SELECT IMSI,MAX(COMPLETEDATE) COMPLETEDATE"
						+ "     	  from(SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE"
						+ "     	            FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B"
						+ "     	            WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713"
						+ "     	            UNION"
						+ "     	            SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE"
						+ "     	            FROM SERVICEORDER A,NEWSERVICEORDERINFO B"
						+ "     	            WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713)"
						+ "     	  GROUP BY IMSI )B "
						+ " WHERE A.IMSI=B.IMSI AND A.COMPLETEDATE =B.COMPLETEDATE ";
		
	    
		Connection subConn2 = getConnect2();

		Statement st = subConn2.createStatement();
		
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			imsitoServiceID.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
		}
		rs.close();
		st.close();
	}
	
	private static void setServiceIDtoIMSI() throws Exception{	
		System.out.println("setServiceIDtoIMSI...");
		sql = " SELECT A.SERVICEID,A.IMSI "
				+ "FROM( "
				+ "		SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE "
				+ "		FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
				+ "		WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
				+ "		UNION "
				+ "		SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE "
				+ "		FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
				+ "		WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A, "
				+ "		(	SELECT SERVICEID,MAX(COMPLETEDATE) COMPLETEDATE "
				+ "			from(	SELECT A.SERVICEID,B.NEWVALUE,A.COMPLETEDATE "
				+ "					FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
				+ "					WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
				+ "					UNION "
				+ "					SELECT A.SERVICEID,B.FIELDVALUE,A.COMPLETEDATE "
				+ "					FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
				+ "					WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713) "
				+ "			GROUP BY SERVICEID )B "
				+ "		WHERE A.SERVICEID=B.SERVICEID AND A.COMPLETEDATE =B.COMPLETEDATE ";
		Connection subConn2 = getConnect2();
		Statement st = subConn2.createStatement();
		
		ResultSet rs=st.executeQuery(sql);
	
		while(rs.next()){
			serviceIDtoIMSI.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
		}
		rs.close();
		st.close();
	}
	
	public static Map<String, String> getImsitoServiceID() {
		return imsitoServiceID;
	}

	public static Map<String, String> getServiceIDtoIMSI() {
		return serviceIDtoIMSI;
	}
	
	//-----------------------

	public int getThreadPeriod() {
		return ThreadPeriod;
	}

	public void setThreadPeriod(int threadPeriod) {
		ThreadPeriod = threadPeriod;
	}
	

}

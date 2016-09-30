package cache;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import dao.CurrentDao;
import action.BaseAction;

public class CacheAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static Properties props =null;
	static Logger logger ;

	//IMSI SERVICEID ������
	static Map<String,String> serviceIDtoIMSI = new HashMap<String,String>();
	static Map<String,String> imsitoServiceID = new HashMap<String,String>();
	
	//Instance �� DB�s��
	//instance �|�y���{���L�k����B�@
	static Connection conn = null;
	static Connection conn2 = null;
	
	static long connTime = 0;
	static long conn2Time = 0;
	
	//Batch thread �ɶ�
	int ThreadPeriod = 24;
	static boolean batchExcute = false;
	
	CurrentDao currentDao = new CurrentDao();
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
		
		connBatchClass cb =new connBatchClass();
		cb.start();
	}
	
	public CacheAction() throws Exception{
		super();
	}
	
	//---------------�إ�  Properties---------------------
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
	
	//---------------�إ�DB �s�� conn1 �D��Ʈw�Bconn2 Mboss----
	public static Connection getConnect1() throws Exception{
		connTime = System.currentTimeMillis();
		if(conn==null)
			connectDB();
		/*if(System.currentTimeMillis() - connTime>1000*60*10){
			try {
				conn.close();
			} catch (Exception e) {

			}
			connectDB();
		}else{
			connTime = System.currentTimeMillis();
		}*/
	
		return conn;
	}
	
	public static Connection getConnect2() throws Exception{
		conn2Time = System.currentTimeMillis();
		if(conn2==null)
			connectDB2();
		/*if(System.currentTimeMillis() - conn2Time>1000*60*10){
			try {
				conn2.close();
			} catch (Exception e) {

			}
			connectDB2();
		}else{
			conn2Time = System.currentTimeMillis();
		}*/
	
		return conn2;
	}
	
	protected static Connection connectDB() throws Exception{
		connTime = System.currentTimeMillis();
		System.out.println("Create connect1!");
		//Connection conn = null;
		System.out.println(classPath);
		String url=props.getProperty("Oracle.URL");
		
		url=url
				.replace("{{Host}}", props.getProperty("Oracle.Host"))
				.replace("{{Port}}", props.getProperty("Oracle.Port"))
				.replace("{{ServiceName}}", (props.getProperty("Oracle.ServiceName")!=null?props.getProperty("Oracle.ServiceName"):""))
				.replace("{{SID}}", (props.getProperty("Oracle.SID")!=null?props.getProperty("Oracle.SID"):""));		
		
		conn=connDB(props.getProperty("Oracle.DriverClass"), url, 
				props.getProperty("Oracle.UserName"), 
				props.getProperty("Oracle.PassWord")
				);
			if(conn==null){
				throw new Exception("DB Connect null !");
			}
		return conn;
	}
	protected static Connection connectDB2() throws Exception {
		conn2Time = System.currentTimeMillis();
		System.out.println("Create connect2!");
		//Connection conn2=null;
		String url=props.getProperty("mBOSS.URL")
				.replace("{{Host}}", props.getProperty("mBOSS.Host"))
				.replace("{{Port}}", props.getProperty("mBOSS.Port"))
				.replace("{{ServiceName}}", (props.getProperty("mBOSS.ServiceName")!=null?props.getProperty("mBOSS.ServiceName"):""))
				.replace("{{SID}}", (props.getProperty("mBOSS.SID")!=null?props.getProperty("mBOSS.SID"):""));
		conn2=connDB(props.getProperty("mBOSS.DriverClass"), url, 
				props.getProperty("mBOSS.UserName"), 
				props.getProperty("mBOSS.PassWord")
				);
			if(conn2==null){
				throw new Exception("DB Connect2 null !");
			}
		return conn2;
	}
	
	
	static boolean statementResult = false;
	public static class statementClass extends Thread{

		Connection conn;
		
		statementClass(Connection conn){
			this.conn = conn;
		}
		
		@Override
		public void run(){
			statementResult = false;
			Statement st = null;
			try {
				st = conn.createStatement();
				//st.execute("select 'A' from dual");
				statementResult = true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally{
				try {
					if(st!=null) st.close();
				} catch (SQLException e) {
				}
			}
		}
	}
	
	
	static boolean connBatch = true;
	public static class connBatchClass extends Thread{
		
		@Override
		public void run(){
			while(connBatch){
				if(System.currentTimeMillis() - connTime>1000*60*10){
					try {
						conn.close();
					} catch (Exception e) {

					}
					conn = null;
				}
				
				
				if(System.currentTimeMillis() - conn2Time>1000*60*10){
					try {
						conn2.close();
					} catch (Exception e) {

					}
					conn2 = null;
				}
				
				try {
					Thread.sleep(1000*60);
				} catch (InterruptedException e) {
				}
			}
		}
	}


	/**
	 * 以Thread控制連接Conn的速度
	 * @author ranger.kao
	 *
	 */
	static Connection subConn;
	public static class connClass extends Thread{
		
		String DriverClass,URL,UserName,PassWord;
		
		connClass(String DriverClass, String URL,
			String UserName, String PassWord){
			this.DriverClass = DriverClass;
			this.URL = URL;
			this.UserName = UserName;
			this.PassWord = PassWord;
		}
		
		@Override
		public void run(){
			try {
				Class.forName(DriverClass);
				DriverManager.setLoginTimeout(20);
				subConn = DriverManager.getConnection(URL, UserName, PassWord);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	public static Connection connDB(String DriverClass, String URL,
			String UserName, String PassWord) throws ClassNotFoundException, SQLException {
		Connection conn = null;

			Class.forName(DriverClass);
			conn = DriverManager.getConnection(URL, UserName, PassWord);
		
		return conn;
	}
	
	/*public Connection connDB(String DriverClass,
			String DBType,String ip,String port,String DBNameorSID,String charset,
			String UserName,String PassWord) throws ClassNotFoundException, SQLException{

			Class.forName(DriverClass);
			String url="jdbc:{{DBType}}:thin:@{{Host}}:{{Port}}:{{ServiceName}}{{charset}}"
					.replace("{{DBType}}", DBType)
					.replace("{{Host}}", ip)
					.replace("{{Port}}", port)
					.replace("{{ServiceName}}", DBNameorSID)
					.replace("{{charset}}", (charset!=null?"?charset="+charset:""));
			
		return connDB(DriverClass, url, UserName, PassWord);
	}*/
	/**
	 * �إ߳s�u
	 * @throws Exception 
	 */
	
	protected static void createConnect() throws Exception{
		conn=getConnect1();
		conn2=getConnect2();
	}
	/**
	 * �����s�u
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
	
	//-------------�M��Cache---------------------
	
	public String flushCache(){
		flushServiceIDwithIMSIMappingCache();
		
		return SUCCESS;
	}
	
	public static String flushServiceIDwithIMSIMappingCache(){
		serviceIDtoIMSI.clear();
		imsitoServiceID.clear();
		
		return SUCCESS;
	}
	
	//-------------����Cache--------------------
	
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
			//closeConnect();
		}
		System.out.println("reload End!");
		return SUCCESS;
	}
	
	//----------------�Ұ�Batch reload------------------
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
        		//�Ұʮɶ����U����0�I
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
	 * �o�eError mail
	 * 
	 * @param content
	 */
	protected static void sendMail(String Receiver,String Subject,String Content){
	
		try {
			sendMail(Subject, Content, "DVRS UI", Receiver);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	static void sendMail(String mailSubject,String mailContent,String mailSender,String mailReceiver){
		String [] cmd=new String[3];
		cmd[0]="/bin/bash";
		cmd[1]="-c";
		cmd[2]= "/bin/echo \""+mailContent+"\" | /bin/mail -s \""+mailSubject+"\" -r "+mailSender+" "+mailReceiver;

		try{
			Process p = Runtime.getRuntime().exec (cmd);
			p.waitFor();
			System.out.println("send mail cmd:"+cmd);
		}catch (Exception e){
			System.out.println("send mail fail:"+mailContent);
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
		
		Statement st = getConnect2().createStatement();
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

		Statement st = getConnect2().createStatement();
		
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

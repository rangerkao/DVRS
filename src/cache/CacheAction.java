package cache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
import java.util.Iterator;
import java.util.List;
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
	static Map<String,String> serviceIDtoICCID = new HashMap<String,String>();
	static Map<String,String> serviceIDtoIMSI = new HashMap<String,String>();
	static Map<String,String> imsitoServiceID = new HashMap<String,String>();

	static long connTime = 0;
	static long conn2Time = 0;
	
	static Long ConnectionTime1;
	static Long ConnectionTime2;
	
	static Connection conn1;
	static Connection conn2;
	
	static SimpleConnectionPoolDataSource CPD1 ;
	static SimpleConnectionPoolDataSource CPD2 ;
	
	static boolean threadExit= false;
	static long connectionCloseTime = 1000*60*20;
	
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
			logger = Logger.getLogger(CacheAction.class);
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
		
		try {
			CPD1 = new SimpleConnectionPoolDataSource(
					props.getProperty("Oracle.DriverClass"), 
					props.getProperty("Oracle.URL")
					.replace("{{Host}}", props.getProperty("Oracle.Host"))
					.replace("{{Port}}", props.getProperty("Oracle.Port"))
					.replace("{{ServiceName}}", 	(props.getProperty("Oracle.ServiceName")!=null?props.getProperty("Oracle.ServiceName"):""))
					.replace("{{SID}}", (props.getProperty("Oracle.SID")!=null?props.getProperty("Oracle.SID"):"")), 
					props.getProperty("Oracle.UserName"), 
					props.getProperty("Oracle.PassWord"),
					30,logger);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			CPD2 = new SimpleConnectionPoolDataSource(
					props.getProperty("mBOSS.DriverClass"), 
					props.getProperty("mBOSS.URL")
					.replace("{{Host}}", props.getProperty("mBOSS.Host"))
					.replace("{{Port}}", props.getProperty("mBOSS.Port"))
					.replace("{{ServiceName}}", (props.getProperty("mBOSS.ServiceName")!=null?props.getProperty("mBOSS.ServiceName"):""))
					.replace("{{SID}}", (props.getProperty("mBOSS.SID")!=null?props.getProperty("mBOSS.SID"):"")), 
					props.getProperty("mBOSS.UserName"), 
					props.getProperty("mBOSS.PassWord"),
					30,logger);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		Thread t = new Thread(new ConnectionWatch());
		t.start();

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
	public static Connection getConn1() throws ClassNotFoundException, SQLException{
		logger.info("get Connect1!");
		return CPD1.getConnection();
	}
	
	public static void releaseConn1(Connection conn) throws SQLException{
		System.out.println("Connect1!");
		conn.close();
		logger.info("release 1!"+CPD1.getConns().size());
	}
	
	public static Connection getConn2() throws ClassNotFoundException, SQLException{
		logger.info("get Connect2!");
		return CPD2.getConnection();
	}
	
	public static void releaseConn2(Connection conn) throws SQLException{
		conn.close();
		logger.info("release 2!"+CPD2.getConns().size());
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

	public static class ConnectionWatch implements Runnable{
		long nowTime;

		@Override
		public void run() {
			while(!threadExit){
				logger.info("ConnectionWatch...");
				try {
					Thread.sleep(connectionCloseTime);
				} catch (InterruptedException e) {}
				
				nowTime = System.currentTimeMillis();
		
				List<Connection> conns1 = null;
				
				try {
					logger.info("Watch 1");
					conns1 = CPD1.getConns();
					synchronized (conns1) {
						Iterator<Connection> it1 = conns1.iterator();
						while (it1.hasNext()) {
							ConnectionWrapper conn = (ConnectionWrapper) it1.next();
							
							if (nowTime-conn.getExecuteTime()>connectionCloseTime-1000*60*5 || !connTest(conn)){
								it1.remove();
								try {
									conn.doClose();
									logger.info("Watch 1 after release!"+CPD1.getConns().size()+"||"+conns1.size());
								} catch (SQLException e) {}
							}
						}
					}
					logger.info("Watch 1 after watches!"+CPD1.getConns().size());
				} catch (Exception e1) {
					logger.info(e1);
				}		
			
				try {
					logger.info("Watch 2");
					List<Connection> conns2 = CPD2.getConns();
					synchronized (conns2) {
						Iterator<Connection> it2 = conns2.iterator();
						while (it2.hasNext()) {
							ConnectionWrapper conn = (ConnectionWrapper) it2.next();
							if (nowTime-conn.getExecuteTime()>connectionCloseTime-1000*60*5|| !connTest(conn)){
								it2.remove();
								try {
									conn.doClose();
									logger.info("Watch 2 after release!"+CPD2.getConns().size()+"||"+conns2.size());
								} catch (SQLException e) {}
							}
						}
					}
					logger.info("Watch 2 after watches!"+CPD2.getConns().size());
				} catch (Exception e1) {
					logger.info(e1);
				}		
			}
		}		
	}

	private static boolean connTest(Connection conn){
		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeQuery("select 'OK' from dual");
		} catch (SQLException e) {
			return false;
		}finally{
			try {
			if(st!=null)	st.close();
			} catch (SQLException e) {	};
		}
		return true;
	}

	
	//-------------����Cache--------------------
	
	public String reloadCache(){
		
		try {
			readCache();
			setCache();
			sendMail("k1988242001@gmail.com","DVRS Cache Reload!","DVRS Cache Reload! At "+new Date());
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
	
	public String readCache() throws Exception{
		readServiceIDtoIMSI();
		readIMSItoServiceID();
		System.out.println("read End!");
		return SUCCESS;
	}
	public String setCache() throws Exception{
		setIMSItoServiceID();
		setServiceIDtoIMSI();
		System.out.println("reload End!");
		return SUCCESS;
	}
	
	
	//----------------�Ұ�Batch reload------------------
	public String batchReloadCache(){
		if(!batchExcute){
			Calendar calendar = Calendar.getInstance();
    		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)+1,0,0,0);
    		Timer timer = new Timer();
    		timer.schedule(new taskClass(), calendar.getTime(), ThreadPeriod*60*60*1000);
			batchExcute =true;
		}else{
			result="Batch had been Started!";
		}
		
		return SUCCESS;
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
		
		Statement st = getConn2().createStatement();
		ResultSet rs=st.executeQuery(sql);
		File FileName = new File("IMSItoServiceidMapping.csv");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileName,false), "UTF8")); 
		
		while(rs.next()){
			//imsitoServiceID.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
			bw.append(rs.getString("IMSI")+","+rs.getString("SERVICEID")+"\n");
		}
		bw.close();
		rs.close();
		st.close();
	}
	
	private static void readIMSItoServiceID() throws Exception{
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("IMSItoServiceidMapping.csv"), "UTF8")); 
		String str = null;
		while ((str = reader.readLine()) != null) {
			String[] content = str.trim().split(",");
			imsitoServiceID.put(content[0], content[1]);
		}
		reader.close();
	}
	
	private static void setServiceIDtoIMSI() throws Exception{	
		System.out.println("setServiceIDtoIMSI...");
		sql = " SELECT A.SERVICEID,A.IMSI,C.ICCID "
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
				+ "			GROUP BY SERVICEID )B  ,IMSI C "
				+ "		WHERE A.SERVICEID=B.SERVICEID AND A.COMPLETEDATE =B.COMPLETEDATE AND A.serviceid = C.serviceid(+) ";

		Statement st = getConn2().createStatement();
		
		ResultSet rs=st.executeQuery(sql);
		File FileName = new File("ServiceidtoIMSIMapping.csv");
		File FileName2 = new File("ServiceidtoICCIDMapping.csv");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileName,false), "UTF8")); 
		BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileName2,false), "UTF8")); 
		while(rs.next()){
			//serviceIDtoIMSI.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
			//serviceIDtoICCID.put(rs.getString("SERVICEID"), rs.getString("ICCID"));
			bw.append(rs.getString("SERVICEID")+","+rs.getString("IMSI")+"\n");
			bw2.append(rs.getString("SERVICEID")+","+rs.getString("ICCID")+"\n");
		}
		bw.close();
		bw2.close();
		rs.close();
		st.close();
	}
	
	private static void readServiceIDtoIMSI() throws Exception{

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("ServiceidtoIMSIMapping.csv"), "UTF8")); 
		String str = null;
		while ((str = reader.readLine()) != null) {
			String[] content = str.trim().split(",");
			serviceIDtoIMSI.put(content[0], content[1]);
		}
		reader.close();

		BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream("ServiceidtoICCIDMapping.csv"), "UTF8")); 
		while ((str = reader2.readLine()) != null) {
			String[] content = str.trim().split(",");
			serviceIDtoICCID.put(content[0], content[1]);
		}
		reader2.close();
	}
	
	
	
	public static Map<String, String> getImsitoServiceID() {
		return imsitoServiceID;
	}

	public static Map<String, String> getServiceIDtoIMSI() {
		return serviceIDtoIMSI;
	}
	public static Map<String, String> getServiceIDtoICCID() {
		return serviceIDtoICCID;
	}
	
	//-----------------------

	public int getThreadPeriod() {
		return ThreadPeriod;
	}

	public void setThreadPeriod(int threadPeriod) {
		ThreadPeriod = threadPeriod;
	}
	

}

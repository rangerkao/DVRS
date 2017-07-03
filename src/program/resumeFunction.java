package program;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class resumeFunction {
	
	//HUR
	Connection conn = null;
	//MBOSS
	Connection conn2 = null;
	
	static Properties props=new Properties();
		
	private static  Logger logger ;
	
	//日期設定
	private String MONTH_FORMATE="yyyyMM";
	private String DAY_FORMATE="yyyyMMdd";	
	private String TIME_FORMATE="ddHH";	
	//系統時間，誤差一小時，系統資料處理時間為當時時間提前一小時
	private String programeTime="";
	private String sYearmonth="";
	private String sYearmonthday="";
	//上個月
	private String sYearmonth2="";

	public static void main(String[] args) {
		new resumeFunction();
	}
	
	resumeFunction(){
		
		loadProperties();
		
		// 進行DB連線
		connectDB();
		connectDB2();
		
		if(
			setDayDate()&&
			setMsisdnMap()&&
			setIMSItoServiceIDMap()&&
			true){
			
			checkResume();
		}
		
		
		
	}
	
	/**
	 * 關閉連線
	 */
	private void closeConnect() {
		
		if (conn != null) {

			try {
				logger.info("closeConnect1...");
				conn.close();
			} catch (SQLException e) {
				ErrorHandle("close Connect Error", e);
			}

		}
		
		if (conn2 != null) {

			try {
				logger.info("closeConnect2...");
				conn2.close();
			} catch (SQLException e) {
				ErrorHandle("close Connect2 Error", e);
			}

		}
	}
	
	/**
	 * 連線至DB1
	 */
	private void connectDB(){
		//conn=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		try {
			String url=props.getProperty("Oracle.URL")
					.replace("{{Host}}", props.getProperty("Oracle.Host"))
					.replace("{{Port}}", props.getProperty("Oracle.Port"))
					.replace("{{ServiceName}}", (props.getProperty("Oracle.ServiceName")!=null?props.getProperty("Oracle.ServiceName"):""))
					.replace("{{SID}}", (props.getProperty("Oracle.SID")!=null?props.getProperty("Oracle.SID"):""));
			
			conn=connDB(props.getProperty("Oracle.DriverClass"), url, 
					props.getProperty("Oracle.UserName"), 
					props.getProperty("Oracle.PassWord")
					);
			logger.info("Connect to "+url);
			
			
		} catch (ClassNotFoundException e) {
			sql="";
			ErrorHandle("At connDB occur ClassNotFoundException error", e);
		} catch (SQLException e) {
			ErrorHandle("At connDB occur SQLException error", e);
		}
	}
	
	/**
	 * 連線至DB2
	 */
	private void connectDB2(){
		// 進行DB連線
		//conn2=tool.connDB(logger, DriverClass, URL, UserName, PassWord);
		try {
			String url=props.getProperty("mBOSS.URL")
					.replace("{{Host}}", props.getProperty("mBOSS.Host"))
					.replace("{{Port}}", props.getProperty("mBOSS.Port"))
					.replace("{{ServiceName}}", (props.getProperty("mBOSS.ServiceName")!=null?props.getProperty("mBOSS.ServiceName"):""))
					.replace("{{SID}}", (props.getProperty("mBOSS.SID")!=null?props.getProperty("mBOSS.SID"):""));
			
			conn2=connDB(props.getProperty("mBOSS.DriverClass"),url, 
					props.getProperty("mBOSS.UserName"), 
					props.getProperty("mBOSS.PassWord"));
			
			logger.info("Connrct to "+url);
		} catch (ClassNotFoundException e) {
			sql="";
			ErrorHandle("At connDB2 occur ClassNotFoundException error", e);
		} catch (SQLException e) {
			sql="";
			ErrorHandle("At connDB2 occur SQLException error", e);;
		}
	}
	
	public Connection connDB(String DriverClass, String URL,
			String UserName, String PassWord) throws ClassNotFoundException, SQLException {
		Connection conn = null;

			Class.forName(DriverClass);
			conn = DriverManager.getConnection(URL, UserName, PassWord);
		return conn;
	}
	
	public Connection connDB(String DriverClass,
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
	}
	
	private static  void loadProperties(){
		System.out.println("initial Log4j, property !");
		String path=DVRSmain.class.getResource("").toString().replace("file:", "")+"/Log4j.properties";
		try {
			props.load(new   FileInputStream(path));
			PropertyConfigurator.configure(props);
			logger =Logger.getLogger(DVRSmain.class);
			logger.info("Logger Load Success!");			
		} catch (FileNotFoundException e) {
			sql="";
			ErrorHandle("At loadProperties occur file not found error \n <br> file path="+path);
		} catch (IOException e) {
			sql="";
			ErrorHandle("At loadProperties occur IOException error !\n <br> file path="+path);
		}
	}
	
	
	
	static String iniform= "yyyy/MM/dd HH:mm:ss";
	public String DateFormat(Date date, String form) {
		
		if(date==null) date=new Date();
		if(form==null ||"".equals(form)) form=iniform;
		
		DateFormat dateFormat = new SimpleDateFormat(form);
		return dateFormat.format(date);
	}
	
	private boolean setDayDate(){
		logger.info("setMonthDate...");
		long subStartTime = System.currentTimeMillis();

		//目前時間
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		
		//TODO test
		//calendar.set(2016,0, 1, 1, 30, 0);
		System.out.println(calendar.getTime());
		
		
		//系統時間提前一小時
		//因為警示發送只計當日，當00：30執行時，所處理的資料為前一天的23：00
		calendar.setTimeInMillis(calendar.getTimeInMillis()-1000*60*60);
		programeTime = DateFormat(calendar.getTime(),TIME_FORMATE);		
		sYearmonth=DateFormat(calendar.getTime(), MONTH_FORMATE);
		sYearmonthday=DateFormat(calendar.getTime(),DAY_FORMATE);
		//上個月時間，減掉Month會-30天，採取到1號向前，確定跨月
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)-1);
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-1);
		sYearmonth2=DateFormat(calendar.getTime(), MONTH_FORMATE);
		calendar.clear();
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		
		return true;
	}
	
	Map<String,Map<String,String>> msisdnMap = new HashMap<String,Map<String,String>>();
	
	Map<String,String> IMSItoServiceIdMap = new HashMap<String,String>();
	Map<String,String> ServiceIdtoIMSIMap = new HashMap<String,String>();
	
	static String sql = null;
	/**
	 * 設定IMSI對應到ServiceID Map 20150115 add
	 * 
	 * 先從MSISDN MAP(IMSI table 資料找尋 SERVICEID)
	 * 找不到再從此Table(換卡記錄)找尋
	 */
	private boolean setIMSItoServiceIDMap(){
		logger.info("setIMSItoServiceIDMap...");
		logger.info("setServiceIDtoImsiMap...");
		IMSItoServiceIdMap.clear();
		ServiceIdtoIMSIMap.clear();
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result =false;
		sql=  ""
				+ "SELECT A.SERVICEID,A.IMSI"
				+ "		FROM("
				+ "			SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE"
				+ "			FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B"
				+ "		    WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713"
				+ "		    UNION"
				+ "		    SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE"
				+ "		    FROM SERVICEORDER A,NEWSERVICEORDERINFO B"
				+ "		    WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A,"
				+ "		    	(SELECT IMSI,MAX(COMPLETEDATE) COMPLETEDATE"
				+ "				from(SELECT A.SERVICEID,B.NEWVALUE IMSI,A.COMPLETEDATE"
				+ "             		FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B"
				+ "                     WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713"
				+ "                     UNION"
				+ "                     SELECT A.SERVICEID,B.FIELDVALUE IMSI,A.COMPLETEDATE"
				+ "                     FROM SERVICEORDER A,NEWSERVICEORDERINFO B"
				+ "                     WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713)"
				+ "                     GROUP BY IMSI )B"
				+ "		WHERE A.IMSI=B.IMSI AND A.COMPLETEDATE =B.COMPLETEDATE";
		
		
		try {
			logger.info("Execute SQL :"+sql);
			st = conn2.createStatement();
			rs=st.executeQuery(sql);
			
			while(rs.next()){
				IMSItoServiceIdMap.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
				ServiceIdtoIMSIMap.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
			}
			result =true;
		} catch (SQLException e) {
			ErrorHandle("At setIMSItoServiceIDMap occur SQLException error!", e);
	}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {

			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	public void checkResume(){
		logger.info("checkResume...");
		//TODO
		String resumeReport="";
		List<Map<String,String>> beenSuspended = new ArrayList<Map<String,String>>();
		Map<String,String> CHTSuspended = new HashMap<String,String>();
		
		Statement st = null;
		ResultSet rs = null;

		try {
			
			//撈出上個月被斷網的客戶	
			String sql ="SELECT A.SERVICEID ,B.SERVICECODE,C.CREATE_DATE "
					+ "FROM HUR_CURRENT A,SERVICE B ,"
					+ "		(	select msisdn,max(create_date) create_date "
					+ "			from HUR_SUSPEND_GPRS_LOG "
					+ "			where  TO_CHAR(CREATE_DATE,'yyyyMM')= '"+sYearmonth2+"' group by msisdn )C "
					+ "WHERE A.SERVICEID=B.SERVICEID AND B.SERVICECODE = C.MSISDN(+) "
					+ "AND TO_CHAR(C.CREATE_DATE,'yyyyMM')= '"+sYearmonth2+"' "
					+ "AND A.MONTH='"+sYearmonth2+"'  AND A.EVER_SUSPEND = 1 ";
			
			/*String sql = "SELECT A.SERVICEID "
					+ "FROM HUR_CURRENT A "
					+ "WHERE A.MONTH='"+sYearmonth2+"'  AND A.EVER_SUSPEND = 1 ";*/
			
			
			st = conn.createStatement();
			logger.info("Query Customers had been suspended last month:"+sql);
			rs = st.executeQuery(sql);
			
			resumeReport += "Customers had been suspended last month:";
			while(rs.next()){
				Map<String,String> m = new HashMap<String,String>();
				m.put("serviceid", rs.getString("SERVICEID"));
				m.put("msisdn", rs.getString("SERVICECODE"));
				m.put("date", rs.getString("CREATE_DATE"));
				beenSuspended.add(m);
				resumeReport += rs.getString("SERVICEID") + ",";
			}
			
			resumeReport = resumeReport.substring(0,resumeReport.length()-1)+"\n";
			logger.info(resumeReport);
			rs.close();
			
			//取得上個月中華最後一筆供裝17的資料，是0
			sql = "SELECT B.IMSI,B.REQTIME "
					+ "FROM( "
					+ "		SELECT A.IMSI,MAX(A.REQTIME) REQTIME "
					+ "		FROM(	SELECT SUBSTR(CONTENT,INSTR(CONTENT, 'S2T_IMSI=')+LENGTH('S2T_IMSI='),INSTR(CONTENT, 'Req_Status')-INSTR(CONTENT, 'S2T_IMSI')-LENGTH('S2T_IMSI=')-1) IMSI "
					+ "						,REQTIME,LOGID "
					+ "				FROM PROVLOG "
					+ "				WHERE CONTENT LIKE '%Req_Status=17%' AND TO_CHAR(REQTIME,'yyyyMM') = '"+sYearmonth2+"'"
					+ "			) A "
					+ "		GROUP BY A.IMSI "
					+ "		) B ,("
					+ "		SELECT A.IMSI,A.REQTIME,CONTENT "
					+ "		FROM(   SELECT SUBSTR(CONTENT,INSTR(CONTENT, 'S2T_IMSI=')+LENGTH('S2T_IMSI='),INSTR(CONTENT, 'Req_Status')-INSTR(CONTENT, 'S2T_IMSI')-LENGTH('S2T_IMSI=')-1) IMSI,CONTENT "
					+ " 	               ,REQTIME,LOGID "
					+ "				FROM PROVLOG "
					+ "				WHERE CONTENT LIKE '%Req_Status=17%' AND TO_CHAR(REQTIME,'yyyyMM') = '"+sYearmonth2+"' AND  CONTENT LIKE '%GPRS_Status=0%' "
					+ "			) A "
					+ "		)C "
					+ "where B.IMSI = C. IMSI AND B.REQTIME = C.REQTIME ";
			
			logger.info("Query TWNLD request is 17 and status is 0 in last month:"+sql);
			rs = st.executeQuery(sql);
			while(rs.next()){
				//CHTSuspended.add(rs.getString("IMSI"));
				CHTSuspended.put(rs.getString("IMSI"), rs.getString("REQTIME"));
			}
		} catch (SQLException e) {
			ErrorHandle("At checkResume got SQLException",e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		
		for(Map<String,String> m: beenSuspended){
			String serviceid = m.get("serviceid");
			String s2tmsisdn = msisdnMap.get(serviceid).get("MSISDN");
			String imsi = ServiceIdtoIMSIMap.get(serviceid);
			
			if(s2tmsisdn == null){
				ErrorHandle("For service id = "+serviceid+" can't find msisdn!");
				continue;
			}
			if(imsi == null){
				ErrorHandle("For service id = "+serviceid+" can't find imsi!");
				continue;
			}
			//如果有中華供裝指定要關閉網路，則跳過
			if(CHTSuspended.keySet().contains(imsi)){
				logger.info(s2tmsisdn+" had required to suspend at "+CHTSuspended.get(imsi)+"!");
				resumeReport+= s2tmsisdn+" had been disable GPRS by CHT at "+CHTSuspended.get(imsi)+"!\n<br>";
				continue;
			}
			String gprsSatatus = Query_GPRSStatus(s2tmsisdn);
			//如果是已斷網狀態才進行復網
			if("0".equals(gprsSatatus)){
				logger.info("resume "+s2tmsisdn+"(suspended at "+m.get("date")+") GPRS");
				resumeReport+= s2tmsisdn+"(suspended at "+m.get("date")+") GPRS is resumed\n<br>";
				doResume(serviceid,s2tmsisdn);
			}else{
				logger.info(s2tmsisdn+"(suspended at "+m.get("date")+") GPRS is active");
				resumeReport+= s2tmsisdn+"(suspended at "+m.get("date")+") GPRS is active\n<br>";
			}
				
		}
		//mailReceiver=props.getProperty("mail.Receiver");
		logger.info("Send resumeReport result.");
		//sendMail("DVRS Resume Report.",resumeReport,"DVRS Alert","k1988242001@gmail.com,Yvonne.lin@sim2travel.com");
		sendMail("DVRS Resume Report.",resumeReport,"DVRS Alert","k1988242001@gmail.com");
		
	}
	
	/**
	 * 取出msisdn
	 * 建立msisdnMap
	 * Key:imsi,Value:Map(MSISDN,PRICEPLANID,SUBSIDIARYID,NCODE,SERVICEID)
	 * 增加serviceid to map，做以serviceid反查
	 */
	private boolean setMsisdnMap(){
		logger.info("setMsisdnMap...");
		msisdnMap.clear();
		long subStartTime = System.currentTimeMillis();
		
		Statement st = null;
		ResultSet rs = null;
		boolean result = false;
		sql=
				"SELECT B.IMSI,A.SERVICECODE,A.PRICEPLANID,A.SUBSIDIARYID,A.SERVICEID,B.ICCID, "
				+ "(CASE A. STATUS WHEN '1' then to_char(C.value) when '3' then to_char( C.value) when '10' then to_char(C.value) else null end) NCODE "
				+ "FROM SERVICE A,IMSI B,PARAMETERVALUE C "
				+ "WHERE A.SERVICEID=B.SERVICEID(+) AND A.SERVICECODE IS NOT NULL "
				+ "AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748";
		
		try {
			logger.info("Execute SQL :"+sql);
			st = conn2.createStatement();
			rs=st.executeQuery(sql);
			
			while(rs.next()){
				Map<String,String> map =new HashMap<String,String>();
				map.put("MSISDN", rs.getString("SERVICECODE"));
				map.put("PRICEPLANID", rs.getString("PRICEPLANID"));
				map.put("SUBSIDIARYID", rs.getString("SUBSIDIARYID"));
				map.put("NCODE", rs.getString("NCODE"));
				map.put("SERVICEID", rs.getString("SERVICEID"));
				map.put("IMSI", rs.getString("IMSI"));
				map.put("ICCID", rs.getString("ICCID"));
				msisdnMap.put(rs.getString("IMSI"), map);
				msisdnMap.put(rs.getString("SERVICEID"), map);
			}
			result = true;
		} catch (SQLException e) {
			ErrorHandle("At setMsisdnMap occur SQLException error!", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
		logger.info("execute time :"+(System.currentTimeMillis()-subStartTime));
		return result;
	}
	void sendMail(String mailSubject,String mailContent,String mailSender,String mailReceiver){
		String [] cmd=new String[3];
		cmd[0]="/bin/bash";
		cmd[1]="-c";
		cmd[2]= "/bin/echo \""+mailContent+"\" | /bin/mail -s \""+mailSubject+"\" -r "+mailSender+" "+mailReceiver;

		try{
			Process p = Runtime.getRuntime().exec (cmd);
			p.waitFor();
			if(logger!=null)
				logger.info("send mail cmd:"+cmd[2]);
			System.out.println("send mail cmd:"+cmd[2]);
		}catch (Exception e){
			if(logger!=null)
				logger.info("send mail fail:"+cmd[2]);
			System.out.println("send mail fail:"+cmd[2]);
		}
	}
	
	public void doResume(String serviceid,String phone){
		//中斷GPRS服務
		//20141113 新增客制定上限不執行斷網
		//20150529 將中斷的部分從發送簡訊中獨立出來

		
		String imsi = msisdnMap.get(serviceid).get("IMSI");
		if(imsi==null || "".equals(imsi))
			imsi = ServiceIdtoIMSIMap.get(serviceid);
		
		if(imsi==null || "".equals(imsi)){
			logger.debug("Resume GPRS fail because without imsi for serviceid is "+serviceid);
			return;
		}
		
		logger.debug("Resume GPRS ... ");		
		changeGPRSStatus(imsi,phone,"1");
	}
	Set<Map<String,String>> serviceOrderNBR = new HashSet<Map<String,String>>();
	/**
	 * 變更GPRS //20160115 change
	 * 
	 * @param imsi
	 * @param msisdn
	 */
	private void changeGPRSStatus(String imsi,String msisdn,String GPRSStatus){
		logger.info("changeGPRSStatus...");
		
		suspendGPRS sus = new suspendGPRS(conn,conn2,logger);
		PreparedStatement pst = null;
		try {
			
			sql = "";
			//20141118 add 傳回suspend排程的 service order nbr
			Map<String,String> orderNBR = sus.doChangeGPRSStatus(0,imsi, msisdn,GPRSStatus,"CHT-GPRS");
			serviceOrderNBR.add(orderNBR);
			sql=
					"INSERT INTO HUR_SUSPEND_GPRS_LOG  "
					+ "(SERVICE_ORDER_NBR,IMSI,CREATE_DATE,MSISDN,GPRS_STATUS) "
					+ "VALUES(?,?,SYSDATE,?,?)";
			
			pst=conn.prepareStatement(sql);
			pst.setString(1,orderNBR.get("cServiceOrderNBR") );
			pst.setString(2,imsi );
			pst.setString(3,msisdn );
			pst.setString(4,GPRSStatus );
			logger.info("Execute SQL : "+sql);
			
			pst.executeUpdate();
		} catch (SQLException e) {
			ErrorHandle("At changeGPRSStatus occur SQLException error!", e);
		} catch (IOException e) {
			sql="";
			ErrorHandle("At changeGPRSStatus occur IOException error!", e);
		} catch (ClassNotFoundException e) {
			sql="";
			ErrorHandle("At changeGPRSStatus occur ClassNotFoundException error!", e);
		} catch (Exception e) {
			sql="";
			ErrorHandle("At changeGPRSStatus occur Exception error!", e);
		}finally{
			try {
				if(pst!=null) pst.close();
				if(sus.Temprs!=null) sus.Temprs.close();
			} catch (SQLException e) {
			}
		}
	}
	
	public String Query_GPRSStatus(String s2tmsisdn){
		 logger.info("check_GPRSStatus...");
		 String sG="";
	     String cGPRS="";
	     
	     sql="SELECT nvl(PDPSUBSID,0) as ab FROM basicprofile WHERE msisdn = '"+s2tmsisdn+"'";
	     logger.debug("Query_GPRSStatus:"+sql);
	          
	     Statement st = null;
	     ResultSet rs = null;
	     try {
	    	 st = conn.createStatement();
			 rs = st.executeQuery(sql);
			 while(rs.next()){
				 sG=rs.getString("ab");
			 }
			 logger.debug("GPRS_Values:"+sG);
			 if ((sG.equals("0"))||(sG.equals(""))){
				 cGPRS="0";
			 }else {
				 cGPRS="1";
			 }
		} catch (SQLException e) {
			ErrorHandle("At Query_GPRSStatus occur SQLException error!", e);
		}finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {
			}
		}
	     return cGPRS;
	}
	
	public static void ErrorHandle(String cont){
		ErrorHandle(cont,null);
	}
	
	static String errorMsg = "";
	public static void ErrorHandle(String cont,Exception e){
		if(e!=null){
			logger.error(cont, e);
			
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			//send mail
			errorMsg=s.toString();
		}else{
			logger.error(cont);
			errorMsg="";
		}
		
		sendErrorMail(cont);
	}
	
	//mail conf
	static String mailSender="";
	static String mailReceiver="";
	static String mailSubject="mail test";
	static String mailContent="mail content text";
	
	static void sendErrorMail(String msg){

		mailReceiver=props.getProperty("mail.Receiver");
		mailSubject="DVRS Warnning Mail";
		mailContent="Error :"+msg+"<br>\n"
				+ "Error occurr time: "+DateFormat()+"<br>\n"
				+ "SQL : "+sql+"<br>\n"
				+ "Error Msg : "+errorMsg;	
		
		String [] cmd=new String[3];
		cmd[0]="/bin/bash";
		cmd[1]="-c";
		cmd[2]= "/bin/echo \""+mailContent+"\" | /bin/mail -s \""+mailSubject+"\" -r DVRS_ALERT "+mailReceiver;

		try{
			Process p = Runtime.getRuntime().exec (cmd);
			p.waitFor();
			if(logger!=null)
				logger.info("send mail cmd:"+cmd[2]);
			System.out.println("send mail cmd:"+cmd[2]);
		}catch (Exception e){
			if(logger!=null)
				logger.info("send mail fail:"+cmd[2]);
			System.out.println("send mail fail:"+cmd[2]);
		}
	}
	
	public static String DateFormat(){
		DateFormat dateFormat = new SimpleDateFormat(iniform);
		return dateFormat.format(new Date());
	}

}

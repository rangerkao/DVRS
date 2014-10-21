package dao;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import program.IJatool;
import program.Jatool;
import program.RFPmain;
import control.BillReport;

public class BaseDao {

	protected Properties props =new Properties();
	protected Logger logger ;
	protected Connection conn =null;
	protected IJatool tool=new Jatool();
	protected String sql="";
	protected String classPath = BillReport.class.getClassLoader().getResource("").toString().replace("file:/", "").replace("%20", " ");
	private String logDB="INSERT INTO HUR_MANERGER_LOG "
			+ "(ID,USERID,PAGE,ACTION,PARAMETER,CREATE_DATE) "
			+ "VALUES(HUR_MANERGE_ID.NEXTVAL,?,?,?,?,SYSDATE)";
	
	public BaseDao(){
		loadProperties();
		connectDB();
	}
	protected void loadProperties() {
		System.out.println("initial Log4g, property !");

		String path=classPath+ "/log4j.properties";
		try {
			props.load(new FileInputStream(path));
			PropertyConfigurator.configure(props);
			logger = Logger.getLogger(RFPmain.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("File Not Found : " + e.getMessage());
			System.out.println("File Path : " + path);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IOException : " + e.getMessage());
		}
	}
	protected void connectDB(){
		try {
			conn=tool.connDB(logger, props.getProperty("Oracle.DriverClass"), 
					props.getProperty("Oracle.URL")
					.replace("{{Host}}", props.getProperty("Oracle.Host"))
					.replace("{{Port}}", props.getProperty("Oracle.Port"))
					.replace("{{ServiceName}}", props.getProperty("Oracle.ServiceName")), 
					props.getProperty("Oracle.UserName"), 
					props.getProperty("Oracle.PassWord"));
			if(conn==null){
				throw new Exception("DB Connect null !");
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public int loggerAction(String userid,String page,String action,String parameter){
		
		int result=0;
		 try {
			PreparedStatement pst = conn.prepareStatement(logDB);
			pst.setString(1, userid);
			pst.setString(2, page);
			pst.setString(3, action);
			pst.setString(4, parameter);
			result= pst.executeUpdate();
			
			pst.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		 closeConnect();
		
		 return result;
	}
	
	
	/**
	 * Ãö³¬³s½u
	 */
	protected void closeConnect() {
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
	}
}

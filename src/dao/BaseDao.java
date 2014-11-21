package dao;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import program.IJatool;
import program.Jatool;
import program.DVRSmain;
import control.BillReport;

public class BaseDao {

	protected Properties props =new Properties();
	protected Logger logger ;
	protected Connection conn =null;
	protected Connection conn2 =null;
	protected IJatool tool=new Jatool();
	protected String sql="";
	protected String classPath = BillReport.class.getClassLoader().getResource("").toString().replace("file:", "").replace("%20", " ");
	
	public BaseDao() throws Exception{
		System.out.println("Base Dao InI...");
		loadProperties();
		connectDB();
		connectDB2();
	}
	protected void loadProperties() throws FileNotFoundException, IOException {
		String path=classPath+ "/program/Log4j.properties";
			props.load(new FileInputStream(path));
			PropertyConfigurator.configure(props);
			

	}
	protected void connectDB() throws Exception{
		String url=props.getProperty("Oracle.URL")
				.replace("{{Host}}", props.getProperty("Oracle.Host"))
				.replace("{{Port}}", props.getProperty("Oracle.Port"))
				.replace("{{ServiceName}}", (props.getProperty("Oracle.ServiceName")!=null?props.getProperty("Oracle.ServiceName"):""))
				.replace("{{SID}}", (props.getProperty("Oracle.SID")!=null?props.getProperty("Oracle.SID"):""));
		conn=tool.connDB(logger, props.getProperty("Oracle.DriverClass"), url, 
				props.getProperty("Oracle.UserName"), 
				props.getProperty("Oracle.PassWord")
				);
			if(conn==null){
				throw new Exception("DB Connect null !");
			}

	}
	
	protected void connectDB2() throws Exception {
		String url=props.getProperty("mBOSS.URL")
				.replace("{{Host}}", props.getProperty("mBOSS.Host"))
				.replace("{{Port}}", props.getProperty("mBOSS.Port"))
				.replace("{{ServiceName}}", (props.getProperty("mBOSS.ServiceName")!=null?props.getProperty("mBOSS.ServiceName"):""))
				.replace("{{SID}}", (props.getProperty("mBOSS.SID")!=null?props.getProperty("mBOSS.SID"):""));
		conn2=tool.connDB(logger, props.getProperty("mBOSS.DriverClass"), url, 
				props.getProperty("mBOSS.UserName"), 
				props.getProperty("mBOSS.PassWord")
				);
			if(conn2==null){
				throw new Exception("DB Connect2 null !");
			}

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

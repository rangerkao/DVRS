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
import program.RFPmain;
import control.BillReport;

public class BaseDao {

	protected Properties props =new Properties();
	protected Logger logger ;
	protected Connection conn =null;
	protected IJatool tool=new Jatool();
	protected String sql="";
	protected String classPath = BillReport.class.getClassLoader().getResource("").toString().replace("file:", "").replace("%20", " ");
	
	public BaseDao() throws Exception{
		System.out.println("Base Dao InI...");
		loadProperties();
		connectDB();
	}
	protected void loadProperties() throws FileNotFoundException, IOException {
		String path=classPath+ "/log4j.properties";
			props.load(new FileInputStream(path));
			PropertyConfigurator.configure(props);
			logger = Logger.getLogger(RFPmain.class);

	}
	protected void connectDB() throws Exception{

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

	}
	
	protected void connectDB2() throws NamingException, SQLException{
			 	DataSource dataSource;
	            // Get DataSource
	            Context initContext  = new InitialContext();
	            Context envContext  = (Context)initContext.lookup("java:/comp/env");
	            dataSource = (DataSource)envContext.lookup("jdbc/testdb");
	            conn = dataSource.getConnection();
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

package control;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import bean.Admin;
import bean.User;
import program.IJatool;
import program.Jatool;
import program.RFPmain;

public class Login {
	Properties props =new Properties();
	Logger logger ;
	Connection conn =null;
	IJatool tool=new Jatool();
	String sql="";
	
	private void loadProperties() {
		System.out.println("initial Log4g, property !");
		String path = BillReport.class.getClassLoader().getResource("").toString().replace("file:/", "")
				+ "/log4j.properties";
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
	
	public String loginC(Map session,String account,String password){
		
		loadProperties();
		logger.info("loginC...");
		String pass="",role="",msg="";
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
			sql=
					"SELECT A.PASSWORD,A.ROLE "
					+ "FROM HUR_ADMIN A "
					+ "WHERE A.ACCOUNT=? ";
			
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, account);
			
			ResultSet rs=pst.executeQuery();
			
			while(rs.next()){
				pass=rs.getString("PASSWORD");
				role=rs.getString("ROLE");
			}
			
			if(pass==null || "".equals(pass)){
				msg="Account error or without !";
				logger.error(account+" "+msg);
			}else if(!pass.equals(password)){
				msg="PassWord Error !";
				logger.error(account+" "+msg);
			}else{
				msg="success";
				User user=new User(account,role);
				session.put("s2tUser", user);
			}
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("DB Connect Error :"+e.getMessage());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("DB Connect Error :"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("DB Connect Error :"+e.getMessage());
		}
		
		
		return msg;
	}

}

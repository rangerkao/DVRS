package control;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import dao.BaseDao;
import program.IJatool;
import program.Jatool;
import program.RFPmain;

public class BaseControl {

	Properties props =new Properties();
	Logger logger ;
	private BaseDao baseDao=new BaseDao();
	public BaseControl(){
		loadProperties();
	}
	
	private void loadProperties() {
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
	
	public int loggerAction(String userid,String page,String action,String parameter){
		return baseDao.loggerAction(userid, page, action, parameter);
	}
	
	
}

package control;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import program.IJatool;
import program.Jatool;
import program.RFPmain;
import dao.BaseDao;

public class BaseControl {

	Properties props =new Properties();
	Logger logger ;
	protected String classPath = BillReport.class.getClassLoader().getResource("").toString().replace("file:", "").replace("%20", " ");
	public BaseControl() throws Exception{
		loadProperties();
	}
	
	private void loadProperties() throws FileNotFoundException, IOException {
			String path=classPath+ "/log4j.properties";
			props.load(new FileInputStream(path));
			PropertyConfigurator.configure(props);
			logger = Logger.getLogger(RFPmain.class);
	}	
}

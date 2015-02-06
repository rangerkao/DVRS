package control;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;

public class BaseControl {

	Properties props =new Properties();
	Logger logger ;
	protected String classPath = BillReport.class.getClassLoader().getResource("").toString().replace("file:", "").replace("%20", " ");
	public BaseControl() throws Exception{
		loadProperties();
	}
	
	private void loadProperties() throws FileNotFoundException, IOException {
			String path=classPath+ "/program/Log4j.properties";
			props.load(new FileInputStream(path));
			PropertyConfigurator.configure(props);
	}	
	
	protected String beanToJSONArray(List list){
		JSONArray jo = (JSONArray) JSONObject.wrap(list);
		return jo.toString();
	}
	protected String beanToJSONObject(Object object){
		JSONObject jo = (JSONObject) JSONObject.wrap(object);
		return jo.toString();
	}
}

package program;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.buf.UEncoder;

public class hello {
	private static String msg;
	public static void main(String[] args) throws IOException{
		System.out.println("Hello!");
		
		Jatool tool =new Jatool();
		
		Date d =new Date(new Date().getTime()-1000*60*60);
			
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-6);
		calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR)-1);
		
		Connection conn=getConnection();
		
		if(conn==null){
			System.out.println("connection is null");
			
		}else{
			try {
				String rss=null;
				ResultSet rs=conn.createStatement().executeQuery("select A.comtent from HUR_SMS_COMTENT A WHERE A.id=100");
			
				while(rs.next()){
					rss=rs.getString("comtent");
					rss=new String(rss.getBytes("big5"),"UTF8");
					System.out.println(rss);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			
		}
		
		
		
		
		Integer id=100;
		Map<String,String> map =new HashMap<String,String>();
		map.put("100","asdff");
		map.put("101","asdffBBBBBBBBBBBBBBBBB");
		System.out.println(map.get(id));
		id+=1;
		System.out.println(map.get(id.toString()));
		
		System.out.println(Pattern.matches("^\\d+(.\\d+)?", "y5450.5345"));
		
		System.out.println(tool.FormatNumString(500000D, "NT#,##0.00"));
		
	}

	
	
	static Connection getConnection()
	  {
		
		try
	    {
	      //Class.forName("org.postgresql.Driver");
			Class.forName("oracle.jdbc.driver.OracleDriver");
	    }
	    catch (Exception localException)
	    {
	      System.err.println("ERROR: failed to load Informix JDBC driver.");
	      msg = ("ERROR: failed to load Informix JDBC driver." + localException.getMessage());
	      return null;
	    }
		
	    Connection localConnection = null;
	    try
	    {
	      DriverManager.setLoginTimeout(10);
	      //localConnection = DriverManager.getConnection("jdbc:postgresql://192.168.10.197:5432/smppdb", "smpper", "SmIpp3r");
	      localConnection = DriverManager.getConnection("jdbc:oracle:thin:@10.42.1.101:1521:S2TBSDEV", "foyadev", "foyadev");
	    }
	    catch (Exception localException)
	    {
	      System.err.println("ERROR DB: failed to connect!");
	      msg = ("ERROR DB: failed to connect!" + localException.getMessage());
	    }
	    return localConnection;
	  }
}

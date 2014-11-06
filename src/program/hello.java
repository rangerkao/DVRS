package program;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.buf.UEncoder;

public class hello {
	private static String msg;
	public static void main(String[] args) throws IOException{
		System.out.println("Hello!");
		
		Jatool tool =new Jatool();
		try {
			tool.DateFormat("2014/10/06 08:56:55", "yyyy/MM/dd HH:mm:ss");
			SimpleDateFormat dFormat2=new SimpleDateFormat("yyMMddHHmm");

			System.out.println(dFormat2.format(new Date()));
			
		 	System.out.println(tool.getDayLastDate(new Date()));
		 	 
		 	Connection conn =getConnection();
		 	System.out.println(msg);
		 	try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 	
		 	
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	static Connection getConnection()
	  {
		
		try
	    {
	      Class.forName("org.postgresql.Driver");
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
	      localConnection = DriverManager.getConnection("jdbc:postgresql://192.168.10.197:5432/smppdb", "smpper", "SmIpp3r");
	    }
	    catch (Exception localException)
	    {
	      System.err.println("ERROR DB: failed to connect!");
	      msg = ("ERROR DB: failed to connect!" + localException.getMessage());
	    }
	    return localConnection;
	  }
}

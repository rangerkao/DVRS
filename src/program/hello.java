package program;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
		
		String param="*環球卡*感謝您的回覆*，我們已為您取消用量達約NTD5,000時自動斷網之管制，祝您一切順利。如需諮詢請電客服{{customerService}}。";
		
		updateDB(5,param);
		
		
		
		Jatool tool =new Jatool();
		
		Date d =new Date(new Date().getTime()-1000*60*60);
			
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-6);
		calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR)-1);
		
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
	      //System.Environment.SetEnvironmentVariable("NLS_LANG", "AMERICAN_AMERICA.WE8ISO8859P1");
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
	
	static void updateDB(int num,String param) throws UnsupportedEncodingException{
Connection conn=getConnection();
		
		if(conn==null){
			System.out.println("connection is null");
			
		}else{

			try {
				
				if(param!=null && !"".equals(param)){
					PreparedStatement pst = conn.prepareStatement("UPDATE HUR_SMS_COMTENT A SET A.COMTENT =? WHERE A.ID=?");
					String pm=new String(param.getBytes(),"ISO8859-1");
					pst.setString(1, pm);
					pst.setInt(2, num);
					pst.executeUpdate();
					
					pst.close();
				}

				PreparedStatement pst2 = conn.prepareStatement("select A.comtent from HUR_SMS_COMTENT A WHERE A.ID=?");
				pst2.setInt(1, num);
				ResultSet rs=pst2.executeQuery();
			
				while(rs.next()){
					String rss=rs.getString("comtent");
					rss=new String(rss.getBytes("ISO8859-1"));
					System.out.println(rss);
				}

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		}
	}
}

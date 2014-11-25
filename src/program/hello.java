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
		
		String param="*環球卡用量通知*提醒您本月上網金額累計已逾{{bracket}}，實際使用金額以帳單為準。除中國、香港、澳門有每日收費上限外，其餘國家按實際用量收費，不提供吃到飽方案，請謹慎使用。若您有申請中華電信日租型上網，請立即切換至中華電信漫遊。如欲使用通話功能，請務必切回環球卡以便節費。若您不知如何操作，請電客服{{customerService}}。";
		
		//updateDB(99,param);
		//updateDB(99,null);
		
		
		Jatool tool =new Jatool();
		
		Date d =new Date(new Date().getTime()-1000*60*60);
			
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-6);
		calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR)-1);
		
		
		System.out.println(Pattern.matches("^\\d+(.\\d+)?", "y5450.5345"));
		
		System.out.println(tool.FormatNumString(500000D, "NT#,##0.00"));
		
		System.out.println(tool.FormatDouble(0.0000D, "0.0000"));
		
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
	      //localConnection = DriverManager.getConnection("jdbc:oracle:thin:@10.42.1.80:1521:s2tbs", "s2tbsadm", "s2tbsadm");
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

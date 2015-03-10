package program;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.buf.UEncoder;

public class hello {
	private static String msg;
	public static void main(String[] args) {
		
		
		
		
		System.out.println("Hello!");
		
		System.out.println("\t:\t1\t2\t3\t4\t5\t6\t7\t8\t9");
		for(int i=1;i<=9;i++){
			System.out.print(i+"\t:");
			for(int j=1;j<=9;j++)
				System.out.print("\t"+(i*j));
			System.out.println();
		}
		
		
		String ip ="";
    	
    	try {
			ip=InetAddress.getLocalHost().getHostAddress()+"";
		} catch (UnknownHostException e) {
			ip="unknow";
			e.printStackTrace();
		}
    	System.out.println(ip);
		
		System.out.println(new Date(new Date().getTime()+600000));
		
		String ipAddr="221.177.44.235";

		if(ipAddr.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")){
			String [] ips = ipAddr.split("\\.");
			long ipNumber =0L;
			for(int i=0;i<ips.length;i++){
				ipNumber+=Integer.parseInt(ips[i])*Math.pow(256, 3-i);
			}
			System.out.println("ipNumber="+ipNumber);
			
		}

		String param="´ú¸Õ123.33215´ú¦¸°¼°¼°¼";
		
		//query();
		//updateDB(999,param);
		//updateDB(999,null);
		
		String msisdn="85288923545";
		
		System.out.println(msisdn.substring(3,msisdn.length()));
		
		System.out.println("1".endsWith("0"));
		
		Jatool tool =new Jatool();
		
		Date d =new Date(new Date().getTime()-1000*60*60*24);
		System.out.println("day : "+d);
		
		
		Calendar calendar = Calendar.getInstance();
		//calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE)-120);
		//calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)-1);
		calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE)-60);
		
		System.out.println(Calendar.getInstance().get(Calendar.MONTH)+1);
		System.out.println("calendar "+calendar.getTime());
		
		System.out.println(Pattern.matches("^\\d+(.\\d+)?", "y5450.5345"));
		
		System.out.println("1235461p".matches("^\\d+$"));
		
		System.out.println(tool.FormatNumString(500000D, "NT#,##0.00"));
		
		System.out.println(tool.FormatDouble(0.0000D, "0.0000"));
		
		List<String> list = tool.regularFind("32321,dsd,434,11,aas,4356,643,234,rer,123,442,1,1233,331", "^\\d{3}\\D|\\D\\d{3}\\D|\\D\\d{3}$");
		
		System.out.println("Double Max\t:\t"+Double.MAX_VALUE);
		System.out.println("Long Max\t:\t"+Long.MAX_VALUE);
		System.out.println("Integer Max\t:\t"+Integer.MAX_VALUE);
		
		/*for(int i =1000 ;i<2000;i++){
			
			String t=String.valueOf(i);
			for(int j=4-t.length();j>0;j--){
				t="0"+t;
			}
			System.out.println("88698909"+t);
		}*/

		
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
					String pm=new String(param.getBytes("BIG5"),"ISO8859-1");
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
					rss=new String(rss.getBytes("ISO8859-1"),"BIG5");
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
	
	static void query(){
		Connection conn=getConnection();
		
		String sql = "select ch_name from MARKETING_DB_COUNTRY A ";

		if(conn==null){
			System.out.println("connection is null");
			
		}else{

			Statement st = null ;
			ResultSet rs = null ;
			PreparedStatement ps = null ;
			try {

				st=conn.createStatement();
				rs=st.executeQuery(sql);
				
				while(rs.next()){
					String rss=rs.getString("ch_name");
					
					if(rss!=null){
						rss=new String(rss.getBytes("ISO8859-1"),"BIG5");
						System.out.println(rss);
					}
				}
				
				Map<String,String> setcountry = new HashMap<String,String>();
				
				
	

				
				sql = "update MARKETING_DB_DATA A set A.country=? where country=? ";
				
				ps = conn.prepareStatement(sql);
				
				for(String s : setcountry.keySet()){
					ps.setString(1, setcountry.get(s));
					ps.setString(2, new String(s.getBytes("BIG5"),"ISO8859-1"));
					System.out.println("effected row "+ps.executeUpdate());
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(ps!=null){
					try {
						ps.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if(st!=null){
					try {
						st.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if(rs!=null){
					try {
						rs.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
}

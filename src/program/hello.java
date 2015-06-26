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
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.apache.tomcat.jni.Local;
import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.buf.UEncoder;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;

public class hello {
	private static String msg;
	static IJatool tool =new Jatool();
	public static void main(String[] args) throws UnknownHostException {
		

		//時間測試
		/*try {
			Date testTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse("2015/06/23 15:35:44");
			
			Calendar startTime = Calendar.getInstance(),endTime = Calendar.getInstance();
			startTime.setTime(testTime);
			startTime.set(Calendar.HOUR_OF_DAY, 0);
			startTime.set(Calendar.MINUTE, 0);
			startTime.set(Calendar.SECOND, 0);
			System.out.println(startTime.getTime());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		

		//簡訊測試   分段
		String msg = "*環球卡高用量提醒*提醒您本月數據用量上網金額統計至前{{bp}}一小時加部份推估，已約達NT65,000.00，"
				+ "實際用量及使用金額以帳單為準。另您已{{bp}}約定上網金額不設限，感謝您的留意。如需諮詢請電客服*123#。";
		
	
		/*int number = 100;
		int length = msg.length();
		byte[] b = msg.getBytes();
		length = b.length;
		int msgN = length/number;
		if(length%number>0)
			msgN += 1;
		String [] sub =new String[msgN];
		
		for(int i=0;i<msgN;i++){
			int last = (i+1)*number;
			if(last>length)
				last=length;
			
			byte[] c=new byte[last-i*number];
			
			System.arraycopy(b, i*number , c, 0, last-i*number);

				sub[i]=new String(c);
	
			sub[i]=msg.substring(i*number,last);
		}*/
		
		String [] sub ={msg.replaceAll("\\{\\{bp\\}\\}","")};
		//String [] sub =msg.split("\\{\\{bp\\}\\}");
		for(String s: sub){
			System.out.println(s);
		}
		/*
		try {
			setSMSPostParam(new String("中文測試".getBytes("BIG5"), "ISO-8859-1"), "886989235253");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	
		//DB測試
		String param="測試123.33215測次側側側";
		
		//query();
		//updateDB(999,param);
		//updateDB(999,null);
		Calendar calendar =Calendar.getInstance();
		//System.out.println(new SimpleDateFormat("yyyy MMM dd HH:mm:ss", Locale.US).format(calendar.getTime()));
		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)+1,0,0,0);
		System.out.println("calendar:"+calendar.getTime());
		
		List<String> list = tool.regularFind("32321,dsd,434,11,aas,4356,643,234,rer,123,442,1,1233,331", "^\\d{3}\\D|\\D\\d{3}\\D|\\D\\d{3}$");
		
		
		System.out.println("Double Max\t:\t"+Double.MAX_VALUE);
		System.out.println("Long Max\t:\t"+Long.MAX_VALUE);
		System.out.println("Integer Max\t:\t"+Integer.MAX_VALUE);
		
		
		

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
				st.executeUpdate(sql);
				/*rs=st.executeQuery(sql);
				
				while(rs.next()){
					String rss=rs.getString("ch_name");
					
					if(rss!=null){
						rss=new String(rss.getBytes("ISO8859-1"),"BIG5");
						System.out.println(rss);
					}
				}*/
				
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
	
	 private static String setSMSPostParam(String msg,String phone) throws IOException{
			StringBuffer sb=new StringBuffer ();

			String PhoneNumber=phone,Text=msg,charset="big5",InfoCharCounter=null,PID=null,DCS=null;
			String param =
					"PhoneNumber=+{{PhoneNumber}}&"
					+ "Text={{Text}}&"
					+ "charset={{charset}}&"
					+ "InfoCharCounter={{InfoCharCounter}}&"
					+ "PID={{PID}}&"
					+ "DCS={{DCS}}&"
					+ "Submit=Submit";
			
			if(PhoneNumber==null)PhoneNumber="";
			if(Text==null)Text="";
			if(charset==null)charset="";
			if(InfoCharCounter==null)InfoCharCounter="";
			if(PID==null)PID="";
			if(DCS==null)DCS="";
			param=param.replace("{{PhoneNumber}}",PhoneNumber );
			param=param.replace("{{Text}}",Text );
			param=param.replace("{{charset}}",charset );
			param=param.replace("{{InfoCharCounter}}",InfoCharCounter );
			param=param.replace("{{PID}}",PID );
			param=param.replace("{{DCS}}",DCS );
			
			
			
			return tool.HttpPost("http://192.168.10.125:8800/Send%20Text%20Message.htm", param,"");
		}
}

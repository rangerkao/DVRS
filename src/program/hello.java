package program;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.buf.UEncoder;

public class hello {

	public static void main(String[] args) throws IOException{
		System.out.println("Hello!");
		
		Jatool tool =new Jatool();
		try {
			tool.DateFormat("2014/10/06 08:56:55", "yyyy/MM/dd HH:mm:ss");
			Calendar calendar = Calendar.getInstance();//默認為當前時間
			calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR)-1);
			
			String PhoneNumber="886235253",Text="testMathod",charset="big5",InfoCharCounter=null,PID=null,DCS=null;
			
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

			System.out.println(tool.HttpPost("http://192.168.10.125:8800/Send%20Text%20Message.htm", param,""));
			
		 	System.out.println(tool.getDayLastDate(new Date()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

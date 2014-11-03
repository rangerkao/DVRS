package program;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
			SimpleDateFormat dFormat2=new SimpleDateFormat("yyMMddHHmm");

			System.out.println(dFormat2.format(new Date()));
			
		 	System.out.println(tool.getDayLastDate(new Date()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

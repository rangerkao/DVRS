package control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProgramControl extends BaseControl {
	
	
	
	
	public ProgramControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	/*public String execute(String filename) throws IOException, InterruptedException{
		String result=null;
		String rError=null;
		String line;
		
			String shpath = classPath+"source/"+filename;
			Process ps = Runtime.getRuntime().exec("sh "+shpath);
			//Process ps = Runtime.getRuntime().exec(shpath);
			System.out.println("Execut:"+shpath);			
			
			ps.waitFor();
			//getErrorStream

			BufferedReader bre = new BufferedReader(new InputStreamReader(
					ps.getErrorStream()));
			StringBuffer sbe = new StringBuffer();
			while ((line = bre.readLine()) != null) {
				sbe.append(line).append("\n<br>");
			}
			rError=sbe.toString();	
			
		//getInputStream result	
			BufferedReader br = new BufferedReader(new InputStreamReader(
					ps.getInputStream()));
			StringBuffer sb = new StringBuffer();
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n<br>");
			}
			result = sb.toString();
			
			
			result=
					"<Error>\n<br>"
					+ rError+"\n<br>"
					+ "</Error>\n<br>"
					+ "\n<br>"
					+ result;
			;
			System.out.println(result);
			
			if(result==null ||"".equals(result)){
				result="Run Success!";
			}
		
		return result;
	}*/
}

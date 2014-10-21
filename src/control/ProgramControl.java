package control;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ProgramControl extends BaseControl {
	protected String classPath = BillReport.class.getClassLoader().getResource("").toString().replace("file:/", "").replace("%20", " ");
	
	public String execute(String filename){
		String result=null;
		
		try {
			String shpath = classPath+"source/"+filename;
			Process ps = Runtime.getRuntime().exec(shpath);
			System.out.println("Execut:"+shpath);
			ps.waitFor();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					ps.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n<br>");
			}
			result = sb.toString();
			System.out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return result;
	}
}

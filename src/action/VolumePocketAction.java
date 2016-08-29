package action;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

import org.json.JSONObject;

import control.VolumePocketControl;
import bean.VolumePocket;

public class VolumePocketAction extends BaseAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public VolumePocketAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	String input;
	VolumePocketControl volumePocketControl = new VolumePocketControl();
	
	
	public String checkCustomer(){
		try {
			result=makeResult(volumePocketControl.checkCustomer(input),null);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		return SUCCESS;
	}
	
	public String queryVolumePocketList(){
		try {
			List<VolumePocket> list = null;
			if(input ==null ||"".equals(input)){
				list=volumePocketControl.queryVolumePocketList();
			}else{
				list=volumePocketControl.queryVolumePocketList(input);
			}
			result=makeResult(list,null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "VolumePocketSetting", "query", input, SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		return SUCCESS;
	}
	
	public String inserVolumePocket(){
		try {
			System.out.println("INPUT="+input);
			JSONObject j= new JSONObject(input);
			VolumePocket v = new VolumePocket(j);
			
			List<VolumePocket> list = volumePocketControl.inserVolumePocket(v);
			result=makeResult(list,null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "VolumePocketSetting", "insert", input, SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		return SUCCESS;
	}
	
	public String updateVolumePocket(){
		try {
			System.out.println("INPUT="+input);
			JSONObject j= new JSONObject(input);
			VolumePocket v = new VolumePocket(j);
			
			List<VolumePocket> list = volumePocketControl.updateVolumePocket(v);
			result=makeResult(list,null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "VolumePocketSetting", "update", input, SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		return SUCCESS;
	}
	
	public String cancelVolumePocket(){
		try {
			System.out.println("INPUT="+input);
			JSONObject j= new JSONObject(input);
			VolumePocket v = new VolumePocket(j);
			
			List<VolumePocket> list = volumePocketControl.cancelVolumePocket(v);
			result=makeResult(list,null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "VolumePocketSetting", "cancel", input, SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		return SUCCESS;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	
	
	
}

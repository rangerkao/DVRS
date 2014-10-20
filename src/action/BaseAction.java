package action;

import java.util.List;
import java.util.Map;

import org.apache.struts2.interceptor.SessionAware;
import org.json.JSONArray;
import org.json.JSONObject;

import program.IJatool;
import program.Jatool;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

import control.BaseControl;

public class BaseAction extends ActionSupport implements SessionAware {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected String result;
	protected Map<String, Object> session;
	private BaseControl baseControl=new BaseControl();
	protected IJatool tool= new Jatool();
	
/*	public void setSession() {
		ActionContext ac = ActionContext.getContext();
		this.session = ac.getSession();
	}*/
	protected String beanToJSONArray(List list){
		JSONArray jo = (JSONArray) JSONObject.wrap(list);
		return jo.toString();
	}
	protected String beanToJSONObject(Object object){
		JSONObject jo = (JSONObject) JSONObject.wrap(object);
		return jo.toString();
	}
	
	protected void loggerAction(String userid,String page,String action,String parameter){
		baseControl.loggerAction(userid, page, action, parameter);
	}
	
	 public String getResult() {
	  return result;
	 }
	public void setResult(String result) {
		this.result = result;
	}
	@Override
	public void setSession(Map<String, Object> session) {
		this.session=session;
		
	}
	public Map<String, Object> getSession() {
		return session;
	}
	 
}

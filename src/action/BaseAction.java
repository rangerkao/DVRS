package action;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.struts2.interceptor.SessionAware;
import org.json.JSONArray;
import org.json.JSONObject;

import bean.User;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

import control.ActionLogControl;
import control.BaseControl;

public class BaseAction extends ActionSupport implements SessionAware {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static String result;
	protected String exception;
	protected Map<String, Object> session;
	protected ActionLogControl actionLogControl = new ActionLogControl();
	
	public BaseAction() throws Exception {
		super();
	}
	
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
	protected User getUser(){
		return (User) session.get("s2tUser");
	}
	protected String makeResult(Object data,String error){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("data", data);
		if(error!=null)
			map.put("error", error);
		
		return beanToJSONObject(map);
	}
	
	//-----------------------------------------------------------------------------//
	
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

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}
	
	
	
	
	 
}

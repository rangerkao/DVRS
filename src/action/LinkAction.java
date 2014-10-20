package action;

import java.util.Map;


import com.opensymphony.xwork2.ActionContext;

import control.LogoutControl;

public class LinkAction extends BaseAction{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String tag;
	private String LOGIN="login";
	private String BILL="bill";
	private String Admin="admin";
	private String DataRate="dataRate";
	private String SmsQuery="smsQuery";
	Map<String, Object> session;
	
	LogoutControl logoutControl=new LogoutControl();
	
	public String bill()  
	{  
	   return BILL;        
	}  
	public String admin()  
	{  
	   return Admin;        
	}  
	public String dataRate()  
	{  
	   return DataRate;        
	}  
	public String smsQuery()  
	{  
	   return SmsQuery;        
	}  
	   
	public String logout()  
	{  
		ActionContext ac = ActionContext.getContext();
		Map session = ac.getSession();
		
		logoutControl.execute(session);
		setTag("你已經登出！");
		return LOGIN;         
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}

package action;

import java.util.Map;

import org.apache.struts2.interceptor.SessionAware;

import bean.Invoice;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

import control.BillReport;
import control.Logout;

public class LinkAction extends ActionSupport{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String tag;
	private String LOGIN="login";
	private String BILL="bill";
	Map<String, Object> session;
	
	Logout logout=new Logout();
	
	public String bill()  
	{  
	   return BILL;        
	}  
	   
	public String logout()  
	{  
		ActionContext ac = ActionContext.getContext();
		Map session = ac.getSession();
		
		logout.execute(session);
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

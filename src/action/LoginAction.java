package action;

import java.util.Map;

import org.apache.struts2.interceptor.SessionAware;

import bean.Invoice;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

import control.Login;



public class LoginAction extends ActionSupport{
		
	   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String userid;
	private String password;
	private String tag;
	Map<String, Object> session;
	
	Login login=new Login();

	public void validate() {
		if (userid == null || "".equals(userid))
			addFieldError("userid", "帳號為必填，請輸入帳號");
		if (password == null || "".equals(password))
			addFieldError("password", "密碼為必填，請輸入密碼");
	}

	public String execute() throws Exception {

		ActionContext ac = ActionContext.getContext();
		Map session = ac.getSession();
		
		
		tag=login.loginC(session,userid,password);
		if(!"success".equals(tag)) return "input";
		else return "success";
		
	}


	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}

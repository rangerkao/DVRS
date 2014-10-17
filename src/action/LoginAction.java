package action;

import java.util.Map;

import com.opensymphony.xwork2.ActionContext;

import control.LoginControl;



public class LoginAction extends BaseAction{
		
	   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String account;
	private String password;
	private String tag;
	Map<String, Object> session;
	
	LoginControl logincControl=new LoginControl();

	public void validate() {
		if (account == null || "".equals(account))
			addFieldError("account", "帳號為必填，請輸入帳號");
		if (password == null || "".equals(password))
			addFieldError("password", "密碼為必填，請輸入密碼");
	}

	public String execute() throws Exception {

		ActionContext ac = ActionContext.getContext();
		Map session = ac.getSession();

		tag=logincControl.loginC(session,account,password);
		if(!"success".equals(tag)) return "input";
		else return "success";
		
	}


	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
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

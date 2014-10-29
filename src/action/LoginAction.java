package action;

import java.util.Map;

import bean.User;

import com.opensymphony.xwork2.ActionContext;

import control.LoginControl;



public class LoginAction extends BaseAction{
		
	   public LoginAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String acc;
	private String psw;
	private String tag;
	private String page="Login";
	
	LoginControl logincControl=new LoginControl();

	public void validate() {
		if (acc == null || "".equals(acc))
			addFieldError("acc", "帳號為必填，請輸入帳號");
		if (psw == null || "".equals(psw))
			addFieldError("psw", "密碼為必填，請輸入密碼");
	}

	public String execute() throws Exception {

		tag=logincControl.loginC(session,acc,psw);
		if(!"success".equals(tag)) result= "input";
		else{ result= "success";
			actionLogControl.loggerAction(super.getUser().getAccount(), "Login", "Login","", result);
		}
		return result;
		
	}


	public String getAcc() {
		return acc;
	}

	public void setAcc(String acc) {
		this.acc = acc;
	}

	public String getPsw() {
		return psw;
	}

	public void setPsw(String psw) {
		this.psw = psw;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}	
}

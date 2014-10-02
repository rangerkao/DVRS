package action;

import bean.Invoice;

import com.opensymphony.xwork2.ActionSupport;

import control.BillReport;

public class LoginAction extends ActionSupport{
		
	   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String userid;
	private String password;
	private String tag;
	
	

	public void validate() {
		if (userid == null || "".equals(userid))
			addFieldError("userid", "�b��������A�п�J�b��");
		if (password == null || "".equals(password))
			addFieldError("password", "�K�X������A�п�J�K�X");
	}

	public String execute() throws Exception {
		
		return "success";
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

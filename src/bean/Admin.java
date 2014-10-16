package bean;

public class Admin {

	String userid;
	String account;
	String password;
	String role;
	
	public Admin(){}
	
	

	
	public Admin(String userid, String account, String password, String role) {
		super();
		this.userid = userid;
		this.account = account;
		this.password = password;
		this.role = role;
	}




	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
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
	
}

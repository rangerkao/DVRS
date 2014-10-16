package bean;

public class User {

	String account;
	String role;
	
	public User(){}
	
	

	
	public User(String account, String role) {
		super();
		this.account = account;
		this.role = role;
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
	
}

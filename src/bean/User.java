package bean;

public class User {

	String userid;
	String rold;
	
	public User(){}
	
	public User(String userid, String rold) {
		super();
		this.userid = userid;
		this.rold = rold;
	}
	
	
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public String getRold() {
		return rold;
	}
	public void setRold(String rold) {
		this.rold = rold;
	}
}

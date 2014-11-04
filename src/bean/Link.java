package bean;

public class Link {

	String belong;
	String action;
	String name;
	
	public Link(){
		
	}
	
	public Link(String belong, String action, String name) {
		super();
		this.belong = belong;
		this.action = action;
		this.name = name;
	}

	public String getBelong() {
		return belong;
	}

	public void setBelong(String belong) {
		this.belong = belong;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
	
}

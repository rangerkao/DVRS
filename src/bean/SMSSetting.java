package bean;

public class SMSSetting {

	String id;
	Double bracket;
	String msg;
	Boolean suspend;
	String pricePlanId;
	
	public SMSSetting(){
		
	}
	
	

	public SMSSetting(String id, Double bracket, String msg, Boolean suspend,
			String pricePlanId) {
		super();
		this.id = id;
		this.bracket = bracket;
		this.msg = msg;
		this.suspend = suspend;
		this.pricePlanId = pricePlanId;
	}



	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Double getBracket() {
		return bracket;
	}

	public void setBracket(Double bracket) {
		this.bracket = bracket;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Boolean getSuspend() {
		return suspend;
	}

	public void setSuspend(Boolean suspend) {
		this.suspend = suspend;
	}



	public String getPricePlanId() {
		return pricePlanId;
	}



	public void setPricePlanId(String pricePlanId) {
		this.pricePlanId = pricePlanId;
	}
	
	

	
}

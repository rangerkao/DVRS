package bean;

public class SMSContent {

	Integer id;
	String comtent;
	String charSet;
	String description;
	
	
	public SMSContent(){
		
	}

	
	

	public SMSContent(Integer id, String comtent, String charSet,
			String description) {
		super();
		this.id = id;
		this.comtent = comtent;
		this.charSet = charSet;
		this.description = description;
	}




	public Integer getId() {
		return id;
	}


	public void setId(Integer id) {
		this.id = id;
	}


	public String getComtent() {
		return comtent;
	}


	public void setComtent(String comtent) {
		this.comtent = comtent;
	}


	public String getCharSet() {
		return charSet;
	}


	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
	
	
	
}

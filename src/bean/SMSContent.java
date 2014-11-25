package bean;

public class SMSContent {

	Integer ID;
	String COMTENT;
	String CHARSET;
	String DESCRIPTION;
	
	
	public SMSContent(){
		
	}
	
	
	public SMSContent(Integer iD, String cOMTENT, String cHARSET,
			String dESCRIPTION) {
		super();
		ID = iD;
		COMTENT = cOMTENT;
		CHARSET = cHARSET;
		DESCRIPTION = dESCRIPTION;
	}
	public Integer getID() {
		return ID;
	}
	public void setID(Integer iD) {
		ID = iD;
	}
	public String getCOMTENT() {
		return COMTENT;
	}
	public void setCOMTENT(String cOMTENT) {
		COMTENT = cOMTENT;
	}
	public String getCHARSET() {
		return CHARSET;
	}
	public void setCHARSET(String cHARSET) {
		CHARSET = cHARSET;
	}
	public String getDESCRIPTION() {
		return DESCRIPTION;
	}
	public void setDESCRIPTION(String dESCRIPTION) {
		DESCRIPTION = dESCRIPTION;
	}
	
	
	
	
}

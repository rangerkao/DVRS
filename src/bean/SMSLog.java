package bean;

import java.util.Date;

public class SMSLog {

	String id;
	String sendNumber;
	String msg;
	Date sendDate;
	String result;
	Date createDate;
	
	public SMSLog(){
		
	}
	
	public SMSLog(String id, String sendNumber, String msg, Date sendDate,
			String result, Date createDate) {
		super();
		this.id = id;
		this.sendNumber = sendNumber;
		this.msg = msg;
		this.sendDate = sendDate;
		this.result = result;
		this.createDate = createDate;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSendNumber() {
		return sendNumber;
	}
	public void setSendNumber(String sendNumber) {
		this.sendNumber = sendNumber;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public Date getSendDate() {
		return sendDate;
	}
	public void setSendDate(Date sendDate) {
		this.sendDate = sendDate;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	
	
}

package bean;

import java.util.Date;

public class ActionLog {

	String id;
	String account;
	String page;
	String action;
	Date createDate;
	String parameter;
	String result;
	
	public ActionLog(){
		
	}
	
	public ActionLog(String id, String account, String page, String action,
			Date createDate, String parameter, String result) {
		super();
		this.id = id;
		this.account = account;
		this.page = page;
		this.action = action;
		this.createDate = createDate;
		this.parameter = parameter;
		this.result = result;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
	
	
}

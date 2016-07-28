package bean;

import org.json.JSONObject;

public class VolumePocket {

	String pid;
	
	String serviceid;
	String mcc;
	String startDate;
	String endDate;
	String currency;
	String alerted;
	String createTime;
	String cancelTime;
	String chtMsisdn;
	
	String id;
	String callerName;
	String customerName;
	String phoneType;
	String email;
	
	
	public VolumePocket(){
		
	}
	
	
	public VolumePocket(JSONObject j){
		for(String name:JSONObject.getNames(j)){
			if("pid".equalsIgnoreCase(name)){
				this.pid = j.getString(name);
			}else if("serviceid".equalsIgnoreCase(name)){
				this.serviceid = j.getString(name);
			}else if("mcc".equalsIgnoreCase(name)){
				this.mcc = j.getString(name);
			}else if("startDate".equalsIgnoreCase(name)){
				this.startDate = j.getString(name);
			}else if("endDate".equalsIgnoreCase(name)){
				this.endDate = j.getString(name);
			}else if("currency".equalsIgnoreCase(name)){
				this.currency = j.getString(name);
			}else if("id".equalsIgnoreCase(name)){
				this.id = j.getString(name);
			}else if("callerName".equalsIgnoreCase(name)){
				this.callerName = j.getString(name);
			}else if("customerName".equalsIgnoreCase(name)){
				this.customerName = j.getString(name);
			}else if("phoneType".equalsIgnoreCase(name)){
				this.phoneType = j.getString(name);
			}else if("email".equalsIgnoreCase(name)){
				this.email = j.getString(name);
			}else if("chtMsisdn".equalsIgnoreCase(name)){
				this.chtMsisdn = j.getString(name);
			}
		};
	}
	
	
	
	public String getServiceid() {
		return serviceid;
	}
	public void setServiceid(String serviceid) {
		this.serviceid = serviceid;
	}
	public String getMcc() {
		return mcc;
	}
	public void setMcc(String mcc) {
		this.mcc = mcc;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getAlerted() {
		return alerted;
	}
	public void setAlerted(String alerted) {
		this.alerted = alerted;
	}
	public String getCreateTime() {
		return createTime;
	}
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
	public String getCancelTime() {
		return cancelTime;
	}
	public void setCancelTime(String cancelTime) {
		this.cancelTime = cancelTime;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	
	
	public String getCallerName() {
		return callerName;
	}


	public void setCallerName(String callerName) {
		this.callerName = callerName;
	}


	public String getCustomerName() {
		return customerName;
	}


	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}


	public String getPhoneType() {
		return phoneType;
	}

	public void setPhoneType(String phoneType) {
		this.phoneType = phoneType;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}


	public String getChtMsisdn() {
		return chtMsisdn;
	}


	public void setChtMsisdn(String chtMsisdn) {
		this.chtMsisdn = chtMsisdn;
	}


	public String getPid() {
		return pid;
	}


	public void setPid(String pid) {
		this.pid = pid;
	}

	
	
}

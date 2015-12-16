package bean;

import java.util.Date;

public class GPRSThreshold {

	String imsi;
	Double threshold;
	String msisdn;
	String createDate;
	String status;
	String cancelDate;
	
	public GPRSThreshold(){
		
	}

	public GPRSThreshold(String imsi, Double threshold, String msisdn,
			String createDate, String status, String cancelDate) {
		super();
		this.imsi = imsi;
		this.threshold = threshold;
		this.msisdn = msisdn;
		this.createDate = createDate;
		this.status = status;
		this.cancelDate = cancelDate;
	}

	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}

	public Double getThreshold() {
		return threshold;
	}

	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getCreateDate() {
		return createDate;
	}

	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCancelDate() {
		return cancelDate;
	}

	public void setCancelDate(String cancelDate) {
		this.cancelDate = cancelDate;
	}
	
	





	
}

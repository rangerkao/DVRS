package bean;

public class GPRSThreshold {

	String imsi;
	Double threshold;
	String msisdn;
	
	
	public GPRSThreshold(){
		
	}
	
	

	public GPRSThreshold(String imsi, Double threshold, String msisdn) {
		super();
		this.imsi = imsi;
		this.threshold = threshold;
		this.msisdn = msisdn;
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
	
	
	

	
}

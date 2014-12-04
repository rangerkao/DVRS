package bean;

public class DataRate {
	Long pricePlanId;
	String pricePlanName;
	String mccmnc;
	String country;
	String netWork;
	Double rate;
	Long chargeunit;
	String currency;
	Double dayCap;
	
	public DataRate(){};
	
	

	public DataRate(Long pricePlanId, String pricePlanName, String mccmnc,
			String country, String netWork, Double rate, Long chargeunit,
			String currency, Double dayCap) {
		super();
		this.pricePlanId = pricePlanId;
		this.pricePlanName = pricePlanName;
		this.mccmnc = mccmnc;
		this.country = country;
		this.netWork = netWork;
		this.rate = rate;
		this.chargeunit = chargeunit;
		this.currency = currency;
		this.dayCap = dayCap;
	}



	public Long getPricePlanId() {
		return pricePlanId;
	}

	public void setPricePlanId(Long pricePlanId) {
		this.pricePlanId = pricePlanId;
	}

	public String getMccmnc() {
		return mccmnc;
	}

	public void setMccmnc(String mccmnc) {
		this.mccmnc = mccmnc;
	}

	public Double getRate() {
		return rate;
	}

	public void setRate(Double rate) {
		this.rate = rate;
	}

	public Long getChargeunit() {
		return chargeunit;
	}

	public void setChargeunit(Long chargeunit) {
		this.chargeunit = chargeunit;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}



	public String getPricePlanName() {
		return pricePlanName;
	}



	public void setPricePlanName(String pricePlanName) {
		this.pricePlanName = pricePlanName;
	}



	public String getCountry() {
		return country;
	}



	public void setCountry(String country) {
		this.country = country;
	}



	public String getNetWork() {
		return netWork;
	}



	public void setNetWork(String netWork) {
		this.netWork = netWork;
	}



	public Double getDayCap() {
		return dayCap;
	}



	public void setDayCap(Double dayCap) {
		this.dayCap = dayCap;
	}
	
	

}

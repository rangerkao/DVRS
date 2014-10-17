package bean;

public class DataRate {
	Long pricePlanId;
	String mccmnc;
	Double rate;
	Long chargeunit;
	String currency;
	
	public DataRate(){};
	
	public DataRate(Long pricePlanId, String mccmnc, Double rate,
			Long chargeunit, String currency) {
		super();
		this.pricePlanId = pricePlanId;
		this.mccmnc = mccmnc;
		this.rate = rate;
		this.chargeunit = chargeunit;
		this.currency = currency;
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
	
	

}

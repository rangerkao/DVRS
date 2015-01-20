package bean;

public class CurrentDay {
	String imsi;
	Double charge;
	Integer lastFileId;
	String lastDataTime;
	Double volume;
	String updateDate;
	String createDate;
	String mccmnc;
	String day; 
	boolean alert;
	
	public CurrentDay(){
		
	}

	public CurrentDay(String imsi, Double charge, Integer lastFileId,
			String lastDataTime, Double volume, String updateDate,
			String createDate, String mccmnc, String day, boolean alert) {
		super();
		this.imsi = imsi;
		this.charge = charge;
		this.lastFileId = lastFileId;
		this.lastDataTime = lastDataTime;
		this.volume = volume;
		this.updateDate = updateDate;
		this.createDate = createDate;
		this.mccmnc = mccmnc;
		this.day = day;
		this.alert = alert;
	}

	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}

	public Double getCharge() {
		return charge;
	}

	public void setCharge(Double charge) {
		this.charge = charge;
	}

	public Integer getLastFileId() {
		return lastFileId;
	}

	public void setLastFileId(Integer lastFileId) {
		this.lastFileId = lastFileId;
	}

	public String getLastDataTime() {
		return lastDataTime;
	}

	public void setLastDataTime(String lastDataTime) {
		this.lastDataTime = lastDataTime;
	}

	public Double getVolume() {
		return volume;
	}

	public void setVolume(Double volume) {
		this.volume = volume;
	}

	public String getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(String updateDate) {
		this.updateDate = updateDate;
	}

	public String getCreateDate() {
		return createDate;
	}

	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}

	public String getMccmnc() {
		return mccmnc;
	}

	public void setMccmnc(String mccmnc) {
		this.mccmnc = mccmnc;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public boolean isAlert() {
		return alert;
	}

	public void setAlert(boolean alert) {
		this.alert = alert;
	}
	
	
	
}

package bean;

public class CurrentMonth {

	String imsi;
	Double charge;
	Integer lastFileId;
	Integer smsTimes;
	String lastDataTime;
	Double volume;
	String month;
	String updateDate;
	String createDate;
	boolean everSuspend;
	Double lastAlertThreshold;
	Double lastAlertVolume;
	
	String ICCID;
	
	public CurrentMonth(){
		
	}

	public CurrentMonth(String imsi, Double charge, Integer lastFileId,
			Integer smsTimes, String lastDataTime, Double volume, String month,
			String updateDate, String createDate, boolean everSuspend,
			Double lastAlertThreshold, Double lastAlertVolume) {
		super();
		this.imsi = imsi;
		this.charge = charge;
		this.lastFileId = lastFileId;
		this.smsTimes = smsTimes;
		this.lastDataTime = lastDataTime;
		this.volume = volume;
		this.month = month;
		this.updateDate = updateDate;
		this.createDate = createDate;
		this.everSuspend = everSuspend;
		this.lastAlertThreshold = lastAlertThreshold;
		this.lastAlertVolume = lastAlertVolume;
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

	public Integer getSmsTimes() {
		return smsTimes;
	}

	public void setSmsTimes(Integer smsTimes) {
		this.smsTimes = smsTimes;
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

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
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

	public boolean isEverSuspend() {
		return everSuspend;
	}

	public void setEverSuspend(boolean everSuspend) {
		this.everSuspend = everSuspend;
	}

	public Double getLastAlertThreshold() {
		return lastAlertThreshold;
	}

	public void setLastAlertThreshold(Double lastAlertThreshold) {
		this.lastAlertThreshold = lastAlertThreshold;
	}

	public Double getLastAlertVolume() {
		return lastAlertVolume;
	}

	public void setLastAlertVolume(Double lastAlertVolume) {
		this.lastAlertVolume = lastAlertVolume;
	}

	public String getICCID() {
		return ICCID;
	}

	public void setICCID(String iCCID) {
		ICCID = iCCID;
	}
	
	
	
}

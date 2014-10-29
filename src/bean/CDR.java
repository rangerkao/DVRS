package bean;

import java.util.Date;

public class CDR {
	String usageId;
	String imsi;
	Date calltime;
	String mccmnc;
	String sgsnAddress;
	Double dataVolume;
	String fileId;
	
	public CDR(){}
	
	public CDR(String usageId, String imsi, Date calltime, String mccmnc,
			String sgsnAddress, Double dataVolume, String fileId) {
		super();
		this.usageId = usageId;
		this.imsi = imsi;
		this.calltime = calltime;
		this.mccmnc = mccmnc;
		this.sgsnAddress = sgsnAddress;
		this.dataVolume = dataVolume;
		this.fileId = fileId;
	}

	public String getUsageId() {
		return usageId;
	}

	public void setUsageId(String usageId) {
		this.usageId = usageId;
	}

	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}

	public Date getCalltime() {
		return calltime;
	}

	public void setCalltime(Date calltime) {
		this.calltime = calltime;
	}

	public String getMccmnc() {
		return mccmnc;
	}

	public void setMccmnc(String mccmnc) {
		this.mccmnc = mccmnc;
	}

	public String getSgsnAddress() {
		return sgsnAddress;
	}

	public void setSgsnAddress(String sgsnAddress) {
		this.sgsnAddress = sgsnAddress;
	}

	public Double getDataVolume() {
		return dataVolume;
	}

	public void setDataVolume(Double dataVolume) {
		this.dataVolume = dataVolume;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
	
	

	
}

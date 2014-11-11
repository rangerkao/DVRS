package bill.bean;

import java.util.List;

public class UsageDetail {
	private String recordFlag;//������O(�ѧO��Ʈ榡)
	private String accountNum;//�b��s��(�b�����)
	private String serviceCode;//�q�ܸ��X
	private String typeSequence;//�Գ�������ܶ���
	private String usageType;//�Գ�����
	private String chargeItemName;//�O�ζ��W��
	private String subTotalDuration;//�Գ������ɶ��p�p
	private String subTotalCharges;//�Գ��������B�p�p
	private String subTotalMessages;//�Գ��������Ƥp�p
	private String caller;//�D�s���X
	private String callee;//�Q�s���X
	private String callerDestination;//�D�s�Ҧb�a
	private String calleeDestination;//�Q�s�Ҧb�a
	private String startDate;//�q�ܰ_�l���
	private String startTime;//�q�ܰ_�l�ɶ�
	private String callType;//�I�s��V
	private String duration;//�q�ܮɪ�
	private String charge;//�O��
	private String eventCount;//����
	private String chargeItemAbbName;//�O�ζ�²�X
	//�ݽT�{�A���S���w�q�����
	private String endDate;//�������
	private String endTime;//�����ɶ�
	private String packages;//�ϥΫʥ]�q
	private String subTotalPackages;//�Գ��������Ƥp�p
	
	private String Data; //Bean ���e���

	public UsageDetail(){
		
	}
	
	public UsageDetail(List<String> list){
		int n=0;
		if(list!=null) n=list.size();
		if(n>=24)n=24;
		
		switch(n){
		case 24:subTotalPackages=list.get(23);
		case 23:packages=list.get(22);
		case 22:endTime=list.get(21);
		case 21:endDate=list.get(20);
		case 20:chargeItemAbbName=list.get(19);
		case 19:eventCount=list.get(18);
		case 18:charge=list.get(17);
		case 17:duration=list.get(16);
		case 16:callType=list.get(15);
		case 15:startTime=list.get(14);
		case 14:startDate=list.get(13);
		case 13:calleeDestination=list.get(12);
		case 12:callerDestination=list.get(11);
		case 11:callee=list.get(10);
		case 10:caller=list.get(9);
		case 9:subTotalMessages=list.get(8);
		case 8:subTotalCharges=list.get(7);
		case 7:subTotalDuration=list.get(6);
		case 6:chargeItemName=list.get(5);
		case 5:usageType=list.get(4);
		case 4:typeSequence=list.get(3);
		case 3:serviceCode=list.get(2);
		case 2:accountNum=list.get(1);
		case 1:recordFlag=list.get(0);
		default:
		}
		
		setData();
	}
	
	

	public UsageDetail(String recordFlag, String accountNum,
			String serviceCode, String typeSequence, String usageType,
			String chargeItemName, String subTotalDuration,
			String subTotalCharges, String subTotalMessages, String caller,
			String callee, String callerDestination, String calleeDestination,
			String startDate, String startTime, String callType,
			String duration, String charge, String eventCount,
			String chargeItemAbbName, String endDate, String endTime,
			String packages, String subTotalPackages) {
		super();
		this.recordFlag = recordFlag;
		this.accountNum = accountNum;
		this.serviceCode = serviceCode;
		this.typeSequence = typeSequence;
		this.usageType = usageType;
		this.chargeItemName = chargeItemName;
		this.subTotalDuration = subTotalDuration;
		this.subTotalCharges = subTotalCharges;
		this.subTotalMessages = subTotalMessages;
		this.caller = caller;
		this.callee = callee;
		this.callerDestination = callerDestination;
		this.calleeDestination = calleeDestination;
		this.startDate = startDate;
		this.startTime = startTime;
		this.callType = callType;
		this.duration = duration;
		this.charge = charge;
		this.eventCount = eventCount;
		this.chargeItemAbbName = chargeItemAbbName;
		this.endDate = endDate;
		this.endTime = endTime;
		this.packages = packages;
		this.subTotalPackages = subTotalPackages;
	}

	public String getRecordFlag() {
		return recordFlag;
	}

	public void setRecordFlag(String recordFlag) {
		this.recordFlag = recordFlag;
	}

	public String getAccountNum() {
		return accountNum;
	}

	public void setAccountNum(String accountNum) {
		this.accountNum = accountNum;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	public String getTypeSequence() {
		return typeSequence;
	}

	public void setTypeSequence(String typeSequence) {
		this.typeSequence = typeSequence;
	}

	public String getUsageType() {
		return usageType;
	}

	public void setUsageType(String usageType) {
		this.usageType = usageType;
	}

	public String getChargeItemName() {
		return chargeItemName;
	}

	public void setChargeItemName(String chargeItemName) {
		this.chargeItemName = chargeItemName;
	}

	public String getSubTotalDuration() {
		return subTotalDuration;
	}

	public void setSubTotalDuration(String subTotalDuration) {
		this.subTotalDuration = subTotalDuration;
	}

	public String getSubTotalCharges() {
		return subTotalCharges;
	}

	public void setSubTotalCharges(String subTotalCharges) {
		this.subTotalCharges = subTotalCharges;
	}

	public String getSubTotalMessages() {
		return subTotalMessages;
	}

	public void setSubTotalMessages(String subTotalMessages) {
		this.subTotalMessages = subTotalMessages;
	}

	public String getCaller() {
		return caller;
	}

	public void setCaller(String caller) {
		this.caller = caller;
	}

	public String getCallee() {
		return callee;
	}

	public void setCallee(String callee) {
		this.callee = callee;
	}

	public String getCallerDestination() {
		return callerDestination;
	}

	public void setCallerDestination(String callerDestination) {
		this.callerDestination = callerDestination;
	}

	public String getCalleeDestination() {
		return calleeDestination;
	}

	public void setCalleeDestination(String calleeDestination) {
		this.calleeDestination = calleeDestination;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getCallType() {
		return callType;
	}

	public void setCallType(String callType) {
		this.callType = callType;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getCharge() {
		return charge;
	}

	public void setCharge(String charge) {
		this.charge = charge;
	}

	public String getEventCount() {
		return eventCount;
	}

	public void setEventCount(String eventCount) {
		this.eventCount = eventCount;
	}

	public String getChargeItemAbbName() {
		return chargeItemAbbName;
	}

	public void setChargeItemAbbName(String chargeItemAbbName) {
		this.chargeItemAbbName = chargeItemAbbName;
	}

	public String getData() {
		return Data;
	}

	public void setData() {
		
		String lineEnd="<br>\n";
		Data = 
				"Record Flag : " + recordFlag + lineEnd +
				"AccountNum : " + accountNum + lineEnd +
				"ServiceCode : " + serviceCode + lineEnd +
				"TypeSequence : " + typeSequence + lineEnd +
				"UsageType : " + usageType + lineEnd +
				"ChargeItemName : " + chargeItemName + lineEnd +
				"SubTotalDuration : " + subTotalDuration + lineEnd +
				"SubTotalCharges : " + subTotalCharges + lineEnd +
				"SubTotalMessages : " + subTotalMessages + lineEnd +
				"Caller : " + caller + lineEnd +
				"Callee : " + callee + lineEnd +
				"CallerDestination : " + callerDestination + lineEnd +
				"CalleeDestination : " + calleeDestination + lineEnd +
				"StartDate : " + startDate + lineEnd +
				"StartTime : " + startTime + lineEnd +
				"CallType : " + callType + lineEnd +
				"Duration : " + duration + lineEnd +
				"Charge : " + charge + lineEnd +
				"EventCount : " + eventCount + lineEnd +
				"ChargeItemAbbName : " + chargeItemAbbName + lineEnd +
				"endDate : " + endDate + lineEnd +
				"endTime : " + endTime + lineEnd +
				"Packages : " + packages + lineEnd +
				"subTotalPackages : " + subTotalPackages + lineEnd ;

	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getPackages() {
		return packages;
	}

	public void setPackages(String packages) {
		this.packages = packages;
	}

	public String getSubTotalPackages() {
		return subTotalPackages;
	}

	public void setSubTotalPackages(String subTotalPackages) {
		this.subTotalPackages = subTotalPackages;
	}
	
	

}

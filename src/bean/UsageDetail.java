package bean;

import java.util.List;

public class UsageDetail {
	private String recordFlag;//資料類別(識別資料格式)
	private String accountNum;//帳戶編號(帳單鍵值)
	private String serviceCode;//電話號碼
	private String typeSequence;//詳單類型顯示順序
	private String usageType;//詳單類型
	private String chargeItemName;//費用項名稱
	private String subTotalDuration;//詳單類型時間小計
	private String subTotalCharges;//詳單類型金額小計
	private String subTotalMessages;//詳單類型次數小計
	private String caller;//主叫號碼
	private String callee;//被叫號碼
	private String callerDestination;//主叫所在地
	private String calleeDestination;//被叫所在地
	private String startDate;//通話起始日期
	private String startTime;//通話起始時間
	private String callType;//呼叫方向
	private String duration;//通話時長
	private String charge;//費用
	private String eventCount;//次數
	private String chargeItemAbbName;//費用項簡碼
	
	private String Data; //Bean 內容資料

	public UsageDetail(){
		
	}
	
	public UsageDetail(List<String> list){
		int n=0;
		if(list!=null) n=list.size();
		if(n>=20)n=20;
		
		switch(n){
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
			String chargeItemAbbName) {
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
				"ChargeItemAbbName : " + chargeItemAbbName + lineEnd ;

	}
	
	

}

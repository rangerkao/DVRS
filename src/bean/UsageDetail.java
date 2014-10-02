package bean;

public class UsageDetail {
	private String RecordFlag;//資料類別(識別資料格式)
	private String AccountNum;//帳戶編號(帳單鍵值)
	private String ServiceCode;//電話號碼
	private String TypeSequence;//詳單類型顯示順序
	private String UsageType;//詳單類型
	private String ChargeItemName;//費用項名稱
	private String SubTotalDuration;//詳單類型時間小計
	private String SubTotalCharges;//詳單類型金額小計
	private String SubTotalMessages;//詳單類型次數小計
	private String Caller;//主叫號碼
	private String Callee;//被叫號碼
	private String CallerDestination;//主叫所在地
	private String CalleeDestination;//被叫所在地
	private String StartDate;//通話起始日期
	private String StartTime;//通話起始時間
	private String CallType;//呼叫方向
	private String Duration;//通話時長
	private String Charge;//費用
	private String EventCount;//次數
	private String ChargeItemAbbName;//費用項簡碼
	
	private String Data; //Bean 內容資料

	public UsageDetail(){
		
	}
	
	public UsageDetail(String[] data){
		int n=0;
		if(data!=null) n=data.length;
		if(n>=20)n=20;
		
		switch(n){
		case 20:ChargeItemAbbName=data[19];
		case 19:EventCount=data[18];
		case 18:Charge=data[17];
		case 17:Duration=data[16];
		case 16:CallType=data[15];
		case 15:StartTime=data[14];
		case 14:StartDate=data[13];
		case 13:CalleeDestination=data[12];
		case 12:CallerDestination=data[11];
		case 11:Callee=data[10];
		case 10:Caller=data[9];
		case 9:SubTotalMessages=data[8];
		case 8:SubTotalCharges=data[7];
		case 7:SubTotalDuration=data[6];
		case 6:ChargeItemName=data[5];
		case 5:UsageType=data[4];
		case 4:TypeSequence=data[3];
		case 3:ServiceCode=data[2];
		case 2:AccountNum=data[1];
		case 1:RecordFlag=data[0];
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
		RecordFlag = recordFlag;
		AccountNum = accountNum;
		ServiceCode = serviceCode;
		TypeSequence = typeSequence;
		UsageType = usageType;
		ChargeItemName = chargeItemName;
		SubTotalDuration = subTotalDuration;
		SubTotalCharges = subTotalCharges;
		SubTotalMessages = subTotalMessages;
		Caller = caller;
		Callee = callee;
		CallerDestination = callerDestination;
		CalleeDestination = calleeDestination;
		StartDate = startDate;
		StartTime = startTime;
		CallType = callType;
		Duration = duration;
		Charge = charge;
		EventCount = eventCount;
		ChargeItemAbbName = chargeItemAbbName;
	}

	public String getRecordFlag() {
		return RecordFlag;
	}

	public void setRecordFlag(String recordFlag) {
		RecordFlag = recordFlag;
	}

	public String getAccountNum() {
		return AccountNum;
	}

	public void setAccountNum(String accountNum) {
		AccountNum = accountNum;
	}

	public String getServiceCode() {
		return ServiceCode;
	}

	public void setServiceCode(String serviceCode) {
		ServiceCode = serviceCode;
	}

	public String getTypeSequence() {
		return TypeSequence;
	}

	public void setTypeSequence(String typeSequence) {
		TypeSequence = typeSequence;
	}

	public String getUsageType() {
		return UsageType;
	}

	public void setUsageType(String usageType) {
		UsageType = usageType;
	}

	public String getChargeItemName() {
		return ChargeItemName;
	}

	public void setChargeItemName(String chargeItemName) {
		ChargeItemName = chargeItemName;
	}

	public String getSubTotalDuration() {
		return SubTotalDuration;
	}

	public void setSubTotalDuration(String subTotalDuration) {
		SubTotalDuration = subTotalDuration;
	}

	public String getSubTotalCharges() {
		return SubTotalCharges;
	}

	public void setSubTotalCharges(String subTotalCharges) {
		SubTotalCharges = subTotalCharges;
	}

	public String getSubTotalMessages() {
		return SubTotalMessages;
	}

	public void setSubTotalMessages(String subTotalMessages) {
		SubTotalMessages = subTotalMessages;
	}

	public String getCaller() {
		return Caller;
	}

	public void setCaller(String caller) {
		Caller = caller;
	}

	public String getCallee() {
		return Callee;
	}

	public void setCallee(String callee) {
		Callee = callee;
	}

	public String getCallerDestination() {
		return CallerDestination;
	}

	public void setCallerDestination(String callerDestination) {
		CallerDestination = callerDestination;
	}

	public String getCalleeDestination() {
		return CalleeDestination;
	}

	public void setCalleeDestination(String calleeDestination) {
		CalleeDestination = calleeDestination;
	}

	public String getStartDate() {
		return StartDate;
	}

	public void setStartDate(String startDate) {
		StartDate = startDate;
	}

	public String getStartTime() {
		return StartTime;
	}

	public void setStartTime(String startTime) {
		StartTime = startTime;
	}

	public String getCallType() {
		return CallType;
	}

	public void setCallType(String callType) {
		CallType = callType;
	}

	public String getDuration() {
		return Duration;
	}

	public void setDuration(String duration) {
		Duration = duration;
	}

	public String getCharge() {
		return Charge;
	}

	public void setCharge(String charge) {
		Charge = charge;
	}

	public String getEventCount() {
		return EventCount;
	}

	public void setEventCount(String eventCount) {
		EventCount = eventCount;
	}

	public String getChargeItemAbbName() {
		return ChargeItemAbbName;
	}

	public void setChargeItemAbbName(String chargeItemAbbName) {
		ChargeItemAbbName = chargeItemAbbName;
	}

	public String getData() {
		return Data;
	}

	public void setData() {
		
		String lineEnd="<br>\n";
		Data = 
				"Record Flag : " + RecordFlag + lineEnd +
				"AccountNum : " + AccountNum + lineEnd +
				"ServiceCode : " + ServiceCode + lineEnd +
				"TypeSequence : " + TypeSequence + lineEnd +
				"UsageType : " + UsageType + lineEnd +
				"ChargeItemName : " + ChargeItemName + lineEnd +
				"SubTotalDuration : " + SubTotalDuration + lineEnd +
				"SubTotalCharges : " + SubTotalCharges + lineEnd +
				"SubTotalMessages : " + SubTotalMessages + lineEnd +
				"Caller : " + Caller + lineEnd +
				"Callee : " + Callee + lineEnd +
				"CallerDestination : " + CallerDestination + lineEnd +
				"CalleeDestination : " + CalleeDestination + lineEnd +
				"StartDate : " + StartDate + lineEnd +
				"StartTime : " + StartTime + lineEnd +
				"CallType : " + CallType + lineEnd +
				"Duration : " + Duration + lineEnd +
				"Charge : " + Charge + lineEnd +
				"EventCount : " + EventCount + lineEnd +
				"ChargeItemAbbName : " + ChargeItemAbbName + lineEnd ;

	}
	
	

}

package bean;

public class Usage {
	private String RecordFlag;//資料類別(識別資料格式)
	private String AccountNum;//帳戶編號(帳單鍵值)
	private String ServiceCode;//電話號碼
	private String CustomerName;//客戶名稱(基於電話號碼)
	private String AccountName;//帳號名稱
	private String CycleBeginDate;//帳期開始時間
	private String CycleEndDate;//帳期結束時間
	private String TotalCharge;//總費用
	private String TotalRecordCount;//總話單資料筆數
	
	private String Data; //Bean 內容資料

	public Usage(){
		
	}
	
	public Usage(String[] data){
		int n=0;
		if(data!=null) n=data.length;
		if(n>=9)n=9;
		
		switch(n){
		case 9:TotalRecordCount=data[8];
		case 8:TotalCharge=data[7];
		case 7:CycleEndDate=data[6];
		case 6:CycleBeginDate=data[5];
		case 5:AccountName=data[4];
		case 4:CustomerName=data[3];
		case 3:ServiceCode=data[2];
		case 2:AccountNum=data[1];
		case 1:RecordFlag=data[0];
		default:
		}
		
		setData();
	}
	
	public Usage(String recordFlag, String accountNum, String serviceCode,
			String customerName, String accountName, String cycleBeginDate,
			String cycleEndDate, String totalCharge, String totalRecordCount) {
		super();
		RecordFlag = recordFlag;
		AccountNum = accountNum;
		ServiceCode = serviceCode;
		CustomerName = customerName;
		AccountName = accountName;
		CycleBeginDate = cycleBeginDate;
		CycleEndDate = cycleEndDate;
		TotalCharge = totalCharge;
		TotalRecordCount = totalRecordCount;
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

	public String getCustomerName() {
		return CustomerName;
	}

	public void setCustomerName(String customerName) {
		CustomerName = customerName;
	}

	public String getAccountName() {
		return AccountName;
	}

	public void setAccountName(String accountName) {
		AccountName = accountName;
	}

	public String getCycleBeginDate() {
		return CycleBeginDate;
	}

	public void setCycleBeginDate(String cycleBeginDate) {
		CycleBeginDate = cycleBeginDate;
	}

	public String getCycleEndDate() {
		return CycleEndDate;
	}

	public void setCycleEndDate(String cycleEndDate) {
		CycleEndDate = cycleEndDate;
	}

	public String getTotalCharge() {
		return TotalCharge;
	}

	public void setTotalCharge(String totalCharge) {
		TotalCharge = totalCharge;
	}

	public String getTotalRecordCount() {
		return TotalRecordCount;
	}

	public void setTotalRecordCount(String totalRecordCount) {
		TotalRecordCount = totalRecordCount;
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
				"CustomerName : " + CustomerName + lineEnd +
				"AccountName : " + AccountName + lineEnd +
				"CycleBeginDate : " + CycleBeginDate + lineEnd +
				"CycleEndDate : " + CycleEndDate + lineEnd +
				"TotalCharge : " + TotalCharge + lineEnd +
				"TotalRecordCount : " + TotalRecordCount + lineEnd ;
	}

}

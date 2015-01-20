package bill.bean;

import java.util.List;

public class Charge {

	private String RecordFlag;//資料類別(識別資料格式)
	private String AccountNum;//帳戶編號(帳單鍵值)
	private String ServiceCode;//電話號碼
	private String PostalCode;//郵遞區號
	private String Addressee;//收件人
	private String BillingAddress;//帳寄地址
	private String CustomerName;//客戶名稱(基於電話號碼)
	private String AccountName;//帳號名稱
	private String CycleBeginDate;//帳期開始時間
	private String CycleEndDate;//帳期結束時間
	private String Priceplan;//價格計畫名稱
	private String TotalAmount;//帳單總額
	private String TotalChargeCount;//總費用項筆數
	
	private String Data; //Bean 內容資料
	
	public Charge(){
		
	}
	
	public Charge(List<String> list){
		int n=0;
		if(list!=null) n=list.size();
		if(n>=13)n=13;
		
		switch(n){
		case 13:TotalChargeCount=list.get(12);
		case 12:TotalAmount=list.get(11);
		case 11:Priceplan=list.get(10);
		case 10:CycleEndDate=list.get(9);
		case 9:CycleBeginDate=list.get(8);
		case 8:AccountName=list.get(7);
		case 7:CustomerName=list.get(6);
		case 6:BillingAddress=list.get(5);
		case 5:Addressee=list.get(4);
		case 4:PostalCode=list.get(3);
		case 3:ServiceCode=list.get(2);
		case 2:AccountNum=list.get(1);
		case 1:RecordFlag=list.get(0);
		default:
		}
		
		setData();
	}

	public Charge(String recordFlag, String accountNum, String serviceCode,
			String postalCode, String addressee, String billingAddress,
			String customerName, String accountName, String cycleBeginDate,
			String cycleEndDate, String priceplan, String totalAmount,
			String totalChargeCount) {
		super();
		RecordFlag = recordFlag;
		AccountNum = accountNum;
		ServiceCode = serviceCode;
		PostalCode = postalCode;
		Addressee = addressee;
		BillingAddress = billingAddress;
		CustomerName = customerName;
		AccountName = accountName;
		CycleBeginDate = cycleBeginDate;
		CycleEndDate = cycleEndDate;
		Priceplan = priceplan;
		TotalAmount = totalAmount;
		TotalChargeCount = totalChargeCount;
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

	public String getPostalCode() {
		return PostalCode;
	}

	public void setPostalCode(String postalCode) {
		PostalCode = postalCode;
	}

	public String getAddressee() {
		return Addressee;
	}

	public void setAddressee(String addressee) {
		Addressee = addressee;
	}

	public String getBillingAddress() {
		return BillingAddress;
	}

	public void setBillingAddress(String billingAddress) {
		BillingAddress = billingAddress;
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

	public String getPriceplan() {
		return Priceplan;
	}

	public void setPriceplan(String priceplan) {
		Priceplan = priceplan;
	}

	public String getTotalAmount() {
		return TotalAmount;
	}

	public void setTotalAmount(String totalAmount) {
		TotalAmount = totalAmount;
	}

	public String getTotalChargeCount() {
		return TotalChargeCount;
	}

	public void setTotalChargeCount(String totalChargeCount) {
		TotalChargeCount = totalChargeCount;
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
				"PostalCode : " + PostalCode + lineEnd +
				"Addressee : " + Addressee + lineEnd +
				"BillingAddress : " + BillingAddress + lineEnd +
				"CustomerName : " + CustomerName + lineEnd +
				"AccountName : " + AccountName + lineEnd +
				"CycleBeginDate : " + CycleBeginDate + lineEnd +
				"CycleEndDate : " + CycleEndDate + lineEnd +
				"Priceplan : " + Priceplan + lineEnd +
				"TotalAmount : " + TotalAmount + lineEnd +
				"TotalChargeCount : " + TotalChargeCount + lineEnd ;
	}
}

package bill.bean;

public class Invoice {
	
	private String RecordFlag;//資料類別(識別資料格式)
	private String AccountNum;//帳戶編號(帳單鍵值)
	private String ServiceCode;//電話號碼
	private String CountryCode;//國家編碼
	private String PostalCode;//郵遞區號
	private String Addressee;//收件人
	private String BillingAddress;//帳寄地址
	private String CustomerName;//客戶名稱(基於Account)
	private String AccountName;//帳號名稱
	private String CycleBeginDate;//帳期開始時間
	private String CycleEndDate;//帳期結束時間
	private String DueDate;//帳單應付時限
	private String AccountBalance;//前帳期用戶欠費
	private String PaymentPosted;//前帳期用戶交費
	private String InvoiceNo;//帳單號碼
	private String TotalAmount;//帳單總額
	private String TotalAmountDue;//欠款餘額
	private String TotalChargeCount;//總費用項筆數
	private String Balance;//前帳期應繳餘額
	private String ServiceCodeCount;//電話筆數
	private String BillingAddressLine1;//帳寄地址行一
	private String BillingAddressLine2;//帳寄地址行二
	private String PaymentMethod;//付款方式
	
	private String Data;//Bean 內容資料

	public Invoice(){};
	
	public Invoice(String[] data) {
		int n=0;
		if(data!=null) n=data.length;
		if(n>=23)n=23;
		
		switch(n){
		case 23:PaymentMethod = data[22];
		case 22:BillingAddressLine2 = data[21];
		case 21:BillingAddressLine1 = data[20];
		case 20:ServiceCodeCount = data[19];
		case 19:Balance = data[18];
		case 18:TotalChargeCount = data[17];
		case 17:TotalAmountDue = data[16];
		case 16:TotalAmount = data[15];
		case 15:InvoiceNo = data[14];
		case 14:PaymentPosted = data[13];
		case 13:AccountBalance = data[12];
		case 12:DueDate = data[11];
		case 11:CycleEndDate = data[10];
		case 10:CycleBeginDate = data[9];
		case 9:AccountName = data[8];
		case 8:CustomerName = data[7];
		case 7:BillingAddress = data[6];
		case 6:Addressee = data[5];
		case 5:PostalCode = data[4];
		case 4:CountryCode = data[3];
		case 3:ServiceCode = data[2];
		case 2:AccountNum = data[1];
		case 1:RecordFlag = data[0];
		default:
		}
		setData();
	}
	
	
	
	public Invoice(String recordFlag, String accountNum, String serviceCode,
			String countryCode, String postalCode, String addressee,
			String billingAddress, String customerName, String accountName,
			String cycleBeginDate, String cycleEndDate, String dueDate,
			String accountBalance, String paymentPosted, String invoiceNo,
			String totalAmount, String totalAmountDue, String totalChargeCount,
			String balance, String serviceCodeCount,
			String billingAddressLine1, String billingAddressLine2,
			String paymentMethod) {
		super();
		RecordFlag = recordFlag;
		AccountNum = accountNum;
		ServiceCode = serviceCode;
		CountryCode = countryCode;
		PostalCode = postalCode;
		Addressee = addressee;
		BillingAddress = billingAddress;
		CustomerName = customerName;
		AccountName = accountName;
		CycleBeginDate = cycleBeginDate;
		CycleEndDate = cycleEndDate;
		DueDate = dueDate;
		AccountBalance = accountBalance;
		PaymentPosted = paymentPosted;
		InvoiceNo = invoiceNo;
		TotalAmount = totalAmount;
		TotalAmountDue = totalAmountDue;
		TotalChargeCount = totalChargeCount;
		Balance = balance;
		ServiceCodeCount = serviceCodeCount;
		BillingAddressLine1 = billingAddressLine1;
		BillingAddressLine2 = billingAddressLine2;
		PaymentMethod = paymentMethod;
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
	public String getCountryCode() {
		return CountryCode;
	}
	public void setCountryCode(String countryCode) {
		CountryCode = countryCode;
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
	public String getDueDate() {
		return DueDate;
	}
	public void setDueDate(String dueDate) {
		DueDate = dueDate;
	}
	public String getAccountBalance() {
		return AccountBalance;
	}
	public void setAccountBalance(String accountBalance) {
		AccountBalance = accountBalance;
	}
	public String getPaymentPosted() {
		return PaymentPosted;
	}
	public void setPaymentPosted(String paymentPosted) {
		PaymentPosted = paymentPosted;
	}
	public String getInvoiceNo() {
		return InvoiceNo;
	}
	public void setInvoiceNo(String invoiceNo) {
		InvoiceNo = invoiceNo;
	}
	public String getTotalAmount() {
		return TotalAmount;
	}
	public void setTotalAmount(String totalAmount) {
		TotalAmount = totalAmount;
	}
	public String getTotalAmountDue() {
		return TotalAmountDue;
	}
	public void setTotalAmountDue(String totalAmountDue) {
		TotalAmountDue = totalAmountDue;
	}
	public String getTotalChargeCount() {
		return TotalChargeCount;
	}
	public void setTotalChargeCount(String totalChargeCount) {
		TotalChargeCount = totalChargeCount;
	}
	public String getBalance() {
		return Balance;
	}
	public void setBalance(String balance) {
		Balance = balance;
	}
	public String getServiceCodeCount() {
		return ServiceCodeCount;
	}
	public void setServiceCodeCount(String serviceCodeCount) {
		ServiceCodeCount = serviceCodeCount;
	}
	public String getBillingAddressLine1() {
		return BillingAddressLine1;
	}
	public void setBillingAddressLine1(String billingAddressLine1) {
		BillingAddressLine1 = billingAddressLine1;
	}
	public String getBillingAddressLine2() {
		return BillingAddressLine2;
	}
	public void setBillingAddressLine2(String billingAddressLine2) {
		BillingAddressLine2 = billingAddressLine2;
	}
	public String getPaymentMethod() {
		return PaymentMethod;
	}
	public void setPaymentMethod(String paymentMethod) {
		PaymentMethod = paymentMethod;
	}
	public String getData() {
		return Data;
	}

	public void setData() {
		String lineEnd="<br>\n";
		
		Data=
				"RecordFlag : "+RecordFlag + lineEnd +
				"AccountNum : "+AccountNum + lineEnd +
				"ServiceCode : "+ServiceCode + lineEnd +
				"CountryCode : "+CountryCode + lineEnd +
				"PostalCode : "+PostalCode + lineEnd +
				"Addressee : "+Addressee + lineEnd +
				"BillingAddress : "+BillingAddress + lineEnd +
				"CustomerName : "+CustomerName + lineEnd +
				"AccountName : "+AccountName + lineEnd +
				"CycleBeginDate : "+CycleBeginDate + lineEnd +
				"CycleEndDate : "+CycleEndDate + lineEnd +
				"DueDate : "+DueDate + lineEnd +
				"AccountBalance : "+AccountBalance + lineEnd +
				"PaymentPosted : "+PaymentPosted + lineEnd +
				"InvoiceNo : "+InvoiceNo + lineEnd +
				"TotalAmount : "+TotalAmount + lineEnd +
				"TotalAmountDue : "+TotalAmountDue + lineEnd +
				"TotalChargeCount : "+TotalChargeCount + lineEnd +
				"Balance : "+Balance + lineEnd +
				"ServiceCodeCount : "+ServiceCodeCount + lineEnd +
				"BillingAddressLine1 : "+BillingAddressLine1 + lineEnd +
				"BillingAddressLine2 : "+BillingAddressLine2 + lineEnd +
				"PaymentMethod : "+PaymentMethod ;
	}

	
	
}

package bean;

public class ChargeDetail {

	private String RecordFlag;//資料類別(識別資料格式)
	private String AccountNum;//帳戶編號(帳單鍵值)
	private String ServiceCode;//電話號碼
	private String CategorySequence;//分類顯示順序
	private String CategoryName;//分類名
	private String CategoryAmountSummary;//分類項費用小計
	private String ChargeItemSequence;//費用項顯示順序
	private String ChargeItemName;//費用項名
	private String Amount;//費用額度
	
	private String Data; //Bean 內容資料

	public ChargeDetail(){
		
	}
	
	public ChargeDetail(String[] data){
		
		int n=0;
		if(data!=null) n=data.length;
		if(n>=9)n=9;
		
		switch(n){
		case 9:Amount=data[8];
		case 8:ChargeItemName=data[7];
		case 7:ChargeItemSequence=data[6];
		case 6:CategoryAmountSummary=data[5];
		case 5:CategoryName=data[4];
		case 4:CategorySequence=data[3];
		case 3:ServiceCode=data[2];
		case 2:AccountNum=data[1];
		case 1:RecordFlag=data[0];
		default:
		}
		
		setData();
	}
	
	public ChargeDetail(String recordFlag, String accountNum,
			String serviceCode, String categorySequence, String categoryName,
			String categoryAmountSummary, String chargeItemSequence,
			String chargeItemName, String amount) {
		super();
		RecordFlag = recordFlag;
		AccountNum = accountNum;
		ServiceCode = serviceCode;
		CategorySequence = categorySequence;
		CategoryName = categoryName;
		CategoryAmountSummary = categoryAmountSummary;
		ChargeItemSequence = chargeItemSequence;
		ChargeItemName = chargeItemName;
		Amount = amount;
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

	public String getCategorySequence() {
		return CategorySequence;
	}

	public void setCategorySequence(String categorySequence) {
		CategorySequence = categorySequence;
	}

	public String getCategoryName() {
		return CategoryName;
	}

	public void setCategoryName(String categoryName) {
		CategoryName = categoryName;
	}

	public String getCategoryAmountSummary() {
		return CategoryAmountSummary;
	}

	public void setCategoryAmountSummary(String categoryAmountSummary) {
		CategoryAmountSummary = categoryAmountSummary;
	}

	public String getChargeItemSequence() {
		return ChargeItemSequence;
	}

	public void setChargeItemSequence(String chargeItemSequence) {
		ChargeItemSequence = chargeItemSequence;
	}

	public String getChargeItemName() {
		return ChargeItemName;
	}

	public void setChargeItemName(String chargeItemName) {
		ChargeItemName = chargeItemName;
	}

	public String getAmount() {
		return Amount;
	}

	public void setAmount(String amount) {
		Amount = amount;
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
				"CategorySequence : " + CategorySequence + lineEnd +
				"CategoryName : " + CategoryName + lineEnd +
				"CategoryAmountSummary : " + CategoryAmountSummary + lineEnd +
				"ChargeItemSequence : " + ChargeItemSequence + lineEnd +
				"ChargeItemName : " + ChargeItemName + lineEnd +
				"Amount : " + Amount + lineEnd ;
	}
	
}

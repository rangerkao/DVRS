package bill.bean;

import java.util.List;

public class ChargeDetail {

	private String recordFlag;//資料類別(識別資料格式)
	private String accountNum;//帳戶編號(帳單鍵值)
	private String serviceCode;//電話號碼
	private String categorySequence;//分類顯示順序
	private String categoryName;//分類名
	private String categoryAmountSummary;//分類項費用小計
	private String chargeItemSequence;//費用項顯示順序
	private String chargeItemName;//費用項名
	private String amount;//費用額度
	
	private String Data; //Bean 內容資料

	public ChargeDetail(){
		
	}
	
	public ChargeDetail(List<String> list){
		int n=0;
		if(list!=null) n=list.size();
		if(n>=9)n=9;
		
		switch(n){
		case 9:amount=list.get(8);
		case 8:chargeItemName=list.get(7);
		case 7:chargeItemSequence=list.get(6);
		case 6:categoryAmountSummary=list.get(5);
		case 5:categoryName=list.get(4);
		case 4:categorySequence=list.get(3);
		case 3:serviceCode=list.get(2);
		case 2:accountNum=list.get(1);
		case 1:recordFlag=list.get(0);
		default:
		}
		
		setData();
	}
	
	public ChargeDetail(String recordFlag, String accountNum,
			String serviceCode, String categorySequence, String categoryName,
			String categoryAmountSummary, String chargeItemSequence,
			String chargeItemName, String amount) {
		super();
		this.recordFlag = recordFlag;
		this.accountNum = accountNum;
		this.serviceCode = serviceCode;
		this.categorySequence = categorySequence;
		this.categoryName = categoryName;
		this.categoryAmountSummary = categoryAmountSummary;
		this.chargeItemSequence = chargeItemSequence;
		this.chargeItemName = chargeItemName;
		this.amount = amount;
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

	public String getCategorySequence() {
		return categorySequence;
	}

	public void setCategorySequence(String categorySequence) {
		this.categorySequence = categorySequence;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryAmountSummary() {
		return categoryAmountSummary;
	}

	public void setCategoryAmountSummary(String categoryAmountSummary) {
		this.categoryAmountSummary = categoryAmountSummary;
	}

	public String getChargeItemSequence() {
		return chargeItemSequence;
	}

	public void setChargeItemSequence(String chargeItemSequence) {
		this.chargeItemSequence = chargeItemSequence;
	}

	public String getChargeItemName() {
		return chargeItemName;
	}

	public void setChargeItemName(String chargeItemName) {
		this.chargeItemName = chargeItemName;
	}
	
	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
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
				"CategorySequence : " + categorySequence + lineEnd +
				"CategoryName : " + categoryName + lineEnd +
				"CategoryAmountSummary : " + categoryAmountSummary + lineEnd +
				"ChargeItemSequence : " + chargeItemSequence + lineEnd +
				"ChargeItemName : " + chargeItemName + lineEnd +
				"Amount : " + amount + lineEnd ;
	}
	
}

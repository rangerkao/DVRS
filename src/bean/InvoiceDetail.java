package bean;

import java.util.List;

public class InvoiceDetail {
	
	private String recordFlag; //資料類別(識別資料格式)
	private String accountNum; //帳戶編號(帳單鍵值)
	private String serviceCode; //電話號碼
	private String orderSequence; //顯示排列順序
	private String chargeItemName; //費用項名
	private String amount; //費用項金額
	
	private String Data; //Bean 內容資料
	
	public InvoiceDetail(){
		
	}
	
	public InvoiceDetail(List<String> list){
		int n=0;
		if(list!=null) n=list.size();
		if(n>=6)n=6;
		
		switch(n){
			case 6:amount = list.get(5);
			case 5:chargeItemName = list.get(4);
			case 4:orderSequence = list.get(3);
			case 3:serviceCode = list.get(2);
			case 2:accountNum = list.get(1);
			case 1:recordFlag = list.get(0);
			default:	
		}	
		
		setData();
	}
	
	public InvoiceDetail(String recordFlag, String accountNum,
			String serviceCode, String orderSequence, String chargeItemName,
			String amount) {
		super();
		this.recordFlag = recordFlag;
		this.accountNum = accountNum;
		this.serviceCode = serviceCode;
		this.orderSequence = orderSequence;
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
	public String getOrderSequence() {
		return orderSequence;
	}
	public void setOrderSequence(String orderSequence) {
		this.orderSequence = orderSequence;
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
		amount = amount;
	}

	public String getData() {
		return Data;
	}

	public void setData() {
		String lineEnd="<br>\n";
		Data = 
				"RecordFlag : " + recordFlag + lineEnd +
				"AccountNum : " + accountNum + lineEnd +
				"ServiceCode : " + serviceCode + lineEnd +
				"OrderSequence : " + orderSequence + lineEnd +
				"ChargeItemName : " + chargeItemName + lineEnd +
				"Amount : " + amount + lineEnd ;
	}

}

package bean;

public class InvoiceDetail {
	
	private String RecordFlag; //資料類別(識別資料格式)
	private String AccountNum; //帳戶編號(帳單鍵值)
	private String ServiceCode; //電話號碼
	private String OrderSequence; //顯示排列順序
	private String ChargeItemName; //費用項名
	private String Amount; //費用項金額
	
	private String Data; //Bean 內容資料
	
	public InvoiceDetail(){
		
	}
	
	public InvoiceDetail(String[] data){
		int n=0;
		if(data!=null) n=data.length;
		if(n>=6)n=6;
		
		switch(n){
			case 6:Amount = data[5];
			case 5:ChargeItemName = data[4];
			case 4:OrderSequence = data[3];
			case 3:ServiceCode = data[2];
			case 2:AccountNum = data[1];
			case 1:RecordFlag = data[0];
			default:	
		}	
		
		setData();
	}
	
	public InvoiceDetail(String recordFlag, String accountNum,
			String serviceCode, String orderSequence, String chargeItemName,
			String amount) {
		super();
		RecordFlag = recordFlag;
		AccountNum = accountNum;
		ServiceCode = serviceCode;
		OrderSequence = orderSequence;
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
	public String getOrderSequence() {
		return OrderSequence;
	}
	public void setOrderSequence(String orderSequence) {
		OrderSequence = orderSequence;
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
				"RecordFlag : " + RecordFlag + lineEnd +
				"AccountNum : " + AccountNum + lineEnd +
				"ServiceCode : " + ServiceCode + lineEnd +
				"OrderSequence : " + OrderSequence + lineEnd +
				"ChargeItemName : " + ChargeItemName + lineEnd +
				"Amount : " + Amount + lineEnd ;
	}

}

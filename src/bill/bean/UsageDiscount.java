package bill.bean;

import java.util.List;

public class UsageDiscount {

	String recordFlag;				//������O(�ѧO��Ʈ榡)
	String accountNum;				//�b��s��(�b�����)
	String serviceCode;				//�q�ܸ��X
	String customerName;			//�Ȥ�W��(���q�ܸ��X)
	String accountName;				//�b���W��
	String cycleBeginDate;			//�b���}�l�ɶ�
	String cycleEndDate;			//�b�������ɶ�
	String totalDiscount;			//�q�H�O�u�f
	String chnctDiscountTime;		//���䤤�걵ť�x�W�౵�ӹq���ɶ�
	String chnctDiscountAmount;		//���䤤�걵ť�x�W�౵�ӹq�u�f���B
	String chnotDiscountTime;		//����(CHNOT)��ť�x�W�౵�ӹq���ɶ�	
	String chnotDiscountAmount;		//����(CHNOT)��ť�x�W�౵�ӹq�u�f���B
	String macDiscountTime;			//�D����ť�x�W�౵�ӹq���ɶ�
	String macDiscountAmount;		//�D����ť�x�W�౵�ӹq�u�f���B
	
	String data;
	
	public UsageDiscount(){
		
	}
	
	public UsageDiscount(List<String> list){
		int n=0;
		if(list!=null) n=list.size();
		if(n>=14)n=14;
		
		switch(n){

		case 14:macDiscountAmount=list.get(13);
		case 13:macDiscountTime=list.get(12);
		case 12:chnotDiscountAmount=list.get(11);
		case 11:chnotDiscountTime=list.get(10);
		case 10:chnctDiscountAmount=list.get(9);
		case 9:chnctDiscountTime=list.get(8);
		case 8:totalDiscount=list.get(7);
		case 7:cycleEndDate=list.get(6);
		case 6:cycleBeginDate=list.get(5);
		case 5:accountName=list.get(4);
		case 4:customerName=list.get(3);
		case 3:serviceCode=list.get(2);
		case 2:accountNum=list.get(1);
		case 1:recordFlag=list.get(0);
		default:
		}
		
		setData();
	}

	public UsageDiscount(String recordFlag, String accountNum,
			String serviceCode, String customerName, String accountName,
			String cycleBeginDate, String cycleEndDate, String totalDiscount,
			String chnctDiscountTime, String chnctDiscountAmount,
			String chnotDiscountTime, String chnotDiscountAmount,
			String macDiscountTime, String macDiscountAmount) {
		super();
		this.recordFlag = recordFlag;
		this.accountNum = accountNum;
		this.serviceCode = serviceCode;
		this.customerName = customerName;
		this.accountName = accountName;
		this.cycleBeginDate = cycleBeginDate;
		this.cycleEndDate = cycleEndDate;
		this.totalDiscount = totalDiscount;
		this.chnctDiscountTime = chnctDiscountTime;
		this.chnctDiscountAmount = chnctDiscountAmount;
		this.chnotDiscountTime = chnotDiscountTime;
		this.chnotDiscountAmount = chnotDiscountAmount;
		this.macDiscountTime = macDiscountTime;
		this.macDiscountAmount = macDiscountAmount;
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

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getCycleBeginDate() {
		return cycleBeginDate;
	}

	public void setCycleBeginDate(String cycleBeginDate) {
		this.cycleBeginDate = cycleBeginDate;
	}

	public String getCycleEndDate() {
		return cycleEndDate;
	}

	public void setCycleEndDate(String cycleEndDate) {
		this.cycleEndDate = cycleEndDate;
	}

	public String getTotalDiscount() {
		return totalDiscount;
	}

	public void setTotalDiscount(String totalDiscount) {
		this.totalDiscount = totalDiscount;
	}

	public String getChnctDiscountTime() {
		return chnctDiscountTime;
	}

	public void setChnctDiscountTime(String chnctDiscountTime) {
		this.chnctDiscountTime = chnctDiscountTime;
	}

	public String getChnctDiscountAmount() {
		return chnctDiscountAmount;
	}

	public void setChnctDiscountAmount(String chnctDiscountAmount) {
		this.chnctDiscountAmount = chnctDiscountAmount;
	}

	public String getChnotDiscountTime() {
		return chnotDiscountTime;
	}

	public void setChnotDiscountTime(String chnotDiscountTime) {
		this.chnotDiscountTime = chnotDiscountTime;
	}

	public String getChnotDiscountAmount() {
		return chnotDiscountAmount;
	}

	public void setChnotDiscountAmount(String chnotDiscountAmount) {
		this.chnotDiscountAmount = chnotDiscountAmount;
	}

	public String getMacDiscountTime() {
		return macDiscountTime;
	}

	public void setMacDiscountTime(String macDiscountTime) {
		this.macDiscountTime = macDiscountTime;
	}

	public String getMacDiscountAmount() {
		return macDiscountAmount;
	}

	public void setMacDiscountAmount(String macDiscountAmount) {
		this.macDiscountAmount = macDiscountAmount;
	}

	public String getData() {
		return data;
	}

	public void setData() {
		String lineEnd="<br>\n";
		data = 
				"Record Flag : " + recordFlag + lineEnd +
				"AccountNum : " + accountNum + lineEnd +
				"ServiceCode : " + serviceCode + lineEnd +
				"Record Flag : " + recordFlag + lineEnd + 
				"AccountNum : " + accountNum + lineEnd + 
				"ServiceCode : " + serviceCode + lineEnd + 
				"CustomerName : " + customerName + lineEnd + 
				"AccountName : " + accountName + lineEnd + 
				"CycleBeginDate : " + cycleBeginDate + lineEnd + 
				"CycleEndDate : " + cycleEndDate + lineEnd + 
				"TotalDiscount : " + totalDiscount + lineEnd + 
				"CHNCTDiscountTime : " + chnctDiscountTime + lineEnd + 
				"CHNCTDiscountAmount : " + chnctDiscountAmount + lineEnd + 
				"CHNOTDiscountTime : " + chnotDiscountTime + lineEnd + 
				"CHNOTDiscountAmount : " + chnotDiscountAmount + lineEnd + 
				"MACDiscountTime : " + macDiscountTime + lineEnd + 
				"MACDiscountAmount : " + macDiscountAmount + lineEnd ;
	}
	
	
	
	
}

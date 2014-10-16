package action;

import control.BillReport;
import bean.BillData;

public class BillAction {
	
	private String fileName;
	private BillData billData;
	
	private BillReport billc=new BillReport();
	
	public String bill()  
	{  
		setBillData(billc.process(fileName));
		
		/*if(billData!=null)
		System.out.println(billData.getI().getData());*/
	   return "bill";        
	}
	
	/**
	 * getter and setter
	 */

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public BillData getBillData() {
		return billData;
	}

	public void setBillData(BillData billData) {
		this.billData = billData;
	}
	
	

}

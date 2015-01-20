package action;

import control.BillReport;
import bill.bean.BillData;

public class BillAction extends BaseAction{
	
	public BillAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String fileName;
	private BillData billData;
	
	private BillReport billc=new BillReport();
	
	public String bill()  
	{  
		//setBillData(billc.process(fileName));
		billc.process(fileName,1);
		/*if(billData!=null)
		System.out.println(billData.getI().getData());*/
	   return SUCCESS;        
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

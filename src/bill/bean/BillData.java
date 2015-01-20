package bill.bean;

import java.util.ArrayList;
import java.util.List;

public class BillData {
	private Invoice I;
	private List<InvoiceDetail> J;
	private List<BillSubData> BS;
	
/*	private Charge C;
	private List<ChargeDetail> D;
	private Usage U;
	private List<UsageDetail> R;
	*/
	public BillData(){
		J=new ArrayList<InvoiceDetail>();
		BS=new ArrayList<BillSubData>();
		/*D=new ArrayList<ChargeDetail>();
		R=new ArrayList<UsageDetail>();*/
	}

	public BillData(Invoice i, List<InvoiceDetail> j, List<BillSubData> bS) {
		super();	
		I = i;	
		J = j;	
		BS = bS;
	}

	public Invoice getI() {
		return I;
	}

	public void setI(Invoice i) {
		I = i;
	}

	public List<InvoiceDetail> getJ() {
		return J;
	}

	public void setJ(List<InvoiceDetail> j) {
		J = j;
	}

	public List<BillSubData> getBS() {
		return BS;
	}

	public void setBS(List<BillSubData> bS) {
		BS = bS;
	}

	
	
	
}

package bill.bean;

import java.util.ArrayList;
import java.util.List;

public class BillData {
	private Invoice I;
	private List<InvoiceDetail> J;
	private Charge C;
	private List<ChargeDetail> D;
	private Usage U;
	private List<UsageDetail> R;
	
	public BillData(){
		J=new ArrayList<InvoiceDetail>();
		D=new ArrayList<ChargeDetail>();
		R=new ArrayList<UsageDetail>();
	}

	public BillData(Invoice i, List<InvoiceDetail> j, Charge c,
			List<ChargeDetail> d, Usage u, List<UsageDetail> r) {
		super();
		I = i;
		J = j;
		C = c;
		D = d;
		U = u;
		R = r;
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

	public Charge getC() {
		return C;
	}

	public void setC(Charge c) {
		C = c;
	}

	public List<ChargeDetail> getD() {
		return D;
	}

	public void setD(List<ChargeDetail> d) {
		D = d;
	}

	public Usage getU() {
		return U;
	}

	public void setU(Usage u) {
		U = u;
	}

	public List<UsageDetail> getR() {
		return R;
	}

	public void setR(List<UsageDetail> r) {
		R = r;
	}

	
	
	
	
}

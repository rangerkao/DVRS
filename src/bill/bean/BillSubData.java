package bill.bean;

import java.util.ArrayList;
import java.util.List;

public class BillSubData {

	private String serviceCode;
	private String priceplan;
	private Charge c;
	private List<ChargeDetail> d;
	private Usage u;
	private List<UsageDetail> r;
	
	//for report preproccess
	private Double totalCurrentCharge;
	private Double totalUsageCharge;
	private List<UsageDetail> voiceUsageCharges;
	private List<UsageDetail> smsCharges;
	private List<UsageDetail> gprsUsageCharges;
	private List<UsageDetail> mmsCharges;
	
	private List<UsageDetail> r1VoiceUsageCharges;
	private List<UsageDetail> r1SmsCharges;
	
	//for template 3 
	private Usage u1;
	private List<UsageDetail> r1;
	private Usage u2;
	private List<UsageDetail> r2;
	private UsageDiscount p;
	
	
	public BillSubData(){
		super();
		d=new ArrayList<ChargeDetail>();
		r=new ArrayList<UsageDetail>();
		
		
		voiceUsageCharges=new ArrayList<UsageDetail>();	
		smsCharges=new ArrayList<UsageDetail>();
		gprsUsageCharges=new ArrayList<UsageDetail>();
		mmsCharges=new ArrayList<UsageDetail>();
		
		r1=new ArrayList<UsageDetail>();
		r2=new ArrayList<UsageDetail>();
		
		r1SmsCharges=new ArrayList<UsageDetail>();
		r1VoiceUsageCharges=new ArrayList<UsageDetail>();	

	}
	
	public BillSubData(String serviceCode,String priceplan){
		super();
		this.serviceCode = serviceCode;
		this.priceplan = priceplan;
		d=new ArrayList<ChargeDetail>();
		r=new ArrayList<UsageDetail>();
		
		voiceUsageCharges=new ArrayList<UsageDetail>();	
		smsCharges=new ArrayList<UsageDetail>();
		gprsUsageCharges=new ArrayList<UsageDetail>();
		mmsCharges=new ArrayList<UsageDetail>();
		
		r1=new ArrayList<UsageDetail>();
		r2=new ArrayList<UsageDetail>();

	}
	

	public BillSubData(String serviceCode, Charge c, List<ChargeDetail> d,
			Usage u, List<UsageDetail> r) {
		super();
		this.serviceCode = serviceCode;
		this.c = c;
		this.d = d;
		this.u = u;
		this.r = r;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	public Charge getC() {
		return c;
	}

	public void setC(Charge c) {
		this.c = c;
	}

	public List<ChargeDetail> getD() {
		return d;
	}

	public void setD(List<ChargeDetail> d) {
		this.d = d;
	}

	public Usage getU() {
		return u;
	}

	public void setU(Usage u) {
		this.u = u;
	}

	public List<UsageDetail> getR() {
		return r;
	}

	public void setR(List<UsageDetail> r) {
		this.r = r;
	}

	public Double getTotalCurrentCharge() {
		return totalCurrentCharge;
	}

	public void setTotalCurrentCharge(Double totalCurrentCharge) {
		this.totalCurrentCharge = totalCurrentCharge;
	}

	public Double getTotalUsageCharge() {
		return totalUsageCharge;
	}

	public void setTotalUsageCharge(Double totalUsageCharge) {
		this.totalUsageCharge = totalUsageCharge;
	}

	public List<UsageDetail> getVoiceUsageCharges() {
		return voiceUsageCharges;
	}

	public void setVoiceUsageCharges(List<UsageDetail> voiceUsageCharges) {
		this.voiceUsageCharges = voiceUsageCharges;
	}

	public List<UsageDetail> getSmsCharges() {
		return smsCharges;
	}

	public void setSmsCharges(List<UsageDetail> smsCharges) {
		this.smsCharges = smsCharges;
	}

	public List<UsageDetail> getGprsUsageCharges() {
		return gprsUsageCharges;
	}

	public void setGprsUsageCharges(List<UsageDetail> gprsUsageCharges) {
		this.gprsUsageCharges = gprsUsageCharges;
	}
	
	public List<UsageDetail> getMmsCharges() {
		return mmsCharges;
	}

	public void setMmsCharges(List<UsageDetail> mmsCharges) {
		this.mmsCharges = mmsCharges;
	}

	public String getPriceplan() {
		return priceplan;
	}

	public void setPriceplan(String priceplan) {
		this.priceplan = priceplan;
	}

	public Usage getU1() {
		return u1;
	}

	public void setU1(Usage u1) {
		this.u1 = u1;
	}

	public List<UsageDetail> getR1() {
		return r1;
	}

	public void setR1(List<UsageDetail> r1) {
		this.r1 = r1;
	}

	public Usage getU2() {
		return u2;
	}

	public void setU2(Usage u2) {
		this.u2 = u2;
	}

	public List<UsageDetail> getR2() {
		return r2;
	}

	public void setR2(List<UsageDetail> r2) {
		this.r2 = r2;
	}

	public UsageDiscount getP() {
		return p;
	}

	public void setP(UsageDiscount p) {
		this.p = p;
	}

	public List<UsageDetail> getR1VoiceUsageCharges() {
		return r1VoiceUsageCharges;
	}

	public void setR1VoiceUsageCharges(List<UsageDetail> r1VoiceUsageCharges) {
		this.r1VoiceUsageCharges = r1VoiceUsageCharges;
	}

	public List<UsageDetail> getR1SmsCharges() {
		return r1SmsCharges;
	}

	public void setR1SmsCharges(List<UsageDetail> r1SmsCharges) {
		this.r1SmsCharges = r1SmsCharges;
	}

	
	

}

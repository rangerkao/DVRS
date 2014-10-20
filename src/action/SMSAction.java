package action;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import control.SMSControl;
import bean.SMSLog;

public class SMSAction extends BaseAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String dateFrom;
	String dateTo;
	SMSLog smsLog;
	List<SMSLog> smsLoglist = new ArrayList<SMSLog>();
	private SMSControl smsControl = new SMSControl();
	
	
	public String querySMSLog() throws ParseException{
		System.out.println("dateFrom:"+dateFrom+";dateTo:"+dateTo);
		if((dateFrom==null||"".equals(dateFrom))&&(dateTo==null||"".equals(dateTo)))
			smsLoglist=smsControl.querySMSLog();
		else
			smsLoglist=smsControl.querySMSLog(tool.DateFormat(dateFrom, "yyyy-MM-dd"),
					tool.DateFormat(dateTo, "yyyy-MM-dd"));		
		
		result=beanToJSONArray(smsLoglist);
		
		return SUCCESS;
	}

	public String getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(String dateFrom) {
		this.dateFrom = dateFrom;
	}





	public String getDateTo() {
		return dateTo;
	}


	public void setDateTo(String dateTo) {
		this.dateTo = dateTo;
	}


	public SMSLog getSmsLog() {
		return smsLog;
	}


	public void setSmsLog(SMSLog smsLog) {
		this.smsLog = smsLog;
	}


	public List<SMSLog> getSmsLoglist() {
		return smsLoglist;
	}


	public void setSmsLoglist(List<SMSLog> smsLoglist) {
		this.smsLoglist = smsLoglist;
	}
	
	
}

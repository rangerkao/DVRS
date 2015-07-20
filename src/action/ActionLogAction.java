package action;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bean.ActionLog;
import bean.SMSLog;
import control.ActionLogControl;

public class ActionLogAction extends BaseAction {

	public ActionLogAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	String dateFrom;
	String dateTo;
	SMSLog actionLog;
	List<ActionLog> actionLoglist = new ArrayList<ActionLog>();
	
	public String queryActionLog() throws ParseException, SQLException{
		try {
			System.out.println("dateFrom:"+dateFrom+";dateTo:"+dateTo);
			if((dateFrom==null||"".equals(dateFrom))&&(dateTo==null||"".equals(dateTo))){
				actionLoglist=actionLogControl.queryActionLog();
			}
			else{
				actionLoglist=actionLogControl.queryActionLog(DateFormat(dateFrom, "yyyy-MM-dd"),
						DateFormat(dateTo, "yyyy-MM-dd"));		
			}

			result=makeResult(actionLoglist, null);
			
			actionLogControl.loggerAction(super.getUser().getAccount(), "ActionLog", "query", "dateFrom:"+dateFrom+";dateTo:"+dateTo, SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		
		return SUCCESS;
	}
	static String iniform= "yyyy/MM/dd HH:mm:ss";
	public static String DateFormat(){
		DateFormat dateFormat = new SimpleDateFormat(iniform);
		return dateFormat.format(new Date());
	}
	public Date DateFormat(String dateString, String form) throws ParseException {
		Date result=new Date();
		
		if(dateString==null) return result;

		if(form==null ||"".equals(form)) form=iniform;
		DateFormat dateFormat = new SimpleDateFormat(form);
		result=dateFormat.parse(dateString);
		
		return result;
	}
	public String DateFormat(Date date, String form) {
		
		if(date==null) date=new Date();
		if(form==null ||"".equals(form)) form=iniform;
		
		DateFormat dateFormat = new SimpleDateFormat(form);
		return dateFormat.format(date);
	}


	//-------------------------------------------------//
	
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


	public SMSLog getActionLog() {
		return actionLog;
	}


	public void setActionLog(SMSLog actionLog) {
		this.actionLog = actionLog;
	}


	public List<ActionLog> getActionLoglist() {
		return actionLoglist;
	}


	public void setActionLoglist(List<ActionLog> actionLoglist) {
		this.actionLoglist = actionLoglist;
	}




	
}

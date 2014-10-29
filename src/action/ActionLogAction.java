package action;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
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
		System.out.println("dateFrom:"+dateFrom+";dateTo:"+dateTo);
		if((dateFrom==null||"".equals(dateFrom))&&(dateTo==null||"".equals(dateTo))){
			actionLoglist=actionLogControl.queryActionLog();
		}
		else{
			actionLoglist=actionLogControl.queryActionLog(tool.DateFormat(dateFrom, "yyyy-MM-dd"),
					tool.DateFormat(dateTo, "yyyy-MM-dd"));		
		}
		result=beanToJSONArray(actionLoglist);

		actionLogControl.loggerAction(super.getUser().getAccount(), "ActionLog", "query", "dateFrom:"+dateFrom+";dateTo:"+dateTo, result);
		
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

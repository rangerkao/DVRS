package control;

import java.util.Date;
import java.util.List;

import bean.ActionLog;
import dao.ActionLogDao;



public class ActionLogControl extends BaseControl {

	private ActionLogDao actionLogDao = new ActionLogDao();
	
	public List<ActionLog> queryActionLog(Date fromDate,Date toDate){
		return actionLogDao.queryActionLog(fromDate, toDate);
	}
	
	public List<ActionLog> queryActionLog(){
		return actionLogDao.queryActionLog();
	}
}

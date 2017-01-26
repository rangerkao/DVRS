package control;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import bean.ActionLog;
import dao.ActionLogDao;



public class ActionLogControl extends BaseControl {

	public ActionLogControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	private ActionLogDao actionLogDao = new ActionLogDao();
	
	
	
	public List<ActionLog> queryActionLog(Date fromDate,Date toDate) throws SQLException, UnsupportedEncodingException,ClassNotFoundException{
		List<ActionLog> r = actionLogDao.queryActionLog(fromDate, toDate);
		
		return r;
	}
	
	public List<ActionLog> queryActionLog() throws SQLException, UnsupportedEncodingException,ClassNotFoundException{
		List<ActionLog> r = actionLogDao.queryActionLog();
		
		return r;
	}
	
	public int loggerAction(String userid,String page,String action,String parameter,String result) throws Exception{
		int r = actionLogDao.loggerAction(userid, page, action, parameter, result);
		
		return r;
	}
}

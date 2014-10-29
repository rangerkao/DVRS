package control;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import bean.SMSLog;
import bean.SMSSetting;
import dao.SMSDao;

public class SMSControl {

	SMSDao smsDao = new SMSDao();;
	
	public SMSControl() throws Exception {
		super();
	}
	
	public List<SMSLog> querySMSLog() throws SQLException{
		return smsDao.querySMSLog();
	}
	public List<SMSLog> querySMSLog(Date fromDate,Date toDate) throws SQLException{
		return smsDao.querySMSLog();
	}
	public List<SMSSetting> querySMSSetting() throws SQLException{
		return smsDao.querySMSSetting();
	}
	public List<SMSSetting> updateSMSSetting(List<SMSSetting> list) throws SQLException{
		return smsDao.updateSMSSetting(list);
	}
}

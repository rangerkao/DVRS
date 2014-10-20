package control;

import java.util.Date;
import java.util.List;

import bean.SMSLog;
import dao.SMSDao;

public class SMSControl {

	private SMSDao smsDao =new SMSDao();
	
	public List<SMSLog> querySMSLog(){
		return smsDao.querySMSLog();
	}
	public List<SMSLog> querySMSLog(Date fromDate,Date toDate){
		return smsDao.querySMSLog();
	}
}

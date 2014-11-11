package control;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import bean.GPRSThreshold;
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
	public List<GPRSThreshold> queryAlertLimit() throws SQLException{
		return smsDao.queryAlertLimit();
	}
	public int insertAlertLimit(String imsi,Double limit) throws SQLException{
		return smsDao.insertAlertLimit(imsi,limit);
	}
	public int updateAlertLimit(String imsi,Double limit) throws SQLException{
		return smsDao.updateAlertLimit(imsi, limit);
	}
	public int deleteAlertLimit(String imsi,Double limit) throws SQLException{
		return smsDao.deleteAlertLimit(imsi, limit);
	}
	public Map<String,String> queryIMSI(String msisdn) throws SQLException{
		return smsDao.queryIMSI(msisdn);
	}

}

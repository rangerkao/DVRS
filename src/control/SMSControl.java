package control;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import bean.GPRSThreshold;
import bean.SMSLog;
import bean.SMSSetting;
import dao.SMSDao;
import program.DVRSmain;
import program.IJatool;
import program.Jatool;

public class SMSControl {

	SMSDao smsDao = new SMSDao();
	IJatool tool =new Jatool();
	
	Logger logger = Logger.getLogger(SMSControl.class);
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
	public int insertAlertLimit(String imsi,Double limit,Boolean sendSMS,String msisdn) throws SQLException, IOException{
		
		if(sendSMS){
			String content=smsDao.getSMSContent(100);
			if(content!=null && !"".equals(content)){
				if(msisdn==null ||"".equals(msisdn)){
					logger.error("Can't send SMS without msisdn number!");
				}else{
					setSMSPostParam(content,msisdn);
				}
					
			}
		}
		return smsDao.insertAlertLimit(imsi,limit);
	}
	public int updateAlertLimit(String imsi,Double limit,Boolean sendSMS,String msisdn) throws SQLException, IOException{
		if(sendSMS){
			String content=smsDao.getSMSContent(100);
			if(content!=null && !"".equals(content)){
				if(msisdn==null ||"".equals(msisdn)){
					logger.error("Can't send SMS without msisdn number!");
				}else{
					setSMSPostParam(content,msisdn);
				}
					
			}
		}
		return smsDao.updateAlertLimit(imsi, limit);
	}
	public int deleteAlertLimit(String imsi,Double limit,Boolean sendSMS,String msisdn) throws SQLException, IOException{
		if(sendSMS){
			String content=smsDao.getSMSContent(100);
			if(content!=null && !"".equals(content)){
				if(msisdn==null ||"".equals(msisdn)){
					logger.error("Can't send SMS without msisdn number!");
				}else{
					setSMSPostParam(content,msisdn);
				}
					
			}
		}
		return smsDao.deleteAlertLimit(imsi, limit);
	}
	public Map<String,String> queryIMSI(String msisdn) throws SQLException{
		return smsDao.queryIMSI(msisdn);
	}
	
	
	
	
	/**
	 * 發送簡訊功能
	 * 處理post到 http網頁的Url並傳送
	 * @param msg
	 * @param phone
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private String setSMSPostParam(String msg,String phone) throws IOException{
		StringBuffer sb=new StringBuffer ();
		
		String PhoneNumber=phone,Text=msg,charset="big5",InfoCharCounter=null,PID=null,DCS=null;
		String param =
				"PhoneNumber=+{{PhoneNumber}}&"
				+ "Text={{Text}}&"
				+ "charset={{charset}}&"
				+ "InfoCharCounter={{InfoCharCounter}}&"
				+ "PID={{PID}}&"
				+ "DCS={{DCS}}&"
				+ "Submit=Submit";
		
		if(PhoneNumber==null)PhoneNumber="";
		if(Text==null)Text="";
		if(charset==null)charset="";
		if(InfoCharCounter==null)InfoCharCounter="";
		if(PID==null)PID="";
		if(DCS==null)DCS="";
		param=param.replace("{{PhoneNumber}}",PhoneNumber );
		param=param.replace("{{Text}}",Text );
		param=param.replace("{{charset}}",charset );
		param=param.replace("{{InfoCharCounter}}",InfoCharCounter );
		param=param.replace("{{PID}}",PID );
		param=param.replace("{{DCS}}",DCS );
		
		
		
		return tool.HttpPost("http://192.168.10.125:8800/Send%20Text%20Message.htm", param,"");
	}
}

package control;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import bean.GPRSThreshold;
import bean.SMSContent;
import bean.SMSLog;
import bean.SMSSetting;
import dao.SMSDao;


public class SMSControl extends BaseControl{

	SMSDao smsDao = new SMSDao();
	
	public SMSControl() throws Exception {
		super();
	}
	
	public List<SMSLog> querySMSLog() throws SQLException, UnsupportedEncodingException{
		return smsDao.querySMSLog();
	}
	public List<SMSLog> querySMSLog(String fromDate,String toDate,String msisdn) throws SQLException, UnsupportedEncodingException{
		if((fromDate==null||"".equals(fromDate))&&(toDate==null||"".equals(toDate))&&(msisdn==null||"".equals(msisdn)) )
			return querySMSLog();
		
		return smsDao.querySMSLog(fromDate,toDate,msisdn);
	}
	public List<SMSSetting> querySMSSetting() throws SQLException{
		return smsDao.querySMSSetting();
	}
	public List<SMSSetting> updateSMSSetting(List<SMSSetting> list) throws SQLException{
		return smsDao.updateSMSSetting(list);
	}
	public List<GPRSThreshold> queryAlertLimit() throws SQLException, ParseException{
		return smsDao.queryAlertLimit();
	}
	public int insertAlertLimit(String imsi,Double limit,Boolean sendSMS,String msisdn) throws Exception{
		
		int result = smsDao.insertAlertLimit(imsi,limit);
		if(sendSMS){
			sendSMS("6",msisdn,imsi,"VIP",null,null);
		}
		return result;
	}
	public int updateAlertLimit(String imsi,Double limit,Boolean sendSMS,String msisdn) throws SQLException, IOException{
		/*if(sendSMS){
			String content=smsDao.getSMSContent(100);
			if(content!=null && !"".equals(content)){
				if(msisdn==null ||"".equals(msisdn)){
					logger.error("Can't send SMS without msisdn number!");
				}else{
					setSMSPostParam(content,msisdn);
				}
					
			}
		}*/
		return smsDao.updateAlertLimit(imsi, limit);
	}
	public int deleteAlertLimit(String imsi,Double limit,Boolean sendSMS,String msisdn) throws SQLException, IOException{
		/*if(sendSMS){
			String content=smsDao.getSMSContent(100);
			if(content!=null && !"".equals(content)){
				if(msisdn==null ||"".equals(msisdn)){
					logger.error("Can't send SMS without msisdn number!");
				}else{
					setSMSPostParam(content,msisdn);
				}
					
			}
		}*/
		return smsDao.deleteAlertLimit(msisdn);
	}
	public String checkAlertExisted(String msisdn) throws SQLException{
		return smsDao.checkAlertExisted(msisdn);
	}
	public Map<String,String> queryIMSI(String msisdn) throws SQLException{
		return smsDao.queryIMSI(msisdn);
	}
	public Map<String,String> queryMSISDN(String imsi) throws SQLException{
		return smsDao.queryMSISDN(imsi);
	}
	public Map<String,String> queryTWNMSISDN(String msisdn) throws SQLException{
		return smsDao.queryTWNMSISDN(msisdn);
	}
	
	public void sendSMS(String smsId,String msisdn,String imsi,String SMStype,String[] paramName,String[] paramValue) throws Exception{
		String content=smsDao.getSMSContent(smsId);
		if(content!=null && !"".equals(content)){
			
			if(paramName!=null){
				for(int i = 0;i<paramName.length;i++){
					content = content.replace(paramName[i], paramValue[i]);
				}
			}
			
			if(msisdn==null ||"".equals(msisdn)){
			}else{
				
				String res=setSMSPostParam(content,msisdn);
				smsDao.logSendSMS(msisdn, content, res,SMStype);
			}
				
		}else {
			throw new Exception("Can't send SMS without content!");
		}
	}
	
	
	
	
	/**
	 * �o�e²�T�\��
	 * �B�zpost�� http������Url�öǰe
	 * @param msg
	 * @param phone
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private String setSMSPostParam(String msg,String phone) throws IOException{
		StringBuffer sb=new StringBuffer ();
		if("true".equals(props.getProperty("progrma.TEST_MODE"))){
			System.out.println("test mode");
			phone=props.getProperty("progrma.DEFAULT_PHONE");
		}
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
		
		return HttpPost("http://10.42.200.100:8800/Send%20Text%20Message.htm", param,"");
	}
	
	public String HttpPost(String url,String param,String charset) throws IOException{
		URL obj = new URL(url);
		
		if(charset!=null && !"".equals(charset))
			param=URLEncoder.encode(param, charset);
		
		
		HttpURLConnection con =  (HttpURLConnection) obj.openConnection();
 
		//add reuqest header
		/*con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");*/
 
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(param);
		wr.flush();
		wr.close();
 
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + new String(param.getBytes("ISO8859-1")));
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
 
		//print result
		return(response.toString());
	}
	private String queryCustmerServicePhone(String imsi) throws SQLException{
		String cphone=null;
		String VLN=smsDao.queryVLR(imsi);
		
		if(VLN!=null && !"".equals(VLN)){
			Map<String,String> tadigMap = smsDao.queryTADIG();
			
			if(tadigMap.size()>0){
				String tadig=null;
				for(int i=VLN.length();i>0;i--){
					tadig=tadigMap.get(VLN.substring(0,i));
					if(tadig!=null &&!"".equals(tadig)){
						break;
					}
				}
				if(tadig!=null &&!"".equals(tadig)){
					String mccmnc=smsDao.queryMccmnc(tadig);
					if(mccmnc!=null &&!"".equals(mccmnc)){
						cphone=smsDao.queryCustomerServicePhone(mccmnc);
					}
				}
			}
		}
		return cphone;
	}
	
	public List<SMSContent> querySMSContent() throws SQLException, UnsupportedEncodingException{
		return smsDao.querySMSContent();
	}
	public List<SMSContent> querySMSContent(String id) throws SQLException, UnsupportedEncodingException{
		if(id==null ||"".equals(id))
			return smsDao.querySMSContent();
		return smsDao.querySMSContent(id);
	}
	
	public int insertSMSContent(SMSContent sc) throws Exception{
		return smsDao.insertSMSContent(sc);
	}
	public int updateSMSContent(SMSContent sc) throws Exception{
		return smsDao.updateSMSContent(sc);
	}
	public int deleteSMSContent(SMSContent sc) throws Exception{
		return smsDao.deleteSMSContent(sc);
	}
	
	public String sendSMS(String msisdn,Map<String,String> content) throws IOException, SQLException{
		

		String res;
		try {
			res = setSMSPostParam(new String(content.get("A").getBytes("BIG5"),"ISO8859-1"),msisdn);
			System.out.println("send A result = "+res);
			smsDao.logSendSMS(msisdn, new String(content.get("A").getBytes("BIG5"),"ISO8859-1"), res,"GPRS_ON");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			res = setSMSPostParam(new String(content.get("B").getBytes("BIG5"),"ISO8859-1"),msisdn);
			System.out.println("send B result = "+res);
			smsDao.logSendSMS(msisdn, new String(content.get("B").getBytes("BIG5"),"ISO8859-1"), res,"GPRS_ON");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			res = setSMSPostParam(new String(content.get("C").getBytes("BIG5"),"ISO8859-1"),msisdn);
			System.out.println("send C result = "+res);
			smsDao.logSendSMS(msisdn, new String(content.get("C").getBytes("BIG5"),"ISO8859-1"), res,"GPRS_ON");
		} finally{
			smsDao.closeConnection();
		}

		//smsDao.logSendSMS(msisdn, content, res);
		return "success";
	}
	
	public Map<String,String> queryGPRSContent() throws SQLException, UnsupportedEncodingException{
		return smsDao.queryGPRSContent();
	}
}

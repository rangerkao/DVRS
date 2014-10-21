package action;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import control.SMSControl;
import bean.SMSLog;
import bean.SMSSetting;

public class SMSAction extends BaseAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String mod;
	String dateFrom;
	String dateTo;
	SMSLog smsLog;
	SMSSetting smsSetting;
	List<SMSLog> smsLoglist = new ArrayList<SMSLog>();
	List<SMSSetting> smsSettinglist = new ArrayList<SMSSetting>();
	String smsSettinglistString;
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
	
	
	public String querySMSSetting(){
		
		smsSettinglist=smsControl.querySMSSetting();
		result=beanToJSONArray(smsSettinglist);
		
		return SUCCESS;
	}
	
	
	public String updateSMSSetting(){
		JSONArray json =new JSONArray(smsSettinglistString);
		
			for(int i=0;i<json.length();i++){
				JSONObject o=json.getJSONObject(i);
				SMSSetting set=new SMSSetting();
				set.setBracket(o.getDouble("bracket"));
				set.setMsg(o.getString("msg"));
				set.setSuspend(o.getBoolean("suspend"));
				smsSettinglist.add(set);
			}
			
			System.out.println("mod:"+mod);
			
			
			if("add".equalsIgnoreCase(mod)){
				boolean inserted=false;
				//�M�䴡�J�I
				for(int i=0;i<smsSettinglist.size();i++){
					SMSSetting s= smsSettinglist.get(i);
					if(s.getBracket()>smsSetting.getBracket()){
						smsSettinglist.add(i,smsSetting);
						inserted=!inserted;
						break;
					}
				}
				if(!inserted){
					smsSettinglist.add(smsSetting);
				}
			}else if("mod".equalsIgnoreCase(mod)){
				smsSettinglist.get(Integer.parseInt(smsSetting.getId())-1).setBracket(smsSetting.getBracket());
				smsSettinglist.get(Integer.parseInt(smsSetting.getId())-1).setMsg(smsSetting.getMsg());
				smsSettinglist.get(Integer.parseInt(smsSetting.getId())-1).setSuspend(smsSetting.getSuspend());
				
			}else if("del".equalsIgnoreCase(mod)){
				smsSettinglist.remove(Integer.parseInt(smsSetting.getId())-1);
			}		
			
			//���s�s��ID
			for(int i=0;i<smsSettinglist.size();i++){
				smsSettinglist.get(i).setId(Integer.toString(i+1));
			}
			smsSettinglist=smsControl.updateSMSSetting(smsSettinglist);
			result=beanToJSONArray(smsSettinglist);

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


	public SMSSetting getSmsSetting() {
		return smsSetting;
	}


	public void setSmsSetting(SMSSetting smsSetting) {
		this.smsSetting = smsSetting;
	}


	public List<SMSSetting> getSmsSettinglist() {
		return smsSettinglist;
	}


	public void setSmsSettinglist(List<SMSSetting> smsSettinglist) {
		this.smsSettinglist = smsSettinglist;
	}


	public String getSmsSettinglistString() {
		return smsSettinglistString;
	}


	public void setSmsSettinglistString(String smsSettinglistString) {
		this.smsSettinglistString = smsSettinglistString;
	}


	public String getMod() {
		return mod;
	}


	public void setMod(String mod) {
		this.mod = mod;
	}
	
	
	
	
}

package action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import control.SMSControl;
import bean.GPRSThreshold;
import bean.SMSContent;
import bean.SMSLog;
import bean.SMSSetting;

public class SMSAction extends BaseAction {

	public SMSAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}


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
	String msisdn;
	String imsi;
	Double gprslimit;
	private SMSControl smsControl = new SMSControl();
	Boolean sendSMS;
	
	String SMSid;
	SMSContent sc ;
	String COMTENT;
	String CHARSET;
	String DESCRIPTION;
	
	public String querySMSLog(){
		try {
			System.out.println("dateFrom:"+dateFrom+";dateTo:"+dateTo+";msisdn:"+msisdn);
			
			smsLoglist=smsControl.querySMSLog(dateFrom,dateTo,msisdn);		
			
			result=beanToJSONArray(smsLoglist);
			actionLogControl.loggerAction(super.getUser().getAccount(), "SMSLog", "query","", SUCCESS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		return SUCCESS;
	}
	
	
	public String querySMSSetting() {
		
		try {
			smsSettinglist=smsControl.querySMSSetting();
			result=beanToJSONArray(smsSettinglist);
			actionLogControl.loggerAction(super.getUser().getAccount(), "SMSSetting", "query","", SUCCESS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		return SUCCESS;
	}
	
	
	public String updateSMSSetting() {
		try {
			JSONArray json =new JSONArray(smsSettinglistString);
			
				for(int i=0;i<json.length();i++){
					JSONObject o=json.getJSONObject(i);
					SMSSetting set=new SMSSetting();
					set.setBracket(o.getDouble("bracket"));
					set.setMsg(o.getString("msg"));
					set.setSuspend(o.getBoolean("suspend"));
					set.setPricePlanId(o.getString("pricePlanId"));
					smsSettinglist.add(set);
				}
				
				System.out.println("mod:"+mod);
				
				
				if("add".equalsIgnoreCase(mod)){
					boolean inserted=false;
					//尋找插入點
					for(int i=0;i<smsSettinglist.size();i++){
						SMSSetting s= smsSettinglist.get(i);
						if(s.getBracket()>smsSetting.getBracket()&&s.getPricePlanId()!=null && s.getPricePlanId().equals(smsSetting.getPricePlanId())){
							smsSettinglist.add(i,smsSetting);
							inserted=!inserted;
							break;
						}
					}
					if(!inserted){
						for(int i=0;i<smsSettinglist.size();i++){
							SMSSetting s= smsSettinglist.get(i);
							if(Integer.valueOf(s.getPricePlanId())>Integer.valueOf(smsSetting.getPricePlanId())){
								smsSettinglist.add(i,smsSetting);
								inserted=!inserted;
								break;
							}
						}
					}
					
					if(!inserted){
						smsSettinglist.add(smsSetting);
					}
				}/*else if("mod".equalsIgnoreCase(mod)){
					smsSettinglist.get(Integer.parseInt(smsSetting.getId())-1).setPricePlanId(smsSetting.getPricePlanId());
					smsSettinglist.get(Integer.parseInt(smsSetting.getId())-1).setBracket(smsSetting.getBracket());
					smsSettinglist.get(Integer.parseInt(smsSetting.getId())-1).setMsg(smsSetting.getMsg());
					smsSettinglist.get(Integer.parseInt(smsSetting.getId())-1).setSuspend(smsSetting.getSuspend());
					
				}*/else if("del".equalsIgnoreCase(mod)){
					smsSettinglist.remove(Integer.parseInt(smsSetting.getId())-1);
				}		
				
				//重新編號ID
				for(int i=0;i<smsSettinglist.size();i++){
					smsSettinglist.get(i).setId(Integer.toString(i+1));
				}
				smsSettinglist=smsControl.updateSMSSetting(smsSettinglist);
				result=beanToJSONArray(smsSettinglist);
				actionLogControl.loggerAction(super.getUser().getAccount(), "SMSSetting", "update",mod+":"+smsSettinglistString, SUCCESS);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}

		return SUCCESS;
	}
	
	public String queryAlertLimit(){
		
		List<GPRSThreshold> list = new ArrayList<GPRSThreshold>();
		try {
			list = smsControl.queryAlertLimit();
			result=beanToJSONArray(list);
			actionLogControl.loggerAction(super.getUser().getAccount(), "LimitSetting", "query","", SUCCESS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		
		return SUCCESS;
	}
	
	public String updateAlertLimit(){
		System.out.println("msisdn:"+msisdn+" ; Limit : "+gprslimit+"; mod:"+mod+"; sendSMS:"+sendSMS);
		
		
		List<GPRSThreshold> list = new ArrayList<GPRSThreshold>();

		if(sendSMS){
			System.out.println("sendSMS is true");
		}else{
			System.out.println("sendSMS is false");
		}
		
		int i=0;
		try {
			if("add".equals(mod)){
				i=smsControl.insertAlertLimit(imsi, Double.valueOf(gprslimit),sendSMS,msisdn);
			}else if("mod".equals(mod)){
				i=smsControl.updateAlertLimit(imsi, Double.valueOf(gprslimit),sendSMS,msisdn);
			}else if("del".equals(mod)){
				i=smsControl.deleteAlertLimit(imsi, Double.valueOf(gprslimit),sendSMS,msisdn);
			}
			
			actionLogControl.loggerAction(super.getUser().getAccount(), "LimitSetting", "update","msisdn:"+msisdn+" ; Limit : "+gprslimit+"; mod:"+mod+"; sendSMS:"+sendSMS+"sendSMS:"+sendSMS, SUCCESS);
				
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result=e.getMessage();
			System.out.println("Exception:"+e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result=e.getMessage();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		
		return SUCCESS;
	}
	
	public String queryIMSI(){
		
		try {
			Map<String,String> map =new HashMap<String,String>();
			map=smsControl.queryIMSI(msisdn);
			result=beanToJSONObject(map);
			actionLogControl.loggerAction(super.getUser().getAccount(), "LimitSetting", "QueryIMSI","", SUCCESS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		
		
		return SUCCESS;
	}
	
	public String queryMSISDN(){
		
		try {
			Map<String,String> map =new HashMap<String,String>();
			map=smsControl.queryMSISDN(imsi);
			result=beanToJSONObject(map);
			actionLogControl.loggerAction(super.getUser().getAccount(), "LimitSetting", "QueryIMSI","", SUCCESS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		
		
		return SUCCESS;
	}
	
	public String queryTWNMSISDN(){ 
		try {
			Map<String,String> map =new HashMap<String,String>();
			map=smsControl.queryTWNMSISDN(msisdn);
			result=beanToJSONObject(map);
			actionLogControl.loggerAction(super.getUser().getAccount(), "LimitSetting", "queryTWNMSISDN","", SUCCESS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		return SUCCESS;
	}
	
	public String querySMSContent(){
		try {
			List<SMSContent> scl=smsControl.querySMSContent(SMSid);
			result=beanToJSONArray(scl);
			actionLogControl.loggerAction(super.getUser().getAccount(), "SMSContentSetting", "Query","", SUCCESS);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		
		return SUCCESS;
	}
	
	
	public String updateSMSContent(){
		
		
		if(sc==null){
			System.out.println("No perameter");
			return SUCCESS;
		}
		
		System.out.println("id:"+sc.getId()+" ; content : "+sc.getComtent()+"; charset:"+sc.getCharSet()+"; description:"+sc.getDescription());
		
		
		int i=0;

		
			try {
				if("add".equals(mod)){
					i=smsControl.insertSMSContent(sc);
				}else if("mod".equals(mod)){
					i=smsControl.updateSMSContent(sc);
				}else if("del".equals(mod)){
					i=smsControl.deleteSMSContent(sc);
				}
				String p = beanToJSONObject(sc);
				actionLogControl.loggerAction(super.getUser().getAccount(), "SMSContentSetting", "update",mod+" : "+p, SUCCESS);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Exception:"+e.getMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Exception:"+e.getMessage());
			}
	
		
		return SUCCESS;
	}

//*******************************************************************//
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


	public String getMsisdn() {
		return msisdn;
	}


	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}


	public Double getGprslimit() {
		return gprslimit;
	}


	public void setGprslimit(Double gprslimit) {
		this.gprslimit = gprslimit;
	}


	public String getImsi() {
		return imsi;
	}


	public void setImsi(String imsi) {
		this.imsi = imsi;
	}


	public Boolean getSendSMS() {
		return sendSMS;
	}


	public void setSendSMS(Boolean sendSMS) {
		this.sendSMS = sendSMS;
	}


	public String getSMSid() {
		return SMSid;
	}


	public void setSMSid(String sMSid) {
		SMSid = sMSid;
	}


	public SMSContent getSc() {
		return sc;
	}


	public void setSc(SMSContent sc) {
		this.sc = sc;
	}


	public String getCOMTENT() {
		return COMTENT;
	}


	public void setCOMTENT(String cOMTENT) {
		COMTENT = cOMTENT;
	}


	public String getCHARSET() {
		return CHARSET;
	}


	public void setCHARSET(String cHARSET) {
		CHARSET = cHARSET;
	}


	public String getDESCRIPTION() {
		return DESCRIPTION;
	}


	public void setDESCRIPTION(String dESCRIPTION) {
		DESCRIPTION = dESCRIPTION;
	}
	
	
	
	
	
	
}

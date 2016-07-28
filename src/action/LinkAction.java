package action;

import interceptor.Authority;

import java.util.Map;




import com.opensymphony.xwork2.ActionContext;

import control.LogoutControl;

public class LinkAction extends BaseAction{
	public LinkAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String tag;
	private String LOGIN="login";
	private String BILL="bill";
	private String Admin="admin";
	private String DataRate="dataRate";
	private String SmsQuery="smsQuery";
	private String ActionQuery="actionQuery";
	private String SmsSetting="smsSetting";
	private String Progrma="program";
	private String CDR="cdr";
	private String LimitSetting="limitSetting";
	private String DVRS="DVRS";
	private String currentMonth="currentMonth";
	private String currentDay="currentDay";
	private String cardChangeHistory="cardChangeHistory";
	private String numberChangeHistory="numberChangeHistory";
	private String queryQos="queryQos";
	private String smsContentSetting="smsContentSetting";
	private String smsThresholdSetting="smsThresholdSetting";
	private String sendSMS="sendSMS";
	private String volumePocketSetting="volumePocketSetting";
	
	Map<String, Object> session;
	
	LogoutControl logoutControl=new LogoutControl();
	
	@Authority(action="admin", privilege="admin")  
	public String admin(){return Admin;}  
	public String DVRS(){return DVRS;}  
	public String bill(){return BILL;}  
	public String dataRate(){return DataRate;}  
	public String smsQuery(){return SmsQuery;}  
	public String actionQuery(){return ActionQuery;}  	
	public String smsSetting(){return SmsSetting;}  	
	public String smsContentSetting(){return smsContentSetting;} 
	public String smsThresholdSetting(){return smsThresholdSetting;} 
	public String program(){return Progrma;}  
	public String cdr(){return CDR;} 
	public String limitSetting(){return LimitSetting;} 
	public String currentMonth(){return currentMonth;} 
	public String currentDay(){return currentDay;} 
	public String cardChangeHistory(){return cardChangeHistory;}
	public String numberChangeHistory(){return numberChangeHistory;}
	public String sendSMS(){return sendSMS;}
	public String volumePocketSetting(){return volumePocketSetting;}
	public String queryQos(){return queryQos;}
	public String logout()  
	{  
		ActionContext ac = ActionContext.getContext();
		Map session = ac.getSession();
		
		logoutControl.execute(session);
		setTag("您已登出");
		return LOGIN;         
	}
//*****************************************
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}

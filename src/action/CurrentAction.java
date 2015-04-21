package action;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import bean.CurrentDay;
import bean.CurrentMonth;
import control.CurrentControl;

public class CurrentAction extends BaseAction {

	public CurrentAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String imsi;
	String from;
	String to;
	String suspend;
	
	CurrentControl currentControl=new CurrentControl();
	
	
	public String queryCurrentMonth(){
		
		try {
			System.out.println("imsi:"+imsi+",from:"+from+",to:"+to+",suspend:"+suspend+","+new Date());
			List<CurrentMonth> list = currentControl.queryCurrentMonth(imsi,from.replace("-",""),to.replace("-",""),suspend);
			result = beanToJSONArray(list);
			actionLogControl.loggerAction(super.getUser().getAccount(), "Current", "queryMonth","from:"+from+" to:"+to+" IMSI:"+imsi, SUCCESS);
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return SUCCESS;
	}
	
	public String queryCurrentDay(){
		
		try {
			System.out.println("imsi:"+imsi+",from:"+from+",to:"+to);
			List<CurrentDay> list = currentControl.queryCurrentDay(imsi,from.replace("-",""),to.replace("-",""));
			
			result = beanToJSONArray(list);
		
			actionLogControl.loggerAction(super.getUser().getAccount(), "Current", "queryDay","from:"+from+" to:"+to+" IMSI:"+imsi, SUCCESS);
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return SUCCESS;
	}
	
	/********************************************/

	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSuspend() {
		return suspend;
	}

	public void setSuspend(String suspend) {
		this.suspend = suspend;
	}
	
	
	
}

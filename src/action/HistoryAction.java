package action;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import control.HistoryControl;

public class HistoryAction extends BaseAction {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public HistoryAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	HistoryControl  historyControl = new HistoryControl();

	String imsi;
	public String queryCardChangeHistory(){
		
		try {
			result = beanToJSONArray(historyControl.queryCardChangeHistory(imsi));
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
	
	public String queryNumberChangeHistory(){
		
		try {
			result = beanToJSONArray(historyControl.queryNumberChangeHistory(imsi));
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

	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}
	
	
}

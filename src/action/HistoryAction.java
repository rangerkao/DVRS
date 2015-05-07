package action;

import java.io.PrintWriter;
import java.io.StringWriter;
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
			result = makeResult(historyControl.queryCardChangeHistory(imsi),null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "LimitSetting", "queryTWNMSISDN","", SUCCESS);
			
		} catch (SQLException e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		
		return SUCCESS;
	}
	
	public String queryNumberChangeHistory(){
		
		try {
			result = makeResult(historyControl.queryNumberChangeHistory(imsi),null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "LimitSetting", "queryTWNMSISDN","", SUCCESS);
		} catch (SQLException e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		
		return SUCCESS;
	}
//*****************************************//
	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}
	
	
}

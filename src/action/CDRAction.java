package action;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import bean.CDR;
import control.CDRControl;

public class CDRAction extends BaseAction {

	public CDRAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String from;
	String to;
	List<CDR> CDRlist =new ArrayList<CDR>();
	
	private CDRControl cdrControl =new CDRControl();
	
	public String queryCDR() throws SQLException, ParseException{
		System.out.println("from:"+from+" to:"+to);
		if(from==null||"".equals(from)||to==null||"".endsWith(to)){
				CDRlist=cdrControl.queryCDR();
		}else{
				CDRlist=cdrControl.queryCDR(tool.DateFormat(from, "yyyy-MM-dd"), tool.DateFormat(to, "yyyy-MM-dd"));
		}
		
		result=beanToJSONArray(CDRlist);
		actionLogControl.loggerAction(super.getUser().getAccount(), "CDR", "query","", result);
		return SUCCESS;
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
	
	
}

package action;

import java.io.PrintWriter;
import java.io.StringWriter;
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
	String IMSI;
	List<CDR> CDRlist =new ArrayList<CDR>();
	
	private CDRControl cdrControl =new CDRControl();
	
	public String queryCDR(){
		try {
			System.out.println("from:"+from+" to:"+to+" IMSI:"+IMSI);
			CDRlist=cdrControl.queryCDR(from,to,IMSI);

			result=makeResult(CDRlist, null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "CDR", "query","from:"+from+" to:"+to+" IMSI:"+IMSI, SUCCESS);
		} catch (SQLException  e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		} catch (ParseException e) {
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
//***********************************************************************//
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

	public String getIMSI() {
		return IMSI;
	}

	public void setIMSI(String iMSI) {
		IMSI = iMSI;
	}
	
	
}

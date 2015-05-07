package action;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import control.QosControl;

public class QosAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public QosAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	String imsi;
	String msisdn;
	
	QosControl qosControl = new QosControl();
	
	public String queryQos(){
		
		try {
			result = makeResult(qosControl.queryQos(imsi, msisdn),null);
			
			actionLogControl.loggerAction(super.getUser().getAccount(), "Qos", "query","IMSI:"+imsi+",msisdn:"+msisdn, SUCCESS);
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

	
	//********************************************************//
	
	public String getImsi() {
		return imsi;
	}


	public void setImsi(String imsi) {
		this.imsi = imsi;
	}


	public String getMsisdn() {
		return msisdn;
	}


	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	
}

package action;

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
			result = beanToJSONArray(qosControl.queryQos(imsi, msisdn));
			
			actionLogControl.loggerAction(super.getUser().getAccount(), "Qos", "query","IMSI:"+imsi+",msisdn:"+msisdn, SUCCESS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return SUCCESS;
	}

	
	//-----------------------------------------------------------------
	
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

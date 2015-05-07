package action;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import bean.DataRate;
import control.DataRateControl;

public class DataRateAction extends BaseAction{

	public DataRateAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	List<DataRate> dataRateList=new ArrayList<DataRate>();
	DataRateControl dataRateControl = new DataRateControl();
	
	public String queryDataRate(){
		try {
			dataRateList=dataRateControl.queryDataRateList();
			result=makeResult(dataRateList,null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "DataRate", "query","", SUCCESS);
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
}

package action;

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
			result=beanToJSONArray(dataRateList);
			actionLogControl.loggerAction(super.getUser().getAccount(), "DataRate", "query","", SUCCESS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		return SUCCESS;
	}
}

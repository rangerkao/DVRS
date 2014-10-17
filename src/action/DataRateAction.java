package action;

import java.util.ArrayList;
import java.util.List;

import bean.DataRate;

import control.DataRateControl;

public class DataRateAction extends BaseAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	List<DataRate> dataRateList=new ArrayList<DataRate>();
	DataRateControl dataRateControl = new DataRateControl();
	
	public String queryDataRate(){
		dataRateList=dataRateControl.queryDataRateList();
		result=beanToJSONArray(dataRateList);
		return SUCCESS;
	}
}

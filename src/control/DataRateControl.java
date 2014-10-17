package control;

import java.util.List;

import bean.DataRate;
import dao.DataRateDao;

public class DataRateControl extends BaseControl{
	
	private DataRateDao dataRateDao = new DataRateDao();
	
	public List<DataRate> queryDataRateList(){
		return dataRateDao.queryDataRateList();
	}

}

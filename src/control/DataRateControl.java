package control;

import java.sql.SQLException;
import java.util.List;

import bean.DataRate;
import dao.DataRateDao;

public class DataRateControl extends BaseControl{
	
	private DataRateDao dataRateDao = new DataRateDao();
	
	
	
	public DataRateControl() throws Exception {
		super();
	}

	public List<DataRate> queryDataRateList() throws SQLException, ClassNotFoundException{
		List<DataRate> r = dataRateDao.queryDataRateList();
		return r;
	}

}

package control;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import dao.HistoryDao;

public class HistoryControl extends BaseControl {

	public HistoryControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	HistoryDao hisatoryDao = new HistoryDao();
	
	public List<Map<String,String>> queryCardChangeHistory(String imsi) throws SQLException {
		List<Map<String,String>> r = hisatoryDao.queryCardChangeHistory(imsi);
		hisatoryDao.closeConnection();
		return r;
	}
	
	public List<Map<String,String>> queryNumberChangeHistory(String imsi) throws SQLException{
		List<Map<String,String>> r = hisatoryDao.queryNumberChangeHistory(imsi);
		hisatoryDao.closeConnection();
		return r;
	}

}

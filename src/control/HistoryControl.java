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
	
	public List<Map<String,String>> queryCardChangeHistory() throws SQLException {
		return hisatoryDao.queryCardChangeHistory();
	}
	
	public List<Map<String,String>> queryNumberChangeHistory() throws SQLException{
		return hisatoryDao.queryNumberChangeHistory();
	}

}

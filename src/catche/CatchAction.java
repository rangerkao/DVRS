package catche;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import dao.CurrentDao;
import action.BaseAction;

public class CatchAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	static Map<String,String> serviceIDtoIMSI = new HashMap<String,String>();
	static Map<String,String> imsitoServiceID = new HashMap<String,String>();
	
	
	CurrentDao currentDao = new CurrentDao();

	public CatchAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public String flushCatche(){
		serviceIDtoIMSI.clear();
		imsitoServiceID.clear();
		
		return SUCCESS;
	}
	
	public String reloadCatche(){
		flushCatche();
		try {
			imsitoServiceID = currentDao.getIMSItoServiceID();
			serviceIDtoIMSI = currentDao.getServiceIDtoIMSI();
			
		} catch (SQLException e) {

			e.printStackTrace();
			result = e.getMessage();
		}

		return SUCCESS;
	}

	public static Map<String, String> getImsitoServiceID() {
		return imsitoServiceID;
	}

	public static Map<String, String> getServiceIDtoIMSI() {
		return serviceIDtoIMSI;
	}

}

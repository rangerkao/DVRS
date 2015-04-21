package control;

import java.sql.SQLException;
import java.util.List;

import dao.QosDao;
import bean.QosBean;

public class QosControl extends BaseControl {

	public QosControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	QosDao qosDao = new QosDao();
	
	public List<QosBean> queryQos(String imsi,String msisdn) throws SQLException{
		if((imsi==null||"".equals(imsi)) && (msisdn==null||"".equals(msisdn)))
				return qosDao.queryQosList();
		else
			return qosDao.queryQosList(imsi, msisdn);
	}

}

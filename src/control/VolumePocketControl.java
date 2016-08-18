package control;

import java.sql.SQLException;
import java.util.List;

import bean.VolumePocket;
import dao.VolumePocketDao;

public class VolumePocketControl extends BaseControl {

	public VolumePocketControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	VolumePocketDao volumePocketDao = new VolumePocketDao();
	SMSControl sMSControl = new SMSControl();
	
	public List<VolumePocket> queryVolumePocketList() throws SQLException{
		
		
		return volumePocketDao.queryVolumePocketList();
	}
	
	public List<VolumePocket> queryVolumePocketList(String chtMsisdn) throws Exception{
		
		return volumePocketDao.queryVolumePocketList(chtMsisdn);
	}
	
	public List<VolumePocket> inserVolumePocket(VolumePocket v) throws Exception{
		String serviceid = volumePocketDao.queryServiceidByTwnMsisdn(v.getChtMsisdn());
		if(serviceid == null)
			throw new Exception("請確認是否有申請環球卡");
		
		if(!volumePocketDao.ckeckVolumePocket(v))
			throw new Exception("The date range error.");
		
		v.setServiceid(serviceid);
		List<VolumePocket> list = volumePocketDao.inserVolumePocket(v);
		sMSControl.sendSMS("703", v.getChtMsisdn(), null, "VP");
		return list;
	}
	
	public boolean ckeckVolumePocket(VolumePocket v) throws SQLException{
		return volumePocketDao.ckeckVolumePocket(v);
	}
	
	public List<VolumePocket> cancelVolumePocket(VolumePocket v) throws Exception{		
		return volumePocketDao.cancelVolumePocket(v);
	}
	
	public List<VolumePocket> updateVolumePocket(VolumePocket v) throws Exception{
		if(!volumePocketDao.ckeckVolumePocket(v))
			throw new Exception("The date range error.");
		
		List<VolumePocket> list = volumePocketDao.updateVolumePocket(v);
		sMSControl.sendSMS("704", v.getChtMsisdn(), null, "VP");
		return list;
	}
}

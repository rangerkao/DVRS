package control;

import java.io.UnsupportedEncodingException;
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
	
	public List<VolumePocket> queryVolumePocketList() throws SQLException, UnsupportedEncodingException{
		
		
		return volumePocketDao.queryVolumePocketList();
	}
	
	public List<VolumePocket> queryVolumePocketList(String chtMsisdn) throws Exception{
		
		return volumePocketDao.queryVolumePocketList(chtMsisdn);
	}
	
	public String checkCustomer(String chtMaiadn) throws Exception{
		String serviceid = volumePocketDao.queryServiceidByTwnMsisdn(chtMaiadn);
		if(serviceid == null)
			return "請確認此客戶是否為環球卡用戶";
		return "此客戶為環球卡用戶" ;
	}
	
	public List<VolumePocket> inserVolumePocket(VolumePocket v) throws Exception{
		String serviceid = volumePocketDao.queryServiceidByTwnMsisdn(v.getChtMsisdn());
		
		if(serviceid == null)
			throw new Exception("請確認此客戶是否為環球卡用戶");
		
		if(!volumePocketDao.ckeckVolumePocket(v))
			throw new Exception("The date range error.");
		
		v.setServiceid(serviceid);
		v.setIMSI(volumePocketDao.queryIMSIByServiceID(serviceid));
		List<VolumePocket> list = volumePocketDao.inserVolumePocket(v);
		sMSControl.sendSMS("703", v.getChtMsisdn(), null, "VP",
				new String[]{"{{date_start}}","{{date_end}}"},
				new String[]{v.getStartDate().substring(4,6)+"/"+v.getStartDate().substring(6,8),v.getEndDate().substring(4,6)+"/"+v.getEndDate().substring(6,8)});
		return list;
	}
	
	public boolean ckeckVolumePocket(VolumePocket v) throws SQLException{
		return volumePocketDao.ckeckVolumePocket(v);
	}
	
	public List<VolumePocket> cancelVolumePocket(VolumePocket v) throws Exception{
		List<VolumePocket> list = volumePocketDao.cancelVolumePocket(v);
		sMSControl.sendSMS("707", v.getChtMsisdn(), null, "VP",
				new String[]{"{{date_start}}","{{date_end}}"},
				new String[]{v.getStartDate().substring(4,6)+"/"+v.getStartDate().substring(6,8),v.getEndDate().substring(4,6)+"/"+v.getEndDate().substring(6,8)});
		
		return list;
	}
	
	public List<VolumePocket> updateVolumePocket(VolumePocket v) throws Exception{
		if(!volumePocketDao.ckeckVolumePocket(v))
			throw new Exception("The date range error.");
		
		List<VolumePocket> list = volumePocketDao.updateVolumePocket(v);
		sMSControl.sendSMS("704", v.getChtMsisdn(), null, "VP",
				new String[]{"{{date_start}}","{{date_end}}"},
				new String[]{v.getStartDate().substring(4,6)+"/"+v.getStartDate().substring(6,8),v.getEndDate().substring(4,6)+"/"+v.getEndDate().substring(6,8)});

		return list;
	}
}

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
	
	public List<VolumePocket> queryVolumePocketList() throws SQLException, UnsupportedEncodingException, ClassNotFoundException{
		List<VolumePocket> r = volumePocketDao.queryVolumePocketList();
		
		return r;
	}
	
	public List<VolumePocket> queryVolumePocketList(String chtMsisdn) throws Exception{
		
		List<VolumePocket> r = volumePocketDao.queryVolumePocketList(chtMsisdn);
		
		return r;
	}
	
	public String checkCustomer(String chtMaiadn) throws Exception{
		String serviceid = volumePocketDao.queryServiceidByTwnMsisdn(chtMaiadn);
		String r = null;
		if(serviceid == null)
			r = "請確認此客戶是否為環球卡用戶";
		else
			r = "此客戶為環球卡用戶" ;
		
		return r;
	}
	
	public List<VolumePocket> inserVolumePocket(VolumePocket v) throws Exception{
		String serviceid = volumePocketDao.queryServiceidByTwnMsisdn(v.getChtMsisdn());
		
		if(serviceid == null){
			
			throw new Exception("請確認此客戶是否為環球卡用戶");
		}
		
		if(!volumePocketDao.ckeckVolumePocket(v)){
			
			throw new Exception("The date range error.");
		}
			
		
		v.setServiceid(serviceid);
		v.setIMSI(volumePocketDao.queryIMSIByServiceID(serviceid));
		List<VolumePocket> r = volumePocketDao.inserVolumePocket(v);
		sMSControl.sendSMS("703", v.getChtMsisdn(), null, "VP",
				new String[]{"{{date_start}}","{{date_end}}"},
				new String[]{v.getStartDate().substring(4,6)+"/"+v.getStartDate().substring(6,8),v.getEndDate().substring(4,6)+"/"+v.getEndDate().substring(6,8)});
		return r;
	}
	
	public boolean ckeckVolumePocket(VolumePocket v) throws SQLException, ClassNotFoundException{
		boolean r = volumePocketDao.ckeckVolumePocket(v);
		return r;
	}
	
	public List<VolumePocket> cancelVolumePocket(VolumePocket v) throws Exception{
		List<VolumePocket> r = volumePocketDao.cancelVolumePocket(v);
		sMSControl.sendSMS("707", v.getChtMsisdn(), null, "VP",
				new String[]{"{{date_start}}","{{date_end}}"},
				new String[]{v.getStartDate().substring(4,6)+"/"+v.getStartDate().substring(6,8),v.getEndDate().substring(4,6)+"/"+v.getEndDate().substring(6,8)});
		return r;
	}
	
	public List<VolumePocket> updateVolumePocket(VolumePocket v) throws Exception{
		if(!volumePocketDao.ckeckVolumePocket(v)){
			
			throw new Exception("The date range error.");
		}
		
		List<VolumePocket> r = volumePocketDao.updateVolumePocket(v);
		sMSControl.sendSMS("704", v.getChtMsisdn(), null, "VP",
				new String[]{"{{date_start}}","{{date_end}}"},
				new String[]{v.getStartDate().substring(4,6)+"/"+v.getStartDate().substring(6,8),v.getEndDate().substring(4,6)+"/"+v.getEndDate().substring(6,8)});

		return r;
	}
}

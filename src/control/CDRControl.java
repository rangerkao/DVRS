package control;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import bean.CDR;
import dao.CDRDao;

public class CDRControl extends BaseControl {

	private CDRDao cdrDao=new CDRDao();

	public CDRControl() throws Exception {
		super();
	}

	public List<CDR> queryCDR() throws SQLException, ParseException{
		
		List<CDR> r = cdrDao.queryCDR();
		cdrDao.closeConnection();
		return r;
	}
	
	public List<CDR> queryCDR(String from,String to,String IMSI) throws SQLException, ParseException{
		
		List<CDR> r = null;
		if(from==null && to==null && IMSI ==null){
			r = queryCDR();
		}else
			r = cdrDao.queryCDR(from,to,IMSI);
		cdrDao.closeConnection();
		return r;
	}
}

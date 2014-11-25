package control;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import bean.CDR;
import dao.CDRDao;

public class CDRControl extends BaseControl {

	private CDRDao cdrDao=new CDRDao();

	public CDRControl() throws Exception {
		super();
	}

	public List<CDR> queryCDR() throws SQLException, ParseException{
		return cdrDao.queryCDR();
	}
	
	public List<CDR> queryCDR(String from,String to,String IMSI) throws SQLException, ParseException{
		if(from==null && to==null && IMSI ==null){
			return queryCDR();
		}
		return cdrDao.queryCDR(from,to,IMSI);
	}
}

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
	
	public List<CDR> queryCDR(Date from,Date to) throws SQLException, ParseException{
		return cdrDao.queryCDR(from,to);
	}
}

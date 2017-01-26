package control;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import dao.CurrentDao;
import bean.CurrentDay;
import bean.CurrentMonth;

public class CurrentControl extends BaseControl {

	CurrentDao currentDao = new CurrentDao();
	
	public CurrentControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	
	public List<CurrentMonth> queryCurrentMonth() throws SQLException, ClassNotFoundException{
		
		List<CurrentMonth> r = currentDao.queryCurrentMonth();
		
		return r;
	}
	
	public List<CurrentMonth> queryCurrentMonth(String imsi,String from,String to,String suspend) throws SQLException, ClassNotFoundException{
		List<CurrentMonth> r = null;
		if((imsi==null || "".equals(imsi))&&(from==null || "".equals(from))&&(to==null || "".equals(to))&&("".equals(suspend)||suspend==null))
			r = currentDao.queryCurrentMonth();
		else
			r = currentDao.queryCurrentMonth(imsi,from,to,suspend);
		
		return r;
	}
	
	public List<CurrentDay> queryCurrentDay() throws SQLException, ClassNotFoundException{
		List<CurrentDay> r = currentDao.queryCurrentDay();
		
		return r;
	}
	
	public List<CurrentDay> queryCurrentDay(String imsi,String from,String to) throws SQLException, ClassNotFoundException{
		List<CurrentDay> r = null;
		if((imsi==null || "".equals(imsi))&&(from==null || "".equals(from))&&(to==null || "".equals(to)))
			r = currentDao.queryCurrentDay();
		else
			r = currentDao.queryCurrentDay(imsi,from,to);
		
		return r;
	}
	
}

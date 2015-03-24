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

	
	public List<CurrentMonth> queryCurrentMonth() throws SQLException{
		return currentDao.queryCurrentMonth();
	}
	
	public List<CurrentMonth> queryCurrentMonth(String imsi,String from,String to,String suspend) throws SQLException{
		System.out.println("ctr queryCurrentMonth..."+","+new Date());
		if((imsi==null || "".equals(imsi))&&(from==null || "".equals(from))&&(to==null || "".equals(to))&&("".equals(suspend)||suspend==null))
			return currentDao.queryCurrentMonth();
		
		return currentDao.queryCurrentMonth(imsi,from,to,suspend);
	}
	
	public List<CurrentDay> queryCurrentDay() throws SQLException{
		return currentDao.queryCurrentDay();
	}
	
	public List<CurrentDay> queryCurrentDay(String imsi,String from,String to) throws SQLException{
		if((imsi==null || "".equals(imsi))&&(from==null || "".equals(from))&&(to==null || "".equals(to)))
			return currentDao.queryCurrentDay();
		
		return currentDao.queryCurrentDay(imsi,from,to);
	}
	
}

package control;

import java.sql.SQLException;
import java.util.List;

import bean.TabOutData;
import dao.TapOutDao;

public class TapOutControl extends BaseControl {
	
	
	public TapOutControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	TapOutDao tapOutDao = new TapOutDao();
	
	public List<TabOutData> queryTapOutData(String from ,String to ,String phonenumber,String type) throws Exception{
		return tapOutDao.queryTapOutData(from, to, phonenumber, type);
	}
 
}

package control;

import java.sql.SQLException;
import java.util.List;

import dao.AdminDao;
import bean.Admin;

public class AdminControl extends BaseControl{
	
	public AdminControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	private AdminDao adminDao=new AdminDao();
	
	public List<Admin> queryAdminList() throws SQLException{
		List<Admin> r = adminDao.queryAdminList();
		adminDao.closeConnection();
		return r;
	}
	
	public int addAdmin(Admin admin) throws SQLException{
		int r = adminDao.insert(admin);
		adminDao.closeConnection();
		return r;
	}
	
	public int modAdmin(Admin admin) throws SQLException{
		int r = adminDao.update(admin);
		adminDao.closeConnection();
		return r;
	}

	public int delAdmin(Admin admin) throws SQLException{
		int r = adminDao.delete(admin);
		adminDao.closeConnection();
		return r;
	}

}

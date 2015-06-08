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
		return adminDao.queryAdminList();
	}
	
	public int addAdmin(Admin admin) throws SQLException{
		return adminDao.insert(admin);
	}
	
	public int modAdmin(Admin admin) throws SQLException{
		return adminDao.update(admin);
	}

	public int delAdmin(Admin admin) throws SQLException{
		return adminDao.delete(admin);
	}

}

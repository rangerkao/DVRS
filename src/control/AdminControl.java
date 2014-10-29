package control;

import java.util.List;

import dao.AdminDao;
import bean.Admin;

public class AdminControl extends BaseControl{
	
	public AdminControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	private AdminDao adminDao=new AdminDao();
	
	public List<Admin> queryAdminList(){
		return adminDao.queryAdminList();
	}
	
	public int addAdmin(Admin admin){
		return adminDao.insert(admin);
	}
	
	public int modAdmin(Admin admin){
		return adminDao.update(admin);
	}

	public int delAdmin(Admin admin){
		return adminDao.delete(admin);
	}

}

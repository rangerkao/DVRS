package control;

import java.util.Map;

import dao.AdminDao;
import bean.Admin;
import bean.User;

public class LoginControl extends BaseControl{

	private AdminDao adminDao = new AdminDao();
	
	public LoginControl() throws Exception {
		super();
	}


	public String loginC(Map session,String account,String password){
		String msg="";
			Admin admin = adminDao.queryAdminByAccount(account);
			if(admin==null || "".equals(admin.getPassword())){
				msg="Account error or without !";
			}else if(!admin.getPassword().equals(password)){
				msg="PassWord Error !";
			}else{
				msg="success";
				User user=new User(admin.getAccount(),admin.getRole());
				session.put("s2tUser", user);
			}
		return msg;
	}

}

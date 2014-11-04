package action;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;







import org.json.JSONArray;
import org.json.JSONObject;

import bean.Admin;

import com.opensymphony.xwork2.ActionSupport;

import control.AdminControl;

public class AdminAction extends BaseAction{

	public AdminAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	 /**
	  * ajax返回结果，也可是其他类型的，这里以String类型为例
	  */
	 
	 private AdminControl adminControl=new AdminControl();
	 private Admin admin;
	 private String mod;
	
	public String queryAdmin() throws SQLException{
		
		List<Admin> adminList=new ArrayList<Admin>();
		adminList=adminControl.queryAdminList();
		System.out.println( beanToJSONArray(adminList));
		result=beanToJSONArray(adminList);
		actionLogControl.loggerAction(super.getUser().getAccount(), "Admin", "query","", SUCCESS);
		return SUCCESS;
	}
	
	public String updateAdmin() throws SQLException{
		result=SUCCESS;
		System.out.println( beanToJSONObject(admin));
		System.out.println(	"mod:"+mod);
		int r=0;
		if(mod.equalsIgnoreCase("add")){
			if(adminControl.addAdmin(admin)!=1)
				result="Error To add new data!";
		}else if(mod.equalsIgnoreCase("mod")){
			if(adminControl.modAdmin(admin)!=1)
				result="Error To modify data!";
		}else if(mod.equalsIgnoreCase("del")){
			if(adminControl.delAdmin(admin)!=1)
				result="Error To delete data!";
		}
		actionLogControl.loggerAction(super.getUser().getAccount(), "Admin", "update", mod+":"+ beanToJSONObject(admin), result);

		return SUCCESS;

	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(Admin admin) {
		this.admin = admin;
	}

	public String getMod() {
		return mod;
	}

	public void setMod(String mod) {
		this.mod = mod;
	} 

}

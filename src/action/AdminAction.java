package action;

import java.util.ArrayList;
import java.util.List;





import org.json.JSONArray;
import org.json.JSONObject;

import bean.Admin;

import com.opensymphony.xwork2.ActionSupport;

import control.AdminControl;

public class AdminAction extends ActionSupport{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	 /**
	  * ajax返回结果，也可是其他类型的，这里以String类型为例
	  */
	 private String result;
	 private AdminControl adminControl=new AdminControl();
	 private Admin admin;
	 private String mod;
	
	public String queryAdmin(){
		
		List<Admin> adminList=new ArrayList<Admin>();
		adminList=adminControl.queryAdminList();
		System.out.println( beanToJSONArray(adminList));
		result=beanToJSONArray(adminList);
		return SUCCESS;
	}
	
	public String updateAdmin(){
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

		return SUCCESS;

	}
	
	private String beanToJSONArray(List admin){
		JSONArray jo = (JSONArray) JSONObject.wrap(admin);
		return jo.toString();
	}
	private String beanToJSONObject(Object admin){
		JSONObject jo = (JSONObject) JSONObject.wrap(admin);
		return jo.toString();
	}
	
	 /**
	  * 
	 * @Title: getResult 
	 * @Description:json调取结果  
	 * @param @return    
	 * @return String
	 * @throws
	  */
	 public String getResult() {
	  return result;
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

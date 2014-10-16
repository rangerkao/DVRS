package action;

import java.util.ArrayList;
import java.util.List;




import org.json.JSONArray;
import org.json.JSONObject;

import bean.Admin;

import com.opensymphony.xwork2.ActionSupport;

public class AdminAction extends ActionSupport{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	 /**
	  * ajax返回结果，也可是其他类型的，这里以String类型为例
	  */
	 private String result;
	 private List<Admin> adminList=new ArrayList<Admin>();
	
	public String queryAdmin(){
		
		
		Admin admin =new Admin();
		admin.setUserid("userid");
		admin.setAccount("account");
		admin.setPassword("password");
		admin.setRole("role");
		adminList.add(admin);
		System.out.println( beanToJson(adminList));
		result=beanToJson(adminList);
		return SUCCESS;
	}
	
	private String beanToJson(Object admin){
		JSONArray jo = (JSONArray) JSONObject.wrap(admin);
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

	public List<Admin> getAdminList() {
		return adminList;
	}

	public void setAdminList(List<Admin> adminList) {
		this.adminList = adminList;
	}
	 

}

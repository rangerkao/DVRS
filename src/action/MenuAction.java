package action;

import java.util.List;

import bean.Link;
import control.MenuControl;




public class MenuAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String role;
	private MenuControl menuControl = new MenuControl();
	public MenuAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public String queryAuthentication(){
		
		List<Link> auth =menuControl.queryAuthentication(super.getUser().getRole());
		
		result=beanToJSONArray(auth);

		return SUCCESS;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

}

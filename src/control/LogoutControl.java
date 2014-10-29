package control;

import java.util.Map;

public class LogoutControl extends BaseControl{

	public LogoutControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	public String execute(Map session){
		if(session!=null)session.remove("s2tUser");
		return null;
	}
}

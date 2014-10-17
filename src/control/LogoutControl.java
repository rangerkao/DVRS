package control;

import java.util.Map;

public class LogoutControl extends BaseControl{

	public String execute(Map session){
		if(session!=null)session.remove("s2tUser");
		return null;
	}
}

package control;

import java.util.Map;

public class Logout {

	public String execute(Map session){
		if(session!=null)session.remove("s2tUser");
		return null;
	}
}

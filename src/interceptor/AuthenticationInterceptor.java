package interceptor;

import java.util.Map;

import bean.User;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public class AuthenticationInterceptor extends AbstractInterceptor{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		//透過invocation獲得呼叫的context
		Map<String,Object> session=invocation.getInvocationContext().getSession();
		
		User user =(User) session.get("s2tUser");
		
		if(user!=null)
			System.out.println("role="+user.getRold());
		else
			System.out.println("Session is null !");
		
		if(user==null){
			//失敗
			return Action.LOGIN;
		}
		//繼續進行
		return invocation.invoke();
	}
}

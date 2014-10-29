package interceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import bean.Admin;
import bean.User;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public class AuthenticationInterceptor extends AbstractInterceptor{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Map rolePromissionMap = new HashMap();
	Map roleLevelMap = new HashMap();
	
	public AuthenticationInterceptor(){
		
	}
	
	
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		//透過invocation獲得呼叫的context
		//取得session
		Map<String,Object> session=invocation.getInvocationContext().getSession();
		
		//取得Action的name
		String aName=invocation.getAction().getClass().getName();
		System.out.println("ActionInvocation called by "+aName);
		
		//取得方法名字
		String methodName = invocation.getProxy().getMethod();
		System.out.println("methodName ： "+methodName);
		
		//取得現在方法
		Method currentMethod = invocation.getAction().getClass().getMethod(methodName); 
		
		//是否需要驗證
		boolean b=currentMethod.isAnnotationPresent(Authority.class);
		System.out.println("Is need validated "+b);
		
		if(b){
			//取得註解
			Authority authority = currentMethod.getAnnotation(Authority.class);
			//取得註解的action
			String actionName = authority.action();  
			//取得註解需要的權限
			String privilege = authority.privilege(); 
			
			System.out.println("Action "+actionName+" Is need privilege "+privilege);
		}
		
		
		
		
		User user =(User) session.get("s2tUser");
		
		if(user!=null)
			System.out.println("role="+user.getRole());
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

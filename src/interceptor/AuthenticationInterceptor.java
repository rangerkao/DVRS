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
		//�z�Linvocation��o�I�s��context
		//���osession
		Map<String,Object> session=invocation.getInvocationContext().getSession();
		
		//���oAction��name
		String aName=invocation.getAction().getClass().getName();
		System.out.println("ActionInvocation called by "+aName);
		
		//���o��k�W�r
		String methodName = invocation.getProxy().getMethod();
		System.out.println("methodName �G "+methodName);
		
		//���o�{�b��k
		Method currentMethod = invocation.getAction().getClass().getMethod(methodName); 
		
		//�O�_�ݭn����
		boolean b=currentMethod.isAnnotationPresent(Authority.class);
		System.out.println("Is need validated "+b);
		
		if(b){
			//���o����
			Authority authority = currentMethod.getAnnotation(Authority.class);
			//���o���Ѫ�action
			String actionName = authority.action();  
			//���o���ѻݭn���v��
			String privilege = authority.privilege(); 
			
			System.out.println("Action "+actionName+" Is need privilege "+privilege);
		}
		
		
		
		
		User user =(User) session.get("s2tUser");
		
		if(user!=null)
			System.out.println("role="+user.getRole());
		else
			System.out.println("Session is null !");
		
		if(user==null){
			//����
			return Action.LOGIN;
		}
		//�~��i��
		return invocation.invoke();
	}
}

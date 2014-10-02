package action;

import bean.Invoice;

import com.opensymphony.xwork2.ActionSupport;

import control.BillReport;

public class LinkAction extends ActionSupport{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String tag;
	
	public String bill()  
	{  
	   return "bill";        
	}  
	   
	public String logout()  
	{  
		setTag("�A�w�g�n�X�I");
	   return "logout";         
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}

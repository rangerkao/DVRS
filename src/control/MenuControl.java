package control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bean.Link;

public class MenuControl extends BaseControl {

	public MenuControl() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public List<Link> queryAuthentication(String role){
		List<Link> result =new ArrayList<Link>();

		//�h�Ť@ (�̰�)
		List<Link> l1=new ArrayList<Link>();
		l1.add(new Link("adminList","adminLink","�ϥΪ̺޲z"));
		l1.add(new Link("adminList","programLink","�{���޲z"));
		l1.add(new Link("elseList","billLink","�b��ץX"));
		
		//�h�ŤG
		List<Link> l2=new ArrayList<Link>();
		l2.add(new Link("settingList","smsSettingLink","²�T�]�w"));
		l2.add(new Link("settingList","limitSettingLink","ĵ�ܤW���]�w"));

		//�h�ŤT
		List<Link> l3=new ArrayList<Link>();
		l3.add(new Link("searchList","actionQueryLink","�ϥΪ̾ާ@�����d��"));
		l3.add(new Link("searchList","smsQueryLink","²�T�o�e�d��"));
		l3.add(new Link("searchList","dataRateLink","��O�޲z"));
		l3.add(new Link("searchList","cdrLink","CDR�d��"));
		
		l3.add(new Link("elseList","logoutLink","�n�X"));
		
		Map<String,Integer> roleAuth=new HashMap<String,Integer>();
		roleAuth.put("act1", 2);
		roleAuth.put("ranger", 1);
		roleAuth.put("admin", 1);
		
		//�޿�}�l
		
		Integer auth=roleAuth.get(role);
		
		if(auth==null || "".equals(auth))
			auth=3;
		
		
		switch(auth){
		case 1:
			result.addAll(l1);
		case 2:
			result.addAll(l2);
		case 3:
			result.addAll(l3);
		default:

		}
		return result;
	}

}

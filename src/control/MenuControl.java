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

		//層級一 (最高)
		List<Link> l1=new ArrayList<Link>();
		l1.add(new Link("adminList","adminLink","使用者管理"));
		l1.add(new Link("adminList","programLink","程式管理"));
		l1.add(new Link("elseList","billLink","帳單匯出"));
		l1.add(new Link("adminList","cdrLink","CDR查詢"));
		l1.add(new Link("adminList","actionQueryLink","使用者操作紀錄查詢"));
		//l1.add(new Link("adminList","dataRateLink","資費查詢"));
		
		//層級二
		List<Link> l2=new ArrayList<Link>();
		l2.add(new Link("settingList","smsSettingLink","簡訊設定"));
		l2.add(new Link("settingList","limitSettingLink","警示上限設定"));

		//層級三
		List<Link> l3=new ArrayList<Link>();
		
		l3.add(new Link("searchList","smsQueryLink","超量簡訊發送查詢頁面"));
		l3.add(new Link("searchList","dataRateLink","費率查詢"));
		l3.add(new Link("searchList","currentMonthLink","每月累計頁面"));
		l3.add(new Link("searchList","currentDayLink","單日累計查詢"));
		
		l3.add(new Link("elseList","logoutLink","登出"));
		
		Map<String,Integer> roleAuth=new HashMap<String,Integer>();
		roleAuth.put("cs", 2);
		roleAuth.put("act1", 2);
		roleAuth.put("ranger", 1);
		roleAuth.put("admin", 1);
		
		//邏輯開始
		
		Integer auth=roleAuth.get(role);
		
		if(auth==null || "".equals(auth))
			auth=3;
		
		
		switch(auth){
		case 1:
			result.addAll(l1);
		case 2:
			result.addAll(l2);
		default:
			result.addAll(l3);
		}
		return result;
	}

}

package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bean.TabOutData;

public class TapOutDao  extends BaseDao {

	public TapOutDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
		
		if(tadigList.size()==0) {
			queryMccmnc();
		}
	}

	static public Map<String,String> tadigList = new HashMap<String,String>();
	
	public List<TabOutData> queryTapOutData(String from ,String to ,String phonenumber,String type) throws Exception{
		List<TabOutData> list = new ArrayList<TabOutData>();
		
		System.out.println("from:"+from+" to:"+to+" phonenumber:"+phonenumber+ " type:"+type);
		
		
		String condition = ""
				+ "and a.STARTTIME>= to_date('"+from+"','yyyy-mm-dd') "
				+ "and a.STARTTIME<= to_date('"+to+"','yyyy-mm-dd')+1 "
				+  (phonenumber!= null && !"".equals(phonenumber) ? " and a.SERVICEID = "
						+ "(nvl((select serviceid from service where servicecode = '"+phonenumber+"' and datecanceled is null),(select max(serviceid) from service where servicecode = '"+phonenumber+"'))) ":"");
		sql = "";
		
		//voice
		if("all".equalsIgnoreCase(type) || "voice".equalsIgnoreCase(type)) {
			sql+= "select to_char(a.STARTTIME,'yyyy/mm/dd') startDate," + 
					"a.location," + 
					"case a.direction when 'T' then a.caller when 'O' then a.callee else '' end phonenumber," + 
					"case when a.location = 'FWTH' then 'RE' else" + 
					"     case a.direction when 'T' then Case when a.caller like '886%' then 'RC' else 'RA' end " + 
					"                      when 'O' then 'R8' else '' end" + 
					"     end type," + 
					"to_char(a.STARTTIME, 'hh24:mi:ss') startTime," + 
					"to_char(a.STARTTIME+a.duration/86400, 'hh24:mi:ss') endTime," + 
					"a.duration unit," + 
					"round(a.AMOUNT,2) amount  " + 
					"from TAPOUTFILEVOICEUSAGE a " + 
					"where 1=1 " + condition ;
		}
		//簡訊
		if("all".equalsIgnoreCase(type) || "sms".equalsIgnoreCase(type)) {
			sql += (!"".equals(sql)?" UNION ":"");
			sql += "select to_char(a.STARTTIME,'yyyy/mm/dd') startDate," + 
					"a.location," + 
					"case a.direction when 'T' then a.caller when 'O' then a.callee else '' end phonenumber," + 
					"'MJ' type," + 
					"to_char(a.STARTTIME, 'hh24:mi:ss') startTime," + 
					"to_char(a.STARTTIME, 'hh24:mi:ss')  endTime," + 
					"1 unit," + 
					"round(a.AMOUNT,2) amount " + 
					"from TAPOUTFILESMSUSAGE a " + 
					"where 1=1 " + condition ;
			
		}
		//數據
		if("all".equalsIgnoreCase(type) || "data".equalsIgnoreCase(type)) {
			sql += (!"".equals(sql)?" UNION ":"");
			sql += "select to_char(a.STARTTIME,'yyyy/mm/dd') startDate," + 
					"a.location," + 
					"'' phonenumber," + 
					"'PC' type," + 
					"to_char(a.STARTTIME, 'hh24:mi:ss') startTime," + 
					"'' endTime," + 
					"a.chargeunit unit," + 
					"a.AMOUNT amount " + 
					"from TAPOUTFILEDATAUSAGE a " + 
					"where 1=1 " + condition ;
		}

		Connection conn =  getConn1();
		Statement st = conn.createStatement();
		
		
		ResultSet rs = null;
		
		
		String countSql = "select count(1) cd from ("+sql+") ";
		
		System.out.println("SQL:"+countSql);
		rs=st.executeQuery(countSql);
		
		//避免筆數過大造成結果呈現緩慢，設定為2萬筆以下

		rs.next();
		if(rs.getInt("cd")>20000) {
			throw new Exception("請縮小範圍");
		}else if(rs.getInt("cd")==0) {
			return list;
		}
		
		System.out.println("SQL:"+sql);
		rs=st.executeQuery(sql);
		
		while(rs.next()){
			TabOutData c = new TabOutData();
			c.setStartDate(rs.getString("startDate"));
			c.setStartTime(rs.getString("startTime"));
			c.setEndTime(nvl(rs.getString("endTime")," "));
			c.setPhonenumber(nvl(rs.getString("phonenumber")," "));
			
			String location = rs.getString("location");
			c.setLocation((tadigList.get(location)!=null?tadigList.get(location):location));
			c.setType(rs.getString("type"));
			c.setUnit(rs.getString("unit"));
			c.setAmount(rs.getString("amount"));
			list.add(c);
		}
		rs.close();
		st.close();
		conn.close();
			
		return list;
	}
	
	
	public void queryMccmnc() throws SQLException, ClassNotFoundException {
		sql = "select a.TADIG,a.COUNTRY " + 
				"from HUR_MCCMNC a ";
		Connection conn =  getConn1();
		Statement st = conn.createStatement();
		System.out.println("SQL:"+sql);
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			tadigList.put(rs.getString("TADIG"),rs.getString("COUNTRY"));
		}
		rs.close();
		st.close();
		conn.close();
	}
}

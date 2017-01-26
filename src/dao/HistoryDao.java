package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryDao extends BaseDao {

	public HistoryDao() throws Exception {
		super();
	}

	public List<Map<String,String>> queryCardChangeHistory(String imsi) throws SQLException, ClassNotFoundException{
		List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		
		if(imsi!=null &&!"".equals(imsi)){
			imsi = imsi.replace("*", "%");
		}
		
		sql = "SELECT C.SUBSIDIARYID, C.SERVICECODE, C.SERVICEID, C.STATUS, C.PRICEPLANID, "
				+ "A.COMPLETEDATE, A.OLDVALUE, A.NEWVALUE "
				+ "FROM SERVICEINFOCHANGEORDER A, SERVICEORDER B, SERVICE C "
				+ "WHERE A.FIELDID=3713 AND A.ORDERID=B.ORDERID "
				+ "AND B.SERVICEID=C.SERVICEID "
				+ "AND a.oldvalue <> a.newvalue "
				//20150506 add
				+ (imsi!=null &&!"".equals(imsi) ? "AND (A.OLDVALUE LIKE '"+imsi+"' OR A.NEWVALUE LIKE '"+imsi+"' ) " :"")
				+ "ORDER BY A.COMPLETEDATE DESC ";
		
		Connection conn =  getConn1();
		Statement st = conn.createStatement();
		
		ResultSet rs = st.executeQuery(sql);		
		while(rs.next()){
			
			//20150506 mod
			/*String oldvalue = rs.getString("OLDVALUE");
			String newvalue = rs.getString("NEWVALUE");
			if(imsi!=null &&!"^$".equals(imsi)){
				if((oldvalue==null&&!newvalue.matches(imsi)) || (!oldvalue.matches(imsi)&& newvalue==null)||(!oldvalue.matches(imsi)&&!newvalue.matches(imsi)) )
					continue;
			}*/
			
			Map<String,String> map = new HashMap<String,String>();
			map.put("oldvalue", rs.getString("OLDVALUE"));
			map.put("newvalue", rs.getString("NEWVALUE"));
			map.put("ststus", rs.getString("STATUS"));
			map.put("completedate", rs.getString("COMPLETEDATE"));
			result.add(map);
		}
		rs.close();
		st.close();
		conn.close();
		
		return result;
	}
	
	public List<Map<String,String>> queryNumberChangeHistory(String imsi) throws SQLException, ClassNotFoundException{
		List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		
		if(imsi!=null &&!"".equals(imsi)){
			imsi = imsi.replace("*", "%");
		}
		
		sql = "SELECT A.ORDERID, A.OLDVALUE, A.NEWVALUE, A.COMPLETEDATE, C.SERVICEID, C.SERVICECODE, C.STATUS "
				+ "FROM SERVICEPARAMVALUECHANGEORDER A, SERVICEORDER B, SERVICE C "
				+ "WHERE A.ORDERID=B.ORDERID AND B.SERVICEID=C.SERVICEID "
				+ "AND A.PARAMETERVALUEID=3792 AND C.SUBSIDIARYID=59 "
				//20150506 add
				+ (imsi!=null &&!"".equals(imsi) ? "AND (A.OLDVALUE LIKE '"+imsi+"' OR A.NEWVALUE LIKE '"+imsi+"' ) " :"")
				+ "ORDER BY A.ORDERID DESC ";
		
		Connection conn =  getConn1();
		Statement st = conn.createStatement();
		
		ResultSet rs = st.executeQuery(sql);
		
		while(rs.next()){
			
			//20150506 mod
			/*String oldvalue = rs.getString("OLDVALUE");
			String newvalue = rs.getString("NEWVALUE");
			if(imsi!=null &&!"^$".equals(imsi)){
				if((oldvalue==null&&!newvalue.matches(imsi)) || (!oldvalue.matches(imsi)&& newvalue==null)||(!oldvalue.matches(imsi)&&!newvalue.matches(imsi)) )
					continue;
			}*/
			
			Map<String,String> map = new HashMap<String,String>();
			map.put("orderid", rs.getString("ORDERID"));
			map.put("oldvalue", rs.getString("OLDVALUE"));
			map.put("newvalue", rs.getString("NEWVALUE"));
			map.put("ststus", rs.getString("STATUS"));
			map.put("completedate", rs.getString("COMPLETEDATE"));
			result.add(map);
		}
		rs.close();
		st.close();
		conn.close();
		return result;
	}
	
}

package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class HistoryDao extends BaseDao {

	public HistoryDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(HistoryDao.class);
	}

	public List<Map<String,String>> queryCardChangeHistory(String imsi) throws SQLException{
		List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		
		sql = "SELECT C.SUBSIDIARYID, C.SERVICECODE, C.SERVICEID, C.STATUS, C.PRICEPLANID, "
				+ "A.COMPLETEDATE, A.OLDVALUE, A.NEWVALUE "
				+ "FROM SERVICEINFOCHANGEORDER A, SERVICEORDER B, SERVICE C "
				+ "WHERE A.FIELDID=3713 AND A.ORDERID=B.ORDERID "
				+ "AND B.SERVICEID=C.SERVICEID "
				+ "AND a.oldvalue <> a.newvalue "
				+ "ORDER BY A.COMPLETEDATE DESC ";
		
		Statement st = conn.createStatement();
		
		ResultSet rs = st.executeQuery(sql);
		logger.info("Execute SQL : "+sql);
		
		if(imsi!=null &&!"".equals(imsi)){
			imsi = imsi.replace("/", "");
		}
		
		while(rs.next()){
			
			String oldvalue = rs.getString("OLDVALUE");
			String newvalue = rs.getString("NEWVALUE");
			if(imsi!=null &&!"^$".equals(imsi)){
				if((oldvalue==null&&!newvalue.matches(imsi)) || (!oldvalue.matches(imsi)&& newvalue==null)||(!oldvalue.matches(imsi)&&!newvalue.matches(imsi)) )
					continue;
			}
			
			Map<String,String> map = new HashMap<String,String>();
			map.put("oldvalue", rs.getString("OLDVALUE"));
			map.put("newvalue", rs.getString("NEWVALUE"));
			map.put("ststus", rs.getString("STATUS"));
			map.put("completedate", rs.getString("COMPLETEDATE"));
			result.add(map);
		}
		return result;
	}
	
	public List<Map<String,String>> queryNumberChangeHistory(String imsi) throws SQLException{
		List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		
		sql = "SELECT A.ORDERID, A.OLDVALUE, A.NEWVALUE, A.COMPLETEDATE, C.SERVICEID, C.SERVICECODE, C.STATUS "
				+ "FROM SERVICEPARAMVALUECHANGEORDER A, SERVICEORDER B, SERVICE C "
				+ "WHERE A.ORDERID=B.ORDERID AND B.SERVICEID=C.SERVICEID "
				+ "AND A.PARAMETERVALUEID=3792 AND C.SUBSIDIARYID=59 "
				+ "ORDER BY A.ORDERID DESC ";
		
		Statement st = conn.createStatement();
		
		ResultSet rs = st.executeQuery(sql);
		logger.info("Execute SQL : "+sql);
		
		if(imsi!=null &&!"".equals(imsi)){
			imsi = imsi.replace("/", "");
		}
		while(rs.next()){
			
			String oldvalue = rs.getString("OLDVALUE");
			String newvalue = rs.getString("NEWVALUE");
			if(imsi!=null &&!"^$".equals(imsi)){
				if((oldvalue==null&&!newvalue.matches(imsi)) || (!oldvalue.matches(imsi)&& newvalue==null)||(!oldvalue.matches(imsi)&&!newvalue.matches(imsi)) )
					continue;
			}
			
			Map<String,String> map = new HashMap<String,String>();
			map.put("orderid", rs.getString("ORDERID"));
			map.put("oldvalue", rs.getString("OLDVALUE"));
			map.put("newvalue", rs.getString("NEWVALUE"));
			map.put("ststus", rs.getString("STATUS"));
			map.put("completedate", rs.getString("COMPLETEDATE"));
			result.add(map);
		}
		return result;
	}
	
}

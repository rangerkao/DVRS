package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;




import java.util.Map;
import java.util.Set;

import cache.CacheAction;
import bean.CurrentDay;
import bean.CurrentMonth;

public class CurrentDao extends BaseDao {

	public CurrentDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	Map<String,String> serviceIDtoIMSI = new HashMap<String,String>();
	Map<String,String> imsitoServiceID = new HashMap<String,String>();
	
	
	public List<CurrentMonth> queryCurrentMonth() throws SQLException{
		
		imsitoServiceID = CacheAction.getImsitoServiceID();
		serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		if(imsitoServiceID.size()==0){
			CacheAction.reloadServiceIDwithIMSIMappingCache();
			imsitoServiceID = CacheAction.getImsitoServiceID();
			serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		}
		
		sql=
				"SELECT A.MONTH,A.SERVICEID,A.CHARGE,A.VOLUME,A.SMS_TIMES,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME,A.EVER_SUSPEND,A.LAST_FILEID "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT A "
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
		List<CurrentMonth> list = new ArrayList<CurrentMonth>();
		
		
			Statement st = conn.createStatement();
			
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				
				String imsi = serviceIDtoIMSI.get(rs.getString("SERVICEID"));
				if(imsi==null || "".equals(imsi))
					imsi=rs.getString("SERVICEID");
				
				CurrentMonth c = new CurrentMonth();
				c.setMonth(rs.getString("MONTH"));
				c.setImsi(imsi);
				c.setCharge(tool.FormatDouble(rs.getDouble("CHARGE"), "0.0000"));
				c.setVolume(rs.getDouble("VOLUME"));
				c.setSmsTimes(rs.getInt("SMS_TIMES"));
				c.setLastAlertThreshold(rs.getDouble("LAST_ALERN_THRESHOLD"));
				c.setLastAlertVolume(rs.getDouble("LAST_ALERN_VOLUME"));
				c.setEverSuspend(("0".equals(rs.getString("EVER_SUSPEND"))?false:true));
				c.setLastFileId(rs.getInt("LAST_FILEID"));
				c.setLastDataTime(rs.getString("LAST_DATA_TIME"));
				c.setUpdateDate(rs.getString("UPDATE_DATE"));
				c.setCreateDate(rs.getString("CREATE_DATE"));
				
				list.add(c);
			}
			rs.close();
			st.close();
			closeConnection();
		return list;
		
	}
	
	public List<CurrentMonth> queryCurrentMonth(String imsi,String from,String to,String suspend) throws SQLException{
		System.out.println("dao queryCurrentMonth..."+","+new Date());
		imsitoServiceID = CacheAction.getImsitoServiceID();
		serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		if(imsitoServiceID.size()==0){
			CacheAction.reloadServiceIDwithIMSIMappingCache();
			imsitoServiceID = CacheAction.getImsitoServiceID();
			serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		}
		
		List<CurrentMonth> list = new ArrayList<CurrentMonth>();
		Set<String> filterServiceID = new HashSet<String>();
		String sServiceID = "";
		
		if(imsi!=null &&!"".equals(imsi)){
			imsi = imsi.replace("/", "");
		}
		
		if(!"^$".equals(imsi)){
			for(String s : imsitoServiceID.keySet()){
				if(s.matches(imsi)){
					filterServiceID.add(imsitoServiceID.get(s));
					sServiceID += ","+imsitoServiceID.get(s);
				}
			}
			if(!"".equals(sServiceID))
				sServiceID="("+sServiceID.substring(1)+")";
			else
				sServiceID="('')";
			
			
		}
		

		
		sql=
				"SELECT A.MONTH,A.SERVICEID,A.CHARGE,A.VOLUME,A.SMS_TIMES,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME,A.EVER_SUSPEND,A.LAST_FILEID "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT A where 1=1 "
				+ (from!=null &&!"".equals(from)?"AND A.MONTH >=  "+from+" ":"")
				+ (to!=null &&!"".equals(to)?"AND A.MONTH <=  "+to+" ":"")
				+ (suspend!=null && !"".equals(suspend)?"AND A.EVER_SUSPEND="+suspend+" ":"")
				+ (sServiceID!=null && !"".equals(sServiceID)? "AND A.SERVICEID in "+sServiceID+" ":"")
				+ "ORDER BY A.LAST_DATA_TIME DESC ";

		
			Statement pst = conn.createStatement();
			ResultSet rs=pst.executeQuery(sql);
			System.out.println("query execute : "+new Date()+sql);

			while(rs.next()){
				
				String serviceid = rs.getString("SERVICEID");
				
				/*if(!"^$".equals(imsi) && (serviceid ==null || !filterServiceID.contains(serviceid)))
					continue;*/
				
				String rimsi = serviceIDtoIMSI.get(serviceid);
				if(rimsi==null || "".equals(rimsi))
					rimsi=rs.getString("SERVICEID");
				
				CurrentMonth c = new CurrentMonth();
				c.setMonth(rs.getString("MONTH"));
				c.setImsi(rimsi);
				c.setCharge(tool.FormatDouble(rs.getDouble("CHARGE"), "0.0000"));
				c.setVolume(rs.getDouble("VOLUME"));
				c.setSmsTimes(rs.getInt("SMS_TIMES"));
				c.setLastAlertThreshold(rs.getDouble("LAST_ALERN_THRESHOLD"));
				c.setLastAlertVolume(rs.getDouble("LAST_ALERN_VOLUME"));
				c.setEverSuspend(("0".equals(rs.getString("EVER_SUSPEND"))?false:true));
				c.setLastFileId(rs.getInt("LAST_FILEID"));
				c.setLastDataTime(rs.getString("LAST_DATA_TIME"));
				c.setUpdateDate(rs.getString("UPDATE_DATE"));
				c.setCreateDate(rs.getString("CREATE_DATE"));
				
				list.add(c);
			}
			System.out.println("query parse end!"+new Date());
			rs.close();
			pst.close();
			closeConnection();
		return list;
		
	}
	
	public List<CurrentDay> queryCurrentDay() throws SQLException{
		imsitoServiceID = CacheAction.getImsitoServiceID();
		serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		if(imsitoServiceID.size()==0){
			CacheAction.reloadServiceIDwithIMSIMappingCache();
			imsitoServiceID = CacheAction.getImsitoServiceID();
			serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		}
		
		sql=
				/*"SELECT A.DAY,B.NETWORK||'('||B.COUNTRY||')' MCCMNC,A.SERVICEID,A.CHARGE,A.VOLUME,A.ALERT,A.LAST_FILEID  "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT_DAY A, HUR_MCCMNC B "
				+ "WHERE A.MCCMNC=B.MCCMNC "
				+ "ORDER BY A.LAST_DATA_TIME DESC ";*/
				//20150327
				
				"SELECT A.DAY,SUBSTR(MCCMNC,4)||'('||(case when B.NAME is not null then  B.NAME else substr(A.MCCMNC,0,3) end)||')' MCCMNC,A.SERVICEID,A.CHARGE,A.VOLUME,A.ALERT,A.LAST_FILEID "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT_DAY A,HUR_CUSTOMER_SERVICE_PHONE B "
				+ "where substr(A.MCCMNC,0,3) = B.CODE(+) "
				+ "ORDER BY A.DAY,A.LAST_DATA_TIME DESC ";
		
		List<CurrentDay> list = new ArrayList<CurrentDay>();
		
		
			Statement st = conn.createStatement();
			
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				String imsi = serviceIDtoIMSI.get(rs.getString("SERVICEID"));
				if(imsi==null || "".equals(imsi))
					imsi=rs.getString("SERVICEID");
				CurrentDay c = new CurrentDay();
				c.setDay(rs.getString("DAY"));
				c.setMccmnc(rs.getString("MCCMNC"));
				c.setImsi(imsi);
				c.setCharge(tool.FormatDouble(rs.getDouble("CHARGE"), "0.0000"));
				c.setVolume(rs.getDouble("VOLUME"));
				c.setAlert(("0".equals(rs.getString("ALERT"))?false:true));
				c.setLastFileId(rs.getInt("LAST_FILEID"));
				c.setLastDataTime(rs.getString("LAST_DATA_TIME"));
				c.setUpdateDate(rs.getString("UPDATE_DATE"));
				c.setCreateDate(rs.getString("CREATE_DATE"));
				
				list.add(c);
			}
			rs.close();
			st.close();
			closeConnection();
		return list;
		
	}
	
	public List<CurrentDay> queryCurrentDay(String imsi,String from,String to) throws SQLException{
		imsitoServiceID = CacheAction.getImsitoServiceID();
		serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		if(imsitoServiceID.size()==0){
			CacheAction.reloadServiceIDwithIMSIMappingCache();
			imsitoServiceID = CacheAction.getImsitoServiceID();
			serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		}
		
		List<CurrentDay> list = new ArrayList<CurrentDay>();
		Set<String> filterServiceID = new HashSet<String>();
		String sServiceid = "";
		
		if(imsi!=null &&!"".equals(imsi)){
			imsi = imsi.replace("/", "");
		}
		
		if(!"^$".equals(imsi)){
			for(String s : imsitoServiceID.keySet()){
				if(s.matches(imsi)){
					filterServiceID.add(imsitoServiceID.get(s));
					sServiceid+=","+imsitoServiceID.get(s);
				}
			}	
			if(!"".equals(sServiceid))
				sServiceid="("+sServiceid.substring(1)+")";
			else
				sServiceid="('')";
		}

		sql=
				"SELECT A.DAY,SUBSTR(MCCMNC,4)||'('||(case when B.NAME is not null then  B.NAME else substr(A.MCCMNC,0,3) end)||')' MCCMNC,A.SERVICEID,A.CHARGE,A.VOLUME,A.ALERT,A.LAST_FILEID  "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT_DAY A, HUR_CUSTOMER_SERVICE_PHONE B "
				+ "where substr(A.MCCMNC,0,3) = B.CODE(+) "
				+ (from!=null &&!"".equals(from)?"AND A.DAY >=  "+from+" ":"")
				+ (to!=null &&!"".equals(to)?"AND A.DAY <=  "+to+" ":"")
				//20150505 add
				+ (sServiceid!=null && !"".equals(sServiceid)?"AND A.SERVICEID in "+sServiceid+" " : "")
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
			System.out.println("Sql:"+sql);
		
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);

			while(rs.next()){
				
				String serviceid = rs.getString("SERVICEID");
				
				//20150505 modifi
				/*if(!"^$".equals(imsi) && (serviceid ==null || !filterServiceID.contains(serviceid)))
					continue;*/
				
				String rimsi = serviceIDtoIMSI.get(serviceid);
				if(rimsi==null || "".equals(rimsi))
					rimsi=rs.getString("SERVICEID");

				CurrentDay c = new CurrentDay();
				c.setDay(rs.getString("DAY"));
				c.setMccmnc(rs.getString("MCCMNC"));
				c.setImsi(rimsi);
				c.setCharge(tool.FormatDouble(rs.getDouble("CHARGE"), "0.0000"));
				c.setVolume(rs.getDouble("VOLUME"));
				c.setAlert(("0".equals(rs.getString("ALERT"))?false:true));
				c.setLastFileId(rs.getInt("LAST_FILEID"));
				c.setLastDataTime(rs.getString("LAST_DATA_TIME"));
				c.setUpdateDate(rs.getString("UPDATE_DATE"));
				c.setCreateDate(rs.getString("CREATE_DATE"));
				list.add(c);
			}
			rs.close();
			st.close();
			closeConnection();
		return list;
		
	}
	

}

package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;




import java.util.Map;

import bean.CurrentDay;
import bean.CurrentMonth;

public class CurrentDao extends BaseDao {

	public CurrentDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	Map<String,String> serviceIDtoIMSI = new HashMap<String,String>();
	Map<String,String> imsitoServiceID = new HashMap<String,String>();
	
	private void setServiceIDtoIMSI() throws SQLException{
		/*sql =
				"SELECT B.IMSI,A.SERVICEID "
				+ "FROM SERVICE A,IMSI B,PARAMETERVALUE C "
				+ "WHERE A.SERVICEID=B.SERVICEID AND A.SERVICECODE IS NOT NULL "
				+ "AND B.SERVICEID=C.SERVICEID(+) AND C.PARAMETERVALUEID(+)=3748 "
				+ "UNION "
				+ "SELECT A.NEWVALUE IMSI, C.SERVICEID "
				+ "FROM SERVICEINFOCHANGEORDER A, SERVICEORDER B, SERVICE C ,"
				+ "		("
				+ "			SELECT max(A.COMPLETEDATE) COMPLETEDATE, A.NEWVALUE,count(1) "
				+ "			FROM SERVICEINFOCHANGEORDER A "
				+ "			WHERE A.FIELDID=3713  AND A.COMPLETEDATE IS NOT NULL "
				+ "	        AND A.OLDVALUE <> A.NEWVALUE "
				+ "			GROUP BY A.NEWVALUE ) D "
				+ "WHERE A.FIELDID=3713 AND A.ORDERID=B.ORDERID "
				+ "AND B.SERVICEID=C.SERVICEID "
				+ "AND A.OLDVALUE <> A.NEWVALUE "
				+ "AND D.COMPLETEDATE=A.COMPLETEDATE "
				+ "AND A.NEWVALUE=D.NEWVALUE "
				+ "AND A.NEWVALUE IN ( SELECT A.IMSI FROM IMSI A WHERE A.SERVICEID IS NULL)";*/
		
		sql =
				"SELECT A.SERVICEID,A.FIELDVALUE IMSI "
				+ "FROM( "
				+ "		SELECT A.SERVICEID,B.NEWVALUE FIELDVALUE,A.COMPLETEDATE "
				+ "		FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
				+ "		WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
				+ "		UNION "
				+ "		SELECT A.SERVICEID,B.FIELDVALUE,A.COMPLETEDATE "
				+ "		FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
				+ "		WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713 )A, "
				
				+ "    (SELECT SERVICEID,MAX(COMPLETEDATE) COMPLETEDATE "
				+ "		FROM( "
				+ "			SELECT A.SERVICEID,B.NEWVALUE,A.COMPLETEDATE "
				+ "			FROM SERVICEORDER A,SERVICEINFOCHANGEORDER B "
				+ "			WHERE A.ORDERID =B.ORDERID AND  B.FIELDID=3713 "
				+ "	     	UNION "
				+ "        	SELECT A.SERVICEID,B.FIELDVALUE,A.COMPLETEDATE "
				+ "         FROM SERVICEORDER A,NEWSERVICEORDERINFO B "
				+ "         WHERE A.ORDERID =B.ORDERID AND   B.FIELDID=3713) "
				+ "	  	GROUP BY SERVICEID )B "
				+ "WHERE A.SERVICEID=B.SERVICEID AND A.COMPLETEDATE =B.COMPLETEDATE ";
		
		Statement st = conn2.createStatement();
		
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			serviceIDtoIMSI.put(rs.getString("SERVICEID"), rs.getString("IMSI"));
			imsitoServiceID.put(rs.getString("IMSI"), rs.getString("SERVICEID"));
		}
		rs.close();
		st.close();
	}
	
	
	public List<CurrentMonth> queryCurrentMonth() throws SQLException{
		
		setServiceIDtoIMSI();
		
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
			closeConnect();
		return list;
		
	}
	
	public List<CurrentMonth> queryCurrentMonth(String imsi,String from,String to) throws SQLException{
		
		setServiceIDtoIMSI();
		String serviceid = null;
		if(imsi!=null &&!"".equals(imsi))
			serviceid=imsitoServiceID.get(imsi);
		sql=
				"SELECT A.MONTH,A.SERVICEID,A.CHARGE,A.VOLUME,A.SMS_TIMES,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME,A.EVER_SUSPEND,A.LAST_FILEID "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT A where 1=1 "
				+ (serviceid!=null &&!"".equals(serviceid)?"AND A.SERVICEID =  "+serviceid+" ":"")
				+ (from!=null &&!"".equals(from)?"AND A.MONTH >=  "+from+" ":"")
				+ (to!=null &&!"".equals(to)?"AND A.MONTH <=  "+to+" ":"")
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
		List<CurrentMonth> list = new ArrayList<CurrentMonth>();
		
		
			Statement pst = conn.createStatement();
			
			ResultSet rs=pst.executeQuery(sql);
			
			while(rs.next()){
				imsi = serviceIDtoIMSI.get(rs.getString("SERVICEID"));
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
			pst.close();
			closeConnect();
		return list;
		
	}
	
	public List<CurrentDay> queryCurrentDay() throws SQLException{
		setServiceIDtoIMSI();
		
		sql=
				"SELECT A.DAY,B.NETWORK||'('||B.COUNTRY||')' MCCMNC,A.SERVICEID,A.CHARGE,A.VOLUME,A.ALERT,A.LAST_FILEID  "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT_DAY A, HUR_MCCMNC B "
				+ "WHERE A.MCCMNC=B.MCCMNC "
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
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
			closeConnect();
		return list;
		
	}
	
	public List<CurrentDay> queryCurrentDay(String imsi,String from,String to) throws SQLException{
		setServiceIDtoIMSI();
		String serviceid = null;
		if(imsi!=null &&!"".equals(imsi))
			serviceid=imsitoServiceID.get(imsi);
		sql=
				"SELECT A.DAY,B.NETWORK||'('||B.COUNTRY||')' MCCMNC,A.SERVICEID,A.CHARGE,A.VOLUME,A.ALERT,A.LAST_FILEID  "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT_DAY A, HUR_MCCMNC B "
				+ "WHERE A.MCCMNC=B.MCCMNC "
				+ (serviceid!=null &&!"".equals(serviceid)?"AND A.SERVICEID =  "+serviceid+" ":"")
				+ (from!=null &&!"".equals(from)?"AND A.DAY >=  "+from+" ":"")
				+ (to!=null &&!"".equals(to)?"AND A.DAY <=  "+to+" ":"")
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
			System.out.println("Sql:"+sql);
				
		
			List<CurrentDay> list = new ArrayList<CurrentDay>();
		
		
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				imsi = serviceIDtoIMSI.get(rs.getString("SERVICEID"));
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
			closeConnect();
		return list;
		
	}
	

}

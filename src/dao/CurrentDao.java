package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;




import bean.CurrentDay;
import bean.CurrentMonth;

public class CurrentDao extends BaseDao {

	public CurrentDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public List<CurrentMonth> queryCurrentMonth() throws SQLException{
		
		sql=
				"SELECT A.MONTH,A.IMSI,A.CHARGE,A.VOLUME,A.SMS_TIMES,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME,A.EVER_SUSPEND,A.LAST_FILEID "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT A "
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
		List<CurrentMonth> list = new ArrayList<CurrentMonth>();
		
		
			Statement st = conn.createStatement();
			
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				CurrentMonth c = new CurrentMonth();
				c.setMonth(rs.getString("MONTH"));
				c.setImsi(rs.getString("IMSI"));
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
	
	public List<CurrentMonth> queryCurrentMonth(String imsi) throws SQLException{
		
		sql=
				"SELECT A.MONTH,A.IMSI,A.CHARGE,A.VOLUME,A.SMS_TIMES,A.LAST_ALERN_THRESHOLD,A.LAST_ALERN_VOLUME,A.EVER_SUSPEND,A.LAST_FILEID "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT A "
				+ "WHERE A.IMSI = ? "
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
		List<CurrentMonth> list = new ArrayList<CurrentMonth>();
		
		
			PreparedStatement pst = conn.prepareStatement(sql);
			
			pst.setString(1, imsi);
			
			ResultSet rs=pst.executeQuery();
			
			while(rs.next()){
				CurrentMonth c = new CurrentMonth();
				c.setMonth(rs.getString("MONTH"));
				c.setImsi(rs.getString("IMSI"));
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
		
		sql=
				"SELECT A.DAY,B.NETWORK||'('||B.COUNTRY||')' MCCMNC,A.IMSI,A.CHARGE,A.VOLUME,A.ALERT,A.LAST_FILEID  "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT_DAY A, HUR_MCCMNC B "
				+ "WHERE A.MCCMNC=B.MCCMNC "
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
		List<CurrentDay> list = new ArrayList<CurrentDay>();
		
		
			Statement st = conn.createStatement();
			
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				CurrentDay c = new CurrentDay();
				c.setDay(rs.getString("DAY"));
				c.setMccmnc(rs.getString("MCCMNC"));
				c.setImsi(rs.getString("IMSI"));
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
		
		sql=
				"SELECT A.DAY,B.NETWORK||'('||B.COUNTRY||')' MCCMNC,A.IMSI,A.CHARGE,A.VOLUME,A.ALERT,A.LAST_FILEID  "
				+ ",TO_CHAR(A.LAST_DATA_TIME,'yyyy/MM/dd hh24:mi:ss') LAST_DATA_TIME,TO_CHAR(A.UPDATE_DATE,'yyyy/MM/dd hh24:mi:ss') UPDATE_DATE,TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd hh24:mi:ss') CREATE_DATE "
				+ "FROM HUR_CURRENT_DAY A, HUR_MCCMNC B "
				+ "WHERE A.MCCMNC=B.MCCMNC "
				+ (imsi!=null &&!"".equals(imsi)?"AND A.IMSI =  "+imsi+" ":"")
				+ (from!=null &&!"".equals(from)?"AND A.DAY >=  "+from+" ":"")
				+ (to!=null &&!"".equals(to)?"AND A.DAY <=  "+to+" ":"")
				+ "ORDER BY A.LAST_DATA_TIME DESC ";
		
			System.out.println("Sql:"+sql);
				
		
			List<CurrentDay> list = new ArrayList<CurrentDay>();
		
		
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				CurrentDay c = new CurrentDay();
				c.setDay(rs.getString("DAY"));
				c.setMccmnc(rs.getString("MCCMNC"));
				c.setImsi(rs.getString("IMSI"));
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

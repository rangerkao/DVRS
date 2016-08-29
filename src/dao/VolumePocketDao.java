package dao;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import bean.VolumePocket;

public class VolumePocketDao extends BaseDao {

	public VolumePocketDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	
	public String queryServiceidByTwnMsisdn(String chtMsisdn) throws SQLException{
		String result = null;
		
		sql=
				"select A.SERVICEID "
				+ "from FOLLOWMEDATA A "
				+ "WHERE A.FOLLOWMENUMBER = '"+chtMsisdn+"'";
		
		
		Statement st = conn.createStatement();
		System.out.println("Execute SQL :"+sql);
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			result = rs.getString("SERVICEID");
		}
		st.close();
		rs.close();
		return result;
	}
	
	public String queryIMSIByServiceID(String ServiceID) throws SQLException{
		String result = null;
		
		sql=
				"select A.IMSI "
				+ "from IMSI A "
				+ "WHERE A.SERVICEID = '"+ServiceID+"'";
		
		
		Statement st = conn.createStatement();
		System.out.println("Execute SQL :"+sql);
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			result= rs.getString("IMSI");
		}
		st.close();
		rs.close();
		return result;
	}
	
	
	public List<VolumePocket> queryVolumePocketList() throws SQLException, UnsupportedEncodingException{
		List<VolumePocket> result = new ArrayList<VolumePocket>();
		
		sql=
				"SELECT A.PID,B.FOLLOWMENUMBER CHTMSISDN,A.SERVICEID,A.MCC,A.ALERTED,A.ID,A.CALLER_NAME,A.CUSTOMER_NAME,A.PHONE_TYPE,A.EMAIL,A.CANCEL_REASON, "
				+ "A.START_DATE,A.END_DATE,"
				+ "TO_CHAR(A.CREATE_TIME,'yyyy/MM/dd hh24:mi:ss') CREATE_TIME,TO_CHAR(A.CANCEL_TIME,'yyyy/MM/dd hh24:mi:ss') CANCEL_TIME "
				+ "from HUR_VOLUME_POCKET A,FOLLOWMEDATA B "
				+ "WHERE A.SERVICEID = B.SERVICEID AND A.TYPE=0 "
				+ "ORDER BY A.START_DATE DESC ";
		
		
		Statement st = conn.createStatement();
		System.out.println("Execute SQL :"+sql);
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			VolumePocket v = new VolumePocket();
			v.setPid(rs.getString("PID"));
			
			v.setServiceid(rs.getString("SERVICEID"));
			v.setMcc(rs.getString("MCC"));
			v.setStartDate(rs.getString("START_DATE"));
			v.setEndDate(rs.getString("END_DATE"));
			v.setAlerted(rs.getString("ALERTED"));
			v.setCreateTime(rs.getString("CREATE_TIME"));
			v.setCancelTime(nvl(rs.getString("CANCEL_TIME"),""));
			
			v.setId(nvl(rs.getString("ID"),""));
			v.setPhoneType(nvl(rs.getString("PHONE_TYPE"),""));
			v.setCallerName(convertString(rs.getString("CALLER_NAME"),"ISO-8859-1","Big5"));
			v.setCustomerName(convertString(rs.getString("CUSTOMER_NAME"),"ISO-8859-1","Big5"));
			v.setEmail(nvl(rs.getString("EMAIL"),""));
			
			v.setChtMsisdn(rs.getString("CHTMSISDN"));
			
			v.setReason(convertString(rs.getString("CANCEL_REASON"),"ISO-8859-1","Big5"));
			result.add(v);
		}
		st.close();
		rs.close();
		return result;
	}
	
	public List<VolumePocket> queryVolumePocketList(String chtMsisdn) throws SQLException, UnsupportedEncodingException{
		List<VolumePocket> result = new ArrayList<VolumePocket>();
		
		sql=
				"SELECT A.PID,B.FOLLOWMENUMBER CHTMSISDN,A.SERVICEID,A.MCC,A.ALERTED,A.ID,A.CALLER_NAME,A.CUSTOMER_NAME,A.PHONE_TYPE,A.EMAIL,A.CANCEL_REASON, "
						+ "A.START_DATE,A.END_DATE,"
						+ "TO_CHAR(A.CREATE_TIME,'yyyy/MM/dd hh24:mi:ss') CREATE_TIME,TO_CHAR(A.CANCEL_TIME,'yyyy/MM/dd hh24:mi:ss') CANCEL_TIME "
						+ "from HUR_VOLUME_POCKET A,FOLLOWMEDATA B "
						+ "WHERE A.SERVICEID = B.SERVICEID AND A.TYPE=0 AND B.FOLLOWMENUMBER='"+chtMsisdn+"' "
						+ "ORDER BY A.START_DATE DESC";		
		
		Statement st = conn.createStatement();
		System.out.println("Execute SQL :"+sql);
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			VolumePocket v = new VolumePocket();
			v.setPid(rs.getString("PID"));
			
			v.setServiceid(rs.getString("SERVICEID"));
			v.setMcc(rs.getString("MCC"));
			v.setStartDate(rs.getString("START_DATE"));
			v.setEndDate(rs.getString("END_DATE"));
			v.setAlerted(rs.getString("ALERTED"));
			v.setCreateTime(rs.getString("CREATE_TIME"));
			v.setCancelTime(nvl(rs.getString("CANCEL_TIME"),""));
			
			v.setId(nvl(rs.getString("ID"),""));
			v.setPhoneType(nvl(rs.getString("PHONE_TYPE"),""));
			v.setCallerName(convertString(rs.getString("CALLER_NAME"),"ISO-8859-1","Big5"));
			v.setCustomerName(convertString(rs.getString("CUSTOMER_NAME"),"ISO-8859-1","Big5"));
			v.setEmail(nvl(rs.getString("EMAIL"),""));
			
			v.setChtMsisdn(rs.getString("CHTMSISDN"));
			v.setReason(convertString(rs.getString("CANCEL_REASON"),"ISO-8859-1","Big5"));
			
			result.add(v);
		}
		st.close();
		rs.close();
		return result;
	}
	
	public List<VolumePocket> inserVolumePocket(VolumePocket v) throws SQLException, UnsupportedEncodingException{
		
		sql = 
				"SELECT NVL(MAX(PID),0)+1 PID "
				+ "FROM HUR_VOLUME_POCKET ";
		
		Statement st = conn.createStatement();
		
		ResultSet rs = st.executeQuery(sql);
		
		while(rs.next()){
			v.setPid(rs.getString("PID"));
		}
		
		sql=
				"INSERT into HUR_VOLUME_POCKET(PID,SERVICEID,START_DATE,END_DATE,CREATE_TIME,ID,CALLER_NAME,CUSTOMER_NAME,PHONE_TYPE,EMAIL,TYPE,IMSI) "
				+ "VALUES('"+v.getPid()+"','"+v.getServiceid()+"','"+v.getStartDate()+"','"+v.getEndDate()+"',sysdate,'"
						+ ""+v.getId()+"','"+convertString(v.getCallerName(),"Big5","ISO-8859-1")+"','"+convertString(v.getCustomerName(),"Big5","ISO-8859-1")+"','"+v.getPhoneType()+"','"+v.getEmail()+"',0,'"+v.getIMSI()+"')";
		
		
		System.out.println("Execute SQL :"+sql);
		st.executeUpdate(sql);

		rs.close();
		st.close();

		return queryVolumePocketList(v.getChtMsisdn());
	}
	
	public boolean ckeckVolumePocket(VolumePocket v) throws SQLException{
		boolean result = false;
		sql=
				"SELECT count(1) c "
				+ "FROM HUR_VOLUME_POCKET A,FOLLOWMEDATA B "
				+ "WHERE A.SERVICEID = B.SERVICEID AND A.CANCEL_TIME IS NULL AND A.TYPE=0 "
				+ "AND B.FOLLOWMENUMBER = '"+v.getChtMsisdn()+"' "
				+ "AND ('"+v.getStartDate()+"' between A.START_DATE AND A.END_DATE "
				+ "		OR '"+v.getEndDate()+"' between A.START_DATE AND A.END_DATE) ";
		if(v.getPid()!=null){
			sql += "AND PID != '"+v.getPid()+"' ";
		}
		
		Statement st = conn.createStatement();
		System.out.println("Execute SQL :"+sql);
		ResultSet rs = st.executeQuery(sql);
	
		while(rs.next()){
			if(0 == rs.getInt("c"))
				result= true;
		}
		
		
		rs.close();
		st.close();
		
		return result;
	}
	
	public List<VolumePocket> updateVolumePocket(VolumePocket v) throws SQLException, UnsupportedEncodingException{
		
		sql=	
				"UPDATE HUR_VOLUME_POCKET SET START_DATE='"+v.getStartDate()+"',END_DATE='"+v.getEndDate()+"' "
				//暫時只允許修改日期
						//+ ",ID='"+v.getId()+"',NAME='"+v.getName()+"',PHONE_TYPE='"+v.getPhoneType()+"',EMAIL='"+v.getEmail()+"' "
						+ "WHERE PID = '"+v.getPid()+"' ";
		
		Statement st = conn.createStatement();
		System.out.println("Execute SQL :"+sql);
		st.executeUpdate(sql);

		st.close();

		return queryVolumePocketList(v.getChtMsisdn());
	}
	
	public List<VolumePocket> cancelVolumePocket(VolumePocket v) throws SQLException, UnsupportedEncodingException{
		
		sql=
				"UPDATE HUR_VOLUME_POCKET A SET A.CANCEL_TIME = sysdate,A.CANCEL_REASON='"+convertString(v.getReason(),"BIG5","ISO-8859-1")+"' WHERE A.PID = '"+v.getPid()+"' ";
		
		Statement st = conn.createStatement();
		System.out.println("Execute SQL :"+sql);
		st.executeUpdate(sql);

		st.close();

		return queryVolumePocketList(v.getChtMsisdn());
	}
	
}

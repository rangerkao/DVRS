package dao;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bean.ActionLog;

public class ActionLogDao extends BaseDao{

	
	public ActionLogDao() throws Exception {
		super();
	}

	public List<ActionLog> queryActionLog(Date fromDate,Date toDate) throws SQLException, UnsupportedEncodingException{
		
		if((fromDate==null||"".equals(fromDate))&&(toDate==null||"".equals(toDate)))
			return queryActionLog();
		
		List<ActionLog> list =new ArrayList<ActionLog>();
		sql=
				"SELECT A.ID,A.USERID,A.PAGE,A.ACTION,A.PARAMETER,to_char(A.create_date,'yyyy/MM/dd HH:mi:ss') CREATEDATE,A.RESULT "
				+ "FROM HUR_ACTION_LOG A "
				+ "WHERE A.CREATE_DATE >= ?  AND A.CREATE_DATE <= ? +1 "
				+ "ORDER BY A.CREATE_DATE DESC";
		
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setDate(1, convertJaveUtilDate_To_JavaSqlDate(fromDate) );
			pst.setDate(2, convertJaveUtilDate_To_JavaSqlDate(toDate));
			ResultSet rs=pst.executeQuery();
			
			while(rs.next()){
				ActionLog log = new ActionLog();
				log.setId(rs.getString("ID"));
				log.setAccount(rs.getString("USERID"));
				log.setPage(rs.getString("PAGE"));
				log.setAction(rs.getString("ACTION"));
				log.setParameter((rs.getString("PARAMETER")!=null? new String(rs.getString("PARAMETER").getBytes("ISO8859-1"),"BIG5"):""));
				log.setCreateDate(rs.getString("CREATEDATE"));
				log.setResult(rs.getString("RESULT"));
				
				if(log.getParameter()==null)
					log.setParameter("");
				
				list.add(log);
			}
			
			rs.close();
			pst.close();
		return list;

	}
	
	public java.sql.Date convertJaveUtilDate_To_JavaSqlDate(java.util.Date date) {
		
		return new java.sql.Date(date.getTime());
	}
	
	public List<ActionLog> queryActionLog() throws SQLException, UnsupportedEncodingException{
		List<ActionLog> list =new ArrayList<ActionLog>();
		sql=
				"SELECT A.ID,A.USERID,A.PAGE,A.ACTION,A.PARAMETER,to_char(A.create_date,'yyyy/MM/dd HH:mi:ss') CREATEDATE,A.RESULT "
				+ "FROM HUR_ACTION_LOG A "
				+ "ORDER BY A.CREATE_DATE DESC ";
		
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				ActionLog log = new ActionLog();
				log.setId(rs.getString("ID"));
				log.setAccount(rs.getString("USERID"));
				log.setPage(rs.getString("PAGE"));
				log.setAction(rs.getString("ACTION"));
				log.setParameter((rs.getString("PARAMETER")!=null? new String(rs.getString("PARAMETER").getBytes("ISO8859-1"),"BIG5"):""));
				log.setCreateDate(rs.getString("CREATEDATE"));
				log.setResult(rs.getString("RESULT"));
				
				
				if(log.getParameter()==null)
					log.setParameter("");
				
				list.add(log);
			}
			
			rs.close();
			st.close();
		return list;
	}
	
	public int loggerAction(String userid,String page,String action,String parameter,String result) throws Exception{
		
		
		sql="INSERT INTO HUR_ACTION_LOG "
				+ "(ID,USERID,PAGE,ACTION,PARAMETER,RESULT,CREATE_DATE) "
				+ "VALUES(HUR_MANERGE_ID.NEXTVAL,?,?,?,?,?,SYSDATE)";
		
		int aResult=0;
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, userid);
			pst.setString(2, page);
			pst.setString(3, action);
			pst.setString(4, (parameter!=null? new String(parameter.getBytes("BIG5"),"ISO8859-1"):""));
			pst.setString(5, result);
			aResult= pst.executeUpdate();
			
			pst.close();
			closeConnection();
			
		 return aResult;
	}
	
}

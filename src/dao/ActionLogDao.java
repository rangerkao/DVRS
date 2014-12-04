package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bean.ActionLog;
import bean.SMSLog;

public class ActionLogDao extends BaseDao{

	
	public ActionLogDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	public List<ActionLog> queryActionLog(Date fromDate,Date toDate) throws SQLException{
		
		if((fromDate==null||"".equals(fromDate))&&(toDate==null||"".equals(toDate)))
			return queryActionLog();
		
		List<ActionLog> list =new ArrayList<ActionLog>();
		sql=
				"SELECT A.ID,A.USERID,A.PAGE,A.ACTION,A.PARAMETER,to_char(A.create_date,'yyyy/MM/dd HH:mi:ss') CREATEDATE,A.RESULT "
				+ "FROM HUR_ACTION_LOG A "
				+ "WHERE A.CREATE_DATE >= ?  AND A.CREATE_DATE <= ? +1 "
				+ "ORDER BY A.CREATE_DATE DESC";
		
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setDate(1, tool.convertJaveUtilDate_To_JavaSqlDate(fromDate) );
			pst.setDate(2, tool.convertJaveUtilDate_To_JavaSqlDate(toDate));
			ResultSet rs=pst.executeQuery();
			
			while(rs.next()){
				ActionLog log = new ActionLog();
				log.setId(rs.getString("ID"));
				log.setAccount(rs.getString("USERID"));
				log.setPage(rs.getString("PAGE"));
				log.setAction(rs.getString("ACTION"));
				log.setParameter(rs.getString("PARAMETER"));
				log.setCreateDate(rs.getString("CREATEDATE"));
				log.setResult(rs.getString("RESULT"));
				
				if(log.getParameter()==null)
					log.setParameter("");
				
				list.add(log);
			}
			
			rs.close();
			pst.close();
			
			closeConnect();
			
		return list;

	}
	
	public List<ActionLog> queryActionLog() throws SQLException{
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
				log.setParameter(rs.getString("PARAMETER"));
				log.setCreateDate(rs.getString("CREATEDATE"));
				log.setResult(rs.getString("RESULT"));
				
				
				if(log.getParameter()==null)
					log.setParameter("");
				
				list.add(log);
			}
			
			rs.close();
			st.close();
			
			closeConnect();
		return list;
	}
	
	public int loggerAction(String userid,String page,String action,String parameter,String result) throws Exception{
		
		if(conn==null || conn.isClosed())
			super.connectDB();
		
		
		sql="INSERT INTO HUR_ACTION_LOG "
				+ "(ID,USERID,PAGE,ACTION,PARAMETER,RESULT,CREATE_DATE) "
				+ "VALUES(HUR_MANERGE_ID.NEXTVAL,?,?,?,?,?,SYSDATE)";
		
		int aResult=0;
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, userid);
			pst.setString(2, page);
			pst.setString(3, action);
			pst.setString(4, parameter);
			pst.setString(5, result);
			aResult= pst.executeUpdate();
			
			pst.close();
			closeConnect();
			
		 return aResult;
	}
	
}

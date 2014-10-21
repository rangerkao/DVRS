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

	
	public List<ActionLog> queryActionLog(Date fromDate,Date toDate){
		
		if((fromDate==null||"".equals(fromDate))&&(toDate==null||"".equals(toDate)))
			return queryActionLog();
		
		
		List<ActionLog> list =new ArrayList<ActionLog>();
		sql=
				"SELECT A.ID,A.USERID,A.PAGE,A.ACTION,A.PARAMETER,A.CREATE_DATE "
				+ "FROM HUR_MANERGER_LOG A "
				+ "WHERE A.CREATE_DATE >= ? -1 AND A.CREATE_DATE <= ? "
				+ "ORDER BY A.CREATE_DATE ";
		
		try {
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
				log.setCreateDate(tool.convertJaveSqlDate_To_JavaUtilDate(rs.getDate("CREATE_DATE")));
				list.add(log);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;

	}
	
	public List<ActionLog> queryActionLog(){
		List<ActionLog> list =new ArrayList<ActionLog>();
		sql=
				"SELECT A.ID,A.USERID,A.PAGE,A.ACTION,A.PARAMETER,A.CREATE_DATE "
				+ "FROM HUR_MANERGER_LOG A "
				+ "ORDER BY A.CREATE_DATE ";
		
		try {
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				ActionLog log = new ActionLog();
				log.setId(rs.getString("ID"));
				log.setAccount(rs.getString("USERID"));
				log.setPage(rs.getString("PAGE"));
				log.setAction(rs.getString("ACTION"));
				log.setParameter(rs.getString("PARAMETER"));
				log.setCreateDate(tool.convertJaveSqlDate_To_JavaUtilDate(rs.getDate("CREATE_DATE")));
				list.add(log);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
}

package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bean.SMSLog;

public class SMSDao extends BaseDao{

	
	
	public List<SMSLog> querySMSLog(Date fromDate,Date toDate){
		
		if((fromDate==null||"".equals(fromDate))&&(toDate==null||"".equals(toDate)))
			return querySMSLog();
		
		
		List<SMSLog> list =new ArrayList<SMSLog>();
		sql=
				"SELECT A.ID,A.MSG,A.RESULT,A.SEND_NUMBER,A.SEND_DATE,A.CREATE_DATE "
				+ "FROM HUR_SMS_LOG A "
				+ "WHERE A.SEND_DATE >= ? -1 AND A.SEND_DATE <= ?";
		
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setDate(1, tool.convertJaveUtilDate_To_JavaSqlDate(fromDate) );
			pst.setDate(2, tool.convertJaveUtilDate_To_JavaSqlDate(toDate));
			ResultSet rs=pst.executeQuery();
			
			while(rs.next()){
				SMSLog log = new SMSLog();
				log.setId(rs.getString("ID"));
				log.setMsg(rs.getString("MSG"));
				log.setResult(rs.getString("RESULT"));
				log.setSendNumber(rs.getString("SEND_NUMBER"));
				log.setSendDate(tool.convertJaveSqlDate_To_JavaUtilDate(rs.getDate("SEND_DATE")));
				log.setCreateDate(tool.convertJaveSqlDate_To_JavaUtilDate(rs.getDate("CREATE_DATE")));
				list.add(log);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;

	}
	
	public List<SMSLog> querySMSLog(){
		List<SMSLog> list =new ArrayList<SMSLog>();
		sql=
				"SELECT A.ID,A.MSG,A.RESULT,A.SEND_NUMBER,A.SEND_DATE,A.CREATE_DATE "
				+ "FROM HUR_SMS_LOG A";
		
		try {
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				SMSLog log = new SMSLog();
				log.setId(rs.getString("ID"));
				log.setMsg(rs.getString("MSG"));
				log.setResult(rs.getString("RESULT"));
				log.setSendNumber(rs.getString("SEND_NUMBER"));
				log.setSendDate(tool.convertJaveSqlDate_To_JavaUtilDate(rs.getDate("SEND_DATE")));
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

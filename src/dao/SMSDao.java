package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bean.SMSLog;
import bean.SMSSetting;

public class SMSDao extends BaseDao{

	
	
	public SMSDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	public List<SMSLog> querySMSLog(Date fromDate,Date toDate) throws SQLException{
		
		if((fromDate==null||"".equals(fromDate))&&(toDate==null||"".equals(toDate)))
			return querySMSLog();
		
		
		List<SMSLog> list =new ArrayList<SMSLog>();
		sql=
				"SELECT A.ID,A.MSG,A.RESULT,A.SEND_NUMBER,A.SEND_DATE,A.CREATE_DATE "
				+ "FROM HUR_SMS_LOG A "
				+ "WHERE A.SEND_DATE >= ? -1 AND A.SEND_DATE <= ? "
				+ "ORDER BY A.CREATE_DATE ";
		
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
		return list;

	}
	
	public List<SMSLog> querySMSLog() throws SQLException{
		List<SMSLog> list =new ArrayList<SMSLog>();
		sql=
				"SELECT A.ID,A.MSG,A.RESULT,A.SEND_NUMBER,A.SEND_DATE,A.CREATE_DATE "
				+ "FROM HUR_SMS_LOG A "
				+ "ORDER BY A.CREATE_DATE ";
		
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

		return list;
	}
	
	public List<SMSSetting> querySMSSetting() throws SQLException{
		List<SMSSetting> list =new ArrayList<SMSSetting>();
		sql=
				"SELECT A.ID,A.BRACKET,A.MEG,A.SUSPEND "
				+ "FROM HUR_SMS_SETTING A "
				+ "ORDER BY A.ID ";
		
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				SMSSetting log = new SMSSetting();
				log.setId(rs.getString("ID"));
				log.setBracket(rs.getDouble("BRACKET"));
				log.setMsg(rs.getString("MEG"));
	
				String s=rs.getString("SUSPEND");
				if("0".equals(s))
					log.setSuspend(false);
				else if("1".equals(s))
					log.setSuspend(true);
					
				list.add(log);
			}

		return list;
	}
	
	public List<SMSSetting> updateSMSSetting(List<SMSSetting> list) throws SQLException{
		
			//移除所有資料
			sql=
					"TRUNCATE  TABLE  HUR_SMS_SETTING";
			
			Statement st = conn.createStatement();
			st.execute(sql);
			st.close();
			//重新匯入資料
			sql=
					"INSERT INTO HUR_SMS_SETTING(ID,BRACKET,MEG,SUSPEND) "
					+ "VALUES(?,?,?,?)";
			PreparedStatement pst = conn.prepareStatement(sql);
			for(SMSSetting s : list){
				pst.setString(1, s.getId());
				pst.setDouble(2, s.getBracket());
				pst.setString(3, s.getMsg());
				if(s.getSuspend())
					pst.setString(4, "1");
				else
					pst.setString(4, "0");
				pst.addBatch();
			}
			pst.executeBatch();
			
			pst.close();
			conn.close();
			

		return list;
	}
	
}

package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import program.DVRSmain;
import bean.GPRSThreshold;
import bean.SMSLog;
import bean.SMSSetting;

public class SMSDao extends BaseDao{
	
	public SMSDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(SMSDao.class);
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
			logger.debug("Execute sql: "+sql);
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
			logger.debug("Execute sql: "+sql);
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
			rs.close();
			st.close();
			closeConnect();
		return list;
	}
	
	public List<SMSSetting> querySMSSetting() throws SQLException{
		logger.info("querySMSSetting...");
		List<SMSSetting> list =new ArrayList<SMSSetting>();
		sql=
				"SELECT A.ID,A.BRACKET,A.MEGID,A.SUSPEND "
				+ "FROM HUR_SMS_SETTING A "
				+ "ORDER BY A.ID ";
		
			Statement st = conn.createStatement();
			logger.debug("Execute sql: "+sql);
			ResultSet rs=st.executeQuery(sql);
			while(rs.next()){
				SMSSetting log = new SMSSetting();
				log.setId(rs.getString("ID"));
				log.setBracket(rs.getDouble("BRACKET"));
				log.setMsg(rs.getString("MEGID"));
	
				String s=rs.getString("SUSPEND");
				if("0".equals(s))
					log.setSuspend(false);
				else if("1".equals(s))
					log.setSuspend(true);
					
				list.add(log);
			}
			rs.close();
			st.close();
			closeConnect();
		return list;
	}
	
	public List<SMSSetting> updateSMSSetting(List<SMSSetting> list) throws SQLException{
		logger.info("updateSMSSetting...");
			//移除所有資料
			sql=
					"TRUNCATE  TABLE  HUR_SMS_SETTING";
			
			Statement st = conn.createStatement();
			st.execute(sql);
			st.close();
			//重新匯入資料
			sql=
					"INSERT INTO HUR_SMS_SETTING(ID,BRACKET,MEGID,SUSPEND) "
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
			logger.debug("Execute sql: "+sql);
			pst.executeBatch();
			pst.close();			
			closeConnect();
		return list;
	}
	
	public List<GPRSThreshold> queryAlertLimit() throws SQLException{
		logger.info("queryAlertLimit...");
		List<GPRSThreshold> list =new ArrayList<GPRSThreshold>();
		sql=
				"SELECT A.IMSI,A.THRESHOLD,C.SERVICECODE "
				+ "FROM HUR_GPRS_THRESHOLD A,IMSI B,SERVICE C "
				+ "WHERE A.IMSI=B.IMSI AND B.SERVICEID = c.SERVICEID ";
		
		Statement st = conn.createStatement();
		logger.debug("Execute sql: "+sql);
		ResultSet rs = st.executeQuery(sql);
		while(rs.next()){
			GPRSThreshold g = new GPRSThreshold();
			g.setImsi(rs.getString("IMSI"));
			g.setMsisdn(rs.getString("SERVICECODE"));
			g.setThreshold(rs.getDouble("THRESHOLD"));
			list.add(g);
		}
		
		st.close();
		rs.close();
		closeConnect();
		return list;
	}
	
	public int insertAlertLimit(String imsi,Double limit) throws SQLException{

		logger.info("insertAlertLimit...");

		sql=
				"INSERT INTO HUR_GPRS_THRESHOLD (IMSI,THRESHOLD) "
				+ "VALUES(?,?)";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setString(1, imsi);
		pst.setDouble(2, limit);
		logger.debug("Execute sql: "+sql);
		int result=pst.executeUpdate();
		pst.close();
		closeConnect();
		return result;
	}
	
	public int updateAlertLimit(String imsi,Double limit) throws SQLException{

		logger.info("updateAlertLimit...");
		
		sql=
				"UPDATE HUR_GPRS_THRESHOLD A "
				+ "SET A.THRESHOLD = ? "
				+ "WHERE A.IMSI=? ";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		pst.setDouble(1, limit);
		pst.setString(2, imsi);
		logger.debug("Execute sql: "+sql);
		int result=pst.executeUpdate();
		pst.close();
		closeConnect();
		return result;
	}
	
	public int deleteAlertLimit(String imsi,Double limit) throws SQLException{
		logger.info("deleteAlertLimit...");
		
		sql=
				"DELETE HUR_GPRS_THRESHOLD A "
				+ "WHERE A.IMSI=? ";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setString(1, imsi);
		logger.debug("Execute sql: "+sql);
		int result=pst.executeUpdate();
		pst.close();
		closeConnect();
		return result;
	}
	
	public Map<String,String> queryIMSI(String msisdn) throws SQLException{
		logger.info("queryIMSI...");
		Map<String,String> map =new HashMap<String,String>();
		String imsi = null;
		String pricaplainid = null;
		sql=
				"SELECT B.IMSI,C.SERVICECODE,C.PRICEPLANID "
				+ "FROM IMSI B,SERVICE C "
				+ "WHERE B.SERVICEID = C.SERVICEID "
				+ "AND C.SERVICECODE = ?";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setString(1, msisdn);
		logger.debug("Execute sql: "+sql);
		ResultSet rs = pst.executeQuery();
		while(rs.next()){
			imsi=rs.getString("IMSI");
			pricaplainid=rs.getString("PRICEPLANID");
		}
		rs.close();
		pst.close();
		map.put("imsi", imsi);
		map.put("pricaplainid", pricaplainid);
		closeConnect();
		return map;
	}
	
	public String getSMSContent(int id) throws SQLException{
		
		String result=null;
		
		sql="SELECT A.COMTENT FROM HUR_SMS_COMTENT A WHERE A.ID=? ";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setInt(1, id);
		logger.debug("Execute sql: "+sql);
		ResultSet rs = pst.executeQuery();
		
		while(rs.next()){
			result=rs.getString("COMTENT");
		}
		rs.close();
		pst.close();
	
		return result;
	}
	
	public String queryVLR(String imsi) throws SQLException{
		String VLN=null;
		
		sql="SELECT VLR_NUMBER FROM UTCN.BASICPROFILE WHERE IMSI='"+imsi+"'";
		logger.debug("Execute sql: "+sql);
		ResultSet rs=conn2.createStatement().executeQuery(sql);
		
		while(rs.next()){
			VLN=rs.getString("VLR_NUMBER");
		}
		rs.close();
		
		return VLN;
	}
	
	public Map<String,String> queryTADIG() throws SQLException{
		Map<String,String> map = new HashMap<String,String>();
		
		sql=" SELECT B.REALMNAME TADIG, A.CHARGEAREACODE VLR FROM CHARGEAREACONFIG A, REALM B "
				+ "WHERE A.AREAREFERENCE=B.AREAREFERENCE ";
		logger.debug("Execute sql: "+sql);
		ResultSet rs=conn2.createStatement().executeQuery(sql);
		
		while(rs.next()){
			map.put(rs.getString("VLR"), rs.getString("TADIG"));
		}
		rs.close();
		return map;
	}
	public String queryMccmnc(String tadig) throws SQLException{
		String mccmnc=null;
				
		sql=" SELECT MCCMNC FROM HUR_MCCMNC WHERE TADIG='"+tadig+"'";
		logger.debug("Execute sql: "+sql);
		ResultSet rs=conn.createStatement().executeQuery(sql);

		while(rs.next()){
			mccmnc=rs.getString("MCCMNC");
		}		
				
		rs.close();

		return mccmnc;		
	}
	
	public String queryCustomerServicePhone(String mccmnc) throws SQLException{
		String cPhone=null;
		String subcode=mccmnc.substring(0,3);
		sql=" SELECT PHONE FROM HUR_CUSTOMER_SERVICE_PHONE A WHERE A.CODE ='"+subcode+"'";
		logger.debug("Execute sql: "+sql);
		ResultSet rs=conn.createStatement().executeQuery(sql);
		
		while(rs.next()){
			cPhone=rs.getString("PHONE");
		}		
		rs.close();
		return cPhone;		
	}
	public void closeConnect() {
		if(conn!=null){
			try {
				super.conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(conn2!=null){
			try {
				super.conn2.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}

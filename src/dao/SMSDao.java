package dao;

import java.io.UnsupportedEncodingException;
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
import bean.SMSContent;

public class SMSDao extends BaseDao{
	
	public SMSDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(SMSDao.class);
	}

	public List<SMSLog> querySMSLog(String fromDate,String toDate,String msisdn) throws SQLException, UnsupportedEncodingException{
		
		List<SMSLog> list =new ArrayList<SMSLog>();
		sql=
				"SELECT A.ID,A.MSG,A.RESULT,A.SEND_NUMBER,to_char(A.SEND_DATE,'yyyy/MM/dd HH:mi:ss') SEND_DATE,to_char(A.CREATE_DATE,'yyyy/MM/dd HH:mi:ss') CREATE_DATE "
				+ "FROM HUR_SMS_LOG A "
				+ "WHERE  1=1 "
				+ (fromDate!=null &&!"".equals(fromDate)?"AND A.SEND_DATE >=to_date('"+fromDate+"','yyyy-mm-dd') ":"")  
				+ (toDate!=null &&!"".equals(toDate)?"AND A.SEND_DATE<=to_date('"+toDate+"','yyyy-mm-dd')+1 ":"")
				+ (msisdn!=null && !"".equals(msisdn)?"AND A.SEND_NUMBER='"+msisdn+"' ":"")
				+ "ORDER BY A.CREATE_DATE DESC";
		
			Statement st = conn.createStatement();
			logger.debug("Execute sql: "+sql);
			ResultSet rs=st.executeQuery(sql);
			while(rs.next()){
				SMSLog log = new SMSLog();
				log.setId(rs.getString("ID"));
				//log.setMsg(rs.getString("MSG"));
				log.setMsg((rs.getString("MSG")==null?"":new String(rs.getString("MSG").getBytes("ISO8859-1"),"BIG5")));
				log.setResult(rs.getString("RESULT"));
				log.setSendNumber(rs.getString("SEND_NUMBER"));
				log.setSendDate(rs.getString("SEND_DATE"));
				log.setCreateDate(rs.getString("CREATE_DATE"));
				list.add(log);
			}
		return list;

	}
	
	public List<SMSLog> querySMSLog() throws SQLException, UnsupportedEncodingException{
		List<SMSLog> list =new ArrayList<SMSLog>();
		sql=
				"SELECT A.ID,A.MSG,A.RESULT,A.SEND_NUMBER,to_char(A.SEND_DATE,'yyyy/MM/dd HH:mi:ss') SEND_DATE,to_char(A.CREATE_DATE,'yyyy/MM/dd HH:mi:ss') CREATE_DATE "
				+ "FROM HUR_SMS_LOG A  "
				+ "WHERE 1=1 "
				+ "ORDER BY A.CREATE_DATE DESC";
		
			Statement st = conn.createStatement();
			logger.debug("Execute sql: "+sql);
			ResultSet rs=st.executeQuery(sql);
			while(rs.next()){
				SMSLog log = new SMSLog();
				log.setId(rs.getString("ID"));
				//log.setMsg(rs.getString("MSG"));
				log.setMsg((rs.getString("MSG")==null?"":new String(rs.getString("MSG").getBytes("ISO8859-1"),"BIG5")));
				log.setResult(rs.getString("RESULT"));
				log.setSendNumber(rs.getString("SEND_NUMBER"));
				log.setSendDate(rs.getString("SEND_DATE"));
				log.setCreateDate(rs.getString("CREATE_DATE"));
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
				"SELECT A.ID,A.BRACKET,A.MEGID,A.SUSPEND,A.PRICEPLANID "
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
				log.setPricePlanId(rs.getString("PRICEPLANID"));
	
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
					"INSERT INTO HUR_SMS_SETTING(ID,BRACKET,MEGID,PRICEPLANID,SUSPEND) "
					+ "VALUES(?,?,?,?,?)";
			PreparedStatement pst = conn.prepareStatement(sql);
			for(SMSSetting s : list){
				pst.setString(1, s.getId());
				pst.setDouble(2, s.getBracket());
				pst.setString(3, s.getMsg());
				pst.setString(4, s.getPricePlanId());
				if(s.getSuspend())
					pst.setString(5, "1");
				else
					pst.setString(5, "0");
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
				"SELECT A.IMSI,A.THRESHOLD,C.SERVICECODE,to_char(A.CREATE_DATE,'yyyy/MM/dd HH24:mi:ss')  CREATE_DATE "
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
			g.setCreateDate(rs.getString("CREATE_DATE"));
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
				"INSERT INTO HUR_GPRS_THRESHOLD (IMSI,THRESHOLD,CREATE_DATE) "
				+ "VALUES(?,?,sysdate)";
		
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
	
	public Map<String,String> queryTWNMSISDN(String msisdn) throws SQLException{
		logger.info("queryTWNMSISDN...");
		Map<String,String> map =new HashMap<String,String>();
		String TWNmsisdn = null;
		
		//方法1
		sql=
				"SELECT A.SERVICEID, B.SERVICECODE, A.FOLLOWMENUMBER, B.DATEACTIVATED, B.STATUS "
				+ "FROM FOLLOWMEDATA A, SERVICE B "
				+ "WHERE A.SERVICEID=B.SERVICEID "
				+ "AND A.FOLLOWMENUMBER=?";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setString(1, msisdn);
		logger.debug("Execute sql: "+sql);
		ResultSet rs = pst.executeQuery();
		while(rs.next()){
			TWNmsisdn=rs.getString("SERVICECODE");
		}
		
		if(rs!=null)rs.close();
		if(pst!=null)pst.close();

		//方法2
		if(TWNmsisdn==null || "".equals(TWNmsisdn)){
			sql=
					"SELECT A.SERVICEID, B.SERVICECODE, A.VALUE, B.DATEACTIVATED, B.STATUS "
					+ "FROM NEWSERVICEORDERPARAMETERVALUE A, SERVICE B "
					+ "WHERE A.SERVICEID=B.SERVICEID AND A.PARAMETERVALUEID=3792 "
					+ "AND VALUE=?";
			
			PreparedStatement pst2 = conn2.prepareStatement(sql);
			
			pst2.setString(1, msisdn);
			logger.debug("Execute sql: "+sql);
			ResultSet rs2 = pst2.executeQuery();
			while(rs2.next()){
				TWNmsisdn=rs2.getString("SERVICECODE");
			}
			
			if(pst2!=null) pst2.close();	
			if(rs2!=null) rs2.close();
		}
		
		//方法3
		if(TWNmsisdn==null || "".equals(TWNmsisdn)){
			sql=
					"SELECT A.ORDERID, A.OLDVALUE, A.NEWVALUE, A.COMPLETEDATE, C.SERVICEID, C.SERVICECODE, C.STATUS "
					+ "FROM SERVICEPARAMVALUECHANGEORDER A, SERVICEORDER B, SERVICE C "
					+ "WHERE A.ORDERID=B.ORDERID AND B.SERVICEID=C.SERVICEID "
					+ "AND A.PARAMETERVALUEID=3792 AND C.SUBSIDIARYID=59 "
					+ "AND A.OLDVALUE<>A.NEWVALUE "
					+ "AND A.NEWVALUE=? "
					+ "ORDER BY A.ORDERID DESC";
			
			PreparedStatement pst2 = conn2.prepareStatement(sql);
			
			pst2.setString(1, msisdn);
			logger.debug("Execute sql: "+sql);
			ResultSet rs2 = pst2.executeQuery();
			while(rs2.next()){
				TWNmsisdn=rs2.getString("SERVICECODE");
			}
			
			if(pst2!=null) pst2.close();	
			if(rs2!=null) rs2.close();
		}		
		map.put("msisdn", TWNmsisdn);
		closeConnect();
		return map;
	}
	
	public Map<String,String> queryMSISDN(String imsi) throws SQLException{
		logger.info("queryIMSI...");
		Map<String,String> map =new HashMap<String,String>();
		String msisdn = null;
		String pricaplainid = null;
		sql=
				"SELECT B.IMSI,C.SERVICECODE,C.PRICEPLANID "
				+ "FROM IMSI B,SERVICE C "
				+ "WHERE B.SERVICEID = C.SERVICEID "
				+ "AND B.IMSI = ?";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setString(1, imsi);
		logger.debug("Execute sql: "+sql);
		ResultSet rs = pst.executeQuery();
		while(rs.next()){
			msisdn=rs.getString("SERVICECODE");
			pricaplainid=rs.getString("PRICEPLANID");
		}
		rs.close();
		pst.close();
		map.put("msisdn", msisdn);
		map.put("pricaplainid", pricaplainid);
		closeConnect();
		return map;
	}
	
	public String getSMSContent(String smsId) throws SQLException{
		
		String result=null;
		
		sql="SELECT A.CONTENT FROM HUR_SMS_CONTENT A WHERE A.ID=? ";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setString(1, smsId);
		logger.debug("Execute sql: "+sql);
		ResultSet rs = pst.executeQuery();
		
		while(rs.next()){
			result=rs.getString("CONTENT");
		}
		rs.close();
		pst.close();
	
		return result;
	}
	
	public void logSendSMS(String phone,String msgid,String res) throws SQLException{
		sql="INSERT INTO HUR_SMS_LOG"
				+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE) "
				+ "VALUES(DVRS_SMS_ID.NEXTVAL,?,?,?,?,SYSDATE)";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		pst.setString(1, phone);
		pst.setString(2, msgid);
		pst.setDate(3,tool.convertJaveUtilDate_To_JavaSqlDate(new Date()));
		pst.setString(4, (res.contains("Message Submitted")?"Success":"failed"));
		pst.executeUpdate();
		pst.close();
		
		
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
				e.printStackTrace();
			}
		}
		if(conn2!=null){
			try {
				super.conn2.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	public List<SMSContent> querySMSContent() throws SQLException, UnsupportedEncodingException{
		List<SMSContent> result = new ArrayList<SMSContent>();
		logger.info("querySMSContent...");

		sql=
				"SELECT  A.ID,A.CONTENT,A.CHARSET,A.DESCRIPTION "
				+ "FROM HUR_SMS_CONTENT A "
				+ "ORDER BY A.ID ";
		
			ResultSet rs = conn.createStatement().executeQuery(sql);
			while(rs.next()){
				SMSContent sc = new SMSContent();
				
				sc.setId(rs.getInt("ID"));
				sc.setComtent((rs.getString("CONTENT")==null?"":new String(rs.getString("CONTENT").getBytes("ISO8859-1"),"BIG5")));
				sc.setCharSet((rs.getString("CHARSET")==null?"":rs.getString("CHARSET")));
				sc.setDescription((rs.getString("DESCRIPTION")==null?"":new String(rs.getString("DESCRIPTION").getBytes("ISO8859-1"),"BIG5")));
				result.add(sc);
			}
			
			rs.close();
			closeConnect();

		return result;
	}
	
	public List<SMSContent> querySMSContent(String id) throws SQLException, UnsupportedEncodingException{
		List<SMSContent> result = new ArrayList<SMSContent>();
		logger.info("querySMSContent...");

		sql=
				"SELECT  A.ID,A.CONTENT,A.CHARSET,A.DESCRIPTION "
				+ "FROM HUR_SMS_CONTENT A "
				+ "WHERE A.ID IN ("+id+") "
				+ "ORDER BY A.ID ";
		
			ResultSet rs = conn.createStatement().executeQuery(sql);
			while(rs.next()){
				SMSContent sc = new SMSContent();
				
				sc.setId(rs.getInt("ID"));
				sc.setComtent((rs.getString("CONTENT")==null?"":new String(rs.getString("CONTENT").getBytes("ISO8859-1"),"BIG5")));
				sc.setCharSet((rs.getString("CHARSET")==null?"":rs.getString("CHARSET")));
				sc.setDescription((rs.getString("DESCRIPTION")==null?"":new String(rs.getString("DESCRIPTION").getBytes("ISO8859-1"),"BIG5")));
				result.add(sc);
			}
			
			rs.close();
			closeConnect();

		return result;
	}
	
	public int insertSMSContent(SMSContent sc) throws Exception{
		int result=0;
		
		logger.info("insertSMSContent...");

		sql=
				"INSERT INTO HUR_SMS_CONTENT (ID,CONTENT,CHARSET,DESCRIPTION) "
				+ "VALUES(?,?,?,?)";
		
			PreparedStatement pst = conn.prepareStatement(sql);
			
			pst.setInt(1, sc.getId());
			pst.setString(2, (sc.getComtent()!=null ? new String(sc.getComtent().getBytes("BIG5"),"ISO8859-1"):""));
			pst.setString(3, sc.getCharSet());
			pst.setString(4, (sc.getDescription()!=null ? new String(sc.getDescription().getBytes("BIG5"),"ISO8859-1"):""));
			
			result=pst.executeUpdate();
			
			pst.close();
			
			closeConnect();
		
		return result;
	}
	public int updateSMSContent(SMSContent sc) throws Exception{
		int result=0;
		
		logger.info("insertSMSContent...");

		sql=
				"UPDATE  HUR_SMS_CONTENT A "
				+ "SET A.CONTENT=?,A.CHARSET=?,A.DESCRIPTION=? "
				+ "WHERE A.ID=?";
		
			PreparedStatement pst = conn.prepareStatement(sql);
			
			pst.setString(1, (sc.getComtent()!=null ? new String(sc.getComtent().getBytes("BIG5"),"ISO8859-1"):""));
			pst.setString(2, sc.getCharSet());
			pst.setString(3, (sc.getDescription()!=null ? new String(sc.getDescription().getBytes("BIG5"),"ISO8859-1"):""));
			pst.setInt(4, sc.getId());
			
			result=pst.executeUpdate();
			
			pst.close();
			
			closeConnect();
			
		return result;
	}
	public int deleteSMSContent(SMSContent sc) throws Exception{
		int result=0;
		
		logger.info("insertSMSContent...");

		sql=
				"DELETE HUR_SMS_CONTENT A "
				+ "WHERE A.ID=?";
		
			PreparedStatement pst = conn.prepareStatement(sql);
			
			pst.setInt(1, sc.getId());
			
			result=pst.executeUpdate();
			
			pst.close();
			
			closeConnect();
		return result;
	}
	
}

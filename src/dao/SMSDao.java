package dao;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cache.CacheAction;
import bean.GPRSThreshold;
import bean.SMSLog;
import bean.SMSSetting;
import bean.SMSContent;

public class SMSDao extends BaseDao{
	
	public SMSDao() throws Exception {
		super();
	}
	
	Map<String,String> serviceIDtoIMSI = new HashMap<String,String>();
	Map<String,String> imsitoServiceID = new HashMap<String,String>();
	
	public List<SMSLog> querySMSLog(String fromDate,String toDate,String msisdn) throws SQLException, UnsupportedEncodingException{
		
		List<SMSLog> list =new ArrayList<SMSLog>();
		sql=
				"SELECT A.ID,A.MSG,A.RESULT,A.SEND_NUMBER,to_char(A.SEND_DATE,'yyyy/MM/dd HH24:mi:ss') SEND_DATE,to_char(A.CREATE_DATE,'yyyy/MM/dd HH24:mi:ss') CREATE_DATE "
				+ "FROM HUR_SMS_LOG A "
				+ "WHERE  1=1 "
				+ (fromDate!=null &&!"".equals(fromDate)?"AND A.SEND_DATE >=to_date('"+fromDate+"','yyyy-mm-dd') ":"")  
				+ (toDate!=null &&!"".equals(toDate)?"AND A.SEND_DATE<=to_date('"+toDate+"','yyyy-mm-dd')+1 ":"")
				+ (msisdn!=null && !"".equals(msisdn)?"AND A.SEND_NUMBER='"+msisdn+"' ":"")
				+ "ORDER BY A.CREATE_DATE DESC";
		
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql);
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
		} finally{
			try {
				if(rs!=null)
					rs.close();
				if(st!=null)
					st.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return list;

	}
	
	public List<SMSLog> querySMSLog() throws SQLException, UnsupportedEncodingException{
		List<SMSLog> list =new ArrayList<SMSLog>();
		sql=
				"SELECT A.ID,A.MSG,A.RESULT,A.SEND_NUMBER,to_char(A.SEND_DATE,'yyyy/MM/dd HH24:mi:ss') SEND_DATE,to_char(A.CREATE_DATE,'yyyy/MM/dd HH24:mi:ss') CREATE_DATE "
				+ "FROM HUR_SMS_LOG A  "
				+ "WHERE 1=1 "
				+ "ORDER BY A.CREATE_DATE DESC";
		
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql);
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
		}finally{
			try {
				if(rs!=null)
					rs.close();
				if(st!=null)
					st.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return list;
	}
	
	public List<SMSSetting> querySMSSetting() throws SQLException{
		List<SMSSetting> list =new ArrayList<SMSSetting>();
		sql=
				"SELECT A.ID,A.BRACKET*100 BRACKET,A.MEGID,A.SUSPEND,PRICEPLANID "
						+ "FROM HUR_SMS_SETTING A "
						+ "ORDER BY A.ID ";
				/*"SELECT A.ID,A.BRACKET,A.MEGID,A.SUSPEND,A.PRICEPLANID "
				+ "FROM HUR_SMS_SETTING A "
				+ "ORDER BY A.ID ";*/
		
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql);
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
		} finally{
			try {
				if(rs!=null)
					rs.close();
				if(st!=null)
					st.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return list;
	}
	
	public List<SMSSetting> updateSMSSetting(List<SMSSetting> list) throws SQLException{
		//�����Ҧ����
		sql=
				"TRUNCATE  TABLE  HUR_SMS_SETTING";
		
		/*"INSERT INTO HUR_SMS_SETTING(ID,BRACKET,MEGID,SUSPEND,PRICEPLANID) "
						+ "VALUES(?,?,?,?,?)";*/
		PreparedStatement pst = null;
		Statement st = null;
		try {
			st = conn.createStatement();
			st.execute(sql);
			sql=
					"INSERT INTO HUR_SMS_SETTING(ID,BRACKET,MEGID,SUSPEND) "
					+ "VALUES(?,?,?,?)";
					pst = conn.prepareStatement(sql);
			for(SMSSetting s : list){
				pst.setString(1, s.getId());
				pst.setDouble(2, s.getBracket()/100);
				pst.setString(3, s.getMsg());
				if(s.getSuspend())
					pst.setString(4, "1");
				else
					pst.setString(4, "0");
				
				//pst.setString(5, s.getPricePlanId());
				pst.addBatch();
			}
			pst.executeBatch();
		}finally{
			try {

				if(st!=null)
					st.close();
				if(pst!=null)
					pst.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return list;
	}
	
	public List<GPRSThreshold> queryAlertLimit() throws SQLException, ParseException{
		imsitoServiceID = CacheAction.getImsitoServiceID();
		serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		if(imsitoServiceID.size()==0){
			CacheAction.reloadServiceIDwithIMSIMappingCache();
			imsitoServiceID = CacheAction.getImsitoServiceID();
			serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		}
		List<GPRSThreshold> list =new ArrayList<GPRSThreshold>();
		sql=
				"SELECT A.SERVICEID,A.THRESHOLD,TO_CHAR(C.DATECREATED,'yyyy/MM/dd HH24:mi:ss') DATECREATED, "
				+ "CASE "
				+ "	     WHEN C.SERVICECODE IS NULL THEN ' ' ELSE C.SERVICECODE END SERVICECODE, "
				+ "CASE "
				+ "	     WHEN A.CANCEL_DATE IS NOT NULL THEN  TO_CHAR(A.CANCEL_DATE,'yyyy/MM/dd HH24:mi:ss') ELSE ' ' END  CANCEL_DATE , "
				+ "CASE "
				+ "	     WHEN A.CREATE_DATE IS NOT NULL THEN  TO_CHAR(A.CREATE_DATE,'yyyy/MM/dd HH24:mi:ss') ELSE ' ' END  CREATE_DATE , "
				+ "CASE "
				+ "	     WHEN C.STATUS IS NULL THEN ' ' "
				+ "      WHEN C.STATUS=1 THEN 'Normal' ELSE 'Inactive' END STATUS "
				+ "FROM HUR_GPRS_THRESHOLD A,IMSI B,SERVICE C "
				+ "WHERE A.SERVICEID=B.SERVICEID(+) AND A.SERVICEID = C.SERVICEID (+) "
				+ "ORDER BY A.CREATE_DATE DESC ";
		
		Statement st = null;
		ResultSet rs = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			while(rs.next()){
				String imsi = "--";

				String dateActive = rs.getString("DATECREATED");
				String createDate = rs.getString("CREATE_DATE");
				if(dateActive != null && !" ".equals(dateActive) && createDate!= null && !" ".equals(createDate) && (sdf.parse(dateActive).before(sdf.parse(createDate))))
					imsi=serviceIDtoIMSI.get(rs.getString("SERVICEID"));
				
				
				if(imsi==null || "".equals(imsi))
					imsi=rs.getString("SERVICEID");
				GPRSThreshold g = new GPRSThreshold();
				
				
				
				g.setImsi(imsi);
				g.setMsisdn(rs.getString("SERVICECODE"));
				g.setThreshold(rs.getDouble("THRESHOLD"));
				g.setCreateDate(rs.getString("CREATE_DATE"));
				g.setStatus(rs.getString("STATUS"));
				g.setCancelDate(rs.getString("CANCEL_DATE"));
				list.add(g);
			}
		} finally{
			try {
				if(rs!=null)
					rs.close();
				if(st!=null)
					st.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return list;
	}
	
	public int insertAlertLimit(String imsi,Double limit) throws SQLException{
		imsitoServiceID = CacheAction.getImsitoServiceID();
		serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		if(imsitoServiceID.size()==0){
			CacheAction.reloadServiceIDwithIMSIMappingCache();
			imsitoServiceID = CacheAction.getImsitoServiceID();
			serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		}
		
		String serviceid = imsitoServiceID.get(imsi);
		sql=
				"INSERT INTO HUR_GPRS_THRESHOLD (SERVICEID,THRESHOLD,CREATE_DATE) "
				+ "VALUES(?,?,sysdate)";
		
		PreparedStatement pst = null;
		int result;
		try {
			pst = conn.prepareStatement(sql);
			
			pst.setString(1, serviceid);
			pst.setDouble(2, limit);
			result = pst.executeUpdate();
		} finally{
			try {
				if(pst!=null)
					pst.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return result;
	}
	
	public int updateAlertLimit(String imsi,Double limit) throws SQLException{
		imsitoServiceID = CacheAction.getImsitoServiceID();
		serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		if(imsitoServiceID.size()==0){
			CacheAction.reloadServiceIDwithIMSIMappingCache();
			imsitoServiceID = CacheAction.getImsitoServiceID();
			serviceIDtoIMSI = CacheAction.getServiceIDtoIMSI();
		}
		String serviceid = imsitoServiceID.get(imsi);		
		sql=
				"UPDATE HUR_GPRS_THRESHOLD A "
				+ "SET A.THRESHOLD = ? "
				+ "WHERE A.SERVICEID=? ";
		
		PreparedStatement pst = null;
		int result;
		try {
			pst = conn.prepareStatement(sql);
			pst.setDouble(1, limit);
			pst.setString(2, serviceid);
			result = pst.executeUpdate();
		} finally{
			try {
				if(pst!=null)
					pst.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return result;
	}
	
	//public int deleteAlertLimit(String imsi,Double limit) throws SQLException{
	public int deleteAlertLimit(String msisdn) throws SQLException{
		sql=
				/*"DELETE HUR_GPRS_THRESHOLD A "
				+ "WHERE A.SERVICEID=? ";*/
				"UPDATE HUR_GPRS_THRESHOLD A SET A.CANCEL_DATE = SYSDATE "
				+ "WHERE A.CANCEL_DATE is null "
				+ "AND A.SERVICEID=(	select serviceid "
				+ "						from (SELECT * FROM SERVICE B "
				+ "						WHERE B.SERVICECODE='"+msisdn+"'  order by B.datecreated desc  ) C  where rownum = 1)";
		
		Statement st = null;
		int result;
		try {

			st=conn.createStatement();
			System.out.print("Execute Delete:"+sql);
			result = st.executeUpdate(sql);
		} finally{
			try {
				if(st!=null)
					st.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return result;
	}
	
	public String checkAlertExisted(String msisdn) throws SQLException{
		String result = null;
		
		sql = "SELECT count(1) AB "
				+ "FROM HUR_GPRS_THRESHOLD A,SERVICE B "
				+ "WHERE A.serviceid = b.serviceid  AND A.CANCEL_DATE IS NULL and B.servicecode(+) = '"+msisdn+"' ";
		Statement st = null;
		ResultSet rs = null;
		
		try{
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()){
				result = rs.getString("AB");
			}
			
		} finally{
			try {
				if(st!=null)
					st.close();
				if(rs!=null)
					rs.close();
			} catch (Exception e) {
			}
			closeConnection();
		}

		return result;
	}
	
	public Map<String,String> queryIMSI(String msisdn) throws SQLException{
		Map<String,String> map =new HashMap<String,String>();
		String imsi = null;
		String pricaplainid = null;
		sql=
				"SELECT B.IMSI,C.SERVICECODE,C.PRICEPLANID "
				+ "FROM IMSI B,SERVICE C "
				+ "WHERE B.SERVICEID = C.SERVICEID "
				+ "AND C.SERVICECODE = ?";
		PreparedStatement pst = null;
		try {
			 pst = conn.prepareStatement(sql);
			
			pst.setString(1, msisdn);
			ResultSet rs = pst.executeQuery();
			while(rs.next()){
				imsi=rs.getString("IMSI");
				pricaplainid=rs.getString("PRICEPLANID");
			}
			map.put("imsi", imsi);
			map.put("pricaplainid", pricaplainid);
		} finally{
			try {
				if(pst!=null)
					pst.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return map;
	}
	
	public Map<String,String> queryTWNMSISDN(String msisdn) throws SQLException{
		Map<String,String> map =new HashMap<String,String>();
		String TWNmsisdn = null;
		
		//��k1
		sql=
				"SELECT A.SERVICEID, B.SERVICECODE, A.FOLLOWMENUMBER, B.DATEACTIVATED, B.STATUS "
				+ "FROM FOLLOWMEDATA A, SERVICE B "
				+ "WHERE A.SERVICEID=B.SERVICEID "
				+ "AND A.FOLLOWMENUMBER=?";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setString(1, msisdn);
		ResultSet rs = pst.executeQuery();
		while(rs.next()){
			TWNmsisdn=rs.getString("SERVICECODE");
		}
		
		if(rs!=null)rs.close();
		if(pst!=null)pst.close();

		//��k2
		if(TWNmsisdn==null || "".equals(TWNmsisdn)){
			sql=
					"SELECT A.SERVICEID, B.SERVICECODE, A.VALUE, B.DATEACTIVATED, B.STATUS "
					+ "FROM NEWSERVICEORDERPARAMETERVALUE A, SERVICE B "
					+ "WHERE A.SERVICEID=B.SERVICEID AND A.PARAMETERVALUEID=3792 "
					+ "AND VALUE=?";
			
			PreparedStatement pst2 = conn2.prepareStatement(sql);
			
			pst2.setString(1, msisdn);
			ResultSet rs2 = pst2.executeQuery();
			while(rs2.next()){
				TWNmsisdn=rs2.getString("SERVICECODE");
			}
			
			if(pst2!=null) pst2.close();	
			if(rs2!=null) rs2.close();
		}
		
		//��k3
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
			ResultSet rs2 = pst2.executeQuery();
			while(rs2.next()){
				TWNmsisdn=rs2.getString("SERVICECODE");
			}
			
			if(pst2!=null) pst2.close();	
			if(rs2!=null) rs2.close();
		}		
		
		closeConnection();
		map.put("msisdn", TWNmsisdn);
		
		return map;
	}
	
	public Map<String,String> queryMSISDN(String imsi) throws SQLException{
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
		ResultSet rs = pst.executeQuery();
		while(rs.next()){
			msisdn=rs.getString("SERVICECODE");
			pricaplainid=rs.getString("PRICEPLANID");
		}
		rs.close();
		pst.close();
		closeConnection();
		
		map.put("msisdn", msisdn);
		map.put("pricaplainid", pricaplainid);
		
		return map;
	}
	
	public String getSMSContent(String smsId) throws SQLException{
		
		String result=null;
		
		sql="SELECT A.CONTENT FROM HUR_SMS_CONTENT A WHERE A.ID=? ";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setString(1, smsId);
		ResultSet rs = pst.executeQuery();
		
		while(rs.next()){
			result=rs.getString("CONTENT");
		}
		rs.close();
		pst.close();
		return result;
	}
	
	public void logSendSMS(String phone,String msgid,String res,String type) throws SQLException{
		sql="INSERT INTO HUR_SMS_LOG"
				+ "(ID,SEND_NUMBER,MSG,SEND_DATE,RESULT,CREATE_DATE,TYPE) "
				+ "VALUES(DVRS_SMS_ID.NEXTVAL,?,?,to_date(?,'yyyyMMddhh24miss'),?,SYSDATE,'"+type+"')";
		
		PreparedStatement pst = null;
		try {
			pst = conn.prepareStatement(sql);
			pst.setString(1, phone);
			pst.setString(2, msgid);
			pst.setString(3,new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
			pst.setString(4, (res.contains("Message Submitted")?"Success":"failed"));
			pst.executeUpdate();
		}finally{
			pst.close();
		}			
	}
	public java.sql.Date convertJaveUtilDate_To_JavaSqlDate(java.util.Date date) {
		
		return new java.sql.Date(date.getTime());
	}
	
	public String queryVLR(String imsi) throws SQLException{
		String VLN=null;
		
		sql="SELECT VLR_NUMBER FROM UTCN.BASICPROFILE WHERE IMSI='"+imsi+"'";
		Statement st =conn2.createStatement();
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			VLN=rs.getString("VLR_NUMBER");
		}
		rs.close();
		st.close();
		return VLN;
	}
	
	public Map<String,String> queryTADIG() throws SQLException{
		Map<String,String> map = new HashMap<String,String>();
		
		sql=" SELECT B.REALMNAME TADIG, A.CHARGEAREACODE VLR FROM CHARGEAREACONFIG A, REALM B "
				+ "WHERE A.AREAREFERENCE=B.AREAREFERENCE ";
		Statement st =conn2.createStatement();
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			map.put(rs.getString("VLR"), rs.getString("TADIG"));
		}
		rs.close();
		st.close();
		return map;
	}
	public String queryMccmnc(String tadig) throws SQLException{
		String mccmnc=null;
				
		sql=" SELECT MCCMNC FROM HUR_MCCMNC WHERE TADIG='"+tadig+"'";
		Statement st =conn.createStatement();
		ResultSet rs=st.executeQuery(sql);

		while(rs.next()){
			mccmnc=rs.getString("MCCMNC");
		}		
				
		rs.close();
		st.close();

		return mccmnc;		
	}
	
	public String queryCustomerServicePhone(String mccmnc) throws SQLException{
		String cPhone=null;
		String subcode=mccmnc.substring(0,3);
		sql=" SELECT PHONE FROM HUR_CUSTOMER_SERVICE_PHONE A WHERE A.CODE ='"+subcode+"'";
		Statement st =conn.createStatement();
		ResultSet rs=st.executeQuery(sql);
		
		while(rs.next()){
			cPhone=rs.getString("PHONE");
		}		
		rs.close();
		st.close();
		return cPhone;		
	}

	public List<SMSContent> querySMSContent() throws SQLException, UnsupportedEncodingException{
		List<SMSContent> result = new ArrayList<SMSContent>();
		sql=
				"SELECT  A.ID,A.CONTENT,A.CHARSET,A.DESCRIPTION "
				+ "FROM HUR_SMS_CONTENT A "
				+ "ORDER BY A.ID ";
		
		Statement st =conn.createStatement();
		ResultSet rs=st.executeQuery(sql);
			while(rs.next()){
				SMSContent sc = new SMSContent();
				
				sc.setId(rs.getInt("ID"));
				sc.setComtent((rs.getString("CONTENT")==null?"":new String(rs.getString("CONTENT").getBytes("ISO8859-1"),"BIG5")));
				sc.setCharSet((rs.getString("CHARSET")==null?"":rs.getString("CHARSET")));
				sc.setDescription((rs.getString("DESCRIPTION")==null?"":new String(rs.getString("DESCRIPTION").getBytes("ISO8859-1"),"BIG5")));
				result.add(sc);
			}
			
			rs.close();
			st.close();
			closeConnection();
		return result;
	}
	
	public List<SMSContent> querySMSContent(String id) throws SQLException, UnsupportedEncodingException{
		List<SMSContent> result = new ArrayList<SMSContent>();
		sql=
				"SELECT  A.ID,A.CONTENT,A.CHARSET,A.DESCRIPTION "
				+ "FROM HUR_SMS_CONTENT A "
				+ "WHERE A.ID IN ("+id+") "
				+ "ORDER BY A.ID ";
		
		Statement st =conn.createStatement();
		ResultSet rs=st.executeQuery(sql);
			while(rs.next()){
				SMSContent sc = new SMSContent();
				
				sc.setId(rs.getInt("ID"));
				sc.setComtent((rs.getString("CONTENT")==null?"":new String(rs.getString("CONTENT").getBytes("ISO8859-1"),"BIG5")));
				sc.setCharSet((rs.getString("CHARSET")==null?"":rs.getString("CHARSET")));
				sc.setDescription((rs.getString("DESCRIPTION")==null?"":new String(rs.getString("DESCRIPTION").getBytes("ISO8859-1"),"BIG5")));
				result.add(sc);
			}
			
			rs.close();
			st.close();
			closeConnection();

		return result;
	}
	
	public int insertSMSContent(SMSContent sc) throws Exception{
		int result=0;
		sql=
				"INSERT INTO HUR_SMS_CONTENT (ID,CONTENT,CHARSET,DESCRIPTION) "
				+ "VALUES(?,?,?,?)";
		
		PreparedStatement pst =conn.prepareStatement(sql);			
		pst.setInt(1, sc.getId());
		pst.setString(2, (sc.getComtent()!=null ? new String(sc.getComtent().getBytes("BIG5"),"ISO8859-1"):""));
		pst.setString(3, sc.getCharSet());
		pst.setString(4, (sc.getDescription()!=null ? new String(sc.getDescription().getBytes("BIG5"),"ISO8859-1"):""));
		
		result=pst.executeUpdate();
		
		pst.close();	
		closeConnection();
		
		return result;
	}
	public int updateSMSContent(SMSContent sc) throws Exception{
		int result=0;
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
		closeConnection();

		return result;
	}
	public int deleteSMSContent(SMSContent sc) throws Exception{
		int result=0;
		sql=
				"DELETE HUR_SMS_CONTENT A "
				+ "WHERE A.ID=?";
		
		PreparedStatement pst = conn.prepareStatement(sql);
		
		pst.setInt(1, sc.getId());
		
		result=pst.executeUpdate();
		
		pst.close();
		closeConnection();
			
		return result;
	}
	
	public Map<String,String> queryGPRSContent() throws SQLException, UnsupportedEncodingException{
		Map<String,String> m = new HashMap<String,String>();
		sql=
				"SELECT case A.id when 201 then 'A' when 202 then 'B' when 203 then 'CA' when 204 then 'CI' END ID,A.CONTENT "
				+ "FROM HUR_SMS_CONTENT A "
				+ "where A.ID in ('201','202','203','204')";
		
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			while(rs.next()){
				m.put(rs.getString("ID"),new String(rs.getString("CONTENT").getBytes("ISO8859-1"),"BIG5"));
			}
		} finally{
			try {
				if(rs!=null)
					rs.close();
				if(st!=null)
					st.close();
				closeConnection();
			} catch (Exception e) {
			}
		}
		return m;
	}	
}

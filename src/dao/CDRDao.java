package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bean.CDR;

public class CDRDao extends BaseDao {

	public CDRDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	public List<CDR> queryCDR() throws SQLException, ParseException{
		sql=
				"SELECT A.USAGEID,A.IMSI,A.CALLTIME,A.MCCMNC,A.SGSNADDRESS,A.DATAVOLUME,A.FILEID "
				+ "FROM HUR_DATA_USAGE A "
				+ "ORDER BY A.FILEID,A.CALLTIME DESC";
		List<CDR> list = new ArrayList<CDR>();
		
			Statement st = conn.createStatement();
			
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				CDR c = new CDR();
				c.setUsageId(rs.getString("USAGEID"));
				c.setImsi(rs.getString("IMSI"));
				c.setCalltime(rs.getString("CALLTIME"));
				c.setMccmnc(rs.getString("MCCMNC"));
				c.setSgsnAddress(rs.getString("SGSNADDRESS"));
				c.setDataVolume(rs.getDouble("DATAVOLUME"));
				c.setFileId(rs.getString("FILEID"));
				list.add(c);
			}
			rs.close();
			st.close();
			closeConnect();
		return list;
		
	}
	
	public List<CDR> queryCDR(String from,String to,String IMSI) throws SQLException, ParseException{
		
		System.out.println("查詢CDR期間從"+from+"到"+to);
		sql=
				"SELECT A.USAGEID,A.IMSI,A.CALLTIME,A.MCCMNC,A.SGSNADDRESS,A.DATAVOLUME,A.FILEID "
				+ "FROM HUR_DATA_USAGE A "
				+ "WHERE 1=1 "
				+ (from!=null &&!"".equals(from)?"AND to_date(A.CALLTIME,'yyyy/MM/dd hh24:mi;ss')>=to_date('"+from+"','yyyy-mm-dd') ":"")  
				+ (to!=null &&!"".equals(to)?"AND to_date(A.CALLTIME,'yyyy/MM/dd hh24:mi;ss')<=to_date('"+to+"','yyyy-mm-dd')+1 ":"")
				+ (IMSI!=null && !"".equals(IMSI)?"AND A.IMSI='"+IMSI+"' ":"")
				+ "ORDER BY A.FILEID,A.CALLTIME DESC";
		List<CDR> list = new ArrayList<CDR>();
		
			Statement st = conn.createStatement();
			System.out.println(sql);
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				CDR c = new CDR();
				c.setUsageId(rs.getString("USAGEID"));
				c.setImsi(rs.getString("IMSI"));
				c.setCalltime(rs.getString("CALLTIME"));
				c.setMccmnc(rs.getString("MCCMNC"));
				c.setSgsnAddress(rs.getString("SGSNADDRESS"));
				c.setDataVolume(rs.getDouble("DATAVOLUME"));
				c.setFileId(rs.getString("FILEID"));
				list.add(c);
			}
			rs.close();
			st.close();
			closeConnect();
		return list;
	}
}

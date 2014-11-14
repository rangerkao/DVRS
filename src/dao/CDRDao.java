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
	
	public List<CDR> queryCDR(String from,String to) throws SQLException, ParseException{
		if(from==null || to==null){
			return queryCDR();
		}
		System.out.println("�d��CDR�����q"+from+"��"+to);
		sql=
				"SELECT A.USAGEID,A.IMSI,A.CALLTIME,A.MCCMNC,A.SGSNADDRESS,A.DATAVOLUME,A.FILEID "
				+ "FROM HUR_DATA_USAGE A "
				+ "WHERE to_date(A.CALLTIME,'yyyy/MM/dd hh24:mi;ss')>=to_date(?,'yyyy-mm-dd')-1 "
				+ "AND to_date(A.CALLTIME,'yyyy/MM/dd hh24:mi;ss')<=to_date(?,'yyyy-mm-dd') "
				+ "ORDER BY A.FILEID,A.CALLTIME DESC";
		List<CDR> list = new ArrayList<CDR>();
		
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, from);
			pst.setString(2, to);
			ResultSet rs=pst.executeQuery();
			
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
			pst.close();
			closeConnect();
		return list;
	}
}

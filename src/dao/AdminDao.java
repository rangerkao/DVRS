package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import bean.Admin;

public class AdminDao extends BaseDao {	
	
	public int insert(Admin admin){
		sql=
				"INSERT INTO HUR_ADMIN(ID,ACCOUNT,PASSWORD,ROLE,CREATE_DATE)"
				+ "VALUES(?,?,?,?,sysdate)";
		int result=0;
		PreparedStatement pst;
		try {
			pst = conn.prepareStatement(sql);
			pst.setString(1, admin.getUserid());
			pst.setString(2, admin.getAccount());
			pst.setString(3, admin.getPassword());
			pst.setString(4, admin.getRole());
			result=pst.executeUpdate();
			pst.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closeConnect();
		return result;
	}
	public int update(Admin admin){
		sql=
				"UPDATE HUR_ADMIN "
				+ "SET ID=?,PASSWORD=?,ROLE=?,UPDATE_DATE=sysdate "
				+ "WHERE ACCOUNT=?";
		int result=0;
		PreparedStatement pst;
		try {
			pst = conn.prepareStatement(sql);
			pst.setString(1, admin.getUserid());
			pst.setString(2, admin.getPassword());
			pst.setString(3, admin.getRole());
			pst.setString(4, admin.getAccount());
			
			result=pst.executeUpdate();
			pst.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closeConnect();
		return result;
	}
	
	public int delete(Admin admin){
		sql=
				"DELETE HUR_ADMIN "
				+ "WHERE ACCOUNT=?";
		int result=0;
		PreparedStatement pst;
		try {
			pst = conn.prepareStatement(sql);
			pst.setString(1, admin.getAccount());
			result=pst.executeUpdate();
			pst.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closeConnect();
		return result;
	}
	
	//¬d¸ß¦Cªí
	public List<Admin> queryAdminList(){
		sql=
				"SELECT A.ID,A.ACCOUNT,A.PASSWORD,A.ROLE "
				+ "FROM HUR_ADMIN A ";
		List<Admin> list=new ArrayList<Admin>();
		
		try {
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			
			while(rs.next()){
				Admin admin =new Admin();
				admin.setUserid(rs.getString("ID"));
				admin.setAccount(rs.getString("ACCOUNT"));
				admin.setPassword(rs.getString("PASSWORD"));
				admin.setRole(rs.getString("ROLE"));
				list.add(admin);
			}
			st.close();
			rs.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closeConnect();
		return list;
		
	}
	
	public Admin queryAdminByAccount(String account){
		Admin admin =new Admin();
		sql=
				"SELECT A.ID,A.ACCOUNT,A.PASSWORD,A.ROLE "
				+ "FROM HUR_ADMIN A "
				+ "WHERE A.ACCOUNT=? ";
		
		PreparedStatement pst;
		try {
			pst = conn.prepareStatement(sql);
			pst.setString(1, account);
			
			ResultSet rs=pst.executeQuery();
			
			while(rs.next()){
				admin.setUserid(rs.getString("ID"));
				admin.setAccount(rs.getString("ACCOUNT"));
				admin.setPassword(rs.getString("PASSWORD"));
				admin.setRole(rs.getString("ROLE"));
			}
			pst.close();
			rs.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closeConnect();
		
		return admin;
	}
}

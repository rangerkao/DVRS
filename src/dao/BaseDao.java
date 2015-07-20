package dao;

import java.sql.Connection;
import java.sql.SQLException;

public class BaseDao {

	protected String sql="";
	protected Connection conn=null;
	protected Connection conn2=null;
	
	BaseDao() throws Exception{
		createConnection();
	}
	protected void createConnection() throws Exception{
		conn=cache.CacheAction.getConnect1();
		conn2=cache.CacheAction.getConnect2();
		System.out.print("Create connect!");
	}
	protected void closeConnection() throws SQLException{
		conn.close();
		conn2.close();
		System.out.print("Close connect!");
	}
	
	
	
}

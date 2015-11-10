package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class BaseDao {

	protected String sql="";
	protected static Properties props =null;
	protected Connection conn=null;
	protected Connection conn2=null;

	public BaseDao() throws Exception{
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

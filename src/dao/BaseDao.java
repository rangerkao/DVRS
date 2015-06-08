package dao;

import java.sql.Connection;
import java.sql.SQLException;

import program.IJatool;
import program.Jatool;


public class BaseDao {

	protected String sql="";
	protected Connection conn=null;
	protected Connection conn2=null;
	protected IJatool tool= new Jatool();
	
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

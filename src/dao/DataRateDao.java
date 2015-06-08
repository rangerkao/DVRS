package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import bean.DataRate;

public class DataRateDao extends BaseDao {
	
	public DataRateDao() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

		//查詢列表
		public List<DataRate> queryDataRateList() throws SQLException{
			sql=
					"SELECT A.PRICEPLANID, C.NAME PRICEPLANNAME,A.MCCMNC,B.COUNTRY, B.NETWORK, "
					+ "A.RATE, A.CHARGEUNIT, A.CURRENCY, A.DAYCAP "
					+ "FROM HUR_DATA_RATE A, HUR_MCCMNC B, PRICEPLAN C "
					+ "WHERE A.PRICEPLANID=C.PRICEPLANID AND A.MCCMNC=B.MCCMNC "
					+ "AND A.PRICEPLANID=139 "
					+ "ORDER BY A.MCCMNC";
			
					/*"SELECT A.PRICEPLANID,A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY "
					+ "FROM HUR_DATA_RATE A";*/
			List<DataRate> list=new ArrayList<DataRate>();
			
				Statement st = conn.createStatement();
				ResultSet rs=st.executeQuery(sql);
				
				while(rs.next()){
					DataRate datarate =new DataRate();
					datarate.setPricePlanId(rs.getLong("PRICEPLANID"));
					datarate.setPricePlanName(rs.getString("PRICEPLANNAME")+("(環球卡)"));
					datarate.setMccmnc(rs.getString("MCCMNC"));
					datarate.setCountry(rs.getString("COUNTRY"));
					datarate.setNetWork(rs.getString("NETWORK"));
					datarate.setRate(rs.getDouble("RATE"));
					datarate.setChargeunit(rs.getLong("CHARGEUNIT"));
					datarate.setCurrency(rs.getString("CURRENCY"));
					datarate.setDayCap(rs.getDouble("DAYCAP"));
					list.add(datarate);
				}
				st.close();
				rs.close();	
				closeConnection();
			return list;
			
		}

}

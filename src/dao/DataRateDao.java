package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import bean.DataRate;

public class DataRateDao extends BaseDao {
	
	//¬d¸ß¦Cªí
		public List<DataRate> queryDataRateList(){
			sql=
					"SELECT A.PRICEPLANID,A.MCCMNC,A.RATE,A.CHARGEUNIT,A.CURRENCY "
					+ "FROM HUR_DATA_RATE A";
			List<DataRate> list=new ArrayList<DataRate>();
			
			try {
				Statement st = conn.createStatement();
				ResultSet rs=st.executeQuery(sql);
				
				while(rs.next()){
					DataRate datarate =new DataRate();
					datarate.setPricePlanId(rs.getLong("PRICEPLANID"));
					datarate.setMccmnc(rs.getString("MCCMNC"));
					datarate.setRate(rs.getDouble("RATE"));
					datarate.setChargeunit(rs.getLong("CHARGEUNIT"));
					datarate.setCurrency(rs.getString("CURRENCY"));
					list.add(datarate);
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

}

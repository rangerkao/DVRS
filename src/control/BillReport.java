package control;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import bean.BillData;
import bean.Charge;
import bean.ChargeDetail;
import bean.Invoice;
import bean.InvoiceDetail;
import bean.Usage;
import bean.UsageDetail;

public class BillReport{

	public BillData process(String fileName){
		
		fileName="85266400998.txt";
		String filePath =this.getClass().getClassLoader().getResource("").toString().replace("file:/", "")+ "source/"+fileName;
		
		System.out.println("filePath:"+filePath);
		
		BufferedReader reader = null;
		
		String str = null;
		String[] data;
		
		BillData result=new BillData();
		
		int count = 0;
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath))); // 指定讀取文件的編碼格式，以免出現中文亂碼
			
			
			while ((str = reader.readLine()) != null) {
				data=str.split("\t");
				String s= data[0];
				
				if("I".equalsIgnoreCase(s)){
					result.setI(new Invoice(data));
				}else if("J".equalsIgnoreCase(s)){
					result.getJ().add(new InvoiceDetail(data));
				}else if("C".equalsIgnoreCase(s)){
					result.setC(new Charge(data));
				}else if("D".equalsIgnoreCase(s)){
					result.getD().add(new ChargeDetail(data));
				}else if("U".equalsIgnoreCase(s)){
					result.setU(new Usage(data));
				}else if("R".equalsIgnoreCase(s)){
					result.getR().add(new UsageDetail(data));
				}


			System.out.println(str);
				count++;
				
				if(count>10) break;
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
			
			reader.close();
			
			} catch (IOException e) {
			
			e.printStackTrace();
			
			}
		}
		
		return result;
	}
}

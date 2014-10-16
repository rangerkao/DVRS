package control;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;










import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import bill.bean.BillData;
import bill.bean.Charge;
import bill.bean.ChargeDetail;
import bill.bean.Invoice;
import bill.bean.InvoiceDetail;
import bill.bean.Usage;
import bill.bean.UsageDetail;

public class BillReport{
	
	
	private static String FileName;
	private static final String filePath =BillReport.class.getClassLoader().getResource("").toString().replace("file:/", "")+ "source/";
	
	
	public BillData process(String fileName){
		
		FileName=fileName;
		
		FileName="85266400998.txt";
		
		System.out.println("filePath:"+filePath+FileName);
		
		BufferedReader reader = null;
		
		String str = null;
		String[] data;
		
		BillData result=new BillData();
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath+FileName))); // 指定讀取文件的編碼格式，以免出現中文亂碼
			
			
			while ((str = reader.readLine()) != null) {
				data=str.split("\t");
				String s= data[0];
				
				List<String> list=new ArrayList<String>();
				for (int i = 0; i < data.length; i++) {
					list.add(data[i]);
				}

				if("I".equalsIgnoreCase(s)){
					result.setI(new Invoice(data));
				}else if("J".equalsIgnoreCase(s)){
					list.add(3, null);//OrderSequence null
					result.getJ().add(new InvoiceDetail(list));
				}else if("C".equalsIgnoreCase(s)){
					result.setC(new Charge(data));
				}else if("D".equalsIgnoreCase(s)){
					list.add(3,null);//CategorySequence null
					result.getD().add(new ChargeDetail(list));
				}else if("U".equalsIgnoreCase(s)){
					result.setU(new Usage(data));
				}else if("R".equalsIgnoreCase(s)){
					result.getR().add(new UsageDetail(list));
				}

			//System.out.println(str);
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
		System.out.println("read file "+fileName+" finish!");
		
		
		
		creatPDF(result);
		
		
		return result;
	}
	
	private void creatPDF(BillData data){
		System.out.println("Start to create bill PDF!");
		
		String templateName="bill2.jrxml";
		String PDFPath=filePath+FileName.replace("txt", "pdf");
		
		try {
			System.out.println("read file convert to jasperFile!");
			String jasperFile=JasperCompileManager.compileReportToFile(filePath+templateName);
			System.out.println("convert to jasperFile finished!");
			System.out.println("");
			
			//參數設置
			Map map=new HashMap();
			map.put("address for",
					"114\n"+
					"台北市內湖區\n"+
					"民權東路六段296巷90號15樓\n"+
					"Jason T. Wang\n先生/小姐");
			
			map.put("Statement for", " Jason T. Wang");
			map.put("Account Number", "InfoF1285*****");
			map.put("Billing Period", "04/01/2014~04/30/2014");
			map.put("Currency", "HKD");
			
			map.put("Previous Balance", new Float("779.27"));
			map.put("Payment Received", new Float("779.27"));
			
			map.put("Monthly Service Charges", new Float("688.00"));
			map.put("Usage Charges", new Float("76.72"));
			
			map.put("applied date", "May 15, 2014");
			
			map.put("D", data.getD());
			map.put("R", data.getR());

			float a=new java.lang.Float(0.00);
			
			List list=new ArrayList();
			
			
			System.out.println("jasperFile convert to jrprintFile!");
			String jrprintFile=JasperFillManager.fillReportToFile(jasperFile,map,new JREmptyDataSource());
			//String jrprintFile=JasperFillManager.fillReportToFile(jasperFile,null,new JREmptyDataSource());
			//String jrprintFile=JasperFillManager.fillReportToFile(jasperFile,map,new JRBeanCollectionDataSource(data.getD()));
			System.out.println("convert to jrprintFile finished!");
			System.out.println("");
			
			System.out.println("Creating PDF file at "+PDFPath+"!");
			JasperExportManager.exportReportToPdfFile(jrprintFile,PDFPath);
			
		} catch (JRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("read file convert to jasperFile failt!");
		}
		
		
	}
	
}

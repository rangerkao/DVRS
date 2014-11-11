<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<%@ include file="../common/TapCSS.jsp"%>
<%@ include file="../common/CSS.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
</head>
<body>
	<div class="wapper">
		<table class="wapper">
			<tr style="height: 15%">
				<td>
					<div style="height: 100%; margin: 0px; padding: 0px">
						<s:form method="post" action="billReport">
							<s:textfield name="fileName" label="檔案名稱(ex:abc.txt)"></s:textfield>
							<s:submit></s:submit>
						</s:form>
					</div>
				</td>
			</tr>
			<tr style="height: 85%; margin: 0px; padding: 0px">
				<td>
					<div class="abgne_tab"
						style="height: 100%; width: 100%; margin: 0px; padding: 0px">
						<ul class="tabs">
							<li><a href="#tab1">Invoice</a></li>
							<li><a href="#tab2">Invoice Detail</a></li>
							<li><a href="#tab3">Charge</a></li>
							<li><a href="#tab4">Charge Detail</a></li>
							<li><a href="#tab5">Usage</a></li>
							<li><a href="#tab6">Usage Detail</a></li>
						</ul>

						<div class="tab_container scroll"
							style="height: 85%; width: 100%; max-height: 400px">
							<div id="tab1" class="tab_content">
								Invoice content: <br>
								<s:property value="billData.I.Data" escape="false" />
							</div>
							<div id="tab2" class="tab_content">
								Invoice Detail: <br>
								<s:iterator value="billData.J">
									<s:property value="Data" escape="false" />
									<br>
									<br>
								</s:iterator>
							</div>
							<div id="tab3" class="tab_content">
								Charge content: <br>
								<s:property value="billData.C.Data" escape="false" />
							</div>
							<div id="tab4" class="tab_content">
								Charge Detail: <br>
								<s:iterator value="billData.D">
									<s:property value="Data" escape="false" />
									<br>
									<br>
								</s:iterator>
							</div>
							<div id="tab5" class="tab_content">
								Usage content: <br>
								<s:property value="billData.U.Data" escape="false" />
							</div>
							<div id="tab6" class="tab_content">
								Usage Detail: <br>
								<s:iterator value="billData.R">
									<s:property value="Data" escape="false" />
									<br>
									<br>
								</s:iterator>
							</div>
						</div>
					</div>
				</td>
			</tr>
		</table>
	</div>
</body>
</html>
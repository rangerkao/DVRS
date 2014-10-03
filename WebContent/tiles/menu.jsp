<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<%@ include file="../common/CSS.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
</head>
<body>
	<div class="wapper" align="center">
		<ul>
			<li><a href="<s:url action="billLink"/>">帳單匯出</a><br></li>
			<li><a href="<s:url action="logoutLink"/>">登出</a><br></li>
		</ul>
	</div>
</body>
</html>
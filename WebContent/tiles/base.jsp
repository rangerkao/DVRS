<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<%@ include file="../common/CSS.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
</head>
<body >
	<div class="wapper">
		<table style="height: 100%; width: 100%">
			<tr style="height: 20%">
				<td colspan="2" bgcolor="#AFFEFF"><tiles:insertAttribute
						name="top" /></td>
			</tr>
			<tr style="height: 80%">
				<td width="15%" bgcolor="#AFFFB0"><tiles:insertAttribute
						name="menu" /></td>
				<td width="85%" bgcolor="#AFFFD8"><tiles:insertAttribute
						name='main' />
				</td>
			</tr>
		</table>
	</div>
</body>
</html>
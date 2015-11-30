<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@ taglib uri="/struts-tags" prefix="s"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<%@ include file="../common/CSS.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
</head>
<body>
<div class="container-fluid max_height" >
	<div class="row max_height">
		<table class="col-xs-12 max_height" >
			<tr>
				<td colspan="2" bgcolor="#AFFEFF" style="height: 10%;"><tiles:insertAttribute
						name="top" /></td>
			</tr>
			<tr style="height: 90%;">
				<td width="190px" bgcolor="#AFFFB0" ><tiles:insertAttribute
						name="menu" /></td>
				<td  bgcolor="#AFFFD8" ><tiles:insertAttribute
						name='main' />
				</td>
			</tr>
		</table>
	</div>
</div>
</body>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js" ></script>
<script src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
</html>
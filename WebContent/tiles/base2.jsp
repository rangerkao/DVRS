<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<META HTTP-EQUIV="PRAGMA" CONTENT="NO-CACHE">
<!-- IE�i�ण���o���� -->
<META HTTP-EQUIV="EXPIRES" CONTENT="0">
<!-- �]�w�����W�N�L�� -->
<META HTTP-EQUIV="CACHE-CONTROL" CONTENT="NO-CACHE">
<!-- �P�Ĥ@��O�P�˪��@�� -->
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
				<td  bgcolor="#AFFFD8"><tiles:insertAttribute
						name='main' />
				</td>
			</tr>
		</table>
	</div>
</div>
</body>
</html>
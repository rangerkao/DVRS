<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script language="JavaScript">
    window.location.replace("<s:url action="DVRSLink"/>");
</script>
</head>
<body>
<div class="container-fluid max_height" >
	<div class="row max_height" align="center" style="vertical-align: middle;">
		<h4>請選擇進入的系統</h4>
		<div style="width: 50%">
			<a id="DVRS" href=<s:url action='DVRSLink'/>  class="btn btn-primary btn-lg btn-block" >DVRS</a>
		</div>
		
	</div>
</div>
<%-- 待實做權限 --%>


</body>
</html>
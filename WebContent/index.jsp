<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head><s:head></s:head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<style type="text/css">
.warning{
color: red ;
font-size: 20px
}


</style>
</head>
<body>

<s:if test="%{tag==null||tag==''}">
	<s:label>Please login to continue</s:label><br/>
</s:if>
<s:else>
	<s:label for="name">
		<font class="warning">
			<s:property value="tag" />
		</font>
	</s:label>
</s:else>
<br/>

<s:form action="login" method="post">
	<s:textfield name="userid"   label="帳號" placeholder="Userid"></s:textfield>
	<s:password name="password"  label="密碼" placeholder="Password"></s:password>
	<s:submit></s:submit>
</s:form>
</body>
</html>
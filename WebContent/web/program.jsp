<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
function execute(){
	alert($('input[name=act]:checked').val());

	$.ajax({
	      url: '<s:url action="executeProgram"/>',
	      data: {"filename":$('input[name=act]:checked').val()},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) { $("#result").html(json); },
	      error: function() { alert('something bad happened'); }
	    });
}
</Script>
</head>
<body>
<div>
	程式執行管理頁面<br>
	<!-- <input type="text" id="filename"> -->
	<input type="radio" name="act" value="test.bat" checked="checked">test <br>
	<input type="radio" name="act" value="run.bat" >run<br>
	<input type="radio" name="act" value="terminate.bat" >terminate<br>
	<input type="button" value="執行" onclick="execute()">
	<label style="width: 500px;height: 300px;" id="result"></label>
</div>
</body>
</html>
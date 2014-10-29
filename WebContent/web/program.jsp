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
$( document ).ready(function(){
	
});


function execute(){
	
	$("#result").html("正在執行，請稍待...");
	var filename;
	
	if($('input[name=act]:checked').val()=='filename'){
		filename=$("#filename").val();
	}else{
		filename="show.sh "+$('input[name=act]:checked').val();
	}
	
	//alert(filename);
	$.ajax({
	      url: '<s:url action="executeProgram"/>',
	      data: {"filename":filename},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) { $("#result").html(json); },
	      error: function() { alert('something bad happened'); }
	    });
};


</Script>
</head>
<body>
<div align="center">
	程式執行管理頁面<br>
	<div align="left" style="width: 200px">
		<input type="radio" name="act" value="info" >info<br>
		<input type="radio" name="act" value="status" >status<br>
		<input type="radio" name="act" value="start" >start<br>
		<input type="radio" name="act" value="stop" >stop<br>
		<input type="radio" name="act" value="filename" ><input type="text" id="filename"><br>
		<input type="button" value="執行" onclick="execute()"><br>
	</div>
	<div style="width: 80%;max-height: 300px;height: 300px; overflow: auto;" align="left">
		<br>
		<label style="width: 100%;height: 100%" id="result"></label>
	</div>
</div>
</body>
</html>
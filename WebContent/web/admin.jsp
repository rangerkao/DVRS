<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<style type="text/css">

#table1, #table1 td, #table1 th {
	border:1px solid #FF7573;
	border-color: #FF7573
}
#table1 td {
	align:center
}
.datatable{
	width:50%;
	background-color: #FF7573
}
.odd_columm{
	background-color: #FEFFAF
}
.even_columm{
	background-color: #C7FF91
}

</style>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script type="text/javascript">
	function queryAdmin(){
		alert("button is clicked!");
		 $.ajax({
		      url: '<s:url action="queryAdmin"/>',
		      data: {}, //parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json) {  
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  alert(json);
		    	  var list=$.parseJSON(json);
		    	  alert("j[0]"+list[0].role);
		    	  $("#table1 tr:gt(0)").remove();//移除>0之後讀tr
		    	  
		    	    $.each(list,function(i,admin){  
                      var _tr = $(	"<tr>"+
                      					"<td align='center'>"+admin.userid+"</td>"+
                      					"<td align='center'>"+admin.account+"</td>"+
                      					"<td align='center'>"+admin.password+"</td>"+
                      					"<td align='center'>"+admin.role+"</td>"+
                      				"</tr>");  
                      
                    $("#table1").append(_tr); });
		    	    $("#table1 tr:odd").addClass("odd_columm");//奇數欄位樣式
		    	    $("#table1 tr:even").addClass("even_columm");
		    	  },
		      error: function() { alert('something bad happened'); }
		    });
	}
</script>
</head>
<body>
<div align="center">
	<div><button onclick="queryAdmin()">查詢</button></div>
	<div >
		<table id="table1" class="datatable" align="center">
			<tr class="even_columm" >
				<td class="columnLabel" align="center">USERID</td>
				<td class="columnLabel" align="center">ACCOUNT</td>
				<td class="columnLabel" align="center">PASSWORD</td>
				<td class="columnLabel" align="center">ROLE</td>
			</tr>
		</table>
	</div>
</div>
</body>
</html>
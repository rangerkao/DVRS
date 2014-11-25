<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
$(document).ready(function(){
	queryDataRate();
});
	function queryDataRate(){
		$("#Qmsg").html("正在查尋，請稍待...");
		$.ajax({
	      url: '<s:url action="queryDataRate"/>',
	      data: {}, //parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  $("#table1 tr:gt(0)").remove();//移除>0之後讀tr
	    	  	adminList=list;
	    	    $.each(list,function(i,dataRate){  
                 var _tr = $(	"<tr align='center'>"+
                 					"<td>"+dataRate.pricePlanId+"</td>"+
                 					"<td>"+dataRate.mccmnc+"</td>"+
                 					"<td>"+dataRate.rate+"</td>"+
                 					"<td>"+dataRate.chargeunit+"</td>"+
                 					"<td>"+dataRate.currency+"</td>"+
                 					//"<td align='center'><button onclick='chooseRow(this)'>選擇</button></td>"+
                 				"</tr>");  
                 
               $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//奇數欄位樣式
	    	    $("#table1 tr:even").addClass("even_columm");
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); }
	    });
	}


</script>

<title>Insert title here</title>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<h3>費率查詢頁面</h3>
		<div class="col-xs-12">
			<label id="Qmsg" style="height: 30px;">&nbsp;</label>
		</div>
		<div class="col-xs-12">
			<table  align="center" class="table-bordered" class="col-xs-10">
				<tr class="even_columm" >
					<td class="columnLabel" align="center" width="20%">資費ID</td>
					<td class="columnLabel" align="center" width="20%">MCCMNC</td>
					<td class="columnLabel" align="center" width="20%">費率</td>
					<td class="columnLabel" align="center" width="20%">計價單位(KB)</td>
					<td class="columnLabel" align="center" width="20%">幣別</td>
				</tr>
				<tr>
					<td colspan="10" >
						<div style="height: 500px;overflow: auto;">
						<table id="table1" class="table-bordered table-hover col-xs-12">
							<tr>
								<td width="20%"></td>
								<td width="20%"></td>
								<td width="20%"></td>
								<td width="20%"></td>
								<td width="20%"></td>
							</tr>
						</table>
						</div>
					</td>
				</tr>
			</table>
		</div>
	</div>
</div>
</body>
</html>
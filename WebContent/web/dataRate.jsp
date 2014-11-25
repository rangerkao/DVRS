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
		$("#Qmsg").html("���b�d�M�A�еy��...");
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
	    	  $("#table1 tr:gt(0)").remove();//����>0����Ūtr
	    	  	adminList=list;
	    	    $.each(list,function(i,dataRate){  
                 var _tr = $(	"<tr align='center'>"+
                 					"<td>"+dataRate.pricePlanId+"</td>"+
                 					"<td>"+dataRate.mccmnc+"</td>"+
                 					"<td>"+dataRate.rate+"</td>"+
                 					"<td>"+dataRate.chargeunit+"</td>"+
                 					"<td>"+dataRate.currency+"</td>"+
                 					//"<td align='center'><button onclick='chooseRow(this)'>���</button></td>"+
                 				"</tr>");  
                 
               $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//�_�����˦�
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
		<h3>�O�v�d�߭���</h3>
		<div class="col-xs-12">
			<label id="Qmsg" style="height: 30px;">&nbsp;</label>
		</div>
		<div class="col-xs-12">
			<table  align="center" class="table-bordered" class="col-xs-10">
				<tr class="even_columm" >
					<td class="columnLabel" align="center" width="20%">��OID</td>
					<td class="columnLabel" align="center" width="20%">MCCMNC</td>
					<td class="columnLabel" align="center" width="20%">�O�v</td>
					<td class="columnLabel" align="center" width="20%">�p�����(KB)</td>
					<td class="columnLabel" align="center" width="20%">���O</td>
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
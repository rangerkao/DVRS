<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css">
<script src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
<script type="text/javascript">
$(function() {
    $(".datapicker").datepicker({
        showOn: "button",
        buttonImage: "source/icon.png",
        buttonImageOnly: true,
        buttonText: "Select date",
        dateFormat: 'yy-mm-dd'
    });
    var Today=new Date();
	var ds=Today.getFullYear()+ "-" + (Today.getMonth()+1) + "-" + Today.getDate();
	$("#dateFrom").val(ds);
	$("#dateTo").val(ds);
  });
  
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}
var CDRList;
	function query(){
		if(!validate()) return false;
		$("#Qmsg").html("正在查尋，請稍待...");
		$.ajax({
	      url: '<s:url action="queryCDR"/>',
	      data: {	"from":$("#dateFrom").val(),
  	  				"to":$("#dateTo").val(),
  	  				"IMSI":$("#IMSI").val()
  	  		},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  $("#table1 tr:gt(0)").remove();//移除>0之後讀tr
	    	  CDRList=list;
	    	    $.each(list,function(i,CDR){  
                 var _tr = $(	"<tr align='center'>"+
                 					"<td>"+CDR.usageId+"</td>"+
                 					"<td>"+CDR.imsi+"</td>"+
                 					"<td>"+CDR.calltime+"</td>"+
                 					"<td>"+CDR.mccmnc+"</td>"+
                 					"<td>"+CDR.sgsnAddress+"</td>"+
                 					"<td>"+CDR.dataVolume+"</td>"+
                 					"<td>"+CDR.fileId+"</td>"+
                 					//"<td align='center'><button onclick='chooseRow(this)'>選擇</button></td>"+
                 				"</tr>");  
                 
               $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//奇數欄位樣式
	    	    $("#table1 tr:even").addClass("even_columm"); 
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
  	  		beforeSend:function(){
  	  		$("#Qmsg").html("正在查尋，請稍待...");
  			disableButton();
	        },
	        complete:function(){
	      	  enableButton();
	        }
	    });
	}
	function validate(){
		var validation=true;
		if((($("#dateFrom").val()==null||$("#dateFrom").val()=="")^($("#dateTo").val()==null||$("#dateTo").val()==""))){
			alert("日期必須同時填或皆不填！")
			validation=false;
		}else
		if($("#dateFrom").val()>$("#dateTo").val()){
			alert("開始日期不可大於結束日期！")
			validation=false;
		}
		return validation;
	}
	function clearDate(){
		//alert("clear clicked");
		$("#dateFrom").val("");
		$("#dateTo").val("");
		$("#IMSI").val("");
	}
</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<form class="form-horizontal" >
		<h4>CDR查詢頁面</h4>
			<div class="col-xs-4" align="right">查詢期間從</div>
			<div class="col-xs-8" align="left"><input type="text"  disabled="disabled" id="dateFrom" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px ">到<input type="text" disabled="disabled" id="dateTo" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px" ></div>
			<div class="col-xs-4" align="right"><label for="IMSI">IMSI:</label></div>
			<div class="col-xs-2" align="left"><input type="text" id="IMSI" /></div>	
			<div class="btn-group col-xs-6">
				<input type="button" class="btn btn-primary btn-sm" onclick="query()" value="查詢">
				<input type="button" class="btn btn-primary btn-sm" onclick="clearDate()" value="清除">
			</div>
			<div class="col-xs-12"><label id="Qmsg" style="height: 30px;">&nbsp;</label></div>	
		</form>
		<div>
			<table class="table-bordered" align="center" style="width: 70%" >
				<tr class="even_columm" >
					<td class="columnLabel" align="center" width="14%">CDRID</td>
					<td class="columnLabel" align="center" width="14%">IMSI</td>
					<td class="columnLabel" align="center" width="16%">使用時間</td>
					<td class="columnLabel" align="center" width="14%">MCCMNC</td>
					<td class="columnLabel" align="center" width="14%">SGSN位置</td>
					<td class="columnLabel" align="center" width="14%">資料用量</td>
					<td class="columnLabel" align="center" width="14%">檔案ID</td>
				</tr>
				<tr>
					<td colspan="10" >
						<div style="height: 500px;overflow: auto;">
						<table id="table1" class="table-bordered table-hover col-xs-12" align="center">
							<tr height="0px">
								<td width="14%"></td>
								<td width="14%"></td>
								<td width="16%"></td>
								<td width="14%"></td>
								<td width="14%"></td>
								<td width="14%"></td>
								<td width="14%"></td>
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
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
  });
var dataList;
var reportName="超量簡訊發送表";
var tHead=[{name:"紀錄ID",col:"id",_width:"10%"},
           {name:"接收門號",col:"sendNumber",_width:"15%"},
           {name:"簡訊ID",col:"msg",_width:"20%"},
           {name:"發送時間",col:"sendDate",_width:"20%"},
           {name:"發送結果",col:"result",_width:"20%"},
           {name:"記錄時間",col:"createDate",_width:"15%"}];
           
function query(){
	if(!validate()) return false;
	$("#Qmsg").html("正在查尋，請稍待...");
	$.ajax({
	      url: '<s:url action="querySMSLog"/>',
	      data: {	"dateFrom":$("#dateFrom").val(),
	    	  		"dateTo":$("#dateTo").val(),
	    	  		"msisdn":$("#MSISDN").val()
	    	  		},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  	dataList=list;
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened');  
	      },
	      beforeSend:function(){
    		  $("#Qmsg").html("正在查詢，請稍待...");
    			disableButton();
          },
          complete:function(){
        	  enableButton();
        	  pagination();
          }
	    });
}

function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
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
	alert("clear clicked");
	$("#dateFrom").val("");
	$("#dateTo").val("");
}

</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<h3>超量簡訊發送查詢頁面</h3>
		<div class="col-xs-4" align="right">查詢期間從</div>
		<div class="col-xs-8" align="left">
			<input type="text"  disabled="disabled" id="dateFrom" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px ">
			到
			<input type="text" disabled="disabled" id="dateTo" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px" >
		</div>
		<div class="col-xs-4" align="right"><label>門號:</label> </div>
		<div class="col-xs-8" align="left"><input type="text" id="MSISDN"></div>
		<div class="col-xs-12">
			<div class="btn-group" >
				<input type="button" class="btn btn-primary btn-sm" onclick="query()" value="查詢">
				<input type="button" class="btn btn-primary btn-sm" onclick="clearDate()" value="清除">
				<input type="button" class="btn btn-primary btn-sm" onclick="createExcel()" value="下載Excel"> 
			</div>
		</div>
		<div class="col-xs-12"><label id="Qmsg" style="height: 30px;">&nbsp;</label></div>
		<div class="col-xs-12"> 
			<button type="button" name="Previous"  class="pagination btn btn-warning"><span class="glyphicon glyphicon-chevron-left"></span> Previous</button>
			<label id="nowPage"></label>
			<button type="button" name="Next" class="pagination btn btn-warning"> <span class="glyphicon glyphicon-chevron-right"></span> Next</button>
			<label id="totalPage" style="margin-right: 10px"></label>
			<label>每頁筆數</label>
			<input id="rown" type="text" value="10" width="5px">
			<input type="button" onclick="pagination()" class="btn btn-primary btn-sm" style="margin: 20px"  value="重新分頁">
		</div>
		<div class="col-xs-12"> 
			<div id="page_contain"></div>
		</div>
	</div>
</div>
</body>
</html>
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
var dataList;
var dateChange;
var currenList;
$(document).ready(function(){
	dateChange=true;
	$(".datapicker").datepicker({
        showOn: "button",
        buttonImage: "source/icon.png",
        buttonImageOnly: true,
        buttonText: "Select date",
        dateFormat: 'yy-mm-dd'
    });
    var Today=new Date();
    var month=Today.getMonth()+1;
    
    if(month<10)
    	month="0"+month;
    
    var Day=Today.getDate();
    if(Day<10)
    	Day="0"+Day;
    
    
    
	var ds=Today.getFullYear()+ "-" + month + "-" + Day;
	$("#dateFrom").val(ds);
	$("#dateTo").val(ds);
});
	function queryCurrentDay(){
		
		if(!dateChange){
			queryList();
			return;
		}
		
		if(!validate()){
			return;
		}
		$.ajax({
	      url: '<s:url action="queryCurrentDay"/>',
	      data: {	//"imsi":$("#imsi").val(),
	    	  		"from":$("#dateFrom").val(),
  					"to":$("#dateTo").val()}, //parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  currenList=list;
	    	  dataList=currenList.slice(0);
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
	      beforeSend:function(){
    		  $("#Qmsg").html("正在查尋，請稍待...");
    			disableButton();
          },
          complete:function(){
        	  enableButton();
        	  //pagination();
        	  queryList();
        	  dateChange=false;
          }
	    });
	}
	var tHead=[{name:"累計日期",col:"day",_width:"9%"},
	           {name:"IMSI",col:"imsi",_width:"10%"},
	           {name:"累計費用",col:"charge",_width:"9%"},
	           {name:"最後累計檔案ID",col:"lastFileId",_width:"9%"},
	           {name:"最後使用時間",col:"lastDataTime",_width:"9%"},
	           {name:"累計流量(byte)",col:"volume",_width:"9%"},
	           {name:"更新時間",col:"updateDate",_width:"9%"},
	           {name:"建立時間",col:"createDate",_width:"9%"},
	           {name:"國家業者",col:"mccmnc",_width:"9%"},
	           {name:"是否發送過每日警示",col:"alert",_width:"9%"}];
	
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
		//alert("clear clicked");
		$("#dateFrom").val("");
		$("#dateTo").val("");
		$("#imsi").val("");
	}
	
	function queryList(){
		var   reg=$("#imsi").val();
		reg="^"+reg+"$"
		reg=reg.replace("*","\\d+");
		reg=new RegExp(reg);	
		
		dataList.splice(0,dataList.length);
		 $.each(currenList,function(i,ListItem){
			 if(reg.test(ListItem.imsi)||($("#imsi").val()==null||$("#imsi").val()=="")){
				 dataList.push(ListItem);
			 }
		}); 
		pagination();
	}
</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<h3>單日累計查詢</h3>
		<div class="col-xs-4" align="right">查詢期間從</div>
		<div class="col-xs-8" align="left"><input type="text"  disabled="disabled" id="dateFrom" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px " onchange="dateChange=true">
		到
		<input type="text" disabled="disabled" id="dateTo" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px" onchange="dateChange=true"></div>
		<div class="col-xs-4" align="right"><label for="imsi">IMSI:</label></div>
		<div class="col-xs-2" align="left"><input type="text" id="imsi" /></div>
		<div class="btn-group col-xs-6">
				<input type="button" class="btn btn-primary btn-sm" onclick="queryCurrentDay()" value="查詢">
				<input type="button" class="btn btn-primary btn-sm" onclick="clearDate()" value="清除">
			</div>
		<div class="col-xs-12">
			<font size="2" color="red">(查詢IMSI時可使用"*"取代某區段號碼進行模糊查詢)</font>
			<label id="Qmsg" style="height: 30px;">&nbsp;</label>
		</div>
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
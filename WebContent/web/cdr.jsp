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
	
	dateChange=true;
	
  });
  
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}
var dateChange;
var dataList;
var cdrList;
	function query(){

/* 		if(!dateChange){
			queryList();
			return;
		} */
		
		if(!validate()) return false;
		
		
		
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
				
				var list=$.parseJSON(json);
				cdrList=list['data'];
				if(cdrList!=null)
					dataList=cdrList.slice(0);
				
				var error = list['error'];
		    	  $('#Error').html(error);
    	  	},
	      	error: function() { 
	      		$("#Qmsg").html('something bad happened'); 
	      	},
  			beforeSend:function(){
	  	  		$("#Qmsg").html("正在查詢，請稍待...");
	    		$('#Error').html("");
	    		dataList=[];
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
	
	var tHead=[{name:"CDRID",col:"usageId",_width:"14%"},
	           {name:"IMSI",col:"imsi",_width:"14%"},
	           {name:"使用時間",col:"calltime",_width:"16%"},
	           {name:"MCCMNC",col:"mccmnc",_width:"14%"},
	           {name:"SGSN位置",col:"sgsnAddress",_width:"14%"},
	           {name:"資料用量",col:"dataVolume",_width:"14%"},
	           {name:"檔案ID",col:"fileId",_width:"14%"}];
	
	function validate(){
		var validation=true;
		
		if($("#IMSI").val()==null||$("#IMSI").val()==""){
			alert("IMSI為必填！")
			validation=false;
		}
		
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
	
	function queryList(){
		var   reg=$("#IMSI").val();
		reg="^"+reg+"$"
		reg=reg.replace("*","\\d+");
		reg=new RegExp(reg);	
		
		dataList.splice(0,dataList.length);
		 $.each(cdrList,function(i,ListItem){
			 if(reg.test(ListItem.imsi)||($("#IMSI").val()==null||$("#IMSI").val()=="")){
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
		<form class="form-horizontal" >
		<h4>CDR查詢頁面</h4>
			<div class="col-xs-4" align="right">查詢期間從</div>
			<div class="col-xs-8" align="left"><input type="text"  disabled="disabled" id="dateFrom" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px " onchange="dateChange=true">到<input type="text" disabled="disabled" id="dateTo" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px" onchange="dateChange=true"></div>
			<div class="col-xs-4" align="right"><label for="IMSI">IMSI:</label></div>
			<div class="col-xs-2" align="left"><input type="text" id="IMSI" /></div>	
			<div class="btn-group col-xs-6">
				<input type="button" class="btn btn-primary btn-sm" onclick="query()" value="查詢">
				<input type="button" class="btn btn-primary btn-sm" onclick="clearDate()" value="清除">
			</div>
			<div class="col-xs-12"><font size="2" color="red">(查詢IMSI時可使用"*"取代某區段號碼進行模糊查詢)</font><label id="Qmsg" style="height: 30px;">&nbsp;</label></div>	
		</form>
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
		<div class="col-xs-12" align="left"> 
			<div id="Error"></div>
		</div>
	</div>
</div>
</body>
</html>
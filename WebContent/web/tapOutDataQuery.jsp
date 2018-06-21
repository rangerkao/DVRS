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

var today=new Date();
var currentDateTime =
    today.getFullYear()+"/"+
    (today.getMonth()+1)+"/"+
    today.getDate()+"_"+
    today.getHours()+':'+today.getMinutes()+":"+today.getSeconds();

$(function() {
	
    $(".datapicker").datepicker({
        showOn: "button",
        buttonImage: "source/icon.png",
        buttonImageOnly: true,
        buttonText: "Select date",
        dateFormat: 'yy-mm-dd'
    });
    
    
    
    
  });
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}
var dataList;
var tapOutList;
var reportName="TapOut匯出_"+currentDateTime;

var tHead=[
	{name:"始話日期",col:"startDate",_width:"10%"},
    {name:"漫遊網",col:"location",_width:"10%"},
    {name:"發話號碼/收話號碼",col:"phonenumber",_width:"15%"},
    {name:"通話種類",col:"type",_width:"6%"},
    {name:"始話時刻",col:"startTime",_width:"10%"},
    {name:"終話時刻",col:"endTime",_width:"10%"},
    {name:"使用量(秒/則/Bytes)",col:"unit",_width:"11%"},
    {name:"漫遊費用",col:"amount",_width:"7%"},
    {name:"原始費用",col:"totalCharge",_width:"7%"},
    {name:"優惠費用",col:"discountCharge",_width:"7%"},
    {name:"結果費用",col:"finalCharge",_width:"7%"}
    ];
    
    //始話日期	漫遊網	發話號碼\收話號碼	通話種類	始話時刻	終話時刻	通話秒數/則數	漫遊網話費	轉接至漫遊網國際話費	費用類別

var preType;
    
function query(){
	if(!validate()) return flase;
	
	 preType = $('input[name=type]:checked').val();
	
	 $.ajax({
	      url: '<s:url action="queryTapOutData"/>',
	      data: {	"from":$("#dateFrom").val(),
	    	  		"to":$("#dateTo").val(),
	    	  		"phonenumber":$("#phonenumber").val(),
	    	  		"type":$('input[name=type]:checked').val()
	    	  		
	    	  		},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var list=$.parseJSON(json);
	    	  if(list['error']){
	    		  var error = list['error'];
	    		  if(error.includes("縮小範圍")){
	    			  $('#Error').html("結果筆數過大，請縮小範圍後重新查詢。");
	    			  alert("結果筆數過大，請縮小範圍後重新查詢。");
	    		  }else{
	    			  $('#Error').html(error);
	    		  }
	    		  
	    	  }else{
	    		  $("#Qmsg").html("Success");
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
					tapOutList = list['data'];
		    	  	if(tapOutList!=null)
		    		  dataList=tapOutList.slice(0);

	    	  }
 
	    	},
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
	      beforeSend:function(){
	    	  $("#Qmsg").html("正在查詢，請稍待...");
	    		$('#Error').html("");
	    		dataList=[];
	    		disableButton();
          },
          complete:function(){
        	  enableButton();
        	  pagination();
          }
	    }); 
}

function validate(){
	var validation=true;
	/* if((($("#dateFrom").val()==null||$("#dateFrom").val()=="")^($("#dateTo").val()==null||$("#dateTo").val()==""))){
		alert("日期必須同時填或皆不填！")
		validation=false;
	}else */
	if( ($("#dateFrom").val()==null||$("#dateFrom").val()=="")||
			($("#dateTo").val()==null||$("#dateTo").val()=="") ){
		alert("日期為必填欄位！");
		validation=false;
	}
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
function typeselected(type){
	if(preType!="all") return;
	
	dataList = [];
	console.log(type);
	if(type == 'all'){
		dataList=tapOutList.slice(0);
	}else{
		var condition;
		if(type == 'voice'){
			condition = function(item){
				if(item.type=='R8' || item.type=='RA' || item.type=='RC' || item.type=='RE') return true;
				else return false;
			};
			
		}else if(type == 'sms'){
			condition = function(item){
				if(item.type=='MJ') return true;
				else return false;
			};

		}else if(type == 'data'){
			condition = function(item){
				if(item.type=='PC') return true;
				else return false;
			};
		}

		$.each(tapOutList,function(i,ListItem){
			
			if(condition(ListItem)){
				dataList.push(ListItem);
			}
		});
	}
	
	pagination();
}
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}


function createTapOutExcel(){
	if(!validate()) return flase;
	
	var param = {	
			"from":$("#dateFrom").val(),
	  		"to":$("#dateTo").val(),
	  		"phonenumber":$("#phonenumber").val(),
	  		"type":$('input[name=type]:checked').val()
	  		}
	$("[name='param']").val(encodeURI(JSON.stringify(param)));
	$("[name='tapOut_colHead']").val(encodeURI(JSON.stringify(tHead)));
	$("[name='tapOut_reportName']").val(encodeURI(reportName));
	$("#tapOut_reportFrom").submit();	
}

</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<h3>TapOut查詢</h3>
		<div class="col-xs-4" align="right">查詢期間從</div>
		<div class="col-xs-8" align="left">
			<input type="text"  disabled="disabled" id="dateFrom" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px ">
			到
			<input type="text" disabled="disabled" id="dateTo" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px" >
			<input type="button" class="btn btn-primary btn-sm" onclick="createTapOutExcel()" value="下載Excel"> 
		</div>
		<div class="col-xs-4" align="right">用戶香港號碼</div>
		<div class="col-xs-8" align="left">
			<input type="text" id="phonenumber">
			<span  align="left" >類型：	
				<input type="radio" name="type" value="all"  onclick="typeselected('all')" checked="checked"  >所有
				<input type="radio" name="type" value="voice" onclick="typeselected('voice')" >語音
				<input type="radio" name="type" value="sms" onclick="typeselected('sms')">簡訊
				<input type="radio" name="type" value="data" onclick="typeselected('data')">數據
			</span>
		
		</div>
		
		<div class="col-xs-12">
			<div class="btn-group" >
				<input type="button" class="btn btn-primary btn-sm" onclick="query()" value="查詢">
				<input type="button" class="btn btn-primary btn-sm" onclick="clearDate()" value="清除">
			</div>
		</div>
		<div class="col-xs-12" align="center">
			RA：受話。R8：發話。RC：國際轉接。RE：回國轉接。MJ：訊息。PC：數據
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
		<div class="col-xs-12" align="left"> 
			<div id="Error"></div>
		</div>
		
		
		<form action="createTapOutExcel" method="post" target="tapOut_sub_iframe" id="tapOut_reportFrom" style="display: none;">
			<input type="text" name="param">
			<input type="text" name="tapOut_colHead">
			<input type="text" name="tapOut_reportName">
		</form>
		<iframe name="tapOut_sub_iframe" width="0" height="0" style="display: none;"></iframe>
	</div>
</div>



</body>
</html>
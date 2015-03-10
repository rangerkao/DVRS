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
});
var historyList;
var dataList;
var tHead=[{name:"舊號",col:"oldvalue",_width:"25%"},
           {name:"新號",col:"newvalue",_width:"25%"},
           {name:"狀態",col:"ststus",_width:"25%"},
           {name:"完成時間",col:"completedate",_width:"25%"}];
var reportName="換卡歷史";
	function queryDataRate(){
		var   reg=$("#imsi").val();
		reg="^"+reg+"$"
		reg=reg.replace("*","\\d+");
		reg=new RegExp(reg);	
		
		$.ajax({
	      url: '<s:url action="queryCardChangeHistory"/>',
	      data: {"imsi" : reg }, //parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  historyList=list;
	    	  dataList=historyList.slice(0);
    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
	      beforeSend:function(){
    		  $("#Qmsg").html("正在查尋，請稍待...");
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
	function queryList(){

		var   reg=$("#imsi").val();
		reg="^"+reg+"$"
		reg=reg.replace("*","\\d+");
		reg=new RegExp(reg);	
		
		dataList.splice(0,dataList.length);
		 $.each(historyList,function(i,ListItem){
			 if(reg.test(ListItem.oldvalue)||reg.test(ListItem.newvalue)||($("#imsi").val()==null||$("#imsi").val()=="")){
				 dataList.push(ListItem);
			 }
		}); 
		pagination();
	}
</script>
<title>Insert title here</title>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<h3>換卡歷史查詢頁面</h3>
		<div class="col-xs-5" align="right"><label>IMSI:</label></div>
		<div class="col-xs-7" align="left"><input type="text" id="imsi"></div>
		<div class="col-xs-12" align="center"><input type="button" value="查詢" onclick="queryDataRate()" class="btn btn-primary btn-sm"></div>
		<div class="col-xs-12"><font size="2" color="red">(查詢IMSI時可使用"*"取代某區段號碼進行模糊查詢)</font><label id="Qmsg" style="height: 30px;">&nbsp;</label></div>
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
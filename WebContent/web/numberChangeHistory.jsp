<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<script type="text/javascript">
$(document).ready(function(){
});
var dataList;
var historyList;
var tHead=[{name:"Order_id",col:"orderid",_width:"20%"},
           {name:"舊號",col:"oldvalue",_width:"20%"},
           {name:"新號",col:"newvalue",_width:"20%"},
           {name:"狀態",col:"ststus",_width:"20%"},
           {name:"完成時間",col:"completedate",_width:"20%"}];
var reportName="換號歷史";
	function queryDataRate(){
		/* var   reg=$("#msisdn").val();
		reg="^"+reg+"$"
		reg=reg.replace("*","\\d+");
		reg=new RegExp(reg); */
		
		$.ajax({
	      url: '<s:url action="queryNumberChangeHistory"/>',
	      data: {
	    	  "imsi" : $("#msisdn").val()
	    	  
	      }, //parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      cache: false,
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  historyList=list['data'];
	    	  if(historyList!=null)
	    		  dataList=historyList.slice(0);
	    	  
	    	  var error = list['error'];
	    	  $('#Error').html(error);
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
	function disableButton(){
		$(':button').attr('disabled', 'disabled');
	}
	function enableButton(){
		$(':button').removeAttr('disabled'); //.attr('disabled', '');
	}
	
	function queryList(){
		var   reg=$("#msisdn").val();
		reg="^"+reg+"$"
		reg=reg.replace("*","\\d+");
		reg=new RegExp(reg);	
		
		dataList.splice(0,dataList.length);
		 $.each(historyList,function(i,ListItem){
			 if(reg.test(ListItem.oldvalue)||reg.test(ListItem.newvalue)||($("#msisdn").val()==null||$("#msisdn").val()=="")){
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
		<h3>換號歷史查詢頁面</h3>
		<div class="col-xs-5" align="right"><label>門號:</label></div>
		<div class="col-xs-7" align="left"><input type="text" id="msisdn"></div>
		<div class="col-xs-12" align="center"><input type="button" value="查詢" onclick="queryDataRate()" class="btn btn-primary btn-sm"></div>
		<div class="col-xs-12">
			<font size="2" color="red">(查詢門號時可使用"*"取代某區段號碼進行模糊查詢)</font>
			<label id="Qmsg" style="height: 30px;">&nbsp;</label>
		</div>
		<div class="col-xs-12" id="page_action"> </div>	

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
<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<script type="text/javascript">
$(document).ready(function(){
	queryDataRate();
});
var dataList;
var tHead=[{name:"資費ID",col:"pricePlanId",_width:"5%"},
           {name:"資費名稱",col:"pricePlanName",_width:"25%"},
           {name:"MCCMNC",col:"mccmnc",_width:"15%"},
           {name:"國家",col:"country",_width:"10%"},
           {name:"網路業者",col:"netWork",_width:"15%"},
           {name:"費率",col:"rate",_width:"5%"},
           {name:"計價單位(KB)",col:"chargeunit",_width:"10%"},
           {name:"幣別",col:"currency",_width:"5%"},
           {name:"每日上限",col:"dayCap",_width:"10%"}];
var reportName="資費表";
	function queryDataRate(){
		$.ajax({
	      url: '<s:url action="queryDataRate"/>',
	      data: {}, //parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      cache: false,
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  dataList=list['data'];
	    	  
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
			<input type="button" class="btn btn-primary btn-sm" onclick="createExcel()" value="下載Excel"> 
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
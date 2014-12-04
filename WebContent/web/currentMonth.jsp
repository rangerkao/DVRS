<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
var dataList;
$(document).ready(function(){
});
	function queryCurrentMonth(){
		$.ajax({
	      url: '<s:url action="queryCurrentMonth"/>',
	      data: {"imsi":$("#imsi").val()}, //parameters go here in object literal form
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
	var tHead=[{name:"IMSI",col:"imsi",_width:"12%"},
	           {name:"累計費用",col:"charge",_width:"8%"},
	           {name:"最後累計檔案ID",col:"lastFileId",_width:"8%"},
	           {name:"發送簡訊次數",col:"smsTimes",_width:"8%"},
	           {name:"最後使用時間",col:"lastDataTime",_width:"8%"},
	           {name:"累計流量",col:"volume",_width:"8%"},
	           {name:"統計月份",col:"month",_width:"8%"},
	           {name:"更新時間",col:"updateDate",_width:"8%"},
	           {name:"建立時間",col:"createDate",_width:"8%"},
	           {name:"是否曾中斷數據",col:"everSuspend",_width:"8%"},
	           {name:"最後警示額度",col:"lastAlertThreshold",_width:"8%"},
	           {name:"最後警示流量",col:"lastAlertVolume",_width:"8%"}];
	
	function disableButton(){
		$(':button').attr('disabled', 'disabled');
	}
	function enableButton(){
		$(':button').removeAttr('disabled'); //.attr('disabled', '');
	}
</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<h3>每月累計頁面</h3>
		<div class="col-xs-5" align="right"><label>IMSI:</label></div>
		<div class="col-xs-7" align="left"><input type="text" id="imsi"></div>
		<div class="col-xs-12" align="center"><input type="button" value="查詢" onclick="queryCurrentMonth()" class="btn btn-primary btn-sm"></div>
		<div class="col-xs-12">
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
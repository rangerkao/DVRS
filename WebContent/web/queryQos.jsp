<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title></title>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
	var tHead=[{name:"provision ID",col:"provisionID",_width:"20%"},
	           {name:"IMSI",col:"imsi",_width:"20%"},
	           {name:"門號",col:"msisdn",_width:"15%"},
	           {name:"動作",col:"action",_width:"5%"},
	           {name:"方案",col:"plan",_width:"5%"},
	           {name:"連線結果",col:"returnCode",_width:"10%"},
	           {name:"供裝結果",col:"resultCode",_width:"10%"},
	           {name:"供裝時間",col:"createTime",_width:"15%"}];
           
	$(function() {
	  
	  });
  
	function disableButton(){
		$(':button').attr('disabled', 'disabled');
	}
	function enableButton(){
		$(':button').removeAttr('disabled'); //.attr('disabled', '');
	}
	
	var dataList;
	var qosList;
	
	function query(){
		
		$.ajax({
	      url: '<s:url action="queryQos"/>',
	      data: {	"imsi":($("#IMSI").val()!=null?$("#IMSI").val().replace("*","%"):""),
  	  				"msisdn":($("#msisdn").val()!=null?$("#msisdn").val().replace("852",""):""),
  	  		},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);

	    	  var list=$.parseJSON(json);
	    	  QosList=list['data'];
	    	  if(QosList!=null)
	    		  dataList=QosList.slice(0);
	    	  
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
	
	function clear(){
		$("#IMSI").val("");
		$("#msisdn").val("");
	}
</script>

</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<form class="form-horizontal" >
		<h4>Qos查詢頁面</h4>
			<!-- <div class="col-xs-5" align="right"><label for="IMSI">IMSI:</label></div>
			<div class="col-xs-7" align="left"><input type="text" id="IMSI" /></div> -->
			<div class="col-xs-5" align="right"><label for="msisdn">MSISDN:</label></div>
			<div class="col-xs-7" align="left"><input type="text" id="msisdn" /></div>
			<div class="col-xs-12" align="center">		
				<div class="btn-group" >
					<input type="button" class="btn btn-primary btn-sm" onclick="query()" value="查詢">
					<input type="button" class="btn btn-primary btn-sm" onclick="clear()" value="清除">
				</div>
			</div>	
			<div class="col-xs-12"><!-- <font size="2" color="red">(查詢IMSI時可使用"*"取代某區段號碼進行模糊查詢)</font> --><label id="Qmsg" style="height: 30px;">&nbsp;</label></div>	
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
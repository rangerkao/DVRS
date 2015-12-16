<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<style type="text/css">
.smsContent {
	background-color: #ffffb3;
}
.smsType {
	background-color: #b3ccff;
}
.sms {
	font-size: 15px;
}

</style>
<script type="text/javascript">
	
var sms =[];
function startsend(){
	disableButton();
	$.ajax({
	      url: '<s:url action="sendSMS"/>',
	      data: {
	    	  "COMTENT":JSON.stringify(sms),
	    	  "msisdn":$("#sendNumber").val()
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var list=$.parseJSON(json);
	    	  var error = list['error'];
	    	  console.log(list);
	    	  if(error){
	    		  alert("error");  
	    		  console.log(error);
	    	  }else{
	    		  alert("success");  
	    	  }
	      },
	      error: function(){
	    	alert("error");  
	      },
	      beforeSend:function(){
	    	  
        },
        complete:function(){
        	enableButton();
        }
	    });
	
};
function querySMSContent(){
	$.ajax({
	      url: '<s:url action="queryGPRSContent"/>',
	      data: {
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var list=$.parseJSON(json);
	    	  var error = list['error'];
	    	  console.log(list);
	    	  if(error){
	    		  alert(error);
	    		  console.log(error);
	    	  }else{
	    		  var data = list['data']
	    		  $("#smsA").text(data['A']);
	    		  $("#smsB").text(data['B']);
	    		  $("#smsCA").text(data['CA']);
	    		  $("#smsCI").text(data['CI']);
	    		  
	    		  sms = {A:$("#smsA").text(),B:$("#smsB").text(),C:$("#smsCA").text()};
	    	  }
	      },
	      beforeSend:function(){
	    	  
        },
        complete:function(){
        }
	    });
	
};
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}
$(document).ready(function(){
	querySMSContent();
	
	
	$("[name='phoneType']").change(function(){
		var type = $("[name='phoneType']:checked").val();
		
		if(type=='iphone'){
			//alert("Iphone Click");
			$("#Android").css("display","none");
			$("#IPhone").css("display","");
			sms = {A:$("#smsA").text(),B:$("#smsB").text(),C:$("#smsCI").text()};
		}else if(type=='android'){
			//alert("Android Click");
			$("#Android").css("display","");
			$("#IPhone").css("display","none");
			sms = {A:$("#smsA").text(),B:$("#smsB").text(),C:$("#smsCA").text()};
		}
		
	});
	
	$("#sending").click(function(){
		//alert("sending click!");
		var value = $("#sendNumber").val();
		//alert(value)
		if(!value|| value==''){
			alert("Please input phone number!");
			return
		};
		
		if(confirm("確定發送？")){
			console.log("sendding!");
			startsend();
		}else{
			console.log("No sendding!");
		};
		
	});
});
	

</script>
</head>
<body>
<div class="container-fluid max_height" >
	<div class="row max_height" align="center" style="vertical-align: middle;">
		<h4>數據開通簡訊發送頁面</h4>
		<div class="col-xs-12" align="center">
			<span> <input type="radio" name="phoneType" value="iphone">IPhone </span>
			<span> <input type="radio" name="phoneType" value="android" checked="checked">Android </span>
			<input type="text" id="sendNumber">
			<input type="button" class="btn btn-primary btn-sm" value="發送" id="sending">
		</div>
		<div class="col-xs-12" align="center">
		&nbsp;&nbsp;
		</div>
		<div class="col-xs-12" align="center">
			<table class="col-xs-12 sms">
				<tr><td>
					<div align="left" class="col-xs-12">
						<label class="col-xs-12 smsType">A</label>
						<div id="smsA" class="col-xs-12 smsContent" >
							
						</div>
					</div>
				</td></tr>
				<tr><td>
					<div align="left" class="col-xs-12">
						<label class="col-xs-12 smsType">B</label>
						<div id="smsB" class="col-xs-12 smsContent">
						</div>
					</div>
				</td></tr>
				<tr><td>
					<div id="Android" style="display: " align="left" class="col-xs-12">
						<label class="col-xs-12 smsType">C(Android)</label>
						<div id="smsCA" class="col-xs-12 smsContent">
						</div>
					</div>
					<div id="IPhone" style="display: none;" align="left" class="col-xs-12">
						<label class="col-xs-12 smsType">C(IPhone)</label>
						<div id="smsCI" class="col-xs-12 smsContent">
						</div>
					</div>
				</td></tr>
			</table>			
		</div>
	</div>
</div>
</body>
</html>
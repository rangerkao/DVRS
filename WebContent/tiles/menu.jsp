<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<style type="text/css">
#menu{
padding: none;

}
</style>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
var mousePos;
function handleMouseMove(event) {
	//alert("mouse");
    event = event || window.event; // IE-ism
    mousePos = {
    		x: event.clientX,
            y: event.clientY
    };
   	$("#x").html(mousePos.x); 
   	$("#y").html(mousePos.y); 
}
window.onmousemove = handleMouseMove;


/**
 * 實做閒置自動登出，以監控滑鼠指標位置為基礎
 */
var oldx,oldy;
var checkPeriod=1000*5; //每5秒檢查一次
var logOutminute=20//閒置登出時間（分鐘）
var logOutTime=1000*60*logOutminute;
var count=logOutTime/checkPeriod;

	window.setInterval(function() {
		check()
	}, checkPeriod);
	function check() {
		var newx = $("#x").html();
		var newy = $("#y").html();
		//alert(newx + ":" + newy);
		if (newx == oldx && newy == oldy) {
			count -= 1;
		} else {
			count = logOutTime / checkPeriod;
		}
		oldx = newx;
		oldy = newy;
		
		if (count <= 0) {
			alert("已閒置超過" + logOutminute + "分鐘，系統自動登出");
			document.getElementById('logoutLink').click();
		}
	}
/*
 *權限查詢
 */
 
 function queryAuth(){
		$("#menuAuth").html("正在查尋，請稍待...");
		  $.ajax({
		      url: '<s:url action="queryAuth"/>',
		      data: {}, //parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json) {  
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
		    	  var list=$.parseJSON(json);
		    	     $.each(list,function(i,Auth){
		    	   
		    	    	 $('#'+Auth.belong).append('<li><p class="text-left"><a id='+Auth.action+' href="<s:url action="'+Auth.action+'"/>">'+Auth.name+'</a></p></li>');
		    	     }); 
		    	  },
		      error: function() { $("#menuAuth").html('something bad happened');}
		    }); 
		 $("#menuAuth").html("&nbsp;");
	}
	
	$(document).ready(function(){
		//$("li").css("display","none");
		queryAuth();
	});

	function volidateNum(val){
		var   reg=/^\d+$/g;
		return reg.test(val);
	}
	
	
	
	
	function tqueryIMSI(){
		
		if($("#tMsisdn").val()==null || $("#tMsisdn").val()==""||$("#tMsisdn").val()=="請輸入門號"){
			alert('查詢IMSI時，門號必填');
			return
		}
		if(!volidateNum($("#tMsisdn").val())){
			alert('門號輸入格式錯誤');
			return
		}

		$.ajax({
		      url: '<s:url action="queryIMSI"/>',
		      data: {
		    	  "msisdn":$("#tMsisdn").val(),
		      },//parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json) {  
		    	  
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
		    	  var v=JSON.parse(json);
		    	  if(json=="" || v.imsi==null || v.imsi==""){
		    		  alert("查無IMSI");
		    	  }else{
		    		  $("#tIMSI").val(v.imsi);
		    	  }
	    	  },
		      error: function(json) {
		    	  alert('something bad happened'); 
		      },
	    	  beforeSend:function(){
    			//disableButton();
    			$("#tIMSI").val("").css('color','#333333');
	          },
	          complete:function(){
	        	  //enableButton();
	          }
		    });
	}

	function tqueryMSISDN(){
		
		if($("#tIMSI").val()==null || $("#tIMSI").val()=="" ||$("#tIMSI").val()=="請輸入IMSI"){
			alert('查詢門號時IMSI不可為空');
			return
		}
		
		if(!volidateNum($("#tIMSI").val())){
			alert('IMSI輸入格式錯誤');
			return
		}

		$.ajax({
		      url: '<s:url action="queryMSISDN"/>',
		      data: {
		    	  "imsi":$("#tIMSI").val(),
		      },//parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json) {  
		    	  
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
		    	  var v=JSON.parse(json);
		    	  if(json=="" || v.msisdn==null || v.msisdn==""){
		    		 alert("查無門號");
		    	  }else{
		    		  $("#tMsisdn").val(v.msisdn).css('color','#333333');
		    	  }
	    	  },
		      error: function(json) {
		    	  alert('something bad happened'); 
		      },
	    	  beforeSend:function(){
	    			//disableButton();
	    			$("#tMsisdn").val("");
	          },
	          complete:function(){
	        	  //enableButton();
	          }
		    });
	}
</script>
</head>
<body>
<div class="container-fluid max_height" >
	<div class="row max_height" align="center" style="vertical-align: top;">
		<div class="col-xs-12">
			<label id="x" style="display: none;">x.index</label>
			<label id="y" style="display: none;">y.index</label>
			<label id="menuAuth"></label>
		</div>
		<div class="col-xs-12">
			<input 	type="text" id="tIMSI" class="col-xs-12"
					value="請輸入IMSI" style="color: #AAAAAA;" 
					onfocus="if (this.value == '請輸入IMSI') {this.value = ''; this.style.color='#333333'}" 
					onblur="if (this.value == '') {this.value = '請輸入IMSI'; this.style.color='#AAAAAA'}"  >
			<input 	type="button" class="btn btn-primary btn-xs col-xs-12" onclick="tqueryMSISDN()" value="IMSI查詢門號">
			<input 	type="text" id="tMsisdn" class="col-xs-12"
					value="請輸入門號" style="color: #AAAAAA;" 
					onfocus="if (this.value == '請輸入門號') {this.value = ''; this.style.color='#333333'}" 
					onblur="if (this.value == '') {this.value = '請輸入門號'; this.style.color='#AAAAAA'}" >
			<input 	type="button" class="btn btn-primary btn-xs col-xs-12" onclick="tqueryIMSI()" value="門號查詢IMSI"> 
		</div>
		
		<ul id="menu">
			<li>
				<p class="text-left">使用者管理</p>
				<ul id="adminList">
				</ul>
			</li>
			<li>
				<p class="text-left">DVRS查詢</p>
				<ul id="searchList">
				</ul>
			</li>
			<li>
				<p class="text-left">DVRS設定</p>
				<ul id="settingList">
				</ul>
			</li>
			<li>
				<p class="text-left">其他</p>
				<ul id="elseList">
				</ul>
			</li>
		</ul>
	</div>
</div>
</body>
</html>
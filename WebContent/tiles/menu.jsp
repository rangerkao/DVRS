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
var logOutminute=5//閒置登出時間（分鐘）
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
		    	   
		    	    	 $('#'+Auth.belong).append('<li><a id='+Auth.action+' href="<s:url action="'+Auth.action+'"/>">'+Auth.name+'</a><br></li>');
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

</script>
</head>
<body>
	<div class="wapper" >
	<label id="x">x.index</label>
	<label id="y">y.index</label>
	<label id="menuAuth"></label>
		<ul id="menu">
			<li>
				使用者管理
				<ul id="adminList">
				</ul>
			</li>
			<li>
				查詢相關
				<ul id="searchList">
				</ul>
			</li>
			<li>
				設定相關
				<ul id="settingList">
				</ul>
			</li>
			<li>
				其他
				<ul id="elseList">
				</ul>
			</li>
		</ul>
	</div>
</body>
</html>
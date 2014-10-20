<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<%@ include file="../common/CSS.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
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
</script>
</head>
<body>
	<div class="wapper" align="center">
	<label id="x">x.index</label>
	<label id="y">y.index</label>
		<ul>
			<li><a href="<s:url action="billLink"/>">帳單匯出</a><br></li>
			<li><a href="<s:url action="adminLink"/>">使用者管理</a><br></li>
			<li><a href="<s:url action="dataRateLink"/>">資費管理</a><br></li>
			<li><a href="<s:url action="smsQueryLink"/>">簡訊發送查詢</a><br></li>
			<li><a id='logoutLink' href="<s:url action="logoutLink"/>" >登出</a><br></li>
		</ul>
	</div>
</body>
</html>
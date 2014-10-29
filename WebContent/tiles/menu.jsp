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
 * �갵���m�۰ʵn�X�A�H�ʱ��ƹ����Ц�m����¦
 */
var oldx,oldy;
var checkPeriod=1000*5; //�C5���ˬd�@��
var logOutminute=5//���m�n�X�ɶ��]�����^
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
			alert("�w���m�W�L" + logOutminute + "�����A�t�Φ۰ʵn�X");
			document.getElementById('logoutLink').click();
		}
	}
</script>
</head>
<body>
	<div class="wapper" >
	<label id="x">x.index</label>
	<label id="y">y.index</label>
		<ul id="menu">
			<li>
				�ϥΪ̺޲z
				<ul>
					<li><a href="<s:url action="adminLink"/>">�ϥΪ̺޲z</a><br></li>
					<li><a href="<s:url action="programLink"/>">�{���޲z</a><br></li>
				</ul>
			</li>
			<li>
				�d�߬���
				<ul>
					<li><a href="<s:url action="actionQueryLink"/>">�ϥΪ̾ާ@�����d��</a><br></li>
					<li><a href="<s:url action="smsQueryLink"/>">²�T�o�e�d��</a><br></li>
					<li><a href="<s:url action="dataRateLink"/>">��O�޲z</a><br></li>
					<li><a href="<s:url action="cdrLink"/>">CDR�d��</a><br></li>
				</ul>
			</li>
			<li>
				�]�w����
				<ul>
					<li><a href="<s:url action="smsSettingLink"/>">²�T�]�w</a><br></li>
				</ul>
			</li>
			<li>
				��L
				<ul>
					<li><a href="<s:url action="billLink"/>">�b��ץX</a><br></li>
					<li><a id='logoutLink' href="<s:url action="logoutLink"/>" >�n�X</a><br></li>
				</ul>
			</li>
		</ul>
	</div>
</body>
</html>
<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<!-- Tab ����  �ϥ� Start-->
<style type="text/css">
ul, li {
	margin: 0;
	padding: 0;
	list-style: none;
}

.abgne_tab {
	clear: left;
	/* width: 400px; */
	margin: 10px 0;
}

ul.tabs1 {
	width: 100%;
	height: 32px;
	/* border-bottom: 1px solid #999; */
	border-left: 1px solid #999;
}

ul.tabs {
	width: 100%;
	height: 32px;
	border-bottom: 1px solid #999;
	border-left: 1px solid #999;
}

ul.tabs li {
	float: left;
	height: 31px;
	line-height: 31px;
	overflow: hidden;
	position: relative;
	margin-bottom: -1px;
	border: 1px solid #999;
	border-left: none;
	background: #e1e1e1;
}

ul.tabs li a {
	display: block;
	padding: 0 20px;
	color: #000;
	border: 1px solid #fff;
	text-decoration: none;
}

ul.tabs li a:hover {
	background: #ccc;
}

ul.tabs li.active {
	background: #fff;
	border-bottom: 1px solid #fff;
}

ul.tabs li.active a:hover {
	background: #fff;
}

div.tab_container {
	clear: left;
	width: 100%;
	/* border: 1px solid #999; */
	border-top: none;
	background: #fff;
}

div.tab_container .tab_content {
	padding: 20px;
	width: 100%;
}

div.tab_container .tab_content h2 {
	margin: 0 0 20px;
}
</style>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
$(function(){
	// �w�]��ܲĤ@�� Tab
	var _showTab = 0;
	var $defaultLi = $('ul.tabs li').eq(_showTab).addClass('active');
	$($defaultLi.find('a').attr('href')).siblings().hide();
 
	// �� li ���ҳQ�I����...
	// �Y�n�令�ƹ����� li ���ҴN������, �� click �令 mouseover
	$('ul.tabs li').click(function() {
		// ��X li �����W�s�� href(#id)
		var $this = $(this),
			_clickTab = $this.find('a').attr('href');
		// ��ثe�I���쪺 li ���ҥ[�W .active
		// �ç�S�̤������� .active �������� class
		//$this.addClass('active').siblings('.active').removeClass('active');
		$('.active').removeClass('active');
		$this.addClass('active');
		// �H�J�۹��������e�����åS�̤���
		$(_clickTab).stop(false, true).fadeIn().siblings().hide();
 
		return false;
	}).find('a').focus(function(){
		this.blur();
	});
});
</script>
<!-- Tab ����  �ϥ�  End-->
</head>
<body>

</body>
</html>
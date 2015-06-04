<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@ include file="../common/CSS.jsp"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<!-- 載入 bootStract -->
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<!-- Latest compiled and minified CSS -->
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css">

<!-- Optional theme -->
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css">

<!-- Latest compiled and minified JavaScript -->
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
<Script type="text/javascript">


var firstPage;
var totalPage;
var nowPage;
function pagination(){
	firstPage=1
	totalPage=firstPage
	nowPage=1;
	
	var rown=$("#rown").val();
	//$("ul.pagination li").remove();
	$("#page_contain").empty();
	
	var count=0;
	$.each(dataList,function(i,data){
		
		if(count==0){
			//$("ul.pagination").append("<li><a href='#page"+totalPage+"'>"+totalPage+"</a></li>");
			
			$("#page_contain").append("<table  align='center' class='pages' id='page"+totalPage+"'  width='95%'></table>");

			$("#page"+totalPage).append("<tr align='center'></tr>");
			$.each(tHead,function(i,head){
				if(head.name=="button" || head.name=="checkbox")
					$("#page"+totalPage+" tr").first().append("<td class='columnLabel' width='"+head._width+"'></td>");
				else
					$("#page"+totalPage+" tr").first().append("<td class='columnLabel' width='"+head._width+"'>"+head.name+"</td>");
			});
		}
		$("#page"+totalPage).append("<tr align='center'></tr>");
		$.each(tHead,function(i,head){
			if(head.name=="button")
				$("#page"+totalPage+" tr").last().append(head.col);
			else if(head.name=="checkbox")
				$("#page"+totalPage+" tr").last().append("<td><input type='checkbox' "+(data[head.col]? "checked='checked'":"")+"disabled='disabled'></td>");
			else
				$("#page"+totalPage+" tr").last().append("<td>"+data[head.col]+"</td>");
		});
		
       count++;
       if(count==rown){
    	   count=0;
    	   totalPage++;
       }   
    });

	$(".pages tr:odd").addClass("odd_columm");//奇數欄位樣式
    $(".pages tr:even").addClass("even_columm");
    $(".pages ").addClass("table-bordered");

    var _showTab=0;
    
	$("#page"+firstPage).siblings().hide();
	$("#nowPage").html(nowPage);
	$("#totalPage").html("共"+totalPage+"頁");

	
	bindPageButtonClick();
	

}
function bindPageButtonClick(){
	//$("ul.pagination li").click(function(){
	$(":button.pagination").unbind();
	$(":button.pagination").click(function(){
		//找出li之中的連結內的#id
		//var $this = $(this),_clickTab = $this.find('a').attr('href');
		var $this = $(this),_clickTab = $this.attr('name');

		// 把目前點擊到的 li 頁籤加上 .active
		// 並把兄弟元素中有 .active 的都移除 class
		//$this.addClass('active').siblings('.active').removeClass('active');
		// 淡入相對應的內容並隱藏兄弟元素
		//$(_clickTab).stop(false, true).fadeIn().siblings().hide();

		if(_clickTab=="Previous" && nowPage>firstPage)
			nowPage=nowPage-1;
	
		if(_clickTab=="Next" && nowPage<totalPage)
			nowPage=nowPage+1;

		$("#page"+nowPage).stop(false, true).fadeIn().siblings().hide();
		$("#nowPage").html(nowPage);
		$("#totalPage").html("共"+totalPage+"頁");
		
		//取消連結的跳轉效果
		return false;
	});
	
	
}
	
function createExcel(){
	$("[name='dataList']").val(encodeURI(JSON.stringify(dataList)));
	$("[name='colHead']").val(encodeURI(JSON.stringify(tHead)));
	$("[name='reportName']").val(encodeURI(JSON.stringify(reportName)));
	$("#reportFrom").submit();	
}

</Script>
<style type="text/css">

.datatable, .datatable td, .datatable th {
	border:1px;
}
#table1 td {
	align:center
}
.datatable{
	width:50%;
}
.odd_columm{
	background-color: #FEFFAF
}
.even_columm{
	background-color: #C7FF91
}
.label{
	
}
/* 擴展 base Start */
* {
	/* border: none; */
	/* padding: 0;
	margin: 0;
	z-index: 0; */
}
html, body{
      height:100%;
}
.max_height {
	height: 100%;
	min-height: 100%; 
	margin:0px;
	padding:0px; 
}
.alert_msg{
	color: red;
}
/* 擴展 base End */
div.scroll {
	overflow: auto;
	overflow: scroll;
}
</style>


</head>
<body>
	<div class="wapper" align="center">
		<h1>Sim2Travel 維運管理系統</h1>
	</div>
	<form action="createExcel" method="post" target="sub_iframe" id="reportFrom" style="display: none;">
			<input type="text" name="dataList">
			<input type="text" name="colHead">
			<input type="text" name="reportName">
		</form>
	<iframe name="sub_iframe" width="0" height="0" style="display: none;"></iframe>
</body>
</html>
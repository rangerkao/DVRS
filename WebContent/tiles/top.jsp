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


<script src="http://code.jquery.com/jquery-latest.js"></script>
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css">
<script src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script>


<!-- Latest compiled and minified JavaScript -->
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>

<Script type="text/javascript">



$(document).ready(function(){
	putActionToTable();
	putHeadToTable();
});

function sort(head,order){
	//alert("sort");
	
	if(order == 0){//預設排序
		dataList = oList.slice();
	}else if(order == 1){//反向排序
		dataList = dataList.sort(function(a,b){
			return a[head]<b[head]?1:-1;
		});
	}else if(order == 2){//正向排序
		dataList = dataList.sort(function(a,b){
			return a[head]>b[head]?1:-1;
		});
	}
	putContainToTable();
	
}


//20161019
function setSortAction(){
	$(".sortable").unbind();
	$(".sortable").click(function(){
		//取得元素與元件名稱
		var $this = $(this),_sortName = $this.attr('name'),_sortCol = $this.attr('col'),_html=$this.html();
		

		if(_html.indexOf("by-order-alt")!=-1){
			$this.html(_sortName+"<span class='glyphicon glyphicon-sort' aria-hidden='true'></span>");
			sort(_sortCol,0);
		}else if(_html.indexOf("by-order")!=-1){
			$this.html(_sortName+"<span class='glyphicon glyphicon-sort-by-order-alt' aria-hidden='true'></span>");
			sort(_sortCol,2);
		}else{
			$this.html(_sortName+"<span class='glyphicon glyphicon-sort-by-order' aria-hidden='true'></span>");
			sort(_sortCol,1);
		}
		
		//取消連結的跳轉效果
		return false;
	});
}

function putHeadToTable(){
	
	if(typeof(tHead)=='undefined')
		return;
	//清空表格
	$("#page_contain").empty();
	//放入表格到指定位置
	$("#page_contain").append("<table  align='center' style='vertical-align: middle; ' class='pages' id='contain_table'  width='95%'></table>");
	//放入第一列用已指定
	$("#contain_table").append("<tr align='center'></tr>");
	//填入Head
	$.each(tHead,function(i,head){
		if(head.name=="radio_disabled"){
			//do nothing
		}else{
			if(head.name=="button" || head.name=="checkbox" || head.name=="radio")
				$("#contain_table tr").first().append("<td class='columnLabel' width='"+head._width+"'></td>");
			else
				$("#contain_table tr").first().append("<td class='columnLabel sortable' width='"+head._width+"' name='"+head.name+"' col='"+head.col+"'>"+head.name+"<span class='glyphicon glyphicon-sort' aria-hidden='true'></span></td>");
		}
	});
}
//20161019 add
function putContainToTable(){
	//取得每頁數量
	var rown=$("#rown").val();
	
	//移除內容
	$("#contain_table tr").first().siblings().remove();
	
	//填入內容
	for(var i =(nowPage-1)*rown ;i<rown*nowPage && i< dataList.length;i++){
		$("#contain_table").append("<tr align='center'></tr>");
		var data = dataList[i];
		$.each(tHead,function(i,head){
			if(head.name=="radio_disabled"){
				//do nothing
			}else{
				if(head.name=="button")
					$("#contain_table tr").last().append(head.col);
				else if(head.name=="checkbox")
					$("#contain_table tr").last().append("<td><input type='checkbox' "+(data[head.col]? "checked='checked'":"")+"disabled='disabled'></td>");
				else if(head.name=="radio")
					$("#contain_table tr").last().append("<input type='radio' name='r' value='"+data[head.col]+"' "+
							(data["radio_disabled"]=="true"?" disabled='disabled'":"")+">");
				else 
					$("#contain_table tr").last().append("<td>"+data[head.col]+"</td>");
			}
			
		});
	}	
}

function putActionToTable(){
	$("#page_action").append("\
			<button type='button' name='Previous'  class='pagination btn btn-warning'><span class='glyphicon glyphicon-chevron-left'></span> Previous</button>\
			<label id='nowPage'></label>\
			<button type='button' name='Next' class='pagination btn btn-warning'> <span class='glyphicon glyphicon-chevron-right'></span> Next</button>\
			<label id='totalPage' style='margin-right: 10px'></label>\
			<label>每頁筆數</label>\
			<input id='rown' type='text' value='10' width='5px'>\
			<input type='button' onclick='pagination()' class='btn btn-primary btn-sm' style='margin: 20px'  value='重新分頁'>\
			"); 
	
}


var firstPage;
var totalPage;
var nowPage;
var oList;
function pagination(){
	//初始化
	firstPage=1
	totalPage=firstPage
	nowPage=1;
	oList=dataList.slice();
	
	
	putContainToTable();
	
	var rown=$("#rown").val();
	
	totalPage = Math.floor(dataList.length/rown)+(dataList.length%rown==0?0:1);
	$("#nowPage").html(nowPage);
	$("#totalPage").html("共"+totalPage+"頁");
	
	
	//綁定換頁動作
	bindPageButtonClick();
	//綁定排序動作
	setSortAction();

	/*
	
	//取得每頁數量
	var rown=$("#rown").val();
	//$("ul.pagination li").remove();
	//清空表格
	$("#page_contain").empty();
	
	
	var count=0;
	
	 $("#page_contain").append("<table  align='center' style='vertical-align: middle; ' class='pages' id='page"+totalPage+"'  width='95%'></table>");

	$("#page"+totalPage).append("<tr align='center'></tr>");
	$.each(tHead,function(i,head){
		if(head.name=="radio_disabled"){
			//do nothing
		}else{
			if(head.name=="button" || head.name=="checkbox" || head.name=="radio")
				$("#page"+totalPage+" tr").first().append("<td class='columnLabel' width='"+head._width+"'></td>");
			else
				$("#page"+totalPage+" tr").first().append("<td class='columnLabel' width='"+head._width+"'>"+head.name+"</td>");
		}
		
	});
	
	count++;
	
	
	$.each(dataList,function(i,data){
		
		if(count==0){
			//$("ul.pagination").append("<li><a href='#page"+totalPage+"'>"+totalPage+"</a></li>");
			
			$("#page_contain").append("<table  align='center' style='vertical-align: middle; ' class='pages' id='page"+totalPage+"'  width='95%'></table>");

			$("#page"+totalPage).append("<tr align='center'></tr>");
			$.each(tHead,function(i,head){
				if(head.name=="radio_disabled"){
					//do nothing
				}else{
					if(head.name=="button" || head.name=="checkbox" || head.name=="radio")
						$("#page"+totalPage+" tr").first().append("<td class='columnLabel' width='"+head._width+"'></td>");
					else 
						$("#page"+totalPage+" tr").first().append("<td class='columnLabel' width='"+head._width+"'>"+head.name+"</td>");
				}
			});
		}
		$("#page"+totalPage).append("<tr align='center'></tr>");
		$.each(tHead,function(i,head){
			if(head.name=="radio_disabled"){
				//do nothing
			}else{
				if(head.name=="button")
					$("#page"+totalPage+" tr").last().append(head.col);
				else if(head.name=="checkbox")
					$("#page"+totalPage+" tr").last().append("<td><input type='checkbox' "+(data[head.col]? "checked='checked'":"")+"disabled='disabled'></td>");
				else if(head.name=="radio")
					$("#page"+totalPage+" tr").last().append("<input type='radio' name='r' value='"+data[head.col]+"' "+
							(data["radio_disabled"]=="true"?" disabled='disabled'":"")+">");
				else 
					$("#page"+totalPage+" tr").last().append("<td>"+data[head.col]+"</td>");
			}
			
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
	
	*/
}


function bindPageButtonClick(){
	
	//20161019 add
	$(":button.pagination").unbind();
	$(":button.pagination").click(function(){
		//取得元素與元件名稱
		var $this = $(this),_clickTab = $this.attr('name');

		if(_clickTab=="Previous" && nowPage>firstPage)
			nowPage=nowPage-1;
	
		if(_clickTab=="Next" && nowPage<totalPage)
			nowPage=nowPage+1;

		putContainToTable();
		
		$("#nowPage").html(nowPage);
		
		//取消連結的跳轉效果
		return false;
	});
	
	
	/* //$("ul.pagination li").click(function(){
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
	}); */
	
	
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
/* 資料表格奇數欄位樣式 */
.pages tr:nth-child(even){
	background-color: #FEFFAF
}
.pages tr:nth-child(odd){
	background-color: #C7FF91
}

</style>


</head>
<body>
	<div class="wapper" align="center">
		<!-- Sim2Travel 維運管理系統 -->
	</div>
	<form action="createExcel" method="post" target="sub_iframe" id="reportFrom" style="display: none;">
			<input type="text" name="dataList">
			<input type="text" name="colHead">
			<input type="text" name="reportName">
		</form>
	<iframe name="sub_iframe" width="0" height="0" style="display: none;"></iframe>
</body>
</html>
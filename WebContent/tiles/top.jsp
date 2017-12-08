<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@ include file="../common/CSS.jsp"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<!-- ���J bootStract -->
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
	
	if(order == 0){//�w�]�Ƨ�
		dataList = oList.slice();
	}else if(order == 1){//�ϦV�Ƨ�
		dataList = dataList.sort(function(a,b){
			return a[head]<b[head]?1:-1;
		});
	}else if(order == 2){//���V�Ƨ�
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
		//���o�����P����W��
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
		
		//�����s��������ĪG
		return false;
	});
}

function putHeadToTable(){
	
	if(typeof(tHead)=='undefined')
		return;
	//�M�Ū��
	$("#page_contain").empty();
	//��J������w��m
	$("#page_contain").append("<table  align='center' style='vertical-align: middle; ' class='pages' id='contain_table'  width='95%'></table>");
	//��J�Ĥ@�C�Τw���w
	$("#contain_table").append("<tr align='center'></tr>");
	//��JHead
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
	//���o�C���ƶq
	var rown=$("#rown").val();
	
	//�������e
	$("#contain_table tr").first().siblings().remove();
	
	//��J���e
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
			<label>�C������</label>\
			<input id='rown' type='text' value='10' width='5px'>\
			<input type='button' onclick='pagination()' class='btn btn-primary btn-sm' style='margin: 20px'  value='���s����'>\
			"); 
	
}


var firstPage;
var totalPage;
var nowPage;
var oList;
function pagination(){
	//��l��
	firstPage=1
	totalPage=firstPage
	nowPage=1;
	oList=dataList.slice();
	
	
	putContainToTable();
	
	var rown=$("#rown").val();
	
	totalPage = Math.floor(dataList.length/rown)+(dataList.length%rown==0?0:1);
	$("#nowPage").html(nowPage);
	$("#totalPage").html("�@"+totalPage+"��");
	
	
	//�j�w�����ʧ@
	bindPageButtonClick();
	//�j�w�Ƨǰʧ@
	setSortAction();

	/*
	
	//���o�C���ƶq
	var rown=$("#rown").val();
	//$("ul.pagination li").remove();
	//�M�Ū��
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

	$(".pages tr:odd").addClass("odd_columm");//�_�����˦�
    $(".pages tr:even").addClass("even_columm");
    $(".pages ").addClass("table-bordered");

    var _showTab=0;
    
	$("#page"+firstPage).siblings().hide();
	$("#nowPage").html(nowPage);
	$("#totalPage").html("�@"+totalPage+"��");

	
	bindPageButtonClick();
	
	*/
}


function bindPageButtonClick(){
	
	//20161019 add
	$(":button.pagination").unbind();
	$(":button.pagination").click(function(){
		//���o�����P����W��
		var $this = $(this),_clickTab = $this.attr('name');

		if(_clickTab=="Previous" && nowPage>firstPage)
			nowPage=nowPage-1;
	
		if(_clickTab=="Next" && nowPage<totalPage)
			nowPage=nowPage+1;

		putContainToTable();
		
		$("#nowPage").html(nowPage);
		
		//�����s��������ĪG
		return false;
	});
	
	
	/* //$("ul.pagination li").click(function(){
	$(":button.pagination").unbind();
	$(":button.pagination").click(function(){
		//��Xli�������s������#id
		//var $this = $(this),_clickTab = $this.find('a').attr('href');
		var $this = $(this),_clickTab = $this.attr('name');

		// ��ثe�I���쪺 li ���ҥ[�W .active
		// �ç�S�̤������� .active �������� class
		//$this.addClass('active').siblings('.active').removeClass('active');
		// �H�J�۹��������e�����åS�̤���
		//$(_clickTab).stop(false, true).fadeIn().siblings().hide();

		if(_clickTab=="Previous" && nowPage>firstPage)
			nowPage=nowPage-1;
	
		if(_clickTab=="Next" && nowPage<totalPage)
			nowPage=nowPage+1;

		$("#page"+nowPage).stop(false, true).fadeIn().siblings().hide();
		$("#nowPage").html(nowPage);
		$("#totalPage").html("�@"+totalPage+"��");
		
		//�����s��������ĪG
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
/* �X�i base Start */
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
/* �X�i base End */
div.scroll {
	overflow: auto;
	overflow: scroll;
}
/* ��ƪ��_�����˦� */
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
		<!-- Sim2Travel ���B�޲z�t�� -->
	</div>
	<form action="createExcel" method="post" target="sub_iframe" id="reportFrom" style="display: none;">
			<input type="text" name="dataList">
			<input type="text" name="colHead">
			<input type="text" name="reportName">
		</form>
	<iframe name="sub_iframe" width="0" height="0" style="display: none;"></iframe>
</body>
</html>
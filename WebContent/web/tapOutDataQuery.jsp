<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css">
<script src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
<script type="text/javascript">

var today=new Date();
var currentDateTime =
    today.getFullYear()+"/"+
    (today.getMonth()+1)+"/"+
    today.getDate()+"_"+
    today.getHours()+':'+today.getMinutes()+":"+today.getSeconds();

$(function() {
	
    $(".datapicker").datepicker({
        showOn: "button",
        buttonImage: "source/icon.png",
        buttonImageOnly: true,
        buttonText: "Select date",
        dateFormat: 'yy-mm-dd'
    });
    
    
    
    
  });
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}
var dataList;
var tapOutList;
var reportName="TapOut�ץX_"+currentDateTime;

var tHead=[
	{name:"�l�ܤ��",col:"startDate",_width:"10%"},
    {name:"���C��",col:"location",_width:"10%"},
    {name:"�o�ܸ��X/���ܸ��X",col:"phonenumber",_width:"15%"},
    {name:"�q�ܺ���",col:"type",_width:"6%"},
    {name:"�l�ܮɨ�",col:"startTime",_width:"10%"},
    {name:"�׸ܮɨ�",col:"endTime",_width:"10%"},
    {name:"�ϥζq(��/�h/Bytes)",col:"unit",_width:"11%"},
    {name:"���C�O��",col:"amount",_width:"7%"},
    {name:"��l�O��",col:"totalCharge",_width:"7%"},
    {name:"�u�f�O��",col:"discountCharge",_width:"7%"},
    {name:"���G�O��",col:"finalCharge",_width:"7%"}
    ];
    
    //�l�ܤ��	���C��	�o�ܸ��X\���ܸ��X	�q�ܺ���	�l�ܮɨ�	�׸ܮɨ�	�q�ܬ��/�h��	���C���ܶO	�౵�ܺ��C����ڸܶO	�O�����O

var preType;
    
function query(){
	if(!validate()) return flase;
	
	 preType = $('input[name=type]:checked').val();
	
	 $.ajax({
	      url: '<s:url action="queryTapOutData"/>',
	      data: {	"from":$("#dateFrom").val(),
	    	  		"to":$("#dateTo").val(),
	    	  		"phonenumber":$("#phonenumber").val(),
	    	  		"type":$('input[name=type]:checked').val()
	    	  		
	    	  		},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var list=$.parseJSON(json);
	    	  if(list['error']){
	    		  var error = list['error'];
	    		  if(error.includes("�Y�p�d��")){
	    			  $('#Error').html("���G���ƹL�j�A���Y�p�d��᭫�s�d�ߡC");
	    			  alert("���G���ƹL�j�A���Y�p�d��᭫�s�d�ߡC");
	    		  }else{
	    			  $('#Error').html(error);
	    		  }
	    		  
	    	  }else{
	    		  $("#Qmsg").html("Success");
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
					tapOutList = list['data'];
		    	  	if(tapOutList!=null)
		    		  dataList=tapOutList.slice(0);

	    	  }
 
	    	},
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
	      beforeSend:function(){
	    	  $("#Qmsg").html("���b�d�ߡA�еy��...");
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

function validate(){
	var validation=true;
	/* if((($("#dateFrom").val()==null||$("#dateFrom").val()=="")^($("#dateTo").val()==null||$("#dateTo").val()==""))){
		alert("��������P�ɶ�άҤ���I")
		validation=false;
	}else */
	if( ($("#dateFrom").val()==null||$("#dateFrom").val()=="")||
			($("#dateTo").val()==null||$("#dateTo").val()=="") ){
		alert("������������I");
		validation=false;
	}
	if($("#dateFrom").val()>$("#dateTo").val()){
		alert("�}�l������i�j�󵲧�����I")
		validation=false;
	} 
	return validation;
}
function clearDate(){
	alert("clear clicked");
	$("#dateFrom").val("");
	$("#dateTo").val("");
}
function typeselected(type){
	if(preType!="all") return;
	
	dataList = [];
	console.log(type);
	if(type == 'all'){
		dataList=tapOutList.slice(0);
	}else{
		var condition;
		if(type == 'voice'){
			condition = function(item){
				if(item.type=='R8' || item.type=='RA' || item.type=='RC' || item.type=='RE') return true;
				else return false;
			};
			
		}else if(type == 'sms'){
			condition = function(item){
				if(item.type=='MJ') return true;
				else return false;
			};

		}else if(type == 'data'){
			condition = function(item){
				if(item.type=='PC') return true;
				else return false;
			};
		}

		$.each(tapOutList,function(i,ListItem){
			
			if(condition(ListItem)){
				dataList.push(ListItem);
			}
		});
	}
	
	pagination();
}
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}


function createTapOutExcel(){
	if(!validate()) return flase;
	
	var param = {	
			"from":$("#dateFrom").val(),
	  		"to":$("#dateTo").val(),
	  		"phonenumber":$("#phonenumber").val(),
	  		"type":$('input[name=type]:checked').val()
	  		}
	$("[name='param']").val(encodeURI(JSON.stringify(param)));
	$("[name='tapOut_colHead']").val(encodeURI(JSON.stringify(tHead)));
	$("[name='tapOut_reportName']").val(encodeURI(reportName));
	$("#tapOut_reportFrom").submit();	
}

</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<h3>TapOut�d��</h3>
		<div class="col-xs-4" align="right">�d�ߴ����q</div>
		<div class="col-xs-8" align="left">
			<input type="text"  disabled="disabled" id="dateFrom" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px ">
			��
			<input type="text" disabled="disabled" id="dateTo" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px" >
			<input type="button" class="btn btn-primary btn-sm" onclick="createTapOutExcel()" value="�U��Excel"> 
		</div>
		<div class="col-xs-4" align="right">�Τ᭻�丹�X</div>
		<div class="col-xs-8" align="left">
			<input type="text" id="phonenumber">
			<span  align="left" >�����G	
				<input type="radio" name="type" value="all"  onclick="typeselected('all')" checked="checked"  >�Ҧ�
				<input type="radio" name="type" value="voice" onclick="typeselected('voice')" >�y��
				<input type="radio" name="type" value="sms" onclick="typeselected('sms')">²�T
				<input type="radio" name="type" value="data" onclick="typeselected('data')">�ƾ�
			</span>
		
		</div>
		
		<div class="col-xs-12">
			<div class="btn-group" >
				<input type="button" class="btn btn-primary btn-sm" onclick="query()" value="�d��">
				<input type="button" class="btn btn-primary btn-sm" onclick="clearDate()" value="�M��">
			</div>
		</div>
		<div class="col-xs-12" align="center">
			RA�G���ܡCR8�G�o�ܡCRC�G����౵�CRE�G�^���౵�CMJ�G�T���CPC�G�ƾ�
		</div>
		<div class="col-xs-12"><label id="Qmsg" style="height: 30px;">&nbsp;</label></div>
		<div class="col-xs-12"> 
			<button type="button" name="Previous"  class="pagination btn btn-warning"><span class="glyphicon glyphicon-chevron-left"></span> Previous</button>
			<label id="nowPage"></label>
			<button type="button" name="Next" class="pagination btn btn-warning"> <span class="glyphicon glyphicon-chevron-right"></span> Next</button>
			<label id="totalPage" style="margin-right: 10px"></label>
			<label>�C������</label>
			<input id="rown" type="text" value="10" width="5px">
			<input type="button" onclick="pagination()" class="btn btn-primary btn-sm" style="margin: 20px"  value="���s����">
		</div>
		<div class="col-xs-12"> 
			<div id="page_contain"></div>
		</div>
		<div class="col-xs-12" align="left"> 
			<div id="Error"></div>
		</div>
		
		
		<form action="createTapOutExcel" method="post" target="tapOut_sub_iframe" id="tapOut_reportFrom" style="display: none;">
			<input type="text" name="param">
			<input type="text" name="tapOut_colHead">
			<input type="text" name="tapOut_reportName">
		</form>
		<iframe name="tapOut_sub_iframe" width="0" height="0" style="display: none;"></iframe>
	</div>
</div>



</body>
</html>
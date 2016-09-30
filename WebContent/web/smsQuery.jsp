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
$(function() {
	dateChange=true;
    $(".datapicker").datepicker({
        showOn: "button",
        buttonImage: "source/icon.png",
        buttonImageOnly: true,
        buttonText: "Select date",
        dateFormat: 'yy-mm-dd'
    });
  });
var dateChange;
var smsList;
var dataList;
var reportName="�W�q²�T�o�e��";
var tHead=[{name:"����ID",col:"id",_width:"10%"},
           {name:"��������",col:"sendNumber",_width:"15%"},
           {name:"²�TID",col:"msg",_width:"20%"},
           {name:"�o�e�ɶ�",col:"sendDate",_width:"20%"},
           {name:"�o�e���G",col:"result",_width:"20%"},
           {name:"�O���ɶ�",col:"createDate",_width:"15%"}];
           
function query(){
	
	if(!dateChange){
		queryList();
		return;
	}
	
	if(!validate()) return false;
	$("#Qmsg").html("���b�d�ߡA�еy��...");
	$.ajax({
	      url: '<s:url action="querySMSLog"/>',
	      data: {	"dateFrom":$("#dateFrom").val(),
	    	  		"dateTo":$("#dateTo").val(),
	    	  		//"msisdn":$("#MSISDN").val()
	    	  		},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	    smsList=list['data'];
	    	    
	    	    if(smsList!=null)
	    	    	dataList=smsList.slice(0);
	    	  	
	    	  	var error = list['error'];
		    	  $('#Error').html(error);
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
        	  //queryList();
        	  queryMSISDN1($("#MSISDN").val());
        	  dateChange=false;
          }
	    });
}
var TWNMSISDN;
var S2TMSISDN;

function queryMSISDN1(tText){
	$.ajax({
	      url: '<s:url action="queryTWNMSISDN"/>',
	      data: {
	    	  "msisdn":tText,
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var v=JSON.parse(json);
	    	  
	    	  if(v && v['error']){
	    		  alert(list['error']);
	    	  }else{
		    	  if(json=="" || v.msisdn==null || v.msisdn==""){
		    		  queryMSISDN2(tText);
		    	  }else{
		    		  S2TMSISDN = tText;
		    		  TWNMSISDN = v.msisdn;
		    		  queryList();
		    	  }
	    	  }
    	  },
	      error: function(json) {
	    	  alert('something bad happened'); 
	      },
    	  beforeSend:function(){
    			//disableButton();
    		  $("#tLabel").val("");
          },
          complete:function(){
        	  //enableButton();
        	  
          }
	    });
}

function queryMSISDN2(tText){
	console.log(TWNMSISDN+":"+S2TMSISDN);
	$.ajax({
	      url: '<s:url action="queryS2TMSISDN"/>',
	      data: {
	    	  "msisdn":tText,
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var v=JSON.parse(json);
	    	  
	    	  if(v && v['error']){
	    		  alert(list['error']);
	    	  }else{
	    		  if(json=="" || v.msisdn==null || v.msisdn==""){
	    			  
		    	  }else{
		    		  S2TMSISDN = v.msisdn;
		    		  TWNMSISDN = tText;
		    		  queryList();
		    	  }
	    	  }
    	  },
	      error: function(json) {
	    	  alert('something bad happened'); 
	      },
    	  beforeSend:function(){
    			//disableButton();
    		  $("#tLabel").val("");
          },
          complete:function(){
        	  //enableButton();  
          }
	    });
}

function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}
function validate(){
	var validation=true;
	if((($("#dateFrom").val()==null||$("#dateFrom").val()=="")^($("#dateTo").val()==null||$("#dateTo").val()==""))){
		alert("��������P�ɶ�άҤ���I")
		validation=false;
	}else
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

function queryList(){
	
	console.log(TWNMSISDN+":"+S2TMSISDN);
	var   reg=$("#MSISDN").val();
	reg="^"+reg+"$"
	reg=reg.replace("*","\\d+");
	reg=new RegExp(reg);	
	
	dataList.splice(0,dataList.length);
	 $.each(smsList,function(i,ListItem){
		 if(reg.test(ListItem.sendNumber)||TWNMSISDN == ListItem.sendNumber||S2TMSISDN == ListItem.sendNumber||($("#MSISDN").val()==null||$("#MSISDN").val()=="")){
			 dataList.push(ListItem);
		 }
	}); 
	pagination();
	enableButton();
}
//

</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<h3>�W�q²�T�o�e�d�߭���</h3>
		<div class="col-xs-4" align="right">�d�ߴ����q</div>
		<div class="col-xs-8" align="left">
			<input type="text"  disabled="disabled" id="dateFrom" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px " onchange="dateChange=true">
			��
			<input type="text" disabled="disabled" id="dateTo" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px" onchange="dateChange=true">
		</div>
		<div class="col-xs-4" align="right"><label>����:</label> </div>
		<div class="col-xs-8" align="left"><input type="text" id="MSISDN"></div>
		<div class="col-xs-12">
			<div class="btn-group" >
				<input type="button" class="btn btn-primary btn-sm" onclick="query()" value="�d��">
				<input type="button" class="btn btn-primary btn-sm" onclick="clearDate()" value="�M��">
				<input type="button" class="btn btn-primary btn-sm" onclick="createExcel()" value="�U��Excel"> 
			</div>
		</div>
		<div class="col-xs-12"><font size="2" color="red">(�d�ߪ����ɥi�ϥ�"*"���N�Y�Ϭq���X�i��ҽk�d��)</font><label id="Qmsg" style="height: 30px;">&nbsp;</label></div>
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
	</div>
</div>
</body>
</html>
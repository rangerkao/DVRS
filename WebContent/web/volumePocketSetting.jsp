<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css">
<!-- Latest compiled and minified JavaScript -->
<script type="text/javascript">
var pid;
var dString;
$(function(){
	$(".datapicker").datepicker({
        showOn: "button",
        buttonImage: "source/icon.png",
        buttonImageOnly: true,
        buttonText: "Select date",
        dateFormat: 'yy-mm-dd'
    });
    var Today=new Date();
    var month=Today.getMonth()+1;
    
    if(month<10)
    	month="0"+month;
    
    var Day=Today.getDate();
    if(Day<10)
    	Day="0"+Day;
	var ds=Today.getFullYear()+ "-" + month + "-" + Day;
	dString = Today.getFullYear()+month+Day;
	$("#dateFrom").val(ds);
	
	whenDateChange();
	init();
	
});

function init(){
	$("#MSISDN").val("886");
	
	$("#update").attr('disabled', true);
	$("#delete").attr('disabled', true);
}

var pocketList;
var dataList;
var tHead=[{name:"radio",col:"pid",_width:"5%"},{name:"radio_disabled",col:"radio_disabled",_width:""},
           {name:"���ت���",col:"chtMsisdn",_width:"10%"},
           {name:"�_�l�ɶ�",col:"startDate",_width:"5%"},
           {name:"�����ɶ�",col:"endDate",_width:"5%"},
           {name:"�Ȥ�m�W",col:"customerName",_width:"10%"},
           {name:"�i�u�̩m�W",col:"callerName",_width:"10%"},
           {name:"�����Ҧr��",col:"id",_width:"10%"},//�\�Ȥ᪺
           {name:"�������",col:"phoneType",_width:"10%"},
           {name:"Email",col:"email",_width:"10%"},
           {name:"�wĵ��",col:"alerted",_width:"5%"},
           {name:"�إ߮ɶ�",col:"createTime",_width:"10%"},
           {name:"�����ɶ�",col:"cancelTime",_width:"10%"}];
           
           
function setdata(){
	
	$.each(pocketList,function(i,pocket){
		if(pocket.cancelTime || pocket.startDate<dString){
			pocket.radio_disabled="true";
		}
	});
}
function query(){
	
	var msisdn = $("#MSISDN").val();
	
	if(msisdn=="886"){
		msisdn = '';
	}
	
	$.ajax({
	      url: '<s:url action="queryVolumePocketList"/>',
	      data: {"input":msisdn},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var list=$.parseJSON(json);
	    	  if(list['error']){
	    		  alert(list['error']);
	    	  }else{
	    		  alert("Success");
		    	 // alert("success");
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
		    	  
		    	  //$("#table1 tr:gt(0)").remove();//����>0����Ūtr
		    	  pocketList=list['data'];
		    	  setdata();
		    	  
		    	  if(pocketList!=null)
		    		  dataList=pocketList.slice(0);
	    	  }
	      },
	      error: function() { 
	    	  alert("Error!");
	      },
    	  beforeSend:function(){
	    		dataList=[];
	    		disableButton();
          },
          complete:function(){
        	  enableButton();
        	  pagination();
        	  init();
          }
	    });
}

function insertItem(){

	if(!validat())
		return;
	if(!confirm("�O�_�T�w�s�W�H")){
		return;
	}
	
	var startDate = $("#dateFrom").val();
	var endDate = new Date(startDate);//���\�Hyyyy-MM-dd�Φ������]�w
	
	startDate = startDate.replace(/\-/g,"");//regex ��ܪk�~�i�H���N����
	
	//var endDate = new Date(startDate.substring(0,4),startDate.substring(4,6)-1,startDate.substring(6,8),0,0,0);
	endDate = new Date(endDate.getTime()+10*24*60*60*1000);//+10��
	endDate = ""+endDate.getFullYear()+(endDate.getMonth()+1<10?'0':'')+(endDate.getMonth()+1)+(endDate.getDate()<10?'0':'')+endDate.getDate();
	var cusInfo = {
			chtMsisdn:$("#MSISDN").val(),
			startDate:$("#dateFrom").val().replace(/\-/g,""),
			endDate:$("#dateTo").val().replace(/\-/g,""),
			id:$("#customerID").val(),
			callerName:$("#callerName").val(),
			customerName:$("#customerName").val(),
			phoneType:$("#phoneType").val(),
			email:$("#email").val()
			};
	
	console.log(cusInfo);

	$.ajax({
	      url: '<s:url action="inserVolumePocket"/>',
	      data: {"input":JSON.stringify(cusInfo)},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json){
	    	  var list=$.parseJSON(json);
	    	  if(list['error']){
	    		  
	    		  if(list['error'].indexOf("The date range error.")!=-1){
	    			  alert("���İ϶����ơA�нT�{");
	    			  if(pocketList!=null)
			    		  dataList=pocketList.slice(0);
	    			  
	    		  }else{
	    			  alert(list['error']);
	    		  }
	    			  
	    		  
	    	  }else{
		    	  alert("success");
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
		    	  
		    	  //$("#table1 tr:gt(0)").remove();//����>0����Ūtr
		    	  pocketList=list['data'];
		    	  setdata();
		    	  if(pocketList!=null)
		    		  dataList=pocketList.slice(0);
		    	  
		    	  $("#MSISDN").val("886");
	        	  $("#customerID").val("");
	        	  $("#callerName").val("");
	        	  $("#customerName").val("");
	        	  $("#phoneType").val("");
	        	  $("#email").val("");
	    	  }
	      },
	      error: function() { 
	    	  alert("error");
	      },
    	  beforeSend:function(){
	    		dataList=[];
	    		disableButton();
          },
          complete:function(){
        	  enableButton();
        	  pagination();
        	  init();
          }
	    });
}
function whenDateChange(){
	var startDate = $("#dateFrom").val();
	var endDate = new Date(startDate);//���\�Hyyyy-MM-dd�Φ������]�w
	
	//var endDate = new Date(startDate.substring(0,4),startDate.substring(4,6)-1,startDate.substring(6,8),0,0,0);
	endDate = new Date(endDate.getTime()+10*24*60*60*1000);//+10��
	$("#dateTo").val(""+endDate.getFullYear()+"-"+(endDate.getMonth()+1<10?'0':'')+(endDate.getMonth()+1)+"-"+(endDate.getDate()<10?'0':'')+endDate.getDate());
	
}


function updateItem(){
	if(!pid){
		alert("�п�ܶ��ءI");
		return;
	}
	if(!validat())
		return;
	if(confirm("�T�w�n��s���]�w�H")){

		var cusInfo = {
				pid:pid,
				chtMsisdn:$("#MSISDN").val(),
				startDate:$("#dateFrom").val().replace(/\-/g,""),
				endDate:$("#dateTo").val().replace(/\-/g,""),
				id:$("#customerID").val(),
				callerName:$("#callerName").val(),
				customerName:$("#customerName").val(),
				phoneType:$("#phoneType").val(),
				email:$("#email").val()
				};
		
		$.ajax({
		      url: '<s:url action="updateVolumePocket"/>',
		      data: {"input":JSON.stringify(cusInfo)},//parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json){
		    	  var list=$.parseJSON(json);
		    	  if(list['error']){
		    		  if(list['error'].indexOf("The date range error.")!=-1){
		    			  alert("���İ϶����ơA�нT�{");
		    			  if(pocketList!=null)
				    		  dataList=pocketList.slice(0);		    			  
		    		  }else{
		    			  alert(list['error']);
		    		  } 		  
		    	  }else{
			    	  alert("success");
			    	  //jQuery.parseJSON,JSON.parse(json)
			    	  //alert(json);
			    	  
			    	  //$("#table1 tr:gt(0)").remove();//����>0����Ūtr
			    	  pocketList=list['data'];
			    	  setdata();
			    	  if(pocketList!=null)
			    		  dataList=pocketList.slice(0);
			    	  
			    	  $("#MSISDN").val("886");
		        	  $("#customerID").val("");
		        	  $("#callerName").val("");
		        	  $("#customerName").val("");
		        	  $("#phoneType").val("");
		        	  $("#email").val("");
		    	  }
		      },
		      error: function() { 
		    	  alert("error");
		      },
	    	  beforeSend:function(){
		    		dataList=[];
		    		disableButton();
	          },
	          complete:function(){
	        	  enableButton();
	        	  pagination();
	        	  init();
	          }
		    });
	}
	
}

function deleteItem(){
	if(!pid){
		alert("�п�ܶ��ءI");
		return;
	}
	
	
	if(confirm("�T�w�n�������]�w�H")){
		var cusInfo = {
				pid:pid,
				chtMsisdn:$("#MSISDN").val()
				};
		$.ajax({
		      url: '<s:url action="cancelVolumePocket"/>',
		      data: {"input":JSON.stringify(cusInfo)},//parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json){
		    	  var list=$.parseJSON(json);
		    	  if(list['error']){
		    		  alert(list['error']);		    		  
		    	  }else{
			    	  alert("success");
			    	  //jQuery.parseJSON,JSON.parse(json)
			    	  //alert(json);
			    	  
			    	  //$("#table1 tr:gt(0)").remove();//����>0����Ūtr
			    	  pocketList=list['data'];
			    	  setdata();
			    	  if(pocketList!=null)
			    		  dataList=pocketList.slice(0);
			    	  
			    	  $("#MSISDN").val("886");
		        	  $("#customerID").val("");
		        	  $("#callerName").val("");
		        	  $("#customerName").val("");
		        	  $("#phoneType").val("");
		        	  $("#email").val("");
		    	  }
		      },
		      error: function() { 
		    	  alert("error");
		      },
	    	  beforeSend:function(){
		    		dataList=[];
		    		disableButton();
	          },
	          complete:function(){
	        	  enableButton();
	        	  pagination();
	          }
		    });
	}
}

function loadItem(){
	
	if(!pocketList){
		alert("�жi��d�߫��ܶ���!");
		return;
	}
	
	
		
	
	pid = $("input[name='r']:checked").val();
	
	if(!pid||pid==''){
		alert("�п�ܶ���!");
		return;
	}
	
	$.each(pocketList,function(i,pocket){
		if((pocket.pid)==pid){
			 $("#MSISDN").val(pocket.chtMsisdn);
			 $("#customerID").val(pocket.id);
			 $("#callerName").val(pocket.callerName);
			 $("#customerName").val(pocket.customerName);
			 $("#phoneType").val(pocket.phoneType);
			 $("#email").val(pocket.email);
			 
			 
			 var s = pocket.startDate;
			 $("#dateFrom").val(s.substring(0,4)+"-"+s.substring(4,6)+"-"+s.substring(6,8));
			 var e = pocket.endDate;
			 
			 $("#dateTo").val(e.substring(0,4)+"-"+e.substring(4,6)+"-"+e.substring(6,8));
			 
			 alert("�a�J���\");
			 $("#update").attr('disabled', false);
			 $("#delete").attr('disabled', false);
			 return;
		}
	});

	
}

function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}
 //20150106 add �R���i�u�aimsi
function validat(){
	
	var validate = true;

	if(!$("#MSISDN").val()||$("#MSISDN").val()==''||$("#MSISDN").val()=='886'){
		alert("�п�J�����I");
		validate = false;
	}
	var today = new Date();
	if(new Date($("#dateFrom").val())<new Date(today.getFullYear(),today.getMonth(),today.getDate())){
		alert("�ɶ����i���󤵤�");
		validate = false;
	}
	
	
	
	return validate;
}
</script>
</head>
<body>

<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<form class="form-horizontal" >
			<h3>����y�q�]�]�w����</h3><label id="pid"></label>
			<div class="col-xs-5" align="right" style="margin-bottom: 5px;"><label for="IMSI">���ت���:</label></div>
		    <div class="col-xs-7" align="left" style="margin-bottom: 5px;">
		    	<input type="text" id="MSISDN" />
		    	<input type="button" id="Query" class="btn btn-primary btn-sm" value="�d��" onclick="query()"/>
		    </div>		    
		    <div class="col-xs-12">
		    	<div class="col-xs-2" align="right" style="margin-bottom: 5px;"><label for="customerName">�Ȥ�m�W:</label></div>
		    	<div class="col-xs-2" align="left" ><input type="text" id="customerName" /></div>
		    	<div class="col-xs-2" align="right" style="margin-bottom: 5px;"><label for="customerID">�����Ҧr��:</label></div>
		    	<div class="col-xs-2" align="left" ><input type="text" id="customerID" /></div>
		    	<div class="col-xs-2" align="right" style="margin-bottom: 5px;"><label for="email">Email:</label></div>
		    	<div class="col-xs-2" align="left" ><input type="text" id="email" /></div>	    
		    </div>
		    <div class="col-xs-12">
		    	<div class="col-xs-2" align="right" style="margin-bottom: 5px;"><label for="callerName">�i�u�̩m�W:</label></div>
		    	<div class="col-xs-2" align="left" ><input type="text" id="callerName" /><font size="1" color="red">�]�D���H�i�u�Τ��q��^</font></div>
		    	<div class="col-xs-2" align="right" style="margin-bottom: 5px;"><label for="phoneType">�������:</label></div>
		    	<div class="col-xs-2" align="left" ><input type="text" id="phoneType" /></div>	
		    	
		    </div>
		    <div class="col-xs-12">
		    	 <div class="col-xs-2" align="right" style="margin-top: 5px;"><label for="dateFrom">�_�l���:</label></div>
		    	 <div class="col-xs-2" align="left" style="margin-top: 5px;">
		    	 	<input type="text"  disabled="disabled" id="dateFrom" class="datapicker" onchange="whenDateChange()" style="height: 25px;text-align: center;position:relative;top: -5px;margin-right: 5px; " >
		    	 </div>
		    	 <div class="col-xs-2" align="right" style="margin-top: 5px;"><label for="dateTo">�������:</label></div>
		    	 <div class="col-xs-2" align="left" style="margin-top: 5px;">
		    	 	<input type="text"  disabled="disabled" id="dateTo" style="height: 25px;text-align: center;position:relative;top: -5px;margin-right: 5px; " >
		    	 </div>
		    	<div class="col-xs-4" align="left" >
		    		 <input type="button" id="insert" class="btn btn-primary btn-sm"  style="margin-left: 5px;" value="�s�W" onclick="insertItem()"/>
			    	 <input type="button" id="load" class="btn btn-primary btn-sm"  style="margin-left: 5px;" value="�a�J" onclick="loadItem()"/>
			    	 <input type="button" id="update" class="btn btn-primary btn-sm"  style="margin-left: 5px;" value="�ק���" onclick="updateItem()"/>
			    	 <input type="button" id="delete" class="btn btn-primary btn-sm"  style="margin-left: 5px;" value="����" onclick="deleteItem()"/>
		    	</div>	
		    </div>
		</form>
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
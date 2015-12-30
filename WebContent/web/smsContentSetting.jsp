<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
       <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script type="text/javascript">
var smsContentlist;
var dataList;

$(function(){
	query();
	
})
function query(){
	$.ajax({
	      url: '<s:url action="querySMSContent"/>',
	      data: {},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  //$("#table2 tr:gt(0)").remove();//����>0����Ūtr
	    	  smsContentlist=list['data'];
	    	  if(smsContentlist!=null)
	    		  dataList=smsContentlist.slice(0);
	    	  
	    	  var error = list['error'];
	    	  $('#Error').html(error);
    	  },
	      error: function() { 
	    	  $("#Qmsg").html('something bad happened');  
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

function queryContentByID(){
	var SMSid=$("#sId").val();
	dataList.splice(0,dataList.length);
	 $.each(smsContentlist,function(i,smsContent){
		if((smsContent.id==SMSid)||(SMSid==null||SMSid=="")){
			dataList.push(smsContent);
		} 
	}); 
	pagination();
	
/*     $("#table2 tr:odd").addClass("odd_columm");//�_�����˦�
    $("#table2 tr:even").addClass("even_columm"); */
	
}
function updateContent(mod,txt){
	if(!validat(mod,txt))
		return;
	$.ajax({
	      url: '<s:url action="updateSMSContent"/>',
	      data: {
	    	  "sc.id":$("#sId").val(),
	    	  "sc.comtent":$("#sContent").val(),
	    	  "sc.charSet":$("#CharSet").val(),
	    	  "sc.description":$("#Discription").val(),
	    	  "mod":mod
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var list=$.parseJSON(json);
	    	  $('#bClear').click();
	    	  $("#Qmsg").html("Success");
	    	  var error = list['error'];
	    	  $('#Error').html(error);
	    	  
	    	  if(error==null||error=="")
	    		  query();
	    	  
	      },
	      error: function() { 
	    	  $("#Qmsg").html('something bad happened'); 
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("���b��s�A�еy��...");
    			disableButton();
    			$('#Error').html("");
          },
          complete:function(){
        	  //enableButton();
          }
	    });
}

var tHead=[{name:"²�TID",col:"id",_width:"10%"},
           {name:"²�T���e",col:"comtent",_width:"40%"},
           {name:"�s�X",col:"charSet",_width:"10%"},
           {name:"����",col:"description",_width:"35%"},
           {name:"button",col:"<td align='center' ><button class='btn btn-primary btn-sm' onclick='chooseRow(this)'>���</button></td>",_width:"5%"}];

function chooseRow(bu){
	
	var row =bu.parentNode.parentNode //this ���V button =(parent)> cell =(parent)> row
	//alert(row.cells[0].innerText);
	$("#sId").val(row.cells[0].innerText);
	$("#sContent").val(row.cells[1].innerText);
	$("#CharSet").val(row.cells[2].innerText);
	$("#Discription").val(row.cells[3].innerText);
	
	$("#LsId").html("&nbsp;");
	$("#LsContent").html("&nbsp;");
	$("#LCharSet").html("&nbsp;");
	$("#LDiscription").html("&nbsp;");
}
function replacetxt(txt){
	$("#sContent").val($("#sContent").val()+txt);
}
var exist;
var validation;
function validat(mod,txt){
	exist=false;
	validation=true;
	
	if($("#sId").val()==null||$("#sId").val()==""){
		$("#LsId").html("��ID���i����!");
		validation=false;
	}
	if($("#sContent").val()==null||$("#sContent").val()==""){
		$("#LsContent").html("���e���i����!");
		validation=false;
	}
	
	if(!volidateNum($('#sId').val())){
		$("#LsId").html("����쥲������ƼƦr!");
		validation=false;
	}
	
	$.each(smsContentlist,function(i,smsContent){
		//alert("Search id:"+$("#sId").val()+"="+smsContent.id);
		if(smsContent.id==$("#sId").val()){
			exist=true;
		}
	});
	
	if(mod=='add'){
		
		if(exist){
			$("#LsId").html("�w�g��²�T���e�I");
			validation=false;
		}
		if(!confirm('�T�{�s�W���e?'))
			validation=false;
	}else{

		if(!exist){
			$("#LsId").html("�L��ID�A�зs�إߤ@�����e!");
			validation=false;
		}else{
			if(mod=='mod'){
				if(!confirm('�T�{�ק鷺�e?'))
					validation=false;
			}else if(mod=='del'){
				if(!confirm('�T�{�R�����e?'))
					validation=false;
			}
		}
	}
	return validation;
}
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}
function labelClear(){
	$("#LsId").html("&nbsp;");
	$("#LsContent").html("&nbsp;");
	$("#LDiscription").html("&nbsp;");
	$("#LCharSet").html("&nbsp;");
}
</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height " align="center">
		<form class="form-horizontal" >
			<h3>²�T���e�]�w����</h3>
			<div class="col-xs-2" align="right"><label for="sId">²�TID:</label></div>
		    <div class="col-xs-4" align="left">
		    	<input type="text" id="sId"/>
		    </div>				    
		    <div class="col-xs-2" align="right"><label for="CharSet">�s�X:</label></div>
		    <div class="col-xs-4" align="left">
		    	<input type="text" id="CharSet"/>
		    </div>
		    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LsId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LCharSet" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		    <div class="col-xs-2" align="right"><label for="sContent">²�T���e�G</label></div>
		    <div class="col-xs-4" align="left">
		    	<textarea rows="5" cols="50" id="sContent"></textarea>
		    </div>
		    <div class="col-xs-2" align="right"><label for="Discription">�����G</label></div>
		    <div class="col-xs-4" align="left">
		    	<textarea rows="5" cols="50" id="Discription"></textarea>
		    </div>
		    <div class="col-xs-12 " style="margin: opx;padding: 0px">
				<label>�ѼƱa�J:</label>
				<input type="button" onclick="replacetxt('{{bracket}}')" value="�B��(NT****)">
				<input type="button" onclick="replacetxt('{{customerService}}')" value="�ȪA�q��(+******)">
		    </div>
		    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LsContent" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LDiscription" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
	
		    <div class="col-xs-12">
		    	<div class="btn-group" class="col-xs-12">
					<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="�M��" id="bClear">
					<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('add','�s�W')" value="�s�W">
					<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('mod','�ק�')" value="�ק�">
					<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('del','�R��')" value="�R��">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryContentByID()" value="�HID�d��">
					<!-- <input type="button" class="btn btn-primary btn-sm" onclick="queryAdmin()" value="�d��"> -->
			    </div>
		    </div>
	    </form>
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
	</div>
</div>
</body>
</html>
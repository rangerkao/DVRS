<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
$(function(){
	query();
})
var limitList;
function query(){
	$.ajax({
	      url: '<s:url action="queryAlertLimit"/>',
	      data: {},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  $("#table1 tr:gt(0)").remove();//����>0����Ūtr
	    	  limitList=list;
	    	    $.each(list,function(i,limitSetting){  
               var _tr = $(	"<tr>"+
               					"<td align='center' >"+limitSetting.imsi+"</td>"+
               					"<td align='center' >"+limitSetting.msisdn+"</td>"+
               					"<td align='center' >"+limitSetting.threshold+"</td>"+
               					"<td align='center' ><button onclick='chooseRow(this)'>���</button></td>"+
               				"</tr>");  
               
             $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//�_�����˦�
	    	    $("#table1 tr:even").addClass("even_columm");
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened');  
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("���b�d�M�A�еy��...");
    			disableButton();
          },
          complete:function(){
        	  enableButton();
          }
	    });
}
//�N�Q��ܪ�table����J�s���
function chooseRow(bu){
	var row =bu.parentNode.parentNode //this ���V button =(parent)> cell =(parent)> row
	//alert(row.cells[0].innerText);
	$("#IMSI").val(row.cells[0].innerText);
	$("#LIMSI").html("");	
	$("#Msisdn").val(row.cells[1].innerText);
	$("#LMsisdn").html("");
	$("#Limit").val(row.cells[2].innerText);
	$("#LLimit").html("");	
}

function updateLimit(mod,txt){
	
	if($("#IMSI").val()==null || $("#IMSI").val()==""){
		alert("�Х�����d��IMSI�A�~��i��ާ@");
		return;
	}
	
	
	if (!validat(mod,txt)) return false;
	
	
	var sendSMS=false;
	if(confirm("�ݭn�o�e�q��²�T�����Ȥ�ܡH")){
		sendSMS=true;
	}
	
	
	$.ajax({
	      url: '<s:url action="updateAlertLimit"/>',
	      data: {
	    	  "imsi":$("#IMSI").val(),
	    	  "gprslimit":$("#Limit").val(),
	    	  "mod":mod,
	    	  "sendSMS":sendSMS,
	    	  "msisdn":$("#Msisdn").val()
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	 query();
    	  },
	      error: function(json) { $("#Qmsg").html('something bad happened'); 
	      },
	      error: function(json) {
	    	  $("#Qmsg").html('something bad happened'); 
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("���b��s�A�еy��...");
    			disableButton();
          },
          complete:function(){
        	  enableButton();
          }
	    });
}
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}

function queryIMSI(){
	
	if($("#Msisdn").val()==null || $("#Msisdn").val()==""){
		$("#LMsisdn").html('����줣�i����');
		return
	}

	$.ajax({
	      url: '<s:url action="queryIMSI"/>',
	      data: {
	    	  "msisdn":$("#Msisdn").val(),
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var v=JSON.parse(json);
	    	  if(json=="" || v.imsi==null || v.imsi==""){
	    		  $("#Qmsg").html("�d�LIMSI");
	    	  }else{
	    		  $("#IMSI").val(v.imsi);
	    		  $("#Qmsg").html("Success");
	    	  }
    	  },
	      error: function(json) {
	    	  $("#Qmsg").html('something bad happened'); 
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("���b�d�ߡA�еy��...");
    			disableButton();
    			$("#IMSI").val("");
          },
          complete:function(){
        	  enableButton();
          }
	    });
}
function validat(mod,txt){
	
	var validate = true;
	
	if($("#Msisdn").val()==null ||$("#Msisdn").val()==""){
		$("#LMsisdn").html('����줣�i����');
		validate = false; 
	}
	if($("#Limit").val()==null ||$("#Limit").val()==""){
		$("#LLimit").html('����줣�i����');
		validate = false;
	}
	
	if(!volidateNum($("#Msisdn").val())){
		$("#LMsisdn").html('�榡���~�A�������¼Ʀr');
		validate = false;
	}
	if(!volidateNum($("#Limit").val())){
		$("#LLimit").html('�榡���~�A�������¼Ʀr');
		validate = false;
	}
	
	if(validate){
		if(mod=='add'){			
			//�T�{��Ʀ��S���b
			 $.each(limitList,function(i,limit){
				 if(limit.msisdn==$("#Msisdn").val()){
					 alert("�������w�]�w�W���A�L�k�s�W!");
					 validate = false;
				 }
			 });
		}else if(mod=='mod' || mod=='del'){
			validate = false;
			$.each(limitList,function(i,limit){
				 if(limit.msisdn==$("#Msisdn").val()){
					 validate = true;
				 }
			 });
			if(!validate){
				 alert("�����������]�w�L�A�L�k�R��,�ק�I");
			}
		}else{
			validate = false;
		}
	}
	return validate;
}
function clearText(txt){
	$("#L"+txt).html("");

	if(!volidateNum($("#"+txt).val()))
		$("#L"+txt).html("�榡���~�A�������¼Ʀr");
}
function volidateNum(val){
	var   reg=/^\d+$/g;
	return reg.test(val);
}

</script>
</head>
<body>
<div align="center">
	<h3>ĵ�ܤW���]�w����</h3>
	<div>
		<form>
		<table >
			<tr>
				<td class="label" align="right"><label>IMSI:</label></td>
				<td><input type="text" id="IMSI" onkeyup="clearText('Msisdn')" disabled="disabled"/></td>
				<td><label id="LIMSI" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>		
			</tr>
			<tr>
				<td class="label" align="right"><label>����:</label></td>
				<td><input type="text" id="Msisdn" onkeyup="clearText('Msisdn')" /></td>
				<td><label id="LMsisdn" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>		
			</tr>
			<tr>
				<td class="label" align="right"><label>�̤j�W��:</label></td>
				<td><input type="text" id="Limit"  onkeyup="clearText('Limit')" /></td>
				<td><label id="LLimit" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>
			</tr>
			<tr>
				<td class="label" align="right"></td>
				<td>
					<input type="button"  onclick="this.form.reset()" value="�M��" id="BClear">
					<input type="button" onclick="updateLimit('add','�s�W')" value="�s�W">
					<input type="button" onclick="updateLimit('mod','�ק�')" value="�ק�">
					<input type="button" onclick="updateLimit('del','�R��')" value="�R��">
					<input type="button" onclick="queryIMSI()" value="�d��IMSI"> 
					<br><label id="Qmsg" style="height: 50px;width: 100px">&nbsp;</label>
				</td>
			</tr>
		</table>
		</form>
	</div>
	<div>
		<table class="datatable" align="center" style="width: 50%" id="table1">
			<tr class="even_columm" >
				<td class="columnLabel" align="center" width="30%">IMSI</td>
				<td class="columnLabel" align="center" width="30%">MSISDN</td>
				<td class="columnLabel" align="center" width="20%">LIMITE</td>
				<td></td>
				<td width="10%">&nbsp;</td>
			</tr>
		</table>
	</div>
</div>
</body>
</html>
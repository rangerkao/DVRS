<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<!-- Latest compiled and minified JavaScript -->
<script type="text/javascript">
$(function(){
	query();
	$("#Limit").val("0");
})

var limitList;
var dataList;
var tHead=[{name:"IMSI",col:"imsi",_width:"20%"},
           {name:"����",col:"msisdn",_width:"20%"},
           {name:"�إ߮ɶ�",col:"createDate",_width:"20%"},
           {name:"���A",col:"status",_width:"20%"},
           {name:"button",col:"<td align='center' ><button onclick='chooseRow(this)' class='btn btn-primary btn-sm'>���</button></td>",_width:"20%"}];
var reportName="VIP�Ȥ�]�w";
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
	    	  //$("#table1 tr:gt(0)").remove();//����>0����Ūtr
	    	  limitList=list;
	    	  dataList=limitList.slice(0);
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened');  
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("���b�d�M�A�еy��...");
    			disableButton();
          },
          complete:function(){
        	  enableButton();
        	  pagination();
        	  $("#IMSI").val("");
        		$("#LIMSI").html("&nbsp;");	
        		$("#Msisdn").val("");
        		$("#LMsisdn").html("&nbsp;");
          }
	    });
}

//�N�Q��ܪ�table����J�s���
function chooseRow(bu){
	var row =bu.parentNode.parentNode //this ���V button =(parent)> cell =(parent)> row
	//alert(row.cells[0].innerText);
	$("#IMSI").val(row.cells[0].innerText);
	$("#LIMSI").html("&nbsp;");	
	$("#Msisdn").val(row.cells[1].innerText);
	$("#LMsisdn").html("&nbsp;");
	//$("#Limit").val(row.cells[2].innerText);
	//$("#LLimit").html("");	
}

function updateLimit(mod,txt){
	
	if($("#IMSI").val()==null || $("#IMSI").val()==""){
		alert("IMSI���i����");
		return;
	}
	
	
	if (!validat(mod,txt)) return false;
	
	
	var sendSMS=false;
	if(mod=="add"){
		if(confirm("�ݭn�o�e�q��²�T�����Ȥ�ܡH")){
			sendSMS=true;
		}
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
          }
	    });
}
function disableButton(){
	$(':button').attr('disabled', 'disabled');
}
function enableButton(){
	$(':button').removeAttr('disabled'); //.attr('disabled', '');
}

 function queryIMSI(mod,txt){
	
	if(mod=='del'){
		updateLimit(mod,txt);
		return;
	}
	 
	if($("#Msisdn").val()==null || $("#Msisdn").val()==""){
		$("#LMsisdn").html('����줣�i����');
		return
	}
	if(!volidateNum($("#Msisdn").val())){
		$("#LMsisdn").html('�榡���~�A�������¼Ʀr');
		validate = false;
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
	    		  $("#Qmsg").html("�������L����IMSI�A�ާ@����");
	    	  }else{
	    		  $("#IMSI").val(v.imsi);
	    		  updateLimit(mod,txt);
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
 //20150106 add �R���i�u�aimsi
function validat(mod,txt){
	
	var validate = true;
	
	if(mod!='del' && ($("#Msisdn").val()==null ||$("#Msisdn").val()=="")){
		$("#LMsisdn").html('����줣�i����');
		validate = false; 
	}
	if($("#Limit").val()==null ||$("#Limit").val()==""){
		$("#LLimit").html('����줣�i����');
		validate = false;
	}
	
	if(mod!='del' && !volidateNum($("#Msisdn").val())){
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
				 if(limit.imsi==$("#IMSI").val()){
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
	$("#L"+txt).html("&nbsp;");

	/* if(!volidateNum($("#"+txt).val()))
		$("#L"+txt).html("�榡���~�A�������¼Ʀr"); */
}
function volidateNum(val){
	var   reg=/^\d+$/g;
	return reg.test(val);
}
function queryVIP(){
	
	var   reg=$("#Msisdn").val();
	reg="^"+reg+"$"
	reg=reg.replace("*","\\d+");
	reg=new RegExp(reg);	
	
	dataList.splice(0,dataList.length);
	 $.each(limitList,function(i,limit){
		 if((reg.test(limit.msisdn)||($("#Msisdn").val()==null||$("#Msisdn").val()==""))&&($("input[name='Status']:checked").val()==limit.status||$("input[name='Status']:checked").val()==undefined)){
			 dataList.push(limit);
		 }
	}); 
	pagination();
}

</script>
</head>
<body>

<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<form class="form-horizontal" >
			<h3>ĵ�ܤW���]�w����</h3>
			<div class="col-xs-5" align="right"><label for="IMSI">IMSI:</label></div>
		    <div class="col-xs-7" align="left" >
		    	<input type="text" id="IMSI" onkeyup="clearText('IMSI')" disabled="disabled"/>
		    </div>
		    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LIMSI" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		    <div class="col-xs-5" align="right"><label for="Msisdn">����:<font color="red">*</font></label></div>
		    <div class="col-xs-7" align="left">
		    	<input type="text" id="Msisdn" onkeyup="clearText('Msisdn')" />
		    </div>
		    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LMsisdn" >�d�߮ɥi�ϥ�"*"���N�Y�Ϭq���X�i��ҽk�d��</label>
		    </div>
		    <div class="col-xs-5" align="right"><label for="Status">���A:</label></div>
		    <div class="col-xs-7" align="left">
			    <label class="radio-inline">
					<input type="radio" name="Status" id="Status1" value="Normal"> Normal
				</label>
				<label class="radio-inline">
					<input type="radio" name="Status" id="Status2" value="Inactive"> Inactive
				</label>
				<label class="radio-inline">
					<input type="radio" name="Status" id="Status3" value=" "> �ŭ�
				</label>
		    </div>
		    <div style="display: none;" class="col-xs-12">
		    	<div class="col-xs-5" align="right"><label for="Limit">���̤j�W��:</label></div>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Limit"  onkeyup="clearText('Limit')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			    	<label id="LLimit" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
		    </div>
		    <div class="col-xs-12">
		    	<div class="btn-group" class="col-xs-12">
			    	<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="�M��" id="BClear">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryIMSI('add','�s�W')" value="�s�W">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryIMSI('mod','�ק�')" value="�ק�" style="display: none;">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryIMSI('del','�R��')" value="�R��">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryVIP()" value="�d��"> 
					<input type="button" class="btn btn-primary btn-sm" onclick="createExcel()" value="�U��Excel"> 
			    </div>
		    </div>
		    <div class="col-xs-12"><label id="Qmsg" style="height: 20px;width: 100px">&nbsp;</label></div>

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
	</div>
</div>
</body>
</html>
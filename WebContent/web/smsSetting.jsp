<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
       <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script src="http://bestdaylong.com/bootstrap/js/bootstrap.min.js"></script>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
var smsSettinglist;

$(function(){
	query();
	queryContent();
})
function query(){
	$.ajax({
	      url: '<s:url action="querySMSSetting"/>',
	      data: {},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  $("#table1 tr:gt(0)").remove();//����>0����Ūtr
	    	  smsSettinglist=list;
	    	    $.each(list,function(i,smsSetting){  
               var _tr = $(	"<tr>"+
               					"<td align='center' >"+smsSetting.id+"</td>"+
               					"<td align='center' >"+smsSetting.pricePlanId+"</td>"+
               					"<td align='center' >"+(smsSetting.bracket*100)+"</td>"+
               					"<td align='center' >"+smsSetting.msg+"</td>"+
               					"<td align='center' ><input type='checkbox' "+(smsSetting.suspend? "checked='checked'":"")+"disabled='disabled'></td>"+
               					"<td align='center' ><button class='btn btn-primary btn-sm' onclick='chooseRow(this)'>���</button></td>"+
               				"</tr>");  
               
             $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//�_�����˦�
	    	    $("#table1 tr:even").addClass("even_columm");
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened');  
	      },
	      beforeSend:function(){
    		  $("#Qmsg").html("���b�d�ߡA�еy��...");
    			disableButton();
          },
          complete:function(){
        	  enableButton();
          }
	    });
}
function validate(){
	var validation=true;

	return validation;
}
//�N�Q��ܪ�table����J�s���
function chooseRow(bu){
	
	var row =bu.parentNode.parentNode //this ���V button =(parent)> cell =(parent)> row
	//alert(row.cells[0].innerText);
	$("#Id").val(row.cells[0].innerText);
	$("#ppId").val(row.cells[1].innerText);
	$("#LppId").html("&nbsp;");
	$("#Bracket").val(row.cells[2].innerText);
	$("#LBracket").html("&nbsp;");
	$("#Msg").val(row.cells[3].innerText);
	$("#LMsg").html("&nbsp;");
	$("#Suspend").prop("checked",(row.cells[4].childNodes[0].checked));//�b�V�U�@�h�M���checkbox
	
}
var exist;
var validation;
function validat(mod,txt){
	exist=false;
	validation=true;
	
	if($("#ppId").val()==null||$("#ppId").val()==""){
		$("#LppId").html("����줣�i����!");
		validation=false;
	}
	
	if($("#Bracket").val()==null||$("#Bracket").val()==""){
		$("#LBracket").html("����줣�i����!");
		validation=false;
	}
	if($("#Msg").val()==null||$("#Msg").val()==""){
		$("#LMsg").html("����줣�i����!");
		validation=false;
	}
	
	if($('#Bracket').val()>100){
		$("#LBracket").html("����줣�i�j��100!");
		validation=false;
	}
	
	if(!volidateNum($('#Bracket').val())){
		$("#LBracket").html("����쥲������ƼƦr!");
		validation=false;
	}
	
	
	if(mod=='add'){
		$.each(smsSettinglist,function(i,smsSetting){
			if((smsSetting.bracket*100)==$("#Bracket").val()&&smsSetting.pricePlanId==$("#ppId").val()){							
				exist=true;
			}
		});
		
		if(exist){
			$("#LBracket").html("�w�g�����B�ת���ơI");
			validation=false;
		}
	}else{
		if($("#Id").val()==null||$("#Id").val()==""){
			alert("�Х���ܸ�ƥH�i��ק�R��!");
			validation=false;
		}else{
			if(mod=='mod'){
				$.each(smsSettinglist,function(i,smsSetting){
					if((smsSetting.bracket*100)==$("#Bracket").val() && smsSetting.id !=$("#Id").val()&&smsSetting.pricePlanId==$("#ppId").val() ){
						exist=true;
					}
				});
				if(exist){
					$("#LBracket").html("�B�׸�ƭ��ơA�L�k�ק�I");
					validation=false;
				}
			}else if(mod=='del'){
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
function volidateNum(val){
	var   reg=/^\d+$/g;
	return reg.test(val);
}

function updateSetting(mod,txt){
	if (!validat(mod,txt)) return false;
	
	$.ajax({
	      url: '<s:url action="updateSMSSetting"/>',
	      data: {
	    	  "smsSettinglistString":JSON.stringify(smsSettinglist),
	    	  "smsSetting.id":$("#Id").val(),
	    	  "smsSetting.pricePlanId":$("#ppId").val(),
	    	  "smsSetting.bracket":($("#Bracket").val()/100),
	    	  "smsSetting.msg":$("#Msg").val(),
	    	  "smsSetting.suspend":$("#Suspend").is(":checked"),
	    	  "mod":mod
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  $("#BClear").click();
	    	  var list=$.parseJSON(json);
	    	  $("#table1 tr:gt(0)").remove();//����>0����Ūtr
	    	  smsSettinglist=list;
	    	    $.each(list,function(i,smsSetting){  
             var _tr = $(	"<tr>"+
             					"<td align='center' >"+smsSetting.id+"</td>"+
             					"<td align='center' >"+smsSetting.pricePlanId+"</td>"+
             					"<td align='center' >"+(smsSetting.bracket*100)+"</td>"+
             					"<td align='center' >"+smsSetting.msg+"</td>"+
             					"<td align='center' ><input type='checkbox' "+(smsSetting.suspend? "checked='checked'":"")+"disabled='disabled'></td>"+
             					"<td align='center' ><button class='btn btn-primary btn-sm' onclick='chooseRow(this)'>���</button></td>"+
             				"</tr>");  
             
           $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//�_�����˦�
	    	    $("#table1 tr:even").addClass("even_columm");
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); 
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
function clearText(txt){
	$("#L"+txt).html("&nbsp;");
}
//******²�T���e�]�w
var smsContentlist;
var dataList;
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
function queryContent(){

	$.ajax({
	      url: '<s:url action="querySMSContent"/>',
	      data: {},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg2").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  //$("#table2 tr:gt(0)").remove();//����>0����Ūtr
	    	  smsContentlist=list;
	    	  dataList=smsContentlist.slice(0);
	    	  },
	      error: function() { $("#Qmsg2").html('something bad happened');  
	      },
	      beforeSend:function(){
    		  $("#Qmsg2").html("���b�d�ߡA�еy��...");
    			disableButton();
          },
          complete:function(){
        	  enableButton();
        	  pagination();
          }
	    });
}
function updateContent(mod,txt){
	if(!validat2(mod,txt))
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
	    	  $("#Qmsg2").html("Success");
	    	  queryContent();
	      },
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("���b��s�A�еy��...");
    			disableButton();
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
           {name:"button",col:"<td align='center' ><button class='btn btn-primary btn-sm' onclick='chooseRow2(this)'>���</button></td>",_width:"5%"}];

function chooseRow2(bu){
	
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
var exist2;
var validation2;
function validat2(mod,txt){
	exist2=false;
	validation2=true;
	
	if($("#sId").val()==null||$("#sId").val()==""){
		$("#LsId").html("����줣�i����!");
		validation2=false;
	}
	if($("#sContent").val()==null||$("#sContent").val()==""){
		$("#LsContent").html("����줣�i����!");
		validation2=false;
	}
	
	if(!volidateNum($('#sId').val())){
		$("#LsId").html("����쥲������ƼƦr!");
		validation2=false;
	}
	
	$.each(smsContentlist,function(i,smsContent){
		//alert("Search id:"+$("#sId").val()+"="+smsContent.id);
		if(smsContent.id==$("#sId").val()){
			exist2=true;
		}
	});
	
	if(mod=='add'){
		
		if(exist2){
			$("#LsId").html("�w�g��²�T���e�I");
			validation2=false;
		}
		if(!confirm('�T�{�s�W���e?'))
			validation2=false;
	}else{

		if(!exist2){
			$("#LsId").html("�L��ID�A�зs�إߤ@�����e!");
			validation2=false;
		}else{
			if(mod=='mod'){
				if(!confirm('�T�{�ק鷺�e?'))
					validation2=false;
			}else if(mod=='del'){
				if(!confirm('�T�{�R�����e?'))
					validation2=false;
			}
		}
	}
	return validation2;
}

</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height " align="center">
		<h3>²�T�]�w����</h3>
		<ul class="nav nav-tabs" id="tabs" >
			<li ><a href="#tab1" data-toggle="tab">²�T�B�׳]�w</a></li>
			<li class="active"><a href="#tab2" data-toggle="tab">²�T���e�]�w</a></li>
		</ul>
		<div class="tab-content">
		
			<div id="tab1" class="tab-pane">
				<form class="form-horizontal" >
					<div class="form-group" >
					    <label for="Id" class="col-xs-5  control-label">�]�wID:</label>
					    <div class="col-xs-7" align="left">
					    	<input type="text" id="Id" onkeyup="clearText('Id')" disabled="disabled"/>
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <label for="ppId" class="col-xs-5  control-label">��OID:</label>
					    <div class="col-xs-7" align="left">
					    	<input type="text" id="ppId" onkeyup="clearText('ppId')"/>
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LppId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <label for="Bracket" class="col-xs-5 control-label" >�B�פ��(%):</label>
					    <div class="col-xs-7" align="left">
					    	<input type="text" id="Bracket"  onkeyup="clearText('Bracket')" />
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LBracket" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
				    	<label for="Msg" class="col-xs-5 control-label">²�TID�G</label>
					    <div class="col-xs-7" align="left">
					    	<input type="text" id="Msg"  onkeyup="clearText('Msg')" />
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					   		<label id="LMsg" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <label for="Suspend" class="col-xs-5 control-label">�O�_���_�ƾڳs�u�G</label>
					    <div class="col-xs-7" align="left">
					    	<input type="checkbox" id="Suspend" />
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					   		<label id="LSuspend" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    
					    <div class="col-xs-12">
					    	<div class="btn-group" class="col-xs-12">
								<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="�M��" id="BClear">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('add','�s�W')" value="�s�W">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('mod','�ק�')" value="�ק�" style="display: none;">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('del','�R��')" value="�R��">
								<!-- <input type="button" class="btn btn-primary btn-sm" onclick="queryAdmin()" value="�d��"> -->
						    </div>
					    </div>
					    <div class="col-xs-12"><label id="Qmsg" style="height: 20px;width: 100px">&nbsp;</label></div>
					</div>
				</form>
				<div>
					<table class="table-bordered table-hover" align="center" style="width: 80%" id="table1">
						<tr class="even_columm" >
							<td class="columnLabel" align="center" width="20%">�]�wID</td>
							<td class="columnLabel" align="center" width="20%">��OID</td>
							<td class="columnLabel" align="center" width="20%">�B�פ��(%)</td>
							<td class="columnLabel" align="center" width="20%">²�TID</td>
							<td class="columnLabel" align="center" width="20%">�O�_���_�ƾڳs�u</td>
							<td width="15%"></td>
						</tr>
					</table>
				</div>
			</div>
			<div id="tab2" class="tab-pane active">
				<!-- ²�T���e�]�w -->
				<form class="form-horizontal" >
					<div class="form-group">
				    	<label for="sId" class="col-xs-2  control-label">²�TID:</label>
					    <div class="col-xs-4" align="left">
					    	<input type="text" id="sId" onkeyup="clearText('sId')" />
					    </div>
					    
					     <label for="CharSet" class="col-xs-2  control-label">�s�X:</label>
					    <div class="col-xs-4" align="left">
					    	<input type="text" id="CharSet" onkeyup="clearText('CharSet')" />
					    </div>
					    
					    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LsId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LCharSet" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <label for="sContent" class="col-xs-2 control-label" >²�T���e�G</label>
					    <div class="col-xs-4" align="left">
					    	<textarea rows="5" cols="50" id="sContent" onkeyup="clearText('sContent')"></textarea>
					    	<div>
								<label>�ѼƱa�J:</label>
								<input type="button" onclick="replacetxt('{{bracket}}')" value="�B��(NT****)">
								<input type="button" onclick="replacetxt('{{customerService}}')" value="�ȪA�q��(+******)">
							</div>
					    </div>
					    
					    <label for="Discription" class="col-xs-2 control-label" >�����G</label>
					    <div class="col-xs-4" align="left">
					    	<textarea rows="5" cols="50" id="Discription" onkeyup="clearText('Discription')"></textarea>
					    </div>
					    

					    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LsContent" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LDiscription" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
		
					    <div class="col-xs-12">
					    	<div class="btn-group" class="col-xs-12">
								<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="�M��" id="BClear">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('add','�s�W')" value="�s�W">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('mod','�ק�')" value="�ק�">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('del','�R��')" value="�R��">
								<input type="button" class="btn btn-primary btn-sm" onclick="queryContentByID()" value="�HID�d��">
								<!-- <input type="button" class="btn btn-primary btn-sm" onclick="queryAdmin()" value="�d��"> -->
						    </div>
					    </div>
					    <div class="col-xs-12"><label id="Qmsg2" style="height: 20px;width: 100px">&nbsp;</label></div>
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
				<!-- <div>
					<table class="table-bordered " align="center" style="width: 80%">
						<tr class="even_columm" >
							<td class="columnLabel" align="center" width="10%">²�TID</td>
							<td class="columnLabel" align="center" width="40%">²�T���e</td>
							<td class="columnLabel" align="center" width="10%">�s�X</td>
							<td class="columnLabel" align="center" width="35%">����</td>
							<td width="5%"></td>
						</tr>
						<tr>
							<td colspan="5">
								<div style="height: 300px;overflow: auto;">
									<table id="table2" class="table-bordered table-hover" width="100%" >
										<tr>
											<td class="columnLabel" align="center" width="10%"></td>
											<td class="columnLabel" align="center" width="40%"></td>
											<td class="columnLabel" align="center" width="10%"></td>
											<td class="columnLabel" align="center" width="35%"></td>
											<td width="5%"></td>
										</tr>
									</table>
								</div>
							</td>
						</tr>
					</table>
				</div> -->
			</div>
		</div>
	</div>
</div>
</body>
</html>
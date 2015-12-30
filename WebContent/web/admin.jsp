<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
var dataList;
var adminList;

	$(document).ready(function(){
		queryAdmin();
	});
		
		function queryAdmin(){
		
			  $.ajax({
			      url: '<s:url action="queryAdmin"/>',
			      data: {}, //parameters go here in object literal form
			      type: 'POST',
			      datatype: 'json',
			      success: function(json) {  
			    	  $("#Qmsg").html("Success");
			    	  //jQuery.parseJSON,JSON.parse(json)
			    	  //alert(json);
			    	  var list=$.parseJSON(json);
			    	   $("#table1 tr:gt(0)").remove();//����>0����Ūtr
			    	  	adminList=list['data']; 
			    	  	if(adminList!=null)
							dataList=adminList.slice(0);
						
						var error = list['error'];
				    	  $('#Error').html(error);
				    	  
			    	     $.each(dataList,function(i,admin){  
	                      var _tr = $(	"<tr align='center'>"+
	                      					"<td >"+admin.userid+"</td>"+
	                      					"<td>"+admin.account+"</td>"+
	                      					"<td>"+admin.password+"</td>"+
	                      					"<td>"+admin.role+"</td>"+
	                      					"<td><button class='btn btn-primary btn-sm' onclick='chooseRow(this)'>���</button></td>"+
	                      				"</tr>");  
	                      
	                    $("#table1").append(_tr); }); 
			    	    $("#table1 tr:odd").addClass("odd_columm");//�_�����˦�
			    	    $("#table1 tr:even").addClass("even_columm"); 
			    	    
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
		          }
			    }); 
			 $("#Qmsg").html("&nbsp;");
		}
		//�N�Q��ܪ�table����J�s���
		function chooseRow(bu){
			var row =bu.parentNode.parentNode //this ���V button =(parent)> cell =(parent)> row
			//alert(row.cells[0].innerText);
			$("#Userid").val(row.cells[0].innerText);
			$("#Account").val(row.cells[1].innerText);
			$("#Password").val(row.cells[2].innerText);
			$("#Role").val(row.cells[3].innerText);
		}
		//�s�W���
		function updateAdmin(mod,String){
			
			if(confirm("�T�{�n"+String+"��ơH")){
				if(!validateForm(mod)){return}
				$.ajax({
				      url: '<s:url action="updateAdmin"/>',
				      data: { "admin.userid":$("#Userid").val(),
					    	  "admin.account":$("#Account").val(),
					    	  "admin.password":$("#Password").val(),
					    	  "admin.role":$("#Role").val(),
					    	  "mod":mod}, //parameters go here in object literal form
				      type: 'POST',
				      datatype: 'json',
				      success: function(json) { 
				    	  	//alert(json);  
				    	  	if(json=='success'){
				    	  		$("#Qmsg").html("Success");
				    	  		queryAdmin();
				    	  	}else{
				    	  		$("#Qmsg").html(json);
				    	  		}
				    	  	
				    	  	var error = list['error'];
					    	  $('#Error').html(error);
				      },
				      error: function(json) { $("#Qmsg").html('something bad happened');
				      },
			    	  beforeSend:function(){
			    		  $("#Qmsg").html("���b��s�A�еy��...");
			    		  $('#Error').html("");
			    			disableButton();
			          },
			          complete:function(){
			        	  enableButton();
			          }
				    });
			}
		}
		function disableButton(){
			$(':button').attr('disabled', 'disabled');
		}
		function enableButton(){
			$(':button').removeAttr('disabled'); //.attr('disabled', '');
		}
		
		//���ұb���O�_�w�s�b
		var exist;
		function validateText(val){
			exist=false;
			$.each(adminList,function(i,admin){
				if(admin.account==val)	exist=true;
			});
		}
		var validation;
		function validateForm(mod){
			validation=true;//�w�]���ҳq�L

			if(mod!='del'&& $("#Userid").val()==''){
				$("#LUserid").html("�ϥΪ�ID������");
				validation=false;
			}
			if($("#Account").val()==''){
				$("#LAccount").html("�ϥΪ̱b��������");
				validation=false;
			}
			if(mod!='del'&& $("#Password").val()==''){
				$("#LPassword").html("�ϥΪ̱K�X������");
				validation=false;
			}
			if(mod!='del'&& $("#Role").val()==''){
				$("#LRole").html("�ϥΪ̨��⬰����");
				validation=false;
			}
			
			validateText($("#Account").val());
			
			if(mod=='add'&& exist){
				$("#LAccount").html("���b���w�s�b�A�L�k�s�W");
				validation=false;
			}
				
			if(mod!='add'&& !exist){
				$("#LAccount").html("���b�����s�b�A�L�k�i��ק�R��");
				validation=false;
			}
			
			return validation;
		}
		function clearText(item){
			$("#L"+item).html("&nbsp;");
		}
</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height" align="center">
		<form class="form-horizontal" >
		<h3>�ϥΪ̺޲z����</h3>
			<div class="form-group">
			    <label for="Userid" class="col-xs-5  control-label">�ϥΪ�ID:</label>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Userid" onkeyup="clearText('Userid')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			    	<label id="LUserid" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
			    
			    <label for="Account" class="col-xs-5 control-label" >�ϥΪ̱b��:<font color="red">*</font></label>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Account" onkeyup="clearText('Account')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			    	<label id="LAccount" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
			    
		    	<label for="Password" class="col-xs-5 control-label">�ϥΪ̱K�X�G</label>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Password" onkeyup="clearText('Password')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			   		<label id="LPassword" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
			    
			    <label for="Role" class="col-xs-5 control-label">��������G</label>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Role" onkeyup="clearText('Role')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			   		<label id="LRole" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
			    
			    
			    <div class="col-xs-12">
			    	<div class="btn-group" class="col-xs-12">
				    	<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="�M��" class="btn btn-primary">
						<input type="button" class="btn btn-primary btn-sm" onclick="updateAdmin('add','�s�W')" value="�s�W">
						<input type="button" class="btn btn-primary btn-sm" onclick="updateAdmin('mod','�ק�')" value="�ק�">
						<input type="button" class="btn btn-primary btn-sm" onclick="updateAdmin('del','�R��')" value="�R��">
						<input type="button" class="btn btn-primary btn-sm" onclick="queryAdmin()" value="�d��">
				    </div>
			    </div>
			    <div class="col-xs-12"><label id="Qmsg" style="height: 20px;width: 100px">&nbsp;</label></div>
			</div>
		</form>
		<div>
			<table class="table-bordered table-hover" align="center" style="width: 50%" id="table1">
				<tr class="even_columm" >
					<td class="columnLabel" align="center">�ϥΪ�ID</td>
					<td class="columnLabel" align="center">�ϥΪ̱b��</td>
					<td class="columnLabel" align="center">�ϥΪ̱K�X</td>
					<td class="columnLabel" align="center">�������</td>
					<td>&nbsp;</td>
				</tr>
			</table>
		</div>
		<div class="col-xs-12" align="left"> 
			<div id="Error"></div>
		</div>
	</div>
</div>
</body>
</html>
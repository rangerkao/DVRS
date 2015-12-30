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
			    	   $("#table1 tr:gt(0)").remove();//移除>0之後讀tr
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
	                      					"<td><button class='btn btn-primary btn-sm' onclick='chooseRow(this)'>選擇</button></td>"+
	                      				"</tr>");  
	                      
	                    $("#table1").append(_tr); }); 
			    	    $("#table1 tr:odd").addClass("odd_columm");//奇數欄位樣式
			    	    $("#table1 tr:even").addClass("even_columm"); 
			    	    
			    	  },
			      error: function() { $("#Qmsg").html('something bad happened');
			      },
		    	  beforeSend:function(){
		    		  $("#Qmsg").html("正在查詢，請稍待...");
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
		//將被選擇的table欄位放入編輯區
		function chooseRow(bu){
			var row =bu.parentNode.parentNode //this 指向 button =(parent)> cell =(parent)> row
			//alert(row.cells[0].innerText);
			$("#Userid").val(row.cells[0].innerText);
			$("#Account").val(row.cells[1].innerText);
			$("#Password").val(row.cells[2].innerText);
			$("#Role").val(row.cells[3].innerText);
		}
		//新增資料
		function updateAdmin(mod,String){
			
			if(confirm("確認要"+String+"資料？")){
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
			    		  $("#Qmsg").html("正在更新，請稍待...");
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
		
		//驗證帳號是否已存在
		var exist;
		function validateText(val){
			exist=false;
			$.each(adminList,function(i,admin){
				if(admin.account==val)	exist=true;
			});
		}
		var validation;
		function validateForm(mod){
			validation=true;//預設驗證通過

			if(mod!='del'&& $("#Userid").val()==''){
				$("#LUserid").html("使用者ID為必填");
				validation=false;
			}
			if($("#Account").val()==''){
				$("#LAccount").html("使用者帳號為必填");
				validation=false;
			}
			if(mod!='del'&& $("#Password").val()==''){
				$("#LPassword").html("使用者密碼為必填");
				validation=false;
			}
			if(mod!='del'&& $("#Role").val()==''){
				$("#LRole").html("使用者角色為必填");
				validation=false;
			}
			
			validateText($("#Account").val());
			
			if(mod=='add'&& exist){
				$("#LAccount").html("此帳號已存在，無法新增");
				validation=false;
			}
				
			if(mod!='add'&& !exist){
				$("#LAccount").html("此帳號不存在，無法進行修改刪除");
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
		<h3>使用者管理頁面</h3>
			<div class="form-group">
			    <label for="Userid" class="col-xs-5  control-label">使用者ID:</label>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Userid" onkeyup="clearText('Userid')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			    	<label id="LUserid" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
			    
			    <label for="Account" class="col-xs-5 control-label" >使用者帳號:<font color="red">*</font></label>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Account" onkeyup="clearText('Account')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			    	<label id="LAccount" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
			    
		    	<label for="Password" class="col-xs-5 control-label">使用者密碼：</label>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Password" onkeyup="clearText('Password')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			   		<label id="LPassword" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
			    
			    <label for="Role" class="col-xs-5 control-label">角色分類：</label>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Role" onkeyup="clearText('Role')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			   		<label id="LRole" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
			    
			    
			    <div class="col-xs-12">
			    	<div class="btn-group" class="col-xs-12">
				    	<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="清除" class="btn btn-primary">
						<input type="button" class="btn btn-primary btn-sm" onclick="updateAdmin('add','新增')" value="新增">
						<input type="button" class="btn btn-primary btn-sm" onclick="updateAdmin('mod','修改')" value="修改">
						<input type="button" class="btn btn-primary btn-sm" onclick="updateAdmin('del','刪除')" value="刪除">
						<input type="button" class="btn btn-primary btn-sm" onclick="queryAdmin()" value="查詢">
				    </div>
			    </div>
			    <div class="col-xs-12"><label id="Qmsg" style="height: 20px;width: 100px">&nbsp;</label></div>
			</div>
		</form>
		<div>
			<table class="table-bordered table-hover" align="center" style="width: 50%" id="table1">
				<tr class="even_columm" >
					<td class="columnLabel" align="center">使用者ID</td>
					<td class="columnLabel" align="center">使用者帳號</td>
					<td class="columnLabel" align="center">使用者密碼</td>
					<td class="columnLabel" align="center">角色分類</td>
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
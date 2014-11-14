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
var smsSettinglist;
$(function(){
	query();
})
function query(){
	$("#Qmsg").html("正在查尋，請稍待...");
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
	    	  $("#table1 tr:gt(0)").remove();//移除>0之後讀tr
	    	  smsSettinglist=list;
	    	    $.each(list,function(i,smsSetting){  
               var _tr = $(	"<tr>"+
               					"<td align='center' >"+smsSetting.id+"</td>"+
               					"<td align='center' >"+smsSetting.bracket+"</td>"+
               					"<td align='center' >"+smsSetting.msg+"</td>"+
               					"<td align='center' ><input type='checkbox' "+(smsSetting.suspend? "checked='checked'":"")+"disabled='disabled'></td>"+
               					"<td align='center' ><button onclick='chooseRow(this)'>選擇</button></td>"+
               				"</tr>");  
               
             $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//奇數欄位樣式
	    	    $("#table1 tr:even").addClass("even_columm");
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened');  }
	    });
}
function validate(){
	var validation=true;

	return validation;
}
//將被選擇的table欄位放入編輯區
function chooseRow(bu){
	
	var row =bu.parentNode.parentNode //this 指向 button =(parent)> cell =(parent)> row
	//alert(row.cells[0].innerText);
	$("#Id").val(row.cells[0].innerText);
	$("#Bracket").val(row.cells[1].innerText);
	$("#LBracket").html("");
	$("#Msg").val(row.cells[2].innerText);
	$("#LMsg").html("");
	$("#Suspend").prop("checked",(row.cells[3].childNodes[0].checked));//在向下一層尋找到checkbox
	
}
var exist;
var validation;
function validat(mod,txt){
	exist=false;
	validation=true;
	
	if($("#Bracket").val()==null||$("#Bracket").val()==""){
		$("#LBracket").html("此欄位不可為空!");
		validation=false;
	}
	if($("#Msg").val()==null||$("#Msg").val()==""){
		$("#LMsg").html("此欄位不可為空!");
		validation=false;
	}
	
	if($('#Bracket').val()>1){
		$("#LBracket").html("此欄位不可大於1!");
		validation=false;
	}
	
	if(!volidateNum($('#Bracket').val())){
		$("#LBracket").html("此欄位必須為小數數字!");
		validation=false;
	}
	
	
	if(mod=='add'){
		alert('add模式');
		$.each(smsSettinglist,function(i,smsSetting){
			if(smsSetting.bracket==$("#Bracket").val()){
				exist=true;
			}
		});
		
		if(exist){
			$("#LBracket").html("已經有此額度的資料！");
			validation=false;
		}
	}else{
		if($("#Id").val()==null||$("#Id").val()==""){
			alert("請先選擇資料以進行修改刪除!");
			validation=false;
		}else{
			if(mod=='mod'){
				alert('mod模式');
				$.each(smsSettinglist,function(i,smsSetting){
					if(smsSetting.bracket==$("#Bracket").val() && smsSetting.id !=$("#Id").val() ){
						exist=true;
					}
				});
				if(exist){
					$("#LBracket").html("額度資料重複，無法修改！");
					validation=false;
				}
			}else if(mod=='del'){
				alert('del模式');
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
	var   reg=/^\d+(\.\d+)?$/g;
	return reg.test(val);
}

function updateSetting(mod,txt){
	if (!validat(mod,txt)) return false;
	$.ajax({
	      url: '<s:url action="updateSMSSetting"/>',
	      data: {
	    	  "smsSettinglistString":JSON.stringify(smsSettinglist),
	    	  "smsSetting.id":$("#Id").val(),
	    	  "smsSetting.bracket":$("#Bracket").val(),
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
	    	  $("#table1 tr:gt(0)").remove();//移除>0之後讀tr
	    	  smsSettinglist=list;
	    	    $.each(list,function(i,smsSetting){  
             var _tr = $(	"<tr>"+
             					"<td align='center' >"+smsSetting.id+"</td>"+
             					"<td align='center' >"+smsSetting.bracket+"</td>"+
             					"<td align='center' >"+smsSetting.msg+"</td>"+
             					"<td align='center' ><input type='checkbox' "+(smsSetting.suspend? "checked='checked'":"")+"disabled='disabled'></td>"+
             					"<td align='center' ><button onclick='chooseRow(this)'>選擇</button></td>"+
             				"</tr>");  
             
           $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//奇數欄位樣式
	    	    $("#table1 tr:even").addClass("even_columm");
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("正在查詢，請稍待...");
    			disableButton();
          },
          complete:function(){
        	  enableButton();
          }
	    });
}
function clearText(txt){
	$("#L"+txt).html("");
}
function replacetxt(txt){
	//$("#Msg").val($("#Msg").val()+txt);
}

</script>
</head>
<body>
<div align="center" >
	<h3>簡訊設定頁面</h3>
	<div>
		<form>
		<table >
			<tr>
				<td class="label" align="right"><label>ID:</label></td>
				<td><input type="text" id="Id" onkeyup="clearText('Id')" disabled="disabled"/></td>
				<td><label id="LId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>		
			</tr>
			<tr>
				<td class="label" align="right"><label>BRACKET:</label></td>
				<td><input type="text" id="Bracket"  onkeyup="clearText('Bracket')" /></td>
				<td><label id="LBracket" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>
			</tr>
			<tr>
				<td class="label" align="right" valign="top"><label>MSGID:</label></td>
				<td><input type="text" id="Msg"  onkeyup="clearText('Msg')" /></td>
				<td><label id="LMsg" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>
			</tr>
			<tr>
				<td class="label" align="right"><label>SUSPEND:</label></td>
				<td><input type="checkbox" id="Suspend" /></td>
				<td><label id="LSuspend" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>
			</tr>
			<tr>
				<td>
					<input type="button"  onclick="this.form.reset()" value="清除" id="BClear">
					<input type="button" onclick="updateSetting('add','新增')" value="新增">
					<input type="button" onclick="updateSetting('mod','修改')" value="修改">
					<input type="button" onclick="updateSetting('del','刪除')" value="刪除">
					<!-- <input type="button" onclick="queryAdmin()" value="查詢"> -->
					<br><label id="Qmsg" style="height: 50px;width: 100px">&nbsp;</label>
				</td>
			</tr>
		</table>
		</form>
	</div>
	<div>
		<table class="datatable" align="center" style="width: 80%" id="table1">
			<tr class="even_columm" >
				<td class="columnLabel" align="center" width="20%">ID</td>
				<td class="columnLabel" align="center" width="20%">BRACKET</td>
				<td class="columnLabel" align="center" width="25%">MSG</td>
				<td class="columnLabel" align="center" width="20%">SUSPEND</td>
				<td width="15%"></td>
			</tr>
		</table>
	</div>
	<div>
		<label>參數帶入:</label>
		<input type="button" onclick="replacetxt('{{bracket}}')" value="警示額度">
	</div>
</div>
</body>
</html>
<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
       <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script type="text/javascript">
var smsSettinglist;
var dataList;


var tHead=[{name:"設定ID",col:"id",_width:"20%"},
           {name:"額度比例(%)",col:"bracket",_width:"20%"},
           {name:"簡訊ID",col:"msg",_width:"20%"},
           {name:"checkbox",col:"suspend",_width:"20%"},
           {name:"button",col:"<td align='center' ><button class='btn btn-primary btn-sm' onclick='chooseRow(this)'>選擇</button></td>",_width:"15%"}];

$(function(){
	query();
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
	    	  //$("#table2 tr:gt(0)").remove();//移除>0之後讀tr
	    	  smsSettinglist=list['data'];
	    	  if(smsSettinglist!=null)
	    		  dataList=smsSettinglist.slice(0);
	    	  
	    	  var error = list['error'];
	    	  $('#Error').html(error);
    	  },
	      error: function() { 
	    	  $("#Qmsg").html('something bad happened');  
	      },
	      beforeSend:function(){
	    	  $("#Qmsg").html("正在查尋，請稍待...");
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

	return validation;
}
//將被選擇的table欄位放入編輯區
function chooseRow(bu){
	
	var row =bu.parentNode.parentNode //this 指向 button =(parent)> cell =(parent)> row
	//alert(row.cells[0].innerText);
	$("#Id").val(row.cells[0].innerText);
	//$("#ppId").val(row.cells[1].innerText);
	//$("#LppId").html("&nbsp;");
	$("#Bracket").val(row.cells[1].innerText);
	$("#LBracket").html("&nbsp;");
	$("#Msg").val(row.cells[2].innerText);
	$("#LMsg").html("&nbsp;");
	$("#Suspend").prop("checked",(row.cells[3].childNodes[0].checked));//在向下一層尋找到checkbox
	
}
var exist;
var validation;
function validat(mod,txt){
	exist=false;
	validation=true;
	
	/* if($("#ppId").val()==null||$("#ppId").val()==""){
		$("#LppId").html("此欄位不可為空!");
		validation=false;
	} */
	
	if($("#Bracket").val()==null||$("#Bracket").val()==""){
		$("#LBracket").html("此欄位不可為空!");
		validation=false;
	}
	if($("#Msg").val()==null||$("#Msg").val()==""){
		$("#LMsg").html("此欄位不可為空!");
		validation=false;
	}
	
	if($('#Bracket').val()>100){
		$("#LBracket").html("此欄位不可大於100!");
		validation=false;
	}
	
	if(!volidateNum($('#Bracket').val())){
		$("#LBracket").html("此欄位必須為整數數字!");
		validation=false;
	}
	
	
	if(mod=='add'){
		$.each(smsSettinglist,function(i,smsSetting){
			if((smsSetting.bracket)==$("#Bracket").val()/* &&smsSetting.pricePlanId==$("#ppId").val() */){							
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
				$.each(smsSettinglist,function(i,smsSetting){
					if((smsSetting.bracket)==$("#Bracket").val() && smsSetting.id !=$("#Id").val()/* &&smsSetting.pricePlanId==$("#ppId").val() */ ){
						exist=true;
					}
				});
				if(exist){
					$("#LBracket").html("額度資料重複，無法修改！");
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
	    	  //"smsSetting.pricePlanId":$("#ppId").val(),
	    	  "smsSetting.bracket":($("#Bracket").val()),
	    	  "smsSetting.msg":$("#Msg").val(),
	    	  "smsSetting.suspend":$("#Suspend").is(":checked"),
	    	  "mod":mod
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var list=$.parseJSON(json);
	    	  $('#bClear').click();
	    	  
	    	  smsSettinglist=list['data'];
	    	  if(smsSettinglist!=null)
	    		  dataList=smsSettinglist.slice(0);
	    	  
	    	    var error = list['error'];
		    	  $('#Error').html(error);
    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("正在查尋，請稍待...");
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
function clearText(txt){
	$("#L"+txt).html("&nbsp;");
}

</script>
</head>
<body>
<div class="container-fluid max_height" style="vertical-align: middle;">
	<div class="row max_height " align="center">
		<form class="form-horizontal" >
			<h3>簡訊警示設定頁面</h3>
			<div class="col-xs-5" align="right"><label for="Id">設定ID:</label></div>
		    <div class="col-xs-7" align="left">
		    	<input type="text" id="Id" onkeyup="clearText('Id')" disabled="disabled"/>
		    </div>
		    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		    <div class="col-xs-5" align="right"><label for="Bracket">額度比例(%):</label></div>
		    <div class="col-xs-7" align="left">
		    	<input type="text" id="Bracket"  onkeyup="clearText('Bracket')" />
		    </div>
		    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LBracket" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		    <div class="col-xs-5" align="right"><label for="Msg">簡訊ID：</label></div>
		    <div class="col-xs-7" align="left">
		    	<input type="text" id="Msg"  onkeyup="clearText('Msg')" />
		    </div>
		    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
		   		<label id="LMsg" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		    <div class="col-xs-5" align="right"><label for="Suspend">是否中斷數據連線：</label></div>
		    <div class="col-xs-7" align="left">
		    	<input type="checkbox" id="Suspend" />
		    </div>
		    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
		   		<label id="LSuspend" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		
		    <div class="col-xs-12">
		    	<div class="btn-group" class="col-xs-12">
					<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="清除" id="bClear">
					<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('add','新增')" value="新增">
					<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('mod','修改')" value="修改" style="display: none;">
					<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('del','刪除')" value="刪除">
					<!-- <input type="button" class="btn btn-primary btn-sm" onclick="queryAdmin()" value="查詢"> -->
			    </div>
		    </div>
	    </form>
	    <div class="col-xs-12"><label id="Qmsg" style="height: 30px;">&nbsp;</label></div>
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
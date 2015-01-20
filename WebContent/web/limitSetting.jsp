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
           {name:"門號",col:"msisdn",_width:"20%"},
           {name:"建立時間",col:"createDate",_width:"20%"},
           {name:"狀態",col:"status",_width:"20%"},
           {name:"button",col:"<td align='center' ><button onclick='chooseRow(this)' class='btn btn-primary btn-sm'>選擇</button></td>",_width:"20%"}];
var reportName="VIP客戶設定";
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
	    	  //$("#table1 tr:gt(0)").remove();//移除>0之後讀tr
	    	  limitList=list;
	    	  dataList=limitList.slice(0);
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened');  
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("正在查尋，請稍待...");
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

//將被選擇的table欄位放入編輯區
function chooseRow(bu){
	var row =bu.parentNode.parentNode //this 指向 button =(parent)> cell =(parent)> row
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
		alert("IMSI不可為空");
		return;
	}
	
	
	if (!validat(mod,txt)) return false;
	
	
	var sendSMS=false;
	if(mod=="add"){
		if(confirm("需要發送通知簡訊給予客戶嗎？")){
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
    		  $("#Qmsg").html("正在更新，請稍待...");
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
		$("#LMsisdn").html('此欄位不可為空');
		return
	}
	if(!volidateNum($("#Msisdn").val())){
		$("#LMsisdn").html('格式錯誤，必須為純數字');
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
	    		  $("#Qmsg").html("此門號無對應IMSI，操作失敗");
	    	  }else{
	    		  $("#IMSI").val(v.imsi);
	    		  updateLimit(mod,txt);
	    	  }
    	  },
	      error: function(json) {
	    	  $("#Qmsg").html('something bad happened'); 
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("正在查詢，請稍待...");
    			disableButton();
    			$("#IMSI").val("");
          },
          complete:function(){
        	  enableButton();
          }
	    });
}
 //20150106 add 刪除可只靠imsi
function validat(mod,txt){
	
	var validate = true;
	
	if(mod!='del' && ($("#Msisdn").val()==null ||$("#Msisdn").val()=="")){
		$("#LMsisdn").html('此欄位不可為空');
		validate = false; 
	}
	if($("#Limit").val()==null ||$("#Limit").val()==""){
		$("#LLimit").html('此欄位不可為空');
		validate = false;
	}
	
	if(mod!='del' && !volidateNum($("#Msisdn").val())){
		$("#LMsisdn").html('格式錯誤，必須為純數字');
		validate = false;
	}
	if(!volidateNum($("#Limit").val())){
		$("#LLimit").html('格式錯誤，必須為純數字');
		validate = false;
	}
	
	if(validate){
		if(mod=='add'){			
			//確認資料有沒有在
			 $.each(limitList,function(i,limit){
				 if(limit.msisdn==$("#Msisdn").val()){
					 alert("此門號已設定上限，無法新增!");
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
				 alert("此門號未曾設定過，無法刪除,修改！");
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
		$("#L"+txt).html("格式錯誤，必須為純數字"); */
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
			<h3>警示上限設定頁面</h3>
			<div class="col-xs-5" align="right"><label for="IMSI">IMSI:</label></div>
		    <div class="col-xs-7" align="left" >
		    	<input type="text" id="IMSI" onkeyup="clearText('IMSI')" disabled="disabled"/>
		    </div>
		    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LIMSI" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
		    </div>
		    <div class="col-xs-5" align="right"><label for="Msisdn">門號:<font color="red">*</font></label></div>
		    <div class="col-xs-7" align="left">
		    	<input type="text" id="Msisdn" onkeyup="clearText('Msisdn')" />
		    </div>
		    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
		    	<label id="LMsisdn" >查詢時可使用"*"取代某區段號碼進行模糊查詢</label>
		    </div>
		    <div class="col-xs-5" align="right"><label for="Status">狀態:</label></div>
		    <div class="col-xs-7" align="left">
			    <label class="radio-inline">
					<input type="radio" name="Status" id="Status1" value="Normal"> Normal
				</label>
				<label class="radio-inline">
					<input type="radio" name="Status" id="Status2" value="Inactive"> Inactive
				</label>
				<label class="radio-inline">
					<input type="radio" name="Status" id="Status3" value=" "> 空值
				</label>
		    </div>
		    <div style="display: none;" class="col-xs-12">
		    	<div class="col-xs-5" align="right"><label for="Limit">門最大上限:</label></div>
			    <div class="col-xs-7" align="left">
			    	<input type="text" id="Limit"  onkeyup="clearText('Limit')" />
			    </div>
			    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
			    	<label id="LLimit" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
			    </div>
		    </div>
		    <div class="col-xs-12">
		    	<div class="btn-group" class="col-xs-12">
			    	<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="清除" id="BClear">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryIMSI('add','新增')" value="新增">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryIMSI('mod','修改')" value="修改" style="display: none;">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryIMSI('del','刪除')" value="刪除">
					<input type="button" class="btn btn-primary btn-sm" onclick="queryVIP()" value="查詢"> 
					<input type="button" class="btn btn-primary btn-sm" onclick="createExcel()" value="下載Excel"> 
			    </div>
		    </div>
		    <div class="col-xs-12"><label id="Qmsg" style="height: 20px;width: 100px">&nbsp;</label></div>

		</form>
		<div class="col-xs-12"> 
			<button type="button" name="Previous"  class="pagination btn btn-warning"><span class="glyphicon glyphicon-chevron-left"></span> Previous</button>
			<label id="nowPage"></label>
			<button type="button" name="Next" class="pagination btn btn-warning"> <span class="glyphicon glyphicon-chevron-right"></span> Next</button>
			<label id="totalPage" style="margin-right: 10px"></label>
			<label>每頁筆數</label>
			<input id="rown" type="text" value="10" width="5px">
			<input type="button" onclick="pagination()" class="btn btn-primary btn-sm" style="margin: 20px"  value="重新分頁">
		</div>
		<div class="col-xs-12"> 
			<div id="page_contain"></div>
		</div>
	</div>
</div>
</body>
</html>
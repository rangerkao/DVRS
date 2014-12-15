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
	    	  $("#table1 tr:gt(0)").remove();//移除>0之後讀tr
	    	  smsSettinglist=list;
	    	    $.each(list,function(i,smsSetting){  
               var _tr = $(	"<tr>"+
               					"<td align='center' >"+smsSetting.id+"</td>"+
               					"<td align='center' >"+smsSetting.pricePlanId+"</td>"+
               					"<td align='center' >"+(smsSetting.bracket*100)+"</td>"+
               					"<td align='center' >"+smsSetting.msg+"</td>"+
               					"<td align='center' ><input type='checkbox' "+(smsSetting.suspend? "checked='checked'":"")+"disabled='disabled'></td>"+
               					"<td align='center' ><button class='btn btn-primary btn-sm' onclick='chooseRow(this)'>選擇</button></td>"+
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
function validate(){
	var validation=true;

	return validation;
}
//將被選擇的table欄位放入編輯區
function chooseRow(bu){
	
	var row =bu.parentNode.parentNode //this 指向 button =(parent)> cell =(parent)> row
	//alert(row.cells[0].innerText);
	$("#Id").val(row.cells[0].innerText);
	$("#ppId").val(row.cells[1].innerText);
	$("#LppId").html("&nbsp;");
	$("#Bracket").val(row.cells[2].innerText);
	$("#LBracket").html("&nbsp;");
	$("#Msg").val(row.cells[3].innerText);
	$("#LMsg").html("&nbsp;");
	$("#Suspend").prop("checked",(row.cells[4].childNodes[0].checked));//在向下一層尋找到checkbox
	
}
var exist;
var validation;
function validat(mod,txt){
	exist=false;
	validation=true;
	
	if($("#ppId").val()==null||$("#ppId").val()==""){
		$("#LppId").html("此欄位不可為空!");
		validation=false;
	}
	
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
			if((smsSetting.bracket*100)==$("#Bracket").val()&&smsSetting.pricePlanId==$("#ppId").val()){							
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
					if((smsSetting.bracket*100)==$("#Bracket").val() && smsSetting.id !=$("#Id").val()&&smsSetting.pricePlanId==$("#ppId").val() ){
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
	    	  $("#table1 tr:gt(0)").remove();//移除>0之後讀tr
	    	  smsSettinglist=list;
	    	    $.each(list,function(i,smsSetting){  
             var _tr = $(	"<tr>"+
             					"<td align='center' >"+smsSetting.id+"</td>"+
             					"<td align='center' >"+smsSetting.pricePlanId+"</td>"+
             					"<td align='center' >"+(smsSetting.bracket*100)+"</td>"+
             					"<td align='center' >"+smsSetting.msg+"</td>"+
             					"<td align='center' ><input type='checkbox' "+(smsSetting.suspend? "checked='checked'":"")+"disabled='disabled'></td>"+
             					"<td align='center' ><button class='btn btn-primary btn-sm' onclick='chooseRow(this)'>選擇</button></td>"+
             				"</tr>");  
             
           $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//奇數欄位樣式
	    	    $("#table1 tr:even").addClass("even_columm");
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); 
	      },
    	  beforeSend:function(){
    		  $("#Qmsg").html("正在更新，請稍待...");
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
//******簡訊內容設定
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
	
/*     $("#table2 tr:odd").addClass("odd_columm");//奇數欄位樣式
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
	    	  //$("#table2 tr:gt(0)").remove();//移除>0之後讀tr
	    	  smsContentlist=list;
	    	  dataList=smsContentlist.slice(0);
	    	  },
	      error: function() { $("#Qmsg2").html('something bad happened');  
	      },
	      beforeSend:function(){
    		  $("#Qmsg2").html("正在查詢，請稍待...");
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
    		  $("#Qmsg").html("正在更新，請稍待...");
    			disableButton();
          },
          complete:function(){
        	  //enableButton();
          }
	    });
}

var tHead=[{name:"簡訊ID",col:"id",_width:"10%"},
           {name:"簡訊內容",col:"comtent",_width:"40%"},
           {name:"編碼",col:"charSet",_width:"10%"},
           {name:"說明",col:"description",_width:"35%"},
           {name:"button",col:"<td align='center' ><button class='btn btn-primary btn-sm' onclick='chooseRow2(this)'>選擇</button></td>",_width:"5%"}];

function chooseRow2(bu){
	
	var row =bu.parentNode.parentNode //this 指向 button =(parent)> cell =(parent)> row
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
		$("#LsId").html("此欄位不可為空!");
		validation2=false;
	}
	if($("#sContent").val()==null||$("#sContent").val()==""){
		$("#LsContent").html("此欄位不可為空!");
		validation2=false;
	}
	
	if(!volidateNum($('#sId').val())){
		$("#LsId").html("此欄位必須為整數數字!");
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
			$("#LsId").html("已經有簡訊內容！");
			validation2=false;
		}
		if(!confirm('確認新增內容?'))
			validation2=false;
	}else{

		if(!exist2){
			$("#LsId").html("無此ID，請新建立一筆內容!");
			validation2=false;
		}else{
			if(mod=='mod'){
				if(!confirm('確認修改內容?'))
					validation2=false;
			}else if(mod=='del'){
				if(!confirm('確認刪除內容?'))
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
		<h3>簡訊設定頁面</h3>
		<ul class="nav nav-tabs" id="tabs" >
			<li ><a href="#tab1" data-toggle="tab">簡訊額度設定</a></li>
			<li class="active"><a href="#tab2" data-toggle="tab">簡訊內容設定</a></li>
		</ul>
		<div class="tab-content">
		
			<div id="tab1" class="tab-pane">
				<form class="form-horizontal" >
					<div class="form-group" >
					    <label for="Id" class="col-xs-5  control-label">設定ID:</label>
					    <div class="col-xs-7" align="left">
					    	<input type="text" id="Id" onkeyup="clearText('Id')" disabled="disabled"/>
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <label for="ppId" class="col-xs-5  control-label">資費ID:</label>
					    <div class="col-xs-7" align="left">
					    	<input type="text" id="ppId" onkeyup="clearText('ppId')"/>
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LppId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <label for="Bracket" class="col-xs-5 control-label" >額度比例(%):</label>
					    <div class="col-xs-7" align="left">
					    	<input type="text" id="Bracket"  onkeyup="clearText('Bracket')" />
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LBracket" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
				    	<label for="Msg" class="col-xs-5 control-label">簡訊ID：</label>
					    <div class="col-xs-7" align="left">
					    	<input type="text" id="Msg"  onkeyup="clearText('Msg')" />
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					   		<label id="LMsg" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <label for="Suspend" class="col-xs-5 control-label">是否中斷數據連線：</label>
					    <div class="col-xs-7" align="left">
					    	<input type="checkbox" id="Suspend" />
					    </div>
					    <div class="col-xs-12 alert_msg" style="margin: opx;padding: 0px">
					   		<label id="LSuspend" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    
					    <div class="col-xs-12">
					    	<div class="btn-group" class="col-xs-12">
								<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="清除" id="BClear">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('add','新增')" value="新增">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('mod','修改')" value="修改" style="display: none;">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateSetting('del','刪除')" value="刪除">
								<!-- <input type="button" class="btn btn-primary btn-sm" onclick="queryAdmin()" value="查詢"> -->
						    </div>
					    </div>
					    <div class="col-xs-12"><label id="Qmsg" style="height: 20px;width: 100px">&nbsp;</label></div>
					</div>
				</form>
				<div>
					<table class="table-bordered table-hover" align="center" style="width: 80%" id="table1">
						<tr class="even_columm" >
							<td class="columnLabel" align="center" width="20%">設定ID</td>
							<td class="columnLabel" align="center" width="20%">資費ID</td>
							<td class="columnLabel" align="center" width="20%">額度比例(%)</td>
							<td class="columnLabel" align="center" width="20%">簡訊ID</td>
							<td class="columnLabel" align="center" width="20%">是否中斷數據連線</td>
							<td width="15%"></td>
						</tr>
					</table>
				</div>
			</div>
			<div id="tab2" class="tab-pane active">
				<!-- 簡訊內容設定 -->
				<form class="form-horizontal" >
					<div class="form-group">
				    	<label for="sId" class="col-xs-2  control-label">簡訊ID:</label>
					    <div class="col-xs-4" align="left">
					    	<input type="text" id="sId" onkeyup="clearText('sId')" />
					    </div>
					    
					     <label for="CharSet" class="col-xs-2  control-label">編碼:</label>
					    <div class="col-xs-4" align="left">
					    	<input type="text" id="CharSet" onkeyup="clearText('CharSet')" />
					    </div>
					    
					    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LsId" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <div class="col-xs-6 alert_msg" style="margin: opx;padding: 0px">
					    	<label id="LCharSet" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
					    </div>
					    
					    <label for="sContent" class="col-xs-2 control-label" >簡訊內容：</label>
					    <div class="col-xs-4" align="left">
					    	<textarea rows="5" cols="50" id="sContent" onkeyup="clearText('sContent')"></textarea>
					    	<div>
								<label>參數帶入:</label>
								<input type="button" onclick="replacetxt('{{bracket}}')" value="額度(NT****)">
								<input type="button" onclick="replacetxt('{{customerService}}')" value="客服電話(+******)">
							</div>
					    </div>
					    
					    <label for="Discription" class="col-xs-2 control-label" >說明：</label>
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
								<input type="button" class="btn btn-primary btn-sm" onclick="this.form.reset()" value="清除" id="BClear">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('add','新增')" value="新增">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('mod','修改')" value="修改">
								<input type="button" class="btn btn-primary btn-sm" onclick="updateContent('del','刪除')" value="刪除">
								<input type="button" class="btn btn-primary btn-sm" onclick="queryContentByID()" value="以ID查詢">
								<!-- <input type="button" class="btn btn-primary btn-sm" onclick="queryAdmin()" value="查詢"> -->
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
					<label>每頁筆數</label>
					<input id="rown" type="text" value="10" width="5px">
					<input type="button" onclick="pagination()" class="btn btn-primary btn-sm" style="margin: 20px"  value="重新分頁">
				</div>
				<div class="col-xs-12"> 
					<div id="page_contain"></div>
				</div>
				<!-- <div>
					<table class="table-bordered " align="center" style="width: 80%">
						<tr class="even_columm" >
							<td class="columnLabel" align="center" width="10%">簡訊ID</td>
							<td class="columnLabel" align="center" width="40%">簡訊內容</td>
							<td class="columnLabel" align="center" width="10%">編碼</td>
							<td class="columnLabel" align="center" width="35%">說明</td>
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
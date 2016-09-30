<%@ page language="java" contentType="text/html; charset=BIG5"
	pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<style type="text/css">
#menu{
padding: none;

}
</style>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script type="text/javascript">
$(document).ready(function(){
	//$("li").css("display","none");
	queryAuth();
	$('#elseList').append('<li><p class="text-left"><a id=logoutLink href="<s:url action="logoutLink"/>">�n�X</a></p></li>');
});


var mousePos;
function handleMouseMove(event) {
	//alert("mouse");
    event = event || window.event; // IE-ism
    mousePos = {
    		x: event.clientX,
            y: event.clientY
    };
   	$("#x").html(mousePos.x); 
   	$("#y").html(mousePos.y); 
}
window.onmousemove = handleMouseMove;


/**
 * �갵���m�۰ʵn�X�A�H�ʱ��ƹ����Ц�m����¦
 */
var oldx,oldy;
var checkPeriod=1000*5; //�C5���ˬd�@��
var logOutminute=20//���m�n�X�ɶ��]�����^
var logOutTime=1000*60*logOutminute;
var count=logOutTime/checkPeriod;

	window.setInterval(function() {
		check()
	}, checkPeriod);
	function check() {
		var newx = $("#x").html();
		var newy = $("#y").html();
		//alert(newx + ":" + newy);
		if (newx == oldx && newy == oldy) {
			count -= 1;
		} else {
			count = logOutTime / checkPeriod;
		}
		oldx = newx;
		oldy = newy;
		
		if (count <= 0) {
			alert("�w���m�W�L" + logOutminute + "�����A�t�Φ۰ʵn�X");
			document.getElementById('logoutLink').click();
		}
	}
/*
 *�v���d��
 */
 
 function queryAuth(){
		$("#menuAuth").html("���b�d�M�A�еy��...");
		  $.ajax({
		      url: '<s:url action="queryAuth"/>',
		      data: {}, //parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json) {  
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
		    	  var list=$.parseJSON(json);
		    	     $.each(list['data'],function(i,Auth){
		    	   
		    	    	 $('#'+Auth.belong).append('<li><p class="text-left"><a id='+Auth.action+' href="<s:url action="'+Auth.action+'"/>">'+Auth.name+'</a></p></li>');
		    	     }); 
		    	  },
		      error: function() { $("#menuAuth").html('something bad happened');}
		    }); 
		 $("#menuAuth").html("&nbsp;");
	}
	
	

	function volidateNum(val){
		var   reg=/^\d+$/g;
		return reg.test(val);
	}
	
	
	
	
	function tqueryIMSI(){
		
		if($("#tText").val()==null || $("#tText").val()=="" ||$("#tText").val()=="�п�J"){
			alert('�d��IMSI�ɡA�������i����');
			return
		}
		if(!volidateNum($("#tText").val())){
			alert('������J�榡���~');
			return
		}

		$.ajax({
		      url: '<s:url action="queryIMSI"/>',
		      data: {
		    	  "msisdn":$("#tText").val(),
		      },//parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json) {  
		    	  
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
		    	  var v=JSON.parse(json);
		    	  if(json=="" || v.imsi==null || v.imsi==""){
		    		  alert("�d�LIMSI");
		    	  }else{
		    		  $("#tLabel").val(v.imsi);
		    	  }
		    	  
		    	  var error = list['error'];
		    	  if(error!=null)
		    		  alert(error);
	    	  },
		      error: function(json) {
		    	  alert('something bad happened'); 
		      },
	    	  beforeSend:function(){
    			//disableButton();
	    		  $("#tLabel").val("");
	          },
	          complete:function(){
	        	  //enableButton();
	          }
		    });
	}

	function tqueryMSISDN(){
		
		if($("#tText").val()==null || $("#tText").val()=="" ||$("#tText").val()=="�п�J"){
			alert('�d�ߪ�����IMSI���i����');
			return
		}
		
		if(!volidateNum($("#tText").val())){
			alert('IMSI��J�榡���~');
			return
		}

		$.ajax({
		      url: '<s:url action="queryMSISDN"/>',
		      data: {
		    	  "imsi":$("#tText").val(),
		      },//parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json) {  
		    	  
		    	  //jQuery.parseJSON,JSON.parse(json)
		    	  //alert(json);
		    	  var v=JSON.parse(json);
		    	  if(json=="" || v.msisdn==null || v.msisdn==""){
		    		 alert("�d�L����");
		    	  }else{
		    		  $("#tLabel").val(v.msisdn);
		    	  }
		    	  var error = list['error'];
		    	  if(error!=null)
		    		  alert(error);
	    	  },
		      error: function(json) {
		    	  alert('something bad happened'); 
		      },
	    	  beforeSend:function(){
	    			//disableButton();
	    		  $("#tLabel").val("");
	          },
	          complete:function(){
	        	  //enableButton();
	          }
		    });
	}
	
function tqueryTWNMSISDN(tText){
		
		if(!tText||tText==""||tText=="�п�J"){
			alert('�d�ߪ����ɤ��i����');
			return
		}
		
		if(!volidateNum(tText)){
			alert('������J�榡���~');
			return
		}

		$.ajax({
		      url: '<s:url action="queryTWNMSISDN"/>',
		      data: {
		    	  "msisdn":tText,
		      },//parameters go here in object literal form
		      type: 'POST',
		      datatype: 'json',
		      success: function(json) {  
		    	  var v=JSON.parse(json);
		    	  
		    	  if(v && v['error']){
		    		  alert(v['error']);
		    	  }else{
			    	  if(json=="" || v.msisdn==null || v.msisdn==""){
			    		 alert("�d�L����");
			    	  }else{
			    		  $("#tLabel").val(v.msisdn);
			    	  }
		    	  }
	    	  },
		      error: function(json) {
		    	  alert('something bad happened'); 
		      },
	    	  beforeSend:function(){
	    			//disableButton();
	    		  $("#tLabel").val("");
	          },
	          complete:function(){
	        	  //enableButton();
	          }
		    });
	}
	
function tqueryS2TMSISDN(tText){
	
	if(!tText||tText==""||tText=="�п�J"){
		alert('�d�ߪ����ɤ��i����');
		return
	}
	
	if(!volidateNum(tText)){
		alert('������J�榡���~');
		return
	}

	$.ajax({
	      url: '<s:url action="queryS2TMSISDN"/>',
	      data: {
	    	  "msisdn":tText,
	      },//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  var v=JSON.parse(json);
	    	  
	    	  if(v && v['error']){
	    		  alert(v['error']);
	    	  }else{
		    	  if(json=="" || v.msisdn==null || v.msisdn==""){
		    		 alert("�d�L����");
		    	  }else{
		    		  $("#tLabel").val(v.msisdn);
		    	  }
	    	  }
    	  },
	      error: function(json) {
	    	  alert('something bad happened'); 
	      },
    	  beforeSend:function(){
    			//disableButton();
    		  $("#tLabel").val("");
          },
          complete:function(){
        	  //enableButton();
          }
	    });
}
	
	
</script>
</head>
<body>
<div class="container-fluid max_height" >
	<div class="row max_height" align="center" style="vertical-align: top;">
		<!-- <div class="col-xs-12" style="visibility: hidden;">
			<label id="x" >x.index</label>
			<label id="y" >y.index</label>
			<label id="menuAuth"></label>
		</div> -->
		<div><font size="3px">Sim2Travel ���B�޲z�t��</font></div>
		<div class="col-xs-12" style="padding-right: 0px;padding-left: 0px;">
			<div class="col-xs-12">
				<!-- <input 	type="text" id="tLabel" class="col-xs-12" disabled="disabled"> -->
				<input 	type="text" id="tText" class="col-xs-12"
							value="�п�J" style="color: #AAAAAA;" 
							onfocus="if (this.value == '�п�J') {this.value = ''; this.style.color='#333333'}" 
							onblur="if (this.value == '') {this.value = '�п�J'; this.style.color='#AAAAAA'}"  >
			</div>
			<div class="dropdown col-xs-12" >
				<button class="btn btn-primary btn-xs col-xs-12 dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown" aria-expanded="true" style="z-index: 1;">
				    ��ܬd�߶���
				<span class="caret"></span>
				</button>
				<ul class="dropdown-menu dropdown-menu-center" role="menu" aria-labelledby="dropdownMenu1" style="left: 35px;height: 50px;padding-top: 0px;">
					<li role="presentation"><input 	type="button" class="btn btn-primary btn-xs col-xs-12" onclick="tqueryMSISDN()" value="IMSI�d�ߪ���"></li>
					<li role="presentation"><input 	type="button" class="btn btn-primary btn-xs col-xs-12" onclick="tqueryIMSI()" value="�����d��IMSI"></li>
					<li role="presentation"><input 	type="button" class="btn btn-primary btn-xs col-xs-12" onclick="tqueryS2TMSISDN($('#tText').val())" value="���ت����d�߭���D��"></li>
					<li role="presentation"><input 	type="button" class="btn btn-primary btn-xs col-xs-12" onclick="tqueryTWNMSISDN($('#tText').val())" value="����D���d�ߤ��ت���"></li>
				</ul>
			</div>
			<div class="col-xs-12">
				<input 	type="text" id="tLabel" class="col-xs-12" disabled="disabled">
			</div>
		</div>
		
		<!-- <div class="col-xs-12">
			<input 	type="text" id="tIMSI" class="col-xs-12"
					value="�п�JIMSI" style="color: #AAAAAA;" 
					onfocus="if (this.value == '�п�JIMSI') {this.value = ''; this.style.color='#333333'}" 
					onblur="if (this.value == '') {this.value = '�п�JIMSI'; this.style.color='#AAAAAA'}"  >
			<input 	type="button" class="btn btn-primary btn-xs col-xs-12" onclick="tqueryMSISDN()" value="IMSI�d�ߪ���">
			<input 	type="text" id="tMsisdn" class="col-xs-12"
					value="�п�J����" style="color: #AAAAAA;" 
					onfocus="if (this.value == '�п�J����') {this.value = ''; this.style.color='#333333'}" 
					onblur="if (this.value == '') {this.value = '�п�J����'; this.style.color='#AAAAAA'}" >
			<input 	type="button" class="btn btn-primary btn-xs col-xs-12" onclick="tqueryIMSI()" value="�����d��IMSI"> 
		</div> -->
		<div class="col-xs-12">
			<ul id="menu">
				<li>
					<p class="text-left">�ϥΪ̺޲z</p>
					<ul id="adminList">
					</ul>
				</li>
				<li>
					<p class="text-left">DVRS�d��</p>
					<ul id="searchList">
					</ul>
				</li>
				<li>
					<p class="text-left">DVRS�]�w</p>
					<ul id="settingList">
					</ul>
				</li>
				<li>
					<p class="text-left">��L</p>
					<ul id="elseList">
					</ul>
				</li>
			</ul>
		</div>
	</div>
</div>
</body>
</html>
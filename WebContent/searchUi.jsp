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
	
})
function queryIMSI(){
	
	if($("#Msisdn").val()==null || $("#Msisdn").val()==""){
		$("#LMsisdn").html('����줣�i����');
		return
	}
	if(!volidateNum($("#Msisdn").val())){
		$("#LMsisdn").html("�榡���~�A�������¼Ʀr");
		return
	}
	$("#Qmsg").html("���b�d�ߡA�еy��...");
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
	    	  
	    	  var item=$.parseJSON(json);
	    	  if(json==null || json=="{}"){
	    		  $("#Qmsg").html("�d�LIMSI");
	    	  }else{
	    		  $("#PRICAPLAIN_ID").val(item.pricaplainid);
	    		  $("#IMSI").val(item.imsi);
	    		  $("#Qmsg").html("Success");
	    	  }
	    	  },
	      error: function(json) { $("#Qmsg").html('something bad happened'); }
	    });
}
function clearText(txt){
	$("#L"+txt).html("&nbsp;");

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
<div>
	
	<div>
		<form>
			<table style="margin-left: 40%;">
				<tr>
					<td>&nbsp;</td>
					<td><h3>�d�ߤu�㭶��</h3></td>
					<td>&nbsp;</td>
				</tr>
				<tr>
					<td class="label" align="right"><label>PRICAPLAIN_ID:</label></td>
					<td><input type="text" id="PRICAPLAIN_ID" onkeyup="clearText('Msisdn')" disabled="disabled"/></td>
					<td><label id="LPRICAPLAIN_ID" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>		
				</tr>
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
					<td>&nbsp;</td>
					<td colspan="2"><input type="button" onclick="queryIMSI()" value="�d��IMSI"></td>
				</tr>
				<tr>
					<td><label id="Qmsg" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>	
				</tr>
			</table>
		</form>
	</div>
</div>
</body>
</html>
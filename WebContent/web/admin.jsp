<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
<%@taglib uri="/struts-tags" prefix="s"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<style type="text/css">

#table1, #table1 td, #table1 th {
	border:1px solid #FF7573;
	border-color: #FF7573
}
#table1 td {
	align:center
}
.datatable{
	width:50%;
	background-color: #FF7573
}
.odd_columm{
	background-color: #FEFFAF
}
.even_columm{
	background-color: #C7FF91
}
.label{
	
}

</style>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script type="text/javascript">
	$(document).ready(function(){
		queryAdmin();
	});
		var adminList;
		function queryAdmin(){
			 $.ajax({
			      url: '<s:url action="queryAdmin"/>',
			      data: {}, //parameters go here in object literal form
			      type: 'POST',
			      datatype: 'json',
			      success: function(json) {  
			    	  //jQuery.parseJSON,JSON.parse(json)
			    	  //alert(json);
			    	  var list=$.parseJSON(json);
			    	  $("#table1 tr:gt(0)").remove();//����>0����Ūtr
			    	  	adminList=list;
			    	    $.each(list,function(i,admin){  
	                      var _tr = $(	"<tr>"+
	                      					"<td align='center'>"+admin.userid+"</td>"+
	                      					"<td align='center'>"+admin.account+"</td>"+
	                      					"<td align='center'>"+admin.password+"</td>"+
	                      					"<td align='center'>"+admin.role+"</td>"+
	                      					"<td ><button onclick='chooseRow(this)'>���</button></td>"+
	                      				"</tr>");  
	                      
	                    $("#table1").append(_tr); });
			    	    $("#table1 tr:odd").addClass("odd_columm");//�_�����˦�
			    	    $("#table1 tr:even").addClass("even_columm");
			    	  },
			      error: function() { alert('something bad happened'); }
			    });
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
				
				alert("updateAdmin clicked!");
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
				    	  		alert("Add Success!")
				    	  		queryAdmin();
				    	  	}else{
				    	  			alert(json);
				    	  		}},
				      error: function(json) { alert('insert error!'+(json)); }
				    });
			}
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
			$("#L"+item).html("");
		}
</script>
</head>
<body>
<div align="center">
	<div>
		<form>
		<table>
			<tr>
				<td class="label" align="right"><label>USERID:</label></td>
				<td><input type="text" id="Userid" onkeyup="clearText('Userid')" /></td>
				<td><label id="LUserid" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>		
			</tr>
			<tr>
				<td class="label" align="right"><label>ACCOUNT:</label></td>
				<td><input type="text" id="Account" onkeyup="clearText('Account')" /></td>
				<td><label id="LAccount" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>
			</tr>
			<tr>
				<td class="label" align="right"><label>PASSWORD:</label></td>
				<td><input type="text" id="Password" onkeyup="clearText('Password')" /></td>
				<td><label id="LPassword" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>
			</tr>
			<tr>
				<td class="label" align="right"><label>ROLE:</label></td>
				<td><input type="text" id="Role" onkeyup="clearText('Role')" /></td>
				<td><label id="LRole" >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label></td>
			</tr>
			<tr>
				<td>
					<input type="button"  onclick="this.form.reset()" value="�M��">
					<input type="button" onclick="updateAdmin('add','�s�W')" value="�s�W">
					<input type="button" onclick="updateAdmin('mod','�ק�')" value="�ק�">
					<input type="button" onclick="updateAdmin('del','�R��')" value="�R��">
					<input type="button" onclick="queryAdmin()" value="�d��">
				</td>
			</tr>
		</table>
		</form>
	</div>
	<div >
		<table id="table1" class="datatable" align="center">
			<tr class="even_columm" >
				<td class="columnLabel" align="center">USERID</td>
				<td class="columnLabel" align="center">ACCOUNT</td>
				<td class="columnLabel" align="center">PASSWORD</td>
				<td class="columnLabel" align="center">ROLE</td>
				<td>&nbsp;</td>
			</tr>
		</table>
	</div>
</div>
</body>
</html>
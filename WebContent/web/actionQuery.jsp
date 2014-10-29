<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
    <%@taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
<script src="http://code.jquery.com/jquery-latest.js"></script>
 <link rel="stylesheet" href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css">
  <script src="//code.jquery.com/jquery-1.10.2.js"></script>
  <script src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
<script type="text/javascript">
$(function() {
    $(".datapicker").datepicker({
        showOn: "button",
        buttonImage: "source/icon.png",
        buttonImageOnly: true,
        buttonText: "Select date",
        dateFormat: 'yy-mm-dd'
    });
  });
var actionLoglist;
function query(){
	if(!validate()) return flase;
	$("#Qmsg").html("���b�d�M�A�еy��...");
	$.ajax({
	      url: '<s:url action="queryActionLog"/>',
	      data: {	"dateFrom":$("#dateFrom").val(),
	    	  		"dateTo":$("#dateTo").val()
	    	  		},//parameters go here in object literal form
	      type: 'POST',
	      datatype: 'json',
	      success: function(json) {  
	    	  $("#Qmsg").html("Success");
	    	  //jQuery.parseJSON,JSON.parse(json)
	    	  //alert(json);
	    	  var list=$.parseJSON(json);
	    	  $("#table1 tr:gt(0)").remove();//����>0����Ūtr
	    	  actionLoglist=list;
	    	    $.each(list,function(i,actionLoglist){  
               var _tr = $(	"<tr>"+
               					"<td align='center' width='9%'>"+actionLoglist.id+"</td>"+
               					"<td align='center' width='15%'>"+actionLoglist.account+"</td>"+
               					"<td align='center' width='20%'>"+actionLoglist.page+"</td>"+
               					"<td align='center' width='20%'>"+actionLoglist.action+"</td>"+
               					"<td align='center' width='18%'>"+actionLoglist.parameter+"</td>"+
               					"<td align='center' width='8%'>"+actionLoglist.createDate+"</td>"+
               					//"<td align='center'><button onclick='chooseRow(this)'>���</button></td>"+
               				"</tr>");  
               
             $("#table1").append(_tr); });
	    	    $("#table1 tr:odd").addClass("odd_columm");//�_�����˦�
	    	    $("#table1 tr:even").addClass("even_columm");
	    	  },
	      error: function() { $("#Qmsg").html('something bad happened'); }
	    });
}
function validate(){
	var validation=true;
	if((($("#dateFrom").val()==null||$("#dateFrom").val()=="")^($("#dateTo").val()==null||$("#dateTo").val()==""))){
		alert("��������P�ɶ�άҤ���I")
		validation=false;
	}else
	if($("#dateFrom").val()>$("#dateTo").val()){
		alert("�}�l������i�j�󵲧�����I")
		validation=false;
	}
	return validation;
}
function clearDate(){
	alert("clear clicked");
	$("#dateFrom").val("");
	$("#dateTo").val("");
}

</script>
</head>
<body>
<div align="center" >
	<h3>�ϥΪ̾ާ@�����d��</h3>
	<div>
		�d�ߴ����q
		<input type="text"  disabled="disabled" id="dateFrom" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px ">
		��
		<input type="text" disabled="disabled" id="dateTo" class="datapicker" style="height: 25px;text-align: center;position:relative;top: -5px" >
		<input type="button" onclick="query()" value="�d��">
		<input type="button" onclick="clearDate()" value="�M��">
		<br><label id="Qmsg" style="height: 50px;width: 100px">&nbsp;</label>
		</div>
		
	<div>
		<table class="datatable" align="center" style="width: 80%">
			<tr class="even_columm" >
				<td class="columnLabel" align="center" width="9%">ID</td>
				<td class="columnLabel" align="center" width="15%">ACCOUNT</td>
				<td class="columnLabel" align="center" width="20%">PAGE</td>
				<td class="columnLabel" align="center" width="20%">ACTION</td>
				<td class="columnLabel" align="center" width="20%">PARAMETER</td>
				<td class="columnLabel" align="center" width="5%">CREATE_DATE</td>
				<td width="1%">&nbsp;</td>
			</tr>
			<tr>
				<td colspan="6" >
					<div style="height: 500px;overflow: auto;">
					<table id="table1" class="wapper">
						<tr><td colspan="10">&nbsp;</td></tr>
					</table>
					</div>
				</td>
			</tr>
		</table>
	</div>
</div>
</body>
</html>
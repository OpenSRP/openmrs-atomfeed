<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<h3>Atomfeed Configuration</h3>
<script type="text/javascript">

window.onbeforeunload = function (e) {
	  var e = e || window.event;
	  // For IE and Firefox
	  if (e) {
	    e.returnValue = "You have attempted to leave this page.  If you have made any changes to the fields, click the Save button, otherwise your changes will be lost.  Are you sure you want to exit this page?";
	  }
	  // For Safari
	  return "You have attempted to leave this page.  If you have made any changes to the fields, click the Save button, otherwise your changes will be lost.  Are you sure you want to exit this page?";
	};
	
</script>
<form method="POST">
    <table>
    	<openmrs:portlet url="globalProperties" parameters="title=${title}|propertyPrefix=atomfeed.|excludePrefix=atomfeed.started"/>
    </table>
</form>

<%@ include file="/WEB-INF/template/footer.jsp"%>
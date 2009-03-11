<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2>Register a webservice</h2>
</div>
<div>
<form action="${pageContext.request.contextPath}/register/check.htm">
    <label for="url">Enter a webservice url</label>
    <input name="url" type="text" size="100" value="http://tapir.austmus.gov.au/tapirlink/tapir.php/ozcam"/>
    <input type="submit" value="submit"/>
</form>
</div>
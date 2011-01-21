<%@ include file="/common/taglibs.jsp"%>
<ala:loginStatus returnUrlPath="http://${pageContext.request.serverName}${pageContext.request.serverPort!=80 ? ':' : ''}${pageContext.request.serverPort!=80 ? pageContext.request.serverPort : ''}${pageContext.request.contextPath}/occurrences/${occurrence.id}"/>
<div class="submitButtons">
    <span class="btn"><input type="button" value="Cancel" class="close"></span>
</div>
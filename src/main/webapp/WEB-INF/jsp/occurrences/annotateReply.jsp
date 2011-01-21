<%@ include file="/common/taglibs.jsp"%>
<div class="boxes">
    <div id="replyTo" class="window">
    	<h2>Annotation Reply</h2>
<c:choose>
<c:when test="${empty pageContext.request.userPrincipal}">
	<jsp:include page="annotateAuth.jsp"/>
</c:when>
<c:otherwise>
        <form action="${pageContext.request.contextPath}/annotation/saveAnnotation" method="post" name="replyToForm" id="replyToForm">
        <table>
          <jsp:include page="annotateCommon1.jsp"/>
        </table>
        <input type="hidden" name="type" value="reply" id="type"/>
        <input type="hidden" name="field" value="" id="field"/>
        <input type="hidden" name="rootAnnotation" id="rootAnnotation"/>
        <jsp:include page="annotateCommon2.jsp"><jsp:param name="section" value="reply" /></jsp:include>
        </form>
        </form>
</c:otherwise>
</c:choose>
    </div>
</div>
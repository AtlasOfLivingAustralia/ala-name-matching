<%@ include file="/common/taglibs.jsp"%>
<div id="topmenu">
<ul>
<li><a href="${pageContext.request.contextPath}/"><spring:message code='topmenu.home'/></a></li>		
<tiles:insert page="mainMenu.jsp"/>
</ul>
</div><!-- End topmenu-->
<%-- 
    Document   : loginMsg
    Created on : Aug 18, 2010, 3:42:26 PM
    Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"

getPathTranslated

${pageContext.request.contextPath}
--%>
<%@ include file="/common/taglibs.jsp" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<c:set var="port">
    <c:if test="${pageContext.request.serverPort != 80}">
        :${pageContext.request.serverPort}
    </c:if>
</c:set>
<c:set var="urlString" value="http://${pageContext.request.serverName}${port}${requestScope['javax.servlet.forward.request_uri']}"/>
<c:set var="queryString" value="${pageContext.request.queryString}"/>
<c:choose>
    <c:when test="${empty queryString}">
        <c:set var="requestUrl" value="${urlString}"/>
    </c:when>
    <c:otherwise>
        <c:set var="requestUrl" value="${urlString}?${fn:replace(queryString, '+', '%2B')}"/>
    </c:otherwise>
</c:choose>
<div class="section">
    <div style="border-top: 1px solid #DDD; margin: 10px 0; padding: 10px 0;">
        You are not logged in. Please <ala:loginLogoutLink returnUrlPath="${requestUrl}"/> to submit a sighting.
    </div>
</div>

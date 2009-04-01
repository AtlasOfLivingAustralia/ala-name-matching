<%@ include file="/common/taglibs.jsp" %>
<%@ attribute name="geoRegionType" required="true" rtexprvalue="true" type="org.ala.web.util.RegionType" %>
<%@ attribute name="requestedType" required="true" rtexprvalue="true" type="java.lang.String" %>
<c:if test="${not empty geoRegionType}">
    <c:choose>
        <c:when test="${geoRegionType.name == requestedType}">
            <li id="chosen"><spring:message code="regions.list.${requestedType}.shorttitle"/></li>
        </c:when>
        <c:otherwise>
            <li><a href="${pageContext.request.contextPath}/regions/browse/${requestedType}"><spring:message code="regions.list.${requestedType}.shorttitle"/></a></li>
        </c:otherwise>
    </c:choose>
</c:if>
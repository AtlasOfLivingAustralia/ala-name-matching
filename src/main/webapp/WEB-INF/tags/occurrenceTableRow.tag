<%@ include file="/common/taglibs.jsp" %><%@
    attribute name="fieldName" required="true" type="java.lang.String" %><%@
    attribute name="fieldNameIsMsgCode" required="false" type="java.lang.Boolean" %><%@
    attribute name="fieldCode" required="false" type="java.lang.String" %><%@
    attribute name="section" required="true" type="java.lang.String" %><%@
    attribute name="annotate" required="true" type="java.lang.Boolean" %><%@
    attribute name="path" required="false" type="java.lang.String" %><%@
    attribute name="guid" required="false" type="java.lang.String" %>
<c:set var="bodyText"><jsp:doBody/></c:set>
<c:set var="annoIcon"><c:if test="${annotate}">${section}</c:if></c:set>
<c:choose>
<c:when test="${not empty guid}">
  <c:set var="link">${path}${guid}</c:set>  
</c:when>
<c:otherwise>
  <c:set var="link"></c:set>
</c:otherwise>
</c:choose>
<c:if test="${not empty bodyText}">
    <tr id="${fieldCode}">
        <th>
            <c:choose>
                <c:when test="${fieldNameIsMsgCode}"><fmt:message key="${fieldName}"/></c:when>
                <c:otherwise>${fieldName}</c:otherwise>
            </c:choose>
        </th>
        <td class="annoText" name="${fieldCode}"></td>
        <td class="value">
            <c:if test="${not empty link}"><a href="${link}"></c:if>${bodyText}<c:if test="${not empty link}"></a></c:if>
            <div class="annoText"></div>
        </td>
        <td class="${annoIcon}" name="${fieldCode}"></td>
    </tr>
</c:if>
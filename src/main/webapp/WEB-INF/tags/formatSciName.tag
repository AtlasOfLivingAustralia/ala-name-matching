<%@ include file="/common/taglibs.jsp" %><%@
attribute name="rankId" required="true" type="java.lang.String" %><%@
attribute name="name" required="true" type="java.lang.String" %><c:choose><c:when test="${rankId >= 6000}"><i>${name}</i></c:when><c:otherwise>${name}</c:otherwise></c:choose>
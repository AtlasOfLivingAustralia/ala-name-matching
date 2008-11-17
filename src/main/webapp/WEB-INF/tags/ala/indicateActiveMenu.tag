<%@ include file="/common/taglibs.jsp"%>
<%@ attribute name="menuName" required="true" rtexprvalue="true" type="java.lang.String"%>
<c:if test="${fn:contains(servletName, menuName)}"><c:out value=" class=active-menu"/></c:if>

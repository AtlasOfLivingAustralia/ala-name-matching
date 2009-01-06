<%@ include file="/common/taglibs.jsp"%>
<%
Object contextAttribute = request.getAttribute("org.springframework.web.servlet.DispatcherServlet.CONTEXT");
String servletName = ((org.springframework.web.context.support.XmlWebApplicationContext)contextAttribute).getNamespace();
request.setAttribute("servletName",servletName);
%>
<li<alatag:indicateActiveMenu menuName="taxonomy"/>><a href="${pageContext.request.contextPath}/species/" title="<spring:message code='topmenu.species.title'/>"><spring:message code='topmenu.species'/></a></li>
<li<alatag:indicateActiveMenu menuName="region"/>><a href="${pageContext.request.contextPath}/regions/" title="<spring:message code='topmenu.regions.title'/>"><spring:message code='topmenu.regions'/></a></li>
<li<alatag:indicateActiveMenu menuName="dataset"/>><a href="${pageContext.request.contextPath}/datasets/" title="<spring:message code='topmenu.datasets.title'/>"><spring:message code='topmenu.datasets'/></a></li>
<li<alatag:indicateActiveMenu menuName="occurrence"/>><a href="${pageContext.request.contextPath}/occurrences/" title="<spring:message code='topmenu.occurrences.title'/>"><spring:message code='topmenu.occurrences'/></a></li>
<li<alatag:indicateActiveMenu menuName="main"/>><a href="${pageContext.request.contextPath}/settings.htm" title="<spring:message code='topmenu.settings.title'/>"><spring:message code='topmenu.settings'/></a></li>
<li<alatag:indicateActiveMenu menuName="tutorial"/>><a href="${pageContext.request.contextPath}/tutorial/" title="<spring:message code='topmenu.about.title'/>"><spring:message code='topmenu.about'/></a></li>
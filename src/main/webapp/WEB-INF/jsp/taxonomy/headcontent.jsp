<%@ include file="/common/taglibs.jsp"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='speciesGlobal.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='filters.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='tables.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='googlemap.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='gbifmap.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='tree.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='wizards.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='gbifmap.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='autocomplete.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='download.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='speciesSpecial.css'/>"/>
<c:if test="${empty regionConcepts}">
  <jsp:include page="/common/scripts.jsp"/>
  <jsp:include page="/WEB-INF/jsp/mapping/headcontent.jsp"/>
</c:if>
<!-- YUI CSS files: -->
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='yui-datatable.css'/>"/>
<!-- YUI JS files: -->
<script src="${pageContext.request.contextPath}/javascript/YUI/yahoo-dom-event.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/connection-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/element-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/datasource-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/datatable-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/json-min.js" type="text/javascript" language="javascript"></script>

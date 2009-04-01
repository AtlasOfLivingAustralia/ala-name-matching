<%@ include file="/common/taglibs.jsp"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='speciesGlobal.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='regionsSpecial.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='tables.css'/>"/>
<!-- YUI CSS files: -->
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='yui-datatable.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='yui-paginator.css'/>"/>
<!-- YUI JS files: -->
<script src="${pageContext.request.contextPath}/javascript/YUI/yahoo-dom-event.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/connection-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/element-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/paginator-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/datasource-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/datatable-min.js" type="text/javascript" language="javascript"></script>
<script src="${pageContext.request.contextPath}/javascript/YUI/json-min.js" type="text/javascript" language="javascript"></script>
<!-- Mapping JS files: -->
<jsp:include page="/WEB-INF/jsp/mapping/headcontent.jsp"/>
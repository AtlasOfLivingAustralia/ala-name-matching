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
<!-- Combo-handled YUI CSS files: -->
<link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/combo?2.7.0/build/datatable/assets/skins/sam/datatable.css">
<!-- Combo-handled YUI JS files: -->
<script type="text/javascript" src="http://yui.yahooapis.com/combo?2.7.0/build/yahoo-dom-event/yahoo-dom-event.js&2.7.0/build/connection/connection-min.js&2.7.0/build/element/element-min.js&2.7.0/build/datasource/datasource-min.js&2.7.0/build/datatable/datatable-min.js&2.7.0/build/json/json-min.js"></script>
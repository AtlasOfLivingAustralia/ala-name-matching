<%@ include file="/common/taglibs.jsp"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='map.css'/>"/>
<jsp:include page="/WEB-INF/jsp/geography/googleMapKey.jsp"/>
<script src="http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=euzuro-openlayers"></script>
<c:set var="geoserverUrl" scope="request"><gbif:propertyLoader bundle="portal" property="geoserver.url"/></c:set>
<script src="${geoserverUrl}/openlayers/OpenLayers.js"></script>
<script src="${pageContext.request.contextPath}/javascript/mapping.js"></script>
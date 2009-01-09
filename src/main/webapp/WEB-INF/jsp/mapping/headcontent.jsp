<%@ include file="/common/taglibs.jsp"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='map.css'/>"/>
<jsp:include page="/WEB-INF/jsp/geography/googleMapKey.jsp"/>
<script src='http://maps.google.com/maps?file=api&amp;v=2&amp;key=${googleKey}'></script>
<script src="http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=euzuro-openlayers"></script>
<script src="http://localhost:8080/geoserver/openlayers/OpenLayers.js"></script>
<script src="${pageContext.request.contextPath}/javascript/mapping.js"></script>
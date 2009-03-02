<%@ include file="/common/taglibs.jsp"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='map.css'/>"/>
<c:set var="workOffline" scope="request"><gbif:propertyLoader bundle="portal" property="workOffline"/></c:set>
<c:if test="${!workOffline}">
  <jsp:include page="/WEB-INF/jsp/geography/googleMapKey.jsp"/>
  <!-- <script src="http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=euzuro-openlayers"></script> -->
  <script src="${pageContext.request.contextPath}/javascript/YUI-utilities.js"></script>
  <script type="text/javascript">
    var YMAPPID = "euzuro-openlayers";
  </script>
  <script type="text/javascript" src="http://us.js2.yimg.com/us.js.yimg.com/lib/map/js/api/ymapapi_3_4_1_7.js"></script>
</c:if>
<c:set var="geoserverUrl" scope="request"><gbif:propertyLoader bundle="portal" property="geoserver.url"/></c:set>
<script src="${geoserverUrl}/openlayers/OpenLayers.js"></script>
<script src="${pageContext.request.contextPath}/javascript/mapping.js"></script>
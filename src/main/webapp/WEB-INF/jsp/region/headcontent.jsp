<%@ include file="/common/taglibs.jsp"%><link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='speciesGlobal.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='countriesSpecial.css'/>"/>
<tiles:insert page="../geography/googleMapKey.jsp"/>
<script src='http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6.2&mkt=en-us'></script>
<script src='http://maps.google.com/maps?file=api&amp;v=2&amp;key=${googleKey}'></script>
<script src="http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=euzuro-openlayers"></script>


<script src="http://localhost:8080/geoserver/openlayers/OpenLayers.js"></script>
<script src="${pageContext.request.contextPath}/javascript/mapping.js"></script>
 <style>
     #openLayersMap {
         width: 1024px;
         height: 500px;
         border: 1px solid #cccccc;
         text-align: left;
         margin: 10px 0px 10px 0px;
     }
     img.mapImage {
         width: 1000px;
         height: 768px;
         border: 1px solid #cccccc;
         text-align: left;
         margin: 10px 0px 10px 0px;
     }
 </style>
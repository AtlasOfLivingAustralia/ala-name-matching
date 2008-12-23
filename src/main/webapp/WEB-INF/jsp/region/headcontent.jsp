<%@ include file="/common/taglibs.jsp"%><link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='speciesGlobal.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='countriesSpecial.css'/>"/>
<tiles:insert page="../geography/googleMapKey.jsp"/>
<script src='http://maps.google.com/maps?file=api&amp;v=2&amp;key=${googleKey}'></script>
<script src="http://localhost:8080/geoserver/openlayers/OpenLayers.js"></script>
 <style>
     #openLayersMap {
         width: 1024px;
         height: 700px;
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
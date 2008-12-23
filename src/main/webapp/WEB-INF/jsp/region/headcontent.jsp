<%@ include file="/common/taglibs.jsp"%><link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='speciesGlobal.css'/>"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/<spring:theme code='countriesSpecial.css'/>"/>
 <script src="http://localhost:8080/geoserver/openlayers/OpenLayers.js"></script>
 <style>
     #openLayersMap {
         width: 1024px;
         height: 512px;
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
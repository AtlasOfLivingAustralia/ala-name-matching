<%@ include file="/common/taglibs.jsp"%>
<c:set var="tilecacheUrl" scope="request"><gbif:propertyLoader bundle="portal" property="tilecache.url"/></c:set>
<c:set var="geoserverUrl" scope="request"><gbif:propertyLoader bundle="portal" property="geoserver.url"/></c:set>
<c:set var="bluemarbleUrl" scope="request"><gbif:propertyLoader bundle="portal" property="bluemarble.layer.url"/></c:set>
<c:set var="cellDensityLayerUrl" scope="request"><gbif:propertyLoader bundle="portal" property="celldensity.layer.url"/></c:set>
<script type="text/javascript">
    var map;
    var tilecacheUrl = '${tilecacheUrl}';
    var geoserverUrl = '${geoserverUrl}';
    var bluemarbleUrl = '${bluemarbleUrl}';
    var cellDensityLayerUrl = '${cellDensityLayerUrl}';
    var useGoogle = ${param['map']=='google' ? 'true': 'false'};
    var brokenContentSize = document.getElementById('content').offsetWidth == 0;
    var extraParams = '${extraParams}';
    /**
     * Redirects to filter search with bounding box
     */
    function redirectToCell (minX, minY, maxX, maxY){
        document.location.href = "${pageContext.request.contextPath}/occurrences/boundingBoxWithCriteria.htm?"
                +extraParams                
                +"&minX="+minX
                +"&minY="+minY
                +"&maxX="+maxX
                +"&maxY="+maxY;
    }
</script>
<script src="${pageContext.request.contextPath}/javascript/layers.js" type="text/javascript" language="javascript"></script>
<div id="map" class="openlayersMap"></div>
<style type="text/css">
    /* Styling for custom widget buttons on map */
    .olControlPanel div { 
      display:block;
      position: relative;
      top: 350px;
      left: 10px;
      width:  24px;
      height: 24px;
      margin: 5px;
      background-color:red;
    }

    .olControlPanel .olControlNavigationItemActive {
      background-color: blue;
      background-image: url("${pageContext.request.contextPath}/images/panning-hand-on.png");
    }

    .olControlPanel .olControlNavigationItemInactive {
      background-color: orange;
      background-image: url("${pageContext.request.contextPath}/images/panning-hand-off.png");
    }
    
    .olControlPanel .olControlZoomBoxItemInactive { 
      width:  22px;  
      height: 22px;
      background-color: orange;
      background-image: url("${pageContext.request.contextPath}/images/drag-rectangle-off.png");
    }

    .olControlPanel .olControlZoomBoxItemActive { 
      width:  22px;  
      height: 22px;
      background-color: blue;
      background-image: url("${pageContext.request.contextPath}/images/drag-rectangle-on.png");
    }
</style>
<script type="text/javascript">
    var mapDivId='map';
    initMap(mapDivId, useGoogle);
    initLayers();
    zoomToBounds();
    window.onload = handleResize;
    window.onresize = handleResize;
</script>
<p>
<c:choose>
    <c:when test="${param['map']=='google'}">
        <a href="${pageContext.request.contextPath}/${entityPath}/${entityId}">
          Use geoserver base layers
        </a>
    </c:when>
    <c:otherwise>
        <a href="${pageContext.request.contextPath}/${entityPath}/${entityId}?map=google">
          Use google base layers
        </a>
    </c:otherwise>
</c:choose>
</p>
<!--<p>
    <a href="javascript:toggleBaseLayer();">Toggle Base Layer <span id="baseLayer"></span></a>
</p>-->
<p>
    <a href="javascript:toggleSelectCentiCell();">Select centi cell</a>
</p>
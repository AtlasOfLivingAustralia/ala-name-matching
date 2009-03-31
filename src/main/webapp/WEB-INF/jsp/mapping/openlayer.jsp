<%@ include file="/common/taglibs.jsp"%>
<c:set var="tilecacheUrl" scope="request"><gbif:propertyLoader bundle="portal" property="tilecache.url"/></c:set>
<c:set var="polygonTilecacheUrl" scope="request"><gbif:propertyLoader bundle="portal" property="polygon.tilecache.url"/></c:set>
<c:set var="geoserverUrl" scope="request"><gbif:propertyLoader bundle="portal" property="geoserver.url"/></c:set>
<c:set var="bluemarbleUrl" scope="request"><gbif:propertyLoader bundle="portal" property="bluemarble.layer.url"/></c:set>
<c:set var="cellDensityLayerUrl" scope="request"><gbif:propertyLoader bundle="portal" property="celldensity.layer.url"/></c:set>
<script type="text/javascript">
    var map;
    var tilecacheUrl = '${tilecacheUrl}';
    var polygonTilecacheUrl = '${polygonTilecacheUrl}';
    var geoserverUrl = '${geoserverUrl}';
    var bluemarbleUrl = '${bluemarbleUrl}';
    var cellDensityLayerUrl = '${cellDensityLayerUrl}';
    var useGoogle = ${param['map']=='google' ? 'true': 'false'};
    var brokenContentSize = document.getElementById('content').offsetWidth == 0;
    var extraParams = '${extraParams}';
    var cellButton;
    var baseLayerButton;
    var pageUrl = "${pageContext.request.contextPath}/${entityPath}/${entityId}";
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
      top: 330px;
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
      /* width:  24px;
      height: 22px; */
      background-color: orange;
      background-image: url("${pageContext.request.contextPath}/images/drag-rectangle-off.png");
    }

    .olControlPanel .olControlZoomBoxItemActive { 
      /* width:  24px;
      height: 22px;*/
      background-color: blue;
      background-image: url("${pageContext.request.contextPath}/images/drag-rectangle-on.png");
    }

    .olControlPanel .selectCellsButtonItemActive {
        /* width:  24px;
        height: 22px; */
        background-color: #AAD5E3;
        background-image: url("${pageContext.request.contextPath}/images/view_next_on.gif");"
    }

    .olControlPanel .selectCellsButtonItemInactive {
        /* width:  24px;
        height: 22px; */
        background-color: #000089;
        background-image: url("${pageContext.request.contextPath}/images/view_next_off.gif");"
    }
    
    .olControlPanel .baseLayerButtonItemActive {
        /* width:  24px;
        height: 22px; */
        background-color: #AAD5E3;
        background-image: url("${pageContext.request.contextPath}/images/google_icon.gif");"
    }

    .olControlPanel .baseLayerButtonItemInactive {
        /* width:  24px;
        height: 22px; */
        background-color: #000089;
        background-image: url("${pageContext.request.contextPath}/images/google_icon.gif");"
    }

    .olControlMousePosition {
        font-family: Verdana;
        font-size: 0.6em;
        color: #DDD;
    }

    .olControlScaleLine {
        font-family: Verdana;
        font-size: 0.8em;
        color: black;
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

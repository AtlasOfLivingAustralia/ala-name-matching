<%@ include file="/common/taglibs.jsp"%>
<c:set var="tilecacheUrl" scope="request"><gbif:propertyLoader bundle="portal" property="tilecache.url"/></c:set>
<c:set var="polygonTilecacheUrl" scope="request"><gbif:propertyLoader bundle="portal" property="polygon.tilecache.url"/></c:set>
<c:set var="geoserverUrl" scope="request"><gbif:propertyLoader bundle="portal" property="geoserver.url"/></c:set>
<c:set var="bluemarbleUrl" scope="request"><gbif:propertyLoader bundle="portal" property="bluemarble.layer.url"/></c:set>
<c:set var="cellDensityLayerUrl" scope="request"><gbif:propertyLoader bundle="portal" property="celldensity.layer.url"/></c:set>
<script type="text/javascript">

    //intialise options
    isFullScreen = ${not empty param['fullScreen'] ? 'true' : 'false'};
    useGoogle = ${param['map']=='google' ? 'true': 'false'};

    //intialise layers
    tilecacheUrl = '${tilecacheUrl}';
    geoserverUrl = '${geoserverUrl}';    
    polygonTilecacheUrl = '${polygonTilecacheUrl}';
    if(useGoogle) polygonTilecacheUrl = geoserverUrl +'/wms?';
    bluemarbleUrl = '${bluemarbleUrl}';
    cellDensityLayerUrl = '${cellDensityLayerUrl}';
    

    //extras
    fullScreenMapUrl='${pageContext.request.contextPath}/mapping/fullScreenMap.htm?fullScreen=true';
    
    //extra parameters
    brokenContentSize = document.getElementById('content').offsetWidth == 0;
    extraParams = '${extraParams}';
    cellButton;
    baseLayerButton;

    //set the referral page
    <c:choose>
    <c:when test="${not empty param['pageUrl']}">
      pageUrl = '${param['pageUrl']}';
    </c:when>
    <c:otherwise>
      pageUrl = '${pageContext.request.contextPath}/${entityPath}/${entityId}';
    </c:otherwise>
    </c:choose>
    
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

    /**
     * Display a popup on map
     */
    function displayCellInfo(lonlat) {
        removePopup();
        var lat = lonlat.lat;
        var lon = lonlat.lon;
        popupLonLat = lonlat
        if(useGoogle){
            //reproject lat long values
            var sourceProjection = new OpenLayers.Projection("EPSG:900913");
            var destinationProjection = new OpenLayers.Projection("EPSG:4326");
            var point = new OpenLayers.Geometry.Point(lon, lat);
            point.transform(sourceProjection,destinationProjection);
            lat = point.y;
            lon = point.x;
        }
        
        var cellInfoUrl = "${pageContext.request.contextPath}/maplayer/cellcounts/";
        var params = {
            unit: cellUnit,
            lat: lat,
            lon: lon,
            extraParams: extraParams,
            entityPath: "${entityPath}",
            entityId: "${entityId}"
        };

        OpenLayers.loadURL(cellInfoUrl, params, this, createPopup, createPopup);
    }
</script>
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
        background-image: url("${pageContext.request.contextPath}/images/view_next_on.gif");
    }

    .olControlPanel .selectCellsButtonItemInactive {
        /* width:  24px;
        height: 22px; */
        background-color: #000089;
        background-image: url("${pageContext.request.contextPath}/images/view_next_off.gif");
    }
    
    .olControlPanel .baseLayerButtonItemActive {
        /* width:  24px;
        height: 22px; */
        background-color: #AAD5E3;
        background-image: url("${pageContext.request.contextPath}/images/google_icon.gif");
    }

    .olControlPanel .baseLayerButtonItemInactive {
        /* width:  24px;
        height: 22px; */
        background-color: #000089;
        background-image: url("${pageContext.request.contextPath}/images/google_icon.gif");
    }

    .olControlPanel .fullScreenButtonItemActive {
        /* width:  24px;
        height: 22px; */
        background-color: #FFFFFF;
        background-image: url("${pageContext.request.contextPath}/images/fullscreen_on.gif");
    }

    .olControlPanel .fullScreenButtonItemInactive {
        /* width:  24px;
        height: 22px; */
        background-color: #FFFFFF;
        background-image: url("${pageContext.request.contextPath}/images/fullscreen_off.gif");
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
        margin-left:60px;
    }

    .olPopup {
        /*border:1px solid grey; 
         background-color:white; 
        -moz-border-radius-bottomleft:5px;
        -moz-border-radius-bottomright:5px;
        -moz-border-radius-topleft:5px;
        -moz-border-radius-topright:5px;*/
        font-size: 11px;
    }

    .olPopupCloseBox {
        background: url("/geoserver/openlayers/img/cancel.png") no-repeat;
        cursor: pointer;
        right: 10px !important;
        top: 5px !important;
    }
</style>
<script type="text/javascript">
    var mapDivId='map';
    initMap(mapDivId, useGoogle);
    initLayers();
    loadLayers();
    if(!isFullScreen){
        zoomToBounds();
        window.onload = handleResize;
        window.onresize = handleResize;
    }
</script>
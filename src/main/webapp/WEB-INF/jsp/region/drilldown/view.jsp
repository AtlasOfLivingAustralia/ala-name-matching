<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2>Geographic Region: <span class="subject">${geoRegion.name}</span></h2>
    <h3>
        <c:if test="${geoRegion.regionType ==1}">Australian State</c:if>
        <c:if test="${geoRegion.regionType ==2}">Australian Territory</c:if>
        <c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">Interim Biogeographic Regionalisation of Australia Region - Biogeographic region</c:if>
        <c:if test="${geoRegion.regionType ==3001}">Integrated Marine and Coastal Regionalisation of Australia Region - Cold Temperate Waters</c:if>
        <c:if test="${geoRegion.regionType ==3002}">Integrated Marine and Coastal Regionalisation of Australia Region - Subtropical Waters</c:if>
        <c:if test="${geoRegion.regionType ==3003}">Integrated Marine and Coastal Regionalisation of Australia Region - Transitional Waters</c:if>
        <c:if test="${geoRegion.regionType ==3004}">Integrated Marine and Coastal Regionalisation of Australia Region - Tropical Waters</c:if>
        <c:if test="${geoRegion.regionType ==3005}">Integrated Marine and Coastal Regionalisation of Australia Region -  Warm Temperate Waters</c:if>
        <c:if test="${geoRegion.regionType ==3005}">Integrated Marine and Coastal Regionalisation of Australia Region -  Warm Temperate Waters</c:if>
        <c:if test="${geoRegion.regionType >=5000 && geoRegion.regionType <6000}">IMCRA Region - River Basin</c:if>
    </h3>
</div>
<div class="subcontainer">
    <script type="text/javascript"><!--
        function initOpenLayersMap(){
        	var map = null;
        	<c:choose>
              <c:when test="${param['map']=='google'}">
                 map = createGoogleMap('openLayersMap');
              </c:when>
              <c:otherwise>
                 map = create4326Map('openLayersMap');
              </c:otherwise>
        	</c:choose>
            var ibraLayer = new OpenLayers.Layer.WMS( "IBRA",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:ibra61_reg_shape",
                    srs: 'EPSG:4326', 
                    version: "1.0.0", 
                    transparent: "true", 
                    format: "image/png"}
                    );
                    
                var imcraLayer = new OpenLayers.Layer.WMS( "IMCRA",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:imcra4_pb_wgs_1984",
                    srs: 'EPSG:4326', 
                    version: "1.0.0", 
                    transparent: "true", 
                    format: "image/png"}
                    );  
                  
                var statesLayer = new OpenLayers.Layer.WMS( "Political States",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:as",
                    srs: 'EPSG:4326', 
                    version: "1.0.0", 
                    transparent: "true", 
                    format: "image/png"}
                    );                

                var cellLayer = new OpenLayers.Layer.WMS( "${geoRegion.name} 1 deg",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:tabDensityLayer",
                    srs: 'EPSG:4326', 
                    version: "1.0.0", 
                    transparent: "true",
                    format: "image/png", 
                    filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA[http://localhost:8080/ala-web/maplayer/simple/?id=${geoRegion.id}&type=8&unit=1]]></Literal></PropertyIsEqualTo></Filter>)"} 
                    );
                  
                var centiCellLayer = new OpenLayers.Layer.WMS( "${geoRegion.name} centi cells",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:tabDensityLayer",
                    srs: 'EPSG:4326', 
                    version: "1.0.0", 
                    transparent: "true",
                    format: "image/png",
                    filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA[http://localhost:8080/ala-web/maplayer/simple/?&id=${geoRegion.id}&type=8&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"} 
                    );

                var allCentiCellLayer = new OpenLayers.Layer.WMS( "All centi cells",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:tabDensityLayer", 
                    srs: 'EPSG:4326',
                    version: "1.0.0", 
                    transparent: "true",
                    format: "image/png", 
                    filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA[http://localhost:8080/ala-web/maplayer/simple/?id=0&type=0&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"} 
                    );
                    
                var countriesLayer = new OpenLayers.Layer.WMS( "Countries",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:countries",
                    srs: 'EPSG:4326',
                    bgcolor: "0x666699",
                    format: "image/png"}
                    );                  

                var cellMarkerLayer = new OpenLayers.Layer.WMS( "Cellmarker",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:map100dd",
                    srs: 'EPSG:4326',
                    transparent: "true",
                    format: "image/png"}
                    );  
                    
                    
                <c:if test="${param['map']!='google'}">
                    var blueMarbleLayer = new OpenLayers.Layer.WMS( "Satellite", 
                            "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'satellite' } );
                    map.addLayer(blueMarbleLayer);
                    map.addLayer(countriesLayer);
                </c:if>
                

                //useful for debug
                //map.addLayer(cellLayer); 
                map.addLayer(centiCellLayer);
                
                <c:if test="${geoRegion.regionType <1000}">map.addLayer(statesLayer);</c:if>
                <c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">map.addLayer(ibraLayer);</c:if>
                <c:if test="${geoRegion.regionType >=3000 && geoRegion.regionType <4000}">map.addLayer(imcraLayer);</c:if>
                
                map.addControl(new OpenLayers.Control.LayerSwitcher());
                map.addControl(new OpenLayers.Control.MousePosition());

                // zoom to the correct bounds for this region
                var bounds = new OpenLayers.Bounds();
                bounds.extend(new OpenLayers.LonLat(${geoRegion.minLongitude}, ${geoRegion.minLatitude}));
                bounds.extend(new OpenLayers.LonLat(${geoRegion.maxLongitude}, ${geoRegion.maxLatitude}));

                <c:if test="${param['map']=='google'}">
                  var proj = new OpenLayers.Projection("EPSG:4326");
                  //reproject latlong values
                  bounds.transform(proj, map.getProjectionObject());
                </c:if>
                map.zoomToExtent(bounds, true);	
        }
    --></script>
   <div id="openLayersMap" class="openlayersMap"></div>
   <p>
            <c:choose>
              <c:when test="${param['map']=='google'}">
                 <a href="${pageContext.request.contextPath}/regions/?id=${geoRegion.id}">Use geoserver base layers</a>
              </c:when>
              <c:otherwise>
                <a href="${pageContext.request.contextPath}/regions/?id=${geoRegion.id}&map=google">Use google base layers</a>
              </c:otherwise>
            </c:choose>
   </p>
   <script>
        initOpenLayersMap();
   </script>
</div>
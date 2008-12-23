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
    <script type="text/javascript">
        function initOpenLayersMap(){
            var map = new OpenLayers.Map('openLayersMap', 
                 {controls: [new OpenLayers.Control.Navigation(), 
                             new OpenLayers.Control.PanZoomBar()], 
                  numZoomLevels: 10 });
            
            var ibraLayer = new OpenLayers.Layer.WMS( "IBRA",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:ibra61_reg_shape", 
                version: "1.0.0", 
                transparent: "true", 
                format: "image/png"}
                );
                
            var imcraLayer = new OpenLayers.Layer.WMS( "IMCRA",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:imcra4_pb_wgs_1984", 
                version: "1.0.0", 
                transparent: "true", 
                format: "image/png"}
                );  
              
            var statesLayer = new OpenLayers.Layer.WMS( "Political States",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:as", 
                version: "1.0.0", 
                transparent: "true", 
                format: "image/png"}
                );                
              
            var tabLayer = new OpenLayers.Layer.WMS( "ALA Occurrence data",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:tabDensityLayer", 
                version: "1.0.0", 
                transparent: "true",
                format: "image/png", 
                filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA[http://localhost:8080/ala-web/maplayer/simple/?id=14&type=6&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"} 
                );
                
            var countriesLayer = new OpenLayers.Layer.WMS( "Countries",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:countries",
                bgcolor: "0x666699",
                format: "image/png"}
                );
            
            map.addLayer(countriesLayer);
            
            var blueMarbleLayer= new OpenLayers.Layer.WMS( "Satellite",
                    "http://wms.jpl.nasa.gov/wms.cgi", 
                    {
                    format: "image/png"}
                    );
            map.addLayer(blueMarbleLayer);
            
            <c:if test="${geoRegion.regionType <1000}">map.addLayer(statesLayer);</c:if>
            <c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">map.addLayer(ibraLayer);</c:if>
            <c:if test="${geoRegion.regionType >=3000 && geoRegion.regionType <4000}">map.addLayer(imcraLayer);</c:if>
            
             map.addControl(new OpenLayers.Control.LayerSwitcher());
            
            
            //map.zoomToMaxExtent();
            
            //var lonLatCenter = new OpenLayers.LonLat(133.775, -25.274);
            //map.setCenter(lonLatCenter, 4, true, true);
            
            //var lonLatCenter = new OpenLayers.LonLat(${(geoRegion.maxLongitude+geoRegion.minLongitude)/2},${(geoRegion.maxLatitude+geoRegion.minLatitude)/2});
            //map.setCenter(lonLatCenter, 5, true, true);       
            
            var bounds = new OpenLayers.Bounds();
            bounds.extend(new OpenLayers.LonLat(${geoRegion.minLongitude}, ${geoRegion.minLatitude}));
            bounds.extend(new OpenLayers.LonLat(${geoRegion.maxLongitude}, ${geoRegion.maxLatitude}));
            map.zoomToExtent(bounds, true);
        }
    </script>
   <div id="openLayersMap" class="openlayersMap"></div>
   <script>
        initOpenLayersMap();
   </script>
</div>
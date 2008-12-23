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

            var cellLayer = new OpenLayers.Layer.WMS( "${geoRegion.name} 1 deg",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:tabDensityLayer", 
                version: "1.0.0", 
                transparent: "true",
                format: "image/png", 
                filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA[http://localhost:8080/ala-web/maplayer/simple/?id=${geoRegion.id}&type=8&unit=1]]></Literal></PropertyIsEqualTo></Filter>)"} 
                );
              
            var centiCellLayer = new OpenLayers.Layer.WMS( "${geoRegion.name} centi cells",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:tabDensityLayer", 
                version: "1.0.0", 
                transparent: "true",
                format: "image/png",
                filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA[http://localhost:8080/ala-web/maplayer/simple/?&id=${geoRegion.id}&type=8&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"} 
                );

            var allCentiCellLayer = new OpenLayers.Layer.WMS( "All centi cells",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:tabDensityLayer", 
                version: "1.0.0", 
                transparent: "true",
                format: "image/png", 
                filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA[http://localhost:8080/ala-web/maplayer/simple/?id=0&type=0&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"} 
                );
                
            var countriesLayer = new OpenLayers.Layer.WMS( "Countries",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:countries",
                bgcolor: "0x666699",
                format: "image/png"}
                );                  

            var cellMarkerLayer = new OpenLayers.Layer.WMS( "Cellmarker",
                "http://localhost:8080/geoserver/wfs?", 
                {layers: "ala:map100dd",
                     transparent: "true",
                format: "image/png"}
                );  

            //map.addLayer(countriesLayer);

            var blueMarbleLayer = new OpenLayers.Layer.WMS( "Satellite", 
                    "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'satellite' } );
            map.addLayer(blueMarbleLayer);

            
            //useful for debug
            //map.addLayer(cellMarkerLayer);
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
            map.zoomToExtent(bounds, true);
        }
    </script>
   <div id="openLayersMap" class="openlayersMap"></div>
   <script>
        initOpenLayersMap();
   </script>
</div>
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
        <c:if test="${geoRegion.regionType >=5000 && geoRegion.regionType <6000}">River Basin</c:if>
    </h3>
</div>
<div class="subcontainer">
    <script type="text/javascript"><!--

    var map, selectControl, selectedFeature, centiCellLayer;
    var brokenContentSize = document.getElementById('content').offsetWidth == 0;
    
        function initOpenLayersMap(){
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
                    {layers: "ala:ibra",
                    srs: 'EPSG:4326', 
                    version: "1.0.0", 
                    transparent: "true", 
                    format: "image/png"}
                    );
                    
                var imcraLayer = new OpenLayers.Layer.WMS( "IMCRA",
                    "http://localhost:8080/geoserver/wfs?", 
                    {layers: "ala:imcra",
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
                  
                centiCellLayer = new OpenLayers.Layer.WMS( "${geoRegion.name} centi cells",
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
                    bgcolor: "0xAEBEE0",
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
                    map.addLayer(countriesLayer);
                    map.addLayer(blueMarbleLayer);
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

        /**
         * Redirects to occurrence search.
         */
        function occurrenceSearch(latitude, longitude, roundingFactor) {
            // 36 pixels represents 0.1 degrees
            var longMin = (Math.floor(longitude*roundingFactor) )/roundingFactor;
            var latMin = (Math.floor(latitude*roundingFactor) )/roundingFactor;
            var longMax = (Math.ceil(longitude*roundingFactor) )/roundingFactor;
            var latMax = (Math.ceil(latitude*roundingFactor) )/roundingFactor;
            redirectToCell(longMin, latMin, longMax, latMax);
        }

        var extraParams = "c[0].s=36&c[0].p=0&c[0].o=${geoRegion.id}"
        
        //Redirects to filter search with bounding box
        function redirectToCell (minX, minY, maxX, maxY){
            document.location.href = "${pageContext.request.contextPath}/occurrences/boundingBoxWithCriteria.htm?"
                    +extraParams                
                    +"&minX="+minX
                    +"&minY="+minY
                    +"&maxX="+maxX
                    +"&maxY="+maxY;
        }

        function getStyle(el, property) {
        	  var style;
        	  if (el.currentStyle) {
        	    style = el.currentStyle[property];
        	  } else if( window.getComputedStyle ) {
        	    style = document.defaultView.getComputedStyle(el,null).getPropertyValue(property);
        	  } else {
        	    style = el.style[property];
        	  }
        	  return style;
        	}

        function resizeContent() {
            var content = document.getElementById('content');
            var rightMargin = parseInt(getStyle(content, "right"));
            content.style.width = document.documentElement.clientWidth - content.offsetLeft - rightMargin;
          }
          
          function resizeMap() {
            var centre = map.getCenter();
            var zoom = map.getZoom();
            var sidebar_width = 20;

            if (sidebar_width > 0) {
              sidebar_width = sidebar_width + 5
            }
            document.getElementById('openLayersMap').style.left = (sidebar_width) + "px";
            document.getElementById('openLayersMap').style.width = (document.getElementById('content').offsetWidth - sidebar_width) + "px";
          }

          function handleResize() {
            if (brokenContentSize) {
              resizeContent();
            }
            resizeMap();
          }
          

        var selectRegister = false;
        function toggleSelectCentiCell(){
            map.events.register('click', map, function (e) {
            	var lonlat = map.getLonLatFromViewPortPx(e.xy);
                occurrenceSearch(lonlat.lat, lonlat.lon, 10);
            });
        }

        window.onload = handleResize;
        window.onresize = handleResize;
        
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
   <p>
    <a href="javascript:toggleSelectCentiCell();">Select centi cell</a>
   </p>
   
   <script>
        initOpenLayersMap();
   </script>
   <div>
		<c:if test="${geoRegion.regionType <1000}">
		    <img src="http://localhost:8080/geoserver/wms?bgcolor=0xAEBEE0&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:as&width=600&height=545&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DADMIN_NAME%26nl%3Dala%3Aas%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
		</c:if>
		<c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">
		    <img src="http://localhost:8080/geoserver/wms?bgcolor=0xAEBEE0&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:ibra&width=600&height=545&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DREG_NAME%26nl%3Dala:ibra%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
		</c:if>
		<c:if test="${geoRegion.regionType >=3001 && geoRegion.regionType <4000}">
		    <img src="http://localhost:8080/geoserver/wms?bgcolor=0xAEBEE0&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:countries,ala:imcra&width=600&height=545&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DPB_NAME%26nl%3Dala:imcra%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
		</c:if>
   </div>
</div>
//OpenLayers.IMAGE_RELOAD_ATTEMPTS = 6;
/**
 * Initialise a Open Layers map
 * 
 * @param useGoogle whether to use google projection
 * @return
 */
function initMap(mapDivId, useGoogle){
  	if(useGoogle){
        map = createGoogleMap(mapDivId);
  	} else {
  		map = create4326Map(mapDivId);
  	}
   resizeMap(mapDivId, false);
   //add controls
   map.addControl(new OpenLayers.Control.LayerSwitcher());
   map.addControl(new OpenLayers.Control.MousePosition());
   map.addControl(new OpenLayers.Control.ScaleLine());
}

/**
 * Initialise a map in the standard geoserver projection
 * 
 * @param the map div
 * @return the initialised map
 */
function create4326Map(mapDivId){
    var map = new OpenLayers.Map(mapDivId, 
            {controls: [new OpenLayers.Control.Navigation(), 
                        new OpenLayers.Control.PanZoomBar()],
             numZoomLevels: 20});
    return map;
}

/**
 * Initialise a map in the spherical mercator projection.
 * 
 * @param the map div
 * @return the initialised map
 */
function createGoogleMap(mapDivId){
	
    /**
     * The commercial layers (Google, Virtual Earth, and Yahoo) are
     * in a custom projection - we're calling this Spherical Mercator.
     * GeoServer understands that requests for EPSG:900913 should
     * match the projection for these commercial layers.  Note that
     * this is not a standard EPSG code - so, if you want to load
     * layers from another WMS, it will have to be configured to work
     * with this projection.
     */
    var options = {
        // the "community" epsg code for spherical mercator
        projection: "EPSG:900913",
        // map horizontal units are meters
        units: "m",
        // this resolution displays the globe in one 256x256 pixel tile
        maxResolution: 156543.0339,
        // these are the bounds of the globe in sperical mercator
        maxExtent: new OpenLayers.Bounds(-20037508, -20037508,
                                         20037508, 20037508)
    };
    // construct a map with the above options
    map = new OpenLayers.Map(mapDivId, options, 
    		{controls: [new OpenLayers.Control.Navigation(), 
                   new OpenLayers.Control.PanZoomBar()],
                    numZoomLevels: 20});
    return map;
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

function resizeMap(mapDivId, centre) {
    resizeMap(mapDivId);
    if(centre){
      map.setCenter(centre, zoom);
    }
  }

function resizeMap(mapDivId) {
    var centre = map.getCenter();
    var zoom = map.getZoom();
    var sidebar_width = 30;
    if (sidebar_width > 0) {
      sidebar_width = sidebar_width + 5
    }
    document.getElementById(mapDivId).style.left = (sidebar_width) + "px";
    document.getElementById(mapDivId).style.width = (document.getElementById('content').offsetWidth - sidebar_width) + "px";
  }

/**
 * Handle window resizing
 */
function handleResize() {
    if (brokenContentSize) {
      resizeContent(mapDivId);
    }
    resizeMap(mapDivId, true);
}

/**
 * Register an event on the click
 * @return
 */
function toggleSelectCentiCell(){
    map.events.register('click', map, function (e) {
    	var lonlat = map.getLonLatFromViewPortPx(e.xy);
        occurrenceSearch(lonlat.lat, lonlat.lon, 10);
    });
}

/**
 * Initialise map layers
 */
function initLayers(){
    if(!useGoogle){
      map.addLayer(countriesLayer);
      map.addLayer(blueMarbleLayer);
      map.addLayer(roadsLayer);
      map.addLayer(placenamesLayer);
//    map.addLayer(alabaseLayer);
    } else {
        var gmap = new OpenLayers.Layer.Google(
                "Google Streets",
                {'sphericalMercator': true}
            ); 
        // create Google layer
        var gsat = new OpenLayers.Layer.Google(
            "Google Satellite",
            {type: G_SATELLITE_MAP, 'sphericalMercator': true}
        );
        // create Yahoo layer
        var yahoosat = new OpenLayers.Layer.Yahoo(
            "Yahoo Satellite",
            {'type': YAHOO_MAP_SAT, 'sphericalMercator': true}
        );
        var yahooreg = new OpenLayers.Layer.Yahoo(
                "Yahoo Regional",
                {'type': YAHOO_MAP_REG, 'sphericalMercator': true}
            );    
        var yahoohyb = new OpenLayers.Layer.Yahoo(
                "Yahoo Hybrid",
                {'type': YAHOO_MAP_HYB, 'sphericalMercator': true}
            );    
        map.addLayers([gmap, gsat, yahoosat, yahooreg, yahoohyb]);	
    }
    
    //useful for debug
    map.addLayer(cellLayer); 
    map.addLayer(centiCellLayer);
    map.addLayer(tenmilliCellLayer);
}

/**
 * Zoom to the correct bounds, re-projecting if necessary.
 */
function zoomToBounds(){
    // zoom to the correct bounds
    if(minLongitude!=null){
        var bounds = new OpenLayers.Bounds();
        bounds.extend(new OpenLayers.LonLat(minLongitude,minLatitude));
        bounds.extend(new OpenLayers.LonLat(maxLongitude,maxLatitude));
        if(useGoogle){
          //reproject latlong values
          var proj = new OpenLayers.Projection("EPSG:4326");
          bounds.transform(proj, map.getProjectionObject());
        }
        map.zoomToExtent(bounds, true);
    }
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
//OpenLayers.IMAGE_RELOAD_ATTEMPTS = 6;
/**
 * Initialise a Open Layers map
 * 
 * @param useGoogle whether to use google projection
 * @return
 */
function initMap(mapDivId, useGoogle){
    var baseLayerButtonTitle;

    if(useGoogle){
        map = createGoogleMap(mapDivId);
        baseLayerButtonTitle = 'Base Layer: switch to default map base layer';
    } else {
        map = create4326Map(mapDivId);
        baseLayerButtonTitle = 'Base Layer: switch to Google map base layer';
    }
    resizeMap(mapDivId, false);
    //add controls
    zoomButton = new OpenLayers.Control.ZoomBox(
        {title:"Zoom box: zoom on an area by clicking and dragging."});
    mouseDrag = new OpenLayers.Control.Navigation(
        {title:'Drag tool: move the map using the mouse',zoomWheelEnabled:false});
    cellButton = new OpenLayers.Control.Button({
        title:'Search on cell square: click to toggle whether clicking cell square performs occurrence search for that cell',
        displayClass: "selectCellsButton", trigger: toggleSelectCentiCell});
    baseLayerButton = new OpenLayers.Control.Button({
        title: baseLayerButtonTitle, displayClass: "baseLayerButton", trigger: toggleBaseLayer});

    var panel = new OpenLayers.Control.Panel({defaultControl:mouseDrag});
    panel.addControls([mouseDrag,zoomButton,cellButton,baseLayerButton]);
    map.addControl(panel);
    map.addControl(new OpenLayers.Control.LayerSwitcher());
    map.addControl(new OpenLayers.Control.MousePosition());
    map.addControl(new OpenLayers.Control.ScaleLine());
    map.addControl(new OpenLayers.Control.Navigation({zoomWheelEnabled: false}));
    map.addControl(new OpenLayers.Control.PanZoomBar({zoomWorldIcon: false}));
}

/**
 * Initialise a map in the standard geoserver projection
 * 
 * @param the map div
 * @return the initialised map
 */
function create4326Map(mapDivId){
    var map = new OpenLayers.Map(mapDivId, {numZoomLevels: 20,controls: []});
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
        // Controls are set in initMap (empty here to prevent double controls appearing)
        controls: [],
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
    		{numZoomLevels: 20});
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
var selectCellToggle = false;

function toggleSelectCentiCell(){
    zoomButton.deactivate();
    mouseDrag.deactivate();
    
    if (selectCellToggle) {
        // turn off
        selectCellToggle = false;
        cellButton.deactivate();
        map.div.style.cursor =  "default";
        map.events.remove('click');
    }
    else {
        // turn on
        selectCellToggle = true;
        cellButton.activate();
        map.div.style.cursor =  "pointer";
        map.events.register('click', map, function (e) {
            var lonlat = map.getLonLatFromViewPortPx(e.xy);
            occurrenceSearch(lonlat.lat, lonlat.lon, 10);
        });
    }
}

/**
 * Initialise map layers
 */
function initLayers(){
    if(!useGoogle){
      map.addLayer(alabaseLayer);
      //map.addLayer(alabaseLayer);
      map.addLayer(blueMarbleLayer);
      //map.addLayer(roadsLayer);
      //map.addLayer(placenamesLayer);
    } else {
        // create Google layer
        var gter = new OpenLayers.Layer.Google(
                "Google Terain",
                {type: G_PHYSICAL_MAP, 'sphericalMercator': true}
            );
    	
        var gsat = new OpenLayers.Layer.Google(
            "Google Satellite",
            {type: G_SATELLITE_MAP, 'sphericalMercator': true}
        );

        var gmap = new OpenLayers.Layer.Google(
                "Google Streets",
                {'sphericalMercator': true}
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
        map.addLayers([gsat, gter, gmap, yahoosat, yahooreg, yahoohyb]);	
    }
    

    //useful for debug
    map.addLayer(cellLayer); 
    map.addLayer(centiCellLayer);
    map.addLayer(tenmilliCellLayer);
    // listener for zoom level to choose best cell layer
    map.events.register('zoomend', map, function (e) {
        var zoom = map.zoom;
        
        if (zoom < 6) {
            cellLayer.setVisibility(true);
            centiCellLayer.setVisibility(false);
            tenmilliCellLayer.setVisibility(false);
        } else if (zoom >= 6 && zoom < 10) {
            cellLayer.setVisibility(false);
            centiCellLayer.setVisibility(true);
            tenmilliCellLayer.setVisibility(false);
        } else if (zoom >= 10) {
            cellLayer.setVisibility(false);
            centiCellLayer.setVisibility(false);
            tenmilliCellLayer.setVisibility(true);
        }
    }
    );

    cellButton.events.register('deactivate', this, function (e) {
        toggleSelectCentiCell();
    });

}

/**
 * Zoom to the correct bounds, re-projecting if necessary.
 */
function zoomToBounds(){
    // zoom to the correct bounds
//    var centre = getRequestParameter("centre");
//    var zoom = getRequestParameter("zoom");
    var bounds;
    var boundsString = getRequestParameter("bounds");

    if (boundsString) {
        bounds = new OpenLayers.Bounds.fromString(getRequestParameter("bounds"));
    } else if (minLongitude!=null) {
        bounds = new OpenLayers.Bounds();
        bounds.extend(new OpenLayers.LonLat(minLongitude,minLatitude));
        bounds.extend(new OpenLayers.LonLat(maxLongitude,maxLatitude));
    } 
    
    if(useGoogle){
        //reproject latlong values
        var proj4326 = new OpenLayers.Projection("EPSG:4326");
        bounds.transform(proj4326, map.getProjectionObject());
    } else if (boundsString) {
        var proj900913 = new OpenLayers.Projection("EPSG:900913");
        bounds.transform(proj900913, map.getProjectionObject());
    }

    map.zoomToExtent(bounds, true);
 }

/**
 * Redirects to occurrence search.
 */
function occurrenceSearch(latitude, longitude, roundingFactor) {
	
    if(useGoogle){
        //reproject lat long values
    	var sourceProjection = new OpenLayers.Projection("EPSG:900913");
        var destinationProjection = new OpenLayers.Projection("EPSG:4326");
        var point = new OpenLayers.Geometry.Point(longitude, latitude);
        point.transform(sourceProjection,destinationProjection);
        latitude = point.y;
        longitude = point.x;
    }
	
    // 36 pixels represents 0.1 degrees
    var longMin = (Math.floor(longitude*roundingFactor) )/roundingFactor;
    var latMin = (Math.floor(latitude*roundingFactor) )/roundingFactor;
    var longMax = (Math.ceil(longitude*roundingFactor) )/roundingFactor;
    var latMax = (Math.ceil(latitude*roundingFactor) )/roundingFactor;
    redirectToCell(longMin, latMin, longMax, latMax);
}

/**
 * JS to reload the page with the new baselayer (Geoserver/Google)
 */
function toggleBaseLayer() {
    //var centre = map.getCenter().toShortString();
    //centre = centre.replace(/\s+/,""); // remove space after comma
    var bounds = map.calculateBounds().toBBOX(); // e.g. Ó5,42,10,45Ó
    //var zoom = map.getZoom();
    var params = "";
    if (bounds) {
        params = "bounds=" + bounds; // + "&zoom=" + zoom;
    }
    if (useGoogle) {
        // switch to WMF
        useGoogle = false;
        baseLayerButton.deactivate();
        window.location.replace( pageUrl + "?" + params);
    }
    else {
        // switch to Google
        useGoogle = true;
        baseLayerButton.activate();
        window.location.replace( pageUrl + "?" + 'map=google&' + params);
    }
}

function getRequestParameter( name ) {
    // returns the request parameter "value" for the given "name""
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp( regexS );
    var results = regex.exec( window.location.href );
    if( results == null )
        return "";
    else
        return results[1];
}

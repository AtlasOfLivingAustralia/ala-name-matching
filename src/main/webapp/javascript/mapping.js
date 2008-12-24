
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
 * Initialise a map int he spherical mercator projection.
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
    map = new OpenLayers.Map(mapDivId, options);
    
    var gmap = new OpenLayers.Layer.Google(
            "Google Streets",
            {'sphericalMercator': true}
        ); 

    // create Google layer
    var gsat = new OpenLayers.Layer.Google(
        "Google Satellite",
        {type: G_SATELLITE_MAP, 'sphericalMercator': true}
    );

    // create Virtual Earth layer
    var veaer = new OpenLayers.Layer.VirtualEarth(
        "Virtual Earth Aerial",
        {'type': VEMapStyle.Aerial, 'sphericalMercator': true}
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
    
    
    map.addLayers([gmap, gsat, veaer, yahoosat, yahooreg, yahoohyb]);
    
    return map;
}
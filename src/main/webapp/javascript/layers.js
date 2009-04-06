//default layer urls - to be overridden by configuration & in JSP
var tilecacheUrl = 'http://localhost:8080/geoserver/wms?';
var geoserverUrl = 'http://localhost:8080/geoserver/';    
var polygonTilecacheUrl = 'http://localhost:8080/geoserver/wms?';
var bluemarbleUrl = 'http://labs.metacarta.com/wms-c/Basic.py?';
var cellDensityLayerUrl = 'http://localhost:8080/ala-web';

//layers
var tileCacheArray, cellDensityArray;
var alabaseLayer, countriesLayer,placenamesLayer, roadsLayer, ibraLayer;
var imcraLayer, statesLayer, gAdminLayer, riverBasinsLayer, cellLayer, centiCellLayer, tenmilliCellLayer;
//external tile laters
var blueMarbleLayer,gter,gsat,gmap,yahoosat,yahooreg,yahoohyb;


var imageFormat;
  var opacity;
  var isIE6 = /msie|MSIE 6/.test(navigator.userAgent); // detects IE6
   
  if (isIE6) {
    //alert('You are using IE6');
    imageFormat = "image/gif";
    opacity = "0.6";
  } else {
    //alert('You are NOT using IE6');
    imageFormat = "image/png";
    opacity = "0.75";
  }

  function initLayers(){
  
	  tileCacheArray = ["http://www1.ala.org.au/tilecache/tilecache.cgi?",
                  "http://www2.ala.org.au/tilecache/tilecache.cgi?",
                  "http://www3.ala.org.au/tilecache/tilecache.cgi?"];
  
	  cellDensityArray = ["http://maps.ala.org.au/geoserver/wms?",
                          "http://solr.ala.org.au/geoserver/wms?",
                          "http://test.ala.org.au/geoserver/wms?"];

     alabaseLayer = new OpenLayers.Layer.WMS("Base Layer",
	  tileCacheArray,
      {layers: "ala:alabase",
      srs: 'EPSG:4326',
      format: "image/png"},
      {wrapDateLine: true}
      );

   countriesLayer = new OpenLayers.Layer.WMS("Countries",
	  tilecacheUrl,
      {layers: "ala:countries,geoscience:roads,geoscience:localities",
      srs: 'EPSG:4326',
      format: "image/png"},
      {wrapDateLine: true}
      );

   placenamesLayer = new OpenLayers.Layer.WMS("Localities",
	  tilecacheUrl,
      {layers: "geoscience:localities",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

   roadsLayer = new OpenLayers.Layer.WMS("Roads",
	  tilecacheUrl,
      {layers: "geoscience:roads",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

   ibraLayer = new OpenLayers.Layer.WMS("IBRA",
	  polygonTilecacheUrl,
      {layers: "ala:ibra",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: "image/png",
      maxExtent: new OpenLayers.Bounds(112.91,-54.76,159.11,-10.06)},
      {alpha: true}
      );

   imcraLayer = new OpenLayers.Layer.WMS("IMCRA",
	  polygonTilecacheUrl,
      {layers: "ala:imcra",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: "image/png",
      maxExtent: new OpenLayers.Bounds(93.42,-58.45,171.81,-8.48)},
      {alpha: true}
      );

   statesLayer = new OpenLayers.Layer.WMS("Political States",
	  polygonTilecacheUrl,
      {layers: "ala:as",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: "image/png",
      maxExtent: new OpenLayers.Bounds(112.91,-54.76,159.11,-10.06)},
      {alpha: true}
      );

   gAdminLayer = new OpenLayers.Layer.WMS("Local Government Areas",
	  polygonTilecacheUrl,
      {layers: "ala:gadm",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png",
      maxExtent: new OpenLayers.Bounds(93.42,-58.45,171.81,-8.48)},
      {alpha: true}
      );

   riverBasinsLayer = new OpenLayers.Layer.WMS("River Basins",
      tilecacheUrl,
      {layers: "geoscience:riverbasins",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png",
      maxExtent: new OpenLayers.Bounds(93.42,-58.45,171.81,-8.48)},
      {alpha: true}
      );

   cellLayer = new OpenLayers.Layer.WMS( entityName+" 1 degree cells",
	  cellDensityArray,
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: imageFormat,
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?id="+entityId+"&type="+entityType+"&unit=1]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false, opacity: opacity, wrapDateLine: true}
      );

   centiCellLayer = new OpenLayers.Layer.WMS.Untiled( entityName+" 0.1 degree cells",
	  cellDensityArray,	
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: imageFormat,
      tiled: "false",
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?&id="+entityId+"&type="+entityType+"&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false, opacity: opacity, wrapDateLine: true, tiled: false}
      );

   tenmilliCellLayer = new OpenLayers.Layer.WMS.Untiled( entityName+" 0.01 degree cells",
	  cellDensityArray,
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: imageFormat,
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?id="+entityId+"&type="+entityType+"&unit=0.01]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false, opacity: opacity, wrapDateLine: true}
      );

   /** Externally housed layers */
   
   blueMarbleLayer = new OpenLayers.Layer.WMS("Satellite","http://labs.metacarta.com/wms-c/Basic.py?",{layers: 'satellite'},{wrapDateLine: true});
   // create Google layer
   gter = new OpenLayers.Layer.Google("Google Terrain", {type: G_PHYSICAL_MAP, 'sphericalMercator': true});
   gsat = new OpenLayers.Layer.Google("Google Satellite",{type: G_SATELLITE_MAP, 'sphericalMercator': true});
   gmap = new OpenLayers.Layer.Google("Google Streets",{'sphericalMercator': true}); 
   // create Yahoo layer
   yahoosat = new OpenLayers.Layer.Yahoo("Yahoo Satellite",{'type': YAHOO_MAP_SAT, 'sphericalMercator': true});
   yahooreg = new OpenLayers.Layer.Yahoo("Yahoo Regional",{'type': YAHOO_MAP_REG, 'sphericalMercator': true});
   yahoohyb = new OpenLayers.Layer.Yahoo("Yahoo Hybrid",{'type': YAHOO_MAP_HYB, 'sphericalMercator': true});
  }
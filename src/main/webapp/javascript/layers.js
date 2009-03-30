  var blueMarbleLayer = new OpenLayers.Layer.WMS("Satellite",
      "http://labs.metacarta.com/wms-c/Basic.py?",
      {layers: 'satellite'}
      );

  var alabaseLayer = new OpenLayers.Layer.WMS("Base Layer",
	  tilecacheUrl,
      {layers: "ala:alabase",
      srs: 'EPSG:4326',
      format: "image/png"}
      );

  var countriesLayer = new OpenLayers.Layer.WMS("Countries",
	  tilecacheUrl,
      {layers: "ala:countries",
      srs: 'EPSG:4326',
      format: "image/png"}
      );

  var ibraLayer = new OpenLayers.Layer.WMS( "IBRA",
	  tilecacheUrl,
      {layers: "ala:ibra",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

  var imcraLayer = new OpenLayers.Layer.WMS( "IMCRA",
	  tilecacheUrl,
      {layers: "ala:imcra",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

  var statesLayer = new OpenLayers.Layer.WMS( "Political States",
      tilecacheUrl,
      {layers: "ala:as",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

  var placenamesLayer = new OpenLayers.Layer.WMS("Localities",
	  tilecacheUrl,
      {layers: "geoscience:localities",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

  var gAdminLayer = new OpenLayers.Layer.WMS("Local Government Areas",
	  tilecacheUrl,
      {layers: "ala:gadm",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

  var riverBasinsLayer = new OpenLayers.Layer.WMS("River Basins",
      tilecacheUrl,
      {layers: "geoscience:riverbasins",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

  var placenamesHighLayer = new OpenLayers.Layer.WMS("Localities (detailed)",
	  tilecacheUrl,
      {layers: "geoscience:localities_detailed",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

  var roadsLayer = new OpenLayers.Layer.WMS("Roads",
	  tilecacheUrl,
      {layers: "geoscience:roads",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"},
      {alpha: true}
      );

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
        opacity = "";
    }

   var cellLayer = new OpenLayers.Layer.WMS( entityName+" cells",
      geoserverUrl+"/wms?",
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: imageFormat,
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?id="+entityId+"&type="+entityType+"&unit=1]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false, opacity: opacity}
      );

   var centiCellLayer = new OpenLayers.Layer.WMS( entityName+" centi cells",
      geoserverUrl+"/wms?",
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: imageFormat,
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?&id="+entityId+"&type="+entityType+"&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false, opacity: opacity}
      );

   var tenmilliCellLayer = new OpenLayers.Layer.WMS( entityName+" tenmilli cells",
      geoserverUrl+"/wms?",
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326',
      version: "1.0.0",
      transparent: "true",
      format: imageFormat,
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?id="+entityId+"&type="+entityType+"&unit=0.01]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false, opacity: opacity}
      );


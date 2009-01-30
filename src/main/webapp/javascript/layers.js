  var blueMarbleLayer = new OpenLayers.Layer.WMS("Satellite", 
      "http://labs.metacarta.com/wms-c/Basic.py?", 
      {layers: 'satellite'} 
      );

  var countriesLayer = new OpenLayers.Layer.WMS("Countries",
      geoserverUrl+"/wfs?", 
      {layers: "ala:countries",
      srs: 'EPSG:4326',
      bgcolor: "0x666699",
      format: "image/png"}
      );
  
  var ibraLayer = new OpenLayers.Layer.WMS( "IBRA",
	  geoserverUrl+"/wfs?",
      {layers: "ala:ibra",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true", 
      format: "image/png"}
      );
            
  var imcraLayer = new OpenLayers.Layer.WMS( "IMCRA",
      geoserverUrl+"/wfs?", 
      {layers: "ala:imcra",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true", 
      format: "image/png"}
      );
          
  var statesLayer = new OpenLayers.Layer.WMS( "Political States",
      geoserverUrl+"/wfs?", 
      {layers: "ala:as",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true", 
      format: "image/png"}
      );

  var cellLayer = new OpenLayers.Layer.WMS( entityName+" Occurrence data",
      geoserverUrl+"/wfs?", 
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true",
      format: "image/png", 
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?id="+entityId+"&type="+entityType+"&unit=1]]></Literal></PropertyIsEqualTo></Filter>)"} 
      );
    
  var centiCellLayer = new OpenLayers.Layer.WMS( entityName+" centi cells",
      geoserverUrl+"/wfs?", 
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true",
      format: "image/png",
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?&id="+entityId+"&type="+entityType+"&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"} 
      );

  var tenmilliCellLayer = new OpenLayers.Layer.WMS( entityName+" tenmilli cells",
      geoserverUrl+"/wfs?", 
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true",
      format: "image/png",
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?id="+entityId+"&type="+entityType+"&unit=0.01]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false}
      );

  var placenamesLayer = new OpenLayers.Layer.WMS("Localities",
      geoserverUrl+"/wfs?", 
      {layers: "geoscience:localities",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"}
      );

  var placenamesHighLayer = new OpenLayers.Layer.WMS("Localities (detailed)",
      geoserverUrl+"/wfs?", 
      {layers: "geoscience:localities_detailed",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"}
      );

  var roadsLayer = new OpenLayers.Layer.WMS("Roads",
      geoserverUrl+"/wfs?", 
      {layers: "geoscience:roads",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"}
      );
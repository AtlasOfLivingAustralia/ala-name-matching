  var blueMarbleLayer = new OpenLayers.Layer.WMS("Satellite", 
      "http://labs.metacarta.com/wms-c/Basic.py?", 
      {layers: 'satellite'} 
      );

  var alabaseLayer = new OpenLayers.Layer.WMS("Base Layer",
		  tilecacheUrl, 
	      {layers: "alabase",
	      srs: 'EPSG:4326',
	      format: "image/png"}
	      );
  
  var countriesLayer = new OpenLayers.Layer.WMS("Countries",
	  tilecacheUrl+'bgcolor=0x666699', 
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
      format: "image/png"}
      );
            
  var imcraLayer = new OpenLayers.Layer.WMS( "IMCRA",	
	  tilecacheUrl, 
      {layers: "ala:imcra",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true", 
      format: "image/png"}
      );
          
  var statesLayer = new OpenLayers.Layer.WMS( "Political States",
      tilecacheUrl, 
      {layers: "ala:as",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true", 
      format: "image/png"}
      );

  var placenamesLayer = new OpenLayers.Layer.WMS("Localities",
	  tilecacheUrl, 
      {layers: "geoscience:localities",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"}
      );

  var gAdminLayer = new OpenLayers.Layer.WMS("Local Government Areas",
	  tilecacheUrl,  
      {layers: "ala:gadm",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"}
      );

  var placenamesHighLayer = new OpenLayers.Layer.WMS("Localities (detailed)",
	  tilecacheUrl,
      {layers: "geoscience:localities_detailed",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"}
      );

  var roadsLayer = new OpenLayers.Layer.WMS("Roads",
	  tilecacheUrl,  
      {layers: "geoscience:roads",
      srs: 'EPSG:4326',
      transparent: "true",
      format: "image/png"}
      );
  
  var cellLayer = new OpenLayers.Layer.WMS( entityName+" cells",
      geoserverUrl+"/wms?", 
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true",
      format: "image/png", 
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?id="+entityId+"&type="+entityType+"&unit=1]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false}
      );
    
  var centiCellLayer = new OpenLayers.Layer.WMS( entityName+" centi cells",
      geoserverUrl+"/wms?", 
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true",
      format: "image/png",
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?&id="+entityId+"&type="+entityType+"&unit=0.1]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false}
      );

  var tenmilliCellLayer = new OpenLayers.Layer.WMS( entityName+" tenmilli cells",
      geoserverUrl+"/wms?", 
      {layers: "ala:tabDensityLayer",
      srs: 'EPSG:4326', 
      version: "1.0.0", 
      transparent: "true",
      format: "image/png",
      filter: "(<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+cellDensityLayerUrl+"/maplayer/simple/?id="+entityId+"&type="+entityType+"&unit=0.01]]></Literal></PropertyIsEqualTo></Filter>)"},
      {visibility:false}
      );

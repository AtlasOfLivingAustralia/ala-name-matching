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
	<script type="text/javascript">
		var entityId = '${geoRegion.id}';
		var entityType = '8';
		var filterId = '36';
		var entityName = '${geoRegion.name}';
		var minLongitude = ${geoRegion.minLongitude};
		var minLatitude = ${geoRegion.minLatitude};
		var maxLongitude = ${geoRegion.maxLongitude};
		var maxLatitude = ${geoRegion.maxLatitude};
	</script>
    <jsp:include page="/WEB-INF/jsp/mapping/openlayer.jsp"/>
	<!-- Region specific layers -->
	<script type="text/javascript">
	<c:if test="${geoRegion.regionType <1000}">map.addLayer(statesLayer);</c:if>
	<c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">map.addLayer(ibraLayer);</c:if>
	<c:if test="${geoRegion.regionType >=3000 && geoRegion.regionType <4000}">map.addLayer(imcraLayer);</c:if>
	</script>
	<div>
	<c:if test="${geoRegion.regionType <1000}">
		<img src="${geoserverUrl}/wms?bgcolor=0x666699&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:as&width=600&height=545&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DADMIN_NAME%26nl%3Dala%3Aas%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
	</c:if> 
	<c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">
		<img src="${geoserverUrl}/wms?bgcolor=0x6666699&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:ibra&width=600&height=545&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DREG_NAME%26nl%3Dala:ibra%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
	</c:if> 
	<c:if test="${geoRegion.regionType >=3001 && geoRegion.regionType <4000}">
		<img src="${geoserverUrl}/wms?bgcolor=0x666699&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:countries,ala:imcra&width=600&height=545&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DPB_NAME%26nl%3Dala:imcra%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
	</c:if>
	</div>
</div>
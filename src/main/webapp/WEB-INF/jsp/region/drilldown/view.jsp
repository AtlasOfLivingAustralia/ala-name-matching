<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
<h2>Geographic Region: <span class="subject"><gbif:capitalize>${geoRegion.name}</gbif:capitalize></span></h2>
<h3>
<c:if test="${geoRegion.regionType ==1}">Australian State</c:if>
<c:if test="${geoRegion.regionType ==2}">Australian Territory</c:if> 
<c:if test="${geoRegion.regionType ==3}">Borough</c:if>
<c:if test="${geoRegion.regionType ==4}">City</c:if>
<c:if test="${geoRegion.regionType ==5}">Community gov. council</c:if>
<c:if test="${geoRegion.regionType ==6}">District council</c:if>
<c:if test="${geoRegion.regionType ==7}">Municipality</c:if>
<c:if test="${geoRegion.regionType ==8}">Rural city</c:if>
<c:if test="${geoRegion.regionType ==9}">Shire</c:if>
<c:if test="${geoRegion.regionType ==10}">Territory</c:if>
<c:if test="${geoRegion.regionType ==11}">Town</c:if>
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
<c:if test="${geoRegion.occurrenceCount>0}">
<div id="furtherActions">
	<h4><spring:message code='actions.for'/> <gbif:capitalize>${geoRegion.name}</gbif:capitalize></h4>
	<table cellspacing="1" class="actionsList">
		<tbody>
			<tr valign="top">
				<td><b><spring:message code="actions.explore"/></b></td>
				<td>
					<ul class="actionsListInline">
						<li>
							<a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="0"/>"><spring:message code="explore.occurrences"/></a>
						</li>
						<li>
							<c:set var="a0">
								<span class='subject'><gbif:capitalize>${geoRegion.name}</gbif:capitalize></span>
							</c:set>
							<a href="${pageContext.request.contextPath}/species/browse/region/${geoRegion.id}"><spring:message code="geography.drilldown.view.taxonomy" text="Explore species recorded in "/> <span class="subject"><gbif:capitalize>${geoRegion.name}</gbif:capitalize></span></a>
						</li>
					</ul>
				</td>
			</tr>
			<tr valign="top">
				<td><b><spring:message code="actions.list"/></b></td>
				<td>
					<ul class="actionsListInline">
						<li>
							<c:set var="a0">
								<span class='subject'><gbif:capitalize>${geoRegion.name}</gbif:capitalize></span>
							</c:set>
							<a href="${pageContext.request.contextPath}/occurrences/searchResources.htm?<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="0"/>"><spring:message code="geography.drilldown.list.resources" arguments="${a0}"/></a>
						</li>
					</ul>
				</td>
			</tr>
			<tr valign="top">
				<td><b><spring:message code="actions.download"/></b></td>
				<td>
					<ul class="actionsListInline">
						<li>
							<a href="${pageContext.request.contextPath}/occurrences/region/celldensity/country-celldensity-${geoRegion.id}.kml"><spring:message code="download.google.earth.celldensity"/></a>
						</li>
					</ul>
				</td>
			</tr>
		</tbody>
	</table>
</div>
</c:if>
<div class="subcontainer">
	<script type="text/javascript">
		var entityId = '${geoRegion.id}';
		var entityType = '8';
		var entityName = '${geoRegion.name}';
		var minLongitude = ${geoRegion.minLongitude};
		var minLatitude = ${geoRegion.minLatitude};
		var maxLongitude = ${geoRegion.maxLongitude};
		var maxLatitude = ${geoRegion.maxLatitude};
	</script>
	<c:set var="extraParams" scope="request"><gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="0"/></c:set>
    <c:set var="entityId" scope="request">${geoRegion.id}</c:set>
    <c:set var="entityPath" scope="request" value="regions"/>
    <jsp:include page="/WEB-INF/jsp/mapping/openlayer.jsp"/>
	<!-- Region specific layers -->
	<script type="text/javascript">
        <c:if test="${geoRegion.regionType <1000}">map.addLayer(statesLayer);</c:if>
        <c:if test="${geoRegion.regionType >=3 && geoRegion.regionType <12}">map.addLayer(gAdminLayer);</c:if>
        <c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">map.addLayer(ibraLayer);</c:if>
        <c:if test="${geoRegion.regionType >=3000 && geoRegion.regionType <4000}">map.addLayer(imcraLayer);</c:if>
        <c:if test="${geoRegion.regionType >=5000 && geoRegion.regionType <5999}">map.addLayer(riverBasinsLayer);</c:if>
   	</script>
	<div>
	<c:if test="${geoRegion.regionType <3}">
		<img src="${geoserverUrl}/wms?bgcolor=0x666699&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:as&width=600&height=600&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DADMIN_NAME%26nl%3Dala%3Aas%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
	</c:if>
    <c:if test="${geoRegion.regionType >=3 && geoRegion.regionType <12}">
        <img src="${geoserverUrl}/wms?bgcolor=0x6666699&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:gadm&width=600&height=600&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DNAME_2%26nl%3Dala:gadm%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
    </c:if>
	<c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">
		<img src="${geoserverUrl}/wms?bgcolor=0x6666699&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:ibra&width=600&height=600&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DREG_NAME%26nl%3Dala:ibra%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
	</c:if> 
	<c:if test="${geoRegion.regionType >=3001 && geoRegion.regionType <4000}">
		<img src="${geoserverUrl}/wms?bgcolor=0x666699&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:countries,ala:imcra&width=600&height=600&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DPB_NAME%26nl%3Dala:imcra%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
	</c:if>
    <c:if test="${geoRegion.regionType >=5000 && geoRegion.regionType <5999}">
        <img src="${geoserverUrl}/wms?bgcolor=0x666699&bbox=110.6,-57,161.4,-7.8&styles=&Format=image/png&request=GetMap&version=1.1.1&layers=ala:countries,geoscience:riverbasins&width=600&height=600&srs=EPSG:4326&sld=http%3A%2F%2Flocalhost%3A8080%2Fala-web%2Fregions%2Fsld.htm%3Fpn%3DRNAME%26nl%3Dgeoscience:riverbasins%26pv%3D<string:encodeUrl>${geoRegion.name}</string:encodeUrl>"/>
    </c:if>
	</div>
    <div id="resources_table" class=" yui-skin-sam">
    <c:if test="${not empty geoRegionDataResources}">
        <h5><spring:message code="geography.drilldown.mapped.resources" text="Resources providing data for Map"/></h5>
        <div id="json"></div>
        <script type="text/javascript">
            YAHOO.util.Event.addListener(window, "load", function() {
                YAHOO.example.XHR_JSON = function() {
                    var formatResourceUrl = function(elCell, oRecord, oColumn, sData) {
                        elCell.innerHTML = "<a href='" + oRecord.getData("dataResourceUrl") + "'>" + sData + "</a>";
                    };
                    var formatOccurrencesUrl = function(elCell, oRecord, oColumn, sData) {
                        elCell.innerHTML = "<a href='" + oRecord.getData("occurrencesUrl") + "'>" + sData + "</a>";
                    };
                    var formatGeoreferencedOccurrencesUrl = function(elCell, oRecord, oColumn, sData) {
                        elCell.innerHTML = "<a href='" + oRecord.getData("georeferencedOccurrencesUrl") + "'>" + sData + "</a>";
                    };

                    var myColumnDefs = [
                        {key:"dataResourceName", label:"Dataset", sortable:true, formatter:formatResourceUrl},
                        {label:"Occurrences",
	                      children: [
                            {key:"occurrences", label:"All", formatter:formatOccurrencesUrl, sortable:true},
                            {key:"georeferencedOccurrences", label:"Georeferenced", formatter:formatGeoreferencedOccurrencesUrl, sortable:true}
                          ]
                        },
                        {key:"basisOfRecord", label:"Basis of Record", sortable:true}
                    ];

                    var myDataSource = new YAHOO.util.DataSource("${pageContext.request.contextPath}/regions/resources/");
                    myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
                    myDataSource.connXhrMode = "queueRequests";
                    myDataSource.responseSchema = {
                        resultsList: "ResultSet.Result",
                        fields: ["dataResourceName","dataResourceUrl",{key:"occurrences",parser:"number"},"occurrencesUrl",
                                 {key:"georeferencedOccurrences",parser:"number"},"georeferencedOccurrencesUrl","basisOfRecord"]
                    };

                    var myDataTable = new YAHOO.widget.DataTable("json", myColumnDefs,
                            myDataSource, {initialRequest:"${geoRegion.id}",sortedBy:{key:"dataResourceName", dir:"asc"}}); // scrollable:true,height:"150px",
                    var mySuccessHandler = function() {
                        this.onDataReturnAppendRows.apply(this,arguments);
                    };
                    var myFailureHandler = function() {
                        this.showTableMessage(YAHOO.widget.DataTable.MSG_ERROR, YAHOO.widget.DataTable.CLASS_ERROR);
                        this.onDataReturnAppendRows.apply(this,arguments);
                    };
                    var callbackObj = {
                        success : mySuccessHandler,
                        failure : myFailureHandler,
                        scope : myDataTable
                    };

                    return {
                        oDS: myDataSource,
                        oDT: myDataTable
                    };
                }();
            });

            </script>
    </c:if>
    </div>
</div>
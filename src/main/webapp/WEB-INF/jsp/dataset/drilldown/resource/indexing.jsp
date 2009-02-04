<%@ include file="/common/taglibs.jsp"%>
<h4><spring:message code="indexed.data"/></h4>
<c:choose>
	<c:when test="${dataResource.occurrenceCount>0}">
		<table class="smallStatistics">
		<tr><td class="property"><spring:message code="occurrences.indexed"/>:</td><td><fmt:formatNumber value="${dataResource.occurrenceCount}" pattern="###,###"/></td></tr>
		<tr><td class="property"><spring:message code="provider.record.count"/>:</td><td><fmt:formatNumber value="${dataResource.providerRecordCount}" pattern="###,###"/></td></tr>
		<tr><td class="property"><spring:message code="occurrences.georeferenced"/>:</td><td><fmt:formatNumber value="${dataResource.occurrenceCoordinateCount}" pattern="###,###"/></td></tr>
		<tr><td class="property"><spring:message code="occurrences.no.geo.issues"/>:</td><td><fmt:formatNumber value="${dataResource.occurrenceCleanGeospatialCount}" pattern="###,###"/></td></tr>
		<tr><td class="property"><spring:message code="count.species"/>:</td><td><fmt:formatNumber value="${dataResource.speciesCount}" pattern="###,###"/></td></tr>
		<tr><td class="property"><spring:message code="count.taxa"/>:</td><td><fmt:formatNumber value="${dataResource.conceptCount}" pattern="###,###"/></td></tr>
		</table>
		<ul class="genericList">
			<li>
				<a href="${pageContext.request.contextPath}/datasets/resource/${dataResource.key}/logs/"><spring:message code="view.event.logs.for" text="View event logs for"/> ${dataResource.name}</a>
			</li>
			<li>
				<span class="new">New!</span> <a href="${pageContext.request.contextPath}/datasets/resource/${dataResource.key}/indexing/"><spring:message code="view.indexing.history.for" text="View indexing history for"/> ${dataResource.name}</a>
			</li>
		</ul>
		<div id="charts">
			<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/swfobject.js"></script>
			<script type="text/javascript">
				var facet = "taxon_name";
				function load_chart(facet) {
					swfobject.embedSWF(
						"${pageContext.request.contextPath}/open-flash-chart.swf", "chart1", "600", "400", "9.0.0", 
						"expressInstall.swf",
						{"data-file":"http://${header['host']}:${header['port']}/ala-portal-search/<string:encodeUrl>solrSearch.json?q=data_resource_id:${dataResource.key}&format=pieChart&per_page=10&facet=</string:encodeUrl>" + facet });							
				}
				load_chart(facet);
			</script>
			<h4>Data Breakdown Charts</h4>
			<ul>
				<li><a href="javascript:load_chart('taxonomic_issue');">taxonomic issues</a></li>
				<li><a href="javascript:load_chart('geospatial_issue');">geospatial issues</a></li>
				<li><a href="javascript:load_chart('other_issue');">other issues</a></li>
				<li><a href="javascript:load_chart('taxon_name');">scientific name</a></li>
			</ul>
			<div id="chart1" style="height:400px; width:600px;"></div>
		</div>
	</c:when>
	<c:otherwise>
	
		<table class="smallStatistics">
		<tr><td colspan="2"><spring:message code="dataset.resource.no.records.indexed.yet"/></td></tr>
		<c:if test="${dataResource.providerRecordCount>0}">
			<tr><td class="property"><spring:message code="provider.record.count"/>:</td><td><fmt:formatNumber value="${dataResource.providerRecordCount}" pattern="###,###"/></td></tr>
		</c:if>
		</table>
	</c:otherwise>
</c:choose>	


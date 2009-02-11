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
		<div id="charts" style="">
            <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/swfobject.js"></script>
            <script type="text/javascript">            
                var flashvars1 = {
            	    path: escape("${pageContext.request.contextPath}/charts/"),
            	    settings_file: escape("${pageContext.request.contextPath}/charts/year_settings.xml"),
            	    chart_data: "${chartData.year}",
            	    preloader_color: "#999999"
			    };
			
                swfobject.embedSWF("${pageContext.request.contextPath}/charts/amxy.swf", "chart1", "80%", "400", "9.0.0", "expressInstall.swf", flashvars1 );

                var flashvars2 = {
                        path: escape("${pageContext.request.contextPath}/charts/"),
                        settings_file: escape("${pageContext.request.contextPath}/charts/month_settings.xml"),
                        chart_data: "${chartData.month}",
                        preloader_color: "#999999"
                    };
                
                swfobject.embedSWF("${pageContext.request.contextPath}/charts/amcolumn.swf", "chart2", "80%", "400", "9.0.0", "expressInstall.swf", flashvars2 );

                var flashvars3 = {
                        path: escape("${pageContext.request.contextPath}/charts/"),
                        settings_file: escape("${pageContext.request.contextPath}/charts/names_settings.xml"),
                        chart_data: "${chartData.taxon_name_id}",
                        preloader_color: "#999999"
                    };
                
                swfobject.embedSWF("${pageContext.request.contextPath}/charts/ampie.swf", "chart3", "80%", "400", "9.0.0", "expressInstall.swf", flashvars3 );
                    
                  
                
            </script>
            <h4>Data Breakdown Charts</h4>
            
            <h5>By Year</h5>
            <div id="chart1"></div>
            <h5>By Month</h5>
            <div id="chart2"></div>
            <h5>By Taxon Name</h5>
            <div id="chart3"></div>
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


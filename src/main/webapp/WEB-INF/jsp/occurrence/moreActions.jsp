<%@ include file="/common/taglibs.jsp"%>
<div id="occurrenceActions">
	<table cellspacing="1" class="actionsList">
        <thead>
            <tr valign="top">
				<th><b><spring:message code="actions.specify"/></b></th>
                <th><b><spring:message code="actions.download"/></b></th>
            </tr>
        </thead>
        <tbody>
            <tr valign="top">
                <td>
                    <ul class="actionsListIcon">
						<li>
							<c:choose>
								<c:when test="${viewName!='providerCounts'}"><a href="${pageContext.request.contextPath}/occurrences/searchProviders.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"><spring:message code="occurrence.switchto.provider.countview"/></a></c:when>
								<c:otherwise>&raquo; <spring:message code="occurrence.switchto.provider.countview"/> &laquo;</c:otherwise>
							</c:choose>
						</li>
						<li>
							<c:choose>
								<c:when test="${viewName!='resourceCounts'}"><a href="${pageContext.request.contextPath}/occurrences/searchResources.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"><spring:message code="occurrence.switchto.resources.countview"/></a></c:when>
								<c:otherwise>&raquo; <spring:message code="occurrence.switchto.resources.countview"/> &laquo;</c:otherwise>
							</c:choose>
						</li>
						<!--<li>
							<c:choose>
								<c:when test="${viewName!='countryCounts'}"><a href="${pageContext.request.contextPath}/occurrences/searchCountries.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"><spring:message code="occurrence.switchto.country.countview"/></a></c:when>
								<c:otherwise>&raquo; <spring:message code="occurrence.switchto.country.countview"/> &laquo;</c:otherwise>
							</c:choose>
						</li>-->
					</ul>
                </td>
                <td>
                    <ul class="actionsListIcon">
						<li>
							<c:choose>
								<c:when test="${viewName!='resultsDownloadSpreadsheet'}"><a href="${pageContext.request.contextPath}/occurrences/downloadSpreadsheet.htm?<gbif:criteria criteria="${criteria}"/>" class="iconDownload"><spring:message code="occurrence.search.filter.action.download.spreadsheet"/></a></c:when>
								<c:otherwise>&raquo; <spring:message code="occurrence.search.filter.action.download.spreadsheet"/> &laquo;</c:otherwise>
							</c:choose>
						</li>
						<!--<li>
							<a href="${pageContext.request.contextPath}/occurrences/downloadResults.htm?format=brief&criteria=<gbif:criteria criteria="${criteria}" urlEncode="true"/>" class="iconDownload"><spring:message code="occurrence.record.download.format.darwin.brief" text="Darwin core (limited to 100,000)"/></a>
						</li>-->
						<li>
							<a href="${pageContext.request.contextPath}/occurrences/downloadResults.htm?format=species&criteria=<gbif:criteria criteria="${criteria}" urlEncode="true"/>" class="iconDownload"><spring:message code="occurrence.record.download.format.species" text="Species in results"/></a>
						</li>
						<li>
							<a href="${pageContext.request.contextPath}/occurrences/downloadResults.htm?format=kml&criteria=<gbif:criteria criteria="${criteria}" urlEncode="true"/>" class="iconEarth"><spring:message code="occurrence.record.download.format.ge"/></a>
						</li>
					</ul>
                </td>
            </tr>
        </tbody>
    </table>
</div>
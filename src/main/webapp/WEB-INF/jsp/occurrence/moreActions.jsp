<%@ include file="/common/taglibs.jsp"%>
<div id="occurrenceActions">
	<h4><spring:message code="occurrence.search.filter.whattodo.title"/></h4>
	<table cellspacing="1" class="actionsList">
		<tbody>
			<tr valign="top">
				<td><b><spring:message code="actions.view"/></b></td>
				<td>	
					<ul class="actionsListInline">
						<li>
							<c:choose>
								<c:when test="${viewName!='resultsTable'}"><a href="${pageContext.request.contextPath}/occurrences/searchWithTable.htm?<gbif:criteria criteria="${criteria}"/>" class="iconTable"><spring:message code="occurrence.search.filter.action.viewtable"/></a></c:when>
								<c:otherwise>&raquo; <spring:message code="occurrence.search.filter.action.viewtable"/> &laquo;</c:otherwise>
							</c:choose>
						</li>		
						<li>
							<c:choose>
								<c:when test="${viewName!='resultsMap'}"><a href="${pageContext.request.contextPath}/occurrences/searchWithMap.htm?<gbif:criteria criteria="${criteria}"/>" class="iconMap"><spring:message code="occurrence.search.filter.action.viewmap"/></a></c:when>
								<c:otherwise>&raquo; <spring:message code="occurrence.search.filter.action.viewmap"/> &laquo;</c:otherwise>
							</c:choose>
						</li>
					</ul>
				</td>
			</tr>
			<tr valign="top">
				<td><b><spring:message code="actions.specify"/></b></td>
				<td>	
					<ul class="actionsListInline">
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
						<li>
							<c:choose>
								<c:when test="${viewName!='countryCounts'}"><a href="${pageContext.request.contextPath}/occurrences/searchCountries.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"><spring:message code="occurrence.switchto.country.countview"/></a></c:when>
								<c:otherwise>&raquo; <spring:message code="occurrence.switchto.country.countview"/> &laquo;</c:otherwise>
							</c:choose>
						</li>
					</ul>
				</td>
			</tr>
			<tr valign="top">
				<td><b><spring:message code="actions.download"/></b></td>
				<td>	
					<ul class="actionsListInline">
						<li> 
							<c:choose>
								<c:when test="${viewName!='resultsDownloadSpreadsheet'}"><a href="${pageContext.request.contextPath}/occurrences/downloadSpreadsheet.htm?<gbif:criteria criteria="${criteria}"/>" class="iconDownload"><spring:message code="occurrence.search.filter.action.download.spreadsheet"/></a></c:when>
								<c:otherwise>&raquo; <spring:message code="occurrence.search.filter.action.download.spreadsheet"/> &laquo;</c:otherwise>
							</c:choose>
						</li>
						<li> 
							<a href="${pageContext.request.contextPath}/occurrences/downloadResults.htm?format=brief&criteria=<gbif:criteria criteria="${criteria}" urlEncode="true"/>" class="iconDownload"><spring:message code="occurrence.record.download.format.darwin.brief" text="Darwin core (limited to 100,000)"/></a>
						</li>
						<li> 
							<a href="${pageContext.request.contextPath}/occurrences/downloadResults.htm?format=kml&criteria=<gbif:criteria criteria="${criteria}" urlEncode="true"/>" class="iconEarth"><spring:message code="occurrence.record.download.format.ge"/></a>
						</li>
						<li> 
							<a href="${pageContext.request.contextPath}/occurrences/downloadResults.htm?format=species&criteria=<gbif:criteria criteria="${criteria}" urlEncode="true"/>" class="iconDownload"><spring:message code="occurrence.record.download.format.species" text="Species in results"/></a>			
						</li>  
					</ul>
				</td>
			</tr>
			<% /* Commented-out Niche model for ALA portal. NdR.
	        <tr valign="top">
				<td><b><spring:message code="actions.create" text="Create:" /></b></td>
				<td>
				<ul class="actionsListInline">
					<li><!--span class="new"><spring:message code="new"/></span-->
					<c:if test="${viewName!='nicheModelling'}">
						<a href="${pageContext.request.contextPath}/occurrences/setupModel.htm?<gbif:criteria criteria="${criteria}"/>"	class="iconModel">
					</c:if><spring:message code="occurrence.search.filter.action.create.model" text="Niche Model" /><c:if test="${viewName!='nicheModelling'}">
						</a>
					</c:if></li>
				</ul>
				</td>
			</tr>
			*/ %>
		</tbody>
	</table>
</div>
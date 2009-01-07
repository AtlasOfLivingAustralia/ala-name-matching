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
							<c:if test="${viewName!='resultsTable'}"><a href="${pageContext.request.contextPath}/occurrences/searchWithTable.htm?<gbif:criteria criteria="${criteria}"/>" class="iconTable"></c:if><spring:message code="occurrence.search.filter.action.viewtable"/><c:if test="${viewName!='resultsTable'}"></a></c:if>
						</li>		
						<li>
							<c:if test="${viewName!='resultsMap'}"><a href="${pageContext.request.contextPath}/occurrences/searchWithMap.htm?<gbif:criteria criteria="${criteria}"/>" class="iconMap"></c:if><spring:message code="occurrence.search.filter.action.viewmap"/><c:if test="${viewName!='resultsMap'}"></a></c:if>
						</li>
					</ul>
				</td>
			</tr>
			<tr valign="top">
				<td><b><spring:message code="actions.specify"/></b></td>
				<td>	
					<ul class="actionsListInline">
						<li> 
							<c:if test="${viewName!='providerCounts'}"><a href="${pageContext.request.contextPath}/occurrences/searchProviders.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"></c:if><spring:message code="occurrence.switchto.provider.countview"/><c:if test="${viewName!='providerCounts'}"></a></c:if>			
						</li>
						<li> 
							<c:if test="${viewName!='resourceCounts'}"><a href="${pageContext.request.contextPath}/occurrences/searchResources.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"></c:if><spring:message code="occurrence.switchto.resources.countview"/><c:if test="${viewName!='resourceCounts'}"></a></c:if>			
						</li>
						<li> 
							<c:if test="${viewName!='countryCounts'}"><a href="${pageContext.request.contextPath}/occurrences/searchCountries.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"></c:if><spring:message code="occurrence.switchto.country.countview"/><c:if test="${viewName!='countryCounts'}"></a></c:if>
						</li>
					</ul>
				</td>
			</tr>
			<tr valign="top">
				<td><b><spring:message code="actions.download"/></b></td>
				<td>	
					<ul class="actionsListInline">
						<li> 
							<c:if test="${viewName!='resultsDownloadSpreadsheet'}"><a href="${pageContext.request.contextPath}/occurrences/downloadSpreadsheet.htm?<gbif:criteria criteria="${criteria}"/>" class="iconDownload"></c:if><spring:message code="occurrence.search.filter.action.download.spreadsheet"/><c:if test="${viewName!='resultsDownloadSpreadsheet'}"></a></c:if>
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
      <tr valign="top">
        <td><b><spring:message code="actions.create" text="Create:"/></b></td>
        <td>  
          <ul class="actionsListInline">
            <li> 
              <!--span class="new"><spring:message code="new"/></span--> <c:if test="${viewName!='nicheModelling'}"><a href="${pageContext.request.contextPath}/occurrences/setupModel.htm?<gbif:criteria criteria="${criteria}"/>" class="iconModel"></c:if><spring:message code="occurrence.search.filter.action.create.model" text="Niche Model"/><c:if test="${viewName!='nicheModelling'}"></a></c:if>
            </li>
          </ul>
        </td>
      </tr>		
		</tbody>
	</table>
</div>
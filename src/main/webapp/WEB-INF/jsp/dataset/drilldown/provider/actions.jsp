<%@ include file="/common/taglibs.jsp"%>
<c:if test="${dataProvider.occurrenceCount>0 || (not empty dataResources && dataResources[0].sharedTaxonomy)}">	
<div id="furtherActions">
	<table cellspacing="1" class="actionsList" style="width:500px;">
		<thead>
            <tr>
                <td><b><spring:message code="actions.explore"/></b></td>
                <c:if test="${dataProvider.occurrenceCoordinateCount>0}">
                <td><b><spring:message code="actions.download"/></b></td>
                </c:if>
            </tr>
        </thead>
        <tbody>
            <tr valign="top">
                <td>	
					<ul class="actionsListIcon">
						<c:if test="${dataProvider.occurrenceCount>0}">	
							<li>
								<a href="${pageContext.request.contextPath}/occurrences/searchWithTable.htm?<gbif:criterion subject="25" predicate="0" value="${dataProvider.key}" index="0"/>" class="iconTable"><spring:message code="explore.occurrences"/></a>
							</li>
						</c:if>
						<c:if test="${not empty dataResources && dataResources[0].sharedTaxonomy}">
							<li>
								<a href="${pageContext.request.contextPath}/species/browse/provider/${dataProvider.key}" class="iconTable"><spring:message code="dataset.taxonomytreelink" arguments="${dataProvider.name}" argumentSeparator="%%%%"/></a>
							</li>
						</c:if>
					</ul>
				</td>
                <c:if test="${dataProvider.occurrenceCoordinateCount>0}">
					<td>	
						<ul class="actionsListIcon">
							<li>
								<a href="${pageContext.request.contextPath}/occurrences/downloadSpreadsheet.htm?<gbif:criterion subject="25" predicate="0" value="${dataProvider.key}" index="0"/>" class="iconDownload"><spring:message code="occurrence.search.filter.action.download.spreadsheet"/></a>
							</li>	
							<li>
								<a href="${pageContext.request.contextPath}/occurrences/provider/celldensity/provider-celldensity-${dataProvider.key}.kml" class="iconEarth"><spring:message code="download.google.earth.celldensity"/></a>
							</li>
						</ul>
					</td>
                </c:if>
            </tr>
        </tbody>
    </table>
</div>
</c:if>
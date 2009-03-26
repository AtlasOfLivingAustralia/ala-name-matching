<%@ include file="/common/taglibs.jsp"%>
<c:if test="${not empty viewName || not empty results.searchResults}">
<div id="furtherActions">
	<!-- <h4><spring:message code="occurrence.search.filter.whattodo.title"/></h4> -->
	<table cellspacing="1" class="actionsList">
		<thead>
            <tr valign="top">
				<th><b><spring:message code="actions.download"/></b></th>
            </tr>
        </thead>
        <tbody>
			<tr valign="top">
				<td>	
					<ul class="actionsListIcon">
						<li> 
							<c:if test="${viewName!='resultsDownloadSpreadsheet'}"><a href="${pageContext.request.contextPath}/species/downloadSpreadsheet.htm?<gbif:criteria criteria="${criteria}"/>" class="iconDownload"></c:if><spring:message code="occurrence.search.filter.action.download.spreadsheet"/><c:if test="${viewName!='resultsDownloadSpreadsheet'}"></a></c:if>
						</li>
					</ul>
				</td>
			</tr>
		</tbody>
	</table>
</div>
</c:if>
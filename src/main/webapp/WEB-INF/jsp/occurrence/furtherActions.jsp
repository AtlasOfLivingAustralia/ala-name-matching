<%@ include file="/common/taglibs.jsp"%>
<c:if test="${fn:length(results) ge 1}">
<h4 id="searchResultCount"><spring:message code="occurrence.search.filter.retrievingcount"/></h4>
<script type="text/javascript">
	<% //retrieve the count %>
	var countCallback = {
		success:function(o){document.getElementById("searchResultCount").innerHTML=o.responseText;},	
		failure: function(o){}
	}	
	YAHOO.util.Connect.asyncRequest('GET',
		"<string:trim>${pageContext.request.contextPath}/occurrences/occurrenceCount?<gbif:criteria criteria="${criteria}"/><gbiftag:occurrenceFilterOptions/></string:trim>", 
		countCallback, 
		null); 	
</script>

<div id="furtherActions">
	<table cellspacing="1" class="actionsList">
        <thead>
            <tr valign="top">
				<th><b><spring:message code="actions.view"/></b></th>
                <th><b><spring:message code="actions.specify"/></b></th>
                <th><b><spring:message code="actions.download"/></b></th>
            </tr>
        </thead>
        <tbody>
            <tr valign="top">
                <td>	
					<ul class="actionsListIcon">
						<li>
							<a href="${pageContext.request.contextPath}/occurrences/searchWithTable.htm?<gbif:criteria criteria="${criteria}"/>" class="iconTable"><spring:message code="occurrence.search.filter.action.viewtable"/></a>
						</li>		
						<!--<li>
							<a href="${pageContext.request.contextPath}/occurrences/searchWithMap.htm?<gbif:criteria criteria="${criteria}"/>" class="iconMap"><spring:message code="occurrence.search.filter.action.viewmap"/></a>
						</li>-->
					</ul>
				</td>
                <td>	
					<ul class="actionsListIcon">
						<li> 
							<a href="${pageContext.request.contextPath}/occurrences/searchProviders.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"><spring:message code="occurrence.switchto.provider.countview"/></a>			
						</li>
						<li> 
							<a href="${pageContext.request.contextPath}/occurrences/searchResources.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"><spring:message code="occurrence.switchto.resources.countview"/></a>			
						</li>
						<!--<li>
							<a href="${pageContext.request.contextPath}/occurrences/searchCountries.htm?<gbif:criteria criteria="${criteria}"/>" class="iconSearchAdd"><spring:message code="occurrence.switchto.country.countview"/></a>			
						</li>-->
					</ul>
				</td>
                <td>
					<ul class="actionsListIcon">
						<li>
							<a href="${pageContext.request.contextPath}/occurrences/downloadSpreadsheet.htm?<gbif:criteria criteria="${criteria}"/>" class="iconDownload"><spring:message code="occurrence.search.filter.action.download.spreadsheet"/></a>
						</li>
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
</c:if>
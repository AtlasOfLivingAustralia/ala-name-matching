<%@ include file="/common/taglibs.jsp"%>
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
<h4><spring:message code="occurrence.search.filter.resultstable.title"/></h4>
<display:table 
	name="results" 
	export="false" 
	class="results" 
	uid="occurrenceRecord" 
	requestURI="/occurrences/searchWithTable.htm?"
	sort="external"
	defaultsort="1"
	pagesize="20"
	size="resultSize"
	
>
  <display:column titleKey="occurrence.search.results.name">
  	<span class="genera">${occurrenceRecord.taxonName}</span>
  </display:column>	
  <display:column titleKey="occurrence.search.results.provider">
  		<span title="${occurrenceRecord.dataResourceName}">
	  		<string:truncateNicely lower="30" upper="40">${occurrenceRecord.dataResourceName}</string:truncateNicely>	
	  	</span>	
  </display:column>	
  <display:column titleKey="occurrence.search.results.institutioncode">
  		<span title="${occurrenceRecord.institutionCode}">
	  		<string:truncateNicely lower="30" upper="40">${occurrenceRecord.institutionCode}</string:truncateNicely>	
	  	</span>	  	  	
  </display:column>	  
  <display:column titleKey="occurrence.search.results.collectioncode">
  		<span title="${occurrenceRecord.collectionCode}">
	  		<string:truncateNicely lower="30" upper="40">${occurrenceRecord.collectionCode}</string:truncateNicely>	
	  	</span>	  	  	  	  	
  </display:column>	
  <display:column titleKey="occurrence.search.results.catalogueno">
  		<span title="${occurrenceRecord.catalogueNumber}">
	  		<string:truncateNicely lower="30" upper="40">${occurrenceRecord.catalogueNumber}</string:truncateNicely>	
	  	</span>	  	  	  	  	
   </display:column>	
  <display:column titleKey="occurrence.search.results.basisofrecord">
  		<span title="${occurrenceRecord.basisOfRecord}">
	  		<string:truncateNicely lower="30" upper="40"><string:capitalize><spring:message code="basis.of.record.${occurrenceRecord.basisOfRecord}"/></string:capitalize></string:truncateNicely>	
	  	</span>	  	  	  	  	
   </display:column>	
  <display:column property="occurrenceDate" decorator="org.gbif.portal.web.ui.DateWrapper" titleKey="occurrence.search.results.date"/>
  <display:column titleKey="occurrence.search.results.coordinates">
  	<c:if test="${occurrenceRecord.latitude!=null && occurrenceRecord.longitude!=null}">
			<gbiftag:latlong latitude="${occurrenceRecord.latitude}" longitude="${occurrenceRecord.longitude}"/>
			<gbiftag:formatGeospatialIssues issuesBit="${occurrenceRecord.geospatialIssue}" messageSource="${messageSource}" locale="${request.getLocale}"/>
			<c:if test="${not empty geospatialIssueText}">
				<c:if test="${occurrenceRecord.latitude!=null || occurrenceRecord.longitude}"><br/></c:if>
				<span style='font-size: 0.7em;'>(${geospatialIssueText})</span>
			</c:if>
		</c:if>
  </display:column>	
  <display:column class="lastColumn">
  	<a href="${pageContext.request.contextPath}/occurrences/${occurrenceRecord.key}/"><spring:message code="occurrence.search.filter.view"/></a>
  </display:column>	
  
  <display:setProperty name="basic.msg.empty_list"><spring:message code="occurrence.search.filter.nonefound"/></display:setProperty>	  
  <display:setProperty name="paging.banner.onepage"> </display:setProperty>	  
  <display:setProperty name="basic.empty.showtable">true</display:setProperty>	 
  <display:setProperty name="basic.msg.empty_list_row">
  	<tr class="empty">
		<td colspan="13"><spring:message code="occurrence.search.filter.nonefound"/></td>
	</tr>
	</tr>
  </display:setProperty>	  
  <display:setProperty name="paging.banner.no_items_found"> </display:setProperty>	  
  <display:setProperty name="pagination.pagenumber.param">pageno</display:setProperty>
  <display:setProperty name="paging.banner.placement">both</display:setProperty>	
</display:table>
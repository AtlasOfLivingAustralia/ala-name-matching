<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*" %>
<%@ page import="javax.servlet.jsp.*" %>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.gbif.portal.dto.occurrence.*" %>
<%@ page import="org.apache.taglibs.string.util.*" %>
<h4><spring:message code="occurrence.search.filter.sampleresults.title"/></h4>
<display:table 
	name="results" 
	class="results" 
	uid="occurrenceRecord" 
	length="5"
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
		<td colspan="10"><spring:message code="occurrence.search.filter.nonefound"/></td>
	</tr>
	</tr>
  </display:setProperty>	  
  <display:setProperty name="paging.banner.no_items_found"> </display:setProperty>	  
</display:table>
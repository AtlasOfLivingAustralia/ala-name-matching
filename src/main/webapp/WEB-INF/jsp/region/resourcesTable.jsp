<%@ include file="/common/taglibs.jsp"%>
<% 
/*  
 * JSON data format adapted from example YOU DataTable
 * 
 * http://developer.yahoo.com/yui/examples/datatable/dt_xhrjson.html
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
*/ 
%>
<json:object prettyPrint="true">
  <json:object name="ResultSet">
    <json:property name="totalResultsAvailable" value="${fn:length(geoRegionDataResources)}"/>
      <json:array name="Result" var="geoRegionDataResource" items="${geoRegionDataResources}">
        <json:object>
          <json:property name="dataResourceName" value="${geoRegionDataResource.dataResourceName}"/>
          <json:property name="dataResourceUrl" value="${pageContext.request.contextPath}/datasets/resource/${geoRegionDataResource.dataResourceId}"/>
          <json:property name="occurrences" value="${geoRegionDataResource.occurrenceCount}"/>
          <json:property name="occurrencesUrl">
              ${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="24" predicate="0" value="${geoRegionDataResource.dataResourceId}" index="0"/>&<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="1"/>
          </json:property>
          <json:property name="georeferencedOccurrences" value="${geoRegionDataResource.occurrenceCoordinateCount}"/>
          <json:property name="georeferencedOccurrencesUrl">
              ${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="24" predicate="0" value="${geoRegionDataResource.dataResourceId}" index="0"/>&<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="1"/>&<gbif:criterion subject="28" predicate="0" value="0" index="2"/>
          </json:property>
          <json:property name="basisOfRecord" value="${geoRegionDataResource.basisOfRecord.name}"/>
        </json:object>
      </json:array>
  </json:object>
</json:object>
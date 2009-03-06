<%@ include file="/common/taglibs.jsp"%><% 
/*  
 * JSON data format adapted from example YOU DataTable
 * 
 * http://developer.yahoo.com/yui/examples/datatable/dt_xhrjson.html
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
*/ 
%><json:object prettyPrint="true">
  <json:object name="ResultSet">
    <json:property name="totalRecords" value="${fn:length(regionConcepts)}"/>
    <json:property name="startIndex" value=""/>
    <json:property name="sort" value=""/>
    <json:property name="dir" value=""/>
    <json:property name="pageSize" value=""/>
      <json:array name="Result" var="regionConcept" items="${regionConcepts}">
        <json:object>
          <json:property name="geoRegionName" value="${geoRegion.name}"/>
          <json:property name="geoRegionNameUrl" value="${pageContext.request.contextPath}/region/${geoRegion.id}"/>
          <json:property name="taxonConceptName" value="${regionConcept.taxonConceptName}"/>
          <json:property name="taxonConceptNameUrl" value="${pageContext.request.contextPath}/species/browse/region/${geoRegion.id}/taxon/${regionConcept.taxonConceptId}/json"/>
          <json:property name="taxonRank" value="${regionConcept.rankName}"/>
          <json:property name="occurrences" value="${regionConcept.occurrenceCount}"/>
          <json:property name="occurrencesUrl">
              ${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="0" predicate="0" value="${regionConcept.taxonConceptId}" index="0"/>&<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="1"/>
          </json:property>
        </json:object>
      </json:array>
  </json:object>
</json:object>
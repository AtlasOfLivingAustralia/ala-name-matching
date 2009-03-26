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
  
    <json:property name="recordsReturned" value="${recordsReturned}"/>
    <json:property name="totalRecords" value="${scientificNamesTotal}"/>
    <json:property name="startIndex" value="${startIndex}"/>
    <json:property name="sort" value="${sort}"/>
    <json:property name="dir" value="${dir}"/>
    <json:property name="pageSize" value="${pageSize}"/>
      <json:array name="result" var="scientificName" items="${scientificNames}">
        <json:object>
          <json:property name="score" value="${scientificName[0]}"/>
          <json:property name="scientificName" value="${scientificName[1].taxonName.canonical}"/>
          <json:property name="scientificNameUrl">
            ${pageContext.request.contextPath}/species/${scientificName[1].id}
          </json:property>
          <json:property name="author" value="${scientificName[1].taxonName.author}"/>
          <json:property name="rank"><alatag:taxonRankfromInt rankValue="${scientificName[1].rank}"/></json:property>
          <json:property name="family" value="${scientificName[1].familyConcept.taxonName.canonical}"/>
          <json:property name="kingdom" value="${scientificName[1].kingdomConcept.taxonName.canonical}"/>
        </json:object>
      </json:array>
  
</json:object>

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
    <json:property name="totalRecords" value="${commonNamesTotal}"/>
    <json:property name="startIndex" value="${startIndex}"/>
    <json:property name="sort" value="${sort}"/>
    <json:property name="dir" value="${dir}"/>
    <json:property name="pageSize" value="${pageSize}"/>
      <json:array name="result" var="commonName" items="${commonNames}">
        <json:object>
          <json:property name="score" value="${commonName[0]}"/>
          <json:property name="commonName" value="${commonName[1].name}"/>
          <json:property name="commonNameUrl">
            ${pageContext.request.contextPath}/species/${commonName[1].taxonConcept.id}/commonName/${commonName[1].name}
          </json:property>
          <json:property name="scientificName" value="${commonName[2]}"/>
          <json:property name="kingdom" value="${commonName[3]}"/>
        </json:object>
      </json:array>
  
</json:object>

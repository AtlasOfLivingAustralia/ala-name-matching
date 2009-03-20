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
    <json:property name="totalRecords" value="${dataResourcesTotal}"/>
    <json:property name="startIndex" value="${startIndex}"/>
    <json:property name="sort" value="${sort}"/>
    <json:property name="dir" value="${dir}"/>
    <json:property name="pageSize" value="${pageSize}"/>
      <json:array name="result" var="dataResource" items="${dataResources}">
        <json:object>
          <json:property name="score" value="${dataResource[0]}"/>
          <json:property name="dataResource" value="${dataResource[1].name}"/>
          <json:property name="dataResourceUrl">
            ${pageContext.request.contextPath}/datasets/resource/${dataResource[1].id}
          </json:property>
          <json:property name="occurrences" value="${dataResource[1].occurrenceCount}"/>
        </json:object>
      </json:array>
</json:object>
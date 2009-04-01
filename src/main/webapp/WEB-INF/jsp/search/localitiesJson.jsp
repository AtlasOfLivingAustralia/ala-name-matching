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
    <json:property name="totalRecords" value="${localitiesTotal}"/>
    <json:property name="startIndex" value="${startIndex}"/>
    <json:property name="sort" value="${sort}"/>
    <json:property name="dir" value="${dir}"/>
    <json:property name="pageSize" value="${pageSize}"/>
      <json:array name="result" var="locality" items="${localities}">
        <json:object>
          <json:property name="score" value="${locality[0]}"/>
          <json:property name="locality" value="${locality[1].name}"/>
          <json:property name="localityUrl">
            ${pageContext.request.contextPath}/regions/${locality[1].geoRegion.id}/locality/${locality[1].id}?map=google
          </json:property>
          <json:property name="state" value="${locality[1].state}"/>
          <json:property name="postcode" value="${locality[1].postcode}"/>
          <json:property name="geoRegion" value="${locality[1].geoRegion.name}"/>
        </json:object>
      </json:array>
</json:object>

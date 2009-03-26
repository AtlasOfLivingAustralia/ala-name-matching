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
    <json:property name="totalRecords" value="${geoRegionsTotal}"/>
    <json:property name="startIndex" value="${startIndex}"/>
    <json:property name="sort" value="${sort}"/>
    <json:property name="dir" value="${dir}"/>
    <json:property name="pageSize" value="${pageSize}"/>
      <json:array name="result" var="geoRegion" items="${geoRegions}">
        <json:object>
          <json:property name="score" value="${geoRegion[0]}"/>
          <json:property name="geoRegion" value="${geoRegion[1].name}"/>
          <json:property name="geoRegionUrl">
            ${pageContext.request.contextPath}/regions/${geoRegion[1].id}
          </json:property>
          <json:property name="acronym" value="${geoRegion[1].acronym}"/>
          <json:property name="geoRegionType" value="${geoRegion[1].geoRegionType.name}"/>
        </json:object>
      </json:array>
</json:object>
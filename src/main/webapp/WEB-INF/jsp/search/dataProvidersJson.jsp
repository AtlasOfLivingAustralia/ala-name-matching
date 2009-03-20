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
    <json:property name="totalRecords" value="${dataProvidersTotal}"/>
    <json:property name="startIndex" value="${startIndex}"/>
    <json:property name="sort" value="${sort}"/>
    <json:property name="dir" value="${dir}"/>
    <json:property name="pageSize" value="${pageSize}"/>
      <json:array name="result" var="dataProvider" items="${dataProviders}">
        <json:object>
          <json:property name="score" value="${dataProvider[0]}"/>
          <json:property name="dataProvider" value="${dataProvider[1].name}"/>
          <json:property name="dataProviderUrl">
            ${pageContext.request.contextPath}/datasets/provider/${dataProvider[1].id}
          </json:property>
          <json:property name="dataResourceCount" value="${dataProvider[1].dataResourceCount}"/>
          <json:property name="occurrences" value="${dataProvider[1].occurrenceCount}"/>
        </json:object>
      </json:array>
</json:object>
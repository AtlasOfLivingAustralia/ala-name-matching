<%@ include file="/common/taglibs.jsp"%>
<style>
thead { color: #0166B9; }
table {border-collapse: collapse;}
td,th,tr { border: 1px solid #CCCCCC; padding:0 4px 0 4px; font-size:12px;}
</style>

<div id="twopartheader">
    <h2>Current Web services</h2>
</div>
<div>

<table>
<thead>
    <th>URL</th>
    <th>Remote ID</th>
    <th>Last Harvest Attempt</th>
    <th>Last Extract Attempt</th>
    <th>Country</th>
</thead>
<tbody>
<c:forEach items="${resourceAccessPoints}" var="resourceAccessPoint">
    <tr>
	    <td><a href="${resourceAccessPoint.url}">${resourceAccessPoint.url}</a></td>
		<td>${resourceAccessPoint.remoteId}</td>
		<td><fmt:formatDate value="${resourceAccessPoint.lastHarvestStart}" pattern="MMM-dd-yyyy"/></td>
        <td>${resourceAccessPoint.lastExtractStart}</td>
        <td><spring:message code="country.${fn:toUpperCase(resourceAccessPoint.isoCountryCode)}" text="${resourceAccessPoint.isoCountryCode}"/></td>
    </tr>
</c:forEach>
</tbody>
</table>
</div>
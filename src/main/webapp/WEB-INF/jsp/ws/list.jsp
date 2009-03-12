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
<p>
Note: Web Services are marked as "unavailable" if an endpoint doesnt respond
    to a metadata request.
</p>

<form action="${pageContext.request.contextPath}/register/list.htm">
    <label for="iso">Select a country</label>
    <select name="iso">
        <option value="au">Australia</option>
        <option value="us">United States</option>
        <option value="">View all</option>
    </select>
    <input type="submit" value="submit"/>
</form>

<table>
	<thead>
	    <th>ID</th>
	    <th>URL</th>
	    <th>Available</th>
	    <th>Remote ID</th>
	    <th>Last Harvest Attempt</th>
	    <th>Last Extract Attempt</th>
        <th>Occurrences (unextracted)</th>
	    <th>Host Country</th>
	</thead>
<tbody>
<c:forEach items="${resourceAccessPoints}" var="resourceAccessPoint">
    <tr>
        <td>${resourceAccessPoint.id}</td>
	    <td><a href="${resourceAccessPoint.url}">${resourceAccessPoint.url}</a></td>
	    <c:choose>
	       <c:when test="${resourceAccessPoint.available}">
    	       <td style="background-color: green;">&nbsp;</td>
    	   </c:when>
    	   <c:otherwise>
    	       <td style="background-color: red;">&nbsp;</td>
    	   </c:otherwise>
	    </c:choose>
		<td>${resourceAccessPoint.remoteId}</td>
		<td><fmt:formatDate value="${resourceAccessPoint.lastHarvestStart}" pattern="MMM-dd-yyyy"/></td>
        <td><fmt:formatDate value="${resourceAccessPoint.lastExtractStart}" pattern="MMM-dd-yyyy"/></td>
        <td>${resourceAccessPoint.occurrenceCount}</td>
        <td><spring:message code="country.${fn:toUpperCase(resourceAccessPoint.isoCountryCode)}" text="${resourceAccessPoint.isoCountryCode}"/></td>
    </tr>
</c:forEach>
</tbody>
</table>
</div>
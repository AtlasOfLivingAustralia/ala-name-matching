<%@ include file="/common/taglibs.jsp"%>
<h4 style="margin:0px">Occurrence Counts</h4>
<p>
    Occurrences:&nbsp;${occurrenceSearchCounts.recordCount}<br/>
    Species:&nbsp;${occurrenceSearchCounts.speciesCount}<br/>
    Genus:&nbsp;${occurrenceSearchCounts.genusCount}<br/>
    Family:&nbsp;${occurrenceSearchCounts.familyCount}<br/>
    Basis Of Record:<br/>
    <c:forEach items="${occurrenceSearchCounts.basisOfRecordCounts}" var="bor">
        &bull;&nbsp;${bor.key}:&nbsp;${bor.value}<br/>
    </c:forEach>
    <br/>
    <c:if test="${extraParams!='' && bbox!=null}">
        <c:set var="searchCriteria" scope="request">
            ${extraParams}&minX=${bbox.minLong}&minY=${bbox.minLat}&maxX=${bbox.maxLong}&maxY=${bbox.maxLat}
        </c:set>
        &raquo;&nbsp;<a href="${pageContext.request.contextPath}/occurrences/boundingBoxWithCriteria.htm?${searchCriteria}">Search for occurrence records in this cell</a>
    </c:if>
</p>
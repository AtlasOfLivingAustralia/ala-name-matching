<%@ include file="/common/taglibs.jsp"%>
<c:set var="pointsTotal" value="${dataResource.occurrenceCoordinateCount}" scope="request"/>
<c:set var="occurrenceCount" value="${dataResource.occurrenceCount}" scope="request"/>
<c:set var="entityName" scope="request">${dataResource.name}</c:set>

<c:set var="entityId" scope="request">${dataResource.key}</c:set>
<c:set var="entityPath" scope="request" value="datasets/resource"/>

<c:set var="extraParams" scope="request"><gbif:criterion subject="24" predicate="0" value="${dataResource.key}" index="0"/></c:set>
<script type="text/javascript">
    var entityId = '${dataResource.key}';
    var entityType = '4';
    var entityName = '${dataResource.name}';
    var minLongitude = 112;
    var minLatitude = -43.7;
    var maxLongitude = 154;
    var maxLatitude = -10.3;
</script>
<jsp:include page="/WEB-INF/jsp/mapping/openlayer.jsp"/>
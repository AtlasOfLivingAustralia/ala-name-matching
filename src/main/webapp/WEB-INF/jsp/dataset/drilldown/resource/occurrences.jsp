<%@ include file="/common/taglibs.jsp"%>
<c:set var="pointsTotal" value="${dataResource.occurrenceCoordinateCount}" scope="request"/>
<c:set var="occurrenceCount" value="${dataResource.occurrenceCount}" scope="request"/>
<c:set var="entityName" scope="request">${dataResource.name}</c:set>
<c:set var="extraParams" scope="request"><gbif:criterion subject="24" predicate="0" value="${dataResource.key}" index="0"/></c:set>
<script type="text/javascript">
    var entityId = '${dataResource.key}';
    var entityType = '4';
    var filterId = '24';
    var entityName = '${dataResource.name}';
    var minLongitude = -180;
    var minLatitude = -90;
    var maxLongitude = 180;
    var maxLatitude = 90;
</script>
<jsp:include page="/WEB-INF/jsp/mapping/openlayer.jsp"/>
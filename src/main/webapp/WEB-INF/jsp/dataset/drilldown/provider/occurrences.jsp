<%@ include file="/common/taglibs.jsp"%>
<c:set var="pointsTotal" value="${dataProvider.occurrenceCoordinateCount}" scope="request"/>
<c:set var="occurrenceCount" value="${dataProvider.occurrenceCount}" scope="request"/>
<c:set var="entityName" scope="request">${dataProvider.name}</c:set>
<c:set var="extraParams" scope="request"><gbif:criterion subject="25" predicate="0" value="${dataProvider.key}" index="0"/></c:set>
<script type="text/javascript">
    var entityId = '${dataProvider.key}';
    var entityType = '5';
    var entityName = '${dataProvider.name}';
    var minLongitude = 112;
    var minLatitude = -46;
    var maxLongitude = 154;
    var maxLatitude = -10;
</script>
<jsp:include page="/WEB-INF/jsp/mapping/openlayer.jsp"/>
<%@ include file="/common/taglibs.jsp"%>
<c:set var="pointsTotal" value="${dataProvider.occurrenceCoordinateCount}" scope="request"/>
<c:set var="occurrenceCount" value="${dataProvider.occurrenceCount}" scope="request"/>
<c:set var="entityName" scope="request">${dataProvider.name}</c:set>
<c:set var="entityId" scope="request">${dataProvider.key}</c:set>
<c:set var="entityPath" scope="request" value="datasets/provider"/>
<c:set var="extraParams" scope="request"><gbif:criterion subject="25" predicate="0" value="${dataProvider.key}" index="0"/></c:set>
<script type="text/javascript">
    var entityId = '${dataProvider.key}';
    var entityType = '3';
    var entityName = '${dataProvider.name}';
    var minLongitude = 112;
    var minLatitude = -43.7;
    var maxLongitude = 154;
    var maxLatitude = -10.3;
</script>
<jsp:include page="/WEB-INF/jsp/mapping/openlayer.jsp"/>
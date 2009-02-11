<%@ include file="/common/taglibs.jsp"%>
<c:set var="entityName" scope="request"><gbif:taxonPrint concept="${taxonConcept}"/></c:set>
<c:set var="mapTitle" scope="request">${taxonConcept.taxonName}</c:set>
<c:set var="entityId" scope="request">${taxonConcept.key}</c:set>
<c:set var="entityPath" scope="request" value="species"/>
<c:set var="extraParams" scope="request"><gbif:criteria criteria="${occurrenceCriteria}"/></c:set>
<script type="text/javascript">
    var entityId = '${taxonConcept.key}';
    var entityType = '1';
    var entityName = '${entityName}';
    var minLongitude = 112;
    var minLatitude = -43.7;
    var maxLongitude = 154;
    var maxLatitude = -10.3;
</script>
<jsp:include page="/WEB-INF/jsp/mapping/openlayer.jsp"/>
<div style="margin-left:30px;">
   <c:set var="occurrenceSearchSubject" value="20" scope="request"/> 
   <c:set var="occurrenceSearchValue" value="${taxonConcept.key}" scope="request"/>
 <tiles:insert page="/WEB-INF/jsp/geography/drilldown/dataRecord.jsp"/>
</div>
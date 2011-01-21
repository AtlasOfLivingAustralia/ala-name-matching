<%@ include file="/common/taglibs.jsp"%>
<table id="geospatialTable" class="occurrenceTable" cellspacing="0">
    <alatag:occurrenceTableRow messageCode="occurrence.record.country" section="geospatial" fieldName="country" annotate="false">
        <c:choose>
            <c:when test="${not empty occurrenceRecord.isoCountryCode}">
                <gbif:capitalize><spring:message code="country.${occurrenceRecord.isoCountryCode}"/></gbif:capitalize>
                <gbiftag:isCountryInferred issuesBit="${occurrenceRecord.otherIssue}"/>
                <c:if test="${countryInferred}">
                    <spring:message code="occurrence.record.inferred.from.coordinates"/>
                </c:if>
            </c:when>
            <c:otherwise>
                <c:if test="${not empty rawOccurrenceRecord.county}">
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-county">${rawOccurrenceRecord.county}</span>
                </c:if>
            </c:otherwise>
        </c:choose>
    </alatag:occurrenceTableRow>
    
    <alatag:occurrenceTableRow messageCode="occurrence.record.country" section="geospatial" fieldName="country" annotate="false">
        <c:if test="${not empty rawOccurrenceRecord.county}">
             <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-county">${rawOccurrenceRecord.county}</span>
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="occurrence.record.state.or.province" section="geospatial" fieldName="state" annotate="true">
        <c:if test="${not empty rawOccurrenceRecord.stateOrProvince}">
            <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-state">${rawOccurrenceRecord.stateOrProvince}</span>
        </c:if>
        <c:if test="${not empty state}">
           (inferred from coordinates as <a href="${pageContext.request.contextPath}/regions/${state.id}">${state.name}</a>)
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="occurrence.record.locality" section="geospatial" fieldName="locality" annotate="true">
        <c:if test="${not empty rawOccurrenceRecord.locality}">
            <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-locality">${rawOccurrenceRecord.locality}</span>
        </c:if>
        <c:if test="${not empty town || not empty city}">
           (inferred from coordinates as 
            <c:if test="${not empty town}"><a href="${pageContext.request.contextPath}/regions/${town.id}">${town.name}</a></c:if>
            <c:if test="${not empty city}"><a href="${pageContext.request.contextPath}/regions/${city.id}">${city.name}</a></c:if>)
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="Shire" section="geospatial" fieldName="shire" annotate="false">
        <c:if test="${not empty shire}">
            <a href="${pageContext.request.contextPath}/regions/${shire.id}">${shire.name}</a>
        </c:if>
    </alatag:occurrenceTableRow>
    
    <alatag:occurrenceTableRow messageCode="Biogeographic&nbsp;region" section="geospatial" fieldName="ibra" annotate="false">
        <c:if test="${not empty ibra.name}">
            <a href="${pageContext.request.contextPath}/regions/${bioregion.id}">${ibra.name}</a>
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="Marine&nbsp;region" section="geospatial" fieldName="imcra" annotate="false">
        <c:if test="${not empty imcra.name}">
            <a href="${pageContext.request.contextPath}/regions/${imcra.id}">${imcra.name}</a>
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="River&nbsp;basin" section="geospatial" fieldName="riverbasin" annotate="false">
        <c:if test="${not empty riverbasin.name}">
            <a href="${pageContext.request.contextPath}/regions/${riverbasin.id}"><gbif:capitalize>${riverbasin.name}</gbif:capitalize></a>
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="occurrence.record.geospatial.latitude" section="geospatial" fieldName="latitude" annotate="true">
        <c:if test="${not empty rawOccurrenceRecord.latitude}">
            <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-latitude">${rawOccurrenceRecord.latitude}</span>
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="occurrence.record.geospatial.longitude" section="geospatial" fieldName="longitude" annotate="true">
        <c:if test="${not empty rawOccurrenceRecord.longitude}">
            <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-longitude">${rawOccurrenceRecord.longitude}</span>
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="occurrence.record.geospatial.latLongPrecision" section="geospatial" fieldName="latLongPrecision" annotate="false">
        <c:if test="${not empty rawOccurrenceRecord.latLongPrecision}">
            <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-latlongPrecision">${rawOccurrenceRecord.latLongPrecision}</span>
        </c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="occurrence.record.geospatial.altitude" section="geospatial" fieldName="altitude" annotate="false">
        <c:if test="${not empty rawOccurrenceRecord.minAltitude}"><spring:message code="minimum"/> <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-minAltitude">${rawOccurrenceRecord.minAltitude}</span></c:if><c:if test="${not empty rawOccurrenceRecord.minAltitude && not empty rawOccurrenceRecord.maxAltitude}">,</c:if>
        <c:if test="${not empty rawOccurrenceRecord.maxAltitude}"><spring:message code="maximum"/> <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-maxAltitude">${rawOccurrenceRecord.maxAltitude}</span></c:if>
        <c:if test="${not empty rawOccurrenceRecord.altitudePrecision}"><spring:message code="occurrence.record.with.precision"/> ${rawOccurrenceRecord.altitudePrecision}</c:if>
        <c:if test="${not empty occurrenceRecord.altitudeInMetres}"><spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.altitudeInMetres} metres" argumentSeparator="$$$$$"/></c:if>
    </alatag:occurrenceTableRow>

    <alatag:occurrenceTableRow messageCode="occurrence.record.geospatial.depth" section="geospatial" fieldName="depth" annotate="false">
        <c:if test="${not empty rawOccurrenceRecord.minDepth}"><spring:message code="minimum"/> <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-minDepth">${rawOccurrenceRecord.minDepth}</span></c:if><c:if test="${not empty rawOccurrenceRecord.minDepth && not empty rawOccurrenceRecord.maxDepth}">,</c:if>
        <c:if test="${not empty rawOccurrenceRecord.maxDepth}"><spring:message code="maximum"/> <span id="occurrenceRecord-${rawOccurrenceRecord.key}-geospatial-maxDepth">${rawOccurrenceRecord.maxDepth}</span></c:if>
        <c:if test="${not empty rawOccurrenceRecord.depthPrecision}"><spring:message code="occurrence.record.with.precision"/> ${rawOccurrenceRecord.depthPrecision}</c:if>
        <c:if test="${not empty occurrenceRecord.depthInMetres}"><spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.depthInMetres} metres" argumentSeparator="$$$$$"/></c:if>
    </alatag:occurrenceTableRow>

    <c:if test="${empty rawOccurrenceRecord.longitude && empty rawOccurrenceRecord.latitude}">
        <tr><td class="label">No geospatial data provided for this record.</td></tr>
    </c:if>
</table>

<c:if test="${occurrenceRecord.latitude!=null && occurrenceRecord.longitude!=null && (occurrenceRecord.longitude!=0 && occurrenceRecord.latitude!=0) }">
	<c:set var="mapDivName" value="map" scope="request"/>
	<c:set var="pointsClickable" value="false" scope="request"/>
	<tiles:insert page="/WEB-INF/jsp/geography/googleMap.jsp"/>
	<a href="${pageContext.request.contextPath}/occurrences/${occurrenceRecord.key}/largeMap"><spring:message code="occurrence.record.view.large.map"/></a>
</c:if>
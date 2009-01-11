<%@ include file="/common/taglibs.jsp"%>
<fieldset>
<p>	
	<label for="country"><spring:message code="occurrence.record.country"/>:</label> 
	<c:choose>
	<c:when test="${not empty occurrenceRecord.isoCountryCode}">
		<a href="${pageContext.request.contextPath}/countries/${occurrenceRecord.isoCountryCode}"><gbif:capitalize><spring:message code="country.${occurrenceRecord.isoCountryCode}"/></gbif:capitalize></a>
		<gbiftag:isCountryInferred issuesBit="${occurrenceRecord.otherIssue}"/>
		<c:if test="${countryInferred}">
			<spring:message code="occurrence.record.inferred.from.coordinates"/>
		</c:if>
	</c:when>
	<c:otherwise>
		${rawOccurrenceRecord.country}
	</c:otherwise>
	</c:choose>	
</p>
<p>	
	<label for="county"><spring:message code="occurrence.record.county"/>:</label> 
	${rawOccurrenceRecord.county}
</p>	
<p>
	<label for="stateOrProvince"><spring:message code="occurrence.record.state.or.province"/>:</label> 
	${rawOccurrenceRecord.stateOrProvince}
</p>	
<p>
	<label for="stateOrProvince"><spring:message code="occurrence.record.region"/>:</label> 
	<c:forEach items="${geoRegions}" var="geoRegion">
		<a href="${pageContext.request.contextPath}/regions/${geoRegion.id}">${geoRegion.name}</a>; 
	</c:forEach>
</p>	
<p>
	<label for="locality"><spring:message code="occurrence.record.locality"/>:</label> 
	${rawOccurrenceRecord.locality}
</p>	
<p>
	<label for="latitude"><spring:message code="occurrence.record.geospatial.latitude"/>:</label> 
	${rawOccurrenceRecord.latitude}
</p>	
<p>
	<label for="longitude"><spring:message code="occurrence.record.geospatial.longitude"/>:</label> 
	${rawOccurrenceRecord.longitude}
</p>	
<p>
	<label for="latLongPrecision"><spring:message code="occurrence.record.geospatial.latLongPrecision"/>:</label> 
	${rawOccurrenceRecord.latLongPrecision}
</p>	
<p>
	<label for="altitude"><spring:message code="occurrence.record.geospatial.altitude"/>:</label> 
	<c:if test="${not empty rawOccurrenceRecord.minAltitude}"><spring:message code="minimum"/> ${rawOccurrenceRecord.minAltitude}</c:if><c:if test="${not empty rawOccurrenceRecord.minAltitude && not empty rawOccurrenceRecord.maxAltitude}">,</c:if> 
	<c:if test="${not empty rawOccurrenceRecord.maxAltitude}"><spring:message code="maximum"/> ${rawOccurrenceRecord.maxAltitude}</c:if>
	<c:if test="${not empty rawOccurrenceRecord.altitudePrecision}"><spring:message code="occurrence.record.with.precision"/> ${rawOccurrenceRecord.altitudePrecision}</c:if>
	<c:if test="${not empty occurrenceRecord.altitudeInMetres}">
		<spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.altitudeInMetres} metres" argumentSeparator="$$$$$"/>
	</c:if>
</p>	
<p>
	<label for="depth"><spring:message code="occurrence.record.geospatial.depth"/>:</label> 
	<c:if test="${not empty rawOccurrenceRecord.minDepth}"><spring:message code="minimum"/> ${rawOccurrenceRecord.minDepth}</c:if><c:if test="${not empty rawOccurrenceRecord.minDepth && not empty rawOccurrenceRecord.maxDepth}">,</c:if> 
	<c:if test="${not empty rawOccurrenceRecord.maxDepth}"><spring:message code="maximum"/> ${rawOccurrenceRecord.maxDepth}</c:if> 
	<c:if test="${not empty rawOccurrenceRecord.depthPrecision}"><spring:message code="occurrence.record.with.precision"/> ${rawOccurrenceRecord.depthPrecision}</c:if>
	<c:if test="${not empty occurrenceRecord.depthInMetres}">
		<spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.depthInMetres} metres" argumentSeparator="$$$$$"/>
	</c:if>
</p>	
</fieldset>

<c:if test="${occurrenceRecord.latitude!=null && occurrenceRecord.longitude!=null && (occurrenceRecord.longitude!=0 && occurrenceRecord.latitude!=0) }">
	<c:set var="mapDivName" value="map" scope="request"/>
	<c:set var="pointsClickable" value="false" scope="request"/>
	<tiles:insert page="/WEB-INF/jsp/geography/googleMap.jsp"/>
	<a href="${pageContext.request.contextPath}/occurrences/${occurrenceRecord.key}/largeMap"><spring:message code="occurrence.record.view.large.map"/></a>
</c:if>
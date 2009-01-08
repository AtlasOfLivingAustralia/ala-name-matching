<%@ include file="/common/taglibs.jsp" %>
<div id="twopartheader">
    <h2><spring:message code="regions.list.main.title"/></h2>
    <ul>
    	<c:choose>
			<c:when test="${geoRegionType.name == 'states'}">
				<li id="chosen"><spring:message code="regions.list.states.shorttitle"/></li>
			</c:when>
			<c:otherwise>
				<li><a href="${pageContext.request.contextPath}/regions/browse/states"><spring:message code="regions.list.states.shorttitle"/></a></li>
			</c:otherwise>
		</c:choose>
    	<c:choose>
			<c:when test="${geoRegionType.name == 'ibra'}">
				<li id="chosen"><spring:message code="regions.list.ibra.shorttitle"/></li>
			</c:when>
			<c:otherwise>
				<li><a href="${pageContext.request.contextPath}/regions/browse/ibra"><spring:message code="regions.list.ibra.shorttitle"/></a></li>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${geoRegionType.name == 'imra'}">
				<li id="chosen"><spring:message code="regions.list.imra.shorttitle"/></li>
			</c:when>
			<c:otherwise>
				<li><a href="${pageContext.request.contextPath}/regions/browse/imra"><spring:message code="regions.list.imra.shorttitle"/></a></li>
			</c:otherwise>
		</c:choose>
    </ul>
</div>
<div class="subcontainer">
<h3>
    <c:if test="${geoRegionType.name == 'states'}"><spring:message code="regions.list.states.title"/></c:if>
    <c:if test="${geoRegionType.name == 'ibra'}"><spring:message code="regions.list.ibra.title"/></c:if>
    <c:if test="${geoRegionType.name == 'imra'}"><spring:message code="regions.list.imra.title"/></c:if>
    <c:if test="${geoRegionType.name == 'rivers'}"><spring:message code="regions.list.rivers.title"/></c:if>
</h3>

<display:table name="geoRegions" export="false" class="statistics" id="geoRegion">
	<display:column titleKey="regions.list.table.title" class="name">
		<a href="${pageContext.request.contextPath}/regions/${geoRegion.id}">${geoRegion.name}</a>
	</display:column>
	<display:column titleKey="dataset.list.occurrence.count" class="countrycount">
	  	<c:if test="${geoRegion.occurrenceCount>0}"><a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="5" predicate="0" value="${geoRegion.isoCountryCode}" index="0"/>"></c:if><fmt:formatNumber value="${geoRegion.occurrenceCount}" pattern="###,###"/><c:if test="${geoRegion.occurrenceCount>0}"></a></c:if>
	  	(<c:if test="${geoRegion.occurrenceCoordinateCount>0}"><a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="5" predicate="0" value="${geoRegion.isoCountryCode}" index="0"/>&<gbif:criterion subject="28" predicate="0" value="0" index="1"/>"></c:if><fmt:formatNumber value="${geoRegion.occurrenceCoordinateCount}" pattern="###,###"/><c:if test="${geoRegion.occurrenceCoordinateCount>0}"></a></c:if>)
	</display:column>	  
	<display:column titleKey="dataset.speciesCount" class="countrycount">
	  	<c:if test="${geoRegion.speciesCount>0}"><a href="${pageContext.request.contextPath}/occurrences/searchSpecies.htm?<gbif:criterion subject="5" predicate="0" value="${geoRegion.isoCountryCode}" index="0"/>"></c:if><fmt:formatNumber value="${geoRegion.speciesCount}" pattern="###,###"/><c:if test="${geoRegion.speciesCount>0}"></a></c:if>
  	    </display:column>
	<display:setProperty name="basic.msg.empty_list">
	</display:setProperty>
	<display:setProperty name="basic.empty.showtable">false</display:setProperty>
</display:table>

</div>
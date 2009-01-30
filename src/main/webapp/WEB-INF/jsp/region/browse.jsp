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
            <c:when test="${geoRegionType.name == 'cities'}">
                <li id="chosen"><spring:message code="regions.list.cities.shorttitle"/></li>
            </c:when>
            <c:otherwise>
                <li><a href="${pageContext.request.contextPath}/regions/browse/cities"><spring:message code="regions.list.cities.shorttitle"/></a></li>
            </c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${geoRegionType.name == 'shires'}">
                <li id="chosen"><spring:message code="regions.list.shires.shorttitle"/></li>
            </c:when>
            <c:otherwise>
                <li><a href="${pageContext.request.contextPath}/regions/browse/shires"><spring:message code="regions.list.shires.shorttitle"/></a></li>
            </c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${geoRegionType.name == 'towns'}">
                <li id="chosen"><spring:message code="regions.list.towns.shorttitle"/></li>
            </c:when>
            <c:otherwise>
                <li><a href="${pageContext.request.contextPath}/regions/browse/towns"><spring:message code="regions.list.towns.shorttitle"/></a></li>
            </c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${geoRegionType.name == 'lga'}">
                <li id="chosen"><spring:message code="regions.list.lga.shorttitle"/></li>
            </c:when>
            <c:otherwise>
                <li><a href="${pageContext.request.contextPath}/regions/browse/lga"><spring:message code="regions.list.lga.shorttitle"/></a></li>
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
			<c:when test="${geoRegionType.name == 'imcra'}">
				<li id="chosen"><spring:message code="regions.list.imcra.shorttitle"/></li>
			</c:when>
			<c:otherwise>
				<li><a href="${pageContext.request.contextPath}/regions/browse/imcra"><spring:message code="regions.list.imcra.shorttitle"/></a></li>
			</c:otherwise>
		</c:choose>
        <c:choose>
            <c:when test="${geoRegionType.name == 'rivers'}">
                <li id="chosen"><spring:message code="regions.list.rivers.shorttitle"/></li>
            </c:when>
            <c:otherwise>
                <li><a href="${pageContext.request.contextPath}/regions/browse/rivers"><spring:message code="regions.list.rivers.shorttitle"/></a></li>
            </c:otherwise>
        </c:choose>
    </ul>
</div>
<div class="subcontainer">
<h3>
    <c:if test="${geoRegionType.name == 'states'}"><spring:message code="regions.list.states.title"/></c:if>
    <c:if test="${geoRegionType.name == 'cities'}"><spring:message code="regions.list.cities.title"/></c:if>
    <c:if test="${geoRegionType.name == 'shires'}"><spring:message code="regions.list.shires.title"/></c:if>
    <c:if test="${geoRegionType.name == 'towns'}"><spring:message code="regions.list.towns.title"/></c:if>
    <c:if test="${geoRegionType.name == 'lga'}"><spring:message code="regions.list.lga.title"/></c:if>
    <c:if test="${geoRegionType.name == 'ibra'}"><spring:message code="regions.list.ibra.title"/></c:if>
    <c:if test="${geoRegionType.name == 'imcra'}"><spring:message code="regions.list.imcra.title"/></c:if>
    <c:if test="${geoRegionType.name == 'rivers'}"><spring:message code="regions.list.rivers.title"/></c:if>
</h3>

<display:table name="geoRegions" export="false" class="statistics" id="geoRegion">
	<display:column titleKey="regions.list.table.title" class="name">
			<a href="${pageContext.request.contextPath}/regions/${geoRegion.id}"><gbif:capitalize>${geoRegion.name}</gbif:capitalize></a>
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
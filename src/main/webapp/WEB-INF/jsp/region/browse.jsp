<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2><spring:message code="regions.list.main.title"/></h2>
    <ul>
    	<li><a href="#states"><spring:message code="regions.list.states.shorttitle"/></a></li>
    	<li><a href="#ibra"><spring:message code="regions.list.ibra.shorttitle"/></a></li>
    	<li><a href="#imra"><spring:message code="regions.list.imra.shorttitle"/></a></li>
    </ul>
</div>
<div class="subcontainer">
<a name="states"/>
<h2><spring:message code="regions.list.states.title"/></h2>
<ul>
	<c:forEach items="${geoRegions}" var="geoRegion">
		<c:if test="${geoRegion.regionType <= 2}">
			<li><a href="${pageContext.request.contextPath}/regions/view.htm?id=${geoRegion.id}">${geoRegion.name}</a></li>
		</c:if>
	</c:forEach>
</ul>
<a name="ibra"/>
<h2><spring:message code="regions.list.ibra.title"/></h2>
<ul>
	<c:forEach items="${geoRegions}" var="geoRegion">
		<c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">
			<li><a href="${pageContext.request.contextPath}/regions/view.htm?id=${geoRegion.id}">${geoRegion.name}</a></li>
		</c:if>
	</c:forEach>
</ul>
<a name="imra"/>
<h2><spring:message code="regions.list.imra.title"/></h2>
<ul>
	<c:forEach items="${geoRegions}" var="geoRegion">
		<c:if test="${geoRegion.regionType >=3000 && geoRegion.regionType <4000}">
			<li><a href="${pageContext.request.contextPath}/regions/view.htm?id=${geoRegion.id}">${geoRegion.name}</a></li>
		</c:if>
	</c:forEach>
</ul>

<% /*
<h2><spring:message code="regions.list.rivers.title"/></h2>
<ul>
	<c:forEach items="${geoRegions}" var="geoRegion">
		<c:if test="${geoRegion.regionType >=5000 && geoRegion.regionType <6000}">
			<li><a href="${pageContext.request.contextPath}/regions/view.htm?id=${geoRegion.id}">${geoRegion.name}</a></li>
		</c:if>
	</c:forEach>
</ul>
*/ %>
</div>
<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2>Browse Geographic Regions</h2>
    <ul>
    	<li><a href="#states">States and Territories</a></li>
    	<li><a href="#ibra">Biogeographic Regions</a></li>
    	<li><a href="#imra">Marine and Coastal Regions</a></li>
    </ul>
</div>
<div class="subcontainer">
<a name="states"/>
<h2>States and Territories</h2>
<ul>
	<c:forEach items="${geoRegions}" var="geoRegion">
		<c:if test="${geoRegion.regionType <= 2}">
			<li><a href="${pageContext.request.contextPath}/regions/view.htm?id=${geoRegion.id}">${geoRegion.name}</a></li>
		</c:if>
	</c:forEach>
</ul>
<a name="ibra"/>
<h2>IBRA - Interim Biogeographic Regionalisation of Australia</h2>
<ul>
	<c:forEach items="${geoRegions}" var="geoRegion">
		<c:if test="${geoRegion.regionType >=2000 && geoRegion.regionType <3000}">
			<li><a href="${pageContext.request.contextPath}/regions/view.htm?id=${geoRegion.id}">${geoRegion.name}</a></li>
		</c:if>
	</c:forEach>
</ul>
<a name="imra"/>
<h2>IMCRA - Integrated Marine and Coastal Regionalisation of Australia</h2>
<ul>
	<c:forEach items="${geoRegions}" var="geoRegion">
		<c:if test="${geoRegion.regionType >=3000 && geoRegion.regionType <4000}">
			<li><a href="${pageContext.request.contextPath}/regions/view.htm?id=${geoRegion.id}">${geoRegion.name}</a></li>
		</c:if>
	</c:forEach>
</ul>

<% /*
<h2>River Basins</h2>
<ul>
	<c:forEach items="${geoRegions}" var="geoRegion">
		<c:if test="${geoRegion.regionType >=5000 && geoRegion.regionType <6000}">
			<li><a href="${pageContext.request.contextPath}/regions/view.htm?id=${geoRegion.id}">${geoRegion.name}</a></li>
		</c:if>
	</c:forEach>
</ul>
*/ %>
</div>
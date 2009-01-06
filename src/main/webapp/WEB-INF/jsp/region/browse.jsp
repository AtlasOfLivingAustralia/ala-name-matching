<%@ include file="/common/taglibs.jsp"%>
<h1>Regions</h1>

<c:forEach items="${geoRegions}" var="geoRegion">
<a href="${pageContext.request.contextPath}/regions/?id=${geoRegion.id}">${geoRegion.name}</a> (${geoRegion.regionType})<br/>
</c:forEach>
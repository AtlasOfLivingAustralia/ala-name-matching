<%@ include file="/common/taglibs.jsp"%>
<c:set var="configuredGoogleKey"><gbif:propertyLoader bundle="portal" property="googleKey"/></c:set>
<c:choose>
<c:when test="${not empty configuredGoogleKey}">
<c:set var="googleKey" value="${configuredGoogleKey}"/>
</c:when>
<c:when test="${header.host=='ala01-be.nexus.csiro.au'}">
<c:set var="googleKey" value="ABQIAAAA-3PPe-HBV33KJbGlAmg4xhQ_aaJi1zMfNZn5_K1VEqCWS5WDDhTetMutTeqBCcJpwcdpdGrAkiTTOQ"/>
</c:when>
<c:when test="${header.host=='alatstweb1-syd.nexus.csiro.au'}">
<c:set var="googleKey" value="ABQIAAAA-3PPe-HBV33KJbGlAmg4xhRyxz6H3jqpDCFlc2XGeGLJRHQ5chQE7J_RvtQf0OlytjYa4NZt79G30A"/>
</c:when>
<c:when test="${header.host=='alatstweb1-syd'}">
<c:set var="googleKey" value="ABQIAAAA-3PPe-HBV33KJbGlAmg4xhTjgHo_nI0iwt9KgELP_FNBeQTyVBTQGK9ZOxaWkSHcbVzkudlNZwA39A"/>
</c:when>
</c:choose>
<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=${googleKey}" type="text/javascript"></script>
<script src="<c:url value='/javascript/LatLonGraticule.js'/>" type="text/javascript" language="javascript"></script>
<script src="<c:url value='/javascript/googlemaps.js'/>" type="text/javascript" language="javascript"></script>
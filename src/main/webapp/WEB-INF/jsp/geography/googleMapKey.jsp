<%@ include file="/common/taglibs.jsp"%>
<c:set var="configuredGoogleKey"><gbif:propertyLoader bundle="portal" property="googleKey"/></c:set>
<c:choose>
<c:when test="${not empty configuredGoogleKey}">
<c:set var="googleKey" value="${configuredGoogleKey}"/>
</c:when>
<c:when test="${header.host=='alatstweb1-syd.nexus.csiro.au'}">
<c:set var="googleKey" value="ABQIAAAA_2zKI52BmWetar1csiyF-RQvRCjc3TQlw05MpVnVZIVuO5vVARQ_lGtzSKXT-B9U2_I5PczEkG-U6w"/>
</c:when>
<c:when test="${header.host=='alatstweb1-syd'}">
<c:set var="googleKey" value="ABQIAAAA-3PPe-HBV33KJbGlAmg4xhTjgHo_nI0iwt9KgELP_FNBeQTyVBTQGK9ZOxaWkSHcbVzkudlNZwA39A"/>
</c:when>
</c:choose>
<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=${googleKey}" type="text/javascript"></script>
<script src="<c:url value='/javascript/LatLonGraticule.js'/>" type="text/javascript" language="javascript"></script>
<script src="<c:url value='/javascript/googlemaps.js'/>" type="text/javascript" language="javascript"></script>
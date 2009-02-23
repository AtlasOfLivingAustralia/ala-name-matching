<%@ include file="/common/taglibs.jsp"%>
<div id="footer">
    <p id="copyright">Atlas of Living Australia &copy; 2008</p>
    <p id="contact_webmaster"><a href="mailto:atlasoflivingaustralia@csiro.au"><spring:message code="contact.us"/></a></p>
</div>

<c:set var="workOffline" scope="request"><gbif:propertyLoader bundle="portal" property="workOffline"/></c:set>
<c:if test="${!workOffline}">
<script type="text/javascript">
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
<script type="text/javascript">
var pageTracker = _gat._getTracker("N/A"); // "UA-4355440-1"
pageTracker._initData();
pageTracker._trackPageview();
</script>
</c:if>
<!-- End footer -->
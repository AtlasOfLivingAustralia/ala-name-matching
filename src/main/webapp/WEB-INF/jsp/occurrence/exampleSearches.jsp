<%@ include file="/common/taglibs.jsp"%>
<div id="exampleSearches">
<h4><spring:message code="occurrence.search.filter.example.searches"/></h4>
<ul id="exampleSearches" class="genericList" style="margin:0px;">
<li><a href="${pageContext.request.contextPath}/occurrences/search.htm?c[0].s=0&c[0].p=0&c[0].o=Spheniscidae&c[1].s=17&c[1].p=0&c[1].o=1">Observations of Penguins</a></li>
<li><a href="${pageContext.request.contextPath}/occurrences/search.htm?c[0].s=0&c[0].p=0&c[0].o=Eucalyptus+rossii&c[1].s=17&c[1].p=0&c[1].o=2">Specimens of Scribbly Gum</a></li>
</ul>
</div>
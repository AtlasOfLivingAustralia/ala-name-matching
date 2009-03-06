<%@ include file="/common/taglibs.jsp"%>
<spring:message code="taxonomy.browser.species.recorded.in" text="Species recorded in"/> <gbif:capitalize>${region.name}</gbif:capitalize>
<c:if test="${selectedConcept!=null}">
 - 
<spring:message code="taxonomy.browser.classification" text="Classification"/> 
<spring:message code="of" text="of"/> <string:capitalize>${selectedConcept.rank}</string:capitalize>: ${selectedConcept.taxonName} ${selectedConcept.author}
</c:if> 
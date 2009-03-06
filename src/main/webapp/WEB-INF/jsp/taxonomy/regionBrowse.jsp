<%@ include file="/common/taglibs.jsp"%>
<div id="taxonomy" class="taxonomyContainer">
	<div id="twopartheader">
		<h2>
		<c:choose>
			<c:when test="${selectedConcept!=null}">
				<spring:message code="taxonomy.browser.classification.of"/> 
				<span class="subject"><string:capitalize>${selectedConcept.rank}</string:capitalize>: 
				<gbif:taxonPrint concept="${selectedConcept}"/></span> 
				${selectedConcept.author}
			</c:when>
			<c:otherwise>
				<spring:message code="taxonomy.browser.classification"/>
			</c:otherwise>
		</c:choose>
		</h2>
		<h3>
			<spring:message code="taxonomy.browser.species.recorded.in" text="Species recorded in"/>:
			<a href="${pageContext.request.contextPath}/regions/${geoRegion.id}"><gbif:capitalize>${geoRegion.name}</gbif:capitalize></a>
		</h3>
	</div>
	<c:choose>
		<c:when test="${not empty regionConcepts}">
			<div id="furtherActions">
				<h4><spring:message code='actions.for'/> <gbif:capitalize>${geoRegion.name}</gbif:capitalize></h4>
				<table cellspacing="1" class="actionsList">
					<tbody>
						<tr valign="top">
							<td><b><spring:message code="actions.explore"/></b></td>
							<td>	
								<ul class="actionsListInline">
									<li>
										<a href="${pageContext.request.contextPath}/species/search.htm?<gbif:criterion subject="12" predicate="0" value="${geoRegion.id}"/>&<gbif:criterion subject="9" predicate="0" value="7000" index="1"/>"><spring:message code="taxonomy.browser.species.recorded.in" text="Species recorded in"/> <gbif:capitalize>${geoRegion.name}</gbif:capitalize></a>
									</li>						
									<li>
										<a href="${pageContext.request.contextPath}/species/search.htm?<gbif:criterion subject="12" predicate="0" value="${geoRegion.id}"/>"><spring:message code="taxonomy.browser.taxa.recorded.in" text="Taxa recorded in"/> <gbif:capitalize>${geoRegion.name}</gbif:capitalize></a>
									</li>						
								</ul>
							</td>
						</tr>					
						<tr valign="top">
							<td><b><spring:message code="actions.download"/></b></td>
							<td>	
								<ul class="actionsListInline">
									<li>
										<a href="${pageContext.request.contextPath}/species/downloadSpreadsheet.htm?<gbif:criterion subject="12" predicate="0" value="${geoRegion.id}"/>&<gbif:criterion subject="9" predicate="0" value="7000" index="1"/>"><spring:message code="taxonomy.browser.species.recorded.in" text="Species recorded in"/> <gbif:capitalize>${geoRegion.name}</gbif:capitalize></a>
									</li>						
									<li>
										<a href="${pageContext.request.contextPath}/species/downloadSpreadsheet.htm?<gbif:criterion subject="12" predicate="0" value="${geoRegion.id}"/>"><spring:message code="taxonomy.browser.taxa.recorded.in" text="Taxa recorded in"/> <gbif:capitalize>${geoRegion.name}</gbif:capitalize></a>
									</li>						
								</ul>
							</td>
						</tr>
					</tbody>
				</table>
			</div><!--end further actions-->		
			<div>
              
              <h4>Top 20 &quot;${regionConcepts[0].rankName}&quot; names ordered by Occurrence Count </h4>
                <ol>
                  <c:forEach items="${regionConcepts}" var="regionConcept" varStatus="status" end="19">
                    <li><a href="${pageContext.request.contextPath}/species/browse/region/${geoRegion.id}/taxon/${regionConcept.taxonConceptId}">${regionConcept.taxonConceptName}</a>
                      (<a href="${pageContext.request.contextPath}/species/${regionConcept.taxonConceptId}">overview</a>) - occurrences: ${regionConcept.occurrenceCount}</li>
                  </c:forEach>
                </ol>
            </div>
            <% /*<div class="smalltree">
				<gbif:smallbrowser concepts="${concepts}" selectedConcept="${selectedConcept}" rootUrl="/species/browse/regions/${geoRegion.id}" markConceptBelowThreshold="${dataProvider.key==nubProvider.key}" highestRank="kingdom" messageSource="${messageSource}"/>
			</div><!--end smalltree-->
             */ %>
		</c:when>
		<c:otherwise>		
			<spring:message code="taxonomy.browser.notree"/> <gbif:capitalize>${geoRegion.name}</gbif:capitalize>
		</c:otherwise>	
	</c:choose>	
</div>	
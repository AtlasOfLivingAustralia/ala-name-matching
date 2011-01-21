<%@ include file="/common/taglibs.jsp"%>
<c:set var="georeferenced" value="${occurrenceRecord.latitude !=null && occurrenceRecord.latitude!=0}"/>
<div id="furtherActions">
	<h4><spring:message code="occurrence.search.filter.whattodo.title"/></h4>
	<table cellspacing="1" class="actionsList">
		<tbody>
			<tr valign="top">
				<td><b><spring:message code="actions.find"/></b></td>
				<td>	
					<ul class="actionsListInline">
						<c:set var="a0">${occurrenceRecord.dataResourceName}</c:set>
						<c:set var="a1"><span class="genera">${occurrenceRecord.taxonName}</span></c:set>
						<li><a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject='0' predicate='0' value='${occurrenceRecord.taxonName}' index='0'/>&<gbif:criterion subject='24' predicate='0' value='${taxonConcept.dataResourceKey}' index='1'/>"><spring:message code="occurrence.record.dataprovider.findmore" arguments="${a0},${a1}"/></a></li>
						<c:if test="${georeferenced}">
								<c:set var="a0"><span class="genera">${occurrenceRecord.taxonName}</span></c:set>
								<c:set var="a1">${minX}&deg;${minX>0?'E':'W'}, ${minY}&deg;${minY>0?'N':'S'}, ${maxX}&deg;${maxX>0?'E':'W'}, ${maxY}&deg;${maxY>0?'N':'S'}</c:set>
						<li><a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject='0' predicate='0' value='${occurrenceRecord.taxonName}' index="0"/>&<gbif:criterion subject='19' predicate='0' value='${minX},${minY},${maxX},${maxY}' index="1"/>"><spring:message code="occurrence.record.geospatial.findall.onedeg" arguments="${a0}%%%${a1}" argumentSeparator="%%%"/></a></li>
						  <li><a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject='1' predicate='0' value='${occurrenceRecord.latitude}' index="0"/>&<gbif:criterion subject='2' predicate='0' value='${occurrenceRecord.longitude}' index="1"/>"><spring:message code="occurrence.record.geospatial.findall.latlong"/>${occurrenceRecord.latitude}, ${occurrenceRecord.longitude}</a></li>
						</c:if>
					</ul>
				</td>
			</tr>
			<tr valign="top">
				<td><b><spring:message code="actions.retrieve"/></b></td>
				<td>	
					<ul class="actionsListInline">
						<c:set var="dataProvider">${occurrenceRecord.dataResourceName}</c:set>
						<li><a href="${pageContext.request.contextPath}/occurrences/${occurrenceRecord.key}/providerMessage.xml"><spring:message code="occurrence.record.dynamic.details"/></a></li>
						<c:if test="${cachedRecordExists}">
							<li><a href="${pageContext.request.contextPath}/occurrences/${occurrenceRecord.key}/cachedRecord.xml"><spring:message code="occurrence.record.cached.details"/></a></li>
						</c:if>
						<c:if test="${georeferenced}">
							<li><a href="${pageContext.request.contextPath}/occurrences/occurrence-${occurrenceRecord.key}.kml"><spring:message code="occurrence.record.geospatial.google.earth"/></a></li>
						</c:if>
					</ul>
				</td>
			</tr>
			<tr valign="top">
				<td><b><spring:message code="actions.view"/></b></td>
				<td>	
					<ul class="actionsListInline">
						<c:set var="a0">
							<span class="genera">${occurrenceRecord.taxonName}</span>
						</c:set>
						<c:set var="a1">
							${occurrenceRecord.dataResourceName}
						</c:set>
						<li><a href="${pageContext.request.contextPath}/species/${occurrenceRecord.taxonConceptKey}"><spring:message code="occurrence.record.taxonomy.viewconcept" arguments="${a0}"/></a></li>
						<li><a href="${pageContext.request.contextPath}/species/browse/taxon/${occurrenceRecord.taxonConceptKey}"><spring:message code="occurrence.record.taxatreelink" arguments="${a0},${a1}"/></a></li>
						
						<c:if test="${not empty institution && not empty institution.name && not empty institution.lsid}">
						<li><a href="http://biocol.org/${institution.lsid}">View the BCI page for ${institution.name}</a></li>
						</c:if>
						
					</ul>
				</td>
			</tr>
			<!--tr>
				<td><b><spring:message code="actions.send"/></b></td>
				<td>	
					<ul class="actionsListInline">
						<li>
							<a class="feedback" href='javascript:feedback("${pageContext.request.contextPath}/feedback/occurrence/${occurrenceRecord.key}")'><spring:message code="feedback.to.provider.link"  arguments="${occurrenceRecord.dataProviderName}" argumentSeparator="|"/></a>
						</li>
					</ul>
				</td>
			</tr-->
		</tbody>
	</table>
	
    
    <h4 id="annotationsHelp" style="border-top:1px solid #FFFFFF; margin-top:5px; padding-top:10px;">
        <div style="float:right;background-color:white;padding-top:6px;"><img src="${pageContext.request.contextPath}/images/uq_logo.gif"/></div>
        Annotate this record
    </h4>
    <p>
        This version of the ALA GIS Portal contains a demonstration of Annotation functionality. This is a collaboration
        between the <a href="http://www.ala.org.au/">ALA</a> and <a href="http://www.uq.edu.au/">The University of Queensland</a>.
    </p>
    <p>At this stage this functionality should be considered for demonstration purposes
       and so we are unable to guarantee your annotations will be maintained in the long term.
       Feel free to annotate occurrence records with suggested issues and comments.</p>
    <p>
        To annotate a record, click on one of the "add annotation" icons (<a class="annotationIcon">&nbsp;</a>), fill out
        the form with your correction/interpretation/comment and click "Submit".
    </p>
    <p>
        To view an existing annotation record, click on one of the "view annotation" icons (<a class="annotatedIcon">&nbsp;</a>)
        and a summary of that annotation should appear under the record field in a blue box.
    </p>
    <p>
        <a href="#show" name="showAnnotations">Show</a> / <a href="#hide" name="hideAnnotations">Hide</a> all annotations.
    </p>
    <p>
      <a class="rssLink" href="http://twitter.com/statuses/user_timeline/55146858.rss" name="rssLink">RSS Feed for all annotations</a>
    </p>
    <p>
      <a class="rssLink" href="http://search.twitter.com/search.rss?q=ala-dataset-${occurrenceRecord.dataResourceKey}" name="rssLink">RSS Feed for annotations this dataset</a>
    </p>
</div>
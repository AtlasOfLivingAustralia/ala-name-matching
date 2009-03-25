<%@ include file="/common/taglibs.jsp"%>
<div id="furtherActions">
	<!--<h4><spring:message code='actions.for'/> ${dataResource.name}</h4>-->
	<table cellspacing="1" class="actionsList">
        <thead>
            <tr valign="top">
				<th width="30%"><b><spring:message code="actions.explore"/></b></th>
                <!--<th><b><spring:message code="actions.list"/></b></th>-->
                <th width="40%"><b><spring:message code="actions.download"/></b></th>
                <!--th width="40%"><b><spring:message code="actions.send"/></b></th-->
            </tr>
        </thead>
        <tbody>
            <tr valign="top">
                <td>
                    <ul class="actionsListIcon">
						<c:if test="${dataResource.occurrenceCount>0}">
							<li>
								<a href="${pageContext.request.contextPath}/occurrences/searchWithTable.htm?<gbif:criterion subject="24" predicate="0" value="${dataResource.key}" index="0"/>" class="iconTable"><spring:message code="explore.occurrences"/></a>
							</li>
						</c:if>
						<li>
							<gbif:taxatreeLink dataResource="${dataResource}" dataProvider="${dataProvider}" cssClass="iconTable">
								<spring:message code="dataset.taxonomytreelink"/>
							</gbif:taxatreeLink>
						</li>
                        <c:if test="${not empty chartData}">
                            <li>
								<a href="#charts" class="iconChart">Data Breakdown Charts</a>
							</li>
                        </c:if>
					</ul>
                </td>
                <!--<td>
                    <ul class="actionsListInline">
                        <li>
                            <c:set var="a0">${dataResource.name}</c:set>
                            <a href="${pageContext.request.contextPath}/occurrences/searchCountries.htm?<gbif:criterion subject="24" predicate="0" value="${dataResource.key}" index="0"/>"><spring:message code="dataset.list.countries" arguments="${a0}"/></a>
                        </li>
                    </ul>
                </td>-->
                <td>
                    <c:if test="${dataResource.occurrenceCount>0}">
					<ul class="actionsListIcon">
						<!--<li>
							<a href="${pageContext.request.contextPath}/ws/rest/occurrence/list/?dataResourceKey=${dataResource.key}&format=darwin"><spring:message code="download.darwin.core"/></a>
						</li>-->
						<li>
							<a href="${pageContext.request.contextPath}/occurrences/resource/celldensity/resource-celldensity-${dataResource.key}.kml" class="iconEarth"><spring:message code="download.google.earth.celldensity"/></a>
						</li>
						<!--<li>
							<a href="${pageContext.request.contextPath}/occurrences/resource/placemarks/resource-placemarks-${dataResource.key}.kml"><spring:message code="download.google.earth.placemarks"/></a>
						</li>-->
					</ul>
					</c:if>
					<ul class="actionsListIcon">
						<c:if test="${dataResource.occurrenceCount>0}">
						<li>
							<a href="${pageContext.request.contextPath}/occurrences/downloadResults.htm?format=species&criteria=<gbif:criterion subject="24" predicate="0" value="${dataResource.key}" index="0" urlEncode="true"/>" class="iconDownload"><spring:message code="dataset.list.species" arguments="${a0}"/></a>
						</li>
						</c:if>
						<li>
							<a href="${pageContext.request.contextPath}/species/downloadSpreadsheet.htm?<gbif:criterion subject="1" predicate="0" value="${dataResource.key}" index="0"/>" class="iconDownload"><spring:message code="dataset.download.taxonomy" text="Taxonomy as spreadsheet"/></a>
						</li>
					</ul>
                </td>
                <!--<td>
                    <ul class="actionsListIcon">
						<li>
							<a class="feedback" href='javascript:feedback("${pageContext.request.contextPath}/feedback/resource/${dataResource.key}")'><spring:message code="feedback.to.provider.link"  arguments="${dataProvider.name}" argumentSeparator="|"/></a>
						</li>
					</ul>
                </td>-->
            </tr>
        </tbody>
    </table>
</div>

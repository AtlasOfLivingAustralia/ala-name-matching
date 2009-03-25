<%@ include file="/common/taglibs.jsp"%>
<div id="furtherActions">
    <table cellspacing="1" class="actionsList" style="width:600px">
        <thead>
            <tr valign="top">
                <th><b><spring:message code="actions.explore"/></b></th>
                <c:if test="${taxonConcept.rank!='kingdom' && taxonConcept.rank!='phylum' && taxonConcept.rank!='class' && taxonConcept.rank!='order'}">
                <th><b><spring:message code="actions.list"/></b></th>
                </c:if>
                <th><b><spring:message code="actions.download"/></b></th>
            </tr>
        </thead>
        <tbody>
            <tr valign="top">
                <td>
                    <ul class="actionsListIcon">
                        <gbif:isMajorRank concept="${taxonConcept}">
                            <li>
                                <a href="${pageContext.request.contextPath}/occurrences/searchWithTable.htm?<gbif:criteria criteria="${occurrenceCriteria}"/>" class="iconTable"><spring:message code="explore.occurrences"/></a>
                            </li>
                        </gbif:isMajorRank>
                            <li>
                                <a href="${pageContext.request.contextPath}/species/browse/taxon/${taxonConcept.key}" class="iconClassification"><spring:message code="taxonconcept.drilldown.explore.names"/></a>
                            </li>
                    </ul>
                </td>
                <c:if test="${taxonConcept.rank!='kingdom' && taxonConcept.rank!='phylum' && taxonConcept.rank!='class' && taxonConcept.rank!='order'}">
                <td>
                    <ul class="actionsListIcon">
                        <li>
                            <a href="${pageContext.request.contextPath}/occurrences/searchResources.htm?<gbif:criteria criteria='${occurrenceCriteria}'/>" class="iconTable"><spring:message code="occurrence.record.taxonomy.list.dataproviders"/></a>
                        </li>   
                    </ul>
                </td>
                </c:if>
                <td>
                    <ul class="actionsListIcon">
                        <li>
                            <a href="${pageContext.request.contextPath}/occurrences/taxon/celldensity/taxon-celldensity-${taxonConcept.key}.kml" class="iconEarth"><spring:message code="download.google.earth.celldensity"/></a>
                        </li>
                    </ul>
                </td>
            </tr>
        </tbody>
    </table>
</div>
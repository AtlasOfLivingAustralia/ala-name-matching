<%@ include file="/common/taglibs.jsp"%>
<div id="furtherActions">
    <h4><spring:message code='actions.for'/> <gbif:taxonPrint concept="${taxonConcept}"/></h4>
    <table cellspacing="1" class="actionsList">
        <tbody>
            <tr valign="top">
                <td><b><spring:message code="actions.explore"/></b></td>
                <td>    
                    <ul class="actionsListInline">
                        <gbif:isMajorRank concept="${taxonConcept}">
                        <li>
                            <a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criteria criteria="${occurrenceCriteria}"/>"><spring:message code="explore.occurrences"/></a>
                        </li>   
                        </gbif:isMajorRank>             
                        <li>
                            <a href="${pageContext.request.contextPath}/species/browse/taxon/${taxonConcept.key}"><spring:message code="taxonconcept.drilldown.explore.names"/></a>
                        </li>
                    </ul>
                </td>
            </tr>
            <gbif:isMajorRank concept="${taxonConcept}">            
            <c:if test="${taxonConcept.rank!='kingdom' && taxonConcept.rank!='phylum' && taxonConcept.rank!='class' && taxonConcept.rank!='order'}">
                <tr valign="top">
                    <td><b><spring:message code="actions.list"/></b></td>
                    <td>
                        <ul class="actionsListInline">
                            <li>
                                <a href="${pageContext.request.contextPath}/occurrences/searchResources.htm?<gbif:criteria criteria='${occurrenceCriteria}'/>"><spring:message code="occurrence.record.taxonomy.list.dataproviders"/></a>
                            </li>   
                        </ul>
                    </td>
                </tr>
            </c:if>
            <tr valign="top">
                <td><b><spring:message code="actions.download"/></b></td>
                <td>    
                    <ul class="actionsListInline">
                        <li>
                            <a href="${pageContext.request.contextPath}/occurrences/taxon/celldensity/taxon-celldensity-${taxonConcept.key}.kml"><spring:message code="download.google.earth.celldensity"/></a>
                        </li>   
                    </ul>
                </td>
            </tr>
            </gbif:isMajorRank>         
        </tbody>
    </table>
</div>
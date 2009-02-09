<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">	
<h2><spring:message code="dataset.list.main.title"/></h2>
	<gbif:alphabetLink rootUrl="/datasets/browse/" selected="${selectedChar}" listClass="flatlist" letters="${alphabet}"/>
</div>

<h2 id="selectedChar">${selectedChar}</h2>
	<c:choose>
	<c:when test="${fn:length(alphabet)==0}">Currently no data resources/providers within the system.</c:when>
	<c:otherwise>
<display:table name="resourceNetworks" export="false" class="statistics" id="resourceNetwork">
  <display:column titleKey="dataset.networks.list.title" class="name">
  	<a href="${pageContext.request.contextPath}/datasets/network/${resourceNetwork.key}"><gbiftag:resourceNetworkPrint resourceNetwork="${resourceNetwork}"/></a>
  </display:column>	  
  <display:column titleKey="dataset.list.occurrence.count" class="singlecount">
  	<fmt:formatNumber value="${resourceNetwork.occurrenceCount}" pattern="###,###"/><c:if test="${resourceNetwork.occurrenceCount==null}">0</c:if>
  	(<fmt:formatNumber value="${resourceNetwork.occurrenceCoordinateCount}" pattern="###,###"/><c:if test="${resourceNetwork.occurrenceCoordinateCount==null}">0</c:if>)
	</display:column>	  
  <display:setProperty name="basic.msg.empty_list"> </display:setProperty>	  
  <display:setProperty name="basic.empty.showtable">false</display:setProperty>	  
</display:table>

<display:table name="dataProviders" export="false" class="statistics" id="dataProvider">
  <display:column titleKey="dataset.providers.list.title" class="name">
  	<a href="${pageContext.request.contextPath}/datasets/provider/${dataProvider.key}">${dataProvider.name}</a>
  	<c:if test='${dataProvider.isoCountryCode!=null}'>
  	<p class="resultsDetails">
			Hosted in <a href="${pageContext.request.contextPath}/countries/${dataProvider.isoCountryCode}"><spring:message code="country.${dataProvider.isoCountryCode}" text=""/></a>,
			providing ${dataProvider.dataResourceCount} data resources.
		</p>  				
		</c:if>
  </display:column>	  
  <display:column titleKey="dataset.list.occurrence.count" class="singlecount">
    <c:choose>
      <c:when test="${dataProvider.occurrenceCount>0}">
      	<a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="25" predicate="0" value="${dataProvider.key}" index="0"/>"><fmt:formatNumber value="${dataProvider.occurrenceCount}" pattern="###,###"/></a>
      	<c:choose>
      	  <c:when test="${dataProvider.occurrenceCoordinateCount>0}">(<a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="25" predicate="0" value="${dataProvider.key}" index="0"/>&<gbif:criterion subject="28" predicate="0" value="0" index="1"/>"><fmt:formatNumber value="${dataProvider.occurrenceCoordinateCount}" pattern="###,###"/></a>)</c:when>
      	  <c:otherwise>(0)</c:otherwise>
      	</c:choose>
      </c:when>
	  <c:otherwise>
	    <p class="notApplicable">
		  	<c:choose>
		  	  <c:when test="${dataProvider.conceptCount>0}">
		  	  	<spring:message code="dataset.not.applicable"/>
		  	  </c:when>
		  	  <c:otherwise>
		  	  	<spring:message code="dataset.not.yet.indexed"/>
		  	  </c:otherwise>
		  	</c:choose>
		 </p>
	  </c:otherwise>
	</c:choose>
	</display:column>	  
  <display:setProperty name="basic.msg.empty_list"> </display:setProperty>	  
  <display:setProperty name="basic.empty.showtable">false</display:setProperty>	  
</display:table>

<display:table name="dataResources" export="false" class="statistics" id="dataResource">
  <display:column sortProperty="dataResource.name" titleKey="dataset.resources.list.title" class="name">
  	<a href="${pageContext.request.contextPath}/datasets/resource/${dataResource.key}">${dataResource.name}</a>
  	<p class="resultsDetails"><a href="${pageContext.request.contextPath}/datasets/provider/${dataResource.dataProviderKey}">${dataResource.dataProviderName}</a></p>
  </display:column>
  <display:column titleKey="dataset.list.occurrence.count" class="bigcount">
    <c:choose>
      <c:when test="${dataResource.occurrenceCount>0}">
  	    <a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="24" predicate="0" value="${dataResource.key}" index="0"/>"><fmt:formatNumber value="${dataResource.occurrenceCount}" pattern="###,###"/></a>
      	<c:choose>
      	  <c:when test="${dataResource.occurrenceCoordinateCount>0}">(<a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="24" predicate="0" value="${dataResource.key}" index="0"/>&<gbif:criterion subject="28" predicate="0" value="0" index="1"/>"><fmt:formatNumber value="${dataResource.occurrenceCoordinateCount}" pattern="###,###"/></a>)</c:when>
      	  <c:otherwise>(0)</c:otherwise>
      	</c:choose>
		  </c:when>
		  <c:otherwise>
		    <p class="notApplicable">
			  	<c:choose>
			  	  <c:when test="${dataResource.conceptCount>0}">
			  	  	<spring:message code="dataset.not.applicable"/>
			  	  </c:when>
			  	  <c:otherwise>
			  	  	<spring:message code="dataset.not.yet.indexed"/>
			  	  </c:otherwise>
			  	</c:choose>
			 </p>
		  </c:otherwise>
		</c:choose>
  </display:column>   
  <display:column titleKey="dataset.list.taxonconcept.count" class="count">
     <c:if test="${dataResource.conceptCount>0}">
  	 	<fmt:formatNumber value="${dataResource.conceptCount}" pattern="###,###"/>
  	 </c:if> 
  </display:column>
  <display:column titleKey="dataset.speciesCount" class="count">
     <c:if test="${dataResource.speciesCount>0}">
		 <fmt:formatNumber value="${dataResource.speciesCount}" pattern="###,###"/> 
  	 </c:if> 
  </display:column>
  <display:setProperty name="basic.msg.empty_list"> </display:setProperty>	  
  <display:setProperty name="basic.empty.showtable">false</display:setProperty>	  
</display:table>

	</c:otherwise>
</c:choose>
<%@ include file="/common/taglibs.jsp"%>
<h4><spring:message code="dataset.information"/></h4>
<fieldset>
<c:if test="${not empty dataProvider.name}"><p><label><spring:message code="name"/>:</label>${dataProvider.name}</p></c:if>
<c:if test="${not empty dataProvider.websiteUrl}"><p><label><spring:message code="website"/>:</label><a href="${dataProvider.websiteUrl}">${dataProvider.websiteUrl}</a></p></c:if> 
<c:if test="${not empty dataProvider.description}"><p><label><spring:message code="description"/>:</label>${dataProvider.description}</p></c:if>
<c:if test="${not empty dataProvider.address}"><p><label><spring:message code="address"/>:</label>${dataProvider.address}</p></c:if>    
<c:if test="${not empty dataProvider.isoCountryCode}"><p><label><spring:message code="country" text=""/>:</label><spring:message code="country.${dataProvider.isoCountryCode}" text=""/></p></c:if>
<c:if test="${not empty dataProvider.email}"><p><label><spring:message code="email"/>:</label><gbiftag:emailPrint email="${dataProvider.email}"/></p></c:if>    
<c:if test="${not empty dataProvider.telephone}"><p><label><spring:message code="telephone"/>:</label>${dataProvider.telephone}</p></c:if>  
<c:if test="${not empty dataProvider.created}"><p><label><spring:message code="date.added"/>:</label><fmt:formatDate value="${dataProvider.created}"/></p></c:if>   
<c:if test="${not empty dataProvider.modified}"><p><label><spring:message code="last.modified"/>:</label><fmt:formatDate value="${dataProvider.modified}"/></p></c:if>  
</fieldset>
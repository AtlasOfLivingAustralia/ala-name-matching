<%-- 
    Document   : sighting
    Created on : Aug 6, 2010, 5:19:21 PM
    Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/common/taglibs.jsp" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<c:set var="googleKey" scope="request"><ala:propertyLoader bundle="biocache" property="googleKey"/></c:set>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta charset="UTF-8" >
        <title>Share a sighting - Thank you | Atlas of Living Australia</title>
    </head>
    <body>
        <div id="header">
            <div id="breadcrumb">
                <a href="${initParam.centralServer}">Home</a>
                <a href="${initParam.centralServer}/share">Share</a>
                Share a sighting
            </div>
            <h1>Share a sighting - Completed</h1>
        </div>
        <c:choose>
            <%-- User is logged in --%>
            <c:when test="${!empty pageContext.request.remoteUser}">
                <c:if test="${not empty taxonConcept && empty error}">
                    <div id="column-one" class="section">
                          <h2>Your sighting has been successfully submitted</h2>
                          <p></p>
                    </div>
                </c:if>
                <c:if test="${not empty error}">
                    <div class="section">${error}</div>
                </c:if>
            </c:when>
            <%-- User is NOT logged in --%>
            <c:otherwise>
                <jsp:include page="loginMsg.jsp"/>
            </c:otherwise>
        </c:choose>
    </body>
</html>
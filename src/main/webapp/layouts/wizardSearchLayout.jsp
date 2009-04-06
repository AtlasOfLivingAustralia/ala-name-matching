<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8" %>
<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*" %>
<%@ page import="javax.servlet.jsp.*" %>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.gbif.portal.web.filter.*" %>
<%@ page import="org.apache.taglibs.string.util.*" %>
<c:set var="viewName" scope="request"><tiles:getAsString name="viewName"/></c:set>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8">		
		<title>
				<c:set var="title"><tiles:getAsString name="title"/></c:set>
				<c:set var="searchTitle"><tiles:getAsString name="searchTitle"/></c:set>
				<spring:message code="${searchTitle}" text=""/>
				<tiles:insert name="subtitle"/>
				
				<c:set var="title" scope="request">
						<c:set var="searchTitle"><tiles:getAsString name="searchTitle"/></c:set>
						<spring:message code="${searchTitle}" text=""/>
						<tiles:insert name="subtitle" flush="false"/>
				</c:set>				
					- 				
				<spring:message code="${title}" text=""/>
		</title>
		<tiles:insert name="headcontent"/>
		<link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" type="image/x-icon" />
	</head>
	<body>
	  <div id="skipNav">
			<ul title="Accessibility options">
	  		<li><a href="#mainContent" accesskey="C">Skip to Content</a></li>
	  		<li><a href="#sidebar" accesskey="S">Skip to Sidebar</a></li>
			</ul> 
	  </div><!-- End skipNav -->
		<div id="cocoon">
			<div id="container">	
				<tiles:insert name="header"/>
				<tiles:insert name="topmenu"/>
				<div id="content">
					<tiles:insert page="/WEB-INF/jsp/filters/filterArrays.jsp"/>
					<div id="twopartheader">					
						<h2><spring:message code="${searchTitle}"/></h2>
						<h3>
							<c:set var="searchHelp"><tiles:getAsString name="searchHelp"/></c:set>
							<c:set var="searchButton"><spring:message code="search"/></c:set>
							<spring:message code="${searchHelp}" arguments="${searchButton}" argumentSeparator="$$$"/>						
						</h3>
					</div><!-- End twopartheader -->
					
					<div id="filtersContainer">
					
					<%//For the 2 columns %>
					<%
							List<FilterDTO> filters = (List<FilterDTO>) request.getAttribute("filters");
							CriteriaDTO criteriaDTO = (CriteriaDTO) request.getAttribute("criteria");		
							List<CriterionDTO> criteria = criteriaDTO.getCriteria();
							pageContext.setAttribute("criteria", criteria);		 
					%>					
					<table id="filtersAndEditor">
						<tr>
							<td id="filterEditorPane">	
								<tiles:insert name="filterEditor"/>
							</td>						
							<td id="constructedFiltersPane">
								<c:set var="filterAction" scope="request"><tiles:getAsString name="filterAction"/></c:set>
								<div id="constructedFilters">
									<c:choose>
										<c:when test="${fn:length(criteria)==0}">									
											<tiles:insert name="filterHelp"/>
										</c:when>
										<c:otherwise>									
											<tiles:insert name="constructedFilter"/>											
										</c:otherwise>
									</c:choose>					
									<tiles:insert name="warnings"/>				
								</div><!-- End constructedFilters -->					
							</td>
						</tr>
					</table><!-- filtersAndEditor -->						
					<c:if test="${fn:length(criteria)!=0}">
						<div id="actionsAndResults">
							<tiles:insert name="actions"/>
							<tiles:insert name="results"/>														
						</div><!-- End actionsAndResults -->
					</c:if>
					</div><!-- End filtersContainer -->
					<tiles:insert name="breadcrumbs"/>				
				</div><!--End content -->
				<tiles:insert name="footer"/>
			</div><!-- End container-->
		</div><!-- End cocoon-->		
	</body>
</html>
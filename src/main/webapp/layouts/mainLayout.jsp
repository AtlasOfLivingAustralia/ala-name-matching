<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8" %><%@ include file="/common/taglibs.jsp"%>
<c:if test="${param['noHeaders']!=1}"><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
	<!-- ALA Portal Version: <gbif:propertyLoader bundle="portal" property="version"/> -->	
	<head>
    	<tiles:insert name="headcontent" flush="false"/>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8">	
        <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" type="image/x-icon" /> 
		<c:if test="${not empty points}">
			<meta name="geo.position" content="<c:forEach items="${points}" var="point">${point.latitude};${point.longitude}</c:forEach>">
		</c:if> 		
		
		<tiles:insert name="keywords" flush="false"/>
		<title>
			<tiles:insert name="subtitle" flush="false"/>
			-
			<c:set var="title" scope="page"><tiles:getAsString name="title"/></c:set>
			<spring:message code="${title}"/> 
		</title>
		<c:set var="title" scope="request"><tiles:insert name="subtitle" flush="false"/></c:set>
	</head>
	<body>
	    <div id="cocoon">
			<div id="container">	
				<tiles:insert name="header" flush="false"/>
				<tiles:insert name="topmenu" flush="false"/>
				<div id="content">
                <c:if test="${param['debug']==1}"></c:if>
</c:if>
      				
					<tiles:insert name="content" flush="false"/>
<c:if test="${param['noHeaders']!=1}">					
					<tiles:insert name="breadcrumbs" flush="false"/>
				</div><!--End content -->				
				<tiles:insert name="footer" flush="false"/>
			</div><!-- End container-->
		</div><!-- End cocoon-->		
	</body>
</html>
</c:if>
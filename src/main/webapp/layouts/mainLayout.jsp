<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8" %><%@ include file="/common/taglibs.jsp"%>
<c:if test="${param['noHeaders']!=1}"><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
	<!-- ALA Portal Version: <gbif:propertyLoader bundle="portal" property="version"/> -->	
	<head>
    	<tiles:insert name="headcontent" flush="true"/>	
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8">	
        <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" type="image/x-icon" /> 
		<c:if test="${not empty points}">
			<meta name="geo.position" content="<c:forEach items="${points}" var="point">${point.latitude};${point.longitude}</c:forEach>">
		</c:if> 		
		
		<tiles:insert name="keywords"/>
		<title>
			<tiles:insert name="subtitle"/>
				- 			
			<c:set var="title" scope="page"><tiles:getAsString name="title"/></c:set>
			<spring:message code="${title}"/> 
		</title>
		<c:set var="title" scope="request"><tiles:insert name="subtitle" flush="false"/></c:set>		
	</head>
	<body>
	    <div id="cocoon">
			<div id="container">	
				<tiles:insert name="header"/>
				<tiles:insert name="topmenu"/>
				<div id="content">
                <c:if test="${param['debug']==1}"><alatag:debugBeans /></c:if>
</c:if>
      				
					<tiles:insert name="content"/>
<c:if test="${param['noHeaders']!=1}">					
					<tiles:insert name="breadcrumbs"/>
				</div><!--End content -->				
				<tiles:insert name="footer"/>
			</div><!-- End container-->
		</div><!-- End cocoon-->		
	</body>
</html>
</c:if>
<%@ include file="/common/taglibs.jsp"%>
<div id="header">
    <h1><a href="welcome.htm"><img src="${pageContext.request.contextPath}/skins/ala/images/ALA-web-banner-2.jpg" /></a></h1>
</div>
<div id="quickSearch">
    <tiles:insert page="blanketSearch.jsp"/>
</div>
<!-- End header-->
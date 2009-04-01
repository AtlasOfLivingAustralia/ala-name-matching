<%@ include file="/common/taglibs.jsp"%>
<div id="announce">
	<h2>Welcome to the Atlas of Living Australia</h2>
    <h5 id="byLine">Sharing biodiversity knowledge to shape our future</h5>

	<p>The Atlas of Living Australia (<a href="http://www.ala.org.au/">ALA</a>) is 
        a government-funded project to develop infrastructure to manage information
        on the biodiversity of the Australian continent and marine areas.</p>
      
	<p>Over the next few years the ALA will provide a set of integrated tools for 
        exploring Australia's biodiversity.</p>

    <p>This site is the first of these tools. It will bring together specimen
        records from Australia's natural history collections and field observations
        of organisms to provide a detailed view of data on the past and present
        distribution of species found in Australia.</p>

	<p>The links along the top of this page allow you to explore these data in 
    different ways:</p>
    
    <ul class="frontPageList">
        <li><a href="${pageContext.request.contextPath}/species/">SPECIES</a> - View all data for a species</li>
        <li><a href="${pageContext.request.contextPath}/regions/">REGIONS</a> - View all data for a region within Australia</li>
        <li><a href="${pageContext.request.contextPath}/datasets/">DATASETS</a> - View all data for a collection or project</li>
        <li><a href="${pageContext.request.contextPath}/occurrences/">OCCURRENCES</a> - Advanced search interface for occurrence records</li>
    </ul>
    
    <p/>

    <p>This web site uses open source software developed by the Global
        Biodiversity Information Facility (<a href="http://www.gbif.org/">GBIF</a>)</p>

    <p>The Atlas of Living Australia is funded under the Australian Government's National Collaborative Research Infrastructure
        Strategy (<a href="http://ncris.innovation.gov.au/">NCRIS</a>)</p>

    <p style="text-align: center"><img src="${pageContext.request.contextPath}/images/gbiflogo.jpg">
        &nbsp;
        <img src="${pageContext.request.contextPath}/images/ncrislogo.jpg"></p>
    <p>&nbsp;</p>
</div><!-- End panes-->
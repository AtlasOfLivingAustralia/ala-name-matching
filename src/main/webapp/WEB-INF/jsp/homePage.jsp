<%@ page contentType="text/html" pageEncoding="UTF-8" %><%@ 
taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" 
%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="pageName" content="home"/>
        <title>BioCache | Atlas of Living Australia</title>
    </head>
    <body>
    	<div class="section">
        <h1> Web Services </h1>

        <h3>Occurrences</h3>
        <ul>
            <li><strong>Occurrence listing:</strong>
                <a href="/ws/occurrences/page">/occurrences/page</a>
                - just shows the first 10 occurrences (for debug only)</li>
            <li><strong>Occurrence view:</strong> /occurrence/{uuid}</li>
            <li><strong>Occurrence for taxon:</strong> /occurrences/taxon/{guid}</li>
            <li><strong>Occurrence for collection:</strong> /occurrences/collections/{uid}</li>
            <li><strong>Occurrence for institution:</strong> /occurrences/institutions/{uid}</li>
            <li><strong>Occurrence for data resource:</strong> /occurrences/data-resources/{uid}</li>
            <li><strong>Occurrence for data provider:</strong> /occurrences/data-providers/{uid}</li>
            <li><strong>Occurrence download:</strong> /occurrences/download - needs request param definition</li>
            <li><strong>Occurrence wms:</strong> /occurrences/wms - requires WMS parameters along with 'q' and 'fq' populated, if available</li>
            <li><strong>Occurrence static:</strong> /occurrences/static - generates an image of AU with points on, filtering on 'q' and 'fq', if available</li>
            <li><strong>Occurrences coordinates:</strong>
                <a href="/ws/occurrences/coordinates">/occurrences/coordinates</a>
                - Displays a list of unique lat,lon that are used by the occurrences
                 </li>
        </ul>

        <h3>Assertions</h3>
        <ul>
            <li><strong>List assertion codes:</strong>
                <a href="/ws/assertions/codes">/assertions/codes</a>
            </li>
            <li><strong>Add an assertion:</strong> /occurrences/{uuid}/assertions/add</li>
            <li><strong>Delete an assertion:</strong> /occurrences/{uuid}/assertions/{assertionUuid}/delete</li>
            <li><strong>List assertions for occurrence:</strong> /occurrences/{uuid}/assertions/</li>
        </ul>

        <h3>Breakdowns</h3>
        <ul>
            <li><strong>Breakdown for collection based on limit:</strong> /breakdown/collections/{id}?max={max} - will return all collections if {id} is omitted</li>
            <li><strong>Breakdown for institution based on limit:</strong> /breakdown/institutions/{id}?max={max} - will return all institutions if {id} is omitted</li>
            <li><strong>Breakdown for collection at a rank:</strong> /breakdown/collections/{id}/rank/{rank} - will return all collections if {id} is omitted</li>
            <li><strong>Breakdown for institution at a rank:</strong> /breakdown/institutions/{id}/rank/{rank} - will return all institutions if {id} is omitted</li>
            <li><strong>Breakdown for collection at the supplied name and rank: </strong> /breakdown/collections/{id}/rank/{rank}/name/{name} - will return all collections if {id} is omitted</li>
            <li><strong>Breakdown for institution at the supplied name and rank: </strong> /breakdown/institutions/{id}/rank/{rank}/name/{name} - will return all institutions if {id} is omitted</li>
        </ul>

        <h2>Free text search of occurrence records (will return JSON)</h2>
		<div id="inpage_search">
			<form id="search-inpage" action="/ws/occurrences/search" method="get" name="search-form">
			<label for="search">Search</label>
			<input type="text" class="filled ac_input" id="search" name="q" placeholder="Search the Atlas" autocomplete="off">
			<span class="search-button-wrapper"><input type="submit" class="search-button" alt="Search" value="Search"></span>
			</form>
		</div>
    </body>
</html>
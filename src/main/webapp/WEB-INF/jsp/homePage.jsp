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
            <li><strong>Occurrence view:</strong> /occurrence/{uuid}</li>
            <li><strong>Occurrence listing:</strong> /occurrence/page - just shows the first 10 occurrences (for debug only)</li>
            <li><strong>Occurrence for taxon:</strong> /occurrences/taxon/{guid}</li>
            <li><strong>Occurrence for collection:</strong> /occurrences/collection/{uid}</li>
            <li><strong>Occurrence for institution:</strong> /occurrences/institution/{uid}</li>
            <li><strong>Occurrence for data resource:</strong> /occurrences/data-resource/{uid}</li>
            <li><strong>Occurrence for data provider:</strong> /occurrences/data-provider/{uid}</li>
            <li><strong>Occurrence download:</strong> /occurrences/download - needs request param definition</li>
            <li><strong>Occurrence wms:</strong> /occurrences/wms - requires WMS parameters along with 'q' and 'fq' populated, if available</li>
            <li><strong>Occurrence static:</strong> /occurrences/static - generates an image of AU with points on, filtering on 'q' and 'fq', if available</li>
        </ul>

        <h3>Assertions</h3>
        <ul>
            <li><strong>List assertion codes:</strong> /occurrences/{uuid}/assertions/codes</li>
            <li><strong>Add an assertion:</strong> /occurrences/{uuid}/assertions/add</li>
            <li><strong>Delete an assertion:</strong> /occurrences/{uuid}/assertions/{assertionUuid}/delete</li>
            <li><strong>List assertions for occurrence:</strong> /occurrences/{uuid}/assertions/</li>
        </ul>

        <h2>Free text search of occurrence records (will return JSON)</h2>
		<div id="inpage_search">
			<form id="search-inpage" action="occurrences/search" method="get" name="search-form">
			<label for="search">Search</label>
			<input type="text" class="filled ac_input" id="search" name="q" placeholder="Search the Atlas" autocomplete="off">
			<span class="search-button-wrapper"><input type="submit" class="search-button" alt="Search" value="Search"></span>
			</form>
		</div>
    </body>
</html>
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
            <li>/occurrence/{uuid}</li>
            <li>/occurrence/page - just shows the first 10 occurrences (for debug only)</li>
            <li>/occurrences/taxon/{guid}</li>
            <li>/occurrences/collection/{uid}</li>
            <li>/occurrences/institution/{uid}</li>
            <li>/occurrences/data-resource/{uid}</li>
            <li>/occurrences/data-provider/{uid}</li>
            <li>/occurrences/download</li>
        </ul>

        <h3>Assertions</h3>
        <ul>
            <li>/occurrence/{uuid}/assertions/codes</li>
            <li>/occurrence/{uuid}/assertions/add</li>
            <li>/occurrence/{uuid}/assertions/{assertionUuid}/delete</li>
            <li>/occurrence/{uuid}/assertions/</li>
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
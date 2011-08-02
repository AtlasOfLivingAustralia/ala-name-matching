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
            <li><strong>Occurrence download:</strong> /occurrences/download - needs request param definition<br>
            The download will include all records that satisfy the q and fq parameters.  The number of records 
            for a data resource may be restricted based on a collectory configured download limit.  Params:
            <ul>
            	<li>q - the initial query</li>
            	<li>fq - filters to be applied to the original query</li>
            	<li>email - the email address of the user requesting the download</li>
            	<li>reason - the reason for the download</li>
            	<li>file - the name to use for the file</li>
            	<li>fields - a CSV list of fields to include in the download (contains a list of default)</li>
            	<li>extra - a CSV list of fields in include in addition to the "fields"</li>
            </ul>
             </li>
            <li><strong>Occurrence wms:</strong> /occurrences/wms - requires WMS parameters along with 'q' and 'fq' populated, if available</li>
            <li><strong>Occurrence static:</strong> /occurrences/static - generates an image of AU with points on, filtering on 'q' and 'fq', if available</li>
            <li><strong>Occurrences coordinates:</strong>
                <a href="/ws/occurrences/coordinates">/occurrences/coordinates</a>
                - Displays a list of unique lat,lon that are used by the occurrences
                 </li>
            <li><strong>List of default facets for occurrence search:</strong> <a href="/ws/search/facets">/search/facets</a></li>
            <li><strong>Facet based download:</strong> /occurrences/facets/download - requires a 'q' and optional 'fq' and one 'facet'. This can be used to download distinct lists of species:
            <ul>
            	<li><a href="/ws/occurrences/facets/download?q=collection_uid:co150&facets=species_guid">/search/facets/download?q=collection_uid:co150&facets=species_guid</a> - downloads a list of species guids and associated scientific names for collection co150</li>
            	<li><a href="/ws/occurrences/facets/download?q=collection_uid:co150&facets=raw_taxon_name">/search/facets/download?q=collection_uid:co150&facets=raw_taxon_name</a> - downloads a list of raw scientific names for collection co150</li>
            </ul>
            </li>
            <li><strong>Spatial Occurrence search: </strong>/occurrences/spatial - supports point-radius and wkt based searches.  To search by wkt the wkt string can be supplied directly or via a gazetteer URL. Examples:
            	<ul>
            		<li><a href="/ws/occurrences/spatial?lat=-35.27&lon=149.15&radius=10">/occurrences/spatial?lat=-35.27&lon=149.15&radius=10</a></li>
            		<li><a href="/ws/occurrences/spatial?wkt=POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))">/occurrences/spatial?wkt=POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))</a></li>
            		<li><a href="/ws/occurrences/spatial?url=http://spatial.ala.org.au/gazetteer/lga/Acton_(Australian_Capital_Territory).xml">/occurrences/spatial?url=http://spatial.ala.org.au/gazetteer/lga/Acton_(Australian_Capital_Territory).xml</a></li>
            	</ul> 
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
        This section outlines the breakdown services that are available.  These services are available for each of the different collectory types:
        <ul>
        	<li>collections</li>
        	<li>institutions</li>
        	<li>dataResources</li>
        	<li>dataProviders</li>
        	<li>dataHubs</li>
        </ul>
        In the services specified below {collectorytype} must be one of the above values.  
        <ul>
            <li><strong>Breakdown based on limit:</strong> /breakdown/{collectorytype}/{uid}?max={max} Example:<br>
            <a href="/ws/breakdown/collections/co50?max=50">/breakdown/collections/co50?max=50</a> 
            </li>           
            <li><strong>Breakdown of a rank:</strong> /breakdown/{collectorytype}/{uid}?rank={rank} Example:<br>
            <a href="/ws/breakdown/dataResources/dr375?rank=class">/breakdown/dataResources/dr375?rank=class</a>
            </li>            
            <li><strong>Breakdown at the supplied name and rank: </strong> /breakdown/{collectorytype}/{uid}?rank={rank}&name={name} Example:<br>
            <a href="/ws/breakdown/dataHubs/dh1?rank=phylum&name=Chordata">/breakdown/dataHubs/dh1?rank=phylum&name=Chordata</a>
            </li>
            
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
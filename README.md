# Atlas Name matching API 

This is the API in use by the Atlas of Living Australia to match scientific name to taxon concepts.
This API borrows heavily from the name parsing great work done by [GBIF](https://github.com/gbif) 
in their [scientific name parser library](https://github.com/gbif/name-parser)
This code contains additions for handling some Australian specific issues.

## Generating a name match index

This requires a single Darwin Core Archive (DwCA) that contains all the scientific names that you wish to add (including synonyms).

There is an example Catalogue of Life DwcA that can be downloaded here: 

[dwca-col.zip](http://biocache.ala.org.au/archives/dwca-col.zip) 

Users can modify the col_dwc.txt file to include any additional species names.

The name matching index can also support common name. Here are the Catalogue of Life common names that can be loaded in conjunction to the Darwin Core Archive: 

[col_vernacular.txt.zip](http://biocache.ala.org.au/archives/col_vernacular.txt.zip)

The name matching supports homonym detection. Homonym detection is supported through the using of IRMNG. 
You can download the IRMNG DwCA for homonyms from the following URL: 

[IRMNG_DWC_HOMONYMS.zip](http://www.cmar.csiro.au/datacentre/downloads/IRMNG_DWC_HOMONYMS.zip)

The indexing code is [in the class DwcaNameIndexer.java](src/main/java/au/org/ala/checklist/lucene/DwcaNameIndexer.java)

An assembly jar file for this can be downloaded from our maven repository : 

[ala-name-matching-2.0-assembly.jar](http://maven.ala.org.au/repository/au/org/ala/ala-name-matching/2.0/ala-name-matching-2.0-assembly.jar)

To generate the name using the assembly jar:
1) Rename the jar :
mv ala-name-matching-2.0-SNAPSHOT-assembly.jar names.jar

2) Extract the lib directory:
 jar –xf names.jar lib

3) Generate the names index – here is the command that I used.

```
java –cp .:names.jar au.org.ala.checklist.lucene.DwcaNameIndexer --all --dwca /data/bie-staging/names-lists/dwca-col --target /data/lucene/testdwc-namematching --irmng /data/bie-staging/irmng/IRMNG_DWC_HOMONYMS --common /data/bie-staging/ala-names/col_vernacular.txt
```

Please be aware that the names indexing could take over an hour to complete.

## Build notes

This library is built with maven. By default a `mvn install` will try to run a test suite which will fail without a local installation of a name index.
The name index can be downloaded [here](http://biocache.ala.org.au/archives/nameindexes/20140610/namematching_v13.tgz) and needs to be extracted to the
directory `/data/lucene/namematching`

To skip this step, run a build with ```mvn install -DskipTests=true```.

## ALA Names List

The ALA sources most of its names from the National Species List (NSL), 
which is made up of the Australian Faunal Directory (AFD), Australian Plant Census (APC) and the 
Australian Plant Name Index (APNI).  These data sources are not complete.  
In areas where this is most apparent we attempt to pad out known families with missing genera and species. 
This becomes most apparent in the Birds and Fish area.  One major risk associated with this is adding duplicate species
 because AFD is missing synonym relationships.

One source we use to include missing species is the Codes for Australian Aquatic Biota (CAAB) species list.  
We take all the species in CAAB, that have distributions in Australian waters, and add them if they do not exist in AFD.

We use AusFungi to supply all the Fungi and AusMoss to supply all the mosses. 
These lists will eventually become part of the NSL, but until then we merge them using DwCA supplied by AusMoss and AusFungi directly.

We pad out the Birds and Jellyfish branches of AFD with species from Catalog of Life 2012 (CoL).

We also use CoL to supply the complete classification of kingdoms that are missing from the NSL.
At the moment this encompasses Viruses, Chromista, Protozoa and Bacteria.

This names list is used as a backbone for the ALA species pages and to create a name matching index.


### Using ALA Name Matching

The ALA Name Matching is available as a library that can be used in other projects.   It is available in the ALA Maven Repository (http://maven.ala.org.au/).  

To use ala-name-matching, include it as a dependency in your pom file:
`
   <dependency>
      <groupId>au.org.ala</groupId>
      <artifactId>ala-name-matching</artifactId>
      <version>2.0</version>
   </dependency>
`

Download the most recently generated name matching index: 

http://biocache.ala.org.au/archives/namematching13.tgz

Unzip this into a /data/lucene directory
In your program create a single new ALANameSearcher to perform all your searches

`
ALANameSearcher  searcher = new ALANameSearcher ("/data/lucene/namematching")
`

The easiest way to perform a search is to have the searcher handle all the exceptional situations using the default handling:


`
LinnaeanRankClassification cl = new LinnaeanRankClassification()
cl.setScientificName("Macropus rufus")
String lsid = searcher.searchForAcceptedLsidDefaultHandling(cl,true)
NameSearchResult result = searcher.searchForAcceptedRecordDefaultHandling(cl, true)
`

### Understanding the Name Matching Algorithm

When the name matching index is created the scientific name is stored is several formats.  

* Raw scientific name including authorship
* Canonical scientific name, without authorship
* In “taxa match” form that will allow fuzzy matches
* In component to allow for phrase name matching

These formats allow a variety of match types to be performed. 

There are 2 distinct phases in the match process.  
  * Finding the potential match candidate.  Only one set of match candidates are ever considered.  If they fail validation no match is returned.   During the match process all higher level classification is used as optional values in the match.  This allows for conflicting higher classification.  Listed below are the different types of matches that are performed.  They are listed in the order that they are checked and when a match is found no further ones are attempted.
    * Attempt to match the exact scientific name that is supplied.  No preprocessing of the name is performed. 
    * Parse the supplied scientific name. 
      * If it is a “Phrase name” a special search is performed where the "phrase" component is compulsory and the "voucher" is optional. 
      * When it is not a “Phrase name” and the name is a validly parsed scientific name a canonical version of the name is constructed and used in the search.
    * If the search is configured to perform a “fuzzy match” the sounds like expressions are generated and then used in the search. 
  * Validating the match.  Once a match is received the following checks are performed:
    * Check if the results are “excluded”. If this check fails an `ExcludedNameException` is thrown.  There are 2 different situations where this is reported:
      * The top result is excluded.  The `ExcludedNameException` will have a value for excludedName but NOT for nonExcludedName. If you don’t care about the exclusion you can use the result in the excludedName variable as your match.  This is the default behaviour.
      * The top result is NOT excluded BUT the second result for the same name is. The `ExcludedNameException` will have a value for both excludedName and nonExcluded name.  You should use the nonExcludedName as your match.  This is the default behaviour.
    * Check if the result represents a "Split species".  When it is split a `ParentSynonymChildException` is thrown.  The recommended action is to use the child result as the match.  This is the default behaviour. 
    * Check to see if the result is a "misapplied name".  When this check fails a `MisappliedException` is thrown. There are 2 situations where this happens:
      * The top result is misapplied and there is no result with an accepted name. In this situation there is NO valid match. The `MisappliedException` will have a value for the matchedResult but NOT misappliedResult.
      * The top result is an accepted concept and the second result indicates that the name has been misapplied in the past.  The `MisappliedException` will have a value for both the matchedResult and misappliedResult. The recommended action is to use the matchedResult as the result. This is the default behaviour.
    * Check to see if the result is a "cross rank homonym".  This check is only performed if NO rank is provided in the search.  A `HomonymException` is thrown.  The recommended and default behaviour is to NOT have a match. 
    * Check to see if the result is a "homonym". This check is performed if the result is a genus or species.  It will use the supplied classification and/or authorship to try to resolve the homonym.  If the homonym cannot be resolved a `HomonymException` is thrown.  The recommended and default behaviour is NOT to have a match.

### Error Types
This section outlines the errors that can be returned as part of a MetricsResultDTO (obtained using the `ALANameSearcher.searchForRecord` Metric methods)
  * speciesPlural - the name was supplied with an spp. marker. A match to the species level can not be performed.  If recursive matching was enabled a genus level match would be performed.
  * indeterminateSpecies - An indeterminate marker was detected and and exact match could not be found.
  * questionSpecies - The scientific name was supplied with a question mark indicating an unsure identification.
  * affinitySpecies - An aff. marker was detected in the supplied name.
  * conferSpecies - A cf. marker was detected in the supplied name.
  * homonym - A homonym was detected with the supplied name. If recursive matching was enabled a match to a higher level would be performed.
  * generic - Indicates that a generic error occurred during the match. Causes of this error should be investigated and fixed through changes to the name matching process.
  * parentChildSynonym - The match concept has been detected as a synonym of a child concept.  This issue is generally caused when a species has been split into multiple subspecies and the original species is considered a synonym to one of the subspecies.
  * excludedSpecies - The matched species is considered excluded from Australia.  A biocache QA could be created when a match of this type is found in Australia.
  * associatedNameExcluded - There were more than one match.  One the matches was excluded and one was not.
  * matchToMisappliedName - The supplied scientific name has been misapplied to another concept in the past.  The match has been made to the supplied name but be aware that your supplied name may not be correctly identified.
  * misappliedName - The supplied scientific name has been misapplied in the past.  The accepted name does NOT exist in the ALA.
  * noIssue - No issue has been detected.

### Glossary

* Cross Rank Homonym - A name that appears more than once at different ranks.
* Excluded - An excluded concept is one that the NSL believes does not occur in Australia.  This is only an issue if you are sure that the name you are matching on is in Australia.
* Fuzzy Match - A search that is performed using a simplified taxa match algorithm.  It will account for masculine and feminine errors and some errors in misspelling. This type of match should be used with caution as there is the potential for false positives to be returned.  A fuzzy match is only supported for scientific names at the genus level or below.
* Homonym -  A name that has been publish more than once describing different species/genera.
* Misapplied Name - A misapplied name is where a scientific name as been incorrect used in a publication to identify a species.
* Phrase - The phrase component of a Phrase Name, generally indicating a location. See Phrase Name for more information.
* Phrase Name - An official scientific name that uses a phrase and optional voucher in place of a specific or infraspecific epithet.   This type of name is generally assigned before an official name can be published.  We need to consider them because we receive occurrence records using them AND more importantly they are included in official conservation lists.  

`
Example of a phrase name:
Stylidium sp. Boulder Rock (A.H. Burbidge 2536)<br>
Genus = Stylidium<br>
Phrase = Boulder Rock<br>
Voucher = A.H. Burbidge 2536<br>
`

Here is a link to all the biocache records that have been matched to a phrase name:
`
http://biocache.ala.org.au/occurrences/search?q=*:*&fq=name_match_metric:phraseMatch
`
* Split Species - A species that has been split into a few subspecies. This results in the original species name becoming a synonym to the subspecies.
* Voucher - The voucher component of a Phrase Name.  This is generally composed of a collector name and identification.  See Phrase Name for more information.  
#Matching Algorithm (v3 and earlier)

This document describes the algorithm used to match a supplied name against the name matching index.
The document applies to v3 (and earlier) versions of the name matching library

##Concepts

* **taxon concept** The placement of a taxon (scientific name) in the taxonomic hierarchy,
  possibly as a synonym of another taxon.
* **canonical name** The scientific name formatted according to the rules of the naming codes without
  extraneous detail.
  (In practice, this means according to the GBIF form.)
* **left** and **right** values The name index is in the form of a tree.
  The index is enumerated from left to right in a top down fashion.
  Each leaf of the tree has an index number assigned to it and the left- and right-values are
  given that number.
  An internal node has a left-value equal to the minimum of the child nodes left-values and a right-value
  equal to the maximum of the child nodes right values.
  Left and right values allow for fast lookup in the biocache index at expense of needing to re-allocate values
  with each new index, since the values are not stable.
* **phrase name** A scientific name for a sample that has not yet been fully described.
  A phrase name consists of a scientific name (usually a genus), a phrase describing the sample and a voucher, the person 'vouching'
  for the sample.
  The voucher is a name and unique voucher numer.
  For example, in *Pultenaea sp. 'Olinda' (Coveny 6616)* the phrase is Olinda and the voucher is Coveny 6616
* **soundex** A treatment of a word into a "sounds-alike" form that can be used to trap misspellings or
  changes in Latin gender. The soundex algorithm used is based on the [Taxamatch](http://www.cmar.csiro.au/datacentre/taxamatch.htm)
  algorithm developed by Tony Rees.
* **excluded** An indication that a name should not be used.
  Usually, this means that something has been identified as a taxon from somewhere else.
  Excluded basically means "you think this is right, but you're wrong"
  Excluded *should* be for a geographical range (eg. excluded in WA) but this is not modelled
  

##Index Structure

The matching algorithm uses a pre-constructed set of lucene indexes.
The indexes are:

#cb

The main index contains taxon concepts.
Each taxon concept contains the following fields:

* **id** A UUID for the entry
* **lsid**  The identifier used to identify the taxon concept in the ALA (taxonID)
* **name** A multivalued list of the supplied scientific name without author, canonical name without author, 
  the complete name with author and, for viruses, the name without the virus keyword.
  In each case, each entry contains the name as supplied, the normalised name 
  (all unicode ligatures, decomposed accents, odd spaces and quotation marks normalised) and
  the basic name (the same as normalised with non-ASCII characters such as 'ß' converted to 'ss'
  and accents removed)
* **name_canonical** The supplied name (normalised)
* **name_complete** The complete name (normalised)
* **author** The authority for the taxon concept
* **rank_id** The numerical rank number, from RankType
* **rank** The rank name, from RankType
* **is_synonym** Set to T if the taxon concept is a synonym
* **accepted** The lsid of the accepted concept if this is a synonym
* **synonym_type** The type of synonym if this is a synonym
* **kingdom** The kingdom name
* **kid** The kingdom lsid
* **phylum** The phylum name
* **pid** The phylum lsid
* **class** The class name
* **cid** The class lsid
* **order** The order name
* **oid** The order lsid
* **family** The family name
* **fid** The family lsid
* **genus** The genus name
* **genus_ex** A soundexed form of the genus name
* **gid** The genus lsid
* **species** The species name
* **species_ex** A soundexed form of the species name (specific epithet)
* **sid** The species lsid
* **infra_ex** A soundexed form of the infraspecific epithet
* **specific** The specific epithet
* **phrase** The descriptive phrase in a phrase name
* **voucher** The voucher in a phrase name
* **left** The left index value
* **right** The right index value
* **ala** A flag set to T indicating that the taxon concept is an ALA-generated concept

The name fields are given a boost based on the source of the name.
The boost ensures that, where there are multiple candidates, the index search will tend to
prefer names from more authoritative sources.

The index constructor essentially navigates the parent-child tree supplied by the input taxonomy
and fills out the kingdom, phylum, class etc. entries for the individual taxon.
Synonyms do not have higher-order taxonomy filled out.

###vernacular

An index of vernacular names.
Each entry contains the following fields:

* **lsid** The identifier of the corresponding taxon concept
* **common_orig** The common name as supplied
* **common** The common name in uppercase form without accented characters, suitable for search
* **name** The scientific name of the taxon concept

###irmng

An index of the Interim Reigster of Marine and Non-Marine Genera.
It contains the following fields:

* **kingdom** The kingdom name
* **phylum** The phylum name
* **class** The class name
* **order** The order name
* **family** The family name
* **genus** The genus name
* **species** The species name (genus and specific epithet)
* **author** The authority for the taxon concept
* **rank** The taxon rank

##Matching Algorithm

There are multiple entry points into this algorithm.
This section describes the basic elements of the algorithm.

###Input

Many of these elements can be defaulted in various ways but the basic elements are:

* **classification** An instance of LinnaeanRankClassification with as much or as little of
  kingdom, phylum, class, order, family, genus, specific epithere, infraspecific epithet,
  scientific name, author and rank as can be supplied.
* **recursiveMatching** If true then attempt a higher classification match if a match isn't found
* **fuzzy** If true then attempt a soundex match if a direct match isn't found
* **ignoreHomonym** Ignore homonym detection if a single result is found

###Initial Analysis

Initial analysis attempts to fill out as many elements of the input as possible, based on
reasonable quesses

####Rank Analysis

If a rank is not supplied then a rank is identified using the following heuristics:

* An initial rank is assigned based on the lowest part of the Linneaen classification with a filled in value.
* If a scientific name is supplied and a rank is not supplied, the name is parsed and any clues to rank are extracted
  * If the name is doubtful then only genus is used and the rank is genus
  * Subspecies and cultivar name parts are detected and used, although rank indicators are not

####Name Analysis

This gets done each search, but it makes sense to put it here

* Blank names and rank-only names are detected and an exception thrown
* Doubtful markers (?) are removed
* Virus keywords are removed
* Multiple species markers (spp.) generate an exception, leading to higher-order matching
* The name is normalised and parsed

###Search

The basic search looks for a name in the name field of the taxonomy index, to which are added the following
terms, used to narrow and condition the results list:

* If a rank is supplied, then the rank is added. Ranks below species are very wobbly, so a range search on
  the rank id is used. (Synonyms and ALA taxa do not exactly match ranks.) 
  **Note: This needs to be done for all ranks. Ranks fall into rather fuzzy groups with sources disagreeing
  on the exact rank something is.**
* Higher-order taxonomic information is added from the Linnaean clasification as non-essential terms

The result is a list of possible matches.
This result will, largely, be ordered by the boosts on the names.
The list is then examined for a number of special conditions, these usually generate exceptions that
trigger resolution at a higher level.
Each exception tends to skip further checks, listed below.
For convienience, the resolution is given with the check.

If a match is not found and the supplied name is a phrase name, 
a phrase name search is performed in the same way, with the genus, phrase, voucher and species supplied.
If multiple results are returned, the results are checked for a common accepted taxon.
Each search undergoes the checks listed below.

If a match is not found, the search is then tried with the parsed, canonical name.

If a match is not found and the parsed name is though to be a cultivar, then a phrase name search is performed
treating the cultivar epithet as the phrase.

Finally, if there is still no match, then a search on the genus and species soundex values is performed.

####Excluded Taxa

If the results only consist of exclded taxa, then an exception is raised, since there is no match.

If the results contain a mixture of excluded and non-excluded taxa, then an exception is raised giving
the preferred non-excluded taxon.
This is because the matching algorithm will accept a non-excluded result over an excluded result without
distribution information
**Note: Not sure why this is an exception**

**Resolution:** If there is a non-excluded taxon, return that, otherwise return the excluded taxon

####Species Split

Parent-child synonyms are detected.
This happens when a species is sub-divided and what was previously only given as a species is now
a subspecies of the original species. For example, *Menura novaehollandiae* (Lyrebird) became 
*Menura novaehollandiae novaehollandiae* with two other subspecies.
This leads to the complicated case where a supplied name of *Menura novaehollandiae* has both
an accepted taxon and a synonym to the subspecies.
If this occurs, a parent-child synonym exception is raised.
**Note: Again, why an exception, since this is "normal"**

There are two possible cases, the first involves a single parent and single child.
The second involves a series of synonyms, all pointing to a single accepted result.

**Resolution:** Use the child result.

####Misapplied Checks

If the first result is a misapplied name, then a missapplied exception is raised.
If there is another, accepted result, then the accepted result is also included in the exception.
**Note: Usual complaint**

**Resolution:** Use the accepted result, if possible

####Homonym Checks and Extraction

If the name is contained in the list of known cross-rank homonyms and no rank is supplied,
then an exception is raised.

For genus- and species-level searches, homonym detection is then performed.
If an author is supplied, then the supplied author and the author from the first result is
compared, using Smith-Waterman-Gotoh similarity. If the authorship is less than 80% similar,
then homonym detection continues.
Homonym detection searches the IRMNG index at the species/genus level for the highest
rank where, given a classification, the number of results in IRMNG to one, the resolution rank.
The list of results is then searched for the first result that matches to the resolution rank.
If a suitable candidate can't be found, then an excepiton is thrown.

**Resolution:** If there is a single result and homonym problems are ignored, then the single result
is returned, otherwise no result is returned.

###Metrics

The search algorithm can either return a simple result or a result with attached metrics.
The metrics are flags indicating the type of match (exact, canonical, fuzzy etc.) and any associated
warnings, such as a parent-child synonyms or excluded names.
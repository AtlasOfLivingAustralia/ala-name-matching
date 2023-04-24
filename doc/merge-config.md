# Taxonomy Merger Configuration

Information on the merge configuration is contained in a JSON document. 
See [ala-taxon-config.json](../data/ala-taxon-config.json) for an example.

The main purpose of the file is to provide a description of the 
various taxonomy sources and rules for scoring individual taxon concepts.

## Header

The header consists of a number of top-level values that 
provide metadata about the configuration.
This metadata is embedded in downstream metadata, such as the
`eml.xml` file in a Darwin Core Archive.
The header contains the following information:

* **id** A unique identifier for the configuration.
* **name** A human-readable name for the configuration.
* **description** A description of the configuration.
* **contact** Contact information in the same form as a GBIF contact description.

## Processors

Class names and defaults for the taxonomy processors:

* **nameAnalyserClass** The class name of the name analyser. 
  Name analysers are used to work out the key name information from a taxon entry
  and resolve vocabulary entries.
  The only implemented name analyser is the `au.org.ala.names.index.ALANameAnalyser`
* **resolverClass** The class name of the taxon resolver.
  Taxon resolvers contain algorithms that decide which of the multiple candidiates
  for a taxon concept is the preferred representation and which of the taxon concepts
  is the the concept that can be used when only a name is present.
  The only implemented resolver is the `au.org.ala.names.index.ALATaxonResolver`
* **acceptedCutoff** A taxon has to be above this score to be considered to be worthy of consideration as a principal.
  The accepted cutoff allows ad-hoc inferred accepted taxa to not be considered superior to synonyms from 
  more authoratative sources.
* **providers** The [provider](#Providers) list.

## Providers

Providers are descriptions of taxonomy providers and how they should be treated.
Each provider contains the following information:

* **id** The identifier of the provider. 
    Generally, this will be the data resource id of the source.
    Supplied taxonomies map onto providers via the `datasetID` or `datasetName` terms.
* **name** A human readable name for the provider.
* **parent** The id of a parent provider. Child providers inherit most properties
  of the parent provider.
* **description** A description of the provider.
* **rightsHolder** The rights holder to the source data.
* **licence** The licence under which the source data is provided.
* **external** Is this an external source of information that can be referred to, 
  as opposed to an internally generated source. Defaults to `true`
* **loose** Does this provider provide "loose" information?
  Generally, the data provided is expected to separate `scientficName`
  and `scientificNameAuthorship` with `nameComplete` containing the
  complete scientific name, if needed. A loose provider does not 
  guarantee name/authorship separation and other features and an attempt
  is made to extract authorship during analysis.
* **defaultNomenclaturalCode** The nomenclatural code to use for a name, if one is not explicitly provided.
* **defaultScore** The default score to use when scoring taxon concepts.
* **defaultVernacularScore** The default score to use when scoring vernacular names.
* **scores** A dictionary of name:score pairs containing non-default scores for specific names. 
  Once a name has been matched, all child taxa of the name inherit that score.
* **adjuster** Adjustments to the scores, based on things like taxonomic status,
  rank etc. See [adjusters](#Adjusters) below.
* **keyAdjuster** Adjustments to the name keys, based on things like name.
  See [adjusters](#Adjusters) below.
* **vernacularAdjuster** Adjustments to the vernacular name scores, based on things like vernacula status,
  language etc. See [adjusters](#Adjusters) below.
* **owner** A list of names that this provider "owns", meaning that an unauthored name
  will always be mapped onto the taxon concept supplied by this provider.
* **discardStrategy** How to handle discarded/forbidden names (eg. unplaced names). This can be one of
  IGNORE (the default) which simply drops the names, IDENTIFIER_TO_PARENT which adds the discarded taxon's
  identifier to the parent taxon as a "discarded" identifier (only the prinary taxonID is added), 
  SYNONYMISE_TO_PARENT makes the discarded taxon a synonym of its parent (use this option with caution, since
  you are likely to get a large number of "Unplaced"s acting as synonyms).
* **unrankedStrategy** How to handle unranked taxa. This can be one of
  `NONE` (the default) which simply leaves unranked taxa as-is, `SYNONYMS` which searches for an existing ranked taxon
  with the same name/author for synonyms only, `INFERRED_AND_SYNONYMS` which searches for inferred status taxa or synonyms
   or `ALL` which searches for an existing ranked taxon for all unranked
  taxa. If you use `SYNONYMS_INFER`, `INFERRED_AND_SYNONYMS_INFER` or `ALL_INFER` an attempt to infer the rank is made, 
  even if there isn't an existing ranked taxon.
* **unknownTaxonID** An identifier for a missing taxon. Synonym loops and the like map onto this
  taxon. If the taxon doesn't already exist, it is created.
* **defaultParentTaxon** The name of a default parent taxon to give to entires without an identifiable
  parent. This allows partial taxonomies to be shimmed into larger, orver-arching taxonomies.
  Use with caution, since the parent is applied without regard to rank.
* **parentOutput** A map from taxonomic status to a boolean.
  If the map explicitly maps a status on to true, a parent identifier is output, if present.
  If the map explicitly maps a status on to false, a parent identifier is not output, even if present.
  The default behavior is to output true if the status is generally regarded as accepted.
  This map allows a per-provider way of ensuring that parents are output if available.
  If a status is not set, then the status is inherited from the parent provider.
* **acceptedOutput** A map from a taxonomic static to a boolean.
  The acceptedOutput map operates in the same way as the parentOutput but
  with respect to the accepted usage, if a taxon is synonym-like.
  By default, accepted output occurs if the taxonomic status of an instance
  is a synonym of some sort.
  * For example, to allow excluded taxa to redirect to a synonym, use
   `{ "EXCLUDED": true }`
    In general, excluded taxa do not have an accepted name.

### Adjusters

Adjusters manipulate the scores and other features of taxa.
The adjusters property is, essentially lists of rules that can be
applied to a taxon concept.
The rules are applied down the parent-child hierarchy, with the parent
rules applied first and then the child rules.
All rules that match, from whatever level, are applied so to cancel out
a rule, a child needs to have a rule that reverses the parent rule.

* **forbidden** A list of [conditions](#Conditions) that forbid a taxon.
  Forbidden taxa are not included in the output taxonomy.
  Synonyms and parent taxa are mapped onto the nearest, non-forbidden
  parent taxon.
* **adjustments** A list of [adjustments](#Adjustments) to be applied
  to the source taxa.

### Conditions

Conditions are identified by the condition class and are composable.
An example condition is:

```
{
  "@class" : "au.org.ala.names.index.provider.MatchTaxonCondition",
  "scientificName" : "Viruses",
  "taxonRank": "KINGDOM"
}
```

The **@class** is the class of the condition.
The possible classes are:

* `au.org.ala.names.index.provider.MatchTaxonCondition` 
   matches a number of specified criteria on a taxon.
* `au.org.ala.names.index.provider.MatchVernacularCondition`
  matches a number of specified criteria on a vernacular name.
  Although there is no particular reson to do so, taxon match and
  vernacular match conditions will accept both
* taxa and vernacular names; they will simply fail to match the wrong type.
* `au.org.ala.names.index.provider.OrTaxonCondition` Any of
   the conditions in the **any** property can match.
* `au.org.ala.names.index.provider.AndTaxonCondition` All of
   the conditions in the **and** property must match.

The major condition type is the MatchTaxonCondition.
This can be used to match a number of criteria on an individual taxon.
All specified criteria must be matched.

* **nomenclaturalCode** Match a specific nomenclatuural code from the
  `org.gbif.api.vocabulary.NomenclaturalCode` vocabulary.
* **datasetID** Match a specific datasetID
* **scientificName** Match a scientificName.
* **scientificNameAuthorship** Match an author
* **matchType** The type of name/author match, one of EXACT the default,
  INSENSITIVE (case insensitive), NORMALISED (accents and odd characters
  normalised to ASCII) and REGEX (regular expression)
* **taxonomicStatus** Match a taxonomic status from the
  `au.org.ala.names.model.TaxonomicType` vocabulary.
* **nomenclaturalStatus** Match a nomenclatural status from the
 `org.gbif.api.vocabulary.NomenclaturalStatus` vocabulary
* **nameType** Match a name type from the
  `org.gbif.api.vocabulary.NameType` vocabulary
* **taxonRank** Match a rank from the 
  `au.org.ala.names.model.RankType` vocabulary
* **year** Match a year of publication

Vernacular names can be match against a MatchVernacularCondition.
This can be used to match a number of criteria on an individual vernacular names.
All specified criteria must be matched.

* **datasetID** Match a specific datasetID
* **vernacularName** Match a scientificName.
* **status** Match a vernacular status from the
  `au.org.ala.names.model.VernacularType` vocabulary.
* **isPreferredName** Match a preferred name flag
* **language** Match a language code. This is usually an ISO code but may not be if more precision is required.
* **locality** Match a location. 
  The locality can be a location identifier or name.
  A vernacular name matches if this locality
  encompasses the locality of the vernacular name
  (eg a name for NSW will match a locality of Australia)
* **matchType** The type of name/language match, one of EXACT the default,
  INSENSITIVE (case insensitive), NORMALISED (accents and odd characters
  normalised to ASCII) and REGEX (regular expression)

### Adjustments

Score adjustments simply have a **adjustment** property containing
a value to add to (or subtract from in the case of negative numbers)
the current taxon score.
Score adjustments can be used to prefer accepted taxa over
synonyms or downgrade doubtful names.

Key adjusters can alter a name key used to categorise the taxon.
These can be used to alter:

* **nomenclaturalCode** Change to a nomenclatuural code from the
  `org.gbif.api.vocabulary.NomenclaturalCode` vocabulary.
* **scientificName** Change the scientificName.
* **scientificNameAuthorship** Change the author
* **nameType** Change to a name type from the
  `org.gbif.api.vocabulary.NameType` vocabulary
* **rank** Change to a rank from the
  `au.org.ala.names.model.RankType` vocabulary
  
#### "Squashing" Ranks

Multiple sources can disagree on the exact rank a taxon has.
Adjusters can be used to "squash" the ranks so that, eg. superfamily, family, subfamily and infrafamily all have
a key rank of family.
Note that this does not change the supplied rank, which is what will be output.

### Default Providers

Default providers are specified in the head object, preferably after
the providers are listed, so that they can be referenced.

* **defaultProvider** The provider to use if one cannot be deduced.
  If a `datasetID` has been provided but is not listed, an ad-hoc loose
  provider is generated with the default provider as a parent.
* **inferenceProvider** The provider to which inferred taxa are attributed.
  The name resolution algorithms may be required to introduce inferred synonyms
  and other taxa that have not been provided by an original source.
  These are attributed to the inferenceProvider.
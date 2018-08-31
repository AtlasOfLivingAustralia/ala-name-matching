# The Large Taxon Collider

The purpose behind the taxonomy builder, also known as the large taxon collider (LTC) is to 
blend multiple taxonomies into a single taxonomy that can be used to match names and place them
into a single (sort of) consistent taxonomy.

## How the LTC Works (the simple version)

The basic appproach behind the LTC is to load all the source taxonomies and collect taxon concepts together
from the different sources.
Once this has been done, the LTC scores the various taxon concepts, based on the original source and other
markers, such as whether the taxon is marked doubtful.

The LTC then chooses a winner from the various source concepts (called taxon variants) which it regards as
the representative or primary taxon for that taxon concept.
The primary concepts are then used to build the taxonomy, with the primary concept's view
of taxonomic status, parent taxon, accepted taxon and so on being used to structure the taxonomy.
Non-primary taxa are mapped on to the primary concept, with the result that child taxa pointing to
the parent or synonyms pointing to an accepted concept 

Once this has been done, the LTC outputs a combined taxonomy.
The core of the taxonomy is built from the primary concepts, however, taxon variants are attached so
that  

Is it more complicated than that? Of course it is!

## How the LTC works (the complicated version)

### Provenance and Logging

A taxon may include multiple provenance statements and taxon remarks, both supplied by the input
taxonomies and added by the LTC.
If the LTC makes changes (other than vocabulary changes) to a supplied taxonomic element, the fact
is recorded in the provenance field.
Additional information about the taxon is included in the taxon remarks field.
The provenance and taxon remarks fields are included in the output, if needed.

The LTC also logs choices made, informational comments, problems and errors.
The log is output as a taxonomy report which can be read into a spreadsheet and examined.

### Name Keys and Taxonomic Elements

Each taxon variant is associated with a unique taxon identifier, given by the Darwin Core term taxonID.
The taxon identifier can be any unique string of characters: an LSID, a URL, a UUID, a number and a string based on the name
are all examples that occur in the ALA names list.
Taxon identifiers are used to link taxonomic elements together and provide identifiers for the output taxonomy..

Name keys are the primary method of deciding whether two taxonomic concepts from different sources
refer to the same thing.
A full name key contains the following elements:

* **nomenclatural code** The code which gives authority to the name
* **rank** The rank of the taxon (kingdom, phylum, class, etc.)
  This is included to distingush between cross-rank homonyms, such as the Polyphaga genus and order.
* **name** The scientific name, without authorship. This name is normalised and canonicalised so that stylistic variants reduce to the same name.
* **author** The author of the taxonomic concept. These are compared using the GBIF author comparator, so that common abbreviations, such as L. for Linnaeus, are handled correctly.
* **name type** The type of name. This is used to provide specialised behaviour for wonky names, such as doubtful or placeholder names.

Source taxonomies should provide the nomenclatural code, taxon rank, scientific name and scientific name authorship.
However, some of these elements are often absent and a hierarchy of concepts is built so that inadequately specified
taxonomic elements can be tracked down.
The various levels of taxonomic element are:

| Element | Description | nomenclatural code | rank | name | author | name type |
| ------- | ----------- | ------------------ | ---- | ---- | ------ | --------- |
| Taxon Concept | A single taxonomic concept | Y | Y | Y | Y | Y |
| Scientific Name | A scientific name within a code but not placed in a taxonomy | Y | Y | Y | | Y |
| Unranked Name | A scientific name without rank information | Y | | Y | | Y |
| Bare Name | A scientific name without any additional information, including nomenclatural code | | | Y | | Y |

These elements form a hierarchy.
A bare name contains one or more unranked names.
An unranked name contains one or more scientific names.
A scientific name contains one or more taxon concepts.
A taxon concept contains one or more taxon variants.

Each element has a concept of a principal sub-element.
The principal sub-element is the one that is chosen to represent that element, in the absence of further contextual information.
Taxon variants are the base element and do not contain any sub-elements.
Taxon concepts have a slightly more complex mapping than a single, primary taxon variant since
they may contain multiple independent variants that do not map onto each other; more about this below.

#### Example

Suppose we have the following, largely made up, taxon variants from different sources:

* APC, ICNAFP, Acacia, Mill., genus
* NZOR, ICNAFP, Acacia, Miller, genus
* APC, ICNAFP, Acacia, Onga, genus
* Conservation codes for plants, ICNAFP, Acacia, Yanko, family
* Animal threatened species list, ICZN, Acacia, Williams, 1956, genus

The taxon concepts would be, with name keys:

* <BOTANICAL, ACACIA, Mill., GENUS, SCIENTIFIC> (**APC, ICNAFP, Acacia, Mill., genus**; NZOR, ICNAFP, Acacia, Miller, genus)
* <BOTANICAL, ACACIA, Onga, GENUS, SCIENTIFIC> (**APC, ICNAFP, Acacia, Onga, genus**)
* <BOTANICAL, ACACIA, Yanko, FAMILY, SCIENTIFIC> (**Conservation codes for plants, ICNAFP, Acacia, Yanko, family**)
* <ZOOLOGICAL, ACACIA, Williams, 1956, GENUS, SCIENTIFIC> (**Animal threatened species list, ICZN, Acacia, Williams, 1956, genus**)

The principal is in bold.
The scientific names would be:

* <BOTANICAL, ACACIA, GENUS, SCIENTIFIC> (**APC, ICNAFP, Acacia, Mill., genus**; NZOR, ICNAFP, Acacia, Miller, genus; APC, ICNAFP, Acacia, Onga, genus)
* <BOTANICAL, ACACIA, FAMILY, SCIENTIFIC> (**Conservation codes for plants, ICNAFP, Acacia, Yanko, family**)
* <ZOOLOGICAL, ACACIA, GENUS, SCIENTIFIC> (**Animal threatened species list, ICZN, Acacia, Williams, 1956, genus**)

The unranked names are:

* <BOTANICAL, ACACIA, SCIENTIFIC> (**APC, ICNAFP, Acacia, Mill., genus**; NZOR, ICNAFP, Acacia, Miller, genus; APC, ICNAFP, Acacia, Onga, genus; Conservation codes for plants, ICNAFP, Acacia, Yanko, family)
* <ZOOLOGICAL, ACACIA, SCIENTIFIC> (**Animal threatened species list, ICZN, Acacia, Williams, 1956, genus**)

And the bare name is:

* <ACACIA, SCIENTIFIC> (**APC, ICNAFP, Acacia, Mill., genus**; NZOR, ICNAFP, Acacia, Miller, genus; APC, ICNAFP, Acacia, Onga, genus; Conservation codes for plants, ICNAFP, Acacia, Yanko, family; Animal threatened species list, ICZN, Acacia, Williams, 1956, genus)

### Inputs

The LTC is guided by a configuration file, which contains the rules about how to treat the input taxonomies.
The configuration file is documented [here](merge-config.md).

The first step of the LTC is to read in all source taxonomies.
These can be in the form of a Darwin Core Archive (DwCA) or a simple CSV file assumed to have Darwin Core (DwC) terms.

With the DwCA form, the following types of input can be used:

* **Taxon** A standard DwC taxon with at least a taxon identifier, dataset identifier, nomenclatural code, 
  scientific name, authorship taxonomic status, rank and parent and accepted taxon information.
  Parent information can come in a number of forms, including the parent taxon identifier, parent taxon name
  or Linnaean classification.
  Accepted taxon information can come as an identifier or name.
  The result of reading a row of taxon information is a taxon variant.
* **Identifier** Zero or more alternate identifiers linked to a taxon.
* **Vernacular Name** Either a stand-alone vernacular name list or zero or more vernacular names linked to
  a taxon.
* **Distribution** Zero or more taxon distributioon statements (not used by the LTC)

These taxonomies are read into a memory structure but also added to a lucene index.
The lucene index contains every piece of data provided by the input data, including terms not used by the LTC.
At output, the taxon variant file contains the additional terms contained in the index, so additional information
can flow through the LTC.

On input, taxon variants are analysed:

* The dataset identifier is matched against a list of providers contained in the configuration.
  If the identifier is absent, a new, default provider is generated.
* The taxonomic status, nomenclatural status, nomenclatural code and rank are parsed and mapped onto
  standard GBIF or ALA vocabularies.
  Where terms are converted into vocabulary terms, a verbatim form of the term is included in the variant,
  so translation errors can be identifier.
* The supplied scientific name, author and nomenclatural code are used to build a name key.
  This analysis is performed by configurable name analyser.
  * The configuration may have rules that adjust the name key, to take care of special cases.
    In particular, the rank may be adjusted to bucket brackets of ranks (eg., superfamily, family, subfamily)
    into a single grouping so that minor differences in rank do not result in multiple taxon concepts.
  * Certain name types (eg. unplaced names) may be given special treatment to ensure that 
* The taxon may be marked as *forbidden*, either by confiugration rules or because the taxon data is
  so poorly structured that the LTC can't make sense of it.
  
Once analysed, the taxon variant is added to the master map of identifier to taxon variant.
The taxon variant is then placed in the taxon concept, scientific name, unranked name, bare name hierarchy.
The final result, post-loading is that each taxon concept contains a list of matching taxon variants.

#### Providers

Providers are the sources of input taxonomies.
Providers match to the datatsetID or datasetName and, via the configuration, contain
rules on how to score and otherwise treat taxon variants coming from that particular source.
Almost all rules that can be applied to a taxon can be configured on a per-provider basis,
with an inheritance mechanism so that default behaviours can be managed.

Two special providers are the default provider, that forms the base template for any providers
that are created on the fly and the inference provider that is used as the source of taxonomic elements
created by the LTC.

Some providers are said to *own* a particular branch of the taxonomic tree.
If a provider owns a branch, then their word on the placement of scientific names is law, with no
alternate taxon concepts permitted.
For example, in the ALA taxonomy, AusFungi owns the Fungi kingdom and alternative taxon concepts for
fungi, such as the choice between Fungi Engl. and Fungi Whittaker, are eliminated.

#### Example

With the [standard ALA configuration](../data/ala-taxon-config.json), the following taxon variant
appears:

| term | value | analysed |
| ---- | ----- | -------- |
| taxonID | http://id.biodiversity.org.au/node/apni/2900822 | |
| datasetID | dr5214 | APC |
| nomenclaturalCode | ICNAFP | BOTANICAL |
| scientificName | Eucalyptus rossii | |
| scientificNameAuthorship | R.T.Baker & H.G.Sm. | |
| taxonomicStatus | accepted | ACCEPTED |
| nomenclaturalStatus | | |
| taxonRank | species | SPECIES | 

This will have a key of <BOTANICAL, EUCALYPTUS ROSSII, R.T.Baker & H.G.Sm., SPECIES, SCIENTIFIC>,
be identified as "http://id.biodiversity.org.au/node/apni/2900822" and be inserted into the taxon concept
hierarchy by the corresponding key.

As another example,

| term | value | analysed |
| ---- | ----- | -------- |
| taxonID | CoL:29944185 | |
| datasetID | dr2705 | Catalogue of Life |
| nomenclaturalCode | ICVN | VIRUS |
| scientificName | Viruses | VIRUSES |
| scientificNameAuthorship | | |
| taxonomicStatus | accepted | ACCEPTED |
| nomenclaturalStatus | | |
| taxonRank | kingdom | KINGDOM |

This will have a key of <VIRUS, VIRUS, , KINGDOM, VIRUS>,
be identified as "CoL:29944185" and be inserted into the taxon concept
hierarchy by the corresponding key.
The change in name in the key from Viruses to Virus is given in the configuration, with
the rules for provider dr2705 saying that a key with a scientific name of "Viruses" be
changed to "Virus".

#### The Unknown Taxon

The unknown taxon is a taxon that is used as a placeholder for taxa that have somehow
required a new parent or accepted taxon or otherwise got lost, usually because of a data problem.
The unknown taxon can either be provided as part of one of the input taxonomies or
a taxon with an identifier of `ALA_The_Unknown_Taxon` is added to the final taxonomy.

### Inference

Inference refers the LTC, or the processing prior to the LTC that generates the inputs, making taxonomic choices.
These are flagged by a taxonomic status of inferred something, indicating that the taxonomy processing needed to
make a choice.
The possible inferred taxonomic status elements are:

* **inferredAccepted** In the absence of any explicit information, this taxon variant is assumed to be accepted.
  Inferred accepted taxon variants are often provided as inputs.
  For example, the taxa provided by the threatened species lists in the ALA are not usually from full taxonomies
  and are assumed to be accepted.
* **inferredSynonym** This taxon variant has been made into a synonym by the LTC.
  Inferred synonyms are created during principal resolution when a provider asserts ownership of a scientific
  name and other taxon concepts are mapped onto the owner's scientific name.
  For example, AusFungi owns Fungi and Fungi Whittaker, from other sources, is turned into an inferred
  synonym of Fungi Engl.
* **inferredUnplaced** In the absence of any explicit information, this taxon variant is assumed to be unplaced.
  A taxon variant may become unplaced due to things like synonym loop breaking or invalid source data.
* **inferredInvalid** In the absence of any explicit information, this taxon variant is assumed to be invalid.
  Inferred invalid taxon variants may be provided as inputs and marked as forbidden (see below) by configuration rules.
  
#### Forbidden Taxon Variants

A taxon variant can be marked as *forbidden*, meaning that, either by provider rules or during resolution,
the taxon variant will be eliminated from the output taxonomy.
If a taxon element has a forbidden element as a parent, then the first non-forbidden parent of that element is used.
Similarly, if a taxon element has an accepted taxon element that is forbidden, then the parent is used.

### Scores

Each taxon variant is provided with a priority score.
Generally, during [resolution](#resolution) the priority scores are used to select
which taxon variant is to be used as the authority on whether a taxon concept is
accepted or a synonym, what the parent or accepted name for the taxon concept is,
and any other status information.
References to other taxon variants within the taxon concept get transferred to the authority.

Scores are calculated by first starting with a provider score, derived from the datasetID.
This provides a base score which travels down the taxonomic hierarchy.
Providers can have a default score and then a specific score that is applied to some
part of the taxonomy.
For example, AusMoss has a default score of 5000 but that rises to 7000 for anything
in the Bryophyta or Bryidae branches in the [standard ALA configuration](../data/ala-taxon-config.json).

An individual taxon variant can also have its base score adjusted by rules in the configuration.
For example, if the taxonomic status of the taxon varient is unplaced, then 300 is subtracted from
the score.
Similarly, if the name is informal, then 110 is subtracted from the score.

During resolution, a *cutoff score* may be used.
Taxon variants (and by extension containing taxonomic elements) with a score below the cutoff
are not accepted as principals unless there are no other candidates.
Usually, accepted taxa are preferred over synonyms, exlcuded taxa and the like as a principal.
Elements below the cutoff score are considered to be from poor sources and excluded from consideration, possibly
resulting in a synonym or other type of taxon to be considered.

#### Example

| term | value | analysed |
| ---- | ----- | -------- |
| taxonID | http://id.biodiversity.org.au/instance/apni/847760 | |
| datasetID | dr5214 | APC |
| nomenclaturalCode | ICNAFP | BOTANICAL |
| scientificName | Arduina | |
| scientificNameAuthorship | Mill. ex L. | |
| taxonomicStatus | heterotypicSynonym | HETEROTYPIC_SYNONYM |
| nomenclaturalStatus | nom. illeg., nom. rej. | ILLEGITIMATE, REJECTED |
| taxonRank | genus | GENUS |

Has a base score of 6000 from the APC, adjusted by -20 for an illegitimate name and another -20 for a rejected
name, giving a score of 5960.

### Resolution

Resolution is the process by which the merged taxonomy is built.
Essentially, the system examines each taxon concept and produces a resolution,
a mapping from all the taxon variants in the taxon concept to the taxon variants that
will actually be used.

Resolution follows these steps:

1. [The Unknown Taxon](#the-unknown-taxon) is created, if needs be
1. [All links are resolved](#resolve-links)
1. [Simple parent or synonym loops are resolved](#resolve-loops)
1. [Name collisions are checked](#resolve-name-collisions)
1. [Taxon concepts are resolved](#resolve-taxon-concepts)
1. [Unranked taxa are resolved](#resolve-unranked-taxa)
1. [Principals are resolved](#resolve-principals)
1. [Discarded taxa are resolved](#resolve-discards)
1. [Vernacular name resolution](#resolve-vernacular-names)

#### Resolve Links

When loaded, parents and accepted taxa are usually represented by identifiers or names.
Link resolution converts these into links to the appropriate taxonomic element.
Ideally, these taxonomic elements are taxon variants, the most accurate linkage.
However, some ways of representing relationships are less accurate and the LTC attempts to
match the parent/accepted taxon using names.

* If a taxon has a parent identifier, then that identifier is found and the parent linked.
* If a taxon has a parent name, then the taxonomy is searched for a taxon with the correct
  name from the same provider as the variant.
* If a taxon has a classification (kingdom, phylum, class, ...) then the taxonomy is searched
  for a taxon with the correct classification name and rank from the same provider as the variant.
  The lowest found classification is then used.
* If a taxon has an accepted name identifier, then that identifier is found and the accepted variant linked.
* If a taxon has an accepted name, then the taxonomy is searched for a taxon with the correct
  name rank from the same provider as the variant.

The resulting parent or accepted taxon may not be a taxon variant, depending on the level of
information supplied.

##### Example

Given the following inputs, the links are resolved according to the parent and accepted entries
at the right of the table. (The principal column shows where the references to scientific names will eventually end up.)

| Index | Type | taxonID | parentNameUsageID | parentNameUsage | acceptedNameUsageID | acceptedNameUsage | kingdom | phylum | class | order | family | genus | nomenclaturalCode | scientificName | scientificNameAuthorship | taxonRank | parent | accepted | principal |
| ---: | ----- | ------: | ----------------: | --------------- | ------------------: | ----------------- | ------- | ------ | ----- | ----- | ------ | ----- | ----------------- | -------------- | ------------------------ | --------- | -----: | -------: | --------: |
| 1  | Variant | 80500000 |                  |                 |                     |                   |         |        |       |       |        |       | ICZN              | Animalia       |                          | kingdom   |        |          |           |
| 2  | Variant | 80510000 | 80500000         |                 |                     |                   |         |        |       |       |        |       | ICZN              | Chordata       |                          | phylum    | 1      |          |           |
| 3  | Variant | 80512000 | 80510000         |                 |                     |                   |         |        |       |       |        |       | ICZN              | Mammalia       |                          | class     | 2      |          |           |
| 4  | Scientific Name |  |                  |                 |                     |                   |         |        |       |       |        |       | ICZN              | Mammalia       |                          | class     |        |          | 3         |
| 5  | Variant | 80512600 |                  | Mammalia        |                     |                   |         |        |       |       |        |       | ICZN              | Diprotodontia  |                          | order     | 4      |          |           |
| 6  | Variant | 80512610 | 80512600         |                 |                     |                   |         |        |       |       |        |       | ICZN              | Macropodidae   |                          | family    | 5      |          |           |
| 7  | Variant | 80512612 | 80512610         |                 |                     |                   |         |        |       |       |        |       | ICZN              | Osphranter     | Gould, 1842              | genus     | 6      |          |           |
| 8  | Scientific Name |  |                  |                 |                     |                   |         |        |       |       |        |       | ICZN              | Osphranter     |                          | genus     |        |          | 7         |
| 9  | Variant | 80512618 |                  |                 |                     |                   |  Animalia |    |    |     |      | Osphranter     | ICZN              | Osphranter rufus | (Desmarest, 1822)      | species   | 8      |          |           |
| 10 | Scientific Name |  |                  |                 |                     |                   |         |        |       |       |        |       | ICZN              | Osphranter rufus |                        | species   |        |          | 9         |
| 11 | Unranked Name   |  |                  |                 |                     |                   |         |        |       |       |        |       | ICZN              | Osphranter rufus |                        |           |        |          | 9         |
| 12 | Bare Name       |  |                  |                 |                     |                   |         |        |       |       |        |       |                   | Osphranter rufus |                        |           |        |          | 9         |
| 13 | Variant | 81512618 |                  |                 | 80612618            |                   |         |        |       |       |        |       | ICZN              | Macropus ruber | Crisp, 1862              | species   |        | 9        |           |
| 14 | Variant | 81512619 |                  |                 |                     | Osphranter rufus  |         |        |       |       |        |       | ICZN              | Kangurus laniger | Gaimard, 1823          |           |        | 11       |           |
| 15 | Variant | 81512620 |                  |                 |                     | Osphranter rufus  |         |        |       |       |        |       |                   | Macropus rufus | (Desmarest, 1822)        |           |        | 12       |           |

#### Resolve Loops

Loops occur when either there is a cycle of synonyms or a cycle of parents.
An example of a synonym loop is
Paracaleana minor (R.Br.) Blaxell is a synonym of Caleana minor R.Br. is a synonym of
Sullivania minor (R.Br.) D.L.Jones & M.A.Clem. is a synonym of Caleana minor R.Br.
with a loop occurring at Caleana minor.

Loops represent an input data problem.
However, they create merry hell with various downstream software systems and need to be identified
and resolved before they exert their malign influence.

A synonym loop occurs at the highest scoring taxon which is part of the loop proper.
For example, the loop proper occurs at Caleana minor and Sullivania minor.
Paracaleana minor is not part of the loop proper, since it is does not occur again
when tracing the loop.

A synonym loop is broken by choosing the highest scoring taxon and converting it
from a synonym into an inferred unplaced taxon with a parent of [the unknown taxon](#the-unknown-taxon).

Parent loops are detected and resolved in a similar matter, with the highest ranking
parent made a child of the unknown taxon.

#### Resolve Name Collisions

This step detects possible cross-nomenclatural code homonyms.
At present, nothing interesting is done with this information other than notifying the user.

#### Resolve Taxon Concepts

Taxon concepts are resolved by building a *resolution*.
The tricky thing with taxon concepts is that a single taxon concept (in the sense of the LTC)
may contain multiple actual taxon concepts of equivalent validity.
For example, the taxon concept may simultaneously be accepted, a pro-parte synonym, excluded in
Western Australia and misapplied to another taxon concept.
The resolution, therefore, contains the following elements:

* **principals** The list of principal taxon variants that will be used to resolve the taxonomy.
* **used** The list of all taxon variants resolved. This *may* include taxon variants not strictly
  associated with the taxon concept when the LTC has to pull shenannigans.
* **unresolved** A set of problem taxon variants
* **resolution** A map of taxon variants onto their chosen principals. This *may* include links to
  principals outside the taxon concept if it is impossible to make a clean match within the concept.

A resolution is computed by a configurable resolver.
The ALA resolver uses the following steps:

* Principals are computed by choosing the highest scoring taxon variant with a primary
  (accepted, synonym etc as opposed to something like excluded) status.
  The *provider* that corresponds to that taxon is then selected and all variants with
  that provider are used as principals, highest score first.
  * There is a cutoff score for low-scoring variants, to avoid minor sources polluting principal selection
  * If there are no computable principals, then all variants become principals
* Individual instances are then matched against principals
  * Principals, of course, map onto themselves
  * Accepted taxa are mapped onto matching taxon concepts, scientific names,
    the least upper bound (lub) of any accepted principals, the lub of any synonyms or itself in that order.
    Note that anything formed by lub will probably end up with a parent taxon concept.
  * Synonym taxa are mapping onto matching taxon concepts with the same status, matches with the same status
    group, scientific names with the same status or status group, the lub of any synonyms or itself in that order.
    Synonym mapping attempts to match like with like.
  * Non-accepted, non-synonym taxa (eg. excluded) are mapped onto accepted taxa where possible.

##### Example

Given the following taxon variants, the resolution is given on the right of the table:

| Index | provider | nomenclaturalCode | scientificName | scientificNameAuthorship | taxonomicStatus | score | primary | resolution |
| ----: | -------- | ----------------- | -------------- | ------------------------ | --------------- | ----: | ------- | ---------: |
| 1     | AFD      | ICZN              | Aproopta       | Turner, 1919             | accepted        | 6000  | Y       | 1          |
| 2     | AFD      | ICZN              | Aproopta       | Turner, 1919             | excluded        | 5700  | Y       | 2          |
| 3     | ASFT     | ICZN              | Aproopta       | Turner, 1919             | accepted        | 1000  | N       | 1          |
| 4     | CCAFX    | ICZN              | Aproopta       | Turner, 1919             | synonym         | 700   | N       | 1          |


#### Resolve Unranked Taxa

Unranked taxa can be a problem where there is a disagreement between sources on whether
a taxon concept is a synonym or not.
Some sources do not provide a rank for the synonym, meaning that the unranked synonym and an
ranked accepted version of the same concept will have different name keys.
[Taxon concept resolution](#resolve-taxon-concept) requires the same concepts to be
grouped together so that the synonyms can be mapped onto accepted concepts if there is a higher-scoring taxonomy.
Is this flakey? Oh yes.
Needless to say, it would be much better if synonyms came with ranks.
However, some sources don't have that information.

The taxonomy is searched for unranked scientific names and (therefore) taxon concepts and taxon variants.
Each variant has a rank *estimated* for it, based on the resolver (see below).
The taxonomy is then searched for an existing taxon concept with the same (ranked) key.
If found, a ranked copy of the variant is created, added to the taxon concept and the taxon concept re-resolved.
A provenance statement is added to indicate the inferred ranking.
Unranked resolution can be applied to either all taxa, just synonyms or nothing on a per-provider basis.

For the ALA resolver, the resolver esitmates a rank by first looking for a unique ranked conmcept with, otherwise, the same
name key or a rank derived from parsing the name (binomials imply a species, for example).

##### Example

Suppose <ICZN, APROOPTA, Turner, 1919, species> has an accepted variant with a score of 700 and
<ICZN, APROOPTA, Turner, 1919, unranked> has a synonym variant with a score of 2000.
After unranked resolution, both variants will be assigned to <ICZN, APROOPTA, Turner, 1919, species>
with the synonym as the primary for resolution.

#### Resolve Principals

Once taxon concepts have been resolved, principals are resolved up the taxon concept, scientific name, 
unranked name, bare name hierarchy.
Each level depends on the results from the previous level.
Principals are chosen by examining the sub-elements contained, looking for the "most suitable" element.

* Scientific names search for formally defined taxon concepts, preferably accepted and authored, and
  then chooses the taxon concept with a variant that has the highest score.
* Unranked names search for ranked scientific names, choosing the name with a principal with the highest score.
* Bare names also prefer unranked names with a ranked principal, choosing the highest score.

In all cases, the cutoff score is in effect, removing elements of low quality.
If there is more than one candidate for a principal, one is chosen abitrarily, and the choice logged.

#### Resolve Discards

Discarded taxa are elements with the *forbidden* flag set.
This can either be due to configuration rules, because the LTC has made another copy of the taxon or because 
the LTC has eliminated the taxon due to a serious problem.
As an example, configuration rules may forbid taxa with unplaced or doubtful names.

Discards are processed by per-provider rules.
The three possible strategies are:

* Simply ignore the forbidden element and eliminate it from the output.
* Attach the identifier of the forbdden element to its parent.
  This strategy allows taxa to be searched for by identifier with the result being the parent.
* Make the forbidden element an inferred synonym of its parent.
  This strategy allows the name to be matched to a higher taxon.
  
#### Resolve Vernacular Names

Vernacular names are alternative, non-scientific names for taxon concepts.
Vernacular names that are supplied as part of a taxonomy DwCA are connected to a taxon concept and can
be directly resolved by taxon identifier.

Vernacular names that are supplied a stand-alone name lists are usually connected to a scientific name,
possibly with some higher-order taxonomic terms such as kingdom or class attached.
These lists may use synonyms or out of date taxonomy.
These names are resolved by building an interim name matching index.
The scientific names are then matched against the index and the vernacular names allocated to taxon identifiers.

### Output

Output is in DwCA form.
The DwCA contains the following elements:

* **eml.xml** Metadata describing the taxonomy. This includes information about the source taxonomies.
* **meta.xml** A schema description of the archive
* **taxon.txt** A tab-separated table of the taxonomy, containing the principal taxon variants.
  The parent and accepted name identifiers are translated to be correct for the resolution and principal taxa.
  This may involve tracing chains of synonyms or linking to parents of parents.
* **taxonvariant.txt** A tab-separated table of all taxon variants, linked to the principal taxon.
  This table contains all the available input data, other than forbidden taxa and can be used to
  link a taxon to alternative representations and spellings.
* **identifier.txt** A tab-separated list of identifiers for each taxon, including alternative identifiers.
* **vernacularname.txt** A tab-separated list of vernacular names, linked to taxa.
* **rightsholder.txt** A tab-separated list of dataset identifiers, names, rights and licencing information.
* **distribution.txt** A tab-separated list of distribution statements, linked to taxa.

In theory, the LTC should be able to eat its own tail and an output DwCA fed back into the LTC.
This has not been tried, due to concerns about putting out the Sun.

## Glossary

* **cutoff score** A point below which taxa are not considered to be of sufficient quality to act as principals
* **Darwin Core (DwC)** The standard set of terms used to describe taxonomic data http://rs.tdwg.org/dwc/terms/
* **Darwin Core Archive (DwCA)** A standard structure for a dataset in the form of DwC terms, with related elements.
* **forbidden** A taxon excluded from the final taxonomy.
* **inferred** A status chosen by the LTC to resolve problems or indicate a choice made on partial data.
* **least upper bound (lub)** For two taxon concepts, the lowest ranking parent that both of them share.
* **lucene** Indexing software that allows a collection of documents to be built that can then be queried.
* **primary concept** The taxon chosen to represent a particular taxonomic element.
* **provider** A single source of taxonomic information (eg. the Australian Faunal Directory)
* **score** An integer describing the quality of a taxon variant (and by extension any containing taxonomic elements).
* **taxon concept** The placement of a taxon in a taxonomy: this thing, described by this name, goes here.
* **taxon element** A generic taxonomic element: a taxon concept, scientific name, unranked name or bare name.
* **taxon variant** A taxon concept from a specific source. Within the LTC, a taxon concept can have multiple variants.
  (In code, taxon variants are called TaxonConceptInstance, a historical artefact.)
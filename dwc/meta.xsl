<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://rs.tdwg.org/dwc/text/">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
    
    <xsl:template match="/metadata">
        <archive metadata="eml.xml">
            <xsl:apply-templates select="structure/*"/>
        </archive>
    </xsl:template>


    <xsl:template match="taxon">
        <core encoding="UTF-8" fieldsTerminatedBy="," linesTerminatedBy="\n" fieldsEnclosedBy="&quot;" ignoreHeaderLines="1" rowType="http://rs.tdwg.org/dwc/terms/Taxon">
            <files>
                <location>taxon.csv</location>
            </files>
            <id index="0" />
            <xsl:choose>
                <xsl:when test="@parent = 'false'">
                    <field index="0" term="http://rs.tdwg.org/dwc/terms/taxonID"/>
                    <field index="1" term="http://rs.tdwg.org/dwc/terms/parentNameUsageID"/>
                    <field index="2" term="http://rs.tdwg.org/dwc/terms/acceptedNameUsageID"/>
                    <field index="3" term="http://rs.tdwg.org/dwc/terms/datasetID"/>
                    <field index="4" term="http://rs.tdwg.org/dwc/terms/datasetName"/>
                    <field index="5" term="http://rs.tdwg.org/dwc/terms/nomenclaturalCode"/>
                    <field index="6" term="http://rs.tdwg.org/dwc/terms/scientificName"/>
                    <field index="7" term="http://rs.tdwg.org/dwc/terms/scientificNameAuthorship"/>
                    <field index="8" term="http://rs.tdwg.org/dwc/terms/kingdom"/>
                    <field index="9" term="http://rs.tdwg.org/dwc/terms/phylum"/>
                    <field index="10" term="http://ala.org.au/terms/1.0/subphylum"/>
                    <field index="11" term="http://rs.tdwg.org/dwc/terms/class"/>
                    <field index="12" term="http://ala.org.au/terms/1.0/subclass"/>
                    <field index="13" term="http://rs.tdwg.org/dwc/terms/order"/>
                    <field index="14" term="http://ala.org.au/terms/1.0/suborder"/>
                    <field index="15" term="http://ala.org.au/terms/1.0/infraorder"/>
                    <field index="16" term="http://rs.tdwg.org/dwc/terms/family"/>
                    <field index="17" term="http://rs.tdwg.org/dwc/terms/genus"/>
                    <field index="18" term="http://rs.tdwg.org/dwc/terms/subgenus"/>
                    <field index="19" term="http://rs.tdwg.org/dwc/terms/specificEpithet"/>
                    <field index="20" term="http://rs.tdwg.org/dwc/terms/infraspecificEpithet"/>
                    <field index="21" term="http://rs.tdwg.org/dwc/terms/taxonRank"/>
                    <field index="22" term="http://rs.tdwg.org/dwc/terms/taxonConceptID"/>
                    <field index="23" term="http://rs.tdwg.org/dwc/terms/scientificNameID"/>
                    <field index="24" term="http://rs.tdwg.org/dwc/terms/taxonomicStatus"/>
                    <field index="25" term="http://rs.tdwg.org/dwc/terms/nomenclaturalStatus"/>
                    <field index="26" term="http://rs.tdwg.org/dwc/terms/establishmentMeans"/>
                    <field index="27" term="http://rs.tdwg.org/dwc/terms/nameAccordingToID"/>
                    <field index="28" term="http://rs.tdwg.org/dwc/terms/nameAccordingTo"/>
                    <field index="29" term="http://rs.tdwg.org/dwc/terms/namePublishedInID"/>
                    <field index="30" term="http://rs.tdwg.org/dwc/terms/namePublishedIn"/>
                    <field index="31" term="http://rs.tdwg.org/dwc/terms/namePublishedInYear"/>
                    <field index="32" term="http://ala.org.au/terms/1.0/nameComplete"/>
                    <field index="33" term="http://ala.org.au/terms/1.0/nameFormatted"/>
                    <field index="34" term="http://rs.tdwg.org/dwc/terms/taxonRemarks"/>
                    <field index="35" term="http://purl.org/dc/terms/provenance"/>
                    <field index="36" term="http://purl.org/dc/terms/source"/>
                </xsl:when>
                <xsl:otherwise>
                    <field index="0" term="http://rs.tdwg.org/dwc/terms/taxonID"/>
                    <field index="1" term="http://rs.tdwg.org/dwc/terms/parentNameUsageID"/>
                    <field index="2" term="http://rs.tdwg.org/dwc/terms/acceptedNameUsageID"/>
                    <field index="3" term="http://rs.tdwg.org/dwc/terms/datasetID"/>
                    <field index="4" term="http://rs.tdwg.org/dwc/terms/nomenclaturalCode"/>
                    <field index="5" term="http://rs.tdwg.org/dwc/terms/scientificName"/>
                    <field index="6" term="http://rs.tdwg.org/dwc/terms/scientificNameAuthorship"/>
                    <field index="7" term="http://rs.tdwg.org/dwc/terms/taxonRank"/>
                    <field index="8" term="http://rs.tdwg.org/dwc/terms/taxonConceptID"/>
                    <field index="9" term="http://rs.tdwg.org/dwc/terms/scientificNameID"/>
                    <field index="10" term="http://rs.tdwg.org/dwc/terms/taxonomicStatus"/>
                    <field index="11" term="http://rs.tdwg.org/dwc/terms/nomenclaturalStatus"/>
                    <field index="12" term="http://rs.tdwg.org/dwc/terms/establishmentMeans"/>
                    <field index="13" term="http://rs.tdwg.org/dwc/terms/nameAccordingToID"/>
                    <field index="14" term="http://rs.tdwg.org/dwc/terms/nameAccordingTo"/>
                    <field index="15" term="http://rs.tdwg.org/dwc/terms/namePublishedInID"/>
                    <field index="16" term="http://rs.tdwg.org/dwc/terms/namePublishedIn"/>
                    <field index="17" term="http://rs.tdwg.org/dwc/terms/namePublishedInYear"/>
                    <field index="18" term="http://ala.org.au/terms/1.0/nameComplete"/>
                    <field index="19" term="http://ala.org.au/terms/1.0/nameFormatted"/>
                    <field index="20" term="http://purl.org/dc/terms/source"/>
                </xsl:otherwise>
            </xsl:choose>
        </core>
    </xsl:template>

    <xsl:template match="vernacular">
        <extension encoding="UTF-8" fieldsTerminatedBy="," linesTerminatedBy="\n" fieldsEnclosedBy="&quot;" ignoreHeaderLines="1" rowType="http://rs.gbif.org/terms/1.0/VernacularName">
            <files>
                <location>vernacularNames.csv</location>
            </files>
            <coreid index="0" />
            <field index="0" term="http://rs.tdwg.org/dwc/terms/taxonID"/>
            <field index="1" term="http://ala.org.au/terms/1.0/nameID"/>
            <field index="2" term="http://rs.tdwg.org/dwc/terms/datasetID"/>
            <field index="3" term="http://rs.tdwg.org/dwc/terms/vernacularName"/>
            <field index="4" term="http://http://ala.org.au/terms/1.0/status"/>
            <field index="5" term="http://purl.org/dc/terms/language"/>
            <field index="6" term="http://purl.org/dc/terms/source"/>
            <field index="7" term="http://purl.org/dc/terms/temporal"/>
            <field index="8" term="http://rs.tdwg.org/dwc/terms/locationID"/>
            <field index="9" term="http://rs.tdwg.org/dwc/terms/locality"/>
            <field index="10" term="http://rs.tdwg.org/dwc/terms/countryCode"/>
            <field index="11" term="http://rs.tdwg.org/dwc/terms/sex"/>
            <field index="12" term="http://rs.tdwg.org/dwc/terms/lifeStage"/>
            <field index="13" term="http://rs.gbif.org/terms/1.0/isPlural"/>
            <field index="14" term="http://rs.gbif.org/terms/1.0/isPreferredName"/>
            <field index="15" term="http://rs.gbif.org/terms/1.0/organismPart"/>
            <field index="16" term="http://ala.org.au/terms/1.0/labels"/>
            <field index="17" term="http://rs.tdwg.org/dwc/terms/taxonRemarks"/>
            <field index="18" term="http://purl.org/dc/terms/provenance"/>
        </extension>
    </xsl:template>

    <xsl:template match="identfier">
        <extension encoding="UTF-8" fieldsTerminatedBy="," linesTerminatedBy="\n" fieldsEnclosedBy="&quot;" ignoreHeaderLines="1" rowType="http://rs.gbif.org/terms/1.0/Identifier">
            <files>
                <location>identifiers.csv</location>
            </files>
            <coreid index="0" />
            <field index="0" term="http://rs.tdwg.org/dwc/terms/taxonID"/>
            <field index="1" term="http://purl.org/dc/terms/identifier"/>
            <field index="2" term="http://purl.org/dc/terms/title"/>
            <field index="3" term="http://purl.org/dc/terms/subject"/>
            <field index="4" term="http://purl.org/dc/terms/format"/>
            <field index="5" term="http://rs.tdwg.org/dwc/terms/datasetID"/>
            <field index="6" term="http://purl.org/dc/terms/source"/>
            <field index="7" term="http://ala.org.au/terms/1.0/status"/>
        </extension>
    </xsl:template>

</xsl:stylesheet>
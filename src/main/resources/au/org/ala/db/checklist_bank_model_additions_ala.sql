-- inserting some missing ranks so that all the exported values will have a valid (?) rank
-- not 100% sure about some of the mappings.
-- 575 infrageneric
-- 725 subvariety
-- 825 cultivar
-- 875 unranked
-- 900 supergenericname

DELETE FROM term_gbif_portal_rank WHERE term_fk in (575, 725, 825, 875, 900);

INSERT INTO term_gbif_portal_rank (term_fk, portal_rank) VALUES 
(575, 6925), 
(725,8015), 
(825,8050), 
(875, 0), 
(900,8200) 
;

-- Create the view that is necessary to export the taxon names in the format that ALA needs
--WHEN sci_pn.is_hybrid_formula = true THEN 1 (necessary for the old CB repository)
--WHEN sci.type = 5 THEN 1 (for the new)
CREATE OR REPLACE VIEW export_ala_taxon_name AS
 SELECT COALESCE(can.id, sci.id) AS id, COALESCE(can.scientific_name, sci.scientific_name) AS canonical,
        
        CASE
            WHEN tr.portal_rank < 6000 THEN sci_pn.monomial
            ELSE NULL::character varying
        END AS supra_generic,
        CASE
            WHEN tr.portal_rank >= 6000 THEN sci_pn.monomial
            ELSE NULL::character varying
        END AS generic, NULL::text AS infrageneric, sci_pn.specific_epithet, sci_pn.infra_specific_epithet AS infraspecific, NULL::text AS infraspecific_marker,
        CASE
            WHEN sci.type = 5 THEN 1 
            ELSE 0
        END AS is_hybrid, tr.portal_rank AS rank, sci_pn.authorship AS author, NULL::unknown AS searchable_canonical
   FROM name_usage nu
   JOIN name_string sci ON nu.name_fk = sci.id
   LEFT JOIN name_string can ON sci.canonical_name_fk = can.id
   LEFT JOIN parsed_name sci_pn ON sci_pn.name_fk = sci.id
   LEFT JOIN term_gbif_portal_rank tr ON nu.rank_fk = tr.term_fk
  WHERE nu.checklist_fk = 1
  GROUP BY 1,2,3,4,5,6,7,8,9,10,11
  ORDER BY COALESCE(can.scientific_name, sci.scientific_name), tr.portal_rank, sci_pn.authorship;

--create the view used for the taxon_concepts
--remove all the parent_fks and kingdom_fks that refer back to the "incertae sedis" record ie id=9.

CREATE OR REPLACE VIEW ala_dwc_classification AS
 SELECT u.id AS id, u.name_fk,COALESCE(n.canonical_name_fk, n.id) as can_id, COALESCE(nc.scientific_name, n.scientific_name) AS name, u.lexical_group_fk,  u.lft AS lft, u.rgt AS rgt, (COALESCE(np.authorship, ''::character varying)::text ||
        CASE
            WHEN np.year IS NOT NULL THEN ', '::text || np.year::text
            ELSE ''::text
        END) ||
        CASE
            WHEN np.authorship_basionym IS NOT NULL OR np.year_basionym IS NOT NULL THEN (' ('::text || COALESCE((np.authorship_basionym::text || ', '::text) || np.year_basionym::text, np.authorship_basionym::text, np.year_basionym::text)) || ')'::text
            ELSE ''::text
        END AS authorship, case u.parent_fk when 9 then null else u.parent_fk end, u.is_synonym, u.rank_fk, r.term as rank, case u.kingdom_fk when 9 then null else u.kingdom_fk end, knc.scientific_name AS kingdom, u.phylum_fk, COALESCE(pnc.scientific_name, pn.scientific_name) AS phylum, u.class_fk, COALESCE(cnc.scientific_name, cn.scientific_name) AS class, u.order_fk, COALESCE(onc.scientific_name, onn.scientific_name) AS "order", u.family_fk, COALESCE(fnc.scientific_name, fn.scientific_name) AS family, u.genus_fk, COALESCE(gnc.scientific_name, gn.scientific_name) AS genus, u.species_fk, COALESCE(snc.scientific_name, sn.scientific_name) AS species
   FROM name_usage u
   LEFT JOIN name_string n ON u.name_fk = n.id
   LEFT JOIN name_string nc ON n.canonical_name_fk = nc.id
   LEFT JOIN parsed_name np ON np.name_fk = n.id
   LEFT JOIN term r ON u.rank_fk = r.id
   LEFT JOIN name_usage ku ON u.kingdom_fk = ku.id
   LEFT JOIN name_string kn ON ku.name_fk = kn.id
   LEFT JOIN name_string knc ON kn.canonical_name_fk = knc.id
   LEFT JOIN name_usage pu ON u.phylum_fk = pu.id
   LEFT JOIN name_string pn ON pu.name_fk = pn.id
   LEFT JOIN name_string pnc ON pn.canonical_name_fk = pnc.id
   LEFT JOIN name_usage cu ON u.class_fk = cu.id
   LEFT JOIN name_string cn ON cu.name_fk = cn.id
   LEFT JOIN name_string cnc ON cn.canonical_name_fk = cnc.id
   LEFT JOIN name_usage ou ON u.order_fk = ou.id
   LEFT JOIN name_string onn ON ou.name_fk = onn.id
   LEFT JOIN name_string onc ON onn.canonical_name_fk = onc.id
   LEFT JOIN name_usage fu ON u.family_fk = fu.id
   LEFT JOIN name_string fn ON fu.name_fk = fn.id
   LEFT JOIN name_string fnc ON fn.canonical_name_fk = fnc.id
   LEFT JOIN name_usage gu ON u.genus_fk = gu.id
   LEFT JOIN name_string gn ON gu.name_fk = gn.id
   LEFT JOIN name_string gnc ON gn.canonical_name_fk = gnc.id
   LEFT JOIN name_usage su ON u.species_fk = su.id
   LEFT JOIN name_string sn ON su.name_fk = sn.id
   LEFT JOIN name_string snc ON sn.canonical_name_fk = snc.id
   WHERE u.checklist_fk = 1;

--may need to materialise the view so that SELECT statements are performant Query returned successfully with no result in 3842271 ms.
drop table IF EXISTS tmp_export_name_usage;

create table tmp_export_name_usage AS SELECT * from ala_dwc_classification;
CREATE INDEX tmp_export_name_id_idx
  ON tmp_export_name_usage
  USING btree
  (id)
  WITH (FILLFACTOR=90);

--create a tmp table with index on lookup columns to improve the performance of the lsid identifier lookup
drop table IF EXISTS tmp_identifiers;

create table tmp_identifiers(
id serial NOT NULL,
lexical_group_fk integer,
name_fk integer,
identifier character varying(500),
checklist_fk integer
);
CREATE INDEX idx_tmp_ids_lg
  ON tmp_identifiers
  USING btree
  (lexical_group_fk, name_fk);

--insert the lsid type identifiers into the temporary identifiers table.  2636708 rows affected, 1107075 ms
--2622695 rows affected, 1129295 ms
--Query returned successfully: 2560234 rows affected, 1781306 ms execution time.
-- NC: Added a order by identifier so that the consistent LSIDs are reported when multiple LSIDs exist for one taxon
INSERT into tmp_identifiers (lexical_group_fk, name_fk, identifier,checklist_fk)
SELECT nu.lexical_group_fk, COALESCE(ns.canonical_name_fk, ns.id), i.identifier, nu.checklist_fk FROM identifier i JOIN name_usage nu ON i.usage_fk = nu.id JOIN name_string ns on nu.name_fk = ns.id where i.type_fk = 2001 ORDER BY CASE nu.checklist_fk WHEN 1001 THEN 1 WHEN 1002 THEN 2 WHEN 1003 THEN 3 ELSE 4 END, i.identifier;

--The SQL below identifies potential lexical groups that will have issues when the nub is genertaed
--The is specific to when 2 different ranks belong to the same lexical group eg Plecoptera is an ORDER and GENUS
-- select lg.id, count(distinct preferred_term_fk) from lexical_group lg join name_usage nu on lg.id = nu.lexical_group_fk join term r on nu.rank_fk=r.id group by lg.id having count(distinct preferred_term_fk)>1
--create the list of known homonyms.
--The list assumes that a genus name that appears more than once in genus table is a homonym
--It is better to use this statement to create a list of known homonyms just in case Tony has not updated the DUPLICATE_FLAG field.
select GENUS into outfile '/tmp/known_homonyms.txt' from MASTER_GENLIST GROUP BY GENUS having count(GENUS)>1;

--create the classification for all the Genus in IRMNG
--This will be used when trying to verify that a synonym has the correct higher classification
select case when UPPER(mf.KINGDOM) like '%UNALLOCATED%' then '' else mf.KINGDOM end,
case when UPPER(mf.PHYLUM) like '%UNALLOCATED%' then '' else mf.PHYLUM end,
case when UPPER(mf.CLASS) like '%UNALLOCATED%' then '' else mf.CLASS end ,
case when UPPER(mf.ORDERNAME) like '%UNALLOCATED%' then '' else mf.ORDERNAME end,
case when UPPER(mf.FAMILY) like '%UNALLOCATED%' then '' else mf.FAMILY end,
mg.GENUS, mg.GENUS_ID, IFNULL(mg.SYNONYM_FLAG, ''), IFNULL(mg.IS_SYN_OF_CODE, ''),
IFNULL(mg.IS_SYN_OF_NAME, ''),IFNULL( mg.DUPLICATE_FLAG,'')
into outfile '/tmp/irmng_classification.txt'
from MASTER_GENLIST mg JOIN MASTER_FAMLIST mf on mg.FAMILY_ID = mf.FAMILY_ID


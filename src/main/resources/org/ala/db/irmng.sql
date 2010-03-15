--create the list of known homonyms.
--The list assumes that a genus name that appears more than once in genus table is a homonym
--It is better to use this statement to create a list of known homonyms just in case Tony has not updated the DUPLICATE_FLAG field.
select GENUS into outfile '/data/irmng/known_homonyms.txt' from MASTER_GENLIST GROUP BY GENUS having count(GENUS)>1;


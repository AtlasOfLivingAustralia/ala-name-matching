--create the COL2010 DwC archive for use in Checklist Bank
-- This script needs to group by record id to prevent mulitle entries occurring when a taxa's name has multiple entries in the scientific_name table
--? What is the reason for having mulitple names??
--Query OK, 2424622 rows affected (23 min 52.22 sec)

select t.record_id ,ifnull(t.lsid,''), ifnull(replace(replace(t.name, '\n', ' '), '\r',''),'') , if(t.parent_id>0, cast(t.parent_id as CHAR), '') , ifnull(t.taxon,'') , ifnull(cast(accepted.record_id as CHAR),'') , ifnull(replace(replace(accepted.name, '\n', ' '), '\r', ''), ''), ifnull(replace(replace(name.author,'\n',' '), '\r', ''),''), ifnull(replace(replace(name.infraspecies,'\n', ' '), '\r', ''), '')
INTO OUTFILE '/data/checklistbank/rawdata/col2010/DarwinCore.txt' character set UTF8
from taxa t 
LEFT JOIN scientific_names name on t.name_code = name.name_code 
LEFT JOIN taxa accepted ON name.accepted_name_code = accepted.name_code and accepted.record_id <> t.record_id
group by t.record_id
order by t.record_id
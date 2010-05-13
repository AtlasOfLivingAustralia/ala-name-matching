--export the CoL common names for use in the name matching API
-- We are only interested in the names that are in English or have no assigned language
-- Mark the Australian common names so that they be given a higher rating
SELECT  cn.common_name, t.name, t.lsid,CASE WHEN cn.country='Australia' then 'T' ELSE '' END
INTO OUTFILE '/data/exports/col_common_names.txt'
FROM common_names cn
JOIN scientific_names sn ON cn.name_code = sn.name_code
JOIN taxa t ON sn.accepted_name_code = t.name_code
WHERE cn.language = 'English' or cn.language is null or cn.language='English;English' or cn.language =''
ORDER BY cn.record_id
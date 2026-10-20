-- avg_length_of_stay
SELECT * FROM ihms_reporting.rm_encounter_stats WHERE stat_date BETWEEN ?1 AND ?2

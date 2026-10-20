-- emergency_wait_time
SELECT * FROM ihms_reporting.rm_encounter_stats WHERE stat_date BETWEEN ?1 AND ?2

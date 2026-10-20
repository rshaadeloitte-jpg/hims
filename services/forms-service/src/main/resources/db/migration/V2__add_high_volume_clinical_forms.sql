-- ============================================================================
-- V2 — ihms_forms: 15 additional high-volume clinical forms (M40-2)
-- Ported from legacy tiuxml_mapping.json categories. Each form has a JSON
-- Schema (draft-07) tuned for the modern renderer (no legacy ui/helpText
-- custom metadata; just the JSON Schema contract).
--
-- Categories covered: Assessment (5), Progress (2), Charts (2), MLC (3),
-- Operative (1), Checklists (1), OBGYN/ANC (1).
--
-- 15 chosen for top clinical volume: PHYSICIAN ASSESSMENT, OBSTETRICS PROGRESS,
-- PEDIATRIC PROGRESS, MLC INJURY, MLC SEXUAL ASSAULT, MLC ALCOHOL/DRUG,
-- VASECTOMY NOTE, INTAKE/OUTPUT, FEEDING CHART, STROKE ICU CHART, ANC,
-- SURGICAL SAFETY CHECKLIST, IUD INSERTION, WOUND ASSESSMENT, FALL RISK
-- (MORSE FALL SCALE).
-- ============================================================================

SET search_path TO ihms_forms, public;

INSERT INTO ihms_forms.t_form_schema (code, name, specialty, description, schema_json, version, status, legacy_tiuxml_ref, added_by, added_datetime)
VALUES
(
    'PHYSICIAN_ASSESSMENT',
    'Physician Assessment Note',
    'GENERAL_MEDICINE',
    'Comprehensive physician assessment (ported from PHYSICIAN ASSESSMENT NOTE category). Identification, history, examination, advice.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Physician Assessment Note",
      "type": "object",
      "properties": {
        "chiefComplaint": {"type": "string", "title": "Chief Complaint", "minLength": 1},
        "historyOfPresentIllness": {"type": "string", "title": "History of Present Illness"},
        "pastMedicalHistory": {"type": "array", "title": "Past Medical History", "items": {"type": "string"}},
        "medications": {"type": "array", "title": "Current Medications", "items": {"type": "object", "properties": {
          "drug": {"type": "string"}, "dose": {"type": "string"}, "frequency": {"type": "string"}
        }}},
        "allergies": {"type": "array", "title": "Allergies", "items": {"type": "string"}},
        "vitalSigns": {"type": "object", "title": "Vital Signs", "properties": {
          "bp": {"type": "string"}, "pulse": {"type": "number"}, "temp": {"type": "number"}, "rr": {"type": "number"}, "spo2": {"type": "number"}
        }},
        "generalExamination": {"type": "string", "title": "General Examination"},
        "systemicExamination": {"type": "object", "title": "Systemic Examination", "properties": {
          "cvs": {"type": "string"}, "rs": {"type": "string"}, "abdomen": {"type": "string"}, "cns": {"type": "string"}
        }},
        "diagnosis": {"type": "array", "title": "Diagnosis", "items": {"type": "string"}},
        "investigationPlan": {"type": "array", "title": "Investigations", "items": {"type": "string"}},
        "treatmentPlan": {"type": "string", "title": "Treatment Plan"},
        "advice": {"type": "string", "title": "Advice"},
        "followUpDate": {"type": "string", "format": "date"}
      },
      "required": ["chiefComplaint", "diagnosis"]
    }'::jsonb,
    1, TRUE, 'ExaminationRISL.json+RISLHIST_V11.json', 1, NOW()
),
(
    'OBSTETRICS_PROGRESS',
    'Obstetrics Progress Note',
    'OBGYN',
    'Obstetric progress (ported from OBSTETRICS PROGRESS NOTE). Female-specific. Pregnancy details, fetal monitoring, plan.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Obstetrics Progress Note",
      "type": "object",
      "properties": {
        "gestationalAgeWeeks": {"type": "integer", "title": "GA (weeks)", "minimum": 1, "maximum": 45},
        "gravida": {"type": "integer", "title": "Gravida", "minimum": 0},
        "para": {"type": "integer", "title": "Para", "minimum": 0},
        "lmp": {"type": "string", "format": "date", "title": "LMP"},
        "edd": {"type": "string", "format": "date", "title": "EDD"},
        "fetalHeartRate": {"type": "integer", "title": "FHR (bpm)", "minimum": 50, "maximum": 220},
        "presentation": {"type": "string", "title": "Presentation", "enum": ["Cephalic", "Breech", "Transverse", "Other"]},
        "cervicalExamination": {"type": "string", "title": "Cervical Examination"},
        "uterineActivity": {"type": "string", "title": "Uterine Activity"},
        "complaints": {"type": "string", "title": "Current Complaints"},
        "examination": {"type": "string", "title": "Examination"},
        "summaryOfCare": {"type": "string", "title": "Summary of Care"},
        "plan": {"type": "string", "title": "Plan"}
      },
      "required": ["gestationalAgeWeeks", "fetalHeartRate"]
    }'::jsonb,
    1, TRUE, 'RISLOBSPROGEXAM_V10.json', 1, NOW()
),
(
    'PEDIATRIC_PROGRESS',
    'Pediatric Progress Note',
    'PEDIATRICS',
    'Pediatric progress (ported from PEDIATRIC PROGRESS NOTE). 0-10y age band, developmental milestones, immunizations.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Pediatric Progress Note",
      "type": "object",
      "properties": {
        "ageMonths": {"type": "integer", "title": "Age (months)", "minimum": 0, "maximum": 120},
        "weightKg": {"type": "number", "title": "Weight (kg)", "minimum": 0, "maximum": 100},
        "heightCm": {"type": "number", "title": "Height (cm)", "minimum": 0, "maximum": 200},
        "headCircumferenceCm": {"type": "number", "title": "Head circumference (cm)"},
        "feedingPattern": {"type": "string", "title": "Feeding Pattern", "enum": ["Exclusive breastfeeding", "Mixed feeding", "Formula", "Solid foods", "Other"]},
        "developmentalMilestones": {"type": "object", "title": "Developmental Milestones", "properties": {
          "socialSmile": {"type": "boolean", "title": "Social smile (2m)"},
          "headControl": {"type": "boolean", "title": "Head control (3-4m)"},
          "sitting": {"type": "boolean", "title": "Sitting (6-8m)"},
          "crawling": {"type": "boolean", "title": "Crawling (9-10m)"},
          "walking": {"type": "boolean", "title": "Walking (12-15m)"},
          "firstWords": {"type": "boolean", "title": "First words (12m)"},
          "twoWordPhrases": {"type": "boolean", "title": "Two-word phrases (24m)"}
        }},
        "immunizationsUpToDate": {"type": "boolean", "title": "Immunizations up-to-date"},
        "complaints": {"type": "string", "title": "Complaints"},
        "examination": {"type": "string", "title": "Examination"},
        "carePlan": {"type": "string", "title": "Care Plan"}
      },
      "required": ["ageMonths", "weightKg"]
    }'::jsonb,
    1, TRUE, 'RISLPEDSHISTV10.json+PEDS_EXAMINATIONPRORISL.json+PEDS_DEVELOPMENTAL_MILESTONES_new.json', 1, NOW()
),
(
    'INJURY_REPORT_MLC',
    'Injury Report (MLC)',
    'FORENSIC',
    'Medico-legal injury report (ported from INJURY REPORT — MLC). Wound description, weapon, time, severity.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Injury Report (MLC)",
      "type": "object",
      "properties": {
        "mlcNumber": {"type": "string", "title": "MLC Number"},
        "incidentDateTime": {"type": "string", "format": "date-time", "title": "Date/Time of Incident"},
        "placeOfIncident": {"type": "string", "title": "Place of Incident", "minLength": 1},
        "allegedWeapon": {"type": "string", "title": "Alleged Weapon/Means"},
        "broughtBy": {"type": "string", "title": "Brought By", "enum": ["Self", "Police", "Family", "Ambulance", "Other"]},
        "injuryDescription": {"type": "string", "title": "Injury Description", "minLength": 1},
        "injuryType": {"type": "string", "title": "Type of Injury", "enum": ["Abrasion", "Contusion", "Laceration", "Stab wound", "Gunshot", "Burn", "Fracture", "Other"]},
        "injuryLocation": {"type": "string", "title": "Body Location"},
        "estimatedAgeOfInjury": {"type": "string", "title": "Estimated Age of Injury"},
        "patientConscious": {"type": "boolean", "title": "Patient Conscious"},
        "alcoholSuspected": {"type": "boolean", "title": "Alcohol Suspected"},
        "policeInformed": {"type": "boolean", "title": "Police Informed"},
        "examiningDoctor": {"type": "string", "title": "Examining Doctor"},
        "remarks": {"type": "string", "title": "Remarks"}
      },
      "required": ["mlcNumber", "placeOfIncident", "injuryDescription", "injuryType"]
    }'::jsonb,
    1, TRUE, 'Injury_ReportV12.json', 1, NOW()
),
(
    'SEXUAL_ASSAULT_REPORT',
    'Sexual Assault Report (MLC)',
    'FORENSIC',
    'Medico-legal sexual assault examination (ported from POTENCY/SEX/ASSAULT REPORT — MLC). Sensitive — chain of custody.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Sexual Assault Report (MLC)",
      "type": "object",
      "properties": {
        "mlcNumber": {"type": "string", "title": "MLC Number"},
        "incidentDateTime": {"type": "string", "format": "date-time", "title": "Date/Time of Incident"},
        "consentForExamination": {"type": "boolean", "title": "Consent for Examination"},
        "policeReference": {"type": "string", "title": "Police Reference/FIR No."},
        "broughtBy": {"type": "string", "title": "Brought By"},
        "lastMenstrualPeriod": {"type": "string", "format": "date", "title": "LMP (if applicable)"},
        "generalExamination": {"type": "string", "title": "General Examination"},
        "localExamination": {"type": "string", "title": "Local Examination"},
        "samplesCollected": {"type": "array", "title": "Samples Collected", "items": {"type": "string", "enum": ["Vaginal swab", "Anal swab", "Oral swab", "Blood", "Urine", "Hair", "Clothing", "Other"]}},
        "injuriesFound": {"type": "string", "title": "Injuries Found"},
        "prophylaxisGiven": {"type": "string", "title": "Prophylaxis Given"},
        "opinion": {"type": "string", "title": "Doctor's Opinion"},
        "examiningDoctor": {"type": "string", "title": "Examining Doctor"}
      },
      "required": ["mlcNumber", "consentForExamination", "samplesCollected"]
    }'::jsonb,
    1, TRUE, 'Sexual_AssaultV12.json', 1, NOW()
),
(
    'ALCOHOL_DRUG_REPORT',
    'Alcohol/Drug Abuse Report (MLC)',
    'FORENSIC',
    'Alcohol/drug influence report (ported from ALCOHOL/DRUG ABUSE REPORT — MLC). BAC, intoxication level.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Alcohol/Drug Abuse Report (MLC)",
      "type": "object",
      "properties": {
        "mlcNumber": {"type": "string", "title": "MLC Number"},
        "incidentDateTime": {"type": "string", "format": "date-time", "title": "Date/Time of Incident"},
        "substance": {"type": "string", "title": "Suspected Substance", "enum": ["Alcohol", "Cannabis", "Opioids", "Cocaine", "Benzodiazepines", "Multiple", "Unknown"]},
        "breathAlcoholReading": {"type": "number", "title": "Breath Alcohol (mg/L)", "minimum": 0, "maximum": 1000},
        "bloodAlcoholReading": {"type": "number", "title": "Blood Alcohol (mg/100ml)", "minimum": 0, "maximum": 1000},
        "clinicalSigns": {"type": "array", "title": "Clinical Signs", "items": {"type": "string", "enum": ["Slurred speech", "Ataxia", "Disorientation", "Aggression", "Drowsiness", "Pinpoint pupils", "Track marks", "Other"]}},
        "gcsScore": {"type": "integer", "title": "GCS", "minimum": 3, "maximum": 15},
        "samplesCollected": {"type": "array", "title": "Samples", "items": {"type": "string"}},
        "opinion": {"type": "string", "title": "Doctor's Opinion"},
        "examiningDoctor": {"type": "string", "title": "Examining Doctor"}
      },
      "required": ["mlcNumber", "substance", "clinicalSigns"]
    }'::jsonb,
    1, TRUE, 'Alcohol_ConsumptionV13.json', 1, NOW()
),
(
    'VASECTOMY_NOTE',
    'Vasectomy Note',
    'OBGYN',
    'Male sterilization operative note (ported from VASECTOMY NOTE). M-specific, pre/intra/post op + discharge.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Vasectomy Note",
      "type": "object",
      "properties": {
        "procedureDate": {"type": "string", "format": "date", "title": "Procedure Date"},
        "surgeon": {"type": "string", "title": "Surgeon"},
        "anesthesia": {"type": "string", "title": "Anesthesia", "enum": ["Local", "General", "Spinal"]},
        "approach": {"type": "string", "title": "Approach", "enum": ["Conventional", "No-scalpel"]},
        "side": {"type": "string", "title": "Side", "enum": ["Right", "Left", "Bilateral"]},
        "findings": {"type": "string", "title": "Operative Findings"},
        "complications": {"type": "string", "title": "Complications"},
        "postOpInstructions": {"type": "string", "title": "Post-op Instructions"},
        "semenAnalysisAdvice": {"type": "string", "title": "Semen Analysis Advice (after 3 months)"},
        "followUpDate": {"type": "string", "format": "date"}
      },
      "required": ["procedureDate", "surgeon", "approach"]
    }'::jsonb,
    1, TRUE, 'VASECTOMY_PROCEDURE_V10.json+VASECTOMY_DISCHARGE_V11.json', 1, NOW()
),
(
    'INTAKE_OUTPUT_CHART',
    'Intake/Output Chart',
    'NURSING',
    'Hourly intake/output tracking (ported from INTAKE/OUTPUT chart pattern). Fluids IV/oral, output urine/drain.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Intake/Output Chart",
      "type": "object",
      "properties": {
        "chartDate": {"type": "string", "format": "date", "title": "Chart Date"},
        "shift": {"type": "string", "title": "Shift", "enum": ["Morning", "Evening", "Night"]},
        "intake": {"type": "array", "title": "Intake entries", "items": {"type": "object", "properties": {
          "hour": {"type": "string", "pattern": "^[0-2][0-9]:00$"},
          "type": {"type": "string", "enum": ["IV fluid", "Oral", "NG feed", "Blood product", "Other"]},
          "fluid": {"type": "string"},
          "amountMl": {"type": "number", "minimum": 0}
        }}},
        "output": {"type": "array", "title": "Output entries", "items": {"type": "object", "properties": {
          "hour": {"type": "string", "pattern": "^[0-2][0-9]:00$"},
          "type": {"type": "string", "enum": ["Urine", "Drain", "Vomit", "Stool", "NG aspirate", "Other"]},
          "amountMl": {"type": "number", "minimum": 0}
        }}},
        "totalIntakeMl": {"type": "number", "title": "Total Intake (ml)"},
        "totalOutputMl": {"type": "number", "title": "Total Output (ml)"},
        "balance": {"type": "number", "title": "Balance (ml)"},
        "nurseSignature": {"type": "string", "title": "Nurse Signature"}
      },
      "required": ["chartDate", "shift"]
    }'::jsonb,
    1, TRUE, 'Intake_Output_V10.json', 1, NOW()
),
(
    'FEEDING_CHART',
    'Feeding Chart',
    'NURSING',
    'Patient feeding chart (ported from FEEDING CHART). Diet, amount taken, refusal, supplement.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Feeding Chart",
      "type": "object",
      "properties": {
        "chartDate": {"type": "string", "format": "date", "title": "Chart Date"},
        "meals": {"type": "array", "title": "Meals", "items": {"type": "object", "properties": {
          "mealType": {"type": "string", "enum": ["Breakfast", "Mid-morning", "Lunch", "Evening snack", "Dinner", "Late night"]},
          "dietType": {"type": "string", "enum": ["Normal", "Soft", "Liquid", "Diabetic", "Renal", "Cardiac", "Tube feed", "Other"]},
          "amountOffered": {"type": "string", "enum": ["Full", "Half", "Quarter", "Nil"]},
          "amountConsumed": {"type": "string", "enum": ["Full", "Half", "Quarter", "Nil"]},
          "refusalReason": {"type": "string"},
          "supplementGiven": {"type": "boolean"}
        }}},
        "totalFluidIntakeMl": {"type": "number", "title": "Total Fluid (ml)"},
        "remarks": {"type": "string", "title": "Remarks"}
      },
      "required": ["chartDate"]
    }'::jsonb,
    1, TRUE, 'ODCWF_FEEDING_CHART.json', 1, NOW()
),
(
    'STROKE_ICU_CHART',
    'Stroke ICU Chart',
    'NEUROLOGY',
    'Stroke ICU monitoring (ported from STROKE ICU CHART). GCS, NIHSS, vitals, neuro checks.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Stroke ICU Chart",
      "type": "object",
      "properties": {
        "chartDate": {"type": "string", "format": "date", "title": "Chart Date"},
        "gcsScore": {"type": "integer", "title": "GCS", "minimum": 3, "maximum": 15},
        "gcsEye": {"type": "integer", "minimum": 1, "maximum": 4},
        "gcsVerbal": {"type": "integer", "minimum": 1, "maximum": 5},
        "gcsMotor": {"type": "integer", "minimum": 1, "maximum": 6},
        "nihssScore": {"type": "integer", "title": "NIHSS", "minimum": 0, "maximum": 42},
        "pupils": {"type": "object", "title": "Pupils", "properties": {
          "rightSize": {"type": "integer", "minimum": 1, "maximum": 8},
          "rightReactive": {"type": "boolean"},
          "leftSize": {"type": "integer", "minimum": 1, "maximum": 8},
          "leftReactive": {"type": "boolean"}
        }},
        "limbStrength": {"type": "object", "title": "Limb strength (MRC 0-5)", "properties": {
          "rightUpper": {"type": "integer", "minimum": 0, "maximum": 5},
          "rightLower": {"type": "integer", "minimum": 0, "maximum": 5},
          "leftUpper": {"type": "integer", "minimum": 0, "maximum": 5},
          "leftLower": {"type": "integer", "minimum": 0, "maximum": 5}
        }},
        "bloodPressure": {"type": "string", "title": "BP"},
        "heartRate": {"type": "integer", "title": "HR"},
        "spo2": {"type": "number", "title": "SpO2"},
        "remarks": {"type": "string", "title": "Remarks"}
      },
      "required": ["chartDate", "gcsScore"]
    }'::jsonb,
    1, TRUE, 'ODCWF_STROKE_ICU_CHART.json', 1, NOW()
),
(
    'ANC_VISIT',
    'ANC Visit Record',
    'OBGYN',
    'Antenatal care visit (ported from ANC_Hist). Pregnancy dating, risk factors, exam, plan.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "ANC Visit Record",
      "type": "object",
      "properties": {
        "visitDate": {"type": "string", "format": "date", "title": "Visit Date"},
        "gestationalAgeWeeks": {"type": "integer", "title": "GA (weeks)", "minimum": 1, "maximum": 45},
        "lmp": {"type": "string", "format": "date", "title": "LMP"},
        "edd": {"type": "string", "format": "date", "title": "EDD"},
        "gravida": {"type": "integer", "title": "Gravida", "minimum": 1},
        "para": {"type": "integer", "title": "Para", "minimum": 0},
        "abortions": {"type": "integer", "title": "Abortions", "minimum": 0},
        "livingChildren": {"type": "integer", "title": "Living Children", "minimum": 0},
        "previousComplications": {"type": "string", "title": "Previous Pregnancy Complications"},
        "highRiskFactors": {"type": "array", "title": "High-risk Factors", "items": {"type": "string", "enum": ["GDM", "PIH", "APH", "Anemia", "BOH", "Previous C-section", "Multiple pregnancy", "Other"]}},
        "weight": {"type": "number", "title": "Weight (kg)"},
        "bloodPressure": {"type": "string", "title": "BP"},
        "fundalHeightWeeks": {"type": "integer", "title": "Fundal Height (weeks)"},
        "fetalHeartRate": {"type": "integer", "title": "FHR"},
        "presentation": {"type": "string", "title": "Presentation"},
        "edema": {"type": "boolean", "title": "Edema"},
        "urineProtein": {"type": "string", "title": "Urine Protein", "enum": ["Nil", "Trace", "+", "++", "+++"]},
        "hemoglobin": {"type": "number", "title": "Hb (g/dl)"},
        "nextVisitDate": {"type": "string", "format": "date"}
      },
      "required": ["visitDate", "gestationalAgeWeeks"]
    }'::jsonb,
    1, TRUE, 'ANC_Hist.json', 1, NOW()
),
(
    'WHO_SURGICAL_SAFETY_CHECKLIST',
    'WHO Surgical Safety Checklist',
    'SURGERY',
    'WHO surgical safety checklist (ported from VASECTOMY_CHECKLIST + similar OT safety forms). Sign-in, time-out, sign-out.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "WHO Surgical Safety Checklist",
      "type": "object",
      "properties": {
        "procedureDate": {"type": "string", "format": "date", "title": "Procedure Date"},
        "patientIdentityConfirmed": {"type": "boolean", "title": "Patient identity, site, procedure, consent confirmed"},
        "siteMarked": {"type": "boolean", "title": "Site marked"},
        "anesthesiaSafetyCheckCompleted": {"type": "boolean", "title": "Anesthesia safety check completed"},
        "pulseOximeterOnPatient": {"type": "boolean", "title": "Pulse oximeter on patient"},
        "allergyKnown": {"type": "boolean", "title": "Allergy known"},
        "difficultAirwayAspirationRisk": {"type": "boolean", "title": "Difficult airway / aspiration risk"},
        "bloodLossRisk": {"type": "boolean", "title": "Risk of >500ml blood loss"},
        "antibioticProphylaxisGiven": {"type": "boolean", "title": "Antibiotic prophylaxis given (last 60 min)"},
        "essentialImagingDisplayed": {"type": "boolean", "title": "Essential imaging displayed"},
        "teamIntroduced": {"type": "boolean", "title": "All team members introduced by name/role"},
        "surgeonAnesthetistNurseConfirmed": {"type": "boolean", "title": "Surgeon, anesthetist, nurse confirm patient/procedure/site"},
        "anticipatedCriticalEvents": {"type": "string", "title": "Anticipated critical events"},
        "sterilityConfirmed": {"type": "boolean", "title": "Sterility confirmed"},
        "equipmentIssues": {"type": "string", "title": "Equipment issues"},
        "specimenLabeled": {"type": "boolean", "title": "Specimen labeled (incl. patient name)"},
        "equipmentProblems": {"type": "string", "title": "Equipment problems to be addressed"},
        "surgeonReview": {"type": "string", "title": "Surgeon: key concerns for recovery"},
        "anesthetistReview": {"type": "string", "title": "Anesthetist: key concerns for recovery"},
        "nurseReview": {"type": "string", "title": "Nurse: key concerns for recovery"},
        "signOutCompletedBy": {"type": "string", "title": "Sign-out completed by"}
      },
      "required": ["procedureDate", "patientIdentityConfirmed", "teamIntroduced"]
    }'::jsonb,
    1, TRUE, 'VASECTOMY_CHECKLIST_V10.json+WHO_template', 1, NOW()
),
(
    'IUD_INSERTION_NOTE',
    'IUD Insertion Note',
    'OBGYN',
    'Intrauterine device insertion (ported from IUD INSERTION NOTE). Female-specific, pre/intra op + discharge.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "IUD Insertion Note",
      "type": "object",
      "properties": {
        "procedureDate": {"type": "string", "format": "date", "title": "Procedure Date"},
        "iudType": {"type": "string", "title": "IUD Type", "enum": ["Cu-T 380A", "Cu-T 375", "LNG-IUS (Mirena)", "Other"]},
        "provider": {"type": "string", "title": "Provider"},
        "consentObtained": {"type": "boolean", "title": "Consent Obtained"},
        "pelvicExamination": {"type": "string", "title": "Pelvic Examination Findings"},
        "uterineDepth": {"type": "number", "title": "Uterine Depth (cm)"},
        "complications": {"type": "string", "title": "Complications"},
        "stringsTrimmed": {"type": "boolean", "title": "Strings Trimmed"},
        "postInsertionInstructions": {"type": "string", "title": "Post-insertion Instructions"},
        "followUpDate": {"type": "string", "format": "date", "title": "Follow-up (4-6 weeks)"}
      },
      "required": ["procedureDate", "iudType", "consentObtained"]
    }'::jsonb,
    1, TRUE, 'IUDINSERTION_PROCEDUREV1_0.json', 1, NOW()
),
(
    'WOUND_ASSESSMENT',
    'Wound Assessment',
    'NURSING',
    'Wound/skin assessment (ported from Wound_Assessment + VIPS_Assessment). Braden-related wound documentation.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Wound Assessment",
      "type": "object",
      "properties": {
        "assessmentDate": {"type": "string", "format": "date", "title": "Assessment Date"},
        "woundType": {"type": "string", "title": "Wound Type", "enum": ["Pressure ulcer Stage I", "Pressure ulcer Stage II", "Pressure ulcer Stage III", "Pressure ulcer Stage IV", "Surgical", "Traumatic", "Diabetic", "Venous", "Arterial", "Other"]},
        "location": {"type": "string", "title": "Location", "minLength": 1},
        "sizeLengthCm": {"type": "number", "title": "Length (cm)", "minimum": 0},
        "sizeWidthCm": {"type": "number", "title": "Width (cm)", "minimum": 0},
        "sizeDepthCm": {"type": "number", "title": "Depth (cm)", "minimum": 0},
        "exudate": {"type": "string", "title": "Exudate", "enum": ["None", "Serous", "Sanguineous", "Serosanguineous", "Purulent"]},
        "exudateAmount": {"type": "string", "title": "Exudate Amount", "enum": ["None", "Scant", "Moderate", "Heavy"]},
        "odor": {"type": "boolean", "title": "Odor Present"},
        "surroundingSkin": {"type": "string", "title": "Surrounding Skin"},
        "pain": {"type": "integer", "title": "Pain (0-10)", "minimum": 0, "maximum": 10},
        "dressingApplied": {"type": "string", "title": "Dressing Applied"},
        "photograph": {"type": "boolean", "title": "Photograph Taken"},
        "nextAssessmentDate": {"type": "string", "format": "date"},
        "nurseName": {"type": "string", "title": "Nurse"}
      },
      "required": ["assessmentDate", "woundType", "location"]
    }'::jsonb,
    1, TRUE, 'Wound_Assessment.json+VIPS_Assessment.json', 1, NOW()
),
(
    'MORSE_FALL_SCALE',
    'Morse Fall Scale',
    'NURSING',
    'Fall risk assessment (ported from Morse Fall Scale items in NURSINGINITIALASSESSMENTfinal1). 6-item scale, score >45 high risk.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Morse Fall Scale",
      "type": "object",
      "properties": {
        "assessmentDate": {"type": "string", "format": "date", "title": "Assessment Date"},
        "historyOfFalling": {"type": "integer", "title": "1. History of falling (0=No, 25=Yes)", "enum": [0, 25]},
        "secondaryDiagnosis": {"type": "integer", "title": "2. Secondary diagnosis (0=No, 15=Yes)", "enum": [0, 15]},
        "ambulatoryAid": {"type": "integer", "title": "3. Ambulatory aid (0=None/Bedrest/Nurse assist, 15=Crutches/Cane/Walker, 30=Furniture)"},
        "ivHeparinLock": {"type": "integer", "title": "4. IV/Heparin lock (0=No, 20=Yes)", "enum": [0, 20]},
        "gait": {"type": "integer", "title": "5. Gait (0=Normal/Bedrest/Wheelchair, 20=Weak, 30=Impaired)"},
        "mentalStatus": {"type": "integer", "title": "6. Mental status (0=Oriented to own ability, 15=Forgets limitations)"},
        "totalScore": {"type": "integer", "title": "Total Morse Fall Scale Score", "minimum": 0, "maximum": 125},
        "riskLevel": {"type": "string", "title": "Risk Level", "enum": ["Low (0-24)", "Moderate (25-44)", "High (45+)"]},
        "interventions": {"type": "array", "title": "Interventions Initiated", "items": {"type": "string", "enum": ["Yellow band", "Bed in low position", "Call bell in reach", "Non-slip footwear", "Frequent rounding", "Family education", "Bedside commode", "Restraints", "Other"]}},
        "nurseName": {"type": "string"}
      },
      "required": ["assessmentDate", "historyOfFalling", "totalScore"]
    }'::jsonb,
    1, TRUE, 'NURSINGINITIALASSESSMENTfinal1.json (Morse Fall subset)', 1, NOW()
)
ON CONFLICT (code, version) DO NOTHING;

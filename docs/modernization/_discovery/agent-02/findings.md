# Agent 02 — HMIS Business Domain Discovery: Evidence Dump

Source of truth: `D:\ihms` (Spring Boot 2.7.5, Java 8, EclipseLink JPA, CXF JAX-RS, FAUG jQuery frontend).
Scope: derive business modules ONLY from codebase artifacts. Research-only; no files in `D:\ihms` were modified.

Date: 2026-08-14

---

## 1. Evidence Inventory (what was enumerated)

| Evidence source | Path | Count |
|---|---|---|
| Frontend configs | `src/main/resources/public/fjs-config_*.json` | 42 files (40 module configs + schemas/templates registry) |
| Module dirs (schemas+templates) | `src/main/resources/public/modules/*` | 40 dirs (+ 24 masters sub-dirs) |
| JS controllers | `src/main/resources/public/js/*-module.js` | 40 files |
| PAS service impls | `src/main/java/com/ohum/services/pas/*ServiceImpl.java` | 55 files |
| Custom service impls | `src/main/java/com/ohum/services/custom/*ServiceImpl.java` | 34 files (incl. helpers) |
| Other service impls (root+integration dirs) | `services/*`, `services/{aadhar,aasmaan,abdm,cssd,ehr,ejs,nurse,patp,payment}` | ~40 files |
| Master entities | `models/masters/*.java` | 609 |
| Transactional entities | `models/transactionals/*.java` | 137 |
| Other model pkgs | `models/{aasmaan,ejs,trx,txt,rcm,chiranjeevi,hcx,statistical}` | ~123 |
| JSON form schemas | `modules/*/schemas/*.json` | 1308 |
| HTML form templates | `modules/*/templates/*.html` | ~1500 |
| Print templates (Velocity) | `templates/*.vm` (+ nhcx, risl subdirs) | ~180 |
| Misc configs | `mastersConfigration.json`, `vitalsConfiguration.json`, `tiuxml_mapping.json`, `chemo_tiuxml_mapping.json`, `nhnc_tiuxml_mapping.json`, `op_tiuxml_mapping.json`, `notesTitle.json` | 7 |
| FHIR/ABDM layer | `com/ohum/xcg/**` (abdm server+client, genserv FHIR R4, hcx dto/helpers) | ~30 |

---

## 2. fjs-config → schema-key inventory (configs are the definitive module map)

Config file → number of JSON schema keys exposed:

| Config | Keys | Config | Keys |
|---|---|---|---|
| fjs-config_masters | 330 | fjs-config_equipments | 34 |
| fjs-config_tiuxmls | 368 | fjs-config_nursing | 28 |
| fjs-config_billing | 77 | fjs-config_cssd | 15 |
| fjs-config_commons | 67 | fjs-config_ot | 14 |
| fjs-config_ip | 65 | fjs-config_bcma | 11 |
| fjs-config_pathology | 57 | fjs-config_queue | 11 |
| fjs-config_reports | 46 | fjs-config_transport | 11 |
| fjs-config_op | 43 | fjs-config_enquiry | 8 |
| fjs-config_bloodBank | 40 | fjs-config_rmrs | 9 |
| fjs-config_pharmacy | 39 | fjs-config_radiology | 9 |
| fjs-config_inventory | 70 | fjs-config_kiosk | 4 |
| fjs-config_chemotherapy | 3 | fjs-config_diet | 4 |
| fjs-config_allocation | 3 | fjs-config_grievance | 3 |
| fjs-config_paymentTransfer | 3 | fjs-config_pmrd | 2 |
| fjs-config_biomedicalwaste | 1 | fjs-config_worklist | 1 |
| fjs-config_nhcx | 1 | fjs-config_rcm | 1 |
| fjs-config_linenlaundry | 1 | fjs-config_fileManagement | 1 |
| fjs-config_biReport | 1 | fjs-config_patientManagement | 1 |
| fjs-config_careplan | 0 (templates-only) | fjs-config_charting | 0 (templates-only) |
| fjs-config_nursingWorklist | 0 (templates-only) | fjs-config_shifthandover | 0 (templates-only) |
| fjs-config_schemas | 0 (registry) | fjs-config_templates | 0 (registry) |

Example (fjs-config_op.json → modules/op/schemas/*.json):
AddMlcDetailsSchema, PatientDetailRegistrationDto, AddPatientRegistrationDto, Appointment-ListDto, Appointment-ListDtoDoctor, ConsultOrderList, DepartmentAppointmentSlots, DepartmentMasterDetailsListDto, DoctorAppointmentSlots, DoctorReferralSchema, DoctorMasterDetailsListDto, FamilyPlanningRegistration, FollowupDeptSchema, FollowUpVisitReqList, InvestigationSchema, UpdateAppointmentDto, WalkinVisitSchema, mlcDetailsSchema_Form, PatientHistorySchema, PatientMedicalRecordSchema, showPatientDetailSchema, showPatientHistorySchema, sponsorSchema, updateJanaadharSchema, updateMLCDetailsSchema, walkinVisitNhnc-schema, abhaPatientRegistration, baalAbhaReKyc, childAbhaPatientRegistration, searchAbhaPatient, addpatientDetailRegdto-schema, patientRghsPackageDetails, shiftMasterDetailsList, showDepartmentsApptSlots, showDepartmentWiseSlots, showDeptDoctorInformationSchema, showDoctorFutureAppointmentSchedule, showDoctorOpSchedule, minimal-updateAppointment_schema, minimal-walkin-appointment, questionnairePatientSchema, patientServiceDetailSchema, patientRecordSchema, doctorReferralSchema, followupDeptSchema, investigationSchema, family_Planning_Registration, emergencyRegType, mlcDetails, addMlcDetails, updateMLCDetails, patientHistory, patientMedicalRecord.

Full per-module schema key lists are recoverable from `fjs-config_*.json` files themselves (the mapping table in section 4 summarizes per-module schema inventory by folder).

---

## 3. Service → module mapping (class-level `@Path` evidence)

### com.ohum.services.pas (55 impls) — core PAS services

| Service impl | @Path | Module |
|---|---|---|
| PatientServiceImpl | /patient | Patient/OPD-IPD |
| VisitServiceImpl | /visit | Patient/Visits |
| AppointmentServiceImpl | /appointment | OPD/Appointments |
| DoctorServiceImpl | /doctor | OPD/Doctors & doctor share |
| IPDAdmissionServiceImpl | /ipdadmission | IPD Admission |
| IPDServiceImpl | /ipd | IPD |
| IPDConfigServiceImpl | /ipdconfig | Masters (ward/bed/room/class/diet plan) |
| BedServiceImpl | /bed | Bed management |
| BedReservationServiceImpl | /bedreservation | Bed management |
| BillServiceImpl | /bill | Billing |
| BillReceiptServiceImpl (custom) | /billReceipt | Billing |
| BillSummaryServiceImpl (custom) | /billSummary | Billing |
| BillDetServiceImpl (custom) | /billDet | Billing |
| BillCancellationServiceImpl (custom) | /cancelBill | Billing |
| AddPaymentServiceImpl (custom) | /addPayment | Billing/Payments |
| AddAdvanceServiceImpl (custom) | /addAdvance | Billing/Advance |
| AddRefundServiceImpl (custom) | /addRefund | Billing/Refund |
| AdvanceBillReceiptServiceImpl (custom) | /advancebillReceipt | Billing |
| AllReceiptsServiceImpl (custom) | /allReceipts | Billing |
| RefundBillReceiptServiceImpl (custom) | /refundBillReceipt | Billing |
| PatientBillEstimationServiceImpl | /patientbillestimation | Billing/Estimation |
| ChargeServiceImpl | /charge | Billing/Charges |
| PaymentServiceImpl | /payment | Billing/Payment |
| RefundServiceImpl | /refund | Billing/Refund |
| ConcessionTemplateServiceImpl | /concessiontemplate | Billing/Concessions |
| CompanyApprovedAmountServiceImpl | /companyapprovedamount | Billing/Company approved amounts |
| DoctorServiceImpl | /doctor | Billing/Doctor share |
| ServiceMasterServiceImpl | /servicemaster | Masters/Services |
| TariffServiceImpl | /tariff | Masters/Tariff |
| PlanMasterServiceImpl | /planmaster | Masters/Health plans |
| PackageServiceServiceImpl | /packageservice | Masters/Packages |
| ItemMasterServiceImpl | /itemmaster | Inventory/Item master |
| ItemCategoryServiceImpl | /itemcategory | Inventory/Item category |
| ItemStockServiceImpl | /itemstock | Inventory/Stock |
| ItemSalesServiceImpl | /itemsales | Pharmacy/Item sales |
| OpeningBalanceServiceImpl | /openingbalance | Inventory/Opening balance |
| MaterialConsumptionServiceImpl | /materialconsumption | Inventory/Consumption |
| PurchaseOrderServiceImpl | /purchaseorder | Inventory/Procurement PO |
| QuotationServiceImpl | /quotation | Inventory/Procurement quotation |
| ExpiryItemServiceImpl | /expiryitem | Inventory/Expiry |
| PrescriptionMasterServiceImpl | /prescriptionmaster | Pharmacy/Prescriptions |
| DrugServiceImpl | /drug | Pharmacy/Drugs |
| PharmacyServiceImpl (custom) | /pharmacy | Pharmacy |
| MedicationServiceImpl (custom) | /medication | Pharmacy/Medication |
| PrescriptionDetailsServiceImpl (custom) | /prescription | Pharmacy/Prescription |
| Pathology: PathoOrderServiceImpl (custom) | /pathoOrder | Pathology |
| RadiologyServiceImpl | /radiology | Radiology |
| InvestigationServiceImpl (custom) | /investigation | Investigation/Ordering |
| PatientRequisitionServiceImpl | /patientrequisition | Blood Bank requisitions |
| BloodBankServiceImpl | /bloodtestmaster | Blood Bank |
| DietServiceImpl | /diet | Diet Kitchen |
| TransportServiceImpl | /transport | Transport/Ambulance |
| EquipmentServiceImpl | /equipment | Equipment |
| PreventiveMaintenanceServiceImpl | /preventivemaintenance | Equipment/PM |
| PreventiveMaintenanceScheduleServiceImpl | /preventivemaintenanceschedule | Equipment/PM schedule |
| ComplaintDetailsServiceImpl | /complaintdetails | Equipment complaints |
| ComplaintResolutionDetailServiceImpl | /complaintresolutiondetail | Equipment complaints |
| OTConfigServiceImpl | /otconfig | OT |
| OTSchedulerServiceImpl | /otscheduler | OT |
| OTDetailsServiceImpl | /otdetails | OT |
| SurgeryDetailsServiceImpl | /surgerydetails | OT/Surgery heads |
| RMRSServiceImpl | /RMRS | RMRS (funds/payments) |
| PmrdServiceImpl | /pmrd | PMRD (medical records) |
| PctsServiceImpl | /pcts | Family welfare (ANC/PCTS) |
| MasterServiceImpl | /masterConfigService | Masters |
| MasterCachedServiceImpl (custom) | /master | Masters cache |
| UserManagementServiceImpl | /usermanagement | Users/Admin |
| NursingStationDefinationServiceImpl | /nursingstationdefination | Masters/Nursing |
| DepartmentScheduleServiceImpl | /departmentschedule | Masters/Department schedules |
| HelperServiceImpl | /helper | Cross-cutting helpers |
| AppointmentScheduleServiceImpl (custom) | /appointmentschedule | OPD/Appointments |
| AddUpdateAppointmentServiceImpl (custom) | /updateappointment | OPD/Appointments |
| PatientRegApptServiceImpl (custom) | /patientRegAppt | OPD/Registration+Appt |
| PatientRegAdmissionServiceImpl (custom) | /patientRegAddmission | IPD/Admission registration |
| PatientDetailRegistrationServiceImpl (custom) | /patientDetailReg | Patient/Registration |
| EmergencyAndWalkinVisitServiceImpl (custom) | /walkinVisit | OPD/Walk-in & emergency |
| MarkAndAddVisitServiceImpl (custom) | /markandaddvisit | Patient/Visits |
| PatientTransferDischargeServiceImpl (custom) | /patientTransferDischarge | IPD/Transfer & Discharge |
| BedManagementServiceImpl (custom) | /bedmanagement | Bed management |
| CancelAdmissionServiceImpl (custom) | /canceladmission | IPD/Admission cancellation |
| AddServiceOrderServiceImpl (custom) | /serviceOrder | Clinical/Service orders |
| AddServiceMasterImpl (custom) | /addServiceMaster | Masters/Services |
| InvestigationServiceImpl (custom) | /investigation | Clinical/Investigations |
| QuetionnaireServiceImpl (custom) | /questionnaireService | Queue/Questionnaires |
| UploadFileServiceImpl (custom) | /uploadFile | Documents |
| UploadHelperServiceImpl (custom) | /upload | Documents |
| NoteDetailsServiceImpl (custom) | /notedetailsdata | Clinical notes |
| ActionNotificationServiceImpl (custom) | /actionNotif | Notifications |
| MachineConfigServiceImpl (custom) | /machineConfig | Machines/Integration |
| MdmServiceImpl (custom) | /mdm | MDM (Janta/central) |
| DoctorListServiceImpl (custom) | /doctorlist | OPD/Doctor lists |
| InventoryEAushadiIntegrationImpl (custom) | /eAushadiIndent | Integration/e-Aushadhi |

### Top-level & integration services

| Service | @Path | Module |
|---|---|---|
| AuthenticationServiceImpl | /auth | Auth |
| ClinicalServiceImpl | /clinical | Clinical (nursing/charting) |
| CWFServiceImpl | /cwf | Clinical Workflow (CWF) |
| BioMedicalWasteManagementServiceImpl | /bioMedicalWasteManagement | Biomedical Waste |
| FileManagementServiceImpl | /fileService | File management |
| LinenLaundryServiceImpl | /linenLaundry | Linen & Laundry |
| QueueManagmentServiceImpl | /queueManagment | Queue |
| RcmServiceImpl | /rcm | RCM (revenue cycle) |
| SendNotificationServiceImpl | /sendEmail | Notifications |
| HelpConfigServiceImpl | /help-config | Help/config |
| IntegrationServiceImpl | /integration | Integration |
| nurse/NursingServiceImpl | /nursingList | Nursing |
| cssd/CSSDServiceImpl | /cssd | CSSD |
| abdm/AbhaServiceImpl | /abha | ABDM/ABHA |
| abdm/HPRServiceImpl | /hpr | ABDM/HPR |
| abdm/NHCXServiceImpl | /nhcx | ABDM/NHCX (insurance) |
| ehr/EHRUtilServiceImpl | /ehr | EHR integration |
| patp/PatientPortalServiceImpl | /patientPortalService | Patient portal |
| patp/PatientHistoryServiceImpl | /patientHistory | Patient history |
| payment/PaymentTransferServiceImpl | /paymentTransfer | Payment transfer |
| ejs/Ejs*ServiceImpl (9) | /ejs-* | EJS (ANM/ASHA) |
| aasmaan/Aasmaan*ServiceImpl (10) | /cases, /devices, /users... | Aasmaan app backend |
| aadhar/* (2 files) | (templates) | Aadhaar integration |
| com.ohum.xcg.abdm.* | ABDM server + client (HIP/HIU/HPR/PHR), FHIR R4 (genserv FhirServer, PatientResourceProvider), HCX (insurance plan, pre-auth, eligibility, claim) | ABDM/NHCX/Interop |

---

## 4. Schema inventory per module folder (schemas/*.json file names — evidence of screens)

- **bcma** (11): iVPUnableToScanSchema, missingDoseRequestSchema, patientMedicationLogSchema, showCoverSheetScema, showDermalSiteSchema, showItemStockSchema, showIVPTabSchema, showMainScreenSchema, showMedLogHeld, showUdTabSchema, showUnableToScanSchema
- **billing** (77): addAdvanceSchema, addApprovalAmountSchema, addBillingSchema, addDeductDoctorShareSchema, addIpBillingSchema, addOpIpBillingSchema, addOTBillEstimationSchema, addPackageSubscriptionSchema, advancePaymentDetailList/View, advanceReceiptByReceiptNoSchema, advanceRefundReceipt, advanceSchema, advance_recieptSchema, advance_refund, allReceiptsSchema, approveAmountListSchema, bedPackageDetails, billReceiptSchema, billSummarySchema, cancelIpBillSummarySchema, cancelOpBillSummarySchema, changeSponsorSchema(+List), combineBillReceiptSchema, companycontractdetails, consultationPackageDetails, creditNoteSchema, crossServiceOrderSchema, diseaseGroupMaster, doctorBillListSchema, doctorPaymentDetailSchema, doctorShareAddDeductSchema, doctorShareBillSchema, doctorShareSchema, doctorvisitcharge, draftAdvanceSummarySchema, draftSummaryBillSchema, getAllBills-schema, getAllCombinBills-schema, getAllCreditNoteListSchema, getAllEstimationBillSchema, getDischargeList-schema, getIpdChargeListSchema, healthplanConsumptionDetailSchema, healthPlanPackageSchema, ipBillPaymentSchema, ipBillSummarySchema, iPDBillReceiptSchema, itemPackageDetails, opBillPaymentSchema, package-order-list-schema, packageListForBillSchema, package_appointment_list-schema, partialPayBillSchema, paymentDetailSchema, pharmacyBillRecSchema, refundDetailSchema, refund_recieptSchema, return_advance, return_bill_advance-schema, service-order-list-schema(+ForHealthPlanCons), serviceListForBillEstimationSchema, serviceListForBillSchema, servicePackageDetails, showPackageDefaultServices, showSponsorSchema, tariffConfigPackageDetails, tariffcopaydetails, tariffRatePackageDetails
- **biomedicalwaste** (1): showBioMedicalWasteStatusSchema
- **bloodBank** (40): addAdminRequisition, addBloodBagOpeningStock, afterAddOpeningStock, AutologousDonationSchema, bagReturnDetailsSchema, bloodBagReturnSchema, bloodComponents, bloodComponentSeperationSchema, bloodCrossMatchingListSchema, blooddonortypemaster, bloodRequisitionCrossMatchingSchema, bloodtestmaster, bloodtestresultmaster, CampDonationSchema, campRequisitionSchema, crossMatchingBloodbagDetails, crossMatchingForm, crossMatchingPatientDetails, crossUnitOrderSchema, crossUnitOrdersListSchema, donorRegistartion, donorrejectionreasonmaster, externalBloodStorageCenterRequisition, externalHospitalRequisition, ExternalPatientRequisitionSchema, openingBloodStockSourceDetailsSchema, patientRequisitionDetails, patientRequisitionList, ReplacementDonationSchema, replacementDonorHistory, requestDetailsGeneric, showBagsReturnListSchema, showBloodBankIndentListSchema, showBloodDiscardListSchema, showBloodStockListSchema, showCrossMatchedBagsSchema, showDiscardedBagsSchema, showDonorListSchema, VoluntaryDonationSchema
- **chemotherapy** (3): showChemoPatientListSchema, showChemoPlanningSchema, showChemotherapyPatientCoversheetSchema
- **commons** (67): addAssessmentSchema, addIpdRosterSlotsSchema, addPatientAssessmentSchema, addPatientUserPassSchema, addShiftDetails-Schema, amendmentFileUpload, appDocumentation, applicationModuleStatus, auditSchema, backGroundTasks, boxCommonVerification, configapplication, ConsentRequestFormSchema, doctormaster, doctorRoomQueueSchema, encountertype, firstVerificationSchema, hospitalCreationSchema, hospitalGeoTagging, inbound, itemSaleDetailpaymentSchema, machineConfigSchema, mortuaryBoxManagement, mortuaryClearanceSchema, mortuaryDischargedPatientSchema, mortuarySchema, mrnoseries, myWalkinVisitSchema, newAuditSchema, notificationTemplateSchema, outbound, OutreachUserCreationSchema, patientCoronaDetailsSchema, patientIPCRSchema, procedureListForOTSchema, questionaireSocialWorker, schemasDesigner, secondVerificationSchema, serviceMasterListSchema, servicemastermapping, serviceMasterSchema, shiftDetails-Schema, shiftRoster-Schema, showAbdmPatientQueue, showCoronaPatientReport, showIpdRosterSlotsSchema, showMachineConfigListSchema, staffmaster, tuserrolemaster, updateProduction, updateUserSchema, uploadDocumentSchema, uploadFileSchema, uploadServiceMappingSchema, userShiftDetailsListSchema, vitaltypemaster
- **cssd** (15): cssdkitissueitem, procedureCssdKitRequestFilterSchema, showCSSDItemKitSterilization(+Filter), showCSSDKitRequestList, showCurrentKitFormationListSchema, showCurrentKitMasterListSchema, showCurrentKitPreparationListSchema, showIndentDetailsEditableOnCSSDSchema, showItemReceiveListforCSSD, showKitRequestLstForCssdStore, showProcedureCssdIssue, showProcedureCSSDKitDetailSchema
- **diet** (4): diet_foodorder_schema, diet_menu_item_schema, diet_schedule_schema, show_diet_kichen_schema
- **enquiry** (8): addEnquirySchema, addPatientFeedbackSchema, bed_Details_Schema, showInquiryDetailsSchema, showPatientFeedbackSchema, showServicesForInquiry
- **equipments** (34): addComplaintResolutionSchema, addEqpDetailsList, addEqpInsuranceSchema, addNewComplaintSchema, addNewEqpPrevntSched, complaintDetailsSchema, eqAmcWarrantyDetails, equipmentIssueList, equipmentPoList, equipOpeningBalListSchema, getAssetDetailsSchema, getAssetDtlsByItemSchema, getAssetVerificationListSchema, getCalibrationMaintenanceListSchema, getComplaintResolutionListSearch, getEqpWarrantyDetailsSchema, getEquipmentGrnListSearch, getItemListSchema, getPreventiveCheckListSchema, getPreventiveMaintenanceList(+Search), getPreventiveMaintenanceScheduleListSearch, getScrapSale, newGeneralComplaintSchema, showEquipMedGasIndentIssueList, showEquipMedGasIndentList, ShowEquipmentGrnApprove, showEquipUsageListSearch, showIndentEquipmentDetOnApproved/Issued, updateComplaintSchema, updateGeneralComplaintSchema, uploadEquipmentDocument/InsuranceDocument/WarrantyDocumentSchema
- **fileManagement** (1): uploadDocumentSchema
- **grievance** (3): grievanceHistorySchema, grievanceSchema, raiseGrievanceTicketSchema
- **inventory** (70): addNewIndentSchema, addNewItemSchema, approvePOPageSchema, assetRegisterList, compareQuotationSchema, currentOpeningBalanceListSchema, customOrderAssignee, customProcessItemDetails, customProcessOrder, damagedItemReturnIndent, expiredDamagedItemReturnIndent, expiryItemReturnSchema, expiryItemReturnToSupplierSchema, freezeCurrentPOListSchema, getItemEnquirySchema, getItemQuotation, getReorderListForDirectItemIssue, hospitalWiseDemandListSchema, issueItemReturn, itemEnquirySchema, itemMasterSchema, itemWiseDemandListSchema, materialConsumptionDetails, paymentDetailModeForGRNSchema, qcStatusUpdate, returnItemRecieve, returnToSupplierSchema, showAllItemIssueListSchema, showApproveGrnListSchema, showConsignmentGRNListSchema, showCurrentGRNListSchema, showCurrentPurchaseOrderList(forOtherUnit)Schema, showCurrentReorderListSchema, showCurrentStockListSchema, showCustomOrderList, showDepartmentalConsumptionList, showDirectItemIssueListSchema, showExpiredItemListSchema, showGrnReturnListSchema, showGRNSchema, showIndentDetails(Editable/OnApproved/OnFreezed/OnSecondLevelApproved/forItemIssue)Schema, showIndentList(onItemIssue)Schema, ShowIssuedPurchaseOrderDetails/ListForGRNSchema, showItemReceiveList, showOrderListforInHouseSupplierSchema, showPODetailsSchema, showPoIssueDetails, showPOPageSchema, showPurchaseOrderDetailsSchema, showPurchaseRequestSchema, showRecallExpiredItemSchema, showRenegotiateSchema, showReturnFlowStore, showStockAdjustmentListSchema, showStockLocationSchema, showStoreComsumptionListSchema, stockListSchema, supplierPoSchema, updateBatchExpiry, updatePODetailsForExpDate, updatePurchaseOrderSchema
- **ip** (65): addBabyAdmissiondto_schema, addpatientAdmissiontdto_schema(+_emergency), addVisitChargeSchema, admissionRequest_list-schema, admissionServiceSchema, admission_list-schema, allPreviuosAdmissionsSchema, attendantPassSchema, bedReleaseSchema, bedReservationdto_schema, bedTransferRequestSchema, bedTransferSchema, bed_management_schema, bed_screen_schema, billEstimatiomSchema, clearancemaster, dischargePatientSchema, dischargeUpdateStatus, discharge_clearance_schema, emergency-schema, getAdmissionClearanceList, interimAdmissions, ipd_patient_service_detail_schema, ipPatientCoronaDetailsSchema, MedicOrderPickListSchema, mlcClearanceFilters, motherChildLinkingSchema, nursingOrderSchema, releaseBedNewSchema, showAllCradlesSchema, showDischargedPatientSchema, showDoctorIpSchedule, tidListSchema, transferRequest_list-schema, updateDoctorFromBM_Schema
- **kiosk** (4): accountSearchSchema, appointmentSearchSchema, selfAppointmentSchema, showSelfRegPatientSchema
- **linenlaundry** (1): showCurrentLinenStatusSchema
- **masters** (330 keys across 24 subdomains — see §5)
- **nhcx** (1): checkEligibilitySchema
- **nursing** (28): alliedStaffListForPatientAllocationschema, ancCoronaSurveySchema, consultPatientSchema, HistoryClinic, incidentReportSchema, incidentResolveSchema, nurseListForDepartmentAllocationSchema, nurseListForDoctorAllocationSchema, nurseListForPatientAllocationschema, nurseWardAllocationSchema, outreach_data_schema, showNursePatientNoteSchema, showNursePlanOfCareSchema, showNurseWorklistSchema, showPatientCoversheetSchema, staffListForClinicAllocationSchema, tempPrescriptionListSchema
- **op** (43 keys — see example in §2)
- **ot** (14): addNewSurgerySchema, addOTBillingSchema, addOTCharge, addPatientProcedureRateSchema, addProcSchema, addSurgHeadSchema, itemCategorySchemaForOT, notesCollectionSchema, otscheduledetails, patientProcedureRateDetailsSchema, procedurePerformanceSchema, showSurgeryOrderList, surgeryheads, surgeryScheduleSchema
- **pathology** (57): acceptReject(Filter/ReportDetails/Report)Schema, addServiceSchema, criticalAlert(Filter/ResultReportDetails/ResultReport/Result)Schema, getLaboratoryReports, machineWiseResultReview(+Details/Filter)Schema, microbiology-schema, microbiologyAntibiotic-schema, pathagencymaster, pathoFilterSchema, pathoGenerateBarCodeSchema(+ForBloodBank), pathoGroupingAndTest, pathoKdlVerify, pathoReportDetails/Report/ResultSchema, pathosamplesutability, pathoSchema, pathoShowCovidStatus, pathoShowTestStatus(+New), pathosubtestmaster, pathoUpdateOrderStatus, PatientFilterLabSchema, reportRelease(+Details/Filter/Microbiology/Report/Template)Schema, resultingDetail/Report/TemplateReportSchema, resultReview(+Filter/Microbiology/ReportDetails/Report/TemplateReportDetails/TemplateReport)Schema, sampleCollectSchema, sampleRejectionSchema, sampleResult(Filter/Report)Schema, sampleRetestSchema, serviceStatusSchema, showLabLoadListSchema, showPatientWorkList
- **patientManagement** (1): addSocialWorker
- **paymentTransfer** (3): generateOTPSchema, incentiveClaimInfoSchema, paymentTransferListSchema
- **pharmacy** (39): addItemGroup, addMedicationFromNurseSchema, addMedicationSchema, addPatientPrescMedSchema, addPatientProblems, addPrescriptionSchema, addPrescVisit, commonVisitCreationAndMedicineDis, dispensingtype, getEntirePrescriptionMaster, getItemAndItemBatchListByStoreId, getItemGroup, getPrescriptionDetailsSchema, ipPharmacyBillingSchema, itemCategoryByStoreIdSchema, itemCategorySchema, itemSaleDetail/List(+ReturnDetails/List)Schema, opIpPharmacyBillSchema, opPharmacyAddPatient, patientOrderListSchema, pharmacyCounterSaleSchema, pharmacyInterventionReport, pharmacyItemSaleRetrunBIllReciptSchema, pharmacyPaymentDetailSchema, pharmacyPaymentForCounterSchema, pharmacyPaymentReturnSchema, prescriptionListSchema, prescriptionUploadSchema, showNursingPrescripListSchema, showPharmacyPatientCoversheetSchema, updateItemGroupSchema, updateItemMaster
- **pmrd** (2): pmrd (patient medical record docs; templates formj1/2/3, hform)
- **queue** (11): addpatientPhoto, appointmentQueueSchema, appointment_list_new_schema, cabinQueueListSchema, callGetPatientListSchema, doctorBatchSlotSchema, manageTokenSchema, nursingDeskForQueue, questionnaireSchema, userQueueSchema
- **radiology** (9): addRadImg, getRadiologyReports, patientFilterRadSchema, radFilterSchema, radOrderBookingSchema, radReportDetailsSchema, radReportSchema, radRescheduleSchema, radRetestSchema
- **rcm** (1): rghsEligibilityCheckSchema (schemas incl. dischargeVerificationSchema, dmApprovalUploadSchema, moicUploadSchema, patientPhotoUploadSchema)
- **reports** (46): ageWiseRegistrationReportSchema, ambulanceRegisterReportSchema, bloodBagIssueReport, bloodBagTypeDetailsReport, bloodDonorRegistrationReport, bloodGroupWiseStaticReport, cancelledAdmissionReportSchema, categoryWisePatientReportSchema, dailyDischargeReportSchema, departmentwiseOpdPatientStatistics, donationBloodGroupWiseReport, donorSerologyTestReport, getServicewiseCollectionReportSchema, getUserwiseCollectionReportSchema, getUserwiseSummaryReportSchema, ipdStatisticalReportSchema, labCenterTestWiseReportSchema, patientDeathReportSchema, patientFeedBackSummarySchema, showAdmissionReportSchema, showAppointmentReportSchema, showBioWasteReportSchema, showBloodBagBalanceSheetReport, showBloodBankBalanceSheetReport, showBloodBankReqBalanceSheetReport, showBloodComponentMasterRecord, showCampBagIssueDetailsReport, showCampDonorDetailsReport, showCampDonorStatisticReport, showCrossMatchRegisterReport, showCssdReportSchema, showDailyBloodstockBookReport, showDonationStatusReport, showDonorPhysicalStatusReport, showEquipmentUsageReportSchema, showFuelConsumptionReportSchema, showHemoglobinopathyTransfusionReport, showLabReportSchema, showLinenLaundryReportSchema, showListOfRecieptforCampReport, showReceivedBackBloodReport, showSupplyComponentTypeWiseReport, showVehicleTripReportSchema
- **rmrs** (9): AddCollection, CollectionIssuedList, DashboardPaymentCollectionSchema, IssueCollection, PaymentCollection, RMRSDashboard, rmrsDashboardAuthorityList, showRMRSPurchaseOrderRequestListSchema, suspenseAccountDetailsSchema
- **tiuxmls** (368): digitized clinical forms — see §7
- **transport** (11): addTransportChargesSchema, doctorTransportRequestSchema, patientTransportRequestSchema, showFuelStationDetailSchema, showTripRequestDetails, vehicleInSchema, vehicleMaintenanceDetailsSchema, vehicleOutSchema, vehicleStatusSchema
- **worklist** (1): nurseTaskList

---

## 5. Masters subdomains (modules/masters/* + fjs-config_masters.json 330 keys)

| Masters submodule | Schemas (sample) | Domain |
|---|---|---|
| masters/admission | actualBedCountMaster, admissiontype, bedmaster, bedstatusmaster, blockmaster, buildingfloorstructure, buildingmaster, classmaster, classWardMappingForAdmissionBlock, daycaremaster, dischargetype, doctortypemaster, floormaster, identitytype, modeOfDelivery, roommaster, wardmaster, wardtypemaster, wingmaster | Ward/bed/room hierarchy, admission types |
| masters/ambulance | accidenttype, ambulancemaster, ambulancetypemaster, driverMaster, vehicleRequestTypeMaster, workshopmaster | Ambulance/vehicles/drivers |
| masters/billing | advanceagainst, billingDepartmentMaster, cancellationreason, cashcountermaster, companyassociatemaster, companymaster, companyplandetails, companytypemaster, concessioncategorymaster, concessiontemplatemaster, ordersetdefinationmaster + ordersetservicemaster + lab/radiology/nursing/consult/procedure/consumables/miscellaneous ordersetservicemaster, ordersetmedicinedetails, packagehealthplandefinationmaster, packagemaster, packages, paymentmodetype, planmaster, procedureordersetservicemaster, servicemaster, services, specialization, subspecialization, tariffmaster, tariffratecopyutility, subPlanmaster | Service master, tariff, packages, health plans, companies, order sets, concessions |
| masters/biomedicalwaste | wastebagmaster | BMW |
| masters/bloodBank | additivemaster, bloodadversereactionmaster, bloodbagtypecomponentmaster, bloodbagtypemaster, bloodcomponentclinic, bloodcomponentmaster, blooddiscardreasonmaster, blooddonationcampmaster, bloodgroupcrossmatching, bloodgroupmaster, bloodrequisitionreasonmaster, bloodscreeningmaster, donordefaultvitalvalue, eraktkoshcomponentmapping, externalbloodstoragecenter, externalhospitalmaster, importerMaster, modeofscarpsale, requesttypemaster | Blood bank |
| masters/cssd | sterilizationexpirymaster, sterilizationmachinemaster, sterilizationmaster, sterilizationqualitytestmaster, sterilizationtypemaster | CSSD |
| masters/diet | dietitemmaster, dietsessionmaster, diettypemaster | Diet |
| masters/ejanswasthya | media | E-Jan Swasthya |
| masters/ejs | ejschecklistquestions, ejsmasteractivity, ejsmasterentity | EJS checklists |
| masters/equipment | amcagent, complaintcategorymaster, complaintstatusmaster, complainttypemaster, compliancemaster, compliancetypemaster, electricalChecklistmaster, equipchecklist, equipmentpreventivechecklist, equipworkingstatus, functionalchecklistmaster, insuranceagent, maintainancetype, serviceagentmaster | Equipment |
| masters/grievance | grievancecategorymaster, grievanceseveritymaster, grievanceseveritytemplatedetails, grievancesubcategorymaster | Grievance |
| masters/hospital | accountheadmaster, addUserSchema, admittingdoctorclassificationmaster, bankaccountmaster, bankbranchmaster, bankmaster, cabinmaster, cadredesignationmaster, cadremaster, casetypemaster, classification, deliveryparticularsmaster, departmentCabinRoomMapping, departmentmaster, departmentmst, departmentRoomMappingMaster, designationmaster, displayUnitmaster, districtzonemaster, doctoraddressmaster, doctorwaiver, emailtemplate, emailTemplateDetails, emergencyregtypemaster, events, getUserDetailsSchema, holidaymaster, hospitalcategorymaster, hospitalGeoLocationMapping, hospitaltypemaster, insurancecategorymaster, insuranceGradeMaster, languageLabel, modulenamemaster, opdMachineMaster, opdStationMaster, phonecategory, policestationmaster, primarysymptoms, priority, provisionaldiagnosis, ptcregistration, referraltypemaster, rghsDepartmentMaster, roomMasterForQueue, schedulemaster, shiftmaster, subdepartmentmaster, taxbuildermaster, unitmaster | Hospital org, users, departments, scheduling |
| masters/inventory | brandmaster, discardrejectionmaster, empanelmaster, gastypemaster, genericbrandmapping, grnitemchecklist, grnrejectreasonmaster, itemcategory, itemcompany, itemgroup, itemmaster, itemMasterListing, itemSupplierRate, item_mst, mappedProductItems, molecule, preganancyclass, rateContractSchema, reorderAndSafetyMaster, salemarkupmaster, store, storeagetype, supplier, supplierItemList, suppliermaster, taxmaster, termsAndCondition, termsofpayment, therapeuticclass, unitofmeasure, unMappedProductItems | Item master, suppliers, stores, tax |
| masters/meds | diagosismaster, instructionmaster, route, scheduleListMaster | Med orders |
| masters/nursing | addUpdateNoteTitle, allergyMaster, chartconfiguration, cwfmapping, diagnosismaster, dischargeDepartmentTemplate, diseaseMaster, documentClassMaster, flagMaster, growthchartdetails/menu/menuchild/series/seriesdata, healthFactorCategoryMaster, healthFactorMaster, highRiskPregnancyFactorMaster, icdcmCategory, imageAnnotationMaster, immunizationagegroupmaster, noteCategoryMaster, noteTitleMaster, notifyablediesase, nursingordermaster, scwfmapping, specialityMaster, vaccineimmunizationagegroup, vaccinemaster, vitalmaster | Nursing/clinical config |
| masters/ot | anesthesianotes, anesthesiatypemaster, intraoperativeinstructionsmaster, operationresultmaster, operationstatusmaster, otschedule, ottablemaster, ottheatremaster, postoperativeinstructionsmaster, preoperativeinstructionsmaster, procedurecategorymaster, procedurechecklist, procedurechecklistitems, procedurecheklistdetails, procedureequipmentdetails, procedureitemdetails, proceduremaster, proceduresubcategorymaster, proceduretypemaster, surgeryheadmaster, surgeryheadmasterdetails, surgerynotes | OT masters |
| masters/pathology | antibioticmachinemap, containermaster, cultureNegativeMaster, discardreasonmaster, errorflagmaster, gendercategory, labconfigmaster, machinemaster, machinetestmaster, organismmachinmap, organmaster, parameterresultingmethod, pathantibioticmaster, pathoCabinMaster, pathocategory, patholocationmaster, pathoparametermaster, pathoparameterunits, pathoparammachinemap, pathoprofilemaster, pathoremarksmaster, pathorganismmaster, pathosamplemaster, pathosubspecialization, pathotemplatemaster, pathotestmaster, pathozonemaster, pathsensitivityresultmaster | Lab masters |
| masters/patient | appointmentreasonmaster, areamaster, bookingtypemaster, campmaster, camptypemaster, castemaster, citymaster, consentmaster, consenttype, countrymaster, districtMaster, feedbackMaster, gendermaster, grademaster, incomecategorymaster, languagepreferedmaster, level, maritalstatusmaster, mothertonguemaster, nationalitymaster, occupationmaster, patientcategorymaster, patientsourcemaster, patienttypemaster, placeofbirthmaster, prefixmaster, qualificationmaster, racemaster, refentitymaster, refentitytypemaster, registrationcustomfieldsmaster, relationmaster, religionmaster, riskfactormaster, statemaster, triagemaster, visittypemaster | Patient demographics |
| masters/Pharmacy | administrationschedule, creditaccountmaster, drugingredients, drugInteractions, medicationroute | Pharmacy/med |
| masters/radiology | imagediscardreasonmaster, radCabinMaster, radfilmsize, radmodalitymaster, radtemplatemaster, radtemplateresult, radtestcategory, radtestmaster | Radiology masters |
| masters/rmrs | attendantpassserviceratemaster, authoritymaster, headsmaster, rmrsreasonmaster | RMRS |
| masters/ruleengine | ruleSet | Rule engine |

---

## 6. Entity → module mapping (representative; full lists in source)

- **Patient/OPD**: models/trx (Encounter, MLCDetails, FollowUpVisitInfo, EmergencyCaseDetails, PatientPackageSubscription, RegistrationQueue, PatientDemoDetailsMismatch, TempPrescriptionNote), models/masters/patient + hospital, PatientServiceImpl
- **Billing**: transactionals (CreditNote, CreditNoteDetails, PatientBillEstimationSurgeryHead, PackageChargeSubscription, Tariffserviceclassratedetail, Doctorvisitcharge, Doctorwaiver), masters/billing
- **Inventory/Procurement**: transactionals (ItemStock, ItemClinic, ItemSupplier, PurchaseRequest(+Details), PurchaseOrderRequest, RateContract(+Details/Units/View), IndentInvoice(+Details), TermsAndCondition, StoreItemCategory, EqiupOpeningBalance), masters/inventory
- **IPD/Beds**: AttendantPassDetails, TransferRequest, masters/admission
- **OT**: PatientProcedureAnesthesiadetails, PatientProcedureRateDetails, PatientScheduleAnasthesiaDetails, Patientprocedurechecklistitems/detailsDto, PatientBillEstimationSurgeryHead, masters/ot
- **Pathology**: PathoPatientDocs, PathOrderBookingDetailDocuments, masters/pathology
- **Blood Bank**: T_BLOODBAGRECEIVE, t_bloodbagissue, t_bloodbagreturns, t_bloodbagcollection, t_bloodbagdecross, t_bloodbagissuedemandslip, T_BloodBankIndent(+Details), t_bloodcrossmatchingresult(+details), t_blooddonorregistration(+details), t_donorscreeningtest, t_donormedicalexaminationresult, masters/bloodBank
- **CSSD**: CssdKitFormation(+Details), Cssdkitissueitem(+details), CssdItemSterilizationDetails, ProcedureCssdRequest(+Details), ProcedureCssdKitDetails, ProcedureCssdIssue(+Detail), masters/cssd
- **Diet**: DietItemScheduleDetails, DietOrderDetails, TodayDietItemMenu, masters/diet
- **Pharmacy**: PrescriptionMaster, PrescriptionDispenseDetails, PatientMedication, PatientPrescptionNote, PatientNonMedOrderDetails, PrescriptionDispenseDetails, masters/Pharmacy + meds
- **Nursing/Clinical**: NurseNote, NurseTaskDetails, NurseTaskFrequencyDetails, PatientNote, PatientVitals, PatientAllergies, PatientComplaints, PatientDiagnosisDetails, PatientImmunizationDetails, PatientAdvice, PatientEorder, PatientDischargeData, PatientHqDetails, CarePlan(+Details), masters/nursing
- **Equipment**: Equipment, EquipmentDetailDocs, EquipmentWarranty, EquipmentTransfer, EquipmentUsageDetails, EqpComplaint(+Details), EqpComplaintResolution(+Details), EqpPMSchedule(+Details), EqpPreventiveMaintenance, masters/equipment
- **Transport**: TransportRequest(+Details), VehicleMaintenance, FuelStationDetails, masters/ambulance
- **Grievance**: PatientGrievanceHistory, PatientGrievanceComments, PatientGrievanceFeedback, GrievanceDocuments, ResolutionAsignee, masters/grievance
- **RMRS**: CollectionHeader, CollectionDetail, RmrsCollection, RMRSDashboardTransaction, SuspenseAccountDetails, TUserHospitalAuthority, masters/rmrs
- **Bio-medical waste**: BioMedicalWaste(+Details), masters/biomedicalwaste
- **Linen/Laundry**: LinenRequest(+Details), masters/linen (linencategory, linendetail)
- **Queue**: PatientQueue, RegistrationQueue, QuestionAnswerDto, masters/hospital (roomMasterForQueue)
- **Payments transfer**: T_PAYMENTTRANSFER(+REQUEST/DETAIL), masters/hospital (bank)
- **EJS**: models/ejs 61 entities (ANDROID_*: ANC, PNC, CHILD_CARE, ASHA_WORK, ATTENDANCE, FAMILY, INCENTIVES, MASTER_* geography, ES_CHECKLISTS, CORONA_*) + masters/ejs
- **Aasmaan**: models/aasmaan 35 entities (app transaction logs, cases, devices, e-tickets, institutions, rsc call logs, stats, users)
- **Statistical**: ServiceWiseCollection, UserWiseCollection (collection reports)
- **trx**: Encounter, MLCDetails, FollowUpVisitInfo, EmergencyCaseDetails, RegistrationQueue, PatientPackageSubscription, TempPrescriptionNote, PatientDemoDetailsMismatch, IhmsCounts
- **txt**: 10 entities (text/templates: e.g. T_DIET..., from grep: txt package files not individually enumerated — UNKNOWN-01)
- **hcx** (2): HCX entities (insurance); **rcm** (2): RCM entities; **chiranjeevi** (2): Chiranjeevi scheme entities
- **Master table prefix convention**: `M_*` for masters (e.g. M_EquipmentIndent, M_EQUIPMENTSERVICEMASTER, M_TermAndCondition), `T_*` for transactionals, `t_*` mixed-case legacy, `IHR_MEDICATION_TMP` legacy temp, `ANDROID_*`/`MASTER_*`/`ES_*`/`USER_`/`MEDIA*`/`PROFILE`/`INDICATORS`/`CORONA_*` for EJS.

---

## 7. tiuxmls — digitized clinical forms engine (368 schemas)

- Configs: `tiuxml_mapping.json` (Assessment/History/Examination/etc. tile maps), `chemo_tiuxml_mapping.json`, `nhnc_tiuxml_mapping.json`, `op_tiuxml_mapping.json`, `tiuxml.json`, `tiuxml.js`
- Evidence: `modules/tiuxmls/schemas/*.json` + `modules/tiuxmls/templates/*.html` — 1:1 (368 each)
- Form families (from file names): History/Examination/Assessment per specialty (ER, CARDIO, NEURO, ORTHO, OBS/GYN, PEDS, NEO (neonatal), GERIATRIC, PHYSIO, OT (occupational therapy), SLP (speech-language pathology), PSY, RISL), nursing (NURSINGINITIALASSESSMENT, Nurses_note, NURSING_PROCEDURE, DAILY_NURSING_REPORT, Misc_Nursing_task), scales (Braden, Barthel, FLACC, Morse Fall, APGAR, Modified Aldrete, MINI_MENTAL, Neonatal Abstinence, Pain, Waterlow, Acuity2), procedures/checklists (CVC, CAUTI, CRBSI, SSI, VAP, MRSA, brain death, sterilization, IUD, vasectomy, tubectomy), discharge (discharge checklists, advice, instruction), emergency (ER, prehospital, trauma), COVID, handover (HANDOFF_COMMUNICATION, Telephonic_Handover, shift handover), social work (SW_*, SSW_*), family welfare (Eligible_Couple, family planning), autopsy (Post_Mortem), death (Death_Certificate, DEATHNOTE).
- Versions observed in names: _V10..V15, _V1, V2, _new, _P (print variant), _B (bilingual?), _AH (allied health), _AL (allied), _nhnc (NHNC variant), _risl (RISL variant), _oqa, _1307/_130717 (dated variants) — indicating ongoing clinical form digitization.

---

## 8. Print templates (root `templates/*.vm`) — reports/prints evidence

- **Discharge**: discharge_summary_pat.vm, dischargeSummaryLabResult.vm, common_dis_form/ticket.vm + specialty variants (cardiology, ent, eye, medicine, neurology, neurosurgery, oncology, ortho, rrc, urology, surgery_plasticsur, endocrinology), admissionFacesheet1.vm, adm6.vm, token_sheet.vm, wardBedInquiry.vm
- **Billing**: bill_receipt.vm, bill_summary.vm, ip_bill_summary.vm, ipbillreceipt.vm, ipDraftBill.vm, opbillRec.vm, advance_rpt.vm, advanceRefundRec.vm, cancelIPReceipt.vm, cancelOPReceipt.vm, creditNoteDetails.vm, creditNoteServices(+ForOP).vm, packageRefundReceipt.vm, billEstimation.vm, billRefundReport.vm, high_billing_report.vm, ipReceiptList.vm, packageConsumptionDetailsPrint.vm, packageDetailsPrint.vm
- **Pharmacy**: prescription.vm, prescription_format.vm, prescription_hindi.vm, newPrescription.vm, pharmacyReceipt.vm, pharmacyMedLabel.vm, pharmacyItemSaleBillNew(+ForCounterSale/WholeDetails).vm, pharmacySaleReturnReport.vm, pharmacyCompoundItem(+AllLabels).vm, ipOutOfStockMedications.vm, printMedNonMedDetails.vm, printRxDetails.vm, printLabResult.vm, cartfillDetail.vm, dispenseItemDetail.vm, showPharmacyDraftBill.vm
- **Inventory**: indentDetails(+OfApproved/OfFreezed).vm, invGrnDetails.vm, eqpGrnDetails(+_GST).vm, goods_issue.vm, invIndentIssueDetails.vm, invItemCurrentStockList.vm, invRecvItemDetail.vm, poDraft(+_New).vm, returnToSupplier.vm, quotationDetails.vm, expiryItemDetails.vm, damageItemDetails.vm, itemBatchDetails.vm, itemEnquiryDetails.vm, stockAdjusmentDetails.vm, grnBarCode.vm, indentIssueBatchBarCode.vm, UtilizatioIndentPrint.vm, requisitionKitDetail.vm
- **Blood bank**: bloodBankIndentDetails.vm, bloodBagDemandSlip.vm, bloodCrossMatchingreport.vm, bloodDonationCertificate(+New).vm, bagLabel.vm
- **OT/Surgery**: surgeryScheduleListDetails.vm, OTReport.vm, anesthesia-related in tiuxmls
- **RMRS/Finance**: rmrsIssuedPaymentReceipt.vm, authorityBillDetailsSummeryReceipt.vm, authorityBillPaidReceipt.vm, printBankStatement.vm, printJournalConfirmReceipt.vm, printReportByDocumentNumber.vm, planAndAccountWiseReport.vm, summaryAccountWiseReport.vm, printJournalConfirmReceipt.vm, ExpenditureReport.vm
- **Reports (generic)**: generic_reports.vm, servicewiseCollectionReport.vm, userwiseCollectionReport.vm, showAgeWiseRegistrationReport.vm, showLabReport.vm, labloadList.vm, linenLaundryReport.vm, cssdReport.vm, patientAllHistory.vm, patientHistory.vm, patientAllocationReport.vm
- **Pathology**: pathoAllResult.vm, pathoMicroReport.vm, pathoParameterReport.vm, pathoTemplateReport.vm, pathoTestStatusReport.vm, allPathoReport.vm
- **Misc patient**: healthCardSheet.vm, patientVisitDetailsForFuture.vm, patPrescriptionPHR.vm, patWellnessRecord.vm, babiesConsentForm.vm, consentForm.vm, cdr_form_4A.vm, cdr_form_4B.vm, DeathReasonForm.vm, death_Certificate.vm, death_form.vm, PatientDeathForm.vm, Post-MortomReport.vm, qrCode.vm, barCode.vm, barCodePrint.vm, bht_barcode.vm, ip_label.vm, opLabelPrint (in op templates), appointment_confirm.vm, appointmentLetter.vm, appointmentOpLabel.vm, appointment_token.vm, attendant_pass.vm, billHeader.vm, customHeader.vm, header.vm, footer.vm, risl_header.vm
- **Integration templates**: nhcx/ (claim_on_submit_template.html, coverage_eligibility_on_check_template.html, insurance_plan_on_request_template.html, preauth_on_submit_template.html + FHIR bundle JSONs), aadhar_* (aadhar_biometric_auth.vm, aadhar_otp_auth_api.vm, bhamashah_api_request.vm, janaadhar_*), rghs* (rghsTID.vm, documentUploadRghsReq.vm, opdMetadataForRghs.vm, rghsSimsData.vm), chiranjeeviEmrTID.vm
- **Specialty notes**: {specialty}_dis_form.vm/{specialty}_dis_ticket.vm families, ccmnote.vm, barthelIndexReport.vm, rabiesReport.vm

---

## 9. Frontend JS controllers (evidence of client-side modules)

abdm-module, admission-module, alliedHealth-module, appointment-list-module, appointment-module, auth-init-module, bcma-module, bed-management-module, billing-module, bioMedicalWaste-module, blood-bank-module, chemotherapy-module, clinic-module, common-module, cssd-module, cwf-module, diet-kitchen-module, file-management-module, grievance-module, incident-reporting-module, inventory-module, item-category-module, itemEnquiry-module, linen-laundry-module, nhcx-module, notification-template-module, nursing-charting-module, nursing-module, nursing-note-module, patho-booking-module, payment-transfer-module, pharmacy-module, pmrd-module, rcm-module, report-module, rmrs-module, scwf-module, transport-module, user-module, vista-module

Plus: docit-roles.js (role screens: Radiology, Lab, Nurse worklist, initial assessment, notifiable disease, transfers, new results, lab service transfer), tiuxml.js.

---

## 10. Configuration files

- `mastersConfigration.json` — master screen tree (Hospital > Unit/Department/Users/…; URLs `#masters:unitmaster/view`, `#masters:showUserList`…)
- `vitalsConfiguration.json` — vitals definitions (TEMP, qualifiers, slider ranges, units)
- `notesTitle.json` — progress-note titles (IEN, Title, consult flag, displayName)
- `tiuxml_mapping.json` — TIU form → tiles (Assessment/History/Examination…)
- `chemo_tiuxml_mapping.json`, `nhnc_tiuxml_mapping.json`, `op_tiuxml_mapping.json` — domain-specific form mappings

---

## 11. Roles/actors observed (from code: docit-roles.js, templates, service permission checks)

Evidence-based actor list (not exhaustive): Front Office/FrontOfficer (op templates: roleFrontOfficer.html), Registration clerk (patientDetailReg), Appointment desk, Cash counter operator (cashcountermaster, addPayment), Billing operator, Doctor/Physician (checkinCheckout, consult orders, prescriptions, doctor share), Nurse (nurse ward allocation, worklist, initial assessment, notes), Nurse In-charge (roleNurseInchargeWorklist), Lab technician (roleLab, sample collection, resulting), Lab supervisor (result review, report release), Radiologist (roleRadiology, radiology queue), Pharmacist (dispensing, counter sale), OT staff (surgeon, anesthetist — otscheduler staff/doctor lists), Dietician (diet kitchen), CSSD operator, Blood bank technician (requisition, cross-matching, component separation), Biomedical waste handler, Linen/laundry staff, Transport staff (driver, dispatcher, workshop), Equipment/maintenance engineer (complaints, PM), Grievance officer, Social worker (patientManagement addSocialWorker, SW forms), Mortuary staff (mortuaryBoxManagement), PMRD/records clerk (formj1/2/3, hform), RMRS authority (authoritymaster, payments, journal), RCM/DM/MOIC roles (rcm dmApprovalUpload, moicUpload), Queue manager / nursing desk (queue module), Kiosk self-service user (kiosk module), Outbound ASHA/ANM (EJS), Aasmaan field users, Patient/attendant (patient portal, attendant pass), Admin (addUser, user roles, hospital config).

---

## 12. Known gaps / UNKNOWN items

- UNKNOWN-01: models/txt package (10 files) — not individually enumerated in this pass.
- UNKNOWN-02: exact screen-permission matrix (docit roles → module mapping) — would need permissions table in DB; not in code.
- UNKNOWN-03: biReport module details (report_BI.html, report_BI_DM.html) — BI report screens, business semantics not documented in code.
- UNKNOWN-04: careplan/charting/nursingWorklist/shifthandover configs expose 0 schema keys but have templates — these are template-driven features of the nursing/clinical module (care plans, charting, shift handover); boundary of module ownership unclear.
- UNKNOWN-05: "vista-module.js", "cwf-module.js", "scwf-module.js" — VISTA (US VA legacy?) and CWF/SCWF (clinical workflow) semantics not fully determined.
- UNKNOWN-06: module `allocation` (nurse/doctor/patient allocation screens) — treated as sub-module of Nursing; config only 3 keys.
- UNKNOWN-07: `fjs-config_schemas.json` and `fjs-config_templates.json` are registries (0 keys) — mechanism unknown beyond naming.
- UNKNOWN-08: masters/ruleengine (ruleSet.json) and RuleEnginHelper — business rules engine semantics UNKNOWN.
- UNKNOWN-09: incoterms of "biReport"/"DM" — DM likely "Data Management"; unverified.
- UNKNOWN-10: kiosk module includes lab result upload (saveKioskLabResult, uploadKioskLabResult in PatientServiceImpl) — kiosk scope broader than registration/appointments.

# M17 — Features (02 §M17)

| Feature | Evidence |
|---|---|
| Donor registration & screening (voluntary/camp/replacement/autologous) | donorRegistartion; VoluntaryDonationSchema, CampDonationSchema, ReplacementDonationSchema, AutologousDonationSchema |
| Blood collection & component separation | bloodComponentSeperationSchema, bloodComponents, bloodBagComponentSeperation/Extraction routes; T_BloodBagExtraction |
| Testing | bloodtestmaster, bloodtestresultmaster; donorSerologyTestReport |
| Cross-matching & issue | bloodCrossMatchingListSchema, crossMatchingForm, issueBloodBag route; t_bloodcrossmatchingresult |
| Requisitions (patient/external hospital/external storage center/admin/camp) | patientRequisitionList(+Details), ExternalPatientRequisitionSchema, externalHospitalRequisition, externalBloodStorageCenterRequisition, addAdminRequisition, campRequisitionSchema |
| Stock (opening, bag status) | showBloodStockListSchema, addBloodBagOpeningStock, openingBloodStockSourceDetailsSchema |
| Indents between units | showBloodBankIndentListSchema, raiseIndent, issueIndent/receiveIndent/rejectIndent/cancelIndent |
| Discard | showBloodDiscardListSchema, bloodBagDiscard route; blooddiscardreasonmaster |
| Bag returns | bagReturnDetailsSchema, bloodBagReturnSchema, showBagsReturnListSchema, retrunBloodbag |
| e-Raktkosh linkage | eraktkoshRequest route, eraktkoshdataupdateservice |
| Cross-unit orders | crossUnitOrderSchema, crossUnitOrdersListSchema |
| Bag trail | bloodBagTrail route |
| Donor list & history | showDonorListSchema, replacementDonorHistory |
| Reorder | BloodBanKReOrder route |

## Routes (31, 03 §2.7)
donorRegistration · donorList · campDonation · patientRequisition(+external) · crossUnitOrder(s) · bloodComponent · bloodBagComponentSeperation/Extraction · bagReturns · crossMatchedBags · redCellSerology · transfusionTransmittedDiseases · crossMatchingList · newCrossMatchingTest · bloodCrossMatching · campRequisition · issueBloodBag · bloodBagDiscard · showIndentList · raiseIndent · bloodBagTrail · retrunBloodbag · eraktkoshRequest · replacementDonorHistory · BloodBanKReOrder · BloodDiscardList · bloodBagOpeningStock · addNewBloodComponent.

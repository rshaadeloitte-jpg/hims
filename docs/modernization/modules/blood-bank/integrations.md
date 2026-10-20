# M17 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| e-Raktkosh (national blood bank network) | Out | eraktkoshdataupdateservice, EraktkoshComponentcodeMapping, eraktkoshRequest route; eRaktkoshDonation.schedule; ApplicationJobs:1205,1383 |
| External storage centers / external hospitals | Both | externalbloodstoragecenter, externalhospitalmaster masters; district stock exchange |
| Pathology serology (M15) | In | pathoGenerateBarCodeSchemaForBloodBank; serology placement ambiguous (02 §M17 "M16 (serology?)" — likely M15) |
| Solr | In-process | patient index from BloodBankServiceImpl:1429-1625, PatientRequisitionServiceImpl:746-854 |

## Scheduled jobs (4 + e-Raktkosh)
- updateExpiredBloodRequisitionStatusJob; updateExpiryStatusOfTheBloodBagsJob (23:00); decrossBloodBag (00:00); donor auto-cancel (ApplicationJobs:832-848); eRaktkosh nightly.

## Reports (17 report routes + vm)
bloodBankIndentDetails.vm · bloodBagDemandSlip.vm · bloodCrossMatchingreport.vm · bloodDonationCertificate(+New).vm · bagLabel.vm · + reports module: bloodBagIssueReport, bloodBagTypeDetailsReport, bloodDonorRegistrationReport, bloodGroupWiseStaticReport, donationBloodGroupWiseReport, donorSerologyTestReport, showBloodBagBalanceSheetReport, showBloodBankBalanceSheetReport, showBloodBankReqBalanceSheetReport, showCampBagIssueDetailsReport, showCampDonorDetailsReport, showCampDonorStatisticReport, showCrossMatchRegisterReport, showDailyBloodstockBookReport, showDonationStatusReport, showDonorPhysicalStatusReport, showHemoglobinopathyTransfusionReport, showListOfRecieptforCampReport, showReceivedBackBloodReport, showSupplyComponentTypeWiseReport.

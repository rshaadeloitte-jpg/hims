# M15 — Features (02 §M15)

| Feature | Evidence |
|---|---|
| Lab order booking | pathoOrderSchema; order auto-creation at billing (BR-BILL-003) |
| Sample collection & barcode | sampleCollectSchema, pathoGenerateBarCodeSchema(+ForBloodBank); qrcodeScanner.html |
| Sample acceptance/rejection | acceptReject(Filter/ReportDetails/Report)Schema; sampleRejectionSchema; discard reasons |
| Result entry (resulting) | resultingDetail/Report/TemplateReportSchema |
| Result review (reviewer) | resultReview(+Filter/Microbiology/ReportDetails/Report/TemplateReportDetails/TemplateReport)Schema |
| Report release | reportRelease(+Details/Filter/Microbiology/Report/Template)Schema |
| Critical alerts | criticalAlert(Filter/ResultReportDetails/ResultReport/Result)Schema |
| Microbiology (culture/sensitivity/antibiotic) | microbiology-schema, microbiologyAntibiotic-schema; organismmachinmap, pathantibioticmaster |
| Machine interfacing (KDL) | pathoKdlVerify, machineWiseResultReview(+Details/Filter)Schema; machinemaster, machinetestmaster |
| Test master/profiles/templates | pathotestmaster, pathoprofilemaster, pathotemplatemaster, pathosubtestmaster, pathoGroupingAndTest |
| Lab workload | showLabLoadListSchema, showPatientWorkList |
| Agency master | pathagencymaster |
| COVID status | pathoShowCovidStatus, showCovidLabList |
| Sample suitability | pathosamplesutability; sampleRetestSchema |

## Routes (14+, 03 §2.6)
`pathology:lab` · `sampleCollection/:opdipd/:mrnNo/:catg(/:date)` · `sampleAcceptReject/...` · `sampleResult/...` · `sampleCriticalAlert/...` · `sampleResultReview/...` · `sampleReportRelease/...` · `sampleMachineWiseResult(/:...)` · `sampleGenerateBarcode` · `LabN` · `selectedOrderType` · `printAllReport` · `workLoadList` · `groupingAndTest` · `kdlVerify` · `showStatus` · `showLabResult` · `showCovidLabList` · `updateOrderStatus` · `updateServiceStatus`.

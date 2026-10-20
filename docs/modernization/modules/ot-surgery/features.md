# M12 — Features (02 §M12)

| Feature | Evidence |
|---|---|
| OT theatre/table config | ottheatremaster, ottablemaster (masters/ot); otconfig (35 endpoints) |
| Procedure master & checklist | proceduremaster, procedurechecklist*, procedurechecklistitems, procedurecheklistdetails (masters/ot) |
| Surgery scheduling with slot/conflict checks | addNewSurgerySchema; checkDoctorAllreadyBookedForSameTime, checkOTAvailability |
| Anesthesia (pre/PACU/post notes) | anesthesianotes, PatientScheduleAnasthesiaDetails; pre-anaesthesia check & PAC notes |
| Surgery charges & billing estimation | addSurgCharge, getSurgeryHeadDetailsForBillEstimation; surgeryheadmaster(+details); addOTBillEstimationSchema |
| OT equipment booking | addProcedureEquipment (procedureequipmentdetails master) |
| Surgery order list from nursing | SurgeryOrderList / showSurgeryOrderList; order from nursing (isOtProcessDone=false filter) |
| Procedure performance tracking | procedurePerformanceSchema; getSurgeryCompleteList |
| Staff/doctor scheduling | otscheduler staff/doctor lists; nurseScheduleEvent, patMovementAudit routes |

## Routes (10, 03 §2.11)
`ot:surgList` · `surgSchedule` · `procPerformance` · `otSchedule/:otId/:tableId` · `otScheduleEvent` · `patMovementAudit` · `nurseScheduleEvent` · `itemDetailsEvent` · `surgSchdule/:otId/:tableId` · `addNewSurg` · `surgDetails`.

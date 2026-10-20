# M16 — Database (05 §3, 10 §2.1 row 8)

## Tables
| Table | Notes |
|---|---|
| T_RADORDERBOOKING (+DETAILS) | Rad orders; IsImageCaptured flag |
| masters/radiology (9) | radtestmaster, radtestcategory, radmodalitymaster, radtemplatemaster, radtemplateresult, radfilmsize, radCabinMaster, imagediscardreasonmaster |

## Stored procedures
- `CIMS_ADDRADORDERBOOKING` (order booking).

## Migration notes (10 §2.1 row 8)
- Target owns T_RADORDERBOOKING(+DETAILS), rad masters.
- Emits `RadOrderCreated`, `RadResultFinalized`; consumes OrderCreated.
- Image storage: MinIO disabled by default (`minio.enabled=false`) → filesystem fallback — storage decision required (07 §3.1 #13).

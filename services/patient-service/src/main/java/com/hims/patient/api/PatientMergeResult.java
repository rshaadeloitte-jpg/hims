package com.hims.patient.api;

/** Response of POST /api/v1/patients/merge — what was re-pointed and where. */
public class PatientMergeResult {

    private Long sourceId;
    private String sourceMrno;
    private Long targetId;
    private String targetMrno;
    private int documentsRepointed;
    private int sponsorsRepointed;

    public static PatientMergeResult of(Long sourceId, String sourceMrno, Long targetId, String targetMrno,
                                        int documentsRepointed, int sponsorsRepointed) {
        PatientMergeResult r = new PatientMergeResult();
        r.sourceId = sourceId;
        r.sourceMrno = sourceMrno;
        r.targetId = targetId;
        r.targetMrno = targetMrno;
        r.documentsRepointed = documentsRepointed;
        r.sponsorsRepointed = sponsorsRepointed;
        return r;
    }

    public Long getSourceId() { return sourceId; }
    public String getSourceMrno() { return sourceMrno; }
    public Long getTargetId() { return targetId; }
    public String getTargetMrno() { return targetMrno; }
    public int getDocumentsRepointed() { return documentsRepointed; }
    public int getSponsorsRepointed() { return sponsorsRepointed; }
}

package com.hims.interop.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.ConflictException;
import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InteropRuleException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.AbhaConsent;
import com.hims.interop.domain.model.AbhaLink;
import com.hims.interop.domain.model.PhrPush;
import com.hims.interop.domain.repository.AbhaConsentRepository;
import com.hims.interop.domain.repository.AbhaLinkRepository;
import com.hims.interop.domain.repository.PhrPushRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** M38 ABHA: link a PHR-Address / Health-ID to a patient, manage consent, queue PHR push. */
@Service
public class AbhaService {
    private static final long ACTOR = 1L;
    private final AbhaLinkRepository linkRepository;
    private final AbhaConsentRepository consentRepository;
    private final PhrPushRepository phrPushRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AbhaService(AbhaLinkRepository linkRepository,
                       AbhaConsentRepository consentRepository,
                       PhrPushRepository phrPushRepository) {
        this.linkRepository = linkRepository;
        this.consentRepository = consentRepository;
        this.phrPushRepository = phrPushRepository;
    }

    // ============================================================ ABHA link

    @Transactional
    public AbhaLink createLink(String abhaAddress, String healthIdNumber, Long patientId, String txnId) {
        if (abhaAddress == null || abhaAddress.isBlank()) {
            throw new InvalidRequestException("BEXA003", "abhaAddress is required");
        }
        if (txnId == null || txnId.isBlank()) {
            throw new InvalidRequestException("BEXA003", "txnId is required");
        }
        if (linkRepository.findFirstByAbhaAddress(abhaAddress).isPresent()) {
            throw new ConflictException("ABHA_DUPLICATE",
                    "ABHA already linked: " + abhaAddress);
        }
        long id = linkRepository.nextId();
        AbhaLink link = new AbhaLink();
        link.setId(id);
        link.setAbhaAddress(abhaAddress);
        link.setHealthIdNumber(healthIdNumber);
        link.setPatientId(patientId);
        link.setTxnId(txnId);
        link.setLinkStatus(AbhaLink.STATUS_PENDING_VERIFY);
        stamp(link);
        return linkRepository.save(link);
    }

    @Transactional
    public AbhaLink verifyLink(long id, String healthIdNumber) {
        AbhaLink link = getLink(id);
        if (link.getLinkStatus() != AbhaLink.STATUS_PENDING_VERIFY) {
            throw new InteropRuleException("ABHA_STATE",
                    "Only PENDING_VERIFY links can be verified");
        }
        link.setLinkStatus(AbhaLink.STATUS_VERIFIED);
        if (healthIdNumber != null) link.setHealthIdNumber(healthIdNumber);
        link.setVerifiedAt(Timestamp.from(Instant.now()));
        stamp(link);
        return linkRepository.save(link);
    }

    @Transactional
    public AbhaLink delink(long id) {
        AbhaLink link = getLink(id);
        if (link.getLinkStatus() == AbhaLink.STATUS_DELINKED) return link;
        link.setLinkStatus(AbhaLink.STATUS_DELINKED);
        link.setDelinkedAt(Timestamp.from(Instant.now()));
        stamp(link);
        return linkRepository.save(link);
    }

    @Transactional
    public AbhaLink revoke(long id) {
        AbhaLink link = getLink(id);
        link.setLinkStatus(AbhaLink.STATUS_REVOKED);
        stamp(link);
        return linkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public AbhaLink getLink(long id) {
        return linkRepository.findById(id)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND", "ABHA link not found"));
    }
    @Transactional(readOnly = true) public List<AbhaLink> findByPatient(Long patientId) {
        return linkRepository.findByPatientId(patientId);
    }
    @Transactional(readOnly = true) public List<AbhaLink> findByStatus(Short status) {
        return linkRepository.findByLinkStatus(status);
    }

    // ============================================================ Consent

    @Transactional
    public AbhaConsent grantConsent(long abhaLinkId, String consentRequestId, String purpose,
                                     String hipId, String hiuId, Timestamp validFrom, Timestamp validTo) {
        if (validFrom == null || validTo == null || validTo.before(validFrom)) {
            throw new InvalidRequestException("BEXA003",
                    "validFrom and validTo are required and validTo must be on/after validFrom");
        }
        AbhaLink link = getLink(abhaLinkId);
        if (link.getLinkStatus() != AbhaLink.STATUS_VERIFIED) {
            throw new InteropRuleException("ABHA_STATE",
                    "Consent can only be granted on VERIFIED links");
        }
        long id = consentRepository.nextId();
        AbhaConsent c = new AbhaConsent();
        c.setId(id);
        c.setAbhaLinkId(abhaLinkId);
        c.setConsentRequestId(consentRequestId);
        c.setPurpose(purpose);
        c.setHipId(hipId);
        c.setHiuId(hiuId);
        c.setValidFrom(validFrom);
        c.setValidTo(validTo);
        c.setConsentStatus(AbhaConsent.STATUS_GRANTED);
        c.setGrantedAt(Timestamp.from(Instant.now()));
        stamp(c);
        return consentRepository.save(c);
    }

    @Transactional
    public AbhaConsent revokeConsent(long id) {
        AbhaConsent c = consentRepository.findById(id)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND", "Consent not found"));
        if (c.getConsentStatus() == AbhaConsent.STATUS_REVOKED) return c;
        c.setConsentStatus(AbhaConsent.STATUS_REVOKED);
        c.setRevokedAt(Timestamp.from(Instant.now()));
        stamp(c);
        return consentRepository.save(c);
    }

    @Transactional
    public AbhaConsent expireConsent(long id) {
        AbhaConsent c = consentRepository.findById(id)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND", "Consent not found"));
        c.setConsentStatus(AbhaConsent.STATUS_EXPIRED);
        stamp(c);
        return consentRepository.save(c);
    }

    @Transactional(readOnly = true) public List<AbhaConsent> consentsFor(long abhaLinkId) {
        return consentRepository.findByAbhaLinkId(abhaLinkId);
    }

    // ============================================================ PHR push

    @Transactional
    public PhrPush queuePush(long abhaLinkId, String payloadType, Object payload) {
        AbhaLink link = getLink(abhaLinkId);
        if (link.getLinkStatus() != AbhaLink.STATUS_VERIFIED) {
            throw new InteropRuleException("ABHA_STATE",
                    "PHR can only be pushed for VERIFIED links");
        }
        long id = phrPushRepository.nextId();
        PhrPush push = new PhrPush();
        push.setId(id);
        push.setAbhaLinkId(abhaLinkId);
        push.setPayloadType(payloadType);
        try { push.setPayload(objectMapper.writeValueAsString(payload)); }
        catch (JsonProcessingException e) { throw new InvalidRequestException("BEXA003", "payload is not serializable"); }
        push.setDocStatus(PhrPush.STATUS_PENDING);
        stamp(push);
        return phrPushRepository.save(push);
    }

    @Transactional
    public PhrPush markPushed(long id) {
        PhrPush p = phrPushRepository.findById(id)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND", "PHR push not found"));
        p.setDocStatus(PhrPush.STATUS_PUSHED);
        p.setPushedAt(Timestamp.from(Instant.now()));
        stamp(p);
        return phrPushRepository.save(p);
    }

    @Transactional
    public PhrPush markFailed(long id, String reason) {
        PhrPush p = phrPushRepository.findById(id)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND", "PHR push not found"));
        p.setDocStatus(PhrPush.STATUS_FAILED);
        p.setFailureReason(reason);
        stamp(p);
        return phrPushRepository.save(p);
    }

    @Transactional(readOnly = true) public List<PhrPush> pendingPushes(long abhaLinkId) {
        return phrPushRepository.findByAbhaLinkIdAndDocStatus(abhaLinkId, PhrPush.STATUS_PENDING);
    }

    private void stamp(com.hims.interop.domain.model.InteropBase entity) {
        entity.setAddedBy(ACTOR);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(ACTOR);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }
}

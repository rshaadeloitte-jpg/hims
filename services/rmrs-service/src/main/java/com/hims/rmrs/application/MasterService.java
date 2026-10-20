package com.hims.rmrs.application;

import com.hims.rmrs.application.exceptions.DuplicateRmrsResourceException;
import com.hims.rmrs.application.exceptions.InvalidRmrsRequestException;
import com.hims.rmrs.domain.model.AuthorityMaster;
import com.hims.rmrs.domain.model.HeadMaster;
import com.hims.rmrs.domain.model.LedgerMaster;
import com.hims.rmrs.domain.repository.AuthorityMasterRepository;
import com.hims.rmrs.domain.repository.HeadMasterRepository;
import com.hims.rmrs.domain.repository.LedgerMasterRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Treasury masters CRUD-minimal (register WP-R5): POST/GET heads, authorities,
 * ledgers. Guards: code + name required (400 BEXA003), code uniqueness
 * (409 RMRS_DUPLICATE_CODE); ledger creation requires an EXISTING headId
 * (FK NOT NULL per register).
 */
@Service
public class MasterService {

    private static final Logger log = LoggerFactory.getLogger(MasterService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final HeadMasterRepository headRepository;
    private final AuthorityMasterRepository authorityRepository;
    private final LedgerMasterRepository ledgerRepository;

    @PersistenceContext
    private EntityManager em;

    public MasterService(HeadMasterRepository headRepository,
                         AuthorityMasterRepository authorityRepository,
                         LedgerMasterRepository ledgerRepository) {
        this.headRepository = headRepository;
        this.authorityRepository = authorityRepository;
        this.ledgerRepository = ledgerRepository;
    }

    // ------------------------------------------------------------------ heads

    @Transactional
    public HeadMaster createHead(String headCode, String headName) {
        requireCode(headCode, "headCode");
        requireName(headName, "headName");
        if (headRepository.existsByHeadCode(headCode)) {
            throw new DuplicateRmrsResourceException("RMRS_DUPLICATE_CODE",
                    "headCode already exists: " + headCode);
        }
        HeadMaster head = new HeadMaster();
        head.setHeadId(nextId("t_head_master_seq"));
        head.setHeadCode(headCode);
        head.setHeadName(headName);
        head.setIsActive(true);
        stampAudit(head);
        headRepository.save(head);
        log.info("Created head {} ({})", headCode, head.getHeadId());
        return head;
    }

    @Transactional(readOnly = true)
    public List<HeadMaster> listHeads() {
        return headRepository.findAll();
    }

    // ------------------------------------------------------------------ authorities

    @Transactional
    public AuthorityMaster createAuthority(String authorityCode, String authorityName, Long headId) {
        requireCode(authorityCode, "authorityCode");
        requireName(authorityName, "authorityName");
        if (authorityRepository.existsByAuthorityCode(authorityCode)) {
            throw new DuplicateRmrsResourceException("RMRS_DUPLICATE_CODE",
                    "authorityCode already exists: " + authorityCode);
        }
        if (headId != null && !headRepository.existsById(headId)) {
            throw new InvalidRmrsRequestException("Unknown headId for authority: " + headId);
        }
        AuthorityMaster authority = new AuthorityMaster();
        authority.setAuthorityId(nextId("t_authority_master_seq"));
        authority.setAuthorityCode(authorityCode);
        authority.setAuthorityName(authorityName);
        authority.setHeadId(headId);   // NULL allowed per register
        stampAudit(authority);
        authorityRepository.save(authority);
        log.info("Created authority {} ({})", authorityCode, authority.getAuthorityId());
        return authority;
    }

    @Transactional(readOnly = true)
    public List<AuthorityMaster> listAuthorities() {
        return authorityRepository.findAll();
    }

    // ------------------------------------------------------------------ ledgers

    @Transactional
    public LedgerMaster createLedger(String ledgerCode, String ledgerName, Long headId) {
        requireCode(ledgerCode, "ledgerCode");
        requireName(ledgerName, "ledgerName");
        if (ledgerRepository.existsByLedgerCode(ledgerCode)) {
            throw new DuplicateRmrsResourceException("RMRS_DUPLICATE_CODE",
                    "ledgerCode already exists: " + ledgerCode);
        }
        if (headId == null) {
            throw new InvalidRmrsRequestException("headId is required for a ledger (head_id FK NOT NULL)");
        }
        if (!headRepository.existsById(headId)) {
            throw new InvalidRmrsRequestException("Unknown headId for ledger: " + headId);
        }
        LedgerMaster ledger = new LedgerMaster();
        ledger.setLedgerId(nextId("t_ledger_master_seq"));
        ledger.setLedgerCode(ledgerCode);
        ledger.setLedgerName(ledgerName);
        ledger.setHeadId(headId);
        stampAudit(ledger);
        ledgerRepository.save(ledger);
        log.info("Created ledger {} under head {}", ledgerCode, headId);
        return ledger;
    }

    @Transactional(readOnly = true)
    public List<LedgerMaster> listLedgers() {
        return ledgerRepository.findAll();
    }

    // ------------------------------------------------------------------ helpers

    private static void requireCode(String code, String field) {
        if (code == null || code.isBlank()) {
            throw new InvalidRmrsRequestException(field + " is required");
        }
    }

    private static void requireName(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new InvalidRmrsRequestException(field + " is required");
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_rmrs." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private void stampAudit(com.hims.rmrs.domain.model.RmrsAuditableBase entity) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
    }
}

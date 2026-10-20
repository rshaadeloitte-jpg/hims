package com.hims.rmrs.application;

import com.hims.rmrs.application.exceptions.DuplicateRmrsResourceException;
import com.hims.rmrs.application.exceptions.InvalidRmrsRequestException;
import com.hims.rmrs.domain.model.AccountingEntry;
import com.hims.rmrs.domain.model.AuthorityMaster;
import com.hims.rmrs.domain.model.HeadMaster;
import com.hims.rmrs.domain.model.LedgerMaster;
import com.hims.rmrs.domain.model.RmrsCollection;
import com.hims.rmrs.domain.repository.AccountingEntryRepository;
import com.hims.rmrs.domain.repository.AuthorityMasterRepository;
import com.hims.rmrs.domain.repository.HeadMasterRepository;
import com.hims.rmrs.domain.repository.LedgerMasterRepository;
import com.hims.rmrs.domain.repository.RmrsCollectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — WP-R5 guards and the balanced-postings invariant, no Spring context.
 * Register assertions: receipt uniqueness (409), amount &gt; 0, head/ledger existence,
 * HEAD-LEDGER CONSISTENCY (400 BEXA003), optional authorityCode must exist, and the
 * DR/CR postings are exactly-one-sided with SUM(debit)=SUM(credit) per collection.
 * The negative outbox assertion lives at L2 (integration) — this service holds NO
 * outbox dependency by design.
 */
class CollectionServiceTest {

    private RmrsCollectionRepository collectionRepository;
    private AccountingEntryRepository entryRepository;
    private HeadMasterRepository headRepository;
    private LedgerMasterRepository ledgerRepository;
    private AuthorityMasterRepository authorityRepository;
    private CollectionService service;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        collectionRepository = mock(RmrsCollectionRepository.class);
        entryRepository = mock(AccountingEntryRepository.class);
        headRepository = mock(HeadMasterRepository.class);
        ledgerRepository = mock(LedgerMasterRepository.class);
        authorityRepository = mock(AuthorityMasterRepository.class);

        EntityManager em = mock(EntityManager.class);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        service = new CollectionService(collectionRepository, entryRepository,
                headRepository, ledgerRepository, authorityRepository);
        ReflectionTestUtils.setField(service, "em", em);

        HeadMaster head = new HeadMaster();
        head.setHeadId(1L);
        head.setHeadCode("PHARMACY");
        when(headRepository.findByHeadCode("PHARMACY")).thenReturn(Optional.of(head));

        LedgerMaster ledger = new LedgerMaster();
        ledger.setLedgerId(11L);
        ledger.setLedgerCode("PH-LEDGER");
        ledger.setHeadId(1L);
        when(ledgerRepository.findByLedgerCode("PH-LEDGER")).thenReturn(Optional.of(ledger));
        // a ledger belonging to a DIFFERENT head — for the consistency guard
        LedgerMaster other = new LedgerMaster();
        other.setLedgerId(22L);
        other.setLedgerCode("OTHER-LEDGER");
        other.setHeadId(2L);
        when(ledgerRepository.findByLedgerCode("OTHER-LEDGER")).thenReturn(Optional.of(other));

        AuthorityMaster authority = new AuthorityMaster();
        authority.setAuthorityId(5L);
        authority.setAuthorityCode("CASH-A");
        when(authorityRepository.findByAuthorityCode("CASH-A")).thenReturn(Optional.of(authority));

        when(collectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(entryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(collectionRepository.existsByReceiptNo(anyString())).thenReturn(false);
        // post-save verification: balanced sums for whatever is saved
        when(entryRepository.sumsByCollectionId(anyLong()))
                .thenAnswer(inv -> List.<Object[]>of(new Object[]{
                        new BigDecimal("100.00"), new BigDecimal("100.00")}));
        when(collectionRepository.findById(anyLong())).thenAnswer(inv ->
                Optional.of(minimalCollection(inv.getArgument(0))));
        when(entryRepository.findByCollectionIdOrderByEntryIdAsc(anyLong())).thenReturn(List.of());
    }

    private static RmrsCollection minimalCollection(long id) {
        RmrsCollection c = new RmrsCollection();
        c.setCollectionId(id);
        return c;
    }

    private static CollectionService.CollectionInput input(String receiptNo, String amount,
                                                           String headCode, String ledgerCode) {
        return new CollectionService.CollectionInput(receiptNo, LocalDate.of(2026, 2, 10),
                505L, amount == null ? null : new BigDecimal(amount),
                headCode, ledgerCode, null, "PAY-1", "counter-staff-1");
    }

    private CollectionService.CollectionInput valid() {
        return input("RCP-0001", "100.00", "PHARMACY", "PH-LEDGER");
    }

    // ------------------------------------------------------------- creation guards

    @Test
    void create_requiresReceiptNo_bexa003() {
        assertThatThrownBy(() -> service.create(input(null, "100.00", "PHARMACY", "PH-LEDGER")))
                .isInstanceOfSatisfying(InvalidRmrsRequestException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("BEXA003"))
                .hasMessageContaining("receiptNo");
    }

    @Test
    void create_requiresCollectionDate_bexa003() {
        CollectionService.CollectionInput in = new CollectionService.CollectionInput(
                "RCP-X", null, 505L, new BigDecimal("10"), "PHARMACY", "PH-LEDGER",
                null, null, null);
        assertThatThrownBy(() -> service.create(in))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("collectionDate");
    }

    @Test
    void create_rejectsNullZeroAndNegativeAmount_bexa003() {
        assertThatThrownBy(() -> service.create(input("RCP-1", null, "PHARMACY", "PH-LEDGER")))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("amount must be > 0");
        assertThatThrownBy(() -> service.create(input("RCP-1", "0", "PHARMACY", "PH-LEDGER")))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("amount must be > 0");
        assertThatThrownBy(() -> service.create(input("RCP-1", "-5.00", "PHARMACY", "PH-LEDGER")))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("amount must be > 0");
    }

    // ------------------------------------------------------------- head/ledger validation

    @Test
    void create_requiresHeadCode_bexa003() {
        assertThatThrownBy(() -> service.create(input("RCP-1", "10.00", null, "PH-LEDGER")))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("headCode is required");
    }

    @Test
    void create_unknownHeadCode_mapsTo400Bexa003() {
        assertThatThrownBy(() -> service.create(input("RCP-1", "10.00", "NOPE", "PH-LEDGER")))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("Unknown headCode: NOPE");
    }

    @Test
    void create_requiresLedgerCode_bexa003() {
        assertThatThrownBy(() -> service.create(input("RCP-1", "10.00", "PHARMACY", null)))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("ledgerCode is required");
    }

    @Test
    void create_unknownLedgerCode_mapsTo400Bexa003() {
        assertThatThrownBy(() -> service.create(input("RCP-1", "10.00", "PHARMACY", "GHOST")))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("Unknown ledgerCode: GHOST");
    }

    @Test
    void create_headLedgerMismatch_mapsTo400Bexa003() {
        // OTHER-LEDGER belongs to head 2, not to PHARMACY (head 1)
        assertThatThrownBy(() -> service.create(
                        input("RCP-1", "10.00", "PHARMACY", "OTHER-LEDGER")))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("head-ledger mismatch");
    }

    @Test
    void create_unknownAuthorityCode_mapsTo400Bexa003() {
        CollectionService.CollectionInput in = new CollectionService.CollectionInput(
                "RCP-1", LocalDate.of(2026, 2, 10), 505L, new BigDecimal("10"),
                "PHARMACY", "PH-LEDGER", "GHOST-A", null, null);
        assertThatThrownBy(() -> service.create(in))
                .isInstanceOf(InvalidRmrsRequestException.class)
                .hasMessageContaining("Unknown authorityCode: GHOST-A");
    }

    // ------------------------------------------------------------- receipt uniqueness

    @Test
    void create_duplicateReceipt_mapsTo409() {
        when(collectionRepository.existsByReceiptNo("RCP-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(input("RCP-DUP", "10.00", "PHARMACY", "PH-LEDGER")))
                .isInstanceOfSatisfying(DuplicateRmrsResourceException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("RMRS_DUPLICATE_RECEIPT"));
    }

    // ------------------------------------------------------------- balanced postings invariant

    @Test
    void create_writesExactlyOneSidedBalancedDrCrPostings() {
        RmrsCollection created = service.create(valid());

        @SuppressWarnings("unchecked")
        List<AccountingEntry> saved =
                (List<AccountingEntry>) captureSavedEntries();

        assertThat(saved).hasSize(2);
        long debitSided = saved.stream().filter(e -> e.getDebit().signum() > 0).count();
        long creditSided = saved.stream().filter(e -> e.getCredit().signum() > 0).count();
        assertThat(debitSided).isEqualTo(1);   // DR the treasury-head ledger
        assertThat(creditSided).isEqualTo(1);  // CR the collection ledger
        for (AccountingEntry e : saved) {
            assertThat(e.isOneSided()).as("entry exactly-one-sided").isTrue();
            assertThat(e.getCollectionId()).isEqualTo(created.getCollectionId());
            assertThat(e.getLedgerId()).isEqualTo(11L);   // the validated head's ledger
            assertThat(e.getEntryDate()).isEqualTo(LocalDate.of(2026, 2, 10));
        }
        BigDecimal sumDebit = saved.stream().map(AccountingEntry::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumCredit = saved.stream().map(AccountingEntry::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumDebit).isEqualByComparingTo(sumCredit);
        assertThat(sumDebit).isEqualByComparingTo("100.00");   // == collection amount
    }

    @SuppressWarnings("unchecked")
    private List<AccountingEntry> captureSavedEntries() {
        var captor = org.mockito.ArgumentCaptor.forClass((Class<Iterable<AccountingEntry>>) (Class) Iterable.class);
        verify(entryRepository).saveAll(captor.capture());
        List<AccountingEntry> out = new java.util.ArrayList<>();
        captor.getValue().forEach(out::add);
        return out;
    }

    @Test
    void assertBalanced_rejectsUnbalancedBatch() {
        AccountingEntry dr = entry(10, 0);
        AccountingEntry cr = entry(0, 7);
        assertThatThrownBy(() -> CollectionService.assertBalanced(1L, List.of(dr, cr)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUM(debit)=10 != SUM(credit)=7");
    }

    @Test
    void assertBalanced_rejectsTwoSidedEntry() {
        AccountingEntry both = entry(10, 10);
        assertThatThrownBy(() -> CollectionService.assertBalanced(1L, List.of(both)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly-one-sided");
    }

    @Test
    void assertBalanced_acceptsBalancedBatch() {
        assertThatCode(() -> CollectionService.assertBalanced(1L, List.of(entry(9, 0), entry(0, 9))))
                .doesNotThrowAnyException();
    }

    private static AccountingEntry entry(int debit, int credit) {
        AccountingEntry e = new AccountingEntry();
        e.setEntryId(1L);
        e.setCollectionId(1L);
        e.setLedgerId(11L);
        e.setDebit(new BigDecimal(debit));
        e.setCredit(new BigDecimal(credit));
        e.setEntryDate(LocalDate.of(2026, 2, 10));
        return e;
    }
}

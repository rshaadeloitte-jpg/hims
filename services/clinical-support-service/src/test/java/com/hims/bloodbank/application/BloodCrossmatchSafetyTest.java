package com.hims.bloodbank.application;

import com.hims.bloodbank.application.exceptions.BagConflictException;
import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodCrossmatchResult;
import com.hims.bloodbank.domain.model.BloodRequisition;
import com.hims.bloodbank.domain.model.BloodRequisitionDetail;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodCrossmatchResultRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionDetailRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Patient-safety verification tests for blood transfusion crossmatch and issue:
 * 1. COMPATIBLE refused for an incompatible group (409)
 * 2. Unknown group blocks
 * 3. Issue refused with PENDING
 * 4. Issue refused with INCOMPATIBLE
 * 5. Issue refused for a different patient or requisition
 * 6. Issue allowed only after COMPATIBLE
 * 7. The tester comes from the principal (never from body)
 * 8. INCOMPATIBLE releases the bag reservation
 * 9. bag.crossMatched becomes true only on COMPATIBLE
 */
class BloodCrossmatchSafetyTest {

    private BloodRequisitionRepository requisitionRepository;
    private BloodRequisitionDetailRepository detailRepository;
    private BloodBagRepository bagRepository;
    private BloodCrossmatchResultRepository crossmatchResultRepository;
    private RequisitionService service;

    private static final Instant NOW = Instant.parse("2027-06-15T10:00:00Z");
    private long idSeq = 100L;

    @BeforeEach
    void setUp() {
        requisitionRepository = mock(BloodRequisitionRepository.class);
        detailRepository = mock(BloodRequisitionDetailRepository.class);
        bagRepository = mock(BloodBagRepository.class);
        crossmatchResultRepository = mock(BloodCrossmatchResultRepository.class);

        ClockHolder clocks = new ClockHolder(Clock.fixed(NOW, ZoneId.of("UTC")));
        service = new RequisitionService(requisitionRepository, detailRepository,
                bagRepository, crossmatchResultRepository, clocks);

        EntityManager em = mock(EntityManager.class);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(any())).thenAnswer(inv -> {
            var q = mock(jakarta.persistence.Query.class);
            when(q.getSingleResult()).thenAnswer(i -> idSeq++);
            return q;
        });

        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(crossmatchResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepository.findByRequisitionId(any())).thenAnswer(inv -> {
            Long reqId = inv.getArgument(0);
            BloodRequisitionDetail d = new BloodRequisitionDetail();
            d.setDetailId(reqId != null ? reqId * 10 : 1L);
            d.setRequisitionId(reqId);
            d.setComponent("PRBC");
            d.setVolumeMl(350);
            return List.of(d);
        });
    }

    private BloodRequisition req(long id, long patientId, String bloodGroup, int orderedVolumeMl) {
        BloodRequisition r = new BloodRequisition();
        r.setRequisitionId(id);
        r.setPatientId(patientId);
        r.setBloodGroup(bloodGroup);
        r.setOrderedVolumeMl(orderedVolumeMl);
        r.setRequesteddate(Timestamp.from(NOW.minusSeconds(3600)));
        r.setStatus(BloodRequisition.STATUS_NEW);
        return r;
    }

    private BloodBag bag(long id, String bloodGroup, int volumeMl) {
        BloodBag b = new BloodBag();
        b.setBagId(id);
        b.setDonorId(10L);
        b.setBloodGroup(bloodGroup);
        b.setVolumeMl(volumeMl);
        b.setCollectedAt(Timestamp.from(NOW.minusSeconds(86400)));
        b.setExpirydate(NOW.atZone(ZoneId.of("UTC")).toLocalDate().plusDays(35));
        b.setCrossMatched(false);
        b.setIsbagexpired(false);
        b.setIssued(false);
        return b;
    }

    // ------------------------------------------------------------ 1. COMPATIBLE refused for incompatible group (409)

    @Test
    @DisplayName("COMPATIBLE refused for an incompatible group -> throws 409 BagConflictException")
    void compatibleRefused_forIncompatibleGroup_throws409() {
        // Patient is O- (can only receive from O-)
        BloodRequisition r = req(1L, 501L, "O-", 350);
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(r));

        // Bag is A+ (incompatible for O- recipient)
        BloodBag b = bag(10L, "A+", 350);
        when(bagRepository.findById(10L)).thenReturn(Optional.of(b));

        // Step 1: crossmatch initiates PENDING result and reserves bag
        BloodCrossmatchResult pending = service.crossmatch(1L, 10L);
        assertThat(pending.getResult()).isEqualTo(BloodCrossmatchResult.RESULT_PENDING);
        assertThat(b.getCrossMatched()).isFalse(); // Not crossmatched yet!
        assertThat(b.getRequisitionId()).isEqualTo(1L); // Reserved

        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        // Step 2: Attempting to record COMPATIBLE must be refused by the ABO/Rh gate
        assertThatThrownBy(() -> service.recordCrossmatchResult(1L, pending.getId(),
                "COMPATIBLE", "dr.patel", null, "Gel Card"))
                .isInstanceOf(BagConflictException.class)
                .hasMessageContaining("Incompatible blood group")
                .extracting("errorCode").isEqualTo("BLD_INCOMPATIBLE");

        // Bag crossMatched must STILL be false
        assertThat(b.getCrossMatched()).isFalse();
    }

    // ------------------------------------------------------------ 2. Unknown group blocks

    @Test
    @DisplayName("Unknown blood group on either side blocks COMPATIBLE result")
    void unknownBloodGroup_blocksCompatible() {
        // Unknown patient group "X+"
        BloodRequisition r = req(2L, 502L, "X+", 350);
        when(requisitionRepository.findById(2L)).thenReturn(Optional.of(r));

        BloodBag b = bag(20L, "O-", 350);
        when(bagRepository.findById(20L)).thenReturn(Optional.of(b));

        BloodCrossmatchResult pending = service.crossmatch(2L, 20L);
        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.recordCrossmatchResult(2L, pending.getId(),
                "COMPATIBLE", "dr.smith", null, null))
                .isInstanceOf(BagConflictException.class)
                .hasMessageContaining("Incompatible blood group");
    }

    // ------------------------------------------------------------ 3. Issue refused with PENDING

    @Test
    @DisplayName("Issue refused when crossmatch result is PENDING")
    void issueRefused_withPendingCrossmatch() {
        BloodRequisition r = req(3L, 503L, "B+", 350);
        when(requisitionRepository.findById(3L)).thenReturn(Optional.of(r));

        BloodBag b = bag(30L, "B+", 350);
        when(bagRepository.findById(30L)).thenReturn(Optional.of(b));

        // Crossmatch initiated -> PENDING
        service.crossmatch(3L, 30L);

        // Attempt issue while PENDING
        assertThatThrownBy(() -> service.issue(3L, 30L))
                .isInstanceOf(InvalidBloodBankRequestException.class)
                .hasMessageContaining("Bag 30 is not crossmatched");
    }

    // ------------------------------------------------------------ 4. Issue refused with INCOMPATIBLE

    @Test
    @DisplayName("Issue refused when crossmatch result is INCOMPATIBLE")
    void issueRefused_withIncompatibleCrossmatch() {
        BloodRequisition r = req(4L, 504L, "A+", 350);
        when(requisitionRepository.findById(4L)).thenReturn(Optional.of(r));

        BloodBag b = bag(40L, "A+", 350);
        when(bagRepository.findById(40L)).thenReturn(Optional.of(b));

        BloodCrossmatchResult pending = service.crossmatch(4L, 40L);
        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        // Record INCOMPATIBLE
        service.recordCrossmatchResult(4L, pending.getId(), "INCOMPATIBLE", "tech1", "Agglutination observed", null);

        // Bag reservation released
        assertThat(b.getRequisitionId()).isNull();
        assertThat(b.getCrossMatched()).isFalse();

        // Attempt issue must be refused
        assertThatThrownBy(() -> service.issue(4L, 40L))
                .isInstanceOf(InvalidBloodBankRequestException.class);
    }

    // ------------------------------------------------------------ 5. Issue refused for different patient/requisition

    @Test
    @DisplayName("Issue refused for a different patient or requisition")
    void issueRefused_forDifferentPatientOrRequisition() {
        // Requisition 1 for Patient 100
        BloodRequisition r1 = req(5L, 100L, "O+", 350);
        // Requisition 2 for Patient 200
        BloodRequisition r2 = req(6L, 200L, "O+", 350);
        when(requisitionRepository.findById(5L)).thenReturn(Optional.of(r1));
        when(requisitionRepository.findById(6L)).thenReturn(Optional.of(r2));

        BloodBag b = bag(50L, "O+", 350);
        when(bagRepository.findById(50L)).thenReturn(Optional.of(b));

        // Crossmatch completed for requisition 1 / patient 100
        BloodCrossmatchResult pending = service.crossmatch(5L, 50L);
        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(crossmatchResultRepository.findFirstByBagIdAndRequisitionIdAndPatientIdAndResultAndIsDecrossedFalse(
                50L, 5L, 100L, BloodCrossmatchResult.RESULT_COMPATIBLE))
                .thenReturn(Optional.of(pending));

        service.recordCrossmatchResult(5L, pending.getId(), "COMPATIBLE", "tech.alice", null, null);

        // Attempting to issue the bag against requisition 2 / patient 200 MUST be refused
        assertThatThrownBy(() -> service.issue(6L, 50L))
                .isInstanceOf(InvalidBloodBankRequestException.class)
                .hasMessageContaining("Bag 50 is not crossmatched against requisition 6");
    }

    // ------------------------------------------------------------ 6. Issue allowed only after COMPATIBLE

    @Test
    @DisplayName("Issue allowed only after COMPATIBLE result recorded")
    void issueAllowed_onlyAfterCompatible() {
        BloodRequisition r = req(7L, 707L, "AB+", 350);
        when(requisitionRepository.findById(7L)).thenReturn(Optional.of(r));

        BloodBag b = bag(70L, "O-", 350); // O- is universal donor to AB+
        when(bagRepository.findById(70L)).thenReturn(Optional.of(b));
        when(bagRepository.findByRequisitionId(7L)).thenReturn(new ArrayList<>(List.of(b)));

        // Step 1: crossmatch (PENDING)
        BloodCrossmatchResult pending = service.crossmatch(7L, 70L);
        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        // Issue before result -> BLOCKED
        assertThatThrownBy(() -> service.issue(7L, 70L))
                .isInstanceOf(InvalidBloodBankRequestException.class);

        // Step 2: Record COMPATIBLE result
        when(crossmatchResultRepository.findFirstByBagIdAndRequisitionIdAndPatientIdAndResultAndIsDecrossedFalse(
                70L, 7L, 707L, BloodCrossmatchResult.RESULT_COMPATIBLE))
                .thenReturn(Optional.of(pending));

        BloodCrossmatchResult compResult = service.recordCrossmatchResult(7L, pending.getId(),
                "COMPATIBLE", "dr.who", "Clear tube", "Coombs");

        assertThat(compResult.getResult()).isEqualTo(BloodCrossmatchResult.RESULT_COMPATIBLE);
        assertThat(b.getCrossMatched()).isTrue();

        // Step 3: Issue now succeeds!
        BloodBag issued = service.issue(7L, 70L);
        assertThat(issued.getIssued()).isTrue();
        assertThat(issued.getIssuedRequisitionId()).isEqualTo(7L);
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_ISSUED);
    }

    // ------------------------------------------------------------ 7. Tester comes from principal

    @Test
    @DisplayName("Tester recorded in crossmatch result comes strictly from principal parameter")
    void testerComesFromPrincipal() {
        BloodRequisition r = req(8L, 808L, "A+", 350);
        when(requisitionRepository.findById(8L)).thenReturn(Optional.of(r));

        BloodBag b = bag(80L, "A+", 350);
        when(bagRepository.findById(80L)).thenReturn(Optional.of(b));

        BloodCrossmatchResult pending = service.crossmatch(8L, 80L);
        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        String authenticatedPrincipal = "lab_scientist_77";
        BloodCrossmatchResult result = service.recordCrossmatchResult(8L, pending.getId(),
                "COMPATIBLE", authenticatedPrincipal, "Normal test", "Gel Test");

        assertThat(result.getTestedBy()).isEqualTo("lab_scientist_77");
    }

    // ------------------------------------------------------------ 8. INCOMPATIBLE releases bag reservation

    @Test
    @DisplayName("INCOMPATIBLE result releases the bag reservation so it can be used elsewhere")
    void incompatibleReleasesBagReservation() {
        BloodRequisition r = req(9L, 909L, "B+", 350);
        when(requisitionRepository.findById(9L)).thenReturn(Optional.of(r));

        BloodBag b = bag(90L, "B+", 350);
        when(bagRepository.findById(90L)).thenReturn(Optional.of(b));

        // Reserve bag onto requisition 9
        BloodCrossmatchResult pending = service.crossmatch(9L, 90L);
        assertThat(b.getRequisitionId()).isEqualTo(9L);
        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        // Laboratory serological test finds INCOMPATIBLE
        BloodCrossmatchResult recorded = service.recordCrossmatchResult(9L, pending.getId(),
                "INCOMPATIBLE", "lab_tech", "Agglutination in tube 2", "Saline");

        assertThat(recorded.getResult()).isEqualTo(BloodCrossmatchResult.RESULT_INCOMPATIBLE);
        // Reservation is released!
        assertThat(b.getRequisitionId()).isNull();
        assertThat(b.getCrossMatched()).isFalse();
        assertThat(b.getCrossMatchedTestDate()).isNull();
    }

    // ------------------------------------------------------------ 9. Decross marks crossmatch result as decrossed

    @Test
    @DisplayName("Requisition cancellation decrosses active crossmatch results")
    void cancelRequisition_decrossesResults() {
        BloodRequisition r = req(10L, 1010L, "O+", 350);
        when(requisitionRepository.findById(10L)).thenReturn(Optional.of(r));

        BloodBag b = bag(100L, "O+", 350);
        when(bagRepository.findById(100L)).thenReturn(Optional.of(b));
        when(bagRepository.findByRequisitionId(10L)).thenReturn(List.of(b));

        BloodCrossmatchResult result = service.crossmatch(10L, 100L);
        result.setResult(BloodCrossmatchResult.RESULT_COMPATIBLE);
        b.setCrossMatched(true);

        when(crossmatchResultRepository.findByRequisitionId(10L)).thenReturn(List.of(result));

        // Cancel requisition
        service.cancel(10L);

        // Result marked decrossed
        assertThat(result.getIsDecrossed()).isTrue();
        // Bag unlinked
        assertThat(b.getCrossMatched()).isFalse();
        assertThat(b.getRequisitionId()).isNull();
    }

    // ------------------------------------------------------------ 10. No component blocks COMPATIBLE

    @Test
    @DisplayName("Requisition with no component blocks COMPATIBLE with BLD_COMPONENT_UNKNOWN")
    void requisitionWithNoComponent_blocksCompatibleWithBldComponentUnknown() {
        BloodRequisition r = req(11L, 1011L, "O+", 350);
        when(requisitionRepository.findById(11L)).thenReturn(Optional.of(r));

        BloodBag b = bag(110L, "O+", 350);
        when(bagRepository.findById(110L)).thenReturn(Optional.of(b));

        BloodCrossmatchResult pending = service.crossmatch(11L, 110L);
        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        // No component can be determined
        when(detailRepository.findByRequisitionId(11L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.recordCrossmatchResult(11L, pending.getId(),
                "COMPATIBLE", "dr.patel", null, null))
                .isInstanceOf(BagConflictException.class)
                .hasMessageContaining("Cannot record COMPATIBLE: blood component is unknown")
                .extracting("errorCode").isEqualTo("BLD_COMPONENT_UNKNOWN");
    }

    // ------------------------------------------------------------ 11. O whole blood to A+ patient refused

    @Test
    @DisplayName("O whole blood to an A+ patient is refused")
    void oWholeBloodToAPositivePatientIsRefused() {
        // Patient A+
        BloodRequisition r = req(12L, 1012L, "A+", 350);
        when(requisitionRepository.findById(12L)).thenReturn(Optional.of(r));

        BloodRequisitionDetail d = new BloodRequisitionDetail();
        d.setDetailId(120L);
        d.setRequisitionId(12L);
        d.setComponent("WHOLE BLOOD");
        d.setVolumeMl(350);
        when(detailRepository.findByRequisitionId(12L)).thenReturn(List.of(d));

        // Donor O+ whole blood
        BloodBag b = bag(120L, "O+", 350);
        when(bagRepository.findById(120L)).thenReturn(Optional.of(b));

        BloodCrossmatchResult pending = service.crossmatch(12L, 120L);
        when(crossmatchResultRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.recordCrossmatchResult(12L, pending.getId(),
                "COMPATIBLE", "dr.patel", null, null))
                .isInstanceOf(BagConflictException.class)
                .hasMessageContaining("Incompatible blood group")
                .extracting("errorCode").isEqualTo("BLD_INCOMPATIBLE");
    }
}

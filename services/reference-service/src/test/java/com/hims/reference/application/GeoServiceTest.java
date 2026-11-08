package com.hims.reference.application;

import com.hims.reference.domain.model.GeoVillage;
import com.hims.reference.domain.repository.GeoBlockRepository;
import com.hims.reference.domain.repository.GeoDistrictRepository;
import com.hims.reference.domain.repository.GeoSubDistrictRepository;
import com.hims.reference.domain.repository.GeoVillageRepository;
import com.hims.reference.domain.repository.LgdLocalBodyRepository;
import com.hims.reference.domain.repository.LgdVillageGramPanchayatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoServiceTest {

    @Mock private GeoDistrictRepository districtRepository;
    @Mock private GeoSubDistrictRepository subDistrictRepository;
    @Mock private GeoVillageRepository villageRepository;
    @Mock private GeoBlockRepository blockRepository;
    @Mock private LgdVillageGramPanchayatRepository gpMappingRepository;
    @Mock private LgdLocalBodyRepository localBodyRepository;

    private GeoService service;

    @BeforeEach
    void setUp() {
        service = new GeoService(districtRepository, subDistrictRepository, villageRepository,
                blockRepository, gpMappingRepository, localBodyRepository);
    }

    @Test
    void resolveLimit_defaultsAndCaps() {
        assertThat(GeoService.resolveLimit(null)).isEqualTo(GeoService.DEFAULT_LIMIT);
        assertThat(GeoService.resolveLimit(0)).isEqualTo(GeoService.DEFAULT_LIMIT);
        assertThat(GeoService.resolveLimit(-5)).isEqualTo(GeoService.DEFAULT_LIMIT);
        assertThat(GeoService.resolveLimit(10)).isEqualTo(10);
        assertThat(GeoService.resolveLimit(300)).isEqualTo(GeoService.MAX_LIMIT);
    }

    @Test
    void villages_noQ_usesUnfilteredPage() {
        GeoVillage v = new GeoVillage();
        v.setId(1L);
        v.setName("Alpha");
        when(villageRepository.findBySubDistrictId(eq(5L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

        List<GeoVillage> result = service.villages(5L, null, 10);

        assertThat(result).hasSize(1);
        verify(villageRepository).findBySubDistrictId(eq(5L), any(PageRequest.class));
    }

    @Test
    void villages_qFilter_usesCaseInsensitiveQuery() {
        GeoVillage v = new GeoVillage();
        v.setId(2L);
        v.setName("Beta");
        when(villageRepository.findBySubDistrictIdAndNameContainingIgnoreCase(eq(5L), eq("be"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

        List<GeoVillage> result = service.villages(5L, "be", 500);

        assertThat(result).hasSize(1);
        verify(villageRepository).findBySubDistrictIdAndNameContainingIgnoreCase(eq(5L), eq("be"), any(PageRequest.class));
    }

    @Test
    void villages_limitCap_applied() {
        when(villageRepository.findBySubDistrictId(eq(5L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.villages(5L, "", 999);
        org.mockito.ArgumentCaptor<PageRequest> captor = org.mockito.ArgumentCaptor.forClass(PageRequest.class);
        verify(villageRepository).findBySubDistrictId(eq(5L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(GeoService.MAX_LIMIT);
    }
}

package com.hims.reference.application;

import com.hims.reference.domain.model.GeoBlock;
import com.hims.reference.domain.model.GeoDistrict;
import com.hims.reference.domain.model.GeoSubDistrict;
import com.hims.reference.domain.model.GeoVillage;
import com.hims.reference.domain.model.LgdLocalBody;
import com.hims.reference.domain.model.LgdVillageGramPanchayat;
import com.hims.reference.domain.repository.GeoBlockRepository;
import com.hims.reference.domain.repository.GeoDistrictRepository;
import com.hims.reference.domain.repository.GeoSubDistrictRepository;
import com.hims.reference.domain.repository.GeoVillageRepository;
import com.hims.reference.domain.repository.LgdLocalBodyRepository;
import com.hims.reference.domain.repository.LgdVillageGramPanchayatRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GeoService {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private final GeoDistrictRepository districtRepository;
    private final GeoSubDistrictRepository subDistrictRepository;
    private final GeoVillageRepository villageRepository;
    private final GeoBlockRepository blockRepository;
    private final LgdVillageGramPanchayatRepository gpMappingRepository;
    private final LgdLocalBodyRepository localBodyRepository;

    public GeoService(GeoDistrictRepository districtRepository,
                      GeoSubDistrictRepository subDistrictRepository,
                      GeoVillageRepository villageRepository,
                      GeoBlockRepository blockRepository,
                      LgdVillageGramPanchayatRepository gpMappingRepository,
                      LgdLocalBodyRepository localBodyRepository) {
        this.districtRepository = districtRepository;
        this.subDistrictRepository = subDistrictRepository;
        this.villageRepository = villageRepository;
        this.blockRepository = blockRepository;
        this.gpMappingRepository = gpMappingRepository;
        this.localBodyRepository = localBodyRepository;
    }

    public List<GeoDistrict> districtsByState(Long stateId) {
        return districtRepository.findByStateIdOrderByNameAsc(stateId);
    }

    public List<GeoSubDistrict> subDistrictsByDistrict(Long districtId) {
        return subDistrictRepository.findByDistrictIdOrderByNameAsc(districtId);
    }

    public List<GeoVillage> villages(Long subDistrictId, String q, Integer limit) {
        int effective = resolveLimit(limit);
        PageRequest page = PageRequest.of(0, effective);
        if (q == null || q.isBlank()) {
            return villageRepository.findBySubDistrictId(subDistrictId, page).getContent();
        }
        return villageRepository.findBySubDistrictIdAndNameContainingIgnoreCase(subDistrictId, q.trim(), page).getContent();
    }

    public List<GeoBlock> blocksByDistrict(Long districtId) {
        return blockRepository.findByDistrictIdOrderByNameAsc(districtId);
    }

    public List<LgdLocalBody> gramPanchayatsByVillage(Long villageId) {
        List<Long> ids = gpMappingRepository.findByVillageId(villageId).stream()
                .map(LgdVillageGramPanchayat::getLocalBodyId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return localBodyRepository.findByIdIn(ids);
    }

    public static int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
    }
}

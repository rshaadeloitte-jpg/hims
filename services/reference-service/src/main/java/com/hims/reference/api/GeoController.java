package com.hims.reference.api;

import com.hims.reference.application.GeoService;
import com.hims.reference.domain.model.GeoBlock;
import com.hims.reference.domain.model.GeoDistrict;
import com.hims.reference.domain.model.GeoSubDistrict;
import com.hims.reference.domain.model.GeoVillage;
import com.hims.reference.domain.model.LgdLocalBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/geo")
@Tag(name = "Geo (LGD)", description = "Read-only LGD geographic hierarchy endpoints")
public class GeoController {

    private final GeoService service;

    public GeoController(GeoService service) {
        this.service = service;
    }

    @GetMapping("/states/{stateId}/districts")
    @Operation(summary = "Districts for a state")
    public List<GeoDistrict> districts(@PathVariable Long stateId) {
        return service.districtsByState(stateId);
    }

    @GetMapping("/districts/{districtId}/sub-districts")
    @Operation(summary = "Sub-districts for a district")
    public List<GeoSubDistrict> subDistricts(@PathVariable Long districtId) {
        return service.subDistrictsByDistrict(districtId);
    }

    @GetMapping("/sub-districts/{subDistrictId}/villages")
    @Operation(summary = "Villages for a sub-district (q filter, limit cap)")
    public List<GeoVillage> villages(@PathVariable Long subDistrictId,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) Integer limit) {
        return service.villages(subDistrictId, q, limit);
    }

    @GetMapping("/districts/{districtId}/blocks")
    @Operation(summary = "Blocks for a district")
    public List<GeoBlock> blocks(@PathVariable Long districtId) {
        return service.blocksByDistrict(districtId);
    }

    @GetMapping("/villages/{villageId}/gram-panchayats")
    @Operation(summary = "Gram panchayats for a village")
    public List<LgdLocalBody> gramPanchayats(@PathVariable Long villageId) {
        return service.gramPanchayatsByVillage(villageId);
    }
}

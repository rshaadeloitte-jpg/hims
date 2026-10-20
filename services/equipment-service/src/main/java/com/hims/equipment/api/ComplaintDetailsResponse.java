package com.hims.equipment.api;

import com.hims.equipment.domain.model.EquipmentComplaint;
import com.hims.equipment.domain.model.EquipmentComplaintDetails;
import com.hims.equipment.domain.model.EquipmentComplaintResolution;

import java.util.List;

/** Complaint-details envelope: header + line items + resolutions (chronological). */
public record ComplaintDetailsResponse(EquipmentComplaint complaint,
                                       List<EquipmentComplaintDetails> details,
                                       List<EquipmentComplaintResolution> resolutions) {
}

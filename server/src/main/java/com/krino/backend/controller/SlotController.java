package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.service.SlotService;
import com.krino.backend.utility.SortWhitelist;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Slots")
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    private static final SortWhitelist SORT_WHITELIST = SortWhitelist.of(
            "id", "interviewDate", "startTime", "endTime", "available", "createdDate", "lastModifiedDate");

    @PostMapping
    @PreAuthorize("hasAuthority('slot:create')")
    public ResponseEntity<SlotResponseDTO> createSlot(@Valid @RequestBody SlotRequestDTO slotRequestDTO) {
        SlotResponseDTO createdSlot = slotService.createSlot(slotRequestDTO);
        return ResponseEntity.created(URI.create("/api/slots/" + createdSlot.getId())).body(createdSlot);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('slot:read')")
    public ResponseEntity<SlotResponseDTO> getSlotByPublicId(@PathVariable UUID publicId) {
        SlotResponseDTO slot = slotService.getSlotByPublicId(publicId);
        return ResponseEntity.ok(slot);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PageResponse<SlotResponseDTO>> getAllSlots(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<SlotResponseDTO> slots = slotService.getAllSlots(SORT_WHITELIST.sanitize(pageable));
        return ResponseEntity.ok(slots);
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('slot:update')")
    public ResponseEntity<SlotResponseDTO> updateSlot(@PathVariable UUID publicId, @Valid @RequestBody SlotUpdateDTO slotUpdateDTO) {
        SlotResponseDTO updatedSlot = slotService.updateSlot(publicId, slotUpdateDTO);
        return ResponseEntity.ok(updatedSlot);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('slot:update')")
    public ResponseEntity<SlotResponseDTO> patchSlot(@PathVariable UUID publicId, @Valid @RequestBody SlotUpdateDTO slotUpdateDTO) {
        SlotResponseDTO patchedSlot = slotService.patchSlot(publicId, slotUpdateDTO);
        return ResponseEntity.ok(patchedSlot);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('slot:delete')")
    public ResponseEntity<Void> deleteSlot(@PathVariable UUID publicId) {
        slotService.deleteSlot(publicId);
        return ResponseEntity.noContent().build();
    }
}

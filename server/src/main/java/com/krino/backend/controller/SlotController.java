package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController
{

    private final SlotService slotService;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_SLOT')")
    public ResponseEntity<SlotResponseDTO> createSlot(@Valid @RequestBody SlotRequestDTO slotRequestDTO)
    {
        SlotResponseDTO createdSlot = slotService.createSlot(slotRequestDTO);
        return ResponseEntity.created(URI.create("/api/slots/" + createdSlot.getId())).body(createdSlot);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_READ_SLOT')")
    public ResponseEntity<SlotResponseDTO> getSlotByPublicId(@PathVariable("publicId") UUID publicId)
    {
        SlotResponseDTO slot = slotService.getSlotByPublicId(publicId);
        return ResponseEntity.ok(slot);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_READ_SLOT')")
    public ResponseEntity<PageResponse<SlotResponseDTO>> getAllSlots(@PageableDefault(size = 20, sort = "id") Pageable pageable)
    {
        PageResponse<SlotResponseDTO> slots = slotService.getAllSlots(pageable);
        return ResponseEntity.ok(slots);
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_SLOT')")
    public ResponseEntity<SlotResponseDTO> updateSlot(@PathVariable("publicId") UUID publicId, @Valid @RequestBody SlotUpdateDTO slotUpdateDTO)
    {
        SlotResponseDTO updatedSlot = slotService.updateSlot(publicId, slotUpdateDTO);
        return ResponseEntity.ok(updatedSlot);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_SLOT')")
    public ResponseEntity<SlotResponseDTO> patchSlot(@PathVariable("publicId") UUID publicId, @RequestBody SlotUpdateDTO slotUpdateDTO)
    {
        SlotResponseDTO patchedSlot = slotService.patchSlot(publicId, slotUpdateDTO);
        return ResponseEntity.ok(patchedSlot);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_DELETE_SLOT')")
    public ResponseEntity<Void> deleteSlot(@PathVariable("publicId") UUID publicId)
    {
        slotService.deleteSlot(publicId);
        return ResponseEntity.noContent().build();
    }
}

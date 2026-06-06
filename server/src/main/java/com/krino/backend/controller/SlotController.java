package com.krino.backend.controller;

import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.slot.SlotUpdateDTO;
import com.krino.backend.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController
{

    private final SlotService slotService;

    @PostMapping("/create")
    public ResponseEntity<SlotResponseDTO> createSlot(@Valid @RequestBody SlotRequestDTO slotRequestDTO)
    {
        SlotResponseDTO createdSlot = slotService.createSlot(slotRequestDTO);
        return ResponseEntity.ok(createdSlot);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SlotResponseDTO> getSlotById(@PathVariable("id") Long id)
    {
        SlotResponseDTO slot = slotService.getSlotById(id);
        return ResponseEntity.ok(slot);
    }

    @GetMapping
    public ResponseEntity<List<SlotResponseDTO>> getAllSlots()
    {
        List<SlotResponseDTO> slots = slotService.getAllSlots();
        return ResponseEntity.ok(slots);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SlotResponseDTO> updateSlot(@PathVariable("id") Long id, @Valid @RequestBody SlotUpdateDTO slotUpdateDTO)
    {
        SlotResponseDTO updatedSlot = slotService.updateSlot(id, slotUpdateDTO);
        return ResponseEntity.ok(updatedSlot);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SlotResponseDTO> patchSlot(@PathVariable("id") Long id, @RequestBody SlotUpdateDTO slotUpdateDTO)
    {
        SlotResponseDTO patchedSlot = slotService.patchSlot(id, slotUpdateDTO);
        return ResponseEntity.ok(patchedSlot);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSlot(@PathVariable("id") Long id)
    {
        slotService.deleteSlot(id);
        return ResponseEntity.ok("Slot deleted successfully");
    }
}

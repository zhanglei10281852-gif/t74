package com.mediation.controller;

import com.mediation.dto.MediationOrganizationDTO;
import com.mediation.entity.MediationOrganization;
import com.mediation.entity.MediationOrganization.AreaType;
import com.mediation.repository.MediationOrganizationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class MediationOrganizationController {

    private final MediationOrganizationRepository organizationRepository;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody MediationOrganizationDTO dto) {
        AreaType areaType;
        try {
            areaType = AreaType.valueOf(dto.getAreaType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的区域类型"));
        }

        MediationOrganization org = MediationOrganization.builder()
                .name(dto.getName())
                .area(dto.getArea())
                .areaType(areaType)
                .director(dto.getDirector())
                .phone(dto.getPhone())
                .build();

        MediationOrganization saved = organizationRepository.save(org);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<MediationOrganization>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String areaType) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MediationOrganization> result;

        if (areaType != null) {
            AreaType at = AreaType.valueOf(areaType);
            List<MediationOrganization> list = organizationRepository.findByAreaType(at);
            long total = list.size();
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), (int) total);
            result = new org.springframework.data.domain.PageImpl<>(
                    list.subList(start, Math.max(start, end)), pageable, total);
        } else {
            result = organizationRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<List<MediationOrganization>> listAll() {
        return ResponseEntity.ok(organizationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<MediationOrganization> orgOpt = organizationRepository.findById(id);
        return orgOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody MediationOrganizationDTO dto) {
        Optional<MediationOrganization> orgOpt = organizationRepository.findById(id);
        if (orgOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AreaType areaType;
        try {
            areaType = AreaType.valueOf(dto.getAreaType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的区域类型"));
        }

        MediationOrganization org = orgOpt.get();
        org.setName(dto.getName());
        org.setArea(dto.getArea());
        org.setAreaType(areaType);
        org.setDirector(dto.getDirector());
        org.setPhone(dto.getPhone());

        MediationOrganization saved = organizationRepository.save(org);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!organizationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        organizationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

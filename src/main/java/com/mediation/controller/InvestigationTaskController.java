package com.mediation.controller;

import com.mediation.entity.InvestigationTask;
import com.mediation.entity.InvestigationTask.TaskStatus;
import com.mediation.repository.InvestigationTaskRepository;
import com.mediation.service.InvestigationTaskScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/investigation-tasks")
@RequiredArgsConstructor
public class InvestigationTaskController {

    private final InvestigationTaskRepository taskRepository;
    private final InvestigationTaskScheduler taskScheduler;

    @GetMapping
    public ResponseEntity<Page<InvestigationTask>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long mediatorId,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String taskMonth) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InvestigationTask> result;

        if (status != null) {
            TaskStatus ts = TaskStatus.valueOf(status);
            result = taskRepository.findByStatus(ts, pageable);
        } else if (mediatorId != null) {
            result = taskRepository.findByMediatorId(mediatorId, pageable);
        } else if (organizationId != null) {
            result = taskRepository.findByOrganizationId(organizationId, pageable);
        } else {
            result = taskRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<InvestigationTask> taskOpt = taskRepository.findById(id);
        return taskOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/trigger-generate")
    public ResponseEntity<?> triggerGenerateTasks() {
        taskScheduler.generateMonthlyTasks();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger-overdue")
    public ResponseEntity<?> triggerMarkOverdue() {
        taskScheduler.markOverdueTasks();
        return ResponseEntity.ok().build();
    }
}

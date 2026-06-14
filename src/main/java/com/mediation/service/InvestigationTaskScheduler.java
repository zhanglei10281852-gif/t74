package com.mediation.service;

import com.mediation.entity.InvestigationTask;
import com.mediation.entity.InvestigationTask.TaskStatus;
import com.mediation.entity.MediationOrganization;
import com.mediation.entity.Mediator;
import com.mediation.entity.Mediator.MediatorStatus;
import com.mediation.repository.InvestigationTaskRepository;
import com.mediation.repository.MediationOrganizationRepository;
import com.mediation.repository.MediatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationTaskScheduler {

    private final InvestigationTaskRepository investigationTaskRepository;
    private final MediationOrganizationRepository mediationOrganizationRepository;
    private final MediatorRepository mediatorRepository;

    @Scheduled(cron = "0 0 1 1 * ?")
    @Transactional
    public void generateMonthlyTasks() {
        log.info("开始生成本月排查任务...");
        String taskMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate deadline = LocalDate.now().withDayOfMonth(15);

        List<MediationOrganization> organizations = mediationOrganizationRepository.findAll();
        int count = 0;

        for (MediationOrganization org : organizations) {
            if (investigationTaskRepository.existsByOrganizationIdAndTaskMonth(org.getId(), taskMonth)) {
                continue;
            }

            List<Mediator> mediators = mediatorRepository.findAll();
            List<Mediator> activeMediators = mediators.stream()
                    .filter(m -> m.getStatus() == MediatorStatus.在岗)
                    .toList();

            if (activeMediators.isEmpty()) {
                log.warn("组织 {} 无在岗调解员，跳过任务生成", org.getName());
                continue;
            }

            Mediator assignedMediator = activeMediators.get(new Random().nextInt(activeMediators.size()));

            InvestigationTask task = InvestigationTask.builder()
                    .taskNo(generateTaskNo())
                    .organizationId(org.getId())
                    .organizationName(org.getName())
                    .mediatorId(assignedMediator.getId())
                    .mediatorName(assignedMediator.getName())
                    .taskMonth(taskMonth)
                    .investigationScope(org.getArea())
                    .deadline(deadline)
                    .status(TaskStatus.待提交)
                    .build();

            investigationTaskRepository.save(task);
            count++;
        }

        log.info("本月排查任务生成完成，共生成 {} 个任务", count);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void markOverdueTasks() {
        log.info("开始检查逾期排查任务...");
        LocalDate today = LocalDate.now();
        List<InvestigationTask> overdueTasks = investigationTaskRepository.findOverdueTasks(today);

        int count = 0;
        for (InvestigationTask task : overdueTasks) {
            task.setStatus(TaskStatus.逾期);
            investigationTaskRepository.save(task);
            count++;
        }

        log.info("逾期任务标记完成，共标记 {} 个逾期任务", count);
    }

    private String generateTaskNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "T" + date + random;
    }
}

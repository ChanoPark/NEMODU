package com.dnd.ground.global.batch.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * @description 챌린지 시작/종료 Batch Job을 실행시키기 위한 스케줄러
 * @author  박찬호
 * @since   2023-05-02
 * @updated 1. 챌린지 시작/종료 Batch Job을 실행시키기 위한 스케줄러 정의
 *          - 2023-05-02 박찬호
 */

@Component
@Slf4j
public class ChallengeScheduler {

    public ChallengeScheduler(JobLauncher jobLauncher,
                              @Qualifier("challenge_start_job") Job challenge_start_job,
                              @Qualifier("challenge_end_job") Job challenge_end_job) {
        this.jobLauncher = jobLauncher;
        this.challenge_start_job = challenge_start_job;
        this.challenge_end_job = challenge_end_job;
    }
    private final JobLauncher jobLauncher;

    private final Job challenge_start_job;

    private final Job challenge_end_job;

    private static final String JOB_PARAM_DATE = "requestDate";

    @Scheduled(cron = "0 0 0 * * *")
    public void challengeStartJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters = new JobParameters(
                Collections.singletonMap(JOB_PARAM_DATE,  new JobParameter(String.valueOf(LocalDateTime.now()))));

        log.info("---- 챌린지 시작 배치 실행");

        jobLauncher.run(challenge_start_job, jobParameters);
        log.info("---- 챌린지 시작 배치 종료");
    }

    @Scheduled(cron = "0 0 0 ? * MON")
    public void challengeEndJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters = new JobParameters(
                Collections.singletonMap(JOB_PARAM_DATE,  new JobParameter(String.valueOf(LocalDateTime.now()))));

        log.info("---- 챌린지 종료 배치 실행");
        jobLauncher.run(challenge_end_job, jobParameters);
        log.info("---- 챌린지 종료 배치 실행");
    }
}

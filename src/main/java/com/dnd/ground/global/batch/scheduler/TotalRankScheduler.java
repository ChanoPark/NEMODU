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
 * @description 전체 누적 랭킹 계산 Job을 실행하기 위한 스케줄러
 * @author  박찬호
 * @since   2023-06-05
 * @updated 1. 스케줄러 정의
 *          - 2023-06-05 박찬호
 */

@Component
@Slf4j
public class TotalRankScheduler {
    private final JobLauncher jobLauncher;
    private final Job total_rank;
    private static final String JOB_PARAM_DATE = "requestDate";

    public TotalRankScheduler(@Qualifier("total_rank") Job total_rank, JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
        this.total_rank = total_rank;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void challengeStartJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters = new JobParameters(
                Collections.singletonMap(JOB_PARAM_DATE,  new JobParameter(String.valueOf(LocalDateTime.now()))));

        jobLauncher.run(total_rank, jobParameters);
    }
}

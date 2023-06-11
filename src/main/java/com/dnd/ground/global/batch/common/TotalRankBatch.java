package com.dnd.ground.global.batch.common;

import com.dnd.ground.domain.exerciseRecord.Repository.ExerciseRecordRepository;
import com.dnd.ground.domain.matrix.dto.RankCond;
import com.dnd.ground.domain.matrix.dto.RankDto;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.global.batch.JobLoggerListener;
import com.dnd.ground.global.batch.RedisZSetItemWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.persistence.EntityManagerFactory;
import java.util.List;

/**
 * @description 전체 누적 랭킹 계산 배치 Job. 자정에 새롭게 계산되어 캐싱된다.
 * @author  박찬호
 * @since   2023-06-05
 * @updated 1.탈퇴한 회원(DeleteUser) 제외
 *          - 2023-06-11 박찬호
 */

@Configuration
public class TotalRankBatch {
    public TotalRankBatch(JobBuilderFactory jobBuilderFactory,
                          StepBuilderFactory stepBuilderFactory,
                          JobLoggerListener jobLoggerListener,
                          EntityManagerFactory entityManagerFactory,
                          @Qualifier("redisTemplateString") RedisTemplate<String, String> redisTemplateString,
                          ExerciseRecordRepository exerciseRecordRepository) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobLoggerListener = jobLoggerListener;
        this.entityManagerFactory = entityManagerFactory;
        this.redisTemplate = redisTemplateString;
        this.exerciseRecordRepository = exerciseRecordRepository;
    }

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLoggerListener jobLoggerListener;
    private final EntityManagerFactory entityManagerFactory;
    private final RedisTemplate<String, String> redisTemplate;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private static final String JOB_NAME = "total_rank";
    private static final int CHUNK_SIZE  = 10;
    private static final String KEY = "totalRank";

    @Bean(name = JOB_NAME + "_job")
    @Qualifier("total_rank")
    public Job totalRankJob(Step total_rank_clear_step,
                            Step total_rank_calculate_step) {
        return jobBuilderFactory.get(JOB_NAME + "_job")
                .start(total_rank_clear_step)
                .next(total_rank_calculate_step)
                .listener(jobLoggerListener)
                .build();
    }

    @JobScope
    @Bean(name = JOB_NAME + "_clear_step")
    public Step totalRankClearStep(Tasklet total_rank_clear_tasklet) {
        return stepBuilderFactory.get(JOB_NAME + "_clear_step")
                .tasklet(total_rank_clear_tasklet)
                .build();
    }

    @JobScope
    @Bean(name = JOB_NAME + "_calculate_step")
    public Step totalRankCalculateStep(JpaPagingItemReader<User> total_rank_calculate_item_reader,
                                       ItemProcessor<User, RankDto> total_rank_calculate_item_processor,
                                       ItemWriter<RankDto> total_rank_calculate_item_writer) {
        return stepBuilderFactory.get(JOB_NAME + "_calculate_step")
                .<User, RankDto>chunk(CHUNK_SIZE) //닉네임 -> 랭킹
                .reader(total_rank_calculate_item_reader)
                .processor(total_rank_calculate_item_processor)
                .writer(total_rank_calculate_item_writer)
                .build();
    }

    @StepScope
    @Bean(name = JOB_NAME + "_calculate_item_reader")
    public JpaPagingItemReader<User> totalRankCalculateItemReader() {
        return new JpaPagingItemReaderBuilder<User>()
                .queryString("SELECT u FROM User u WHERE u.nickname <> '(알 수 없음)'")
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .name("total_rank_calculate_item_reader")
                .build();
    }

    @StepScope
    @Bean(name = JOB_NAME + "_calculate_item_processor")
    public ItemProcessor<User, RankDto> totalRankCalculateItemProcessor() {
        return item -> exerciseRecordRepository.findRankMatrixRankAllTime(new RankCond(List.of(item))).get(0);
    }

    @StepScope
    @Bean(name = JOB_NAME + "_calculate_item_writer")
    public ItemWriter<RankDto> totalRankCalculateItemWriter() {
        return new RedisZSetItemWriter(redisTemplate, KEY);
    }


    @Bean(name = JOB_NAME + "_clear_tasklet")
    public Tasklet totalRankClearTasklet() {
        return (contribution, chunkContext) -> {
            redisTemplate.delete(KEY);
            return RepeatStatus.FINISHED;
        };
    }
}

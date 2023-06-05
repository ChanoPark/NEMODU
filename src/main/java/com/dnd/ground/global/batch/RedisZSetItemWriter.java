package com.dnd.ground.global.batch;

import com.dnd.ground.domain.matrix.dto.RankDto;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * @description 누적 랭킹 정보를 Redis에 저장하기 위한 ItemWriter
 * @author  박찬호
 * @since   2023-06-05
 * @updated 1. ItemWriter 구현
 *          - 2023-06-05 박찬호
 */
public class RedisZSetItemWriter implements ItemWriter<RankDto> {
    private final RedisTemplate<String, String> redisTemplate;
    private final String key;

    public RedisZSetItemWriter(RedisTemplate<String, String> redisTemplate, String key) {
        this.redisTemplate = redisTemplate;
        this.key = key;
    }

    @Override
    public void write(List<? extends RankDto> items) throws Exception {
        for (RankDto item : items) {
            redisTemplate.opsForZSet().add(key, item.getNickname(), (double) item.getScore());
        }
    }
}

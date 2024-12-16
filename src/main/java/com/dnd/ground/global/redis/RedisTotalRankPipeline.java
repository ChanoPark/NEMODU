package com.dnd.ground.global.redis;

import com.dnd.ground.domain.matrix.dto.UserRankDto;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;

import java.util.List;

/**
 * @description 회원의 누적 랭킹을 조회하기 위한 Redis Pipeline
 * @author  박찬호
 * @since   2023-06-05
 * @updated 1. 랭킹 조회 Pipeline 구성
 *          - 2023-06-05 박찬호
 */

@AllArgsConstructor
public class RedisTotalRankPipeline implements RedisCallback<UserRankDto> {
    private final String nickname;
    private static final String KEY = "totalRank";

    @Override
    public UserRankDto doInRedis(RedisConnection connection) throws DataAccessException {
        connection.openPipeline();

        connection.zSetCommands().zScore(KEY.getBytes(), nickname.getBytes());
        connection.zSetCommands().zRevRank(KEY.getBytes(), nickname.getBytes());

        List<Object> results = connection.closePipeline();

        Double score = (Double) results.get(0);
        Long rank = (Long) results.get(1);

        Integer responseRank = rank == null ? null : rank.intValue();

        connection.closePipeline();
        return new UserRankDto(nickname, score, responseRank);
    }
}

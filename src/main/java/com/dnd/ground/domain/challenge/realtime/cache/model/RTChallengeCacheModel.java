package com.dnd.ground.domain.challenge.realtime.cache.model;

import com.dnd.ground.domain.challenge.ChallengeStatus;
import com.dnd.ground.global.util.JsonUtil;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 캐싱되는 실시간 챌린지 정보
 */

@AllArgsConstructor
@NoArgsConstructor
@Builder
@RedisHash(value = "cng:rt")
@Getter
public class RTChallengeCacheModel {
    @Id
    private String uuid;
    private String name;
    private ChallengeStatus status;
    private LocalDateTime started;
    private LocalDateTime ended;
    private List<String> members;

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return JsonUtil.writeValueAsString(this);
    }
}

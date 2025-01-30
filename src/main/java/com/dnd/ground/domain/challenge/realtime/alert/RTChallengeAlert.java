package com.dnd.ground.domain.challenge.realtime.alert;

import com.dnd.ground.domain.challenge.ChallengeStatus;
import com.dnd.ground.global.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.dnd.ground.global.redis.RedisKeyConstant.RT_ALERT_KEY;
import static com.dnd.ground.global.redis.RedisKeyConstant.SEPARATOR;

/**
 * @description 실시간 챌린지 푸시 알림 모델
 * @author  박찬호
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RTChallengeAlert {
    private String uuid;
    private ChallengeStatus status;
    private Long expired;

    @Override
    public String toString() {
        return JsonUtil.writeValueAsString(this);
    }

    /**
     * key pattern in redis: RT_ALERT_KEY:alert:{uuid}
     * @return KeyInRedis
     */
    public String getKey() {
        return RT_ALERT_KEY.getValue() + this.status + SEPARATOR.getValue() + this.uuid;
    }
}

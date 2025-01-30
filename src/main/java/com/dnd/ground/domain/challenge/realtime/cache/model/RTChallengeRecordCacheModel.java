package com.dnd.ground.domain.challenge.realtime.cache.model;

import com.dnd.ground.domain.challenge.ChallengeStatus;
import com.dnd.ground.global.redis.RedisKeyConstant;
import com.dnd.ground.global.util.JsonUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;

@NoArgsConstructor
@RedisHash("cng:rt")
@Getter
public class RTChallengeRecordCacheModel {
    @Id
    private String redisKeyId; // for redis key [uuid + ':' + nickname]
    private String nickname;
    private String uuid;
    private ChallengeStatus status;
    private List<RTChallengeRecordModel> records;
    private Integer score;

    public RTChallengeRecordCacheModel(String nickname, String uuid, ChallengeStatus status,
                                       List<RTChallengeRecordModel> records, Integer score) {
        this.redisKeyId = uuid + RedisKeyConstant.SEPARATOR.getValue() + nickname;
        this.nickname = nickname;
        this.uuid = uuid;
        this.status = status;
        this.records = records;
        this.score = score;
    }

    @Override
    public String toString() {
        return JsonUtil.writeValueAsString(this);
    }
}

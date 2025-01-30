package com.dnd.ground.global.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description Redis Expire Key prefix constants.
 * @author  박찬호
 * @since   2024-01-20
 * @updated 1. 구분자 변경 및 실시간 챌린지 관련 값 추가
 */

@AllArgsConstructor
@Getter
public enum RedisKeyConstant {
    SEPARATOR(":"),

    /**
     * Realtime-Challenge Key prefix
     */
    // @note RTChallengeCacheModel, RTChallengeRecordCacheModel @RedisHash 값과 맞춰줄 것.
    RT_KEY("cng:rt:"),               // cng:rt:{uuid}
    RT_ALERT_KEY("cng:rt:alert:"),   // cnt:rt:alert:{uuid}

    /**
     * Expire Message Patterns.
     */
    FCM_PATTERN("^fcm.*$")                          // fcm:deviceType:nickname
    , RT_CHALLENGE_PATTERN("^cng:rt(?!:alert).*$")  // cng:rt:{status}:{uuid}
    , RT_ALERT_PATTERN("^cng:rt:alert.*$")          // cng:rt:{uuid}

    ;

    public static final String REDIS_KEY_SEPARATOR = ":";
    private final String value;

    public Matcher getMatcher(String text) {
        return Pattern.compile(this.value).matcher(text);
    }

    public boolean find(String text) {
        return getMatcher(text).find();
    }
}

package com.dnd.ground.global.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description Redis Expire Key prefix constants.
 * @author  박찬호
 * @since   2024-01-20
 * @updated 1. 레디스 키 prefix에 따른 분기를 위한 상수 모음.
 *          - 2024-01-20 박찬호
 */

@AllArgsConstructor
@Getter
public enum RedisKeyConstant {
    SEPARATOR("/"),

    /**
     * Realtime-Challenge Key prefix
     */
    RT_KEY("/rt/"),
    RT_WAIT_KEY("/rt/wait/"),

    RT_ALERT_KEY("/rt/alert/"),


    /**
     * Expire Message Patterns.
     */
    FCM_PATTERN("^fcm.*$") // // fcm:deviceType:nickname  @todo 키 형식 수정 필요
    , RT_CHALLENGE_PATTERN("^rt.*$") // rt/{ChallengeStatus}/{uuid}
    , RT_ALERT_PATTERN("")
    ;

    public static final String REDIS_KEY_SEPARATOR = "/";
    private final String value;

    public Pattern getPattern() {
        return Pattern.compile(this.value);
    }

    public Matcher getMatcher(String text) {
        return this.getPattern().matcher(text);
    }

    public boolean find(String text) {
        return getMatcher(text).find();
    }
}

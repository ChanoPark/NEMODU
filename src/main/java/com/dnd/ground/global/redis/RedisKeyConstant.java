package com.dnd.ground.global.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description Redis Expire Key prefix constants. (Only prefix pattern.)
 * @author  박찬호
 * @since   2024-01-20
 * @updated 1. 레디스 키 prefix에 따른 분기를 위한 상수 모음.
 *          - 2024-01-20 박찬호
 */

@AllArgsConstructor
@Getter
public enum RedisKeyConstant {
    FCM_PATTERN("^fcm.*$", Pattern.compile("^fcm.*$"))

    ;

    private final String regex;
    private final Pattern pattern;

    public Matcher getMatcher(String text) {
        return this.pattern.matcher(text);
    }

    public boolean find(String text) {
        return getMatcher(text).find();
    }
}

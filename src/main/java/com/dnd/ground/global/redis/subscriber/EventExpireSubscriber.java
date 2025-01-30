package com.dnd.ground.global.redis.subscriber;

/**
 * @description Redis Key expire 이벤트 처리
 * @author  박찬호
 * @since   2024-01-20
 * @updated 1. 패키지 이동
 */

public interface EventExpireSubscriber {
    void handleExpireEvent(String message);
}

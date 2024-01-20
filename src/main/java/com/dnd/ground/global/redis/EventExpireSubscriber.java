package com.dnd.ground.global.redis;

/**
 * @description Redis Key expire 이벤트 처리
 * @author  박찬호
 * @since   2024-01-20
 * @updated 1. Redis Key expired event 처리 구조 개선
 *          - 2024-01-20 박찬호
 */

public interface EventExpireSubscriber {
    void handleExpireEvent(String message);
}

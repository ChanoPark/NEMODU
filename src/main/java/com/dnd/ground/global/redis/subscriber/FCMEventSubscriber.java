package com.dnd.ground.global.redis.subscriber;

import com.dnd.ground.global.notification.repository.FcmTokenRepository;
import com.dnd.ground.global.notification.service.FcmService;
import com.dnd.ground.global.redis.RedisKeyConstant;
import com.dnd.ground.global.util.DeviceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @description FCM 토큰 만료 처리를 위한 Redis FCM Key expire 이벤트 처리
 * @author  박찬호
 * @since   2024-01-20
 * @updated 1. 패키지 이동
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class FCMEventSubscriber implements EventExpireSubscriber {
    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public void handleExpireEvent(String message) {
        String[] keyAndValue = RedisKeyConstant.FCM_PATTERN.getMatcher(message).group().split(":");

        if (keyAndValue.length != 2) {
            log.warn("FCM 토큰 만료 이벤트의 메시지가 올바르지 않습니다: {}", message);
            return;
        }

        String nickname = keyAndValue[1];
        DeviceType type = DeviceType.getType(keyAndValue[0].split("_")[1]);

        String token = fcmTokenRepository.findToken(nickname, type);
        FcmService.requestReissueFCMToken(nickname, token);
    }
}

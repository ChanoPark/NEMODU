package com.dnd.ground.domain.challenge.realtime.alert;

import com.dnd.ground.domain.user.User;
import com.dnd.ground.global.notification.NotificationMessage;
import com.dnd.ground.global.notification.dto.NotificationForm;
import com.dnd.ground.global.redis.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dnd.ground.global.notification.PushNotificationParamList.CHALLENGE_UUID;

/**
 * @description 실시간 챌린지 푸시 알림 관련 서비스
 * @author      박찬호
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RTChallengeAlertService {
    @Qualifier("redisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    private final ApplicationEventPublisher pushNotificationPublisher;

    /**
     * 실시간 챌린지 알림 캐싱
     * @param alert Realtime-Challenge Alert
     */
    public void saveRTChallengeAlert(RTChallengeAlert alert) {
        String key = RedisKeyConstant.RT_ALERT_KEY.getValue() + alert.getUuid();

        redisTemplate.opsForValue().set(key, alert);
        redisTemplate.expire(key, alert.getExpired(), TimeUnit.SECONDS);
    }

    /**
     * 실시간 챌린지에 참여하는 모든 멤버에게 푸시 알림 전송
     * @param members challenge members
     * @param challengeName challenge name
     * @param message notification message
     */
    public void sendNotificationToAllMembers(List<User> members, String challengeName, NotificationMessage message, String uuid) {
        NotificationForm notificationForm =
                new NotificationForm(members, List.of(challengeName), List.of(challengeName), message, Map.of(CHALLENGE_UUID.name(), uuid));
        pushNotificationPublisher.publishEvent(notificationForm);
    }
}

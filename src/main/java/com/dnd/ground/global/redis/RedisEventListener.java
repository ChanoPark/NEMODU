package com.dnd.ground.global.redis;

import com.dnd.ground.global.redis.subscriber.EventExpireSubscriber;
import com.dnd.ground.global.redis.subscriber.FCMEventSubscriber;
import com.dnd.ground.global.redis.subscriber.RTChallengeAlertSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * @description Redis Expire Event Listener
 * @author  박찬호
 * @since   2023-05-04
 * @updated 구조 변경
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventListener implements MessageListener {
    private final FCMEventSubscriber fcmEventSubscriber;
    private final RTChallengeAlertSubscriber rtChallengeAlertSubscriber;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.debug(">>> Redis expired key event: {}", message);
        EventExpireSubscriber eventExpireSubscriber;
        String messageStr = message.toString();

        if (RedisKeyConstant.FCM_PATTERN.find(messageStr)) {
            // 푸시 알람
            eventExpireSubscriber = fcmEventSubscriber;
        } else if (RedisKeyConstant.RT_ALERT_PATTERN.find(messageStr)) {
            // 실시간 챌린지 알람
            eventExpireSubscriber = rtChallengeAlertSubscriber;
        } else {
            log.warn("Not found EventListener. meg:{}\tpattern:{}", message, new String(pattern));
            return ;
        }

        eventExpireSubscriber.handleExpireEvent(message.toString());
    }
}

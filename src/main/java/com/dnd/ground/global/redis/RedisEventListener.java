package com.dnd.ground.global.redis;

import com.dnd.ground.global.util.ApplicationContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * @description Redis Expire Event Listener
 * @author  박찬호
 * @since   2023-05-04
 * @updated 1.재발급 요청 방식 변경
 *          - 2023-05-11 박찬호
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.debug(">>> Redis expired key event: {}", message);
        EventExpireSubscriber eventExpireSubscriber;
        String messageStr = message.toString();

        if (RedisKeyConstant.FCM_PATTERN.find(messageStr)) {
            eventExpireSubscriber = ApplicationContextProvider.getBean(FCMEventSubscriber.class);
        } else {
            log.warn("Not found EventListener. meg:{}\tpattern:{}", message, new String(pattern));
            return ;
        }

        eventExpireSubscriber.handleExpireEvent(message.toString());
    }
}

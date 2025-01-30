package com.dnd.ground.global.redis.subscriber;

import com.dnd.ground.domain.challenge.Challenge;
import com.dnd.ground.domain.challenge.UserChallenge;
import com.dnd.ground.domain.challenge.realtime.cache.service.RTChallengeCacheTemplateService;
import com.dnd.ground.domain.challenge.realtime.alert.RTChallengeAlert;
import com.dnd.ground.domain.challenge.realtime.alert.RTChallengeAlertService;
import com.dnd.ground.domain.challenge.repository.ChallengeRepository;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.global.exception.ChallengeException;
import com.dnd.ground.global.exception.ExceptionCodeSet;
import com.dnd.ground.global.notification.NotificationMessage;
import com.dnd.ground.global.redis.RedisKeyConstant;
import com.dnd.ground.global.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.dnd.ground.domain.challenge.ChallengeStatus.*;

/**
 * @description 실시간 챌린지 알림 발송 [10분 전/5분 전/1분 전/시작]
 * @author  박찬호
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class RTChallengeAlertSubscriber implements EventExpireSubscriber {
    private final ChallengeRepository challengeRepository;
    private final RTChallengeAlertService rtChallengeAlertService;
    private final RTChallengeCacheTemplateService rtChallengeCacheRepository;

    @Override
    public void handleExpireEvent(String message) {
        // Find challenge from key (format: cng:rt:alert:{UUID})
        String uuidHex = message.replaceAll(RedisKeyConstant.RT_ALERT_KEY.getValue(), "");
        Challenge challenge = challengeRepository.findByUuidFetchMembers(UuidUtil.hexToBytes(uuidHex))
                .orElseThrow(() -> {
                    log.warn("## Not found RTChallenge in message: {}", message);
                    throw new ChallengeException(ExceptionCodeSet.CHALLENGE_NOT_FOUND);
                });

        List<User> members = challenge.getUsers().stream()
                .map(UserChallenge::getUser)
                .collect(Collectors.toList());

        RTChallengeAlert alert = new RTChallengeAlert(uuidHex, challenge.getStatus(), 0L); // 만료되어 온 메시지이므로 expired=0

        // status: 마지막 처리 단계 (Ex. RT_WAIT=아무 알람 안보냄, 10_MIN=10분 전 알람까지 보냄)
        switch (alert.getStatus()) {
            case RT_WAIT:
                processWait(alert, members, challenge.getName(), challenge.getStarted());
                break;
            case RT_WAIT_10_MIN:
                process10Min(alert, members, challenge.getName(), challenge.getStarted());
                break;
            case RT_WAIT_5_MIN:
                process5Min(alert, members, challenge.getName(), challenge.getStarted());
                break;
            case RT_WAIT_1_MIN:
                process1Min(alert, members, challenge.getName());
                break;
            default:
                log.warn("## Invalid RT Challenge status, message: {}", message);
        }
    }

    /**
     * 챌린지 대기 상태 > 10분 전 알람 처리
     */
    private void processWait(RTChallengeAlert alert, List<User> members, String challengeName, LocalDateTime started) {
        // 10분 전 알람 보내기
        rtChallengeAlertService.sendNotificationToAllMembers(members, challengeName, NotificationMessage.RT_CHALLENGE_WAIT_10_MIN, alert.getUuid());

        // 챌린지 상태 변경
        rtChallengeCacheRepository.updateStatus(alert.getUuid(), RT_WAIT_10_MIN);

        // 5분 전으로 알람 갱신
        alert.setStatus(RT_WAIT_10_MIN);
        alert.setExpired(Duration.between(LocalDateTime.now().minusMinutes(5), started).getSeconds());
        rtChallengeAlertService.saveRTChallengeAlert(alert);

        log.debug("## [RT_WAIT] Set RTChallenge alert result: {}", alert);
    }

    /**
     * 챌린지 10분 전 상태 > 5분 전 알람 처리
     */
    private void process10Min(RTChallengeAlert alert, List<User> members, String challengeName, LocalDateTime started) {
        // 5분 전 알람 보내기
        rtChallengeAlertService.sendNotificationToAllMembers(members, challengeName, NotificationMessage.RT_CHALLENGE_WAIT_5_MIN, alert.getUuid());

        // 챌린지 상태 변경
        rtChallengeCacheRepository.updateStatus(alert.getUuid(), RT_WAIT_5_MIN);

        // 1분 전으로 알람 갱신
        alert.setStatus(RT_WAIT_5_MIN);
        alert.setExpired(Duration.between(LocalDateTime.now().minusMinutes(1), started).getSeconds());
        rtChallengeAlertService.saveRTChallengeAlert(alert);

        log.debug("## [10MIN] Set RTChallenge alert result: {}", alert);
    }

    /**
     * 챌린지 5분 전 상태 > 1분 전 알람 처리
     */
    private void process5Min(RTChallengeAlert alert, List<User> members, String challengeName, LocalDateTime started) {
        // 5분 전 알람 발송
        rtChallengeAlertService.sendNotificationToAllMembers(members, challengeName, NotificationMessage.RT_CHALLENGE_WAIT_1_MIN, alert.getUuid());

        // 챌린지 상태 변경
        rtChallengeCacheRepository.updateStatus(alert.getUuid(), RT_WAIT_1_MIN);

        // 1분 전으로 알람 갱신
        alert.setStatus(RT_WAIT_1_MIN);
        alert.setExpired(Duration.between(started, LocalDateTime.now()).getSeconds());
        rtChallengeAlertService.saveRTChallengeAlert(alert);

        log.debug("## [5MIN] Set RTChallenge alert result: {}", alert);
    }

    /**
     * 챌린지 1분 전 상태 > 챌린지 시작 처리
     */
    private void process1Min(RTChallengeAlert alert, List<User> members, String challengeName) {
        // 실시간 챌린지 시작 알람 발송
        rtChallengeAlertService.sendNotificationToAllMembers(members, challengeName, NotificationMessage.RT_CHALLENGE_PROGRESS, alert.getUuid());

        // 챌린지 상태 변경
        rtChallengeCacheRepository.updateStatus(alert.getUuid(), PROGRESS);
        
        log.debug("## RTChallenge is started. {} [{}]", challengeName, alert.getUuid());
    }
}
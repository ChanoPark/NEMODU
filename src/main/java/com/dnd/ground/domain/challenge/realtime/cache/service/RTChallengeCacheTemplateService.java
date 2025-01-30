package com.dnd.ground.domain.challenge.realtime.cache.service;

import com.dnd.ground.domain.challenge.ChallengeStatus;
import com.dnd.ground.domain.challenge.realtime.cache.model.RTChallengeCacheModel;
import com.dnd.ground.domain.challenge.realtime.cache.model.RTChallengeRecordCacheModel;
import com.dnd.ground.domain.challenge.realtime.cache.repository.RTChallengeCacheRepository;
import com.dnd.ground.domain.challenge.realtime.cache.repository.RTChallengeRecordCacheRepository;
import com.dnd.ground.global.exception.ChallengeException;
import com.dnd.ground.global.exception.ExceptionCodeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RTChallengeCacheTemplateService {

    private final RTChallengeCacheRepository rtChallengeCacheRepository;
    private final RTChallengeRecordCacheRepository rtChallengeRecordCacheRepository;

    /**
     * 실시간 챌린지 및 멤버 캐싱
     * @param uuid challenge uuid(hex)
     * @param name challenge name
     * @param members challenge member list
     * @param started challenge started time
     * @param ended challenge ended time
     */
    public void saveRTChallenge(String uuid, String name, List<String> members, LocalDateTime started, LocalDateTime ended) {
        RTChallengeCacheModel rtChallengeCacheModel = RTChallengeCacheModel.builder()
                .uuid(uuid)
                .name(name)
                .status(ChallengeStatus.RT_WAIT)
                .started(started)
                .ended(ended)
                .members(members)
                .build();

        // Save RT Challenge in cache.
        rtChallengeCacheRepository.save(rtChallengeCacheModel);

        // Save RTChallenge member in cache
        for (String nickname : members) {
            RTChallengeRecordCacheModel record =
                    new RTChallengeRecordCacheModel(nickname, rtChallengeCacheModel.getUuid(), ChallengeStatus.WAIT, new ArrayList<>(), 0);

            rtChallengeRecordCacheRepository.save(record);
        }

        log.debug("## RT Challenge is saved in redis, uuid: {}", uuid);
    }

    /**
     * 챌린지 상태 변경
     * @param uuid challenge uuid(hex)
     * @param afterStatus status after processing this method
     */
    public void updateStatus(String uuid, ChallengeStatus afterStatus) {
        RTChallengeCacheModel rtChallengeCacheModel = rtChallengeCacheRepository.findById(uuid)
                .orElseThrow(() -> {
                    log.warn("## Not found rtChallenge in updateStatus: {}", uuid);
                    throw new ChallengeException(ExceptionCodeSet.CHALLENGE_NOT_FOUND);
                });

        rtChallengeCacheModel.setStatus(afterStatus);
        rtChallengeCacheRepository.save(rtChallengeCacheModel);
    }
}

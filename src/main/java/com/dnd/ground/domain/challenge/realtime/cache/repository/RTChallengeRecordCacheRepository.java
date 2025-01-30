package com.dnd.ground.domain.challenge.realtime.cache.repository;

import com.dnd.ground.domain.challenge.realtime.cache.model.RTChallengeRecordCacheModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RTChallengeRecordCacheRepository extends CrudRepository<RTChallengeRecordCacheModel, String> {
}

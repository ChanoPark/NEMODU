package com.dnd.ground.domain.challenge.realtime.cache.repository;

import com.dnd.ground.domain.challenge.realtime.cache.model.RTChallengeCacheModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RTChallengeCacheRepository extends CrudRepository<RTChallengeCacheModel, String> {
}

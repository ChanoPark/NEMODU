package com.dnd.ground.domain.challenge.repository;

import com.dnd.ground.domain.challenge.Challenge;
import com.dnd.ground.domain.challenge.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @description 챌린지와 관련한 레포지토리
 * @author  박찬호
 * @since   2022-08-03
 * @updated 1.미사용 쿼리 삭제
 *          2.UUID로 챌린지 조회 파라미터 타입 변경(String->byte[])
 *          - 2023.03.03 박찬호
 */
@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long>, ChallengeQueryRepository {

    //UUID로 챌린지 조회
    Optional<Challenge> findByUuid(@Param("uuid") byte[] uuid);

    //챌린지 이름으로 UUID 조회 - 테스트
    @Query("SELECT c.uuid " +
            "FROM Challenge c " +
            "WHERE c.name = :name")
    Optional<byte[]> findUUIDByName(@Param("name") String name);

    @Query("SELECT c " +
            "FROM Challenge c " +
            "WHERE c.started <= :now AND c.status = :status")
    List<Challenge> findWaitChallenge(@Param("now") LocalDateTime now, @Param("status") ChallengeStatus status);
}
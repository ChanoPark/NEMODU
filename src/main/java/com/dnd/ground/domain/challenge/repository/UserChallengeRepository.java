package com.dnd.ground.domain.challenge.repository;

import com.dnd.ground.domain.challenge.Challenge;
import com.dnd.ground.domain.challenge.ChallengeColor;
import com.dnd.ground.domain.challenge.UserChallenge;
import com.dnd.ground.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @description 회원-챌린지 간 조인엔티티와 관련한 레포지토리
 * @author  박찬호
 * @since   2022-08-03
 * @updated 1. 미사용 쿼리 제거
 *          - 2023.02.28 박찬호
 */
@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long>, ChallengeQueryRepository {
    //챌린지에 포함된 회원 조회
    @Query("SELECT uc.user " +
            "FROM UserChallenge uc " +
            "WHERE uc.challenge = :challenge")
    List<User> findMembers(@Param("challenge") Challenge challenge);

    //유저와 챌린지를 통해 UserChallenge 조회
    Optional<UserChallenge> findByUserAndChallenge(User user, Challenge challenge);

    //해당 챌린지의 UC 조회
    List<UserChallenge> findByChallenge(@Param("challenge") Challenge challenge);

    //챌린지의 주최자 조회
    @Query("SELECT uc.user " +
            "FROM UserChallenge uc " +
            "WHERE uc.challenge = :challenge " +
                "AND uc.status IN ('MASTER', 'MASTER_PROGRESS', 'MASTER_DONE')")
    User findMasterInChallenge(@Param("challenge") Challenge challenge);

    @Query("SELECT u " +
            "FROM UserChallenge uc " +
            "INNER JOIN uc.user u " +
            "INNER JOIN FETCH u.property " +
            "INNER JOIN uc.challenge c " +
            "WHERE c.uuid = :uuid " +
                "AND uc.status IN ('MASTER', 'MASTER_PROGRESS', 'MASTER_DONE')")
    User findMasterInChallenge(@Param("uuid") byte[] uuid);

    //챌린지 색깔 조회
    @Query("SELECT uc.color " +
            "FROM UserChallenge uc " +
            "WHERE uc.user=:user " +
                "AND uc.challenge=:challenge")
    ChallengeColor findChallengeColor(@Param("user") User user, @Param("challenge") Challenge challenge);

    //챌린지-회원 관계 테이블에 데이터가 있는 회원 조회
    @Query("SELECT uc " +
            "FROM UserChallenge uc " +
            "JOIN FETCH Challenge c " +
            "ON uc.challenge = c " +
            "WHERE uc.user=:user")
    List<UserChallenge> findUCs(@Param("user") User user);

    @Query("SELECT uc " +
            "FROM UserChallenge uc " +
            "INNER JOIN FETCH User u " +
            "ON uc.user = u " +
            "INNER JOIN FETCH UserProperty up " +
            "ON u.property = up " +
            "INNER JOIN FETCH Challenge c " +
            "ON uc.challenge = c " +
            "WHERE uc.challenge = :challenge")
    List<UserChallenge> findWaitUCs(@Param("challenge") Challenge challenge);
}

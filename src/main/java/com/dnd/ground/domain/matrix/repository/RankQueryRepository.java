package com.dnd.ground.domain.matrix.repository;

import com.dnd.ground.domain.challenge.Challenge;
import com.dnd.ground.domain.challenge.ChallengeStatus;
import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;
import com.dnd.ground.domain.matrix.dto.RankCond;
import com.dnd.ground.domain.matrix.dto.RankDto;
import com.dnd.ground.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @description 운동 기록 query 인터페이스
 * @author  박찬호
 * @since   2022-08-01
 * @updated 1.챌린지 랭킹 조회 페이징 쿼리 추가
 *          2023-06-09 박찬호
 */

public interface RankQueryRepository {
    List<ExerciseRecord> findRecord(Long id, LocalDateTime start, LocalDateTime end);
    List<RankDto> findRankMatrixRankAllTime(RankCond condition);
    List<RankDto> findRankArea(RankCond condition);
    List<RankDto> findRankStep(RankCond condition);
    Map<Challenge, List<RankDto>> findChallengeMatrixRank(User targetUser, List<ChallengeStatus> status);
    Map<Challenge, List<RankDto>> findChallengeMatrixRankPage(User targetUser, List<ChallengeStatus> status, Long offset, int size);
    Map<Challenge, List<RankDto>> findChallengeMatrixRankWithUsers(User targetUser, List<User> friend, List<ChallengeStatus> status);
    List<RankDto> findRankByChallenge(Challenge targetChallenge);
}

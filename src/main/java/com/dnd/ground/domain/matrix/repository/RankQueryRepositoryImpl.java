package com.dnd.ground.domain.matrix.repository;

import com.dnd.ground.domain.challenge.*;
import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;

import com.dnd.ground.domain.matrix.dto.RankCond;
import com.dnd.ground.domain.matrix.dto.RankDto;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.global.exception.ExceptionCodeSet;
import com.dnd.ground.global.exception.ExerciseRecordException;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.dnd.ground.domain.challenge.QChallenge.challenge;
import static com.dnd.ground.domain.challenge.QUserChallenge.userChallenge;
import static com.dnd.ground.domain.exerciseRecord.QExerciseRecord.exerciseRecord;
import static com.dnd.ground.domain.matrix.QMatrix.matrix;
import static com.dnd.ground.domain.user.QUser.user;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

/**
 * @description 운동 기록(영역) 관련 QueryDSL 레포지토리
 * @author  박찬호
 * @since   2022-08-01
 * @updated 1.ChallengeType -> ChallengeScoreType 클래스명 및 관련 변수명 수정
 *          - 2023-12-27 박찬호
 */

@Repository
@Slf4j
@RequiredArgsConstructor
public class RankQueryRepositoryImpl implements RankQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<ExerciseRecord> findRecord(Long id, LocalDateTime start, LocalDateTime end){
        return queryFactory
                .select(exerciseRecord)
                .from(exerciseRecord)
                .distinct()
                .innerJoin(matrix)
                .on(matrix.exerciseRecord.eq(exerciseRecord))
                .fetchJoin()
                .where(
                        exerciseRecord.user.id.eq(id),
                        exerciseRecord.started.between(start, end)
                )
                .fetch();
    }

    /**
     * 랭킹 쿼리 실행 메소드
     * @param constructor 조회할 랭킹 종류(Matrix, Area, Step ..)
     * @param order 정렬 기준(조회하는 데이터 기준)
     * @param tableJoinCondition 테이블 조인 조건(기간 등)
     * @param condition 검색 조건
     * @return 랭킹 결과
     */
    private List<RankDto> execQuery(ConstructorExpression<RankDto> constructor,
                                    OrderSpecifier<Long> order,
                                    BooleanExpression tableJoinCondition,
                                    RankCond condition) {
        return queryFactory
                .select(constructor)
                .from(user)
                .leftJoin(exerciseRecord)
                .on(tableJoinCondition)
                .leftJoin(matrix)
                .on(matrix.exerciseRecord.eq(exerciseRecord))
                .where(user.in(condition.getUsers()))
                .groupBy(user.nickname)
                .orderBy(order)
                .fetch();
    }

    @Override
    public List<RankDto> findRankMatrixRankAllTime(RankCond condition) {
        ConstructorExpression<RankDto> constructor = Projections.constructor(RankDto.class,
                user.nickname,
                user.picturePath,
                matrix.count()
        );

        BooleanExpression tableJoinCondition = exerciseRecord.user.eq(user).and(allTime());
        OrderSpecifier<Long> order = matrix.count().desc();
        return execQuery(constructor, order, tableJoinCondition, condition);
    }

    @Override
    public List<RankDto> findRankArea(RankCond condition) {
        if (condition.getStarted() == null || condition.getEnded() == null) throw new ExerciseRecordException(ExceptionCodeSet.INVALID_TIME);

        ConstructorExpression<RankDto> constructor = Projections.constructor(
                RankDto.class,
                user.nickname,
                user.picturePath,
                matrix.point.countDistinct()
        );

        BooleanExpression tableJoinCondition = userEqAndInPeriod(condition.getStarted(), condition.getEnded());
        OrderSpecifier<Long> order = matrix.point.countDistinct().desc();
        return execQuery(constructor, order, tableJoinCondition, condition);
    }

    @Override
    public List<RankDto> findRankStep(RankCond condition) {
        if (condition.getStarted() == null || condition.getEnded() == null) throw new ExerciseRecordException(ExceptionCodeSet.INVALID_TIME);

        return queryFactory
                .select(Projections.constructor(RankDto.class,
                        user.nickname,
                        user.picturePath,
                        exerciseRecord.stepCount.sum()
                                .castToNum(Long.class)
                                .coalesce(0L)
                ))
                .from(user)
                .leftJoin(exerciseRecord)
                .on(userEqAndInPeriod(condition.getStarted(), condition.getEnded()))
                .where(user.in(condition.getUsers()))
                .groupBy(user.nickname)
                .orderBy(exerciseRecord.stepCount.sum()
                        .castToNum(Long.class)
                        .desc())
                .fetch();
    }

    @Override
    public Map<Challenge, List<RankDto>> findChallengeMatrixRank(User targetUser, List<ChallengeStatus> status) {
        QChallenge subChallenge = new QChallenge("subChallenge");
        QUserChallenge subUC = new QUserChallenge("subUC");

        return queryFactory
                .select(
                        user.nickname,
                        user.picturePath,
                        challenge.scoreType,
                        new CaseBuilder()
                                .when(challenge.scoreType.eq(ChallengeScoreType.ACCUMULATE))
                                .then(matrix.count())
                                .when(challenge.scoreType.eq(ChallengeScoreType.WIDEN))
                                .then(matrix.point.countDistinct())
                                .otherwise(0L))
                .from(user)
                .innerJoin(userChallenge)
                .on(userChallenge.user.eq(user))
                .innerJoin(challenge)
                .on(
                        userChallenge.challenge.eq(challenge),
                        challenge.status.in(status),
                        challenge.in(
                                JPAExpressions
                                        .selectFrom(subChallenge)
                                        .innerJoin(subUC)
                                        .on(
                                                subUC.challenge.eq(subChallenge),
                                                subUC.user.eq(targetUser)
                                        )
                        )
                )
                .leftJoin(exerciseRecord)
                .on(
                        exerciseRecord.user.eq(user),
                        exerciseRecord.started.goe(challenge.started),
                        exerciseRecord.ended.loe(challenge.ended)
                )
                .leftJoin(matrix)
                .on(matrix.exerciseRecord.eq(exerciseRecord))
                .groupBy(challenge, user.nickname)
                .orderBy(challenge.started.asc())
                .transform(
                        groupBy(challenge).as(
                                list(Projections.constructor(RankDto.class,
                                        user.nickname,
                                        user.picturePath,
                                        new CaseBuilder()
                                                .when(challenge.scoreType.eq(ChallengeScoreType.ACCUMULATE))
                                                .then(matrix.count())
                                                .when(challenge.scoreType.eq(ChallengeScoreType.WIDEN))
                                                .then(matrix.point.countDistinct())
                                                .otherwise(0L)
                                ))
                        )
                );
    }

    @Override
    public Map<Challenge, List<RankDto>> findChallengeMatrixRankPage(User targetUser, List<ChallengeStatus> status, Long offset, int size) {
        QChallenge subChallenge = new QChallenge("subChallenge");
        QUserChallenge subUC = new QUserChallenge("subUC");

        return queryFactory
                .select(
                        user.nickname,
                        user.picturePath,
                        challenge.scoreType,
                        new CaseBuilder()
                                .when(challenge.scoreType.eq(ChallengeScoreType.ACCUMULATE))
                                .then(matrix.count())
                                .when(challenge.scoreType.eq(ChallengeScoreType.WIDEN))
                                .then(matrix.point.countDistinct())
                                .otherwise(0L))
                .distinct()
                .from(user)
                .innerJoin(userChallenge)
                .on(userChallenge.user.eq(user))
                .innerJoin(challenge)
                .on(
                        userChallenge.challenge.eq(challenge),
                        challenge.status.in(status),
                        challenge.in(
                                JPAExpressions
                                        .selectFrom(subChallenge)
                                        .innerJoin(subUC)
                                        .on(
                                                subUC.challenge.eq(subChallenge),
                                                subUC.user.eq(targetUser)
                                        )
                        )
                )
                .leftJoin(exerciseRecord)
                .on(
                        exerciseRecord.user.eq(user),
                        exerciseRecord.started.goe(challenge.started),
                        exerciseRecord.ended.loe(challenge.ended)
                )
                .leftJoin(matrix)
                .on(matrix.exerciseRecord.eq(exerciseRecord))
                .where(
                        userEq(targetUser),
                        challengeIdLt(offset)
                )
                .groupBy(
                        challenge,
                        user.nickname
                )
                .orderBy(
                        challenge.started.desc(),
                        challenge.id.desc()
                )
                .limit(size + 1)
                .transform(
                        groupBy(challenge).as(
                                list(Projections.constructor(RankDto.class,
                                        user.nickname,
                                        user.picturePath,
                                        new CaseBuilder()
                                                .when(challenge.scoreType.eq(ChallengeScoreType.ACCUMULATE))
                                                .then(matrix.count())
                                                .when(challenge.scoreType.eq(ChallengeScoreType.WIDEN))
                                                .then(matrix.point.countDistinct())
                                                .otherwise(0L)
                                ))
                        )
                );
    }

    @Override
    public Map<Challenge, List<RankDto>> findChallengeMatrixRankWithUsers(User targetUser, List<User> friend, List<ChallengeStatus> status) {
        QChallenge subChallenge = new QChallenge("subChallenge");
        QUserChallenge subUC = new QUserChallenge("subUC");
        QUserChallenge subUC2 = new QUserChallenge("subUC2");

        return queryFactory
                .select(
                        user.nickname,
                        user.picturePath,
                        challenge.scoreType,
                        new CaseBuilder()
                                .when(challenge.scoreType.eq(ChallengeScoreType.ACCUMULATE))
                                .then(matrix.count())
                                .when(challenge.scoreType.eq(ChallengeScoreType.WIDEN))
                                .then(matrix.point.countDistinct())
                                .otherwise(0L))
                .from(user)
                .innerJoin(userChallenge)
                .on(userChallenge.user.eq(user))
                .innerJoin(challenge)
                .on(
                        userChallenge.challenge.eq(challenge),
                        challenge.status.in(status),
                        challenge.in(
                                JPAExpressions
                                        .selectFrom(subChallenge)
                                        .innerJoin(subUC)
                                        .on(
                                                subUC.challenge.eq(subChallenge),
                                                subUC.user.eq(targetUser)
                                        )
                                        .innerJoin(subUC2)
                                        .on(
                                                subUC2.challenge.eq(subChallenge),
                                                subUC2.user.in(friend)
                                        )
                        )
                )
                .leftJoin(exerciseRecord)
                .on(
                        exerciseRecord.user.eq(user),
                        exerciseRecord.started.goe(challenge.started),
                        exerciseRecord.ended.loe(challenge.ended)
                )
                .leftJoin(matrix)
                .on(matrix.exerciseRecord.eq(exerciseRecord))
                .groupBy(challenge, user.nickname)
                .orderBy(challenge.started.asc())
                .transform(
                        groupBy(challenge).as(
                                list(Projections.constructor(RankDto.class,
                                        user.nickname,
                                        user.picturePath,
                                        new CaseBuilder()
                                                .when(challenge.scoreType.eq(ChallengeScoreType.ACCUMULATE))
                                                .then(matrix.count())
                                                .when(challenge.scoreType.eq(ChallengeScoreType.WIDEN))
                                                .then(matrix.point.countDistinct())
                                                .otherwise(0L)
                                ))
                        )
                );
    }

    @Override
    public List<RankDto> findRankByChallenge(Challenge targetChallenge) {
        return queryFactory
                .select(Projections.constructor(RankDto.class,
                        user.nickname,
                        user.picturePath,
                        challenge.scoreType,
                        new CaseBuilder()
                                .when(challenge.scoreType.eq(ChallengeScoreType.ACCUMULATE))
                                .then(matrix.count())
                                .when(challenge.scoreType.eq(ChallengeScoreType.WIDEN))
                                .then(matrix.point.countDistinct())
                                .otherwise(0L)
                ))
                .from(user)
                .innerJoin(challenge)
                .on(challenge.eq(targetChallenge))
                .innerJoin(userChallenge)
                .on(
                        userChallenge.challenge.eq(challenge),
                        userChallenge.user.eq(user)
                )
                .leftJoin(exerciseRecord)
                .on(
                        exerciseRecord.user.eq(user),
                        exerciseRecord.started.goe(challenge.started),
                        exerciseRecord.ended.loe(challenge.ended)
                )
                .leftJoin(matrix)
                .on(matrix.exerciseRecord.eq(exerciseRecord))
                .groupBy(challenge, user.nickname)
                .orderBy(challenge.started.asc())
                .fetch();
    }

    private BooleanExpression userEqAndInPeriod(LocalDateTime started, LocalDateTime ended) {
        return started != null && ended != null ?
                exerciseRecord.user.eq(user).and(exerciseRecord.started.after(started))
                        .and(exerciseRecord.ended.before(ended))
                :
                null;
    }

    private BooleanExpression allTime() {
        return exerciseRecord.started.after(user.created)
                .and(exerciseRecord.ended.before(LocalDateTime.now()));
    }

    private BooleanExpression challengeIdLt(Long id) {
        return id != null ? challenge.id.lt(id) : null;
    }

    private BooleanExpression userEq(User target) {
        return target != null ? user.eq(target) : null;
    }
}
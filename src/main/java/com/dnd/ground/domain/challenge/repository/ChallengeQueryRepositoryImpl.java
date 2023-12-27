package com.dnd.ground.domain.challenge.repository;

import com.dnd.ground.domain.challenge.*;
import com.dnd.ground.domain.challenge.dto.*;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.global.util.UuidUtil;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dnd.ground.domain.challenge.QChallenge.challenge;
import static com.dnd.ground.domain.challenge.QUserChallenge.userChallenge;
import static com.dnd.ground.domain.user.QUser.user;
import static com.dnd.ground.domain.user.QUserProperty.userProperty;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

/**
 * @description QueryDSL을 활용한 챌린지 관련 구현체
 * @author 박찬호
 * @since 2023-02-15
 * @updated 1.챌린지 목록 페이징 쿼리 추가
 *          - 2023.06.06 박찬호
 */
@RequiredArgsConstructor
@Slf4j
public class ChallengeQueryRepositoryImpl implements ChallengeQueryRepository {
    private final JPAQueryFactory queryFactory;

    /*진행 중인 챌린지 멤버 조회*/
    @Override
    public List<User> findUCInProgress(User user) {
        return queryFactory
                .select(userChallenge.user)
                .from(userChallenge)
                .innerJoin(challenge)
                .on(userChallenge.challenge.eq(challenge))
                .where(
                        containUserInChallenge(user),
                        eqChallengeStatusList(List.of(ChallengeStatus.PROGRESS, ChallengeStatus.MASTER_PROGRESS)),
                        userChallenge.user.ne(user)
                )
                .distinct()
                .fetch();
    }

    /*진행 중인 챌린지 개수 조회*/
    @Override
    public Map<User, Long> findUserProgressChallengeCount(User user) {
        return queryFactory
                .select(userChallenge.count(),
                        userChallenge.user)
                .from(userChallenge)
                .innerJoin(challenge)
                .on(userChallenge.challenge.eq(challenge))
                .where(
                        eqChallengeStatusList(List.of(ChallengeStatus.PROGRESS, ChallengeStatus.MASTER_PROGRESS)),
                        containUserInChallenge(user),
                        userChallenge.user.ne(user)
                )
                .groupBy(userChallenge.user)
                .transform(groupBy(userChallenge.user).as(userChallenge.count()));
    }

    /*진행 중인 챌린지 정보 조회(인원)*/
    @Override
    public Map<User, Challenge> findProgressChallengesInfo(User user) {
        return queryFactory
                .select(userChallenge.user, userChallenge.challenge)
                .from(userChallenge)
                .innerJoin(challenge)
                .on(userChallenge.challenge.eq(challenge))
                .where(
                        eqChallengeStatusList(List.of(ChallengeStatus.PROGRESS, ChallengeStatus.MASTER_PROGRESS)),
                        containUserInChallenge(user),
                        userChallenge.user.ne(user)
                )
                .orderBy(challenge.started.asc())
                .transform(groupBy(userChallenge.user).as(userChallenge.challenge));
    }

    /*챌린지 상태에 따른 색깔 조회*/
    @Override
    public Map<Challenge, ChallengeColor> findChallengesColor(ChallengeCond condition) {
        return queryFactory
                .select(new QChallengeColorDto(userChallenge.challenge, userChallenge.color))
                .from(userChallenge)
                .innerJoin(challenge)
                .on(userChallenge.challenge.eq(challenge))
                .where(
                        userChallenge.user.eq(condition.getUser()),
                        eqChallengeStatusList(condition.getStatusList())
                )
                .transform(
                        groupBy(challenge).as(userChallenge.color)
                );
    }


    /*참여 중인 챌린지 개수 조회*/
    @Override
    public Map<User, Long> findUsersJoinChallengeCount(Set<String> users) {
        return queryFactory
                .from(user)
                .innerJoin(userProperty)
                .on(user.property.eq(userProperty))
                .fetchJoin()
                .leftJoin(userChallenge)
                .on(
                        userChallenge.user.eq(user),
                        userChallenge.status.in(
                                ChallengeStatus.READY,
                                ChallengeStatus.MASTER,
                                ChallengeStatus.PROGRESS,
                                ChallengeStatus.MASTER_PROGRESS)
                )
                .where(user.nickname.in(users))
                .groupBy(user)
                .transform(groupBy(user).as(userChallenge.count()));
    }

    @Override
    public Integer findUserJoinChallengeCount(User target) {
        return queryFactory
                .select(user.count())
                .from(user)
                .innerJoin(userProperty)
                .on(user.property.eq(userProperty))
                .fetchJoin()
                .leftJoin(userChallenge)
                .on(
                        userChallenge.user.eq(user),
                        userChallenge.status.in(
                                ChallengeStatus.READY,
                                ChallengeStatus.MASTER,
                                ChallengeStatus.PROGRESS,
                                ChallengeStatus.MASTER_PROGRESS)
                )
                .where(user.eq(target))
                .groupBy(user)
                .fetch()
                .size();
    }

    /*조건에 따른 조회*/
    @Override
    public List<Challenge> findChallengesByCond(ChallengeCond condition) {
        return queryFactory
                .selectFrom(challenge)
                .innerJoin(userChallenge)
                .on(userChallenge.challenge.eq(challenge))
                .where(
                        eqChallengeStatusList(condition.getStatusList()),
                        userChallenge.user.eq(condition.getUser()),
                        inPeriod(condition.getStarted(), condition.getEnded())
                )
                .orderBy(challenge.created.asc())
                .fetch();
    }

    @Override
    public List<Challenge> findChallengesPageByCond(ChallengeCond condition) {
        return queryFactory
                .selectFrom(challenge)
                .innerJoin(userChallenge)
                .on(userChallenge.challenge.eq(challenge))
                .where(
                        challengeIdLt(condition.getId()),
                        eqChallengeStatusList(condition.getStatusList()),
                        userChallenge.user.eq(condition.getUser()),
                        inPeriod(condition.getStarted(), condition.getEnded())
                )
                .orderBy(
                        challenge.created.desc(),
                        challenge.id.desc()
                )
                .limit(condition.getSize() + 1)
                .fetch();
    }

    /*닉네임과 UUID를 기반으로 UC조회*/
    @Override
    public Optional<UserChallenge> findUC(String nickname, String uuid) {
        return Optional.ofNullable(queryFactory
                .selectFrom(userChallenge)
                .innerJoin(challenge)
                .on(
                        userChallenge.challenge.eq(challenge),
                        challenge.uuid.eq(UuidUtil.hexToBytes(uuid))
                )
                .fetchJoin()
                .innerJoin(user)
                .on(
                        userChallenge.user.eq(user),
                        user.nickname.eq(nickname)
                )
                .fetchJoin()
                .fetchOne());
    }

    /*챌린지 상태에 따라, 챌린지와 챌린지에 참여하고 있는 인원의 UC 조회*/
    @Override
    public Map<Challenge, List<UCDto.UCInfo>> findUCInChallenge(ChallengeCond condition) {
        QUserChallenge subUC = new QUserChallenge("subUC");

        return queryFactory
                .from(challenge)
                .innerJoin(userChallenge)
                .on(
                        userChallenge.challenge.eq(challenge),
                        userChallenge.challenge.in(
                                JPAExpressions
                                        .select(subUC.challenge)
                                        .from(subUC)
                                        .where(subUC.user.eq(condition.getUser()))
                        )
                )
                .innerJoin(user)
                .on(user.eq(userChallenge.user))
                .where(
                        eqChallengeStatusList(condition.getStatusList()),
                        ucEqChallengeUuid(condition.getUuid())
                )
                .orderBy(challenge.created.asc())
                .transform(
                        groupBy(challenge).as(
                                list(new QUCDto_UCInfo(userChallenge.user.picturePath, userChallenge.user.nickname, userChallenge.status))
                        )
                );
    }

    @Override
    public List<ChallengeUcsDto> findChallengeUcs(User user) {
        QChallenge subChallenge = new QChallenge("subChallenge");
        QUserChallenge subUC = new QUserChallenge("subUC");

        return queryFactory
                .select(new QChallengeUcsDto(challenge, list(userChallenge)))
                .from(userChallenge)
                .innerJoin(challenge)
                .on(userChallenge.challenge.eq(challenge))
                .where(
                        userChallenge.challenge.in(
                                JPAExpressions
                                        .selectFrom(subChallenge)
                                        .innerJoin(subUC)
                                        .on(
                                                subUC.challenge.eq(subChallenge),
                                                subUC.user.eq(user)
                                        )
                        )
                )
                .transform(groupBy(challenge).list(new QChallengeUcsDto(challenge, list(userChallenge))));
    }

    private Predicate inPeriod(LocalDateTime started, LocalDateTime ended) {
        return started != null && ended != null ? challenge.started.loe(started).and(challenge.ended.goe(ended)) : null;
    }

    private BooleanExpression containUserInChallenge(User user) {
        QUserChallenge ucSub = new QUserChallenge("ucSub");

        return user != null ? challenge.id.in(
                JPAExpressions
                        .select(ucSub.challenge.id)
                        .from(ucSub)
                        .where(ucSub.user.eq(user)))
                :
                null;
    }

    private BooleanExpression ucEqChallengeUuid(byte[] uuid) {
        return uuid != null ? userChallenge.challenge.uuid.eq(uuid) : null;
    }

    private BooleanExpression eqChallengeStatusList(List<ChallengeStatus> statusList) {
        return statusList != null ? challenge.status.in(statusList) : null;
    }

    private BooleanExpression challengeIdLt(Long id) {
        return id != null ? challenge.id.lt(id) : null;
    }
}
package com.dnd.ground.domain.matrix.service;

import com.dnd.ground.domain.exerciseRecord.Repository.ExerciseRecordRepository;
import com.dnd.ground.domain.matrix.dto.RankDto;
import com.dnd.ground.domain.friend.service.FriendService;
import com.dnd.ground.domain.matrix.dto.RankCond;
import com.dnd.ground.domain.matrix.dto.UserRankDto;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.dto.RankResponseDto;
import com.dnd.ground.domain.user.dto.UserRequestDto;
import com.dnd.ground.domain.user.dto.UserResponseDto;
import com.dnd.ground.domain.user.repository.UserRepository;
import com.dnd.ground.global.exception.CommonException;
import com.dnd.ground.global.exception.ExceptionCodeSet;
import com.dnd.ground.global.exception.UserException;
import com.dnd.ground.global.redis.RedisTotalRankPipeline;
import lombok.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @description 운동 영역 서비스 클래스
 * @author  박찬호
 * @since   2022-08-01
 * @updated 1.전체 누적 랭킹 조회 API 구현
 *          - 2023-06-06 박찬호
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankServiceImpl implements RankService {
    private final UserRepository userRepository;
    private final FriendService friendService;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final RedisTemplate<String, String> redisTemplateString;

    @Value("${picture.path}")
    private String DEFAULT_PATH;

    //본인의 누적 랭킹 조회
    public UserResponseDto.Ranking matrixRankingAllTime(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserException(ExceptionCodeSet.USER_NOT_FOUND));

        UserRankDto userRankDto = redisTemplateString.execute(new RedisTotalRankPipeline(nickname));
        if (userRankDto == null || userRankDto.getRank() == null || userRankDto.getScore() == null) {
            return new UserResponseDto.Ranking(0, user.getNickname(), 0L, user.getPicturePath());
        } else {
            return new UserResponseDto.Ranking(userRankDto.getRank() + 1, user.getNickname(), Math.round(userRankDto.getScore()), user.getPicturePath());
        }
    }

    //누적 랭킹 조회
    public RankResponseDto.Matrix matrixRankingAllTime(int offset, int size) {
        final String TOTAL_RANK = "totalRank";
        int rank = offset;
        double prevScore = Double.MAX_VALUE;
        int interval = 1;
        String picturePath;

        List<UserResponseDto.Ranking> rankings = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplateString.opsForZSet().reverseRangeWithScores(TOTAL_RANK, offset, offset + size - 1);

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String nickname = tuple.getValue();
            Double score = tuple.getScore();
            if (score == null) score = prevScore;

            Optional<User> opt = userRepository.findByNickname(nickname);
            if (opt.isPresent()) picturePath = opt.get().getPicturePath();
            else picturePath = DEFAULT_PATH;

            if (prevScore <= score) interval++;
            else {
                prevScore = score;
                rank += interval;
                interval = 1;
            }

            rankings.add(new UserResponseDto.Ranking(rank, nickname, Math.round(score), picturePath));
        }

        return new RankResponseDto.Matrix(rankings);
    }

    //영역 랭킹 조회
    @Override
    public RankResponseDto.Area areaRanking(UserRequestDto.LookUp requestDto) {
        LocalDateTime started = requestDto.getStarted();
        LocalDateTime ended = requestDto.getEnded();

        User user = userRepository.findByNickname(requestDto.getNickname()).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND));

        List<User> userAndFriends = friendService.getFriends(user);
        userAndFriends.add(user);
        List<RankDto> areaRank = exerciseRecordRepository.findRankArea(new RankCond(userAndFriends, started, ended));
        return new RankResponseDto.Area(calculateUsersRank(areaRank));
    }

    //걸음수 랭킹 조회
    @Override
    public RankResponseDto.Step stepRanking(UserRequestDto.LookUp requestDto) {
        LocalDateTime start = requestDto.getStarted();
        LocalDateTime end = requestDto.getEnded();

        User user = userRepository.findByNickname(requestDto.getNickname()).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND));

        List<User> userAndFriends = friendService.getFriends(user);
        userAndFriends.add(user);
        List<RankDto> result = exerciseRecordRepository.findRankStep(new RankCond(userAndFriends, start, end));
        return new RankResponseDto.Step(calculateUsersRank(result));
    }

    //점수 기준 랭킹 계산
    @Override
    public List<UserResponseDto.Ranking> calculateUsersRank(List<RankDto> ranks) {
        List<UserResponseDto.Ranking> response = new ArrayList<>();
        Collections.sort(ranks);

        RankDto first = ranks.remove(0);
        int rank = 1;
        int interval = 1;
        long prevScore = first.getScore();
        response.add(new UserResponseDto.Ranking(rank, first.getNickname(), first.getScore(), first.getPicturePath()));

        for (RankDto rankInfo : ranks) {
            Long score = rankInfo.getScore();
            if (score < prevScore) {
                rank += interval;
                prevScore = score;
                interval = 1;
            } else {
                interval++;
            }

            response.add(new UserResponseDto.Ranking(rank, rankInfo.getNickname(), score, rankInfo.getPicturePath()));
        }
        return response;
    }

    @Override
    public UserResponseDto.Ranking calculateUserRank(List<RankDto> ranks, User targetUser) {
        RankDto first = ranks.remove(0);
        if (first.getNickname().equals(targetUser.getNickname()))
            return new UserResponseDto.Ranking(1, targetUser.getNickname(), first.getScore(), targetUser.getPicturePath());

        int rank = 1;
        int interval = 1;
        long prevScore = first.getScore();

        for (RankDto rankInfo : ranks) {
            Long score = rankInfo.getScore();
            if (score < prevScore) {
                rank += interval;
                prevScore = score;
                interval = 1;
            } else {
                interval++;
            }

            if (rankInfo.getNickname().equals(targetUser.getNickname()))
                return new UserResponseDto.Ranking(rank, targetUser.getNickname(), score, targetUser.getPicturePath());
        }
        throw new CommonException(ExceptionCodeSet.RANKING_CAL_FAIL);
    }
}
package com.dnd.ground.domain.challenge.service;

import com.dnd.ground.domain.challenge.*;
import com.dnd.ground.domain.challenge.dto.ChallengeCreateRequestDto;
import com.dnd.ground.domain.challenge.dto.ChallengeRequestDto;
import com.dnd.ground.domain.challenge.dto.ChallengeResponseDto;
import com.dnd.ground.domain.challenge.repository.ChallengeRepository;
import com.dnd.ground.domain.challenge.repository.UserChallengeRepository;
import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;
import com.dnd.ground.domain.exerciseRecord.Repository.ExerciseRecordRepository;
import com.dnd.ground.domain.matrix.dto.MatrixDto;
import com.dnd.ground.domain.matrix.matrixRepository.MatrixRepository;
import com.dnd.ground.domain.matrix.matrixService.MatrixService;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.dto.RankResponseDto;
import com.dnd.ground.domain.user.dto.UserResponseDto;
import com.dnd.ground.domain.user.repository.UserRepository;
import com.dnd.ground.global.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Tuple;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * @description 챌린지와 관련된 서비스의 역할을 분리한 구현체
 * @author  박찬호
 * @since   2022-08-03
 * @updated 1. 챌린지 조회 시, 종류에 따른 랭킹 계산 반영 -박찬호
 *          2. 해당 운동기록이 참여하고 있는 챌린지 - 박세헌
 *          - 2022.08.18
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class ChallengeServiceImpl implements ChallengeService {

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final MatrixService matrixService;
    private final MatrixRepository matrixRepository;

    /*챌린지 생성*/
    @Transactional
    public ResponseEntity<?> createChallenge(ChallengeCreateRequestDto requestDto) {

        User master = userRepository.findByNickname(requestDto.getNickname()).orElseThrow(); //예외 처리 예정

        Challenge challenge = Challenge.create()
                .uuid(UuidUtil.createUUID())
                .name(requestDto.getName())
                .started(requestDto.getStarted())
                .message(requestDto.getMessage()) //메시지 처리 방식 결과에 따라 수정 요망
                .type(requestDto.getType())
                .build();

        challengeRepository.save(challenge);

        //챌린지 개수에 따른 색상 결정
        ChallengeColor[] color = {ChallengeColor.Red, ChallengeColor.Pink, ChallengeColor.Yellow};

        //주최자의 챌린지 생성 과정
        int challengeCount = userChallengeRepository.findCountChallenge(master); //참여한 챌린지 개수 (챌린지 상태 상관X)

        //챌린지가 3개 이상이면 챌린지 생성 거부
        if (challengeCount > 3) {
            challengeRepository.deleteById(challenge.getId()); //롤백 구현 필요
            return new ResponseEntity(HttpStatus.BAD_REQUEST); //구체적인 예외처리 필요
        }

        UserChallenge masterChallenge = userChallengeRepository.save(new UserChallenge(challenge, master, color[challengeCount]));
        masterChallenge.changeStatus(ChallengeStatus.Master);
        
        //챌린지 멤버의 챌린지 생성 과정
        for (String nickname : requestDto.getFriends()) {
            User member = userRepository.findByNickname(nickname).orElseThrow(); //예외 처리 예정

            challengeCount = userChallengeRepository.findCountChallenge(member);

            //챌린지가 3개 이상이면 챌린지 생성 거부
            if (challengeCount > 3) {
                challengeRepository.deleteById(challenge.getId()); //롤백 구현 필요
                return new ResponseEntity(HttpStatus.BAD_REQUEST); //구체적인 예외처리 필요
            }

            userChallengeRepository.save(new UserChallenge(challenge, member, color[challengeCount]));
        }

        return new ResponseEntity(HttpStatus.CREATED);
    }

    /*유저-챌린지 상태 변경*/
    @Transactional
    public ResponseEntity<?> changeUserChallengeStatus(ChallengeRequestDto.CInfo requestDto, ChallengeStatus status) {
        //정보 조회
        Challenge challenge = challengeRepository.findByUuid(requestDto.getUuid()).orElseThrow(); //예외 처리 예정
        User user = userRepository.findByNickname(requestDto.getNickname()).orElseThrow(); // 예외 처리 예정
        //상태 변경
        UserChallenge userChallenge = userChallengeRepository.findByUserAndChallenge(user, challenge).orElseThrow();
        if (userChallenge.getStatus() == ChallengeStatus.Master) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        userChallenge.changeStatus(status);

        return new ResponseEntity(HttpStatus.OK);
    }

    /*챌린지 상태 변경(매일 00:00 실행)*/
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void startPeriodChallenge() {
        //챌린지 시작일이 오늘인 챌린지 리스트
        List<Challenge> challenges = challengeRepository.findChallengesNotStarted(LocalDate.now());
        int countDelete = 0; //삭제된 챌린지 수
        int countUser = 0; //삭제된 유저 수
        int countProgress = 0; // 진행 상태로 바뀐 챌린지 수

        for (Challenge challenge : challenges) {
            //Wait, Reject 상태의 유저 삭제
            countUser += userChallengeRepository.deleteUCByChallenge(challenge);

            List<UserChallenge> userChallenge = userChallengeRepository.findUCByChallenge(challenge);

            //주최자만 남은 경우 챌린지와 주최자 삭제
            if (userChallenge.size() == 1) {
                userChallengeRepository.delete(userChallenge.get(0));
                challengeRepository.delete(challenge);
                countDelete++;
            }
            //챌린지 진행 상태로 변경
            else {
                challenge.updateStatus(ChallengeStatus.Progress);
            }
        }

        log.info("**챌린지 시작 메소드 실행** 현재 시간:{} | 삭제된 챌린지 개수:{} | 삭제된 유저 수:{} | 진행 상태로 바뀐 챌린지 개수:{}",
                LocalDateTime.now(), countDelete, countUser, countProgress);
    }

    /*일주일 챌린지 마감(매주 일요일 오후 11시 59분 50초 실행)*/
    @Transactional
    @Scheduled(cron = "50 59 23 * * 0")
    public void endPeriodChallenge() {
        //진행 중인 챌린지 모두 조회
        List<Challenge> challenges = challengeRepository.findChallengesByStatusEquals(ChallengeStatus.Progress);

        for (Challenge challenge : challenges) {
            //챌린지 완료
            challenge.updateStatus(ChallengeStatus.Done);

            //각 유저들도 완료 상태 변경
            List<UserChallenge> userChallenge = userChallengeRepository.findUCByChallenge(challenge);
            userChallenge.forEach(uc -> uc.changeStatus(ChallengeStatus.Done));
        }

        log.info("**챌린지 종료 메소드 실행** 현재 시간:{} | 종료된 챌린지 개수:{}", LocalDateTime.now(), challenges.size());
    }
    
    /*초대 받은 챌린지 조회*/
    public List<ChallengeResponseDto.Invite> findInviteChallenge(String nickname) {
        User user = userRepository.findByNickname(nickname).orElseThrow(); //예외 처리 예정

        List<Challenge> challenges = challengeRepository.findChallengeInWait(user);
        List<ChallengeResponseDto.Invite> response = new ArrayList<>();

        for (Challenge challenge : challenges) {

            response.add(
                    ChallengeResponseDto.Invite.builder()
                            .name(challenge.getName())
                            .InviterNickname(userChallengeRepository.findMasterInChallenge(challenge).getNickname())
                            .message(challenge.getMessage())
                            .created(challenge.getCreated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .build()
            );
        }

        return response;
    }

    /*진행 대기 중인 챌린지 리스트 조회*/
    public List<ChallengeResponseDto.Wait> findWaitChallenge(String nickname) {
        User user = userRepository.findByNickname(nickname).orElseThrow(); //예외 처리 예정

        List<Challenge> waitChallenge = challengeRepository.findWaitChallenge(user);
        List<ChallengeResponseDto.Wait> response = new ArrayList<>();

        for (Challenge challenge : waitChallenge) {

            LocalDate started = challenge.getStarted();

            response.add(
                    ChallengeResponseDto.Wait.builder()
                            .name(challenge.getName())
                            .started(started)
                            .ended(started.plusDays(7-started.getDayOfWeek().getValue()))
                            .totalCount(userChallengeRepository.findUCCount(challenge)) //챌린지에 참여하는 전체 인원 수
                            .readyCount(userChallengeRepository.findUCWaitCount(challenge) + 1) //Progress 상태 회원 수 + 주최자
                            .color(userChallengeRepository.findChallengeColor(user, challenge))
                            .build()
            );
        }

        return response;
    }

    /*진행 중인 챌린지 리스트 조회*/
    public List<ChallengeResponseDto.Progress> findProgressChallenge(String nickname) {
        User user = userRepository.findByNickname(nickname).orElseThrow(); //예외 처리 예정

        List<Challenge> progressChallenge = challengeRepository.findProgressChallenge(user);
        List<ChallengeResponseDto.Progress> response = new ArrayList<>();

        for (Challenge challenge : progressChallenge) {
            Integer rank = -1; //랭킹
            LocalDate started = challenge.getStarted(); //챌린지 시작 날짜

            //해당 회원의 랭킹 추출
            if (challenge.getType() == ChallengeType.Widen) {
                RankResponseDto.Area rankList = matrixService.challengeRank(challenge, started.atStartOfDay(), LocalDateTime.now());

                for (UserResponseDto.Ranking ranking : rankList.getAreaRankings()) {
                    if (ranking.getNickname().equals(nickname)) {
                        rank = ranking.getRank();
                        break;
                    }
                }
            }
            else if (challenge.getType() == ChallengeType.Accumulate) {
                //챌린지를 함께 진행하는 회원 목록
                List<User> member = userChallengeRepository.findChallengeUsers(challenge);
                //기록 조회
                List<Tuple> matrixCount = exerciseRecordRepository.findMatrixCount(member, started.atStartOfDay(), LocalDateTime.now());
                //랭킹 계산
                for (UserResponseDto.Ranking ranking : matrixService.calculateMatrixRank(matrixCount, member)) {
                    if (ranking.getNickname().equals(nickname)) {
                        rank=ranking.getRank();
                        break;
                    }
                }
            }

            response.add(
                    ChallengeResponseDto.Progress.builder()
                            .name(challenge.getName())
                            .started(started)
                            .ended(started.plusDays(7-started.getDayOfWeek().getValue()))
                            .rank(rank) //!!랭킹 == -1에 대한 예외 처리 필요
                            .color(userChallengeRepository.findChallengeColor(user, challenge))
                            .build()
            );
        }

        return response;
    }

    /*친구와 함께 진행 중인 챌린지 리스트 조회*/
    public List<ChallengeResponseDto.Progress> findProgressChallenge(String userNickname, String friendNickname) {
        User user = userRepository.findByNickname(userNickname).orElseThrow(); //예외 처리 예정
        User friend = userRepository.findByNickname(friendNickname).orElseThrow(); //예외 처리 예정

        List<Challenge> progressChallenge = challengeRepository.findChallengesWithFriend(user, friend);
        List<ChallengeResponseDto.Progress> response = new ArrayList<>();

        for (Challenge challenge : progressChallenge) {
            Integer rank = -1; //랭킹
            LocalDate started = challenge.getStarted(); //챌린지 시작 날짜

            //해당 회원(친구)의 랭킹 추출
            if (challenge.getType() == ChallengeType.Widen) {
                RankResponseDto.Area rankList = matrixService.challengeRank(challenge, started.atStartOfDay(), LocalDateTime.now());

                for (UserResponseDto.Ranking ranking : rankList.getAreaRankings()) {
                    if (ranking.getNickname().equals(friendNickname)) {
                        rank = ranking.getRank();
                        break;
                    }
                }
            }
            else if (challenge.getType() == ChallengeType.Accumulate) {
                //챌린지를 함께 진행하는 회원 목록
                List<User> member = userChallengeRepository.findChallengeUsers(challenge);
                //기록 조회
                List<Tuple> matrixCount = exerciseRecordRepository.findMatrixCount(member, started.atStartOfDay(), LocalDateTime.now());
                //랭킹 계산
                for (UserResponseDto.Ranking ranking : matrixService.calculateMatrixRank(matrixCount, member)) {
                    if (ranking.getNickname().equals(friendNickname)) {
                        rank=ranking.getRank();
                        break;
                    }
                }
            }

            response.add(
                    ChallengeResponseDto.Progress.builder()
                            .name(challenge.getName())
                            .started(started)
                            .ended(started.plusDays(7-started.getDayOfWeek().getValue()))
                            .rank(rank) //!!랭킹 == -1에 대한 예외 처리 필요
                            .color(userChallengeRepository.findChallengeColor(user, challenge))
                            .build()
            );
        }

        return response;
    }

    /*진행 완료된 챌린지 리스트 조회*/
    public List<ChallengeResponseDto.Done> findDoneChallenge(String nickname) {
        User user = userRepository.findByNickname(nickname).orElseThrow(); //예외 처리 예정

        List<Challenge> doneChallenge = challengeRepository.findDoneChallenge(user);
        List<ChallengeResponseDto.Done> response = new ArrayList<>();

        for (Challenge challenge : doneChallenge) {
            Integer rank = -1; //랭킹
            LocalDate started = challenge.getStarted(); //챌린지 시작 날짜

            //해당 회원의 랭킹 추출
            RankResponseDto.Area rankList = matrixService.challengeRank(challenge, started.atStartOfDay(), LocalDateTime.now());

            for (UserResponseDto.Ranking ranking : rankList.getAreaRankings()) {
                if (ranking.getNickname().equals(nickname)) {
                    rank = ranking.getRank();
                    break;
                }
            }

            response.add(
                    ChallengeResponseDto.Done.builder()
                            .name(challenge.getName())
                            .started(started)
                            .ended(started.plusDays(7-started.getDayOfWeek().getValue()))
                            .rank(rank) //!!랭킹 == -1에 대한 예외 처리 필요
                            .color(userChallengeRepository.findChallengeColor(user, challenge))
                            .build()
            );
        }

        return response;
    }

    /*진행 중인 챌린지 상세 조회*/
    public ChallengeResponseDto.Detail getDetailProgress(ChallengeRequestDto.CInfo request) {
        User user = userRepository.findByNickname(request.getNickname()).orElseThrow(); //예외 처리 예정
        Challenge challenge = challengeRepository.findByUuid(request.getUuid()).orElseThrow(); //예외 처리 예정

        //필요한 변수 선언
        List<User> members = userChallengeRepository.findChallengeUsers(challenge); //본인 포함 챌린지에 참여하는 인원들

        List<MatrixDto> matrices; //영역 기록
        List<UserResponseDto.Ranking> rankings = new ArrayList<>(); //랭킹
        List<ExerciseRecord> records; //영역 기록

        LocalDate started = challenge.getStarted(); //챌린지 시작 날짜
        LocalDate ended = started.plusDays(7-started.getDayOfWeek().getValue()); //챌린지 끝나는 날(해당 주 일요일)

        Integer distance = 0; //거리
        Integer exerciseTime = 0; //운동시간
        Integer stepCount = 0; //걸음수

        //개인 기록 계산
        records = exerciseRecordRepository.findRecord(user.getId(), started.atStartOfDay(), ended.atTime(LocalTime.MAX));

        for (ExerciseRecord record : records) {
            distance += record.getDistance();
            exerciseTime += record.getExerciseTime();
            stepCount += record.getStepCount();
        }

        //영역 정보 조회
        matrices = matrixRepository.findMatrixSetByRecords(records);

        //챌린지 타입에 따른 랭킹 정보(순위, 닉네임, 점수) 계산
        if (challenge.getType().equals(ChallengeType.Widen)) {
            for (User member : members) {
                //각 유저의 챌린지 기간동안의 기록
                records = exerciseRecordRepository.findRecord(member.getId(), started.atStartOfDay(), ended.atTime(LocalTime.MAX));

                //랭킹 리스트에 추가
                rankings.add(new UserResponseDto.Ranking(1, member.getNickname(),
                                (long) matrixRepository.findMatrixSetByRecords(records).size()));
            }
            //랭킹 정렬
            rankings = matrixService.calculateAreaRank(rankings);
        }
        else if (challenge.getType().equals(ChallengeType.Accumulate)) {
            //모든 회원의 칸 수 기록을 Tuple[닉네임, 이번주 누적 칸수] 내림차순으로 정리
            List<Tuple> matrixCount = exerciseRecordRepository.findMatrixCount(members, started.atStartOfDay(), ended.atTime(LocalTime.MAX));
            //랭킹 계산
            rankings = matrixService.calculateMatrixRank(matrixCount, members);
        }

        return ChallengeResponseDto.Detail.builder()
                .name(challenge.getName())
                .type(challenge.getType())
                .started(started)
                .ended(started.plusDays(7-started.getDayOfWeek().getValue()))
                .color(userChallengeRepository.findChallengeColor(user,challenge))
                .matrices(matrices)
                .rankings(rankings)
                .distance(distance)
                .exerciseTime(exerciseTime)
                .stepCount(stepCount)
                .build();
    }

    /*해당 운동기록이 참여하고 있는 챌린지*/
    public List<ChallengeResponseDto.CInfoRes> findChallengeByRecord(ExerciseRecord exerciseRecord){

        User user = userRepository.findByExerciseRecord(exerciseRecord).orElseThrow(); // 에외 처리

        // LocalDate 형태로 변환
        LocalDateTime startedTime = exerciseRecord.getStarted();
        LocalDate startedDate = LocalDate.of(startedTime.getYear(), startedTime.getMonth(), startedTime.getDayOfMonth());
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 해당주 월요일 ~ 기록 시간 사이 시작한 챌린지들 조회
        List<Challenge> challenges = challengeRepository.findChallengesBetweenStartAndEnd(user, monday, startedDate);
        List<ChallengeResponseDto.CInfoRes> cInfoRes = new ArrayList<>();

        challenges.forEach(c -> cInfoRes.add(ChallengeResponseDto.CInfoRes
                .builder()
                .name(c.getName())
                .started(c.getStarted())
                .ended(c.getStarted().plusDays(7-c.getStarted().getDayOfWeek().getValue()))
                .color(userChallengeRepository.findChallengeColor(user, c))
                .build()));

        return cInfoRes;
    }
}
package com.dnd.ground.common;

import com.dnd.ground.domain.challenge.*;
import com.dnd.ground.domain.challenge.dto.ChallengeCreateRequestDto;
import com.dnd.ground.domain.challenge.repository.ChallengeRepository;
import com.dnd.ground.domain.challenge.repository.UserChallengeRepository;
import com.dnd.ground.domain.challenge.service.ChallengeService;
import com.dnd.ground.domain.user.LoginType;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.UserProperty;
import com.dnd.ground.domain.user.repository.UserRepository;
import com.dnd.ground.global.util.JwtUtil;
import com.dnd.ground.global.util.UuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataProvider {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ChallengeService challengeService;

    @Autowired
    ChallengeRepository challengeRepository;

    @Autowired
    UserChallengeRepository userChallengeRepository;

    @Value("${picture.path}")
    private String DEFAULT_PATH;

    @Value("${picture.name}")
    private String DEFAULT_NAME;

    private static final double LATITUDE = 37.334501;
    private static final double LONGITUDE = 126.810133;

    public void createUser(int size) {

        for (int i = 1; i <= size; i++) {
            User user = User.builder()
                    .nickname("nick" + i)
                    .email("email" + i + "@gmail.com")
                    .intro("nick" + i + "의 소개 메시지")
                    .created(LocalDateTime.now())
                    .pictureName(DEFAULT_NAME)
                    .picturePath(DEFAULT_PATH)
                    .latitude(LATITUDE + (0.3740 * i))
                    .longitude(LONGITUDE + (0.3040 * i))
                    .loginType(i % 2 == 0 ? LoginType.APPLE : LoginType.KAKAO)
                    .build();

            UserProperty property = UserProperty.builder()
                    .socialId(i % 2 == 0 ? null : String.valueOf(i))
                    .isExceptRecommend(false)
                    .isShowMine(true)
                    .isShowFriend(true)
                    .isPublicRecord(true)
                    .notiWeekStart(true)
                    .notiWeekEnd(true)
                    .notiFriendRequest(true)
                    .notiFriendAccept(true)
                    .notiChallengeRequest(true)
                    .notiChallengeAccept(true)
                    .notiChallengeStart(true)
                    .notiChallengeCancel(true)
                    .notiChallengeResult(true)
                    .build();

            user.setUserProperty(property);
            userRepository.save(user);
        }
    }

    public String getAccessToken() {
        User testDummyUser = userRepository.findByNickname("TEST_DUMMY")
                .orElseGet(() -> {
                    User user = User.builder()
                            .nickname("TEST_DUMMY")
                            .email("TEST_DUMMY" + "@gmail.com")
                            .intro("TEST_DUMMY" + "의 소개 메시지")
                            .created(LocalDateTime.now())
                            .pictureName(DEFAULT_NAME)
                            .picturePath(DEFAULT_PATH)
                            .latitude(LATITUDE)
                            .longitude(LONGITUDE)
                            .loginType(LoginType.KAKAO)
                            .build();

                    UserProperty property = UserProperty.builder()
                            .socialId("TEST_DUMMY")
                            .isExceptRecommend(false)
                            .isShowMine(false)
                            .isShowFriend(false)
                            .isPublicRecord(false)
                            .notiWeekStart(false)
                            .notiWeekEnd(false)
                            .notiFriendRequest(false)
                            .notiFriendAccept(true)
                            .notiChallengeRequest(false)
                            .notiChallengeAccept(false)
                            .notiChallengeStart(false)
                            .notiChallengeCancel(false)
                            .notiChallengeResult(false)
                            .build();

                    user.setUserProperty(property);
                    return userRepository.save(user);
                });

        return JwtUtil.createAccessToken(testDummyUser.getEmail(), LocalDateTime.now());
    }

    @Transactional
    public void createNewChallenge(String masterNickname, String member1Nickname, String member2Nickname,
                                   String challengeName, String message, ChallengeScoreType type, LocalDateTime started) {
        Set<String> members = new HashSet<>();
        if (member1Nickname != null) members.add(member1Nickname);
        if (member2Nickname != null) members.add(member2Nickname);

        started = started != null ? started : LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);

        challengeService.createChallenge(new ChallengeCreateRequestDto(masterNickname, challengeName, message, started, type, members));
    }

    @Transactional
    public void createDoneChallenge(String name, long minusWeek, String message, ChallengeScoreType type, User master, User member1, User member2) {
        Challenge challenge = Challenge.builder()
                .uuid(UuidUtil.createUUID())
                .name(name)
                .created(LocalDateTime.now().minusWeeks(minusWeek).minusDays(1))
                .started(LocalDateTime.now().minusWeeks(minusWeek))
                .ended(ChallengeService.getSunday(LocalDateTime.now().minusWeeks(minusWeek)))
                .message(message)
                .scoreType(type)
                .status(ChallengeStatus.DONE)
                .build();

        challengeRepository.save(challenge);

        if (master != null) userChallengeRepository.save(new UserChallenge(challenge, master, ChallengeColor.RED, ChallengeStatus.MASTER_DONE));
        if (member1 != null) userChallengeRepository.save(new UserChallenge(challenge, member1, ChallengeColor.PINK, ChallengeStatus.DONE));
        if (member2 != null) userChallengeRepository.save(new UserChallenge(challenge, member2, ChallengeColor.YELLOW, ChallengeStatus.DONE));
    }
}

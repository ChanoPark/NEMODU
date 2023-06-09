package com.dnd.ground.domain.challenge;

import com.dnd.ground.common.DataProvider;
import com.dnd.ground.domain.challenge.dto.ChallengeCreateRequestDto;
import com.dnd.ground.domain.challenge.dto.ChallengeDoneListResponseDto;
import com.dnd.ground.domain.challenge.dto.ChallengeInviteListResponseDto;
import com.dnd.ground.domain.challenge.dto.ChallengeResponseDto;
import com.dnd.ground.domain.challenge.repository.ChallengeRepository;
import com.dnd.ground.domain.challenge.repository.UserChallengeRepository;
import com.dnd.ground.domain.challenge.service.ChallengeService;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.repository.UserRepository;
import com.dnd.ground.global.exception.ExceptionCodeSet;
import com.dnd.ground.global.exception.UserException;
import com.dnd.ground.global.util.UuidUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("챌린지: 챌린지 목록 조회 테스트")
@Transactional
public class ChallengeListReadTest {
    @Autowired
    DataProvider dataProvider;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ChallengeRepository challengeRepository;

    @Autowired
    UserChallengeRepository userChallengeRepository;

    @Autowired
    ChallengeService challengeService;

    static String AUTHORIZATION_TOKEN;

    @BeforeEach
    public void init() {
        dataProvider.createUser(20);
        AUTHORIZATION_TOKEN = "Bearer " + dataProvider.getAccessToken();
    }

    @Nested
    @DisplayName("초대 받은 챌린지 목록 조회")
    class ReadInviteChallenge {
        @Test
        @DisplayName("초대 받은 챌린지 목록 조회 성공")
        void readInviteChallenge_Success() throws Exception {
            System.out.println(">>> 초대 받은 챌린지 목록 조회 성공 <<< 테스트 START");

            //GIVEN
            String masterNickname = "nick1";
            User master = userRepository.findByNickname("nick1")
                    .orElseThrow(() -> new UserException(ExceptionCodeSet.USER_NOT_FOUND));

            String member1Nickname = "nick2";
            String member2Nickname = "nick3";

            Set<String> members = new HashSet<>();
            members.add(member1Nickname);
            members.add(member2Nickname);

            String challengeName = "초대 받은 챌린지 목록 조회 테스트";
            String message = "초대 받은 챌린지 목록 조회 테스트입니다.";
            LocalDateTime started = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);
            ChallengeType type = ChallengeType.WIDEN;

            challengeService.createChallenge(new ChallengeCreateRequestDto(masterNickname, challengeName, message, started, type, members));


            String challenge2Name = "nick2가 포함되지 않은 챌린지 테스트";
            String message2 = "nick2가 포함되지 않은 챌린지입니다.";
            members.remove(member1Nickname);
            challengeService.createChallenge(new ChallengeCreateRequestDto(masterNickname, challenge2Name, message2, started, type, members));

            //WHEN
            String response = mvc
                    .perform(get("/challenge/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_TOKEN)
                            .param("size", "5")
                            .param("nickname", member1Nickname)
                    )
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);

            //THEN
            ChallengeInviteListResponseDto result = mapper.readValue(response, ChallengeInviteListResponseDto.class);

            List<ChallengeResponseDto.Invite> infos = result.getInfos();
            assertThat(infos.size()).isEqualTo(1);

            ChallengeResponseDto.Invite inviteChallenge = infos.get(0);
            System.out.println(">> " + inviteChallenge.toString());

            assertThat(inviteChallenge.getInviterNickname()).isEqualTo(masterNickname);
            assertThat(inviteChallenge.getName()).isEqualTo(challengeName);
            assertThat(inviteChallenge.getPicturePath()).isEqualTo(master.getPicturePath());

            Optional<Challenge> challengeOpt = challengeRepository.findByUuid(UuidUtil.hexToBytes(inviteChallenge.getUuid()));
            assertThat(challengeOpt.isPresent()).isTrue();
        }

        @Test
        @DisplayName("초대 받은 챌린지 목록 조회 성공: 아무것도 초대 받지 않았을 때")
        void readInviteChallenge_Success_NotInviteMember() throws Exception {
            System.out.println(">>> 초대 받은 챌린지 목록 조회 성공: 아무것도 초대 받지 않았을 때 <<< 테스트 START");

            //GIVEN
            String nickname = "nick1";

            //WHEN
            String response = mvc
                    .perform(get("/challenge/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_TOKEN)
                            .param("nickname", nickname)
                            .param("size", "50")
                    )
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);

            //THEN
            ChallengeInviteListResponseDto result = mapper.readValue(response, ChallengeInviteListResponseDto.class);
            assertThat(result.getInfos().isEmpty()).isTrue();
            assertThat(result.getIsLast()).isTrue();
            assertThat(result.getSize()).isEqualTo(0);
        }

        @Test
        @DisplayName("초대 받은 챌린지 목록 성공: 페이징")
        void readInviteChallenge_Success_Paging() throws Exception {
            System.out.println(">>> 초대 받은 챌린지 목록 성공: 페이징 <<< 테스트 START");

            //GIVEN
            dataProvider.createNewChallenge("nick2", "nick1", "nick3", "챌린지1", "초대 받은 챌린지 페이징1", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick3", "nick1", "nick4", "챌린지2", "초대 받은 챌린지 페이징2", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick4", "nick1", "nick5", "챌린지3", "초대 받은 챌린지 페이징3", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick5", "nick1", "nick6", "챌린지4", "초대 받은 챌린지 페이징4", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick6", "nick1", "nick7", "챌린지5", "초대 받은 챌린지 페이징5", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick7", "nick1", "nick8", "챌린지6", "초대 받은 챌린지 페이징6", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick8", "nick1", "nick9", "챌린지7", "초대 받은 챌린지 페이징7", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick9", "nick1", "nick10", "챌린지8", "초대 받은 챌린지 페이징8", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick10", "nick1", "nick11", "챌린지9", "초대 받은 챌린지 페이징9", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick11", "nick1", "nick12", "챌린지10", "초대 받은 챌린지 페이징10", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick12", "nick1", "nick13", "챌린지11", "초대 받은 챌린지 페이징11", ChallengeType.WIDEN, null);

            final int size = 3;

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("nickname", "nick1");
            params.add("size", String.valueOf(size));

            //WHEN + THEN
            ChallengeInviteListResponseDto result;
            LocalDateTime lastCreated = LocalDateTime.now();
            int lastIdx = Integer.MAX_VALUE;

            do {
                String response = mvc
                        .perform(get("/challenge/invite")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_TOKEN)
                                .params(params)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

                result = mapper.readValue(response, ChallengeInviteListResponseDto.class);

                assertThat(result.getSize()).isLessThanOrEqualTo(size);

                //최근 생성순 조회
                for (ChallengeResponseDto.Invite info : result.getInfos()) {
                    System.out.println(">>>" + info.toString());
                    int idx = Integer.parseInt(info.getName().substring(3));
                    assertThat(idx).isLessThan(lastIdx);
                    assertThat(info.getCreated()).isBefore(lastCreated);
                    lastCreated = info.getCreated();
                    lastIdx = idx;
                }

                if (!result.getIsLast()) {
                    params.remove("offset");
                    params.add("offset", String.valueOf(result.getOffset()));
                }
            } while (!result.getIsLast());
        }

        @Test
        @DisplayName("초대 받은 챌린지 목록 실패: 필수 파라미터 누락")
        void readInviteChallenge_Fail_InvalidParam() throws Exception {
            System.out.println(">>> 초대 받은 챌린지 목록 실패: 필수 파라미터 누락 <<< 테스트 START");

            //GIVEN
            dataProvider.createNewChallenge("nick2", "nick1", "nick3", "챌린지1", "초대 받은 챌린지 페이징1", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick3", "nick1", "nick4", "챌린지2", "초대 받은 챌린지 페이징2", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick4", "nick1", "nick5", "챌린지3", "초대 받은 챌린지 페이징3", ChallengeType.WIDEN, null);
            Thread.sleep(1000);
            dataProvider.createNewChallenge("nick5", "nick1", "nick6", "챌린지4", "초대 받은 챌린지 페이징4", ChallengeType.WIDEN, null);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("nickname", "nick1");

            //WHEN + THEN
            mvc.perform(get("/challenge/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_TOKEN)
                            .params(params)
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("완료된 챌린지 목록 조회")
    class ReadDoneChallenge {
        @Test
        @DisplayName("완료된 챌린지 목록 조회 성공")
        void readDoneChallenge_Success() throws Exception {
            System.out.println(">>> 완료된 챌린지 목록 조회 성공 <<< 테스트 START");

            //GIVEN
            String targetNickname = "nick1";
            User user1 = userRepository.findByNickname(targetNickname).get();
            User user2 = userRepository.findByNickname("nick2").get();
            User user3 = userRepository.findByNickname("nick3").get();
            User user4 = userRepository.findByNickname("nick4").get();
            User user5 = userRepository.findByNickname("nick5").get();

            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회1", 6, "챌린지 목록 조회1", ChallengeType.WIDEN, user1, user2, user3);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회2", 5, "챌린지 목록 조회2", ChallengeType.WIDEN, user2, user1, user4);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회3", 4, "챌린지 목록 조회3", ChallengeType.ACCUMULATE, user3, user4, user1);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회4", 3, "챌린지 목록 조회4", ChallengeType.WIDEN, user1, user5, user4);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회5", 2, "챌린지 목록 조회5", ChallengeType.ACCUMULATE, user5, user1, user2);

            final int size = 5;

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("nickname", targetNickname);
            params.add("size", String.valueOf(size));

            //WHEN
            String response = mvc
                    .perform(get("/challenge/done")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_TOKEN)
                            .params(params)
                    )
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);


            //THEN
            ChallengeDoneListResponseDto result = mapper.readValue(response, ChallengeDoneListResponseDto.class);

            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getIsLast()).isTrue();
            assertThat(result.getOffset()).isNull();

            List<ChallengeResponseDto.Done> infos = result.getInfos();
            LocalDateTime lastStarted = LocalDateTime.now();

            for (ChallengeResponseDto.Done info : infos) {
                System.out.println(">>>" + info.toString());

                assertThat(info.getStarted()).isBefore(lastStarted);
                lastStarted = info.getStarted();
            }
        }

        @Test
        @DisplayName("완료된 챌린지 목록 조회 성공: 완료된 챌린지가 없을 때")
        void readDoneChallenge_Success_Empty() throws Exception {
            System.out.println(">>> 완료된 챌린지 목록 조회 성공: 완료된 챌린지가 없을 때 <<< 테스트 START");

            //GIVEN
            User user1 = userRepository.findByNickname("nick1").get();
            User user2 = userRepository.findByNickname("nick2").get();
            User user3 = userRepository.findByNickname("nick3").get();
            User user4 = userRepository.findByNickname("nick4").get();
            String nickname = "nick5";

            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회1", 6, "챌린지 목록 조회1", ChallengeType.WIDEN, user1, user2, user3);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회2", 5, "챌린지 목록 조회2", ChallengeType.WIDEN, user2, user1, user4);
            final int size = 5;

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("nickname", nickname);
            params.add("size", String.valueOf(size));

            //WHEN
            String response = mvc
                    .perform(get("/challenge/done")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_TOKEN)
                            .params(params)
                    )
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);


            //THEN
            ChallengeDoneListResponseDto result = mapper.readValue(response, ChallengeDoneListResponseDto.class);

            assertThat(result.getInfos().isEmpty()).isTrue();
            assertThat(result.getIsLast()).isTrue();
            assertThat(result.getOffset()).isNull();
        }

        @Test
        @DisplayName("완료된 받은 챌린지 목록 성공: 페이징")
        void readDoneChallenge_Success_Paging() throws Exception {
            System.out.println(">>> 완료된 챌린지 목록 성공: 페이징 <<< 테스트 START");

            //GIVEN
            String targetNickname = "nick1";
            User user1 = userRepository.findByNickname(targetNickname).get();
            User user2 = userRepository.findByNickname("nick2").get();
            User user3 = userRepository.findByNickname("nick3").get();
            User user4 = userRepository.findByNickname("nick4").get();
            User user5 = userRepository.findByNickname("nick5").get();

            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회1", 6, "챌린지 목록 조회1", ChallengeType.WIDEN, user1, user2, user3);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회2", 5, "챌린지 목록 조회2", ChallengeType.WIDEN, user2, user1, user4);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회3", 4, "챌린지 목록 조회3", ChallengeType.ACCUMULATE, user3, user4, user1);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회4", 3, "챌린지 목록 조회4", ChallengeType.WIDEN, user1, user5, user4);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회5", 2, "챌린지 목록 조회5", ChallengeType.ACCUMULATE, user5, user1, user2);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회6", 2, "챌린지 목록 조회6", ChallengeType.WIDEN, user1, user2, user3);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회7", 3, "챌린지 목록 조회7", ChallengeType.WIDEN, user2, user3, user4);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회8", 4, "챌린지 목록 조회8", ChallengeType.ACCUMULATE, user3, user4, user5);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회9", 5, "챌린지 목록 조회9", ChallengeType.WIDEN, user4, user5, user1);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회10", 6, "챌린지 목록 조회10", ChallengeType.ACCUMULATE, user5, user1, user2);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회11", 7, "챌린지 목록 조회11", ChallengeType.WIDEN, user1, user2, user3);

            final int size = 3;

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("nickname", targetNickname);
            params.add("size", String.valueOf(size));

            //WHEN + THEN
            ChallengeDoneListResponseDto result;
            LocalDateTime lastStarted = LocalDateTime.now();
            int lastIdx = Integer.MAX_VALUE;

            do {
                String response = mvc
                        .perform(get("/challenge/done")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_TOKEN)
                                .params(params)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

                result = mapper.readValue(response, ChallengeDoneListResponseDto.class);

                assertThat(result.getSize()).isLessThanOrEqualTo(size);

                //최근 생성순 조회
                for (ChallengeResponseDto.Done info : result.getInfos()) {
                    System.out.println(">>>" + info.toString());
                    int idx = Integer.parseInt(info.getName().substring(13));

                    assertThat(idx).isLessThan(lastIdx);
                    assertThat(info.getStarted()).isBefore(lastStarted);

                    lastStarted = info.getStarted();
                    lastIdx = idx;
                }

                if (!result.getIsLast()) {
                    params.remove("offset");
                    params.add("offset", String.valueOf(result.getOffset()));
                }
            } while (!result.getIsLast());
        }

        @Test
        @DisplayName("완료된 챌린지 목록 실패: 필수 파라미터 누락")
        void readDoneChallenge_Fail_InvalidParam() throws Exception {
            System.out.println(">>> 완료된 챌린지 목록 실패: 필수 파라미터 누락 <<< 테스트 START");

            //GIVEN
            String targetNickname = "nick1";
            User user1 = userRepository.findByNickname(targetNickname).get();
            User user2 = userRepository.findByNickname("nick2").get();
            User user3 = userRepository.findByNickname("nick3").get();
            User user4 = userRepository.findByNickname("nick4").get();

            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회1", 6, "챌린지 목록 조회1", ChallengeType.WIDEN, user1, user2, user3);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회2", 5, "챌린지 목록 조회2", ChallengeType.WIDEN, user2, user1, user4);
            dataProvider.createDoneChallenge("완료된 챌린지 목록 조회3", 4, "챌린지 목록 조회3", ChallengeType.ACCUMULATE, user3, user4, user1);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("nickname", "nick1");

            //WHEN + THEN
            mvc.perform(get("/challenge/done")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_TOKEN)
                            .params(params)
                    )
                    .andExpect(status().isBadRequest());
        }
    }
}

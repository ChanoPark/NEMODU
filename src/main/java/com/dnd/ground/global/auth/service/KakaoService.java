package com.dnd.ground.global.auth.service;

import com.amazonaws.util.StringUtils;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.dto.KakaoDto;
import com.dnd.ground.domain.user.repository.UserPropertyRepository;
import com.dnd.ground.global.auth.dto.SocialResponseDto;
import com.dnd.ground.domain.user.repository.UserRepository;
import com.dnd.ground.global.auth.vo.KakaoFriendVo;
import com.dnd.ground.global.exception.AuthException;
import com.dnd.ground.global.exception.ExceptionCodeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 박찬호
 * @description 카카오를 비롯한 회원 정보와 관련한 서비스
 * @since 2022-08-23
 * @updated 1. 카카오 친구 목록 조회 API 수정
 *           - 2023.01.20 박찬호
 */

@RequiredArgsConstructor
@Slf4j
@Service
public class KakaoService {
    WebClient webClient;
    private final UserRepository userRepository;
    private final UserPropertyRepository userPropertyRepository;

    @Value("${kakao.REST_KEY}")
    private static String REST_API_KEY;

    @Value("${kakao.REDIRECT_URI}")
    private static String REDIRECT_URI;

    @Value("${picture.path}")
    private static String DEFAULT_PATH;

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    private static final String REDIRECT_URI_KEY = "redirect_uri";
    private static final String CODE = "code";

    @PostConstruct
    public void initWebClient() {
        webClient = WebClient.create();
    }

    /**
     * 카카오 토큰 발급
     */
    public SocialResponseDto.KakaoRedirectDto kakaoRedirect(String code) throws NullPointerException {
        //토큰을 받기 위한 HTTP Body 생성
        MultiValueMap<String, String> getTokenBody = new LinkedMultiValueMap<>();
        getTokenBody.add(GRANT_TYPE, "authorization_code");
        getTokenBody.add(CLIENT_ID, REST_API_KEY);
        getTokenBody.add(REDIRECT_URI_KEY, REDIRECT_URI);
        getTokenBody.add(CODE, code);

        //카카오 토큰 발급 API 호출
        KakaoDto.Token token = webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(getTokenBody))
                .retrieve()
                .bodyToMono(KakaoDto.Token.class)
                .block();

        SocialResponseDto.KakaoRedirectDto response = new SocialResponseDto.KakaoRedirectDto(token.getAccess_token(), token.getRefresh_token());

        //이메일 받아오기
        KakaoDto.UserInfo userInfo = getUserInfo(token.getAccess_token());
        response.setEmail(userInfo.getEmail());

        return response;
    }

    public SocialResponseDto kakaoLogin(String token) {
        KakaoDto.UserInfo userInfo = getUserInfo(token);
        Optional<User> userOpt = userRepository.findByEmail(userInfo.getEmail());

        boolean isSigned;
        String email = userInfo.getEmail();
        String picturePath;
        String pictureName;

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            pictureName = user.getPictureName();
            picturePath= user.getPicturePath();
            isSigned = true;
        } else {
            picturePath = userInfo.getPicturePath();
            pictureName = userInfo.getPictureName();
            isSigned = false;
        }

        return new SocialResponseDto(email, picturePath, pictureName, isSigned);
    }

    /*카카오 엑세스 토큰 정보 확인*/
    public KakaoDto.TokenInfo getTokenInfo(String token) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v1/user/access_token_info")
                .header(AUTHORIZATION, BEARER + token)
                .retrieve()
                .bodyToMono(KakaoDto.TokenInfo.class)
                .block();
    }

    /*사용자 정보 확인*/
    public KakaoDto.UserInfo getUserInfo(String token) {
        String userKakaoInfo = webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(AUTHORIZATION, BEARER + token)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();

        JSONObject jsonObject;
        try{
            jsonObject = (JSONObject) jsonParser.parse(userKakaoInfo); //Cast: String -> Json Object
        } catch (ParseException e) {
            throw new AuthException(ExceptionCodeSet.INTERNAL_SERVER_ERROR);
        }

        if (jsonObject == null) {
            throw new AuthException(ExceptionCodeSet.INTERNAL_SERVER_ERROR);
        }

        long kakaoId = (long) jsonObject.get("id"); // 카카오 회원번호 추출
        jsonObject = (JSONObject) jsonObject.get("kakao_account"); // 필요한 회원 정보가 있는 Object 분리
        JSONObject pictureObject = (JSONObject) jsonObject.get("profile"); //프로필 사진과 관련한 Object 분리

        return KakaoDto.UserInfo.builder()
                .kakaoId(kakaoId)
                .email((String) jsonObject.get("email"))
                .pictureName("kakao/" + kakaoId)
                .picturePath((String) pictureObject.get("profile_image_url"))
                .build();
    }


    /*카카오 친구 목록 조회*/
    public KakaoDto.KakaoFriendResponse getKakaoFriends(String token, Integer offset, Integer size) {
        KakaoDto.FriendsInfoFromKakao friendsInfoFromKakao = requestKakaoFriends(token, offset, size);
        List<KakaoDto.KakaoFriendResponse.KakaoFriend> friends = new ArrayList<>();

        List<String> kakaoIds = friendsInfoFromKakao.getElements().stream()
                .map(KakaoDto.FriendsInfoFromKakao.KakaoFriendElement::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());

        Map<Long, User> signedFriend = userPropertyRepository.findBySocialIds(kakaoIds)
                .stream()
                .collect(Collectors.toMap(KakaoFriendVo::getSocialId, KakaoFriendVo::getUser));


        for (KakaoDto.FriendsInfoFromKakao.KakaoFriendElement element : friendsInfoFromKakao.getElements()) {
            String nickname = null;
            boolean isSigned = false;
            String picturePath = StringUtils.isNullOrEmpty(element.getProfile_thumbnail_image()) ? DEFAULT_PATH : element.getProfile_thumbnail_image();

            User user = signedFriend.getOrDefault(element.getId(), null);
            if (user != null) {
                nickname = user.getNickname();
                isSigned = true;
                picturePath = user.getPicturePath();
            }


            friends.add(new KakaoDto.KakaoFriendResponse.KakaoFriend(
                    element.getUuid(),
                    element.getProfile_nickname(),
                    nickname,
                    isSigned,
                    picturePath
            ));
        }

        return new KakaoDto.KakaoFriendResponse(friendsInfoFromKakao.getAfter_url() == null, friends, offset + friends.size());
    }

    /*카카오 친구 목록 요청*/
    public KakaoDto.FriendsInfoFromKakao requestKakaoFriends(String token, int offset, int limit) {
        return webClient.get()
                .uri(UriComponentsBuilder.newInstance()
                        .scheme("https")
                        .host("kapi.kakao.com")
                        .path("/v1/api/talk/friends")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .toUriString())
                .header(AUTHORIZATION, BEARER + token)
                .retrieve()
                .bodyToMono(KakaoDto.FriendsInfoFromKakao.class)
                .block();
    }

    public Map<String, String> reissueKakaoToken(String token) {
        MultiValueMap<String, String> reissueTokenBody = new LinkedMultiValueMap<>();
        reissueTokenBody.add(GRANT_TYPE, "refresh_token");
        reissueTokenBody.add(CLIENT_ID, REST_API_KEY);
        reissueTokenBody.add(REDIRECT_URI_KEY, REDIRECT_URI);
        reissueTokenBody.add("refresh_token", token);

        KakaoDto.ReissueToken result = webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(reissueTokenBody))
                .retrieve()
                .bodyToMono(KakaoDto.ReissueToken.class)
                .block();

        HashMap<String, String> response = new HashMap<>();
        response.put("Kakao-Access-Token", result.getAccess_token());
        response.put("Kakao-Refresh-Token", result.getRefresh_token());

        return response;
    }
}
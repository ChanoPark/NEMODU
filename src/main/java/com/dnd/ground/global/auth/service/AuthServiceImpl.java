package com.dnd.ground.global.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.dnd.ground.domain.friend.service.FriendService;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.global.auth.UserClaim;
import com.dnd.ground.domain.user.dto.*;
import com.dnd.ground.domain.user.repository.UserRepository;
import com.dnd.ground.global.auth.dto.UserSignDto;
import com.dnd.ground.global.exception.*;
import com.dnd.ground.global.auth.dto.JWTReissueResponseDto;
import com.dnd.ground.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @description 회원의 인증/인가 및 회원 정보 관련 서비스 구현체
 * @author  박세헌, 박찬호
 * @since   2022-09-07
 * @updated 1. 미사용 API 삭제(온보딩, 회원가입V1)
 *          - 2022.01-23 박찬호
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService, UserDetailsService {

    private final UserRepository userRepository;
    private final KakaoService kakaoService;
    private final FriendService friendService;
    @Value("${jwt.secret_key}")
    private String SECRET_KEY;

    @Value("${ip}")
    private String IP;

    /*회원 저장*/
    @Transactional
    public User save(JwtUserDto user){
        return userRepository.save(User.builder()
                .nickname(user.getNickname())
                .email(user.getMail())
                .created(LocalDateTime.now())
                .intro("")
                .latitude(null)
                .longitude(null)
                .isShowMine(true)
                .isShowFriend(true)
                .isPublicRecord(user.getIsPublicRecord())
                .pictureName(user.getPictureName())
                .picturePath(user.getPicturePath())
                .build());
    }

    /**
     * 회원가입 V1
     * @deprecated
     * */
//    public ResponseEntity<UserResponseDto.SignUp> signUp(String kakaoAccessToken, UserRequestDto.SignUp request) throws ParseException, UnknownHostException {
//
//        //카카오 회원 정보 조회(카카오 ID, 이메일, 프로필 사진)
//        KakaoDto.UserInfo kakaoUserInfo = kakaoService.getUserInfo(kakaoAccessToken);
//
//        WebClient webClient = WebClient.create();
//
//        JwtUserDto jwtUserDto = JwtUserDto.builder()
//                .id(kakaoUserInfo.getKakaoId())
//                .kakaoRefreshToken(request.getKakaoRefreshToken())
//                .nickname(request.getNickname())
//                .isPublicRecord(request.getIsPublicRecord())
//                .mail(kakaoUserInfo.getEmail())
//                .pictureName(kakaoUserInfo.getPictureName())
//                .picturePath(kakaoUserInfo.getPicturePath())
//                .build();
//
//        //필터 호출
//        ResponseEntity<UserResponseDto.SignUp> response = webClient.post()
//                .uri("http://"+IP+":8080/sign")//서버 배포시 서버에 할당된 IP로 변경 예정
//                .body(Mono.just(jwtUserDto), JwtUserDto.class)
//                .retrieve()
//                .toEntity(UserResponseDto.SignUp.class)
//                .block();
//
//        return response;
//    }

    @Transactional
    public UserClaim signUp(UserSignDto signDto) {
        //닉네임 중복 확인
        String nickname = signDto.getNickname();
        if (userRepository.findByNickname(nickname).isPresent())
            throw new AuthException(ExceptionCodeSet.DUPLICATE_NICKNAME);

        //회원 생성
        Optional<User> userOpt = userRepository.findByEmail(signDto.getEmail());
        if (userOpt.isEmpty()) {
            User user = User.builder()
                    .created(LocalDateTime.now())
                    .nickname(nickname)
                    .email(signDto.getEmail())
                    .intro(signDto.getIntro())
                    .latitude(null)
                    .longitude(null)
                    .isShowMine(true)
                    .isShowFriend(true)
                    .isPublicRecord(signDto.getIsPublicRecord())
                    .pictureName(signDto.getPictureName())
                    .picturePath(signDto.getPicturePath())
                    .loginType(signDto.getLoginType())
                    .build();

            userRepository.save(user);

            //친구 신청
            for (String friendNickname : signDto.getFriends()) {
                Boolean result = friendService.requestFriend(nickname, friendNickname);
                if (!result) throw new FriendException(ExceptionCodeSet.FRIEND_NOT_FOUND);
            }

            return new UserClaim(signDto.getEmail(), nickname, user.getCreated(), user.getLoginType());

        } else throw new AuthException(ExceptionCodeSet.SIGN_DUPLICATED);
    }

    /* 토큰으로 닉네임 찾은 후 반환하는 함수 */
//    public ResponseEntity<Map<String, String>> getNicknameByToken(HttpServletRequest request){
//        String accessToken = request.getHeader("Authorization");
//        String refreshToken = request.getHeader("Refresh-Token");
//
//        UserClaim result = null;
//        if (accessToken != null) {
//            result = JwtUtil.verify(accessToken.substring("Bearer ".length()));
//        }
//        else{
//            result = JwtUtil.verify(refreshToken.substring("Bearer ".length()));
//        }
//
//        Map<String, String> nick = new HashMap<>();
//        nick.put("nickname", result.getNickname());
//
//        return ResponseEntity.ok(nick);
//    }

    /* AuthenticationManager가 User를 검증하는 함수 */
    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new FilterException(ExceptionCodeSet.USER_NOT_FOUND)
        );

        long createdMilli = UserClaim.changeCreatedFormat(user.getCreated());

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password(passwordEncoder.encode(user.getNickname()+createdMilli))
                .authorities("BASIC")
                .build();
    }

    /**--회원 정보 관련 로직--**/

    /*닉네임 Validation*/
    public Boolean validateNickname(String nickname) {
        Pattern rex = Pattern.compile("[^\uAC00-\uD7A3xfe0-9a-zA-Z]");
        return nickname.length() >= 2 && nickname.length() <= 6 // 2~6글자
                && userRepository.findByNickname(nickname).isEmpty() //중복X
                && !rex.matcher(nickname).find(); // 특수문자
    }

    /*기존 유저인지 판별*/
    public Boolean isOriginalUser(HttpServletRequest request) {
        String accessToken = request.getHeader("Kakao-Access-Token");

        KakaoDto.TokenInfo tokenInfo = kakaoService.getTokenInfo(accessToken);

        return userRepository.findByKakaoId(tokenInfo.getId()).isPresent();
    }

    /* 리프레시 토큰이 오면 JWTCheckFilter에서 검증 후 성공적으로 filter를 통과 했다면 해당 로직에서 토큰 재발급 */
    public ResponseEntity<JWTReissueResponseDto> issuanceToken(String refreshToken){

        String token = refreshToken.substring("Bearer ".length());
        UserClaim result = JwtUtil.verify(token);
        // 토큰 재발급, 리프레시 토큰은 저장
        String accessToken = JwtUtil.createAccessToken(result.getNickname());
        refreshToken = JwtUtil.createRefreshToken(result.getNickname());

        User user = userRepository.findByNickname(result.getNickname()).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Refresh-Token", "Bearer " + refreshToken);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new JWTReissueResponseDto(ExceptionCodeSet.OK.getMessage(), ExceptionCodeSet.OK.getCode(), user.getLoginType()));
    }
    /* 새로운 닉네임으로 토큰 재발급 */
    public ResponseEntity<UserResponseDto.UInfo> issuanceTokenByNickname(String nickname){
        String accessToken = JwtUtil.createAccessToken(nickname);
        String refreshToken = JwtUtil.createRefreshToken(nickname);

        User user = userRepository.findByNickname(nickname).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Refresh-Token", "Bearer " + refreshToken);

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(new UserResponseDto.UInfo(user.getNickname(), user.getPicturePath()));
    }

    /*토큰을 통해 UserClaim 반환*/
    public UserClaim getUserClaim(String token) {
        final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);

        DecodedJWT decodedToken = JWT.require(ALGORITHM).build().verify(token);
        String email = decodedToken.getSubject();

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return new UserClaim(email, user.getNickname(), user.getCreated(), user.getLoginType());
        } else throw new AuthException(ExceptionCodeSet.WRONG_TOKEN);
    }

}
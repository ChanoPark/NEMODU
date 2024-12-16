package com.dnd.ground.domain.challenge.controller;

import com.dnd.ground.domain.challenge.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;

/**
 * @description 챌린지와 관련된 컨트롤러의 역할을 분리한 인터페이스
 * @author  박찬호
 * @since   2022-08-01
 * @updated 1.초대 받은 챌린지 페이징 적용
 *          2023-06-06 박찬호
 */

public interface ChallengeController {
    ResponseEntity<ChallengeCreateResponseDto> createChallenge(@RequestBody ChallengeCreateRequestDto challengeCreateRequestDto);
    ResponseEntity<ChallengeResponseDto.Status> acceptChallenge(@RequestBody ChallengeRequestDto.CInfo requestDto);
    ResponseEntity<ChallengeResponseDto.Status> rejectChallenge(@RequestBody ChallengeRequestDto.CInfo requestDto);
    ResponseEntity<List<ChallengeResponseDto.Wait>> getWaitChallenges(@RequestParam("nickname") String nickname);
    ResponseEntity<List<ChallengeResponseDto.Progress>> getProgressChallenges(@RequestParam("nickname") String nickname);
    ResponseEntity<ChallengeInviteListResponseDto> getInviteChallenges(@ModelAttribute ChallengeRequestDto.ChallengePageRequest request);
    ResponseEntity<ChallengeResponseDto.Detail> getDetailProgressChallenge(@RequestBody ChallengeRequestDto.CInfo requestDto);
    ResponseEntity<ChallengeMapResponseDto.DetailMap> getChallengeDetailMap(@Valid @ModelAttribute ChallengeMapRequestDto request);
}

package com.dnd.ground.domain.challenge.dto;

import com.dnd.ground.domain.user.dto.UserResponseDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description 챌린지 생성과 관련한 Response DTO
 * @author  박찬호
 * @since   2022-08-26
 * @updated 1. 시간 필드 타입 LocalDate -> LocalDateTime으로 변경
 *          - 2023.02.27
 */

@Getter
@Builder
public class ChallengeCreateResponseDto {
    @ApiModelProperty(value = "회원 목록", example = "\\'users\\': [{\\'nickname\\': \\'NickB\\',\\'picturePath\\': https://dnd-ground-bucket.s3.ap-northeast-2.amazonaws.com/user/profile/default_profile.png}]")
    private List<UserResponseDto.UInfo> members;

    @ApiModelProperty(value = "챌린지 메시지", example = "너~ 가보자고~")
    private String message;

    @ApiModelProperty(value = "챌린지 시작 날짜", example = "2022-08-16T00:00:00")
    private LocalDateTime started;
    
    @ApiModelProperty(value = "챌린지 종료 날짜", example = "2022-08-21T00:00:00")
    private LocalDateTime ended;

    @ApiModelProperty(value = "챌린지에서 제외된 멤버 수", example = "2")
    private Integer exceptMemberCount;

    @ApiModelProperty(value = "챌린지에서 제외된 멤버 닉네임", example = "[NickB, NickC]")
    private List<String> exceptMembers;
}

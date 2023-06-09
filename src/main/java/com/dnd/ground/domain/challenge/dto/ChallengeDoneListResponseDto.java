package com.dnd.ground.domain.challenge.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @description 완료된 챌린지 목록 Response DTO
 * @author  박찬호
 * @since   2023-06-09
 * @updated 1. 페이징 적용에 따른 DTO 분리
 *          - 2023.06.09
 */

@AllArgsConstructor
@Getter
public class ChallengeDoneListResponseDto {
    @ApiModelProperty(value="초대 받은 챌린지 정보")
    private List<ChallengeResponseDto.Done> infos;

    @ApiModelProperty(value="챌린지 수", example="3")
    private Integer size;

    @ApiModelProperty(value="마지막 페이지 여부", example = "true")
    private Boolean isLast;

    @ApiModelProperty(value="다음 페이지에 필요한 ID", example = "4")
    private Long offset;
}

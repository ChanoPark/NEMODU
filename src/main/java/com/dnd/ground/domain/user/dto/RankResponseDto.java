package com.dnd.ground.domain.user.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @description 랭킹 Response Dto
 * @author  박세헌, 박찬호
 * @since   2022-08-08
 * @updated 1. @Data 어노테이션 제거
 *          2. 누적 랭킹 DTO(Matrix) 클래스 분리
 *          - 2023.06.02 박찬호
 */

public class RankResponseDto {
    @Getter
    @AllArgsConstructor
    public static class Matrix {
        @ApiModelProperty(value="누적 영역의 수를 기준 내림차순으로 유저들을 정렬", required = true)
        List<UserResponseDto.Ranking> matrixRankings;
    }

    @Getter
    @AllArgsConstructor
    public static class Area {
        @ApiModelProperty(value="누적 칸의 수를 기준 내림차순으로 유저들을 정렬", required = true)
        List<UserResponseDto.Ranking> areaRankings;
    }

    @Getter
    @AllArgsConstructor
    public static class Step {
        @ApiModelProperty(value="누적 걸음수를 기준 내림차순으로 유저들을 정렬", required = true)
        List<UserResponseDto.Ranking> stepRankings;
    }

}

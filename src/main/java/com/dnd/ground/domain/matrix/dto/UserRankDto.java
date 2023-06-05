package com.dnd.ground.domain.matrix.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description 누적 랭킹 조회할 때 사용하는 DTO
 * @author  박찬호
 * @since   2023-06-05
 * @updated 1.클래스 생성
 *          - 2023-06-05 박찬호
 */

@AllArgsConstructor
@Getter
public class UserRankDto {
    private String nickname;
    private Double score;
    private Integer rank;
}

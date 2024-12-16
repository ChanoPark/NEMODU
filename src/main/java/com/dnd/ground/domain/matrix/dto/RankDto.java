package com.dnd.ground.domain.matrix.dto;

import com.dnd.ground.domain.challenge.ChallengeScoreType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @description QueryDSL에서 받아올 랭킹 관련 DTO
 * @author  박찬호
 * @since   2023-02-19
 * @updated 1.ChallengeType -> ChallengeScoreType 클래스명 및 관련 변수명 수정
 *          - 2023-12-27 박찬호
 */

@AllArgsConstructor
@Setter
@Getter
public class RankDto implements Comparable<RankDto> {
    private String nickname;
    private String picturePath;
    private ChallengeScoreType scoreType;
    private Long score;

    public RankDto(String nickname, String picturePath, Long score) {
        this.nickname = nickname;
        this.picturePath = picturePath;
        this.score = score;
    }

    @Override
    public int compareTo(RankDto o) {
        return (int) (o.getScore() - this.score);
    }
}

package com.dnd.ground.domain.challenge;

/**
 * @description 챌린지 점수 계산 방식
 *              ACCUMULATE  - 칸 많이 누적하기
 *              WIDEN       - 영역 넓히기
 * @author  박찬호
 * @since   2022-08-09
 * @updated 1.ChallengeType -> ChallengeScoreType 클래스명 및 관련 변수명 수정
 *          - 2023-12-27 박찬호
 */

public enum ChallengeScoreType {
    ACCUMULATE, WIDEN
}

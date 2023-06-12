package com.dnd.ground.domain.challenge.dto;

import com.dnd.ground.domain.challenge.Challenge;
import com.dnd.ground.domain.challenge.UserChallenge;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.util.List;

/**
 * @description 챌린지와 포함된 UC를 전달하기 위한 DTO
 * @author  박찬호
 * @since   2023-04-12
 * @updated 1. 클래스 생성
 *          2023-04-12 박찬호
 */


@Getter
public class ChallengeUcsDto {
    private Challenge challenge;
    private List<UserChallenge> ucs;

    @QueryProjection
    public ChallengeUcsDto(Challenge challenge, List<UserChallenge> ucs) {
        this.challenge = challenge;
        this.ucs = ucs;
    }
}

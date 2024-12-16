package com.dnd.ground.domain.challenge.dto;

import com.dnd.ground.domain.challenge.ChallengeStatus;
import com.dnd.ground.domain.user.User;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author 박찬호
 * @description 챌린지 조회를 위한 조건 클래스
 * @since 2023-03-01
 * @updated 1.페이징 관련 필드 추가
 *          2023-06-06 박찬호
 */

@Getter
public class ChallengeCond {
    private User user;
    private List<ChallengeStatus> statusList;
    private LocalDateTime started;
    private LocalDateTime ended;
    private byte[] uuid;
    private Long id;
    private Integer size;

    public ChallengeCond(User user, List<ChallengeStatus> statusList) {
        this.user = user;
        this.statusList = statusList;
    }

    public ChallengeCond(Long id, Integer size, User user, List<ChallengeStatus> statusList) {
        this.id = id;
        this.size = size;
        this.user = user;
        this.statusList = statusList;
    }

    public ChallengeCond(User user, LocalDateTime started, LocalDateTime ended) {
        this.user = user;
        this.started = started;
        this.ended = ended;
    }

    public ChallengeCond(User user) {
        this.user = user;
    }
}

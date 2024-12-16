package com.dnd.ground.domain.challenge;

import com.dnd.ground.domain.user.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @description User - Challenge 간 조인 테이블
 * @author  박찬호
 * @since   2022-07-27
 * @updated 1. 각 회원별 챌린지 시작-종료 시간 관리 추가
 *          - 2023-12-21 박찬호
 */

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(name = "user_challenge")
@Entity
public class UserChallenge {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChallengeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ChallengeColor color;

    @Column(name = "challenge_started", nullable = false)
    private LocalDateTime started;

    @Column(name = "challenge_ended", nullable = false)
    private LocalDateTime ended;

    //Constructor
    public UserChallenge(Challenge challenge, User user, ChallengeColor color, ChallengeStatus status) {
        this.user = user;
        this.challenge = challenge;
        this.status = status;
        this.color = color;
    }

    public UserChallenge(Challenge challenge, User user, ChallengeColor color, ChallengeStatus status, LocalDateTime started) {
        this.user = user;
        this.challenge = challenge;
        this.status = status;
        this.color = color;
        this.started = started;
    }

    /**
     * 챌린지 상태 변경
     */
    public void changeStatus(ChallengeStatus status) {
        this.status = status;
    }

    /**
     * 회원 삭제 과정에서 '알 수 없음' 회원으로 변경할 때 사용.
     * @param user
     */
    public void changeUser(User user) {
        this.user = user;
    }

    public void setEnded(LocalDateTime ended) {
        this.ended = ended;
    }
}

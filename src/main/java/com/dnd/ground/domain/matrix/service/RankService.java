package com.dnd.ground.domain.matrix.service;

import com.dnd.ground.domain.matrix.dto.RankDto;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.dto.RankResponseDto;
import com.dnd.ground.domain.user.dto.UserRequestDto;
import com.dnd.ground.domain.user.dto.UserResponseDto;

import java.util.List;

/**
 * @description 랭킹 관련 서비스
 * @author  박찬호
 * @since   2022-08-01
 * @updated 1.전체 누적 랭킹 조회 API 구현
 *          - 2023-06-06 박찬호
 */

public interface RankService {
    UserResponseDto.Ranking matrixRankingAllTime(String nickname);
    RankResponseDto.Matrix matrixRankingAllTime(int offset, int size);
    RankResponseDto.Area areaRanking(UserRequestDto.LookUp requestDto);
    RankResponseDto.Step stepRanking(UserRequestDto.LookUp requestDto);
    List<UserResponseDto.Ranking> calculateUsersRank(List<RankDto> rankMatrixRank);
    UserResponseDto.Ranking calculateUserRank(List<RankDto> ranks, User targetUser);
}

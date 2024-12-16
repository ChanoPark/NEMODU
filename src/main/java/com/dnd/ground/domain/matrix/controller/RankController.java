package com.dnd.ground.domain.matrix.controller;

import com.dnd.ground.domain.user.dto.RankResponseDto;
import com.dnd.ground.domain.user.dto.UserRequestDto;
import com.dnd.ground.domain.user.dto.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @description 랭킹 관련 컨트롤러 인터페이스
 * @author  박찬호
 * @since   2022-08-02
 * @updated 1.전체 누적 랭킹 조회 API 구현
 *          - 2023-06-06 박찬호
 */

public interface RankController {
    ResponseEntity<UserResponseDto.Ranking> matrixRank(@PathVariable("nickname") String nickname);
    ResponseEntity<RankResponseDto.Matrix> matrixRank(@RequestParam("offset") Integer offset, @RequestParam("size") Integer size);
    ResponseEntity<RankResponseDto.Area> areaRank(@RequestBody UserRequestDto.LookUp requestDto);
    ResponseEntity<RankResponseDto.Step> stepRank(@ModelAttribute UserRequestDto.LookUp requestDto);
}

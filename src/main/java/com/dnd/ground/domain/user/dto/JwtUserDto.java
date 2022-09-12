package com.dnd.ground.domain.user.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description 카카오 정보를 받기 위한 dto
 * @author  박세헌
 * @since   2022-08-24
 * @updated 1.카카오 리프레시 토큰 추가
 *          - 2022.09.12 박찬호
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtUserDto {
    private Long id;  // 카카오 id
    private String nickname;
    private String mail;
    private String pictureName;
    private String picturePath;
    private String kakaoRefreshToken;
}
package com.dnd.ground.global.exception;

import java.util.List;

/**
 * @description 에러 코드 인터페이스: 각 패키지(기능) 별 예외 처리가 분리될 경우를 대비(확장성 고려)한 인터페이스 추상화
 * @author  박찬호
 * @since   2022-08-24
 * @updated 1. 예외 상황을 사용자에게 알리는 용도의 에러 메시지와 디버깅용 메시지 분리
 *          - 2023.05.23 박찬호
 */

public interface BaseException {
    ExceptionCodeSet getExceptionCode();
    String getDebugMessage();
    String getMessage();
    String getCode();
    List<String> fewStackTrace();
}

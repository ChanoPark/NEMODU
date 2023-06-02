package com.dnd.ground.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLIntegrityConstraintViolationException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 박찬호
 * @description 전역 예외 처리를 위한 Advice 클래스
 * @since 2022-08-24
 * @updated 1. 예외 처리 정리
 *          - 2023.06.02 박찬호
 *
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    /*NEMODU EXCEPTION*/
    @ExceptionHandler({
            UserException.class,
            AuthException.class,
            FriendException.class,
            ExerciseRecordException.class,
            KakaoException.class,
            CommonException.class
    })
    public ResponseEntity<ErrorResponse> handleNemoduException(BaseException e) {
        log.error("{}! code:{} | Message:{} | trace:{}", e.getClass(), e.getCode(), e.getDebugMessage(), e.fewStackTrace());

        String message = e.getMessage() != null ? e.getMessage() : e.getDebugMessage();
        return makeResponseFormat(e.getExceptionCode(), e.fewStackTrace(), message);
    }

    @ExceptionHandler(ChallengeException.class)
    public ResponseEntity<ErrorResponse> handleChallengeException(ChallengeException e) {
        log.error("{}! code:{} | Message:{} | trace:{}", e.getClass(), e.getCode(), e.getDebugMessage(), e.fewStackTrace());

        //4500 이상은 회원-챌린지 간 예외사항
        if (Integer.parseInt(e.getCode()) >= 4500) {
            log.error("UserChallenge exception: Code:{} | Message:{} | StackTrace:{} | NicknameList:{}", e.getCode(), e.getDebugMessage(), e.fewStackTrace(), e.getNicknames());
        } else {
            log.error("Challenge exception: Code:{} | Message:{} | StackTrace:{}", e.getCode(), e.getDebugMessage(), e.fewStackTrace());
        }

        String message = e.getMessage() != null ? e.getMessage() : e.getDebugMessage();
        return makeResponseFormat(e.getExceptionCode(), e.fewStackTrace(), message);
    }

    /*DEFAULT EXCEPTION*/
    @ExceptionHandler({
            SQLIntegrityConstraintViolationException.class,
            NullPointerException.class,
            WebClientException.class,
            ParseException.class,
            Exception.class
    })
    public ResponseEntity<ErrorResponse> handleDefaultException(Exception e) {
        List<String> trace = getFewTrace(e.getStackTrace());
        ExceptionCodeSet exceptionByMsg = ExceptionCodeSet.findExceptionByMsg(e.getMessage());

        if (exceptionByMsg == null) {
            log.error("{}!: Code:{} | Message:{} | StackTrace:{}", e.getClass(), ExceptionCodeSet.INTERNAL_SERVER_ERROR.getCode(), e.getMessage(), trace);
            return makeResponseFormat(ExceptionCodeSet.INTERNAL_SERVER_ERROR, trace, "오류가 발생했습니다.");
        } else {
            log.error("{}!: Code:{} | Message:{} | StackTrace:{}", e.getClass(), exceptionByMsg.getCode(), e.getMessage(), trace);
            return makeResponseFormat(exceptionByMsg, trace, "오류가 발생했습니다.");
        }
    }

    /**
     * 사전에 정의된 커스텀 예외에 대한 Response 생성
     * @param exceptionCode
     */
    private ResponseEntity<ErrorResponse> makeResponseFormat(ExceptionCodeSet exceptionCode, List<String> trace, String message) {
        return ResponseEntity.status(exceptionCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .code(exceptionCode.getCode())
                        .message(message)
                        .trace(trace)
                        .build()
                );
    }

    /**
     * 역추적을 위한 간소화된 Stack trace 계산
     */
    private List<String> getFewTrace(StackTraceElement[] trace) {
        if (trace == null) {
            log.error("Stack trace is null");
            return List.of("No stack trace");
        } else if (trace.length < 3) {
            return Arrays.stream(Arrays.copyOfRange(trace, 0, trace.length))
                    .map(StackTraceElement::toString)
                    .collect(Collectors.toList());
        } else {
            return Arrays.stream(Arrays.copyOfRange(trace, 0, 3))
                    .map(StackTraceElement::toString)
                    .collect(Collectors.toList());
        }
    }
}
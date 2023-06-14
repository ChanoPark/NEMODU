package com.dnd.ground.global.exception;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description 각 예외에 대한 공통 부분을 묶은 추상 클래스
 * @author  박찬호
 * @since   2022-12-01
 * @updated 1. 예외 상황을 사용자에게 알리기 위한 message 필드 생성
 *          - 2023.05.23 박찬호
 */
@AllArgsConstructor
public abstract class BaseExceptionAbs extends RuntimeException implements BaseException {

    private static final int STACK_TRACE_LINE_LIMIT = 3;
    private ExceptionCodeSet exceptionCode;
    private String message;

    public BaseExceptionAbs(ExceptionCodeSet exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    @Override
    public ExceptionCodeSet getExceptionCode() {
        return this.exceptionCode;
    }

    @Override
    public String getDebugMessage() {
        return this.exceptionCode.getMessage();
    }

    @Override
    public String getCode() {
        return this.exceptionCode.getCode();
    }

    @Override
    public List<String> fewStackTrace() {
        try {
            return Arrays.stream(Arrays.copyOfRange(getStackTrace(), 0, STACK_TRACE_LINE_LIMIT))
                    .map(StackTraceElement::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

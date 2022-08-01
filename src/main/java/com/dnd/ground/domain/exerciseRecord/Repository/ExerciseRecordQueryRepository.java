package com.dnd.ground.domain.exerciseRecord.Repository;

import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description 운동 기록 query 인터페이스
 * @author  박세헌
 * @since   2022-08-01
 * @updated 2022-08-01 생성 : 박세헌
 */

public interface ExerciseRecordQueryRepository {
    List<ExerciseRecord> findRecord(Long id, LocalDateTime start, LocalDateTime end);
}
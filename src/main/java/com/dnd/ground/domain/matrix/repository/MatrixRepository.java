package com.dnd.ground.domain.matrix.repository;

import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;
import com.dnd.ground.domain.matrix.Matrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @description 운동 영역 리포지토리 인터페이스
 * @author  박찬호
 * @since   2022-08-01
 * @updated 1.MatrixDto 의존성 제거
 *          -2023.03.05 박찬호
 */

public interface MatrixRepository extends JpaRepository<Matrix, Long>, MatrixRepositoryQuery {
    @Query("select m from Matrix m where m.exerciseRecord=:exerciseRecord")
    List<Matrix> findByRecord(@Param("exerciseRecord") ExerciseRecord exerciseRecord);

    @Modifying
    @Transactional
    @Query("DELETE FROM Matrix m WHERE m.exerciseRecord in :records")
    void deleteAllByRecord(@Param("records") List<ExerciseRecord> records);
}

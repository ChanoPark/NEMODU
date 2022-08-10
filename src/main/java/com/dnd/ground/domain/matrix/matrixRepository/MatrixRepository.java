package com.dnd.ground.domain.matrix.matrixRepository;

import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;
import com.dnd.ground.domain.matrix.Matrix;
import com.dnd.ground.domain.matrix.dto.MatrixDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

/**
 * @description 운동 영역 리포지토리 인터페이스
 * @author  박세헌
 * @since   2022-08-01
 * @updated 2022-08-10 / 매트릭스 정보 조회 함수 구현 : 박세헌
 */

public interface MatrixRepository extends JpaRepository<Matrix, Long> {

    // 운동기록들을 통해 매트릭스 정보 조회(중복o)
    @Query("select new com.dnd.ground.domain.matrix.dto.MatrixDto(m.latitude, m.longitude) " +
            "from Matrix m join m.exerciseRecord e where e in :exerciseRecords")
    List<MatrixDto> findMatrixByExercise(List<ExerciseRecord> exerciseRecords);

    // 운동기록들을 통해 매트릭스 정보 조회(중복x)
    @Query("select new com.dnd.ground.domain.matrix.dto.MatrixDto(m.latitude, m.longitude) " +
            "from Matrix m join m.exerciseRecord e where e in :exerciseRecords")
    Set<MatrixDto> findMatrixSetByExercise(List<ExerciseRecord> exerciseRecords);

}

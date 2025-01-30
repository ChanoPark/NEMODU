package com.dnd.ground.domain.challenge.realtime.cache.model;

import com.dnd.ground.global.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RTChallengeRecordModel {
    private Double latitude;
    private Double longitude;
    private LocalDateTime created; // 클라랑 협의가 안되면 데이터 받을 때 추가해서 넣는것으로 ..

    @Override
    public String toString() {
        return JsonUtil.writeValueAsString(this);
    }
}

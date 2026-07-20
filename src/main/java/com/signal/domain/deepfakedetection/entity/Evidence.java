package com.signal.domain.deepfakedetection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evidence {

    private String type;

    @Column(length = 500)
    private String description;

    @Embedded
    private Region region;

    /**
     * 영상 입력에서 근거가 발견된 프레임 번호. 이미지 입력이면 null이다.
     */
    private Integer frame;

    @Builder
    public Evidence(String type, String description, Region region, Integer frame) {
        this.type = type;
        this.description = description;
        this.region = region;
        this.frame = frame;
    }
}

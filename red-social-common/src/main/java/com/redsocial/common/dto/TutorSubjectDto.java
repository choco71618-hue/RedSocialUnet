package com.redsocial.common.dto;

import java.math.BigDecimal;

public record TutorSubjectDto(
        Long id,
        Long subjectId,
        String subjectName,
        BigDecimal pricePerHour
) {
}

package com.sysm.devsync.controller.dto.request;

import com.sysm.devsync.domain.enums.QuestionStatus;

public record AnswerCreateUpdate(
        String title,
        String content) {
}

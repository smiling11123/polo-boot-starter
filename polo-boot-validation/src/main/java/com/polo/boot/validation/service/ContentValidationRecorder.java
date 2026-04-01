package com.polo.boot.validation.service;

import com.polo.boot.validation.model.ContentValidationRecord;

public interface ContentValidationRecorder {
    void record(ContentValidationRecord record);
}

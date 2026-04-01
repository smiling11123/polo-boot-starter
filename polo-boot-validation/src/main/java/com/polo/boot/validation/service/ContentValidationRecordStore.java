package com.polo.boot.validation.service;

import com.polo.boot.validation.model.ContentValidationRecord;

public interface ContentValidationRecordStore {
    void save(ContentValidationRecord record);
}

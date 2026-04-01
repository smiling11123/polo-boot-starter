package com.polo.boot.validation.service;

import com.polo.boot.validation.model.ContentValidationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

@Slf4j
public class DefaultContentValidationRecorder implements ContentValidationRecorder {
    private final ContentValidationRecordStore recordStore;

    public DefaultContentValidationRecorder(ObjectProvider<ContentValidationRecordStore> recordStoreProvider) {
        this.recordStore = recordStoreProvider.getIfAvailable();
    }

    @Override
    public void record(ContentValidationRecord record) {
        if (record == null) {
            return;
        }

        log.warn("[内容校验记录] detectorType={}, strategy={}, message={}, contentPreview={}, matchedDetail={}",
                record.getDetectorType(),
                record.getStrategy(),
                record.getMessage(),
                record.getContentPreview(),
                record.getMatchedDetail());

        if (recordStore == null) {
            return;
        }

        try {
            recordStore.save(record);
        } catch (Exception ex) {
            log.error("保存内容校验记录失败", ex);
        }
    }
}

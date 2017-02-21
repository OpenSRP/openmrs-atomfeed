package org.openmrs.module.atomfeed.api;

import java.util.List;

public interface AllEventRecordsExtra {
	
    void add(EventRecordExtra eventRecordextra);
    
    EventRecordExtra get(String uuid);
    
    EventRecordExtra getByEventRecord(String eventUuid);

    List<EventRecordExtra> getAll();

    void delete(String uuid);
}
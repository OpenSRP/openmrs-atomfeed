package org.openmrs.module.atomfeed.api;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "event_records_extras")
public class EventRecordExtra {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid")
    private String uuid;
    
    @Column(name = "event_uuid")
    private String eventUuid;

    @Column(name = "timestamp", insertable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timeStamp;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;

    public EventRecordExtra() { }

    public EventRecordExtra(String uuid, String eventUuid, String name, String value) {
        this.uuid = uuid;
        this.eventUuid = eventUuid;
        this.name = name;
        this.value = value;
    }

    public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getEventUuid() {
		return eventUuid;
	}

	public void setEventUuid(String eventUuid) {
		this.eventUuid = eventUuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Date getTimeStamp() {
        return timeStamp != null ? timeStamp : new Date();
    }

    @Override
    public String toString() {
        return "EventRecordExtra [id=" + id + ", uuid=" + uuid + ", eventUuid=" + eventUuid
                + ", timeStamp=" + timeStamp + ", name=" + name + ", value="+ value + "]";
    }
}
package org.openmrs.module.atomfeed.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.ict4h.atomfeed.Configuration;
import org.ict4h.atomfeed.jdbc.JdbcConnectionProvider;
import org.ict4h.atomfeed.jdbc.JdbcResultSetMapper;
import org.ict4h.atomfeed.jdbc.JdbcUtils;
import org.ict4h.atomfeed.server.exceptions.AtomFeedRuntimeException;

public class AllEventRecordsExtraJdbcImpl implements AllEventRecordsExtra {

    private JdbcConnectionProvider provider;

    public AllEventRecordsExtraJdbcImpl(JdbcConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void add(EventRecordExtra eventRecordExtra) {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = provider.getConnection();
            String insertSql = String.format("insert into %s (uuid, event_uuid, name, value, timestamp) values (?, ?, ?, ?, ?)",
                    JdbcUtils.getTableName(Configuration.getInstance().getSchema(), "event_records_extras"));
            stmt = connection.prepareStatement(insertSql);
            stmt.setString(1, eventRecordExtra.getUuid());
            stmt.setString(2, eventRecordExtra.getEventUuid());
            stmt.setString(3, eventRecordExtra.getName());
            stmt.setString(4, eventRecordExtra.getValue());
            stmt.setTimestamp(5, new Timestamp(eventRecordExtra.getTimeStamp().getTime()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AtomFeedRuntimeException(e);
        } finally {
            close(stmt);
        }
    }

    @Override
    public EventRecordExtra get(String uuid) {
        Connection connection;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            connection = provider.getConnection();
            String sql = String.format("select id, uuid, event_uuid, name, value, timestamp from %s where uuid = ?",
                    JdbcUtils.getTableName(Configuration.getInstance().getSchema(), "event_records_extras"));
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, uuid);
            rs = stmt.executeQuery();
            List<EventRecordExtra> events = mapEventRecordsExtra(rs);
            if ((events != null) && !events.isEmpty()) {
                return events.get(0);
            }
        } catch (SQLException e) {
            throw new AtomFeedRuntimeException(e);
        } finally {
            closeAll(stmt, rs);
        }
        return null;
    }
    
    @Override
    public EventRecordExtra getByEventRecord(String eventUuid) {
        Connection connection;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            connection = provider.getConnection();
            String sql = String.format("select id, uuid, event_uuid, name, value, timestamp from %s where event_uuid = ?",
                    JdbcUtils.getTableName(Configuration.getInstance().getSchema(), "event_records_extras"));
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, eventUuid);
            rs = stmt.executeQuery();
            List<EventRecordExtra> events = mapEventRecordsExtra(rs);
            if ((events != null) && !events.isEmpty()) {
                return events.get(0);
            }
        } catch (SQLException e) {
            throw new AtomFeedRuntimeException(e);
        } finally {
            closeAll(stmt, rs);
        }
        return null;
    }

    private void closeAll(PreparedStatement stmt, ResultSet rs) {
        close(rs);
        close(stmt);
    }

    private void close(AutoCloseable rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception e) {
            throw new AtomFeedRuntimeException(e);
        }
    }

    @Override
    public List<EventRecordExtra> getAll() {
        Connection connection;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            connection = provider.getConnection();
            String sql = String.format("select id, uuid, event_uuid, name, value, timestamp from %s",
                    JdbcUtils.getTableName(Configuration.getInstance().getSchema(), "event_records_extras"));
            stmt = connection.prepareStatement(sql);
            rs = stmt.executeQuery();
            return mapEventRecordsExtra(rs);
        } catch (SQLException e) {
            throw new AtomFeedRuntimeException(e);
        } finally {
            closeAll(stmt, rs);
        }
    }

    @Override
    public void delete(String uuid) {
        Connection connection;
        PreparedStatement stmt = null;
        try {
            connection = provider.getConnection();
            String sql = String.format("delete from %s where uuid = ?",
                    JdbcUtils.getTableName(Configuration.getInstance().getSchema(), "event_records_extras"));
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AtomFeedRuntimeException(e);
        } finally {
            close(stmt);
        }
    }

    private List<EventRecordExtra> mapEventRecordsExtra(ResultSet results) {
        return new JdbcResultSetMapper<EventRecordExtra>().mapResultSetToObject(results, EventRecordExtra.class);
    }
}
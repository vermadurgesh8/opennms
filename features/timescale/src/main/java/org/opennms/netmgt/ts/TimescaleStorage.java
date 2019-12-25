/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.ts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.joda.time.Duration;
import org.opennms.netmgt.timeseries.api.TimeSeriesStorage;
import org.opennms.netmgt.timeseries.api.domain.Metric;
import org.opennms.netmgt.timeseries.api.domain.Sample;
import org.opennms.netmgt.timeseries.api.domain.StorageException;
import org.opennms.netmgt.timeseries.api.domain.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.swrve.ratelimitedlogger.RateLimitedLog;

@Service
public class TimescaleStorage implements TimeSeriesStorage {

    private static final Logger LOG = LoggerFactory.getLogger(TimescaleStorage.class);

    private static final RateLimitedLog RATE_LIMITED_LOGGER = RateLimitedLog
            .withRateLimit(LOG)
            .maxRate(5).every(Duration.standardSeconds(30))
            .build();

    private DataSource dataSource;

    private Connection connection;

    private int maxBatchSize = 100; // TODO Patrick: do we need to make value configurable?

    @Autowired
    public TimescaleStorage(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void store(List<Sample> entries) throws StorageException {
        String sql = "INSERT INTO timescale_time_series(time, key, value)  values (?, ?, ?)";

        try {
            if (this.connection == null) {
                this.connection = this.dataSource.getConnection();
            }

            PreparedStatement ps = connection.prepareStatement(sql);
            // Partition the samples into collections smaller then max_batch_size
            for (List<Sample> batch : Lists.partition(entries, maxBatchSize)) {
                try {
                    LOG.debug("Inserting {} samples", batch.size());
                    for (Sample sample : batch) {
                        ps.setTimestamp(1, new Timestamp(sample.getTime().toEpochMilli()));
                        ps.setString(2, sample.getMetric().getKey());
                        ps.setDouble(3, sample.getValue());
                        ps.addBatch();
                        saveTags(sample.getMetric(), Metric.TagType.intrinsic, sample.getMetric().getTags());
                        saveTags(sample.getMetric(), Metric.TagType.meta, sample.getMetric().getMetaTags());
                    }
                    ps.executeBatch();

                    if (LOG.isDebugEnabled()) {
                        String keys = batch.stream()
                                .map(s -> s.getMetric().getKey())
                                .distinct()
                                .collect(Collectors.joining(", "));
                        LOG.debug("Successfully inserted samples for resources with ids {}", keys);
                    }
                } catch (Throwable t) {
                    RATE_LIMITED_LOGGER.error("An error occurred while inserting samples. Some sample may be lost.", t);
                }
            }
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    private void saveTags(final Metric metric, final Metric.TagType tagType, final Collection<Tag> tags) throws SQLException {
        final String sql = "INSERT INTO timescale_tag(fk_timescale_metric, key, value, type)  values (?, ?, ?, ?) ON CONFLICT (fk_timescale_metric, key, value, type) DO NOTHING;";
        PreparedStatement ps = connection.prepareStatement(sql);
        for (Tag tag : tags) {
            ps.setString(1, metric.getKey());
            ps.setString(2, tag.getKey());
            ps.setString(3, tag.getValue());
            ps.setString(4, tagType.name());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    public List<Metric> getMetrics(Collection<Tag> tags) throws StorageException {
        Objects.requireNonNull(tags, "tags collection can not be null");
        try {
            // TODO: Patrick: do db stuff properly

            String sql = createMetricsSQL(tags);
            if (connection == null) {
                this.connection = this.dataSource.getConnection();
            }

            // Get all relevant metricKeys
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            Set<String> metricKeys = new HashSet<>();
            while (rs.next()) {
                metricKeys.add(rs.getString("fk_timescale_metric"));
            }
            rs.close();

            // Load the actual metrics
            List<Metric> metrics = new ArrayList<>();
            sql = "SELECT * FROM timescale_tag WHERE fk_timescale_metric=?";
            for(String metricKey : metricKeys) {
                ps = connection.prepareStatement(sql);
                ps.setString(1, metricKey);
                rs = ps.executeQuery();
                Metric.MetricBuilder metric = Metric.builder();
                while (rs.next()) {
                    Tag tag = new Tag(rs.getString("key"), rs.getString("value"));
                    Metric.TagType type = Metric.TagType.valueOf(rs.getString("type"));
                    if ((type == Metric.TagType.intrinsic)) {
                        metric.tag(tag);
                    } else {
                        metric.metaTag(tag);
                    }
                }
                metrics.add(metric.build());
                rs.close();
            }

            return metrics;
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    String createMetricsSQL(Collection<Tag> tags) {
        Objects.requireNonNull(tags, "tags collection can not be null");
        StringBuilder b = new StringBuilder("select distinct fk_timescale_metric from timescale_tag");
        if (!tags.isEmpty()) {
            b.append(" where 1=2");
            for (Tag tag : tags) {
                b.append(" or");
                b.append(" (key").append(handleNull(tag.getKey())).append(" AND ");
                b.append("value").append(handleNull(tag.getValue())).append(")");
            }
        }
        b.append(";");
        return b.toString();
    }

    private String handleNull(String input) {
        if (input == null) {
            return " is null";
        }
        return "='" + input + "'";
    }

    @Override
    public List<Sample> getTimeseries(Metric metric, Instant start, Instant end, java.time.Duration step) {

        // TODO: Patrick: do db stuff properly
        ArrayList<Sample> samples;
        try {
            long stepInSeconds = step.getSeconds();
            String resourceId = "response:127.0.0.1:response-time"; // TODO Patrick: deduct from sources
            String sql = "SELECT time_bucket_gapfill('" + stepInSeconds + " Seconds', time) AS step, min(value), avg(value), max(value) FROM timescale_time_series where " +
                    "key=? AND time > ? AND time < ? GROUP BY step ORDER BY step ASC";
//            if(maxrows>0) {
//                sql = sql + " LIMIT " + maxrows;
//            }
            if (connection == null) {
                this.connection = this.dataSource.getConnection();

            }
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, resourceId);
            statement.setTimestamp(2, new java.sql.Timestamp(start.toEpochMilli()));
            statement.setTimestamp(3, new java.sql.Timestamp(end.toEpochMilli()));
            ResultSet rs = statement.executeQuery();

            samples = new ArrayList<>();
            while (rs.next()) {
                long timestamp = rs.getTimestamp("step").getTime();
                samples.add(new Sample(metric, Instant.ofEpochMilli(timestamp), rs.getDouble("avg")));
            }

            rs.close();
        } catch (SQLException e) {
            LOG.error("Could not retrieve FetchResults", e);
            throw new RuntimeException(e);
        }
        return samples;
    }
}

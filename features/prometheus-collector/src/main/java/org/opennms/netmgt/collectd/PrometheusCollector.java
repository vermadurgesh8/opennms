/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.collectd;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hawkular.agent.prometheus.PrometheusScraper;
import org.hawkular.agent.prometheus.text.TextSample;
import org.hawkular.agent.prometheus.types.Counter;
import org.hawkular.agent.prometheus.types.Gauge;
import org.hawkular.agent.prometheus.types.Histogram;
import org.hawkular.agent.prometheus.types.Metric;
import org.hawkular.agent.prometheus.types.MetricVisitor;
import org.hawkular.agent.prometheus.types.Summary;
import org.hawkular.agent.prometheus.walkers.MetricCollectingWalker;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.utils.ParameterMap;
import org.opennms.netmgt.collection.api.AbstractRemoteServiceCollector;
import org.opennms.netmgt.collection.api.AttributeType;
import org.opennms.netmgt.collection.api.CollectionAgent;
import org.opennms.netmgt.collection.api.CollectionException;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.collection.api.ServiceParameters.ParameterName;
import org.opennms.netmgt.collection.support.builder.CollectionSetBuilder;
import org.opennms.netmgt.collection.support.builder.DeferredGenericTypeResource;
import org.opennms.netmgt.collection.support.builder.NodeLevelResource;
import org.opennms.netmgt.collection.support.builder.Resource;
import org.opennms.netmgt.config.prometheus.Collection;
import org.opennms.netmgt.config.prometheus.Group;
import org.opennms.netmgt.config.prometheus.NumericAttribute;
import org.opennms.netmgt.config.prometheus.PrometheusDatacollectionConfig;
import org.opennms.netmgt.config.prometheus.StringAttribute;
import org.opennms.netmgt.dao.PrometheusDataCollectionConfigDao;
import org.opennms.netmgt.rrd.RrdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.google.common.base.Strings;

/*
 * 
 * 
       <group name="node-exporter-filesystems" resource-type="nodeExporterFilesytem"
         filter="name matches 'node_filesystem_.*'"
         group-by="labels[mountpoint]">
         <numeric-attribute alias-exp="name.substring('node_filesystem_'.length())"/>
         <string-attribute alias="fstype" value-exp="labels[fstype]"/>
         <string-attribute alias="device" value-exp="labels[device]"/>
       </group>


node_filesystem_avail{device="/dev/mapper/ssd-root",fstype="ext4",mountpoint="/rootfs"} 2.10503274496e+11

 *     <group name="node-exporter-cpu" resource-type="nodeExporterCPU"
         filter="#name matches 'node_cpu'"
         group-by="#labels[cpu]"
         with-attributes="#labels[mode]" />
         
         node_cpu{cpu="cpu0",mode="guest"} 0
    node_cpu{cpu="cpu0",mode="idle"} 16594.88
    node_cpu{cpu="cpu0",mode="iowait"} 163.99
    node_cpu{cpu="cpu0",mode="irq"} 62.55
    node_cpu{cpu="cpu0",mode="nice"} 1134.72
    node_cpu{cpu="cpu0",mode="softirq"} 58.2
    node_cpu{cpu="cpu0",mode="steal"} 0
    node_cpu{cpu="cpu0",mode="system"} 316.98
    node_cpu{cpu="cpu0",mode="user"} 1638.27
 */

/**
 * 
 * See https://github.com/hawkular/hawkular-agent/tree/0.23.0.Final.
 * (Removed in 0.24.0)
 * 
 * @author jwhite
 */
public class PrometheusCollector extends AbstractRemoteServiceCollector {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusCollector.class);

    private static final String PROMETHEUS_COLLECTION_KEY = "prometheusCollection";

    private static final Map<String, Class<?>> TYPE_MAP = Collections.unmodifiableMap(Stream.of(
            new SimpleEntry<>(PROMETHEUS_COLLECTION_KEY, Collection.class))
            .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

    private PrometheusDataCollectionConfigDao m_prometheusCollectionDao;

    public PrometheusCollector() {
        super(TYPE_MAP);
    }

    @Override
    public void initialize() {
        // Retrieve the configuration DAOs
        if (m_prometheusCollectionDao == null) {
            m_prometheusCollectionDao = BeanUtils.getBean("daoContext", "prometheusDataCollectionConfigDao", PrometheusDataCollectionConfigDao.class);
        }
    }
 
    @Override
    public Map<String, Object> getRuntimeAttributes(CollectionAgent agent, Map<String, Object> parameters) {
        final Map<String, Object> runtimeAttributes = new HashMap<>();

        // Retrieve the name of the JMX data collector
        final String collectionName = ParameterMap.getKeyedString(parameters, ParameterName.COLLECTION.toString(), null);
        final Collection collection = m_prometheusCollectionDao.getCollectionByName(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException(String.format("PrometheusCollector: No collection found with name '%s'.", collectionName));
        }
        runtimeAttributes.put(PROMETHEUS_COLLECTION_KEY, collection);

        return runtimeAttributes;
    }

    @Override
    public CollectionSet collect(CollectionAgent agent, Map<String, Object> map) throws CollectionException {
        final Collection collection = (Collection)map.get(PROMETHEUS_COLLECTION_KEY);
        final String url = ParameterMap.getKeyedString(map, "url", null);
        if (Strings.isNullOrEmpty(url)) {
            throw new IllegalArgumentException("url parameter is required.");
        }

        final URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException e) {
            throw new CollectionException("Invalid URL: " + url, e);
        }

        final MetricCollectingWalker walker = new MetricCollectingWalker();
        final PrometheusScraper scraper = new PrometheusScraper(parsedUrl);
        try {
            scraper.scrape(walker);
        } catch (IOException e) {
            throw new CollectionException("Failed to scrape metrics for: " + url, e);
        }

        return toCollectionSet(agent, collection, walker.getMetrics());
    }

    protected static CollectionSet toCollectionSet(CollectionAgent agent, Collection collection, List<Metric> metrics) {
        final CollectionSetBuilder builder = new CollectionSetBuilder(agent);

        for (Group group : collection.getGroup()) {
            // First, we find the metrics that belong to this group
            final List<Metric> metricsForGroup = filterMetrics(group.getFilterExp(), metrics);
            if (metricsForGroup.isEmpty()) {
                // Don't bother continuing if we have no metric
                LOG.debug("No metrics found in group named '%s' on agent %s.", group.getName(), agent);
                continue;
            }

            // Next, we group the metrics by instance
            final Map<String, List<Metric>> metricsByInstance = groupMetrics(group, metricsForGroup);

            // Build the resource mapper
            final NodeLevelResource nodeLevelResource = new NodeLevelResource(agent.getNodeId());
            Function<String, Resource> resourceMapper = (instance) -> nodeLevelResource;
            if (!"node".equalsIgnoreCase(group.getResourceType())) {
                resourceMapper = (instance) -> {
                    // JW: FIXME: TODO - This belongs elsewhere
                    final String sanitizedInstance = instance.replaceAll("/", "_").replaceAll("\\\\", "_");
                    return new DeferredGenericTypeResource(nodeLevelResource, group.getResourceType(), sanitizedInstance);
                };
            }

            // Process the metrics instance, by instance
            for (Entry<String, List<Metric>> entry : metricsByInstance.entrySet()) {
                final Resource resource = resourceMapper.apply(entry.getKey());
                
                // First, process the numeric attributes
                for (NumericAttribute attribute : group.getNumericAttribute()) {
                    // Filter
                    final List<Metric> metricsForAttribute = filterMetrics(attribute.getFilterExp(), entry.getValue());

                    ExpressionParser parser = new SpelExpressionParser();
                    Expression exp = parser.parseExpression(attribute.getAliasExp());
                    Function<Metric, String> attributeNameMapper = (metric) -> {
                        StandardEvaluationContext context = new StandardEvaluationContext(metric);
                        return exp.getValue(context, String.class);
                    };

                    for (Metric metric : metricsForAttribute) {
                        final String attributeName = attributeNameMapper.apply(metric);
                        if (attributeName == null) {
                            LOG.info("Skipping metric with null attribute name: {}", metric);
                            continue;
                        }

                        metric.visit(new MetricVisitor() {
                            @Override
                            public void visitCounter(Counter counter) {
                                builder.withNumericAttribute(resource, group.getName(), attributeName, counter.getValue(), AttributeType.COUNTER);
                            }

                            @Override
                            public void visitGauge(Gauge gauge) {
                                builder.withNumericAttribute(resource, group.getName(), attributeName, gauge.getValue(), AttributeType.GAUGE);
                            }

                            @Override
                            public void visitHistogram(Histogram histogram) {
                                // pass
                            }

                            @Override
                            public void visitSummary(Summary summary) {
                                // pass
                            }

                            @Override
                            public void visitTextSample(TextSample textSample) {
                                // pass
                            }
                        });
                    }
                }

                // And next, the process the string attributes
                for (StringAttribute attribute : group.getStringAttribute()) {
                    ExpressionParser parser = new SpelExpressionParser();
                    Expression stringAttributeValueExp = parser.parseExpression(attribute.getValueExp());
                    for (Metric metric : entry.getValue()) {
                        StandardEvaluationContext context = new StandardEvaluationContext(metric);
                        String stringValue = stringAttributeValueExp.getValue(context, String.class);
                        if (stringValue != null) {
                            builder.withStringAttribute(resource, group.getName(), attribute.getAlias(), stringValue);
                            // Only process the first match
                            break;
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    private static List<Metric> filterMetrics(String filterExpression, List<Metric> metrics) {
        if (filterExpression == null) {
            return metrics;
        }

        final ExpressionParser parser = new SpelExpressionParser();
        final Expression exp = parser.parseExpression(filterExpression);
        final List<Metric> filteredMetrics = new ArrayList<>();
        for (Metric metric : metrics) {
            StandardEvaluationContext context = new StandardEvaluationContext(metric);

            boolean passed = false;
            try {
                passed = exp.getValue(context, Boolean.class);
            } catch (Exception e) {
                LOG.warn("Failed to evaluate expression '{}'. The metric will not be included.",
                        filterExpression, e);
            }
            LOG.trace("Rule '{}' on {} passed? {}", filterExpression, metric, passed);

            if (passed) {
                filteredMetrics.add(metric);
            }
        }
        return filteredMetrics;
    }

    /**
     * Groups the metrics by instance using the given group-by expression.
     * If no expression is set, all the metrics belong to a single group with instance name 'node'
     */
    private static Map<String, List<Metric>> groupMetrics(Group group, List<Metric> metrics) {
        final Map<String, List<Metric>> metricsByInstance = new LinkedHashMap<>();
        if (group.getGroupByExp() == null) {
            metricsByInstance.put("node", metrics);
            return metricsByInstance;
        }

        final ExpressionParser parser = new SpelExpressionParser();
        final Expression exp = parser.parseExpression(group.getGroupByExp());
        for (Metric metric : metrics) {
            final StandardEvaluationContext context = new StandardEvaluationContext(metric);
            try {
                final String instance = exp.getValue(context, String.class);
                LOG.trace("Rule '{}' on {} returned instance: {}", group.getGroupByExp(), metric, instance);

                // Insert or append
                List<Metric> metricsInInstance = metricsByInstance.get(instance);
                if (metricsInInstance == null) {
                    metricsInInstance = new LinkedList<>();
                    metricsByInstance.put(instance, metricsInInstance);
                }
                metricsInInstance.add(metric);
            } catch (Exception e) {
                LOG.warn("Failed to evaluate expression '{}' in the group named '%s'. The metric will not be included.",
                        group.getGroupByExp(), group.getName(), e);
            }
        }
        return metricsByInstance;
    }

    @Override
    public RrdRepository getRrdRepository(String collectionName) {
        LOG.debug("getRrdRepository({})", collectionName);

        PrometheusDatacollectionConfig config = m_prometheusCollectionDao.getConfig();
        Collection collection = m_prometheusCollectionDao.getCollectionByName(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("No configuration found for collection with name: " + collectionName);
        }

        RrdRepository rrdRepository = new RrdRepository();
        rrdRepository.setStep(collection.getRrd().getStep());
        rrdRepository.setHeartBeat(2 * rrdRepository.getStep());
        rrdRepository.setRraList(collection.getRrd().getRra());
        rrdRepository.setRrdBaseDir(new File(config.getRrdRepository()));

        LOG.debug("Using RRD repository: {} for collection: {}", rrdRepository, collectionName);
        return rrdRepository;
    }

    public PrometheusDataCollectionConfigDao getPrometheusCollectionDao() {
        return m_prometheusCollectionDao;
    }

    public void setPrometheusCollectionDao(PrometheusDataCollectionConfigDao prometheusCollectionDao) {
        m_prometheusCollectionDao = prometheusCollectionDao;
    }

}

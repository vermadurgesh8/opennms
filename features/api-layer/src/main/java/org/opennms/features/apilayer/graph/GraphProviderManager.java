/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020-2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.features.apilayer.graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.opennms.features.apilayer.utils.InterfaceMapper;
import org.opennms.integration.api.v1.graph.Graph;
import org.opennms.integration.api.v1.graph.GraphInfo;
import org.opennms.integration.api.v1.graph.GraphProvider;
import org.opennms.netmgt.graph.api.ImmutableGraph;
import org.opennms.netmgt.graph.api.generic.GenericEdge;
import org.opennms.netmgt.graph.api.generic.GenericGraph;
import org.opennms.netmgt.graph.api.generic.GenericVertex;
import org.osgi.framework.BundleContext;

import com.google.common.collect.ImmutableMap;

public class GraphProviderManager extends InterfaceMapper<GraphProvider, org.opennms.netmgt.graph.api.service.GraphProvider> {

    public GraphProviderManager(BundleContext bundleContext) {
        super(org.opennms.netmgt.graph.api.service.GraphProvider.class, bundleContext);
    }

    @Override
    public org.opennms.netmgt.graph.api.service.GraphProvider map(GraphProvider extension) {
        return new org.opennms.netmgt.graph.api.service.GraphProvider() {

            @Override
            public ImmutableGraph<?, ?> loadGraph() {
                final Graph extensionGraph = extension.loadGraph();
                Objects.requireNonNull(extensionGraph, "extension.loadGraph() must return not null value");
                final List<GenericVertex> vertices = extensionGraph.getVertices().stream()
                        .map(v -> GenericVertex.builder().properties(v.getProperties()).build())
                        .collect(Collectors.toList());
                final List<GenericEdge> edges = extensionGraph.getEdges().stream()
                        .map(e -> GenericEdge.builder()
                                .properties(e.getProperties())
                                .source(e.getSource().getNamespace(), e.getSource().getId())
                                .target(e.getTarget().getNamespace(), e.getTarget().getId())
                                .build())
                        .collect(Collectors.toList());
                final GenericGraph convertedGraph = GenericGraph.builder().properties(extensionGraph.getProperties())
                    .addVertices(vertices)
                    .addEdges(edges)
                    .build();
                return convertedGraph;
            }

            @Override
            public org.opennms.netmgt.graph.api.info.GraphInfo<?> getGraphInfo() {
                final GraphInfo extensionGraphInfo = extension.getGraphInfo();
                Objects.requireNonNull(extensionGraphInfo, "extension.getGraphInfo() must return not null value");
                return new org.opennms.netmgt.graph.api.info.GraphInfo<GenericVertex>() {

                    @Override
                    public String getNamespace() {
                        return extensionGraphInfo.getNamespace();
                    }

                    @Override
                    public String getDescription() {
                        return extensionGraphInfo.getDescription();
                    }

                    @Override
                    public String getLabel() {
                        return extensionGraphInfo.getLabel();
                    }

                    @Override
                    public Class<GenericVertex> getVertexType() {
                        return GenericVertex.class;
                    }
                };
            }
        };
    }

    @Override
    public Map<String, Object> getServiceProperties(GraphProvider extension) {
        return ImmutableMap.<String, Object>builder()
                .put("expose-to-topology", Boolean.toString(extension.isTopology()))
                .build();
    }
}

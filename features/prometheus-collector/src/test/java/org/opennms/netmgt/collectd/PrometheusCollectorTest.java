package org.opennms.netmgt.collectd;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.core.collection.test.CollectionSetUtils;
import org.opennms.netmgt.collection.adapters.ResourceTypeMapper;
import org.opennms.netmgt.collection.api.CollectionException;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.collection.support.IndexStorageStrategy;
import org.opennms.netmgt.collection.support.PersistAllSelectorStrategy;
import org.opennms.netmgt.dao.PrometheusDataCollectionConfigDao;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.snmp.InetAddrUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;

import org.opennms.netmgt.config.datacollection.PersistenceSelectorStrategy;
import org.opennms.netmgt.config.datacollection.ResourceType;
import org.opennms.netmgt.config.datacollection.StorageStrategy;
import org.opennms.netmgt.config.prometheus.Collection;
import org.opennms.netmgt.config.prometheus.Group;
import org.opennms.netmgt.config.prometheus.NumericAttribute;
import org.opennms.netmgt.config.prometheus.StringAttribute;

public class PrometheusCollectorTest {

    private PrometheusCollector collector = new PrometheusCollector();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
            .withRootDirectory(Paths.get("src", "test", "resources").toString())
            .dynamicPort());

    @Before
    public void setUp() {
        stubFor(post(urlEqualTo("/metrics"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "Content-Type: text/plain; version=0.0.4")
                        .withBodyFile("linux.metrics")));
    }

    @Test
    public void canGatherLoadAverageMetrics() {
        // Define our configuration
        Collection collection = new Collection();
        Group nodeExporterLoadAverage = new Group();
        nodeExporterLoadAverage.setName("node-exporter-loadavg");
        nodeExporterLoadAverage.setFilterExp("name matches 'node_load.*'");
        nodeExporterLoadAverage.setResourceType("node");

        NumericAttribute attribute = new NumericAttribute();
        attribute.setAliasExp("name.substring('node_'.length())");
        nodeExporterLoadAverage.getNumericAttribute().add(attribute);

        collection.getGroup().add(nodeExporterLoadAverage);

        // Collect!
        CollectionSet collectionSet = collect(collection);

        // Verify
        List<String> collectionSetKeys = CollectionSetUtils.flatten(collectionSet);
        assertEquals(Arrays.asList("0/node-exporter-loadavg/load1[null,0.58]",
                "0/node-exporter-loadavg/load15[null,0.64]",
                "0/node-exporter-loadavg/load5[null,0.36]"), collectionSetKeys);
    }

    @Test
    public void canGatherFileSystemMetrics() {
        // Define our configuration
        Collection collection = new Collection();
        Group nodeExporterCpu = new Group();
        nodeExporterCpu.setName("node-exporter-filesystems");
        nodeExporterCpu.setFilterExp("name matches 'node_filesystem_.*' and labels[mountpoint] matches '.*home'");
        nodeExporterCpu.setGroupByExp("labels[mountpoint]");
        nodeExporterCpu.setResourceType("nodeExporterFilesytem");

        NumericAttribute attribute = new NumericAttribute();
        attribute.setAliasExp("name.substring('node_filesystem_'.length())");
        nodeExporterCpu.getNumericAttribute().add(attribute);

        StringAttribute fsTypeStringAttribute = new StringAttribute();
        fsTypeStringAttribute.setAlias("fstype");
        fsTypeStringAttribute.setValueExp("labels[fstype]");
        nodeExporterCpu.getStringAttribute().add(fsTypeStringAttribute);

        StringAttribute deviceStringAttribute = new StringAttribute();
        deviceStringAttribute.setAlias("device");
        deviceStringAttribute.setValueExp("labels[device]");
        nodeExporterCpu.getStringAttribute().add(deviceStringAttribute);

        collection.getGroup().add(nodeExporterCpu);

        // Define the resource type
        ResourceType resourceType = createStandardResourceType("nodeExporterFilesytem");
        ResourceTypeMapper.getInstance().setResourceTypeMapper((rt) -> resourceType);

        // Collect!
        CollectionSet collectionSet = collect(collection);

        // Verify
        List<String> collectionSetKeys = CollectionSetUtils.flatten(collectionSet);
        PrometheusCollectorTest.printAsArrayList(collectionSetKeys);
        assertEquals(Arrays.asList("0/nodeExporterFilesytem/_rootfs_home/node-exporter-filesystems/avail[null,9.4895054848E10]",
                "0/nodeExporterFilesytem/_rootfs_home/node-exporter-filesystems/files[null,2.4420352E7]",
                "0/nodeExporterFilesytem/_rootfs_home/node-exporter-filesystems/files_free[null,2.189922E7]",
                "0/nodeExporterFilesytem/_rootfs_home/node-exporter-filesystems/free[null,1.14911793152E11]",
                "0/nodeExporterFilesytem/_rootfs_home/node-exporter-filesystems/readonly[null,0.0]",
                "0/nodeExporterFilesytem/_rootfs_home/node-exporter-filesystems/size[null,3.93585647616E11]",
                "0/nodeExporterFilesytem/_rootfs_home/node-exporter-filesystems/fstype[ext4,null]",
                "0/nodeExporterFilesytem/_rootfs_home/node-exporter-filesystems/device[/dev/mapper/optical-home,null]"), collectionSetKeys);
    }

    @Test
    public void canGatherDiskMetrics() {
        // Define our configuration
        Collection collection = new Collection();
        Group nodeExporterDisks = new Group();
        nodeExporterDisks.setName("node-exporter-disks");
        nodeExporterDisks.setFilterExp("name matches 'node_disk_.*'");
        nodeExporterDisks.setGroupByExp("labels[device]");
        nodeExporterDisks.setResourceType("nodeExporterDisk");

        NumericAttribute attribute = new NumericAttribute();
        attribute.setAliasExp("name.substring('node_disk_'.length())");
        nodeExporterDisks.getNumericAttribute().add(attribute);

        collection.getGroup().add(nodeExporterDisks);

        // Define the resource type
        ResourceType resourceType = createStandardResourceType("nodeExporterDisk");
        ResourceTypeMapper.getInstance().setResourceTypeMapper((rt) -> resourceType);
        
        // Collect!
        CollectionSet collectionSet = collect(collection);

        // Verify
        List<String> collectionSetKeys = CollectionSetUtils.flatten(collectionSet);
        assertEquals(Arrays.asList("0/nodeExporterDisk/dm-0/node-exporter-disks/bytes_read[null,2539520.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/bytes_written[null,1318912.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/io_now[null,0.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/io_time_ms[null,604.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/io_time_weighted[null,755.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/read_time_ms[null,591.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/reads_completed[null,156.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/reads_merged[null,0.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/sectors_read[null,4960.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/sectors_written[null,2576.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/write_time_ms[null,164.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/writes_completed[null,322.0]",
                "0/nodeExporterDisk/dm-0/node-exporter-disks/writes_merged[null,0.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/bytes_read[null,1.792955392E9]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/bytes_written[null,8.90744832E8]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/io_now[null,2.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/io_time_ms[null,106110.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/io_time_weighted[null,1761109.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/read_time_ms[null,157601.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/reads_completed[null,76622.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/reads_merged[null,0.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/sectors_read[null,3501866.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/sectors_written[null,1739736.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/write_time_ms[null,1602698.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/writes_completed[null,95298.0]",
                "0/nodeExporterDisk/dm-1/node-exporter-disks/writes_merged[null,0.0]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/bytes_read[null,4.133188608E9]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/bytes_written[null,6.6522103808E10]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/io_now[null,0.0]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/io_time_ms[null,371775.0]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/io_time_weighted[null,7.0852048E7]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/read_time_ms[null,117412.0]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/reads_completed[null,317168.0]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/reads_merged[null,0.0]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/sectors_read[null,8072634.0]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/sectors_written[null,1.29925984E8]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/write_time_ms[null,7.0724706E7]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/writes_completed[null,2246867.0]",
                "0/nodeExporterDisk/dm-2/node-exporter-disks/writes_merged[null,0.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/bytes_read[null,1.809073664E9]",
                "0/nodeExporterDisk/sda/node-exporter-disks/bytes_written[null,8.92125696E8]",
                "0/nodeExporterDisk/sda/node-exporter-disks/io_now[null,0.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/io_time_ms[null,103955.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/io_time_weighted[null,711198.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/read_time_ms[null,109492.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/reads_completed[null,74745.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/reads_merged[null,2634.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/sectors_read[null,3533347.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/sectors_written[null,1742433.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/write_time_ms[null,600041.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/writes_completed[null,68129.0]",
                "0/nodeExporterDisk/sda/node-exporter-disks/writes_merged[null,32275.0]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/bytes_read[null,4.135788032E9]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/bytes_written[null,6.6522103808E10]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/io_now[null,0.0]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/io_time_ms[null,351586.0]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/io_time_weighted[null,1.4474691E7]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/read_time_ms[null,83011.0]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/reads_completed[null,303217.0]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/reads_merged[null,13779.0]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/sectors_read[null,8077711.0]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/sectors_written[null,1.29925984E8]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/write_time_ms[null,1.4379594E7]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/writes_completed[null,1024681.0]",
                "0/nodeExporterDisk/sdb/node-exporter-disks/writes_merged[null,1234629.0]"), collectionSetKeys);
    }

    @Test
    public void canGatherCpuMetrics() {
        // Define our configuration
        Collection collection = new Collection();
        Group nodeExporterCpu = new Group();
        nodeExporterCpu.setName("node-exporter-cpu");
        nodeExporterCpu.setFilterExp("name matches 'node_cpu'");
        nodeExporterCpu.setGroupByExp("labels[cpu]");
        nodeExporterCpu.setResourceType("nodeExporterCPU");

        NumericAttribute attribute = new NumericAttribute();
        attribute.setAliasExp("labels[mode]");
        nodeExporterCpu.getNumericAttribute().add(attribute);

        collection.getGroup().add(nodeExporterCpu);

        // Define the resource type
        ResourceType resourceType = createStandardResourceType("nodeExporterCPU");
        ResourceTypeMapper.getInstance().setResourceTypeMapper((rt) -> resourceType);
        
        // Collect!
        CollectionSet collectionSet = collect(collection);

        // Verify
        List<String> collectionSetKeys = CollectionSetUtils.flatten(collectionSet);
        assertEquals(Arrays.asList("0/nodeExporterCPU/cpu0/node-exporter-cpu/guest[null,0.0]",
                "0/nodeExporterCPU/cpu0/node-exporter-cpu/idle[null,16594.88]",
                "0/nodeExporterCPU/cpu0/node-exporter-cpu/iowait[null,163.99]",
                "0/nodeExporterCPU/cpu0/node-exporter-cpu/irq[null,62.55]",
                "0/nodeExporterCPU/cpu0/node-exporter-cpu/nice[null,1134.72]",
                "0/nodeExporterCPU/cpu0/node-exporter-cpu/softirq[null,58.2]",
                "0/nodeExporterCPU/cpu0/node-exporter-cpu/steal[null,0.0]",
                "0/nodeExporterCPU/cpu0/node-exporter-cpu/system[null,316.98]",
                "0/nodeExporterCPU/cpu0/node-exporter-cpu/user[null,1638.27]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/guest[null,0.0]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/idle[null,17790.51]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/iowait[null,71.6]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/irq[null,39.27]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/nice[null,660.0]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/softirq[null,34.22]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/steal[null,0.0]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/system[null,207.24]",
                "0/nodeExporterCPU/cpu1/node-exporter-cpu/user[null,1182.24]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/guest[null,0.0]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/idle[null,16907.35]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/iowait[null,134.94]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/irq[null,35.18]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/nice[null,928.18]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/softirq[null,23.12]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/steal[null,0.0]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/system[null,316.74]",
                "0/nodeExporterCPU/cpu2/node-exporter-cpu/user[null,1616.54]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/guest[null,0.0]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/idle[null,17780.12]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/iowait[null,52.55]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/irq[null,26.66]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/nice[null,696.89]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/softirq[null,9.73]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/steal[null,0.0]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/system[null,203.84]",
                "0/nodeExporterCPU/cpu3/node-exporter-cpu/user[null,1217.81]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/guest[null,0.0]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/idle[null,16817.38]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/iowait[null,121.42]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/irq[null,29.09]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/nice[null,986.56]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/softirq[null,12.99]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/steal[null,0.0]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/system[null,311.9]",
                "0/nodeExporterCPU/cpu4/node-exporter-cpu/user[null,1696.0]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/guest[null,0.0]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/idle[null,17891.54]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/iowait[null,44.88]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/irq[null,20.32]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/nice[null,570.46]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/softirq[null,7.6]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/steal[null,0.0]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/system[null,202.13]",
                "0/nodeExporterCPU/cpu5/node-exporter-cpu/user[null,1252.44]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/guest[null,0.0]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/idle[null,16782.14]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/iowait[null,146.53]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/irq[null,26.75]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/nice[null,1001.04]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/softirq[null,12.32]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/steal[null,0.0]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/system[null,300.31]",
                "0/nodeExporterCPU/cpu6/node-exporter-cpu/user[null,1705.1]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/guest[null,0.0]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/idle[null,17955.02]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/iowait[null,49.39]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/irq[null,17.68]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/nice[null,553.09]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/softirq[null,7.85]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/steal[null,0.0]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/system[null,200.86]",
                "0/nodeExporterCPU/cpu7/node-exporter-cpu/user[null,1203.71]"), collectionSetKeys);
    }

    private CollectionSet collect(Collection collection) {
        // Create the agent
        OnmsNode node = mock(OnmsNode.class);
        OnmsIpInterface iface = mock(OnmsIpInterface.class);
        when(iface.getNode()).thenReturn(node);
        when(iface.getIpAddress()).thenReturn(InetAddrUtils.getLocalHostAddress());

        IpInterfaceDao ifaceDao = mock(IpInterfaceDao.class);
        when(ifaceDao.load(1)).thenReturn(iface);
        PlatformTransactionManager transMgr = mock(PlatformTransactionManager.class);
        final SnmpCollectionAgent agent = DefaultCollectionAgent.create(1, ifaceDao, transMgr);

        PrometheusDataCollectionConfigDao collectionDao = mock(PrometheusDataCollectionConfigDao.class);
        when(collectionDao.getCollectionByName(any())).thenReturn(collection);
        collector.setPrometheusCollectionDao(collectionDao);

        try {
            Map<String, Object> serviceParams = new ImmutableMap.Builder<String, Object>()
                    .put("collection", "default")
                    .put("url", String.format("http://127.0.0.1:%d/metrics", wireMockRule.port()))
                    .build();
            Map<String, Object> runtimeParams = collector.getRuntimeAttributes(agent, serviceParams);
            Map<String, Object> allParams = new HashMap<>();
            allParams.putAll(serviceParams);
            allParams.putAll(runtimeParams);

            return collector.collect(agent, allParams);
        } catch (CollectionException e) {
            throw new RuntimeException(e);
        }
    }

    private static ResourceType createStandardResourceType(String name) {
        final ResourceType resourceType = new ResourceType();
        resourceType.setName(name);
        resourceType.setLabel("Label for: " + name);
        resourceType.setResourceLabel("${instance}");
        final StorageStrategy storageStrategy = new StorageStrategy();
        storageStrategy.setClazz(IndexStorageStrategy.class.getCanonicalName());
        resourceType.setStorageStrategy(storageStrategy);
        final PersistenceSelectorStrategy persistenceSelectorStrategy = new PersistenceSelectorStrategy();
        persistenceSelectorStrategy.setClazz(PersistAllSelectorStrategy.class.getCanonicalName());
        resourceType.setPersistenceSelectorStrategy(persistenceSelectorStrategy);
        return resourceType;
    }

    public static void printAsArrayList(List<String> lines) {
        try {
            Thread.sleep(100);
            for (String line : lines) {
                System.err.printf("\"%s\",\n", line);
            }
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

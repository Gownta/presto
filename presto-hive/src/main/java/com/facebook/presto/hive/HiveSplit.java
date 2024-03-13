/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.hive;

import com.facebook.presto.hive.metastore.Storage;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.HostAddress;
import com.facebook.presto.spi.NodeProvider;
import com.facebook.presto.spi.SplitWeight;
import com.facebook.presto.spi.schedule.NodeSelectionStrategy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import static com.facebook.presto.spi.schedule.NodeSelectionStrategy.SOFT_AFFINITY;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class HiveSplit
        implements ConnectorSplit
{
    private final HiveFileSplit fileSplit;
    private final Storage storage;
    private final List<HivePartitionKey> partitionKeys;
    private final List<HostAddress> addresses;
    private final String database;
    private final String table;
    private final String partitionName;
    private final OptionalInt readBucketNumber;
    private final OptionalInt tableBucketNumber;
    private final NodeSelectionStrategy nodeSelectionStrategy;
    private final int partitionDataColumnCount;
    private final TableToPartitionMapping tableToPartitionMapping;
    private final Optional<BucketConversion> bucketConversion;
    private final boolean s3SelectPushdownEnabled;
    private final CacheQuotaRequirement cacheQuotaRequirement;
    private final Optional<EncryptionInformation> encryptionInformation;
    private final Set<ColumnHandle> redundantColumnDomains;
    private final SplitWeight splitWeight;
    private final Optional<byte[]> rowIdPartitionComponent;

    @JsonCreator
    public HiveSplit(
            @JsonProperty("fileSplit") HiveFileSplit fileSplit,
            @JsonProperty("database") String database,
            @JsonProperty("table") String table,
            @JsonProperty("partitionName") String partitionName,
            @JsonProperty("storage") Storage storage,
            @JsonProperty("partitionKeys") List<HivePartitionKey> partitionKeys,
            @JsonProperty("addresses") List<HostAddress> addresses,
            @JsonProperty("readBucketNumber") OptionalInt readBucketNumber,
            @JsonProperty("tableBucketNumber") OptionalInt tableBucketNumber,
            @JsonProperty("nodeSelectionStrategy") NodeSelectionStrategy nodeSelectionStrategy,
            @JsonProperty("partitionDataColumnCount") int partitionDataColumnCount,
            @JsonProperty("tableToPartitionMapping") TableToPartitionMapping tableToPartitionMapping,
            @JsonProperty("bucketConversion") Optional<BucketConversion> bucketConversion,
            @JsonProperty("s3SelectPushdownEnabled") boolean s3SelectPushdownEnabled,
            @JsonProperty("cacheQuota") CacheQuotaRequirement cacheQuotaRequirement,
            @JsonProperty("encryptionMetadata") Optional<EncryptionInformation> encryptionInformation,
            @JsonProperty("redundantColumnDomains") Set<ColumnHandle> redundantColumnDomains,
            @JsonProperty("splitWeight") SplitWeight splitWeight,
            @JsonProperty("rowIdPartitionComponent") Optional<byte[]> rowIdPartitionComponent)
    {
        requireNonNull(fileSplit, "fileSplit is null");
        requireNonNull(database, "database is null");
        requireNonNull(table, "table is null");
        requireNonNull(partitionName, "partitionName is null");
        requireNonNull(storage, "storage is null");
        requireNonNull(partitionKeys, "partitionKeys is null");
        requireNonNull(addresses, "addresses is null");
        requireNonNull(readBucketNumber, "readBucketNumber is null");
        requireNonNull(tableBucketNumber, "tableBucketNumber is null");
        requireNonNull(nodeSelectionStrategy, "nodeSelectionStrategy is null");
        requireNonNull(tableToPartitionMapping, "tableToPartitionMapping is null");
        requireNonNull(bucketConversion, "bucketConversion is null");
        requireNonNull(cacheQuotaRequirement, "cacheQuotaRequirement is null");
        requireNonNull(encryptionInformation, "encryptionMetadata is null");
        requireNonNull(redundantColumnDomains, "redundantColumnDomains is null");
        requireNonNull(rowIdPartitionComponent, "rowIdPartitionComponent is null");

        this.fileSplit = fileSplit;
        this.database = database;
        this.table = table;
        this.partitionName = partitionName;
        this.storage = storage;
        this.partitionKeys = ImmutableList.copyOf(partitionKeys);
        this.addresses = ImmutableList.copyOf(addresses);
        this.readBucketNumber = readBucketNumber;
        this.tableBucketNumber = tableBucketNumber;
        this.nodeSelectionStrategy = nodeSelectionStrategy;
        this.partitionDataColumnCount = partitionDataColumnCount;
        this.tableToPartitionMapping = tableToPartitionMapping;
        this.bucketConversion = bucketConversion;
        this.s3SelectPushdownEnabled = s3SelectPushdownEnabled;
        this.cacheQuotaRequirement = cacheQuotaRequirement;
        this.encryptionInformation = encryptionInformation;
        this.redundantColumnDomains = ImmutableSet.copyOf(redundantColumnDomains);
        this.splitWeight = requireNonNull(splitWeight, "splitWeight is null");
        this.rowIdPartitionComponent = rowIdPartitionComponent;
    }

    @JsonProperty
    public HiveFileSplit getFileSplit()
    {
        return fileSplit;
    }

    @JsonProperty
    public String getDatabase()
    {
        return database;
    }

    @JsonProperty
    public String getTable()
    {
        return table;
    }

    @JsonProperty
    public String getPartitionName()
    {
        return partitionName;
    }

    @JsonProperty
    public Storage getStorage()
    {
        return storage;
    }

    @JsonProperty
    public List<HivePartitionKey> getPartitionKeys()
    {
        return partitionKeys;
    }

    @JsonProperty
    public List<HostAddress> getAddresses()
    {
        return addresses;
    }

    @Override
    public List<HostAddress> getPreferredNodes(NodeProvider nodeProvider)
    {
        if (getNodeSelectionStrategy() == SOFT_AFFINITY) {
            return nodeProvider.get(fileSplit.getPath(), 2);
        }
        return addresses;
    }

    @JsonProperty
    public OptionalInt getReadBucketNumber()
    {
        return readBucketNumber;
    }

    @JsonProperty
    public OptionalInt getTableBucketNumber()
    {
        return tableBucketNumber;
    }

    @JsonProperty
    public int getPartitionDataColumnCount()
    {
        return partitionDataColumnCount;
    }

    @JsonProperty
    public TableToPartitionMapping getTableToPartitionMapping()
    {
        return tableToPartitionMapping;
    }

    @JsonProperty
    public Optional<BucketConversion> getBucketConversion()
    {
        return bucketConversion;
    }

    @JsonProperty
    @Override
    public NodeSelectionStrategy getNodeSelectionStrategy()
    {
        return nodeSelectionStrategy;
    }

    @JsonProperty
    public boolean isS3SelectPushdownEnabled()
    {
        return s3SelectPushdownEnabled;
    }

    @JsonProperty
    public CacheQuotaRequirement getCacheQuotaRequirement()
    {
        return cacheQuotaRequirement;
    }

    @JsonProperty
    public Optional<EncryptionInformation> getEncryptionInformation()
    {
        return encryptionInformation;
    }

    @JsonProperty
    public Set<ColumnHandle> getRedundantColumnDomains()
    {
        return redundantColumnDomains;
    }

    @JsonProperty
    @Override
    public SplitWeight getSplitWeight()
    {
        return splitWeight;
    }

    @JsonProperty
    public Optional<byte[]> getRowIdPartitionComponent()
    {
        return rowIdPartitionComponent;
    }

    @Override
    public Object getInfo()
    {
        return ImmutableMap.builder()
                .put("path", fileSplit.getPath())
                .put("start", fileSplit.getStart())
                .put("length", fileSplit.getLength())
                .put("fileSize", fileSplit.getFileSize())
                .put("fileModifiedTime", fileSplit.getFileModifiedTime())
                .put("hosts", addresses)
                .put("database", database)
                .put("table", table)
                .put("nodeSelectionStrategy", nodeSelectionStrategy)
                .put("partitionName", partitionName)
                .put("s3SelectPushdownEnabled", s3SelectPushdownEnabled)
                .put("cacheQuotaRequirement", cacheQuotaRequirement)
                .build();
    }

    @Override
    public Map<String, String> getInfoMap()
    {
        return ImmutableMap.<String, String>builder()
                .put("path", fileSplit.getPath())
                .put("start", Long.toString(fileSplit.getStart()))
                .put("length", Long.toString(fileSplit.getLength()))
                .put("fileSize", Long.toString(fileSplit.getFileSize()))
                .put("fileModifiedTime", Long.toString(fileSplit.getFileModifiedTime()))
                .put("hosts", addresses.toString())
                .put("database", database)
                .put("table", table)
                .put("nodeSelectionStrategy", nodeSelectionStrategy.toString())
                .put("partitionName", partitionName)
                .put("s3SelectPushdownEnabled", Boolean.toString(s3SelectPushdownEnabled))
                .put("cacheQuotaRequirement", cacheQuotaRequirement.toString())
                .build();
    }

    @Override
    public Object getSplitIdentifier()
    {
        return ImmutableMap.builder()
                .put("path", fileSplit.getPath())
                .put("start", fileSplit.getStart())
                .put("length", fileSplit.getLength())
                .build();
    }

    @Override
    public OptionalLong getSplitSizeInBytes()
    {
        return OptionalLong.of(fileSplit.getLength());
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .addValue(fileSplit.getPath())
                .addValue(fileSplit.getStart())
                .addValue(fileSplit.getLength())
                .addValue(fileSplit.getFileSize())
                .addValue(s3SelectPushdownEnabled)
                .addValue(cacheQuotaRequirement)
                .toString();
    }

    public static class BucketConversion
    {
        private final int tableBucketCount;
        private final int partitionBucketCount;
        private final List<HiveColumnHandle> bucketColumnNames;
        // tableBucketNumber is needed, but can be found in tableBucketNumber field of HiveSplit.

        @JsonCreator
        public BucketConversion(
                @JsonProperty("tableBucketCount") int tableBucketCount,
                @JsonProperty("partitionBucketCount") int partitionBucketCount,
                @JsonProperty("bucketColumnHandles") List<HiveColumnHandle> bucketColumnHandles)
        {
            this.tableBucketCount = tableBucketCount;
            this.partitionBucketCount = partitionBucketCount;
            this.bucketColumnNames = requireNonNull(bucketColumnHandles, "bucketColumnHandles is null");
        }

        @JsonProperty
        public int getTableBucketCount()
        {
            return tableBucketCount;
        }

        @JsonProperty
        public int getPartitionBucketCount()
        {
            return partitionBucketCount;
        }

        @JsonProperty
        public List<HiveColumnHandle> getBucketColumnHandles()
        {
            return bucketColumnNames;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BucketConversion that = (BucketConversion) o;
            return tableBucketCount == that.tableBucketCount &&
                    partitionBucketCount == that.partitionBucketCount &&
                    Objects.equals(bucketColumnNames, that.bucketColumnNames);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(tableBucketCount, partitionBucketCount, bucketColumnNames);
        }
    }
}

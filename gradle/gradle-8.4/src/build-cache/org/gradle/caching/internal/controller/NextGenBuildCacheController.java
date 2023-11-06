/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.caching.internal.controller;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.io.Closer;
import com.google.common.io.CountingInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.caching.BuildCacheKey;
import org.gradle.caching.internal.CacheableEntity;
import org.gradle.caching.internal.DefaultBuildCacheKey;
import org.gradle.caching.internal.NextGenBuildCacheService;
import org.gradle.caching.internal.controller.CacheManifest.ManifestEntry;
import org.gradle.caching.internal.controller.operations.LoadOperationDetails;
import org.gradle.caching.internal.controller.operations.LoadOperationHitResult;
import org.gradle.caching.internal.controller.operations.LoadOperationMissResult;
import org.gradle.caching.internal.controller.operations.PackOperationDetails;
import org.gradle.caching.internal.controller.operations.PackOperationResult;
import org.gradle.caching.internal.controller.operations.StoreOperationDetails;
import org.gradle.caching.internal.controller.operations.StoreOperationResult;
import org.gradle.caching.internal.controller.operations.UnpackOperationDetails;
import org.gradle.caching.internal.controller.operations.UnpackOperationResult;
import org.gradle.caching.internal.controller.service.BuildCacheLoadResult;
import org.gradle.caching.internal.operations.BuildCacheArchivePackBuildOperationType;
import org.gradle.caching.internal.operations.BuildCacheArchiveUnpackBuildOperationType;
import org.gradle.caching.internal.operations.BuildCacheRemoteLoadBuildOperationType;
import org.gradle.caching.internal.operations.BuildCacheRemoteStoreBuildOperationType;
import org.gradle.caching.internal.origin.OriginMetadata;
import org.gradle.caching.internal.packaging.impl.RelativePathParser;
import org.gradle.internal.exceptions.DefaultMultiCauseException;
import org.gradle.internal.file.BufferProvider;
import org.gradle.internal.file.Deleter;
import org.gradle.internal.file.FileType;
import org.gradle.internal.file.TreeType;
import org.gradle.internal.file.impl.DefaultFileMetadata;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.snapshot.DirectorySnapshotBuilder;
import org.gradle.internal.snapshot.FileSystemLocationSnapshot;
import org.gradle.internal.snapshot.FileSystemSnapshot;
import org.gradle.internal.snapshot.MerkleDirectorySnapshotBuilder;
import org.gradle.internal.snapshot.RegularFileSnapshot;
import org.gradle.internal.snapshot.RelativePathTracker;
import org.gradle.internal.snapshot.SnapshotUtil;
import org.gradle.internal.snapshot.SnapshotVisitResult;
import org.gradle.internal.vfs.FileSystemAccess;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.gradle.internal.file.FileMetadata.AccessType.DIRECT;
import static org.gradle.internal.file.FileType.Directory;
import static org.gradle.internal.snapshot.DirectorySnapshotBuilder.EmptyDirectoryHandlingStrategy.INCLUDE_EMPTY_DIRS;

/**
 * Controller for next-gen build cache.
 *
 * <h3>Legacy build operations</h3>
 *
 * <p>
 * Uses legacy build operations to capture next-gen build cache logic.
 * Since the new protocol is more fine-grained, some peculiarities can be observed.
 * </p>
 *
 * <ul>
 *   <li>
 *     When downloading entries from the remote cache, a single {@link BuildCacheRemoteLoadBuildOperationType}
 *     is emitted per cached result, but only if we had to touch the remote cache.
 *     The cache key is the key for the manifest, the reported archive size is the sum number of bytes downloaded
 *     (including manifest and content pieces).
 *   </li>
 *   <li>
 *       Similarly during upload at most one {@link BuildCacheRemoteStoreBuildOperationType} is emitted,
 *       if we actually tried to upload something to the remote cache (a manifest or a content entry).
 *       The cache key reported is the manifest key, and the size reported is the total size of all the entries
 *       in the manifest plus the size of the manifest itself. Note: unlike with downloading, we do not report
 *       the actual amount of uploaded data; this is because the operation requires the size to be specified
 *       up front, before we know what has been stored and what hasn't.
 *   </li>
 *   <li>
 *       For packing we report exactly one {@link BuildCacheArchivePackBuildOperationType}, the number of
 *       stored entries and the total size reflect the actual number of entries and number of (uncompressed) bytes
 *       stored in the local cache. (This includes the manifest's size and count.)
 *   </li>
 *   <li>
 *       For unpacking we report exactly one {@link BuildCacheArchiveUnpackBuildOperationType}; similarly to the upload
 *       case the archive size reported is actually not the copied amount, but the total size in the manifest
 *       (plus the size of the manifest). The entry count reflects the actual files unpacked (plus one for the manifest).
 *       However, for now this should always match the total number of files in the manifest, as (for now) we always
 *       delete any previous output when loading from cache, and thus we need to unpack every entry.
 *   </li>
 * </ul>
 */
public class NextGenBuildCacheController implements BuildCacheController {

    public static final String NEXT_GEN_CACHE_SYSTEM_PROPERTY = "org.gradle.unsafe.cache.ng";

    private final BufferProvider bufferProvider;
    private final BuildOperationExecutor buildOperationExecutor;
    private final NextGenBuildCacheAccess cacheAccess;
    private final FileSystemAccess fileSystemAccess;
    private final String buildInvocationId;
    private final Logger logger;
    private final Deleter deleter;
    private final StringInterner stringInterner;
    private final Gson gson;

    public NextGenBuildCacheController(
        String buildInvocationId,
        Logger logger,
        Deleter deleter,
        FileSystemAccess fileSystemAccess,
        BufferProvider bufferProvider,
        StringInterner stringInterner,
        BuildOperationExecutor buildOperationExecutor,
        NextGenBuildCacheAccess cacheAccess
    ) {
        this.buildInvocationId = buildInvocationId;
        this.logger = logger;
        this.deleter = deleter;
        this.fileSystemAccess = fileSystemAccess;
        this.bufferProvider = bufferProvider;
        this.buildOperationExecutor = buildOperationExecutor;
        this.cacheAccess = cacheAccess;
        this.stringInterner = stringInterner;
        this.gson = createGson();

        logger.warn("Creating next-generation build cache controller");
    }

    @VisibleForTesting
    public static Gson createGson() {
        return new GsonBuilder()
            .registerTypeAdapter(Duration.class, new TypeAdapter<Duration>() {
                @Override
                public void write(JsonWriter out, Duration value) throws IOException {
                    out.value(value.toMillis());
                }

                @Override
                public Duration read(JsonReader in) throws IOException {
                    return Duration.ofMillis(in.nextLong());
                }
            })
            .registerTypeAdapter(HashCode.class, new TypeAdapter<HashCode>() {
                @Override
                public void write(JsonWriter out, HashCode value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public HashCode read(JsonReader in) throws IOException {
                    return HashCode.fromString(in.nextString());
                }
            })
            .create();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isEmitDebugLogging() {
        return false;
    }

    @Override
    public Optional<BuildCacheLoadResult> load(BuildCacheKey manifestKey, CacheableEntity cacheableEntity) {
        try (OperationFiringLoadHandlerFactory handlerFactory = new OperationFiringLoadHandlerFactory(manifestKey)) {
            // TODO Make load() return T
            AtomicReference<CacheManifest> manifestRef = new AtomicReference<>();
            AtomicLong manifestSize = new AtomicLong(-1L);
            cacheAccess.load(Collections.singletonMap(manifestKey, null), handlerFactory.create((manifestStream, __) -> {
                CountingInputStream counterStream = new CountingInputStream(manifestStream);
                manifestRef.set(gson.fromJson(new InputStreamReader(counterStream), CacheManifest.class));
                manifestSize.set(counterStream.getCount());
            }));
            CacheManifest manifest = manifestRef.get();
            if (manifest == null) {
                return Optional.empty();
            }

            long totalSize = manifest.getPropertyManifests().values().stream()
                .flatMap(List::stream)
                .map(ManifestEntry::getLength)
                .reduce(manifestSize.get(), Long::sum);
            handlerFactory.ensureUnpackOperationStrated(totalSize);
            ImmutableSortedMap<String, FileSystemSnapshot> resultingSnapshots = loadContent(cacheableEntity, manifest, handlerFactory);
            return Optional.of(new BuildCacheLoadResult() {
                @Override
                public long getArtifactEntryCount() {
                    return handlerFactory.unpackedEntryCount.get();
                }

                @Override
                public OriginMetadata getOriginMetadata() {
                    return manifest.getOriginMetadata();
                }

                @Override
                public ImmutableSortedMap<String, FileSystemSnapshot> getResultingSnapshots() {
                    return resultingSnapshots;
                }
            });
        }
    }

    /**
     * A wrapper for on-demand build operations that are only started when needed.
     *
     * E.g. if we don't download anything, we don't need to start a legacy load operation.
     */
    private class OnDemandBuildOperationWrapper {
        private volatile BuildOperationContext context;
        private final List<Throwable> failures = new CopyOnWriteArrayList<>();

        public void ensureStarted(Supplier<BuildOperationDescriptor.Builder> describer) {
            if (context == null) {
                synchronized (this) {
                    if (context == null) {
                        context = buildOperationExecutor.start(describer.get());
                    }
                }
            }
        }

        public void fail(Throwable failure) {
            failures.add(failure);
        }

        public void finishIfNecessary(Supplier<Object> result) {
            if (context != null) {
                if (failures.isEmpty()) {
                    context.setResult(result.get());
                } else {
                    context.failed(failures.size() == 1
                        ? failures.get(0)
                        : new DefaultMultiCauseException("Errors encountered while loading entries from remote cache", failures));
                }
            }
        }
    }

    private class OperationFiringLoadHandlerFactory implements Closeable {
        private final BuildCacheKey manifestKey;

        private final OnDemandBuildOperationWrapper unpackBuildOp = new OnDemandBuildOperationWrapper();
        private final OnDemandBuildOperationWrapper loadBuildOp = new OnDemandBuildOperationWrapper();
        private final AtomicLong unpackedEntryCount = new AtomicLong(0L);
        private final AtomicLong totalUnpackedSize = new AtomicLong(0L);
        private final AtomicLong totalDownloadedSize = new AtomicLong(0L);
        private final AtomicBoolean missEncountered = new AtomicBoolean(false);

        private OnDemandBuildOperationWrapper parentOp;

        public OperationFiringLoadHandlerFactory(BuildCacheKey manifestKey) {
            this.manifestKey = manifestKey;
        }

        public <T> NextGenBuildCacheAccess.LoadHandler<T> create(BiConsumer<InputStream, T> delegate) {
            return new NextGenBuildCacheAccess.LoadHandler<T>() {
                @Override
                public void handle(InputStream input, T payload) {
                    CountingInputStream countingStream = new CountingInputStream(input);
                    delegate.accept(countingStream, payload);
                    totalUnpackedSize.addAndGet(countingStream.getCount());
                    unpackedEntryCount.incrementAndGet();
                }

                @Override
                public void ensureLoadOperationStarted(BuildCacheKey key) {
                    loadBuildOp.ensureStarted(() -> BuildOperationDescriptor.displayName("Load entry " + manifestKey.getDisplayName() + " from remote build cache")
                        .details(new LoadOperationDetails(manifestKey))
                        .progressDisplayName("Requesting from remote build cache"));
                    if (parentOp == null) {
                        parentOp = loadBuildOp;
                    }
                }

                @Override
                public void recordLoadHit(BuildCacheKey key, long size) {
                    totalDownloadedSize.addAndGet(size);
                }

                @Override
                public void recordLoadMiss(BuildCacheKey key) {
                    missEncountered.set(true);
                }

                @Override
                public void recordLoadFailure(BuildCacheKey key, Throwable failure) {
                    loadBuildOp.fail(failure);
                }

                @Override
                public void recordUnpackFailure(BuildCacheKey key, Throwable failure) {
                    // Make sure an unpack operation is running
                    ensureUnpackOperationStrated(-1);
                    unpackBuildOp.fail(failure);
                }
            };
        }

        public void ensureUnpackOperationStrated(long totalSize) {
            unpackBuildOp.ensureStarted(() -> {
                // TODO Use "load" instead of "unpack" here
                return BuildOperationDescriptor.displayName("Unpack build cache entry " + manifestKey.getHashCode())
                    .details(new UnpackOperationDetails(manifestKey, totalSize))
                    .progressDisplayName("Unpacking build cache entry");
            });
            if (parentOp == null) {
                parentOp = unpackBuildOp;
            }
        }

        @Override
        public void close() {
            // Load and unpack operations can be started in any order, and we have to finish them in child-first-then-parent order
            if (parentOp == loadBuildOp) {
                finishUnpackIfNecessary();
                finishLoadIfNecessary();
            } else {
                finishLoadIfNecessary();
                finishUnpackIfNecessary();
            }
        }

        private void finishLoadIfNecessary() {
            loadBuildOp.finishIfNecessary(() -> missEncountered.get()
                ? LoadOperationMissResult.INSTANCE
                : new LoadOperationHitResult(totalDownloadedSize.get()));
        }

        private void finishUnpackIfNecessary() {
            unpackBuildOp.finishIfNecessary(() -> new UnpackOperationResult(unpackedEntryCount.get()));
        }
    }

    private ImmutableSortedMap<String, FileSystemSnapshot> loadContent(CacheableEntity cacheableEntity, CacheManifest manifest, OperationFiringLoadHandlerFactory handlerFactory) {
        ImmutableSortedMap.Builder<String, FileSystemSnapshot> snapshots = ImmutableSortedMap.naturalOrder();

        cacheableEntity.visitOutputTrees((propertyName, type, root) -> {
            // Invalidate VFS
            fileSystemAccess.write(Collections.singleton(root.getAbsolutePath()), () -> {});

            // TODO Apply diff to outputs instead of clearing them here and loading everything
            try {
                cleanOutputDirectory(type, root);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            // Note that there can be multiple output files with the same content
            ImmutableListMultimap.Builder<BuildCacheKey, File> filesBuilder = ImmutableListMultimap.builder();
            List<ManifestEntry> manifestEntries = manifest.getPropertyManifests().get(propertyName);
            manifestEntries.forEach(entry -> {
                File file = new File(root, entry.getRelativePath());
                switch (entry.getType()) {
                    case Directory:
                        // TODO set correct file permissions
                        // TODO Handle this
                        //noinspection ResultOfMethodCallIgnored
                        file.mkdirs();
                        break;
                    case RegularFile:
                        // TODO set correct file permissions
                        filesBuilder.put(new DefaultBuildCacheKey(entry.getContentHash()), file);
                        break;
                    case Missing:
                        FileUtils.deleteQuietly(file);
                        break;
                }
            });

            // TODO Filter out entries that are already in the right place in the output directory
            cacheAccess.load(filesBuilder.build().asMap(), handlerFactory.create((input, filesForHash) -> {
                try (Closer closer = Closer.create()) {
                    OutputStream output = filesForHash.stream()
                        .map(file -> {
                            try {
                                return closer.register(new FileOutputStream(file));
                            } catch (FileNotFoundException e) {
                                throw new UncheckedIOException("Couldn't create " + file.getAbsolutePath(), e);
                            }
                        })
                        .map(OutputStream.class::cast)
                        .reduce(TeeOutputStream::new)
                        .orElse(NullOutputStream.NULL_OUTPUT_STREAM);

                    IOUtils.copyLarge(input, output, bufferProvider.getBuffer());
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }));

            createSnapshot(type, root, manifestEntries)
                .ifPresent(snapshot -> {
                    snapshots.put(propertyName, snapshot);
                    fileSystemAccess.record(snapshot);
                });
        });

        return snapshots.build();
    }

    // TODO Extract snapshotting part to it's own class
    @VisibleForTesting
    Optional<FileSystemLocationSnapshot> createSnapshot(TreeType type, File root, List<ManifestEntry> entries) {
        switch (type) {
            case DIRECTORY:
                return Optional.of(createDirectorySnapshot(root, entries));
            case FILE:
                if (entries.size() != 1) {
                    throw new IllegalStateException("Expected a single manifest entry, found " + entries.size());
                }
                ManifestEntry rootEntry = entries.get(0);
                switch (rootEntry.getType()) {
                    case Directory:
                        throw new IllegalStateException("Directory manifest entry found for a file output");
                    case RegularFile:
                        return Optional.of(createFileSnapshot(rootEntry, root));
                    case Missing:
                        // No need to create a
                        return Optional.empty();
                    default:
                        throw new AssertionError("Unknown manifest entry type " + rootEntry.getType());
                }
            default:
                throw new AssertionError("Unknown output type " + type);
        }
    }

    // TODO We should not capture any snapshot for a missing directory output
    private FileSystemLocationSnapshot createDirectorySnapshot(File root, List<ManifestEntry> entries) {
        String rootPath = root.getName() + "/";
        RelativePathParser parser = new RelativePathParser(rootPath);
        DirectorySnapshotBuilder builder = MerkleDirectorySnapshotBuilder.noSortingRequired();
        builder.enterDirectory(DIRECT, stringInterner.intern(root.getAbsolutePath()), stringInterner.intern(root.getName()), INCLUDE_EMPTY_DIRS);

        for (ManifestEntry entry : Iterables.skip(entries, 1)) {
            File file = new File(root, entry.getRelativePath());

            boolean isDirectory = entry.getType() == Directory;
            String relativePath = isDirectory
                ? rootPath + entry.getRelativePath() + "/"
                : rootPath + entry.getRelativePath();
            boolean outsideOfRoot = parser.nextPath(relativePath, isDirectory, builder::leaveDirectory);
            if (outsideOfRoot) {
                break;
            }

            switch (entry.getType()) {
                case Directory:
                    String internedAbsolutePath = stringInterner.intern(file.getAbsolutePath());
                    String internedName = stringInterner.intern(parser.getName());
                    builder.enterDirectory(DIRECT, internedAbsolutePath, internedName, INCLUDE_EMPTY_DIRS);
                    break;
                case RegularFile:
                    RegularFileSnapshot fileSnapshot = createFileSnapshot(entry, file);
                    builder.visitLeafElement(fileSnapshot);
                    break;
                case Missing:
                    // No need to store a snapshot for a missing file
                    break;
            }
        }

        parser.exitToRoot(builder::leaveDirectory);
        builder.leaveDirectory();
        return checkNotNull(builder.getResult());
    }

    private RegularFileSnapshot createFileSnapshot(CacheManifest.ManifestEntry entry, File file) {
        return new RegularFileSnapshot(
            stringInterner.intern(file.getAbsolutePath()),
            stringInterner.intern(file.getName()),
            entry.getContentHash(),
            DefaultFileMetadata.file(file.lastModified(), entry.getLength(), DIRECT)
        );
    }

    @Override
    public void store(BuildCacheKey manifestKey, CacheableEntity entity, Map<String, FileSystemSnapshot> snapshots, Duration executionTime) {
        ImmutableMap.Builder<String, List<ManifestEntry>> propertyManifests = ImmutableMap.builder();

        AtomicLong contentSize = new AtomicLong(0L);
        entity.visitOutputTrees((propertyName, type, root) -> {
            ImmutableList.Builder<ManifestEntry> manifestEntries = ImmutableList.builder();
            FileSystemSnapshot rootSnapshot = snapshots.get(propertyName);
            rootSnapshot.accept(new RelativePathTracker(), (snapshot, relativePath) -> {
                if (relativePath.isRoot()) {
                    assertCorrectType(type, snapshot);
                }
                long size = SnapshotUtil.getLength(snapshot);
                manifestEntries.add(new ManifestEntry(
                    snapshot.getType(),
                    relativePath.toRelativePath(),
                    snapshot.getHash(),
                    size));
                contentSize.addAndGet(size);
                return SnapshotVisitResult.CONTINUE;
            });
            propertyManifests.put(propertyName, manifestEntries.build());
        });

        CacheManifest manifest = new CacheManifest(
            new OriginMetadata(buildInvocationId, executionTime),
            entity.getType().getName(),
            entity.getIdentity(),
            propertyManifests.build());

        String manifestJson = gson.toJson(manifest);
        byte[] manifestBytes = manifestJson.getBytes(StandardCharsets.UTF_8);

        long totalUploadSize = contentSize.get() + manifestBytes.length;
        try (OperationFiringStoreHandlerFactory handlerFactory = new OperationFiringStoreHandlerFactory(manifestKey, totalUploadSize)) {
            storeInner(manifestKey, entity, manifest, manifestBytes, handlerFactory);
        }
    }

    private class OperationFiringStoreHandlerFactory implements Closeable {
        private final BuildCacheKey manifestKey;
        private final long totalUploadSize;

        private final OnDemandBuildOperationWrapper packBuildOp = new OnDemandBuildOperationWrapper();
        private final AtomicLong packEntryCount = new AtomicLong(0L);
        private final AtomicLong totalPackSize = new AtomicLong(0L);
        private final OnDemandBuildOperationWrapper storeBuildOp = new OnDemandBuildOperationWrapper();
        private final AtomicBoolean storeEncountered = new AtomicBoolean(false);

        public OperationFiringStoreHandlerFactory(BuildCacheKey manifestKey, long totalUploadSize) {
            this.manifestKey = manifestKey;
            this.totalUploadSize = totalUploadSize;
            this.packBuildOp.ensureStarted(() -> BuildOperationDescriptor.displayName("Pack build cache entry " + manifestKey)
                .details(new PackOperationDetails(manifestKey))
                .progressDisplayName("Packing build cache entry"));
        }

        public <T> NextGenBuildCacheAccess.StoreHandler<T> create(Function<T, NextGenBuildCacheService.NextGenWriter> delegate) {
            return new NextGenBuildCacheAccess.StoreHandler<T>() {
                @Override
                public NextGenBuildCacheService.NextGenWriter createWriter(T payload) {
                    return delegate.apply(payload);
                }

                @Override
                public void ensureStoreOperationStarted(BuildCacheKey key) {
                    storeBuildOp.ensureStarted(() -> BuildOperationDescriptor.displayName("Store entry " + manifestKey.getDisplayName() + " in remote build cache")
                        .details(new StoreOperationDetails(manifestKey, totalUploadSize))
                        .progressDisplayName("Uploading to remote build cache"));
                }

                @Override
                public void recordStoreFinished(BuildCacheKey key, boolean stored) {
                    if (stored) {
                        storeEncountered.set(true);
                    }
                }

                @Override
                public void recordStoreFailure(BuildCacheKey key, Throwable failure) {
                    storeBuildOp.fail(failure);
                }

                @Override
                public void recordPackFailure(BuildCacheKey key, Throwable failure) {
                    packBuildOp.fail(failure);
                }
            };
        }

        @Override
        public void close() {
            storeBuildOp.finishIfNecessary(() -> storeEncountered.get()
                ? StoreOperationResult.STORED
                : StoreOperationResult.NOT_STORED);
            packBuildOp.finishIfNecessary(() -> new PackOperationResult(packEntryCount.get(), totalPackSize.get()));
        }
    }

    private static abstract class CountingWriter implements NextGenBuildCacheService.NextGenWriter {
        private final AtomicLong entryCount;
        private final AtomicLong totalSize;

        public CountingWriter(AtomicLong entryCount, AtomicLong totalSize) {
            this.entryCount = entryCount;
            this.totalSize = totalSize;
        }

        @Override
        public final InputStream openStream() throws IOException {
            markStored();
            return doOpenStream();
        }

        @Override
        public final void writeTo(OutputStream output) throws IOException {
            markStored();
            doWriteTo(output);
        }

        protected abstract InputStream doOpenStream() throws IOException;

        protected abstract void doWriteTo(OutputStream output) throws IOException;

        private void markStored() {
            entryCount.incrementAndGet();
            totalSize.addAndGet(getSize());
        }
    }

    private void storeInner(BuildCacheKey manifestKey, CacheableEntity entity, CacheManifest manifest, byte[] manifestBytes, OperationFiringStoreHandlerFactory handlerFactory) {
        entity.visitOutputTrees((propertyName, type, root) -> {
            Map<BuildCacheKey, ManifestEntry> manifestIndex = manifest.getPropertyManifests().get(propertyName).stream()
                .filter(entry -> entry.getType() == FileType.RegularFile)
                .collect(ImmutableMap.toImmutableMap(
                    manifestEntry -> new DefaultBuildCacheKey(manifestEntry.getContentHash()),
                    Function.identity(),
                    // When there are multiple identical files to store, it doesn't matter which one we read
                    (a, b) -> a)
                );

            cacheAccess.store(manifestIndex, handlerFactory.create(manifestEntry -> new CountingWriter(handlerFactory.packEntryCount, handlerFactory.totalPackSize) {
                @Override
                protected InputStream doOpenStream() throws IOException {
                    // TODO Replace with "Files.newInputStream()" as it seems to be more efficient
                    //      Might be a good idea to pass `root` as `Path` instead of `File` then
                    //noinspection IOStreamConstructor
                    return new FileInputStream(new File(root, manifestEntry.getRelativePath()));
                }

                @Override
                protected void doWriteTo(OutputStream output) throws IOException {
                    try (InputStream input = openStream()) {
                        IOUtils.copyLarge(input, output, bufferProvider.getBuffer());
                    }
                }

                @Override
                public long getSize() {
                    return manifestEntry.getLength();
                }
            }));
        });

        cacheAccess.store(Collections.singletonMap(manifestKey, manifest), handlerFactory.create(__ -> new CountingWriter(handlerFactory.packEntryCount, handlerFactory.totalPackSize) {
            @Override
            protected InputStream doOpenStream() {
                return new UnsynchronizedByteArrayInputStream(manifestBytes);
            }

            @Override
            protected void doWriteTo(OutputStream output) throws IOException {
                output.write(manifestBytes);
            }

            @Override
            public long getSize() {
                return manifestBytes.length;
            }
        }));
    }

    private static void assertCorrectType(TreeType type, FileSystemLocationSnapshot snapshot) {
        if (snapshot.getType() == FileType.Missing) {
            return;
        }
        switch (type) {
            case DIRECTORY:
                if (snapshot.getType() != Directory) {
                    throw new IllegalArgumentException(String.format("Expected '%s' to be a directory", snapshot.getAbsolutePath()));
                }
                break;
            case FILE:
                if (snapshot.getType() != FileType.RegularFile) {
                    throw new IllegalArgumentException(String.format("Expected '%s' to be a file", snapshot.getAbsolutePath()));
                }
                break;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public void close() throws IOException {
        logger.warn("Closing next-generation build cache controller");
        cacheAccess.close();
    }

    // FIXME code duplicate
    private void cleanOutputDirectory(TreeType type, File root) throws IOException {
        switch (type) {
            case DIRECTORY:
                deleter.ensureEmptyDirectory(root);
                break;
            case FILE:
                if (!makeDirectory(root.getParentFile())) {
                    if (root.exists()) {
                        deleter.deleteRecursively(root);
                    }
                }
                break;
            default:
                throw new AssertionError();
        }
    }

    // FIXME code duplicate
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean makeDirectory(File target) throws IOException {
        if (target.isDirectory()) {
            return false;
        } else if (target.isFile()) {
            deleter.delete(target);
        }
        FileUtils.forceMkdir(target);
        return true;
    }

    public static boolean isNextGenCachingEnabled() {
        return Boolean.getBoolean(NEXT_GEN_CACHE_SYSTEM_PROPERTY) == Boolean.TRUE;
    }
}

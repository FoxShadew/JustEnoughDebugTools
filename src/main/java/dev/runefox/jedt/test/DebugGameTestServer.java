//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.runefox.jedt.test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import net.minecraft.world.flag.FeatureFlagSet;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;

import dev.runefox.jedt.api.gametest.GameTestCIUtil;
import dev.runefox.jedt.api.gametest.GameTestEvents;

import java.io.File;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Deprecated(forRemoval = true)
@SuppressWarnings("removal")
public class DebugGameTestServer extends MinecraftServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PROGRESS_INTERVAL = 20;

    private static final Services TEST_SERVICES = new Services(null, ServicesKeySet.EMPTY, null, null);

    private static final GameRules TEST_GAME_RULES = Util.make(new GameRules(FeatureFlagSet.of()), rules -> {
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
    });

    private static final WorldOptions TEST_OPTIONS = new WorldOptions(
        0L,    // Seed
        false, // Generate structures
        false  // Bonus chest
    );

    private static final LevelSettings TEST_SETTINGS = new LevelSettings(
        "Test Level",                   // Name
        GameType.CREATIVE,              // Gamemode
        false,                          // Hardcore
        Difficulty.NORMAL,              // Difficulty
        true,                           // Commands
        TEST_GAME_RULES,                // Game rules
        WorldDataConfiguration.DEFAULT  // Data packs
    );

    private List<GameTestBatch> testBatches;
    private final RuntimeTestConfig config;
    private final File serverDir;

    private ServerLevel testLevel;

    @Nullable
    private MultipleTestTracker testTracker;

    public static DebugGameTestServer create(Thread thread, File serverDir, LevelStorageSource.LevelStorageAccess lsa, PackRepository datapacks, RuntimeTestConfig config) {

        datapacks.reload();

        WorldDataConfiguration worldConfig = new WorldDataConfiguration(
            new DataPackConfig(
                new ArrayList<>(datapacks.getAvailableIds()), java.util.List.of()
            ),
            FeatureFlags.REGISTRY.allFlags()
        );

        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(datapacks, worldConfig, false, true);
        WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, CommandSelection.DEDICATED, 4);

        try {
            LOGGER.debug("Starting resource loading");

            Stopwatch stopwatch = Stopwatch.createStarted();
            WorldStem worldStem = Util.blockUntilDone(
                executor -> WorldLoader.load(
                    initConfig,
                    ctx -> {
                        Registry<LevelStem> levelStems = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();

                        // Create a superflat world
                        WorldDimensions.Complete dimensions = ctx.datapackWorldgen()
                                                                 .lookupOrThrow(Registries.WORLD_PRESET)
                                                                 .getOrThrow(WorldPresets.FLAT)
                                                                 .value()
                                                                 .createWorldDimensions()
                                                                 .bake(levelStems);

                        return new WorldLoader.DataLoadOutput<>(
                            new PrimaryLevelData(
                                TEST_SETTINGS, TEST_OPTIONS,
                                dimensions.specialWorldProperty(),
                                dimensions.lifecycle()
                            ),
                            dimensions.dimensionsRegistryAccess()
                        );
                    },
                    WorldStem::new,
                    Util.backgroundExecutor(),
                    executor
                )
            ).get();
            stopwatch.stop();

            LOGGER.debug("Finished resource loading after {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return new DebugGameTestServer(thread, serverDir, lsa, datapacks, worldStem, config);
        } catch (Exception var11) {
            LOGGER.warn("Failed to load vanilla datapack, bit oops", var11);
            System.exit(-1);
            throw new IllegalStateException();
        }
    }

    private static Collection<GameTestBatch> groupTestsIntoBatches(ServerLevel level, Stream<TestFunction> tests, int partitionSize) {
        Map<String, List<TestFunction>> byBatchName = tests.collect(Collectors.groupingBy(TestFunction::batchName));

        return byBatchName.entrySet().stream().flatMap(entry -> {
            String string = entry.getKey();

            Consumer<ServerLevel> before = DebugGameTestRegistry.getBeforeBatchFunction(string);
            Consumer<ServerLevel> after = DebugGameTestRegistry.getAfterBatchFunction(string);
            MutableInt batchNr = new MutableInt();
            Collection<TestFunction> batchTests = entry.getValue();

            return Streams.stream(Iterables.partition(batchTests, partitionSize))
                          .map(partition -> new GameTestBatch(
                              string + ":" + batchNr.incrementAndGet(),
                              partition.stream().map(func -> new GameTestInfo(
                                  func,
                                  Rotation.NONE,
                                  level,
                                  RetryOptions.noRetries()
                              )).toList(),
                              before, after
                          ));
        }).collect(ImmutableList.toImmutableList());
    }


    private DebugGameTestServer(Thread thread, File serverDir, LevelStorageSource.LevelStorageAccess lsa, PackRepository datapacks, WorldStem stem, RuntimeTestConfig config) {
        super(thread, lsa, datapacks, stem, Proxy.NO_PROXY, DataFixers.getDataFixer(), TEST_SERVICES, LoggerChunkProgressListener::createFromGameruleRadius);
        this.config = config;
        this.serverDir = serverDir;
    }

    @Override
    public boolean initServer() {
        setPlayerList(new PlayerList(this, registries(), playerDataStorage, 1) {
            // N/A
        });


        loadLevel();

        String levelName = config.dimension();
        testLevel = getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(levelName)));
        if (testLevel == null) {
            throw new RuntimeException("There is no such dimension named '" + levelName + "'");
        }

        Collection<GameTestBatch> tests = groupTestsIntoBatches(testLevel, config.filteredTests(), config.maxSimultaneous());
        if (tests.isEmpty()) {
            throw new RuntimeException("No test batches were given!");
        }
        this.testBatches = Lists.newArrayList(tests);

        testLevel.setDefaultSpawnPos(config.start(), 0);
        testLevel.setWeatherParameters(20000000, 20000000, false, false);

        LOGGER.info("Started JEDT game test server");
        return true;
    }

    @Override
    public void tickServer(BooleanSupplier booleanSupplier) {
        super.tickServer(booleanSupplier);

        if (!haveTestsStarted()) {
            startTests(testLevel);
        }

        if (testLevel.getGameTime() % PROGRESS_INTERVAL == 0L) {
            LOGGER.info(testTracker.getProgressBar());
        }

        if (testTracker.isDone()) {
            halt(false);
            LOGGER.info(testTracker.getProgressBar());

            GlobalTestReporter.finish();

            LOGGER.info("========= {} GAME TESTS COMPLETE IN {} ======================", this.testTracker.getTotalCount(), this.stopwatch.stop());
            if (testTracker.hasFailedRequired()) {
                LOGGER.info("{} required tests failed :(", testTracker.getFailedRequiredCount());

                testTracker.getFailedRequired().forEach(
                    test -> LOGGER.info("   - {}", test.getTestName())
                );
            } else {
                LOGGER.info("All {} required tests passed :)", this.testTracker.getTotalCount());
            }

            if (testTracker.hasFailedOptional()) {
                LOGGER.info("{} optional tests failed", testTracker.getFailedOptionalCount());

                testTracker.getFailedOptional().forEach(
                    test -> LOGGER.info("   - {}", test.getTestName())
                );
            }

            LOGGER.info("====================================================");
        }

    }

    private final LocalSampleLogger sampleLogger = new LocalSampleLogger(4);

    @Override
    protected SampleLogger getTickTimeLogger() {
        return sampleLogger;
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return false;
    }

    @Override
    public void waitUntilNextTick() {
        runAllTasks();
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport report) {
        report.setDetail("Type", "JEDT Game test server");
        return report;
    }

    @Override
    public void onServerExit() {
        super.onServerExit();

        config.exportPath(serverDir.toPath()).ifPresent(path -> {
            LOGGER.info("Exporting test world as zip to " + path);
            GameTestCIUtil.exportTestWorldAsZip(this, path.toFile());
        });

        LOGGER.info("JEDT game test server shutting down");

        GameTestEvents.TEST_SERVER_DONE.invoker().onTestServerDone(this);

        assert testTracker != null;
        System.exit(testTracker.getFailedRequiredCount());
    }

    @Override
    public void onServerCrash(CrashReport report) {
        super.onServerCrash(report);

        config.exportPath(serverDir.toPath()).ifPresent(path -> {
            LOGGER.info("Exporting test world as zip to " + path);
            GameTestCIUtil.exportTestWorldAsZip(this, path.toFile());
        });

        LOGGER.error("JEDT game test server crashed\n{}", report.getFriendlyReport(ReportType.TEST));
        System.exit(1);
    }

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private void startTests(ServerLevel level) {
        BlockPos blockPos = new BlockPos(
            level.random.nextIntBetweenInclusive(-14999992, 14999992), -59, level.random.nextIntBetweenInclusive(-14999992, 14999992)
        );
        GameTestRunner runner = GameTestRunner.Builder.fromBatches(testBatches, level)
                                                      .newStructureSpawner(new StructureGridSpawner(blockPos, 8, false))
                                                      .build();
        Collection<GameTestInfo> tests = runner.getTestInfos();

        testTracker = new MultipleTestTracker(tests);
        LOGGER.info("{} tests are now running!", testTracker.getTotalCount());
        stopwatch.reset();
        stopwatch.start();
        runner.start();
    }

    private boolean haveTestsStarted() {
        return testTracker != null;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return 0;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return 4;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean isEpollEnabled() {
        return false;
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return true;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(GameProfile gameProfile) {
        return false;
    }
}

package org.kynosarges.tektosyne.demo;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

import org.kynosarges.tektosyne.*;
import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides a dialog for benchmarking several Tektosyne algorithms.
 * Runs a selected benchmark suite in a background thread and
 * appends any new results to a scrollable {@link TextArea}.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class BenchmarkDialog extends Stage {

    final static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private Exception _testException;
    private Future<?> _testFuture;

    private final ComboBox<TestCategory> _categories = new ComboBox<>();
    private final TextArea _output = new TextArea();
    private final Button _run, _runAll, _stop;

    /**
     * Creates a {@link BenchmarkDialog}.
     */
    public BenchmarkDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Choose Run All, or select Test and choose Run. Copy output via clipboard.\n" +
                "Tests run on background thread and can be interrupted using Stop or Close.");
        message.setMinHeight(36); // reserve space for two lines
        message.setWrapText(true);

        _categories.getItems().addAll(
                new TestCategory("Geometric Algorithms", () -> {
                    geometryBasicTest();
                    output("\n");
                    geometryTest();
                }),
                new TestCategory("Multiline Intersection", this::intersectionTest),
                new TestCategory("Nearest Point Search", this::nearestPointTest),
                new TestCategory("Range Tree Search", this::rangeTreeTest),
                new TestCategory("Subdivision Intersection", this::subdivisionTest),
                new TestCategory("Subdivision Search", () -> {
                    subdivisionSearchTest(false);
                    output("\n");
                    subdivisionSearchTest(true);
                })
        );
        _categories.getSelectionModel().select(0);
        _categories.setEditable(false);
        _categories.setTooltip(new Tooltip("Select test to run (Alt+T)"));

        final Label categoryLabel = new Label("_Test");
        categoryLabel.setLabelFor(_categories);
        categoryLabel.setMnemonicParsing(true);
        
        _run = new Button("_Run");
        _run.setDefaultButton(true);
        _run.setOnAction(t -> runTest());
        _run.setTooltip(new Tooltip("Run selected benchmark (Alt+R)"));

        _runAll = new Button("Run _All");
        _runAll.setOnAction(t -> runAllTests());
        _runAll.setTooltip(new Tooltip("Run all benchmarks (Alt+A)"));

        final HBox selection = new HBox(categoryLabel, _categories, _run, _runAll);
        selection.setAlignment(Pos.CENTER);
        selection.setSpacing(4);
        HBox.setMargin(_run, new Insets(0, 8, 0, 8));

        _output.setEditable(true); // for visibility and selection
        _output.setFont(Font.font("Monospaced"));
        _output.setPrefRowCount(12);
        _output.setWrapText(false);

        _stop = new Button("_Stop");
        _stop.setDisable(true);
        _stop.setOnAction(t -> stopTest());
        _stop.setTooltip(new Tooltip("Stop benchmark in progress (Alt+S)"));

        final Button close = new Button("Close");
        close.setCancelButton(true);
        close.setOnAction(t -> { stopTest(); close(); });
        close.setTooltip(new Tooltip("Close dialog (Escape, Alt+F4)"));

        final HBox controls = new HBox(_stop, close);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(8);

        final VBox root = new VBox(message, selection, _output, controls);
        root.setPadding(new Insets(8));
        root.setSpacing(8);
        VBox.setVgrow(_output, Priority.ALWAYS);

        setResizable(true);
        setScene(new Scene(root));
        setTitle("Benchmark Tests");
        sizeToScene();
    }

    /**
     * Runs selected test using an {@link #EXECUTOR} thread.
     * Does nothing if a test is already running.
     */
    private void runTest() {
        if (_testFuture != null || EXECUTOR.isShutdown())
            return;

        final TestCategory category = _categories.getSelectionModel().getSelectedItem();
        if (category == null) return;

        output(String.format("%s\nTest Started: %s\n\n", category.name, Instant.now()));
        enableControls(true);
        _testException = null;
        _testFuture = EXECUTOR.submit(() -> {
            try {
                category.action.run();
            } catch (Exception e) {
                _testException = e;
            } finally {
                Platform.runLater(this::endTest);
            }
        });
    }

    /**
     * Runs all available tests using an {@link #EXECUTOR} thread.
     * Does nothing if a test is already running.
     */
    private void runAllTests() {
        if (_testFuture != null || EXECUTOR.isShutdown())
            return;

        enableControls(true);
        _testException = null;
        _testFuture = EXECUTOR.submit(() -> {
            try {
                for (TestCategory category: _categories.getItems()) {
                    output(String.format("\n%s\nTest Started: %s\n\n", category.name, Instant.now()));
                    category.action.run();
                }
            } catch (Exception e) {
                _testException = e;
            } finally {
                Platform.runLater(this::endTest);
            }
        });
    }

    /**
     * Ends test running in an {@link #EXECUTOR} thread.
     * Also shows the stack trace of any exception that occurred.
     * Merely resets the user interface if no test is running.
     */
    private void endTest() {
        if (_testFuture != null) {
            try {
                _testFuture.get();
            } catch (Exception e) {
                _testException = e;
            }

            if (_testException != null) {
                if (_testException instanceof CancellationException)
                    output("Stopped at user request.\n");
                else
                    output(Arrays.toString(_testException.getStackTrace()));
            }
            output(String.format("\nTest Completed: %s\n", Instant.now()));
        }
        
        _testFuture = null;
        _testException = null;
        enableControls(false);
    }

    /**
     * Stops test running in an {@link #EXECUTOR} thread.
     * Requests cancellation which occurs the next time
     * {@link #output} or {@link TestCase#plus} is called.
     */
    private void stopTest() {
        if (_testFuture != null)
            _testFuture.cancel(true);
    }

    /**
     * Enables or disables input controls, depending on whether a test is running.
     * @param running {@code true} if a test is about to run, else {@code false}
     */
    private void enableControls(boolean running) {
        _categories.setDisable(running);
        _run.setDisable(running);
        _runAll.setDisable(running);
        _stop.setDisable(!running);
    }
    
    /**
     * Shows the specified {@link String} in the output {@link TextArea}.
     * Dispatches to the JavaFX application thread and checks for cancellation
     * when called by a test running on an {@link #EXECUTOR} thread.
     * 
     * @param text the {@link String} to show
     * @throws CancellationException if the {@link #EXECUTOR} thread was interrupted
     */
    private void output(String text) {
        if (Platform.isFxApplicationThread()) {
            _output.appendText(text);
            return;
        }

        if (Thread.interrupted())
            throw new CancellationException();

        Platform.runLater(() -> {
            _output.appendText(text);
        });
    }

    // ----- Test Suites -----

    private final static TestCase[] GEOMETRY_BASIC_TESTS = {
        new TestCase("Line Intersection", 2, null),
        new TestCase("Point in Polygon",  2, null),
    };

    private void geometryBasicTest() {
        Instant start, stop;

        final double epsilon = 1e-10;
        final int outerLoop = 10000, innerLoop = 10000;
        final long iterations = outerLoop * innerLoop;

        // clear results
        for (TestCase test: GEOMETRY_BASIC_TESTS)
            test.clearAll();

        for (int i = 0; i < outerLoop; i++) {
            final PointD[] polygon = GeoUtils.randomPolygon(0, 0, 1000, 1000);
            final LineD line = GeoUtils.randomLine(0, 0, 1000, 1000);
            final LineD line2 = GeoUtils.randomLine(0, 0, 1000, 1000);
            final PointD q = GeoUtils.randomPoint(0, 0, 1000, 1000);

            // preload code
            if (i == 0) {
                GeoUtils.pointInPolygon(q, polygon);
                GeoUtils.pointInPolygon(q, polygon, epsilon);
                line.intersect(line2);
                line.intersect(line2, epsilon);
            }

            start = Instant.now();
            for (int j = 0; j < innerLoop; j++)
                line.intersect(line2);
            stop = Instant.now();
            GEOMETRY_BASIC_TESTS[0].plus(0, start, stop);

            start = Instant.now();
            for (int j = 0; j < innerLoop; j++)
                line.intersect(line2, epsilon);
            stop = Instant.now();
            GEOMETRY_BASIC_TESTS[0].plus(1, start, stop);

            start = Instant.now();
            for (int j = 0; j < innerLoop; j++)
                GeoUtils.pointInPolygon(q, polygon);
            stop = Instant.now();
            GEOMETRY_BASIC_TESTS[1].plus(0, start, stop);

            start = Instant.now();
            for (int j = 0; j < innerLoop; j++)
                GeoUtils.pointInPolygon(q, polygon, epsilon);
            stop = Instant.now();
            GEOMETRY_BASIC_TESTS[1].plus(1, start, stop);
        }

        output("                  ");
        output(String.format("%12s", "Exact"));
        output(String.format("%12s", "Epsilon"));

        output(String.format("\n%18s", GEOMETRY_BASIC_TESTS[0].name));
        output(String.format("%,12.2f", GEOMETRY_BASIC_TESTS[0].averageNanos(0, iterations)));
        output(String.format("%,12.2f", GEOMETRY_BASIC_TESTS[0].averageNanos(1, iterations)));

        output(String.format("\n%18s", GEOMETRY_BASIC_TESTS[1].name));
        output(String.format("%,12.2f", GEOMETRY_BASIC_TESTS[1].averageNanos(0, iterations)));
        output(String.format("%,12.2f", GEOMETRY_BASIC_TESTS[1].averageNanos(1, iterations)));

        output("\n\nTimes are nsec averages for exact and epsilon comparisons.\n");
        output("Point in Polygon uses random polygons with 3-60 vertices.\n");
    }

    private final static TestCase[] GEOMETRY_TESTS = {
        new TestCase("ConvexHull", 1, (Consumer<PointD[]>) GeoUtils::convexHull),
        new TestCase("Voronoi",    1, (Consumer<PointD[]>) Voronoi::findAll),
        new TestCase("Delaunay",   1, (Consumer<PointD[]>) Voronoi::findDelaunay)
    };

    @SuppressWarnings("unchecked")
    private void geometryTest() {
        Instant start, stop;

        output("      ");
        for (TestCase test: GEOMETRY_TESTS)
            output(String.format("%12s", test.name));
        output("\n");

        final int outerLoop = 100, innerLoop = 100;
        final long iterations = outerLoop * innerLoop;

        for (int size = 10; size <= 120; size += 10) {
            final PointD[] points = new PointD[size];

            for (int i = 0; i < outerLoop; i++) {
                for (int j = 0; j < points.length; j++)
                    points[j] = GeoUtils.randomPoint(0, 0, 1000, 1000);

                for (TestCase test: GEOMETRY_TESTS) {
                    final Consumer<PointD[]> action = (Consumer<PointD[]>) test.value;

                    // preload code and clear results
                    if (i == 0) {
                        action.accept(points);
                        test.clearAll();
                    }

                    start = Instant.now();
                    for (int k = 0; k < innerLoop; k++)
                        action.accept(points);
                    stop = Instant.now();
                    test.plus(0, start, stop);
                }
            }

            output(String.format("%,6d", size));
            for (TestCase test: GEOMETRY_TESTS)
                output(String.format("%,12.2f", test.averageNanos(0, iterations) / 1000));
            output("\n");
        }

        output("\nTimes are µsec averages for point sets of the indicated size.\n");
    }

    private static final TestCase[] INTERSECTION_TESTS = {
        new TestCase("SweepLine",  3, (Consumer<LineD[]>) MultiLineIntersection::find),
        new TestCase("BruteForce", 3, (Consumer<LineD[]>) MultiLineIntersection::findSimple)
    };

    @SuppressWarnings("unchecked")
    private void intersectionTest() {
        Instant start, stop;

        output("      ");
        for (TestCase test: INTERSECTION_TESTS) {
            output(String.format("%26s", test.name));
            output("          ");
        }
        output("\n      ");
        for (TestCase test: INTERSECTION_TESTS) {
            output(String.format("%12s", "0"));
            output(String.format("%12s", "n"));
            output(String.format("%12s", "(n^2)/4"));
        }
        output("\n\n");

        final int iterations = 1000;
        for (int size = 10; size <= 120; size += 10) {

            // create three non-random line sets per test case
            final LineD[][] lines = new LineD[3][];
            for (int i = 0; i < lines.length; i++)
                lines[i] = new LineD[size];

            // Zero intersections: Set of near-horizontal lines
            // Best case for horizontal sweep line = O(n)
            for (int j = 0; j < size; j++) {
                final double y = j * 1000 / size;
                lines[0][j] = new LineD(0, y, 1000, y + 2);
            }

            // Linear number of intersections (n):
            // Set of near-horizontal lines with one vertical line
            lines[1][0] = new LineD(500, 0, 500, 1000);
            for (int j = 1; j < size; j++) {
                final double y = j * 1000 / size;
                lines[1][j] = new LineD(0, y, 1000, y + 2);
            }

            // Quadratic number of intersections (n^2 / 4):
            // Crosshatch of near-horizontal and near-vertical lines
            for (int j = 0; j < size / 2; j++) {
                final double xy = j * 2000 / size;
                lines[2][j] = new LineD(0, xy, 1000, xy + 2);
                lines[2][j + size / 2] = new LineD(xy, 0, xy + 2, 1000);
            }

            for (TestCase test: INTERSECTION_TESTS) {
                test.clearAll();
                final Consumer<LineD[]> action = (Consumer<LineD[]>) test.value;

                // preload code
                action.accept(lines[lines.length - 1]);

                // run each test on all line sets
                for (int j = 0; j < lines.length; j++) {
                    start = Instant.now();
                    for (int i = 0; i < iterations; i++)
                        action.accept(lines[j]);
                    stop = Instant.now();
                    test.plus(j, start, stop);
                }
            }

            output(String.format("%,6d", size));
            for (TestCase test: INTERSECTION_TESTS)
                for (int j = 0; j < lines.length; j++)
                    output(String.format("%,12.2f", test.averageNanos(j, iterations) / 1000));
            output("\n");
        }

        output("\nTimes are µsec averages for line sets of the indicated size,\n");
        output("and with the indicated relative number of intersections.\n");
    }

    @FunctionalInterface
    private interface NearestPointConsumer {
        void accept(PointDComparator comparer, List<PointD> points, PointD query);
    }

    private final static TestCase[] NEAREST_POINT_TESTS = {
        new TestCase("Unsorted", 1, (NearestPointConsumer) (c, p, q) -> GeoUtils.nearestPoint(p, q)),
        new TestCase("Sorted",   1, (NearestPointConsumer) (c, p, q) -> c.findNearest(p, q))
    };

    private void nearestPointTest() {
        Instant start, stop;

        output("      ");
        for (TestCase test: NEAREST_POINT_TESTS)
            output(String.format("%12s", test.name));
        output("\n");

        final int outerLoop = 200, innerLoop = 400;
        final long iterations = outerLoop * innerLoop;

        final PointDComparator comparer = new PointDComparatorY(0);
        final PointD[] query = new PointD[innerLoop];

        for (int size = 1000; size <= 12000; size += 1000) {
            final List<PointD> points = new ArrayList<>(size);
            for (int i = 0; i < size; i++)
                points.add(GeoUtils.randomPoint(0, 0, 1000, 1000));
            points.sort(comparer);

            // preload code and clear results
            for (TestCase test: NEAREST_POINT_TESTS) {
                ((NearestPointConsumer) test.value).accept(comparer, points, PointD.EMPTY);
                test.clearAll();
            }

            for (int j = 0; j < outerLoop; j++) {
                for (int k = 0; k < query.length; k++)
                    query[k] = GeoUtils.randomPoint(0, 0, 1000, 1000);

                for (TestCase test: NEAREST_POINT_TESTS) {
                    final NearestPointConsumer action = (NearestPointConsumer) test.value;
                    start = Instant.now();
                    for (PointD q: query)
                        action.accept(comparer, points, q);
                    stop = Instant.now();
                    test.plus(0, start, stop);
                }
            }

            output(String.format("%,6d", size));
            for (TestCase test: NEAREST_POINT_TESTS)
                output(String.format("%,12.2f", test.averageNanos(0, iterations) / 1000.0));
            output("\n");
        }

        output("\nTimes are µsec averages for point lists of the indicated size.\n");
    }

    private static final TestCase[] RANGE_TREE_TESTS = {
        new TestCase("TreeMap",  5, new TreeMap<PointD, String>(new PointDComparatorY(0))),
        new TestCase("QuadTree", 5, new QuadTree<String>(new RectD(0, 0, 10000, 10000)))
    };

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void rangeTreeTest() {
        Instant start, stop;

        output("        ");
        for (TestCase test: RANGE_TREE_TESTS)
            output(String.format("%12s", test.name));
        output("\n");

        // count units of size x operation in milliseconds,
        // rather than individual operations in microseconds
        final int outerLoop = 10, innerLoop = 10;
        final long iterations = outerLoop * innerLoop * 1000;

        // bounds of search space, size of point set,
        // range & iterations for range search
        final int bounds = 10000, size = 60000;
        final int range = size / 80, rangeIterations = size / 120;

        final PointD[] keys = new PointD[size];
        for (int i = 0; i < outerLoop; i++) {

            // generate random spatial keys
            for (int j = 0; j < keys.length; j++)
                keys[j] = GeoUtils.randomPoint(0, 0, bounds, bounds);

            // preload code and clear results
            if (i == 0)
                for (TestCase test: RANGE_TREE_TESTS) {
                    test.clearAll();
                    final Map<PointD, String> map = (Map<PointD, String>) test.value;
                    map.clear();
                    for (PointD key: keys)
                        map.put(key, null);
                }

            for (int j = 0; j < innerLoop; j++)
                for (TestCase test: RANGE_TREE_TESTS) {
                    final Map<PointD, String> map = (Map<PointD, String>) test.value;
                    map.clear();

                    start = Instant.now();
                    for (PointD key: keys)
                        map.put(key, null);
                    stop = Instant.now();
                    test.plus(0, start, stop);

                    double sum = 0;
                    start = Instant.now();
                    for (PointD key: map.keySet())
                        sum += key.x;
                    stop = Instant.now();
                    test.plus(1, start, stop);

                    start = Instant.now();
                    for (PointD key: keys)
                        map.containsKey(key);
                    stop = Instant.now();
                    test.plus(2, start, stop);

                    final TreeMap treeMap = (map instanceof TreeMap ? (TreeMap<PointD, String>) map : null);
                    final QuadTree quadTree = (map instanceof QuadTree ? (QuadTree<String>) map: null);

                    start = Instant.now();
                    for (int l = 0; l < keys.length; l += size / rangeIterations) {
                        final PointD p = keys[l];
                        final RectD rect = new RectD(p.x, p.y, p.x + range, p.y + range);

                        if (treeMap != null)
                            ((PointDComparator) treeMap.comparator()).findRange(treeMap, rect);
                        else if (quadTree != null)
                            quadTree.findRange(rect);
                    }
                    stop = Instant.now();
                    test.plus(3, start, stop);

                    start = Instant.now();
                    for (PointD key: keys)
                        map.remove(key);
                    stop = Instant.now();
                    test.plus(4, start, stop);
                }
        }

        output(String.format("%8s", "Add"));
        for (TestCase test: RANGE_TREE_TESTS)
            output(String.format("%,12.2f", test.averageNanos(0, iterations) / 1000));

        output(String.format("\n%8s", "Iterate"));
        for (TestCase test: RANGE_TREE_TESTS)
            output(String.format("%,12.2f", test.averageNanos(1, iterations) / 1000));

        output(String.format("\n%8s", "Search"));
        for (TestCase test: RANGE_TREE_TESTS)
            output(String.format("%,12.2f", test.averageNanos(2, iterations) / 1000));

        output(String.format("\n%8s", "Range"));
        for (TestCase test: RANGE_TREE_TESTS)
            output(String.format("%,12.2f", test.averageNanos(3, iterations) / 1000));

        output(String.format("\n%8s", "Remove"));
        for (TestCase test: RANGE_TREE_TESTS)
            output(String.format("%,12.2f", test.averageNanos(4, iterations) / 1000));

        final double share = range / (double) bounds;
        output(String.format("\n\nTimes are µsec averages for %,d random points.\n", size));
        output(String.format("Range search performs %,d iterations on %.2f%% of search space.\n",
            rangeIterations, share * share * 100));
    }

    private static final TestCase[] SUBDIVISION_TESTS =  {
        new TestCase("0%–100%", 1, 0.0),
        new TestCase("10%–90%", 1, 0.1),
        new TestCase("50%–50%", 1, 0.5),
        new TestCase("90%–10%", 1, 0.9),
        new TestCase("100%–0%", 1, 1.0)
    };

    private void subdivisionTest() {
        Instant start, stop;

        output("      ");
        for (TestCase test: SUBDIVISION_TESTS)
            output(String.format("%12s", test.name));
        output("\n");

        // ensure reasonable vertex spacing
        final double epsilon = 1e-6;
        final int outerLoop = 20, innerLoop = 40;
        final long iterations = outerLoop * innerLoop;
        final Subdivision[] divisions = new Subdivision[2];

        for (int size = 20; size <= 240; size += 20) {
            for (int j = 0; j < outerLoop; j++) {
                for (TestCase test: SUBDIVISION_TESTS) {
                    final double value = (double) test.value;

                    for (int s = 0; s < 2; s++) {
                        int count = (int) (size * (s == 0 ? value : (1 - value)));
                        divisions[s] = createSubdivision(count, epsilon);
                    }

                    // preload code and clear results
                    if (j == 0) {
                        Subdivision.intersection(divisions[0], divisions[1]);
                        test.clearAll();
                    }

                    start = Instant.now();
                    for (int k = 0; k < innerLoop; k++) {
                        final SubdivisionIntersection result =
                                Subdivision.intersection(divisions[0], divisions[1]);
                        result.division.validate();
                    }
                    stop = Instant.now();
                    test.plus(0, start, stop);
                }
            }

            output(String.format("%,6d", size));
            for (TestCase test: SUBDIVISION_TESTS)
                output(String.format("%,12.2f", test.averageNanos(0, iterations) / 1000));
            output("\n");
        }

        output("\nTimes are µsec averages for two subdivisions with the indicated combined" +
            "\nedge count, distributed as indicated between the two subdivisions.\n");
    }

    private static final TestCase[] SUBDIVISION_SEARCH_TESTS = {
        new TestCase("BruteForce", 1, null),
        new TestCase("Ordered",    1, null),
        new TestCase("Randomized", 1, null)
    };

    @SuppressWarnings("unchecked")
    private void subdivisionSearchTest(boolean random) {
        Instant start, stop;

        output("      ");
        for (TestCase test: SUBDIVISION_SEARCH_TESTS)
            output(String.format("%12s", test.name));
        output("\n");

        final int outerLoop = 200, innerLoop = 400;
        final long iterations = outerLoop * innerLoop;
        final PointD[] query = new PointD[innerLoop];

        PolygonGrid grid = null;
        int sizeMin, sizeMax, sizeStep;
        if (random) {
            sizeMin = 100; sizeMax = 1200; sizeStep = 100;
        } else {
            sizeMin = 6; sizeMax = 30; sizeStep = 2;
            final RegularPolygon polygon = new RegularPolygon(10, 4, PolygonOrientation.ON_EDGE);
            grid = new PolygonGrid(polygon);
        }

        for (int size = sizeMin; size <= sizeMax; size += sizeStep) {
            Subdivision division;
            if (random) {
                // create subdivision from random lines (few faces)
                division = createSubdivision(size, 1e-10);
            } else {
                // create subdivision from grid of diamonds (many faces)
                grid.setElement(new RegularPolygon(900.0 / size, 4, PolygonOrientation.ON_EDGE));
                grid.setSize(new SizeI(size, size));
                division = new PolygonGridMap(grid, PointD.EMPTY, 0).source();
            }
            final SubdivisionSearch ordered = new SubdivisionSearch(division, true);
            final SubdivisionSearch randomized = new SubdivisionSearch(division, false);

            // test cases: BruteForce, Ordered, Randomized
            SUBDIVISION_SEARCH_TESTS[0].value = (Function<PointD, SubdivisionElement>)
                    (q -> division.find(q, division.epsilon));
            SUBDIVISION_SEARCH_TESTS[1].value = (Function<PointD, SubdivisionElement>) (q -> ordered.find(q));
            SUBDIVISION_SEARCH_TESTS[2].value = (Function<PointD, SubdivisionElement>) (q -> randomized.find(q));

            // preload code and clear results
            for (TestCase test: SUBDIVISION_SEARCH_TESTS) {
                ((Function<PointD, SubdivisionElement>) test.value).apply(PointD.EMPTY);
                test.clearAll();
            }

            for (int j = 0; j < outerLoop; j++) {
                for (int k = 0; k < query.length; k++)
                    query[k] = GeoUtils.randomPoint(0, 0, 1000, 1000);

                for (TestCase test: SUBDIVISION_SEARCH_TESTS) {
                    final Function<PointD, SubdivisionElement> action =
                            (Function<PointD, SubdivisionElement>) test.value;

                    start = Instant.now();
                    for (PointD q: query) {
                        action.apply(q);
                    }
                    stop = Instant.now();
                    test.plus(0, start, stop);
                }
            }

            output(String.format("%,6d", division.edges().size() / 2));
            for (TestCase test: SUBDIVISION_SEARCH_TESTS)
                output(String.format("%,12.2f", test.averageNanos(0, iterations) / 1000));
            output("\n");
        }

        output("\nTimes are µsec averages for subdivisions of the indicated edge count,\n");
        if (random)
            output("based on random line sets (few faces, completely random edges).\n");
        else
            output("based on grids of squares (many faces, strictly ordered edges).\n");
    }

    /**
     * Creates a random {@link Subdivision} with the specified number of full edges and comparison epsilon.
     * @param size the number of full edges, i.e. half the number of {@link Subdivision#edges}
     *             in the returned {@link Subdivision}
     * @param epsilon the maximum absolute difference at which coordinates are considered equal
     * @return a random {@link Subdivision} with the specified {@code size} and {@code epsilon}
     */
    private static Subdivision createSubdivision(int size, double epsilon) {
        final LineD[] lines = new LineD[size];
        for (int i = 0; i < size; i++)
            lines[i] = GeoUtils.randomLine(0, 0, 1000, 1000);

        // split random set into non-intersecting line segments
        final MultiLinePoint[] crossings = MultiLineIntersection.findSimple(lines, epsilon);
        final LineD[] splitLines = MultiLineIntersection.split(lines, crossings);
        System.arraycopy(splitLines, 0, lines, 0, size);

        // re-randomize lines to eliminate split ordering
        Collections.shuffle(Arrays.asList(lines));
        final Subdivision division = Subdivision.fromLines(lines, epsilon);
        division.validate();
        return division;
    }

    /**
     * Defines one of multiple test cases within a {@link TestCategory}.
     * Each {@link TestCase} offers multiple {@link Duration} counters
     * whose number is specified during construction.
     */
    private static class TestCase {

        /** The name of the {@link TestCase}. */
        final String name;

        /** The {@link Duration} counters tracked by the {@link TestCase}. */
        final Duration[] counters;

        /**
         * An optional {@link Object} required by the {@link TestCase}.
         * May be {@code null}, or set while the {@link TestCase} is being run.
         */
        Object value;

        /**
         * Creates a {@link TestCase}.
         * @param name the name of the {@link TestCase}
         * @param count the total number of {@link Duration} counters to track
         * @param value an optional {@link Object} required by the {@link TestCase}
         * @throws IllegalArgumentException if {@code count} is less than zero
         */
        TestCase(String name, int count, Object value) {
            this.name = name;
            this.counters = new Duration[count];
            Arrays.fill(this.counters, Duration.ZERO);
            this.value = value;
        }

        /**
         * Computes the average number of nanoseconds for the specified {@link Duration} counter,
         * assuming the specified number of test iterations.
         * 
         * @param index the index of the {@link Duration} counter to examine
         * @param iterations the total number of iterations that were counted
         * @return the average number of nanoseconds for the indicated counter
         * @throws ArrayIndexOutOfBoundsException if {@code index} is less than zero
         *         or equal to or greater than the total number of counters
         */
        double averageNanos(int index, long iterations) {
            return counters[index].toNanos() / (double) iterations;
        }

        /**
         * Clears the specified {@link Duration} counter.
         * @param index the index of the {@link Duration} counter to clear
         * @throws ArrayIndexOutOfBoundsException if {@code index} is less than zero
         *         or equal to or greater than the total number of counters
         */
        void clear(int index) {
            counters[index] = Duration.ZERO;
        }

        /**
         * Clears all {@link Duration} counters.
         */
        void clearAll() {
            Arrays.fill(counters, Duration.ZERO);
        }
        
        /**
         * Adds the specified time interval to the specified {@link Duration} counter.
         * Also checks for cancellation of a test running on the {@link #EXECUTOR} thread.
         * 
         * @param index the index of the {@link Duration} counter to increase
         * @param start the {@link Instant} marking the start of the time interval
         * @param stop the {@link Instant} marking the end of the time interval
         * @throws ArrayIndexOutOfBoundsException if {@code index} is less than zero
         *         or equal to or greater than the total number of counters
         * @throws CancellationException if the {@link #EXECUTOR} thread was interrupted
         * @throws NullPointerException if {@code start} or {@code stop} is {@code null}
         */
        void plus(int index, Instant start, Instant stop) {
            if (Thread.interrupted())
                throw new CancellationException();

            counters[index] = counters[index].plus(Duration.between(start, stop));
        }
        
        /**
         * Returns a {@link String} representation of the {@link TestCase}.
         * @return the {@link #name} of the {@link TestCase}
         */
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Defines a test category selectable by the user.
     */
    private static class TestCategory {

        /** The name of the {@link TestCategory}. */
        final String name;

        /** The {@link Runnable} that starts the associated tests. */
        final Runnable action;

        /**
         * Creates a {@link TestCategory}.
         * @param name the name of the {@link TestCategory}
         * @param action a {@link Runnable} that starts the associated tests
         */
        TestCategory(String name, Runnable action) {
            this.name = name;
            this.action = action;
        }

        /**
         * Returns a {@link String} representation of the {@link TestCategory}.
         * @return the {@link #name} of the {@link TestCase}
         */
        @Override
        public String toString() {
            return name;
        }
    }
}

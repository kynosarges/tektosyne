package org.kynosarges.tektosyne;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides unit tests for class {@link QuadTree}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked"})
public class QuadTreeTest {

    QuadTree<String> _tree;
    PointD firstKey = new PointD(0, 0),
        secondKey = new PointD(50, 0),
        thirdKey = new PointD(0, 50),
        fourthKey = new PointD(50, 50),
        invalidKey = new PointD(0, 300);

    @Before
    public void create() {
        _tree = new QuadTree<String>(new RectD(-100, -100, 100, 100));
        assertEquals(0, _tree.size());
        assertEquals(1, _tree.nodes().size());

        _tree.put(firstKey, "first value");
        _tree.put(secondKey, "second value");
        _tree.put(thirdKey, "third value");

        assertEquals(3, _tree.size());
        assertEquals(1, _tree.nodes().size());
    }

    @After
    public void delete() {
        _tree.clear();
        _tree = null;
    }

    @Test
    public void testConstructor() {
        final QuadTree<String> tree = new QuadTree<>(new RectD(0, 0, 100, 100));
        tree.put(new PointD(10, 10), "foo value");
        tree.put(new PointD(20, 20), "bar value");
        assertEquals(2, tree.size());

        final QuadTree<String> clone = new QuadTree<String>(new RectD(0, 0, 100, 100), tree);
        assertEquals(2, clone.size());
    }

    @Test
    public void testCount() {
        _tree.put(new PointD(-50, 0), "foo value");
        _tree.put(new PointD(0, -50), "bar value");
        assertEquals(5, _tree.size());

        _tree.remove(new PointD(0, -50));
        assertEquals(4, _tree.size());
    }

    @Test
    public void testKeys() {
        assertEquals(_tree.size(), _tree.keySet().size());
        for (PointD key: _tree.keySet())
            assertTrue(_tree.containsKey(key));
    }

    @Test
    public void testValues() {
        assertEquals(_tree.size(), _tree.values().size());
        for (String value: _tree.values())
            assertTrue(_tree.containsValue(value));
    }

    @Test
    public void testAdd() {
        assertEquals("second value", _tree.put(secondKey, "another second value"));
        assertEquals("another second value", _tree.get(secondKey));
        assertEquals(null, _tree.put(fourthKey, null));
        assertEquals(null, _tree.get(fourthKey));

        try {
            _tree.put(invalidKey, "invalid key");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void testAddEntry() {
        assertFalse(_tree.entrySet().add(createEntry(secondKey, "second value")));
        assertTrue(_tree.entrySet().add(createEntry(secondKey, "another second value")));
        assertEquals("another second value", _tree.get(secondKey));

        assertTrue(_tree.entrySet().add(createEntry(fourthKey, "fourth value")));
        assertEquals(4, _tree.size());
        assertTrue(_tree.containsKey(fourthKey));
        assertTrue(_tree.containsValue("fourth value"));
    }

    @Test
    public void testAddRange() {
        final QuadTree<String> tree = new QuadTree<String>(_tree.bounds);
        tree.put(fourthKey, "fourth value");
        tree.put(new PointD(-50, -50), "fifth value");
        _tree.putAll(tree);

        assertEquals(5, _tree.size());
        assertTrue(_tree.containsKey(fourthKey));
        assertTrue(_tree.containsValue("fourth value"));
        assertTrue(_tree.containsKey(new PointD(-50, -50)));
        assertTrue(_tree.containsValue("fifth value"));
    }

    @Test
    public void testClear() {
        _tree.clear();
        assertEquals(0, _tree.size());
        assertEquals(0, _tree.rootNode.entries().size());

        assertNull(_tree.rootNode.minXminY());
        assertNull(_tree.rootNode.minXmaxY());
        assertNull(_tree.rootNode.maxXminY());
        assertNull(_tree.rootNode.maxXmaxY());
    }

    @Test
    public void testContains() {
        assertTrue(_tree.entrySet().contains(createEntry(firstKey, "first value")));
        assertTrue(_tree.entrySet().contains(createEntry(secondKey, "second value")));
        assertTrue(_tree.entrySet().contains(createEntry(thirdKey, "third value")));

        assertFalse(_tree.entrySet().contains(createEntry(firstKey, "second value")));
        assertFalse(_tree.entrySet().contains(createEntry(fourthKey, "fourth value")));
        assertFalse(_tree.entrySet().contains(createEntry(invalidKey, null)));
    }

    @Test
    public void testContainsKey() {
        assertTrue(_tree.containsKey(firstKey));
        assertTrue(_tree.containsKey(secondKey));
        assertTrue(_tree.containsKey(thirdKey));

        assertFalse(_tree.containsKey(fourthKey));
        assertFalse(_tree.containsKey(invalidKey));
    }

    @Test
    public void testContainsValue() {
        assertTrue(_tree.containsValue("first value"));
        assertTrue(_tree.containsValue("second value"));
        assertTrue(_tree.containsValue("third value"));

        assertFalse(_tree.containsValue("fourth value"));
        assertFalse(_tree.containsValue(null));
    }

    @Test
    public void testContainsWithNode() {
        final QuadTree.Node<String>[] nodes = new QuadTree.Node[] {
            null,
            _tree.findNode(firstKey),
            _tree.findNode(secondKey),
            _tree.findNode(thirdKey)
        };

        assertNotNull(nodes[1]);
        assertNotNull(nodes[2]);
        assertNotNull(nodes[3]);

        QuadTree.Node<String> node;
        for (QuadTree.Node<String> startNode: nodes) {
            node = startNode; assertTrue(_tree.containsKey(firstKey, node));
            node = startNode; assertTrue(_tree.containsKey(secondKey, node));
            node = startNode; assertTrue(_tree.containsKey(thirdKey, node));

            node = startNode; assertFalse(_tree.containsKey(fourthKey, node));
            node = startNode; assertFalse(_tree.containsKey(invalidKey, node));

            node = startNode; assertTrue(_tree.containsValue("first value", node));
            node = startNode; assertTrue(_tree.containsValue("second value", node));
            node = startNode; assertTrue(_tree.containsValue("third value", node));

            node = startNode; assertFalse(_tree.containsValue("fourth value", node));
            node = startNode; assertFalse(_tree.containsValue(null, node));
        }
    }

    @Test
    public void testCopyTo() {
        final Map.Entry<PointD, String>[] array = new Map.Entry[4];
        try {
            _tree.copyTo(array, 3);
            fail("expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) { }

        _tree.copyTo(array, 0);
        for (int i = 0; i < _tree.size(); i++)
            assertTrue(_tree.entrySet().contains(array[i]));

        _tree.copyTo(array, 1);
        for (int i = 0; i < _tree.size(); i++)
            assertTrue(_tree.entrySet().contains(array[i + 1]));
    }

    @Test
    public void testCopyToEmpty() {
        _tree.clear();
        final Map.Entry<PointD, String>[] array = new Map.Entry[0];
        _tree.copyTo(array, 0);
    }

    @Test
    public void testEquals() {
        final QuadTree<String> tree = new QuadTree<String>(_tree.bounds);
        tree.put(firstKey, "first value");
        tree.put(secondKey, "second value");
        tree.put(thirdKey, "third value");
        assertTrue(_tree.equals(tree));

        tree.put(secondKey, "foo value");
        assertFalse(_tree.equals(tree));

        tree.put(secondKey, "second value");
        assertTrue(_tree.equals(tree));

        tree.put(fourthKey, "second value");
        assertFalse(_tree.equals(tree));
    }

    @Test
    public void testFindRangeCircle() {
        assertTrue(_tree.findRange(new PointD(250, 250), 50).isEmpty());
        assertTrue(_tree.findRange(new PointD(75, 75), 25).isEmpty());

        Map<PointD, String> output = _tree.findRange(new PointD(5, 5), 10);
        assertEquals(1, output.size());
        assertEquals("first value", output.get(firstKey));

        output = _tree.findRange(new PointD(0, 50), 60);
        assertEquals(2, output.size());
        assertEquals("first value", output.get(firstKey));
        assertEquals("third value", output.get(thirdKey));
    }

    @Test
    public void testFindRangeRect() {
        assertTrue(_tree.findRange(new RectD(200, 200, 300, 300)).isEmpty());
        assertTrue(_tree.findRange(new RectD(50, 50, 150, 150)).isEmpty());

        Map<PointD, String> output = _tree.findRange(new RectD(0, 0, 20, 20));
        assertEquals(1, output.size());
        assertEquals("first value", output.get(firstKey));

        output = _tree.findRange(new RectD(0, 0, 50, 50));
        assertEquals(3, output.size());
        assertEquals("first value", output.get(firstKey));
        assertEquals("second value", output.get(secondKey));
        assertEquals("third value", output.get(thirdKey));
    }

    @Test
    public void testGet() {
        assertEquals("first value", _tree.get(firstKey));
        assertEquals(null, _tree.get(invalidKey));
    }

    @Test
    public void testIterator() {
        // iteration sequence is arbitrary, test uses observed values
        final Iterator<Map.Entry<PointD, String>> iter = _tree.entrySet().iterator();
        assertTrue(iter.hasNext());
        assertEquals(createEntry(firstKey, "first value"), iter.next());
        assertTrue(iter.hasNext());
        assertEquals(createEntry(thirdKey, "third value"), iter.next());
        assertTrue(iter.hasNext());
        assertEquals(createEntry(secondKey, "second value"), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMove() {
        QuadTree.Node<String> node = _tree.move(firstKey, new PointD(10, 10), null);
        assertFalse(_tree.containsKey(firstKey));
        assertEquals("first value", _tree.get(new PointD(10, 10)));

        node = _tree.move(secondKey, new PointD(30, 30), node);
        assertFalse(_tree.containsKey(secondKey));
        assertEquals("second value", _tree.get(new PointD(30, 30)));
    }

    @Test
    public void testRemove() {
        assertEquals("second value", _tree.remove(secondKey));
        assertNull(_tree.remove(fourthKey));
        assertEquals(2, _tree.size());

        assertEquals("first value", _tree.remove(firstKey));
        assertEquals("third value", _tree.remove(thirdKey));
        assertEquals(0, _tree.size());
    }

    @Test
    public void testRemovePair() {
        assertTrue(_tree.entrySet().remove(createEntry(secondKey, "second value")));
        assertFalse(_tree.entrySet().remove(createEntry(fourthKey, "fourth value")));
        assertEquals(2, _tree.size());
    }

    @Test
    public void testToArray() {
        final Map.Entry<PointD, String>[] array = _tree.entrySet().toArray(new Map.Entry[_tree.size()]);
        assertEquals(_tree.size(), array.length);
        for (int i = 0; i < _tree.size(); i++)
            assertTrue(_tree.entrySet().contains(array[i]));
    }

    @Test
    public void testWalkTree() {
        final int radius = 3000;
        final Map.Entry<PointD, String>[] array = new Map.Entry[2 * radius];
        for (int i = 0; i < array.length; i++)
            array[i] = createEntry(
                new PointD(i - radius, radius - i),
                new String(String.format("bar%3d value", i)));

        final QuadTree<String> tree = new QuadTree<String>(
            new RectD(-2 * radius, -2 * radius, 2 * radius, 2 * radius));
        assertEquals(tree, tree.rootNode.owner());
        assertEquals(1, tree.nodes().size());

        // test adding elements
        for (Map.Entry<PointD, String> entry: array)
            tree.put(entry.getKey(), entry.getValue());
        assertEquals(array.length, tree.size());
        assertEquals(145, tree.nodes().size());

        // test moving elements without hint node
        final PointD offset = new PointD(0.1, 0.1);
        for (Map.Entry<PointD, String> entry: array)
            tree.move(entry.getKey(), entry.getKey().add(offset), null);

        // test moving elements with hint node
        QuadTree.Node<String> node = null;
        for (Map.Entry<PointD, String> entry: array)
            node = tree.move(entry.getKey().add(offset), entry.getKey(), node);

        assertEquals(array.length, tree.size());
        assertEquals(145, tree.nodes().size());

        // test finding elements
        for (Map.Entry<PointD, String> entry: array) {
            String value = tree.get(entry.getKey());
            assertNotNull(value);
            assertEquals(entry.getValue(), value);

            node = tree.findNode(entry.getKey());
            assertEquals(tree, node.owner());
            assertTrue(node.entries().entrySet().contains(entry));

            final QuadTree.Node<String> valueNode = tree.findNodeByValue(entry.getValue());
            assertEquals(node, valueNode);
        }

        // test finding elements in range
        final RectD range = new RectD(-radius, 0, radius, 2 * radius);
        final Map<PointD, String> rangeMap = tree.findRange(range);
        assertEquals(radius + 1, rangeMap.size());

        for (int i = 0; i <= radius; i++) {
            final String value = rangeMap.get(array[i].getKey());
            assertNotNull(value);
            assertEquals(array[i].getValue(), value);
        }

        // compare range search to TreeMap via PointDComparator
        final PointDComparatorY cmp = new PointDComparatorY(0);
        final TreeMap<PointD, String> treeMap = new TreeMap<>(cmp);
        for (Map.Entry<PointD, String> entry: array)
            treeMap.put(entry.getKey(), entry.getValue());

        final NavigableMap<PointD, String> rangeTreeMap = cmp.findRange(treeMap, range);
        assertEquals(rangeMap, rangeTreeMap);

        // test element enumeration against full TreeMap
        for (Map.Entry<PointD, String> entry: tree.entrySet()) {
            final String value = treeMap.get(entry.getKey());
            assertNotNull(value);
            assertEquals(value, entry.getValue());
            treeMap.remove(entry.getKey());
        }
        assertEquals(0, treeMap.size());

        // test removing elements
        for (Map.Entry<PointD, String> entry: array)
            assertNotNull(tree.remove(entry.getKey()));

        assertEquals(0, tree.size());
        assertEquals(1, tree.nodes().size());
    }
    
    private static Map.Entry<PointD, String> createEntry(PointD key, String value) {
        return new AbstractMap.SimpleEntry<PointD, String>(key, value);
    }
}

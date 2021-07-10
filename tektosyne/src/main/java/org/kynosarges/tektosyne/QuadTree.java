package org.kynosarges.tektosyne;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a generic collection of {@link PointD} keys and arbitrary values
 * that are sorted in two dimensions using a quadrant tree.
 * Provides a two-dimensional search tree with {@link PointD} keys. The root node
 * covers a specified rectangle (not necessarily a square), and each child node
 * recursively covers one quadrant of its parent rectangle. Each leaf node holds
 * one or more key-and-value pairs in a hashtable. Internal nodes hold no data.
 * <ul><li><p>
 * The tree structure is exposed through the {@link Node} class. You can find the
 * node associated with any given key, or with any given tree level and quadrant
 * grid coordinates, and follow links to its four descendants and parent node.
 * </p></li><li><p>
 * All tree nodes have a unique signature that doubles as their key in a hashtable,
 * providing fast tree-wide enumeration and O(1) access to any node with a given level
 * and quadrant grid coordinates. {@link QuadTree#findNode(PointD)} exploits this fact
 * for a depth probe algorithm that greatly shortens search times in large trees.
 * </p></li><li><p>
 * {@link QuadTree#findRange} performs a two-dimensional range search that finds
 * all elements within a given rectangular or circular key range.
 * </p></li></ul>
 * <p>
 * Java Collections Framework features are provided by extending {@link AbstractMap},
 * and {@link AbstractSet} for key-and-value entries. Keys cannot be {@code null} but
 * values may be {@code null}. Iterators do not support modification, and do not check
 * for concurrent external modifications.</p>
 * <p>
 * {@link QuadTree} was inspired by the <code>QuadTree</code> class by Michael J. Laszlo,
 * <em>Computational Geometry and Computer Graphics in C++</em>, Prentice Hall 1996, p.231ff.</p>
 * 
 * @param <V> the type of all values in the {@link QuadTree}
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class QuadTree<V> extends AbstractMap<PointD, V> {
    /**
     * The maximum level for any {@link QuadTree}.
     * Holds the zero-based index of the deepest level in any {@link QuadTree}. The maximum
     * total number of levels therefore equals {@link #MAX_LEVEL} + 1, including the
     * {@link #rootNode} at level zero.
     * <p>
     * When a {@link Node} is created on {@link #MAX_LEVEL}, its maximum {@link #capacity}
     * is ignored so that its {@link Node#entries} collection may grow unbounded.</p>
     * <p>
     * {@link #MAX_LEVEL} is fixed at 14 so that each {@link Node} can be uniquely identified by
     * a 32-bit {@link Node#signature} containing a bitwise combination of the following indices:
     * </p><ul><li>
     * The lowest 4 bits contain the node’s {@link Node#level}.
     * </li><li>
     * The middle 14 bits contain the node’s {@link Node#gridX} index.
     * </li><li>
     * The highest 14 bits contain the node’s {@link Node#gridY} index.
     * </li></ul><p>
     * With a {@link #MAX_LEVEL} of 14, the deepest level can hold 16,384 x 16,384 nodes,
     * and the entire {@link QuadTree} can hold 357,913,941 nodes.</p>
     */
    public final static int MAX_LEVEL = 14;

    /**
     * The minimum level at which {@link #findNode(PointD)} begins using a depth probe.
     * {@link #findNode(PointD)} switches from a normal tree search to a heuristic depth probe algorithm
     * when the number of {@link #nodes} in the {@link QuadTree} reaches 4^{@link #PROBE_LEVEL},
     * indicating that a large proportion of {@link #nodes} resides at or below that level.
     * <p>
     * {@link #PROBE_LEVEL} is currently fixed at four, so that the depth probe starts at 256
     * {@link #nodes}. {@link #PROBE_LEVEL} cannot be less than two since the depth probe ascends
     * two levels at a time.</p>
     */
    public final static int PROBE_LEVEL = 4;

    /**
     * The {@link RectD} bounds of all keys in the {@link QuadTree}.
     * Holds a {@link RectD} with positive {@link RectD#width} and {@link RectD#height} 
     * that contains all {@link PointD} keys in the {@link QuadTree}.
     * <p>
     * The {@link QuadTree} always divides both dimensions into the same number of grid cells
     * at each {@link Node#level}, but the dimensions do not have to be equal.</p>
     * <p>
     * Attempting to add a {@link PointD} key outside of {@link #bounds} always throws an
     * {@link IllegalArgumentException}, and searching for such a key always fails.</p>
     */
    public final RectD bounds;

    /**
     * The maximum capacity for the {@link Node#entries} collections
     * of all leaf {@link #nodes} above {@link #MAX_LEVEL}.
     * Usually indicates the maximum number of elements in the {@link Node#entries} collection
     * of a {@link Node}. However, {@link #capacity} is ignored and the collection size is
     * unbounded for any {@link Node} whose {@link Node#level} equals {@link #MAX_LEVEL}.
     */
    public final int capacity;

    /**
     * The fixed {@link Node} at the root of the {@link QuadTree}.
     * Never removed from the {@link QuadTree}. An empty {@link QuadTree} contains only the 
     * {@link #rootNode}. This is the only {@link Node} whose {@link Node#entries} collection
     * may be empty when it has no descendants.
     * <p>
     * All other nodes are descendants of {@link #rootNode}. The chain of {@link Node#parent}
     * links from any other {@link Node} in the {@link QuadTree} ends in the {@link #rootNode},
     * whose own {@link Node#parent} is always {@code null}.</p>
     */
    public final Node<V> rootNode;

    // Map.Entry set implementing collections methods
    private final EntrySet _entrySet = new EntrySet();

    // status of last depth probe conducted by findNode
    private final ProbeStatus _probe = new ProbeStatus();

    // maps signatures to all nodes in the quadtree
    private final HashMap<Integer, Node<V>> _nodes;

    // total number of keys and values stored in nodes
    private int _size;

    /**
     * Creates an empty {@link QuadTree} with the specified {@link #bounds}.
     * {@link #capacity} defaults to 128.
     * 
     * @param bounds the {@link RectD} bounds of all keys in the {@link QuadTree}
     * @throws IllegalArgumentException if the {@link RectD#width} or {@link RectD#height}
     *                                  of {@code bounds} is equal to or less than zero
     * @throws NullPointerException if {@code bounds} is {@code null}
     */
    public QuadTree(RectD bounds) {
        this(bounds, 128);
    }

    /**
     * Creates an empty {@link QuadTree} with the specified {@link #bounds} and {@link #capacity}.
     * @param bounds the {@link RectD} bounds of all keys in the {@link QuadTree}
     * @param capacity the maximum capacity for the {@link Node#entries} collections
     *                 of all leaf {@link #nodes} above {@link #MAX_LEVEL}
     * @throws IllegalArgumentException if {@code capacity} or the {@link RectD#width}
     *         or {@link RectD#height} of {@code bounds} is equal to or less than zero
     * @throws NullPointerException if {@code bounds} is {@code null}
     */
    public QuadTree(RectD bounds, int capacity) {
        if (bounds.width() <= 0)
            throw new IllegalArgumentException("bounds.width <= 0");
        if (bounds.height() <= 0)
            throw new IllegalArgumentException("bounds.height <= 0");
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");

        this.bounds = bounds;
        this.capacity = capacity;

        this._nodes = new HashMap<>();
        this.rootNode = new Node<>(this, 0, null, bounds);
    }

    /**
     * Creates a {@link QuadTree} with the specified {@link #bounds}
     * and initial elements copied from the specified {@link Map}.
     * {@link #capacity} defaults to 128. Calls {@link #putAll} to copy the
     * elements of the specified {@code map} into the {@link QuadTree}.
     * 
     * @param bounds the {@link RectD} bounds of all keys in the {@link QuadTree}
     * @param map the {@link Map} providing initial elements for the {@link QuadTree}
     * @throws IllegalArgumentException if the {@link RectD#width} or {@link RectD#height}
     *         of {@code bounds} is equal to or less than zero, or if {@code map} contains
     *         a {@link PointD} key that lies outside of {@code bounds}
     * @throws NullPointerException if {@code bounds} or {@code map} is {@code null}
     */
    public QuadTree(RectD bounds, Map<PointD, V> map) {
        this(bounds, 128);
        putAll(map);
    }

    /**
     * Determines whether the {@link QuadTree} contains the specified key, 
     * searching the specified {@link Node} first.
     * Generally succeeds if {@link #findNode(PointD)} finds a {@link Node}
     * that contains the specified {@code key}.
     * <p>
     * However, if the specified {@code node} is a valid leaf node that contains
     * {@code key}, {@code containsKey} succeeds immediately without calling
     * {@link #findNode(PointD)}, resulting in an O(1) operation.</p>
     * 
     * @param key the {@link PointD} key to find
     * @param node an optional {@link Node} to search for {@code key}
     *             before searching the entire {@link QuadTree}
     * @return {@code true} if the {@link QuadTree} contains {@code key}, else {@code false}
     * @throws IllegalArgumentException if {@code node} is not {@code null},
     *         and its {@link Node#owner} differs from the {@link QuadTree}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public boolean containsKey(PointD key, Node<V> node) {
        if (key == null)
            throw new NullPointerException("key");

        if (checkLeaf(node) && node._entries.containsKey(key))
            return true;

        node = findNode(key);
        return (checkLeaf(node) && node._entries.containsKey(key));
    }

    /**
     * Determines whether the {@link QuadTree} contains the specified value.
     * Generally succeeds if {@link #findNodeByValue} finds a {@link Node} that
     * contains the specified {@code value}, which may be {@code null}.
     * <p>
     * However, if the specified {@code node} is a valid leaf node that contains
     * {@code value}, {@code containsValue} succeeds immediately without calling
     * {@link #findNodeByValue}, resulting in an O(k) operation where k is the
     * number of {@link Node#entries} stored in {@code node}.</p>
     * 
     * @param value the <b>V</b> value to find
     * @param node an optional {@link Node} to search for {@code value}
     *             before searching the entire {@link QuadTree}
     * @return {@code true} if the {@link QuadTree} associates {@code value}
     *         with at least one {@link PointD} key, else {@code false}
     * @throws IllegalArgumentException if {@code node} is not {@code null},
     *         and its {@link Node#owner} differs from the {@link QuadTree}
     */
    public boolean containsValue(V value, Node<V> node) {
        if (checkLeaf(node) && node._entries.containsValue(value))
            return true;

        node = findNodeByValue(value);
        return (node != null);
    }

    /**
     * Copies all {@link QuadTree} entries to the specified array,
     * starting at the specified array index.
     * Copies entries in the {@link Iterator} sequence of {@link #entrySet}
     * which is arbitrary.
     * 
     * @param array the array that receives all {@link QuadTree} entries
     * @param arrayIndex the zero-based index in {@code array} where copying begins
     * @throws ArrayIndexOutOfBoundsException if {@code arrayIndex} is less than zero, or
     *         {@code arrayIndex} plus {@link #size} is greater than the length of {@code array}
     * @throws NullPointerException if {@code array} is {@code null}
     */
    public void copyTo(Map.Entry<PointD, V>[] array, int arrayIndex) {
        for (Map.Entry<PointD, V> entry: entrySet())
            array[arrayIndex++] = entry;
    }

    /**
     * Finds the {@link Node} at the specified level and grid coordinates within the {@link QuadTree}.
     * Returns {@code null} in the following cases:
     * <ul><li>
     * {@code level} is less than zero or greater than {@link #MAX_LEVEL}
     * </li><li>
     * {@code gridX} and/or {@code gridY} is less than zero, or equal to or greater
     * than the number of grid cells in the corresponding dimension at {@code level}
     * </li><li>
     * All three arguments are valid, but no {@link Node} exists at the specified location
     * </li></ul>
     * {@code findNode} combines its arguments into the unique {@link Node#signature}
     * of the desired {@link Node} which is used retrieve the node from the {@link #nodes}
     * hashtable. This is an O(1) operation.
     * 
     * @param level the {@link Node#level} to search
     * @param gridX the {@link Node#gridX} coordinate to find
     * @param gridY the {@link Node#gridY} coordinate to find
     * @return the {@link Node} at the specified {@code gridX} and {@code gridY} coordinates
     *         on the specified {@code level}, if any, else {@code null}
     */
    public Node<V> findNode(int level, int gridX, int gridY) {

        // check if level is valid
        if (level < 0 || level > MAX_LEVEL)
            return null;

        // check if grid coordinates are valid for level
        final int sideCount = (1 << level);
        if (gridX < 0 || gridX >= sideCount || gridY < 0 || gridY >= sideCount)
            return null;

        // compose signature from level and grid coordinates
        final int signature = (level | (gridX << 4) | (gridY << 18));
        return _nodes.get(signature);
    }

    /**
     * Finds the {@link Node} within the {@link QuadTree} that <em>should</em> contain the specified key.
     * Does not check if the returned {@link Node} actually contains {@code key}.
     * Returns {@code null} only if {@code key} is outside of {@link #bounds}.
     * <p>
     * Performs a range search for {@code key}, starting with the {@link #rootNode}.
     * This is usually an O(log m) operation where m is the number of {@link #nodes}.</p>
     * <p>
     * If {@link #nodes} contains at least 4^{@link #PROBE_LEVEL} elements,
     * {@code findNode} first probes a deeper level of the {@link QuadTree} to 
     * rapidly approach the {@link Node} containing the specified {@code key}.</p>
     * <p>
     * The probe begins at level log4 m, where m is the number of {@link #nodes},
     * and ascends two levels at a time until either above {@link #PROBE_LEVEL} or
     * a valid {@link Node} is found at the grid coordinates that contain {@code key}.
     * {@code findNode} then performs a regular tree search for the desired leaf node.</p>
     * <p>
     * The depth probe is derived from a binary depth search algorithm given by Sariel Har-Peled
     * in his lecture on 17 March 2010, <em>QuadTrees – Hierarchical Grids</em>. This lecture was
     * part of the series <em>Approximation Algorithms in Geometry</em>, originally available
     * <a href="http://valis.cs.uiuc.edu/~sariel/teach/notes/aprx/">here</a> (but now apparently
     * integrated into a published book).</p>
     * 
     * @param key the {@link PointD} key to find
     * @return the {@link Node} whose {@link Node#bounds} contain {@code key}, if any, else {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public Node<V> findNode(PointD key) {
        if (key == null)
            throw new NullPointerException("key");

        if (!bounds.contains(key)) return null;
        Node<V> node;

        // check if depth probe is worthwhile
        final int count = (_nodes.size() >> (PROBE_LEVEL << 1));
        if (count == 0)
            node = rootNode;
        else {
            // try to reuse last probe data
            int level = _probe.level;
            int gridCount = (1 << level);

            if (count != _probe.nodeCount) {
                if (_probe.useBitMask) {
                    // determine probe level (favors higher node counts)
                    level = ((count & 0xF00000) != 0 ? 14 :
                            ((count & 0xFC0000) != 0 ? 13 :
                            ((count & 0xFF0000) != 0 ? 12 :
                            ((count & 0xFFC000) != 0 ? 11 :
                            ((count & 0xFFF000) != 0 ? 10 :
                            ((count & 0xFFFC00) != 0 ? 9 :
                            ((count & 0xFFFF00) != 0 ? 8 :
                            ((count & 0xFFFFC0) != 0 ? 7 :
                            ((count & 0xFFFFF0) != 0 ? 6 :
                            ((count & 0xFFFFFC) != 0 ? 5 : 4))))))))));
                } else {
                    // determine probe level (favors lower node counts)
                    level = 1;
                    while ((count >> (level << 1)) > 0) level++;
                    level += (PROBE_LEVEL - 1);
                    if (level > MAX_LEVEL) level = MAX_LEVEL;
                }

                _probe.level = level;
                _probe.nodeCount = count;

                // compute grid divisions at specified level
                gridCount = (1 << level);
                _probe.gridWidth = bounds.width() / gridCount;
                _probe.gridHeight = bounds.height() / gridCount;
            }

            // compute grid coordinates for specified point
            int gridX = (int) ((key.x - bounds.min.x) / _probe.gridWidth);
            int gridY = (int) ((key.y - bounds.min.y) / _probe.gridHeight);

            // map extreme bottom/right pixels to bottom/right cell
            if (gridX == gridCount) --gridX;
            if (gridY == gridCount) --gridY;

            assert(gridX >= 0 && gridX < gridCount);
            assert(gridY >= 0 && gridY < gridCount);

            while (true) {
                // compose signature from level and grid coordinates
                final int signature = (level | (gridX << 4) | (gridY << 18));

                /*
                 * Mathematically, a node found by signature should contain the key, but we
                 * need an extra bounds check to ward against floating-point inaccuracies.
                 */

                // probe grid cell at current level for key
                node = _nodes.get(signature);
                if (node != null && node.bounds.containsOpen(key))
                    break;

                // probe parent grid cell two levels up
                if (level < PROBE_LEVEL) {
                    node = rootNode;
                    break;
                }

                level -= 2; gridX >>= 2; gridY >>= 2;
            }
        }

        // perform normal tree search for key
        assert(node != null);
        while (true) {
            final Node<V> child = node.findChild(key);
            if (child == null) return node;
            node = child;
        }
    }

    /**
     * Finds a {@link Node} within the {@link QuadTree} that contains the specified value.
     * Iterates over all {@link #nodes} and then over the {@link Node#entries} of each
     * {@link Node}, until the specified {@code value} is found, which may be {@code null}.
     * This is an O({@link #size}) operation.
     * <p>
     * This iteration sequence is arbitrary, so it is impossible to predict which
     * {@link Node} will be returned if multiple {@link #nodes} contain {@code value}.</p>
     * 
     * @param value the <b>V</b> value to find
     * @return a {@link Node} whose {@link Node#entries} contain {@code value}, if found, else {@code null}
     */
    public Node<V> findNodeByValue(V value) {

        for (Node<V> node: _nodes.values())
            if (node._entries != null && node._entries.containsValue(value))
                return node;

        return null;
    }

    /**
     * Finds all entries in the {@link QuadTree} whose keys lie within the specified circular range.
     * Immediately returns an empty map if the square circumscribed around the indicated
     * key range does not intersect with {@link #bounds}. Otherwise, performs a recursive
     * search starting with the {@link #rootNode}.
     * <p>
     * Depending on the size of the specified {@code radius} relative to {@link #bounds},
     * the runtime of this operation ranges from O(log m) to O({@link #size}), where
     * m is the number of {@link #nodes}.</p>
     * 
     * @param center a {@link PointD} indicating the center of the key range to search
     * @param radius the radius of the key range around {@code center} to search
     * @return a {@link Map} containing all {@link PointD} keys and associated <b>V</b> values
     *         where the key lies within {@code radius} around {@code center}
     * @throws IllegalArgumentException if {@code radius} is less than zero
     * @throws NullPointerException if {@code center} is {@code null}
     */
    public Map<PointD, V> findRange(PointD center, double radius) {
        final RectD range = new RectD(
                center.x - radius, center.y - radius,
                center.x + radius, center.y + radius);

        final Map<PointD, V> map = new HashMap<>();
        if (!bounds.intersectsWith(range)) return map;

        rootNode.findRange(range, true, map);
        return map;
    }

    /**
     * Finds all entries in the {@link QuadTree} whose keys lie within the specified rectangular range.
     * Immediately returns an empty map if the specified key {@code range} does not
     * intersect with {@link #bounds}. Otherwise, performs a recursive search starting
     * with the {@link #rootNode}.
     * <p>
     * Depending on the size of {@code range} relative to {@link #bounds}, the runtime
     * of this operation ranges from O(log m) to O({@link #size}), where m is the number
     * of {@link #nodes}.</p>
     * 
     * @param range a {@link RectD} indicating key range to search
     * @return a {@link Map} containing all {@link PointD} keys and associated <b>V</b> values
     *         where the key lies within {@code range}
     * @throws NullPointerException if {@code range} is {@code null}
     */
    public Map<PointD, V> findRange(RectD range) {

        final Map<PointD, V> map = new HashMap<>();
        if (!bounds.intersectsWith(range)) return map;

        rootNode.findRange(range, false, map);
        return map;
    }

    /**
     * Moves the specified entry to a different key within the {@link QuadTree}.
     * Has the same effect as calling {@link #remove(Object)} with {@code oldKey},
     * followed by {@link #put(PointD, Object)} with {@code newKey} and the value
     * previously associated with {@code oldKey}. However, {@code move} introduces
     * two shortcuts to avoid the O(log m) tree search performed by each method,
     * where m is the number of {@link #nodes}.
     * <ol><li><p>
     * If {@code node} is a valid leaf node that contains {@code oldKey}, {@code move}
     * skips the first tree search for {@code oldKey}. When moving multiple keys in close
     * proximity, always set {@code node} to the previous {@code move} result.
     * </p></li><li><p>
     * If {@code oldKey} and {@code newKey} both fall within the {@link Node#bounds}
     * of the same leaf node, {@code move} skips the second tree search for {@code newKey}
     * and directly adjusts that leaf node’s {@link Node#entries}.
     * </p></li></ol>
     * Either shortcut avoids one O(log m) tree search. When moving nearby keys over a short
     * distance, both shortcuts may apply and reduce {@code move} to an O(1) operation.
     * 
     * @param oldKey the existing {@link PointD} key of the entry to move
     * @param newKey the new {@link PointD} key to replace {@code oldKey}
     * @param node an optional {@link Node} to search for {@code oldKey}
     *             before searching the entire {@link QuadTree}
     * @return the {@link Node} that contained {@code oldKey}, or {@code null}
     *         if that {@link Node} was removed as the result of the move
     * @throws IllegalArgumentException if {@code node} is not {@code null}, and its
     *         {@link Node#owner} differs from the {@link QuadTree}, or if {@code newKey}
     *         is outside {@link #bounds} or already exists in the {@link QuadTree}
     * @throws NoSuchElementException if {@code oldKey} does not exist in the {@link QuadTree}
     * @throws NullPointerException if {@code oldKey} or {@code newKey} is {@code null}
     */
    public Node<V> move(PointD oldKey, PointD newKey, Node<V> node) {

        if (!checkLeaf(node) || !node._entries.containsKey(oldKey)) {
            node = findNode(oldKey);
            if (node == null)
                throw new NoSuchElementException("oldKey");
        }

        final V value = node._entries.get(oldKey);
        node._entries.remove(oldKey);

        // Right/Bottom may belong to neighboring leaf node
        if (node.bounds.containsOpen(newKey))
            node._entries.put(newKey, value);
        else {
            --_size;
            if (node._entries.isEmpty() && node.parent != null) {
                node.parent.removeChild(node);
                node = null;
            }
            put(newKey, value);
        }

        return node;
    }

    /**
     * Gets all {@link Node} instances in the {@link QuadTree}.
     * Always contains at least one element with a {@link Node#signature} of zero,
     * which is the permanent {@link #rootNode} of the {@link QuadTree}.
     * <p>
     * Generally contains fewer than {@link #size} elements since leaf nodes may contain
     * up to {@link #capacity} entries, or more if they reside on {@link #MAX_LEVEL}.</p>
     * 
     * @return an unmodifiable {@link Map} that maps {@link Node#signature} values
     *         to all corresponding {@link Node} instances in the {@link QuadTree}
     */
    public Map<Integer, Node<V>> nodes() {
        return Collections.unmodifiableMap(_nodes);
    }

    /**
     * Enables or disables the use of a bit mask for finding depth probe levels.
     * This is an experimental feature of the depth probe shortcut described in
     * {@link QuadTree#findNode(PointD)}. See the source code for implementation details.
     * 
     * @param value {@code true} to use a bit mask, {@code false} to use a loop
     */
    public void setProbeUseBitMask(boolean value) {
        _probe.useBitMask = value;
    }

    /**
     * Determines whether the specified {@link Node} is a valid leaf node.
     * @param node the {@link Node} to check
     * @return {@code true} if {@code node} and its {@link Node#entries} collection
     *         are not {@code null}, else {@code false}
     * @throws IllegalArgumentException if {@code node} is not {@code null},
     *         and its {@link Node#owner} differs from the {@link QuadTree}
     */
    private boolean checkLeaf(Node<V> node) {
        if (node == null) return false;

        if (node._owner != this)
            throw new IllegalArgumentException("node not in tree");

        return (node._entries != null);
    }
    
    // ----- AbstractMap implementation -----

    /**
     * Determines whether the {@link QuadTree} contains the specified key.
     * Succeeds if {@link #findNode(PointD)} finds a {@link Node}
     * that contains the specified {@code key}.
     * 
     * @param key the {@link PointD} key to find
     * @return {@code true} if the {@link QuadTree} contains {@code key}, else {@code false}
     * @throws ClassCastException if {@code key} cannot be cast to {@link PointD}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @Override
    public boolean containsKey(Object key) {
        final PointD realKey = (PointD) key;
        final Node<V> node = findNode(realKey);
        return (checkLeaf(node) && node._entries.containsKey(realKey));
    }

    /**
     * Determines whether the {@link QuadTree} contains the specified value.
     * Succeeds if {@link #findNodeByValue} finds a {@link Node} that contains
     * the specified {@code value}, which may be {@code null}.
     * 
     * @param value the <b>V</b> value to find
     * @return {@code true} if the {@link QuadTree} associates {@code value}
     *         with at least one {@link PointD} key, else {@code false}
     * @throws ClassCastException if {@code value} cannot be cast to <b>V</b>
     */
    @Override
    public boolean containsValue(Object value) {
        @SuppressWarnings("unchecked")
        final V realValue = (V) value;
        return (findNodeByValue(realValue) != null);
    }

    /**
     * Gets a {@link Set} view of all entries in the {@link QuadTree}.
     * Returns a {@link Set} view of all {@link Entry} instances of {@link PointD}
     * keys and associated <b>V</b> values in the {@link QuadTree}. The set is backed
     * by the {@link QuadTree}, so changes to the {@link QuadTree} are reflected
     * in the set, and vice-versa.
     * <p>
     * If the {@link QuadTree} is structurally modified while an iteration over the set
     * is in progress, the results of the iteration are undefined. This implementation
     * does not support {@link Iterator#remove} on set iterators, but it does support
     * {@link Set#add} and {@link Set#addAll} on the set itself. The latter operations
     * will replace values for existing keys, returning {@code true} if so.</p>
     * 
     * @return a {@link Set} view of the entries in the {@link QuadTree}
     */
    @Override
    public Set<Entry<PointD, V>> entrySet() {
        return _entrySet;
    }

    /**
     * Gets the value associated with the specified key in the {@link QuadTree}.
     * Calls {@link #findNode(PointD)} to find the {@link Node} that contains the
     * specified {@code key}. Note that {@code get} may return {@code null} even
     * if {@code key} is found, namely if its associated value is {@code null}.
     * 
     * @param key the {@link PointD} key whose <b>V</b> value to get
     * @return the <b>V</b> value associated with {@code key} if found, else {@code null}
     * @throws ClassCastException if {@code key} cannot be cast to {@link PointD}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @Override
    public V get(Object key) {
        final Node<V> node = findNode((PointD) key);
        return (checkLeaf(node) ? node._entries.get(key) : null);
    }

    /**
     * Puts the specified entry in the {@link QuadTree}.
     * Calls {@link #findNode(PointD)} to find the {@link Node} for the specified
     * {@code key}, but may then create one or more child nodes if {@code key}
     * is not already present. Any existing value associated with {@code key}
     * is overwritten and returned.
     * <p>
     * May return {@code null} even if there was a previous value associated
     * with {@code key}, namely if that value was itself {@code null}.</p>
     * 
     * @param key the {@link PointD} key of the entry to store
     * @param value the <b>V</b> value of the entry to store
     * @return the <b>V</b> value previously associated with {@code key},
     *         if already present, else {@code null}
     * @throws IllegalArgumentException if {@code key} is outside of {@link #bounds}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @Override
    public V put(PointD key, V value) {

        Node<V> node = findNode(key);
        if (node == null)
            throw new IllegalArgumentException("key not in bounds");

        // replace value if key already present
        if (node._entries != null && node._entries.containsKey(key)) {
            final V oldValue = node._entries.get(key);
            node._entries.put(key, value);
            return oldValue;
        }

        // find insertion point for new key and value
        while (node.level() < MAX_LEVEL && !node.hasCapacity()) {
            if (node._entries != null) node.split();
            node = node.findOrCreateChild(key);
        }

        assert(!node._entries.containsKey(key));
        node._entries.put(key, value);
        _size++;
        return null;
    }

    /**
     * Puts all entries from the specified {@link Map} in the {@link QuadTree}.
     * Iterates over the specified {@code map} and calls {@link #put} for each
     * {@link PointD} key and associated <b>V</b> value. Note this means that any
     * values associated with existing keys will be overwritten.
     * 
     * @param map the {@link Map} whose {@link PointD} keys and <b>V</b> values to add
     * @throws IllegalArgumentException if {@code map} contains any keys outside of {@link #bounds}
     * @throws NullPointerException if {@code map} or any of its keys is {@code null}
     */
    @Override
    public void putAll(Map<? extends PointD, ? extends V> map) {
        for (Map.Entry<? extends PointD, ? extends V> entry: map.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    /**
     * Removes the entry with the specified key from the {@link QuadTree}.
     * Calls {@link #findNode(PointD)} to find the {@link Node} that contains the specified
     * {@code key}. Also removes the {@link Node} itself, and possibly recursively its chain
     * of {@link Node#parent} nodes, if removing {@code key} leaves it empty.
     * 
     * @param key the {@link PointD} key whose entry to remove
     * @return the previous <b>V</b> value associated with {@code key},
     *         or {@code null} if {@code key} was not found
     * @throws ClassCastException if {@code key} cannot be cast to {@link PointD}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @Override
    public V remove(Object key) {
        
        final PointD realKey = (PointD) key;
        final Node<V> node = findNode(realKey);
        if (!checkLeaf(node) || !node._entries.containsKey(realKey))
            return null;

        --_size;
        final V oldValue = node._entries.get(realKey);
        node._entries.remove(realKey);

        if (node._entries.isEmpty() && node.parent != null)
            node.parent.removeChild(node);

        return oldValue;
    }

    /**
     * Removes the specified key and value from the {@link QuadTree}.
     * Calls {@link #findNode(PointD)} to find the {@link Node} that contains the
     * specified {@code key}. The entry is removed only if {@code key} is currently
     * associated with the specified {@code value}.
     * <p>
     * Also removes the {@link Node} itself, and possibly recursively its chain of
     * {@link Node#parent} nodes, if removing the specified entry leaves it empty.</p>
     * 
     * @param key the {@link PointD} key of the entry to remove
     * @param value the <b>V</b> value of the entry to remove
     * @return {@code true} if {@code key} and {@code value} were found and removed, else {@code false}
     * @throws ClassCastException if {@code key} cannot be cast to {@link PointD}
     *                            or {@code value} cannot be cast to <b>V</b>
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @Override
    public boolean remove(Object key, Object value) {
        final PointD realKey = (PointD) key;
        @SuppressWarnings("unchecked")
        final V realValue = (V) value;

        final Node<V> node = findNode(realKey);
        if (!checkLeaf(node) || !node._entries.containsKey(realKey))
            return false;

        // cannot use get for key test: null values are permissible
        if (!Objects.equals(realValue, node._entries.get(realKey)))
            return false;

        --_size;
        node._entries.remove(realKey);
        if (node._entries.isEmpty() && node.parent != null)
            node.parent.removeChild(node);

        return true;
    }

    /**
     * Replaces the value of the specified existing key with the specified value.
     * Calls {@link #findNode(PointD)} to find the {@link Node} that contains
     * the specified {@code key}. The associated value is replaced with
     * {@code value} only if an existing association was found.
     * <p>
     * May return {@code null} even if there was a previous value associated
     * with {@code key}, namely if that value was itself {@code null}.</p>
     * 
     * @param key the {@link PointD} key whose value to replace
     * @param value the new <b>V</b> value to associate with {@code key}
     * @return the previous <b>V</b> value associated with {@code key} if found, else {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @Override
    public V replace(PointD key, V value) {

        final Node<V> node = findNode(key);
        if (!checkLeaf(node) || !node._entries.containsKey(key))
            return null;

        final V oldValue = node._entries.get(key);
        node._entries.put(key, value);
        return oldValue;
    }

    /**
     * Replaces the value of the specified key with the specified value,
     * but only if currently mapped to the specified existing value.
     * Calls {@link #findNode(PointD)} to find the {@link Node} that contains
     * the specified {@code key}. The associated value is replaced with
     * {@code newValue} only if it currently equals {@code oldValue}.
     * 
     * @param key the {@link PointD} key whose value to replace
     * @param oldValue the old <b>V</b> value associated with {@code key}
     * @param newValue the new <b>V</b> value to associate with {@code key}
     * @return {@code true} if {@code key} and {@code oldValue} were found and 
     *         {@code oldValue} replaced with {@code newValue}, else {@code false}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @Override
    public boolean replace(PointD key, V oldValue, V newValue) {

        final Node<V> node = findNode(key);
        if (!checkLeaf(node) || !node._entries.containsKey(key))
            return false;

        // cannot use get for key test: null values are permissible
        if (!Objects.equals(oldValue, node._entries.get(key)))
            return false;

        node._entries.put(key, newValue);
        return true;
    }

    /**
     * Provides a {@link Set} of all entries in the {@link QuadTree}.
     * The {@link QuadTree} stores a permanent instance of {@link EntrySet} 
     * which is returned by {@link #entrySet}.
     */
    private final class EntrySet extends AbstractSet<Entry<PointD, V>> {
        /**
         * Adds the specified entry to the {@link QuadTree}.
         * Calls {@link #put} with the {@link PointD} key and <b>V</b> value
         * of the specified {@code entry}. Note this means that any value
         * associated with an existing key will be overwritten.
         * 
         * @param entry the {@link Entry} to add
         * @return {@code true} if adding {@code entry} added a new key or
         *         changed the value of an existing key, else {@code false}
         * @throws IllegalArgumentException if the key of {@code entry} is outside of {@link #bounds}
         * @throws NullPointerException if {@code entry} or its key is {@code null}
         */
        @Override
        public boolean add(Entry<PointD, V> entry) {
            final int oldSize = _size;

            final V value = entry.getValue();
            final boolean changed = !Objects.equals(value, put(entry.getKey(), value));

            return (changed || oldSize != _size);
        }

        /**
         * Adds all specified entries to the {@link QuadTree}.
         * Iterates over the specified {@code collection} and calls {@link #put}
         * for each {@link PointD} key and associated <b>V</b> value. Note this means
         * that any values associated with existing keys will be overwritten.
         * 
         * @param collection the {@link Collection} to add to the {@link QuadTree}
         * @return {@code true} if adding {@code collection} added any new keys
         *         or changed the values of any existing keys, else {@code false}
         * @throws IllegalArgumentException if {@code collection} contains any keys outside of {@link #bounds}
         * @throws NullPointerException if {@code collection} or any of its entries or keys is {@code null}
         */
        @Override
        public boolean addAll(Collection<? extends Entry<PointD, V>> collection) {
            final int oldSize = _size;
            boolean changed = false;

            for (Map.Entry<PointD, V> entry: collection) {
                final V value = entry.getValue();
                if (!Objects.equals(value, put(entry.getKey(), value)))
                    changed = true;
            }

            return (changed || oldSize != _size);
        }

        /**
         * Removes all entries from the {@link QuadTree}.
         * Does nothing if the {@link QuadTree} is already empty. Otherwise this is
         * an O({@link #nodes}) operation, as all node links are set to {@code null}.
         */
        @Override
        public void clear() {
            if (_size == 0) {
                assert(_nodes.size() == 1);
                assert(_nodes.get(0) == rootNode);
                return;
            }

            for (Node<V> node: _nodes.values())
                node.clear();

            _nodes.clear();
            _nodes.put(0, rootNode);
            _size = 0;
        }

        /**
         * Determines whether the {@link QuadTree} contains the specified entry.
         * Succeeds if {@link #findNode(PointD)} finds the {@link PointD} key
         * of the specified entry, and the resulting {@link Node} also associates
         * that key with the entry’s <b>V</b> value.
         * 
         * @param obj the element to examine
         * @return {@code true} if {@code obj} was found, else {@code false}
         * @throws ClassCastException if {@code obj} cannot be cast to {@link Map.Entry}
         *                            with a {@link PointD} key and a <b>V</b> value
         * @throws NullPointerException if {@code obj} or its key is {@code null}
         */
        @Override
        public boolean contains(Object obj) {
            @SuppressWarnings("unchecked")
            final Entry<PointD, V> entry = (Entry<PointD, V>) obj;

            final PointD key = entry.getKey();
            final Node<V> node = findNode(key);
            if (!checkLeaf(node) || !node._entries.containsKey(key))
                return false;

            // cannot use get for key test: null values are permissible
            final V value = node._entries.get(key);
            return Objects.equals(value, entry.getValue());
        }

        /**
         * Indicates whether the {@link QuadTree} is empty.
         * This is an O(1) operation.
         * 
         * @return {@code true} if the {@link QuadTree} is empty, else {@code false}
         */
        @Override
        public boolean isEmpty() {
            return (_size == 0);
        }

        /**
         * Returns an {@link Iterator} over the entries in the {@link QuadTree}.
         * The iteration sequence is arbitrary, depending on how the {@link QuadTree}
         * internally created, split, and linked nodes as entries were added.
         * <p>
         * Any operation that relies on the {@link Iterator} to copy the keys and/or
         * values of the {@link QuadTree} to another collection will likewise produce
         * them in arbitrary order, and must be sorted manually to obtain a specific
         * order, e.g. lexicographically by {@link PointD} keys.</p>
         * 
         * @return an {@link Iterator} for the entire {@link QuadTree}
         */
        @Override
        public Iterator<Entry<PointD, V>> iterator() {
            return new EntryIterator();
        }

        /**
         * Removes the specified entry from the {@link QuadTree}.
         * Calls {@link #findNode(PointD)} to find the {@link Node} that contains the
         * {@link PointD} key of the specified entry. The entry is removed only if the
         * key is associated with a <b>V</b> value that also matches the entry’s.
         * <p>
         * Also removes the {@link Node} itself, and possibly recursively its chain of
         * {@link Node#parent} nodes, if removing the specified entry leaves it empty.</p>
         * 
         * @param obj the element to remove
         * @return {@code true} if {@code obj} was found and removed, else {@code false}
         * @throws ClassCastException if {@code obj} cannot be cast to {@link Map.Entry}
         *                            with a {@link PointD} key and a <b>V</b> value
         * @throws NullPointerException if {@code obj} or its key is {@code null}
         */
        @Override
        public boolean remove(Object obj) {
            @SuppressWarnings("unchecked")
            final Entry<PointD, V> entry = (Entry<PointD, V>) obj;
            return QuadTree.this.remove(entry.getKey(), entry.getValue());
        }

        /**
         * Gets the number of entries in the {@link QuadTree}.
         * This is an O(1) operation, as the {@link QuadTree} keeps a running count of its entries.
         * 
         * @return the number of entries in the {@link QuadTree}
         */
        @Override
        public int size() {
            return _size;
        }
    }

    /**
     * Provides an {@link Iterator} for the {@link QuadTree}.
     * Does not support modification, and does not check for concurrent external modification.
     */
    private final class EntryIterator implements Iterator<Entry<PointD, V>> {

        private final Iterator<Node<V>> _nodeIter = _nodes.values().iterator();
        private Iterator<Entry<PointD, V>> _entryIter = null;
        private int _index = 0;

        /**
         * Indicates whether the {@link Iterator} has any next elements.
         * @return {@code true} if a subsequent call to {@link #next} would succeed, else {@code false}
         */
        @Override
        public boolean hasNext() {
            return (_index < _size);
        }

        /**
         * Returns the next element in the {@link QuadTree},
         * and moves the {@link Iterator} forward.
         * 
         * @return the next element in the {@link QuadTree}
         * @throws NoSuchElementException if there is no next element
         */
        @Override
        public Entry<PointD, V> next() {
            _index++;

            // return next entry from current node, if any
            if (_entryIter != null && _entryIter.hasNext())
                return _entryIter.next();

            // search next node with attached entries
            Node<V> node;
            do {
                node = _nodeIter.next();
            } while (!checkLeaf(node) || node._entries.isEmpty());

            // start iterating over entries in next node
            _entryIter = node._entries.entrySet().iterator();
            return _entryIter.next();
        }
    }

    /**
     * Provides a generic tree node within a {@link QuadTree}.
     * Represents a collection of {@link PointD} keys and <b>V</b> values within a
     * {@link QuadTree}. This collection and all references to related {@link Node}
     * instances within the {@link QuadTree} are exposed as read-only properties.
     * <p>
     * {@link Node} was inspired by the <code>QuadTreeNode</code> class by
     * Michael J. Laszlo, <em>Computational Geometry and Computer Graphics in C++</em>,
     * Prentice Hall 1996, p.236ff.</p>
     * 
     * @param <V> the type of all values in the {@link QuadTree}
     */
    public static final class Node<V> {

        private Node<V> _minXminY, _minXmaxY, _maxXminY, _maxXmaxY;
        private Map<PointD, V> _entries;
        private QuadTree<V> _owner;

        /**
         * The {@link RectD} bounds of all {@link PointD} keys in the {@link Node}.
         * Indicates the subrange of the containing {@link QuadTree} that is covered
         * by the {@link Node} and its children. Always has a positive {@link RectD#width}
         * and {@link RectD#height}. The two dimensions are not necessarily equal.
         * <p>
         * Any {@link PointD} keys stored in the associated {@link #entries} lie within
         * {@link #bounds}. The extreme {@link RectD#max} coordinates are considered part
         * of the neighboring {@link Node} on that side, if any.</p>
         */
        public final RectD bounds;

        /**
         * The center of the {@link #bounds} of the {@link Node}.
         * Divides the associated {@link #bounds} into four equal-sized quadrants,
         * corresponding to the {@link #minXminY}, {@link #minXmaxY}, {@link #maxXminY},
         * and {@link #maxXmaxY} child nodes. {@link #center} is precomputed to speed up
         * the traversal of the tree structure.
         */
        public final PointD center;

        /**
         * The parent of the {@link Node} in the {@link QuadTree}.
         * Never {@code null} except for the permanent {@link #rootNode}
         * of the containing {@link QuadTree}.
         */
        public final Node<V> parent;

        /**
         * The unique signature of the {@link Node}.
         * Holds an {@link Integer} value containing a bitwise combination of {@link #level},
         * {@link #gridX}, and {@link #gridY}. This value uniquely identifies the position
         * of each {@link Node} in the containing {@link QuadTree}, and also serves as its
         * key within the {@link #nodes} hashtable.
         */
        public final int signature;

        /**
         * Creates a {@link Node} with the specified initial data.
         * @param tree the {@link QuadTree} that contains the {@link Node}
         * @param signature the unique signature of the {@link Node}
         * @param parent the parent of the {@link Node} in the tree structure,
         *               or {@code null} to create the {@link #rootNode}
         * @param bounds the bounds of all {@link PointD} keys in the {@link Node}
         * @throws IllegalArgumentException if {@code bounds} has a {@link RectD#width}
         *         or {@link RectD#height} that is equal to or less than zero
         * @throws NullPointerException if {@code tree} or {@code bounds} is {@code null}
         */
        private Node(QuadTree<V> tree, int signature, Node<V> parent, RectD bounds) {
            if (tree == null)
                throw new NullPointerException("tree");
            if (bounds.width() <= 0)
                throw new IllegalArgumentException("bounds.width < 0");
            if (bounds.height() <= 0)
                throw new IllegalArgumentException("bounds.height < 0");

            this._owner = tree;
            this.signature = signature;
            this.parent = parent;
            this.bounds = bounds;

            this.center = bounds.center();
            this._entries = new HashMap<>(tree.capacity);
            tree._nodes.put(this.signature, this);
        }

        /**
         * Gets all keys and values stored in the {@link Node}.
         * Usually contains up to {@link QuadTree#capacity} elements when not {@code null}.
         * If {@link #level} equals {@link QuadTree#MAX_LEVEL}, the number of elements is
         * unbounded. Use the containing {@link QuadTree} to add, change, or remove elements.
         * <p>
         * If {@link #entries} is valid, the four child links {@link #minXminY},
         * {@link #minXmaxY}, {@link #maxXminY}, and {@link #maxXmaxY} are all {@code null}.
         * Conversely, at least one link is valid if {@link #entries} is {@code null}.</p>
         * 
         * @return an unmodifiable {@link Map} of {@link PointD} keys and <b>V</b> values,
         *         or {@code null} if {@link #isLeaf} is {@code false}
         */
        public Map<PointD, V> entries() {
            return (_entries == null ? null :
                    Collections.unmodifiableMap(_entries));
        }

        /**
         * Gets the x-coordinate of the {@link Node} in the tree structure.
         * Returns the middle 14 bits of {@link #signature}.
         * 
         * @return the x-coordinate of the {@link Node} within the grid of its
         *         {@link #level}, ranging from zero to 2^{@link #level}
         */
        public int gridX() {
            return ((signature & 0x0003FFF0) >> 4);
        }

        /**
         * Gets the y-coordinate of the {@link Node} in the tree structure.
         * Returns the highest 14 bits of {@link #signature}.
         * 
         * @return the y-coordinate of the {@link Node} within the grid of its
         *         {@link #level}, ranging from zero to 2^{@link #level}
         */
        public int gridY() {
            return ((signature & 0xFFFC0000) >> 18);
        }

        /**
         * Indicates whether the {@link Node} has any remaining {@link #entries} capacity.
         * Does not check whether {@link #level} equals {@link #MAX_LEVEL}, in which case
         * the number of {@link #entries} may exceed {@link #capacity}.
         * 
         * @return {@code true} if {@link #isLeaf} is {@code true} and the number of
         *         {@link #entries} is less than {@link #capacity}, else {@code false}
         */
        public boolean hasCapacity() {
            return (_entries != null && _entries.size() < _owner.capacity);
        }

        /**
         * Indicates whether the {@link Node} is a leaf node.
         * Returns {@code true} exactly if {@link #entries} is not {@code null}.
         * 
         * @return {@code true} if the {@link Node} is a leaf node,
         *         {@code false} if it is an internal node
         */
        public boolean isLeaf() {
            return (_entries != null);
        }

        /**
         * Gets the level of the {@link Node} in the {@link QuadTree}.
         * Returns the lowest four bits of {@link #signature}.
         * 
         * @return the level of the {@link Node} in the {@link QuadTree},
         *         ranging from zero to {@link QuadTree#MAX_LEVEL}
         */
        public int level() {
            return (signature & 0x0000000F);
        }

        /**
         * Gets the child {@link Node} for the {@link #bounds} quadrant
         * containing its greater x- and greater y-coordinates.
         * 
         * @return the child {@link Node} for the {@link #bounds} quadrant containing its
         *         greater x- and greater y-coordinates, or {@code null} if none
         */
        public Node<V> maxXmaxY() {
            return _maxXmaxY;
        }

        /**
         * Gets the child {@link Node} for the {@link #bounds} quadrant
         * containing its greater x- and smaller y-coordinates.
         * 
         * @return the child {@link Node} for the {@link #bounds} quadrant containing its
         *         greater x- and smaller y-coordinates, or {@code null} if none
         */
        public Node<V> maxXminY() {
            return _maxXminY;
        }

        /**
         * Gets the child {@link Node} for the {@link #bounds} quadrant
         * containing its smaller x- and greater y-coordinates.
         * 
         * @return the child {@link Node} for the {@link #bounds} quadrant containing its
         *         smaller x- and greater y-coordinates, or {@code null} if none
         */
        public Node<V> minXmaxY() {
            return _minXmaxY;
        }

        /**
         * Gets the child {@link Node} for the {@link #bounds} quadrant
         * containing its smaller x- and smaller y-coordinates.
         * 
         * @return the child {@link Node} for the {@link #bounds} quadrant containing its
         *         smaller x- and smaller y-coordinates, or {@code null} if none
         */
        public Node<V> minXminY() {
            return _minXminY;
        }

        /**
         * Gets the {@link QuadTree} that contains the {@link Node}.
         * Returns {@code null} if the {@link Node} has been removed from its {@link QuadTree}.
         * Otherwise, returns the same valid object throughout the lifetime of the {@link Node}.
         * 
         * @return the {@link QuadTree} that contains the {@link Node}
         */
        public QuadTree<V> owner() {
            return _owner;
        }
        
        /**
         * Clears the {@link Node}.
         * Clears all fields of the {@link Node} so it can be removed without leaking memory,
         * or in the case of the {@link #rootNode}, stay as root of an empty {@link QuadTree}.
         * 
         * @throws IllegalStateException if {@link #owner} is {@code null}
         */
        private void clear() {
            if (_owner == null)
                throw new IllegalStateException("node already removed");

            _minXminY = _maxXminY = _minXmaxY = _maxXmaxY = null;
            if (_entries != null) _entries.clear();

            if (this != _owner.rootNode) {
                _entries = null;
                _owner = null;
            } else if (_entries == null)
                _entries = new HashMap<>(_owner.capacity);
        }

        /**
         * Creates the indicated child node of the {@link Node}.
         * Both {@code deltaX} and {@code deltaY} must be zero or one.
         * 
         * @param deltaX the offset for the {@link #gridX} coordinate on the next {@link #level}
         * @param deltaY the offset for the {@link #gridY} coordinate on the next {@link #level}
         * @return a new {@link Node} that is the child of the current instance
         *         and has the indicated {@link #bounds} and grid coordinates
         */
        private Node<V> createChild(int deltaX, int deltaY) {

            assert(deltaX == 0 || deltaX == 1);
            assert(deltaY == 0 || deltaY == 1);

            // compute grid coordinates for child node
            final int x = (gridX() << 1) + deltaX;
            final int y = (gridY() << 1) + deltaY;

            // compose signature from level and grid coordinates
            final int sig = ((level() + 1) | (x << 4) | (y << 18));

            // compute bounding rectangle for child node
            double minX, minY, maxX, maxY;
            if (deltaX == 0) {
                minX = bounds.min.x;
                maxX = center.x;
            } else {
                minX = center.x;
                maxX = bounds.max.x;
            }
            if (deltaY == 0) {
                minY = bounds.min.y;
                maxY = center.y;
            } else {
                minY = center.y;
                maxY = bounds.max.y;
            }

            return new Node<>(_owner, sig, this, new RectD(minX, minY, maxX, maxY));
        }

        /**
         * Finds the child node of the {@link Node} that contains the specified {@link PointD} key.
         * Compares the specified {@code key} to the {@link #center} point of the
         * {@link Node} to determine the containing quadrant.
         * 
         * @param key the {@link PointD} key to locate, which must lie within {@link #bounds}
         * @return the child of the {@link Node} that contains {@code key},
         *         or {@code null} if the corresponding child does not yet exist
         */
        private Node<V> findChild(PointD key) {
            assert(bounds.contains(key));

            final double relX = key.x - center.x;
            final double relY = key.y - center.y;

            return (relX < 0 ?
                   (relY < 0 ? _minXminY : _minXmaxY) :
                   (relY < 0 ? _maxXminY : _maxXmaxY));
        }

        /**
         * Finds the child node of the {@link Node} that contains the specified {@link PointD} key,
         * creating the child node if necessary.
         * Compares the specified {@code key} to the {@link #center} point of the
         * {@link Node} to determine the containing quadrant.
         * 
         * @param key the {@link PointD} key to locate, which must lie within {@link #bounds}
         * @return the child of the {@link Node} that contains {@code key}, which is created
         *         if it does not yet exist
         * @throws NullPointerException if {@code key} is {@code null}
         */
        private Node<V> findOrCreateChild(PointD key) {
            assert(bounds.contains(key));

            final double relX = key.x - center.x;
            final double relY = key.y - center.y;

            if (relX < 0) {
                if (relY < 0) {
                    if (_minXminY == null)
                        _minXminY = createChild(0, 0);
                    return _minXminY;
                } else {
                    if (_minXmaxY == null)
                        _minXmaxY = createChild(0, 1);
                    return _minXmaxY;
                }
            } else {
                if (relY < 0) {
                    if (_maxXminY == null)
                        _maxXminY = createChild(1, 0);
                    return _maxXminY;
                } else {
                    if (_maxXmaxY == null)
                        _maxXmaxY = createChild(1, 1);
                    return _maxXmaxY;
                }
            }
        }

        /**
         * Finds all keys and values within the specified key range
         * that are stored in the {@link Node} or its child nodes.
         * 
         * @param range a {@link RectD} indicating the key range to search, which must intersect
         *              with {@link #bounds}, and must be square if {@code useCircle} is {@code true}
         * @param useCircle {@code true} to search only a circle inscribed within {@code range},
         *                  {@code false} to search the entire {@code range}
         * @param output a {@link Map{PointD, V}} that receives any elements whose key lies
         *               within the specified {@code range}, given {@code useCircle}
         * @throws NullPointerException if {@code range} or {@code output} is {@code null}
         */
        private void findRange(RectD range, boolean useCircle, Map<PointD, V> output) {
            assert(bounds.intersectsWith(range));

            // collect output data in leaf nodes
            if (_entries != null) {
                if (useCircle) {
                    final double radius = range.width() / 2;
                    final double x = range.min.x + radius;
                    final double y = range.min.y + radius;

                    for (Map.Entry<PointD, V> pair: _entries.entrySet()) {
                        final PointD key = pair.getKey();
                        if (range.contains(key)) {
                            final double dx = key.x - x, dy = key.y - y;
                            if (dx * dx + dy * dy <= radius * radius)
                                output.put(key, pair.getValue());
                        }
                    }
                } else {
                    for (Map.Entry<PointD, V> pair: _entries.entrySet())
                        if (range.contains(pair.getKey()))
                            output.put(pair.getKey(), pair.getValue());
                }
                return;
            }

            // search child nodes of non-leaf nodes
            final boolean topRange =    (range.min.y < center.y);
            final boolean bottomRange = (range.max.y >= center.y);
            final boolean leftRange =   (range.min.x < center.x);
            final boolean rightRange =  (range.max.x >= center.x);

            if (topRange) {
                if (leftRange && _minXminY != null)
                    _minXminY.findRange(range, useCircle, output);
                if (rightRange && _maxXminY != null)
                    _maxXminY.findRange(range, useCircle, output);
            }

            if (bottomRange) {
                if (leftRange && _minXmaxY != null)
                    _minXmaxY.findRange(range, useCircle, output);
                if (rightRange && _maxXmaxY != null)
                    _maxXmaxY.findRange(range, useCircle, output);
            }
        }

        /**
         * Removes the specified child node from the {@link Node}.
         * Recursively removes the {@link Node} from its {@link #parent} if the specified
         * {@code child} was its last valid child. If {@link #parent} is {@code null},
         * {@link #removeChild} recreates an empty {@link #entries} collection instead.
         * 
         * @param child the child node to remove from the {@link Node}
         * @throws IllegalArgumentException if {@code child} does not equal {@link #minXminY},
         *         {@link #minXmaxY}, {@link #maxXminY}, or {@link #maxXmaxY}
         */
        private void removeChild(Node<V> child) {

            // remove specified child node
            if (child == _minXminY) {
                _owner._nodes.remove(_minXminY.signature);
                _minXminY = null;
            }
            else if (child == _maxXminY) {
                _owner._nodes.remove(_maxXminY.signature);
                _maxXminY = null;
            }
            else if (child == _minXmaxY) {
                _owner._nodes.remove(_minXmaxY.signature);
                _minXmaxY = null;
            }
            else if (child == _maxXmaxY) {
                _owner._nodes.remove(_maxXmaxY.signature);
                _maxXmaxY = null;
            } else
                throw new IllegalArgumentException("child is not a child node");

            // remove empty node if all children removed
            if (_minXminY == null && _minXmaxY == null &&
                _maxXminY == null && _maxXmaxY == null) {

                // root reverts to leaf node instead
                if (parent == null)
                    _entries = new HashMap<>(_owner.capacity);
                else
                    parent.removeChild(this);
            }
        }

        /**
         * Splits the {@link Node} into child nodes.
         * Transfers all {@link #entries} of the current {@link Node} to newly created
         * children. Children that would receive no {@link #entries} are not created.
         */
        private void split() {

            for (Map.Entry<PointD, V> pair: _entries.entrySet()) {
                final PointD key = pair.getKey();
                final Node<V> child = findOrCreateChild(key);
                child._entries.put(key, pair.getValue());
            }

            _entries = null;
        }
    }

    /**
     * Contains the status of the last depth probe conducted by {@link #findNode(PointD)}.
     * Stores all level-specific data of the last depth probe, and reuses that
     * data until the estimated starting level changes. This happens only when
     * the number of {@link #nodes} changes by at least 256 elements, since depth
     * probes are not conducted above tree level 4.
     */
    private static final class ProbeStatus {
        /**
         * The height of one grid cell at the recorded {@link #level}.
         */
        double gridHeight;
        /**
         * The width of one grid cell at the recorded {@link #level}.
         */
        double gridWidth;
        /**
         * The starting level for the recorded {@link #nodeCount}.
         */
        int level;
        /**
         * The number of {@link #nodes} at the last depth probe, right-shifted by 8 bits.
         */
        int nodeCount;
        /**
         * Experimental flag to determine {@link #level} with a bit mask rather than a loop.
         */
        boolean useBitMask;
    }
}

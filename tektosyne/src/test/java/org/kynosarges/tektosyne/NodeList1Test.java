package org.kynosarges.tektosyne;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

/**
 * Provides unit tests for class {@link NodeList}, part one.
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * <p>
 * CN: The original test suite was written for testing {@link ArrayDeque}.
 * I added some required fields from a superclass which is not present here,
 * and removed serialization testing which {@link NodeList} does not support.
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/ArrayDequeTest.java?view=co
 * </p>
 * @author Christoph Nahr
 * @version 6.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked"})
public class NodeList1Test {
    /**
     * The number of elements to place in collections, arrays, etc.
     */
    private static final int SIZE = 20;

    private static final Integer ZERO  = new Integer(0);
    private static final Integer ONE   = new Integer(1);
    private static final Integer TWO   = new Integer(2);
    private static final Integer THREE = new Integer(3);
    private static final Integer FOUR  = new Integer(4);

    /**
     * Returns a new deque of given size containing consecutive
     * Integers 0 ... n.
     */
    private NodeList<Integer> populatedDeque(int n) {
        NodeList<Integer> q = new NodeList<Integer>();
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; ++i)
            assertTrue(q.offerLast(new Integer(i)));
        assertFalse(q.isEmpty());
        assertEquals(n, q.size());
        return q;
    }

    /**
     * new deque is empty
     */
    @Test
    public void testConstructor1() {
        assertEquals(0, new NodeList().size());
    }

    /**
     * Initializing from null Collection throws NPE
     */
    @Test
    public void testConstructor3() {
        try {
            new NodeList((Collection) null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NPE
     */
    @Test
    public void testConstructor4() {
        try {
            new NodeList(Arrays.asList(new Integer[SIZE]));
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection with some null elements throws NPE
     */
    @Test
    public void testConstructor5() {
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE - 1; ++i)
            ints[i] = new Integer(i);
        try {
            new NodeList(Arrays.asList(ints));
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * Deque contains all elements of collection used to initialize
     */
    @Test
    public void testConstructor6() {
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i)
            ints[i] = new Integer(i);
        NodeList q = new NodeList(Arrays.asList(ints));
        for (int i = 0; i < SIZE; ++i)
            assertEquals(ints[i], q.pollFirst());
    }

    /**
     * isEmpty is true before add, false after
     */
    @Test
    public void testEmpty() {
        NodeList q = new NodeList();
        assertTrue(q.isEmpty());
        q.add(new Integer(1));
        assertFalse(q.isEmpty());
        q.add(new Integer(2));
        q.removeFirst();
        q.removeFirst();
        assertTrue(q.isEmpty());
    }

    /**
     * size changes when elements added and removed
     */
    @Test
    public void testSize() {
        NodeList q = populatedDeque(SIZE);
        
        // CN: test internally tracked size
        assertEquals(q.countNodes(), q.size());

        for (int i = 0; i < SIZE; ++i) {
            assertEquals(SIZE - i, q.size());
            assertEquals(q.countNodes(), q.size());
            q.removeFirst();
        }
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.size());
            assertEquals(q.countNodes(), q.size());
            q.add(new Integer(i));
        }
    }

    /**
     * push(null) throws NPE
     */
    @Test
    public void testPushNull() {
        NodeList q = new NodeList();
        try {
            q.push(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * peekFirst() returns element inserted with push
     */
    @Test
    public void testPush() {
        NodeList q = populatedDeque(3);
        q.pollLast();
        q.push(FOUR);
        assertSame(FOUR, q.peekFirst());
    }

    /**
     * pop() removes next element, or throws NSEE if empty
     */
    @Test
    public void testPop() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.pop());
        }
        try {
            q.pop();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
    }

    /**
     * offer(null) throws NPE
     */
    @Test
    public void testOfferNull() {
        NodeList q = new NodeList();
        try {
            q.offer(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * offerFirst(null) throws NPE
     */
    @Test
    public void testOfferFirstNull() {
        NodeList q = new NodeList();
        try {
            q.offerFirst(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * offerLast(null) throws NPE
     */
    @Test
    public void testOfferLastNull() {
        NodeList q = new NodeList();
        try {
            q.offerLast(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * offer(x) succeeds
     */
    @Test
    public void testOffer() {
        NodeList q = new NodeList();
        assertTrue(q.offer(ZERO));
        assertTrue(q.offer(ONE));
        assertSame(ZERO, q.peekFirst());
        assertSame(ONE, q.peekLast());
    }

    /**
     * offerFirst(x) succeeds
     */
    @Test
    public void testOfferFirst() {
        NodeList q = new NodeList();
        assertTrue(q.offerFirst(ZERO));
        assertTrue(q.offerFirst(ONE));
        assertSame(ONE, q.peekFirst());
        assertSame(ZERO, q.peekLast());
    }

    /**
     * offerLast(x) succeeds
     */
    @Test
    public void testOfferLast() {
        NodeList q = new NodeList();
        assertTrue(q.offerLast(ZERO));
        assertTrue(q.offerLast(ONE));
        assertSame(ZERO, q.peekFirst());
        assertSame(ONE, q.peekLast());
    }

    /**
     * add(null) throws NPE
     */
    @Test
    public void testAddNull() {
        NodeList q = new NodeList();
        try {
            q.add(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * addFirst(null) throws NPE
     */
    @Test
    public void testAddFirstNull() {
        NodeList q = new NodeList();
        try {
            q.addFirst(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * addLast(null) throws NPE
     */
    @Test
    public void testAddLastNull() {
        NodeList q = new NodeList();
        try {
            q.addLast(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * add(x) succeeds
     */
    @Test
    public void testAdd() {
        NodeList q = new NodeList();
        assertTrue(q.add(ZERO));
        assertTrue(q.add(ONE));
        assertSame(ZERO, q.peekFirst());
        assertSame(ONE, q.peekLast());
    }

    /**
     * addFirst(x) succeeds
     */
    @Test
    public void testAddFirst() {
        NodeList q = new NodeList();
        q.addFirst(ZERO);
        q.addFirst(ONE);
        assertSame(ONE, q.peekFirst());
        assertSame(ZERO, q.peekLast());
    }

    /**
     * addLast(x) succeeds
     */
    @Test
    public void testAddLast() {
        NodeList q = new NodeList();
        q.addLast(ZERO);
        q.addLast(ONE);
        assertSame(ZERO, q.peekFirst());
        assertSame(ONE, q.peekLast());
    }

    /**
     * addAll(null) throws NPE
     */
    @Test
    public void testAddAll1() {
        NodeList q = new NodeList();
        try {
            q.addAll(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with null elements throws NPE
     */
    @Test
    public void testAddAll2() {
        NodeList q = new NodeList();
        try {
            q.addAll(Arrays.asList(new Integer[SIZE]));
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with any null elements throws NPE after
     * possibly adding some elements
     */
    @Test
    public void testAddAll3() {
        NodeList q = new NodeList();
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE - 1; ++i)
            ints[i] = new Integer(i);
        try {
            q.addAll(Arrays.asList(ints));
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * Deque contains all elements, in traversal order, of successful addAll
     */
    @Test
    public void testAddAll5() {
        Integer[] empty = new Integer[0];
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i)
            ints[i] = new Integer(i);
        NodeList q = new NodeList();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(ints)));
        for (int i = 0; i < SIZE; ++i)
            assertEquals(ints[i], q.pollFirst());
    }

    /**
     * pollFirst() succeeds unless empty
     */
    @Test
    public void testPollFirst() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.pollFirst());
        }
        assertNull(q.pollFirst());
    }

    /**
     * pollLast() succeeds unless empty
     */
    @Test
    public void testPollLast() {
        NodeList q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            assertEquals(i, q.pollLast());
        }
        assertNull(q.pollLast());
    }

    /**
     * poll() succeeds unless empty
     */
    @Test
    public void testPoll() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * remove() removes next element, or throws NSEE if empty
     */
    @Test
    public void testRemove() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.remove());
        }
        try {
            q.remove();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
    }

    /**
     * remove(x) removes x and returns true if present
     */
    @Test
    public void testRemoveElement() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.contains(i));
            assertTrue(q.remove((Object) i));
            assertFalse(q.contains(i));
            assertTrue(q.contains(i - 1));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.contains(i));
            assertTrue(q.remove((Object) i));
            assertFalse(q.contains(i));
            assertFalse(q.remove((Object) (i + 1)));
            assertFalse(q.contains(i + 1));
        }
        assertTrue(q.isEmpty());
    }

    /**
     * peekFirst() returns next element, or null if empty
     */
    @Test
    public void testPeekFirst() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.peekFirst());
            assertEquals(i, q.pollFirst());
            assertTrue(q.peekFirst() == null ||
                       !q.peekFirst().equals(i));
        }
        assertNull(q.peekFirst());
    }

    /**
     * peek() returns next element, or null if empty
     */
    @Test
    public void testPeek() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.peek());
            assertEquals(i, q.poll());
            assertTrue(q.peek() == null ||
                       !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /**
     * peekLast() returns next element, or null if empty
     */
    @Test
    public void testPeekLast() {
        NodeList q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            assertEquals(i, q.peekLast());
            assertEquals(i, q.pollLast());
            assertTrue(q.peekLast() == null ||
                       !q.peekLast().equals(i));
        }
        assertNull(q.peekLast());
    }

    /**
     * element() returns first element, or throws NSEE if empty
     */
    @Test
    public void testElement() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.element());
            assertEquals(i, q.poll());
        }
        try {
            q.element();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
    }

    /**
     * getFirst() returns first element, or throws NSEE if empty
     */
    @Test
    public void testFirstElement() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.getFirst());
            assertEquals(i, q.pollFirst());
        }
        try {
            q.getFirst();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
    }

    /**
     * getLast() returns last element, or throws NSEE if empty
     */
    @Test
    public void testLastElement() {
        NodeList q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            assertEquals(i, q.getLast());
            assertEquals(i, q.pollLast());
        }
        try {
            q.getLast();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
        assertNull(q.peekLast());
    }

    /**
     * removeFirst() removes first element, or throws NSEE if empty
     */
    @Test
    public void testRemoveFirst() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.removeFirst());
        }
        try {
            q.removeFirst();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
        assertNull(q.peekFirst());
    }

    /**
     * removeLast() removes last element, or throws NSEE if empty
     */
    @Test
    public void testRemoveLast() {
        NodeList q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            assertEquals(i, q.removeLast());
        }
        try {
            q.removeLast();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
        assertNull(q.peekLast());
    }

    /**
     * removeFirstOccurrence(x) removes x and returns true if present
     */
    @Test
    public void testRemoveFirstOccurrence() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.removeFirstOccurrence(new Integer(i)));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.removeFirstOccurrence(new Integer(i)));
            assertFalse(q.removeFirstOccurrence(new Integer(i + 1)));
        }
        assertTrue(q.isEmpty());
    }

    /**
     * removeLastOccurrence(x) removes x and returns true if present
     */
    @Test
    public void testRemoveLastOccurrence() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(new Integer(i)));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(new Integer(i)));
            assertFalse(q.removeLastOccurrence(new Integer(i + 1)));
        }
        assertTrue(q.isEmpty());
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    @Test
    public void testContains() {
        NodeList q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.contains(new Integer(i)));
            assertEquals(i, q.pollFirst());
            assertFalse(q.contains(new Integer(i)));
        }
    }

    /**
     * clear removes all elements
     */
    @Test
    public void testClear() {
        NodeList q = populatedDeque(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
        assertTrue(q.add(new Integer(1)));
        assertFalse(q.isEmpty());
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * containsAll(c) is true when c contains a subset of elements
     */
    @Test
    public void testContainsAll() {
        NodeList q = populatedDeque(SIZE);
        NodeList p = new NodeList();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.containsAll(p));
            assertFalse(p.containsAll(q));
            assertTrue(p.add(new Integer(i)));
        }
        assertTrue(p.containsAll(q));
    }

    /**
     * retainAll(c) retains only those elements of c and reports true if changed
     */
    @Test
    public void testRetainAll() {
        NodeList q = populatedDeque(SIZE);
        NodeList p = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            boolean changed = q.retainAll(p);
            assertEquals(changed, (i > 0));
            assertTrue(q.containsAll(p));
            assertEquals(SIZE - i, q.size());
            p.removeFirst();
        }
    }

    /**
     * removeAll(c) removes only those elements of c and reports true if changed
     */
    @Test
    public void testRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            NodeList q = populatedDeque(SIZE);
            NodeList p = populatedDeque(i);
            assertTrue(q.removeAll(p));
            assertEquals(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                assertFalse(q.contains(p.removeFirst()));
            }
        }
    }

    private void checkToArray(NodeList q) {
        int size = q.size();
        Object[] o = q.toArray();
        assertEquals(size, o.length);
        Iterator it = q.iterator();
        for (int i = 0; i < size; i++) {
            Integer x = (Integer) it.next();
            assertEquals((Integer)o[0] + i, (int) x);
            assertSame(o[i], x);
        }
    }

    /**
     * toArray() contains all elements in FIFO order
     */
    @Test
    public void testToArray() {
        NodeList q = new NodeList();
        for (int i = 0; i < SIZE; i++) {
            checkToArray(q);
            q.addLast(i);
        }
        // Provoke wraparound
        for (int i = 0; i < SIZE; i++) {
            checkToArray(q);
            assertEquals(i, q.poll());
            q.addLast(SIZE + i);
        }
        for (int i = 0; i < SIZE; i++) {
            checkToArray(q);
            assertEquals(SIZE + i, q.poll());
        }
    }

    private void checkToArray2(NodeList q) {
        int size = q.size();
        Integer[] a1 = (size == 0) ? null : new Integer[size - 1];
        Integer[] a2 = new Integer[size];
        Integer[] a3 = new Integer[size + 2];
        if (size > 0) Arrays.fill(a1, 42);
        Arrays.fill(a2, 42);
        Arrays.fill(a3, 42);
        Integer[] b1 = (size == 0) ? null : (Integer[]) q.toArray(a1);
        Integer[] b2 = (Integer[]) q.toArray(a2);
        Integer[] b3 = (Integer[]) q.toArray(a3);
        assertSame(a2, b2);
        assertSame(a3, b3);
        Iterator it = q.iterator();
        for (int i = 0; i < size; i++) {
            Integer x = (Integer) it.next();
            assertSame(b1[i], x);
            assertEquals(b1[0] + i, (int) x);
            assertSame(b2[i], x);
            assertSame(b3[i], x);
        }
        assertNull(a3[size]);
        assertEquals(42, (int) a3[size + 1]);
        if (size > 0) {
            assertNotSame(a1, b1);
            assertEquals(size, b1.length);
            for (int i = 0; i < a1.length; i++) {
                assertEquals(42, (int) a1[i]);
            }
        }
    }

    /**
     * toArray(a) contains all elements in FIFO order
     */
    @Test
    public void testToArray2() {
        NodeList q = new NodeList();
        for (int i = 0; i < SIZE; i++) {
            checkToArray2(q);
            q.addLast(i);
        }
        // Provoke wraparound
        for (int i = 0; i < SIZE; i++) {
            checkToArray2(q);
            assertEquals(i, q.poll());
            q.addLast(SIZE + i);
        }
        for (int i = 0; i < SIZE; i++) {
            checkToArray2(q);
            assertEquals(SIZE + i, q.poll());
        }
    }

    /**
     * toArray(null) throws NullPointerException
     */
    @Test
    public void testToArray_NullArg() {
        NodeList l = new NodeList();
        l.add(new Object());
        try {
            l.toArray(null);
            fail("NullPointerException expected");
        } catch (NullPointerException success) {}
    }

    /**
     * toArray(incompatible array type) throws ArrayStoreException
     */
    @Test
    public void testToArray1_BadArg() {
        NodeList l = new NodeList();
        l.add(new Integer(5));
        try {
            l.toArray(new String[10]);
            fail("ArrayStoreException expected");
        } catch (ArrayStoreException success) {}
    }

    /**
     * Iterator iterates through all elements
     */
    @Test
    public void testIterator() {
        NodeList q = populatedDeque(SIZE);
        Iterator it = q.iterator();
        int i;
        for (i = 0; it.hasNext(); i++)
            assertTrue(q.contains(it.next()));
        assertEquals(i, SIZE);
        assertFalse(it.hasNext());
    }

    /**
     * iterator of empty collection has no elements
     */
    @Test
    public void testEmptyIterator() {
        Deque c = new NodeList();
        assertFalse(c.iterator().hasNext());
        assertFalse(c.descendingIterator().hasNext());
    }

    /**
     * Iterator ordering is FIFO
     */
    @Test
    public void testIteratorOrdering() {
        final NodeList q = new NodeList();
        q.add(ONE);
        q.add(TWO);
        q.add(THREE);
        int k = 0;
        for (Iterator it = q.iterator(); it.hasNext();) {
            assertEquals(++k, it.next());
        }
        assertEquals(3, k);

        // CN: additional test for descending ListIterator
        for (ListIterator it = q.listIterator(q.size()); it.hasPrevious();) {
            assertEquals(k--, it.previous());
        }
        assertEquals(0, k);
    }

    /**
     * iterator.remove() removes current element
     */
    @Test
    public void testIteratorRemove() {
        final NodeList q = new NodeList();
        final Random rng = new Random();
        for (int iters = 0; iters < 100; ++iters) {
            int max = rng.nextInt(5) + 2;
            int split = rng.nextInt(max - 1) + 1;
            for (int j = 1; j <= max; ++j)
                q.add(new Integer(j));
            Iterator it = q.iterator();
            for (int j = 1; j <= split; ++j)
                assertEquals(it.next(), new Integer(j));
            it.remove();
            assertEquals(it.next(), new Integer(split + 1));
            for (int j = 1; j <= split; ++j)
                q.remove(new Integer(j));
            it = q.iterator();
            for (int j = split + 1; j <= max; ++j) {
                assertEquals(it.next(), new Integer(j));
                it.remove();
            }
            assertFalse(it.hasNext());
            assertTrue(q.isEmpty());
        }
    }

    /**
     * Descending iterator iterates through all elements
     */
    @Test
    public void testDescendingIterator() {
        NodeList q = populatedDeque(SIZE);
        int i = 0;
        Iterator it = q.descendingIterator();
        while (it.hasNext()) {
            assertTrue(q.contains(it.next()));
            ++i;
        }
        assertEquals(i, SIZE);
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
    }

    /**
     * Descending iterator ordering is reverse FIFO
     */
    @Test
    public void testDescendingIteratorOrdering() {
        final NodeList q = new NodeList();
        for (int iters = 0; iters < 100; ++iters) {
            q.add(new Integer(3));
            q.add(new Integer(2));
            q.add(new Integer(1));
            int k = 0;
            for (Iterator it = q.descendingIterator(); it.hasNext();) {
                assertEquals(++k, it.next());
            }

            assertEquals(3, k);
            q.remove();
            q.remove();
            q.remove();
        }
    }

    /**
     * descendingIterator.remove() removes current element
     */
    @Test
    public void testDescendingIteratorRemove() {
        final NodeList q = new NodeList();
        final Random rng = new Random();
        for (int iters = 0; iters < 100; ++iters) {
            int max = rng.nextInt(5) + 2;
            int split = rng.nextInt(max - 1) + 1;
            for (int j = max; j >= 1; --j)
                q.add(new Integer(j));
            Iterator it = q.descendingIterator();
            for (int j = 1; j <= split; ++j)
                assertEquals(it.next(), new Integer(j));
            it.remove();
            assertEquals(it.next(), new Integer(split + 1));
            for (int j = 1; j <= split; ++j)
                q.remove(new Integer(j));
            it = q.descendingIterator();
            for (int j = split + 1; j <= max; ++j) {
                assertEquals(it.next(), new Integer(j));
                it.remove();
            }
            assertFalse(it.hasNext());
            assertTrue(q.isEmpty());
        }
    }

    /**
     * toString() contains toStrings of elements
     */
    @Test
    public void testToString() {
        NodeList q = populatedDeque(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }
}

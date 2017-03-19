package org.kynosarges.tektosyne;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

/**
 * Provides unit tests for class {@link NodeList}, part two.
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 * <p>
 * CN: The original test suite was written for testing {@link LinkedList}.
 * I added some required fields from a superclass which is not present here.
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/LinkedListTest.java?view=co
 * </p>
 * @author Christoph Nahr
 * @version 6.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked"})
public class NodeList2Test {
    /**
     * The number of elements to place in collections, arrays, etc.
     */
    private static final int SIZE = 20;

    private static final Integer ONE   = new Integer(1);
    private static final Integer TWO   = new Integer(2);
    private static final Integer THREE = new Integer(3);
    private static final Integer FOUR  = new Integer(4);

    /**
     * Returns a new queue of given size containing consecutive
     * Integers 0 ... n.
     */
    private NodeList<Integer> populatedQueue(int n) {
        NodeList<Integer> q = new NodeList<Integer>();
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; ++i)
            assertTrue(q.offer(new Integer(i)));
        assertFalse(q.isEmpty());
        assertEquals(n, q.size());
        return q;
    }

    /**
     * new queue is empty
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
     * Queue contains all elements of collection used to initialize
     */
    @Test
    public void testConstructor6() {
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i)
            ints[i] = i;
        NodeList q = new NodeList(Arrays.asList(ints));
        for (int i = 0; i < SIZE; ++i)
            assertEquals(ints[i], q.poll());
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
        q.remove();
        q.remove();
        assertTrue(q.isEmpty());
    }

    /**
     * size changes when elements added and removed
     */
    @Test
    public void testSize() {
        NodeList q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(SIZE - i, q.size());
            q.remove();
        }
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.size());
            q.add(new Integer(i));
        }
    }

    /**
     * Offer succeeds
     */
    @Test
    public void testOffer() {
        NodeList q = new NodeList();
        assertTrue(q.offer(new Integer(0)));
        assertTrue(q.offer(new Integer(1)));
    }

    /**
     * add succeeds
     */
    @Test
    public void testAdd() {
        NodeList q = new NodeList();
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.size());
            assertTrue(q.add(new Integer(i)));
        }
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
     * Queue contains all elements, in traversal order, of successful addAll
     */
    @Test
    public void testAddAll5() {
        Integer[] empty = new Integer[0];
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i)
            ints[i] = i;
        NodeList q = new NodeList();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(ints)));
        for (int i = 0; i < SIZE; ++i)
            assertEquals(ints[i], q.poll());
    }

    /**
     * addAll with too large an index throws IOOBE
     */
    @Test
    public void testAddAll2_IndexOutOfBoundsException() {
        NodeList l = new NodeList();
        l.add(new Object());
        NodeList m = new NodeList();
        m.add(new Object());
        try {
            l.addAll(4,m);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException success) {}
    }

    /**
     * addAll with negative index throws IOOBE
     */
    @Test
    public void testAddAll4_BadIndex() {
        NodeList l = new NodeList();
        l.add(new Object());
        NodeList m = new NodeList();
        m.add(new Object());
        try {
            l.addAll(-1,m);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException success) {}
    }

    /**
     * poll succeeds unless empty
     */
    @Test
    public void testPoll() {
        NodeList q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * peek returns next element, or null if empty
     */
    @Test
    public void testPeek() {
        NodeList q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.peek());
            assertEquals(i, q.poll());
            assertTrue(q.peek() == null ||
                       !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /**
     * element returns next element, or throws NSEE if empty
     */
    @Test
    public void testElement() {
        NodeList q = populatedQueue(SIZE);
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
     * remove removes next element, or throws NSEE if empty
     */
    @Test
    public void testRemove() {
        NodeList q = populatedQueue(SIZE);
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
        NodeList q = populatedQueue(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.contains(i));
            assertTrue(q.remove((Integer)i));
            assertFalse(q.contains(i));
            assertTrue(q.contains(i - 1));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.contains(i));
            assertTrue(q.remove((Integer)i));
            assertFalse(q.contains(i));
            assertFalse(q.remove((Integer)(i + 1)));
            assertFalse(q.contains(i + 1));
        }
        assertTrue(q.isEmpty());
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    @Test
    public void testContains() {
        NodeList q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.contains(new Integer(i)));
            q.poll();
            assertFalse(q.contains(new Integer(i)));
        }
    }

    /**
     * clear removes all elements
     */
    @Test
    public void testClear() {
        NodeList q = populatedQueue(SIZE);
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
        NodeList q = populatedQueue(SIZE);
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
        NodeList q = populatedQueue(SIZE);
        NodeList p = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            boolean changed = q.retainAll(p);
            if (i == 0)
                assertFalse(changed);
            else
                assertTrue(changed);

            assertTrue(q.containsAll(p));
            assertEquals(SIZE - i, q.size());
            p.remove();
        }
    }

    /**
     * removeAll(c) removes only those elements of c and reports true if changed
     */
    @Test
    public void testRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            NodeList q = populatedQueue(SIZE);
            NodeList p = populatedQueue(i);
            assertTrue(q.removeAll(p));
            assertEquals(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                Integer x = (Integer)(p.remove());
                assertFalse(q.contains(x));
            }
        }
    }

    /**
     * toArray contains all elements in FIFO order
     */
    @Test
    public void testToArray() {
        NodeList q = populatedQueue(SIZE);
        Object[] o = q.toArray();
        for (int i = 0; i < o.length; i++)
            assertSame(o[i], q.poll());
    }

    /**
     * toArray(a) contains all elements in FIFO order
     */
    @Test
    public void testToArray2() {
        NodeList<Integer> q = populatedQueue(SIZE);
        Integer[] ints = new Integer[SIZE];
        Integer[] array = q.toArray(ints);
        assertSame(ints, array);
        for (int i = 0; i < ints.length; i++)
            assertSame(ints[i], q.poll());
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
     * iterator iterates through all elements
     */
    @Test
    public void testIterator() {
        NodeList q = populatedQueue(SIZE);
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
        assertFalse(new NodeList().iterator().hasNext());
    }

    /**
     * iterator ordering is FIFO
     */
    @Test
    public void testIteratorOrdering() {
        final NodeList q = new NodeList();
        q.add(new Integer(1));
        q.add(new Integer(2));
        q.add(new Integer(3));
        int k = 0;
        for (Iterator it = q.iterator(); it.hasNext();) {
            assertEquals(++k, it.next());
        }

        assertEquals(3, k);
    }

    /**
     * iterator.remove removes current element
     */
    @Test
    public void testIteratorRemove() {
        final NodeList q = new NodeList();
        q.add(new Integer(1));
        q.add(new Integer(2));
        q.add(new Integer(3));
        Iterator it = q.iterator();
        assertEquals(1, it.next());
        it.remove();
        it = q.iterator();
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Descending iterator iterates through all elements
     */
    @Test
    public void testDescendingIterator() {
        NodeList q = populatedQueue(SIZE);
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
        q.add(new Integer(3));
        q.add(new Integer(2));
        q.add(new Integer(1));
        int k = 0;
        for (Iterator it = q.descendingIterator(); it.hasNext();) {
            assertEquals(++k, it.next());
        }

        assertEquals(3, k);
    }

    /**
     * descendingIterator.remove removes current element
     */
    @Test
    public void testDescendingIteratorRemove() {
        final NodeList q = new NodeList();
        q.add(THREE);
        q.add(TWO);
        q.add(ONE);
        Iterator it = q.descendingIterator();
        it.next();
        it.remove();
        it = q.descendingIterator();
        assertSame(it.next(), TWO);
        assertSame(it.next(), THREE);
        assertFalse(it.hasNext());
    }

    /**
     * toString contains toStrings of elements
     */
    @Test
    public void testToString() {
        NodeList q = populatedQueue(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * peek returns element inserted with addFirst
     */
    @Test
    public void testAddFirst() {
        NodeList q = populatedQueue(3);
        q.addFirst(FOUR);
        assertSame(FOUR, q.peek());
    }

    /**
     * peekFirst returns element inserted with push
     */
    @Test
    public void testPush() {
        NodeList q = populatedQueue(3);
        q.push(FOUR);
        assertSame(FOUR, q.peekFirst());
    }

    /**
     * pop removes next element, or throws NSEE if empty
     */
    @Test
    public void testPop() {
        NodeList q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.pop());
        }
        try {
            q.pop();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException success) {}
    }

    /**
     * OfferFirst succeeds
     */
    @Test
    public void testOfferFirst() {
        NodeList q = new NodeList();
        assertTrue(q.offerFirst(new Integer(0)));
        assertTrue(q.offerFirst(new Integer(1)));
    }

    /**
     * OfferLast succeeds
     */
    @Test
    public void testOfferLast() {
        NodeList q = new NodeList();
        assertTrue(q.offerLast(new Integer(0)));
        assertTrue(q.offerLast(new Integer(1)));
    }

    /**
     * pollLast succeeds unless empty
     */
    @Test
    public void testPollLast() {
        NodeList q = populatedQueue(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            assertEquals(i, q.pollLast());
        }
        assertNull(q.pollLast());
    }

    /**
     * peekFirst returns next element, or null if empty
     */
    @Test
    public void testPeekFirst() {
        NodeList q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.peekFirst());
            assertEquals(i, q.pollFirst());
            assertTrue(q.peekFirst() == null ||
                       !q.peekFirst().equals(i));
        }
        assertNull(q.peekFirst());
    }

    /**
     * peekLast returns next element, or null if empty
     */
    @Test
    public void testPeekLast() {
        NodeList q = populatedQueue(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            assertEquals(i, q.peekLast());
            assertEquals(i, q.pollLast());
            assertTrue(q.peekLast() == null ||
                       !q.peekLast().equals(i));
        }
        assertNull(q.peekLast());
    }

    @Test
    public void testFirstElement() {
        NodeList q = populatedQueue(SIZE);
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
     * getLast returns next element, or throws NSEE if empty
     */
    @Test
    public void testLastElement() {
        NodeList q = populatedQueue(SIZE);
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
     * removeFirstOccurrence(x) removes x and returns true if present
     */
    @Test
    public void testRemoveFirstOccurrence() {
        NodeList q = populatedQueue(SIZE);
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
        NodeList q = populatedQueue(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(new Integer(i)));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(new Integer(i)));
            assertFalse(q.removeLastOccurrence(new Integer(i + 1)));
        }
        assertTrue(q.isEmpty());
    }
}

package org.kynosarges.tektosyne;

import java.util.*;

/**
 * Provides a generic doubly-linked list that exposes its node structure.
 * Provides a generic doubly-linked list like the standard {@link LinkedList},
 * but publicly exposes its {@link Node} structure. This allows for O(1) navigation,
 * insertion, and removal at arbitrary positions for which {@link Node} instances
 * have already been obtained, rather than performing an O(n) lookup each time.
 * <p>
 * Java Collections Framework features, including index access and direct access
 * to element values stored in {@link Node}, are provided by implementing
 * {@link Deque} and extending {@link AbstractSequentialList}. Iterators “fail fast”
 * by checking for concurrent modifications by non-iterator methods.</p>
 * 
 * @param <T> the type of all elements in the {@link NodeList}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class NodeList<T> extends AbstractSequentialList<T> implements Deque<T> {

    private Node<T> _first, _last;
    private int _size;

    /**
     * Creates an empty {@link NodeList}.
     */
    public NodeList() { }

    /**
     * Creates a {@link NodeList} containing the elements of the specified {@link Collection}.
     * All {@code collection} elements are added in iteration order.
     * 
     * @param collection the {@link Collection} whose elements to add
     * @throws NullPointerException if {@code collection} is {@code null} or contains any {@code null} elements
     */
    public NodeList(Collection<? extends T> collection) {
        for (T value: collection)
            addLast(value);
    }

    /**
     * Gets the first {@link Node} in the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the first {@link Node} in the {@link NodeList},
     *         or {@code null} if the {@link NodeList} is empty
     */
    public Node<T> first() {
        return _first;
    }

    /**
     * Gets the last {@link Node} in the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the last {@link Node} in the {@link NodeList},
     *         or {@code null} if the {@link NodeList} is empty
     */
    public Node<T> last() {
        return _last;
    }

    /**
     * Adds the specified element after the specified {@link Node}.
     * This is an O(1) operation.
     * 
     * @param node the {@link Node} after which to add {@code value}
     * @param value the element to add
     * @throws IllegalArgumentException if {@code node} is not part of the {@link NodeList}
     * @throws NullPointerException if {@code node} or {@code value} is {@code null}
     */
    public void addAfter(Node<T> node, T value) {
        if (node._owner != this)
            throw new IllegalArgumentException("node not in list");

        final Node<T> newNode = new Node<>(this, value);
        _size++;
        modCount++;

        if (node == _last)
            _last = newNode;
        else {
            node._next._previous = newNode;
            newNode._next = node._next;
        }

        node._next = newNode;
        newNode._previous = node;

        assert(_first._previous == null);
        assert(_last._next == null);
    }

    /**
     * Adds the specified element before the specified {@link Node}.
     * This is an O(1) operation.
     * 
     * @param node the {@link Node} before which to add {@code value}
     * @param value the element to add
     * @throws IllegalArgumentException if {@code node} is not part of the {@link NodeList}
     * @throws NullPointerException if {@code node} or {@code value} is {@code null}
     */
    public void addBefore(Node<T> node, T value) {
        if (node._owner != this)
            throw new IllegalArgumentException("node not in list");

        final Node<T> newNode = new Node<>(this, value);
        _size++;
        modCount++;

        if (node == _first) 
            _first = newNode;
        else {
            node._previous._next = newNode;
            newNode._previous = node._previous;
        }

        node._previous = newNode;
        newNode._next = node;

        assert(_first._previous == null);
        assert(_last._next == null);
    }

    /**
     * Counts the number of elements in the {@link NodeList}.
     * This is an O(n) operation, counting each {@link Node} from {@link #first}
     * to {@link #last}. Intended for testing the internally tracked {@link #size}.
     * 
     * @return the number of elements in the {@link NodeList}
     */
    public int countNodes() {
        int size = 0;

        for (Node<T> node = _first; node != null; node = node._next)
            size++;

        return size;
    }

    /**
     * Finds the first {@link Node} in the {@link NodeList} that contains the specified element.
     * Searches the {@link NodeList} from {@link #first} to {@link #last}, using
     * {@link Object#equals} to compare elements. This is an O(n) operation.
     * 
     * @param value the element to search
     * @return the first {@link Node} in the {@link NodeList} whose {@link Node#value}
     *         equals {@code value}, or {@code null} if no such {@link Node} exists
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public Node<T> findFirstNode(T value) {
        if (value == null)
            throw new NullPointerException("value");

        for (Node<T> node = _first; node != null; node = node._next)
            if (value.equals(node._value))
                return node;

        return null;
    }

    /**
     * Finds the last {@link Node} in the {@link NodeList} that contains the specified element.
     * Searches the {@link NodeList} from {@link #last} to {@link #first}, using
     * {@link Object#equals} to compare elements. This is an O(n) operation.
     * 
     * @param value the element to search
     * @return the last {@link Node} in the {@link NodeList} whose {@link Node#value}
     *         equals {@code value}, or {@code null} if no such {@link Node} exists
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public Node<T> findLastNode(T value) {
        if (value == null)
            throw new NullPointerException("value");

        for (Node<T> node = _last; node != null; node = node._previous)
            if (value.equals(node._value))
                return node;

        return null;
    }

    /**
     * Gets the {@link Node} at the specified position in the {@link NodeList}.
     * Uses the internally tracked {@link #size} to search from {@link #last}
     * rather than {@link #first} if {@code index} is greater than {@link #size}/2.
     * This is an O(min({@code index}, {@link #size} – {@code index})) operation.
     * 
     * @param index the index of the {@link Node} to get
     * @return the {@link Node} at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is less than zero,
     *         or equal to or greater than {@link #size}
     */
    public Node<T> getNode(int index) {
        if (index < _size / 2) {
            int i = 0;
            for (Node<T> node = _first; node != null; node = node._next, i++)
                if (i == index) return node;

            // only reachable if index < 0
            throw new IndexOutOfBoundsException("index < 0");
        } else {
            int i = _size - 1;
            for (Node<T> node = _last; node != null; node = node._previous, i--)
                if (i == index) return node;

            // only reachable if index >= size
            throw new IndexOutOfBoundsException("index >= size");
        }
    }

    /**
     * Removes the specified {@link Node} from the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @param node the {@link Node} to remove
     * @throws IllegalArgumentException if {@code node} is not part of the {@link NodeList}
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public void remove(Node<T> node) {
        if (node._owner != this)
            throw new IllegalArgumentException("node not in list");

        _size--;
        modCount++;
        node._owner = null;

        if (_first == _last) {
            assert(node == _first);
            _first = _last = null;
        }
        else if (node == _first) {
            _first = node._next;
            _first._previous = null;
        }
        else if (node == _last) {
            _last = node._previous;
            _last._next = null;
        }
        else {
            node._previous._next = node._next;
            node._next._previous = node._previous;
        }

        node._previous = null;
        node._next = null;
    }

    // ----- AbstractSequentialList overrides -----

    /**
     * Adds the specified element at the end of the {@link NodeList}.
     * Calls {@link #addLast} with {@code value}. This is an O(1) operation.
     * 
     * @param value the element to add
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public boolean add(T value) {
        addLast(value);
        return true;
    }

    /**
     * Adds the specified element at the specified position in the {@link NodeList}.
     * This is an O(1) operation if {@code index} equals {@link #size} in which case
     * {@code value} is added at the end. Otherwise, this is an O({@code index})
     * operation, as described in {@link #getNode}.
     * 
     * @param index the index at which to add {@code value}
     * @param value the element to add at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is less than zero or greater than {@link #size}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public void add(int index, T value) {
        if (index == _size)
            addLast(value);
        else {
            final Node<T> node = getNode(index);
            addBefore(node, value);
        }
    }

    /**
     * Adds all specified elements at the specified position in the {@link NodeList}.
     * This is an O(m) operation if {@code index} equals {@link #size} in which case
     * {@code collection} is added at the end. Otherwise, this is an O({@code index} + m)
     * operation, as described in {@link #getNode}. Here m represents the size of the
     * {@code collection}. All {@code collection} elements are added in iteration order.
     * 
     * @param index the index at which to add {@code collection}
     * @param collection the {@link Collection} to add at {@code index}
     * @return {@code true} if {@code collection} contains any elements, else {@code false}
     * @throws IndexOutOfBoundsException if {@code index} is less than zero or greater than {@link #size}
     * @throws NullPointerException if {@code collection} is {@code null} or contains any {@code null} elements
     */
    @Override
    public boolean addAll(int index, Collection<? extends T> collection) {
        if (index == _size)
            for (T value: collection)
                addLast(value);
        else {
            final Node<T> node = getNode(index);
            for (T value: collection)
                addBefore(node, value);
        }

        return !collection.isEmpty();
    }

    /**
     * Removes all elements from the {@link NodeList}.
     * Does nothing if the {@link NodeList} is already empty. Otherwise
     * this is an O(n) operation, as all node links are set to {@code null}.
     */
    @Override
    public void clear() {
        if (_size == 0) {
            assert(_first == null);
            assert(_last == null);
            return;
        }

        for (Node<T> node = _first; node != null; node = node._next) {
            node._owner = null;
            if (node._previous != null) {
                node._previous._next = null;
                node._previous = null;
            }
        }

        _last._previous = null;
        _first = _last = null;
        _size = 0;
        modCount++;
    }

    /**
     * Determines whether the {@link NodeList} contains the specified element.
     * Attempts to cast {@code obj} to <b>T</b>, then calls {@link #findFirstNode}
     * and succeeds exactly if a {@link Node} was found. This is an O(n) operation.
     * 
     * @param obj the element to examine
     * @return {@code true} if {@code obj} was found, else {@code false}
     * @throws ClassCastException if {@code obj} cannot be cast to <b>T</b>
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @Override
    public boolean contains(Object obj) {
        @SuppressWarnings("unchecked")
        final Node<T> node = findFirstNode((T) obj);
        return (node != null);
    }

    /**
     * Gets the element at the specified position in the {@link NodeList}.
     * This is an O({@code index}) operation, as described in {@link #getNode}.
     * 
     * @param index the index of the element to get
     * @return the element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is less than zero,
     *                                   or equal to or greater than {@link #size}
     */
    @Override
    public T get(int index) {
        final Node<T> node = getNode(index);
        return node._value;
    }

    /**
     * Indicates whether the {@link NodeList} is empty.
     * This is an O(1) operation.
     * 
     * @return {@code true} if the {@link NodeList} is empty, else {@code false}
     */
    @Override
    public boolean isEmpty() {
        return (_size == 0);
    }

    /**
     * Returns a {@link ListIterator} over the elements in the {@link NodeList},
     * starting at the specified position.
     * The {@link ListIterator} is positioned after the last element
     * if {@code index} equals {@link #size}. Otherwise, this is an
     * O({@code index}) operation, as described in {@link #getNode}.
     * 
     * @param index the index of the first element to be returned by {@link ListIterator#next}
     * @return a {@link ListIterator} for the {@link NodeList}, starting at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is less than zero or greater than {@link #size}
     */
    @Override
    public ListIterator<T> listIterator(int index) {
        return new NodeIterator(index);
    }

    /**
     * Removes the element at the specified position in the {@link NodeList}.
     * This is an O({@code index}) operation, as described in {@link #getNode}.
     * 
     * @param index the index of the element to remove
     * @return the removed element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is less than zero,
     *                                   or equal to or greater than {@link #size}
     */
    @Override
    public T remove(int index) {
        final Node<T> node = getNode(index);
        remove(node);
        return node._value;
    }

    /**
     * Removes the first occurence of the specified element from the {@link NodeList}.
     * Returns {@link #removeFirstOccurrence}. This is an O(n) operation.
     * 
     * @param obj the element to remove
     * @return {@code true} if {@code obj} was found and removed, else {@code false}
     * @throws ClassCastException if {@code obj} cannot be cast to <b>T</b>
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @Override
    public boolean remove(Object obj) {
        return removeFirstOccurrence(obj);
    }

    /**
     * Removes all elements within the specified index range from the {@link NodeList}.
     * This is an O({@code index}) operation, as described in {@link #getNode},
     * followed by an O(m) operation where m is the number of elements to remove.
     * 
     * @param fromIndex the index of the first element to remove (inclusive)
     * @param toIndex the index of the last element to remove (exclusive)
     * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex}
     *                                   is less than zero or greater than {@link #size}
     */
    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        if (fromIndex > _size)
            throw new IndexOutOfBoundsException("toIndex > size");

        if (fromIndex == _size || fromIndex == toIndex)
            return;

        Node<T> node = getNode(fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            final Node<T> next = node._next;
            remove(node);
            node = next;
        }
    }

    /**
     * Sets the specified position in the {@link NodeList} to the specified element.
     * This is an O({@code index}) operation, as described in {@link #getNode}.
     * Calling {@code set} will <em>not</em> cause existing iterators to throw
     * {@link ConcurrentModificationException} as it involves no structural change.
     * 
     * @param index the index at which to store {@code value}
     * @param value the element to store at {@code index}
     * @return the element previously stored at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is less than zero,
     *                                   or equal to or greater than {@link #size}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public T set(int index, T value) {
        if (value == null)
            throw new NullPointerException("value");

        final Node<T> node = getNode(index);
        final T oldValue = node._value;
        node._value = value;

        return oldValue;
    }

    /**
     * Gets the number of elements in the {@link NodeList}.
     * This is an O(1) operation, as the {@link NodeList} keeps a running count of its elements.
     * 
     * @return the number of elements in the {@link NodeList}
     */
    @Override
    public int size() {
        return _size;
    }

    // ----- Deque implementation -----

    /**
     * Adds the specified element at the start of the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @param value the element to add
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public void addFirst(T value) {
        final Node<T> newNode = new Node<>(this, value);
        _size++;
        modCount++;

        if (_first == null) {
            assert(_last == null);
            _first = _last = newNode;
        } else {
            assert(_last != null);
            _first._previous = newNode;
            newNode._next = _first;
            _first = newNode;
        }

        assert(_first._previous == null);
        assert(_last._next == null);
    }

    /**
     * Adds the specified element at the end of the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @param value the element to add
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public void addLast(T value) {
        final Node<T> newNode = new Node<>(this, value);
        _size++;
        modCount++;

        if (_first == null) {
            assert(_last == null);
            _first = _last = newNode;
        } else {
            assert(_last != null);
            _last._next = newNode;
            newNode._previous = _last;
            _last = newNode;
        }

        assert(_first._previous == null);
        assert(_last._next == null);
    }

    /**
     * Returns an {@link Iterator} over the elements in the {@link NodeList}
     * in reverse order, starting after the last element.
     * This is an O(1) operation.
     * 
     * @return an {@link Iterator} for the {@link NodeList},
     *         in reverse order and starting after the last element
     */
    @Override
    public Iterator<T> descendingIterator() {
        return new ReverseNodeIterator();
    }

    /**
     * Retrieves, but does not remove, the first element in the {@link NodeList}.
     * Returns {@link #getFirst}. This is an O(1) operation.
     * 
     * @return the first element in the {@link NodeList}
     * @throws NoSuchElementException if the {@link NodeList} is empty
     */
    @Override
    public T element() {
        return getFirst();
    }

    /**
     * Retrieves, but does not remove, the first element in the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the first element in the {@link NodeList}
     * @throws NoSuchElementException if the {@link NodeList} is empty
     */
    @Override
    public T getFirst() {
        if (_first == null)
            throw new NoSuchElementException("empty list");

        return _first._value;
    }

    /**
     * Retrieves, but does not remove, the last element in the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the last element in the {@link NodeList}
     * @throws NoSuchElementException if the {@link NodeList} is empty
     */
    @Override
    public T getLast() {
        if (_last == null)
            throw new NoSuchElementException("empty list");

        return _last._value;
    }

    /**
     * Adds the specified element at the end of the {@link NodeList}.
     * Calls {@link #addLast} with {@code value}. Always succeeds as the
     * {@link NodeList} is not capacity-restricted. This is an O(1) operation.
     * 
     * @param value the element to add
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public boolean offer(T value) {
        addLast(value);
        return true;
    }

    /**
     * Adds the specified element at the start of the {@link NodeList}.
     * Calls {@link #addFirst} with {@code value}. Always succeeds as the
     * {@link NodeList} is not capacity-restricted. This is an O(1) operation.
     * 
     * @param value the element to add
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public boolean offerFirst(T value) {
        addFirst(value);
        return true;
    }

    /**
     * Adds the specified element at the end of the {@link NodeList}.
     * Calls {@link #addLast} with {@code value}. Always succeeds as the
     * {@link NodeList} is not capacity-restricted. This is an O(1) operation.
     * 
     * @param value the element to add
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public boolean offerLast(T value) {
        addLast(value);
        return true;
    }

    /**
     * Retrieves, but does not remove, the first element in the {@link NodeList}.
     * Returns {@link #peekFirst}. This is an O(1) operation.
     * 
     * @return the first element in the {@link NodeList},
     *         or {@code null} if the {@link NodeList} is empty
     */
    @Override
    public T peek() {
        return peekFirst();
    }

    /**
     * Retrieves, but does not remove, the first element in the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the first element in the {@link NodeList},
     *         or {@code null} if the {@link NodeList} is empty
     */
    @Override
    public T peekFirst() {
        return (_first == null ? null : _first._value);
    }

    /**
     * Retrieves, but does not remove, the last element in the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the last element in the {@link NodeList},
     *         or {@code null} if the {@link NodeList} is empty
     */
    @Override
    public T peekLast() {
        return (_last == null ? null : _last._value);
    }

    /**
     * Retrieves and removes the first element in the {@link NodeList}.
     * Returns {@link #pollFirst}. This is an O(1) operation.
     * 
     * @return the removed first element in the {@link NodeList},
     *         or {@code null} if the {@link NodeList} is empty
     */
    @Override
    public T poll() {
        return pollFirst();
    }

    /**
     * Retrieves and removes the first element in the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the removed first element in the {@link NodeList},
     *         or {@code null} if the {@link NodeList} is empty
     */
    @Override
    public T pollFirst() {
        return (_first == null ? null : removeFirst());
    }

    /**
     * Retrieves and removes the last element in the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the removed last element in the {@link NodeList},
     *         or {@code null} if the {@link NodeList} is empty
     */
    @Override
    public T pollLast() {
        return (_last == null ? null : removeLast());
    }

    /**
     * Retrieves and removes the first element from the {@link NodeList}.
     * Returns {@link #removeFirst}. This is an O(1) operation.
     * 
     * @return the removed first element in the {@link NodeList}
     * @throws NoSuchElementException if the {@link NodeList} is empty
     */
    @Override
    public T pop() {
        return removeFirst();
    }

    /**
     * Adds the specified element at the start of the {@link NodeList}.
     * Calls {@link #addFirst} with {@code value}. This is an O(1) operation.
     * 
     * @param value the element to add
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public void push(T value) {
        addFirst(value);
    }

    /**
     * Retrieves and removes the first element from the {@link NodeList}.
     * Returns {@link #removeFirst}. This is an O(1) operation.
     * 
     * @return the removed first element in the {@link NodeList}
     * @throws NoSuchElementException if the {@link NodeList} is empty
     */
    @Override
    public T remove() {
        return removeFirst();
    }

    /**
     * Retrieves and removes the first element from the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the removed first element in the {@link NodeList}
     * @throws NoSuchElementException if the {@link NodeList} is empty
     */
    @Override
    public T removeFirst() {
        if (_first == null)
            throw new NoSuchElementException("empty list");

        _size--;
        modCount++;

        _first._owner = null;
        assert(_first._previous == null);
        final T value = _first._value;

        if (_first == _last) {
            assert(_first._next == null);
            _first = _last = null;
        } else {
            _first = _first._next;
            _first._previous._next = null;
            _first._previous = null;
        }

        return value;
    }

    /**
     * Removes the first occurence of the specified element from the {@link NodeList}.
     * Attempts to cast {@code obj} to <b>T</b>, then calls {@link #findFirstNode}
     * and finally {@link #remove} on success. This is an O(n) operation.
     * 
     * @param obj the element to remove
     * @return {@code true} if {@code obj} was found and removed, else {@code false}
     * @throws ClassCastException if {@code obj} cannot be cast to <b>T</b>
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @Override
    public boolean removeFirstOccurrence(Object obj) {
        @SuppressWarnings("unchecked")
        final Node<T> node = findFirstNode((T) obj);
        if (node == null) return false;

        remove(node);
        return true;
    }

    /**
     * Retrieves and removes the last element from the {@link NodeList}.
     * This is an O(1) operation.
     * 
     * @return the removed last element in the {@link NodeList}
     * @throws NoSuchElementException if the {@link NodeList} is empty
     */
    @Override
    public T removeLast() {
        if (_last == null)
            throw new NoSuchElementException("empty list");

        _size--;
        modCount++;

        _last._owner = null;
        assert(_last._next == null);
        final T value = _last._value;

        if (_first == _last) {
            assert(_first._previous == null);
            _first = _last = null;
        } else {
            _last = _last._previous;
            _last._next._previous = null;
            _last._next = null;
        }

        return value;
    }

    /**
     * Removes the last occurence of the specified element from the {@link NodeList}.
     * Attempts to cast {@code obj} to <b>T</b>, then calls {@link #findLastNode}
     * and finally {@link #remove} on success. This is an O(n) operation.
     * 
     * @param obj the element to remove
     * @return {@code true} if {@code obj} was found and removed, else {@code false}
     * @throws ClassCastException if {@code obj} cannot be cast to <b>T</b>
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @Override
    public boolean removeLastOccurrence(Object obj) {
        @SuppressWarnings("unchecked")
        final Node<T> node = findLastNode((T) obj);
        if (node == null) return false;

        remove(node);
        return true;
    }

    /**
     * Provides a generic node within a {@link NodeList}.
     * @param <T> the type of all elements in the {@link NodeList}
     */
    public static final class Node<T> {

        private NodeList<T> _owner;
        private T _value;
        private Node<T> _next, _previous;

        /**
         * Creates a {@link Node} for the specified {@link NodeList} and element.
         * @param owner the {@link NodeList} that owns the {@link Node}
         * @param value the element to store in the {@link Node}
         * @throws NullPointerException if {@code owner} or {@code value} is {@code null}
         */
        private Node(NodeList<T> owner, T value) {
            if (owner == null)
                throw new NullPointerException("owner");
            if (value == null)
                throw new NullPointerException("value");

            this._owner = owner;
            this._value = value;
        }

        /**
         * Gets the next {@link Node} in the {@link NodeList}.
         * Returns {@code null} for the last {@link Node}, or for a {@link Node} that
         * has been removed from its {@link NodeList}. This is an O(1) operation.
         * 
         * @return the next {@link Node} in the {@link NodeList}
         */
        public Node<T> next() {
            return _next;
        }

        /**
         * Gets the {@link NodeList} that contains the {@link Node}.
         * Returns {@code null} if the {@link Node} has been removed from its {@link NodeList}.
         * Otherwise, returns the same valid object throughout the lifetime of the {@link Node}.
         * 
         * @return the {@link NodeList} that contains the {@link Node}
         */
        public NodeList<T> owner() {
            return _owner;
        }
        
        /**
         * Gets the previous {@link Node} in the {@link NodeList}.
         * Returns {@code null} for the first {@link Node}, or for a {@link Node} that
         * has been removed from its {@link NodeList}. This is an O(1) operation.
         * 
         * @return the previous {@link Node} in the {@link NodeList}
         */
        public Node<T> previous() {
            return _previous;
        }

        /**
         * Sets the element stored in the {@link Node}.
         * Calling {@link #setValue} will <em>not</em> cause existing iterators to throw
         * {@link ConcurrentModificationException} as it involves no structural change.
         * 
         * @param value the element stored in the {@link Node}
         * @throws NullPointerException if {@code value} is {@code null}
         */
        public void setValue(T value) {
            if (value == null)
                throw new NullPointerException("value");

            _value = value;
        }

        /**
         * Gets the element stored in the {@link Node}.
         * Remains unchanged when a {@link Node} is removed from its {@link NodeList}.
         * 
         * @return the element stored in the {@link Node}
         */
        public T value() {
            return _value;
        }
    }

    /**
     * Provides a {@link ListIterator} for the {@link NodeList}.
     * All supported operations except for index construction are O(1).
     */
    private final class NodeIterator implements ListIterator<T> {

        private int _nextIndex, _expectedModCount = modCount;
        private Node<T> _nextNode, _gotNode;

        /**
         * Creates a {@link NodeIterator}.
         * Positioned before the first element of the {@link NodeList}.
         */
        NodeIterator() {
            this._nextIndex = 0;
            this._nextNode = _first;
        }

        /**
         * Creates a {@link NodeIterator} starting at the specified position.
         * Positioned after the last element if {@code index} equals {@link #size}.
         * Otherwise, calls {@link #getNode} and has the same runtime behavior.
         * 
         * @param index the index of the first element to be returned by {@link #next}
         * @throws IndexOutOfBoundsException if {@code index} is less than zero or greater than {@link #size}
         */
        NodeIterator(int index) {
            this._nextIndex = index;
            this._nextNode = (index == _size ? null : getNode(index));
        }

        /**
         * Adds the specified element to the {@link NodeList}.
         * The specified {@code value} is added before the element that would be returned by
         * {@link #next}, and so becomes the element that will be returned by {@link #previous}.
         * 
         * @param value the element to insert
         * @throws ConcurrentModificationException if the {@link NodeList} was externally modified
         * @throws NullPointerException if {@code value} is {@code null}
         */
        @Override
        public void add(T value) {
            if (modCount != _expectedModCount)
                throw new ConcurrentModificationException();

            if (_nextNode == null)
                addLast(value);
            else
                addBefore(_nextNode, value);

            _expectedModCount++;
            _nextIndex++;
            _gotNode = null;
        }

        /**
         * Indicates whether the {@link ListIterator} has any next elements.
         * @return {@code true} if a subsequent call to {@link #next} would succeed, else {@code false}
         */
        @Override
        public boolean hasNext() {
            return (_nextNode != null);
        }

        /**
         * Indicates whether the {@link ListIterator} has any previous elements.
         * @return {@code true} if a subsequent call to {@link #previous} would succeed, else {@code false}
         */
        @Override
        public boolean hasPrevious() {
            return (_nextIndex > 0);
        }

        /**
         * Returns the next element in the {@link NodeList},
         * and moves the {@link ListIterator} forward.
         * 
         * @return the next element in the {@link NodeList}
         * @throws ConcurrentModificationException if the {@link NodeList} was externally modified
         * @throws NoSuchElementException if there is no next element
         */
        @Override
        public T next() {
            if (_nextNode == null)
                throw new NoSuchElementException("no next element");
            if (modCount != _expectedModCount)
                throw new ConcurrentModificationException();

            _nextIndex++;
            _gotNode = _nextNode;
            _nextNode = _nextNode._next;

            return _gotNode._value;
        }

        /**
         * Returns the index of the element returned by a subsequent call to {link #next}.
         * Returns {@link #size} if the {@link ListIterator} is at the end of the {@link NodeList}.
         * 
         * @return the index of the element returned by a subsequent call to {link #next},
         *         or {@link #size} if the {@link ListIterator} is at the end of the {@link NodeList}
         */
        @Override
        public int nextIndex() {
            return (_nextNode == null ? _nextIndex : _nextIndex + 1);
        }

        /**
         * Returns the previous element in the {@link NodeList},
         * and moves the {@link ListIterator} backward.
         * 
         * @return the previous element in the {@link NodeList}
         * @throws ConcurrentModificationException if the {@link NodeList} was externally modified
         * @throws NoSuchElementException if there is no previous element
         */
        @Override
        public T previous() {
            if (_nextIndex == 0)
                throw new NoSuchElementException("no previous element");
            if (modCount != _expectedModCount)
                throw new ConcurrentModificationException();

            _nextIndex--;
            _nextNode = (_nextNode == null ? _last : _nextNode._previous);
            _gotNode = _nextNode;

            return _gotNode._value;
        }

        /**
         * Returns the index of the element returned by a subsequent call to {link #previous}.
         * Returns -1 if the {@link ListIterator} is at the beginning of the {@link NodeList}.
         * 
         * @return the index of the element returned by a subsequent call to {link #previous},
         *         or -1 if the {@link ListIterator} is at the beginning of the {@link NodeList}
         */
        @Override
        public int previousIndex() {
            assert(_nextIndex >= 0);
            return (_nextIndex - 1);
        }

        /**
         * Removes the last returned element from the {@link NodeList}.
         * Removes the last element returned by {@link #next} or {@link #previous}.
         * Only possible if neither {@link #add} nor {@link #remove} were called since then.
         * 
         * @throws ConcurrentModificationException if the {@link NodeList} was externally modified
         * @throws IllegalStateException if neither {@link #next} nor {@link #previous}
         *         have been called, or {@link #add(Object)} or {@link #remove()} have
         *         been called after the last call to {@link #next} or {@link #previous}
         */
        @Override
        public void remove() {
            if (_gotNode == null)
                throw new IllegalStateException("no next/previous result");
            if (modCount != _expectedModCount)
                throw new ConcurrentModificationException();

            if (_nextNode == _gotNode)
                _nextNode = _nextNode._next;

            NodeList.this.remove(_gotNode);
            _expectedModCount++;
            _gotNode = null;
        }

        /**
         * Replaces the last returned element with the specified element.
         * Replaces the last element returned by {@link #next} or {@link #previous}.
         * Only possible if neither {@link #add(Object)} nor {@link #remove()}
         * were called since then.
         * 
         * @param value the element to replace the last returned element
         * @throws ConcurrentModificationException if the {@link NodeList} was externally modified
         * @throws IllegalStateException if neither {@link #next} nor {@link #previous}
         *         have been called, or {@link #add(Object)} or {@link #remove()} have
         *         been called after the last call to {@link #next} or {@link #previous}
         * @throws NullPointerException if {@code value} is {@code null}
         */
        @Override
        public void set(T value) {
            if (_gotNode == null)
                throw new IllegalStateException("no next/previous result");
            if (modCount != _expectedModCount)
                throw new ConcurrentModificationException();
            if (value == null)
                throw new NullPointerException("value");

            _gotNode._value = value;
        }
    }

    /**
     * Provides a descending {@link Iterator} for the {@link NodeList}.
     * Iterates over the {@link NodeList} in reverse order. All operations are O(1).
     */
    private final class ReverseNodeIterator implements Iterator<T> {

        private int _expectedModCount = modCount;
        private Node<T> _nextNode, _gotNode;

        /**
         * Creates a {@link ReverseNodeIterator}.
         * Positioned after the last element of the {@link NodeList}.
         */
        ReverseNodeIterator() {
            this._nextNode = _last;
        }

        /**
         * Indicates whether the {@link Iterator} has any next elements.
         * Since this {@link Iterator} operates in reverse order, this actually
         * indicates whether any <em>previous</em> elements are present.
         * 
         * @return {@code true} if a subsequent call to {@link #next} would succeed, else {@code false}
         */
        @Override
        public boolean hasNext() {
            return (_nextNode != null);
        }

        /**
         * Returns the next element in the {@link NodeList}, and moves the {@link Iterator} forward.
         * Since this {@link Iterator} operates in reverse order, this actually
         * moves the {@link Iterator} <em>backward</em> in the {@link NodeList}.
         * 
         * @return the next element in the {@link NodeList}
         * @throws ConcurrentModificationException if the {@link NodeList} was externally modified
         * @throws NoSuchElementException if there is no next element
         */
        @Override
        public T next() {
            if (_nextNode == null)
                throw new NoSuchElementException("no next element");
            if (modCount != _expectedModCount)
                throw new ConcurrentModificationException();

            _gotNode = _nextNode;
            _nextNode = _nextNode._previous;
            return _gotNode._value;
        }

        /**
         * Removes the last returned element from the {@link NodeList}.
         * Removes the last element returned by {@link #next}.
         * Only possible if {@link #remove()} was not called since then.
         * 
         * @throws ConcurrentModificationException if the {@link NodeList} was externally modified
         * @throws IllegalStateException if {@link #next} has not been called, or
         *         {@link #remove()} has been called after the last call to {@link #next}
         */
        @Override
        public void remove() {
            if (_gotNode == null)
                throw new IllegalStateException("no next result");
            if (modCount != _expectedModCount)
                throw new ConcurrentModificationException();

            NodeList.this.remove(_gotNode);
            _expectedModCount++;
            _gotNode = null;
        }
    }
}

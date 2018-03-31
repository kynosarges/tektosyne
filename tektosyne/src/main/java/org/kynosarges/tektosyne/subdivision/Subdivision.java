package org.kynosarges.tektosyne.subdivision;

import java.util.*;

import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.graph.*;

/**
 * Represents a planar subdivision as a doubly-connected edge list.
 * Represents a planar subdivision containing only straight bounded edges, i.e. a collection
 * of line segments in two-dimensional space that do not intersect except at their end points.
 * The vertices of the subdivision are the end points of all line segments. The entire
 * structure of edges and vertices constitutes the planar embedding of a graph.
 * <p>
 * In addition to edges and vertices, {@link Subdivision} also stores the faces formed by
 * all edges. Faces are any polygonal regions that are bounded by edges, whether on the inside,
 * on the outside, or both. Edges are represented by the {@link SubdivisionEdge} class,
 * and faces are represented by the {@link SubdivisionFace} class.</p>
 * <p>
 * {@link Subdivision} supports generic graph algorithms through its {@link Graph} implementation.
 * The graph nodes are the {@link PointD} coordinates of all vertices. Two nodes are considered
 * connected if an edge exists between their corresponding vertices. The distance measure is the
 * Euclidean distance between vertices.
 * <p>
 * The planar subdivision is implemented as the doubly-connected edge list described by Mark
 * de Berg et al., <em>Computational Geometry</em> (3rd ed.), Springer-Verlag 2008, p.29-43.
 * This implementation represents edges as “twin” pairs of half-edges.</p>
 * 
 * @author Christoph Nahr
 * @version 6.2.0
 */
public class Subdivision implements Graph<PointD> {
    /**
     * The epsilon used for coordinate comparisons within the {@link #vertices} collection.
     * Defines the maximum absolute difference at which vertex coordinates should be considered
     * equal. Always equals the {@link PointDComparator#epsilon} of the {@link PointDComparator}
     * responsible for ordering the keys of the {@link #vertices} collection.
     */
    public final double epsilon;

    /**
     * All {@link SubdivisionEdge} instances in the {@link Subdivision},
     * sorted by {@link SubdivisionEdge#key}.
     */
    private final NavigableMap<Integer, SubdivisionEdge> _edges;

    /**
     * All {@link SubdivisionFace} instances in the {@link Subdivision},
     * sorted by {@link SubdivisionFace#key}.
     */
    private final NavigableMap<Integer, SubdivisionFace> _faces;

    /**
     * All {@link PointD} vertices in the {@link Subdivision} with one of their
     * incident {@link SubdivisionEdge} instances, sorted lexicographically.
     */
    private final NavigableMap<PointD, SubdivisionEdge> _vertices;

    /**
     * The {@link Graph} regions associated with all {@link PointD} vertices, if any.
     */
    private final Map<PointD, PointD[]> _vertexRegions;

    /**
     * The maximum number of {@link SubdivisionEdge} instances originating from any vertex.
     */
    private int _connectivity;
    /**
     * The unique key for the next {@link SubdivisionEdge} instance.
     */
    private int _nextEdgeKey;
    /**
     * The unique key for the next {@link SubdivisionFace} instance.
     */
    private int _nextFaceKey;
    /**
     * The current y-coordinate of the sweep line of a plane sweep algorithm.
     */
    private double _cursorY;

    /**
     * Creates an empty {@link Subdivision}.
     * All collections are empty, except for {@link #faces} which contains
     * only the unbounded {@link SubdivisionFace}.
     * 
     * @param epsilon the epsilon used for coordinate comparisons within {@link #vertices}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     */
    public Subdivision(double epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        this.epsilon = epsilon;

        _edges = new TreeMap<>();
        _faces = new TreeMap<>();
        _vertices = new TreeMap<>(new PointDComparatorY(epsilon));
        _vertexRegions = new HashMap<>();

        // create unbounded face
        final SubdivisionFace face = new SubdivisionFace(this, _nextFaceKey++);
        _faces.put(face._key, face);
    }

    /**
     * Gets all {@link SubdivisionEdge} instances in the {@link Subdivision}.
     * Always contains an even number of elements since every edge in the {@link Subdivision}
     * is comprised of two {@link SubdivisionEdge} instances.
     * <p>
     * This collection is provided for convenience, unit testing, and faster edge scanning.
     * It is not strictly needed since a list of all {@link SubdivisionEdge} instances can be
     * obtained by iterating over all {@link #vertices}, e.g. using {@link #getEdgesByOrigin}.</p>
     * <p>
     * Maintaining the {@link #edges} collection consumes little extra runtime but a significant
     * amount of memory, so an alternative {@link Subdivision} implementation might choose to
     * remove this collection and create new {@link SubdivisionEdge} collections on demand.</p>
     * 
     * @return an unmodifiable {@link NavigableMap} mapping the {@link SubdivisionEdge#key}
     *         of each {@link SubdivisionEdge} instance to the corresponding instance
     */
    public NavigableMap<Integer, SubdivisionEdge> edges() {
        return Collections.unmodifiableNavigableMap(_edges);
    }

    /**
     * Gets all {@link SubdivisionFace} instances in the {@link Subdivision}.
     * Always contains at least one element which is the unbounded face. The unbounded
     * {@link SubdivisionFace} always has a {@link SubdivisionFace#key} of zero.
     * 
     * @return an unmodifiable {@link NavigableMap} mapping the {@link SubdivisionFace#key}
     *         of each {@link SubdivisionFace} instance to the corresponding instance
     */
    public NavigableMap<Integer, SubdivisionFace> faces() {
        return Collections.unmodifiableNavigableMap(_faces);
    }

    /**
     * Indicates whether the {@link Subdivision} is empty.
     * Returns {@code true} exactly if the {@link #edges} collection is empty. For any valid
     * {@link Subdivision}, that is the case exactly if the {@link #vertices} collection is
     * also empty, and the {@link #faces} collection contains only the unbounded face.
    * 
     * @return {@code true} if the {@link Subdivision} is empty except for the
     *         unbounded {@link SubdivisionFace}, else {@code false}
     */
    public boolean isEmpty() {
        return _edges.isEmpty();
    }

    /**
     * Gets a mapping of {@link #vertices} to {@link Graph} world regions.
     * Always returns an empty collection by default, as {@link Subdivision} does not inherently
     * associate world regions with {@link #vertices}. Clients must explicitly add any desired
     * key-and-value pairs. This collection is therefore modifiable.
     * <p>
     * {@link #getWorldRegion} attempts to return the polygonal region that {@link #vertexRegions}
     * associates with a specified {@link #vertices} key, i.e. {@link Graph} node, and returns
     * {@code null} if no association is found.</p>
     * 
     * @return a {@link Map} of {@link #vertices} keys, representing {@link Graph} nodes,
     *         to the corresponding {@link Graph} world regions
     */
    public Map<PointD, PointD[]> vertexRegions() {
        return _vertexRegions;
    }

    /**
     * Gets all {@link PointD} vertices with an incident {@link SubdivisionEdge} in the {@link Subdivision}.
     * Sorted lexicographically by {@link PointD} keys, using the ordering established
     * by {@link PointDComparatorY}. That is, keys are sorted first by {@link PointD#y}
     * and then by {@link PointD#x} coordinates.
     * <p>
     * Every {@link #vertices} key is associated with a valid {@link SubdivisionEdge}.
     * That is, a {@link Subdivision} never contains isolated points, only edges. If multiple
     * {@link SubdivisionEdge} instances originate from the same vertex, one is selected
     * arbitrarily for the {@link #vertices} collection, depending on the construction
     * order of the {@link Subdivision}.</p>
     * 
     * @return an unmodifiable {@link NavigableMap} mapping each {@link PointD} vertex in the
     *         {@link Subdivision} to a {@link SubdivisionEdge} that originates at the vertex
     */
    public NavigableMap<PointD, SubdivisionEdge> vertices() {
        return Collections.unmodifiableNavigableMap(_vertices);
    }

    /**
     * Adds a new edge to the {@link Subdivision} that connects the specified {@link PointD}
     * vertices, and returns additional information on any changed and added {@link #faces}.
     * First checks if the new edge would intersect any existing {@link #edges}, except
     * at the specified {@code start} and {@code end} coordinates, and fails if so.
     * <p>
     * Otherwise, creates two new {@link #edges} elements, from {@code start} to {@code end}
     * and vice versa. If {@code start} and/or {@code end} are not found in the {@link #vertices}
     * collection, the corresponding {@link #vertices} elements are added as well.</p>
     * <p>
     * If the added edge connects an inner cycle of the containing {@link SubdivisionFace} with
     * its outer cycle, the inner cycle is removed. If the added edge connects two inner cycles,
     * one of them is arbitrarily chosen for removal.</p>
     * <p>
     * If the added edge connects two half-edges within the same inner cycle, a new
     * {@link SubdivisionFace} for the resulting enclosed area is created. If the added edge
     * connects two half-edges within the outer cycle, a new {@link SubdivisionFace} for the
     * part enclosing the smaller area is created.</p>
     * 
     * @param start the {@link PointD} coordinates of the first vertex to connect
     * @param end the {@link PointD} coordinates of the second vertex to connect
     * @return an {@link AddEdgeResult} containing the result of the operation
     * @throws NullPointerException if {@code start} or {@code end} is {@code null}
     */
    public AddEdgeResult addEdge(PointD start, PointD end) {

        int changedFace = -1, addedFace = -1;
        if (start.equals(end))
            return new AddEdgeResult(null, changedFace, addedFace);

        final SubdivisionEdge startEdge = _vertices.get(start);
        final SubdivisionEdge endEdge = _vertices.get(end);

        SubdivisionFace face = null;
        SubdivisionEdge nextStartEdge = null, nextEndEdge = null;

        if (startEdge == null && endEdge == null)
            face = findFace(start);
        else {
            SubdivisionEdge previousStartEdge = null, previousEndEdge = null;

            // check for existing edge connecting vertices
            if (startEdge != null && endEdge != null)
                for (SubdivisionEdge edge: startEdge.originEdges())
                    if (edge._twin._origin.equals(end))
                        return null;

            // find neighboring edges in start vertex chain
            if (startEdge != null) {
                final SubdivisionEdge[] startNeighbors = startEdge.findEdgePosition(end);
                nextStartEdge = startNeighbors[0];
                previousStartEdge = startNeighbors[1];
                assert(nextStartEdge._previous == previousStartEdge._twin);
                face = nextStartEdge._face;
            }

            // find neighboring edges in end vertex chain
            if (endEdge != null) {
                final SubdivisionEdge[] endNeighbors = endEdge.findEdgePosition(start);
                nextEndEdge = endNeighbors[0];
                previousEndEdge = endNeighbors[1];
                assert(nextEndEdge._previous == previousEndEdge._twin);

                if (face == null)
                    face = nextEndEdge._face;
                else if (face != nextEndEdge._face)
                    return null;
            }

            assert(nextStartEdge != nextEndEdge);
            assert(previousStartEdge != previousEndEdge);
        }

        final LineD line = new LineD(start, end);
        int startInnerCycle = -1, endInnerCycle = -1;

        if (face._outerEdge != null) {
            SubdivisionEdge edge = face._outerEdge;
            do {
                // check for proper intersection with outer cycle edges
                final LineIntersection result = line.intersect(edge.toLine(), epsilon);
                if (result.existsBetween())
                    return null;

                edge = edge._next;
            } while (edge != face._outerEdge);
        }

        if (face._innerEdges != null)
            for (int i = 0; i < face._innerEdges.size(); i++) {
                final SubdivisionEdge innerEdge = face._innerEdges.get(i);
                SubdivisionEdge edge = innerEdge;
                do {
                    // check for proper intersection with inner cycle edge
                    final LineIntersection result = line.intersect(edge.toLine(), epsilon);
                    if (result.existsBetween())
                        return null;

                    // record inner cycle index for neighboring edge
                    if (edge == nextStartEdge) startInnerCycle = i;
                    else if (edge == nextEndEdge) endInnerCycle = i;

                    edge = edge._next;
                } while (edge != innerEdge);
            }

        // create half-edges and (if necessary) vertices
        final CreateEdgeResult result = createTwinEdges(start, end);
        final SubdivisionEdge newEdge = result.startEdge;
        newEdge._face = face;
        newEdge._twin._face = face;
        changedFace = face._key;

        /*
         * If the new edge connects two new vertices, we have a new single-edge inner cycle.
         * 
         * If the new edge connects an existing vertex and a new vertex, we have a new
         * zero-area protrusion of some existing cycle and don't need to do anything.
         * 
         * If the new edge connects two existing vertices and merges two different cycles,
         * one of them must be an inner cycle which is now obsolete and can be deleted.
         * 
         * If the new edge connects two existing vertices and establishes a new connection
         * within the same cycle, we have a new outer cycle that constitutes a new face.
         */
        if (startEdge == null && endEdge == null) {
            face.addInnerEdge(newEdge);
        }
        else if (startEdge != null && endEdge != null) {
            if (startInnerCycle != endInnerCycle) {
                if (endInnerCycle >= 0)
                    face._innerEdges.remove(endInnerCycle);
                else {
                    assert(startInnerCycle >= 0);
                    face._innerEdges.remove(startInnerCycle);
                }
            } else {
                assert(startInnerCycle == endInnerCycle);
                SubdivisionEdge newFaceEdge;

                if (startInnerCycle < 0) {
                    final double edgeArea = newEdge.cycleArea();
                    final double twinArea = newEdge._twin.cycleArea();
                    assert(edgeArea > 0 && twinArea > 0);

                    // face with greater area keeps old key
                    if (edgeArea < twinArea) {
                        newFaceEdge = newEdge;
                        face._outerEdge = newEdge._twin;
                    } else {
                        newFaceEdge = newEdge._twin;
                        face._outerEdge = newEdge;
                    }
                } else {
                    SubdivisionEdge pivot = newEdge;
                    for (SubdivisionEdge edge: newEdge.cycleEdges())
                        if (_vertices.comparator().compare(pivot._origin, edge._origin) > 0)
                            pivot = edge;

                    // use pivot vertex to determine outer cycle
                    final double length = pivot._previous._origin.
                            crossProductLength(pivot._origin, pivot._next._origin);

                    if (length > 0) {
                        newFaceEdge = newEdge;
                        face._innerEdges.set(startInnerCycle, newEdge._twin);
                    } else {
                        newFaceEdge = newEdge._twin;
                        face._innerEdges.set(startInnerCycle, newEdge);
                    }
                }

                // create new face and update incident edges
                final SubdivisionFace newFace = new SubdivisionFace(this, _nextFaceKey++, newFaceEdge, null);
                _faces.put(newFace._key, newFace);
                newFaceEdge.setAllFaces(newFace);
                addedFace = newFace._key;

                // move inner cycles to new face if necessary
                if (face._innerEdges != null) {
                    final int count = face._innerEdges.size();
                    for (int i = count - 1; i >= 0; i--) {
                        if (startInnerCycle == i) continue;

                        final SubdivisionEdge innerEdge = face._innerEdges.get(i);
                        final PolygonLocation location = newFaceEdge.locate(innerEdge._origin);

                        if (location == PolygonLocation.INSIDE) {
                            face._innerEdges.remove(i);
                            newFace.addInnerEdge(innerEdge);
                        }
                    }

                    if (face._innerEdges.isEmpty())
                        face._innerEdges = null;
                }
            }
        }

        _connectivity = 0;
        return new AddEdgeResult(newEdge, changedFace, addedFace);
    }

    /**
     * Creates a deep copy of the {@link Subdivision}.
     * Replicates the entire structure of the {@link Subdivision}, creating a new
     * {@link SubdivisionEdge} and {@link SubdivisionFace} with identical keys
     * for each corresponding instance found in the current structure.
     * {@link #structureEquals} will succeed for comparing both instances.
     * 
     * @return a deep copy of the {@link Subdivision}
     */
    public Subdivision copy() {
        final Subdivision division = new Subdivision(epsilon);

        // copy internal counters
        division._connectivity = _connectivity;
        division._nextEdgeKey = _nextEdgeKey;
        division._nextFaceKey = _nextFaceKey;

        // copy edges with key and origin
        for (SubdivisionEdge oldEdge: _edges.values()) {
            final SubdivisionEdge newEdge = new SubdivisionEdge(oldEdge._key);
            division._edges.put(newEdge._key, newEdge);
            newEdge._origin = oldEdge._origin;
        }

        // copy vertices with new edge references
        for (SubdivisionEdge oldEdge: _vertices.values())
            division._vertices.put(oldEdge._origin, division._edges.get(oldEdge._key));

        // copy unbounded face with new edge references
        SubdivisionFace oldFace = _faces.get(0);
        if (oldFace._innerEdges != null) {
            final SubdivisionFace newFace = division._faces.get(0);
            newFace._innerEdges = new ArrayList<>(oldFace._innerEdges.size());
            for (SubdivisionEdge oldEdge: oldFace._innerEdges)
                newFace._innerEdges.add(division._edges.get(oldEdge._key));
        }

        // copy bounded faces with new edge references
        for (int key: _faces.keySet()) {
            if (key == 0) continue;

            oldFace = _faces.get(key);
            final SubdivisionFace newFace = new SubdivisionFace(division, key);
            division._faces.put(key, newFace);

            newFace._outerEdge = division._edges.get(oldFace._outerEdge._key);
            if (oldFace._innerEdges != null) {
                newFace._innerEdges = new ArrayList<>(oldFace._innerEdges.size());
                for (SubdivisionEdge oldEdge: oldFace._innerEdges)
                    newFace._innerEdges.add(division._edges.get(oldEdge._key));
            }
        }

        // update edge & face references of all edges
        for (SubdivisionEdge newEdge: division._edges.values()) {
            final SubdivisionEdge oldEdge = _edges.get(newEdge._key);

            newEdge._face = division._faces.get(oldEdge._face._key);
            newEdge._twin = division._edges.get(oldEdge._twin._key);
            newEdge._next = division._edges.get(oldEdge._next._key);
            newEdge._previous = division._edges.get(oldEdge._previous._key);
        }

        return division;
    }

    /**
     * Finds the {@link SubdivisionElement} at the specified {@link PointD} coordinates.
     * First calls {@link #findFace} to determine the smallest {@link #faces} element that
     * contains {@code q}, then checks all its {@link SubdivisionFace#outerEdge} and
     * {@link SubdivisionFace#innerEdges} cycles to determine whether {@code q} coincides
     * with an incident {@link #edges} or {@link #vertices} element.
     * <p>
     * {@code find} performs a slow brute-force search. For better performance, create a
     * {@link SubdivisionSearch} for repeated searches within the same {@link Subdivision},
     * or examine the {@link #edges} and {@link #vertices} collections directly if you expect
     * {@code q} to coincide with one of their elements.</p>
     * 
     * @param q the {@link PointD} coordinates to examine
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal,
     *                or zero to use exact coordinate comparisons
     * @return the {@link SubdivisionElement} that coincides with {@code q}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public SubdivisionElement find(PointD q, double epsilon) {
        if (q == null)
            throw new NullPointerException("q");
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        final SubdivisionFace face = findFace(q);

        // check all outer cycle edges
        if (face._outerEdge != null) {
            SubdivisionEdge edge = face._outerEdge;
            do {
                final LineD line = edge.toLine();
                final LineLocation location = (epsilon == 0 ?
                        line.locate(q) : line.locate(q, epsilon));

                switch (location) {
                    case START:
                        return new SubdivisionElement(line.start);
                    case END:
                        return new SubdivisionElement(line.end);
                    case BETWEEN:
                        return new SubdivisionElement(edge);
                }

                edge = edge._next;
            } while (edge != face._outerEdge);
        }

        /*
         * Check all inner cycle edges.
         * 
         * Technically, we should not hit edges of inner cycles with a positive area,
         * as findFace would have returned the corresponding nested face in that case.
         * But we must check for possible zero-area cycles, and also for epsilon-matching
         * of inner edges or vertices.
         */
        if (face._innerEdges != null)
            for (SubdivisionEdge innerEdge: face._innerEdges) {
                SubdivisionEdge edge = innerEdge;
                do {
                    final LineD line = edge.toLine();
                    final LineLocation location = (epsilon == 0 ?
                            line.locate(q) : line.locate(q, epsilon));

                    switch (location) {
                        case START:
                            return new SubdivisionElement(line.start);
                        case END:
                            return new SubdivisionElement(line.end);
                        case BETWEEN:
                            return new SubdivisionElement(edge);
                    }

                    edge = edge._next;
                } while (edge != innerEdge);
            }

        return new SubdivisionElement(face);
    }

    /**
     * Finds the {@link SubdivisionEdge} with the specified {@link PointD} origin and destination.
     * First attempts to find {@code origin} in the {@link #vertices} collection, then calls
     * {@link SubdivisionEdge#findEdgeTo} to find the {@link SubdivisionEdge} leading from
     * {@code origin} to {@code destination}.
     * <p>
     * This is an O(ld n + m) operation, where n is the number of {@link #vertices} and m is
     * the number of half-edges originating from {@code origin}. All coordinate comparisons
     * use the current {@link #epsilon} of the {@link Subdivision}.</p>
     * 
     * @param origin the {@link SubdivisionEdge#origin} of the {@link SubdivisionEdge}
     * @param destination the {@link SubdivisionEdge#destination} of the {@link SubdivisionEdge}
     * @return the {@link SubdivisionEdge} with the specified {@code origin} and {@code destination},
     *         or {@code null} if {@link #edges} contains no matching element
     * @throws NullPointerException if {@code origin} or {@code destination} is {@code null}
     */
    public SubdivisionEdge findEdge(PointD origin, PointD destination) {

        // PointDComparator checks for null keys
        final SubdivisionEdge edge = _vertices.get(origin);
        if (edge == null) return null;

        return (epsilon == 0 ?
                edge.findEdgeTo(destination) :
                edge.findEdgeTo(destination, epsilon));
    }

    /**
     * Finds the smallest {@link SubdivisionFace} that contains the specified {@link PointD} coordinates.
     * Performs a linear search through all bounded {@link #faces} for an
     * {@link SubdivisionFace#outerEdge} boundary that contains {@code q}, i.e. one for
     * which {@link SubdivisionEdge#locate} does not return {@link PolygonLocation#OUTSIDE}.
     * <p>
     * The first containing face that has no {@link SubdivisionFace#innerEdges} is immediately
     * returned. If all containing faces have one or more {@link SubdivisionFace#innerEdges},
     * the one with the smallest outer {@link SubdivisionEdge#cycleArea} is returned.
     * If no containing face was found, the unbounded face is returned.</p>
     * <p>
     * {@code findFace} has an average runtime of O(n/2) where n is the number of bounded
     * {@link #faces}, unless {@link #faces} elements with {@link SubdivisionFace#innerEdges}
     * are frequent in which case the runtime approaches O(n).</p>
     * 
     * @param q the {@link PointD} coordinates to examine
     * @return the smallest {@link SubdivisionFace} that contains {@code q}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public SubdivisionFace findFace(PointD q) {
        if (q == null)
            throw new NullPointerException("q");

        SubdivisionFace resultFace = null;
        double resultArea = 0;

        // check all bounded faces for containment
        for (SubdivisionFace face: _faces.values()) {
            if (face._outerEdge == null)
                continue;

            final PolygonLocation location = face._outerEdge.locate(q);
            if (location == PolygonLocation.OUTSIDE)
                continue;

            // succeed if no nested faces exist
            if (face._innerEdges == null)
                return face;

            if (resultFace == null)
                resultFace = face;
            else {
                // switch to nested face with smaller area
                final double area = face._outerEdge.cycleArea();
                if (resultArea == 0)
                    resultArea = resultFace._outerEdge.cycleArea();

                if (resultArea > area) {
                    resultArea = area;
                    resultFace = face;
                }
            }
        }

        // default to unbounded face
        return (resultFace == null ? _faces.get(0) : resultFace);
    }

    /**
     * Finds the {@link SubdivisionFace} whose outer boundary equals the specified {@link PointD} polygon.
     * Calls {@link #findEdge} on each pair of consecutive {@code polygon} elements, and
     * immediately returns {@code null} if no {@link SubdivisionEdge} is found for any such pair.
     * Otherwise, the {@link SubdivisionEdge#face} links of both twin half-edges are examined.
     * If the {@link SubdivisionFace} on the same side of the {@code polygon} ever changes,
     * all half-edges on that side are eliminated from the search.
     * <p>
     * If {@code verify} is {@code false}, the {@link SubdivisionFace} on the remaining side when
     * the other has been eliminated is immediately returned. Otherwise, {@code findFace} continues
     * checking the half-edges on the remaining side, verifying that they form a cycle around a
     * single {@link SubdivisionFace} which also contains its {@link SubdivisionFace#outerEdge}.</p>
     * <p>
     * The specified {@code polygon} may begin with any incident vertex, and the sequence of vertices
     * may follow either the chain of {@link SubdivisionEdge#next} or {@link SubdivisionEdge#previous}
     * links around the incident half-edges.</p>
     * <p>
     * Depending on the {@code verify} flag, {@code findFace} has a runtime between O(ld n + 2m)
     * and O(ld n + km), where n is the number of {@link #vertices}, m is the number of half-edges
     * originating from each vertex, and k is the number of {@code polygon} vertices. All coordinate
     * comparisons use the current {@link #epsilon} of the {@link Subdivision}.</p>
     * 
     * @param polygon an array whose {@link PointD} coordinates represent the consecutive
     *                vertices of the outer boundary of the {@link SubdivisionFace}
     * @param verify {@code true} to verify that the outer boundary is fully congruent with
     *               {@code polygon}; {@code false} to return a result as soon as all potential
     *               alternatives have been eliminated
     * @return the {@link SubdivisionFace} whose outer boundary equals {@code polygon},
     *             or {@code null} if no matching {@link #faces} element was found
     * @throws IllegalArgumentException if {@code polygon} has fewer than three elements
     * @throws NullPointerException if {@code polygon} is {@code null}
     */
    public SubdivisionFace findFace(PointD[] polygon, boolean verify) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (polygon.length < 3)
            throw new IllegalArgumentException("polygon.length < 3");

        SubdivisionEdge edge = findEdge(polygon[polygon.length - 1], polygon[0]);
        if (edge == null) return null;

        // one edge provides two possible faces, one on each side
        SubdivisionFace face = edge._face, twinFace = edge._twin._face;
        boolean isOuter = (edge == face._outerEdge);
        boolean isTwinOuter = (edge._twin == twinFace._outerEdge);

        // check remaining edges to see which face is correct
        for (int i = 1; i < polygon.length; i++) {

            edge = (epsilon == 0 ?
                    edge._twin.findEdgeTo(polygon[i]) :
                    edge._twin.findEdgeTo(polygon[i], epsilon));

            if (edge == null) return null;

            // eliminate side with two different cycles
            if (face != null) {
                if (face != edge._face) {
                    if (!verify) return twinFace;
                    face = null;
                    if (twinFace == null) return null;
                } else
                    isOuter |= (edge == face._outerEdge);
            }

            if (twinFace != null) {
                if (twinFace != edge._twin._face) {
                    if (!verify) return face;
                    twinFace = null;
                    if (face == null) return null;
                } else
                    isTwinOuter |= (edge._twin == twinFace._outerEdge);
            }
        }

        // only one face left, return other face
        if (face == null) {
            assert(twinFace != null);
            return (isTwinOuter ? twinFace : null);
        }
        if (twinFace == null)
            return (isOuter ? face : null);

        // two faces left, check for outer boundary
        if (isOuter) return face;
        if (isTwinOuter) return twinFace;
        return null;
    }

    /**
     * Finds the {@link SubdivisionEdge} in the {@link Subdivision} that is
     * nearest to and facing the specified {@link PointD} coordinates.
     * First calls {@link #findFace(PointD) } to determine the {@link #faces} element
     * that contains {@code q}, and then calls {@link SubdivisionFace#findNearestEdge}
     * on that {@link SubdivisionFace} to determine the nearest facing {@link SubdivisionEdge}
     * and its distance from {@code q}.
     * 
     * @param q the {@link PointD} coordinates to examine
     * @return a {@link FindEdgeResult} containing the values described above
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public FindEdgeResult findNearestEdge(PointD q) {
        final SubdivisionFace face = findFace(q);
        return face.findNearestEdge(q);
    }

    /**
     * Finds the {@link PointD} vertex in the {@link Subdivision}
     * that is nearest to the specified {@link PointD} coordinates.
     * Returns the result of {@link PointDComparator#findNearest} for {@code q}, executing
     * on the {@link PointDComparatorY} used to sort the {@link #vertices} collection.
     * 
     * @param q the {@link PointD} coordinates to examine
     * @return the {@link #vertices} key with the smallest Euclidean distance to {@code q}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public PointD findNearestVertex(PointD q) {
        return ((PointDComparatorY) _vertices.comparator()).
                findNearest(_vertices.navigableKeySet(), q);
    }

    /**
     * Creates a {@link Subdivision} from the specified {@link LineD} segments.
     * Also determines the {@link #faces} that are formed by the {@link #edges} of the new
     * {@link Subdivision}, and sets its comparison {@link #epsilon} to the specified
     * {@code epsilon}. The new {@link Subdivision} is empty if {@code lines} is empty.
     * <p>
     * <b>Caution:</b> The specified {@code lines} must not intersect or overlap anywhere except
     * in their {@link LineD#start} and {@link LineD#end} points. {@code fromLines} does not
     * check this condition. If violated, the returned {@link Subdivision} will be invalid.</p>
     * 
     * @param lines an array of {@link LineD} instances representing the {@link #edges}
     *              in the new {@link Subdivision}
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal,
     *                or zero to use exact coordinate comparisons
     * @return a new {@link Subdivision} whose {@link #edges} are the specified {@code lines},
     *         and whose {@link #vertices} are their {@link LineD#start} and {@link LineD#end} points
     * @throws IllegalArgumentException if {@code lines} contains an element whose {@link LineD#start}
     *         point equals its {@link LineD#end} point, or if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code lines} is {@code null}
     */
    public static Subdivision fromLines(LineD[] lines, double epsilon) {
        if (lines == null)
            throw new NullPointerException("lines");

        final Subdivision division = new Subdivision(epsilon);
        if (lines.length > 0)
            division.createAllFromLines(lines);

        return division;
    }

    /**
     * Creates a {@link Subdivision} from the specified {@link PointD} polygons.
     * Sets the comparison {@link #epsilon} of the new {@link Subdivision} to the specified
     * {@code epsilon}. The new {@link Subdivision} is empty if {@code polygons} is empty.
     * <p>
     * <b>Caution:</b> The specified {@code polygons} may share common edges and vertices,
     * and may be fully contained within one another, but must not otherwise intersect
     * or overlap. {@code fromPolygons} does not check this condition. If violated, the
     * returned {@link Subdivision} will be invalid.</p>
     * 
     * @param polygons an array of {@link PointD} arrays representing the outer boundaries
     *                 of all bounded {@link #faces} in the new {@link Subdivision}
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal,
     *                or zero to use exact coordinate comparisons
     * @return a new {@link Subdivision} whose bounded {@link #faces} are the specified {@code polygons}
     * @throws IllegalArgumentException if {@code polygons} contains an array with less than three
     *         elements or two consecutive identical elements, or if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code polygons} or any of its elements is {@code null}
     */
    public static Subdivision fromPolygons(PointD[][] polygons, double epsilon) {
        if (polygons == null)
            throw new NullPointerException("polygons");

        final Subdivision division = new Subdivision(epsilon);
        if (polygons.length != 0)
            division.createAllFromPolygons(polygons);

        return division;
    }

    /**
     * Gets all {@link SubdivisionEdge} instances in the {@link Subdivision},
     * lexicographically sorted by {@link SubdivisionEdge#origin}.
     * Does not use the {@link #edges} collection but rather scans the {@link #vertices}
     * collection by lexicographically ascending coordinates. All {@link SubdivisionEdge}
     * instances originating from the same vertex are stored in consecutive index positions,
     * proceeding clockwise around the vertex.
     * 
     * @return an array of all {@link #edges}, but sorted lexicographically by
     *         {@link SubdivisionEdge#origin} rather than by {@link SubdivisionEdge#key}
     */
    public SubdivisionEdge[] getEdgesByOrigin() {
        final SubdivisionEdge[] edges = new SubdivisionEdge[_edges.size()];
        int edgeIndex = 0;

        for (SubdivisionEdge firstEdge: _vertices.values()) {
            SubdivisionEdge edge = firstEdge;
            do {
                edges[edgeIndex++] = edge;
                edge = edge._twin._next;
            } while (edge != firstEdge);
        }

        assert(edgeIndex == edges.length);
        return edges;
    }

    /**
     * Gets all {@link SubdivisionEdge} cycles in the {@link Subdivision} that enclose no area.
     * Returns all {@link SubdivisionFace#innerEdges} of all {@link #faces}
     * for which {@link SubdivisionEdge#isCycleAreaZero} succeeds.
     * 
     * @return a {@link List} containing one {@link SubdivisionEdge} from each cycle
     *         in the {@link Subdivision} that encloses no area
     */
    public List<SubdivisionEdge> getZeroAreaCycles() {
        List<SubdivisionEdge> cycles = new ArrayList<>();

        for (SubdivisionFace face: _faces.values()) {
            if (face._innerEdges == null) continue;

            for (SubdivisionEdge edge: face._innerEdges)
                if (edge.isCycleAreaZero())
                    cycles.add(edge);
        }

        return cycles;
    }

    /**
     * Creates a {@link Subdivision} from the intersection of two specified instances,
     * and returns additional information on the corresponding {@link #faces}.
     * First intersects the {@link #edges} of {@code division1} with those of {@code division2},
     * and then creates the resulting {@link #faces} with consecutive keys that equal their index
     * positions. The original {@link #faces} that equal or contain the new {@link #faces} are
     * indicated by key collections in the returned {@link SubdivisionIntersection}.
     * <p>
     * {@link #intersection} uses the {@link #epsilon} of {@code division1} to compare
     * {@link #vertices} for equality. Therefore, {@code division2} must use the same or a greater
     * {@link #epsilon}; otherwise, some of its {@link #edges} might not be representable in the
     * new {@link Subdivision}. Moreover, the comparison epsilon used to detect edge intersections
     * is raised to a minimum of 1e-10 for better numerical stability.</p>
     * <p>
     * {@link #intersection} performs best if either {@code division1} or {@code division2} is empty,
     * and worst if both instances are of equal size. That is because the algorithm intersects the
     * {@link #edges} of {@code division1} with those of {@code division2}, rather than intersecting
     * all {@link #edges} of the combined {@link Subdivision} with each other.</p>
     * 
     * @param division1 the first {@link Subdivision} to intersect
     * @param division2 the second {@link Subdivision} to intersect
     * @return a {@link SubdivisionIntersection} representing the result of the intersection
     * @throws IllegalArgumentException if the {@link #epsilon} of {@code division1} is greater than that
     *         of {@code division2}, or {@code division1} or {@code division2} is structurally invalid
     * @throws NullPointerException if {@code division1} or {@code division2} is {@code null}
     */
    public static SubdivisionIntersection intersection(Subdivision division1, Subdivision division2) {

        // implicit null checks for both arguments
        if (division1.epsilon > division2.epsilon)
            throw new IllegalArgumentException("division1.epsilon > division2.epsilon");

        /*
         * Prepare dictionaries that map half-edge keys of the combined subdivision to the
         * corresponding incident face keys in each intersected subdivisions.
         * 
         * The first HashMap can be initialized directly since copyEdges also copies the
         * keys of all copied half-edges. IntersectEdges may later add new elements if the
         * first-division edges are split, and will also store all second-division face keys
         * for newly inserted second-division edges and their possible split-offs.
         */
        final int capacity = division1._edges.size() + division2._edges.size();
        final Map<Integer, Integer> edgeToFace1 = new HashMap<>(capacity);
        final Map<Integer, Integer> edgeToFace2 = new HashMap<>(capacity);

        // store known face mapping for first division
        for (SubdivisionEdge edge: division1._edges.values())
            edgeToFace1.put(edge._key, edge._face._key);

        // combine all edges from both subdivisions
        final Subdivision division = division1.copyEdges();
        division.intersectEdges(division2, edgeToFace1, edgeToFace2);

        // find all cycles and convert them into faces
        final List<EdgeCycle>[] cycles = division.findCycles();
        division.createFacesFromCycles(cycles[0], cycles[1]);

        // map created faces to containing intersected faces
        final int[] faceKeys1 = new int[division._faces.size()];
        final int[] faceKeys2 = new int[division._faces.size()];

        // initialize all bounded faces to -1 for processing
        Arrays.fill(faceKeys1, 1, faceKeys1.length, -1);
        Arrays.fill(faceKeys2, 1, faceKeys2.length, -1);

        /*
         * Mapping from division1 edges to division1 faces was stored above,
         * and intersectEdges mapped division2 edges to division2 faces.
         *
         * To finalize face mapping, we need to check for cycles that consist
         * of either division1 or division2 edges exclusively, and thus know
         * nothing about faces in the other intersecting division.
         */
        for (SubdivisionEdge edge: division._edges.values()) {
            final int newFace = edge._face._key;

            // unbounded face always maps to original unbounded faces
            if (newFace == 0) {
                assert (faceKeys1[newFace] == 0);
                assert (faceKeys2[newFace] == 0);
                continue;
            }

            // determine mappings of new bounded faces
            if (faceKeys1[newFace] < 0) {
                final int oldFace = division1.findInputFace(edge, edgeToFace1);
                faceKeys1[newFace] = oldFace;

                assert (oldFace >= 0 && oldFace < division1.faces().size());
                assert checkCycleFaces(edge, oldFace, edgeToFace1);
            }

            if (faceKeys2[newFace] < 0) {
                final int oldFace = division2.findInputFace(edge, edgeToFace2);
                faceKeys2[newFace] = oldFace;

                assert (oldFace >= 0 && oldFace < division2.faces().size());
                assert checkCycleFaces(edge, oldFace, edgeToFace2);
            }
        }

        // recompute connectivity on next access
        division._connectivity = 0;

        return new SubdivisionIntersection(division, faceKeys1, faceKeys2);
    }

    /**
     * Moves the specified {@link PointD} vertex to the specified {@link PointD} coordinates.
     * First checks whether {@link #vertices} already contains {@code newVertex}, or whether
     * moving the {@link SubdivisionEdge#origin} of any incident {@link SubdivisionEdge} to
     * {@code newVertex} would create an intersection with any non-incident {@link SubdivisionEdge}
     * on any boundary of any incident {@link SubdivisionFace}.
     * <p>
     * If so, {@code moveVertex} returns {@code false}. Otherwise, {@code oveVertex} updates
     * the {@link #vertices} collection and the {@link SubdivisionEdge#origin} of all incident
     * {@link #edges} and returns {@code true}.</p>
     * 
     * @param oldVertex the existing {@link PointD} coordinates of the vertex to move
     * @param newVertex the {@link PointD} coordinates to move {@code oldVertex} to
     * @return {@code true} if {@code oldVertex} was moved to {@code newVertex}, else {@code false}
     * @throws NullPointerException if {@code oldVertex} or {@code newVertex} is {@code null}
     */
    public boolean moveVertex(PointD oldVertex, PointD newVertex) {
        if (_vertices.containsKey(newVertex))
            return false;

        final int capacity = (_connectivity > 0 ? _connectivity : 8);
        final List<SubdivisionEdge> oldEdges = new ArrayList<>(capacity);
        final List<SubdivisionFace> faces = new ArrayList<>(capacity);

        // get incident edges and distinct incident faces
        final SubdivisionEdge oldEdge = _vertices.get(oldVertex);
        for (SubdivisionEdge edge: oldEdge.originEdges()) {
            oldEdges.add(edge);
            if (!faces.contains(edge._face))
                faces.add(edge._face);
        }

        // create line segments that represent new edges
        final LineD[] newEdges = new LineD[oldEdges.size()];
        for (int i = 0; i < newEdges.length; i++)
            newEdges[i] = new LineD(newVertex, oldEdges.get(i)._twin._origin);

        // check for intersections of new edges with any other edges
        for (SubdivisionFace face: faces)
            for (SubdivisionEdge edge: face.allCycleEdges()) {
                if (oldEdges.contains(edge) || oldEdges.contains(edge._twin))
                    continue;

                final LineD edgeLine = edge.toLine();
                for (LineD line: newEdges) {
                    final LineIntersection result = line.intersect(edgeLine, epsilon);
                    if (result.existsBetween()) return false;
                }
            }

        // adjust vertex and incident edges
        _vertices.remove(oldVertex);
        _vertices.put(newVertex, oldEdge);

        for (SubdivisionEdge edge: oldEdges)
            edge._origin = newVertex;

        return true;
    }

    /**
     * Removes the specified edge from the {@link Subdivision}, and returns
     * additional information on any changed and removed {@link #faces}.
     * Fails immediately if {@code edgeKey} is not found in the {@link #edges} collection.
     * <p>
     * If the removed {@link SubdivisionEdge} and its {@link SubdivisionEdge#twin} bound two
     * different faces, the {@link #faces} element whose <em>outer</em> boundary contains a removed
     * half-edge is also removed. If <em>both</em> removed half-edges constitute outer boundaries,
     * the {@link #faces} element with the greater {@link SubdivisionFace#key} is removed.</p>
     * <p>
     * If the {@link SubdivisionEdge#origin} or {@link SubdivisionEdge#destination} of a removed
     * {@link SubdivisionEdge} does not terminate any other {@link #edges}, the corresponding
     * {@link #vertices} element(s) are also removed.</p>
     * 
     * @param edgeKey the {@link SubdivisionEdge#key} of one {@link SubdivisionEdge} to remove,
     *                with its {@link SubdivisionEdge#twin} implicitly removed as well
     * @return a {@link RemoveEdgeResult} containing the result of the operation
     */
    public RemoveEdgeResult removeEdge(int edgeKey) {

        int changedFace = -1, removedFace = -1;
        SubdivisionEdge edge = _edges.get(edgeKey);
        if (edge == null)
            return new RemoveEdgeResult(changedFace, removedFace);

        /*
         * 1. If both faces are equal, the edge is (part of) a zero-area protrusion of an outer
         *    or inner boundary. No face is removed. This is the only case where both half-edges
         *    may be inner boundaries, and may form a cycle containing no other half-edges.
         * 
         * 2. If both faces are different and both half-edges are outer boundaries, retain the
         *    face with the smaller key. The new combined boundary enlarges its outer boundary.
         *    All inner boundaries of the removed face are copied to the retained face.
         * 
         * 3. If both faces are different and one half-edge is an inner boundary, the other must
         *    be an outer boundary. Retain the face with the inner boundary. The new combined
         *    boundary enlarges that inner boundary. All inner boundaries of the removed face
         *    are copied to the retained face.
         */
        SubdivisionEdge twin = edge._twin;
        if (edge._face == twin._face) {
            changedFace = edge._face._key;
            edge._face.moveEdge(edge);
        }
        else {
            // determine edge whose face has the smaller key
            SubdivisionEdge e0, e1;
            if (edge._face._key < twin._face._key) {
                e0 = edge; e1 = twin;
            } else {
                e0 = twin; e1 = edge;
            }

            // check if face with greater key is outer boundary
            SubdivisionFace e1Face = e1._face;
            findFace: {
                if (e1Face._innerEdges == null) {
                    assert(e1Face._outerEdge != null);
                    break findFace;
                }

                if (e1Face._outerEdge != null) {
                    SubdivisionEdge cursor = e1;
                    do {
                        if (cursor == e1Face._outerEdge)
                            break findFace;
                        cursor = cursor._next;
                    } while (cursor != e1);
                }

                // always keep face that is inner boundary
                e1Face = e0._face;
                e0 = e1;
            }

            e0._face.moveEdge(e0);
            e0._face.addInnerEdges(e1Face._innerEdges);
            e1Face.setAllEdgeFaces(e0._face);

            changedFace = e0._face._key;
            removedFace = e1Face._key;
            _faces.remove(removedFace);
        }

        // remove half-edges from vertex chains
        removeAtOrigin(edge);
        removeAtOrigin(twin);

        /*
         * If both faces are equal, and we did not entirely remove an inner boundary or the
         * zero-area tip of a boundary, then we cut a zero-area protrusion or connection into
         * two parts. One part now forms a new inner cycle within the same face.
         * 
         * Note that we check removedFace rather than comparing edge.Face to twin.Face because
         * all incident faces have already been updated. At this point, edge.Face and twin.Face
         * both refer to changedFace rather than removedFace, even if the latter is valid.
         */
        if (removedFace < 0 && edge._next != twin && edge._previous != twin) {
            final SubdivisionEdge outerEdge = edge._face._outerEdge;
            final List<SubdivisionEdge> innerEdges = edge._face._innerEdges;

            /*
             * To find the new inner cycle, we explore the cycles starting after and before the
             * removed half-edge. If we arrive at an existing InnerEdges element, we know that
             * both cycles are inner cycles, and add the other cycle as a new inner cycle.
             * 
             * Otherwise, if we arrive at the existing OuterEdge, we must find and compare the
             * lexicographically smallest vertices of both cycles to determine which one is the
             * shortened outer cycle and which is added as a new inner cycle.
             * 
             * We check both conditions for both cycles in a single loop which we exit as soon
             * as we match any InnerEdges element. We stop doing OuterEdge and InnerEdges
             * comparisons as soon as we match OuterEdge in either cycle.
             */
            SubdivisionEdge nextCursor = edge._next, prevCursor = edge._previous;
            PointD nextPivot = nextCursor._origin, prevPivot = prevCursor._origin;
            boolean nextIsOuter = false, prevIsOuter = false;
            final PointDComparatorY vertexComparer = (PointDComparatorY) _vertices.comparator();

            do {
                if (nextCursor != null) {
                    if (!nextIsOuter && !prevIsOuter) {
                        if (outerEdge == nextCursor)
                            nextIsOuter = true;
                        else if (innerEdges != null && innerEdges.contains(nextCursor)) {
                            edge._face.addInnerEdge(edge._previous);
                            break;
                        }
                    }

                    if (nextCursor == twin._previous)
                        nextCursor = null;
                    else {
                        nextCursor = nextCursor._next;
                        if (vertexComparer.compare(nextPivot, nextCursor._origin) > 0)
                            nextPivot = nextCursor._origin;
                    }
                }

                if (prevCursor != null) {
                    if (!nextIsOuter && !prevIsOuter) {
                        if (outerEdge == prevCursor)
                            prevIsOuter = true;
                        else if (innerEdges != null && innerEdges.contains(prevCursor)) {
                            edge._face.addInnerEdge(edge._next);
                            break;
                        }
                    }

                    if (prevCursor == twin._next)
                        prevCursor = null;
                    else {
                        prevCursor = prevCursor._previous;
                        if (vertexComparer.compare(prevPivot, prevCursor._origin) > 0)
                            prevPivot = prevCursor._origin;
                    }
                }

            } while (nextCursor != null || prevCursor != null);

            /*
             * If either cycle contains the existing OuterEdge, one of the two cycles is the
             * shortened outer cycle -- but not necessarily the one containing OuterEdge.
             * 
             * We compare the lexicographically smallest pivot vertices in each cycle to find
             * the actual outer cycle, switch OuterEdge to a half-edge within that cycle if
             * necessary, and add the other cycle as a new inner cycle.
             */
            if (nextIsOuter || prevIsOuter) {
                int pivotCompare = vertexComparer.compare(nextPivot, prevPivot);

                if (pivotCompare < 0) {
                    if (prevIsOuter) edge._face._outerEdge = edge._next;
                    edge._face.addInnerEdge(edge._previous);
                } else {
                    assert(pivotCompare > 0);
                    if (nextIsOuter) edge._face._outerEdge = edge._previous;
                    edge._face.addInnerEdge(edge._next);
                }
            }
        }

        _edges.remove(edgeKey);
        _edges.remove(twin._key);

        _connectivity = 0;
        return new RemoveEdgeResult(changedFace, removedFace);
    }

    /**
     * Removes the specified {@link PointD} vertex by joining both incident edges.
     * First checks whether {@link #vertices} does not contain {@code vertex}, or whether
     * {@code vertex} has not exactly two distinct incident full edges, or whether joining
     * them would disturb vertex chains or create an intersection with any non-incident
     * {@link SubdivisionEdge} on any boundary of the incident {@link SubdivisionFace}.
     * <p>
     * If so, {@code removeVertex} returns {@code false}. Otherwise, {@code removeVertex}
     * links the twins of the incident {@link #edges} into a new {@link SubdivisionEdge} pair,
     * updates the {@link #vertices} and {@link #edges} collections, and returns {@code true}.</p>
     * 
     * @param vertex the {@link PointD} vertex to remove
     * @return {@code true} if {@code vertex} and both incident edges were removed, else {@code false}
     * @throws NullPointerException if {@code vertex} is {@code null}
     */
    public boolean removeVertex(PointD vertex) {

        // check for exactly two incident edges
        final SubdivisionEdge edge1 = _vertices.get(vertex);
        if (edge1 == null) return false;

        final SubdivisionEdge twin1 = edge1._twin, edge2 = twin1._next, twin2 = edge2._twin;
        if (edge1 == edge2 || edge1._previous != twin2)
            return false;

        // check for existing connecting edge
        for (SubdivisionEdge edge: twin1.originEdges())
            if (edge._twin._origin == twin2._origin)
                return false;

        // check joined edge position in vertex chains
        if (!twin1.isCompatibleDestination(twin2._origin) ||
            !twin2.isCompatibleDestination(twin1._origin))
            return false;

        // check for other edges intersecting joined edge
        final double length = twin2._origin.crossProductLength(edge1._origin, twin1._origin);
        if (Math.abs(length) > epsilon) {
            final SubdivisionFace face = (length < 0) ? edge2._face : edge1._face;
            final LineD line = new LineD(twin2._origin, twin1._origin);

            for (SubdivisionEdge edge: face.allCycleEdges())
                if (edge != edge1 && edge != twin1 && edge != edge2 && edge != twin2) {
                    final LineIntersection result = line.intersect(edge.toLine(), epsilon);
                    if (result.existsBetween()) return false;
                }
        }

        edge1._face.moveEdge(edge1, twin2);
        edge2._face.moveEdge(edge2, twin1);

        // connect twins of incident edges
        twin1._twin = twin2;
        twin1._next = edge2._next;
        edge2._next._previous = twin1;

        twin2._twin = twin1;
        twin2._next = edge1._next;
        edge1._next._previous = twin2;

        // remove vertex and incident edges
        _edges.remove(edge1._key);
        _edges.remove(edge2._key);
        _vertices.remove(vertex);

        // recompute connectivity on next access
        if (_connectivity == 2) _connectivity = 0;
        return true;
    }

    /**
     * Renumbers all {@link #edges} so that each {@link SubdivisionEdge#key}
     * equals the iterator position of the corresponding {@link SubdivisionEdge}.
     * Deleting {@link #edges} from an existing {@link Subdivision} may leave gaps in the
     * {@link SubdivisionEdge#key} sequence. {@link #renumberEdges} eliminates any such gaps,
     * restoring the original equivalence of {@link SubdivisionEdge#key} value and iterator
     * position in the {@link #edges} collection.
     * <p>
     * {@link #renumberEdges} does not change the sequence of {@link SubdivisionEdge} instances
     * in the {@link #edges} collection, only their {@link SubdivisionEdge#key} values.</p>
     * 
     * @return {@code true} if any {@link SubdivisionEdge#key} was changed, else {@code false}
     */
    public boolean renumberEdges() {
        if (_edges.size() == _nextEdgeKey)
            return false;

        final SubdivisionEdge[] edges = new SubdivisionEdge[_edges.size()];
        int key = 0;
        for (SubdivisionEdge edge: _edges.values()) {
            assert(edge._key >= key);
            edges[key] = edge;
            edge._key = key++;
        }

        _edges.clear();
        for (SubdivisionEdge edge: edges)
            _edges.put(edge._key, edge);

        _nextEdgeKey = edges.length;
        return true;
    }

    /**
     * Renumbers all {@link #faces} so that each {@link SubdivisionFace#key}
     * equals the iterator position of the corresponding {@link SubdivisionFace}.
     * Deleting {@link #edges} from an existing {@link Subdivision} may leave gaps in the
     * {@link SubdivisionFace#key} sequence of the {@link #faces} collection.
     * {@link #renumberFaces} eliminates any such gaps, restoring the original equivalence of
     * {@link SubdivisionFace#key} value and iterator position in the {@link #faces} collection.
     * <p>
     * {@link #renumberFaces} does not change the sequence of {@link SubdivisionFace} instances
     * in the {@link #faces} collection, only their {@link SubdivisionFace#key} values.
     * Note that this invalidates any existing {@link SubdivisionMap} face mappings.</p>
     * 
     * @return {@code true} if any {@link SubdivisionEdge#key} was changed, else {@code false}
     */
    public boolean renumberFaces() {
        if (_faces.size() == _nextFaceKey)
            return false;

        final SubdivisionFace[] faces = new SubdivisionFace[_faces.size()];
        int key = 0;
        for (SubdivisionFace face: _faces.values()) {
            assert(face._key >= key);
            faces[key] = face;
            face._key = key++;
        }

        _faces.clear();
        for (SubdivisionFace face: faces)
            _faces.put(face._key, face);

        _nextFaceKey = faces.length;
        return true;
    }

    /**
     * Splits the specified edge in half.
     * Returns {@code null} if {@code edgeKey} was not found in the {@link #edges}
     * collection, or if the new vertex would equal an existing {@link #vertices} key,
     * given the current {@link #epsilon}.
     * <p>
     * Otherwise, creates a new {@link #vertices} element in the center of the split edge,
     * and two new {@link SubdivisionEdge} instances that originate from the new vertex.
     * Each is paired with one of the original {@link SubdivisionEdge} twins, effectively
     * shortening them to end at the new vertex.</p>
     * 
     * @param edgeKey the {@link SubdivisionEdge#key} of one {@link SubdivisionEdge} to split,
     *                implicitly splitting its {@link SubdivisionEdge#twin} as well
     * @return one of the two new {@link #edges} that originate from the new {@link #vertices}
     *         element if the split was successful, else {@code null}
     */
    public SubdivisionEdge splitEdge(int edgeKey) {
        final SubdivisionEdge edge = _edges.get(edgeKey);
        if (edge == null) return null;

        final PointD a = edge._origin, b = edge._twin._origin;
        final PointD vertex = new PointD((a.x + b.x) / 2, (a.y + b.y) / 2);
        if (_vertices.containsKey(vertex))
            return null;

        if (_connectivity < 2) _connectivity = 2;
        return splitEdgeAtVertex(edge, vertex, true);
    }

    /**
     * Determines whether the specified {@link Subdivision} and this instance are structurally equal.
     * Compares the number, order, and internal structure of all {@link #edges} and {@link #faces}
     * in the two {@link Subdivision} instances to test for structural equality. Individual objects
     * are compared using their own {@link Object#equals} methods.
     * <p>
     * Intended for testing the {@link #copy} method which duplicates keys but not objects.</p>
     * 
     * @param division the {@link Subdivision} to compare to this instance
     * @return {@code true} if {@code division} and this instance are structurally equal, else {@code false}
     * @throws NullPointerException if {@code division} is {@code null}
     */
    public boolean structureEquals(Subdivision division) {

        // compare internal counters
        if (_nextEdgeKey != division._nextEdgeKey) return false;
        if (_nextFaceKey != division._nextFaceKey) return false;

        // compare collection counts
        if (_faces.size() != division._faces.size()) return false;
        if (_vertices.size() != division._vertices.size()) return false;

        // compare contents of vertex collection
        for (PointD key: _vertices.keySet()) {
            final SubdivisionEdge edge = _vertices.get(key);
            final SubdivisionEdge otherEdge = division._vertices.get(key);
            if (!edge.equals(otherEdge)) return false;
        }

        // compare contents of face collection
        for (int key: _faces.keySet()) {
            final SubdivisionFace face = _faces.get(key);
            final SubdivisionFace otherFace = division._faces.get(key);
            if (!face.equals(otherFace)) return false;
        }

        return true;
    }

    /**
     * Converts all edges in the {@link Subdivision} to {@link LineD} instances.
     * The returned array has half as many elements as the {@link #edges} collection since
     * each {@link LineD} corresponds to a twin pair of {@link SubdivisionEdge} instances.
     * <p>
     * The returned array is sorted by the smaller {@link SubdivisionEdge#key}
     * of the two {@link SubdivisionEdge} instances that constitute each full edge,
     * yielding the same ordering as the {@link #edges} collection but excluding the
     * higher-keyed twin of each {@link SubdivisionEdge} pair.</p>
     * 
     * @return an array of {@link LineD} instances representing all edges in the {@link Subdivision}
     */
    public LineD[] toLines() {
        final LineD[] lines = new LineD[_edges.size() / 2];
        int lineIndex = 0;

        // iterate over half-edges by ascending keys
        for (SubdivisionEdge edge: _edges.values()) {
            final SubdivisionEdge twin = edge._twin;

            // add full edge when encountering first twin only;
            // that is, when current key is less than twin key
            if (edge._key < twin._key)
                lines[lineIndex++] = new LineD(edge._origin, twin._origin);
        }

        assert(lineIndex == lines.length);
        return lines;
    }

    /**
     * Converts the outer boundaries of all bounded {@link #faces} in the {@link Subdivision}
     * to {@link PointD} polygons.
     * Each nested {@link PointD} array within the returned array contains the
     * {@link SubdivisionEdge#cyclePolygon} for the {@link SubdivisionFace#outerEdge}
     * of one bounded {@link #faces} element.
     * <p>
     * The {@link PointD} arrays are sorted by the {@link SubdivisionFace#key} of the
     * corresponding {@link SubdivisionFace}, yielding the same ordering as the
     * {@link #faces} collection but excluding the unbounded face.</p>
     * 
     * @return an array of {@link PointD} polygons representing
     *         all bounded {@link #faces} in the {@link Subdivision}
     */
    public PointD[][] toPolygons() {
        final PointD[][] polygons = new PointD[_faces.size() - 1][];

        int faceIndex = 0;
        for (SubdivisionFace face: _faces.values())
            if (face._key > 0)
                polygons[faceIndex++] = face._outerEdge.cyclePolygon();

        assert(faceIndex == polygons.length);
        return polygons;
    }

    /**
     * Validates the structure of the {@link Subdivision}.
     * Uses {@code assert} statements to verify the structural invariants of the {@link Subdivision}.
     * Assertions must be enabled at runtime for {@link #validate} to have any effect.
     * 
     * @throws AssertionError if the structure of the {@link Subdivision} is invalid
     */
    public void validate() {
        final boolean isEmpty = _edges.isEmpty();

        // check vertex comparer epsilon against cached value
        assert(((PointDComparatorY) _vertices.comparator()).epsilon == epsilon);

        // check number of edges, vertices & faces
        assert(_edges.size() % 2 == 0);
        assert((isEmpty && _vertices.isEmpty()) ||
              (!isEmpty && _vertices.size() >= 2));
        assert(!_faces.isEmpty());
        assert(_faces.firstKey() == 0);

        // check mandatory unbounded face
        final SubdivisionFace faceZero = _faces.get(0);
        assert(faceZero._outerEdge == null);
        assert((isEmpty && faceZero._innerEdges == null) ||
              (!isEmpty && faceZero._innerEdges != null));
        assert(faceZero._innerEdges == null || !faceZero._innerEdges.isEmpty());

        // check incident faces and next/previous chains
        for (Map.Entry<Integer, SubdivisionEdge> entry: _edges.entrySet()) {
            final SubdivisionEdge edge = entry.getValue();
            assert(entry.getKey() == edge._key);
            assert(edge._face != null);
            assert(edge._face.owner == this);
            assert(edge._twin._twin == edge);
            assert(edge._next._previous == edge);
            assert(edge._previous._next == edge);
        }

        // check incident edges and vertex chains
        for (Map.Entry<PointD, SubdivisionEdge> entry: _vertices.entrySet()) {
            final PointD vertex = entry.getKey();
            SubdivisionEdge edge = entry.getValue();
            do {
                assert(edge._origin.equals(vertex));
                edge = edge._twin._next;
            } while (edge != entry.getValue());

            do {
                assert(edge._origin.equals(vertex));
                edge = edge._previous._twin;
            } while (edge != entry.getValue());
        }

        for (Map.Entry<Integer, SubdivisionFace> entry: _faces.entrySet()) {
            final SubdivisionFace face = entry.getValue();
            assert(entry.getKey() == face._key);
            assert(face.owner == this);

            // check incident faces of outer cycle
            if (face._outerEdge != null) {
                SubdivisionEdge edge = face._outerEdge;
                do {
                    assert(edge._face == face);
                    edge = edge._next;
                } while (edge != face._outerEdge);

                do {
                    assert(edge._face == face);
                    edge = edge._previous;
                } while (edge != face._outerEdge);
            }

            // check incident faces of all inner cycles
            if (face._innerEdges != null) {
                for (SubdivisionEdge innerEdge: face._innerEdges) {
                    SubdivisionEdge edge = innerEdge;
                    do {
                        assert(edge._face == face);
                        edge = edge._next;
                    } while (edge != innerEdge);

                    do {
                        assert(edge._face == face);
                        edge = edge._previous;
                    } while (edge != innerEdge);
                }
            }
        }
    }

    // ----- Graph Implementation -----

    /**
     * Gets the maximum number of direct neighbors for any {@link Graph} node.
     * Always greater than zero. Returns the maximum number of {@link SubdivisionEdge} instances
     * that originate from any single vertex. Scans the entire {@link #vertices} collection on
     * first access, then returns a cached value on subsequent accesses. The scan is repeated
     * whenever the structure of the {@link Subdivision} changes.
     * 
     * @return the maximum number of direct neighbors for any {@link Graph} node
     */
    @Override
    public int connectivity() {
        if (_connectivity == 0 && !_vertices.isEmpty()) {
            for (SubdivisionEdge firstEdge: _vertices.values()) {
                int count = 0;

                // count half-edges at current vertex
                SubdivisionEdge edge = firstEdge;
                do {
                    ++count;
                    edge = edge._twin._next;
                } while (edge != firstEdge);

                if (_connectivity < count)
                    _connectivity = count;
            }
        }

        return _connectivity;
    }

    /**
     * Gets the total number of {@link #nodes} in the {@link Graph}.
     * Never less than zero. Returns the current number of {@link #vertices}.
     * 
     * @return the total number of {@link #nodes} in the {@link Graph}
     */
    @Override
    public int nodeCount() {
        return _vertices.size();
    }

    /**
     * Gets all nodes in the {@link Graph}.
     * Always contains {@link #nodeCount} elements. Returns all {@link PointD} keys
     * in the {@link #vertices} collection, using its current sorting order.
     * 
     * @return a {@link Set} of all nodes in the {@link Graph}
     */
    @Override
    public Set<PointD> nodes() {
        return _vertices.keySet();
    }

    /**
     * Determines whether the {@link Graph} contains the specified node.
     * Returns {@code true} exactly if the {@link #vertices} collection contains
     * a {@link PointD} key that equals the specified {@code node}. This is an O(ld n)
     * operation, where n is the number of {@link #vertices}.
     * 
     * @param node the {@link Graph} node to examine
     * @return {@code true} if the {@link Graph} contains {@code node}, else {@code false}
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public boolean contains(PointD node) {
        return _vertices.containsKey(node);
    }

    /**
     * Finds the {@link Graph} node nearest to the specified {@link PointD} world location.
     * Returns the result of {@link #findNearestVertex} for the specified {@code location}.
     * 
     * @param location the {@link PointD} location, in world coordinates, to examine
     * @return the {@link Graph} node whose {@link #getWorldLocation} result
     *         is nearest to the specified {@code location}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    @Override
    public PointD findNearestNode(PointD location) {
        return findNearestVertex(location);
    }

    /**
     * Gets the distance between two specified {@link Graph} nodes.
     * Returns zero if the specified {@code source} and {@code target} are equal,
     * and the Euclidean distance between {@code source} and {@code target} otherwise.
     * This is equivalent to the absolute length of the edge that connects these vertices.
     * <p>
     * {@code getDistance} does not check whether the {@link Subdivision} actually contains
     * {@code source} and {@code target}, or whether they are connected by an edge.</p>
     * 
     * @param source the source node in the {@link Graph}
     * @param target the target node in the {@link Graph}
     * @return the non-negative distance between {@code source} and {@code target}
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     */
    @Override
    public double getDistance(PointD source, PointD target) {

        final double dx = target.x - source.x;
        final double dy = target.y - source.y;

        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Gets all direct neighbors of the specified {@link Graph} node.
     * Returns an empty {@link List} if {@code node} is not a {@link #vertices} key. Otherwise,
     * returns the destinations of all {@link SubdivisionEdge} instances that originate from
     * {@code node}. This is an O(ld n + m) operation, where n is the total number of
     * {@link #vertices} and m is the number of half-edges originating from {@code node}.
     * 
     * @param node the {@link Graph} node whose direct neighbors to collect
     * @return a {@link List} of all {@link Graph} nodes that are directly connected
     *         with {@code node}, numbering from zero to {@link #connectivity}
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public List<PointD> getNeighbors(PointD node) {

        // find origin in vertex collection
        final SubdivisionEdge firstEdge = _vertices.get(node);
        if (node == null) return Collections.emptyList();

        // find all destinations from origin
        final List<PointD> neighbors = new ArrayList<>(connectivity());
        SubdivisionEdge edge = firstEdge;
        do {
            final SubdivisionEdge twin = edge._twin;
            neighbors.add(twin._origin);
            edge = twin._next;
        } while (edge != firstEdge);

        return neighbors;
    }

    /**
     * Gets the world location of the specified {@link Graph} node.
     *  Simply returns the specified {@code node}, without checking
     * whether it is actually part of the {@link Subdivision}.
     * 
     * @param node the {@link Graph} node whose world location to find
     * @return the {@link PointD} location of {@code node}, in world coordinates
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public PointD getWorldLocation(PointD node) {
        return node;
    }

    /**
     * Gets the world region covered by the specified {@link Graph} node.
     * Returns the polygonal region that the user-defined {@link #vertexRegions} collection
     * associates with the specified {@code node}, if found, else, {@code null}.
     * 
     * @param node the {@link Graph} node whose world region to find
     * @return an array of {@link PointD} vertices defining the polygonal region
     *         covered by {@code node}, in world coordinates
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public PointD[] getWorldRegion(PointD node) {
        return _vertexRegions.get(node);
    }

    // ----- Private Methods -----

    /**
     * Checks that all {@link #edges} within the cycle of the specified {@link SubdivisionEdge}
     * are mapped either to {@code null} or to the specified {@link SubdivisionFace} key.
     *
     * @param edge the {@link SubdivisionEdge} whose cycle to traverse
     * @param face the expected key of the {@link SubdivisionFace} in {@code edgeToFace}
     * @param edgeToFace the {@link Map} mapping {@link SubdivisionEdge} keys to {@link SubdivisionFace} keys
     * @return {@code true} if all {@link SubdivisionEdge} keys in the cycle containing {@code edge}
     *         map to either {@code null} or {@code face}, else {@code false}
     * @throws NullPointerException if {@code edge} or {@code edgeToFace} is {@code null}
     */
    private static boolean checkCycleFaces(SubdivisionEdge edge, int face, Map<Integer, Integer> edgeToFace) {
        SubdivisionEdge cycle = edge;
        do {
            final Integer mapFace = edgeToFace.get(cycle._key);
            if (mapFace != null && mapFace != face)
                return false;

            cycle = cycle._next;
        } while (cycle != edge);

        return true;
    }

    /**
     * Creates a deep copy of the {@link Subdivision}, except for the {@link #faces} collection.
     * Identical to {@link #copy} but creates no {@link #faces}, so the returned {@link Subdivision}
     * only contains the unbounded face. Used internally by {@link #intersection}.
     * 
     * @return a deep copy of the {@link Subdivision}, except for the {@link #faces} collection
     */
    private Subdivision copyEdges() {
        final Subdivision division = new Subdivision(epsilon);

        // copy internal counters
        division._connectivity = _connectivity;
        division._nextEdgeKey = _nextEdgeKey;

        // copy edges with key and origin
        for (SubdivisionEdge oldEdge: _edges.values()) {
            final SubdivisionEdge newEdge = new SubdivisionEdge(oldEdge._key);
            division._edges.put(newEdge._key, newEdge);
            newEdge._origin = oldEdge._origin;
        }

        // copy vertices with new edge references
        for (SubdivisionEdge oldEdge: _vertices.values())
            division._vertices.put(oldEdge._origin, division._edges.get(oldEdge._key));

        // update edge references of all edges
        for (SubdivisionEdge newEdge: division._edges.values()) {
            final SubdivisionEdge oldEdge = _edges.get(newEdge._key);

            newEdge._twin = division._edges.get(oldEdge._twin._key);
            newEdge._next = division._edges.get(oldEdge._next._key);
            newEdge._previous = division._edges.get(oldEdge._previous._key);
        }

        return division;
    }

    /**
     * Compares two specified {@link SubdivisionEdge} instances
     * and returns an indication of their sweep line ordering.
     * Internally used by the plane sweep algorithm performed by {@link #findCycles}.
     * 
     * @param a the first {@link SubdivisionEdge} to compare
     * @param b the second {@link SubdivisionEdge} to compare
     * @return a negative value, zero, or a positive value if {@code a} compares less than,
     *         equal to, or greater than {@code b}, respectively
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    private int compareEdges(SubdivisionEdge a, SubdivisionEdge b) {
        if (a == b) return 0;

        // compute inverse slopes of intersecting half-edges
        final LineD aLine = a.toLine(), bLine = b.toLine();
        double aSlope = aLine.inverseSlope(), bSlope = bLine.inverseSlope();
        double ax, bx;

        /*
         * Compute x-coordinates of sweep line intersections.
         * 
         * We default horizontal edges to their destination, which is their left point
         * since we always use downward-pointing half-edges.
         * 
         * This means that a horizontal edge will get sorted AFTER any other edge touching
         * its destination (due to the maximum slope) and BEFORE any other edge touching its
         * origin (because the origin is lexicographically greater than the destination).
         * 
         * If an edge touches the sweep line with its origin, it extends below the sweep line.
         * We must then negate its slope for the possible follow-up comparison, so that the
         * sorting order remains consistent with earlier intersections below the sweep line.
         * 
         * We need no special treatment for computed sweep line intersection points, as in
         * the case of multiline intersection, because we know that no two edges intersect
         * or overlap except at their end points.
         */
        if (_cursorY == aLine.end.y || aSlope == Double.MAX_VALUE)
            ax = aLine.end.x;
        else if (_cursorY == aLine.start.y) {
            ax = aLine.start.x;
            aSlope = -aSlope;
        } else
            ax = aLine.start.x + (_cursorY - aLine.start.y) * aSlope;

        if (_cursorY == bLine.end.y || bSlope == Double.MAX_VALUE)
            bx = bLine.end.x;
        else if (_cursorY == bLine.start.y) {
            bx = bLine.start.x;
            bSlope = -bSlope;
        } else
            bx = bLine.start.x + (_cursorY - bLine.start.y) * bSlope;

        // sort on sweep line intersection if different
        if (ax < bx) return -1;
        if (ax > bx) return +1;

        // else sort on slope above sweep line
        if (aSlope < bSlope) return -1;
        if (aSlope > bSlope) return +1;

        return (a._key - b._key);
    }

    /**
     * Initializes an empty {@link Subdivision} from the specified {@link LineD} segments.
     * Implements {@link #fromLines} for a non-empty {@code lines} array.
     * 
     * @param lines an array of {@link LineD} instances representing the new {@link #edges}
     *              in the {@link Subdivision}
     * @throws IllegalArgumentException if {@code lines} contains an element whose {@link LineD#start}
     *         point equals its {@link LineD#end} point, or if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code lines} is {@code null}
     */
    private void createAllFromLines(LineD[] lines) {

        assert(_edges.isEmpty());
        assert(_faces.size() == 1);
        assert(_vertices.isEmpty());

        // convert all lines into half-edges
        for (LineD line: lines)
            createTwinEdges(line.start, line.end);

        // find all inner and outer cycles
        final List<EdgeCycle>[] cycles = findCycles();

        // convert cycles into face records
        createFacesFromCycles(cycles[0], cycles[1]);
    }

    /**
     * Initializes an empty {@link Subdivision} from the specified {@link PointD} polygons.
     * Implements {@link #fromPolygons} for a non-empty {@code polygons} array.
     * 
     * @param polygons an array of {@link PointD} arrays representing the outer boundaries
     *                 of all bounded {@link #faces} in the new {@link Subdivision}
     * @throws IllegalArgumentException if {@code polygons} contains an array with less than three
     *         elements or two consecutive identical elements, or if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code polygons} or any of its elements is {@code null}
     */
    private void createAllFromPolygons(PointD[][] polygons) {

        assert(_edges.isEmpty());
        assert(_faces.size() == 1);
        assert(_vertices.isEmpty());

        // convert all polygon edges into half-edges
        for (PointD[] polygon: polygons) {
            for (int j = 0; j < polygon.length; j++) {
                final PointD start = polygon[j];
                final PointD end = polygon[(j + 1) % polygon.length];
                createTwinEdges(start, end);
            }
        }

        // find all inner and outer cycles
        final List<EdgeCycle>[] cycles = findCycles();

        // convert cycles into face records
        createFacesFromCycles(cycles[0], cycles[1]);
    }

    /**
     * Initializes an empty {@link #faces} collection from the specified {@link EdgeCycle} chains.
     * Implements the final step of {@link #createAllFromLines} and {@link #createAllFromPolygons}.
     * Also used internally by {@link #intersection}.
     * 
     * @param innerCycles all {@link EdgeCycle} chains that begin with inner cycles,
     *                    and are therefore directly contained in the unbounded face
     * @param outerCycles all {@link EdgeCycle} chains that begin with outer cycles,
     *                    representing bounded faces which contain all subsequent
     *                    inner cycles within the same {@link EdgeCycle} chain
     * @throws NullPointerException if {@code innerCycles} or {@code outerCycles} is {@code null},
     *                              or contains any {@code null} elements
     */
    private void createFacesFromCycles(
        List<EdgeCycle> innerCycles, List<EdgeCycle> outerCycles) {
        /*
         * The unbounded face receives all inner cycles that aren’t linked
         * to any outer cycles. Those inner cycles may still be linked to
         * other inner cycles, however, so we must traverse their Next chains.
         */
        assert(_faces.size() == 1);
        SubdivisionFace face = _faces.get(0);

        // add any unlinked inner cycles to unbounded face
        for (int i = 0; i < innerCycles.size(); i++)
            for (EdgeCycle cycle = innerCycles.get(i); cycle != null; cycle = cycle.next)
                cycle.addToFace(face, false);

        /*
         * Create zero or more bounded faces, one for each outer cycle.
         * Each bounded face also receives any inner cycles which are linked
         * to that outer cycle along the chain of Next references.
         */
        for (EdgeCycle cycle: outerCycles) {

            // create bounded face for outer cycle
            face = new SubdivisionFace(this, _nextFaceKey++);
            _faces.put(face._key, face);
            cycle.addToFace(face, true);

            // add any linked inner cycles to bounded face
            for (cycle = cycle.next; cycle != null; cycle = cycle.next)
                cycle.addToFace(face, false);
        }
    }

    /**
     * Finds or creates both twin {@link SubdivisionEdge} instances
     * between the two specified {@link PointD} coordinates.
     * Adds {@code start} and {@code end} to {@link #vertices} if not already present.
     * Otherwise, the {@link SubdivisionEdge#origin} of a newly created {@link SubdivisionEdge}
     * is set to the corresponding {@link #vertices} key, which may differ from {@code start}
     * or {@code end} if the current {@link #epsilon} is greater than zero.
     * <p>
     * The {@link Subdivision} remains unchanged if the {@link #edges} collection
     * already contains twin half-edges between {@code start} and {@code end}.</p>
     * 
     * @param start the {@link SubdivisionEdge#origin} of the first {@link SubdivisionEdge},
     *              and the {@link SubdivisionEdge#destination} of the second instance
     * @param end the {@link SubdivisionEdge#destination} of the first {@link SubdivisionEdge},
     *            and the {@link SubdivisionEdge#origin} of the second instance
     * @return a {@link CreateEdgeResult} containing the result of the operation
     * @throws IllegalArgumentException if {@code start} equals {@code end}
     * @throws NullPointerException if {@code start} or {@code end} is {@code null}
     */
    private CreateEdgeResult createTwinEdges(PointD start, PointD end) {
        if (start.equals(end))
            throw new IllegalArgumentException("start == end");

        // search for vertices at start/end coordinates
        final SubdivisionEdge oldStartEdge = _vertices.get(start);
        final SubdivisionEdge oldEndEdge = _vertices.get(end);

        // both vertices exist: check if half-edges also exist
        SubdivisionEdge startEdge;
        if (oldStartEdge != null && oldEndEdge != null) {
            startEdge = oldStartEdge;
            do {
                if (startEdge._twin._origin == oldEndEdge._origin)
                    return new CreateEdgeResult(startEdge, false);

                startEdge = startEdge._twin._next;
            } while (startEdge != oldStartEdge);
        }

        // create twin edges for line segment
        startEdge = new SubdivisionEdge(_nextEdgeKey++);
        _edges.put(startEdge._key, startEdge);

        final SubdivisionEdge endEdge = new SubdivisionEdge(_nextEdgeKey++);
        _edges.put(endEdge._key, endEdge);

        // initialize Twin pointers (unchanged in this method)
        startEdge._twin = endEdge; endEdge._twin = startEdge;

        // initialize Next and Previous pointers
        // (may change if edges are linked to existing vertices)
        startEdge._next = startEdge._previous = endEdge;
        endEdge._next = endEdge._previous = startEdge;

        /*
         * Vertices might be performing searches with a non-zero epsilon, which means that
         * start/end might not exactly equal the vertex that is reported as identical.
         * Therefore, if a vertex already exists, we must assign the existing coordinates
         * to the half-edge’s Origin, and not the specified start/end coordinates.
         */

        // create or expand origin of start half-edge
        if (oldStartEdge == null) {
            startEdge._origin = start;
            _vertices.put(start, startEdge);
        } else
            startEdge._origin = oldStartEdge._origin;

        // create or expand origin of end half-edge
        if (oldEndEdge == null) {
            endEdge._origin = end;
            _vertices.put(end, endEdge);
        } else
            endEdge._origin = oldEndEdge._origin;

        // create links only after both Origins are set
        if (oldStartEdge != null) insertAtEdge(startEdge, oldStartEdge);
        if (oldEndEdge != null) insertAtEdge(endEdge, oldEndEdge);

        return new CreateEdgeResult(startEdge, true);
    }

    /**
     * Finds all inner and outer cycles formed by the {@link #edges} in the {@link Subdivision}.
     * @return an array containing two lists of {@link EdgeCycle} chains, the first
     *         representing all unlinked inner cycles contained by the unbounded face,
     *         and the second representing all outer cycles of bounded faces
     */
    private List<EdgeCycle>[] findCycles() {
        final PointDComparatorY vertexComparer = (PointDComparatorY) _vertices.comparator();

        // edge cycles containing all half-edges
        final Map<Integer, EdgeCycle> edgeToCycle = new HashMap<>(_edges.size());

        // lexicographically smallest vertices of all inner cycles
        final Map<PointD, EdgeCycle> pivotToInnerCycle = new HashMap<>(_edges.size() / 4);

        // output list containing all outer cycles
        final List<EdgeCycle> outerCycles = new ArrayList<>(_edges.size() / 4);

        // one half-edge from each twin pair in a cycle
        final Set<Integer> cycleTwinEdges = new HashSet<>();

        // queue all half-edges for examination
        final Map<Integer, SubdivisionEdge> edgeQueue = new HashMap<>(_edges);
        while (edgeQueue.size() > 0) {

            // take arbitrary half-edge to start a cycle
            SubdivisionEdge edge = edgeQueue.values().iterator().next();
            SubdivisionEdge pivot = edge;
            final EdgeCycle cycle = new EdgeCycle(edge);

            // prepare to check for zero-area cycles
            cycleTwinEdges.clear();
            int cycleTwinCount = 0;

            do {
                // map each half-edge to its containing cycle
                edgeToCycle.put(edge._key, cycle);

                // count full twin pairs contained in the cycle
                if (cycleTwinEdges.contains(edge._twin._key))
                    ++cycleTwinCount;
                else
                    cycleTwinEdges.add(edge._key);

                // continue cycle and determine lower-left vertex
                edgeQueue.remove(edge._key);
                edge = edge._next;
                if (vertexComparer.compare(pivot._origin, edge._origin) > 0)
                    pivot = edge;

            } while (edge != cycle.firstEdge);

            /*
             * If the cycle contains only full twin pairs, it encloses no area at all.
             * Therefore, all half-edges bound the same face, forming an inner cycle.
             * Otherwise, the cycle does enclose an area and we must continue testing.
             * 
             * Compute cross-product length (CPL) of the pivot vertex (lower-left corner)
             * with the previous and next vertex within the same cycle. This description
             * assumes mathematical coordinates, i.e. y-coordinates increase upward.
             * 
             * Collinear (CPL = 0) means inner cycle, as the twin half-edges at the pivot
             * vertex form a zero-area protrusion into a face on the outside of the cycle.
             * 
             * Right turn (CPL < 0) means inner cycle, as the incident faces are always
             * to the left of the half-edges, and therefore on the outside of a right turn.
             * 
             * Left turn (CPL > 0) means outer cycle, as the incident faces are always
             * to the left of the half-edges, and therefore on the inside of a left turn.
             */
            boolean isInnerCycle;
            if (cycleTwinCount == cycleTwinEdges.size())
                isInnerCycle = true;
            else {
                final double length = pivot._previous._origin.
                        crossProductLength(pivot._origin, pivot._next._origin);
                isInnerCycle = (length <= 0);
            }

            /*
             * Add outer cycles directly to output list, but store inner cycles in a
             * lookup table with their pivot vertex. They might get linked to other
             * (inner or outer) cycles rather than directly to the unbounded face.
             */
            if (isInnerCycle)
                pivotToInnerCycle.put(pivot._origin, cycle);
            else
                outerCycles.add(cycle);
        }

        // output list containing all unlinked inner cycles
        final List<EdgeCycle> innerCycles = new ArrayList<>(pivotToInnerCycle.size());
        int innerCyclesFound = 0;

        // current horizontal sweep line, moving upward
        final TreeSet<SubdivisionEdge> sweepLine = new TreeSet<>(this::compareEdges);

        for (SubdivisionEdge firstEdge: _vertices.values()) {
            _cursorY = firstEdge._origin.y;

            /*
             * Check if we are on the pivot vertex of an inner cycle, and if so,
             * if another edge is directly to its left within the current sweep line.
             * 
             * Since all edges in the sweep line point downward, and each edge’s face
             * points to the left relative to its direction, that edge’s face is either
             * the outer cycle bounding the inner cycle or a neighboring inner cycle.
             * 
             * If there is such an edge, we link both cycles; otherwise, the inner cycle
             * belongs to the unbounded face and is added to the general output list.
             * 
             * We can use any arbitrary edge that originates with the current vertex
             * to query the sweep line, so we just use the first one. While we may find
             * a previous node, we will never find an exact match since the pivot vertex
             * is defined as the lexicographically smallest vertex of all attached edges,
             * so none of these edges have been added to the sweep line yet.
             */
            final EdgeCycle innerCycle = pivotToInnerCycle.get(firstEdge._origin);
            if (innerCycle != null) {

                final SubdivisionEdge leftNode = sweepLine.floor(firstEdge._twin);
                if (leftNode != null) {
                    final EdgeCycle leftCycle = edgeToCycle.get(leftNode._key);
                    innerCycle.next = leftCycle.next;
                    leftCycle.next = innerCycle;
                } else
                    innerCycles.add(innerCycle);

                // we’re done if all pivot vertices were processed
                if (++innerCyclesFound == pivotToInnerCycle.size())
                    break;
            }

            /*
             * Sweepline ordering is easier if all edges point in the same direction.
             * Choose the twin half-edge that points downward, so that its face points
             * toward the pivot vertex of any inner cycle to its right (see above).
             * 
             * The origin of a downward-pointing half-edge is its lexicographically
             * greater point (= towards the upper-right), and the destination is its
             * lexicographically smaller point (= towards the lower-left).
             * 
             * The sweep line moves upward (= increasing y-coordinates), which means:
             * 
             * 1. When we find an edge’s destination, we must have encountered that edge
             *    for the first time and therefore need to add it to the sweep line.
             * 
             * 2. When we find an edge’s origin, we must have already added that edge at
             *    its destination, and therefore need to remove it from the sweep line.
             */
            SubdivisionEdge edge = firstEdge;
            do {
                final int direction = vertexComparer.compare(edge._origin, edge._twin._origin);
                assert(direction != 0);
                final SubdivisionEdge downEdge = (direction < 0 ? edge._twin : edge);

                if (downEdge._origin.equals(firstEdge._origin)) {
                    assert(sweepLine.contains(downEdge));
                    sweepLine.remove(downEdge);
                } else {
                    assert(downEdge._twin._origin.equals(firstEdge._origin));
                    sweepLine.add(downEdge);
                }
                edge = edge._twin._next;

            } while (edge != firstEdge);
        }

        assert(innerCyclesFound == pivotToInnerCycle.size());
        assert(!innerCycles.isEmpty());

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final List<EdgeCycle>[] result = new List[] { innerCycles, outerCycles };
        return result;
    }

    /**
     * Finds any existing {@link SubdivisionFace} mapping within the cycle
     * containing the specified {@link SubdivisionEdge}.
     *
     * @param edge the {@link SubdivisionEdge} whose cycle to examine
     * @param edgeToFace a partially filled {@link Map} mapping the keys of all output {@link #edges}
     *                   to the keys of the containing {@link #faces} in an input {@link Subdivision}
     * @return an existing valid mapping (including zero for the unbounded face) within {@code edgeToFace}
     *         for any {@link #edges} within the cycle containing {@code edge}, or {@code null} if none found
     * @throws NullPointerException if any argument is {@code null}
     */
    private static Integer findCycleFace(SubdivisionEdge edge, Map<Integer, Integer> edgeToFace) {

        SubdivisionEdge cycle = edge;
        do {
            final Integer face = edgeToFace.get(cycle._key);
            if (face != null) return face;

            cycle = cycle._next;
        } while (cycle != edge);

        return null;
    }

    /**
     * Finds the {@link SubdivisionFace} in the current input {@link Subdivision}
     * that is incident on or contains the specified output {@link SubdivisionEdge}.
     *
     * @param edge the {@link SubdivisionEdge} in the output {@link Subdivision}
     *             whose incident or containing input {@link SubdivisionFace} to find
     * @param edgeToFace a {@link Map} mapping {@link SubdivisionEdge} keys in the
     *                   output {@link Subdivision} to {@link SubdivisionFace} keys
     *                   in the current input {@link Subdivision}
     * @return the non-negative {@link SubdivisionFace} key in the current input
     *         {@link Subdivision} that is incident on or contains {@code edge}
     * @throws NullPointerException if any argument is {@code null}
     */
    private int findInputFace(SubdivisionEdge edge, Map<Integer, Integer> edgeToFace) {
        /*
         * Check four possible sources of face mappings for the current
         * subdivision, which is one of the two intersecting divisions.
         *
         * 1. The specified edge already has an edgeToFace mapping,
         *    established either during initialization or intersection.
         */
        Integer oldFace = edgeToFace.get(edge._key);
        if (oldFace != null) return oldFace;

        /*
         * 2. The edge is in a cycle that includes one or more mapped edges.
         *    Any non-null mappings in the same cycle must be equal. Method
         *    intersection() calls checkCycleFaces to ensure this condition.
         */
        oldFace = findCycleFace(edge, edgeToFace);
        if (oldFace != null) return oldFace;

        /*
         * 3. All cycle edges are wholly contained within one face of the
         *    current input division, but there might be a connection to
         *    an input edge through the origin vertex. Process all cycles
         *    around the same origin as above until a mapping is found.
         */
        SubdivisionEdge nextCycle = edge._twin._next;
        while (nextCycle != edge._previous._twin) {

            oldFace = findCycleFace(nextCycle, edgeToFace);
            if (oldFace != null) return oldFace;

            nextCycle = nextCycle._twin._next;
        }

        /*
         * 4. All cycle edges are wholly contained within one face of the
         *    current division, with no contact through the origin vertex
         *    either. Perform brute-force search for the containing face.
         */
        final SubdivisionFace face2 = findFace(edge._origin);
        return face2._key;
    }

    /**
     * Inserts the specified {@link SubdivisionEdge} into the vertex chain
     * that contains another specified instance.
     * @param edge the {@link SubdivisionEdge} to insert into the vertex chain containing {@code oldEdge}
     * @param oldEdge a {@link SubdivisionEdge} that is already linked into the vertex chain
     *                around its {@link SubdivisionEdge#origin}, which must equal that of {@code edge}
     * @throws IllegalArgumentException if the {@link SubdivisionEdge#origin} of {@code edge}
     *                                  does not equal that of {@code oldEdge}
     * @throws NullPointerException if {@code edge} or {@code oldEdge} is {@code null}
     */
    private static void insertAtEdge(SubdivisionEdge edge, SubdivisionEdge oldEdge ) {
        assert(edge != oldEdge);
        if (!edge._origin.equals(oldEdge._origin))
            throw new IllegalArgumentException("edge.origin != oldEdge.origin");

        if (oldEdge._previous == oldEdge._twin) {
            // simple case: no other edge linked to old edge
            edge._previous = oldEdge._twin;
            edge._twin._next = oldEdge;
        } else {
            // find position of edge in existing vertex chain
            final SubdivisionEdge[] neighbors = oldEdge.findEdgePosition(edge._twin._origin);
            edge._previous = neighbors[1]._twin;
            edge._twin._next = neighbors[0];
        }

        // establish double-link invariants
        edge._previous._next = edge;
        edge._twin._next._previous = edge._twin;
    }

    /**
     * Inserts the specified {@link SubdivisionEdge} into the vertex chain
     * around its {@link SubdivisionEdge#origin}.
     * Also adds the {@link SubdivisionEdge#origin} of {@code edge}
     * to the {@link #vertices} collection if not already present.
     * 
     * @param edge the {@link SubdivisionEdge} to insert into the vertex chain
     *             around its {@link SubdivisionEdge#origin}
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    private void insertAtOrigin(SubdivisionEdge edge) {

        // get incident edge at existing vertex, if any
        final PointD key = edge._origin;
        final SubdivisionEdge oldEdge = _vertices.get(key);

        // add new vertex with edge pair if not present
        if (oldEdge == null) {
            _vertices.put(key, edge);
            edge._previous = edge._twin;
            edge._twin._next = edge;
            return;
        }

        // check if new edge already inserted
        SubdivisionEdge cursor = oldEdge;
        do {
            if (cursor == edge) return;
            cursor = cursor._twin._next;
        } while (cursor != oldEdge);

        // not found, insert edge as usual
        insertAtEdge(edge, oldEdge);
    }

    /**
     * Intersects the {@link #edges} of this instance with those of the specified {@link Subdivision}.
     * Partially implements {@link #intersection}. As noted there, the {@link #epsilon} of the
     * specified {@code division2} cannot be smaller than that of the current {@link Subdivision},
     * which represents the output of the intersection operation and adopts the {@link #epsilon}
     * of the first {@link #intersection} argument.
     * <p>
     * The specified {@code edgeToFace1} collection may receive new elements if any existing
     * {@link #edges} are split by the intersection. All new elements will record the same
     * {@link SubdivisionFace#key} as the element that refers to the split {@link SubdivisionEdge}.</p>
     * 
     * @param division2 the {@link Subdivision} whose {@link #edges} to intersect with this instance
     * @param edgeToFace1 a filled {@link Map} mapping the keys of any existing {@link #edges} to the keys
     *                    of the incident bounded {@link #faces} of the corresponding {@link #edges} in the
     *                    {@link Subdivision} that was the first {@link #intersection} argument
     * @param edgeToFace2 an empty {@link Map}, on return mapping the keys of any newly inserted
     *                    {@link #edges} to the keys of the incident bounded {@link #faces} of the
     *                    corresponding {@link #edges} in the intersecting {@code division2}
     * @throws IllegalArgumentException if {@code division2} contains invalid {@link #edges}
     * @throws IllegalStateException if the current {@link Subdivision} contains invalid {@link #edges}
     * @throws NullPointerException if any argument is {@code null}
     */
    private void intersectEdges(Subdivision division2, 
        Map<Integer, Integer> edgeToFace1, Map<Integer, Integer> edgeToFace2) {

        assert (epsilon <= division2.epsilon);
        assert edgeToFace2.isEmpty();

        // minimum epsilon for edge intersection algorithm
        final double minEpsilon = Math.max(epsilon, 1e-10);

        // store one twin out of each half-edge pair
        final List<SubdivisionEdge> edge1List = new ArrayList<>(_edges.size());
        for (SubdivisionEdge edge1: _edges.values())
            if (edge1._key < edge1._twin._key)
                edge1List.add(edge1);

        /*
         * Intersect all division1 edges with all division2 edges, testing one half-edge per pair.
         * 
         * If any edge is split, we add one new half-edge to the corresponding collection for
         * future comparisons, some of which are redundant.
         * 
         * New division1 edges go into edge1List. New division2 edges go into a temporary
         * stack which is emptied before we move on to the next original division2 edge.
         * 
         * Any duplicate (congruent) edges are skipped in for division2 only. We never completely
         * delete any pre-existing division1 edges, although they may be shortened.
         */
        final Stack<SubdivisionEdge> edge2Stack = new Stack<>();
        final Iterator<SubdivisionEdge> edge2Iterator = division2._edges.values().iterator();

    skipCurrentEdge2:
        while (true) {
            SubdivisionEdge edge2;

            if (!edge2Stack.isEmpty()) {
                // process any newly split edges first
                edge2 = edge2Stack.pop();
            } else {
                // we’re done if all second-instance edges are processed
                if (!edge2Iterator.hasNext())
                    break;

                // fetch one twin from each half-edge pair
                final SubdivisionEdge edge = edge2Iterator.next();
                if (edge._key > edge._twin._key) continue;

                // recreate edge or find existing first-instance edge
                final CreateEdgeResult result = createTwinEdges(edge._origin, edge._twin._origin);
                edge2 = result.startEdge;

                // store all incident division2 faces for face mapping
                edgeToFace2.put(edge2._key, edge._face._key);
                edgeToFace2.put(edge2._twin._key, edge._twin._face._key);

                // skip edge if duplicated by existing first-instance edge
                if (!result.isEdgeCreated) continue;
            }

            // compare all first-instance edges against current second-instance edge
            // (cannot use enhanced for loop, body might add list elements!)
            for (int edge1Index = 0; edge1Index < edge1List.size(); edge1Index++) {
                SubdivisionEdge edge1 = edge1List.get(edge1Index);

                // intersect first-instance and second-instance edge
                final LineIntersection crossing = LineIntersection.find(
                    edge1._origin, edge1._twin._origin,
                    edge2._origin, edge2._twin._origin, minEpsilon);

                if (!crossing.exists()) continue;
                SplitEdgeResult split;

                if (crossing.relation == LineRelation.COLLINEAR) {
                    /*
                     * The various cases for overlapping collinear edges assume that both edges
                     * are equidirectional. We must ensure this condition using our intersection
                     * epsilon to handle infinitesimal y-differences in the wrong direction.
                     */
                    if (PointDComparatorY.compareEpsilon(
                            edge1._origin, edge1._twin._origin, minEpsilon) > 0)
                        edge1 = edge1._twin;

                    if (PointDComparatorY.compareEpsilon(
                            edge2._origin, edge2._twin._origin, minEpsilon) > 0)
                        edge2 = edge2._twin;

                    // relative locations of edge2’s origin & destination
                    final LineLocation edge2Start = LineIntersection.locateCollinear(
                            edge1._origin, edge1._twin._origin, edge2._origin, minEpsilon);

                    final LineLocation edge2End = LineIntersection.locateCollinear(
                            edge1._origin, edge1._twin._origin, edge2._twin._origin, minEpsilon);

                    switch (edge2Start) {
                    case BEFORE:
                        switch (edge2End) {

                        case BEFORE:
                            // see below for documentation of this exception
                            throw new IllegalStateException("locateCollinear");

                        case START:
                            // edges already linked, nothing to do
                            continue;

                        case BETWEEN:
                            // edge1     --------->
                            // edge2 --------->
                            split = trySplitEdge(edge1, edge2._twin._origin);
                            split.updateFaces(edge1, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge1List.add(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                throw new IllegalStateException("trySplitEdge found overlapping edges");

                            if (moveOriginToEdge(edge2._twin, split.originEdge) != null)
                                continue skipCurrentEdge2;
                            continue;

                        case END:
                            // edge1       ----->
                            // edge2 ----------->
                            if (moveOriginToEdge(edge2._twin, edge1) != null)
                                continue skipCurrentEdge2;
                            continue;

                        case AFTER:
                            // edge1      ----->
                            // edge2 --------------->
                            split = trySplitEdge(edge2, edge1._origin);
                            split.updateFaces(edge2, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge2Stack.push(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                throw new IllegalStateException("trySplitEdge found overlapping edges");

                            if (moveOriginToEdge(split.destinationEdge, edge1._twin) != null) {
                                if (split.destinationEdge == edge2)
                                    continue skipCurrentEdge2;
                                else if (split.destinationEdge == split.createdEdge)
                                    edge2Stack.pop();
                            }
                            continue;
                        }
                        break;

                    case START:
                        switch (edge2End) {

                        case BEFORE:
                            throw new IllegalStateException("locateCollinear");

                        case START:
                            // second instance contains zero-length edge
                            throw new IllegalArgumentException("division contains zero-length edge");

                        case BETWEEN:
                            // edge1 ----------->
                            // edge2 ----->
                            if (moveOriginToEdge(edge1, edge2._twin) != null)
                                throw new IllegalStateException("moveOriginToEdge found overlapping edges");
                            continue;

                        case END:
                            // exactly congruent edges
                            throw new IllegalStateException("locateCollinear");

                        case AFTER:
                            // edge1 ----->
                            // edge2 ----------->
                            if (moveOriginToEdge(edge2, edge1._twin) != null)
                                continue skipCurrentEdge2;
                            continue;
                        }
                        break;

                    case BETWEEN:
                        switch (edge2End) {

                        case BEFORE:
                        case START:
                            throw new IllegalStateException("locateCollinear");

                        case BETWEEN:
                            // edge1 --------------->
                            // edge2      ----->
                            split = trySplitEdge(edge1, edge2._origin);
                            split.updateFaces(edge1, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge1List.add(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                throw new IllegalStateException("trySplitEdge found overlapping edges");

                            if (moveOriginToEdge(split.destinationEdge, edge2._twin) != null)
                                throw new IllegalStateException("moveOriginToEdge found overlapping edges");
                            continue;

                        case END:
                            // edge1 ----------->
                            // edge2       ----->
                            if (moveOriginToEdge(edge1._twin, edge2) != null)
                                throw new IllegalStateException("moveOriginToEdge found overlapping edges");
                            continue;

                        case AFTER:
                            // edge1 --------->
                            // edge2     --------->
                            split = trySplitEdge(edge1, edge2._origin);
                            split.updateFaces(edge1, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge1List.add(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                throw new IllegalStateException("trySplitEdge found overlapping edges");

                            if (moveOriginToEdge(edge2, split.destinationEdge._twin) != null)
                                continue skipCurrentEdge2;
                            continue;
                        }
                        break;

                    case END:
                        switch (edge2End) {

                        case BEFORE:
                        case START:
                        case BETWEEN:
                            throw new IllegalStateException("locateCollinear");

                        case END:
                            throw new IllegalArgumentException("division contains zero-length edge");

                        case AFTER:
                            // edges already linked, nothing to do
                            continue;
                        }
                        break;

                    case AFTER:
                        throw new IllegalStateException("locateCollinear");
                    }

                    /*
                     * Both collinear edges point in opposite directions, which is impossible
                     * since they are sorted by lexicographically smaller origins; or else they
                     * are exactly congruent, which is also impossible since they were created
                     * by createTwinEdges which never creates duplicate edges.
                     */
                    throw new IllegalStateException("locateCollinear");
                } else {
                    assert(crossing.relation == LineRelation.DIVERGENT);
                    /*
                     * Divergent intersecting edges: We need only consider the case where either
                     * or both edges cross between their origin and destination, as the case
                     * with two edges touching at a common existing vertex is already covered by
                     * the vertex chain insertion performed by createTwinEdges.
                     */
                    if (crossing.first == LineLocation.BETWEEN) {
                        switch (crossing.second) {

                        case START:
                            split = trySplitEdge(edge1, edge2._origin);
                            split.updateFaces(edge1, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge1List.add(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                throw new IllegalStateException("trySplitEdge found overlapping edges");
                            continue;

                        case END:
                            split = trySplitEdge(edge1, edge2._twin._origin);
                            split.updateFaces(edge1, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge1List.add(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                throw new IllegalStateException("trySplitEdge found overlapping edges");
                            continue;

                        case BETWEEN:
                            split = trySplitEdge(edge1, crossing.shared);
                            split.updateFaces(edge1, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge1List.add(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                throw new IllegalStateException("trySplitEdge found overlapping edges");

                            split = trySplitEdge(edge2, split.destinationEdge._origin);
                            split.updateFaces(edge2, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge2Stack.push(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                continue skipCurrentEdge2;
                        }
                    }
                    else if (crossing.second == LineLocation.BETWEEN) {
                        switch (crossing.first) {

                        case START:
                            split = trySplitEdge(edge2, edge1._origin);
                            split.updateFaces(edge2, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge2Stack.push(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                continue skipCurrentEdge2;
                            continue;

                        case END:
                            split = trySplitEdge(edge2, edge1._twin._origin);
                            split.updateFaces(edge2, edgeToFace1, edgeToFace2);

                            if (split.createdEdge != null)
                                edge2Stack.push(split.createdEdge);
                            else if (split.isEdgeDeleted)
                                continue skipCurrentEdge2;
                        }
                    }
                }
            }
        }
    }

    /**
     * Moves the specified {@link SubdivisionEdge} to the vertex chain
     * that contains another specified instance.
     * Changes the {@link SubdivisionEdge#origin} of the specified {@code edge}, and also
     * updates all incident links on its old and new {@link SubdivisionEdge#origin}.
     * <p>
     * Deletes {@code edge} and its {@link SubdivisionEdge#twin} from the {@link #edges}
     * collection if an existing element connects its {@link SubdivisionEdge#destination}
     * and the {@link SubdivisionEdge#origin} of {@code oldEdge}.</p>
     * 
     * @param edge the {@link SubdivisionEdge} to move from its current vertex chain
     *             to the one containing {@code oldEdge}
     * @param oldEdge a {@link SubdivisionEdge} that is already linked into the vertex chain
     *                around its {@link SubdivisionEdge#origin}
     * @return {@code null} if {@code edge} was successfully moved to the vertex chain containing
     *         {@code oldEdge}, or an existing {@link SubdivisionEdge} already linked into that
     *         vertex chain and whose {@link SubdivisionEdge#destination} equals that of {@code edge}
     * @throws NullPointerException if {@code edge} or {@code oldEdge} is {@code null}
     */
    private SubdivisionEdge moveOriginToEdge(SubdivisionEdge edge, SubdivisionEdge oldEdge) {

        removeAtOrigin(edge);
        final SubdivisionEdge twin = edge._twin;

        // check for existing edge between vertices
        SubdivisionEdge cursor = oldEdge;
        do {
            if (cursor._twin._origin == twin._origin) {
                removeAtOrigin(twin);
                _edges.remove(edge._key);
                _edges.remove(twin._key);
                return cursor;
            }
            cursor = cursor._twin._next;
        } while (cursor != oldEdge);

        // re-insert edge at new origin
        edge._origin = oldEdge._origin;
        insertAtEdge(edge, oldEdge);
        return null;
    }

    /**
     * Removes the specified {@link SubdivisionEdge} from the vertex chain
     * around its {@link SubdivisionEdge#origin}.
     * Also removes the {@link SubdivisionEdge#origin} of {@code edge} from the {@link #vertices}
     * collection if there are no other incident half-edges, and otherwise changes the incident
     * half-edge in the {@link #vertices} collection if it equals {@code edge}.
     * 
     * @param edge the {@link SubdivisionEdge} to remove from its {@link SubdivisionEdge#origin} chain
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    private void removeAtOrigin(SubdivisionEdge edge) {

        final SubdivisionEdge twin = edge._twin;
        final PointD key = edge._origin;
        assert(_vertices.containsKey(key));

        // remove vertex entirely if no other edges
        if (edge._previous == twin) {
            _vertices.remove(key);
            return;
        }

        // remove half-edge from vertex chain
        edge._previous._next = twin._next;
        twin._next._previous = edge._previous;

        // update incident half-edge if necessary
        if (_vertices.get(key) == edge)
            _vertices.put(key, edge._twin._next);
    }

    /**
     * Splits the specified edge in two parts around the specified {@link PointD} vertex.
     * @param edge the {@link SubdivisionEdge} to split, whose {@link SubdivisionEdge#twin} is split as well
     * @param vertex the {@link PointD} vertex around which to split {@code edge} and its twin
     * @param addVertex {@code true} to add {@code vertex} to {@link #vertices}, else {@code false}
     * @return one of the two new {@link SubdivisionEdge} instances that originate from {@code vertex}
     * @throws NullPointerException if {@code edge} or {@code vertex} is {@code null}
     */
    private SubdivisionEdge splitEdgeAtVertex(SubdivisionEdge edge, PointD vertex, boolean addVertex) {
        if (vertex == null)
            throw new NullPointerException("vertex");

        final SubdivisionEdge twin = edge._twin;
        final SubdivisionEdge newEdge = new SubdivisionEdge(_nextEdgeKey++);
        _edges.put(newEdge._key, newEdge);

        newEdge._origin = vertex;
        newEdge._face = edge._face;
        newEdge._twin = twin; twin._twin = newEdge;
        newEdge._next = edge._next;
        newEdge._next._previous = newEdge;

        final SubdivisionEdge newTwin = new SubdivisionEdge(_nextEdgeKey++);
        _edges.put(newTwin._key, newTwin);

        newTwin._origin = vertex;
        newTwin._face = twin._face;
        newTwin._twin = edge; edge._twin = newTwin;
        newTwin._next = twin._next;
        twin._next._previous = newTwin;

        if (addVertex) {
            _vertices.put(vertex, newEdge);
            edge._next = newEdge;
            newEdge._previous = edge;
            twin._next = newTwin;
            newTwin._previous = twin;
        } else {
            newEdge._previous = twin;
            twin._next = newEdge;
            newTwin._previous = edge;
            edge._next = newTwin;
            insertAtOrigin(newEdge);
            insertAtOrigin(newTwin);
        }

        return newEdge;
    }

    /**
     * Attempts to split the specified edge in two parts around the specified {@link PointD} vertex.
     * Also adds {@code vertex} to the {@link #vertices} collection if not already present.
     * 
     * @param edge the {@link SubdivisionEdge} to split, whose {@link SubdivisionEdge#twin} is split as well
     * @param vertex the {@link PointD} vertex around which to split {@code edge} and its twin
     * @return a {@link SplitEdgeResult} containing the result of the operation
     * @throws NullPointerException if {@code edge} or {@code vertex} is {@code null}
     */
    private SplitEdgeResult trySplitEdge(SubdivisionEdge edge, PointD vertex) {

        SubdivisionEdge twin = edge._twin;
        assert (vertex != edge._origin);
        assert (vertex != twin._origin);
        SubdivisionEdge originEdge = null, destinationEdge = null;

        // check for existing edges between vertex and end points
        final SubdivisionEdge incidentEdge = _vertices.get(vertex);
        if (incidentEdge != null) {
            SubdivisionEdge cursor = incidentEdge;
            do {
                PointD origin = cursor._twin._origin;

                if (origin == edge._origin) {
                    originEdge = cursor._twin;
                    if (destinationEdge != null) break;
                }
                else if (origin == twin._origin) {
                    destinationEdge = cursor;
                    if (originEdge != null) break;
                }

                cursor = cursor._twin._next;
            } while (cursor != incidentEdge);

            // set vertex to existing coordinates
            vertex = incidentEdge._origin;
        }

        // no connecting edges exist: create two new half-edges
        if (originEdge == null && destinationEdge == null) {
            SubdivisionEdge newEdge = splitEdgeAtVertex(edge, vertex, (incidentEdge == null));
            return new SplitEdgeResult(edge, newEdge, newEdge, false);
        }

        // both connecting edges exist: delete edge to be split
        if (originEdge != null && destinationEdge != null) {
            removeAtOrigin(edge);
            removeAtOrigin(twin);
            _edges.remove(edge._key);
            _edges.remove(twin._key);
            return new SplitEdgeResult(originEdge, destinationEdge, null, true);
        }

        // one connecting edge exist: shorten edge for other part
        if (originEdge != null) {
            removeAtOrigin(edge);
            edge._origin = vertex;
            insertAtEdge(edge, originEdge._twin);
            return new SplitEdgeResult(originEdge, edge, null, false);
        }
        else {
            assert(destinationEdge != null);
            removeAtOrigin(edge._twin);
            edge._twin._origin = vertex;
            insertAtEdge(edge._twin, destinationEdge);
            return new SplitEdgeResult(edge, destinationEdge, null, false);
        }
    }
}

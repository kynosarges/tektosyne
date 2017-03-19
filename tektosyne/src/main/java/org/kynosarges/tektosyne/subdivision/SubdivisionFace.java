package org.kynosarges.tektosyne.subdivision;

import java.util.*;
import java.util.function.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Represents a face in a planar {@link Subdivision}.
 * Represents any polygonal region that is bounded by the edges of a {@link Subdivision}, whether
 * on the inside, on the outside, or both. There is always exactly one face without an outer boundary,
 * called the unbounded face, which comprises the entire plane outside of the {@link Subdivision}.
 * <p>
 * A {@link SubdivisionFace} stores one {@link SubdivisionEdge} for each of its outer and inner
 * boundaries. The corresponding polygonal region can be reconstructed from the cycle of half-edges
 * that begins with an incident {@link SubdivisionFace#outerEdge} or {@link SubdivisionFace#innerEdges} 
 * element. Use the <b>cycle…</b> properties of these half-edges to obtain face boundaries etc.</p>
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class SubdivisionFace {
    /**
     * The {@link Subdivision} that contains the {@link SubdivisionFace}.
     * Never {@code null}.
     */
    public final Subdivision owner;

    /**
     * The unique key of the {@link SubdivisionFace}.
     */
    int _key;

    /**
     * A {@link SubdivisionEdge} on the outer boundary of the {@link SubdivisionFace}.
     */
    SubdivisionEdge _outerEdge;

    /**
     * A {@link List} containing one {@link SubdivisionEdge}
     * on each inner boundary of the {@link SubdivisionFace}.
     * Defaults to {@code null} to save memory in the frequent case that the {@link SubdivisionFace}
     * contains no inner boundaries. Use {@link #innerEdges} to avoid checking for {@code null}.
     */
    List<SubdivisionEdge> _innerEdges;

    /**
     * Creates a {@link SubdivisionFace} with the specified containing {@link Subdivision} and unique key.
     * @param owner the {@link Subdivision} that contains the {@link SubdivisionFace}
     * @param key the unique key of the {@link SubdivisionFace} within {@code owner}
     * @throws NullPointerException if {@code owner} is {@code null}
     */
    SubdivisionFace(Subdivision owner, int key) {
        if (owner == null)
            throw new NullPointerException("owner");

        this.owner = owner;
        this._key = key;
    }

    /**
     * Creates a {@link SubdivisionFace} with the specified containing {@link Subdivision},
     * unique key, and outer and inner {@link SubdivisionEdge} boundaries.
     * {@code outerEdge} and {@code innerEdges} may be {@code null}.
     * 
     * @param owner the {@link Subdivision} that contains the {@link SubdivisionFace}
     * @param key the unique key of the {@link SubdivisionFace} within {@code owner}
     * @param outerEdge a {@link SubdivisionEdge} on the outer boundary of the {@link SubdivisionFace}
     * @param innerEdges a {@link Collection} containing one {@link SubdivisionEdge}
     *                   on each inner boundary of the {@link SubdivisionFace}
     * @throws NullPointerException if {@code owner} is {@code null}
     */
    SubdivisionFace(Subdivision owner, int key,
        SubdivisionEdge outerEdge, Collection<SubdivisionEdge> innerEdges) {
        this(owner, key);

        this._outerEdge = outerEdge;
        if (innerEdges != null)
            this._innerEdges = new ArrayList<>(innerEdges);
    }

    /**
     * Gets the unique key of the {@link SubdivisionFace}.
     * Starts at zero for the first {@link SubdivisionFace} in a {@link Subdivision},
     * and is incremented by one for each additional {@link SubdivisionFace}. {@link #key}
     * thus reflects the order in which {@link SubdivisionFace} instances were created.
     * Immutable unless {@link Subdivision#renumberFaces} is called on the {@link #owner}.
     * 
     * @return the unique key of the {@link SubdivisionFace} within {@link #owner}
     */
    public int key() {
        return _key;
    }

    /**
     * Gets all {@link SubdivisionEdge} instances on all boundaries of the {@link SubdivisionFace}.
     * Starts with {@link #outerEdge}, if any, and follows the chain of {@link SubdivisionEdge#next}
     * links until the cycle is complete. This process is then repeated for all {@link #innerEdges}.
     * 
     * @return a {@link List} containing all {@link SubdivisionEdge} instances
     *         surrounding all boundaries of the {@link SubdivisionFace}
     */
    public List<SubdivisionEdge> allCycleEdges() {
        final List<SubdivisionEdge> edges = new ArrayList<>();

        // add all outer cycle edges
        if (_outerEdge != null) {
            SubdivisionEdge edge = _outerEdge;
            do {
                edges.add(edge);
                edge = edge._next;
            } while (edge != _outerEdge);
        }

        if (_innerEdges == null)
            return edges;

        // add all inner cycle edges
        for (SubdivisionEdge innerEdge: _innerEdges) {
            SubdivisionEdge edge = innerEdge;
            do {
                edges.add(edge);
                edge = edge._next;
            } while (edge != innerEdge);
        }
        
        return edges;
    }

    /**
     * Gets a {@link SubdivisionEdge} on the outer boundary of the {@link SubdivisionFace}.
     * @return an inward-facing {@link SubdivisionEdge} on the outer boundary of the {@link SubdivisionFace},
     *         or {@code null} if the {@link SubdivisionFace} has no outer boundary
     */
    public SubdivisionEdge outerEdge() {
        return _outerEdge;
    }

    /**
     * Gets a {@link List} containing one {@link SubdivisionEdge}
     * on each inner boundary of the {@link SubdivisionFace}.
     * Never returns {@code null}. Returns an empty {@link List} if the
     * {@link SubdivisionFace} contains no inner boundaries, or “holes.”
     * 
     * @return an unmodifiable {@link List} containing one outward-facing {@link SubdivisionEdge}
     *         on each disconnected inner boundary of the {@link SubdivisionFace}
     */
    @SuppressWarnings("unchecked")
    public List<SubdivisionEdge> innerEdges() {
        return (_innerEdges == null ? Collections.EMPTY_LIST :
                Collections.unmodifiableList(_innerEdges));
    }

    /**
     * Finds the {@link SubdivisionEdge} bounding the {@link SubdivisionFace}
     * that is nearest to and facing the specified {@link PointD} coordinates.
     * Traverses the {@link #outerEdge} boundary and all {@link #innerEdges} boundaries,
     * computing the distance from {@code q} to each {@link SubdivisionEdge}. This is an O(n)
     * operation where n is the number of half-edges incident on the {@link SubdivisionFace}.
     * <p>
     * If {@code q} is nearest to an edge that belongs to a zero-area protrusion into the
     * {@link SubdivisionFace}, {@link #findNearestEdge} returns the twin half-edge that faces
     * {@code q}, according to its {@link SubdivisionEdge#face} orientation.</p>
     * 
     * @param q the {@link PointD} coordinates to examine
     * @return a {@link FindEdgeResult} containing the {@link SubdivisionEdge} on any boundary
     *         of the {@link SubdivisionFace} nearest to and facing {@code q}, or {@code null}
     *         if the {@link SubdivisionFace} is completely unbounded; and the distance between
     *         {@code q} and the returned {@link SubdivisionEdge}, if any, else {@link Double#MAX_VALUE}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public FindEdgeResult findNearestEdge(PointD q) {
        double distance = Double.MAX_VALUE;
        SubdivisionEdge nearestEdge = null;

        // find smallest distance to any outer cycle edge
        if (_outerEdge != null) {
            SubdivisionEdge edge = _outerEdge;
            do {
                final double d = edge.toLine().distanceSquared(q);
                if (distance > d) {
                    if (d == 0) return new FindEdgeResult(edge, 0);
                    distance = d;
                    nearestEdge = edge;
                }
                edge = edge._next;
            } while (edge != _outerEdge);
        }

        // find smallest distance to any inner cycle edge
        if (_innerEdges != null)
            for (SubdivisionEdge innerEdge: _innerEdges) {
                SubdivisionEdge edge = innerEdge;
                do {
                    final double d = edge.toLine().distanceSquared(q);
                    if (distance > d) {
                        if (d == 0) return new FindEdgeResult(edge, 0);
                        distance = d;
                        nearestEdge = edge;
                    }
                    edge = edge._next;
                } while (edge != innerEdge);
            }

        if (nearestEdge == null)
            return new FindEdgeResult(null, Double.MAX_VALUE);

        // check twin in case of zero-area protrusion
        if (nearestEdge._twin._face == this) {
            final LineLocation location = nearestEdge.toLine().locate(q);
            if (location == LineLocation.RIGHT)
                nearestEdge = nearestEdge._twin;
        }

        return new FindEdgeResult(nearestEdge, Math.sqrt(distance));
    }

    /**
     * Adds the specified {@link SubdivisionEdge} to {@link #innerEdges}.
     * @param edge the {@link SubdivisionEdge} to add
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    void addInnerEdge(SubdivisionEdge edge) {
        if (edge == null)
            throw new NullPointerException("edge");

        if (_innerEdges == null)
            _innerEdges = new ArrayList<>();

        _innerEdges.add(edge);
    }

    /**
     * Adds the specified {@link SubdivisionEdge} instances to {@link #innerEdges}.
     * Does nothing if {@code edges} is {@code null} or empty.
     * 
     * @param edges a {@link List} containing the {@link SubdivisionEdge} instances to add
     */
    void addInnerEdges(List<SubdivisionEdge> edges) {
        if (edges == null || edges.isEmpty())
            return;

        if (_innerEdges == null)
            _innerEdges = new ArrayList<>();

        _innerEdges.addAll(edges);
    }

    /**
     * Moves the incident {@link SubdivisionEdge} on one of the boundaries of the
     * {@link SubdivisionFace} away from the specified {@link SubdivisionEdge}.
     * If {@link #outerEdge} equals {@code oldEdge} or its twin, {@link #outerEdge} is set
     * to the result of {@link SubdivisionEdge#getOtherCycleEdge} for {@code oldEdge}.
     * <p>
     * Otherwise, {@code moveEdge} searches for an {@link #innerEdges} element that equals
     * {@code oldEdge} or its twin. On success, that element is removed if the cycle contains
     * no other half-edges; otherwise, that element is set to the result of
     * {@link SubdivisionEdge#getOtherCycleEdge} for {@code oldEdge}.</p>
     * 
     * @param oldEdge the incident {@link SubdivisionEdge} to replace with another instance
     *                on the same boundary of the {@link SubdivisionFace}
     * @return a {@link MoveEdgeResult} value indicating which {@link SubdivisionFace}
     *         properties were changed, if any
     * @throws NullPointerException if {@code oldEdge} is {@code null}
     */
    MoveEdgeResult moveEdge(SubdivisionEdge oldEdge) {
        final SubdivisionEdge oldTwin = oldEdge._twin;

        if (_outerEdge == oldEdge || _outerEdge == oldTwin) {
            _outerEdge = _outerEdge.getOtherCycleEdge(oldEdge);
            assert(_outerEdge != null);
            return MoveEdgeResult.OUTER_CHANGED;
        }

        if (_innerEdges == null)
            return MoveEdgeResult.UNCHANGED;
        
        for (int i = 0; i < _innerEdges.size(); i++) {
            final SubdivisionEdge innerEdge = _innerEdges.get(i);
            if (innerEdge != oldEdge && innerEdge != oldTwin)
                continue;

            if (oldEdge._next == oldTwin && oldEdge._previous == oldTwin) {
                _innerEdges.remove(i);
                if (_innerEdges.isEmpty())
                    _innerEdges = null;
                return MoveEdgeResult.INNER_REMOVED;
            }

            _innerEdges.set(i, innerEdge.getOtherCycleEdge(oldEdge));
            assert(_innerEdges.get(i) != null);
            return MoveEdgeResult.INNER_CHANGED;
        }

        return MoveEdgeResult.UNCHANGED;
    }

    /**
     * Moves the incident {@link SubdivisionEdge} on one of the boundaries of the
     * {@link SubdivisionFace} from one specified {@link SubdivisionEdge} to another.
     * If {@link #outerEdge} equals {@code oldEdge}, {@link #outerEdge} is set to {@code newEdge}.
     * Otherwise, the first {@link #innerEdges} element (if any) that equals {@code oldEdge}
     * is set to {@code newEdge}.
     * <p>
     * Unlike the other {@link #moveEdge} overload, this method does not check the
     * {@link SubdivisionEdge#twin} of {@code oldEdge}, nor remove single-edge cylces.</p>
     * 
     * @param oldEdge the incident {@link SubdivisionEdge} to replace with {@code newEdge}
     * @param newEdge the incident {@link SubdivisionEdge} that replaces {@code oldEdge}
     * @return a {@link MoveEdgeResult} value indicating which {@link SubdivisionFace}
     *         properties were changed, if any
     * @throws NullPointerException if {@code oldEdge} or {@code newEdge} is {@code null}
     */
    MoveEdgeResult moveEdge(SubdivisionEdge oldEdge, SubdivisionEdge newEdge) {
        if (oldEdge == null)
            throw new NullPointerException("oldEdge");
        if (newEdge == null)
            throw new NullPointerException("newEdge");

        if (_outerEdge == oldEdge) {
            _outerEdge = newEdge;
            return MoveEdgeResult.OUTER_CHANGED;
        }

        if (_innerEdges != null)
            for (int i = 0; i < _innerEdges.size(); i++)
                if (_innerEdges.get(i) == oldEdge) {
                    _innerEdges.set(i, newEdge);
                    return MoveEdgeResult.INNER_CHANGED;
                }

        return MoveEdgeResult.UNCHANGED;
    }

    /**
     * Sets the {@link SubdivisionEdge#face} of each {@link SubdivisionEdge} in the
     * {@link #outerEdge} cycle and all {@link #innerEdges} cycles to the specified instance.
     * @param face the new {@link Subdivision#face} for each {@link SubdivisionEdge}
     * @throws NullPointerException if {@code face} is {@code null}
     */
    void setAllEdgeFaces(SubdivisionFace face) {
        if (face == null)
            throw new NullPointerException("face");

        if (_outerEdge != null)
            _outerEdge.setAllFaces(face);

        if (_innerEdges != null)
            for (int i = 0; i < _innerEdges.size(); i++)
                _innerEdges.get(i).setAllFaces(face);
    }

    /**
     * Compares the specified {@link Object} to this {@link SubdivisionFace} instance.
     * Intended for testing, as any two {@link SubdivisionFace} instances in the same
     * {@link Subdivision} are never equal.
     * 
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link SubdivisionFace} instance
     *         whose {@link #key} and incident edges equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof SubdivisionFace))
            return false;
        
        final SubdivisionFace face = (SubdivisionFace) obj;
        return (_key == face._key &&
                Objects.equals(_outerEdge, face._outerEdge) &&
                Objects.equals(_innerEdges, face._innerEdges));
    }

    /**
     * Returns a hash code for the {@link SubdivisionFace}.
     * Returns {@link #key} which is guaranteed to be unique within {@link #owner}.
     * 
     * @return an {@link Integer} hash code for the {@link SubdivisionFace}
     */
    @Override
    public int hashCode() {
        return _key;
    }

    /**
     * Returns a {@link String} representation of the {@link SubdivisionFace}.
     * Objects that are {@code null} are represented with a key value of -1.
     * 
     * @return a {@link String} containing the values of {@link #key} and the keys
     *         of {@link #outerEdge} and all {@link #innerEdges} elements
     */
    @Override
    public String toString() {
        final ToIntFunction<SubdivisionEdge> edgeKey = (e -> e == null ? -1 : e._key);
        final String innerEdges = (_innerEdges == null ? "none" :
                Arrays.toString(_innerEdges.stream().mapToInt(edgeKey).toArray()));

        return String.format("SubdivisionFace[key=%d, outerEdge=%d, innerEdges=%s]",
                _key, edgeKey.applyAsInt(_outerEdge), innerEdges);
    }
}

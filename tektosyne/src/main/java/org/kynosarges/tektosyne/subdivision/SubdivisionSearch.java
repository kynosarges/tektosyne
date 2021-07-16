package org.kynosarges.tektosyne.subdivision;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a fast search structure for a planar {@link Subdivision}.
 * Creates a search structure that achieves a query time of O(log n) for point location
 * in a planar {@link Subdivision} with n full edges. However, the search structure itself
 * occupies O(n) fairly big objects which require O(n log n) steps to construct, and must
 * be recreated whenever the underlying {@link Subdivision} changes.
 * <p>
 * Moreover, {@link SubdivisionSearch} requires a minimum epsilon of 1e-10 for coordinate
 * comparisons to reliably construct its search structure, and must use the construction
 * epsilon for all point location queries. Use the brute force {@link Subdivision#find}
 * algorithm to avoid construction costs or perform searches with a different epsilon.</p>
 * <p>
 * The algorithm to incrementally construct a search structure for the trapezoidal map of a
 * planar subdivision was adapted from Mark de Berg et al., <em>Computational Geometry</em>
 * (3rd ed.), Springer-Verlag 2008, p.122-137. This implementation uses null half-edges and
 * {@link Double#POSITIVE_INFINITY} to indicate the unbounded face, rather than placing an
 * actual bounding rectangle around the {@link Subdivision}.</p>
 * 
 * @author Christoph Nahr
 * @version 6.1.1
 */
public class SubdivisionSearch {

    // search graph containing nodes & trapezoids
    private Object _tree = new Trapezoid();

    /**
     * The maximum absolute difference at which two coordinates should be considered equal.
     * Equals either 1e-10 or the comparison {@link Subdivision#epsilon} of the associated
     * {@link #source}, whichever is greater.
     * <p>
     * The {@link SubdivisionSearch} algorithm always uses a positive {@link #epsilon} to
     * guard against {@link Subdivision#vertices} with infinitesimal coordinate differences
     * that might corrupt the search structure. If you encounter exceptions or incorrect
     * search results for a given {@link Subdivision}, try using a greater comparison
     * {@link Subdivision#epsilon} in its creation.</p>
     */
    public final double epsilon;

    /**
     * The {@link Subdivision} for which the {@link SubdivisionSearch} graph was created.
     * Never {@code null}. The {@link SubdivisionSearch} graph is not updated to reflect
     * structural changes in the associated {@link Subdivision}. You must create a new
     * {@link SubdivisionSearch} to receive correct results for a changed {@link #source}.
     */
    public final Subdivision source;

    /**
     * Creates a {@link SubdivisionSearch} graph for the specified {@link Subdivision}.
     * The {@code ordered} parameter is intended for tests that require a known edge
     * insertion order. A random permutation is usually preferable, as the original
     * {@link Subdivision#edges} order can result in worst-case performance for the
     * {@link SubdivisionSearch} algorithm.
     * 
     * @param source the {@link Subdivision} to search
     * @param ordered {@code true} to insert the {@link Subdivision#edges} of {@code source}
     *                in their original order, {@code false} to apply a random permutation
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public SubdivisionSearch(Subdivision source, boolean ordered) {
        this.source = source;
        this.epsilon = Math.max(1e-10, source.epsilon);

        final SubdivisionEdge[] edges = new SubdivisionEdge[source.edges().size() / 2];
        int index = 0;

        // find all twin half-edges with lexicographically smaller origin
        for (SubdivisionEdge edge: source.edges().values())
            if (PointDComparatorX.compareEpsilon(edge._origin, edge._twin._origin, epsilon) < 0)
                edges[index++] = edge;

        assert(index == edges.length);
        if (!ordered) Collections.shuffle(Arrays.asList(edges));

        for (SubdivisionEdge edge: edges)
            insertEdge(edge);
    }

    /**
     * Finds the {@link SubdivisionElement} at the specified {@link PointD}
     * coordinates within the associated {@link #source}.
     * Returns {@link SubdivisionElement#NULL_FACE} if {@code q} lies within the unbounded face
     * of the associated {@link #source}.
     * <p>
     * If {@code q} coincides with a {@link SubdivisionEdge}, {@code find} always returns the twin
     * {@link SubdivisionEdge} whose {@link SubdivisionEdge#origin} is lexicographically smaller
     * than its {@link SubdivisionEdge#destination}, according to {@link PointDComparatorX}.</p>
     * <p>
     * {@code find} always uses the default {@link #epsilon} to determine whether {@code q}
     * coincides with an {@link SubdivisionElement#edge} or {@link SubdivisionElement#vertex}.
     * The {@link SubdivisionSearch} algorithm does not support a comparison epsilon that is
     * significantly different from that which was used to construct the search structure.</p>
     * 
     * @param q the {@link PointD} coordinates to examine
     * @return the {@link SubdivisionElement} that coincides with {@code q}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public SubdivisionElement find(PointD q) {
        if (_tree instanceof Trapezoid)
            return SubdivisionElement.NULL_FACE;

        return ((Node) _tree).find(q);
    }

    /**
     * Returns a {@link String} that represents the entire {@link SubdivisionSearch} graph.
     * Implemented as a new method, rather than overriding {@link Object#toString},
     * because the returned {@link String} may be hundreds or thousands of lines long.
     * 
     * @return a {@link String} containing the {@link Object#toString} results for all nodes
     *         in the {@link SubdivisionSearch} graph, traversed in breadth-first order
     */
    public String format() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Root: ");

        final Queue<Object> queue = new ArrayDeque<>();
        queue.add(_tree);

        while (!queue.isEmpty()) {
            final Object obj = queue.remove();

            builder.append(obj);
            builder.append("\n\n");

            if (obj instanceof Node) {
                final Node node = (Node) obj;
                if (node.left != null) queue.add(node.left);
                if (node.right != null) queue.add(node.right);
            }
        }

        return builder.toString();
    }

    /**
     * Validates the structure of the {@link SubdivisionSearch} graph.
     * Uses {@code assert} statements to verify the structural invariants of the {@link SubdivisionSearch}
     * graph. Assertions must be enabled at runtime for {@link #validate} to have any effect.
     * 
     * @throws AssertionError if the structure of the {@link SubdivisionSearch} graph is invalid
     */
    public void validate() {
        final Queue<Object> queue = new ArrayDeque<>();
        queue.add(_tree);

        while (!queue.isEmpty()) {
            final Object obj = queue.remove();

            if (obj instanceof VertexNode) {
                final VertexNode vertexNode = (VertexNode) obj;

                assert !(vertexNode.left instanceof VertexNode) ||
                        (PointDComparatorX.compareEpsilon(vertexNode.vertex,
                        ((VertexNode) vertexNode.left).vertex, epsilon) > 0);

                assert !(vertexNode.right instanceof VertexNode) ||
                        (PointDComparatorX.compareEpsilon(vertexNode.vertex,
                        ((VertexNode) vertexNode.right).vertex, epsilon) < 0);
            }

            if (obj instanceof Node) {
                final Node node = (Node) obj;
                assert(node.left != null);
                queue.add(node.left);

                assert(node.right != null);
                queue.add(node.right);

                continue;
            }

            final Trapezoid delta = (Trapezoid) obj;
            assert(!delta.isDeleted());
            assert(_tree == delta || !delta.parents.isEmpty());
            for (Node parent: delta.parents)
                assert(parent.left == delta || parent.right == delta);

            if (delta.upperLeft != null) {
                assert(!delta.upperLeft.isDeleted());
                assert(delta.upperLeft.upperRight == delta);
                assert(delta.topEdge == delta.upperLeft.topEdge);
                assert(delta.leftVertex == delta.upperLeft.rightVertex);
            }

            if (delta.upperRight != null) {
                assert(!delta.upperRight.isDeleted());
                assert(delta.upperRight.upperLeft == delta);
                assert(delta.topEdge == delta.upperRight.topEdge);
                assert(delta.rightVertex == delta.upperRight.leftVertex);
            }

            if (delta.lowerLeft != null) {
                assert(!delta.lowerLeft.isDeleted());
                assert(delta.lowerLeft.lowerRight == delta);
                assert(delta.bottomEdge == delta.lowerLeft.bottomEdge);
                assert(delta.leftVertex == delta.lowerLeft.rightVertex);
            }

            if (delta.lowerRight != null) {
                assert(!delta.lowerRight.isDeleted());
                assert(delta.lowerRight.lowerLeft == delta);
                assert(delta.bottomEdge == delta.lowerRight.bottomEdge);
                assert(delta.rightVertex == delta.lowerRight.leftVertex);
            }
        }
    }

    /**
     * Finds all {@link Trapezoid} instances that intersect the specified {@link LineD} edge.
     * The specified {@code edge} must be oriented so that its {@link LineD#start}
     * point is lexicographically smaller than its {@link LineD#end} point,
     * according to {@link PointDComparatorX}.
     * 
     * @param edge the {@link LineD} edge to examine
     * @return a list of all {@link Trapezoid} instances that intersect {@code edge},
     *         sorted by increasing x-coordinates
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    private List<Trapezoid> findEdge(LineD edge) {
        assert(_tree instanceof Node);
        assert(PointDComparatorX.compareEpsilon(edge.start, edge.end, epsilon) < 0);

        Trapezoid delta = ((Node) _tree).findEdge(edge);
        final List<Trapezoid> deltas = new ArrayList<>();
        deltas.add(delta);

        while (PointDComparatorX.compareEpsilon(edge.end, delta.rightVertex, epsilon) > 0) {
            final LineLocation result = edge.locate(delta.rightVertex);
            switch (result) {

                case LEFT:
                    delta = delta.lowerRight;
                    break;

                case RIGHT:
                    delta = delta.upperRight;
                    break;

                default:
                    throw new IllegalStateException("edge.locate != LEFT/RIGHT");
            }

            assert(delta != null);
            deltas.add(delta);
        }

        return deltas;
    }

    /**
     * Inserts the specified {@link SubdivisionEdge} into the {@link SubdivisionSearch} graph.
     * The specified {@code edge} must be oriented so that its {@link SubdivisionEdge#origin}
     * is lexicographically smaller than its {@link SubdivisionEdge#destination},
     * according to {@link PointDComparatorX}.
     * 
     * @param edge the {@link SubdivisionEdge} to insert.
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    private void insertEdge(SubdivisionEdge edge) {
        final LineD edgeLine = edge.toLine();
        assert(PointDComparatorX.compareEpsilon(edgeLine.start, edgeLine.end, epsilon) < 0);

        Trapezoid delta = null;
        if (_tree instanceof Trapezoid)
            delta = (Trapezoid) _tree;

        // find all trapezoids intersected by edge
        List<Trapezoid> deltas;
        if (delta != null) {
            deltas = new ArrayList<>(1);
            deltas.add(delta);
        } else
            deltas = findEdge(edgeLine);

        // create y-node tree for each intersected trapezoid
        final Node[] nodes = new Node[deltas.size()];
        for (int i = 0; i < nodes.length; i++) {
            final EdgeNode node = new EdgeNode(edge, edgeLine, epsilon);
            nodes[i] = node;
            delta = deltas.get(i);

            // get previous top & bottom trapezoids, if any
            Trapezoid oldUpperLeaf = null, oldLowerLeaf = null;
            if (i > 0) {
                oldUpperLeaf = (Trapezoid) nodes[i - 1].left;
                oldLowerLeaf = (Trapezoid) nodes[i - 1].right;
            }

            // link to previous top trapezoid unless edge changed
            if (oldUpperLeaf != null && delta.topEdge == oldUpperLeaf.topEdge) {
                oldUpperLeaf.rightVertex = delta.rightVertex;
                node.setLeft(oldUpperLeaf);
            } else {
                final Trapezoid upperLeaf = new Trapezoid();
                upperLeaf.leftVertex = delta.leftVertex;
                upperLeaf.rightVertex = delta.rightVertex;
                upperLeaf.bottomEdge = edge;
                upperLeaf.topEdge = delta.topEdge;
                            
                if (oldUpperLeaf != null) {
                    // establish link across new edge
                    upperLeaf.lowerLeft = oldUpperLeaf;
                    oldUpperLeaf.lowerRight = upperLeaf;

                    // copy links to existing trapezoids
                    upperLeaf.copyUpperLeft(delta);
                    oldUpperLeaf.copyUpperRight(deltas.get(i - 1));
                }
                node.setLeft(upperLeaf);
            }

            // link to previous bottom trapezoid unless edge changed
            if (oldLowerLeaf != null && delta.bottomEdge == oldLowerLeaf.bottomEdge) {
                oldLowerLeaf.rightVertex = delta.rightVertex;
                node.setRight(oldLowerLeaf);
            } else {
                final Trapezoid lowerLeaf = new Trapezoid();
                lowerLeaf.leftVertex = delta.leftVertex;
                lowerLeaf.rightVertex = delta.rightVertex;
                lowerLeaf.bottomEdge = delta.bottomEdge;
                lowerLeaf.topEdge = edge._twin;

                if (oldLowerLeaf != null) {
                    // establish link across new edge
                    lowerLeaf.upperLeft = oldLowerLeaf;
                    oldLowerLeaf.upperRight = lowerLeaf;

                    // copy links to existing trapezoids
                    lowerLeaf.copyLowerLeft(delta);
                    oldLowerLeaf.copyLowerRight(deltas.get(i - 1));
                }
                node.setRight(lowerLeaf);
            }
        }

        // get trapezoids at extreme ends
        final Trapezoid upperFirstLeaf = (Trapezoid) nodes[0].left;
        final Trapezoid lowerFirstLeaf = (Trapezoid) nodes[0].right;
        Trapezoid upperLastLeaf, lowerLastLeaf;

        if (nodes.length == 0) {
            upperLastLeaf = upperFirstLeaf;
            lowerLastLeaf = lowerFirstLeaf;
        } else {
            upperLastLeaf = (Trapezoid) nodes[nodes.length - 1].left;
            lowerLastLeaf = (Trapezoid) nodes[nodes.length - 1].right;
        }

        // create right x-node for new right vertex
        delta = deltas.get(deltas.size() - 1);
        if (delta.rightVertex != edgeLine.end) {
            upperLastLeaf.rightVertex = lowerLastLeaf.rightVertex = edgeLine.end;

            final Trapezoid leaf = new Trapezoid();
            leaf.leftVertex = edgeLine.end;
            leaf.rightVertex = delta.rightVertex;
            leaf.bottomEdge = delta.bottomEdge;
            leaf.topEdge = delta.topEdge;

            leaf.copyUpperRight(delta);
            leaf.copyLowerRight(delta);

            upperLastLeaf.upperRight = lowerLastLeaf.lowerRight = leaf;
            leaf.upperLeft = upperLastLeaf;
            leaf.lowerLeft = lowerLastLeaf;

            final VertexNode node = new VertexNode(edgeLine.end, epsilon);
            node.setRight(leaf);
            node.left = nodes[nodes.length - 1];
            nodes[nodes.length - 1] = node;
        } else {
            upperLastLeaf.copyUpperRight(delta);
            lowerLastLeaf.copyLowerRight(delta);
        }

        // create left x-node for new left vertex
        delta = deltas.get(0);
        if (!delta.leftVertex.equals(edgeLine.start)) {
            upperFirstLeaf.leftVertex = lowerFirstLeaf.leftVertex = edgeLine.start;

            final Trapezoid leaf = new Trapezoid();
            leaf.leftVertex = delta.leftVertex;
            leaf.rightVertex = edgeLine.start;
            leaf.bottomEdge = delta.bottomEdge;
            leaf.topEdge = delta.topEdge;

            leaf.copyUpperLeft(delta);
            leaf.copyLowerLeft(delta);

            upperFirstLeaf.upperLeft = lowerFirstLeaf.lowerLeft = leaf;
            leaf.upperRight = upperFirstLeaf;
            leaf.lowerRight = lowerFirstLeaf;

            final VertexNode node = new VertexNode(edgeLine.start, epsilon);
            node.setLeft(leaf);
            node.right = nodes[0];
            nodes[0] = node;
        } else {
            upperFirstLeaf.copyUpperLeft(delta);
            lowerFirstLeaf.copyLowerLeft(delta);
        }

        // attach nodes to root or all previous parents
        if (_tree == deltas.get(0)) {
            _tree = nodes[0];
            deltas.get(0).setIsDeleted();
        } else {
            for (int i = 0; i < nodes.length; i++) {
                delta = deltas.get(i);
                for (Node parent: delta.parents) {
                    if (parent.left == delta) {
                        assert(parent.right != delta);
                        parent.left = nodes[i];
                    } else {
                        assert(parent.right == delta);
                        parent.right = nodes[i];
                    }
                }
                delta.setIsDeleted();
            }
        }
    }

    /**
     * Shows the {@link Object#hashCode} of the specified {@link Object}, if any.
     * Used by nested private classes for debugging output.
     * 
     * @param obj the {@link Object} whose {@link Object#hashCode} to show
     * @return the {@link Object#hashCode} of {@code obj}, or "null" if {@code obj} is {@code null}
     */
    private static String showHashCode(Object obj) {
        return (obj == null ? "null" : Integer.toString(obj.hashCode()));
    }

    /**
     * Represents one of the elements of the trapezoidal map created from the {@link Subdivision} to search.
     */
    private static class Trapezoid {

        // marker for deleted trapezoid
        private static final SubdivisionEdge DELETED_EDGE = new SubdivisionEdge(-1);

        /**
         * All {@link Node} parents of the {@link Trapezoid}.
         * A fully initialized {@link Trapezoid} always has at least one valid
         * {@link Node} parent, but may have more than one.
         */
        final List<Node> parents = new ArrayList<>(2);

        /**
         * The {@link SubdivisionEdge} that forms the lower boundary of the {@link Trapezoid}, if any.
         * Set to {@code null} if the lower side of the {@link Trapezoid}
         * opens toward to the unbounded {@link SubdivisionFace}.
         */
        SubdivisionEdge bottomEdge;

        /**
         * The {@link SubdivisionEdge} that forms the upper boundary of the {@link Trapezoid}, if any.
         * Set to {@code null} if the upper side of the {@link Trapezoid}
         * opens toward to the unbounded {@link SubdivisionFace}.
         */
        SubdivisionEdge topEdge;

        /**
         * The {@link PointD} vertex that marks the left boundary of the {@link Trapezoid}, if any.
         * The {@link PointD#x} coordinate is set to {@link Double#NEGATIVE_INFINITY} if the left
         * side of the {@link Trapezoid} opens toward to the unbounded {@link SubdivisionFace}.
         */
        PointD leftVertex = new PointD(Double.NEGATIVE_INFINITY, 0);

        /**
         * The {@link PointD} vertex that marks the right boundary of the {@link Trapezoid}, if any.
         * The {@link PointD#x} coordinate is set to {@link Double#POSITIVE_INFINITY} if the right
         * side of the {@link Trapezoid} opens toward to the unbounded {@link SubdivisionFace}.
         */
        PointD rightVertex = new PointD(Double.POSITIVE_INFINITY, 0);

        /**
         * The {@link Trapezoid} to the left of the current instance that
         * shares the same {@link #bottomEdge}, if any, else {@code null}.
         */
        Trapezoid lowerLeft;

        /**
         * The {@link Trapezoid} to the right of the current instance that
         * shares the same {@link #bottomEdge}, if any, else {@code null}.
         */
        Trapezoid lowerRight;

        /**
         * The {@link Trapezoid} to the left of the current instance that
         * shares the same {@link #topEdge}, if any, else {@code null}.
         */
        Trapezoid upperLeft;

        /**
         * The {@link Trapezoid} to the right of the current instance that
         * shares the same {@link #topEdge}, if any, else {@code null}.
         */
        Trapezoid upperRight;

        /**
         * Copies the {@link #lowerLeft} link from the specified {@link Trapezoid}, if valid.
         * Also changes the {@link #lowerRight} link of a valid {@link #lowerLeft}
         * neighbor to the current instance.
         * 
         * @param trapezoid the {@link Trapezoid} whose {@link #lowerLeft} link to copy
         * @throws NullPointerException if {@code trapezoid} is {@code null}
         */
        void copyLowerLeft(Trapezoid trapezoid) {
            if (trapezoid.lowerLeft != null) {
                lowerLeft = trapezoid.lowerLeft;
                assert(lowerLeft.lowerRight == trapezoid);
                lowerLeft.lowerRight = this;
            }
        }

        /**
         * Copies the {@link #lowerRight} link from the specified {@link Trapezoid}, if valid.
         * Also changes the {@link #lowerLeft} link of a valid {@link #lowerRight}
         * neighbor to the current instance.
         * 
         * @param trapezoid the {@link Trapezoid} whose {@link #lowerRight} link to copy
         * @throws NullPointerException if {@code trapezoid} is {@code null}
         */
        void copyLowerRight(Trapezoid trapezoid) {
            if (trapezoid.lowerRight != null) {
                lowerRight = trapezoid.lowerRight;
                assert(lowerRight.lowerLeft == trapezoid);
                lowerRight.lowerLeft = this;
            }
        }

        /**
         * Copies the {@link #upperLeft} link from the specified {@link Trapezoid}, if valid.
         * Also changes the {@link #upperRight} link of a valid {@link #upperLeft}
         * neighbor to the current instance.
         * 
         * @param trapezoid the {@link Trapezoid} whose {@link #upperLeft} link to copy
         * @throws NullPointerException if {@code trapezoid} is {@code null}
         */
        void copyUpperLeft(Trapezoid trapezoid) {
            if (trapezoid.upperLeft != null) {
                upperLeft = trapezoid.upperLeft;
                assert(upperLeft.upperRight == trapezoid);
                upperLeft.upperRight = this;
            }
        }

        /**
         * Copies the {@link #upperRight} link from the specified {@link Trapezoid}, if valid.
         * Also changes the {@link #upperLeft} link of a valid {@link #upperRight}
         * neighbor to the current instance.
         * 
         * @param trapezoid the {@link Trapezoid} whose {@link #upperRight} link to copy
         * @throws NullPointerException if {@code trapezoid} is {@code null}
         */
        void copyUpperRight(Trapezoid trapezoid) {
            if (trapezoid.upperRight != null) {
                upperRight = trapezoid.upperRight;
                assert(upperRight.upperLeft == trapezoid);
                upperRight.upperLeft = this;
            }
        }

        /**
         * Gets the {@link SubdivisionFace} that contains the {@link Trapezoid}.
         * Returns {@link SubdivisionElement#NULL_FACE} if {@link #topEdge}
         * and {@link #bottomEdge} are both {@code null}.
         * 
         * @return a {@link SubdivisionElement} wrapping the {@link SubdivisionFace}
         *         that contains the {@link Trapezoid}
         */
        SubdivisionElement face() {
            if (topEdge != null)
                return new SubdivisionElement(topEdge._face);

            if (bottomEdge != null)
                return new SubdivisionElement(bottomEdge._face);

            return SubdivisionElement.NULL_FACE;
        }

        /**
         * Indicates whether the {@link Trapezoid} has been removed from the {@link SubdivisionSearch} graph.
         * {@link SubdivisionSearch#validate} checks {@link #isDeleted} to find obsolete
         * {@link Trapezoid} instances that remain erroneously linked to the {@link SubdivisionSearch}
         * graph, or to neighboring {@link Trapezoid} instances.
         * 
         * @return {@code true} if {@link #topEdge} equals a special {@link SubdivisionEdge}
         *         instance with an invalid {@link SubdivisionEdge#key} of -1, else {@code false}
         */
        boolean isDeleted() {
            return (topEdge == DELETED_EDGE);
        }

        /**
         * Marks the {@link Trapezoid} as removed from the {@link SubdivisionSearch} graph.
         * Sets {@link #isDeleted} permanently to {@code true}.
         */
        void setIsDeleted() {
            topEdge = DELETED_EDGE;
        }

        /**
         * Returns a {@link String} representation of the {@link Trapezoid}.
         * @return a {@link String} containing the associated {@link Subdivision} edges and vertices,
         *         and the {@link Object#hashCode} results for the {@link Trapezoid}, its neighbors,
         *         and its {@link #parents}, or "null" for any objects that are {@code null}
         */
        @Override
        public String toString() {

            String parentString = "null";
            if (!parents.isEmpty()) {
                final StringBuilder builder = new StringBuilder();
                for (Node parent: parents) {
                    if (builder.length() > 0) builder.append(", ");
                    builder.append(parent.hashCode());
                }
                parentString = builder.toString();
            }

            return String.format(
                    "%d Trapezoid Parents %s %n\tLeft %s %n\tRight %s %n\tTop %s %n\t" +
                    "Bottom %s %n\tLeft Upper %s, Lower %s %n\tRight Upper %s, Lower %s",
                    hashCode(), parentString, leftVertex, rightVertex,
                    Objects.toString(topEdge), Objects.toString(bottomEdge),
                    showHashCode(upperLeft), showHashCode(lowerLeft),
                    showHashCode(upperRight), showHashCode(lowerRight));
        }
    }

    /**
     * Represents one of the inner nodes of the {@link SubdivisionSearch} graph
     * that spans the trapezoidal map created from the {@link Subdivision} to search.
     * The leaf nodes of the search graph are always {@link Trapezoid} instances.
     */
    private abstract static class Node {
        /**
         * The maximum absolute difference at which coordinates should be considered equal.
         * Always equals the {@link SubdivisionSearch#epsilon} of the containing {@link SubdivisionSearch}
         * graph. Replicated here to avoid using a {@link SubdivisionSearch} pointer just for this value.
         */
        final double epsilon;

        /**
         * The {@link Node} or {@link Trapezoid} that is the left descendant of the current instance.
         * Never {@code null} for a fully initialized {@link Node}.
         * All inner nodes of a {@link SubdivisionSearch} graph have two descendants.
         */
        Object left;

        /**
         * The {@link Node} or {@link Trapezoid} that is the right descendant of the current instance.
         * Never {@code null} for a fully initialized {@link Node}.
         * All inner nodes of a {@link SubdivisionSearch} graph have two descendants.
         */
        Object right;

        /**
         * Creates a {@link Node} with the specified comparison epsilon.
         * @param epsilon the maximum absolute difference at which coordinates should be considered equal
         * @throws AssertionError if {@code epsilon} is equal to or less than zero
         */
        Node(double epsilon) {
            assert(epsilon > 0);
            this.epsilon = epsilon;
        }

        /**
         * Finds the {@link SubdivisionElement} at the specified {@link PointD}
         * coordinates within the subtree starting at the {@link Node}.
         * @param q the {@link PointD} coordinates to examine
         * @return the {@link SubdivisionElement} that coincides with {@code q}
         * @throws NullPointerException if {@code q} is {@code null}
         */
        abstract SubdivisionElement find(PointD q);

        /**
         * Finds the {@link Trapezoid} that contains the {@link LineD#start} of the
         * specified {@link LineD} edge within the subtree starting at the {@link Node}.
         * @param edge the {@link LineD} edge to examine
         * @return the {@link Trapezoid} that contains the {@link LineD#start} of {@code edge}
         * @throws NullPointerException if {@code edge} is {@code null}
         */
        abstract Trapezoid findEdge(LineD edge);

        /**
         * Sets the {@link #left} descendant to the specified {@link Trapezoid}.
         * Also adds the {@link Node} to the {@link Trapezoid#parents} of {@code trapezoid}.
         * 
         * @param trapezoid the {@link Trapezoid} that is the new {@link #left} descendant
         * @throws NullPointerException if {@code trapezoid} is {@code null}
         */
        void setLeft(Trapezoid trapezoid) {
            left = trapezoid;
            trapezoid.parents.add(this);
        }

        /**
         * Sets the {@link #right} descendant to the specified {@link Trapezoid}.
         * Also adds the {@link Node} to the {@link Trapezoid#parents} of {@code trapezoid}.
         * 
         * @param trapezoid the {@link Trapezoid} that is the new {@link #right} descendant
         * @throws NullPointerException if {@code trapezoid} is {@code null}
         */
        void setRight(Trapezoid trapezoid) {
            right = trapezoid;
            trapezoid.parents.add(this);
        }

        /**
         * Returns a {@link String} representation of the {@link Node}.
         * @return a {@link String} containing the {@link Object#hashCode} of the {@link #left}
         *         and {@link #right} descendants, or "null" for any {@code null} object
         */
        @Override
        public String toString() {
            return String.format("Left %s, Right %s",
                    showHashCode(left), showHashCode(right));
        }
    }

    /**
     * Represents a {@link Node} that divides its subtree along a {@link SubdivisionEdge}.
     * The {@link Node#left} child of an {@link EdgeNode} contains all {@link SubdivisionSearch}
     * graph nodes to the left of its {@link EdgeNode#edge}, and the {@link Node#right} child
     * contains all nodes to the right, assuming that y-coordinates increase upward.
     */
    private final static class EdgeNode extends Node {
        /**
         * The {@link SubdivisionEdge} that divides the subtree beginning at the {@link EdgeNode}.
         * Always oriented so that its {@link SubdivisionEdge#origin} is lexicographically smaller
         * than its {@link SubdivisionEdge#destination}, according to {@link PointDComparatorX}.
         */
        final SubdivisionEdge edge;

        /**
         * The {@link LineD} representation of the associated {@link #edge}.
         * Stored separately to speed up searches.
         */
        final LineD edgeLine;

        /**
         * Creates an {@link EdgeNode} with the specified {@link SubdivisionEdge}.
         * The specified {@code edge} must be oriented so that its {@link SubdivisionEdge#origin}
         * is lexicographically smaller than its {@link SubdivisionEdge#destination},
         * according to {@link PointDComparatorX}.
         * 
         * @param edge the {@link SubdivisionEdge} that divides the subtree beginning at the {@link EdgeNode}
         * @param edgeLine the {@link LineD} representation of {@code edge}
         * @param epsilon the maximum absolute difference at which coordinates should be considered equal
         * @throws AssertionError if {@code epsilon} is equal to or less than zero,
         *         or {@code edgeLine} is not the {@link LineD} representation of {@code edge},
         *         or {@code edge} is not oriented as described above
         * @throws NullPointerException if {@code edge} or {@code edgeLine} is {@code null}
         */
        EdgeNode(SubdivisionEdge edge, LineD edgeLine, double epsilon) {
            super(epsilon);

            assert(edge.toLine().equals(edgeLine));
            assert(PointDComparatorX.compareEpsilon(edgeLine.start, edgeLine.end, epsilon) < 0);

            this.edge = edge;
            this.edgeLine = edgeLine;
        }

        /**
         * Finds the {@link SubdivisionElement} at the specified {@link PointD}
         * coordinates within the subtree starting at the {@link EdgeNode}.
         * @param q the {@link PointD} coordinates to examine
         * @return the {@link SubdivisionElement} that coincides with {@code q}
         * @throws NullPointerException if {@code q} is {@code null}
         */
        @Override
        final SubdivisionElement find(PointD q) {

            final LineLocation result = edgeLine.locate(q, epsilon);
            Object obj = null;

            switch (result) {
                case START:
                    // should have been preempted by VertexNode
                    return new SubdivisionElement(edgeLine.start);

                case END:
                    // should have been preempted by VertexNode
                    return new SubdivisionElement(edgeLine.end);

                case BETWEEN:
                    return new SubdivisionElement(edge);

                case LEFT:
                case BEFORE:
                    obj = left; break;

                case RIGHT:
                case AFTER:
                    obj = right; break;
            }

            if (obj instanceof Node)
                return ((Node) obj).find(q);

            assert(obj instanceof Trapezoid);
            return ((Trapezoid) obj).face();
        }

        /**
         * Finds the {@link Trapezoid} that contains the {@link LineD#start} of the
         * specified {@link LineD} edge within the subtree starting at the {@link EdgeNode}.
         * The specified {@code edge} must be oriented so that its {@link LineD#start}
         * point is lexicographically smaller than its {@link LineD#end} point,
         * according to {@link PointDComparatorX}.
         * 
         * @param edge the {@link LineD} edge to examine
         * @return the {@link Trapezoid} that contains the {@link LineD#start} of {@code edge}
         * @throws AssertionError if {@code edge} is not oriented as described above
         * @throws NullPointerException if {@code edge} is {@code null}
         */
        @Override
        final Trapezoid findEdge(LineD edge) {
            assert(PointDComparatorX.compareEpsilon(edge.start, edge.end, epsilon) < 0);

            final LineLocation result = edgeLine.locate(edge.start);
            Object obj;
            switch (result) {
                case LEFT:  obj = left;  break;
                case RIGHT: obj = right; break;
                default:
                    /*
                     * Any Start point that lies on the current Edge must coincide with its Start
                     * point -- not its End point due to the lexicographic ordering of all edges,
                     * not a Between point since subdivision edges are non-intersecting, and not a
                     * Before or After point since that would be outside the current node’s range.
                     * 
                     * We now order the two edges by slope, with the greater slope interpreted as
                     * left/above. Straight up is maximum slope (positive infinity), horizontal to
                     * the right is slope zero, and almost straight down is minimum slope (close to
                     * negative infinity). Equal slopes are impossible, as that would imply
                     * overlapping subdivision edges.
                     */
                    assert(result == LineLocation.START);
                    if (Math.abs(edge.start.x - edge.end.x) <= epsilon)
                        obj = left;
                    else if (Math.abs(edgeLine.start.x - edgeLine.end.x) <= epsilon)
                        obj = right;
                    else
                        obj = (edge.slope() > edgeLine.slope() ? left : right);
                    break;
            }

            if (obj instanceof Node)
                return ((Node) obj).findEdge(edge);

            return (Trapezoid) obj;
        }

        /**
         * Returns a {@link String} representation of the {@link EdgeNode}.
         * @return a {@link String} containing the {@link Object#hashCode} of the
         *         {@link EdgeNode}, and the {@link Object#toString} results for the
         *         associated {@link #edgeLine} and {@link #edge} and the {@link Node} class
         */
        @Override
        public String toString() {
            return String.format("%d Y-Node %s %n\t%s %n\t%s",
                    hashCode(), edgeLine, edge, super.toString());
        }
    }

    /**
     * Represents a {@link Node} that divides its subtree across a {@link PointD} vertex.
     * The {@link Node#left} child of a {@link VertexNode} contains all {@link SubdivisionSearch}
     * graph nodes to the left of its {@link VertexNode#vertex}, and the {@link Node#right} child
     * contains all nodes to the right.
     */
    private final static class VertexNode extends Node {
        /**
         * The {@link PointD} vertex that divides the subtree beginning at the {@link VertexNode}.
         */
        final PointD vertex;

        /**
         * Creates a {@link VertexNode} with the specified {@link PointD} vertex.
         * @param vertex the {@link PointD} vertex that divides the subtree beginning at the {@link VertexNode}
         * @param epsilon the maximum absolute difference at which coordinates should be considered equal
         * @throws AssertionError if {@code epsilon} is equal to or less than zero
         * @throws NullPointerException if {@code vertex} is {@code null}
         */
        VertexNode(PointD vertex, double epsilon) {
            super(epsilon);
            this.vertex = vertex;
        }

        /**
         * Finds the {@link SubdivisionElement} at the specified {@link PointD}
         * coordinates within the subtree starting at the {@link VertexNode}.
         * @param q the {@link PointD} coordinates to examine
         * @return the {@link SubdivisionElement} that coincides with {@code q}
         * @throws NullPointerException if {@code q} is {@code null}
         */
        @Override
        final SubdivisionElement find(PointD q) {

            final int result = PointDComparatorX.compareEpsilon(q, vertex, epsilon);
            if (result == 0) return new SubdivisionElement(vertex);

            final Object obj = (result < 0 ? left : right);
            if (obj instanceof Node)
                return ((Node) obj).find(q);

            return ((Trapezoid) obj).face();
        }

        /**
         * Finds the {@link Trapezoid} that contains the {@link LineD#start} of the
         * specified {@link LineD} edge within the subtree starting at the {@link VertexNode}.
         * @param edge the {@link LineD} edge to examine
         * @return the {@link Trapezoid} that contains the {@link LineD#start} of {@code edge}
         * @throws NullPointerException if {@code edge} is {@code null}
         */
        @Override
        final Trapezoid findEdge(LineD edge) {

            final int result = PointDComparatorX.compareEpsilon(edge.start, vertex, epsilon);
            final Object obj = (result < 0 ? left : right);

            if (obj instanceof Node)
                return ((Node) obj).findEdge(edge);

            return (Trapezoid) obj;
        }

        /**
         * Returns a {@link String} representation of the {@link VertexNode}.
         * @return a {@link String} containing the the {@link Object#hashCode}
         *         of the {@link VertexNode}, and the {@link Object#toString} results
         *         for the associated {@link #vertex} and the {@link Node} class
         */
        @Override
        public String toString() {
            return String.format("%d X-Node %s %n\t%s",
                    hashCode(), vertex, super.toString());
        }
    }
}

package org.kynosarges.tektosyne.geometry;

import java.util.*;

import org.kynosarges.tektosyne.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Contains the results of the {@link Voronoi} algorithm.
 * Holds the Voronoi diagram and Delaunay triangulation found by
 * the {@link Voronoi} algorithm when both results were requested.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class VoronoiResults {
    /*
     * The four corners of {@link #clippingBounds} appear as {@link #voronoiRegions}
     * vertices but do not have corresponding indices in {@link #voronoiVertices}.
     * The following constants therefore provide four negative pseudo-indices
     * for these four vertices that are accepted by {@link #getVertex}.
     */
    
    /** Indicates the point {@code (min.x, min.y)} in {@link #clippingBounds}. */
    private final static int MINX_MINY = -1;

    /** Indicates the point {@code (max.x, min.y)} in {@link #clippingBounds}. */
    private final static int MAXX_MINY = -2;

    /** Indicates the point {@code (min.x, max.y)} in {@link #clippingBounds}. */
    private final static int MINX_MAXY = -3;

    /** Indicates the point {@code (max.x, max.y)} in {@link #clippingBounds}. */
    private final static int MAXX_MAXY = -4;

    /**
     * The clipping bounds for the entire Voronoi diagram.
     * Contains the {@link RectD} that was actually used by the {@link Voronoi} algorithm,
     * which may be larger than that originally supplied to {@link Voronoi#findAll}.
     * <p>
     * All Voronoi edges are terminated with a pseudo-vertex in {@link #voronoiVertices}
     * when they intersect the {@link #clippingBounds}. No {@link #voronoiVertices} lie
     * outside the {@link #clippingBounds}.</p>
     * <p>
     * Moreover, the four corners of the {@link #clippingBounds} are always part of some
     * {@link #voronoiRegions} that were originally unbounded. {@link #voronoiVertices}
     * does not usually contain these corner vertices.</p>
     */
    public final RectD clippingBounds;

    /**
     * The generator sites for the Voronoi diagram and Delaunay triangulation.
     * Holds the {@link PointD} coordinates whose Voronoi diagram and Delaunay triangulation
     * are provided by the {@link VoronoiResults}. This is the original {@link PointD}
     * array that was supplied to the {@link Voronoi} algorithm and resulted in the
     * {@link VoronoiResults}. This field is provided merely for convenience.
     */
    public final PointD[] generatorSites;

    /**
     * The edge list for the Voronoi diagram.
     * Holds all edges in the Voronoi diagram, stored as double index pairs relative to the
     * {@link #generatorSites} and {@link #voronoiVertices} arrays.
     * <p>
     * The complete Voronoi diagram is defined both by the {@link #voronoiVertices} and by
     * the {@link #voronoiEdges} that connect the vertices. All coordinates are bounded by
     * the current {@link #clippingBounds}.</p>
     */
    public final VoronoiEdge[] voronoiEdges;

    /**
     * The vertex list for the Voronoi diagram.
     * Holds the {@link PointD} coordinates of all vertices in the Voronoi diagram.
     * <p>
     * The complete Voronoi diagram is defined both by the {@link #voronoiVertices} and by
     * the {@link #voronoiEdges} that connect the vertices. All coordinates are bounded by
     * the current {@link #clippingBounds}.</p>
     */
    public final PointD[] voronoiVertices;

    private PointD[][] _voronoiRegions;

    /**
     * Creates {@link VoronoiResults} with the specified input and output data.
     * @param clippingBounds the clipping bounds for the entire Voronoi diagram
     * @param generatorSites the generator sites for the Voronoi diagram and Delaunay triangulation
     * @param voronoiVertices the vertex list for the Voronoi diagram
     * @param voronoiEdges the edge list for the Voronoi diagram
     * @throws NullPointerException if any argument is {@code null}
     */
    VoronoiResults(RectD clippingBounds, PointD[] generatorSites,
        PointD[] voronoiVertices, VoronoiEdge[] voronoiEdges) {

        if (clippingBounds == null)
            throw new NullPointerException("clippingBounds");
        if (generatorSites == null)
            throw new NullPointerException("generatorSites");
        if (voronoiVertices == null)
            throw new NullPointerException("voronoiVertices");
        if (voronoiEdges == null)
            throw new NullPointerException("voronoiEdges");
        
        this.clippingBounds = clippingBounds;
        this.generatorSites = generatorSites;
        this.voronoiVertices = voronoiVertices;
        this.voronoiEdges = voronoiEdges;
    }

    /**
     * Gets the edge list for the Delaunay triangulation.
     * Returns an array of the same size as {@link #voronoiEdges}. Each {@link LineD} element
     * connects the {@link #generatorSites} indicated by {@link VoronoiEdge#site1} and
     * {@link VoronoiEdge#site2} of the {@link VoronoiEdge} at the same index position.
    * 
     * @return an array of all {@link LineD} edges in the Delaunay triangulation
     */
    public LineD[] delaunayEdges() {
        final LineD[] edges = new LineD[voronoiEdges.length];

        for (int i = 0; i < voronoiEdges.length; i++)
            edges[i] = new LineD(
                    generatorSites[voronoiEdges[i].site1],
                    generatorSites[voronoiEdges[i].site2]);

        return edges;
    }

    /**
     * Gets the regions of the Voronoi diagram.
     * Calculated on first success, then cached for repeated access. All coordinates
     * are bounded by the current {@link #clippingBounds}.
     * <p>
     * The {@link PointD} array for each generator site contains the vertices of a convex
     * polygon. The last vertex is implicitly assumed to be connected with the first vertex.
     * Most vertices also appear in {@link #voronoiVertices}, except for the four corners
     * of the {@link #clippingBounds} which terminate the outermost regions.</p>
     * 
     * @return an array containing {@link PointD} polygons that represent the Voronoi
     *         regions corresponding to the same {@link #generatorSites} indices
     */
    public PointD[][] voronoiRegions() {
        if (_voronoiRegions == null)
            createRegions();

        return _voronoiRegions;
    }

    /**
     * Clears the {@link #voronoiRegions} cache.
     * This will cause {@link #voronoiRegions} to be recalculated (with identical results)
     * when requested again. Call this method to reduce memory consumption when the cached
     * {@link #voronoiRegions} are no longer required, e.g. after creating {@link VoronoiMap}.
     */
    public void clearVoronoiRegions() {
        _voronoiRegions = null;
    }

    /**
     * Clips the edge list for the Delaunay triangulation to the specified bounds.
     * Returns all {@link #delaunayEdges} whose corresponding {@link #voronoiEdges}
     * element fulfils two conditions:
     * <ul>
     * <li>{@link VoronoiEdge#site1} and {@link VoronoiEdge#site2} both fall within
     * the specified {@code bounds}.</li>
     * <li>The line segment indicated by {@link VoronoiEdge#vertex1} and {@link
     * VoronoiEdge#vertex2} intersects the specified {@code bounds}.</li>
     * </ul>
     * In other words, {@link #clipDelaunayEdges} selects those {@link #delaunayEdges} that
     * fall entirely within {@code bounds}, and which connect two {@link #voronoiRegions}
     * that share a common border within {@code bounds}.
     * 
     * @param bounds a {@link RectD} indicating the clipping bounds for all {@link #delaunayEdges}
     * @return an array of all {@link #delaunayEdges} which intersect {@code bounds}, as defined above
     * @throws NullPointerException if {@code bounds} is {@code null}
     */
    public LineD[] clipDelaunayEdges(RectD bounds) {
        final List<LineD> delaunayEdges = new ArrayList<>(voronoiEdges.length);

        for (VoronoiEdge edge: voronoiEdges) {
            final PointD s1 = generatorSites[edge.site1];
            final PointD s2 = generatorSites[edge.site2];

            if (bounds.contains(s1) && bounds.contains(s2)) {
                final PointD v1 = voronoiVertices[edge.vertex1];
                final PointD v2 = voronoiVertices[edge.vertex2];

                if (bounds.intersectsWith(new LineD(v1, v2)))
                    delaunayEdges.add(new LineD(s1, s2));
            }
        }

        return delaunayEdges.toArray(new LineD[delaunayEdges.size()]);
    }

    /**
     * Converts all {@link #delaunayEdges} to a planar {@link Subdivision},
     * using the default {@link #clippingBounds}.
     * 
     * @param addRegions {@code true} to add all {@link #voronoiRegions} with the corresponding
     *                   {@link #generatorSites} to the {@link Subdivision#vertexRegions}
     *                   of the new {@link Subdivision}, else {@code false}
     * @return a new {@link Subdivision} whose {@link Subdivision#edges} correspond to the
     *         {@link #delaunayEdges} of the {@link VoronoiResults}
     */
    public Subdivision toDelaunaySubdivision(boolean addRegions) {
        final Subdivision division = Subdivision.fromLines(delaunayEdges(), 0);

        if (addRegions)
            for (int i = 0; i < generatorSites.length; i++)
                division.vertexRegions().put(generatorSites[i], voronoiRegions()[i]);

        return division;
    }

    /**
     * Converts all {@link #delaunayEdges} to a planar {@link Subdivision},
     * using the specified clipping bounds.
     * The specified {@code bounds} determine the subset of {@link #generatorSites}
     * and {@link #delaunayEdges} that is stored in the new {@link Subdivision},
     * as described in {@link #clipDelaunayEdges}.
     * <p>
     * If {@code addRegions} is {@code true}, the polygons added to
     * {@link Subdivision#vertexRegions} are also clipped the specified {@code bounds}.</p>
     * 
     * @param bounds a {@link RectD} indicating the clipping bounds for all {@link #delaunayEdges}
     * @param addRegions {@code true} to add all {@link #voronoiRegions} with the corresponding
     *                   {@link #generatorSites} to the {@link Subdivision#vertexRegions}
     *                   of the new {@link Subdivision}, else {@code false}
     * @return a new {@link Subdivision} whose {@link Subdivision#edges} correspond to the
     *         {@link #delaunayEdges} of the {@link VoronoiResults}
     * @throws NullPointerException if {@code bounds} is {@code null}
     */
    public Subdivision toDelaunaySubdivision(RectD bounds, boolean addRegions) {

        final LineD[] edges = clipDelaunayEdges(bounds);
        final Subdivision division = Subdivision.fromLines(edges, 0);

        if (addRegions)
            for (int i = 0; i < generatorSites.length; i++) {
                final PointD site = generatorSites[i];
                if (!bounds.contains(site)) continue;

                final PointD[] region = bounds.intersect(voronoiRegions()[i]);
                if (region != null)
                    division.vertexRegions().put(site, region);
            }

        return division;
    }

    /**
     * Closes the specified Voronoi region by adding pseudo-edges across one or two
     * {@link #clippingBounds} corner between the specified Voronoi vertices.
     * Adds one or two {@link PointI} pseudo-edges beginning with negative pseudo-indices to
     * the specified {@code region}. {@code v1} and {@code v2} must differ in both coordinates.
     * 
     * @param region a {@link NodeList} containing the known Voronoi vertex indices
     *               of the Voronoi region to close.
     * @param v1 the {@link PointD} coordinates of the first known vertex in {@code region}
     * @param v2 the {@link PointD} coordinates of the last known vertex {@code region}
     * @throws NullPointerException if any argument is {@code null}
     */
    private void closeCornerRegion(NodeList<PointI> region, PointD v1, PointD v2) {
        assert(v1.x != v2.x && v1.y != v2.y);

        if (v2.x == clippingBounds.min.x || v2.x == clippingBounds.max.x) {

            if (v1.y == clippingBounds.min.y || v1.y == clippingBounds.max.y) {
                // p is on adjacent border, spanning one corner
                final PointD corner = new PointD(v2.x, v1.y);
                region.addLast(createCornerEdge(corner));
            } else {
                // p is on opposite vertical border, spanning two corners
                // region opens to top or bottom clipping border
                final PointD corner = new PointD(v2.x, findHorizontalBorder(v1, v2));
                final PointD secondCorner = new PointD(v1.x, corner.y);

                region.addLast(createCornerEdge(corner));
                region.addLast(createCornerEdge(secondCorner));
            }
        } else {
            assert(v2.y == clippingBounds.min.y || v2.y == clippingBounds.max.y);

            if (v1.x == clippingBounds.min.x || v1.x == clippingBounds.max.x) {
                // p is on adjacent border, spanning one corner
                final PointD corner = new PointD(v1.x, v2.y);
                region.addLast(createCornerEdge(corner));
            } else {
                // p is on opposite horizontal border, spanning two corners
                // region opens to left or right clipping border
                final PointD corner = new PointD(findVerticalBorder(v1, v2), v2.y);
                final PointD secondCorner = new PointD(corner.x, v1.y);

                region.addLast(createCornerEdge(corner));
                region.addLast(createCornerEdge(secondCorner));
            }
        }
    }

    /**
     * Connects the specified candidate edges to the specified Voronoi region by inserting
     * edges that span a border or corner of the {@link #clippingBounds}.
     * Adds one or two edges at the beginning of the specified {@code candidates} list.
     * If two edges are added, both will contain one pseudo-vertex with a negative index.
     * 
     * @param candidates a {@link NodeList} containing the candidate edges that
     *                   must be connected to the specified {@code region}
     * @param region a {@link NodeList} containing the sorted and connected edges of the Voronoi
     *               region, with the first and last vertex touching {@link #clippingBounds}
     * @throws NullPointerException if any argument is {@code null}
     */
    private void connectCandidates(NodeList<PointI> candidates, NodeList<PointI> region) {

        final int firstIndex = region.first().value().x;
        final int lastIndex = region.last().value().y;

        final PointD firstVertex = getVertex(firstIndex);
        final PointD lastVertex = getVertex(lastIndex);

        assert(isAtClippingBounds(firstVertex));
        assert(isAtClippingBounds(lastVertex));

        // outer vertex indices of connecting edge(s)
        int connect1 = -1, connect2 = -1;

        for (PointI candidate: candidates) {
            connect1 = candidate.x;
            PointD connectVertex = getVertex(connect1);

            if (meetAtClippingBounds(connectVertex, firstVertex)) {
                connect2 = firstIndex; break;
            }
            if (meetAtClippingBounds(connectVertex, lastVertex)) {
                connect2 = lastIndex; break;
            }

            connect1 = candidate.y;
            connectVertex = getVertex(connect1);

            if (meetAtClippingBounds(connectVertex, firstVertex)) {
                connect2 = firstIndex; break;
            }
            if (meetAtClippingBounds(connectVertex, lastVertex)) {
                connect2 = lastIndex; break;
            }
        }

        if (connect2 >= 0) {
            // add connecting edge on same clipping border
            candidates.addFirst(new PointI(connect1, connect2));
            return;
        }

        connect1 = -1; connect2 = -1;
        int corner = 0;

        for (PointI candidate: candidates) {
            connect1 = candidate.x;
            PointD connectVertex = getVertex(connect1);

            corner = getCornerIndex(connectVertex, firstVertex);
            if (corner != 0) {
                connect2 = firstIndex; break;
            }
            corner = getCornerIndex(connectVertex, lastVertex);
            if (corner != 0) {
                connect2 = lastIndex; break;
            }

            connect1 = candidate.y;
            connectVertex = getVertex(connect1);

            corner = getCornerIndex(connectVertex, firstVertex);
            if (corner != 0) {
                connect2 = firstIndex; break;
            }
            corner = getCornerIndex(connectVertex, lastVertex);
            if (corner != 0) {
                connect2 = lastIndex; break;
            }
        }

        // add connecting edges across clipping corner
        assert(connect2 >= 0);
        candidates.addFirst(new PointI(corner, connect2));
        candidates.addFirst(new PointI(connect1, corner));
    }

    /**
     * Creates a pseudo-edge that begins with a negative pseudo-index corresponding
     * to the specified {@link PointD} coordinates.
     * The {@link PointI#y} component of the returned pseudo-edge is deliberately invalid
     * because {@link #createCornerEdge} is only used in the last stage of {@link #createRegions}
     * when {@link PointI#y} components are ignored.
     * 
     * @param p a {@link PointD} that coincides with one of the four {@link #clippingBounds} corners
     * @return a {@link PointI} whose {@link PointI#x} component equals the result of {@link #getCornerIndex}
     *         for {@code p}, and whose {@link PointI#y} component equals {@link Integer#MIN_VALUE}
     * @throws NullPointerException if {@code p} is {@code null}
     */
    private PointI createCornerEdge(PointD p) {
        final int index = getCornerIndex(p);
        assert(index != 0);
        return new PointI(index, Integer.MIN_VALUE);
    }

    /**
     * Creates the regions of the Voronoi diagram.
     * See {@link #voronoiRegions} for details.
     */
    private void createRegions() {
        /*
         * 1. Create list of unsorted edges for each region
         * ================================================
         * First we accumulate the raw material for each Voronoi region.
         * All edges are stored as indices into VoronoiVertices here.
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final NodeList<PointI>[] listRegions = new NodeList[generatorSites.length];
        for (int i = 0; i < listRegions.length; i++)
            listRegions[i] = new NodeList<>();

        for (VoronoiEdge edge: voronoiEdges) {
            final PointI vertex = new PointI(edge.vertex1, edge.vertex2);
            listRegions[edge.site1].addLast(vertex);
            listRegions[edge.site2].addLast(vertex);
        }

        /*
         * 2. Sort and complete list of edges for each region
         * ==================================================
         * Sort each edge list so that an edge’s second vertex index equals
         * the first vertex index of the subsequent edge in the region list.
         * We may need to swap an edge’s vertex indices to allow this connection.
         * 
         * Because all Voronoi edges are terminated with a pseudo-vertex where they
         * intersect the clipping rectangle, a Voronoi region may appear as several
         * internally connected, but mutually disconnected series of edges. To get
         * a single connected list, we must then insert pseudo-edges that span either
         * a border or even a corner of the clipping rectangle.
         */
        for (int i = 0; i < listRegions.length; i++) {
            final NodeList<PointI> candidates = listRegions[i];
            final NodeList<PointI> listRegion = new NodeList<>();

            // start with first unsorted edge
            listRegion.addFirst(candidates.first().value());
            candidates.removeFirst();

            NodeList.Node<PointI> candidate = candidates.first();
            boolean wasEdgeAdded = false;

            while (candidate != null) {
                // save next candidate before removing current one
                final NodeList.Node<PointI> nextCandidate = candidate.next();

                PointI vertex = candidate.value();
                for (NodeList.Node<PointI> node = listRegion.first();
                        node != null; node = node.next()) {

                    // all vertices are distinct
                    assert(!vertex.equals(node.value()));

                    // invert edges with out-of-order indices
                    if (vertex.x == node.value().x || vertex.y == node.value().y)
                        vertex = new PointI(vertex.y, vertex.x);

                    // move preceding edge to sorted list
                    if (vertex.y == node.value().x) {
                        candidates.remove(candidate);
                        listRegion.addBefore(node, vertex);
                        wasEdgeAdded = true;
                        break;
                    }

                    // move succeeding edge to sorted list
                    if (node.value().y == vertex.x) {
                        candidates.remove(candidate);
                        listRegion.addAfter(node, vertex);
                        wasEdgeAdded = true;
                        break;
                    }
                }

                // try next unsorted edge
                candidate = nextCandidate;
                if (candidate == null && !candidates.isEmpty()) {

                    // connection across border or corner required
                    if (!wasEdgeAdded)
                        connectCandidates(candidates, listRegion);

                    // start over with first candidate node
                    candidate = candidates.first();
                    wasEdgeAdded = false;
                }
            }

            // replace unsorted with sorted list
            listRegions[i] = listRegion;
        }

        /*
         * 3. Transform index list into polygon for each region
         * ====================================================
         * We now have sorted lists containing all edges of each Voronoi region.
         * For closed (interior) regions, the last edge connects to the first,
         * and we store exactly one vertex per edge (we always choose the first).
         * 
         * For open (exterior) regions, the first edge begins and the last edge ends
         * with two different pseudo-vertices. We must now close them in the same way
         * in which we connected separate sublists in step 2.
         * 
         * That is, we add one or more pseudo-edges that connect the outer vertices
         * of the list across a border or corner of the clipping rectangle. Unlike
         * step 2, we may need to extend the connection across two corners.
         */
        _voronoiRegions = new PointD[generatorSites.length][];

        for (int i = 0; i < listRegions.length; i++) {
            final NodeList<PointI> listRegion = listRegions[i];

            final int firstIndex = listRegion.first().value().x;
            final int lastIndex = listRegion.last().value().y;

            if (firstIndex != lastIndex) {
                // extend region to last pseudo-vertex
                listRegion.addLast(new PointI(lastIndex, Integer.MIN_VALUE));

                final PointD firstVertex = getVertex(firstIndex);
                final PointD lastVertex = getVertex(lastIndex);

                // check if pseudo-vertices span one or two corners of clipping region
                if (firstVertex.x != lastVertex.x && firstVertex.y != lastVertex.y)
                    closeCornerRegion(listRegion, firstVertex, lastVertex);
            }

            final PointD[] region = new PointD[listRegion.size()];
            _voronoiRegions[i] = region;

            // store coordinates for first vertex of each edge
            int j = 0;
            for (PointI edge: listRegion)
                region[j++] = getVertex(edge.x);
        }
    }

    /**
     * Finds the horizontal border of the {@link #clippingBounds} towards which the
     * Voronoi region containing the specified vertical border coordinates is open.
     * The specified {@code p} and {@code q} must lie on opposite vertical borders of the
     * {@link #clippingBounds}, and their Voronoi region must open to one of the horizontal
     * borders, including both adjacent corners.
     * 
     * @param p the first {@link PointD} to examine
     * @param q the second {@link PointD} to examine
     * @return the minimum or maximum vertical coordinate within {@link #clippingBounds},
     *         depending on the specified {@code p} and {@code q}
     * @throws NullPointerException if {@code p} or {@code q} is {@code null}
     */
    private double findHorizontalBorder(PointD p, PointD q) {

        // line from left to right of clipping bounds
        final LineD line = ((p.x < q.x) ? new LineD(p, q) : new LineD(q, p));
        assert(line.start.x == clippingBounds.min.x);
        assert(line.end.x == clippingBounds.max.x);

        // check for vertex on either side of line
        for (PointD vertex: voronoiVertices)
            switch (line.locate(vertex)) {
                case LEFT:  return clippingBounds.min.y;
                case RIGHT: return clippingBounds.max.y;
            }

        assert false : "Cannot identify open side of Voronoi region.";
        return clippingBounds.min.y;
    }

    /**
     * Finds the vertical border of the {@link #clippingBounds} towards which the
     * Voronoi region containing the specified horizontal border coordinates is open.
     * The specified {@code p} and {@code q} must lie on opposite horizontal borders of the
     * {@link #clippingBounds}, and their Voronoi region must open to one of the vertical
     * borders, including both adjacent corners.
     * 
     * @param p the first {@link PointD} to examine
     * @param q the second {@link PointD} to examine
     * @return the minimum or maximum horizontal coordinate within {@link #clippingBounds},
     *         depending on the specified {@code p} and {@code q}
     * @throws NullPointerException if {@code p} or {@code q} is {@code null}
     */
    private double findVerticalBorder(PointD p, PointD q) {

        // line from top to bottom of clipping bounds
        final LineD line = ((p.y < q.y) ? new LineD(p, q) : new LineD(q, p));
        assert(line.start.y == clippingBounds.min.y);
        assert(line.end.y == clippingBounds.max.y);

        // check for vertex on either side of line
        for (PointD vertex: voronoiVertices)
            switch (line.locate(vertex)) {
                case LEFT:  return clippingBounds.max.x;
                case RIGHT: return clippingBounds.min.x;
            }

        assert false : "Cannot identify open side of Voronoi region.";
        return clippingBounds.min.x;
    }

    /**
     * Gets the pseudo-index of the {@link #clippingBounds} corner
     * that equals the specified {@link PointD} coordinates, if any.
     * 
     * @param p the {@link PointD} to examine
     * @return the negative pseudo-index of the {@link #clippingBounds} corner that equals
     *         {@code p}, or zero if {@code p} does not equal any {@link #clippingBounds} corner
     * @throws NullPointerException if {@code p} is {@code null}
     */
    private int getCornerIndex(PointD p) {

        if (p.x == clippingBounds.min.x) {
            if (p.y == clippingBounds.min.y) return MINX_MINY;
            if (p.y == clippingBounds.max.y) return MINX_MAXY;
        }
        else if (p.x == clippingBounds.max.x) {
            if (p.y == clippingBounds.min.y) return MAXX_MINY;
            if (p.y == clippingBounds.max.y) return MAXX_MAXY;
        }

        return 0;
    }

    /**
     * Gets the pseudo-index of the {@link #clippingBounds} corner
     * that separates the specified {@link PointD} coordinates, if any.
     * 
     * @param p the first {@link PointD} to examine
     * @param q the second {@link PointD} to examine
     * @return the negative pseudo-index of the {@link #clippingBounds} corner that
     *         separates {@code p} and {@code q}, or zero if {@code p} and {@code q}
     *         do not lie on adjacent {@link #clippingBounds} borders
     * @throws NullPointerException if {@code p} or {@code q} is {@code null}
     */
    private int getCornerIndex(PointD p, PointD q) {

        if (p.x == clippingBounds.min.x) {
            if (q.y == clippingBounds.min.y) return MINX_MINY;
            if (q.y == clippingBounds.max.y) return MINX_MAXY;
        }
        else if (p.x == clippingBounds.max.x) {
            if (q.y == clippingBounds.min.y) return MAXX_MINY;
            if (q.y == clippingBounds.max.y) return MAXX_MAXY;
        }
        else if (p.y == clippingBounds.min.y) {
            if (q.x == clippingBounds.min.x) return MINX_MINY;
            if (q.x == clippingBounds.max.x) return MAXX_MINY;
        }
        else if (p.y == clippingBounds.max.y) {
            if (q.x == clippingBounds.min.x) return MINX_MAXY;
            if (q.x == clippingBounds.max.x) return MAXX_MAXY;
        }

        return 0;
    }

    /**
     * Gets the Voronoi vertex with the specified index.
     * The specified {@code index} must be either a valid zero-based {@link #voronoiVertices}
     * index or a negative {@link #clippingBounds} pseudo-index.
     * 
     * @param index the index of the Voronoi vertex to retrieve
     * @return one of the four {@link #clippingBounds} corners if {@code index} equals a
     *         negative pseudo-index, else the {@link #voronoiVertices} element at {@code index}
     * @throws ArrayIndexOutOfBoundsException if {@code index} is neither a
     *         {@link #clippingBounds} pseudo-index nor a {@link #voronoiVertices} index
     */
    private PointD getVertex(int index) {
        switch (index) {

            case MINX_MINY: return clippingBounds.min;
            case MAXX_MINY: return new PointD(clippingBounds.max.x, clippingBounds.min.y);
            case MINX_MAXY: return new PointD(clippingBounds.min.x, clippingBounds.max.y);
            case MAXX_MAXY: return clippingBounds.max;

            default: return voronoiVertices[index];
        }
    }

    /**
     * Determines whether the specified {@link PointD} coordinates
     * touch the {@link #clippingBounds}.
     * 
     * @param p the {@link PointD} to examine
     * @return {@code true} if the x- or y-coordinate of {@code p} equal the corresponding
     *         coordinate of {@link RectD#min} or {@link Rect#max} of {@link #clippingBounds},
     *         else {@code false}
     * @throws NullPointerException if {@code p} is {@code null}
     */
    private boolean isAtClippingBounds(PointD p) {
        return (p.x == clippingBounds.min.x ||
                p.x == clippingBounds.max.x ||
                p.y == clippingBounds.min.y ||
                p.y == clippingBounds.max.y);
    }

    /**
     * Determines whether the specified {@link PointD} coordinates
     * lie on the same border of the {@link #clippingBounds}.
     * 
     * @param p the first {@link PointD} to examine
     * @param q the second {@link PointD} to examine
     * @return {@code true} if either the x- or y-coordinates of {@code p} and {@code q}
     *         equal each other as well as the corresponding coordinate of {@link RectD#min}
     *         or {@link Rect#max} of {@link #clippingBounds}, else {@code false}
     * @throws NullPointerException if {@code p} or {@code q} is {@code null}
     */
    private boolean meetAtClippingBounds(PointD p, PointD q) {
        return ((p.x == q.x &&
                (p.x == clippingBounds.min.x || p.x == clippingBounds.max.x)) ||
                (p.y == q.y &&
                (p.y == clippingBounds.min.y || p.y == clippingBounds.max.y)));
    }
}

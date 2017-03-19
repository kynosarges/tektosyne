package org.kynosarges.tektosyne.geometry;

import java.util.*;
import org.kynosarges.tektosyne.subdivision.Subdivision;

/**
 * Provides a sweep line algorithm for Voronoi diagrams and Delaunay triangulations.
 * Call {@link Voronoi#findAll} to find both the Voronoi diagram and the Delaunay triangulation for a
 * given {@link PointD} set, or {@link Voronoi#findDelaunay} to find only the Delaunay triangulation.
 * <p>
 * Since the outgoing edges of a Voronoi diagram continue indefinitely, {@link Voronoi} employs
 * a clipping rectangle slightly larger than the bounding box of the specified point set. Voronoi
 * edges that cross the clipping rectangle are terminated with a pseudo-vertex at the point of
 * intersection. True Voronoi vertices beyond the clipping rectangle are not found. You may specify
 * a larger clipping rectangle if desired.</p>
 * <p>
 * {@link Voronoi} performs Fortune’s sweep line algorithm with an asymptotic runtime of O(n log n).
 * This algorithm was first published in Steven J. Fortune, <em>A Sweepline Algorithm for Voronoi
 * Diagrams,</em> Algorithmica 2 (1987), p.153-174. This Java implementation was adapted from
 * Fortune’s own C implementation, which used to be available as <code>sweep2.gz</code> at the
 * archive page <code>netlib/voronoi</code> of Sandia National Laboratories. Unfortunately the <a
 * href="http://netlib.sandia.gov/voronoi/index.html">original URL</a> has since become invalid.
 * The following copyright statement is reproduced from the original C program, as required by
 * the copyright conditions.</p>
 * <p>
 * The author of this software is Steven Fortune.  Copyright (c) 1994 by AT&amp;T Bell Laboratories.</p>
 * <p>
 * Permission to use, copy, modify, and distribute this software for any purpose without fee is
 * hereby granted, provided that this entire notice is included in all copies of any software
 * which is or includes a copy or modification of this software and in all copies of the
 * supporting documentation for such software.</p>
 * <p>
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED WARRANTY.  IN
 * PARTICULAR, NEITHER THE AUTHORS NOR AT&amp;T MAKE ANY REPRESENTATION OR WARRANTY OF ANY KIND
 * CONCERNING THE MERCHANTABILITY OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.</p>
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class Voronoi {
    /**
     * Finds the Voronoi diagram and the Delaunay triangulation for the specified set
     * of {@link PointD} coordinates, using a default clipping rectangle.
     * 
     * @param points an array containing the {@link PointD} coordinates whose
     *               Voronoi diagram and Delaunay triangulation to find
     * @return a {@link VoronoiResults} instance containing the Voronoi diagram
     *         and Delaunay triangulation for the specified {@code points}
     * @throws IllegalArgumentException if {@code points} contains less than three elements
     * @throws NullPointerException if {@code points} or any of its elements is {@code null}
     */
    public static VoronoiResults findAll(PointD[] points) {
        return findAll(points, RectD.EMPTY);
    }

    /**
     * Finds the Voronoi diagram and the Delaunay triangulation for the specified set
     * of {@link PointD} coordinates, using the specified clipping rectangle.
     * The actual clipping rectangle always extends somewhat beyond the bounding rectangle
     * of the specified {@code points}. The specified {@code clip} rectangle can only further
     * extend this area, not restrict it.
     * 
     * @param points an array containing the {@link PointD} coordinates whose
     *               Voronoi diagram and Delaunay triangulation to find
     * @param clip a {@link RectD} indicating the clipping bounds for pseudo-vertices
     * @return a {@link VoronoiResults} instance containing the Voronoi diagram
     *         and Delaunay triangulation for the specified {@code points}
     * @throws IllegalArgumentException if {@code points} contains less than three elements
     * @throws NullPointerException if {@code points} or {@code clip} is {@code null},
     *                              or any {@code points} element is {@code null}
     */
    public static VoronoiResults findAll(PointD[] points, RectD clip) {

        final RectD[] clipRef = new RectD[] { clip };
        final Voronoi voronoi = new Voronoi(points, clipRef, false);
        voronoi.sweepLine();

        final List<PointD> vertices = voronoi._voronoiVertices;
        final List<VoronoiEdge> edges = voronoi._voronoiEdges;

        return new VoronoiResults(clipRef[0], points,
            vertices.toArray(new PointD[vertices.size()]),
            edges.toArray(new VoronoiEdge[edges.size()]));
    }

    /**
     * Finds the Delaunay triangulation for the specified set of {@link PointD} coordinates.
     * The {@link PointI#x} and {@link PointI#y} components of each {@link PointI} element
     * in the returned array hold the indices of any two {@code points} elements that are
     * connected by an edge in the Delaunay triangulation.
     * <p>
     * Use {@link LineD#fromIndexPoints} to combine {@code points} and the returned index array
     * into a {@link LineD} array representing the edges of the Delaunay triangulation.</p>
     * 
     * @param points an array containing the {@link PointD} coordinates whose Delaunay triangulation to find
     * @return an array containing all edges of the Delaunay triangulation,
     *         stored as index pairs relative to the specified {@code points}
     * @throws IllegalArgumentException if {@code points} contains less than three elements
     * @throws NullPointerException if {@code points} or any of its elements is {@code null}
     */
    public static PointI[] findDelaunay(PointD[] points) {

        final RectD[] clipRef = new RectD[] { RectD.EMPTY };
        final Voronoi voronoi = new Voronoi(points, clipRef, true);
        voronoi.sweepLine();

        final List<PointI> edges = voronoi._delaunayEdges;
        return edges.toArray(new PointI[edges.size()]);
    }

    /**
     * Finds the Delaunay triangulation for the specified set of {@link PointD} coordinates
     * and creates the corresponding {@link Subdivision}.
     * Convenience method that first calls {@link #findDelaunay} on the specified
     * {@code points}, then {@link LineD#fromIndexPoints} on the resulting indices, and
     * finally {@link Subdivision#fromLines} with an epsilon of zero on the resulting lines.
     * <p>
     * The {@link Subdivision} does not include a mapping of vertices to Voronoi regions.
     * Call {@link #findAll} and then {@link VoronoiResults#toDelaunaySubdivision} to
     * obtain this mapping, and also to apply an optional clipping rectangle.</p>
     * 
     * @param points an array containing the {@link PointD} coordinates whose Delaunay triangulation to find
     * @return a {@link Subdivision} whose {@link Subdivision#edges} correspond
     *         to the  edges of the Delaunay triangulation for {@code points}
     * @throws IllegalArgumentException if {@code points} contains less than three elements
     * @throws NullPointerException if {@code points} or any of its elements is {@code null}
     */
    public static Subdivision findDelaunaySubdivision(PointD[] points) {

        final PointI[] indices = findDelaunay(points);
        final LineD[] lines = LineD.fromIndexPoints(points, indices);
        return Subdivision.fromLines(lines, 0);
    }
    
    /**
     * Creates a {@link Voronoi} instance with the specified set of {@link PointD}
     * coordinates and requested actions.
     * Call {@link #sweepLine} on the returned {@link Voronoi} instance to actually
     * compute the Voronoi diagram and/or Delaunay triangulation for {@code points}.
     * <p>
     * The specified {@code clip} is ignored if {@code findDelaunay} is {@code true},
     * or if its {@link RectD#width} or {@link RectD#height} is not positive.</p>
     * 
     * @param points an array containing the {@link PointD} coordinates whose 
     *               Voronoi diagram and/or Delaunay triangulation to find
     * @param clip an array whose single element is a {@link RectD} indicating the
     *             desired clipping bounds for pseudo-vertices, and on return
     *             contains the actual clipping bounds for the Voronoi diagram
     * @param findDelaunay {@code true} to find only the Delaunay triangulation for
     *             {@code points}, {@code false} to also find the Voronoi diagram
     * @throws IllegalArgumentException if {@code points} contains less than three elements,
     *                                  or {@code clip} does not contain exactly one element
     * @throws NullPointerException if {@code points} or {@code clip} is {@code null},
     *                              or any of their elements are {@code null}
     */
    private Voronoi(PointD[] points, RectD[] clip, boolean findDelaunay) {
        if (points == null)
            throw new NullPointerException("points");
        if (points.length < 3)
            throw new IllegalArgumentException("points.length < 3");
        if (clip == null)
            throw new NullPointerException("clip");
        if (clip.length != 1)
            throw new IllegalArgumentException("clip.length != 1");

        /*
         * Copy input array points[] into work array _sites[].
         * _sites[].index values refer to original points[] array,
         * not to _sites[] which is subsequently sorted and modified.
         */
        _sites = new SiteVertex[points.length];
        for (int i = 0; i < _sites.length; i++)
            _sites[i] = new SiteVertex(points[i], i);

        // sort by ascending y-coordinates, then x-coordinates
        final Comparator<SiteVertex> comparator = (s, t) -> {
            if (s == t) return 0;

            if (s.y < t.y) return -1;
            if (s.y > t.y) return 1;
            if (s.x < t.x) return -1;
            if (s.x > t.x) return 1;

            return 0;
        };

        Arrays.sort(_sites, comparator);

        // find minimum & maximum x-coordinates
        _minX = _maxX = _sites[0].x;
        for (int i = 1; i < _sites.length; i++) {
            final double x = _sites[i].x;
            if (x < _minX) _minX = x;
            if (x > _maxX) _maxX = x;
        }

        // sort defines minimum & maximum y-coordinates
        _minY = _sites[0].y;
        _maxY = _sites[_sites.length - 1].y;

        /*
         * Voronoi diagrams and Delaunay triangulations contain at most 3n-6 edges,
         * and Voronoi diagrams contain at most 2n-5 vertices for n >= 3 input points.
         * 
         * We allocate space for 2n vertices to allow for additional pseudo-vertices.
         * This extra space is not required for the vertex index mapping array since
         * pseudo-vertices don’t use SiteVertex objects with index remapping.
         */
        final int maxVertexCount = 2 * _sites.length - 5;
        final int maxEdgeCount = 3 * _sites.length - 6;

        if (findDelaunay) {
            // only allocate Delaunay edge output storage
            _delaunayEdges = new ArrayList<>(maxEdgeCount);
            return;
        }

        // calculate clipping region for Voronoi edges
        final double dx = _maxX - _minX;
        final double dy = _maxY - _minY;
        final double d = Math.max(dx, dy) * 1.1;

        _minClipX = _minX - (d - dx) / 2;
        _maxClipX = _maxX + (d - dx) / 2;
        _minClipY = _minY - (d - dy) / 2;
        _maxClipY = _maxY + (d - dy) / 2;

        // extend clipping region to specified bounds, if any
        if (clip[0].width() > 0 && clip[0].height() > 0) {
            _minClipX = Math.min(_minClipX, clip[0].min.x);
            _maxClipX = Math.max(_maxClipX, clip[0].max.x);
            _minClipY = Math.min(_minClipY, clip[0].min.y);
            _maxClipY = Math.max(_maxClipY, clip[0].max.y);
        }

        // return computed clipping region
        clip[0] = new RectD(
            new PointD(_minClipX, _minClipY),
            new PointD(_maxClipX, _maxClipY));

        // allocate Voronoi output & auxiliary storage
        _voronoiVertices = new ArrayList<>(maxVertexCount + 5);
        _voronoiEdges = new ArrayList<>(maxEdgeCount);
        _vertexIndices = new int[maxVertexCount];
    }

    // marks edge as deleted within edge list
    private final static FullEdge DELETED_EDGE = new FullEdge();

    // bounding rectangle of input coordinates
    private double _minX, _maxX, _minY, _maxY;

    // clipping rectangle for Voronoi output coordinates
    // (slightly larger than input to show unbounded edges)
    private double _minClipX, _maxClipX, _minClipY, _maxClipY;

    // generator sites for Voronoi diagram
    private SiteVertex[] _sites;

    // counter yielding unique internal index
    private int _vertexCount;

    // mapping of internal to output indices
    private int[] _vertexIndices;

    // Delaunay triangulation and Voronoi diagram
    private List<PointI> _delaunayEdges;
    private List<VoronoiEdge> _voronoiEdges;
    private List<PointD> _voronoiVertices;

    /**
     * Performs Fortune’s sweep line algorithm on the current set of input coordinates.
     */
    private void sweepLine() {

        SiteVertex lowSite, highSite, p, v;
        PointD minSite = PointD.EMPTY;
        HalfEdge leftHE, rightHE, prevHE, nextHE, bisectHE;
        FullEdge bisector;

        priQueueInit();
        edgeListInit();

        // get second input site
        int newSiteIndex = 1;
        SiteVertex newSite = _sites[newSiteIndex];

        while (true) {
            if (_priQueueCount != 0) minSite = priQueuePeek();

            if (newSite != null &&
                (_priQueueCount == 0 || newSite.y < minSite.y ||
                (newSite.y == minSite.y && newSite.x < minSite.x))) {

                // new site is smallest
                leftHE = edgeListLeftBound(newSite);
                rightHE = leftHE.right;
                lowSite = getRightSite(leftHE);
                bisector = bisectSites(lowSite, newSite);
                bisectHE = new HalfEdge(bisector, false);

                edgeListInsert(leftHE, bisectHE);
                p = intersect(leftHE, bisectHE);
                if (p != null) {
                    priQueueDelete(leftHE);
                    priQueueInsert(leftHE, p, getDistance(p, newSite));
                }
                leftHE = bisectHE;
                bisectHE = new HalfEdge(bisector, true);

                edgeListInsert(leftHE, bisectHE);
                p = intersect(bisectHE, rightHE);
                if (p != null)
                    priQueueInsert(bisectHE, p, getDistance(p, newSite));

                newSite = null;
                if (++newSiteIndex < _sites.length)
                    newSite = _sites[newSiteIndex];
            }
            else if (_priQueueCount != 0) {

                // intersection is smallest
                leftHE = priQueuePop();
                rightHE = leftHE.right;
                prevHE = leftHE.left;
                nextHE = rightHE.right;

                lowSite = getLeftSite(leftHE);
                highSite = getRightSite(rightHE);
                v = leftHE.vertex;
                v.index = _vertexCount++;

                // create new Voronoi vertex if within plotting area
                if (_voronoiEdges != null &&
                    v.x >= _minClipX && v.x <= _maxClipX &&
                    v.y >= _minClipY && v.y <= _maxClipY) {

                    _vertexIndices[v.index] = _voronoiVertices.size();
                    _voronoiVertices.add(new PointD(v.x, v.y));
                }

                addVertex(leftHE.edge, leftHE.isRight, v);
                addVertex(rightHE.edge, rightHE.isRight, v);

                edgeListDelete(leftHE);
                priQueueDelete(rightHE);
                edgeListDelete(rightHE);

                boolean isRight = false;
                if (lowSite.y > highSite.y) {
                    final SiteVertex tmpSite = lowSite;
                    lowSite = highSite;
                    highSite = tmpSite;
                    isRight = true;
                }

                bisector = bisectSites(lowSite, highSite);
                bisectHE = new HalfEdge(bisector, isRight);
                edgeListInsert(prevHE, bisectHE);
                addVertex(bisector, !isRight, v);

                p = intersect(prevHE, bisectHE);
                if (p != null) {
                    priQueueDelete(prevHE);
                    priQueueInsert(prevHE, p, getDistance(p, lowSite));
                }

                p = intersect(bisectHE, nextHE);
                if (p != null)
                    priQueueInsert(bisectHE, p, getDistance(p, lowSite));
            } else
                break;
        }

        // output remaining Voronoi edges (those with only one vertex)
        if (_voronoiEdges != null)
            for (HalfEdge he = _edgeListLeft.right;
                he != _edgeListRight; he = he.right)
                storeVoronoiEdge(he.edge);
    }

    /**
     * Adds the specified vertex to the specified side of the specified edge.
     * When {@code e} contains vertices on both sides, it is added to the Voronoi diagram.
     * 
     * @param e the {@link FullEdge} to which to add {@code s}
     * @param isRight {@code true} to add {@code s} to the right side of {@code e},
     *                {@code false} to add {@code s} to the left side of {@code e}
     * @param s the {@link SiteVertex} that represents the vertex to add
     */
    private void addVertex(FullEdge e, boolean isRight, SiteVertex s) {
        e.setVertex(isRight, s);
        if (_voronoiEdges != null && e.getVertex(!isRight) != null)
            storeVoronoiEdge(e);
    }

    /**
     * Creates a Voronoi edge that bisects the two specified sites.
     * Also creates a corresponding Delaunay edge if requested.
     * 
     * @param s the {@link SiteVertex} that represents the first site to bisect
     * @param t the {@link SiteVertex} that represents the second site to bisect
     * @return the new {@link FullEdge} the represents the Voronoi edge bisecting {@code s} and {@code t}
     */
    private FullEdge bisectSites(SiteVertex s, SiteVertex t) {

        final FullEdge e = new FullEdge();
        e.leftSite = s;
        e.rightSite = t;

        final double dx = t.x - s.x;
        final double dy = t.y - s.y;
        final double adx = (dx > 0 ? dx : -dx);
        final double ady = (dy > 0 ? dy : -dy);
        e.c = s.x * dx + s.y * dy + (dx * dx + dy * dy) / 2;

        if (adx > ady) {
            // horizontal edge (+45...0...-45 degrees)
            e.a = 1;
            e.b = dy / dx;
            e.c = e.c / dx;
        } else {
            // vertical edge (-45...90...+45 degrees)
            e.a = dx / dy;
            e.b = 1;
            e.c = e.c / dy;
        }

        /*
         *  By definition, whenever two sites s and t have been bisected by a
         *  Voronoi edge e they also form an edge of the Delaunay triangulation.
         *  So if a triangulation is desired, the Delaunay edge is now stored.
         */
        if (_delaunayEdges != null)
            _delaunayEdges.add(new PointI(s.index, t.index));

        return e;
    }

    /**
     * Computes the distance between the two specified sites or vertices.
     * @param s the first {@link SiteVertex} to examine
     * @param t the second {@link SiteVertex} to examine
     * @return the Euclidean distance between the {@link SiteVertex#x} and
     *         {@link SiteVertex#y} coordinates of {@code s} and {@code t}
     */
    private static double getDistance(SiteVertex s, SiteVertex t) {
        final double dx = s.x - t.x;
        final double dy = s.y - t.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Gets the site to the left of the specified edge.
     * @param he the {@link HalfEdge} to examine
     * @return the {@link FullEdge#rightSite} of the {@link HalfEdge#edge} of the specified {@code he}
     *         if its {@link HalfEdge#isRight} flag is {@code true}, else the {@link FullEdge#leftSite}
     */
    private SiteVertex getLeftSite(HalfEdge he) {
        if (he.edge == null)
            return _sites[0];

        if (he.isRight)
            return he.edge.rightSite;
        else
            return he.edge.leftSite;
    }

    /**
     * Gets the site to the right of the specified edge.
     * @param he the {@link HalfEdge} to examine
     * @return the {@link FullEdge#rightSite} of the {@link HalfEdge#edge} of the specified {@code he}
     *         if its {@link HalfEdge#isRight} flag is {@code false}, else the {@link FullEdge#leftSite}
     */
    private SiteVertex getRightSite(HalfEdge he) {
        if (he.edge == null)
            return _sites[0];

        if (he.isRight)
            return he.edge.leftSite;
        else
            return he.edge.rightSite;
    }

    /**
     * Creates a Voronoi vertex at the intersection of the specified edges.
     * @param he1 the first {@link HalfEdge} to intersect
     * @param he2 the second {@link HalfEdge} to intersect
     * @return the new {@link SiteVertex} that represents the Voronoi vertex at the
     *         intersection of {@code he1} and {@code he2}, if found, else {@code null}
     */
    private static SiteVertex intersect(HalfEdge he1, HalfEdge he2) {

        final FullEdge e1 = he1.edge;
        final FullEdge e2 = he2.edge;
        if (e1 == null || e2 == null) return null;
        if (e1.rightSite == e2.rightSite) return null;

        final double d = e1.a * e2.b - e1.b * e2.a;
        if (Math.abs(d) < 1.0e-10) return null;

        final double xint = (e1.c * e2.b - e2.c * e1.b) / d;
        final double yint = (e2.c * e1.a - e1.c * e2.a) / d;

        HalfEdge el; FullEdge e;
        if ((e1.rightSite.y < e2.rightSite.y) ||
            (e1.rightSite.y == e2.rightSite.y && e1.rightSite.x < e2.rightSite.x)) {
            el = he1; e = e1;
        } else {
            el = he2; e = e2;
        }

        final boolean isRightOfSite = (xint >= e.rightSite.x);
        if ((isRightOfSite && !el.isRight) || (!isRightOfSite && el.isRight))
            return null;

        return new SiteVertex(xint, yint);
    }

    /**
     * Determines whether the specified site is to the right of the specified edge.
     * @param he the {@link HalfEdge} to examine
     * @param p the {@link SiteVertex} representing the site to examine
     * @return {@code true} if {@code p} is to the right of {@code he}, else {@code false}
     */
    private static boolean isRightOf(HalfEdge he, SiteVertex p) {

        final FullEdge e = he.edge;
        final boolean isRightOfSite = (p.x > e.rightSite.x);
        if (isRightOfSite && !he.isRight) return true;
        if (!isRightOfSite && he.isRight) return false;

        boolean isAbove;
        if (e.a == 1) {
            final double dyp = p.y - e.rightSite.y;
            final double dxp = p.x - e.rightSite.x;
            boolean isFast = false;

            if ((!isRightOfSite && e.b < 0) || (isRightOfSite && e.b >= 0)) {
                isAbove = (dyp >= e.b * dxp);
                isFast = isAbove;
            } else {
                isAbove = (p.x + p.y * e.b > e.c);
                if (e.b < 0) isAbove = !isAbove;
                if (!isAbove) isFast = true;
            }

            if (!isFast) {
                final double dxs = e.rightSite.x - e.leftSite.x;
                isAbove = (e.b * (dxp * dxp - dyp * dyp) <
                        dxs * dyp * (1 + 2 * dxp / dxs + e.b * e.b));
                if (e.b < 0) isAbove = !isAbove;
            }
        } else {
            final double yl = e.c - e.a * p.x;
            final double t1 = p.y - yl;
            final double t2 = p.x - e.rightSite.x;
            final double t3 = yl - e.rightSite.y;
            isAbove = (t1 * t1 > t2 * t2 + t3 * t3);
        }

        return (he.isRight ? !isAbove : isAbove);
    }

    /**
     * Stores the specified Voronoi edge, clipped to the desired output region.
     * Also creates a new pseudo-vertex in the Voronoi diagram if {@code e}
     * extends beyond the desired output region.
     * 
     * @param e the {@link FullEdge} that represents the Voronoi edge to store
     */
    private void storeVoronoiEdge(FullEdge e) {
        assert(_voronoiEdges != null);

        SiteVertex s1, s2;
        double x1, x2, y1, y2;

        /*
         * e.LeftVertex stores the left vertex of e, and e.RightVertex stores the right vertex.
         *
         * For "vertical" edges (e.a != 1 && e.b == 1), this is all we need to know.
         * s1 is the left vertex, s2 is the right vertex, as in the "else" branch.
         *
         * For "horizontal" edges (e.a == 1), however, s1 should be the lower vertex and s2
         * the higher one. If the edge is pointing downward (e.b >= 0), the right vertex is
         * lower and the left one is higher. So we must reverse the vertices in the "if" branch.
         *
         * If the edge is pointing upward (e.b < 0), the left vertex is lower and the right
         * one is higher. We can reuse the "else" branch in this case.
         */
        if (e.a == 1 && e.b >= 0) {
            s1 = e.rightVertex;
            s2 = e.leftVertex;
        } else {
            s1 = e.leftVertex;
            s2 = e.rightVertex;
        }

        // the following holds true as per bisectSites
        assert(e.a == 1 || e.b == 1);

        if (e.a == 1) {
            // horizontal edge (+45...0...-45 degrees)

            if (s1 != null && s1.y > _minClipY) {
                y1 = s1.y;
                // edge invisible if lower vertex above limits
                if (y1 > _maxClipY) return;
            } else
                y1 = _minClipY;

            if (s2 != null && s2.y < _maxClipY) {
                y2 = s2.y;
                // edge invisible if higher vertex below limits
                if (y2 < _minClipY) return;
            } else
                y2 = _maxClipY;

            x1 = e.c - e.b * y1;
            x2 = e.c - e.b * y2;

            if (x1 > _maxClipX) {
                if (x2 > _maxClipX) return;
                x1 = _maxClipX; y1 = (e.c - x1) / e.b;
            }
            else if (x1 < _minClipX) {
                if (x2 < _minClipX) return;
                x1 = _minClipX; y1 = (e.c - x1) / e.b;
            }

            if (x2 > _maxClipX) {
                x2 = _maxClipX; y2 = (e.c - x2) / e.b;
            }
            else if (x2 < _minClipX) {
                x2 = _minClipX; y2 = (e.c - x2) / e.b;
            }
        } else {
            // (e.a != 1), hence (e.b == 1): vertical edge (-45...90...+45 degrees)

            if (s1 != null && s1.x > _minClipX) {
                x1 = s1.x;
                // edge invisible if left vertex right of limits
                if (x1 > _maxClipX) return;
            } else
                x1 = _minClipX;

            if (s2 != null && s2.x < _maxClipX) {
                x2 = s2.x;
                // edge invisible if right vertex left of limits
                if (x2 < _minClipX) return;
            } else
                x2 = _maxClipX;

            y1 = e.c - e.a * x1;
            y2 = e.c - e.a * x2;

            if (y1 > _maxClipY) {
                if (y2 > _maxClipY) return;
                y1 = _maxClipY; x1 = (e.c - y1) / e.a;
            }
            else if (y1 < _minClipY) {
                if (y2 < _minClipY) return;
                y1 = _minClipY; x1 = (e.c - y1) / e.a;
            }

            if (y2 > _maxClipY) {
                y2 = _maxClipY; x2 = (e.c - y2) / e.a;
            }
            else if (y2 < _minClipY) {
                y2 = _minClipY; x2 = (e.c - y2) / e.a;
            }
        }

        /*
         * Voronoi vertices s1, s2 are output if they are defined (i.e. the edge is
         * closed on that end) and the vertex lies within the plotting region. Otherwise
         * a new pseudo-vertex is created, situated on one of the region boundaries.
         */
        int vertex1, vertex2;
        if (s1 != null &&
            s1.x >= _minClipX && s1.x <= _maxClipX &&
            s1.y >= _minClipY && s1.y <= _maxClipY) {
            // use existing vertex s1 to start new edge
            vertex1 = _vertexIndices[s1.index];
        } else {
            // create new pseudo-vertex at (x1, y1)
            vertex1 = _voronoiVertices.size();
            _voronoiVertices.add(new PointD(x1, y1));
        }

        if (s2 != null &&
            s2.x >= _minClipX && s2.x <= _maxClipX &&
            s2.y >= _minClipY && s2.y <= _maxClipY) {
            // use existing vertex s2 to end new edge
            vertex2 = _vertexIndices[s2.index];
        } else {
            // create new pseudo-vertex at (x2, y2)
            vertex2 = _voronoiVertices.size();
            _voronoiVertices.add(new PointD(x2, y2));
        }

        // add Voronoi vertex with indices of bisected sites
        final VoronoiEdge ve = new VoronoiEdge(e.leftSite.index, e.rightSite.index, vertex1, vertex2);
        _voronoiEdges.add(ve);
    }

    // ----- Edge list implementation ----

    private HalfEdge[] _edgeList;
    private HalfEdge _edgeListLeft, _edgeListRight;

    /**
     * Initializes the edge list.
     */
    private void edgeListInit() {

        final int n = (int) (2 * Math.sqrt(_sites.length + 4));
        _edgeList = new HalfEdge[n];

        _edgeListLeft = new HalfEdge();
        _edgeListRight = new HalfEdge();

        _edgeListLeft.right = _edgeListRight;
        _edgeListRight.left = _edgeListLeft;

        _edgeList[0] = _edgeListLeft;
        _edgeList[n - 1] = _edgeListRight;
    }

    /**
     * Deletes the specified {@link HalfEdge} from the edge list.
     * @param he the {@link HalfEdge} to delete
     */
    private static void edgeListDelete(HalfEdge he) {
        he.left.right = he.right;
        he.right.left = he.left;
        he.edge = Voronoi.DELETED_EDGE;
    }

    /**
     * Gets the {@link HalfEdge} at the specified hash bucket in the edge list.
     * If the {@link HalfEdge} at the specified {@code bucket} references a deleted {@link FullEdge},
     * {@link #edgeListHash} removes it from the edge list and returns {@code null}.
     * 
     * @param bucket the hash bucket to search
     * @return the {@link HalfEdge} at the specified {@code bucket}, if any, else {@code null}
     */
    private HalfEdge edgeListHash(int bucket) {
        if (bucket < 0 || bucket >= _edgeList.length)
            return null;

        final HalfEdge he = _edgeList[bucket];
        if (he == null || he.edge != Voronoi.DELETED_EDGE)
            return he;

        // hashtable points to deleted half edge
        _edgeList[bucket] = null;
        return null;
    }

    /**
     * Inserts the specified {@link HalfEdge} at the specified position in the edge list.
     * @param hePos the {@link HalfEdge} in the edge list that will be the left neighbor
     *              of the inserted {@code heNew}
     * @param heNew the {@link HalfEdge} to insert to the right of {@code hePos}
     */
    private static void edgeListInsert(HalfEdge hePos, HalfEdge heNew) {
        heNew.left = hePos;
        heNew.right = hePos.right;
        hePos.right.left = heNew;
        hePos.right = heNew;
    }

    /**
     * Finds the left bound of the specified site in the edge list.
     * @param s the {@link SiteVertex} that represents the site to find
     * @return the {@link HalfEdge} that represents the left bound of {@code s}
     */
    private HalfEdge edgeListLeftBound(SiteVertex s) {

        // use hash table to get close to desired half-edge
        final int n = _edgeList.length;
        int bucket = (int) ((s.x - _minX) / (_maxX - _minX) * n);
        if (bucket < 0) bucket = 0;
        if (bucket >= n) bucket = n - 1;

        HalfEdge he = edgeListHash(bucket);
        if (he == null)
            for (int i = 1; true; i++) {
                he = edgeListHash(bucket - i); if (he != null) break;
                he = edgeListHash(bucket + i); if (he != null) break;
            }
        assert(he != null);

        // now search linear list of half-edges for the correct one
        if (he == _edgeListLeft ||
            (he != _edgeListRight && isRightOf(he, s))) {
            do
                he = he.right;
            while (he != _edgeListRight && isRightOf(he, s));
            he = he.left;
        } else {
            do
                he = he.left;
            while (he != _edgeListLeft && !isRightOf(he, s));
        }

        // update hash table
        if (bucket > 0 && bucket < n - 1)
            _edgeList[bucket] = he;

        return he;
    }

    // ----- Priority queue implementation -----

    private HalfEdge[] _priQueue;
    private int _priQueueCount, _priQueueMin;

    /**
     * Gets the hash bucket for the specified {@link HalfEdge} in the priority queue.
     * @param he the {@link HalfEdge} whose hash bucket to return
     * @return the hash bucket for the specified {@code he}
     */
    private int priQueueBucket(HalfEdge he) {

        final int n = _priQueue.length;
        int bucket = (int) ((he.yStar - _minY) / (_maxY - _minY) * n);

        if (bucket < 0) bucket = 0;
        if (bucket >= n) bucket = n - 1;

        if (bucket < _priQueueMin)
            _priQueueMin = bucket;

        return bucket;
    }

    /**
     * Deletes the specified {@link HalfEdge} from the priority queue.
     * @param he the {@link HalfEdge} to delete
     */
    private void priQueueDelete(HalfEdge he) {
        if (he.vertex == null) return;

        HalfEdge hash = _priQueue[priQueueBucket(he)];
        while (hash.next != he) hash = hash.next;
        hash.next = he.next;

        --_priQueueCount;
        he.vertex = null;
    }

    /**
     * Initializes the priority queue.
     */
    private void priQueueInit() {

        _priQueueCount = _priQueueMin = 0;
        final int n = (int) (4 * Math.sqrt(_sites.length + 4));
        _priQueue = new HalfEdge[n];

        for (int i = 0; i < _priQueue.length; i++)
            _priQueue[i] = new HalfEdge();
    }

    /**
     * Inserts the specified {@link HalfEdge} with the specified vertex in the priority queue.
     * @param he the {@link HalfEdge} to insert
     * @param v the new value for the {@link HalfEdge#vertex} of {@code he}
     * @param offset the offset to add to the {@link HalfEdge#yStar} coordinate of {@code he}
     */
    private void priQueueInsert(HalfEdge he, SiteVertex v, double offset) {
        he.vertex = v;
        he.yStar = v.y + offset;

        HalfEdge hash = _priQueue[priQueueBucket(he)];
        HalfEdge next = hash.next;

        while (next != null && (he.yStar > next.yStar ||
            (he.yStar == next.yStar && v.x > next.vertex.x))) {
            hash = next;
            next = hash.next;
        }

        he.next = hash.next;
        hash.next = he;
        ++_priQueueCount;
    }

    /**
     * Returns the coordinates of the first {@link HalfEdge} in the priority queue, without removing it.
     * @return a {@link PointD} containing the {@link SiteVertex#x} and {@link HalfEdge#yStar}
     *         coordinates of the first {@link HalfEdge} in the priority queue
     */
    private PointD priQueuePeek() {
        while (_priQueue[_priQueueMin].next == null)
            ++_priQueueMin;

        return new PointD(
            _priQueue[_priQueueMin].next.vertex.x,
            _priQueue[_priQueueMin].next.yStar);
    }

    /**
     * Removes and returns the first {@link HalfEdge} in the priority queue.
     * @return the removed first {@link HalfEdge} in the priority queue
     */
    private HalfEdge priQueuePop() {
        HalfEdge he = _priQueue[_priQueueMin].next;
        _priQueue[_priQueueMin].next = he.next;
        --_priQueueCount;
        return he;
    }

    /**
     * Represents one full edge in the Voronoi diagram.
     */
    private static class FullEdge {
        /**
         * The a component of the line equation for the {@link FullEdge} (ax + by = c).
         */
        double a;
        /**
         * The b component of the line equation for the {@link FullEdge} (ax + by = c).
         */
        double b;
        /**
         * The c component of the line equation for the {@link FullEdge} (ax + by = c).
         */
        double c;
        /**
         * The {@link SiteVertex} that represents the left-hand generator site
         * of the pair that is bisected by the {@link FullEdge}.
         */
        SiteVertex leftSite;
        /**
         * The {@link SiteVertex} that represents the left-hand Voronoi vertex
         * terminating the {@link FullEdge}.
         */
        SiteVertex leftVertex;
        /**
         * The {@link SiteVertex} that represents the right-hand generator site
         * of the pair that is bisected by the {@link FullEdge}.
         */
        SiteVertex rightSite;
        /**
         * The {@link SiteVertex} that represents the right-hand Voronoi vertex
         * terminating the {@link FullEdge}.
         */
        SiteVertex rightVertex;

        /**
         * Gets the Voronoi vertex on the specified side of the {@link FullEdge}.
         * @param isRight {@code true} to get {@link #rightVertex}, {@code false} to get {@link #leftVertex}
         * @return {@link #rightVertex} if {@code isRight} is {@code true}, else {@link #leftVertex}
         */
        SiteVertex getVertex(boolean isRight) {
            return (isRight ? rightVertex : leftVertex);
        }

        /**
         * Sets the Voronoi vertex on the specified side of the {@link FullEdge}
         * to the specified {@link VoronoiVertex}.
         * 
         * @param isRight {@code true} to set {@link #rightVertex}, {@code false} to set {@link #leftVertex}
         * @param vertex the new value for {@link #rightVertex} if {@code isRight} is {@code true},
         *               else the new value for {@link #leftVertex}
         */
        void setVertex(boolean isRight, SiteVertex vertex) {
            if (isRight)
                rightVertex = vertex;
            else
                leftVertex = vertex;
        }
    }

    /**
     * Represents one side of an edge of the Voronoi diagram.
     */
    private static class HalfEdge {
        /**
         * The {@link FullEdge} of which the {@link HalfEdge} is a part.
         */
        FullEdge edge;
        /**
         * Indicates whether the {@link HalfEdge} is the right or left part of its full {@link #edge}.
         */
        boolean isRight;
        /**
         * The {@link HalfEdge} to the left of this instance in the edge list.
         */
        HalfEdge left;
        /**
         * The {@link HalfEdge} following this instance in the priority queue.
         */
        HalfEdge next;
        /**
         * The {@link HalfEdge} to the right of this instance in the edge list.
         */
        HalfEdge right;
        /**
         * The {@link SiteVertex} that represents the Voronoi vertex terminating the {@link HalfEdge}.
         */
        SiteVertex vertex;
        /**
         * The modified y-coordinate of the {@link HalfEdge} (y + d(z)).
         */
        double yStar;

        /**
         * Creates an empty {@link HalfEdge}.
         */
        HalfEdge() { }

        /**
         * Creates a {@link HalfEdge} with the specified {@link FullEdge} and direction flag.
         * @param edge the {@link FullEdge} of which the {@link HalfEdge} is a part
         * @param isRight {@code true} if the {@link HalfEdge} is the right part of {@code edge},
         *                {@code false} if it is the left part
         */
        HalfEdge(FullEdge edge, boolean isRight) {
            this.edge = edge;
            this.isRight = isRight;
        }
    }

    /**
     * Represents a generator site or vertex in the Voronoi diagram.
     */
    private static class SiteVertex {
        /**
         * The unique internal index of the {@link SiteVertex}.
         */
        int index;
        /**
         * The x-coordinate of the {@link SiteVertex}.
         */
        final double x;
        /**
         * The y-coordinate of the {@link SiteVertex}.
         */
        final double y;

        /**
         * Creates a {@link SiteVertex} with the specified {@link Double} coordinates.
         * @param x the x-coordinate of the {@link SiteVertex}
         * @param y the y-coordinate of the {@link SiteVertex}
         */
        SiteVertex(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Creates a {@link SiteVertex} with the specified {@link PointD} coordinates and unique index.
         * @param p a {@link PointD} containing the coordinates of the {@link SiteVertex}
         * @param index the unique internal index of the {@link SiteVertex}
         */
        SiteVertex(PointD p, int index) {
            this.x = p.x;
            this.y = p.y;
            this.index = index;
        }
    }
}

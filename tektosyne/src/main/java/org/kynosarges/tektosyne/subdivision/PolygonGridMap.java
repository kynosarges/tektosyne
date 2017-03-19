package org.kynosarges.tektosyne.subdivision;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Maps the faces of a planar {@link Subdivision} to {@link PolygonGrid} locations.
 * Provides a mapping between all faces of a planar {@link Subdivision} and the {@link PointI}
 * locations of the {@link PolygonGrid} from which the {@link Subdivision} was created.
 * <p>
 * The mapping is realized by a pair of arrays for optimal runtime efficiency.
 * However, {@link PolygonGridMap} will not reflect changes to the underlying
 * {@link Subdivision} or {@link PolygonGrid}.</p>
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class PolygonGridMap implements SubdivisionMap<PointI> {

    private final Subdivision _source;
    private final PolygonGrid _target;
    private final PointI[] _faceToGrid;
    private final SubdivisionFace[][] _gridToFace;

    /**
     * Creates a {@link PolygonGridMap} from a new {@link Subdivision} to the specified {@link PolygonGrid}.
     * Sets {@link #target} to {@code grid}, and {@link #source} to a new {@link Subdivision}
     * that contains all mapped {@link Subdivision#faces} created from {@code grid} locations.
     * All {@link Subdivision} coordinates are based on {@link PolygonGrid#gridToWorld}.
     * <p>
     * {@code offset} may be {@code null} which is interpreted as {@link PointD#EMPTY}. Otherwise,
     * the {@link Subdivision#faces} of the new {@link Subdivision} are shifted by {@code offset}.</p>
     * <p>
     * {@code epsilon} may be equal to or less than zero, in which case one millionth of the
     * {@link RegularPolygon#length} of the current {@link PolygonGrid#element} is used. We
     * cannot use exact coordinate comparisons because shared vertices of adjacent grid elements
     * are unlikely to evaluate to the exact same coordinates in all cases.</p>
     * 
     * @param grid the {@link PolygonGrid} that defines all mapped {@link PointI} locations
     * @param offset an optional offset by which to shift {@code grid}
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal
     * @throws NullPointerException if {@code grid} is {@code null}
     */
    public PolygonGridMap(PolygonGrid grid, PointD offset, double epsilon) {
        if (offset == null)
            offset = PointD.EMPTY;
        if (epsilon <= 0)
            epsilon = grid.element().length * 1e-6;

        final int width = grid.size().width, height = grid.size().height;
        final PointD[][] polygons = new PointD[width * height][];
        final PointD[] vertices = grid.element().vertices;

        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {
                final PointD[] polygon = new PointD[vertices.length];

                // shift polygon vertices by grid coordinates plus offset
                final PointD element = grid.gridToWorld(x, y);
                for (int i = 0; i < polygon.length; i++)
                    polygon[i] = vertices[i].add(element).add(offset);

                polygons[x * height + y] = polygon;
            }

        _target = grid;
        _source = Subdivision.fromPolygons(polygons, epsilon);
        _faceToGrid = new PointI[width * height];
        _gridToFace = new SubdivisionFace[width][height];

        // determine equivalence of faces and grid elements
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {
                final PointD[] polygon = polygons[x * height + y];
                final SubdivisionFace face = _source.findFace(polygon, false);

                // bounded faces start at creation index one
                _faceToGrid[face.key() - 1] = new PointI(x, y);
                _gridToFace[x][y] = face;
            }
    }

    /**
     * Gets the {@link Subdivision} that contains all mapped {@link Subdivision#faces}.
     * @return the {@link Subdivision} that contains all {@link Subdivision#faces} accepted
     *         and returned by {@link #fromFace} and {@link #toFace}, respectively
     */
    @Override
    public Subdivision source() {
        return _source;
    }

    /**
     * Gets the {@link PolygonGrid} that defines all mapped {@link PointI} locations.
     * @return the {@link PolygonGrid} that defines all {@link PointI} locations returned
     *         and accepted by {@link #fromFace} and {@link #toFace}, respectively
     */
    @Override
    public PolygonGrid target() {
        return _target;
    }

    /**
     * Converts the specified {@link SubdivisionFace} into the associated {@link PointI} location.
     * @param face the {@link SubdivisionFace} to convert
     * @return the {@link PointI} location associated with {@code face}
     * @throws IllegalArgumentException if {@link #source} does not contain {@code face},
     *                                  or {@code face} is the unbounded {@link SubdivisionFace}
     * @throws NullPointerException if {@code face} is {@code null}
     */
    @Override
    public PointI fromFace(SubdivisionFace face) {
        try {
            return _faceToGrid[face.key() - 1];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("face", e);
        }
    }

    /**
     * Converts the specified {@link PointI} location into the associated {@link SubdivisionFace}.
     * @param value the {@link PointI} location to convert
     * @return the {@link SubdivisionFace} associated with {@code value}
     * @throws IllegalArgumentException if {@code value} does not map to any
     *                                  {@link SubdivisionFace} within {@link #source}
     */
    @Override
    public SubdivisionFace toFace(PointI value) {
        try {
            return _gridToFace[value.x][value.y];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("value", e);
        }
    }
}

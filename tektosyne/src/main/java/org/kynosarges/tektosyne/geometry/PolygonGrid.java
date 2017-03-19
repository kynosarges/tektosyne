package org.kynosarges.tektosyne.geometry;

import java.util.*;

import org.kynosarges.tektosyne.*;
import org.kynosarges.tektosyne.graph.*;

/**
 * Represents a rectangular grid composed of regular polygons.
 * Represents a mesh of identical squares or regular hexagons. The shape of each element
 * is described by a {@link RegularPolygon} which must have either four or six
 * {@link RegularPolygon#sides}.
 * <p>
 * Each polygonal element within the {@link PolygonGrid} corresponds to a coordinate pair
 * within an integer rectangle. That is, coordinates range from zero to a user-defined
 * width and height. The exact mapping of elements to coordinates depends on the underlying
 * {@link RegularPolygon} and on the associated {@link PolygonGridShift} value.</p>
 * <p>
 * {@link PolygonGrid} supports generic graph algorithms through its implementation of the
 * {@link Graph} interface. The graph nodes are the {@link PointI} coordinates of all grid
 * locations. Two nodes are considered connected if they correspond to neighboring grid
 * locations. The distance measure is the number of intervening grid locations.</p>
 * <p>
 * Other methods provide topological information on grid locations, conversion to and from
 * {@link Graph} world coordinates defined by the size of the supplied {@link RegularPolygon},
 * and the creation of a read-only wrapper.</p>
 * <p>
 * <b>Note:</b> {@link PolygonGrid} defines its location by {@link Integer} indices but unlike
 * geometric primitives, calculations do not check for {@link Integer} overflow. We assume that
 * {@link PolygonGrid} always has a fairly moderate size so that overflow cannot occur.</p>
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class PolygonGrid implements Graph<PointI> {

    // mutable instance data
    private final InstanceData _data;

    /**
     * Represents an invalid {@link PolygonGrid} location.
     * Holds a {@link PointI} whose {@link PointI#x} and {@link PointI#y} components are both -1.
     * This represents a location outside of any {@link PolygonGrid} since column and row indices
     * are always zero-based.
     * <p>
     * Various {@link PolygonGrid} methods return {@link #INVALID_LOCATION} to indicate that a 
     * valid {@link PolygonGrid} location could not be found. Clients are encouraged to use this
     * read-only field for the same purpose.</p>
     */
    public static final PointI INVALID_LOCATION = new PointI(-1, -1);

    /**
     * Indicates whether the {@link PolygonGrid} is read-only.
     * Attempting to modify a read-only {@link PolygonGrid} will throw an {@link IllegalStateException}.
     * Use {@link #asReadOnly} to create a read-only wrapper around a given {@link PolygonGrid}.
     */
    public final boolean isReadOnly;

    /**
     * Creates a {@link PolygonGrid} with the specified {@link #element}.
     * {@link #gridShift} is set to an appropriate default value for {@code element}.
     * {@link #size} is set to a default value of (1,1).
     * 
     * @param element the {@link RegularPolygon} that constitutes an element of the {@link PolygonGrid}
     * @throws IllegalArgumentException if {@code element} is neither a square nor a hexagon
     * @throws NullPointerException if {@code element} is {@code null}
     */
    public PolygonGrid(RegularPolygon element) {
        if (element == null)
            throw new NullPointerException("element");

        isReadOnly = false;
        _data = new InstanceData();
        setElement(element);
        setSize(new SizeI(1, 1));
    }

    /**
     * Creates a {@link PolygonGrid} with the specified {@link #element} and {@link #gridShift}.
     * {@link #size} is set to a default value of (1,1).
     * 
     * @param element the {@link RegularPolygon} that constitutes an element of the {@link PolygonGrid}
     * @param gridShift a {@link PolygonGridShift} value indicating the shifting of rows or columns
     *                  in the {@link PolygonGrid}
     * @throws IllegalArgumentException if {@code element} is neither a square nor a hexagon,
     *                                  or {@code gridShift} is incompatible with {@code element}
     * @throws NullPointerException if {@code element} or {@code gridShift} is {@code null}
     */
    public PolygonGrid(RegularPolygon element, PolygonGridShift gridShift) {
        this(element);
        setGridShift(gridShift);
    }

    /**
     * Creates a {@link PolygonGrid} that is a copy of the specified instance.
     * The two {@link PolygonGrid} instances share no mutable data and can be altered independently.
     * 
     * @param grid the {@link PolygonGrid} whose data to copy to the new instance
     * @throws NullPointerException if {@code grid} is {@code null}
     */
    public PolygonGrid(PolygonGrid grid) {
        if (grid == null)
            throw new NullPointerException("grid");

        isReadOnly = false;
        _data = new InstanceData(grid._data);
    }

    /**
     * Creates a {@link PolygonGrid} that is a read-only view of the specified {@link InstanceData}.
     * Sets {@link #isReadOnly} to {@code true}. Used by the {@link #asReadOnly} factory method.
     * 
     * @param data the {@link InstanceData} to share
     * @throws NullPointerException if {@code data} is {@code null}
     */
    private PolygonGrid(InstanceData data) {
        if (data == null)
            throw new NullPointerException("data");

        isReadOnly = true;
        _data = data;
    }

    /**
     * Gets the world distance between the center points of neighboring {@link #element} shapes.
     * {@link SizeD#width} holds the horizontal distance between neighboring elements
     * within the same row, and {@link SizeD#height} holds the vertical distance between
     * neighboring elements within the same column.
     * 
     * @return a {@link SizeD} indicating the distance, in world coordinates, between the center
     *         points of neighboring {@link #element} shapes, given the current {@link #gridShift}
     */
    public SizeD centerDistance() {
        return _data.centerDistance;
    }

    /**
     * Gets all coordinate offsets to reach a neighboring location on a shared edge.
     * Identical to {@link #neighborOffsets} except that inner arrays never contain offsets
     * to neighboring locations on shared vertices, even if {@link RegularPolygon#vertexNeighbors}
     * is {@code true} for the current {@link #element}. Use {@link #getEdgeNeighborOffsets}
     * to determine the correct inner array for a given location.
     * <p>
     * <b>Note:</b> For better performance, each {@link PolygonGrid} instance directly returns its
     * own copy of this array, rather than making another copy on each {@link #edgeNeighborOffsets}
     * call. Do not change the array contents!</p>
     * 
     * @return a jagged array containing 1x4, 2x4, or 2x6 {@link PointI} instances
     *         whose coordinates range from -1 to +1 in both dimensions
     */
    public PointI[][] edgeNeighborOffsets() {
        return _data.edgeNeighborOffsets;
    }

    /**
     * Gets the {@link RegularPolygon} that constitutes a {@link PolygonGrid} element.
     * Always either a square or a hexagon.
     * 
     * @return the {@link RegularPolygon} that constitutes a {@link PolygonGrid} element
     */
    public final RegularPolygon element() {
        return _data.element;
    }

    /**
     * Gets a {@link PolygonGridShift} value indicating how rows and columns are shifted.
     * Automatically reset to an appropriate default value by {@link #setElement} if the new
     * {@link #element} is incompatible with the current value. The following table shows the
     * default {@link #gridShift} values for all possible {@link #element} shapes:
     * <br><br><table>
     * <caption>Default Values</caption>
     * <tr><th>{@code element}</th><th>{@code gridShift}</th></tr>
     * <tr><td>Square on Edge</td><td>{@link PolygonGridShift#NONE}</td></tr>
     * <tr><td>Square on Vertex</td><td>{@link PolygonGridShift#ROW_RIGHT}</td></tr>
     * <tr><td>Hexagon on Edge</td><td>{@link PolygonGridShift#COLUMN_DOWN}</td></tr>
     * <tr><td>Hexagon on Vertex</td><td>{@link PolygonGridShift#ROW_RIGHT}</td></tr>
     * </table>
     * 
     * @return a {@link PolygonGridShift} value indicating how rows and columns are shifted
     */
    public final PolygonGridShift gridShift() {
        return _data.gridShift;
    }

    /**
     * Gets all coordinate offsets to reach a neighboring location.
     * The outer array of contains either a single array if {@link #gridShift} is {@code null},
     * or two arrays for any other {@link #gridShift} value. In that case, the first inner array
     * contains offsets for left-shifted rows or up-shifted columns, and the second inner array
     * contains offsets for right-shifted rows or down-shifted columns.
     * <p>
     * The inner arrays contain the number of index positions  indicated by the
     * {@link RegularPolygon#connectivity} of the current {@link #element}.</p>
     * <p>
     * The array element at index position [<em>i</em>][<em>j</em>] contains the coordinate offsets
     * to reach the neighboring location on edge <em>j</em> when the current location resides in an
     * odd- or even-numbered row or column, as indicated by <em>i</em>. Counting starts at the topmost
     * edge if {@link RegularPolygon#hasTopIndex} is {@code true} and with the edge to the right
     * of the topmost vertex otherwise, continuing clockwise.</p>
     * <p>
     * If {@link RegularPolygon#vertexNeighbors} is {@code true} for the current {@link #element},
     * the inner arrays instead contain the offsets to the neighboring locations on all edges
     * and vertices in an alternating sequence. Counting starts with the topmost edge for
     * {@link PolygonOrientation#ON_EDGE} orientation and with the topmost vertex otherwise,
     * continuing clockwise.</p>
     * <p>
     * Use {@link #getNeighborOffsets} to determine the correct inner array for a given location.
     * Use {@link #getNeighbor} and {@link #getNeighbors} to directly find one or more neighbors
     * of a given location.</p>
     * <p>
     * <b>Note:</b> For better performance, each {@link PolygonGrid} instance directly returns its
     * own copy of this array, rather than making another copy on each {@link #neighborOffsets} call.
     * Do not change the array contents!</p>
     * 
     * @return a jagged array containing 1x4, 2x4, 2x6, or 2x8 {@link PointI} instances
     *         whose coordinates range from -2 to +2 in both dimensions
     */
    public PointI[][] neighborOffsets() {
        return _data.neighborOffsets;
    }

    /**
     * Gets the number of rows and columns in the {@link PolygonGrid}.
     * Each dimension is always at equal to or greater than one.
     * 
     * @return a {@link SizeI} whose {@link SizeI#height} indicates the number of rows
     *         and whose {@link SizeI#width} indicates the number of columns
     */
    public final SizeI size() {
        return _data.size;
    }

    /**
     * Gets the world bounds of the {@link PolygonGrid}.
     * {@link RectD#min} is always (0,0). {@link RectD#max} defines a size that
     * exactly covers the {@link PolygonGrid}, translated to world coordinates.
     * 
     * @return a {@link RectD} indicating the bounds of the {@link PolygonGrid} in world coordinates,
     *         given the current {@link #element}, {@link #gridShift}, and {@link #size}
     */
    public RectD worldBounds() {
        return _data.worldBounds;
    }

    /**
     * Determines whether the specified {@link RegularPolygon} is compatible
     * with the specified {@link PolygonGridShift} for a {@link PolygonGrid}.
     * <br><br><table>
     * <caption>Legal Combinations</caption>
     * <tr><th>{@code element}</th><th>{@code gridShift}</th></tr>
     * <tr><td>Square on Edge</td><td>{@link PolygonGridShift#NONE}</td></tr>
     * <tr><td>Square on Vertex, Hexagon on Edge</td>
     * <td>{@link PolygonGridShift#COLUMN_UP}, {@link PolygonGridShift#COLUMN_DOWN}</td></tr>
     * <tr><td>Square on Vertex, Hexagon on Vertex</td>
     * <td>{@link PolygonGridShift#ROW_LEFT}, {@link PolygonGridShift#ROW_RIGHT}</td></tr>
     * </table>
     * 
     * @param element the {@link RegularPolygon} to test
     * @param gridShift the {@link PolygonGridShift} to test
     * @return {@code true} if {@code element} and {@code gridShift} are compatible as the {@link #element}
     *         and {@link #gridShift} values of the same {@link PolygonGrid}, else {@code false}
     * @throws IllegalArgumentException if {@code element} is neither a square nor a hexagon
     * @throws NullPointerException if {@code element} or {@code gridShift} is {@code null}
     */
    public static boolean areCompatible(RegularPolygon element, PolygonGridShift gridShift) {
        if (element == null)
            throw new NullPointerException("element");
        if (gridShift == null)
            throw new NullPointerException("gridShift");

        final boolean isSquare = (element.sides == 4), isHexagon = (element.sides == 6),
                onEdge = (element.orientation == PolygonOrientation.ON_EDGE),
                onVertex = (element.orientation == PolygonOrientation.ON_VERTEX);

        if (!isSquare && !isHexagon)
            throw new IllegalArgumentException("element.sides != 4 or 6");

        switch (gridShift) {
            case NONE:
                return (isSquare && onEdge);
            
            case COLUMN_UP:
            case COLUMN_DOWN:
                return ((isSquare && onVertex) || (isHexagon && onEdge));

            case ROW_LEFT:
            case ROW_RIGHT:
                return (onVertex && (isSquare || isHexagon));

            default:
                throw new IllegalArgumentException("gridShift");
        }
    }

    /**
     * Creates a read-only view of the {@link PolygonGrid}.
     * Returns the current instance if {@link #isReadOnly} is already {@code true}.
     * <p>
     * The read-only view is a {@link PolygonGrid} whose {@link #isReadOnly} flag is {@code true}
     * and whose data is shared with the original instance. Any changes to the original instance
     * will be reflected by the read-only view, whereas attempting to modify the read-only view
     * throws an {@link IllegalStateException}.</p>
     * 
     * @return a read-only view of the {@link PolygonGrid}
     */
    public PolygonGrid asReadOnly() {
        return (isReadOnly ? this : new PolygonGrid(_data));
    }

    /**
     * Determines whether the {@link PolygonGrid} contains the specified column and row indices.
     * @param column the zero-based index of a {@link PolygonGrid} column
     * @param row the zero-based index of a {@link PolygonGrid} row
     * @return {@code true} if the {@link PolygonGrid} contains the location
     *         at the specified {@code column} and {@code row}, else {@code false}
     */
    public boolean contains(int column, int row) {
        return (column >= 0 && column < size().width &&
                row >= 0 && row < size().height);
    }

    /**
     * Determines whether the {@link PolygonGrid} entirely contains the specified {@link RectI}.
     * @param rect the {@link RectI} to examine
     * @return {@code true} if the {@link PolygonGrid} contains all locations
     *         within {@code rect}, else {@code false}
     */
    public boolean contains(RectI rect) {
        return contains(rect.min) && contains(rect.max);
    }

    /**
     * Creates a two-dimensional array with the same {@link #size} as the {@link PolygonGrid}.
     * Convenience wrapper for the reflection method {@link java.lang.reflect.Array#newInstance}.
     * 
     * @param <T> the type of all array elements
     * @param clazz the {@link Class} object for T (required for array creation)
     * @return a two-dimensional array of T elements whose first dimension equals the {@link SizeI#width}
     *         and whose second dimension equals the {@link SizeI#height} of {@link #size}
     * @throws IllegalArgumentException if {@code clazz} is {@link Void#TYPE}
     * @throws NullPointerException if {@code clazz} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T[][] createArray(Class<T> clazz) {
        return (T[][]) java.lang.reflect.Array.newInstance(clazz, size().width, size().height);
    }

    /**
     * Gets the inner array of {@link #edgeNeighborOffsets} that matches the specified location.
     * Does not check whether {@code location} is actually within the {@link PolygonGrid}.
     * See {@link #edgeNeighborOffsets} and {@link #neighborOffsets} for a description
     * of the storage format used for neighbor coordinate offsets.
     * <p>
     * <b>Note:</b> For better performance, each {@link PolygonGrid} instance directly returns its
     * own copy of this array, rather than making another copy on each {@link #getEdgeNeighborOffsets}
     * call. Do not change the array contents!</p>
     * 
     * @param location the {@link PointI} location to examine
     * @return the inner array of {@link #edgeNeighborOffsets} that matches {@code location}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public PointI[] getEdgeNeighborOffsets(PointI location) {

        final int index = ((gridShift().isRightRow(location.y) ||
                gridShift().isDownColumn(location.x)) ? 1 : 0);

        return edgeNeighborOffsets()[index];
    }

    /**
     * Gets the bounding rectangle of the {@link #element} at the specified column and row indices.
     * Always returns a region within the current {@link #worldBounds} if {@code column} and
     * {@code row} are within the {@link PolygonGrid}, but does not check whether that is the case.
     * 
     * @param column the zero-based index of a {@link PolygonGrid} column
     * @param row the zero-based index of a {@link PolygonGrid} row
     * @return a {@link RectD} that circumscribes the {@link #element} shape
     *         at the specified {@code column} and {@code row}
     */
    public RectD getElementBounds(int column, int row) {

        // determine center of specified element
        final PointD center = gridToWorld(column, row);

        // offset element bounds by center point
        return element().bounds.offset(center);
    }

    /**
     * Gets the bounding rectangle of the {@link #element} at the specified location.
     * Always returns a region within the current {@link #worldBounds} if {@code location}
     * is within the {@link PolygonGrid}, but does not check whether that is the case.
    * 
     * @param location the {@link PointI} location to examine
     * @return the {@link RectD} that circumscribes the {@link #element} shape at {@code location}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public RectD getElementBounds(PointI location) {
        return getElementBounds(location.x, location.y);
    }

    /**
     * Gets the bounding rectangle of all {@link #element} shapes within the specified {@link RectI}.
     * Always returns a region within the current {@link #worldBounds} if {@code region} is
     * fully within the {@link PolygonGrid}, but does not check whether that is the case.
     * 
     * @param region a {@link RectI} comprising all locations to examine
     * @return a {@link RectD} that circumscribes all {@link #element} shapes within {@code region}
     * @throws IllegalArgumentException if {@code region} has a zero {@link RectI#width} or {@link RectI#height}
     * @throws NullPointerException if {@code region} is {@code null}
     */
    public RectD getElementBounds(RectI region) {
        if (region.width() == 0)
            throw new IllegalArgumentException("region.width == 0");
        if (region.height() == 0)
            throw new IllegalArgumentException("region.height == 0");

        // check for single-element region
        if (region.width() == 1 && region.height() == 1)
            return getElementBounds(region.min);

        double width = element().bounds.width(), width2 = width / 2.0;
        double height = element().bounds.height(), height2 = height / 2.0;

        // compute bounding rectangle without overhang
        final PointD location = gridToWorld(region.min);
        double left = location.x - width2;
        double top = location.y - height2;
        width += (region.width() - 1) * centerDistance().width;
        height += (region.height() - 1) * centerDistance().height;

        // add overhang for shifted rows or columns
        switch (gridShift()) {

            case COLUMN_UP:
            case COLUMN_DOWN:
                if (region.width() > 1) {
                    height += height2;
                    if (gridShift().isDownColumn(region.min.x))
                        top -= height2;
                }
                break;

            case ROW_LEFT:
            case ROW_RIGHT:
                if (region.height() > 1) {
                    width += width2;
                    if (gridShift().isRightRow(region.min.y))
                        left -= width2;
                }
                break;
        }

        return new RectD(left, top, width, height);
    }

    /**
     * Gets the polygon vertices of the {@link #element} at the specified column and row indices.
     * Shifts all {@link RegularPolygon#vertices} of the {@link #element} by the result
     * of {@link #gridToWorld} for the specified {@code column} and {@code row}.
     * The grid location is not checked against the bounds of the {@link PolygonGrid}.
     * 
     * @param column the zero-based index of a {@link PolygonGrid} column
     * @param row the zero-based index of a {@link PolygonGrid} row
     * @return the {@link RegularPolygon#vertices} of the {@link #element} shape
     *         at the specified {@code column} and {@code row}
     */
    public PointD[] getElementVertices(int column, int row) {

        final PointD[] vertices = element().vertices;
        final PointD[] shiftedVertices = new PointD[vertices.length];

        // shift vertices to center of specified element
        final PointD center = gridToWorld(column, row);
        for (int i = 0; i < vertices.length; i++)
            shiftedVertices[i] = vertices[i].add(center);

        return shiftedVertices;
    }

    /**
     * Gets the location that borders the specified location on the specified edge or vertex.
     * Does not check whether {@code location} or the returned coordinates are actually within
     * the {@link PolygonGrid}. You must perform your own coordinate validation if desired.
     * <p>
     * The specified {@code index} is taken {@link Fortran#modulo} the length of the inner
     * arrays within {@link #neighborOffsets}, and may therefore be negative or greater than
     * the maximum index. See {@link #neighborOffsets} for a description of the index order.</p>
     * 
     * @param location the {@link PointI} location whose neighbor to return
     * @param index the zero-based index for the inner arrays within {@link #neighborOffsets}
     *              indicating an edge or vertex of {@code location}
     * @return the {@link PointI} coordinates of the location that borders {@code location}
     *         on the edge or vertex indicated by {@code index}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public PointI getNeighbor(PointI location, int index) {

        // offset location by normalized index in its group
        final PointI[] offsets = getNeighborOffsets(location);
        index = Fortran.modulo(index, offsets.length);
        return location.add(offsets[index]);
    }

    /**
     * Gets the edge or vertex on which the specified location borders another neighboring location.
     * Does not check whether {@code location} or {@code neighbor} are actually within the
     * {@link PolygonGrid}. You must perform your own coordinate validation if desired.
     * <p>
     * {@code getNeighborIndex} is the inverse of {@link #getNeighbor}. That is,
     * the following relations hold for all locations <em>p</em> with valid neighbors
     * <em>q</em> and neighbor indices <em>i</em>:</p>
     * <p>
     * <code>getNeighbor(p, getNeighborIndex(p, q)).equals(q);<br>
     * getNeighborIndex(p, getNeighbor(p, i)) == i;</code></p>
     * <p>
     * See {@link #neighborOffsets} for a description of the index order.</p>
     * 
     * @param location the {@link PointI} location whose edge or vertex to return
     * @param neighbor a {@link PointI} location neighboring {@code location}
     * @return a zero-based index for the inner arrays of {@link #neighborOffsets}, indicating
     *         the edge or vertex of {@code location} on which it borders {@code neighbor}
     * @throws IllegalArgumentException if {@code location} and {@code neighbor}
     *         are not neighboring locations in the {@link PolygonGrid}
     * @throws NullPointerException if {@code location} or {@code neighbor} is {@code null}
     */
    public int getNeighborIndex(PointI location, PointI neighbor) {

        // determine actual offset of specified neighbor
        final PointI offset = neighbor.subtract(location);

        // determine offset group for given location
        final PointI[] offsets = getNeighborOffsets(location);

        // try to find neighbor offset in group
        for (int i = 0; i < offsets.length; i++)
            if (offsets[i].equals(offset))
                return i;

        throw new IllegalArgumentException("no neighbor of location");
    }

    /**
     * Gets the inner array of {@link #neighborOffsets} that matches the specified location.
     * Does not check whether {@code location} is actually within the {@link PolygonGrid}.
     * See {@link #neighborOffsets} for a description of the storage format used for
     * neighbor coordinate offsets.
     * 
     * @param location the {@link PointI} location to examine
     * @return the inner array of {@link #neighborOffsets} that matches {@code location}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public PointI[] getNeighborOffsets(PointI location) {

        final int index = ((gridShift().isRightRow(location.y) ||
                gridShift().isDownColumn(location.x)) ? 1 : 0);

        return neighborOffsets()[index];
    }

    /**
     * Determines the distance between two specified locations, in movement steps.
     * Returns zero if {@code source} and {@code target} are equal, and the minimum number of
     * location transitions required to move from {@code source} to {@code target} otherwise.
     * Does not check whether the {@link PolygonGrid} actually contains {@code source} and {@code target}.
     * <p>
     * All distance calculations are O(1) operations, regardless of the concrete values of
     * {@code source} and {@code target}. The calculations for hexagon grids were adopted
     * from a Usenet post by Matthew V. Jessick.</p>
     * 
     * @param source the {@link PointI} coordinates of the source location
     * @param target the {@link PointI} coordinates of the target location
     * @return the non-negative distance between {@code source} and {@code target}, in movement steps
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     */
    public int getStepDistance(PointI source, PointI target) {
        return _data.getStepDistanceCore(source.x, source.y, target.x, target.y);
    }

    /**
     * Converts the specified column and row indices to world coordinates.
     * Always returns coordinates within the current {@link #worldBounds} if the specified
     * indices are within the {@link PolygonGrid}, but does not check whether that is the case.
     * 
     * @param column the zero-based index of a {@link PolygonGrid} column
     * @param row the zero-based index of a {@link PolygonGrid} row
     * @return the {@link PointD} world coordinates of the center of the {@link #element}
     *         shape at the specified {@code column} and {@code row} in the {@link PolygonGrid}
     */
    public PointD gridToWorld(int column, int row) {
        return _data.gridToWorldCore(column, row);
    }

    /**
     * Converts the specified {@link PointI} location to world coordinates.
     * Always returns coordinates within the current {@link #worldBounds} if {@code location}
     * is within the {@link PolygonGrid}, but does not check whether that is the case.
     * 
     * @param location the {@link PointI} location to convert
     * @return the {@link PointD} world coordinates of the center of the {@link #element}
     *         shape at the specified {@code location} in the {@link PolygonGrid}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public PointD gridToWorld(PointI location) {
        return gridToWorld(location.x, location.y);
    }

    /**
     * Determines whether the specified column and row indices are valid.
     * @param column the {@link PolygonGrid} column index to examine
     * @param row the {@link PolygonGrid} row index to examine
     * @return {@code true} if {@code column} and {@code row} are both equal to or greater than zero
     *         and strictly less than the corresponding {@link #size} dimension, else {@code false}
     */
    public boolean isValid(int column, int row) {
        return (column >= 0 && row >= 0 &&
                column < _data.size.width && row < _data.size.height);
    }

    /**
     * Determines whether the specified {@link PointI} location is valid.
     * Always fails for {@link #INVALID_LOCATION}.
     * 
     * @param location the {@link PointI} location to examine
     * @return {@code true} if {@code location} is not {@code null} and {@link #isValid(int, int)}
     *         succeeds for its components, else {@code false}
     */
    public boolean isValid(PointI location) {
        return (location != null && isValid(location.x, location.y));
    }

    /**
     * Sets the {@link RegularPolygon} that constitutes a {@link PolygonGrid} element.
     * Also resets {@link #gridShift} to an appropriate default value if the current value
     * is incompatible with {@code element}, as determined by {@link #areCompatible}.
     * 
     * @param element the {@link RegularPolygon} that constitutes a {@link PolygonGrid} element
     * @throws IllegalArgumentException if {@code element} is neither a square nor a hexagon
     * @throws IllegalStateException if {@link #isReadOnly} is {@code true}
     * @throws NullPointerException if {@code element} is {@code null}
     */
    public final void setElement(RegularPolygon element) {
        if (isReadOnly)
            throw new IllegalStateException("isReadOnly");

        // check for invalid value and incompatible gridShift
        if (!areCompatible(element, gridShift())) {
            final boolean onEdge = (element.orientation == PolygonOrientation.ON_EDGE);

            if (element.sides == 4) {
                _data.gridShift = (onEdge ?
                        PolygonGridShift.NONE : PolygonGridShift.ROW_RIGHT);
            } else {
                assert(element.sides == 6);
                _data.gridShift = (onEdge ?
                        PolygonGridShift.COLUMN_DOWN : PolygonGridShift.ROW_RIGHT);
            }
        }

        _data.element = element;
        _data.onGeometryChanged();
    }
   
    /**
     * Sets a {@link PolygonGridShift} value indicating how rows and columns are shifted.
     * See {@link #gridShift} for default values depending on {@link #element}.
     * 
     * @param gridShift a {@link PolygonGridShift} value indicating how rows and columns are shifted
     * @throws IllegalArgumentException if {@code gridShift} is incompatible with {@link #element}
     * @throws IllegalStateException if {@link #isReadOnly} is {@code true}
     * @throws NullPointerException if {@code gridShift} is {@code null}
     */
    public final void setGridShift(PolygonGridShift gridShift) {
        if (isReadOnly)
            throw new IllegalStateException("isReadOnly");
        if (!areCompatible(element(), gridShift))
            throw new IllegalArgumentException("gridShift incompatible with element");

        _data.gridShift = gridShift;
        _data.onGeometryChanged();
    }

    /**
     * Sets the number of rows and columns in the {@link PolygonGrid}.
     * @param size a {@link SizeI} whose {@link SizeI#height} indicates the number of rows
     *             and whose {@link SizeI#width} indicates the number of columns
     * @throws IllegalArgumentException if any dimension of {@code size} is less than one
     * @throws IllegalStateException if {@link #isReadOnly} is {@code true}
     * @throws NullPointerException if {@code size} is {@code null}
     */
    public final void setSize(SizeI size) {
        if (isReadOnly)
            throw new IllegalStateException("isReadOnly");

        if (size.width < 1)
            throw new IllegalArgumentException("size.width < 1");
        if (size.height < 1)
            throw new IllegalArgumentException("size.height < 1");

        _data.size = size;
        _data.onSizeChanged();
    }

    /**
     * Converts the specified world coordinates to a {@link PointI} location.
     * Returns coordinates between (0,0) and one less than the current {@link #size}
     * in either dimension if the specified world coordinates are within
     * {@link #worldBounds}, and the constant value {@link #INVALID_LOCATION} otherwise.
     * 
     * @param x the x-coordinate to convert
     * @param y the y-coordinate to convert
     * @return the {@link PointI} location of the {@link #element} whose shape contains
     *         ({@code x},{@code y}), if any, else {@link #INVALID_LOCATION}
     */
    public PointI worldToGrid(double x, double y) {
        /*
         * We first check for obviously invalid display coordinates.
         * 
         * However, worldBounds may include a small number of pixels
         * between convex border polygons that don’t hit any polygon,
         * so we need to check again when worldToGridCore returns.
         */
        if (!worldBounds().contains(x, y))
            return INVALID_LOCATION;

        final PointI element = _data.worldToGridCore(x, y);
        return (contains(element) ? element : INVALID_LOCATION);
    }

    /**
     * Converts the specified {@link PointD} world location to a {@link PointI} location.
     * Returns coordinates between (0,0) and one less than the current {@link #size} in
     * either dimension if the specified {@code location} is within {@link #worldBounds},
     * and the constant value {@link #INVALID_LOCATION} otherwise.
     * 
     * @param location the {@link PointD} world location to convert
     * @return the {@link PointI} location of the {@link #element} whose shape contains
     *         {@code location}, if any, else {@link #INVALID_LOCATION}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public PointI worldToGrid(PointD location) {
        return worldToGrid(location.x, location.y);
    }

    /**
     * Converts the specified world coordinates to a {@link PointI} location,
     * clipping to the nearest {@link #element} if necessary.
     * Always returns coordinates between (0,0) and one less than the current
     * {@link #size} in either dimension, regardless of whether the specified
     * world coordinates are within {@link #worldBounds}.
     * 
     * @param x the x-coordinate to convert
     * @param y the y-coordinate to convert
     * @return the {@link PointI} location of the {@link #element} whose shape contains
     *         ({@code x},{@code y}), if any, else of the nearest {@link #element}
     */
    public PointI worldToGridClipped(double x, double y) {
        /*
         * worldToGridCore maps invalid display coordinates to elements
         * in a hypothetically extended grid. When the resulting grid
         * coordinates are clipped, the resulting actual elements may not 
         * be the visually nearest to the original display coordinates.
         * 
         * We get better results by moving display coordinates near the grid
         * border inward by half a polygon diameter before translating them.
         * This makes them valid and usually guarantees that they get mapped
         * to the visually nearest actual grid elements.
         */
        final double marginX = element().bounds.width() / 2.0;
        final double marginY = element().bounds.height() / 2.0;

        if (x <= marginX)
            x = marginX + 1.0;
        else if (x >= worldBounds().width() - marginX)
            x = worldBounds().width() - marginX - 1.0;

        if (y <= marginY)
            y = marginY + 1.0;
        else if (y >= worldBounds().height() - marginY)
            y = worldBounds().height() - marginY - 1.0;

        final PointI element = _data.worldToGridCore(x, y);
        return element.restrict(0, 0, size().width - 1, size().height - 1);
    }

    /**
     * Converts the specified {@link PointD} world location to a {@link PointI} location,
     * clipping to the nearest {@link #element} if necessary.
     * Always returns coordinates between (0,0) and one less than the current {@link #size} in
     * either dimension, regardless of whether {@code location} is within {@link #worldBounds}.
     * 
     * @param location the {@link PointD} world location to convert
     * @return the {@link PointI} location of the {@link #element} whose shape contains
     *         {@code location}, if any, else of the nearest {@link #element}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public PointI worldToGridClipped(PointD location) {
        return worldToGridClipped(location.x, location.y);
    }

    // ----- Graph Implementation -----

    /**
     * Gets the maximum number of direct neighbors for any {@link Graph} node.
     * Returns the {@link RegularPolygon#connectivity} of the current {@link #element}.
     * 
     * @return the maximum number of direct neighbors for any {@link Graph} node
     */
    @Override
    public int connectivity() {
        return element().connectivity;
    }

    /**
     * Gets the total number of {@link #nodes} in the {@link Graph}.
     * Returns the product of {@link SizeI#width} and {@link SizeI#height}
     * for the current {@link #size}.
     * 
     * @return the total number of {@link #nodes} in the {@link Graph}
     */
    @Override
    public int nodeCount() {
        return size().width * size().height;
    }

    /**
     * Gets all nodes in the {@link Graph}.
     * Contains all {@link PointI} locations defined by {@link #size},
     * starting at (0,0) and incrementing x-coordinates before y-coordinates.
     * 
     * @return a {@link List} of all nodes in the {@link Graph}
     */
    @Override
    public List<PointI> nodes() {
        final int width = size().width, height = size().height;
        final List<PointI> nodes = new ArrayList<>(width * height);

        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                nodes.add(new PointI(x, y));

        return nodes;
    }

    /**
     * Determines whether the {@link Graph} contains the specified node.
     * Returns the result of {@link #contains(int, int)} for the {@link PointI#x}
     * and {@link PointI#y} coordinates of {@code node}.
     * 
     * @param node the {@link Graph} node to examine
     * @return {@code true} if the {@link Graph} contains {@code node}, else {@code false}
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public boolean contains(PointI node) {
        return contains(node.x, node.y);
    }

    /**
     * Finds the {@link Graph} node nearest to the specified {@link PointD} world location.
     * Returns the result of {@link #worldToGridClipped(PointD) } for {@code location}.
     * 
     * @param location the {@link PointD} location, in world coordinates, to examine
     * @return the {@link Graph} node whose {@link #getWorldLocation} result
     *         is nearest to the specified {@code location}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    @Override
    public PointI findNearestNode(PointD location) {
        return worldToGridClipped(location.x, location.y);
    }

    /**
     * Gets the distance between two specified {@link Graph} nodes.
     * Returns the result of {@link #getStepDistance} for {@code source} and {@code target},
     * which is always an {@link Integer} value cast to {@link Double}.
     * 
     * @param source the source node in the {@link Graph}
     * @param target the target node in the {@link Graph}
     * @return the non-negative distance between {@code source} and {@code target}
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     */
    @Override
    public double getDistance(PointI source, PointI target) {
        return getStepDistance(source, target);
    }

    /**
     * Gets all direct neighbors of the specified {@link Graph} node.
     * Returns an empty {@link List} if {@code node} or all its direct neighbors are not part
     * of the {@link Graph}. Otherwise, calculates all direct neighbors by adding each element
     * in the appropriate {@link #neighborOffsets} array to {@code node}, omitting any
     * neighbors outside the {@link Graph}. The resulting list is ordered clockwise.
     * 
     * @param node the {@link Graph} node whose direct neighbors to collect
     * @return a {@link List} of all {@link Graph} nodes that are directly connected
     *         with {@code node}, numbering from zero to {@link #connectivity}
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public List<PointI> getNeighbors(PointI node) {
        if (node == null)
            throw new NullPointerException("node");

        // do nothing if specified location invalid
        if (!contains(node)) return Collections.emptyList();

        // determine offset group for given location
        final PointI[] offsets = getNeighborOffsets(node);
        final List<PointI> neighbors = new ArrayList<>(offsets.length);

        // build list containing all valid neighbors
        for (PointI offset: offsets) {
            final PointI neighbor = node.add(offset);
            if (contains(neighbor)) neighbors.add(neighbor);
        }

        return neighbors;
    }

    /**
     * Gets all neighbors of the specified {@link Graph} node within the specified step distance.
     * Returns an empty {@link List} if {@code node} or all its direct neighbors are not
     * part of the {@link Graph}. Otherwise, returns a {@link List} ordered by increasing
     * x- and then y-coordinates unless {@code steps} is one, in which case the result of
     * {@link #getNeighbors(PointI)} is returned.
     * 
     * @param node the {@link Graph} node whose neighbors within {@code steps} to collect
     * @param steps the distance around {@code node}, in movement steps, in which
     *              other nodes are considered neighbors of {@code node}
     * @return a {@link List} of all {@link Graph} nodes whose step distance from
     *         {@code node} is greater than zero, but equal to or less than {@code steps}
     * @throws IllegalArgumentException if {@code steps} is equal to or less than zero
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public List<PointI> getNeighbors(PointI node, int steps) {
        if (steps <= 0)
            throw new IllegalArgumentException("distance <= 0");
        if (!contains(node))
            return Collections.emptyList();

        // return direct neighbors for single step
        if (steps == 1) return getNeighbors(node);

        final List<PointI> neighbors = new ArrayList<>();
        int distanceX = steps, distanceY = steps;

        // standing squares: double or halve coordinate distances
        if (element().sides == 4 && element().orientation == PolygonOrientation.ON_VERTEX) {
            if (gridShift().anyColumns()) {
                if (element().vertexNeighbors)
                    distanceX *= 2;
                else
                    distanceY = (distanceY + 1) / 2;
            } else {
                assert(gridShift().anyRows());
                if (element().vertexNeighbors)
                    distanceY *= 2;
                else
                    distanceX = (distanceX + 1) / 2;
            }
        }

        // compute rectangle covering potential neighbors
        final int minX = Math.max(node.x - distanceX, 0);
        final int maxX = Math.min(node.x + distanceX, size().width - 1);
        final int minY = Math.max(node.y - distanceY, 0);
        final int maxY = Math.min(node.y + distanceY, size().height - 1);

        // add all rectangle locations within step distance
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++) {
                final PointI neighbor = new PointI(x, y);
                final int delta = getStepDistance(node, neighbor);
                if (delta > 0 && delta <= steps)
                    neighbors.add(neighbor);
            }

        return neighbors;
    }
    /**
     * Gets all neighbors of the specified {@link Graph} node within the specified step distance.
     * Returns the result of the {@link Graph} interface default implementation
     * of {@link Graph#getNeighbors(Object, int)}. Intended for testing.
     * 
     * @param node the {@link Graph} node whose neighbors within {@code steps} to collect
     * @param steps the distance around {@code node}, in movement steps, in which
     *              other nodes are considered neighbors of {@code node}
     * @return a {@link List} of all {@link Graph} nodes whose step distance from
     *         {@code node} is greater than zero, but equal to or less than {@code steps}
     * @throws IllegalArgumentException if {@code steps} is equal to or less than zero
     * @throws NullPointerException if {@code node} is {@code null}
     */
    Collection<PointI> getNeighborsGraph(PointI node, int steps) {
        return Graph.super.getNeighbors(node, steps);
    }

    /**
     * Gets the world location of the specified {@link Graph} node.
     * Returns the result of {@link #gridToWorld(int, int)} for the
     * {@link PointI#x} and {@link PointI#y} coordinates of {@code node}.
     * 
     * @param node the {@link Graph} node whose world location to find
     * @return the {@link PointD} location of {@code node}, in world coordinates
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public PointD getWorldLocation(PointI node) {
        return gridToWorld(node.x, node.y);
    }

    /**
     * Gets the world region covered by the specified {@link Graph} node.
     * Returns the result of {@link #getElementVertices} for the {@link PointI#x}
     * and {@link PointI#y} coordinates of {@code node}.
     * 
     * @param node the {@link Graph} node whose world region to find
     * @return an array of {@link PointD} vertices defining the polygonal region
     *         covered by {@code node}, in world coordinates
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public PointD[] getWorldRegion(PointI node) {
        return getElementVertices(node.x, node.y);
    }

    /**
     * Contains the instance data of a {@link PolygonGrid}.
     * Read-only views share the {@link InstanceData} of the underlying writable {@link PolygonGrid}.
     * This allows the read-only view to reflect all changes to the original instance.
     */
    private static class InstanceData {

        // precomputed value for worldToGrid
        private static final double SQRT_3 = Math.sqrt(3.0);

        // settable PolygonGrid properties
        RegularPolygon element;
        PolygonGridShift gridShift = PolygonGridShift.NONE;
        SizeI size;

        // dependent PolygonGrid properties
        SizeD centerDistance;
        RectD worldBounds;
        PointI[][] edgeNeighborOffsets, neighborOffsets;

        /**
         * Creates an empty {@link InstanceData} container.
         * Used by {@link PolygonGrid} constructors that specify an {@link #element}.
         */
        InstanceData() { }

        /**
         * Creates an {@link InstanceData} container that is a copy of the specified instance.
         * Used by the copy constructor of the {@link PolygonGrid} class.
         * 
         * @param data the {@link InstanceData} to copy
         * @throws NullPointerException if {@code data} is {@code null}
         */
        InstanceData(InstanceData data) {
            if (data == null)
                throw new NullPointerException("data");

            // settable PolygonGrid properties
            element = data.element;
            gridShift = data.gridShift;
            size = data.size;

            // dependent PolygonGrid properties
            centerDistance = data.centerDistance;
            worldBounds = data.worldBounds;
            edgeNeighborOffsets = data.edgeNeighborOffsets.clone();
            neighborOffsets = data.neighborOffsets.clone();
        }

        int getStepDistanceCore(int sourceX, int sourceY, int targetX, int targetY) {

            int signDeltaX = targetX - sourceX;
            int signDeltaY = targetY - sourceY;
            if (signDeltaX == 0 && signDeltaY == 0)
                return 0;

            if (element.sides == 4) {
                final int deltaX = Math.abs(signDeltaX);
                final int deltaY = Math.abs(signDeltaY);

                if (element.orientation == PolygonOrientation.ON_EDGE)
                    return (element.vertexNeighbors ?
                            Math.max(deltaX, deltaY) : deltaX + deltaY);

                if (gridShift.anyColumns()) {
                    if (element.vertexNeighbors) {
                        final int adjustX = (gridShift.isDownColumn(sourceX) ?
                                (signDeltaY <= 0 ? 1 : 0) :
                                (signDeltaY >= 0 ? 1 : 0));

                        return deltaX / 2 + deltaY + adjustX * (deltaX % 2);
                    } else {
                        if (2 * deltaY <= deltaX) return deltaX;

                        assert(signDeltaY != 0);
                        int adjustX = (gridShift.isDownColumn(sourceX) ?
                                (signDeltaY < 0 ? 1 : -1) :
                                (signDeltaY > 0 ? 1 : -1));

                        return 2 * deltaY + adjustX * (deltaX % 2);
                    }
                } else {
                    if (element.vertexNeighbors) {
                        final int adjustY = (gridShift.isRightRow(sourceY) ?
                                (signDeltaX <= 0 ? 1 : 0) :
                                (signDeltaX >= 0 ? 1 : 0));

                        return deltaY / 2 + deltaX + adjustY * (deltaY % 2);
                    } else {
                        if (2 * deltaX <= deltaY) return deltaY;

                        assert(signDeltaX != 0);
                        final int adjustY = (gridShift.isRightRow(sourceY) ?
                                (signDeltaX < 0 ? 1 : -1) :
                                (signDeltaX > 0 ? 1 : -1));

                        return 2 * deltaX + adjustY * (deltaY % 2);
                    }
                }
            } else {
                assert(element.sides == 6);

                if (gridShift.anyColumns()) {
                    final int deltaX = Math.abs(signDeltaX);
                    signDeltaY -= (gridShift.isUpColumn(sourceX) ? deltaX : deltaX + 1) / 2;
                    final int deltaY = Math.abs(signDeltaY);
                    return (signDeltaY < 0 ? Math.max(deltaX, deltaY) : deltaX + deltaY);
                }
                else {
                    final int deltaY = Math.abs(signDeltaY);
                    signDeltaX -= (gridShift.isLeftRow(sourceY) ? deltaY : deltaY + 1) / 2;
                    final int deltaX = Math.abs(signDeltaX);
                    return (signDeltaX < 0 ? Math.max(deltaX, deltaY) : deltaX + deltaY);
                }
            }
        }

        PointD gridToWorldCore(int column, int row) {

            final boolean isDownColumn = gridShift.isDownColumn(column);
            final boolean isRightRow = gridShift.isRightRow(row);
            PointI factor; // to be multipled by (width/4, height/4)

            if (element.sides == 4) {
                if (element.orientation == PolygonOrientation.ON_EDGE)
                    factor = new PointI(4 * column + 2, 4 * row + 2);
                else
                    factor = (gridShift.anyColumns() ?
                        new PointI(2 * column + 2, 4 * row + (isDownColumn ? 4 : 2)) :
                        new PointI(4 * column + (isRightRow ? 4 : 2), 2 * row + 2));
            } else {
                assert(element.sides == 6);
                factor = (gridShift.anyColumns() ?
                    new PointI(3 * column + 2, 4 * row + (isDownColumn ? 4 : 2)) :
                    new PointI(4 * column + (isRightRow ? 4 : 2), 3 * row + 2));
            }

            return new PointD(
                factor.x * element.bounds.width() / 4.0,
                factor.y * element.bounds.height() / 4.0);
        }

        /**
         * Updates the {@link InstanceData} to reflect a changed {@link #element} or {@link #gridShift}.
         * Also calls {@link #onSizeChanged} to update {@link #worldBounds} accordingly.
         */
        void onGeometryChanged() {
            PointI[][] neighbors, edgeNeighbors;

            if (element.sides == 4) {
                final int index = (element.vertexNeighbors ? 1 : 0);

                if (element.orientation == PolygonOrientation.ON_EDGE) {
                    final double side = element.length;
                    centerDistance = new SizeD(side, side);

                    neighbors = GeometryOffsets.SQUARE_EDGE_OFFSETS[index];
                    edgeNeighbors = GeometryOffsets.SQUARE_EDGE_OFFSETS[0];
                }
                else {
                    final double width = element.bounds.width();
                    final double height = element.bounds.height();

                    if (gridShift.anyColumns()) {
                        centerDistance = new SizeD(width / 2.0, height);
                        neighbors = GeometryOffsets.SQUARE_VERTEX_COLUMN_OFFSETS[index];
                        edgeNeighbors = GeometryOffsets.SQUARE_VERTEX_COLUMN_OFFSETS[0];
                    }
                    else {
                        centerDistance = new SizeD(width, height / 2.0);
                        neighbors = GeometryOffsets.SQUARE_VERTEX_ROW_OFFSETS[index];
                        edgeNeighbors = GeometryOffsets.SQUARE_VERTEX_ROW_OFFSETS[0];
                    }
                }
            }
            else {
                assert(element.sides == 6);

                final double edgeDistance = 2.0 * element.innerRadius;
                final double vertexDistance = (3.0 * element.outerRadius) / 2.0;

                if (gridShift.anyColumns()) {
                    centerDistance = new SizeD(vertexDistance, edgeDistance);
                    neighbors = edgeNeighbors = GeometryOffsets.HEXAGON_EDGE_OFFSETS;
                }
                else {
                    centerDistance = new SizeD(edgeDistance, vertexDistance);
                    neighbors = edgeNeighbors = GeometryOffsets.HEXAGON_VERTEX_OFFSETS;
                }
            }

            neighborOffsets = neighbors.clone();
            edgeNeighborOffsets = edgeNeighbors.clone();

            onSizeChanged();
        }

        /**
         * Updates the {@link InstanceData} to reflect a changed {@link #size}.
         * Only recalculates {@link #worldBounds}.
         */
        void onSizeChanged() {

            // check for calls before size was set
            if (size == null || size.isEmpty()) {
                worldBounds = RectD.EMPTY;
                return;
            }

            // compute display bounds without overhang
            final double elementWidth = element.bounds.width();
            final double elementHeight = element.bounds.height();

            double width = elementWidth + (size.width - 1) * centerDistance.width;
            double height = elementHeight + (size.height - 1) * centerDistance.height;

            // add overhang for shifted rows or columns
            switch (gridShift) {

                case COLUMN_UP:
                case COLUMN_DOWN:
                    height += elementHeight / 2.0;
                    break;

                case ROW_LEFT:
                case ROW_RIGHT:
                    width += elementWidth / 2.0;
                    break;
            }

            worldBounds = new RectD(0, 0, width, height);
        }

        /**
         * Converts the specified world coordinates to a {@link PolygonGrid} location.
         * Does not check whether {@code x} and {@code y} fall within {@link #worldBounds}.
         * The returned grid coordinates may therefore be less than zero or greater than
         * the current {@link #size} less one. In that case, they indicate elements on a
         * hypothetical extension of the actual current {@link PolygonGrid}.
         * 
         * @param x the x-coordinate to convert
         * @param y the y-coordinate to convert
         * @return the {@link PointI} location of the {@link #element} whose shape contains
         *         the specified {@code x} and {@code y} world coordinates
         */
        PointI worldToGridCore(double x, double y) {

            final double width = element.bounds.width();
            final double height = element.bounds.height();

            // squares on edge: all coordinates are valid
            if (element.sides == 4 && element.orientation == PolygonOrientation.ON_EDGE)
                return new PointI((int) (x / width), (int) (y / height));

            // determine suitable comparison epsilon for assertions
            final double assertEpsilon = Math.min(width / 100.0, height / 100.0);
            final double width2 = width / 2.0, height2 = height / 2.0;
            int column, row, index;

            if (element.sides == 4) {
                assert(element.orientation != PolygonOrientation.ON_EDGE);

                if (gridShift.anyColumns()) {
                    // determine closest element
                    column = (int) (x / width2);
                    if (gridShift.isDownColumn(column)) {
                        index = 1;
                        row = (int) ((y - height2) / height);
                    } else {
                        index = 0;
                        row = (int) (y / height);
                    }

                    // offset to center of element
                    x -= (column + 1) * width2;
                    y -= (2 * row + index + 1) * height2;

                    assert(MathUtils.compare(x, -width2, assertEpsilon) >= 0);
                    assert(MathUtils.compare(x, 0, assertEpsilon) <= 0);

                    // check if we hit element or neighbor
                    if (Math.abs(y) <= x + width2)
                        return new PointI(column, row);
                    else {
                        final PointI offset = edgeNeighborOffsets[index][y < 0 ? 3 : 2];
                        return new PointI(column + offset.x, row + offset.y);
                    }
                } else {
                    // determine closest element
                    row = (int) (y / height2);
                    if (gridShift.isRightRow(row)) {
                        index = 1;
                        column = (int) ((x - width2) / width);
                    } else {
                        index = 0;
                        column = (int) (x / width);
                    }

                    // offset to center of element
                    x -= (2 * column + index + 1) * width2;
                    y -= (row + 1) * height2;

                    assert(MathUtils.compare(y, -height2, assertEpsilon) >= 0);
                    assert(MathUtils.compare(y, 0, assertEpsilon) <= 0);

                    // check if we hit element or neighbor
                    if (Math.abs(x) <= y + height2)
                        return new PointI(column, row);
                    else {
                        final PointI offset = edgeNeighborOffsets[index][x < 0 ? 3 : 0];
                        return new PointI(column + offset.x, row + offset.y);
                    }
                }
            } else {
                assert(element.sides == 6);
                final double width4 = width / 4.0, height4 = height / 4.0;

                if (gridShift.anyColumns()) {
                    // determine closest element
                    column = (int) (x / (3.0 * width4));
                    if (gridShift.isDownColumn(column)) {
                        index = 1;
                        row = (int) ((y - height2) / height);
                    } else {
                        index = 0;
                        row = (int) (y / height);
                    }

                    // offset to center of element
                    x -= (3 * column + 2) * width4;
                    y -= (2 * row + index + 1) * height2;

                    assert(MathUtils.compare(x, -width2, assertEpsilon) >= 0);
                    assert(MathUtils.compare(x, width4, assertEpsilon) <= 0);

                    // check if we hit element or neighbor
                    if (Math.abs(y) <= (x + width2) * SQRT_3)
                        return new PointI(column, row);
                    else {
                        final PointI offset = edgeNeighborOffsets[index][y < 0 ? 5 : 4];
                        return new PointI(column + offset.x, row + offset.y);
                    }
                } else {
                    // determine closest element
                    row = (int) (y / (3.0 * height4));
                    if (gridShift.isRightRow(row)) {
                        index = 1;
                        column = (int) ((x - width2) / width);
                    } else {
                        index = 0;
                        column = (int) (x / width);
                    }

                    // offset to center of element
                    x -= (2 * column + index + 1) * width2;
                    y -= (3 * row + 2) * height4;

                    assert(MathUtils.compare(y, -height2, assertEpsilon) >= 0);
                    assert(MathUtils.compare(y, height4, assertEpsilon) <= 0);

                    // check if we hit element or neighbor
                    if (Math.abs(x) <= (y + height2) * SQRT_3)
                        return new PointI(column, row);
                    else {
                        final PointI offset = edgeNeighborOffsets[index][x < 0 ? 5 : 0];
                        return new PointI(column + offset.x, row + offset.y);
                    }
                }
            }
        }
    }

    /**
     * Contains {@link PointI} arrays that define neighbor offsets for all
     * possible {@link #element} and {@link #gridShift} combinations.
     * The appropriate (sub-) arrays are copied to {@link PolygonGrid} instances as required.
     */
    private static class GeometryOffsets {
        /**
         * Offsets for squares lying on an edge, with no grid shift.
         * The first array applies if {@link RegularPolygon#vertexNeighbors} is {@code false},
         * the second array applies if it is {@code true}.
         */
        static final PointI[][][] SQUARE_EDGE_OFFSETS = {
            {
                {
                    // edge connections only
                    new PointI( 0, -1),  // North
                    new PointI( 1,  0),  // East
                    new PointI( 0,  1),  // South
                    new PointI(-1,  0),  // West
                }
            }, {
                {
                    // edge & vertex connections
                    new PointI( 0, -1),  // North
                    new PointI( 1, -1),  // North-East
                    new PointI( 1,  0),  // East
                    new PointI( 1,  1),  // South-East
                    new PointI( 0,  1),  // South
                    new PointI(-1,  1),  // South-West
                    new PointI(-1,  0),  // West
                    new PointI(-1, -1),  // North-West
                }
            }
        };

        /**
         * Offsets for squares standing on a vertex, with columns shifted up and down.
         * The first array applies if {@link RegularPolygon#vertexNeighbors} is {@code false},
         * the second array applies if it is {@code true}.
         */
        static final PointI[][][] SQUARE_VERTEX_COLUMN_OFFSETS = {
            {
                {
                    // edge only, column up
                    new PointI( 1, -1),  // North-East
                    new PointI( 1,  0),  // South-East
                    new PointI(-1,  0),  // South-West
                    new PointI(-1, -1),  // North-West
                }, {
                    // edge only, column down
                    new PointI( 1,  0),  // North-East
                    new PointI( 1,  1),  // South-East
                    new PointI(-1,  1),  // South-West
                    new PointI(-1,  0),  // North-West
                }
            }, {
                {
                    // edge & vertex, column up
                    new PointI( 0, -1),  // North
                    new PointI( 1, -1),  // North-East
                    new PointI( 2,  0),  // East
                    new PointI( 1,  0),  // South-East
                    new PointI( 0,  1),  // South
                    new PointI(-1,  0),  // South-West
                    new PointI(-2,  0),  // West
                    new PointI(-1, -1),  // North-West
                }, {
                    // edge & vertex, column down
                    new PointI( 0, -1),  // North
                    new PointI( 1,  0),  // North-East
                    new PointI( 2,  0),  // East
                    new PointI( 1,  1),  // South-East
                    new PointI( 0,  1),  // South
                    new PointI(-1,  1),  // South-West
                    new PointI(-2,  0),  // West
                    new PointI(-1,  0),  // North-West
                }
            }
        };

        /**
         * Offsets for squares standing on a vertex, with rows shifted left and right.
         * The first array applies if {@link RegularPolygon#vertexNeighbors} is {@code false},
         * the second array applies if it is {@code true}.
         */
        static final PointI[][][] SQUARE_VERTEX_ROW_OFFSETS = {
            {
                {
                    // edge only, row left
                    new PointI( 0, -1),  // North-East
                    new PointI( 0,  1),  // South-East
                    new PointI(-1,  1),  // South-West
                    new PointI(-1, -1),  // North-West
                }, {
                    // edge only, row right
                    new PointI( 1, -1),  // North-East
                    new PointI( 1,  1),  // South-East
                    new PointI( 0,  1),  // South-West
                    new PointI( 0, -1),  // North-West
                }
            }, {
                {
                    // edge & vertex, row left
                    new PointI( 0, -2),  // North
                    new PointI( 0, -1),  // North-East
                    new PointI( 1,  0),  // East
                    new PointI( 0,  1),  // South-East
                    new PointI( 0,  2),  // South
                    new PointI(-1,  1),  // South-West
                    new PointI(-1,  0),  // West
                    new PointI(-1, -1),  // North-West
                }, {
                    // edge & vertex, row right
                    new PointI( 0, -2),  // North
                    new PointI( 1, -1),  // North-East
                    new PointI( 1,  0),  // East
                    new PointI( 1,  1),  // South-East
                    new PointI( 0,  2),  // South
                    new PointI( 0,  1),  // South-West
                    new PointI(-1,  0),  // West
                    new PointI( 0, -1),  // North-West
                }
            }
        };

        /**
         * Offsets for hexagons lying on an edge, with columns shifted up and down.
         */
        static final PointI[][] HEXAGON_EDGE_OFFSETS = {
            {
                // column up
                new PointI( 0, -1),  // North
                new PointI( 1, -1),  // North-East
                new PointI( 1,  0),  // South-East
                new PointI( 0,  1),  // South
                new PointI(-1,  0),  // South-West
                new PointI(-1, -1),  // North-West
            }, {
                // column down
                new PointI( 0, -1),  // North
                new PointI( 1,  0),  // North-East
                new PointI( 1,  1),  // South-East
                new PointI( 0,  1),  // South
                new PointI(-1,  1),  // South-West
                new PointI(-1,  0),  // North-West
            }
        };

        /**
         * Offsets for hexagons standing on a vertex, with rows shifted left and right.
         */
        static final PointI[][] HEXAGON_VERTEX_OFFSETS = {
            {
                // row left
                new PointI( 0, -1),  // North-East
                new PointI( 1,  0),  // East
                new PointI( 0,  1),  // South-East
                new PointI(-1,  1),  // South-West
                new PointI(-1,  0),  // West
                new PointI(-1, -1),  // North-West
            }, {
                // row right
                new PointI( 1, -1),  // North-East
                new PointI( 1,  0),  // East
                new PointI( 1,  1),  // South-East
                new PointI( 0,  1),  // South-West
                new PointI(-1,  0),  // West
                new PointI( 0, -1),  // North-West
            }
        };
    }
}

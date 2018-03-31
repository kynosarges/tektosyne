package org.kynosarges.tektosyne.geometry;

/**
 * Specifies the shifting of rows or columns in a {@link PolygonGrid}.
 * Valid choices depend on the underlying {@link RegularPolygon}. Specifies how
 * even-numbered rows or columns are shifted, relative to odd-numbered ones. For
 * this purpose, counting starts at one for the {@link PolygonGrid} row or column
 * at index position zero, so the first row or column is considered odd-numbered.
 * 
 * @author Christoph Nahr
 * @version 6.1.0
 */
public enum PolygonGridShift {
    /**
     * Specifies that no rows or columns are shifted.
     * {@link #NONE} is the only valid choice for a square grid
     * with {@link PolygonOrientation#ON_EDGE} orientation.
     */
    NONE,

    /**
     * Specifies that even-numbered columns are shifted upward.
     * {@link #COLUMN_UP} and {@link #COLUMN_DOWN} are the only valid choices
     * for a hexagon grid with {@link PolygonOrientation#ON_EDGE} orientation.
     */
    COLUMN_UP,

    /**
     * Specifies that even-numbered columns are shifted downward.
     * {@link #COLUMN_UP} and {@link #COLUMN_DOWN} are the only valid choices
     * for a hexagon grid with {@link PolygonOrientation#ON_EDGE} orientation.
     */
    COLUMN_DOWN,

    /**
     * Specifies that even-numbered rows are shifted right.
     * {@link #ROW_RIGHT} and {@link #ROW_LEFT} are the only valid choices
     * for a hexagon grid with {@link PolygonOrientation#ON_VERTEX} orientation.
     */
    ROW_RIGHT,

    /**
     * Specifies that even-numbered rows are shifted left.
     * {@link #ROW_RIGHT} and {@link #ROW_LEFT} are the only valid choices
     * for a hexagon grid with {@link PolygonOrientation#ON_VERTEX} orientation.
     */
    ROW_LEFT;

    /**
     * Indicates whether the {@link PolygonGridShift} shifts any columns.
     * @return {@code true} for {@link #COLUMN_UP} or {@link #COLUMN_DOWN}, else {@code false}
     */
    public boolean anyColumns() {
        return (this == COLUMN_UP || this == COLUMN_DOWN);
    }

    /**
     * Indicates whether the {@link PolygonGridShift} shifts any rows.
     * @return {@code true} for {@link #ROW_LEFT} or {@link #ROW_RIGHT}, else {@code false}
     */
    public boolean anyRows() {
        return (this == ROW_LEFT || this == ROW_RIGHT);
    }

    /**
     * Determines whether the specified column is shifted down compared to its neighbors.
     * Always returns {@code false} if {@link #anyColumns} is {@code false}.
     * 
     * @param column the zero-based index of the column to test
     * @return {@code true} if {@code column} is shifted down, else {@code false}
     */
    public boolean isDownColumn(int column) {
        switch (this) {
            case COLUMN_UP:   return ((column % 2) == 0);
            case COLUMN_DOWN: return ((column % 2) != 0);
            default:          return false;
        }
    }

    /**
     * Determines whether the specified row is shifted left compared to its neighbors.
     * Always returns {@code false} if {@link #anyRows} is {@code false}.
     * 
     * @param row the zero-based index of the row to test
     * @return {@code true} if {@code row} is shifted left, else {@code false}
     */
    public boolean isLeftRow(int row) {
        switch (this) {
            case ROW_LEFT:  return ((row % 2) != 0);
            case ROW_RIGHT: return ((row % 2) == 0);
            default:        return false;
        }
    }

    /**
     * Determines whether the specified row is shifted right compared to its neighbors.
     * Always returns {@code false} if {@link #anyRows} is {@code false}.
     * 
     * @param row the zero-based index of the row to test
     * @return {@code true} if {@code row} is shifted right, else {@code false}
     */
    public boolean isRightRow(int row) {
        switch (this) {
            case ROW_LEFT:  return ((row % 2) == 0);
            case ROW_RIGHT: return ((row % 2) != 0);
            default:        return false;
        }
    }

    /**
     * Determines whether the specified column is shifted up compared to its neighbors.
     * Always returns {@code false} if {@link #anyColumns} is {@code false}.
     * 
     * @param column the zero-based index of the column to test
     * @return {@code true} if {@code column} is shifted up, else {@code false}
     */
    public boolean isUpColumn(int column) {
        switch (this) {
            case COLUMN_UP:   return ((column % 2) != 0);
            case COLUMN_DOWN: return ((column % 2) == 0);
            default:          return false;
        }
    }
}

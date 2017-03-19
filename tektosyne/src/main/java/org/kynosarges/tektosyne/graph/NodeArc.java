package org.kynosarges.tektosyne.graph;

/**
 * Defines the visibility of a {@link Graph} node from a source node.
 * Encapsulates data required by the {@link Visibility} line-of-sight algorithm.
 * The {@link Graph} node that defines the {@link NodeArc} and the source node that
 * defines the viewpoint are not stored in the {@link NodeArc}.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class NodeArc {

    // total or obscured tangential arc
    private double _start, _sweep;

    /**
     * The visible fraction of the {@link NodeArc}.
     */
    double _visibleFraction = 1;

    /**
     * The positive distance from the source node to the {@link NodeArc}.
     * Measured in world coordinates from the {@link Graph#getWorldLocation} result
     * for the source node to the nearest vertex of the {@link Graph#getWorldRegion}
     * result for the graph node that defines the tangential arc.
     */
    public final double distance;

    /**
     * Creates a {@link NodeArc} with the specified tangential arc and node distance.
     * @param start the starting angle of the tangential arc from the source node, measured
     *              clockwise in radians from the x-axis, assuming y-coordinates increase upward
     * @param sweep the positive sweep angle of the tangential arc from the source node,
     *              measured clockwise in radians, assuming y-coordinates increase upward
     * @param distance the positive distance from the source node to the graph node
     *                 that defines the tangential arc, in world coordinates
     * @throws IllegalArgumentException if {@code sweep} is less than zero or greater than 
     *         2 * {@link Math#PI}, or {@code distance} is equal to or less than zero
     */
    NodeArc(double start, double sweep, double distance) {
        if (sweep < 0)
            throw new IllegalArgumentException("sweep < 0");
        if (sweep > 2 * Math.PI)
            throw new IllegalArgumentException("sweep > 2 * Math.PI");
        if (distance <= 0)
            throw new IllegalArgumentException("distance <= 0");

        this._start = start;
        this._sweep = sweep;
        this.distance = distance;
    }

    /**
     * Creates a {@link NodeArc} with data copied from the specified instance.
     * @param arc the {@link NodeArc} whose tangential arc and node distance to copy
     * @throws NullPointerException if {@code arc} is {@code null}
     */
    NodeArc(NodeArc arc) {
        this._start = arc._start;
        this._sweep = arc._sweep;
        this.distance = arc.distance;
    }

    /**
     * Gets the starting angle of the {@link NodeArc}.
     * Measured clockwise in radians from the x-axis, assuming y-coordinates increase upward.
     * 
     * @return the starting angle of the {@link NodeArc}
     */
    public double start() {
        return _start;
    }

    /**
     * Gets the positive sweep angle of the {@link NodeArc}.
     * Measured clockwise in radians, assuming y-coordinates increase upward.
     * Never zero or negative in published {@link NodeArc} instances, but set to
     * zero for some instances internally used by the {@link Visibility} algorithm.
     * 
     * @return the positive sweep angle of the {@link NodeArc}
     */
    public double sweep() {
        return _sweep;
    }

    /**
     * Gets the visible fraction of the {@link NodeArc}.
     * Defaults to one for a newly created {@link NodeArc}. May be inaccurate if it is
     * smaller than the current {@link Visibility#threshold}, as the {@link Visibility}
     * algorithm stops updating a {@link NodeArc} as soon as it is considered obscured.
     * 
     * @return the fraction of the {@link #sweep} angle that remains unobscured, from zero to one
     */
    public double visibleFraction() {
        return _visibleFraction;
    }

    /**
     * Determines whether the current {@link NodeArc} completely obscures
     * the specified instance, or vice versa.
     * Takes the {@link #distance} of both instances into account to
     * determine which instance obscures the other, if any.
     * 
     * @param arc the other {@link NodeArc} to examine
     * @return zero if neither {@link NodeArc} completely obscures the other;
     *         less than zero if {@code arc} completely obscures this instance;
     *         greater than zero if this instance completely obscures {@code arc}
     * @throws NullPointerException if {@code arc} is {@code null}
     */
    public int isObscured(NodeArc arc) {

        // start of specified arc relative to current arc
        double relativeStart = arc._start - _start;
        if (relativeStart <= -Math.PI)
            relativeStart += 2 * Math.PI;
        else if (relativeStart > Math.PI)
            relativeStart -= 2 * Math.PI;

        assert(relativeStart > -Math.PI && relativeStart <= Math.PI);
        final double relativeSweep = relativeStart + arc._sweep;

        // specified arc completely obscures current arc
        if (relativeStart <= 0 && relativeSweep >= _sweep && arc.distance <= distance)
            return -1;

        // current arc completely obscures specified arc
        if (relativeStart >= 0 && relativeSweep <= _sweep && arc.distance > distance)
            return +1;

        return 0;
    }

    /**
     * Obscures the specified {@link NodeArc} with this instance.
     * May increase the {@link #start} angle and decrease the {@link #sweep}
     * angle of {@code arc} to reflect a smaller visible tangential arc.
     * 
     * @param arc the other {@link NodeArc} to obscure
     * @throws IllegalArgumentException if the {@link #distance} of this instance is
     *         equal to or greater than that of {@code arc}, so obscuring is impossible
     * @throws NullPointerException if {@code arc} is {@code null}
     */
    void obscure(NodeArc arc) {
        if (distance >= arc.distance)
            throw new IllegalArgumentException("distance >= arc.distance");

        // start of current arc, relative to specified start
        double relativeStart = this._start - arc._start;
        if (relativeStart <= -Math.PI)
            relativeStart += 2 * Math.PI;
        else if (relativeStart > Math.PI)
            relativeStart -= 2 * Math.PI;

        // sweep angle of current arc, relative to specified start
        final double relativeSweep = relativeStart + this._sweep;

        // check for completely distinct arcs
        if (relativeSweep <= 0 || arc._sweep <= relativeStart)
            return;

        // arc is completely obscured
        if (relativeStart <= 0 && relativeSweep >= arc._sweep) {
            arc._sweep = 0;
            return;
        }

        // start of arc is obscured
        if (relativeStart <= 0) {
            assert(relativeSweep < this._sweep);
            arc._start += relativeSweep;
            arc._sweep -= relativeSweep;
            return;
        }

        // end of arc is obscured
        if (relativeSweep >= this._sweep) {
            assert(relativeStart > 0);
            arc._sweep = relativeStart;
            return;
        }

        // middle of arc is obscured, keep greater visible part
        assert(relativeStart > 0 && relativeSweep < arc._sweep);
        if (relativeStart >= arc._sweep - relativeSweep)
            arc._sweep = relativeStart;
        else {
            arc._start += relativeSweep;
            arc._sweep -= relativeSweep;
        }
    }
}

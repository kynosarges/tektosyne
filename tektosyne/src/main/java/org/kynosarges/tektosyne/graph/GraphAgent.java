package org.kynosarges.tektosyne.graph;

/**
 * Represents an agent that can navigate a {@link Graph}.
 * Moves across direct node connections defined by the {@link Graph},
 * allowing the implementation of pathfinding algorithms.
 * 
 * @param <T> the type of all nodes in the {@link Graph}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public interface GraphAgent<T> {
    /**
     * Indicates whether the {@link GraphAgent} can enter {@link Graph} nodes
     * that exceed the maximum path cost.
     * If {@link #relaxedRange} is {@code false}, the maximum path cost for a movement is the
     * absolute upper limit that determines reachable nodes. The {@link GraphAgent} will not enter
     * any node whose total path cost exceeds this limit, as determined by {@link #getStepCost}.
     * <p>
     * If {@link #relaxedRange} is {@code true}, the {@link GraphAgent} can enter any node as the
     * <em>final</em> step of a movement path, regardless of the actual {@link #getStepCost} result
     * for that node, as long as the total path cost of all <em>previous</em> steps is less than
     * the maximum path cost.</p>
     * 
     * @return {@code true} if the {@link GraphAgent} may end its movement on a {@link Graph}
     *         node that exceeds the movement’s maximum path cost, else {@code false}
     */
    boolean relaxedRange();

    /**
     * Determines whether the {@link GraphAgent} can move from one specified
     * {@link Graph} node to another neighboring node.
     * Considers only whether the {@link GraphAgent} could move to {@code target} <em>if</em>
     * already placed on {@code source}, not whether the {@link GraphAgent} could move to either
     * {@code source} or {@code target} from its actual present {@link Graph} node, if any.
     * <p>
     * {@code canMakeStep} should succeed if the {@link GraphAgent} could occupy {@code target}
     * either temporarily or permanently. Use {@link #canOccupy} to impose additional restrictions
     * on stopping a movement at specific nodes.</p>
     * 
     * @param source the {@link Graph} node where the move starts
     * @param target the {@link Graph} node where the move ends,
     *               which must be a direct neighbor of {@code source}
     * @return {@code true} if the {@link GraphAgent} can move from {@code source} to {@code target},
     *         else {@code false}
     * @throws IllegalArgumentException if {@code source} and {@code target} are not
     *                                  direct neighbors in the {@link Graph}
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     */
    boolean canMakeStep(T source, T target);

    /**
     * Determines whether the {@link GraphAgent} can permanently occupy the specified
     * {@link Graph} node.
     * Determines whether the {@link GraphAgent} could stop at {@code target} and permanently
     * occupy that node, assuming {@code target} has already been reached during movement.
     * <p>
     * {@code canOccupy} should <em>not</em> consider whether the {@link GraphAgent} could
     * temporarily occupy {@code target} during a continuing multi-step movement, or whether
     * {@code target} could be reached at all from any other node.</p>
     * <p>
     * The default implementation simply returns {@code true}. Pathfinding algorithms always
     * specify a {@code target} node for which {@link #canMakeStep} has already succeeded,
     * so you should return {@code false} only if you wish to specifically prevent the
     * {@link GraphAgent} from ending a move on {@code target}.
     * 
     * @param target the {@link Graph} node to occupy
     * @return {@code true} if the {@link GraphAgent} can permanently occupy {@code target},
     *         else {@code false}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    default boolean canOccupy(T target) {
        if (target == null)
            throw new NullPointerException("target");

        return true;
    }

    /**
     * Determines the step cost for moving the {@link GraphAgent} from one specified
     * {@link Graph} node to another neighboring node.
     * Does not verify whether the {@link GraphAgent} can actually move from {@code source} to
     * {@code target}. Clients should call {@link #canMakeStep} to ensure this condition.
     * <p>
     * {@code getStepCost} should compute the movement cost under the assumption that the
     * {@link GraphAgent} was already placed on {@code source}. The cost of reaching {@code source}
     * from its actual present {@link Graph} node, if any, should be ignored.</p>
     * 
     * @param source the {@link Graph} node where the move starts
     * @param target the {@link Graph} node where the move ends,
     *               which must be a direct neighbor of {@code source}
     * @return the cost for moving the {@link GraphAgent} from {@code source} to {@code target},
     *         equal to or greater than the result of {@link Graph#getDistance} for these nodes
     * @throws IllegalArgumentException if {@code source} and {@code target} are not
     *                                  direct neighbors in the {@link Graph}
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     */
    double getStepCost(T source, T target);

    /**
     * Determines whether two specified {@link Graph} nodes are near enough to end a move.
     * The default implementation ignores {@code distance} and returns {@code true}
     * exactly if {@code source} and {@code target} are the same object.
     * <p>
     * More complex implementations might check for a maximum {@code distance}, or
     * examine application-specific properties of {@code node} and {@code target}.</p>
     * 
     * @param node the {@link Graph} node to examine
     * @param target the {@link Graph} node that is the target of the move
     * @param distance the {@link Graph#getDistance} result for {@code node} and {@code target},
     *                 if known, or a negative value to have {@link #isNearTarget} compute it
     * @return {@code true} if a move toward {@code target} should be considered complete
     *         once {@code node} is reached, else {@code false}
     * @throws NullPointerException if {@code node} or {@code target} is {@code null}
     */
    default boolean isNearTarget(T node, T target, double distance) {
        if (node == null)
            throw new NullPointerException("node");
        if (target == null)
            throw new NullPointerException("target");
        
        return (node == target);
    }
}

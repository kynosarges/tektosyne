package org.kynosarges.tektosyne.subdivision;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Maps the faces of a planar {@link Subdivision} to {@link VoronoiResults#generatorSites} indices.
 * Provides a mapping between all faces of a planar {@link Subdivision} and the
 * {@link VoronoiResults#generatorSites} indices of the {@link VoronoiResults}
 * from which the {@link Subdivision} was created.
 * <p>
 * The mapping is realized by a pair of arrays for optimal runtime efficiency. However,
 * {@link VoronoiMap} will not reflect changes to the underlying {@link Subdivision}.</p>
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class VoronoiMap implements SubdivisionMap<Integer> {

    // property backers
    private final Subdivision _source;
    private final VoronoiResults _target;

    // mapping arrays
    private final int[] _faceToSite;
    private final SubdivisionFace[] _siteToFace;

    /**
     * Creates a {@link VoronoiMap} from a new {@link Subdivision} to the specified {@link VoronoiResults}.
     * The bounded {@link Subdivision#faces} of the new {@link Subdivision} correspond
     * to the {@link VoronoiResults#voronoiRegions} of the specified {@code results}.
     * <p>
     * All {@link VoronoiResults#voronoiRegions} are calculated if necessary. After the
     * {@link VoronoiMap} has been created, you may call {@link VoronoiResults#clearVoronoiRegions}
     * if you do not otherwise require this data.</p>
     * 
     * @param results the {@link VoronoiResults} that defines all mapped
     *                {@link VoronoiResults#generatorSites} indices
     * @throws NullPointerException if {@code results} is {@code null}
     */
    public VoronoiMap(VoronoiResults results) {

        final PointD[][] regions = results.voronoiRegions();
        _source = Subdivision.fromPolygons(regions, 0);
        _target = results;

        assert(results.generatorSites.length == regions.length);
        assert(_source.faces().size() == regions.length + 1);

        _faceToSite = new int[regions.length];
        _siteToFace = new SubdivisionFace[regions.length];

        // determine equivalence of faces and voronoiRegions indices
        // (which are in turn equivalent to generatorSites indices)
        for (int i = 0; i < regions.length; i++) {
            final PointD[] polygon = regions[i];
            final SubdivisionFace face = _source.findFace(polygon, false);

            // bounded faces start at creation index one
            _faceToSite[face.key() - 1] = i;
            _siteToFace[i] = face;
        }
    }

    /**
     * Gets the {@link VoronoiResults} that defines all mapped {@link VoronoiResults#generatorSites} indices.
     * @return the {@link VoronoiResults} that defines all {@link VoronoiResults#generatorSites} indices
     *         returned and accepted by {@link #fromFace} and {@link #toFace}, respectively
     */
    @Override
    public VoronoiResults target() {
        return _target;
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
     * Converts the specified {@link SubdivisionFace} into the associated
     * {@link VoronoiResults#generatorSites} index.
     * 
     * @param face the {@link SubdivisionFace} to convert
     * @return the {@link VoronoiResults#generatorSites} index associated with {@code face}
     * @throws ArrayIndexOutOfBoundsException if {@code face} is the unbounded {@link SubdivisionFace},
     *                                        or its {@link SubdivisionFace#key} is greater than
     *                                        the number of {@link VoronoiResults#generatorSites}
     * @throws NullPointerException if {@code face} is {@code null}
     */
    @Override
    public Integer fromFace(SubdivisionFace face) {
        return _faceToSite[face.key() - 1];
    }

    /**
     * Converts the specified {@link VoronoiResults#generatorSites} index
     * into the associated {@link SubdivisionFace}.
     * 
     * @param value the {@link VoronoiResults#generatorSites} index to convert
     * @return the {@link SubdivisionFace} associated with {@code value}
     * @throws ArrayIndexOutOfBoundsException if {@code value} is not a valid
     *                                        {@link VoronoiResults#generatorSites} index
     */
    @Override
    public SubdivisionFace toFace(Integer value) {
        return _siteToFace[value];
    }

    /**
     * Converts the specified {@link VoronoiResults#generatorSites} index
     * into the associated {@link SubdivisionFace}.
     * Overload typed with a primitive {@code int} parameter to avoid {@link Integer} boxing.
     * 
     * @param value the {@link VoronoiResults#generatorSites} index to convert
     * @return the {@link SubdivisionFace} associated with {@code value}
     * @throws ArrayIndexOutOfBoundsException if {@code value} is not a valid
     *                                        {@link VoronoiResults#generatorSites} index
     */
    public SubdivisionFace toFace(int value) {
        return _siteToFace[value];
    }
}

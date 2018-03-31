package org.kynosarges.tektosyne.subdivision;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides unit tests for class {@link SubdivisionIntersection}.
 * @author Christoph Nahr
 * @version 6.2.0
 */
public class SubdivisionIntersectionTest {

    @Test
    public void voronoiWithPolygon() {
        for (int i = 0; i < 10; i++)
            testVoronoiWithPolygon();
    }

    private static void testVoronoiWithPolygon() {
        final VoronoiResults results = VoronoiTest.createRandom(10, -100, -100, 200, 200);
        final Subdivision voronoi = Subdivision.fromPolygons(results.voronoiRegions(), 0);
        voronoi.validate();

        final PointD[] points = GeoUtils.randomPolygon(-100, -100, 200, 200);
        final Subdivision polygon = Subdivision.fromPolygons(new PointD[][] { points }, 0.1);
        polygon.validate();

        final SubdivisionIntersection inter = Subdivision.intersection(voronoi, polygon);
        inter.division.validate();

        assertEquals(0, inter.faceKeys1[0]);
        assertEquals(0, inter.faceKeys2[0]);

        for (SubdivisionFace face: inter.division.faces().values()) {
            if (face.key() == 0) continue;

            final PointD[] facePoly = face.outerEdge().cyclePolygon();
            final PointD centroid = face.outerEdge().cycleCentroid();

            // randomPolygon may be partly concave which can result in
            // centroids that are not actually within the containing face
            if (GeoUtils.pointInPolygon(centroid, facePoly) != PolygonLocation.INSIDE)
                continue;

            // check mapping of intersection faces to all Voronoi faces
            final int voronoiKey = inter.faceKeys1[face.key()];
            final SubdivisionFace voronoiFace = voronoi.faces().get(voronoiKey);
            assertEquals(voronoiFace, voronoi.findFace(centroid));

            // check mapping of intersection faces to two polygon faces
            final PolygonLocation location = GeoUtils.pointInPolygon(centroid, points);
            if (location == PolygonLocation.OUTSIDE) {
                assertEquals(0, inter.faceKeys2[face.key()]);
            } else {
                // centroid should not hit any vertices or edges
                assertEquals(PolygonLocation.INSIDE, location);
                assertEquals(1, inter.faceKeys2[face.key()]);
            }
        }
    }

    @Test
    public void squareWithStar() {
        final Subdivision square = SubdivisionLinesTest.createSquare(false);
        final Subdivision star = SubdivisionLinesTest.createStar(false);

        final SubdivisionIntersection inter = Subdivision.intersection(square, star);
        inter.division.validate();

        assertEquals(5, inter.division.faces().size());
        assertEquals(16, inter.division.edges().size());
        assertEquals(5, inter.division.vertices().size());

        assertEquals(0, inter.faceKeys1[0]);
        for (int i = 1; i< 4; i++)
            assertEquals(1, inter.faceKeys1[i]);
        for (int i = 0; i< 4; i++)
            assertEquals(0, inter.faceKeys2[i]);

        // outer face of intersection must match single inner face of square
        final List<SubdivisionEdge> cycle = inter.division.faces().get(0).innerEdges();
        assertEquals(1, cycle.size());

        final PointD[] polygon = cycle.get(0).cyclePolygon();
        assertEquals(4, polygon.length);

        final SubdivisionFace face = square.findFace(polygon, true);
        assertEquals(face, square.faces().get(1));
    }

    @Test
    public void squareWithTriforce() {
        final Subdivision square = SubdivisionLinesTest.createSquare(false);
        final Subdivision triforce = SubdivisionLinesTest.createTriforce(false);

        final SubdivisionIntersection inter = Subdivision.intersection(square, triforce);
        inter.division.validate();

        assertEquals(4, inter.division.faces().size());
        assertEquals(18, inter.division.edges().size());
        assertEquals(8, inter.division.vertices().size());

        assertEquals(0, inter.faceKeys1[0]);
        assertEquals(0, inter.faceKeys2[0]);

        // outer face of intersection must match first face of triforce
        List<SubdivisionEdge> cycle = inter.division.faces().get(0).innerEdges();
        assertEquals(1, cycle.size());

        PointD[] polygon = cycle.get(0).cyclePolygon();
        assertEquals(3, polygon.length);

        SubdivisionFace face = triforce.findFace(polygon, true);
        assertEquals(face, triforce.faces().get(1));

        // inner cycle of outer triforce face of intersection must match square face
        SubdivisionFace interFace = inter.division.findFace(polygon, true);
        final int outerTriforceIndex = interFace.key();
        assertEquals(1, inter.faceKeys2[outerTriforceIndex]);

        cycle = interFace.innerEdges();
        assertEquals(1, cycle.size());

        polygon = cycle.get(0).cyclePolygon();
        assertEquals(4, polygon.length);

        face = square.findFace(polygon, true);
        assertEquals(face, square.faces().get(1));

        // intersection must contain second face of triforce
        polygon = triforce.faces().get(2).outerEdge().cyclePolygon();
        interFace = inter.division.findFace(polygon, true);
        assertNotNull(interFace);

        final int innerTriforceIndex = interFace.key();
        assertEquals(2, inter.faceKeys2[innerTriforceIndex]);

        // remaining triforce mapping indices point to first face
        for (int i = 1; i < inter.faceKeys2.length; i++)
            if (i != outerTriforceIndex && i != innerTriforceIndex)
                assertEquals(1, inter.faceKeys2[i]);

        // all square mapping indices point to first face, except outer triforce
        assertEquals(0, inter.faceKeys1[outerTriforceIndex]);
        for (int i = 1; i < inter.faceKeys1.length; i++)
            if (i != outerTriforceIndex)
                assertEquals(1, inter.faceKeys1[i]);
    }

    @Test
    public void triforceWithSquare() {
        final Subdivision square = SubdivisionLinesTest.createSquare(false);
        final Subdivision triforce = SubdivisionLinesTest.createTriforce(false);

        final SubdivisionIntersection inter = Subdivision.intersection(triforce, square);
        inter.division.validate();

        assertEquals(4, inter.division.faces().size());
        assertEquals(18, inter.division.edges().size());
        assertEquals(8, inter.division.vertices().size());

        assertEquals(0, inter.faceKeys1[0]);
        assertEquals(0, inter.faceKeys2[0]);

        // outer face of intersection must match first face of triforce
        List<SubdivisionEdge> cycle = inter.division.faces().get(0).innerEdges();
        assertEquals(1, cycle.size());

        PointD[] polygon = cycle.get(0).cyclePolygon();
        assertEquals(3, polygon.length);

        SubdivisionFace face = triforce.findFace(polygon, true);
        assertEquals(face, triforce.faces().get(1));

        // inner cycle of outer triforce face of intersection must match square face
        SubdivisionFace interFace = inter.division.findFace(polygon, true);
        final int outerTriforceIndex = interFace.key();
        assertEquals(1, inter.faceKeys1[outerTriforceIndex]);

        cycle = interFace.innerEdges();
        assertEquals(1, cycle.size());

        polygon = cycle.get(0).cyclePolygon();
        assertEquals(4, polygon.length);

        face = square.findFace(polygon, true);
        assertEquals(face, square.faces().get(1));

        // intersection must contain second face of triforce
        polygon = triforce.faces().get(2).outerEdge().cyclePolygon();
        interFace = inter.division.findFace(polygon, true);
        assertNotNull(interFace);

        final int innerTriforceIndex = interFace.key();
        assertEquals(2, inter.faceKeys1[innerTriforceIndex]);

        // remaining triforce mapping indices point to first face
        for (int i = 1; i < inter.faceKeys1.length; i++)
            if (i != outerTriforceIndex && i != innerTriforceIndex)
                assertEquals(1, inter.faceKeys1[i]);

        // all square mapping indices point to first face, except outer triforce
        assertEquals(0, inter.faceKeys2[outerTriforceIndex]);
        for (int i = 1; i < inter.faceKeys2.length; i++)
            if (i != outerTriforceIndex)
                assertEquals(1, inter.faceKeys2[i]);
    }
}

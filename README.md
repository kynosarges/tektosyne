[![Build Status](https://travis-ci.org/kynosarges/tektosyne.svg?branch=master)](https://travis-ci.org/kynosarges/tektosyne)
[![codecov](https://codecov.io/gh/kynosarges/tektosyne/branch/master/graph/badge.svg)](https://codecov.io/gh/kynosarges/tektosyne)
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://raw.githubusercontent.com/kynosarges/tektosyne/master/LICENSE.txt)



# Tektosyne

The Tektosyne Library for Java provides algorithms for computational geometry and graph-based pathfinding,
along with supporting mathematical utilities and specialized collections.

## Overview

The following list gives a summary of Tektosyne’s main features.
See the [User’s Guide](https://github.com/kynosarges/tektosyne/raw/master/docs/TektosyneGuide.pdf) and the [Javadoc](https://kynosarges.github.io/tektosyne/javadoc/) class reference for more details.

* Geometric primitives: points (doubling as vectors), sizes, line segments, and rectangles, all available with int and double coordinates

* Geometric algorithms: convex hull, point in polygon, intersections of two or more line segments, point location relative to line segments and rectangles, etc.

* Lexicographic point ordering preferring x- or y-coordinates, with efficient nearest point and range search in sorted standard collections

* Graph algorithms: A* pathfinding, path coverage, flood fill, line of sight, all performed on interfaces that can be implemented by arbitrary concrete geometric structures

* Planar subdivision represented as doubly-connected edge list (DCEL), with support for graph algorithms, dynamic modification, and fast point location

* Regular polygons and rectangular grids of squares or hexagons, with support for graph algorithms, mapping between grid & display coordinates, and conversion to DCEL subdivision

* Voronoi diagram and Delaunay triangulation, with conversion to DCEL subdivision and consequently support for graph algorithms

* Collections: generic linked list and generic quadrant tree, both with exposed node structure

* Mathematical helper methods, including a library of Fortran 90 functions


### Samples

The repository includes a [JavaFX demo application](https://github.com/kynosarges/tektosyne/raw/master/tektosyne-demo/demo-jar/tektosyne-demo.jar) that allows you to interactively explore many of these algorithms.
Screenshots of two demo dialogs appear below.

* Planar Subdivision Test: visualization and interactive manipulation of a randomly generated planar subdivision. The highlighted half-edge and vertex are nearest the (hidden) mouse cursor.

* Graph Algorithms Test: shows A* pathfinding along the edges of a Delaunay triangulation (yellow dashes) of a random Voronoi diagram whose regions were assigned random step costs.

![alt demo](https://raw.githubusercontent.com/kynosarges/tektosyne/master/docs/img/TektosyneDemo.png)

### System Requirements

The Tektosyne library itself requires only the Java SE 8 Compact 1 profile.

The included GUI demo application requires Oracle JRE 8 Update 66 with JavaFX 8 or later, or an equivalent Java runtime environment.
Windows users should consult Oracle Java on Windows to avoid Oracle’s unfortunate default Windows JRE. Please see the following for more information:

## Dependencies

### Gradle
```
repositories {
    jcenter()
}

dependencies {
    compile group: 'org.kynosarges', name: 'tektosyne', version: '6.0.0'
}
```

### Maven
```
  <repositories>
    <repository>
      <id>jcenter</id>
      <url>http://jcenter.bintray.com </url>
      <snapshots>
        <enabled>true</enabled>
        <updatePolicy>never</updatePolicy>
        <checksumPolicy>warn</checksumPolicy>
      </snapshots>
      <releases>
        <enabled>true</enabled>
        <checksumPolicy>warn</checksumPolicy>
      </releases>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>org.kynosarges</groupId>
      <artifactId>tektosyne</artifactId>
      <version>6.0.0</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
```

## License


Tektosyne is © 2002–2017 by [Christoph Nahr](http://kynosarges.org/index.html) but available for free under the MIT license.

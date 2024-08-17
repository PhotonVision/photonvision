# Contour Filtering and Grouping

Contours that make it past thresholding are filtered and grouped so that only likely targets remain.

## Filtering Options

### Reflective

Contours can be filtered by area, width/height ratio, "fullness", and "speckle rejection" percentage.

Area filtering adjusts the percentage of overall image area that contours are allowed to occupy. The area of valid contours is shown in the "target info" card on the right.

Ratio adjusts the width to height ratio of allowable contours. For example, a width to height filtering range of \[2, 3\] would allow targets that are 250 x 100 pixels in size through.

Fullness is a measurement of the ratio between the contour's area and the area of its bounding rectangle. This can be used to reject contours that are for example solid blobs.

Finally, speckle rejection is an algorithm that can discard contours whose area are below a certain percentage of the average area of all visible contours. This might be useful in rejecting stray lights or image noise.

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/AreaRatioFullness.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```

### Colored Shape

The contours tab has new options for specifying the properties of your colored shape.  The target shape types are:

- Circle - No edges
- Triangle - 3 edges
- Quadrilateral - 4 edges
- Polygon - Any number of edges

```{image} images/triangle.png
:alt: Dropdown to select the colored shape pipeline type.
:width: 600
```

Only the settings used for the current target shape are available.

- Shape Simplification - This is the only setting available for polygon, triangle, and quadrilateral target shapes.  If you are having issues with edges being "noisy" or "unclean", adjust this setting to be higher (>75).  This high setting helps prevent imperfections in the edge from being counted as a separate edge.
- Circle Match Distance - How close the centroid of a contour must be to the center of the circle in order for them to be matched.  This value is usually pretty small (\<25) as you usually only want to identify circles that are nearly centered in the contour.
- Radius - Percentage of the frame that the radius of the circle represents.
- Max Canny Threshold - This sets the amount of change between pixels needed to be considered an edge. The smaller it is, the more false circles may be detected. Circles with more points along their ring having high contrast values will be returned first.
- Circle Accuracy - This determines how perfect the circle contour must be in order to be considered a circle.  Low values (\<40) are required to detect things that aren't perfect circles.

```{image} images/pumpkin.png
:alt: Dropdown to select the colored shape pipeline type.
:width: 600
```

## Contour Grouping and Sorting

These options change how contours are grouped together and sorted. Target grouping can pair adjacent contours, such as the targets found in 2019. Target intersection defines where the targets would intersect if you extended them infinitely, for example, to only group targets tipped "towards" each other in 2019.

Finally, target sort defines how targets are ranked, from "best" to "worst." The available options are:

- Largest
- Smallest
- Highest (towards the top of the image)
- Lowest
- Rightmost (Best target on the right, worst on left)
- Leftmost
- Centermost

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/groupingSorting.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```

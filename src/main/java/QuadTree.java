import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A tree where each parent has 4 children: upper-left, upper-right, lower-left, lower-right tiles.
 *
 * @author Moo Jin Kim
 */
public class QuadTree {
    private class Node {
        private String name;
        private int depth;
        private Node ul, ur, ll, lr;  // upper-left, upper-right, lower-left, lower-right children
        private double ULLON, ULLAT, LRLON, LRLAT;  // ULLONgitude, ULLATitude, etc.

        Node(String name, int depth, double ULLON, double ULLAT, double LRLON, double LRLAT) {
            this.name = name;
            this.depth = depth;
            this.ULLON = ULLON;
            this.ULLAT = ULLAT;
            this.LRLON = LRLON;
            this.LRLAT = LRLAT;
        }

        /** Returns whether or not a tile intersects the query box. */
        public boolean intersectsQuery(double queryULLON, double queryULLAT,
                                       double queryLRLON, double queryLRLAT) {
            /* If left edge of tile is to the right of right edge of query box, no overlap */
            if (this.ULLON > queryLRLON) {
                return false;
            }
            /* If right edge of tile is to the left of left edge of query box, no overlap */
            if (this.LRLON < queryULLON) {
                return false;
            }
            /* If bottom edge of tile is above top edge of query box, no overlap */
            if (this.LRLAT > queryULLAT) {
                return false;
            }
            /* If top edge of tile is below bottom edge of query box, no overlap */
            if (this.ULLAT < queryLRLAT) {
                return false;
            }
            return true;
        }
    }

    private Node root;
    /* Longitudinal distance per pixel: determines the depth of tiles returned to Rasterer */
    private final double lonDPP_0, lonDPP_1, lonDPP_2, lonDPP_3, lonDPP_4, lonDPP_5,
            lonDPP_6, lonDPP_7;

    /** Constructor: Store all tiles in tree. */
    QuadTree() {
        root = new Node("", 0, MapServer.ROOT_ULLON, MapServer.ROOT_ULLAT,
                        MapServer.ROOT_LRLON, MapServer.ROOT_LRLAT);
        lonDPP_0 = getLonDPP(MapServer.ROOT_ULLON, MapServer.ROOT_LRLON, MapServer.TILE_SIZE);
        lonDPP_1 = lonDPP_0 / 2;
        lonDPP_2 = lonDPP_1 / 2;
        lonDPP_3 = lonDPP_2 / 2;
        lonDPP_4 = lonDPP_3 / 2;
        lonDPP_5 = lonDPP_4 / 2;
        lonDPP_6 = lonDPP_5 / 2;
        lonDPP_7 = lonDPP_6 / 2;
        insertAll(root, 0);
    }


    /** Returns the LonDPP of a box. */
    public double getLonDPP(double ULLON, double LRLON, double width) {
        return (LRLON - ULLON) / width;
    }

    /** Inserts all tiles into tree recursively. */
    private void insertAll(Node n, int depth) {
        if (depth == 7) {
            return;
        }

        double newULLON, newULLAT, newLRLON, newLRLAT;

        /* 1: Upper left tile: same ULLON & ULLAT, diff LRLON & LRLAT */
        newULLON = n.ULLON;
        newULLAT = n.ULLAT;
        newLRLON = ((n.LRLON - n.ULLON) / 2) + n.ULLON;
        newLRLAT = ((n.ULLAT - n.LRLAT) / 2) + n.LRLAT;  // order switched bc ULLAT > LRLAT
        n.ul = new Node(n.name + "1", depth + 1, newULLON, newULLAT, newLRLON, newLRLAT);
        insertAll(n.ul, depth + 1);

        /* 2: Upper right tile: same ULLAT & LRLON, diff ULLON & LRLAT */
        newULLON = ((n.LRLON - n.ULLON) / 2) + n.ULLON;
        newULLAT = n.ULLAT;
        newLRLON = n.LRLON;
        newLRLAT = ((n.ULLAT - n.LRLAT) / 2) + n.LRLAT;
        n.ur = new Node(n.name + "2", depth + 1, newULLON, newULLAT, newLRLON, newLRLAT);
        insertAll(n.ur, depth + 1);

        /* 3: Lower left tile: same ULLON & LRLAT, diff ULLAT & LRLON */
        newULLON = n.ULLON;
        newULLAT = ((n.ULLAT - n.LRLAT) / 2) + n.LRLAT;
        newLRLON = ((n.LRLON - n.ULLON) / 2) + n.ULLON;
        newLRLAT = n.LRLAT;
        n.ll = new Node(n.name + "3", depth + 1, newULLON, newULLAT, newLRLON, newLRLAT);
        insertAll(n.ll, depth + 1);

        /* 4: Lower right tile: same LRLON & LRLAT, diff ULLON & ULLAT */
        newULLON = ((n.LRLON - n.ULLON) / 2) + n.ULLON;
        newULLAT = ((n.ULLAT - n.LRLAT) / 2) + n.LRLAT;
        newLRLON = n.LRLON;
        newLRLAT = n.LRLAT;
        n.lr = new Node(n.name + "4", depth + 1, newULLON, newULLAT, newLRLON, newLRLAT);
        insertAll(n.lr, depth + 1);
    }

    /** Returns the depth that the tiles returned to Rasterer should be obtained from. */
    public int getTargetDepth(double query_lonDPP) {
        /* Return the first depth whose lonDPP <= query_lonDPP. */
        if (lonDPP_0 <= query_lonDPP) {
            return 0;
        } else if (lonDPP_1 <= query_lonDPP) {
            return 1;
        } else if (lonDPP_2 <= query_lonDPP) {
            return 2;
        } else if (lonDPP_3 <= query_lonDPP) {
            return 3;
        } else if (lonDPP_4 <= query_lonDPP) {
            return 4;
        } else if (lonDPP_5 <= query_lonDPP) {
            return 5;
        } else if (lonDPP_6 <= query_lonDPP) {
            return 6;
        } else {
            return 7;
        }
    }

    /** Recursively returns list of tiles that intersect query box and have small enough LonDPP. */
    private ArrayList<Node> getIntersectingTiles(Node n, double queryULLON, double queryULLAT,
                                                  double queryLRLON, double queryLRLAT,
                                                  int targetDepth) {
        /* If the tile does not intersect the query box, return null. */
        if (!n.intersectsQuery(queryULLON, queryULLAT, queryLRLON, queryLRLAT)) {
            return null;
        }

        /* If the tile intersects query box and is at the target depth, return just the tile. */
        if (n.depth == targetDepth) {
            ArrayList<Node> ret = new ArrayList<>();
            ret.add(n);
            return ret;
        } else {
            /* If the tile intersects query box but is not at the target depth,
               collect lists of tiles from all 4 children. */
            assert n.depth < targetDepth;
            ArrayList<Node> ulList = getIntersectingTiles(n.ul, queryULLON, queryULLAT,
                    queryLRLON, queryLRLAT, targetDepth);
            ArrayList<Node> urList = getIntersectingTiles(n.ur, queryULLON, queryULLAT,
                    queryLRLON, queryLRLAT, targetDepth);
            ArrayList<Node> llList = getIntersectingTiles(n.ll, queryULLON, queryULLAT,
                    queryLRLON, queryLRLAT, targetDepth);
            ArrayList<Node> lrList = getIntersectingTiles(n.lr, queryULLON, queryULLAT,
                    queryLRLON, queryLRLAT, targetDepth);

            /* Merge all 4 lists and return the result. */
            ArrayList<Node> ret = new ArrayList<>();
            if (ulList != null) {
                ret.addAll(ulList);
            }
            if (urList != null) {
                ret.addAll(urList);
            }
            if (llList != null) {
                ret.addAll(llList);
            }
            if (lrList != null) {
                ret.addAll(lrList);
            }
            return ret;
        }
    }

    /** Returns 2D array of tiles that intersect query box. */
    private Node[][] getTileArray(ArrayList<Node> listOfTiles) {

        /* Turn the list of tiles into a map of unique ULLAT -> list of tiles on same row.
           Using a TreeMap to sort rows in order of increasing ULLAT. */
        Map<Double, ArrayList<Node>> latToRow = new TreeMap<>();
        for (Node n : listOfTiles) {
            /* If Node's ULLAT was not found, add a list (representing tile row) to latToRow. */
            if (!latToRow.containsKey(n.ULLAT)) {
                latToRow.put(n.ULLAT, new ArrayList<>());
                latToRow.get(n.ULLAT).add(n);
            } else {
                /* If Node's ULLAT was found, insert Node into the existing list while
                   maintaining order of increasing ULLON. */
                int existingListSize = latToRow.get(n.ULLAT).size();
                int i;
                for (i = 0; i < existingListSize; i++) {
                    if (n.ULLON < latToRow.get(n.ULLAT).get(i).ULLON) {
                        break;
                    }
                }
                latToRow.get(n.ULLAT).add(i, n);
            }
        }

        /* Set up 2D String array. */
        int numRows = latToRow.size();
        int numCols = 0;
        for (ArrayList<Node> row : latToRow.values()) {
            numCols = row.size();
            break;
        }
        Node[][] ret = new Node[numRows][numCols];

        /* Turn the map into a 2D String array. Since tiles with largest ULLAT must be on the top,
           insert in "backward" order into array: bottom to top. */
        int i = numRows - 1;
        for (ArrayList<Node> row : latToRow.values()) {
            for (int k = 0; k < numCols; k++) {
                ret[i][k] = row.get(k);
            }
            i--;
        }
        return ret;
    }

    /** Returns String in the form "img/" + num + ".png". */
    private String stringify(String num) {
        return "img/" + num + ".png";
    }

    /** Converts 2D array of Nodes into 2D array of tile names. */
    private String[][] nodesToString(Node[][] tileArray) {
        int numRows = 0, numCols = 0;
        numRows = tileArray.length;
        numCols = tileArray[0].length;
        String[][] ret = new String[numRows][numCols];
        for (int i = 0; i < numRows; i++) {
            for (int k = 0; k < numCols; k++) {
                ret[i][k] = stringify(tileArray[i][k].name);
            }
        }
        return ret;
    }

    /** Returns array containing raster_ul_lon, raster_ul_lat, raster_lr_lon, raster_lr_lat. */
    private double[] getRasterCoords(Node[][] tileArray) {
        double[] ret = new double[4];
        int numRows = 0, numCols = 0;
        numRows = tileArray.length;
        numCols = tileArray[0].length;

        /* Top left corner of query box corresponds to very first item in tileArray. */
        ret[0] = tileArray[0][0].ULLON;
        ret[1] = tileArray[0][0].ULLAT;

        /* Bottom right corner of query box corresponds to very last item in tileArray. */
        ret[2] = tileArray[numRows - 1][numCols - 1].LRLON;
        ret[3] = tileArray[numRows - 1][numCols - 1].LRLAT;

        return ret;
    }

    /** Returns map of results needed to serve query in Rasterer. */
    public Map<String, Object> serveQuery(double queryULLON, double queryULLAT,
                                          double queryLRLON, double queryLRLAT,
                                          int targetDepth) {
        /* Get 2D array of tiles needed to serve query. */
        ArrayList<Node> list = getIntersectingTiles(root, queryULLON, queryULLAT,
                queryLRLON, queryLRLAT, targetDepth);
        assert list.size() != 0;
        Node[][] tileArray = getTileArray(list);

        /* Store query response in map of results. */
        Map<String, Object> results = new HashMap<>();
        double[] rasterCoords = getRasterCoords(tileArray);
        results.put("render_grid", nodesToString(tileArray));
        results.put("raster_ul_lon", rasterCoords[0]);
        results.put("raster_ul_lat", rasterCoords[1]);
        results.put("raster_lr_lon", rasterCoords[2]);
        results.put("raster_lr_lat", rasterCoords[3]);
        results.put("depth", tileArray[0][0].depth);
        results.put("query_success", true);

        return results;
    }
}

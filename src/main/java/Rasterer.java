import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    // Recommended: QuadTree instance variable. You'll need to make
    //              your own QuadTree since there is no built-in quadtree in Java.
    QuadTree qt;

    /** imgRoot is the name of the directory containing the images.
     *  You may not actually need this for your class. */
    public Rasterer(String imgRoot) {
        qt = new QuadTree();
    }

    /** Determines whether the query coordinates are valid (at least intersects image data set). */
    private boolean isValidQuery(double queryULLON, double queryULLAT,
                          double queryLRLON, double queryLRLAT) {

        /* If left edge of query box is to the right of right edge of data set, no overlap */
        if (queryULLON > MapServer.ROOT_LRLON) {
            return false;
        }
        /* If right edge of query box is to the left of left edge of data set, no overlap */
        if (queryLRLON < MapServer.ROOT_ULLON) {
            return false;
        }
        /* If bottom edge of query box is above top edge of data set, no overlap */
        if (queryLRLAT > MapServer.ROOT_ULLAT) {
            return false;
        }
        /* If top edge of query box is below bottom edge of data set, no overlap */
        if (queryULLAT < MapServer.ROOT_LRLAT) {
            return false;
        }
        /* If query box coordinates are nonsense, return false. */
        if (queryULLON > queryLRLON || queryLRLAT > queryULLAT) {
            return false;
        }
        return true;
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     * <p>
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *               See REQUIRED_RASTER_REQUEST_PARAMS in MapServer.java.
     *
     * @return A map of results for the front end as specified:
     * "render_grid"   -> String[][], the files to display
     * "raster_ul_lon" -> Number, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Number, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Number, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Number, the bounding lower right latitude of the rastered image <br>
     * "depth"         -> Number, the 1-indexed quadtree depth of the nodes of the rastered image.
     *                    Can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" -> Boolean, whether the query was able to successfully complete. Don't
     *                    forget to set this to true! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        Map<String, Object> results;

        /* Extract query box coordinates. */
        double queryULLON, queryULLAT, queryLRLON, queryLRLAT;
        queryULLON = params.get("ullon");
        queryULLAT = params.get("ullat");
        queryLRLON = params.get("lrlon");
        queryLRLAT = params.get("lrlat");

        /* Verify that query box is valid. */
        if (!isValidQuery(queryULLON, queryULLAT, queryLRLON, queryLRLAT)) {
            results = new HashMap<>();
            results.put("query_success", false);
            return results;
        }

        /* Serve query. */
        double queryLONDPP = qt.getLonDPP(queryULLON, queryLRLON, params.get("w"));
        int targetDepth = qt.getTargetDepth(queryLONDPP);
        results = qt.serveQuery(queryULLON, queryULLAT, queryLRLON, queryLRLAT,
                targetDepth);

        return results;
    }

}

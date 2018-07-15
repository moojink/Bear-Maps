import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    private class Node {
        private long id;
        private double lon, lat;
        private String name;  // optional
        private double f, g, h;  // f(n) = g(n) + h(n), where g(n) == shortest known distance
                           // to Node, and h(n) == heuristic (straight line distance to dest).
                           // Lower f --> higher priority.
        private boolean marked;  // true if Node was visited in findShortestPath()
        Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            this.name = "";
            this.f = Double.MAX_VALUE;
            this.g = Double.MAX_VALUE;
            this.h = Double.MAX_VALUE;
            this.marked = false;
        }
    }

    private class NodeComparator implements Comparator<Node> {
        public int compare(Node n1, Node n2) {
            if (n1.f < n2.f) {
                return -1;
            } else if (n1.f > n2.f) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private HashMap<Long, Node> nodes;  // all nodes in our graph
    private HashMap<Long, HashSet<Long>> adjLists;  // adjacency lists for each Node
    private Node lastNode;  // pointer to the last node added to the Graph
    private ArrayList<Long> way;  // temporary path that may become a series of edges in graph
    private boolean isValidWay;              // if true, adds the Way edges to the graph
    private Trie nameTrie;  // a reTRIEval data structure for name Autocomplete functionality
    private HashMap<String, ArrayList<Node>> nameToNodes;  // used to speedup getLocations() runtime

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            nodes = new HashMap<>();
            adjLists = new HashMap<>();
            lastNode = null;
            way = null;
            isValidWay = false;
            nameTrie = new Trie();
            nameToNodes = new HashMap<>();
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputFile, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        trackNodeNames();
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /** Keeps track of all nodes with names. This list will be used by getLocations(). */
    private void trackNodeNames() {
        for (HashMap.Entry<Long, Node> entry : nodes.entrySet()) {
            Node n = entry.getValue();
            if (!n.name.equals("")) {
                /* Add the cleaned name (for efficiency purposes). */
                String cleanName = cleanString(n.name);
                if (nameToNodes.get(cleanName) == null) {
                    nameToNodes.put(cleanName, new ArrayList<>());
                }

                nameToNodes.get(cleanName).add(n);
            }
        }
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        ArrayList<Long> toDelete = new ArrayList<>();
        Iterator<HashMap.Entry<Long, HashSet<Long>>> iter = adjLists.entrySet().iterator();
        while (iter.hasNext()) {
            HashMap.Entry<Long, HashSet<Long>> entry = iter.next();
            /* If the node is not adjacent to other nodes, remove it from the graph. */
            if (entry.getValue().isEmpty()) {
                /* Mark a Node as ready to delete. */
                toDelete.add(entry.getKey());
            }
        }

        for (long id : toDelete) {
            /* Remove the Nodes from both adjLists and nodes. */
            adjLists.remove(id);
            nodes.remove(id);
        }
    }

    /** Returns an iterable of all vertex IDs in the graph. */
    Iterable<Long> vertices() {
        return nodes.keySet();
    }

    /** Returns ids of all vertices adjacent to v. */
    Iterable<Long> adjacent(long v) {
        return adjLists.get(v);
    }

    /** Returns the Euclidean distance between vertices v and w, where Euclidean distance
     *  is defined as sqrt( (lonV - lonV)^2 + (latV - latV)^2 ). */
    double distance(long v, long w) {
        Node nodeV = nodes.get(v);
        Node nodeW = nodes.get(w);
        return Math.sqrt(Math.pow(nodeV.lon - nodeW.lon, 2) + Math.pow(nodeV.lat - nodeW.lat, 2));
    }

    /** Returns the vertex id closest to the given longitude and latitude. */
    long closest(double lon, double lat) {
        double distance = Double.MAX_VALUE;
        long closestID = 0;
        for (HashMap.Entry<Long, Node> entry : nodes.entrySet()) {
            double nodeLon = entry.getValue().lon;
            double nodeLat = entry.getValue().lat;
            double temp = Math.sqrt(Math.pow(nodeLon - lon, 2) + Math.pow(nodeLat - lat, 2));
            if (temp < distance) {
                distance = temp;
                closestID = entry.getValue().id;
            }
        }
        return closestID;
    }

    /** Longitude of vertex v. */
    double lon(long v) {
        return nodes.get(v).lon;
    }

    /** Latitude of vertex v. */
    double lat(long v) {
        return nodes.get(v).lat;
    }

    /** Adds a Node to the graph. */
    public void addNode(long id, double lon, double lat) {
        Node n = new Node(id, lon, lat);
        nodes.put(id, n);
        adjLists.put(id, new HashSet<>());
        /* Keep track of the last node added to the graph. */
        lastNode = n;
    }

    /** Adds a name to the last Node added to the graph. */
    public void addNameToNode(String name) {
        lastNode.name = name;
    }

    /**
     * Adds a Node to the Way.
     * @param id The node's ID.
     */
    public void addWayNode(long id) {
        /* Create a new Way if it doesn't exist. */
        if (way == null) {
            way = new ArrayList<>();
        }
        way.add(id);
    }

    /** Sets the isValidWay boolean variable. */
    public void setValidWay(boolean val) {
        isValidWay = val;
    }

    /** Adds edges stored in the Way to the graph if Way is valid, and resets Way. */
    public void processWay() {
        if (isValidWay && way.size() >= 2) {
            /* For 2 adjacent Nodes in the Way, we need to modify adjLists for both. */
            for (int i = 0; i < way.size() - 1; i++) {  // stop after 2nd to last Node
                int k = i + 1;  // the next Node
                long currentID = way.get(i);
                long nextID = way.get(k);
                adjLists.get(currentID).add(nextID);
                adjLists.get(nextID).add(currentID);
            }
        }
        /* Reset Way. */
        way = null;
        isValidWay = false;
    }

    /** Returns true if the two Nodes have same lon and lat. */
    private boolean equals(Node n1, Node n2) {
        if (n1.lon == n2.lon && n1.lat == n2.lat) {
            return true;
        }
        return false;
    }

    /** Returns a copy of a Node, not just a pointer. */
    public Node returnCopy(Node n) {
        Node ret = new Node(n.id, n.lon, n.lat);
        ret.name = n.name;
        ret.f = n.f;
        ret.g = n.g;
        ret.h = n.h;
        return ret;
    }

    /** Using the A* algorithm, finds shortest path b/t 2 Nodes closest to given arguments. */
    public LinkedList<Long> findShortestPath(double stlon, double stlat,
                                             double destlon, double destlat) {
        /* Initialize necessary data structures. */
        PriorityQueue<Node> pq = new PriorityQueue<>(1, new NodeComparator());
        ArrayList<Long> markedNodes = new ArrayList<>();
        HashMap<Long, Double> distanceToNode = new HashMap<>();
        HashMap<Long, Long> parentOfNode = new HashMap<>();

        Long startID = closest(stlon, stlat);
        Long destID = closest(destlon, destlat);
        Node start = returnCopy(nodes.get(startID));
        Node dest = nodes.get(destID);

        start.g = 0;
        start.h = distance(startID, destID);
        start.f = start.g + start.h;

        distanceToNode.put(startID, 0.0);
        markedNodes.add(startID);
        Node n = start;

        /* Branch out from the source. */
        while (!equals(n, dest)) {
            /* If the Node is already visited, don't revisit. */
            if (n.marked) {
                n = pq.poll();
                continue;
            }

            /* Mark the current Node as visited so that we don't revisit it later. */
            n.marked = true;

            Iterable<Long> adjacentNodes = adjacent(n.id);
            for (Long l : adjacentNodes) {
                double tempDistance = n.g + distance(n.id, l);
                if (!distanceToNode.containsKey(l)) {  // add shortest distance to Node
                    distanceToNode.put(l, tempDistance);
                } else if (tempDistance < distanceToNode.get(l)) {  // overwrite shortest distance
                    distanceToNode.put(l, tempDistance);
                } else {
                    continue;
                }

                Node nodeToAdd = returnCopy(nodes.get(l));
                nodeToAdd.g = tempDistance;
                nodeToAdd.h = distance(nodeToAdd.id, dest.id);
                nodeToAdd.f = nodeToAdd.g + nodeToAdd.h;
                parentOfNode.put(nodeToAdd.id, n.id);
                pq.add(nodeToAdd);
            }
            /* Get the next Node with the highest priority. */
            n = pq.poll();
        }

        /* Now we have Node n pointing to the destination Node. Trace back through its
           parent, its parent's parent, etc. to get the shortest path. */
        LinkedList<Long> ret = new LinkedList<>();
        ret.addFirst(n.id);
        Long parent = parentOfNode.get(n.id);
        while (parent != null) {
            ret.addFirst(parent);
            parent = parentOfNode.get(parent);
        }
        return ret;
    }

    /** Adds a name to the Graph's Trie so that query Autocomplete works correctly. */
    public void addNameToTrie(String name) {
        nameTrie.insertName(cleanString(name), name);
    }

    /** Returns a list of names in Graph that match the given prefix. */
    public LinkedList<String> autocomplete(String prefix) {
        return nameTrie.findMatches(cleanString(prefix));
    }

    /**
     * Returns a list of locations that match the given name. For example,
     * if the name is "Chipotle", we return a List of Maps of all the Chipotle's.
     */
    public List<Map<String, Object>> getLocations(String locationName) {
        String cleanedLocationName = cleanString(locationName);
        List<Map<String, Object>> ret = new LinkedList<>();

        /* Check if the graph contains matching node(s). */
        ArrayList<Node> list = nameToNodes.get(cleanedLocationName);
        if (list != null) {
            for (Node n : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", n.id);
                map.put("name", n.name);
                map.put("lon", n.lon);
                map.put("lat", n.lat);
                ret.add(map);
            }
        }
        return ret;
    }

}

import java.util.HashMap;
import java.util.LinkedList;

public class Trie {
    private class Node {
        private HashMap<Character, Node> children;
        boolean isFullName;
        String name;
        Node(String name) {
            children = new HashMap<>();
            isFullName = false;
            this.name = name;
        }
    }

    Node root;

    /**
     * Creates a Trie (reTRIEval tree) where each Node represents a character, and a sequence
     * of Nodes represents a name for a location in the GraphDB.
     */
    Trie() {
        root = new Node("");
    }

    /**
     * Inserts a name into the Trie.
     * @param cleanName all lower-case name without any punctuation
     * @param fullName full name with both upper- and lower-case, plus punctuation
     *                 (We need both names because tree search is case- and punctuation-insensitive,
     *                 whereas the names returned by Autocomplete must be sensitive.)
     */
    public void insertName(String cleanName, String fullName) {
        insertName(root, cleanName, fullName, 0);
    }

    /** Helper function for insertName(String name). */
    private void insertName(Node n, String cleanName, String fullName, int index) {
        if (cleanName.length() == 0) {
            return;
        }

        Character c = cleanName.charAt(index);

        /* Base case: adding the full name to the leaf node in Trie. */
        if (index == cleanName.length() - 1) {  // last char of string
            /* If the name already exists since it is a prefix of another name,
            don't create a new Node. For example, we may have added 'Poopie' earlier
            but now are adding 'Poop' -- we want to keep 'Poopie' and just mark
            'Poop' as a full name. Remember that we also set the name to fullName
            since the prefix 'poop' was lower case when stored in the trie. */
            if (n.children.containsKey(c)) {
                n.children.get(c).name = fullName;  // ex: 'poop' changed to 'Poop'
                n.children.get(c).isFullName = true;  // setting the prefix as full name
                return;
            }

            /* Add the full name to the trie and mark it as a complete name. */
            n.children.put(c, new Node(fullName));
            n.children.get(c).isFullName = true;
            return;
        }

        /* Recursion: Adding partial names to the trie. */
        if (n.children.containsKey(c)) {
            insertName(n.children.get(c), cleanName, fullName, index + 1);
        } else {
            /* Add the partial name to the trie and continue the recursion. */
            String partialName = "";
            for (int i = 0; i <= index; i++) {
                partialName += cleanName.charAt(i);
            }
            n.children.put(c, new Node(partialName));
            insertName(n.children.get(c), cleanName, fullName, index + 1);
        }
    }

    /** Finds and returns all the names that match the given prefix. */
    public LinkedList<String> findMatches(String prefix) {
        return findMatches(root, prefix, 0);
    }

    /**
     * Helper function for findMatches(String prefix):
     * Finds the prefix, and then calls the second helper function to find matches.
     */
    private LinkedList<String> findMatches(Node n, String prefix, int index) {
        /* Find the Node (if it exists) containing all characters in prefix.
           If such a Node does not exist, return null. */
        Character c = prefix.charAt(index);

        if (index != prefix.length() - 1) {
            /* We are still recursively iterating through the prefix. */
            if (!n.children.containsKey(c)) {
                return null;
            }
            return findMatches(n.children.get(c), prefix, index + 1);
        } else {
            /* Now we found the prefix in the trie. Return all the names
               that match the prefix. */
            return findMatches(n.children.get(c).children, prefix);
        }
    }

    /**
     * Helper function for findMatches(Node n, String prefix, int index):
     * Finds and returns list of all names that match the prefix.
     */
    private LinkedList<String> findMatches(HashMap<Character, Node> children, String prefix) {
        LinkedList<String> ret = new LinkedList<>();
        for (HashMap.Entry<Character, Node> entry : children.entrySet()) {
            Node n = entry.getValue();
            /* We found a full name along the way, but that doesn't mean that we are done
            searching downwards from this point. For example, there might be a name
            'Poop' and then 'Poopie', so we keep going. */
            if (n.isFullName) {
                ret.add(n.name);
            }

            /* Keep searching downward through the tree until we hit a leaf. */
            if (!n.children.isEmpty()) {
                LinkedList<String> temp = findMatches(n.children, prefix);
                for (String s : temp) {
                    ret.add(s);
                }
            }
        }
        return ret;
    }
}

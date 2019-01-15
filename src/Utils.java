import org.htmlcleaner.CompactXmlSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;

/**
 * A static class, containing helper methods
 */
final class Utils {

    /**
     * Traverse a tree breadth first
     *
     * @param root The root
     * @return The list of elements, in the desired order
     */
    static ArrayList<CustomElement> breadthFirstSearch(CustomElement root) {
        ArrayList<CustomElement> queue = new ArrayList<>(), result = new ArrayList<>();
        queue.add(root);
        while (queue.size() > 0) {
            CustomElement item = queue.remove(0);
            result.add(item);
            queue.addAll(item.getCustomChildren(false));
        }
        return result;
    }

    /**
     * Traverse a tree post order
     *
     * @param root The root
     * @return The list of elements, in the desired order
     */
    static ArrayList<CustomElement> postOrderTraverse(CustomElement root) {
        ArrayList<CustomElement> result = new ArrayList<>();
        for (CustomElement child : root.getCustomChildren(false)) {
            result.addAll(postOrderTraverse(child));
        }
        result.add(root);
        return result;
    }

    /**
     * Traverse a tree post order reversed
     *
     * @param root The root
     * @return The list of elements, in the desired order
     */
    static ArrayList<CustomElement> reversePostOrderTraverse(CustomElement root) {
        ArrayList<CustomElement> result = new ArrayList<>();
        List<CustomElement> children = new ArrayList<>(root.getCustomChildren(false));
        Collections.reverse(children);
        for (CustomElement child : children)
            result.addAll(reversePostOrderTraverse(child));
        result.add(root);
        return result;
    }

    /**
     * Get the lcs of children in the two list of children. Return the list as tuples
     * https://www.geeksforgeeks.org/printing-longest-common-subsequence/
     *
     * @param leftChildren  The list of children in left
     * @param rightChildren The list of children in right
     * @param eq            The function that decides whether two nodes are equal
     * @return The elements that occur in the lcs, as a list of tuples
     */
    static HashMap<CustomElement, CustomElement> lcs(ArrayList<CustomElement> leftChildren, ArrayList<CustomElement> rightChildren, BiFunction<CustomElement, CustomElement, Boolean> eq) {
        int m = leftChildren.size();
        int n = rightChildren.size();
        int[][] matrix = new int[m + 1][n + 1];

        // Following steps build L[m+1][n+1] in bottom up fashion. Note
        // that L[i][j] contains length of LCS of X[0..i-1] and Y[0..j-1]
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0)
                    matrix[i][j] = 0;
                else if (eq.apply(leftChildren.get(i - 1), rightChildren.get(j - 1)))
                    matrix[i][j] = matrix[i - 1][j - 1] + 1;
                else
                    matrix[i][j] = Math.max(matrix[i - 1][j], matrix[i][j - 1]);
            }
        }

        // Following code is used to get LCS

        // Create a map to store tuples in the lcs
        HashMap<CustomElement, CustomElement> matches = new HashMap<>();

        // Start from the right-most-bottom-most corner and
        // one by one store characters in lcs[]
        int i = m, j = n;
        while (i > 0 && j > 0) {
            // If current character in X[] and Y are same, then
            // current character is part of LCS
            if (eq.apply(leftChildren.get(i - 1), rightChildren.get(j - 1))) {
                // Put current character in result
                matches.put(leftChildren.get(i - 1), rightChildren.get(j - 1));

                // reduce values of i and j
                i--;
                j--;
            }

            // If not same, then find the larger of two and
            // go in the direction of larger value
            else if (matrix[i - 1][j] > matrix[i][j - 1])
                i--;
            else
                j--;
        }
        return matches;
    }

    /**
     * Split the style string to a list of attributes
     * https://stackoverflow.com/questions/17108310/retrieve-html-inline-style-attribute-value-with-jsoup
     *
     * @param styleStr The style represented as a string
     * @return map of attribute value pairs
     */
    static HashMap<String, String> styleStringToList(String styleStr) {
        styleStr = styleStr == null ? "" : styleStr;
        HashMap<String, String> keymaps = new HashMap<>();
        String[] keys = styleStr.split(":");
        String[] split;
        if (keys.length > 1) {
            for (int i = 0; i < keys.length; i++) {
                if (i % 2 != 0) {
                    split = keys[i].split(";");
                    if (split.length == 1) break;
                    keymaps.put(split[1].trim(), keys[i + 1].split(";")[0].trim());
                } else {
                    split = keys[i].split(";");
                    if (i + 1 == keys.length) break;
                    keymaps.put(keys[i].split(";")[split.length - 1].trim(), keys[i + 1].split(";")[0].trim());
                }
            }
        }
        return keymaps;
    }

    /**
     * Get the key of a value in a key -> value map
     *
     * @param map   The map
     * @param value The value
     * @return The key
     */
    static String valueToKey(HashMap<String, String> map, String value) {
        for (Map.Entry<String, String> entry : map.entrySet())
            if (entry.getValue().equals(value))
                return entry.getKey();
        return "";
    }

    /**
     * Rewrite a tree with root Element, to a tree where all elements are of type CustomElement
     *
     * @param children the children of the original root
     * @param parent   The customCustomElement version of the original root
     */
    private static void addChildrenToParent(List<Content> children, CustomElement parent) {
        while (children.size() > 0) {
            Content first = children.remove(0);
            if (first instanceof Text)
                parent.setCustomText(new Text(parent.getCustomText() + ((Text) first).getText()));
            else if(first instanceof  CDATA)
                parent.setCustomText(new Text(parent.getCustomText() + ((CDATA)first).getTextNormalize()));
            else if (first instanceof Element) {
                if(((Element)first).getName().equals("style"))
                    System.out.println("");
                CustomElement newChild = new CustomElement((Element) first);
                addChildrenToParent(((Element) first).getContent(), newChild);
                if (children.size() > 0 && children.get(0) instanceof Text)
                    newChild.setTail((Text) children.remove(0));
                parent.addContent(newChild);
            }
        }
    }

    /**
     * Create a tree by a filename
     *
     * @param filename The filename
     * @return The tree
     * @throws JDOMException SaxBuilder might throw this exception
     * @throws IOException   SaxBuilder might throw this exception
     */
    static CustomElement filenameToTree(String filename) throws JDOMException, IOException {
        String content = fileNameToCleanContent(filename);
        Document d = new SAXBuilder().build(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        CustomElement root = new CustomElement(d.getRootElement());
        addChildrenToParent(d.getRootElement().getContent(), root);
        return root;
    }

    /**
     * Read an html file, clean it using HTMLCleaner, and return its clean content as a string
     *
     * @param filename The name of the input file
     * @return The clean contents of the input file
     * @throws IOException Might be thrown
     */
    private static String fileNameToCleanContent(String filename) throws IOException {
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode root = cleaner.clean(new File(filename), "utf-8");
        return new CompactXmlSerializer(cleaner.getProperties()).getAsString(root);
    }
}

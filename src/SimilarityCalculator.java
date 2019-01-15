import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jdom2.Attribute;

import java.util.*;

/**
 * The task of this calculator is to calculate how equal two nodes are.
 * It uses all characteristics of nodes when calculating the value. these are CustomText, Tail, Attributes,
 * StyleAttributes, children and parent.
 * We use variables to denote the importance of characteristics of nodes. Changing these attributes changes the way
 * that nodes will be matched. Using this method, a user can choose to stress the similarity of for example
 * text attributes.
 */
class SimilarityCalculator {
    /**
     * Define the importance of CustomText and Tail values
     */
    private static final double TEXT_WEIGHT = 1;
    /**
     * Define the importance of attribute values
     */
    private static final double ATTRIBUTE_WEIGHT = 1;
    /**
     * Define the importance of style values
     */
    private static final double STYLE_WEIGHT = 1;

    /**
     * Calculate similarity of two nodes, expressed as the number of equal children divided by the total number of children
     *
     * @param left  left node
     * @param right right node
     * @return similarity
     */
    static double childSimilarity(CustomElement left, CustomElement right, HashMap<CustomElement, Partner> l2r) {
        List<CustomElement> leftChildren = left.getCustomChildren(true);
        List<CustomElement> rightChildren = new ArrayList<>(right.getCustomChildren(true));
        if (leftChildren.size() <= 0 && rightChildren.size() <= 0)//If both don't nodes don't have any children,
            // we don't take child similarity into account
            return -1;
        int count = 0;
        int maxChildCount = Math.max(leftChildren.size(), rightChildren.size());
        for (CustomElement leftChild : leftChildren) {
            Partner partner = l2r.get(leftChild);
            CustomElement partnerElement = partner == null ? null : partner.getPartner();
            if (partnerElement != null) {
                count += rightChildren.remove(partnerElement) ? 1 : 0;//If the partner of leftChild is found in
                // rightChildren, increment the count and remove the child in rightChildren
            }
        }
        return count / (double) maxChildCount;
    }

    /**
     * Calculate similarity of two nodes, expressed as the weighed similarity of the attributes, styles and texts.
     *
     * @param left  left node
     * @param right right node
     * @return similarity
     */
    static double nodeSimilarity(CustomElement left, CustomElement right, double F, HashMap<CustomElement, Partner> l2r) {
        if (!couldBeMatched(left, right))
            return 0.0;
        //Calculate style similarity
        float styleSim = attributeSimilarity(left.getStyleAttributes(), right.getStyleAttributes());
        HashMap<String, String> aAttrs = new HashMap<>();
        HashMap<String, String> bAttrs = new HashMap<>();
        for (Attribute a : left.getAttributes())
            aAttrs.put(a.getName(), a.getValue());
        for (Attribute b : right.getAttributes())
            bAttrs.put(b.getName(), b.getValue());
        //Calculate attribute similarity
        float attrSim = attributeSimilarity(aAttrs, bAttrs);
        //Calculate text similarity (inner text and tail)
        float textSim = sim(left.getCustomText(), right.getCustomText());
        float tailSim = sim(left.getTail(), right.getTail());
        return ratiosToSum(attrSim, styleSim, textSim, tailSim, F, left, right, l2r);
    }

    /**
     * Used to exclude certain types of elements from being matched. Example: <img>'s can only be matched if their
     * sources are equal
     * Todo: Add extra checks
     *
     * @param l The left node
     * @param r The right node
     * @return Indicator whether they match the requirements
     */
    static Boolean couldBeMatched(CustomElement l, CustomElement r) {
        if (l.getName().equals("img") && r.getName().equals("img") && l.getAttributeValue("src") != null &&
                !l.getAttributeValue("src").equals(r.getAttributeValue("src")))
            return false;
        return true;
    }

    /**
     * When separate similarities are calculated, we multiply the values by their weight.
     * When a value is -1, we don't use that value, as this means that it's absent in both nodes
     *
     * @param attrSim  Attribute similarity
     * @param styleSim Style similarity
     * @param textSim  Text similarity (text and tail)
     * @return Weighed similarity
     */
    static double ratiosToSum(float attrSim, float styleSim, float textSim, float tailSim, double F, CustomElement l, CustomElement r, HashMap<CustomElement, Partner> l2r) {
        double max = 0;
        double count = 0;
        if (attrSim > -1) {
            max += ATTRIBUTE_WEIGHT;
            count += attrSim * ATTRIBUTE_WEIGHT;
        }
        if (styleSim > -1) {
            max += STYLE_WEIGHT;
            count += styleSim * STYLE_WEIGHT;
        }
        if (textSim > -1) {
            max += TEXT_WEIGHT;
            count += textSim * TEXT_WEIGHT;
        }
        if (tailSim > -1) {
            max += TEXT_WEIGHT;
            count += tailSim * TEXT_WEIGHT;
        }
        if (max == 0) {//If the elements have no identifiers at all
            return similarityForNonIdentifiableNodes(l, r, l2r);
        }
        //Weigh the similarities
        double result = count / max;
        return result;
//        return getSimilarityMultiplier(l, r, l2r) * result;
//        return Math.min(getSimilarityMultiplier(l, r, l2r) * result, 1.0);
    }

    /**
     * Calculate similarity for NonIdentifiable leafs differently. Leafs are 100% equal when they have no children,
     * the parents match, and their tags match. Else they are unequal
     *
     * @param l   The left leaf
     * @param r   The right leaf
     * @param l2r Map that matches left nodes to right nodes
     * @return The similarity
     */
    private static double similarityForNonIdentifiableNodes(CustomElement l, CustomElement r, HashMap<CustomElement, Partner> l2r) {
        if (l.getCustomChildren(false).size() <= 0 &&
                r.getCustomChildren(false).size() <= 0) {//If they don't have children
            if (l.getName().equals(r.getName())) {//If the tags are equal
                Partner lPartner = l2r.get(l.getCustomParentElement());
                CustomElement lParent = lPartner == null ? null : lPartner.getPartner();
                if (lParent != null && lParent.equals(r.getCustomParentElement()))//If the parents
                    // are equal
                    return 1.0;
                else
                    return 0.0;
            } else
                return 0.0;
        } else//Don't take the nodeSimilarity into account at all
            return -1.0;
    }

    /**
     * Define a multiplier which increases the similarity by use of several heuristics
     * Todo: Add more cases for the multiplier
     *
     * @param l   Left element
     * @param r   Right element
     * @param l2r Left to right map
     * @return Calculated multiplier
     */
    private static Double getSimilarityMultiplier(CustomElement l, CustomElement r, HashMap<CustomElement, Partner> l2r) {
        double multiplier = 1.0;
        if (l.getName().equals(r.getName()))//Increase similarity by 20% if the tags are equal
            multiplier += 0.2;
        CustomElement lparent = l.getCustomParentElement();
        Partner lparentpartner = null;
        if (lparent != null)
            lparentpartner = l2r.get(lparent);
        CustomElement lparentpartnerelement = null;
        if (lparentpartner != null)
            lparentpartnerelement = lparentpartner.getPartner();
        if (lparentpartnerelement != null && lparentpartnerelement.equals(r.getCustomParentElement()))//Increase
            // similarity by 20% if the parents of both elements are matched
            multiplier += 0.2;
        return multiplier;
    }

    /**
     * Calculate the similarity between two strings, using the LevenstheinDistance
     * https://en.wikipedia.org/wiki/Levenshtein_distance
     *
     * @param a first string
     * @param b second string
     * @return 0 <= similarity <= 1
     */
    static float sim(String a, String b) {
        if (a.equals("") && b.equals(""))
            return -1;
        if (a.equals(b))
            return 1;
        int lenMax = Math.max(a.length(), b.length());
        double distance = LevenshteinDistance.getDefaultInstance().apply(a, b);
        return 1 - (((float) distance) / lenMax);
    }

    /**
     * Calculate the similarity between two lists of attributes, also works for styles
     *
     * @param l left list
     * @param r right list
     * @return similarity
     */
    private static float attributeSimilarity(HashMap<String, String> l, HashMap<String, String> r) {
        if (l.size() <= 0 && r.size() <= 0)
            return -1;
        int totalAttributes = Math.max(l.size(), r.size());
        float summedSim = (float) 0.0;
        HashMap<String, String> left = new HashMap<>(l);
        HashMap<String, String> right = new HashMap<>(r);
        for (Map.Entry<String, String> entry : left.entrySet()) {
            if (right.containsKey(entry.getKey())) {
                switch (entry.getKey()) {
                    case "class"://Handle class different: determine nr of equal classes
                        summedSim += classSimilarity(entry.getValue(), right.get(entry.getKey()));
                        break;
                    default:
                        float calculatedSim = sim(entry.getValue(), right.get(entry.getKey()));
                        summedSim += calculatedSim == -1 ? 1 : calculatedSim;//If both values are empty, count as similar
                        break;
                }
                right.remove(entry.getKey());
            }
        }
        return summedSim / totalAttributes;
    }

    /**
     * Calculate similarity between classes. It splits the classes by " ", and checks the number of equal classes
     *
     * @param l left classValue
     * @param r right classValue
     * @return ratio of similar classes
     */
    private static float classSimilarity(String l, String r) {
        int similarClasses = 0;
        ArrayList<String> left = new ArrayList<>(Arrays.asList(l.split(" ")));
        ArrayList<String> right = new ArrayList<>(Arrays.asList(r.split(" ")));
        int totalClasses = Math.max(left.size(), right.size());
        for (String c : left)
            if (right.contains(c))
                similarClasses++;
        return (float) similarClasses / totalClasses;
    }

}

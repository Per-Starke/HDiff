import java.util.*;

/**
 * This class is used to match children of two elements
 */
public class Matcher {
    /**
     * The two input roots
     */
    private CustomElement left, right;
    /**
     * Saved partners for each node, l2r and r2l
     */
    private HashMap<CustomElement, Partner> leftPartners, rightPartners;
    /**
     * At any point, this list holds all the left nodes that still need a bestMatch
     */
    private ArrayList<CustomElement> unmatchedLefts;
    /**
     * Minimum similarity that two nodes must have in order to be matched, regarding their attributes
     */
    private double F;
    /**
     * Minimum similarity that two nodes must have in order to be matched, regarding percentage of equal children
     */
    private double T;

    Matcher(double f, double t) {
        this.F = f;
        this.T = t;
    }

    /**
     * After it is determined that two elements are the currently best bestMatch for each other, save the bestMatch
     *
     * @param l     The left element
     * @param r     The right element
     * @param value The similarity between the two elements
     */
    private void saveMatch(CustomElement l, CustomElement r, Double value) {
        this.unmatchedLefts.remove(l);
        leftPartners.put(l, new Partner(r, value));
        rightPartners.put(r, new Partner(l, value));
    }

    /**
     * Check if the found bestMatch is currently the best bestMatch for both elements. Save the bestMatch if so.
     * We use the stable marriage problem to ensure a perfect matching
     * https://en.wikipedia.org/wiki/Stable_marriage_problem
     *
     * @param l     The left element
     * @param r     The right element
     * @param value The similarity between the two elements
     */
    private void updateMatchIfCurrentBest(CustomElement l, CustomElement r, Double value) {
        Partner currentPartnerOfLeft = leftPartners.get(l);
        Partner currentPartnerOfRight = rightPartners.get(r);
        if (currentPartnerOfLeft != null && currentPartnerOfLeft.getSimilarity() > value)//l currently has a better
            // partner than one being added, l and r are not matched
            return;
        if (currentPartnerOfRight != null) {//r already has a match
            if (value > currentPartnerOfRight.getSimilarity()) {//r prefers l over its current partner
                if (currentPartnerOfLeft != null)//If l already has a partner, remove that partner, as l is now matched to r
                    rightPartners.remove(currentPartnerOfLeft.getPartner());
                //The old partner of r is unmatched
                unmatchedLefts.add(currentPartnerOfRight.getPartner());
                leftPartners.remove(currentPartnerOfRight.getPartner());
                //The new partner of r, l, becomes matched
                saveMatch(l, r, value);
            } else {//r currently has a better partner than l, so l and r are not matched (add for clarification)
                return;
            }
        } else {//r is unmatched; l and r become partners
            if (currentPartnerOfLeft != null)//If l already has a partner, remove that partner, as l is now matched to r
                rightPartners.remove(currentPartnerOfLeft.getPartner());
            saveMatch(l, r, value);
        }
    }

    /**
     * This matching algorithm is different from the paper. It describes an algorithm that calculates 'good matches',
     * and not 'best matches'. We use the solution of the stable marriage problem, to ensure the best possible matches.
     * This algorithm is more time-consuming, but gives better results.
     *
     * @param l The left root
     * @param r The right root
     * @return Two maps that bestMatch left nodes to right nodes and vice versa.
     * @throws Exception This exception is thrown when the sizes of the l2r map and r2l map don't match
     */
    ArrayList<HashMap<CustomElement, CustomElement>> bestMatch(CustomElement l, CustomElement r) throws Exception {
        this.left = l;
        this.right = r;
        this.leftPartners = new HashMap<>();
        this.rightPartners = new HashMap<>();
        //Loop over the left nodes bottom-up
        this.unmatchedLefts = Utils.postOrderTraverse(this.left);
        ArrayList<CustomElement> rightNodes = Utils.postOrderTraverse(this.right);
        //We always match the roots. To do so, we match them now and we remove the right and left root from the list
        this.updateMatchIfCurrentBest(this.left, this.right, 1.0);
        rightNodes.remove(this.right);
        unmatchedLefts.remove(this.left);
        while (unmatchedLefts.size() > 0) {
            CustomElement leftNode = unmatchedLefts.remove(0);
            if (leftNode.isUnidentifiableLeaf())//Don't match these elements
                continue;
            double currentT = leftNode.getNrOfLeafsInSubtree() <= 4 ? Math.min(0.4, this.T) : this.T;//Lower the threshold for small
            // subtrees
            for (CustomElement rightNode : rightNodes) {
                if (rightNode.isUnidentifiableLeaf())//Don't match these elements
                    continue;
                //If it is possible that rightNode matches better with leftNode than it's current bestMatch:
                Partner currentPartnerOfRight = rightPartners.get(rightNode);
                if (currentPartnerOfRight == null || currentPartnerOfRight.getSimilarity() < 1 || (currentPartnerOfRight.getSimilarity() >= 1 && !rightNode.isEqualTo(currentPartnerOfRight.getPartner(), false))) {
                    //Calculate the similarity of the characteristics
                    Double nodeSimilarity = SimilarityCalculator.nodeSimilarity(leftNode, rightNode, this.F, this.leftPartners);
                    //Calculate the similarity expressed using the number of equal children
                    Double childSimilarity = SimilarityCalculator.childSimilarity(leftNode, rightNode, leftPartners);
                    //Take the average of the two similarities
                    //Save the bestMatch if they are similar enough, and if its the current best bestMatch for left and right
                    if ((nodeSimilarity <= -1 || nodeSimilarity >= this.F) && (childSimilarity <= -1 || childSimilarity >= currentT)) {
                        Double combined = 0.0;
                        if (nodeSimilarity > -1)
                            combined += nodeSimilarity;
                        if (childSimilarity > -1) {
                            if (combined > 0)
                                combined = (combined + childSimilarity) / 2;//Define the similarity as the average of the two
                            else
                                combined = childSimilarity;//match is only <= -1 if the nodes have tags without any
                            // identifiers. In that case, we don't want to take it into account
                        }
                        updateMatchIfCurrentBest(leftNode, rightNode, combined);
                        if (combined >= 1 && leftNode.isEqualTo(rightNode, true)) {//No better bestMatch will be found
                            // in this case, we also check isEqualTo, because of the getSimilarityMultiplier, more than 1 element can have a similarity >1, we
                            // keep comparing if the elements are not exactly equal, as its possible we find another node with a higher similarity.
                            break;
                        }
                    }
                }
            }
        }
        if (leftPartners.size() != rightPartners.size())
            throw new Exception("The partner mappings don't bestMatch!");
        return outputResult();
    }

    /**
     * After the bestMatch is complete, return the data in the desired format
     *
     * @return l2r and r2l in a list
     */
    private ArrayList<HashMap<CustomElement, CustomElement>> outputResult() {
        ArrayList<HashMap<CustomElement, CustomElement>> result = new ArrayList<>();
        HashMap<CustomElement, CustomElement> l2r = new HashMap<>();
        HashMap<CustomElement, CustomElement> r2l = new HashMap<>();
        for (Map.Entry<CustomElement, Partner> entry : leftPartners.entrySet())
            l2r.put(entry.getKey(), entry.getValue().getPartner());
        for (Map.Entry<CustomElement, Partner> entry : rightPartners.entrySet())
            r2l.put(entry.getKey(), entry.getValue().getPartner());
        result.add(l2r);
        result.add(r2l);
        return result;
    }
}

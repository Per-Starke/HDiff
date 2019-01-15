import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;

/**
 * This class performs the diffing algorithm described in the paper:
 * http://ilpubs.stanford.edu:8090/115/1/1995-46.pdf
 */
public class Differ {
    /**
     * The two input files
     */
    private CustomElement left, right;
    /**
     * Maps left nodes to matching right nodes and vice versa
     */
    private HashMap<CustomElement, CustomElement> l2r, r2l;
    /**
     * Defines the minimum similarity between two leafs for them to be equal
     */
    private double F;
    /**
     * Defines the minimum percentage of equal children two inner nodes must have for them to be equal
     */
    private double T;
    /**
     * Set of nodes that are ordered in its parent
     */
    private Set<CustomElement> inorder;
    /**
     * The resulting list of EditActions to transform left into right
     */
    private ArrayList<EditAction> result;
    /**
     * Class that edits this.left after every creation of an EditAction
     */
    private TreeEditor editor;

    public Differ(Double f, Double t) {
        this.F = f;
        this.T = t;
        if (!(0 < f && f < 1))
            this.F = 0.5;
        if (!(0.5 <= t && t <= 1))
            this.T = 0.2;
    }

    /**
     * Find the position at which the rightNode has to be inserted / moved.
     * The numbers refer to the second function in figure 9 of the paper
     *
     * @param node The node in the right tree
     * @return The position to which the new rightNode has to be added / moved in the left tree
     */
    private Integer findPosition(CustomElement node) {
        //1.
        CustomElement parent = node.getCustomParentElement();
        //To prevent nullPointerExceptions
        if (parent == null)
            return 0;
        //2. We skip this step. It takes too much time and the algorithm works without
        //3. Find the rightmost sibling of 'node' that (1) is on the left of 'node', (2) is in order
        List<CustomElement> children = parent.getCustomChildren(false);
        int i = children.indexOf(node);
        CustomElement foundSibling = null;
        while (i >= 1) {
            i -= 1;
            CustomElement sibling = children.get(i);
            if (this.inorder.contains(sibling)) {
                foundSibling = sibling;
                break;
            }
        }
        //If we have found a sibling, go to step 4
        if (foundSibling != null) {
            //4. Find the partner of the found sibling
            CustomElement siblingsPartner = this.r2l.get(foundSibling);
            //5. Get the index i of siblingsPartner, and return i+1
            // While calculating this index, we only count ordered children of the parent of foundSiblings partner,
            // until we encounter foundSiblings partner
            CustomElement nodesPartner = this.r2l.get(node);
            i = 0;
            //To prevent nullPointerExceptions
            if (siblingsPartner.getCustomParentElement() == null)
                return 0;
            for (CustomElement child : siblingsPartner.getCustomParentElement().getCustomChildren(false)) {
                if (child.equals(nodesPartner))//Don't count the node we're looking for, as it will be moved
                    continue;
                if (this.inorder.contains(child) || !this.l2r.containsKey(child))//These are left nodes that are
                    // in order, or will be deleted. We also count these 'incorrectly placed nodes', as the position in
                    // left should be increased by that number with regard to the position in right, as right doesn't
                    // have these nodes
                    i += 1;
                if (child.equals(siblingsPartner))//We've found the partner of foundSibling, return its index in left
                    break;
            }
            return i;
        } else
            return 0;

    }

    /**
     * This function performs the diff algorithm as explained in the paper, only on the HTML attributes,
     * not on the whole object. The AttributeDiffer also uses the StyleDiffer to diff styles explicitly
     *
     * @param leftNode  the old node
     * @param rightNode the new node
     * @throws Exception This exception is thrown when a malformed EditAction is created
     */
    private void updateNodeAttributes(CustomElement leftNode, CustomElement rightNode) throws Exception {
        result.addAll(new AttributeDiffer(editor).diff(leftNode, rightNode));
    }

    /**
     * Update the tag of the left element. Here, right is equal to left, except for the tag
     *
     * @param left  the old node
     * @param right the new node
     * @throws Exception This exception is thrown when a malformed EditAction is created
     */
    private void updateNodeTag(CustomElement left, CustomElement right) throws Exception {
        if (!left.getName().equals(right.getName())) {
            EditAction action = new EditAction(EditAction.ActionType.RenameNode, left, right.getName());
            result.add(action);
            //Update the left node
            editor.renameNode(action);
        }
    }

    /**
     * Align the children of left and right
     * The numbers and letters refer to the first function in figure 9 of the paper
     *
     * @param left  left root
     * @param right right root
     * @throws Exception This exception is thrown when a malformed EditAction is created
     */
    private void alignChildren(CustomElement left, CustomElement right) throws Exception {
        //1. All children in in_order are sure to be in order, so we can skip these.
        //2. Create list of nodes which parents are left / right, and their partners parents are right / left
        ArrayList<CustomElement> leftChildren = new ArrayList<>();
        ArrayList<CustomElement> rightChildren = new ArrayList<>();
        for (CustomElement child : left.getCustomChildren(false))
            if (l2r.containsKey(child))
                if (l2r.get(child).getCustomParentElement() != null && l2r.get(child).getCustomParentElement().equals(right))
                    leftChildren.add(child);
        for (CustomElement child : right.getCustomChildren(false))
            if (r2l.containsKey(child))
                if (r2l.get(child).getCustomParentElement() != null && r2l.get(child).getCustomParentElement().equals(left))
                    rightChildren.add(child);

        //No children to align
        if (leftChildren.size() <= 0 && rightChildren.size() <= 0)
            return;
        //3. Create and equal function, which defines whether two nodes are partners
        BiFunction<CustomElement, CustomElement, Boolean> equal = (element, element2) -> this.l2r.get(element).equals(element2);
        //4. Create the LCS of leftChildren and rightChildren
        HashMap<CustomElement, CustomElement> matches = Utils.lcs(leftChildren, rightChildren, equal);
        //5. Mark all nodes in LCS as in_order
        for (Map.Entry<CustomElement, CustomElement> entry : matches.entrySet()) {
            this.inorder.add(entry.getKey());
            this.inorder.add(entry.getValue());
        }

        //6. Loop over all left children that are not in order, find their partner and its position, and create
        // an EditAction
        ArrayList<CustomElement> unalignedLeft = new ArrayList<>(leftChildren);
        unalignedLeft.removeAll(this.inorder);
        for (CustomElement leftElem : unalignedLeft) {
            CustomElement rightElem = this.l2r.get(leftElem);
            //(a) Find the position of rightElem
            Integer rightPosition = this.findPosition(rightElem);
            CustomElement rightTarget = rightElem.getCustomParentElement();
            CustomElement leftTarget = this.r2l.get(rightTarget);
            //(b). Create the EditAction to move leftElem to the position of rightElem in parent rightTarget
            EditAction action = new EditAction(EditAction.ActionType.MoveNode, leftElem, leftTarget, rightPosition);
            result.add(action);
            //Do the move
            editor.moveNode(action);
            //(c) Mark leftElem and rightElem as in order
            this.inorder.add(leftElem);
            this.inorder.add(rightElem);
        }
    }

    /**
     * Update left's text and tail
     *
     * @param left  left root
     * @param right right root
     * @throws Exception This exception is thrown when a malformed EditAction is created
     */
    private void updateNodeText(CustomElement left, CustomElement right) throws Exception {
        if (!left.getCustomText().equals(right.getCustomText())) {
            String updatedText = new TextDiffer().diff(left.getCustomText(), right.getCustomText());
            EditAction action = new EditAction(EditAction.ActionType.UpdateTextIn, left, updatedText);
            result.add(action);
            //Update the left node
            editor.updateText(action);
        }
        if (!left.getTail().equals(right.getTail())) {
            String updatedTail = new TextDiffer().diff(left.getTail(), right.getTail());
            EditAction action = new EditAction(EditAction.ActionType.UpdateTail, left, updatedTail);
            result.add(action);
            //Update the left node
            editor.updateTail(action);
        }
    }

    /**
     * Perform the diffing algorithm using the 5 phases described in the paper.
     * The numbers and letters refer to figure 8 of the paper
     *
     * @param f1 Filename 1
     * @param f2 Filename 2
     * @param o Output filename
     * @throws Exception This exception is thrown if an incorrect EditAction if created
     */
    ArrayList<EditAction> diff(String f1, String f2, String o) throws Exception {
        //1.
        result = new ArrayList<>();
        this.left = Utils.filenameToTree(f1);
        this.right = Utils.filenameToTree(f2);
        this.editor = new TreeEditor(this.left);
        this.inorder = new HashSet<>();
        ArrayList<HashMap<CustomElement, CustomElement>> matchResult = new Matcher(this.F, this.T).bestMatch(this.left, this.right);
        if (matchResult.size() != 2)
            throw new Exception("Cannot bestMatch trees!");
        this.l2r = matchResult.get(0);
        this.r2l = matchResult.get(1);
        //2.
        for (CustomElement rightNode : Utils.breadthFirstSearch(this.right)) {
            if (rightNode.isUnidentifiableLeaf())//Don't diff these kind of nodes
                continue;
            //(a)
            CustomElement rightParent = rightNode.getCustomParentElement();
            CustomElement leftTarget = this.r2l.get(rightParent);
            CustomElement leftNode;//Will be created below
            //(b): If rightNode has no partner -> Insert phase
            if (!r2l.containsKey(rightNode)) {
                //i. Find the position at which the new node has to be inserted
                Integer position = this.findPosition(rightNode);
                //ii. Create a new EditAction
                leftNode = rightNode.clone();
                EditAction action = new EditAction(EditAction.ActionType.InsertNode, leftNode, leftTarget, position);
                result.add(action);
                //iii. Create the bestMatch, and apply the EditAction to the left node
                // We create only the element, without its contents. The contents and attributes will be added below,
                // this results in an EditAction for each attribute of rightNode. Might it be better to fully copy
                // rightNode, and create one EditAction?
                this.editor.insertNode(action);
                matchRecursively(leftNode, rightNode);
                //As an addition to the paper, we also update attributes. The paper assumes only labels and values,
                // we also assume styles and other attributes. Nodes also have texts, but we add these later
                this.updateNodeAttributes(leftNode, rightNode);
            }
            //(c) If rightNode does have a partner -> Update, Move, and Align phase
            else {
                //i. Get the partner, and its parent of rightNode,
                leftNode = this.r2l.get(rightNode);
                CustomElement leftParent = leftNode.getCustomParentElement();
                //ii. Update phase: update the tags and attributes of the leftNode, if needed
                // A. & B. We create the EditAction, and update leftNode
                this.updateNodeTag(leftNode, rightNode);
                this.updateNodeAttributes(leftNode, rightNode);
                //iii. Move phase
                // If the parent of leftNode is not equal to leftTarget, leftNode needs to be moved to leftTarget
                if (leftParent != null && !leftParent.equals(leftTarget)) {
                    //A. Already done: leftTarget
                    //B. Find the position to which the leftNode should be moved
                    Integer position = this.findPosition(rightNode);
                    //C. Create the EditAction
                    EditAction action = new EditAction(EditAction.ActionType.MoveNode, leftNode, leftTarget, position);
                    result.add(action);
                    //D. Update leftNode
                    editor.moveNode(action);
                    this.inorder.add(leftNode);
                    this.inorder.add(rightNode);
                }
            }
            //(d) Align phase
            alignChildren(leftNode, rightNode);
            leftNode = this.r2l.get(rightNode);
            //Update text and tail
            this.updateNodeText(leftNode, rightNode);
        }
        //3. Delete phase
        for (CustomElement leftNode : Utils.reversePostOrderTraverse(this.left)) {
            if (leftNode.isUnidentifiableLeaf())//Don't diff these kind of nodes
                continue;
            //(a)
            if (!this.l2r.containsKey(leftNode)) {
                //(b) If this item has no match, create a DeleteNode EditAction, and apply it
                EditAction action = new EditAction(EditAction.ActionType.DeleteNode, leftNode);
                result.add(action);
                //Update left, the result of deleteNode is a list of all Edit Actions of the children of the node.
                // These actions are removed, as we don't want edit actions on children of a deleted node.
                this.result.removeAll(editor.deleteNode(action));
            }
        }
        //4. Done!
        createOutputFile(o);
        return this.result;
    }

    /**
     * Add two equal nodes to the matches. This function is called when a node is inserted: The inserted node is cloned
     * to this.left, and children are matched
     *
     * @param l The newly inserted node
     * @param r The matching node in this.right
     * @throws Exception When !l.equals(r) or l doesn't have the same number of children as r, an exception is thrown
     */
    private void matchRecursively(CustomElement l, CustomElement r) throws Exception {
        if (!l.isEqualTo(r, false))
            throw new Exception("Cant match unequal elements!");
        else {
            this.inorder.add(l);
            this.inorder.add(r);
            this.l2r.put(l, r);
            this.r2l.put(r, l);
            List<CustomElement> lc = l.getCustomChildren(false);
            List<CustomElement> rc = r.getCustomChildren(false);
            if (lc.size() != rc.size())
                throw new Exception("Matching a cloned node that hasn't got the same number of children as its match!");
            for (int i = 0; i < lc.size(); i++)
                matchRecursively(lc.get(i), rc.get(i));
        }
    }

    /**
     * Create the desired output in html format
     *
     * @throws IOException Might be thrown while opening the file
     */
    private void createOutputFile(String filename) throws IOException {
        if(this.result.size() > 0)
            new Outputter().output(this.left, filename);
    }
}

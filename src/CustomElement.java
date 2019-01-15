import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

import java.util.*;

/**
 * An extension of jdom2.Element. It changes its structure, such that we can work with inner texts, tails and
 * style attributes
 */
public class CustomElement extends Element {

    /**
     * Hold the text and tail value
     */
    private Text tail, text;
    /**
     * Hold the styleAttributes; these are removed from this.Attributes()
     */
    private HashMap<String, String> styleAttributes;
    /**
     * A list of associated EditActions
     */
    private ArrayList<EditAction> actions;
    /**
     * Hold the popuptexts according to the associated EditActions
     */
    private ArrayList<String> popupTexts;
    /**
     * Inidicates whether this node has been deleted from left
     */
    private boolean isDeleted = false;
    /**
     * The number of leafs in the subtree rooted at this node
     */
    private int nrOfLeafsInSubtree = -1;

    /**
     * Create a CustomElement, holding the values of the original Element, except for the parent and the text and tail
     * The function is used in Utils.addChildrenToParent. The parents is not cloned because the parent of 'e' is a
     * normal Element. e is transformed to a CustomElement and that element is set to be the parent of this
     * CustomElement. Text and tails are not set as they will later be set by the function addChildrenToParent. We
     * copy elements this way because we edit the tree structure to conform with the tree structure used in XMLDiff
     * (https://xmldiff.readthedocs.io/) which uses the package etree.
     *
     * @param e The original Element
     */
    CustomElement(Element e) {
        this.name = e.getName();
        this.namespace = e.getNamespace();
        this.addNamespaceDeclaration(e.getNamespace());
        List<Attribute> attributes = e.getAttributes();
        for (Attribute a : attributes) {
            this.setAttribute(a.getName(), a.getValue().replaceAll("\"", "&quot;"));
        }
        this.removeAttribute("style");
        this.styleAttributes = Utils.styleStringToList(e.getAttributeValue("style"));
    }

    /**
     * Clone this CustomElement, copying all characteristics except the parent (and of course the EditActions and
     * Popuptexts als also skipped)
     *
     * @return The clone
     */
    @Override
    public CustomElement clone() {
        CustomElement cloned = new CustomElement(this);//Clone "this" as if it were a normal Element
        //Now copy all characteristics of "this" as a CustomElement
        cloned.styleAttributes = this.getStyleAttributes();
        cloned.isDeleted = this.isDeleted;
        cloned.nrOfLeafsInSubtree = this.nrOfLeafsInSubtree;
        cloned.setCustomText(new Text(this.getCustomText()));
        cloned.setTail(new Text(this.getTail()));
        //Clone all this' children in the same way
        for (CustomElement child : this.getCustomChildren(true))
            cloned.addContent(child.clone());
        return cloned;
    }

    /**
     * Check whether this element has at least one style attribute
     *
     * @return The value
     */
    Boolean hasStyle() {
        return this.styleAttributes.size() > 0;
    }

    HashMap<String, String> getStyleAttributes() {
        return this.styleAttributes;
    }

    void setStyleAttribute(String k, String v) {
        this.styleAttributes.put(k, v);
    }

    void removeStyleAttribute(String k) {
        this.styleAttributes.remove(k);
    }

    void setCustomText(Text t) {
        String trimmed = trim(t.getText());
        if (t.getParent() != null)
            t.getParent().removeContent(t);
        this.text = new Text(trimmed);
    }

    void setTail(Text t) {
        this.tail = new Text(trim(t.getText()));
    }

    String getTail() {
        if (this.tail == null)
            return "";
        return this.tail.getText();
    }

    String getCustomText() {
        if (this.text == null)
            return "";
        return this.text.getText();
    }

    /**
     * Return a list of the children, as CustomElements instead of Elements. We are sure that all children
     * actually are of type CustomElement, as they are created in the function Utils.addChildrenToParent()
     *
     * @param returnDeletedChildren specifies whether 'deleted' children should be returned. This is only true
     *                              when called by the Outputter, which needs to show deleted children
     * @return The list of children as CustomElements
     */
    List<CustomElement> getCustomChildren(Boolean returnDeletedChildren) {
        ArrayList<CustomElement> result = new ArrayList<>();
        for (Element e : this.getChildren()) {
            if (returnDeletedChildren || !((CustomElement) e).isDeleted)
                result.add((CustomElement) e);
        }
        return result;
    }

    /**
     * Delete redundant spaces, tabs and newlines
     *
     * @param s The original string
     * @return The trimmed string
     */
    private String trim(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    /**
     * Add an action to this CustomElement
     * @param a The EditAction
     */
    void addAction(EditAction a) {
        if (this.actions == null)
            this.actions = new ArrayList<>();
        this.actions.add(a);
    }

    /**
     * Save a class to this CustomElement
     * @param c The class
     */
    private void addClass(String c) {
        String currentClass = this.getAttributeValue("class");
        currentClass = currentClass == null ? c : currentClass + " " + c;
        this.setAttribute("class", currentClass);
    }

    /**
     * Create a popuptext for this CustomElement. But only do so if the action is a visible action. Also save the action
     *
     * @param action The action
     * @param s      The popuptext
     */
    void addPopupText(EditAction action, String s) {
        if (action.isVisible()) {
            if (this.popupTexts == null)
                this.popupTexts = new ArrayList<>();
            this.popupTexts.add(s);
        }
        this.addAction(action);
    }

    /**
     * Get the parent elements as CustomElement
     *
     * @return the parent
     */
    CustomElement getCustomParentElement() {
        Element parent = this.getParentElement();
        return (parent == null || ((CustomElement)parent).isDeleted) ? null : (CustomElement)parent;
    }

    /**
     * "Delete" a child. Actually it is not being deleted, but marked as being deleted. We can then still return it
     * in the function getCustomChildren
     *
     * @return All Edit Actions of all children of this node. These EditActions will be removed of the result of
     * the Differ, as children of a deleted node don't need any EditActions.
     */
    ArrayList<EditAction> delete() {
        ArrayList<EditAction> childrensActions = new ArrayList<>();
        this.isDeleted = true;
        for (CustomElement child : this.getCustomChildren(true))
            childrensActions.addAll(child.unmarkAsDeleted());
        return childrensActions;
    }

    /**
     * This function is called on elements of which the parent is being deleted. When the parent is deleted, the
     * actions on this element don't matter anymore. So we delete and return them recursively on the children
     *
     * @return The list of EditActions this node had before this unmark. This list will be removed from the result
     * of the Differ, as they are removed from this element.
     */
    ArrayList<EditAction> unmarkAsDeleted() {
        ArrayList<EditAction> result = new ArrayList<>();
        this.isDeleted = false;
        this.popupTexts = null;
        if (this.actions != null)
            result = new ArrayList(this.actions);
        this.actions = null;
        for (CustomElement child : this.getCustomChildren(true))
            result.addAll(child.unmarkAsDeleted());
        return result;
    }

    /**
     * Adds classes "edited"  and "deleted" if needed
     */
    void addClassesForOutputter() {
        if (this.popupTexts != null) {
            if (this.isDeleted)
                this.addClass("deleted");
            this.addClass("edited");
        }
    }

    Boolean hasPopupTexts() {
        return this.popupTexts != null;
    }

    /**
     * Return the popuptexts as a <ul> string
     *
     * @return The html
     */
    String popupTextsToHtml() {
        StringBuilder sb = new StringBuilder(" <ul>");
        if (this.popupTexts != null)
            for (String text : this.popupTexts)
                sb.append(String.format("<li>%s</li>", text));
        return sb.append("</ul>").toString();
    }

    /**
     * Return the index of this CustomElement in its parent, taking into account deleted children as well. It is only
     * used by the TreeEditor to create a PopupText
     * @return The position
     */
    Integer getPositionInParent() {
        return this.getCustomParentElement().getCustomChildren(true).indexOf(this);
    }

    /**
     * Check whether this element has all characteristics, except for the parent element, equal to another element
     *
     * @param other                 The other element
     * @param returnDeletedChildren Indictates whether deleted children should be taken into account
     * @return The result.
     */
    Boolean isEqualTo(CustomElement other, Boolean returnDeletedChildren) {
        if (!this.getCustomText().equals(other.getCustomText()))
            return false;
        if (!this.getTail().equals(other.getTail()))
            return false;
        for (Attribute attr : this.getAttributes()) {
            Attribute otherAttr = other.getAttribute(attr.getName());
            if (otherAttr == null || !attr.getValue().equals(otherAttr.getValue()))
                return false;
        }
        for (Map.Entry<String, String> style : this.styleAttributes.entrySet()) {
            String otherStyle = other.styleAttributes.get(style.getKey());
            if (otherStyle == null || !style.getValue().equals(otherStyle))
                return false;
        }
        List<CustomElement> thisChildren = this.getCustomChildren(returnDeletedChildren);
        List<CustomElement> otherChildren = other.getCustomChildren(returnDeletedChildren);
        if (thisChildren.size() != otherChildren.size())
            return false;
        for (int i = 0; i < thisChildren.size(); i++) {
            if (!thisChildren.get(i).isEqualTo(otherChildren.get(i), returnDeletedChildren))
                return false;
        }
        return true;
    }

    /**
     * Indicates whether this element is an unidentifiable leaf. This means that it has no children and no
     * characteristics. The Matcher and Differ skip these kind of elements
     *
     * @return The result
     */
    Boolean isUnidentifiableLeaf() {
        return this.getCustomText().equals("") && this.getTail().equals("") &&
                !this.hasStyle() && this.getAttributes().size() <= 0 &&
                this.getCustomChildren(true).size() <= 0;

    }

    /**
     * Return the number of leafs in the subtree rooted at this node
     *
     * @return The value
     */
    int getNrOfLeafsInSubtree() {
        if (this.nrOfLeafsInSubtree == -1) {
            if (this.getCustomChildren(false).size() <= 0)//This is a leaf
                this.nrOfLeafsInSubtree = 1;
            else {
                int sum = 0;
                for (CustomElement e : this.getCustomChildren(false))
                    sum += e.getNrOfLeafsInSubtree();
                this.nrOfLeafsInSubtree = sum;
            }
        }
        return this.nrOfLeafsInSubtree;
    }

    /**
     * Count all deleted children before a certain element.
     * @param child the element
     * @return The nr of deleted nodes before "child"
     */
    int getNrOfDeletedChildrenBeforeChild(CustomElement child){
        int count = 0;
        for(CustomElement e : this.getCustomChildren(true)){
            if(e.equals(child))
                break;
            if(e.isDeleted)
                count++;
        }
        return count;
    }

    @Override
    public Element addContent(int index, Content child) {
        int adjustedIndex = index;
        int currentAdjustedPos = 0;
        int i = 0;
        for(CustomElement e : this.getCustomChildren(true)){
            if(currentAdjustedPos == index)
                break;
            else if(e.isDeleted)
                adjustedIndex++;
            else
                currentAdjustedPos++;
        }
        try{
            return super.addContent(adjustedIndex, child);
        }catch(Exception e){
            return super.addContent(index, child);
        }
    }

}

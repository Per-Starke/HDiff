import org.jdom2.Text;

import java.util.ArrayList;

/**
 * This class is responsible for editing the left tree when EditActions are created. It edits the tree and creates
 * popup texts
 */
class TreeEditor {
    /**
     * The left tree
     */
    private CustomElement root;

    TreeEditor(CustomElement root) {
        this.root = root;
    }


    /**
     * Apply the InsertNode action
     *
     * @param action The action
     */
    void insertNode(EditAction action) {
        action.getTarget().addPopupText(action, "This node is inserted");
        action.getNewParent().addContent(action.getPosition(), action.getTarget());
    }

    /**
     * Apply the RenameNode action
     *
     * @param action The action
     */
    void renameNode(EditAction action) {
        action.getTarget().addPopupText(action, String.format("Tag rename: %s->%s", action.getTarget().getName(), action.getValue()));
        action.getTarget().setName(action.getValue());
    }

    /**
     * Apply the MoveNode action
     *
     * @param action The action
     */
    void moveNode(EditAction action) {
        if (action.getNewParent().equals(action.getTarget().getCustomParentElement()))
            action.getTarget().addPopupText(action, String.format("Moved from %s(%d) to %s(%d)", action.getTarget().getCustomParentElement().getName(), action.getTarget().getCustomParentElement().getCustomChildren(false).indexOf(action.getTarget()), action.getNewParent().getName(), action.getPosition()));
        else
            action.getTarget().addPopupText(action, "Moved to a new parent");
        //We clone the target, and mark that clone as deleted. It won't be considered in the algorithm anymore,
        // but the output file will show the cloned node as being moved to another position. So this node will be shown
        // in  its old and in its new position.
        CustomElement oldNode = action.getTarget().clone();
        oldNode.delete();
        oldNode.addPopupText(action, "This node is moved");
        action.getTarget().getCustomParentElement().addContent(action.getTarget().getCustomParentElement().getCustomChildren(false).indexOf(action.getTarget()), oldNode);

        action.getTarget().getCustomParentElement().removeContent(action.getTarget());
        action.getNewParent().addContent(action.getPosition(), action.getTarget());
    }

    /**
     * Apply the UpdateTextIn action
     * We don't add a popuptext for this option, as the new text contains <ins> and <del> elements, which already
     * ensure that this change is shown to the user
     *
     * @param action The action
     */
    void updateText(EditAction action) {
        action.getTarget().setCustomText(new Text(action.getValue()));
        action.getTarget().addAction(action);
    }

    /**
     * Apply the UpdateTail action
     * We don't add a popuptext for this option, as the new text contains <ins> and <del> elements, which already
     * ensure that this change is shown to the user
     *
     * @param action The action
     */
    void updateTail(EditAction action) {
        action.getTarget().setTail(new Text(action.getValue()));
        action.getTarget().addAction(action);
    }

    /**
     * Apply the DeleteNode action
     *
     * @param action The action
     * @return Contains all editactions of all children of the action on which this node is performed. We remove this
     * list from the resulting list of edit actions, as children of a deleted node don't need any edit actions on them
     */
    ArrayList<EditAction> deleteNode(EditAction action) {
        action.getTarget().addPopupText(action, "This node is deleted");
        return action.getTarget().delete();
    }

    /**
     * Apply a list of actions of type UpdateAttribute, InsertAttribute, RenameAttribute, and DeleteAttribute
     *
     * @param actions The list of actions
     */
    void updateAttributes(ArrayList<EditAction> actions) {
        for (EditAction a : actions) {
            switch (a.getType()) {
                case UpdateAttribute:
                    a.getTarget().addPopupText(a, String.format("Updated %s: %s->%s", a.getKey(), a.getTarget().getAttributeValue(a.getKey()), a.getValue()));
                    updateAttribute(a.getTarget(), a.getKey(), a.getValue());
                    break;
                case InsertAttribute:
                    a.getTarget().addPopupText(a, String.format("Inserted: %s->%s", a.getKey(), a.getValue()));
                    updateAttribute(a.getTarget(), a.getKey(), a.getValue());
                    break;
                case RenameAttribute:
                    a.getTarget().addPopupText(a, String.format("Renamed: %s->%s", a.getKey(), a.getValue()));
                    updateAttribute(a.getTarget(), a.getValue(), a.getTarget().getAttributeValue(a.getKey()));
                    a.getTarget().removeAttribute(a.getKey());
                    break;
                case DeleteAttribute:
                    a.getTarget().addPopupText(a, String.format("Deleted: %s", a.getKey()));
                    a.getTarget().removeAttribute(a.getKey());
                    break;
            }
        }
    }

    /**
     * Update the attribute of a node
     *
     * @param n The node
     * @param k The attribute key
     * @param v The attribute value
     */
    void updateAttribute(CustomElement n, String k, String v) {
        n.setAttribute(k, v);
    }

    /**
     * Update a style attribute of a node
     *
     * @param n The node
     * @param k The attribute key
     * @param v The attribute value
     */
    void updateStyleAttribute(CustomElement n, String k, String v) {
        n.setStyleAttribute(k, v);
    }

    /**
     * Apply a list of actions of type UpdateStyleAttribute, RenameStyleAttribute, InsertStyleAttribute, and DeleteStyleAttribute
     *
     * @param actions The list of actions
     */
    void updateStyles(ArrayList<EditStyleAction> actions) {
        for (EditStyleAction a : actions) {
            switch (a.getStyleType()) {
                case UpdateStyleAttribute:
                    a.getTarget().addPopupText(a, String.format("Updated style %s: %s->%s", a.getKey(), a.getTarget().getAttributeValue(a.getKey()), a.getValue()));
                    updateStyleAttribute(a.getTarget(), a.getKey(), a.getValue());
                    break;
                case RenameStyleAttribute:
                    a.getTarget().addPopupText(a, String.format("Renamed style: %s->%s", a.getKey(), a.getValue()));
                    updateStyleAttribute(a.getTarget(), a.getValue(), a.getTarget().getAttributeValue(a.getKey()));
                    a.getTarget().removeStyleAttribute(a.getKey());
                    break;
                case InsertStyleAttribute:
                    a.getTarget().addPopupText(a, String.format("Inserted style: %s->%s", a.getKey(), a.getValue()));
                    updateStyleAttribute(a.getTarget(), a.getKey(), a.getValue());
                    break;
                case DeleteStyleAttribute:
                    a.getTarget().addPopupText(a, String.format("Deleted style: %s", a.getKey()));
                    a.getTarget().removeStyleAttribute(a.getKey());
                    break;
            }
        }
    }

    public CustomElement getRoot() {
        return this.root;
    }
}

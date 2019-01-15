import org.jdom2.Attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * This class diffs attributes of a node
 */
class AttributeDiffer {
    private CustomElement left, right;
    private ArrayList<EditAction> result;
    /**
     * Holds all keys that do occur in left but don't in right
     */
    private ArrayList<String> removedKeys;
    /**
     * Holds all keys that occur in both left and right
     */
    private ArrayList<String> commonKeys;
    /**
     * Holds all attributes that occur in right but don't in left
     */
    private HashMap<String, String> newAttributes;
    private TreeEditor editor;

    private HashMap<String, String> leftAttributes;
    private HashMap<String, String> rightAttributes;

    AttributeDiffer(TreeEditor editor) {
        this.editor = editor;
    }

    /**
     * This function performs the 5 steps as explained in the paper
     * It edits the left tree at the end
     *
     * @param left  left node
     * @param right right node
     * @return List of EditAction to change left's attributes into right's attributes
     */
    ArrayList<EditAction> diff(CustomElement left, CustomElement right) throws Exception {
        this.left = left;
        this.right = right;
        result = new ArrayList<>();
        this.removedKeys = new ArrayList<>();
        this.commonKeys = new ArrayList<>();

        //First create EditActions for the style attribute, using the StyleDiffer
        diffStyles();
        if (left.hasAttributes() || right.hasAttributes()) {
            //Save all the current attributes in the needed format
            getKeyLists();

            update();
            //Align: Not needed here, we don't care about the order of attributes.
            // Move: Check if any of the new attributes have the same value as the removed attributes.
            // If they do, it's actually a renaming, which is cheaper than a remove + insert.
            move();
            insert();
            delete();
            //Apply the EditActions to left
            editor.updateAttributes(this.result);
        }
        return this.result;
    }

    /**
     * Performs the diffing algorithm on the style attribute, using the StyleDiffer
     *
     * @throws Exception Thrown when an incorrect EditStyleAction is created
     */
    private void diffStyles() throws Exception {
        result.addAll(new StyleDiffer(editor).diff(this.left, this.right));
    }

    /**
     * Initialise lists
     */
    private void getKeyLists() {
        leftAttributes = new HashMap<>();
        for (Attribute a : left.getAttributes())
            leftAttributes.put(a.getName(), a.getValue());
        rightAttributes = new HashMap<>();
        for (Attribute a : right.getAttributes())
            rightAttributes.put(a.getName(), a.getValue());
        //Skip the style attributes, they are handled by the StyleDiffer
        leftAttributes.remove("style");
        rightAttributes.remove("style");
        this.createAttributeLists();
        Collections.sort(commonKeys);
        Collections.sort(removedKeys);
    }

    /**
     * Initialise lists
     */
    private void createAttributeLists() {
        this.newAttributes = new HashMap<>(this.rightAttributes);
        for (Map.Entry<String, String> leftAttribute : this.leftAttributes.entrySet()) {
            if (this.rightAttributes.containsKey(leftAttribute.getKey())) {
                this.commonKeys.add(leftAttribute.getKey());
                this.newAttributes.remove(leftAttribute.getKey());
            } else
                this.removedKeys.add(leftAttribute.getKey());
        }
    }

    /**
     * Update attributes with new values
     *
     * @throws Exception Thrown when an incorrect EditAction is created
     */
    private void update() throws Exception {
        for (String key : commonKeys)
            if (!leftAttributes.get(key).equals(rightAttributes.get(key))) {
                String newValue = rightAttributes.get(key);
                result.add(new EditAction(EditAction.ActionType.UpdateAttribute, left, key, newValue));
                //Update the left attributes list
                leftAttributes.put(key, newValue);
            }
    }

    /**
     * Update changed keys
     *
     * @throws Exception Thrown when an incorrect EditAction is created
     */
    private void move() throws Exception {
        for (String removedKey : removedKeys) {
            String value = leftAttributes.get(removedKey);
            if (newAttributes.containsValue(value)) {
                String newKey = Utils.valueToKey(rightAttributes, value);
                result.add(new EditAction(EditAction.ActionType.RenameAttribute, left, removedKey, newKey));
                //Remove it from new attributes, as we've already handled this key
                newAttributes.remove(newKey);
                //Update the left attributes list
                leftAttributes.put(newKey, value);
                leftAttributes.remove(removedKey);
            }
        }
    }

    /**
     * Create new attributes
     *
     * @throws Exception Thrown when an incorrect EditAction is created
     */
    private void insert() throws Exception {
        for (Map.Entry<String, String> newAttribute : newAttributes.entrySet()) {
            result.add(new EditAction(EditAction.ActionType.InsertAttribute, left, newAttribute.getKey(), newAttribute.getValue()));
            //Update the left node
            leftAttributes.put(newAttribute.getKey(), newAttribute.getValue());
        }
    }

    /**
     * Delete removed attributes
     *
     * @throws Exception Thrown when an incorrect EditAction is created
     */
    private void delete() throws Exception {
        for (String key : removedKeys) {
            //If we have moved this attribute, don't delete it
            if (!leftAttributes.containsKey(key))
                continue;
            result.add(new EditAction(EditAction.ActionType.DeleteAttribute, left, key));
            //Update the left attributes list
            leftAttributes.remove(key);
        }
    }
}

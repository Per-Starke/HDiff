import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class diffs style attributes of a node
 */
class StyleDiffer {
    private CustomElement left, right;
    private ArrayList<EditStyleAction> result;
    /**
     * Holds all keys that do occur in left but don't in right
     */
    private ArrayList<String> removedAttributes;
    /**
     * Holds all keys that occur in both left and right
     */
    private ArrayList<String> commonAttributes;
    /**
     * Holds all attributes that occur in right but don't in left
     */
    private HashMap<String, String> newAttributes;
    private HashMap<String, String> leftAttributes;
    private HashMap<String, String> rightAttributes;
    private TreeEditor editor;

    StyleDiffer(TreeEditor editor) {
        this.editor = editor;
    }

    /**
     * This function performs the 5 steps as explained in the paper, applied on style attributes
     * It edits the left tree at the end
     *
     * @param left  left node
     * @param right right node
     * @return List of required edit actions to change left's style attributes into right's style attributes
     */
    ArrayList<EditStyleAction> diff(CustomElement left, CustomElement right) throws Exception {
        this.left = left;
        this.right = right;
        result = new ArrayList<>();
        if (left.hasStyle() || right.hasStyle()) {
            //Fist save all the current attributes in the needed format
            getKeyLists();

            update();
            //Align: Not needed here, we don't care about the order of style attributes.
            // Move: Check if any of the new attributes have the same value as the removed attributes.
            // If they do, it's actually a renaming, which is cheaper than a remove + insert.
            move();
            insert();
            delete();
            //Apply the EditActions to left
            editor.updateStyles(this.result);
        }
        return this.result;

    }

    /**
     * Initialise lists
     */
    private void getKeyLists() {
        leftAttributes = new HashMap<>(this.left.getStyleAttributes());
        rightAttributes = new HashMap<>(this.right.getStyleAttributes());
        commonAttributes = new ArrayList<>();
        removedAttributes = new ArrayList<>();
        this.createAttributeLists();
        Collections.sort(commonAttributes);
        Collections.sort(removedAttributes);
    }

    /**
     * Initialise lists
     */
    private void createAttributeLists() {
        this.newAttributes = new HashMap<>(this.rightAttributes);
        for (Map.Entry<String, String> leftAttribute : this.leftAttributes.entrySet()) {
            if (this.rightAttributes.containsKey(leftAttribute.getKey())) {
                this.commonAttributes.add(leftAttribute.getKey());
                this.newAttributes.remove(leftAttribute.getKey());
            } else
                this.removedAttributes.add(leftAttribute.getKey());
        }
    }

    /**
     * Update styles attributes with new values
     *
     * @throws Exception Thrown when an incorrect EditStyleAction is created
     */
    private void update() throws Exception {
        for (String key : commonAttributes)
            if (!leftAttributes.get(key).equals(rightAttributes.get(key))) {
                String newValue = rightAttributes.get(key);
                result.add(new EditStyleAction(EditStyleAction.StyleActionType.UpdateStyleAttribute, left, key, newValue));
                //Update the left attributes list
                leftAttributes.put(key, newValue);
            }
    }

    /**
     * Update changed keys
     *
     * @throws Exception Thrown when an incorrect EditStyleAction is created
     */
    private void move() throws Exception {
        for (String removedKey : removedAttributes) {
            String value = leftAttributes.get(removedKey);
            if (newAttributes.containsValue(value)) {
                String newKey = Utils.valueToKey(rightAttributes, value);
                result.add(new EditStyleAction(EditStyleAction.StyleActionType.RenameStyleAttribute, left, removedKey, newKey));
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
     * @throws Exception Thrown when an incorrect EditStyleAction is created
     */
    private void insert() throws Exception {
        for (Map.Entry<String, String> entry : newAttributes.entrySet()) {
            result.add(new EditStyleAction(EditStyleAction.StyleActionType.InsertStyleAttribute, this.left, entry.getKey(), entry.getValue()));
            //Update the left node
            leftAttributes.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Delete removed attributes
     *
     * @throws Exception Thrown when an incorrect EditStyleAction is created
     */
    private void delete() throws Exception {
        for (String key : removedAttributes) {
            //If we have moved this attribute, don't delete it
            if (!leftAttributes.containsKey(key))
                continue;
            result.add(new EditStyleAction(EditStyleAction.StyleActionType.DeleteStyleAttribute, left, key));
            //Update the left attributes list
            leftAttributes.remove(key);
        }
    }

}

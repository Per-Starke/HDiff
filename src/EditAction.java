import org.apache.commons.lang3.ArrayUtils;

/**
 * This object defines an action to perform on an (style)attribute of an Element
 */
class EditAction {
    /**
     * We don't want to show edits to the following tags, as elements with these tags are invisible to the user
     */
    private static final String[] invisibleTags = new String[]{"script", "meta",
            "base", "style", "noscript", "comment", "head", "body", "html", "header", "main", "param", "progress",
    };

    public enum ActionType {
        InsertNode, DeleteNode, MoveNode, RenameNode,
        InsertAttribute, DeleteAttribute, RenameAttribute, UpdateAttribute, UpdateStyleAttribute,
        UpdateTextIn, UpdateTail
    }


    /**
     * The type of action
     */
    protected ActionType type;
    /**
     * The targeted node
     */
    protected CustomElement target;
    /**
     * The new parent of the targeted node; might be null
     */
    private CustomElement newParent;
    /**
     * The position in the new parent of the targeted node; might be null
     */
    private Integer position;
    /**
     * The attribute key; might be null
     */
    String key;
    /**
     * The attribute value, or the new attribute key; might be null
     */
    protected String value;

    EditAction(ActionType type, Object... data) throws Exception {
        this.type = type;
        try {
            switch (this.type) {
                case InsertNode:
                    this.target = (CustomElement) data[0];//The node to be inserted: a copy of the node in the right
                    // tree
                    this.newParent = (CustomElement) data[1];//The parent at which the node should be inserted
                    this.position = (Integer) data[2];
                    break;
                case UpdateAttribute:
                    this.target = (CustomElement) data[0];
                    this.key = (String) data[1];
                    this.value = (String) data[2];
                    break;
                case InsertAttribute:
                    this.target = (CustomElement) data[0];
                    this.key = (String) data[1];
                    this.value = (String) data[2];
                    break;
                case RenameAttribute:
                    this.target = (CustomElement) data[0];
                    this.key = (String) data[1];
                    this.value = (String) data[2];
                    break;
                case DeleteAttribute:
                    this.target = (CustomElement) data[0];
                    this.key = (String) data[1];
                    break;
                case RenameNode:
                    this.target = (CustomElement) data[0];
                    this.value = (String) data[1];
                    break;
                case MoveNode:
                    this.target = (CustomElement) data[0];
                    this.newParent = (CustomElement) data[1];
                    this.position = (Integer) data[2];
                    break;
                case UpdateTextIn:
                case UpdateTail:
                    this.target = (CustomElement) data[0];
                    this.value = (String) data[1];
                    break;
                case DeleteNode:
                    this.target = (CustomElement) data[0];

            }
        } catch (Exception e) {
            throw new Exception(String.format("Setting incorrect data for %s!", this.type.toString()));
        }
    }

    public ActionType getType() {
        return this.type;
    }

    public CustomElement getTarget() {
        return this.target;
    }

    CustomElement getNewParent() {
        return newParent;
    }

    Integer getPosition() {
        return this.position;
    }

    public String getValue() {
        return this.value;
    }

    public String getKey() {
        return this.key;
    }

    /**
     * Method to indicate whether this action with its variables is an action that is visible to the user
     * Todo: Enhance this method, think of more types that are never visible
     *
     * @return The result
     */
    Boolean isVisible() {
        CustomElement e = this.getTarget();
        if (ArrayUtils.contains(invisibleTags, e.getName()))
            return false;
        //Add more cases here!
        return true;
    }
}


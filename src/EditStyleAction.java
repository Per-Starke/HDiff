/**
 * This object defines an action to perform on a style attribute of an Element
 */
class EditStyleAction extends EditAction {
    public enum StyleActionType {
        InsertStyleAttribute, DeleteStyleAttribute, RenameStyleAttribute, UpdateStyleAttribute;
    }

    /**
     * The type of action
     */
    private StyleActionType styleType;


    EditStyleAction(StyleActionType styleType, Object... data) throws Exception {
        super(ActionType.UpdateStyleAttribute);
        this.styleType = styleType;
        try {
            switch (this.styleType) {
                case UpdateStyleAttribute:
                    this.target = (CustomElement) data[0];
                    this.key = (String) data[1];
                    this.value = (String) data[2];
                    break;
                case RenameStyleAttribute:
                    this.target = (CustomElement) data[0];
                    this.key = (String) data[1];
                    this.value = (String) data[2];
                    break;
                case InsertStyleAttribute:
                    this.target = (CustomElement) data[0];
                    this.key = (String) data[1];
                    this.value = (String) data[2];
                    break;
                case DeleteStyleAttribute:
                    this.target = (CustomElement) data[0];
                    this.key = (String) data[1];
                    break;
            }
        } catch (Exception e) {
            throw new Exception(String.format("Setting incorrect data for %s!", this.styleType.toString()));
        }
    }

    StyleActionType getStyleType() {
        return this.styleType;
    }


}

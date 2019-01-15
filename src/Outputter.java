import org.jdom2.Attribute;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * The task of this class is to write a CustomElement in HTML format to a file
 */
class Outputter {
    private PrintWriter out;

    void output(CustomElement root, String filename) throws IOException {
        openWriter(filename);
        write(root, 0);
        printLine("</html>", 0);
        closeWriter();
    }

    /**
     * Write an element to the outputfile
     *
     * @param root  The element
     * @param level Used for indenting spaces for different levels inside the document
     */
    void write(CustomElement root, Integer level) {
        if(root.getName().equals("style"))
            System.out.println("");
        root.addClassesForOutputter();
        printLine(open(root) + attributes(root) + styles(root) + popupTexts(root) + close(), level);
        level++;
        printLine(root.getCustomText(), level);
        for (CustomElement child : root.getCustomChildren(true))
            write(child, level);
        level--;
        printLine(finish(root), level);
        printLine(root.getTail(), level);
    }

    /**
     * Return the popuptexts as HTML Attribute
     *
     * @param root The element
     * @return The popuptexts
     */
    String popupTexts(CustomElement root) {
        if (root.hasPopupTexts())
            return String.format("popup=\"%s\"", root.popupTextsToHtml());
        return "";
    }

    /**
     * Return the styles as an HTML attribute
     *
     * @param root The element
     * @return The styles
     */
    String styles(CustomElement root) {
        if (root.getStyleAttributes().size() <= 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : root.getStyleAttributes().entrySet())
            sb.append(String.format("%s:%s; ", entry.getKey(), entry.getValue()));
        return String.format("style=\"%s\"", sb.toString());
    }

    /**
     * Return the attributes in html format
     *
     * @param root The element
     * @return The attributes in format "key1=\"val1\" key2=\"val2\" "
     */
    String attributes(CustomElement root) {
        StringBuilder sb = new StringBuilder();
        for (Attribute a : root.getAttributes())
            sb.append(String.format("%s=\"%s\" ", a.getName(), a.getValue()));
        return sb.toString();
    }

    /**
     * Open an element
     *
     * @param e The element
     * @return The opening tag in format "<tagname "
     */
    String open(CustomElement e) {
        return String.format("<%s ", e.getName());
    }

    /**
     * Close an element
     *
     * @return ">"
     */
    String close() {
        return ">";
    }

    /**
     * Close an element after all of its contents and attributes are written
     *
     * @param root The element
     * @return The closure of an element in format "</tagname>"
     */
    String finish(CustomElement root) {
        return String.format("</%s>", root.getName());
    }

    /**
     * Open the file to be written to, using a template importing the js and css
     *
     * @param filename The filename
     * @throws IOException Is possibly thrown when opening the file
     */
    private void openWriter(String filename) throws IOException {
        Files.copy(new File("htmls/template.html").toPath(), new File(filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
        FileWriter fw = new FileWriter(filename, true);
        BufferedWriter bw = new BufferedWriter(fw);
        this.out = new PrintWriter(bw);
    }

    /**
     * Close the output file
     */
    private void closeWriter() {
        this.out.close();
    }

    /**
     * Print a line of text.
     *
     * @param s     The text
     * @param level The level of the texts. This is used to prepend 4 spaces per level
     */
    private void printLine(String s, Integer level) {
        if (!s.equals("")) {
            StringBuilder spaces = new StringBuilder();
            for (int i = 0; i < level * 4; i++) spaces.append(" ");
            out.println(spaces + s);
        }
    }
}

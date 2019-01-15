import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Take a snapshot of a website
 */
public class Snapshotter {
    /**
     * Login url
     */
    private String ul;
    /**
     * Data url
     */
    private String ud;
    /**
     * Username
     */
    private String username;
    /**
     * Password
     */
    private String password;
    /**
     * Attribute key used to uniquely identify <a> elements
     */
    private String k;
    /**
     * Folder in which the snapshot is saved
     */
    private String outputfolder;
    /**
     * Errors that occur durring snapshotting
     */
    private HashMap<String, String> errors;

    public Snapshotter(){

    };

    public Snapshotter(String outputfolder){
        this.outputfolder = outputfolder;
    }

    HashMap<String, String> snapshot(String ul, String ud, String username, String password, String k, String outputfolder) throws IOException {
        this.ul = ul;
        this.ud = ud;
        this.username = username;
        this.password = password;
        this.k = k;
        this.outputfolder = outputfolder;
        HashMap<String, String> links = getLinks();
        errors = new HashMap<>();
        saveFiles(links);
        return errors;
    }

    /**
     * Save all filename->url mappings
     *
     * @param links The mapping
     */
    private void saveFiles(HashMap<String, String> links) {
        WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        for (Map.Entry<String, String> entry : links.entrySet()) {
            String content = getContent(webClient, entry.getKey(), entry.getValue());
            if (content != null)
                writeHtml(content, this.outputfolder + entry.getKey()/*.replaceAll("\\W+", "")*/);
        }
    }

    /**
     * Get the content of a url
     *
     * @param webClient The client, used to open the url
     * @param name      The filename where the HTML will be saved
     * @param url       The url
     * @return The resulting html
     */
    String getContent(WebClient webClient, String name, String url) {
        String content;
        try {
            Page p = webClient.getPage(url);
            if (p.isHtmlPage()) {
                HtmlPage page = (HtmlPage) p;
                WebResponse response = page.getWebResponse();
                if (response.getStatusCode() == 200) {
                    // Add a <base> element, such that relative image paths are loaded when opening the html file
                    DomNode head = page.querySelector("head");
                    if (head == null) {
                        head = page.createElement("head");
                        page.appendChild(head);
                    }
                    if (head.querySelector("base") == null) {
                        DomElement base = page.createElement("base");
                        base.setAttribute("href", new URL(new URL(url), "/").toString());
                        head.appendChild(base);
                    }
                    content = page.asXml();
                } else {
                    errors.put(name, "Wrong http response: " + response.getStatusCode());
                    return null;
                }
            } else {
                errors.put(name, "No html!");
                return null;
            }
        } catch (Exception e) {
            errors.put(name, e.getMessage());
            return null;
        }
        return content;
    }

    /**
     * Save html to a file
     *
     * @param html     The html
     * @param filename The filename
     */
    void writeHtml(String html, String filename) {
        OutputStream os = null;
        try {
            File file = new File(filename);
            file.createNewFile();
            os = new FileOutputStream(file);
            os.write(html.getBytes(), 0, html.length());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get all links present at ud, after login in at ul
     *
     * @return List of name->url mappings
     * @throws IOException Might be thrown while opening a url
     */
    private HashMap<String, String> getLinks() throws IOException {
        String base = new URL(new URL(this.ul), "/").toString();
        base = base.substring(0, base.length() - 1);
        HashMap<String, String> result = new HashMap<>();

        WebClient client = new WebClient(BrowserVersion.BEST_SUPPORTED);

        //Get login page, save variables
        client.getOptions().setJavaScriptEnabled(true);
        HtmlPage p = client.getPage(this.ul);
        DomNode form = p.getDocumentElement()
                .querySelectorAll("form[name=loginform]").get(0);
        String id = form.querySelectorAll("input[name=id]").get(0)
                .getAttributes().getNamedItem("value").getNodeValue();
        String checksum = form.querySelectorAll("input[name=swfrmsig]").get(0)
                .getAttributes().getNamedItem("value").getNodeValue();

        //Post login form
        String postUrl = base + form.getAttributes().getNamedItem("action").getNodeValue();
        client.getOptions().setJavaScriptEnabled(false);
        client.getPage(new WebRequest(new URL(String.format("%s?id=%s&password=%s&user=%s&swfrmsig=%s",
                postUrl, id, this.password, this.username, URLEncoder.encode(checksum, "UTF-8"))), HttpMethod.POST));

        //Get page containing <a> elements
        HtmlPage l = client.getPage(this.ud);
        for (DomNode node : l.getDocumentElement().querySelectorAll("a")) {
            Node attr = node.getAttributes().getNamedItem(this.k);
            result.put(attr == null ? node.getTextContent() : attr.getNodeValue(), node.getAttributes().getNamedItem("href").getNodeValue());
        }
        return result;
    }

    void snapShotTwoURLs(String f1, String f2, String u1, String u2){
        HashMap<String, String> map = new HashMap<>();
        map.put(f1, u1);
        map.put(f2, u2);
        this.saveFiles(map);
    }
}

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;

public class Main {

    /**
     * Three options here:
     * 1. Diff two files locally
     * 2. Create a snapshot
     * 3. Diff two snapshots
     * @param argv Needs to be implemented, such that the user can execute by commandline
     * @throws Exception .
     */
    public static void main(String[] argv) throws Exception {
        String dir = "examples/";
        String filename1  = "page3_old.html";//file
        String filename2  = "page3_new.html";//file
        String outputfile = "page3_output.html";//file
        String snapshot1  = "";//folder
        String snapshot2  = "";//folder
        String snapshotoutputdir = "";//folder
        String u1 = "http://www.cs.ru.nl/~freek/courses/rbo/lezingen_20162017.html";
        String u2 = "http://www.cs.ru.nl/~freek/courses/rbo/lezingen_20172018.html";
        (new Snapshotter(dir)).snapShotTwoURLs(filename1, filename2, u1, u2);
        //Diff two files
        diffFiles(dir + filename1, dir + filename2, dir + outputfile);
        //Create a snapshot
//        snapshot(
//                "http://localhost/web/login",
//                "http://localhost/web/webmanager?id=39016",
//                "Administrator",
//                "A6j2fna$_SZ54jRr",
//                null,
//                "snapshotoutputdir"
//                );
        //Diff two snapshots
//        diffSnapshots(snapshot1, snapshot2, snapshotoutputdir);

    }

    /**
     * Compute differences between two files
     *
     * @param filename1  File 1
     * @param filename2  File 2
     * @param outputfile Output file
     * @return List of actions
     */
    private static ArrayList<EditAction> diffFiles(String filename1, String filename2, String outputfile) throws Exception {
        Differ differ = new Differ(0.0, 0.0);
        return differ.diff(filename1, filename2, outputfile);
    }

    /**
     * Compute differences between two snapshots
     *
     * @param dir1      Directory holding snapshot 1
     * @param dir2      Directory holding snapshot 2
     * @param outputdir Output directory
     */
    private static void diffSnapshots(String dir1, String dir2, String outputdir) throws Exception {
        Iterator it = FileUtils.iterateFiles(new File(dir1), null, false);
        while (it.hasNext()) {
            File current = (File) it.next();
            String currentName = current.getName();
            File currentMatch = new File(dir2 + currentName);
            if (Files.isRegularFile(currentMatch.toPath()))
                diffFiles(current.getPath(), currentMatch.getPath(), outputdir + currentName);

        }
    }

    /**
     * Create a snaphost of a website
     *
     * @param ui    The login url
     * @param ud    The url of the page containing all links
     * @param uname The username
     * @param pw    The password
     * @param k     The uniquely identifying key
     * @param outputfolder The folder to which the snapshot is saved
     */
    private static void snapshot(String ui, String ud, String uname, String pw, String k, String outputfolder) throws IOException {
        new Snapshotter().snapshot(ui, ud, uname, pw, k, outputfolder);
    }
}

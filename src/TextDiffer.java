import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;

/**
 * This class is responsible for diffing text and tail attributes of CustomElements. It uses the SequenceMatcher
 * to find the opcodes. It then uses the opcodes to create <ins>'s and <del>'s
 */
public class TextDiffer {

    public String diff(String a, String b) {
        if (SimilarityCalculator.sim(a, b) < 0.5)
            return String.format("<del>%s</del><ins>%s</ins>", a, b);
        StringBuilder result = new StringBuilder();
        ArrayList<SequenceMatcher.Opcode> opcodes = (new SequenceMatcher(a, b)).getOpcodes();
        for (SequenceMatcher.Opcode code : opcodes) {
            switch (code.tag) {
                case "equal":
                    result.append(a, code.aStart, code.aEnd);
                    break;
                case "delete":
                    result.append(write_delete(a.substring(code.aStart, code.aEnd)));
                    break;
                case "insert":
                    result.append(write_insert(b.substring(code.bStart, code.bEnd)));
                    break;
                case "replace":
                    result.append(write_delete(a.substring(code.aStart, code.aEnd)));
                    result.append(write_insert(b.substring(code.bStart, code.bEnd)));
                    break;
            }
        }
        return result.toString();
    }

    private String write_delete(String s) {
        return String.format("<del>%s</del>", s);
    }

    private String write_insert(String s) {
        return String.format("<ins>%s</ins>", s);
    }
}

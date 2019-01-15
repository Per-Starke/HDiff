import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class implements get_opcodes() of the SequenceMatcher of Python's difflib library
 * (https://docs.python.org/2/library/difflib.html), excluding the is_junk() method.
 * It is used to find accurate differences in two strings. The input will be the getCustomText() or getTail() functions
 * of CustomElements. The differ will then not just mark a text as "Replaced", "Inserted", or "Deleted", but it
 * will create <ins>'s and <del>'s inside the text
 */
class SequenceMatcher {
    private String a, b;

    /**
     * For each distinct character x in b, charToIndices.get(x) is a list of the indices (into b)
     * at which x appears, so if b2j.get('a') = [2, 4], b.charAt(2).equals(b.charAt(4) && b.charAt(2).equals('a')
     */
    private HashMap<Character, ArrayList<Integer>> charToIndices;

    SequenceMatcher(String a, String b) {
        this.a = a;
        this.b = b;
        saveIndices();
    }

    /**
     * For each character in b, save at which indices it occurs
     */
    private void saveIndices() {
        this.charToIndices = new HashMap<>();
        for (int i = 0; i < this.b.length(); i++) {
            char c = b.charAt(i);
            ArrayList<Integer> list = charToIndices.getOrDefault(c, new ArrayList<>());
            list.add(i);
            this.charToIndices.put(c, list);
        }
    }

    /**
     * Get the opcodes to rewrite a to b.
     *
     * @return The opcodes
     */
    ArrayList<Opcode> getOpcodes() {
        ArrayList<Opcode> result = new ArrayList<>();
        ArrayList<MatchingBlock> blocks =
                mergeBlocks(this.getMatchingBlocks(0, this.a.length(), 0, this.b.length()));
        int lastI = 0;
        int lastJ = 0;
        for (MatchingBlock b : blocks) {
            String tag = "";
            if (lastI < b.i && lastJ < b.j)
                tag = "replace";
            else if (lastI < b.i)
                tag = "delete";
            else if (lastJ < b.j)
                tag = "insert";
            if (!tag.equals(""))
                result.add(new Opcode(tag, lastI, b.i, lastJ, b.j));
            lastI = b.i + b.n;
            lastJ = b.j + b.n;
            if (b.n > 0)
                result.add(new Opcode("equal", b.i, lastI, b.j, lastJ));
        }
        return result;
    }

    /**
     * Get all matching blocks in a.substring(alo, ahi) and b.substring(blo, bhi)
     *
     * @param alo Start index in a
     * @param ahi End index in a
     * @param blo Start index in b
     * @param bhi End index in b
     * @return The matching blocks
     */
    private ArrayList<MatchingBlock> getMatchingBlocks(int alo, int ahi, int blo, int bhi) {
        ArrayList<MatchingBlock> result = new ArrayList<>();
        MatchingBlock longestMatch = findLongestMatch(alo, ahi, blo, bhi);
        if (longestMatch.n > 0) {
            if (alo < longestMatch.i && blo < longestMatch.j)
                result.addAll(getMatchingBlocks(alo, longestMatch.i, blo, longestMatch.j));
            result.add(longestMatch);
            if (longestMatch.i + longestMatch.n < ahi && longestMatch.j + longestMatch.n < bhi)
                result.addAll(getMatchingBlocks(longestMatch.i + longestMatch.n, ahi, longestMatch.j + longestMatch.n, bhi));
        }
        return result;
    }

    /**
     * When the MatchingBlocks are created, merge MatchingBlocks that are next to each other
     *
     * @param blocks The blocks
     * @return The merged blocks
     */
    private ArrayList<MatchingBlock> mergeBlocks(ArrayList<MatchingBlock> blocks) {
        ArrayList<MatchingBlock> result = new ArrayList<>();
        if (blocks.size() > 0) {
            MatchingBlock previousBlock = blocks.remove(0);
            for (MatchingBlock b : blocks) {
                if (previousBlock.i + previousBlock.n == b.i && previousBlock.j + previousBlock.n == b.j)//These
                    // blocks are adjacent. so increase the previous block and continue
                    previousBlock.increaseSizeBy(b.n);
                else {//Not adjacent. Add previousBlock and continue with b as previousBlock
                    result.add(previousBlock);
                    previousBlock = b;
                }
            }
            result.add(previousBlock);
            result.add(new MatchingBlock(this.a.length(), this.b.length(), 0));
        }
        return result;
    }


    /**
     * Find the longest possible matching block in a.substring(alo, ahi) and a.substring(blo, bhi)
     *
     * @param alo The start of our substring in a
     * @param ahi The end of our substring in a
     * @param blo The start of our substring in b
     * @param bhi The end of our substring in b
     * @return The longest matching block
     */
    private MatchingBlock findLongestMatch(Integer alo, Integer ahi, Integer blo, Integer bhi) {
        int besti = alo;
        int bestj = blo;
        int bestsize = 0;
        //newBestAtJ contains the length of the longest match ending in a at i - 1 and in b at j
        HashMap<Integer, Integer> bestAtJ = new HashMap<>();
        for (int i = alo; i < ahi; i++) {
            char currentCharInA = a.charAt(i);
            HashMap<Integer, Integer> newBestAtJ = new HashMap<>();
            ArrayList<Integer> indicesOfCurrentCharInB = this.charToIndices.getOrDefault(currentCharInA, new ArrayList<>());
            for (Integer j : indicesOfCurrentCharInB) {
                //a.get(i).equals(b.get(j))
                if (j < blo)//This char falls out of our scope
                    continue;
                if (j >= bhi)//We've looked at all possible chars in our scope
                    break;
                int k = bestAtJ.getOrDefault(j - 1, 0) + 1;//If last char matched, increase longest
                // match by 1
                newBestAtJ.put(j, k);
                if (k > bestsize) {
                    besti = i - k + 1;
                    bestj = j - k + 1;
                    bestsize = k;
                }
            }
            //Update bestAtJ for this i, we don't need the old bestAtJ anymore
            bestAtJ = newBestAtJ;
        }
        return new MatchingBlock(besti, bestj, bestsize);
    }

    /**
     * Elements of this type indicate how a part of string a should be converted to a part of string b
     */
    class Opcode {
        /**
         * Indicates the type of change: replace, delete, insert, or equal (no change)
         */
        String tag;
        /**
         * Indicate the part in a at which this Opcode is applicable
         */
        Integer aStart, aEnd;
        /**
         * Indicate the part in b at which this Opcode is applicable
         */
        Integer bStart, bEnd;

        Opcode(String t, Integer as, Integer ae, Integer bs, Integer be) {
            this.tag = t;
            this.aStart = as;
            this.aEnd = ae;
            this.bStart = bs;
            this.bEnd = be;
        }
    }

    /**
     * Elements of this type indicate a part at which a is equal to b.
     */
    class MatchingBlock {
        /**
         * a.substring(i, i+n).equals(b.substring(j, j+n))
         */
        Integer i, j, n;

        MatchingBlock(Integer i, Integer j, Integer n) {
            this.i = i;
            this.j = j;
            this.n = n;
        }

        void increaseSizeBy(int extra) {
            n += extra;
        }

    }

}

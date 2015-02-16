import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;


/**
 * A visual representation for the number of times various words occur in a given document.
 *
 * In a tag cloud, the most common English words are usually ignored (e.g. the, a, of), since they
 * do not give any useful information about the topic of the document being visualized.
 *
 * @author Robert C Duvall
 * @author Christopher La Pilla
 */
public class TagCloud {
    // some basic defaults
    private static final int DEFAULT_NUM_GROUPS = 20;
    private static final int DEFAULT_MIN_FONT = 6;
    private static final int DEFAULT_INCREMENT = 4;
    private static final String DEFAULT_IGNORE_FILE = "common.txt";
    // regular expression that represents one or more digits or punctuation
    private static final String PUNCTUATION = "[\\d\\p{Punct}]+";

    // set of common words to ignore when displaying tag cloud
    private Set<String> myCommonWords;
    // words and the number of times each appears in the file
    private List<Entry<String, Integer>> myTagWords;


    /**
     * Constructs an empty TagCloud.
     */
    public TagCloud (Scanner ignoreWords) {
        myTagWords = new ArrayList<>();
        myCommonWords = new HashSet<>();
        // create list of words that should not be included in final word counts
        while (ignoreWords.hasNext()) {
            myCommonWords.add(sanitize(ignoreWords.next()));
        }
    }

    /**
     * Reads given text file and counts non-common words it contains.
     *
     * Each word read is converted to lower case with leading and trailing punctuation removed
     * before it is counted.
     */
    public void countWords (Scanner input) {
        Map<String, Integer> wordCounts = new HashMap<>();
        while (input.hasNext()) {
            String word = sanitize(input.next());
            if (isTaggable(word)) {
                if (wordCounts.containsKey(word)) {
                    wordCounts.put(word, wordCounts.get(word) + 1);
                }
                else {
                    wordCounts.put(word, 1);
                }
            }
        }
        myTagWords.addAll(wordCounts.entrySet());
    }

    /**
     * Sorts words alphabetically, keeping only those that appeared most often.
     */
    public void topWords (int numWordsToKeep, int groupSize) {
        // sort from most frequent to least
        myTagWords.sort(new Comparator<Entry<String, Integer>>() {
            @Override
            public int compare (Entry<String, Integer> a, Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        // keep only the top ones
        myTagWords.subList(numWordsToKeep, myTagWords.size()).clear();
        // convert frequencies into groups
        for (int k = 0; k < myTagWords.size(); k++) {
            Entry<String, Integer> word = myTagWords.get(k);
            myTagWords.set(k,
                           new SimpleEntry<String, Integer>(word.getKey(), word.getValue() / groupSize));
        }
        // sort alphabetically
        myTagWords.sort(new Comparator<Entry<String, Integer>>() {
            @Override
            public int compare (Entry<String, Integer> a, Entry<String, Integer> b) {
                return a.getKey().compareTo(b.getKey());
            }
        });
    }

    /**
     * Convert each word to appropriately sized font based on its frequency.
     */
    @Override
    public String toString () {
        StringBuilder result = new StringBuilder();
        result.append(HTMLPage.startPage(DEFAULT_NUM_GROUPS, DEFAULT_MIN_FONT, DEFAULT_INCREMENT));
        for (Entry<String, Integer> word : myTagWords) {
            result.append(HTMLPage.formatWord(word.getKey(), word.getValue()));
        }
        result.append(HTMLPage.endPage());
        return result.toString();
    }

    // Return true if the given word should be tagged
    private boolean isTaggable (String word) {
        return word.length() > 0 && !myCommonWords.contains(word);
    }

    // Return string with leading and trailing punctuation removed from the given word
    private String sanitize (String word) {
        return word.replaceFirst("^" + PUNCTUATION, "").replaceFirst(PUNCTUATION + "$", "")
                   .toLowerCase();
    }

    public static void main (String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: #words file");
        }
        else {
            try {
                TagCloud cloud = new TagCloud(
                                   new Scanner(TagCloud.class.getResourceAsStream(DEFAULT_IGNORE_FILE)));
                cloud.countWords(new Scanner(new File(args[1])));
                cloud.topWords(Integer.parseInt(args[0]), DEFAULT_NUM_GROUPS);
                System.out.println(cloud);
            }
            catch (FileNotFoundException e) {
                System.err.println("File not found: " + args[1]);
            }
        }
    }
}

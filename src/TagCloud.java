import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;


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
    // key regular expressions
    private static final String PUNCTUATION = "[\\d\\p{Punct}]+";
    private static final String END_OF_FILE = "\\Z";
    private static final String WHITESPACE = "\\s";

    // set of common words to ignore when displaying tag cloud
    private Set<String> myCommonWords;
    // words and the number of times each appears in the file
    private List<Entry<String, Integer>> myTagWords;


    /**
     * Constructs an empty TagCloud.
     */
    public TagCloud (Scanner ignoreWords) {
        // this value should never be null
        myTagWords = new ArrayList<>();
        // create list of words that should not be included in final word counts
        myCommonWords = new HashSet<>(readWords(ignoreWords));
    }

    /**
     * Create a word cloud from the given input.
     */
    public void makeCloud (Scanner input, int numWordsToKeep, int groupSize) {
        myTagWords = topWords(countWords(input), numWordsToKeep, groupSize);
    }

    /**
     * Convert each word to appropriately sized font based on its frequency.
     */
    @Override
    public String toString () {
        String words = myTagWords.stream()
                                 .map(HTMLPage::formatWord)
                                 .collect(Collectors.joining(" "));
        return HTMLPage.startPage(DEFAULT_NUM_GROUPS, DEFAULT_MIN_FONT, DEFAULT_INCREMENT) + 
               words +
               HTMLPage.endPage();
    }

    // Reads given text file and counts non-common words it contains.
    // Each word read is converted to lower case with leading and trailing punctuation removed
    // before it is counted.
    private List<Entry<String, Integer>> countWords (Scanner input) {
        final Map<String, Integer> wordCounts = new HashMap<>();
        readWords(input).forEach(w -> {
            if (isTaggable(w)) {
                wordCounts.put(w, wordCounts.getOrDefault(w, 0) + 1);
            }
        });
        return new ArrayList<Entry<String, Integer>>(wordCounts.entrySet());
    }

    // Sorts words alphabetically, keeping only those that appeared most often.
    private List<Entry<String, Integer>> topWords (List<Entry<String, Integer>> tagWords,
                                                   int numWordsToKeep,
                                                   int groupSize) {
        // sort from most frequent to least
        tagWords.sort(Comparator.comparing(Entry<String, Integer>::getValue).reversed());
        // keep only the top ones
        tagWords.subList(numWordsToKeep, tagWords.size()).clear();
        // convert frequencies into groups
        tagWords = tagWords.stream()
                           .map(w -> new SimpleEntry<String, Integer>(w.getKey(), w.getValue() / groupSize))
                           .collect(Collectors.toList());
        // sort alphabetically
        tagWords.sort(Comparator.comparing(Entry<String, Integer>::getKey));
        return tagWords;
    }

    // Return true if the given word should be tagged
    private boolean isTaggable (String word) {
        return word.length() > 0 && !myCommonWords.contains(word);
    }

    // Remove the leading and trailing punctuation from the given word
    private static String sanitize (String word) {
        return word.replaceFirst("^" + PUNCTUATION, "")
                   .replaceFirst(PUNCTUATION + "$", "")
                   .toLowerCase();
    }

    // Remove the leading and trailing punctuation from the given word
    private List<String> readWords (Scanner input) {
        List<String> words = Arrays.asList(input.useDelimiter(END_OF_FILE).next().split(WHITESPACE));
        return words.stream().map(TagCloud::sanitize)
                    .collect(Collectors.toList());
    }


    public static void main (String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: #words file");
        }
        else {
            try {
                TagCloud cloud = new TagCloud(
                                     new Scanner(TagCloud.class.getResourceAsStream(DEFAULT_IGNORE_FILE)));
                cloud.makeCloud(new Scanner(new File(args[1])), 
                                Integer.parseInt(args[0]),
                                DEFAULT_NUM_GROUPS);
                System.out.println(cloud);
            }
            catch (FileNotFoundException e) {
                System.err.println("File not found: " + args[1]);
            }
        }
    }
}

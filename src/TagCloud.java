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
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
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

    // words and the number of times each appears in the file
    private List<Entry<String, Integer>> myTagWords;


    /**
     * Constructs an empty TagCloud.
     */
    public TagCloud () {
        // this value should never be null
        myTagWords = new ArrayList<>();
    }

    /**
     * Reads given text file and counts non-common words it contains.
     *
     * Each word read is converted to lower case with leading and trailing punctuation removed
     * before it is counted.
     */
    public TagCloud countWords (Scanner input, Predicate<String> select) {
        final Map<String, Integer> wordCounts = new HashMap<>();
        readWords(input, TagCloud::sanitize, select).stream()
                 .forEach(w -> {
                     wordCounts.put(w, wordCounts.getOrDefault(w, 0) + 1);
                 });
        myTagWords.addAll(wordCounts.entrySet());
        return this;
    }

    /**
     * Sorts words alphabetically, keeping only those that appeared most often.
     */
    public TagCloud topWords (int numWordsToKeep, int groupSize) {
        // sort from most frequent to least
        myTagWords.sort(Comparator.comparing(Entry<String, Integer>::getValue).reversed());
        // keep only the top ones
        myTagWords.subList(numWordsToKeep, myTagWords.size()).clear();
        System.out.println(myTagWords);
        // convert frequencies into groups
        myTagWords = myTagWords.stream()
                               .map(w -> new SimpleEntry<String, Integer>(w.getKey(), w.getValue() / groupSize))
                               .collect(Collectors.toList());
        // sort alphabetically
        myTagWords.sort(Comparator.comparing(Entry<String, Integer>::getKey));
        return this;
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


    // Returns a function that returns true if the given word should be tagged
    private static Predicate<String> isTaggable (Scanner ignoreWords) {
        // set of common words to ignore when displaying tag cloud
        final Set<String> commonWords = new HashSet<>(readWords(ignoreWords,
                                                                TagCloud::sanitize,
                                                                x -> true));
        return new Predicate<String>() {
            @Override
            public boolean test (String word) {
                return word.length() > 0 && !commonWords.contains(word);
            }
        };
    }

    // Remove the leading and trailing punctuation from the given word
    private static String sanitize (String word) {
        return word.replaceFirst("^" + PUNCTUATION, "")
                   .replaceFirst(PUNCTUATION + "$", "")
                   .toLowerCase();
    }

    // Remove the leading and trailing punctuation from the given word
    private static List<String> readWords (Scanner input,
                                           UnaryOperator<String> xform,
                                           Predicate<String> select) {
        List<String> words = Arrays.asList(input.useDelimiter(END_OF_FILE).next().split(WHITESPACE));
        return words.stream().map(xform).filter(select).collect(Collectors.toList());
    }


    public static void main (String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: #words file");
        }
        try {
            TagCloud cloud = new TagCloud()
                .countWords(new Scanner(new File(args[1])),
                                        isTaggable(new Scanner(TagCloud.class.getResourceAsStream(DEFAULT_IGNORE_FILE))))
                .topWords(Integer.parseInt(args[0]), DEFAULT_NUM_GROUPS);
            System.out.println(cloud);
        }
        catch (FileNotFoundException e) {
            System.err.println("File not found: " + args[1]);
        }
    }
}

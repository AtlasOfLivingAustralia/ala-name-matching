package au.org.ala.checklist.lucene.analyzer;

import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * A custom KeywordAnalyzer that converts the text to lowercase before tokenizing
 * the complete string as one token
 *
 * @author Natasha
 */
public final class LowerCaseKeywordAnalyzer extends Analyzer{

    @Override
    public  TokenStream tokenStream(String fieldName, Reader reader) {
         //convert to lowercase
        return new LowerCaseFilter(new KeywordTokenizer(reader));
    }

}

package au.org.ala.names.lucene.analyzer;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.Version;

/**
 * A custom KeywordAnalyzer that converts the text to lowercase before tokenizing
 * the complete string as one token
 *
 * @author Natasha
 */
public final class LowerCaseKeywordAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName,
            Reader reader) {
            //return new TokenStreamComponents(new KeywordTokenizer(reader));

        KeywordTokenizer src = new KeywordTokenizer(reader);
        TokenStream result = new LowerCaseFilter(Version.LUCENE_34, src);
        //KeywordTokenizer src =
        return new TokenStreamComponents(src, result) {

            @Override
            protected void setReader(final Reader reader) throws IOException {
                super.setReader(reader);
            }
        };
    }
}

package au.org.ala.checklist.lucene.analyzer;

import au.org.ala.checklist.lucene.NameIndexField;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
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


//    @Override
//    public TokenStream tokenStream(String fieldName, Reader reader) {
//        //if the field name is VOUCHER then we want to remove fullstops and spaces
////        if(fieldName.equals(NameIndexField.VOUCHER.toString())){
////
////        }
//         //convert to lowercase
//        return new LowerCaseFilter(Version.LUCENE_34, new KeywordTokenizer(reader));
//    }
//
//// private final class CharacterRemovalFilter extends TokenFilter {
////     private CharacterRemovalFilter(TokenStream in) {
////          super(in);
////          termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);
////  }
////
////  private TermAttribute termAtt;
////
//// }

}

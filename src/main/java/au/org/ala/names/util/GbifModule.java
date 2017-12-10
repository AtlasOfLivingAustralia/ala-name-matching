package au.org.ala.names.util;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

import java.io.IOException;

/**
 * Module for Gbif terms in jackson 2.0
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class GbifModule extends SimpleModule {
    private TermFactory factory = TermFactory.instance();

    public GbifModule() {
        super("GbifModule", new Version(1, 0, 0, null, null, null));
        JsonSerializer<Term> ser = new StdSerializer<Term>(Term.class) {
            @Override
            public void serialize(Term value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(value.qualifiedName());
            }
        };
        JsonDeserializer<Term> des = new StdDeserializer<Term>(Term.class) {
            @Override
            public Term deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
                    return factory.findTerm(p.getText());
                }
                throw ctxt.mappingException("Expected JSON String");
            }
        };
        KeyDeserializer kdes = new StdKeyDeserializer(-1, Term.class) {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
                return factory.findTerm(key);
            }
        };
        this.addSerializer(Term.class, ser);
        this.addKeySerializer(Term.class, ser);
        this.addDeserializer(Term.class, des);
        this.addKeyDeserializer(Term.class, kdes);
    }
}

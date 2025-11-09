package luvml.jsoup2luvml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import luvml.element.SemanticContainerElement_I;
import luvx.Node_I;
import org.jsoup.nodes.Element;
import luvml.element.SemanticElement_I;
import luvx.ContainerElement_I;
import luvx.mutable.MutableContainerElement_I;
import org.jsoup.nodes.Document;

/**
 * Generic factory for creating semantic elements from JSoup elements.
 * Uses a registry-based approach for extensible custom element mapping.
 *
 * Usage:
 * - Register custom semantic elements: factory.register(MyElement.class, MyElement::new)
 * - Convert JSoup to LUVML: factory.createSemanticElement(jsoupElement)
 */
public class SemanticElementConverter {
    private SemanticElementConverter(){}
    private final LinkedHashMap<String,SemanticElementDef<?>> defs = new LinkedHashMap<>();

    public <T extends SemanticElement_I<T>> void register(Class<T> clss, Supplier<T> s){
        var ss = new SemanticElementDef(clss,s);
        defs.put(ss.tagName(), ss);
    }
    
    public static <T extends SemanticElement_I<T>> SemanticElementDef<T> def(Supplier<T> constructor){
        return new SemanticElementDef(constructor.get().getClass(), constructor);
    }
    
    public static <T extends SemanticElement_I<T>> SemanticElementDef<T> def(Class<T> classDef, Supplier<T> constructor){
        return new SemanticElementDef(classDef, constructor);
    }
    
    public static SemanticElementConverter semanticElementConverter(
           SemanticElementDef ... defs){
        var x = new SemanticElementConverter();
        for (SemanticElementDef def : defs) {
            x.defs.put(def.tagName(), def);
        }
        return x;
    }
    
    public <T extends SemanticElement_I<T>> String tagNameForClass(Class<T> clss){
        for (var def : defs.values()) {
            if(def.classDef().equals(clss)){
                return def.tagName();
            }
        }
        return null;
    }
    
    public <T extends SemanticElement_I<T>> List<T> filter(Document doc, Class<T> clss){
        var tagName = tagNameForClass(clss);
        if(tagName==null) throw new IllegalArgumentException("Looks like this class "+clss+" is not registered, first register it.");
        return doc.select(tagName).stream()
            .map(this::createSemanticElement)
            .filter(e -> clss.isInstance(e))
            .map(e -> (T) e)
            .toList();
    }

    /**
     * Convert JSoup element to semantic element using registry
     */
    public Node_I<?> createSemanticElement(Element jsoupElement) {
        if (jsoupElement == null) {
            return null;
        }

        var tagName = jsoupElement.tagName();

        // Check registry for registered custom elements
        var def = defs.get(tagName);
        if (def != null) {
            return createFromRegistry(jsoupElement, def);
        }

        // Fallback to standard HTML elements
        return StandardHtmlConverter.convertElement(jsoupElement);
    }

    /**
     * Create semantic element from registry definition
     */
    private <T extends SemanticElement_I<T>> T createFromRegistry(Element jsoupElement, SemanticElementDef<T> def) {
        var element = def.constructor().get();
        
        // whether container or void, attributes can be added in both
        for (var attr : jsoupElement.attributes()) {
            var htmlAttr = new luvml.HtmlAttribute(attr.getKey(), attr.getValue());
            element.addAttributes(htmlAttr);
        }

        if(element instanceof SemanticContainerElement_I celement){            
            convertChildNodes(jsoupElement, celement);
        }

        return element;
    }


    /**
     * Convert JSoup child nodes to LUVML, checking registry first for semantic elements
     */
    private void convertChildNodes(Element jsoupElement, ContainerElement_I luvmlElement) {
        for (var childNode : jsoupElement.childNodes()) {
            Node_I<?> convertedNode;

            // For element children, try semantic conversion first (checks registry)
            if (childNode instanceof Element childElement) {
                convertedNode = createSemanticElement(childElement);
            } else {
                // For text nodes, comments, etc., use standard converter
                convertedNode = StandardHtmlConverter.convertNode(childNode);
            }

            if (convertedNode != null) {
                // whether inline or block, we can add in a mutable container
                if (luvmlElement instanceof MutableContainerElement_I blockElement) {
                    blockElement.addContent(convertedNode);
                }
            }
        }
    }

    /**
     * Process mixed JSoup fragment with semantic and standard HTML elements
     */
    public luvml.Frags convertMixedFragment(String htmlFragment) {
        var jsoupDoc = org.jsoup.Jsoup.parseBodyFragment(htmlFragment);
        var fragments = new java.util.ArrayList<Node_I<?>>();

        for (var element : jsoupDoc.body().children()) {
            var converted = createSemanticElement(element);
            if (converted != null) {
                fragments.add(converted);
            }
        }

        return luvml.Frags.of(fragments.toArray(new Node_I[0]));
    }
}
package luvml.jsoup2luvml;

import java.util.LinkedHashMap;
import java.util.function.Supplier;
import luvml.element.SemanticContainerElement_I;
import luvx.Node_I;
import org.jsoup.nodes.Element;
import luvml.element.SemanticElement_I;
import luvx.ContainerElement_I;
import luvx.mutable.MutableContainerElement_I;

/**
 * Generic factory for creating semantic elements from JSoup elements.
 * Uses a registry-based approach for extensible custom element mapping.
 *
 * Usage:
 * - Register custom semantic elements: factory.register(MyElement.class, MyElement::new)
 * - Convert JSoup to LUVML: factory.createSemanticElement(jsoupElement)
 */
public class SemanticElementFactory {

    private final JSoupToLuvmlConverter baseConverter;
    private final LinkedHashMap<String,SemanticElementDef<?>> defs = new LinkedHashMap<>();

    public SemanticElementFactory() {
        this.baseConverter = new JSoupToLuvmlConverter();
    }

    public SemanticElementFactory(JSoupToLuvmlConverter converter) {
        this.baseConverter = converter;
    }
    
    public <T extends SemanticElement_I<T>> void register(Class<T> clss, Supplier<T> s){
        var ss = new SemanticElementDef(clss,s);
        defs.put(ss.tagName(), ss);
    }

    /**
     * Convert JSoup element to semantic element using registry
     */
    public Node_I<?> createSemanticElement(Element jsoupElement) {
        if (jsoupElement == null) {
            return null;
        }

        var tagName = jsoupElement.tagName().toLowerCase();

        // Check registry for registered custom elements
        var def = defs.get(tagName);
        if (def != null) {
            return createFromRegistry(jsoupElement, def);
        }

        // Fallback to standard HTML elements
        return baseConverter.convertElement(jsoupElement);
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
     * Convert JSoup child nodes to LUVML using base converter
     */
    private void convertChildNodes(Element jsoupElement, ContainerElement_I luvmlElement) {
        for (var childNode : jsoupElement.childNodes()) {
            var convertedNode = baseConverter.convertNode(childNode);
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
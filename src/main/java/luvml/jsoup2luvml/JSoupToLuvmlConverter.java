package luvml.jsoup2luvml;

import luvml.*;
import luvx.Node_I;
import luvx.ContainerElement_I;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import luvml.element.BlockContainerElement;
import luvml.element.InlineContainerElement;
import luvx.Element_I;
import luvx.mutable.MutableContainerElement_I;

/**
 * JSoup-to-LUVML converter using type-safe switch expressions and E.java mappings.
 * Converts JSoup DOM elements directly to LUVML semantic elements without string manipulation.
 *
 * Architecture:
 * - Uses E.getElementType() to determine CONTAINER vs VOID elements
 * - Uses E.getDisplayType() to determine BLOCK vs INLINE elements
 * - JSoup Element → correct LUVML element type (BlockContainer, InlineContainer, BlockVoid, InlineVoid)
 * - JSoup TextNode → LUVML HtmlText
 * - Recursive conversion with type-safe pattern matching
 */
public class JSoupToLuvmlConverter {

    /**
     * Convert JSoup Document to LUVML Document structure
     */
    public BlockContainerElement convertDocument(Document jsoupDoc) {
        // Convert document to a block container (like <body>)
        return (BlockContainerElement) convertElement(jsoupDoc.body());
    }

    /**
     * Convert JSoup Element to appropriate LUVML Element using E.java type mappings
     */
    public Element_I<?> convertElement(Element jsoupElement) {
        if (jsoupElement == null) {
            return null;
        }

        var tagName = jsoupElement.tagName().toLowerCase();
        var ed = HtmlElementData.get(tagName);
        var elementType = ed==null?null:ed.elementType();
        var displayType = ed==null?null:ed.displayType();

        // Handle unknown elements (custom semantic elements) - default to block container
        if (elementType == null) {
            elementType = ElementType.CONTAINER;
        }
        if (displayType == null) {
            displayType = DisplayType.BLOCK;
        }

        // Use type-safe switch expression for element creation
        var luvmlElement = switch (elementType) {
            case CONTAINER, RAW_TEXT, ESCAPABLE_RAW_TEXT ->
                switch (displayType) {
                    case BLOCK -> E.blockContainer(tagName);
                    case INLINE, INLINE_BLOCK -> E.inlineContainer(tagName);
                    default -> E.blockContainer(tagName); // fallback
                };
            case VOID ->
                switch (displayType) {
                    case BLOCK -> E.blockVoidElement(tagName);
                    case INLINE, INLINE_BLOCK -> E.inlineVoidElement(tagName);
                    default -> E.inlineVoidElement(tagName); // fallback for void
                };
            default -> E.blockContainer(tagName); // fallback
        };

        // Convert attributes for all element types
        convertAttributes(jsoupElement, luvmlElement);

        // Convert child nodes only for container elements
        if (luvmlElement instanceof ContainerElement_I<?> containerElement) {
            convertChildNodes(jsoupElement, containerElement);
        }

        return luvmlElement;
    }

    /**
     * Convert JSoup child nodes to LUVML child nodes using type discrimination
     */
    private void convertChildNodes(Element jsoupElement, ContainerElement_I<?> containerElement) {
        for (var childNode : jsoupElement.childNodes()) {
            var convertedNode = convertNode(childNode);
            if (convertedNode != null) {
                // Use type-safe casting to access addContent method
                if (containerElement instanceof BlockContainerElement blockElement) {
                    blockElement.addContent(convertedNode);
                } else if (containerElement instanceof InlineContainerElement inlineElement) {
                    inlineElement.addContent(convertedNode);
                }
            }
        }
    }

    /**
     * Convert individual JSoup Node using type-safe switch expressions
     */
    public Node_I<?> convertNode(Node jsoupNode) {
        return switch (jsoupNode) {
            // no processing instruction or document type 
            case Element element -> convertElement(element);
            case TextNode textNode -> {
                var text = textNode.text().trim();
                yield text.isEmpty() ? null : new HtmlText(text);
            }
            case Comment comment ->
                new BlockComment(comment.getData()); // there is no way to distinguish between inline comment and block, we assume block, besides it is a rendering choice nothing todo semantically
            case DataNode dataNode ->
                new BlockCData(dataNode.getWholeData()); // inline Cdata is not meaningful, block makes more sense, besides there is no way for us to distinguish
            default -> {
                // Handle unknown node types gracefully
                System.err.println("Unknown JSoup node type: " + jsoupNode.getClass().getSimpleName());
                yield null;
            }
        };
    }

    /**
     * Convert JSoup attributes to LUVML attributes
     */
    private void convertAttributes(Element jsoupElement, Element_I<?> luvmlElement) {
        for (var attr : jsoupElement.attributes()) {
            // Create HtmlAttribute and add as content
            var htmlAttr = new HtmlAttribute(attr.getKey(), attr.getValue());
            if (luvmlElement instanceof BlockContainerElement blockElement) {
                blockElement.addContent(htmlAttr);
            } else if (luvmlElement instanceof InlineContainerElement inlineElement) {
                inlineElement.addContent(htmlAttr);
            }
        }
    }

    /**
     * Convert multiple JSoup elements to LUVML Frags collection
     */
    public Frags convertElements(Elements jsoupElements) {
        var fragments = new ArrayList<Node_I<?>>();

        for (var element : jsoupElements) {
            var converted = convertElement(element);
            if (converted != null) {
                fragments.add(converted);
            }
        }

        return Frags.of(fragments.toArray(new Node_I[0]));
    }

    /**
     * Convert JSoup fragment string to LUVML elements
     */
    public Frags convertFragment(String htmlFragment) {
        var jsoupDoc = org.jsoup.Jsoup.parseBodyFragment(htmlFragment);
        var bodyChildren = jsoupDoc.body().children();
        return convertElements(bodyChildren);
    }
}
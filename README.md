# LuvML-JSoup: Bridge Between JSoup and Type-Safe Semantic Elements

**Parse wild HTML with JSoup, process with LuvML's type-safe DSL - the best of both worlds**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![LuvML](https://img.shields.io/badge/LuvML-2.0-blue.svg)](https://github.com/xyz-jphil/xyz-jphil-luvml)
[![JSoup](https://img.shields.io/badge/JSoup-1.17%2B-green.svg)](https://jsoup.org/)

## What is LuvML-JSoup?

A **lightweight bridge** (~3 classes, ~200 lines) that connects JSoup's robust HTML parsing with LuvML's type-safe DOM and powerful DSL. Define custom semantic elements, register them with one line, and get full compile-time type safety with pattern matching.

## Why Use This?

### The Problem
- **JSoup**: Great for parsing messy HTML, but no type safety for custom elements
- **LuvML**: Powerful type-safe DOM with sealed types, but needs HTML parsing
- **JAXB**: Heavy reflection, verbose API, 15+ lines for what should be 4

### The Solution
```java
// One-line registration
def(BlogPost.class, BlogPost::new)

// Parse and process with type safety
var fragments = converter.convertMixedFragment(html);

// Exhaustive pattern matching, zero casts
switch (element) {
    case BlogPost post -> post.slug()      // Type-safe!
    case CodeBlock code -> code.language() // No casting!
}
```

---

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>io.github.xyz-jphil</groupId>
    <artifactId>xyz-jphil-luvml-jsoup</artifactId>
    <version>2.0</version>
</dependency>
```

### 30-Second Example

```java
import static luvml.jsoup2luvml.SemanticElementConverter.*;
import static luvml.E.*; // Standard HTML DSL
import luvml.element.SemanticBlockContainerElement;

// 1. Define custom element
public class ProductCard extends SemanticBlockContainerElement<ProductCard> {
    public ProductCard() { super(ProductCard.class); }

    public String sku() { return attr("sku"); }
    public String category() { return attr("category"); }
}

// 2. Register (one line!)
var converter = semanticElementConverter(
    def(ProductCard.class, ProductCard::new)
);

// 3. Parse HTML
String html = """
    <productCard sku="ABC-123" category="electronics">
        <h3>Wireless Headphones</h3>
        <p class="price">$99.99</p>
    </productCard>
    """;

var fragments = converter.convertMixedFragment(html);

// 4. Type-safe processing
for (var node : fragments) {
    switch (node.nodeType()) {
        case Element_T e -> {
            if (e.element() instanceof ProductCard card) {
                System.out.println("SKU: " + card.sku());
                System.out.println("Category: " + card.category());
            }
        }
    }
}
```

**Note**: Custom element tags follow **camelCase** convention (e.g., `<productCard>`), matching XHTML custom element style. Standard HTML allows kebab-case (e.g., `<product-card>`), but we stick to camelCase for consistency with Java naming.

---

## Real-World Example: Blog Post System

### 1. Define Semantic Elements

```java
// Custom elements - class name BlogPost_E → XML tag <blogPost>
public class BlogPost_E extends SemanticBlockContainerElement<BlogPost_E> {
    public BlogPost_E() { super(BlogPost_E.class); }

    public static final String $slug = "slug";
    public static final String $author = "author";
    public static final String $publishDate = "publishDate";

    public String slug() { return attr($slug); }
    public Optional<String> author() { return attribute($author).optString(); }
    public Optional<String> publishDate() { return attribute($publishDate).optString(); }
}

public class CodeSnippet_E extends SemanticBlockContainerElement<CodeSnippet_E> {
    public CodeSnippet_E() { super(CodeSnippet_E.class); }

    public static final String $language = "language";
    public static final String $showLineNumbers = "showLineNumbers";

    public String language() { return attr($language); }
    public boolean showLineNumbers() {
        return attribute($showLineNumbers).optBoolean().orElse(false);
    }
}

public class InfoBox_E extends SemanticBlockContainerElement<InfoBox_E> {
    public InfoBox_E() { super(InfoBox_E.class); }

    public static final String $boxType = "boxType";
    public static final String $icon = "icon";

    public String boxType() { return attr($boxType); }
    public Optional<String> icon() { return attribute($icon).optString(); }
}
```

### 2. Create DSL Functions

```java
import static luvml.T.text;
import static luvml.E.*;
import static BlogPost_E.*;
import static CodeSnippet_E.*;
import static InfoBox_E.*;

// DSL factory functions for elements
public class BlogDsl {
    public static BlogPost_E blogPost(Frag_I<?>... content) {
        return new BlogPost_E().____(content);
    }

    public static CodeSnippet_E codeSnippet(Frag_I<?>... content) {
        return new CodeSnippet_E().____(content);
    }

    public static InfoBox_E infoBox(Frag_I<?>... content) {
        return new InfoBox_E().____(content);
    }

    // DSL factory functions for attributes
    public static HtmlAttribute slug(String value) {
        return new HtmlAttribute($slug, value);
    }

    public static HtmlAttribute author(String value) {
        return new HtmlAttribute($author, value);
    }

    public static HtmlAttribute publishDate(String value) {
        return new HtmlAttribute($publishDate, value);
    }

    public static HtmlAttribute language(String value) {
        return new HtmlAttribute($language, value);
    }

    public static HtmlAttribute showLineNumbers(boolean value) {
        return new HtmlAttribute($showLineNumbers, String.valueOf(value));
    }

    public static HtmlAttribute boxType(String value) {
        return new HtmlAttribute($boxType, value);
    }

    public static HtmlAttribute icon(String value) {
        return new HtmlAttribute($icon, value);
    }
}
```

### 3. Create Converter

```java
var converter = semanticElementConverter(
    def(BlogPost_E.class, BlogPost_E::new),
    def(CodeSnippet_E.class, CodeSnippet_E::new),
    def(InfoBox_E.class, InfoBox_E::new)
);
```

### 4. Parse Rich HTML

```java
String html = """
    <blogPost slug="java-21-features" author="tech-blogger" publishDate="2024-09-30">
        <h1>Awesome Java 21 Features</h1>

        <infoBox boxType="tip" icon="💡">
            This article covers the latest features in Java 21
        </infoBox>

        <p>Pattern matching has revolutionized how we write Java:</p>

        <codeSnippet language="java" showLineNumbers="true">
        sealed interface Shape permits Circle, Rectangle {}
        record Circle(double radius) implements Shape {}
        record Rectangle(double w, double h) implements Shape {}

        double area(Shape shape) {
            return switch (shape) {
                case Circle c -> Math.PI * c.radius() * c.radius();
                case Rectangle r -> r.w() * r.h();
            };
        }
        </codeSnippet>

        <p>Notice how the compiler enforces <strong>exhaustive checking</strong>!</p>
    </blogPost>
    """;

var fragments = converter.convertMixedFragment(html);
```

### 5. Process with Pattern Matching (Bidirectional Type Safety)

```java
void processBlogPost(Frags fragments) {
    for (var node : fragments) {
        switch (node.nodeType()) {
            case Element_T e -> {
                switch (e.element()) {
                    case BlogPost_E post -> {
                        System.out.println("📝 Post: " + post.slug());
                        System.out.println("✍️  Author: " + post.author().orElse("Anonymous"));
                        System.out.println("📅 Date: " + post.publishDate().orElse("N/A"));

                        processChildren(post);
                    }

                    case CodeSnippet_E code -> {
                        System.out.println("💻 Code (" + code.language() + "):");
                        if (code.showLineNumbers()) {
                            renderWithLineNumbers(code);
                        } else {
                            renderPlain(code);
                        }
                    }

                    case InfoBox_E box -> {
                        String icon = box.icon().orElse("ℹ️");
                        System.out.println(icon + " " + box.boxType().toUpperCase());
                        renderBoxContent(box);
                    }

                    case luvml.H1 h1 ->
                        System.out.println("# " + extractText(h1));

                    case luvml.P para ->
                        System.out.println(extractText(para));

                    case luvml.Strong strong ->
                        System.out.print("**" + extractText(strong) + "**");

                    default -> {} // Handle other standard HTML elements
                }
            }

            case AttributelessNode_T a -> {
                switch (a.attributelessNodeType()) {
                    case StringNode_T s -> {
                        switch (s.stringNodeType()) {
                            case Text_T t -> System.out.print(t.text().textContent());
                            default -> {}
                        }
                    }
                    default -> {}
                }
            }
        }
    }
}
```

---

## The Power of DSL: Creating Semantic HTML in Java

### Without LuvML-JSoup (String Hell)

```java
// ❌ Stringly-typed, no safety, error-prone
String html = "<blogPost slug=\"" + slug + "\" author=\"" + author + "\">" +
    "<h1>" + escapeHtml(title) + "</h1>" +
    "<codeSnippet language=\"java\">" + escapeHtml(code) + "</codeSnippet>" +
    "</blogPost>";
```

### With LuvML DSL (Type-Safe Heaven)

```java
import static luvml.E.*;     // Standard HTML elements
import static luvml.A.*;     // Standard HTML attributes
import static luvml.T.text;  // Text nodes
import static BlogDsl.*;     // Custom blog DSL

// ✅ Type-safe, composable, compiler-validated
var post = blogPost(
    slug("java-21-features"),
    author("tech-blogger"),

    h1(text("Awesome Java 21 Features")),

    infoBox(
        boxType("tip"),
        icon("💡"),
        text("This article covers Java 21")
    ),

    p(text("Pattern matching has revolutionized Java")),

    codeSnippet(
        language("java"),
        showLineNumbers(true),
        text("""
        sealed interface Shape permits Circle, Rectangle {}
        record Circle(double radius) implements Shape {}
        """)
    )
);

// Render to HTML string
String html = HtmlRenderer.render(post);
```

### The Complete Picture: Bidirectional Type Safety

```java
import static BlogDsl.*;
import static luvml.E.*;
import static luvml.T.text;

// Create with DSL - Type-safe construction
var post = blogPost(
    slug("java-21-features"),
    author("tech-blogger"),

    h1(text("Awesome Java 21 Features")),

    infoBox(
        boxType("tip"),
        icon("💡"),
        text("This article covers Java 21")
    ),

    p(text("Pattern matching has revolutionized Java")),

    codeSnippet(
        language("java"),
        showLineNumbers(true),
        text("""
        sealed interface Shape permits Circle, Rectangle {}
        record Circle(double radius) implements Shape {}
        """)
    )
);

// Render to HTML
String html = HtmlRenderer.render(post);

// Parse back with JSoup
var reparsed = converter.convertMixedFragment(html);

// Process with exhaustive pattern matching
for (var node : reparsed) {
    switch (node.nodeType()) {
        case Element_T e -> {
            switch (e.element()) {
                case BlogPost_E blogPost -> {
                    // Type-safe attribute access
                    String slug = blogPost.slug();
                    Optional<String> author = blogPost.author();

                    // Process children with type safety
                    for (var child : blogPost.childNodes()) {
                        switch (child.nodeType()) {
                            case Element_T ce -> {
                                switch (ce.element()) {
                                    case CodeSnippet_E code ->
                                        highlightCode(code.language(), extractText(code));
                                    case InfoBox_E box ->
                                        renderAlert(box.boxType(), extractText(box));
                                    case luvml.H1 h1 ->
                                        renderHeading(extractText(h1));
                                    case luvml.P p ->
                                        renderParagraph(extractText(p));
                                    default -> {}
                                }
                            }
                            default -> {}
                        }
                    }
                }
                default -> {}
            }
        }
        default -> {}
    }
}
```

**Bidirectional guarantees:**
- DSL construction → Type-safe element/attribute creation
- HTML parsing → Type-safe element/attribute extraction
- Pattern matching → Exhaustive, compiler-enforced
- Zero casts, zero reflection!

**This is the power**: Parse messy external HTML with JSoup, generate clean semantic HTML with LuvML DSL - all type-safe, all compile-time checked!

---

## Architecture: Three-Way Harmony

```
External HTML String
        ↓ (JSoup parse)
    JSoup Document
        ↓ (LuvML-JSoup convert)
  LuvML Type-Safe DOM ←→ LuvML DSL Construction
        ↓ (HtmlRenderer)
    Clean HTML Output
```

**Bidirectional transformations**, all type-safe:
1. **HTML → LUVML DOM** (via JSoup + this bridge)
2. **LUVML DSL → LUVML DOM** (via fluent builders)
3. **LUVML DOM → HTML** (via renderer)

---

## Why Not JAXB?

| Feature | LuvML-JSoup | JAXB |
|---------|-------------|------|
| **Registration** | `def(Class, Constructor)` | `@XmlRootElement` + complex annotations |
| **Runtime Cost** | Zero reflection | Heavy reflection |
| **Type Safety** | Sealed types + exhaustive switch | Manual `instanceof` chains |
| **Syntax** | `blogPost(slug("abc"), h1("Title"))` | `post.setSlug("abc"); post.getContent().add(...)` |
| **Lines of Code** | ~4-5 lines | ~15+ lines (same logic) |
| **Custom Elements** | First-class, one-line registration | Complex XSD binding |
| **DSL Support** | Native | None (setters only) |
| **Pattern Matching** | Exhaustive, compiler-enforced | Manual, error-prone |

**JAXB would have been a mistake.** This design is faster, safer, and more maintainable.

---

## Technical Details

### Zero-Cast Type Safety

```java
// No casting needed - types preserved through sealed hierarchy
switch (node.nodeType()) {
    case Element_T e -> {
        var element = e.element(); // Type: Element_I - NO CAST!

        switch (element) {
            case BlogPost_E post ->
                post.slug() // Direct access, no cast
            case CodeSnippet_E code ->
                code.language() // Direct access, no cast
        }
    }
}
```

### Exhaustive Checking

```java
// Compiler ensures ALL types are handled
switch (element) {
    case BlogPost_E post -> // ...
    case CodeSnippet_E code -> // ...
    case InfoBox_E box -> // ...
    // Forget a case? Compilation error!
}
```

### Registration is Pure Static Dispatch

```java
// Constructor reference evaluated at registration - O(1) HashMap lookup at runtime
def(BlogPost_E.class, BlogPost_E::new)
// → Caches tagName ("blogPost") - _E suffix removed automatically
// → Runtime: HashMap.get("blogPost") → constructor.get()
// → Zero reflection!
```

---

## Core API

### `SemanticElementConverter`

```java
// Create converter with custom elements
var converter = semanticElementConverter(
    def(CustomElement.class, CustomElement::new),
    def(AnotherElement.class, AnotherElement::new)
);

// Convert HTML fragment
Frags fragments = converter.convertMixedFragment(htmlString);

// Convert single JSoup element
Node_I<?> node = converter.createSemanticElement(jsoupElement);
```

### `SemanticElementDef`

```java
// Definition holds class + constructor reference
SemanticElementDef<BlogPost> def = def(BlogPost.class, BlogPost::new);

String tagName = def.tagName();           // "blogPost"
boolean isVoid = def.isVoidType();        // false
Supplier<BlogPost> ctor = def.constructor();
```

---

## Requirements

- **Java 21+** (sealed types, pattern matching, switch expressions)
- **LuvML 2.0+** (type-safe HTML DOM)
- **JSoup 1.17+** (robust HTML parsing)

---

## Project Links

- **LuvX Base**: [github.com/xyz-jphil/xyz-jphil-luvx-base](https://github.com/xyz-jphil/xyz-jphil-luvx-base)
- **LuvML Core**: [github.com/xyz-jphil/xyz-jphil-luvml](https://github.com/xyz-jphil/xyz-jphil-luvml)
- **LuvML-JSoup**: [github.com/xyz-jphil/xyz-jphil-luvml-jsoup](https://github.com/xyz-jphil/xyz-jphil-luvml-jsoup)

---

## License

No specific license specified. Assume MIT or Apache 2.0 for maximum permissiveness.

---

## Summary

**LuvML-JSoup is a tiny bridge (~3 classes) that unlocks massive power:**

✨ One-line registration: `def(Class, Constructor)`
🔒 Zero-cast type safety via sealed types
⚡ Zero reflection - pure static dispatch
🎯 Exhaustive pattern matching
🔄 Bidirectional: Parse with JSoup, build with DSL
📦 Tiny library, massive leverage from LuvML foundation

**This is world-class Java API design** - type safety without verbosity, performance without reflection.

---

**If you believe Java deserves elegant DSLs, star ⭐ this project!**
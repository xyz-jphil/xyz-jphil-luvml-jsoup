package luvml.jsoup2luvml;

import java.util.function.Supplier;
import luvml.element.SemanticElement_I;
import luvml.element.SemanticVoidElement_I;

public final class SemanticElementDef<T extends SemanticElement_I<T>>  {
    private final Class<T> classDef;
    private final Supplier<T> constructor;

    private final String tagName;
    private final boolean voidType;
    
    public SemanticElementDef(Class<T> classDef, Supplier<T> constructor) {
        this.classDef = classDef;
        this.constructor = constructor;
        var s = constructor.get();
        tagName = s.tagName();
        voidType = s instanceof SemanticVoidElement_I ;
    }
    
    public static <T extends SemanticElement_I<T>> SemanticElementDef<T> semanticElementDef(Class<T> classDef, Supplier<T> constructor){
        return new SemanticElementDef(classDef, constructor);
    }

    public Class<T> classDef() {
        return classDef;
    }

    public Supplier<T> constructor() {
        return constructor;
    }
    
    public String tagName(){
        return tagName;
    }
    
    public boolean isVoidType(){
        return voidType;
    }

}

package com.aitusoftware.transport.messaging.proxy;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("com.aitusoftware.transport.messaging.Topic")
public final class AnnotationPublisherGenerator extends AbstractProcessor
{
    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv)
    {
        System.out.println(annotations);
//        final Filer filer = this.processingEnv.getFiler();
//        ((Filer) filer).createSourceFile("")
//        // returning true implies claimed
        return false;
    }
}

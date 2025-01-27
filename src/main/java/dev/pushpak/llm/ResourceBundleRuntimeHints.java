package dev.pushpak.llm;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.stereotype.Component;
import gg.jte.generated.precompiled.JteindexGenerated;
import gg.jte.generated.precompiled.JteresultGenerated;
import gg.jte.generated.precompiled.layout.JtepageGenerated;
import gg.jte.*;

@Component
public class ResourceBundleRuntimeHints implements RuntimeHintsRegistrar{

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("**/*.bin");

        hints.reflection()
                .registerType(JtepageGenerated.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(JteindexGenerated.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(JteresultGenerated.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS);
    }

}
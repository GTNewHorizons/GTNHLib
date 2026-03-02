// Copyright (c) 2025 Nikolay Sitnikov https://github.com/Nikolay-Sitnikov
// Licensed under the LGPL-3 License
// Copied from Salis Arcana
// https://github.com/rndmorris/Salis-Arcana/

package com.gtnewhorizon.gtnhlib.api.thaumcraft;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import thaumcraft.api.research.ResearchPage;

public class FormattedResearchPage extends ResearchPage {

    public final Object[] formattingData;

    public FormattedResearchPage(String text, Object[] formattingData) {
        super(text);
        this.formattingData = formattingData;
    }

    public FormattedResearchPage(String research, String text, Object[] formattingData) {
        super(research, text);
        this.formattingData = formattingData;
    }

    public FormattedResearchPage(ResourceLocation image, String caption, Object[] formattingData) {
        super(image, caption);
        this.formattingData = formattingData;
    }

    @Override
    public String getTranslatedText() {
        return StatCollector.translateToLocalFormatted(this.text, formattingData);
    }
}

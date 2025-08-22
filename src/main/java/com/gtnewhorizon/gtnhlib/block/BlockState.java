package com.gtnewhorizon.gtnhlib.block;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.block.Block;

@Desugar
public record BlockState(Block block, int meta) {}

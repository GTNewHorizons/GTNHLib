package com.gtnewhorizon.gtnhlib.block;

import net.minecraft.block.Block;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record BlockState(Block block, int meta) {}

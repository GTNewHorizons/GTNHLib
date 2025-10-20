package com.gtnewhorizon.gtnhlib.client.model.state;

import net.minecraft.block.Block;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record BlockState(Block block, int meta) {}

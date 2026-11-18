package com.gtnewhorizon.gtnhlib.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.gtnewhorizon.gtnhlib.chat.autocomplete.AutocompleteSuggestions;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.ChatAutocompleteRegistry;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class AutocompleteSuggestionsPacket implements IPacket {

    private int sequence;
    private AutocompleteSuggestions suggestions;

    @Internal
    public AutocompleteSuggestionsPacket() {
        // reflected
    }

    public AutocompleteSuggestionsPacket(int sequence, AutocompleteSuggestions suggestions) {
        this.sequence = sequence;
        this.suggestions = suggestions;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(sequence);

        if (suggestions == null) {
            buf.writeByte(0);
            return;
        } else {
            buf.writeByte(1);
        }

        buf.writeVarIntToBuffer(suggestions.snipStart());
        buf.writeVarIntToBuffer(suggestions.snipEnd());
        buf.writeVarIntToBuffer(suggestions.replacements().size());

        for (var repl : suggestions.replacements()) {
            buf.writeStringToBuffer(repl);
        }

        if (suggestions.continuation() != null) {
            buf.writeByte(1);
            buf.writeStringToBuffer(suggestions.continuation());
        } else {
            buf.writeByte(0);
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        this.sequence = buf.readVarIntFromBuffer();

        if (buf.readByte() == 0) {
            return;
        }

        int snipStart = buf.readVarIntFromBuffer();
        int snipEnd = buf.readVarIntFromBuffer();
        int replCount = buf.readVarIntFromBuffer();

        List<String> replacements = new ArrayList<>();

        for (int i = 0; i < replCount; i++) {
            replacements.add(buf.readStringFromBuffer(1024));
        }

        String continuation = buf.readByte() == 1 ? buf.readStringFromBuffer(1024) : null;

        suggestions = new AutocompleteSuggestions(snipStart, snipEnd, replacements, continuation);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IPacket executeClient(NetHandlerPlayClient handler) {
        ChatAutocompleteRegistry.handleSyncedSuggestions(sequence, this.suggestions);

        return null;
    }
}

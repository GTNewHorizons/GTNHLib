package com.gtnewhorizon.gtnhlib.integration.mui2;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.network.PacketBuffer;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.teams.TeamRole;

import cpw.mods.fml.common.network.ByteBufUtils;

@Desugar
/**
 * spotless:off
 * Represents a display item on the ListDisplay flag: Representation changes depending on the ScreenType this
 * displayItem was sent, as follows:
 * VIEW_CONSUMPTION_REQUESTS: ignored.
 * PLAYER_LIST: Set to true if player cannot be demoted or kicked (last owner of team).
 * TEAM_LIST: Set to true if team can be disbanded (size > 1).
 * TEAMS_INVITING_PLAYERS: Set to true if the player is unable to accept the invite (eg. last
 * owner of their team with more than one member.
 * INVITE_PLAYERS, REQUEST_MERGE: Set to true if an invite or merge requests to this player/team already exists.
 *
 * uuid: represents team or player id depending on what screen this displayitem was sent.
 * spotless:on
 */
public record DisplayItem(@Nonnull DisplayItemType type, String text, @Nullable TeamRole role, @Nonnull UUID uuid,
        boolean flag) {

    /**
     * Represents team or player id depending on what screen this displayitem was sent.
     */
    @Override
    public UUID uuid() {
        return uuid;
    }

    /**
     * Represents a display item on the ListDisplay flag: Representation changes depending on the ScreenType this
     * displayItem was sent, as follows:
     * <ul>
     * <li><b>VIEW_CONSUMPTION_REQUESTS</b>: ignored.</li>
     * <li><b>PLAYER_LIST</b>: Set to true if player cannot be demoted or kicked (last owner of team).</li>
     * <li><b>TEAM_LIST</b>: Set to true if team can be disbanded (size > 1).</li>
     * <li><b>TEAMS_INVITING_PLAYERS</b>: Set to true if the player is unable to accept the invite (eg. last owner of
     * their team with more than one member.</li>
     * <li><b>INVITE_PLAYERS, REQUEST_MERGE</b>: Set to true if an invite or merge requests to this player/team already
     * exists.</li>
     * </ul>
     */
    @Override
    public boolean flag() {
        return flag;
    }

    public static DisplayItem copyOf(DisplayItem other) {
        return new DisplayItem(other.type, other.text, other.role, other.uuid, other.flag);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DisplayItem that)) return false;
        return flag == that.flag && Objects.equals(uuid, that.uuid)
                && Objects.equals(text, that.text)
                && role == that.role
                && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, text, role, uuid, flag);
    }

    public void serializeInto(PacketBuffer buffer) {
        buffer.writeShort(type.ordinal());
        ByteBufUtils.writeUTF8String(buffer, text);
        buffer.writeBoolean(role == null);
        if (role != null) buffer.writeShort(role.ordinal());
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
        buffer.writeBoolean(flag);
    }

    public static DisplayItem deserializeFrom(PacketBuffer buffer) {
        return new DisplayItem(
                DisplayItemType.values()[buffer.readShort()],
                ByteBufUtils.readUTF8String(buffer),
                buffer.readBoolean() ? null : TeamRole.values()[buffer.readShort()],
                new UUID(buffer.readLong(), buffer.readLong()),
                buffer.readBoolean());
    }
}

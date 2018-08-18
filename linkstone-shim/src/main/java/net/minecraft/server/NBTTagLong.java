package net.minecraft.server;

import net.glowstone.linkstone.annotations.LClassfile;
import net.glowstone.linkstone.annotations.LConstructor;
import net.glowstone.linkstone.annotations.LField;
import net.glowstone.linkstone.annotations.LGenerate;
import net.glowstone.linkstone.annotations.LMethod;
import net.glowstone.linkstone.annotations.LOverride;
import net.glowstone.util.nbt.LongTag;

import java.util.Objects;

import static net.glowstone.linkstone.annotations.Version.V1_12_R1;

@LClassfile(version = V1_12_R1)
public class NBTTagLong extends NBTNumber {
    @LGenerate
    @LField(version = V1_12_R1)
    private long data;

    @LConstructor(version = V1_12_R1)
    public NBTTagLong(long data) {
        this.data = data;
    }

    @Override
    @LOverride
    public byte getTypeId() {
        return ID_LONG;
    }

    @Override
    @LOverride
    public NBTBase clone() {
        return _clone();
    }

    @LMethod(version = V1_12_R1, name = "c")
    public NBTTagLong _clone() {
        return new NBTTagLong(data);
    }

    @Override
    @LOverride
    public byte asByte() {
        return (byte) data;
    }

    @Override
    @LOverride
    public short asShort() {
        return (short) data;
    }

    @Override
    @LOverride
    public int asInt() {
        return (int) data;
    }

    @Override
    @LOverride
    public long asLong() {
        return data;
    }

    @Override
    @LOverride
    public float asFloat() {
        return data;
    }

    @Override
    @LOverride
    public double asDouble() {
        return data;
    }

    @Override
    public LongTag toGlowstone() {
        return new LongTag(data);
    }

    @Override
    public boolean equals(Object o) {
        // implementation might differ from nms implementation
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NBTTagLong that = (NBTTagLong) o;
        return data == that.data;
    }

    @Override
    public int hashCode() {
        // implementation might differ from nms implementation
        return Objects.hash(data);
    }
}

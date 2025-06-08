package com.aetherteam.ozone.claims;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public interface IFilter<Self extends Enum<Self> & IFilter<Self>> extends StringRepresentable, java.lang.constant.Constable, Comparable<Self>, java.io.Serializable {
    default String getName() {
        return getSerializedName();
    }

    int ordinal();

    Component getDescription();

    @SuppressWarnings("unchecked")
    default Self atLeast(Self min) {
        return this.compareTo(min) < 0? min : (Self)this;
    }
}

package com.aetherteam.ozone.claims;

import net.minecraft.util.StringRepresentable;

public sealed interface IFilterField<Self extends Enum<Self> & IFilterField<Self, Filter>, Filter extends Enum<Filter> & IFilter<Filter>>
        extends StringRepresentable, java.lang.constant.Constable, java.io.Serializable
        permits EntityFilterField, PlayerFilterField {
    default String getName() {
        return getSerializedName();
    }
    
    int ordinal();

    Filter getMinValue();

    Filter getDefaultValue();

    Filter get(IFilters claim);

    void set(IFilters claim, Filter value);

    Class<Filter> getFilterClass();
}

package com.onectrm.akshare.api.generator;

public class AKShareAPIProperty {
    private Class type;
    private String name;
    private AKShareRawAPIParameter rawAPIParameter;

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AKShareRawAPIParameter getRawAPIParameter() {
        return rawAPIParameter;
    }

    public void setRawAPIParameter(AKShareRawAPIParameter rawAPIParameter) {
        this.rawAPIParameter = rawAPIParameter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AKShareAPIProperty property = (AKShareAPIProperty) o;

        return name.equals(property.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

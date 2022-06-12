package com.onectrm.akshare.api.generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AKShareAPIClass {
    private final List<String> importedPackages = new ArrayList<>();
    private final Set<AKShareAPIProperty> properties = new HashSet<>();
    private AKShareRawAPI rawAPI;
    private String className;
    private String packageName;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getImportedPackages() {
        return importedPackages;
    }

    public Set<AKShareAPIProperty> getProperties() {
        return properties;
    }

    public AKShareRawAPI getRawAPI() {
        return rawAPI;
    }

    public void setRawAPI(AKShareRawAPI rawAPI) {
        this.rawAPI = rawAPI;
    }
}

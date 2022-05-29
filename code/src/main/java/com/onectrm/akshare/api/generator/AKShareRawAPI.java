package com.onectrm.akshare.api.generator;

import java.util.List;

public class AKShareRawAPI {
    private String category;
    private String interfaceName;
    private String source;
    private String description;
    private String returnDescription;
    private List<AKShareRawAPIParameter> input;
    private List<AKShareRawAPIParameter> output;
    private boolean multipleOutputs;

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<AKShareRawAPIParameter> getInput() {
        return input;
    }

    public void setInput(List<AKShareRawAPIParameter> input) {
        this.input = input;
    }

    public List<AKShareRawAPIParameter> getOutput() {
        return output;
    }

    public void setOutput(List<AKShareRawAPIParameter> output) {
        this.output = output;
    }

    public boolean isMultipleOutputs() {
        return multipleOutputs;
    }

    public void setMultipleOutputs(boolean multipleOutputs) {
        this.multipleOutputs = multipleOutputs;
    }

    public String getReturnDescription() {
        return returnDescription;
    }

    public void setReturnDescription(String returnDescription) {
        this.returnDescription = returnDescription;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

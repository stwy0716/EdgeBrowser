package com.edge.browser.extensions;

public class ExtensionInfo {
    public String id;
    public String name;
    public String description;
    public String version;
    public String jsContent;
    public String cssContent;
    public boolean enabled;
    public long installedAt;

    public ExtensionInfo() {}

    public ExtensionInfo(String id, String name, String description, String version,
                         String jsContent, String cssContent, boolean enabled, long installedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.jsContent = jsContent;
        this.cssContent = cssContent;
        this.enabled = enabled;
        this.installedAt = installedAt;
    }
}
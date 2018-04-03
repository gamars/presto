package com.facebook.presto.plugin.bigquery;

import io.airlift.configuration.Config;

public class BigQueryConfig {

    private String projectId;


    public String getProjectId() {
        return projectId;
    }

    @Config("projectId")
    public BigQueryConfig setProjectId(String projectId) {
        this.projectId = projectId;
        return this;
    }
}

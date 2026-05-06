package com.sessionmanagementservice.config;



import org.hibernate.dialect.H2Dialect;

public class CustomH2Dialect extends H2Dialect {

    public CustomH2Dialect() {
        super();
    }

    @Override
    public String getTableTypeString() {
        // Return empty string to remove ENGINE=InnoDB
        return "";
    }
}
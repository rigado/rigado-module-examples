package com.rigado.androidbtle;

/**
 * Used to read the JSON records from res/raw
 * The member variable names must match the key names in the JSON
 */
public class JsonFirmwareType {

    // These two fields are always expected to exist in the JSON
    private String fwname;
    private Properties properties;

    public String getFwname() {
        return fwname;
    }
    public void setFwname(String fwname) {
        this.fwname = fwname;
    }
    public Properties getProperties() {
        return properties;
    }
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    // sub-structure in JSON, any of these fields may be omitted and an empty string will be returned by getter instead of null
    public class Properties {
        private String version;
        private String build;
        private String filename1;
        private String filename2;//an extra file that might be desirable to use
        private String comment;

        public String getVersion() {
            if (version == null) return "";
            return version;
        }
        public void setVersion(String version) {
            this.version = version;
        }
        public String getBuild() {
            if (build == null) return "";
            return build;
        }
        public void setBuild(String build) {
            this.build = build;
        }
        public String getFilename1() {
            if (filename1 == null) return "";
            return filename1;
        }
        public void setFilename1(String filename1) {
            this.filename1 = filename1;
        }
        public String getFilename2() {
            if (filename2 == null) return "";
            return filename2;
        }
        public void setFilename2(String filename2) {
            this.filename2 = filename2;
        }
        public String getComment() {
            if (comment == null) return "";
            return comment;
        }
        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}

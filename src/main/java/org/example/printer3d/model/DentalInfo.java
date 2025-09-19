package org.example.printer3d.model;

public class DentalInfo {
    private String name;
    private String website;
    private String email;

    public DentalInfo(String name, String website, String email) {
        this.name = name;
        this.website = website;
        this.email = email;
    }

    // Getters
    public String getName() { return name; }
    public String getWebsite() { return website; }
    public String getEmail() { return email; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setWebsite(String website) { this.website = website; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return String.format("DentalInfo{name='%s', website='%s', email='%s'}", name, website, email);
    }
}

package org.example.printer3d.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UrlWithDepth {
    String url;
    int depth;

    public UrlWithDepth(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }
}

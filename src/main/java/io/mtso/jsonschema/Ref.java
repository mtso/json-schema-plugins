package io.mtso.jsonschema;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class Ref {
  private final String path;
  private final String fragment;
  private final boolean isLocalRef;

  public static String escape(String str) {
    if (null == str) {
      return null;
    }
    return str
        .replaceAll("\\{",  "%7B")
        .replaceAll("}",  "%7D");
  }

  public Ref(String refString) throws IOException {
    final URI uri;
    try {
      uri = new URI(escape(refString));
    } catch (final URISyntaxException e) {
      throw new IOException(e);
    }

    if (Objects.isNull(uri.getRawFragment())) {
      path = refString.replaceFirst("file:", "");
      fragment = null;
    } else {
      path = refString.replaceFirst("file:", "").replace("#" + uri.getRawFragment(), "");
      fragment = "#" + uri.getRawFragment();
    }
    isLocalRef = path.isEmpty();
  }

  public String getPath() {
    return path;
  }

  public String getFragment() {
    return fragment;
  }

  public boolean isLocalRef() {
    return isLocalRef;
  }
}

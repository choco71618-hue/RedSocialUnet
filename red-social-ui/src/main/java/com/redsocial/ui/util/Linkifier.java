package com.redsocial.ui.util;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte texto plano en un párrafo con las URLs detectadas como enlaces
 * clickeables (seguro: sin innerHTML, sin inyección).
 */
public final class Linkifier {

    private static final Pattern URL_PATTERN =
            Pattern.compile("(https?://[^\\s]+)", Pattern.CASE_INSENSITIVE);

    private static final String TRAILING_PUNCTUATION = ".,;:!?)]}»\"'";

    private Linkifier() {
    }

    public static Paragraph linkify(String text) {
        Paragraph paragraph = new Paragraph();
        paragraph.addClassName("post-content");
        if (text == null || text.isEmpty()) {
            return paragraph;
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                paragraph.add(text.substring(last, matcher.start()));
            }
            String url = matcher.group();
            String trimmed = stripTrailing(url);
            int removed = url.length() - trimmed.length();

            Anchor anchor = new Anchor(trimmed, trimmed);
            anchor.setTarget("_blank");
            anchor.addClassName("post-link");
            paragraph.add(anchor);

            int end = matcher.end() - removed;
            if (removed > 0) {
                paragraph.add(text.substring(end, matcher.end()));
            }
            last = matcher.end();
        }
        if (last < text.length()) {
            paragraph.add(text.substring(last));
        }
        return paragraph;
    }

    private static String stripTrailing(String url) {
        int end = url.length();
        while (end > 0 && TRAILING_PUNCTUATION.indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }
}

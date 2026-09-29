package com.starterkit.ticket.ticket.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MentionService {

    // Matches @username (alphanumeric + underscore + dash, 3-50 chars)
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([a-zA-Z0-9_.-]{3,50})");

    /** Extract usernames from text */
    public Set<String> extractUsernames(String text) {
        Set<String> result = new LinkedHashSet<>();
        if (text == null) return result;

        Matcher m = MENTION_PATTERN.matcher(text);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }
}

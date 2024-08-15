package com.strangequark.emailservice.utility;

public class StringUtility {
    public static String removeQuotes(String input) {
        return (input.replaceAll("\"", "")
                .replaceAll("%22=", "")
                .replaceAll("%22", ""));
    }
}

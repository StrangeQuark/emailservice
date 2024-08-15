package com.strangequark.emailservice.utility;

import com.strangequark.emailservice.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class LoggerUtility {
    public final static Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    public static void logStackTrace(Exception ex) {
        Writer buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        ex.printStackTrace(pw);
        LOGGER.error(buffer.toString());
    }
}

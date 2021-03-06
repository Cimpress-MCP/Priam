/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.priam.utils;


import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;

import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Utility functions for date.
 * Created by aagrawal on 7/10/17.
 */
@Singleton
public class DateUtil {

    private final static String yyyyMMdd = "yyyyMMdd";
    private final static String yyyyMMddHHmm = "yyyyMMddHHmm";
    private final static String[] patterns = {yyyyMMddHHmm, yyyyMMdd};
    private final static ZoneId defaultZoneId = ZoneId.systemDefault();
    /**
     * Format the given date in format yyyyMMdd
     *
     * @param date to format
     * @return date formatted in yyyyMMdd
     */
    public static String formatyyyyMMdd(Date date) {
        if (date == null)
            return null;
        return DateUtils.formatDate(date, yyyyMMdd);
    }

    /**
     * Format the given date in format yyyyMMddHHmm
     *
     * @param date to format
     * @return date formatted in yyyyMMddHHmm
     */
    public static String formatyyyyMMddHHmm(Date date) {
        if (date == null)
            return null;
        return DateUtils.formatDate(date, yyyyMMddHHmm);
    }

    /**
     * Format the given date in given format
     *
     * @param date    to format
     * @param pattern e.g. yyyyMMddHHmm
     * @return formatted date
     */
    public static String formatDate(Date date, String pattern) {
        return DateUtils.formatDate(date, pattern);
    }

    /**
     * Parse the string to date
     *
     * @param date to parse. Accepted formats are yyyyMMddHHmm and yyyyMMdd
     * @return the parsed date or null if input could not be parsed
     */
    public static Date getDate(String date) {
        if (StringUtils.isEmpty(date))
            return null;
        return DateUtils.parseDate(date, patterns);
    }

    /**
     * Convert date to LocalDateTime using system default zone.
     * @param date Date to be transformed
     * @return converted date to LocalDateTime
     */
    public static LocalDateTime convert(Date date){
        if (date == null) return null;
        return date.toInstant().atZone(defaultZoneId).toLocalDateTime();
    }

    /**
     * Format the given date in format yyyyMMdd
     *
     * @param date to format
     * @return date formatted in yyyyMMdd
     */
    public static String formatyyyyMMdd(LocalDateTime date){
        if (date == null) return null;
        return date.format(DateTimeFormatter.ofPattern(yyyyMMdd));
    }

    /**
     * Format the given date in format yyyyMMddHHmm
     *
     * @param date to format
     * @return date formatted in yyyyMMddHHmm
     */
    public static String formatyyyyMMddHHmm(LocalDateTime date) {
        if (date == null) return null;
        return date.format(DateTimeFormatter.ofPattern(yyyyMMddHHmm));
    }

    /**
     * Parse the string to LocalDateTime
     *
     * @param date to parse. Accepted formats are yyyyMMddHHmm and yyyyMMdd
     * @return the parsed LocalDateTime or null if input could not be parsed
     */
    public static LocalDateTime getLocalDateTime(String date){
        if (StringUtils.isEmpty(date))
            return null;

        for (String pattern : patterns){
            LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(pattern));
            if (localDateTime != null)
                return localDateTime;
        }
        return null;
    }

}

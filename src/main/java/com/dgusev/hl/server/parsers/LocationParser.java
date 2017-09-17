package com.dgusev.hl.server.parsers;

import com.dgusev.hl.server.exceptions.BadRequest;
import com.dgusev.hl.server.model.Location;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created by dgusev on 17.08.2017.
 */
public class LocationParser {

    private static Field fieldS;
    private static Field fieldSB;

    static {
        try {
            fieldS = String.class.getDeclaredField("value");
            fieldS.setAccessible(true);
            fieldSB = Class.forName("java.lang.AbstractStringBuilder").getDeclaredField("value");
            fieldSB.setAccessible(true);
        } catch (NoSuchFieldException e) {

        } catch (ClassNotFoundException e) {
        }
    }

    public static Location parseLocation(char[] buf, int from, int count) {
        try {
            if (count == 0) {
                throw new BadRequest();
            }
            String input = new String(buf, from, count);
            char[] value = getValue(input);
            int position = 0;
            Location location = new Location();
            while (true) {
                if (input.charAt(position) == '}') {
                    return location;
                }
                int open = input.indexOf('"', position);
                if (open == -1) {
                    return location;
                }
                int close = input.indexOf('"', open + 1);
                String field = new String(value, open + 1, close - open - 1);
                if (field.equals("id")) {
                    int colon = input.indexOf(':', close);
                    int comma = input.indexOf(',', colon + 1);
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = input.indexOf('}', colon + 1);
                    } else {
                        totalEnd = input.indexOf(',', colon + 1);
                    }
                    int end = totalEnd;
                    colon++;
                    while (value[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (value[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && new String(value, colon, end + 1 - colon).equals("null")) {
                        throw new BadRequest();
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        location.id = Integer.valueOf(new String(value, colon, end + 1 - colon));
                    }
                    position = totalEnd;
                } else if (field.equals("country")) {
                    int colon = input.indexOf(':', close);
                    int comma = input.indexOf(',', colon + 1);
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = input.indexOf('}', colon + 1);
                    } else {
                        totalEnd = input.indexOf(',', colon + 1);
                    }
                    int end = totalEnd;
                    colon++;
                    while (value[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (value[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && new String(value, colon, end + 1 - colon).equals("null")) {
                        throw new BadRequest();
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        String country = parseString(value, colon, end + 1 - colon);
                        if (country.length()>50) {
                            throw new BadRequest();
                        } else {
                            location.country = country;
                        }
                    }
                    position = totalEnd;
                } else if (field.equals("city")) {
                    int colon = input.indexOf(':', close);
                    int comma = input.indexOf(',', colon + 1);
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = input.indexOf('}', colon + 1);
                    } else {
                        totalEnd = input.indexOf(',', colon + 1);
                    }
                    int end = totalEnd;
                    colon++;
                    while (value[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (value[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && new String(value, colon, end + 1 - colon).equals("null")) {
                        throw new BadRequest();
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        String city = parseString(value, colon, end + 1 - colon);
                        if (city.length()>50) {
                            throw new BadRequest();
                        } else {
                            location.city = city;
                        }
                    }
                    position = totalEnd;
                } else if (field.equals("place")) {
                    int colon = input.indexOf(':', close);
                    int comma = input.indexOf(',', colon + 1);
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = input.indexOf('}', colon + 1);
                    } else {
                        totalEnd = input.indexOf(',', colon + 1);
                    }
                    int end = totalEnd;
                    colon++;
                    while (value[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (value[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && new String(value, colon, end + 1 - colon).equals("null")) {
                        throw new BadRequest();
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        location.place = parseString(value, colon, end + 1 - colon);
                    }
                    position = totalEnd;
                } else if (field.equals("distance")) {
                    int colon = input.indexOf(':', close);
                    int comma = input.indexOf(',', colon + 1);
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = input.indexOf('}', colon + 1);
                    } else {
                        totalEnd = input.indexOf(',', colon + 1);
                    }
                    int end = totalEnd;
                    colon++;
                    while (value[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (value[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && new String(value, colon, end + 1 - colon).equals("null")) {
                        throw new BadRequest();
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        Integer distance = Integer.valueOf(new String(value, colon, end + 1 - colon));
                        if (distance < 0) {
                            throw new BadRequest();
                        } else {
                            location.distance=distance;
                        }
                    }
                    position = totalEnd;
                } else {
                    throw new BadRequest();
                }
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException | IOException | BadRequest ex) {
            throw new BadRequest();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static char[] getValue(String s) throws Exception {
        return ((char[]) fieldS.get(s));
    }

    private static char[] getValue(StringBuilder s) {
        try {
            return ((char[]) fieldSB.get(s));
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static String parseString(char[] buf, int start, int count) {
        StringBuilder stringBuilder = new StringBuilder(count / 6 + 1);
        int index = 0;
        while (index < count) {
            if (buf[start + index] == '\\') {
                stringBuilder.append((char) Integer.parseInt(new String(buf, start + index + 2, 4), 16));
                index += 6;
            } else {
                stringBuilder.append(buf[start + index]);
                index++;
            }
        }
        return new String(getValue(stringBuilder), 0, stringBuilder.length());
    }
}
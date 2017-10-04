package com.dgusev.hl.server.parsers;


import com.dgusev.hl.server.exceptions.BadRequest;
import com.dgusev.hl.server.model.User;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created by dgusev on 17.08.2017.
 */
public class UserParser {

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

    public static User parseUser(char[] buf, int from, int count) {
        try {
            if (count == 0) {
                throw BadRequest.INSTANCE;
            }
            String input = new String(buf, from, count);
            char[] value = getValue(input);
            int position = 0;
            User user = new User();
            while (true) {
                if (input.charAt(position) == '}') {
                    return user;
                }
                int open = input.indexOf('"', position);
                if (open == -1) {
                    return user;
                }
                int close = input.indexOf('"', open+1);
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
                        throw BadRequest.INSTANCE;
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        Integer id = Integer.valueOf(new String(value, colon, end + 1 - colon));
                        user.id = id;
                    }
                    position = totalEnd;
                } else if (field.equals("birth_date")) {
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
                        throw BadRequest.INSTANCE;
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        user.birthDate = Long.valueOf(new String(value, colon, end + 1 - colon));
                    }
                    position = totalEnd;
                } else if (field.equals("gender")) {
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
                        throw BadRequest.INSTANCE;
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        String gender = new String(value, colon, end + 1 - colon);
                        if (!gender.equals("f") && !gender.equals("m")) {
                            throw BadRequest.INSTANCE;
                        } else {
                            user.gender = new String(value, colon, end + 1 - colon);
                        }
                    }
                    position = totalEnd;
                } else if (field.equals("email")) {
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
                        throw BadRequest.INSTANCE;
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        String email = new String(value, colon, end + 1 - colon);
                        if (email.length() > 100) {
                            throw BadRequest.INSTANCE;
                        }
                        user.email = email;
                    }
                    position = totalEnd;
                } else if (field.equals("first_name")) {
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
                        throw BadRequest.INSTANCE;
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        String firstName = parseString(value, colon, end + 1 - colon);
                        if (firstName.length() > 50) {
                            throw BadRequest.INSTANCE;
                        } else {
                            user.firstName = firstName;
                        }
                    }
                    position = totalEnd;
                } else if (field.equals("last_name")) {
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
                        throw BadRequest.INSTANCE;
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        String lastName = parseString(value, colon, end + 1 - colon);
                        if (lastName.length() > 50) {
                            throw BadRequest.INSTANCE;
                        } else {
                            user.lastName = lastName;
                        }
                    }
                    position = totalEnd;
                } else {
                    throw BadRequest.INSTANCE;
                }
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException | IOException | BadRequest ex) {
            throw BadRequest.INSTANCE;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private static  char[] getValue(String s) throws Exception {
        return ((char[])fieldS.get(s));
    }

    private static char[] getValue(StringBuilder s)  {
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

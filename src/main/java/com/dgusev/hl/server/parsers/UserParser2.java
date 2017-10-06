package com.dgusev.hl.server.parsers;


import com.dgusev.hl.server.exceptions.BadRequest;
import com.dgusev.hl.server.model.User;
import com.dgusev.hl.server.util.CharArrayUtil;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created by dgusev on 17.08.2017.
 */
public class UserParser2 {

    private static final char[] ID_FIELD = "id".toCharArray();
    private static final char[] BD_FIELD = "birth_date".toCharArray();
    private static final char[] GENDER_FIELD = "gender".toCharArray();
    private static final char[] EMAIL_FIELD = "email".toCharArray();
    private static final char[] FIRST_NAME_FIELD = "first_name".toCharArray();
    private static final char[] LAST_NAME_FIELD = "last_name".toCharArray();
    private static final char[] NULL_FIELD = "null".toCharArray();

    public static User parseUser(char[] buf, int from, int to) {
            if (to - from == 0) {
                return null;
            }
            int position = from;
            User user = new User();
            while (true) {
                if (buf[position] == '}') {
                    return user;
                }
                int open = CharArrayUtil.indexOf(buf,position, to, '"');
                if (open == -1) {
                    return user;
                }
                int close = CharArrayUtil.indexOf(buf, open + 1, to, '"');
                if (CharArrayUtil.equals(buf, open + 1, close, ID_FIELD)) {
                    int colon = CharArrayUtil.indexOf(buf, close, to, ':');
                    int comma = CharArrayUtil.indexOf(buf, colon + 1, to ,',');
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, '}');
                    } else {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    }
                    int end = totalEnd;
                    colon++;
                    while (buf[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (buf[end] == ' ') {
                        end--;
                    }

                    if (end - colon == 3 && CharArrayUtil.equals(buf, colon, end + 1, NULL_FIELD)) {
                        return null;
                    } else {
                        while (buf[colon] == '"') {
                            colon++;
                        }
                        while (buf[end] == '"') {
                            end--;
                        }
                        int id = CharArrayUtil.parsePositiveInteger(buf, colon, end + 1);
                        if (id == -1) {
                            return null;
                        }
                        user.id = id;
                    }
                    position = totalEnd;
                } else if (CharArrayUtil.equals(buf, open + 1, close, BD_FIELD)) {
                    int colon = CharArrayUtil.indexOf(buf, close, to, ':');
                    int comma = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, '}');
                    } else {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    }
                    int end = totalEnd;
                    colon++;
                    while (buf[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (buf[end] == ' ') {
                        end--;
                    }

                    if (end - colon == 3 && CharArrayUtil.equals(buf, colon, end + 1, NULL_FIELD)) {
                        return null;
                    } else {
                        while (buf[colon] == '"') {
                            colon++;
                        }
                        while (buf[end] == '"') {
                            end--;
                        }
                        long birthDate = CharArrayUtil.parseLong(buf, colon, end + 1);
                        if (birthDate == -1) {
                            return null;
                        }
                        user.birthDate = birthDate;
                    }
                    position = totalEnd;
                } else if (CharArrayUtil.equals(buf, open + 1, close, GENDER_FIELD)) {
                    int colon = CharArrayUtil.indexOf(buf, close, to, ':');
                    int comma = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, '}');
                    } else {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    }
                    int end = totalEnd;
                    colon++;
                    while (buf[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (buf[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && CharArrayUtil.equals(buf, colon, end + 1, NULL_FIELD)) {
                        return null;
                    } else {
                        while (buf[colon] == '"') {
                            colon++;
                        }
                        while (buf[end] == '"') {
                            end--;
                        }
                        if (end + 1 - colon != 1) {
                            return null;
                        }
                        if (buf[colon] != 'f' && buf[colon] != 'm') {
                            return null;
                        } else {
                            user.gender = new String(buf, colon, 1);
                        }
                    }
                    position = totalEnd;
                } else if (CharArrayUtil.equals(buf, open + 1, close, EMAIL_FIELD)) {
                    int colon = CharArrayUtil.indexOf(buf, close, to, ':');
                    int comma = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, '}');
                    } else {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    }
                    int end = totalEnd;
                    colon++;
                    while (buf[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (buf[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && CharArrayUtil.equals(buf, colon, end + 1, NULL_FIELD)) {
                        return null;
                    } else {
                        while (buf[colon] == '"') {
                            colon++;
                        }
                        while (buf[end] == '"') {
                            end--;
                        }
                        String email = CharArrayUtil.createString(buf, colon, end + 1);
                        if (email.length() > 100) {
                            return null;
                        }
                        user.email = email;
                    }
                    position = totalEnd;
                } else if (CharArrayUtil.equals(buf, open + 1, close, FIRST_NAME_FIELD)) {
                    int colon = CharArrayUtil.indexOf(buf, close, to, ':');
                    int comma = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, '}');
                    } else {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    }
                    int end = totalEnd;
                    colon++;
                    while (buf[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (buf[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && CharArrayUtil.equals(buf, colon, end + 1, NULL_FIELD)) {
                        return null;
                    } else {
                        while (buf[colon] == '"') {
                            colon++;
                        }
                        while (buf[end] == '"') {
                            end--;
                        }
                        String firstName = CharArrayUtil.parseString(buf, colon, end + 1);
                        if (firstName.length() > 50) {
                            return null;
                        } else {
                            user.firstName = firstName;
                        }
                    }
                    position = totalEnd;
                } else if (CharArrayUtil.equals(buf, open + 1, close, LAST_NAME_FIELD)) {
                    int colon = CharArrayUtil.indexOf(buf, close, to, ':');
                    int comma = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    int totalEnd;
                    if (comma == -1) {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, '}');
                    } else {
                        totalEnd = CharArrayUtil.indexOf(buf, colon + 1, to, ',');
                    }
                    int end = totalEnd;
                    colon++;
                    while (buf[colon] == ' ') {
                        colon++;
                    }
                    end--;
                    while (buf[end] == ' ') {
                        end--;
                    }
                    if (end - colon == 3 && CharArrayUtil.equals(buf, colon, end + 1, NULL_FIELD)) {
                        return null;
                    } else {
                        while (buf[colon] == '"') {
                            colon++;
                        }
                        while (buf[end] == '"') {
                            end--;
                        }
                        String lastName = CharArrayUtil.parseString(buf, colon, end + 1);
                        if (lastName.length() > 50) {
                            return null;
                        } else {
                            user.lastName = lastName;
                        }
                    }
                    position = totalEnd;
                } else {
                    return null;
                }
            }
    }

}

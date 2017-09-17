package com.dgusev.hl.server.parsers;

import com.dgusev.hl.server.exceptions.BadRequest;
import com.dgusev.hl.server.model.Visit;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created by dgusev on 17.08.2017.
 */
public class VisitParser {

    private static Field field;

    static {
        try {
            field = String.class.getDeclaredField("value");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {

        }
    }

    public static Visit parseVisit(char[] buf, int from, int count) {
        try {
            if (count == 0) {
                throw new BadRequest();
            }
            String input = new String(buf, from, count);
            char[] value = getValue(input);
            int position = 0;
            Visit visit = new Visit();
            while (true) {
                if (input.charAt(position) == '}') {
                    return visit;
                }
                int open = input.indexOf('"', position);
                if (open == -1) {
                    return visit;
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
                        throw new BadRequest();
                    } else {
                        while (value[colon] == '"') {
                            colon++;
                        }
                        while (value[end] == '"') {
                            end--;
                        }
                        visit.id = Integer.valueOf(new String(value, colon, end + 1 - colon));
                    }
                    position = totalEnd;
                }  else if (field.equals("user")) {
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
                        visit.user = Integer.valueOf(new String(value, colon, end + 1 - colon));
                    }
                    position = totalEnd;
                } else if (field.equals("location")) {
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
                        visit.location = Integer.valueOf(new String(value, colon, end + 1 - colon));
                    }
                    position = totalEnd;
                } else if (field.equals("visited_at")) {
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
                        visit.visitedAt = Long.parseLong(new String(value, colon, end + 1 - colon));
                    }
                    position = totalEnd;
                } else if (field.equals("mark")) {
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
                        Integer mark = Integer.valueOf(new String(value, colon, end + 1 - colon));
                        if (mark < 0 || mark > 5) {
                            throw new BadRequest();
                        } else {
                            visit.mark = mark.byteValue();
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
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    private static  char[] getValue(String s) throws Exception {
        return ((char[])field.get(s));
    }

}

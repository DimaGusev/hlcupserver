package com.dgusev.hl.server.service;


import com.dgusev.hl.server.WebCache;
import com.dgusev.hl.server.exceptions.BadRequest;
import com.dgusev.hl.server.exceptions.EntityNotFound;
import com.dgusev.hl.server.model.Location;
import com.dgusev.hl.server.model.User;
import com.dgusev.hl.server.model.Visit;
import com.dgusev.hl.server.model.VisitResponse;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by dgusev on 12.08.2017.
 */
@Service
public class TravelService {

    public static volatile User[] users = new User[1100000];
    public static volatile Location[] locations = new Location[1100000];
    public static volatile Visit[] visits = new Visit[11000000];

    public static volatile Visit[][] dateUserVisitIndex = new Visit[1100000][];
    public static volatile Visit[][] dateLocationVisitIndex = new Visit[1100000][];

    public static final long[] dates = new long[200];

    static {

    }

    public void init(long serverTime) {
        ZonedDateTime current = Instant.ofEpochSecond(serverTime).atZone(ZoneId.of("UTC"));
        Period period = Period.ofYears(1);
        for (int i = 0; i < 200; i++) {
            current = current.minus(period);
            dates[200 - i - 1] = current.toInstant().getEpochSecond();
        }
    }


    private final Set<String> userEmails = new HashSet<>(1100000, 1);

    public User getUser(Integer id) {
        User user = users[id];
        if (user == null) {
            throw EntityNotFound.INSTANCE;
        } else {
            return user;
        }
    }

    public void createUser(User user) {
        if (users[user.id] != null) {
            throw BadRequest.INSTANCE;
        }
        if (userEmails.contains(user.email)) {
            throw BadRequest.INSTANCE;
        }
        userEmails.add(user.email);
        user.age = 201 + Arrays.binarySearch(dates, user.birthDate);
        users[user.id] = user;
        users = users;
        WebCache.cacheUser(user);
    }

    public boolean validateUser(int id) {
        if (id >= 1100000 || id < 0 || users[id] == null) {
            return false;
        }
        return true;
    }

    public void updateUser(User user) {
        User dbUser = users[user.id];
        if (user.email != null) {
            if (!dbUser.email.equals(user.email)) {
                if (userEmails.contains(user.email)) {
                    throw BadRequest.INSTANCE;
                }
                userEmails.remove(dbUser.email);
                userEmails.add(user.email);
            }
            dbUser.email = user.email;
        }
        if (user.birthDate != null) {
            dbUser.birthDate = user.birthDate;
            dbUser.age = 201 + Arrays.binarySearch(dates, dbUser.birthDate);
        }
        if (user.firstName != null) {
            dbUser.firstName = user.firstName;
        }
        if (user.lastName != null) {
            dbUser.lastName = user.lastName;
        }
        if (user.gender != null) {
            dbUser.gender = user.gender;
        }
        users = users;
        WebCache.cacheUser(dbUser);
    }


    public Location getLocation(Integer id) {
        Location user = locations[id];
        if (user == null) {
            throw EntityNotFound.INSTANCE;
        } else {
            return user;
        }
    }

    public void createLocation(Location location) {

        if (locations[location.id] != null) {
            throw BadRequest.INSTANCE;
        } else {
            locations[location.id] = location;
        }
        locations = locations;
        WebCache.cacheLocation(location);
    }

    public boolean validateLocation(int id) {
        if (id >= 1100000 || id < 0 || locations[id] == null) {
            return false;
        }
        return true;
    }

    public void updateLocation(Location location) {
        Location dbLocation = locations[location.id];
        if (location.city != null) {
            dbLocation.city = location.city;
        }
        if (location.country != null) {
            dbLocation.country = location.country;
        }
        if (location.distance != null) {
            dbLocation.distance = location.distance;
        }
        if (location.place != null) {
            dbLocation.place = location.place;
        }
        locations = locations;
        WebCache.cacheLocation(dbLocation);
    }


    public Visit getVisit(Integer id) {
        return visits[id];
    }

    public void createVisit(Visit visit) {
        if (visit.user == -1 || visit.location == -1 || visit.id == -1) {
            throw BadRequest.INSTANCE;
        }
        if (users[visit.user] == null || locations[visit.location] == null || visits[visit.id] != null) {
            throw BadRequest.INSTANCE;
        }
        visits[visit.id] = visit;
        if (dateUserVisitIndex[visit.user] == null) {
            dateUserVisitIndex[visit.user] = new Visit[0];
        }
        Visit[] dateListConcurrentSkipListMap = dateUserVisitIndex[visit.user];
        Visit[] newArray = new Visit[dateListConcurrentSkipListMap.length + 1];
        System.arraycopy(dateListConcurrentSkipListMap, 0, newArray, 0, dateListConcurrentSkipListMap.length);
        dateUserVisitIndex[visit.user] = newArray;
        dateListConcurrentSkipListMap = newArray;
        int index = 0;
        for (; dateListConcurrentSkipListMap[index] != null && dateListConcurrentSkipListMap[index].visitedAt < visit.visitedAt; index++)
            ;
        System.arraycopy(dateListConcurrentSkipListMap, index, dateListConcurrentSkipListMap, index + 1,
                dateListConcurrentSkipListMap.length - index - 1);
        dateListConcurrentSkipListMap[index] = visit;
        index = 0;
        if (dateLocationVisitIndex[visit.location] == null) {
            dateLocationVisitIndex[visit.location] = new Visit[0];
        }
        Visit[] dateLocationListConcurrentSkipListMap = dateLocationVisitIndex[visit.location];
        Visit[] newArray2 = new Visit[dateLocationListConcurrentSkipListMap.length + 1];
        System.arraycopy(dateLocationListConcurrentSkipListMap, 0, newArray2, 0, dateLocationListConcurrentSkipListMap.length);
        dateLocationVisitIndex[visit.location] = newArray2;
        dateLocationListConcurrentSkipListMap = newArray2;
        for (; dateLocationListConcurrentSkipListMap[index] != null && dateLocationListConcurrentSkipListMap[index].visitedAt < visit.visitedAt; index++)
            ;
        System.arraycopy(dateLocationListConcurrentSkipListMap, index, dateLocationListConcurrentSkipListMap, index + 1,
                dateLocationListConcurrentSkipListMap.length - index - 1);
        dateLocationListConcurrentSkipListMap[index] = visit;
        visits = visits;
    }

    public boolean validateVisit(int id) {
        if (id >= 11000000 || id < 0 || visits[id] == null) {
            return false;
        }
        return true;
    }

    public void updateVisit(Visit visit) {
        if (visit.user != -1) {
            if (users[visit.user] == null) {
                throw BadRequest.INSTANCE;
            }
        }
        if (visit.location != -1) {
            if (locations[visit.location] == null) {
                throw BadRequest.INSTANCE;
            }
        }
        Visit dbVisit = visits[visit.id];
        if (visit.mark != -1) {
            dbVisit.mark = visit.mark;
        }

        boolean locationDateCorrected = false;
        boolean userDateCorrected = false;

        if (visit.location != -1) {
            if (visit.location!=dbVisit.location) {
                Visit[] dateLocationConcurrentSkipListMap = dateLocationVisitIndex[dbVisit.location];
                int index = 0;
                for (; dateLocationConcurrentSkipListMap[index] != dbVisit; index++) ;
                System.arraycopy(dateLocationConcurrentSkipListMap, index + 1, dateLocationConcurrentSkipListMap, index,
                        dateLocationConcurrentSkipListMap.length - index - 1);
                dateLocationConcurrentSkipListMap[dateLocationConcurrentSkipListMap.length -1] = null;
                if (dateLocationVisitIndex[visit.location] == null) {
                    dateLocationVisitIndex[visit.location] = new Visit[0];
                }
                Visit[] newIndex = dateLocationVisitIndex[visit.location];
                Visit[] newArray3 = new Visit[newIndex.length + 1];
                System.arraycopy(newIndex, 0, newArray3, 0, newIndex.length);
                dateLocationVisitIndex[visit.location] = newArray3;
                newIndex = newArray3;

                index = 0;
                for (; newIndex[index] != null && newIndex[index].visitedAt < (visit.visitedAt != Long.MIN_VALUE ? visit.visitedAt : dbVisit.visitedAt); index++)
                    ;
                System.arraycopy(newIndex, index, newIndex, index + 1,
                        newIndex.length - index - 1);
                newIndex[index] = dbVisit;
                locationDateCorrected = true;
            }
            dbVisit.location = visit.location;
        }
        if (visit.user != -1) {
            if (visit.user != dbVisit.user) {
                Visit[] dateLocationConcurrentSkipListMap = dateUserVisitIndex[dbVisit.user];
                int index = 0;
                for (; dateLocationConcurrentSkipListMap[index] != dbVisit; index++) ;
                System.arraycopy(dateLocationConcurrentSkipListMap, index + 1, dateLocationConcurrentSkipListMap, index,
                        dateLocationConcurrentSkipListMap.length - index - 1);
                dateLocationConcurrentSkipListMap[dateLocationConcurrentSkipListMap.length -1] = null;
                if (dateUserVisitIndex[visit.user] == null) {
                    dateUserVisitIndex[visit.user] = new Visit[0];
                }
                Visit[] newIndex = dateUserVisitIndex[visit.user];
                Visit[] newArray3 = new Visit[newIndex.length + 1];
                System.arraycopy(newIndex, 0, newArray3, 0, newIndex.length);
                dateUserVisitIndex[visit.user] = newArray3;
                newIndex = newArray3;
                index = 0;
                for (; newIndex[index] != null && newIndex[index].visitedAt < (visit.visitedAt != Long.MIN_VALUE ? visit.visitedAt : dbVisit.visitedAt); index++)
                    ;
                System.arraycopy(newIndex, index, newIndex, index + 1,
                        newIndex.length - index - 1);
                newIndex[index] = dbVisit;
                userDateCorrected = true;
            }
            dbVisit.user = visit.user;
        }
        if (visit.visitedAt != Long.MIN_VALUE) {
            if (dbVisit.visitedAt!=visit.visitedAt) {
                if (!userDateCorrected) {
                    Visit[] dateListConcurrentSkipListMap = dateUserVisitIndex[dbVisit.user];
                    int index = 0;
                    for (; dateListConcurrentSkipListMap[index] != dbVisit; index++) ;
                    System.arraycopy(dateListConcurrentSkipListMap, index + 1, dateListConcurrentSkipListMap, index,
                            dateListConcurrentSkipListMap.length - index - 1);
                    dateListConcurrentSkipListMap[dateListConcurrentSkipListMap.length-1] = null;
                    index = 0;
                    for (; dateListConcurrentSkipListMap[index] != null && dateListConcurrentSkipListMap[index].visitedAt < visit.visitedAt; index++)
                        ;
                    System.arraycopy(dateListConcurrentSkipListMap, index, dateListConcurrentSkipListMap, index + 1,
                            dateListConcurrentSkipListMap.length - index - 1);
                    dateListConcurrentSkipListMap[index] = dbVisit;
                }
                if (!locationDateCorrected) {
                    Visit[] dateLocationConcurrentSkipListMap = dateLocationVisitIndex[dbVisit.location];
                    int index = 0;
                    for (; dateLocationConcurrentSkipListMap[index] != dbVisit; index++) ;
                    System.arraycopy(dateLocationConcurrentSkipListMap, index + 1, dateLocationConcurrentSkipListMap, index,
                            dateLocationConcurrentSkipListMap.length - index - 1);
                    dateLocationConcurrentSkipListMap[dateLocationConcurrentSkipListMap.length-1] = null;
                    index = 0;
                    for (; dateLocationConcurrentSkipListMap[index] != null && dateLocationConcurrentSkipListMap[index].visitedAt < visit.visitedAt; index++)
                        ;
                    System.arraycopy(dateLocationConcurrentSkipListMap, index, dateLocationConcurrentSkipListMap, index + 1,
                            dateLocationConcurrentSkipListMap.length - index - 1);
                    dateLocationConcurrentSkipListMap[index] = dbVisit;
                }
            }
            dbVisit.visitedAt = visit.visitedAt;
        }
        visits = visits;
    }

    public int searchUserVisits(Integer userId, Long fromDate, Long toDate, String country, Integer toDistance, VisitResponse[]  visitResponses) {
        int position = 0;
        if (fromDate != null && toDate != null) {
            Visit[] userIndex = dateUserVisitIndex[userId];
            if (userIndex == null) {
                return 0;
            }
            for (int i = 0; i < userIndex.length && userIndex[i] != null; i++) {
                Visit visit = userIndex[i];
                if (visit.visitedAt > fromDate && visit.visitedAt < toDate) {
                    if (filterVisits(visit, country, toDistance, visitResponses[position])) {
                        position++;
                    }
                }
            }
            return position;
        } else if (fromDate != null) {
            Visit[] userIndex = dateUserVisitIndex[userId];
            if (userIndex == null) {
                return 0;
            }
            for (int i = 0; i < userIndex.length && userIndex[i] != null; i++) {
                Visit visit = userIndex[i];
                if (visit.visitedAt > fromDate) {
                    if (filterVisits(visit, country, toDistance, visitResponses[position])) {
                        position++;
                    }
                }
            }
            return position;
        } else if (toDate != null) {
            Visit[] userIndex = dateUserVisitIndex[userId];
            if (userIndex == null) {
                return 0;
            }
            for (int i = 0; i < userIndex.length && userIndex[i] != null; i++) {
                Visit visit = userIndex[i];
                if (visit.visitedAt < toDate) {
                    if (filterVisits(visit, country, toDistance, visitResponses[position])) {
                        position++;
                    }
                }
            }
            return position;
        } else {
            Visit[] userIndex = dateUserVisitIndex[userId];
            if (userIndex == null) {
                return 0;
            }
            for (int i = 0; i < userIndex.length && userIndex[i] != null; i++) {
                Visit visit = userIndex[i];
                if (filterVisits(visit, country, toDistance, visitResponses[position])) {
                    position++;
                }
            }
            return position;
        }
    }

    public boolean filterVisits(Visit v, String country, Integer toDistance, VisitResponse response) {
        if (country == null && toDistance == null) {
            response.mark = v.mark;
            response.visitedAt = v.visitedAt;
            response.place = locations[v.location].place;
            return true;
        }
        Location location = locations[v.location];
        if (country != null) {
            if (!location.country.equals(country)) {
                return false;
            }
        }
        if (toDistance != null) {
            if (location.distance >= toDistance) {
                return false;
            }
        }
        response.mark = v.mark;
        response.visitedAt = v.visitedAt;
        response.place = location.place;
        return true;
    }

    public Double calculateLocationMark(Integer locationId, Long fromDate, Long toDate, Integer fromAge, Integer toAge, String gender) {
        if (fromDate != null && toDate != null) {
            Visit[] locationIndex = dateLocationVisitIndex[locationId];
            if (locationIndex == null) {
                return 0.0;
            }
            double total = 0;
            double count = 0;
            for (int i = 0; i < locationIndex.length && locationIndex[i] != null; i++) {
                Visit visit = locationIndex[i];
                if (visit.visitedAt > fromDate && visit.visitedAt < toDate && filterVisits(visit, fromAge, toAge, gender)) {
                    count++;
                    total += visit.mark;
                }
            }
            if (count == 0) {
                return 0.0;
            } else {
                return total / count;
            }
        } else if (fromDate != null) {
            Visit[] locationIndex = dateLocationVisitIndex[locationId];
            if (locationIndex == null) {
                return 0.0;
            }
            double total = 0;
            double count = 0;
            for (int i = 0; i < locationIndex.length && locationIndex[i] != null; i++) {
                Visit visit = locationIndex[i];
                if (visit.visitedAt > fromDate && filterVisits(visit, fromAge, toAge, gender)) {
                    count++;
                    total += visit.mark;
                }
            }
            if (count == 0) {
                return 0.0;
            } else {
                return total / count;
            }
        } else if (toDate != null) {
            Visit[] locationIndex = dateLocationVisitIndex[locationId];
            if (locationIndex == null) {
                return 0.0;
            }
            double total = 0;
            double count = 0;
            for (int i = 0; i < locationIndex.length && locationIndex[i] != null; i++) {
                Visit visit = locationIndex[i];
                if (visit.visitedAt < toDate && filterVisits(visit, fromAge, toAge, gender)) {
                    count++;
                    total += visit.mark;
                }
            }
            if (count == 0) {
                return 0.0;
            } else {
                return total / count;
            }
        } else {
            Visit[] locationIndex = dateLocationVisitIndex[locationId];
            if (locationIndex == null) {
                return 0.0;
            }
            double total = 0;
            double count = 0;
            for (int i = 0; i < locationIndex.length && locationIndex[i] != null; i++) {
                Visit visit = locationIndex[i];
                if (filterVisits(visit, fromAge, toAge, gender)) {
                    count++;
                    total += visit.mark;
                }
            }
            if (count == 0) {
                return 0.0;
            } else {
                return total / count;
            }
        }
    }

    public boolean filterVisits(Visit v, Integer fromAge, Integer toAge, String gender) {
        if (fromAge == null && toAge == null && gender == null) {
            return true;
        }
        User user = users[v.user];
        if (gender != null && !user.gender.equals(gender)) {
            return false;
        }

        if (fromAge == null && toAge == null) {
            return true;
        }

        if (fromAge != null) {
            if (user.age < fromAge) {
                return false;
            }
        }

        if (toAge != null) {
            if (user.age >= toAge) {
                return false;
            }
        }

        return true;
    }

}

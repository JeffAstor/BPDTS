package io.swagger.api;



import io.swagger.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-09-01T10:41:34.364Z")

@Controller
public class UserApiController implements UserApi {

    private static final Logger log = LoggerFactory.getLogger(UserApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;
    private final String url_default = "http://bpdts-test-app.herokuapp.com";

    @org.springframework.beans.factory.annotation.Autowired
    public UserApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();

    }

    private  double getDistanceFrom(double LongA, double LatA,double LongB, double LatB)
    {
        // Using haversine formula
        // Assuming input is Signed degree format
        // Latitudes range from - 90 to 90.
        // Longitudes range from - 180 to 180.


        double R = 6371e3; // metres (magic number)
        double radlatA = Math.PI * LatA / 180.0;
        double radlatB = Math.PI * LatB / 180.0;

        double delta_lat = radlatA - radlatB;
        double delta_long = ((LongB - LongA) / 180.0) * Math.PI;

        double a = Math.sin(delta_lat / 2) * Math.sin(delta_lat / 2) + Math.cos(radlatB) * Math.cos(radlatA) * Math.sin(delta_long / 2) * Math.sin(delta_long / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double d = R * c;
        return d / 1609;
    }

    private User[] getUserListFromURL(String url_string)
    {
        User[] ua;
        RestTemplate restTemplate = getRestTemplate();

        ResponseEntity<User[]> responseEntity = restTemplate.getForEntity(url_string,User[].class);
        ua = responseEntity.getBody();

        return ua;
    }


    public ResponseEntity<List<User>> findUsersInOrNear(@NotNull @ApiParam(value = "longitude in signed decimal", required = true) @Valid @RequestParam(value = "longitude", required = true) Double longitude,@NotNull @ApiParam(value = "latitude in signed decimal", required = true) @Valid @RequestParam(value = "latitude", required = true) Double latitude,@NotNull @ApiParam(value = "Maximum distance from coordinates in miles. The search range.", required = true) @Valid @RequestParam(value = "distance", required = true) Double distance,@ApiParam(value = "User home location.  Example, London") @Valid @RequestParam(value = "location", required = false) String location) {
        String accept = request.getHeader("Accept");

        if (accept != null && accept.contains("application/json")) {

            // Check input for bad request

            if ((latitude <-90)||(latitude > 90))
                throw new InputException("Latitude is out of range (-90 - 90)");
            if ((longitude <-180)||(longitude > 180))
                throw new InputException("Longitude is out of range (-180 - 180)");
            if (distance<0)
                throw new InputException("Distance from coordinates can not be below zero");



            try {
                // city wasn't returned as a field, I have 2 lists. I could be testing the same
                // user twice.  set check_for_duplicate if having duplicates is bad. I assume it is
                boolean check_for_duplicate = true;

                List<User> pojoList = new ArrayList<User>();
                User[] usr_all_array = null;
                User[] usr_inloc_array = null;
                double d = 0;

                usr_all_array = getUserListFromURL(url_default + "/users");
                if (location!=null) {
                    usr_inloc_array = getUserListFromURL(url_default + "/city/" + location + "/users");
                }


                if (usr_inloc_array!=null) {
                    for (User usr : usr_inloc_array) {
                        pojoList.add(usr);
                    }
                }

                if (usr_all_array!=null) {
                    for (User usr : usr_all_array) {
                        d = getDistanceFrom(longitude, latitude, usr.getLongitude(), usr.getLatitude());
                        if (d < distance) {
                            if ((check_for_duplicate) && (usr_inloc_array!=null)){
                                boolean dupe=false;
                                long id_1 = usr.getId();
                                for (User usr2 : usr_inloc_array) {
                                    long id_2 = usr2.getId();

                                    // comp with getId() failed, hence the 2 id vars
                                    if (id_1==id_2) {
                                        dupe = true;  // duplicate, don't add
                                        break;
                                    }
                                }
                                if (!dupe)
                                    pojoList.add(usr);
                            }
                            else { pojoList.add(usr); }
                        }
                    }
                }
                if (pojoList.size()==0) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                }

                return new ResponseEntity<List<User>>(pojoList, HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<User>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }

        return new ResponseEntity<List<User>>(HttpStatus.NOT_IMPLEMENTED);
    }

}

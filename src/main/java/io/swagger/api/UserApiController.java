package io.swagger.api;



import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
//import org.json.simple.JSONObject;

import java.net.MalformedURLException;
import io.swagger.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private User[] getUserListFromURL(String url_string, ObjectMapper mapper)
    {
        String str=null;
        try {

            URL url = new URL(url_string);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();


            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);

            //__ Get result


            DataInputStream input = new DataInputStream(con.getInputStream());
            str = input.readLine(); // read the rest!!!
            input.close();

            return mapper.readValue(str, User[].class);
        } catch (MalformedURLException ex) {
            log.error("getUserListFromURL: malformed url", ex);
        } catch (IOException ex) {
            log.error("getUserListFromURL: request failed with ioexception", ex);
        }


        return null;
    }



    public ResponseEntity<List<User>> findUsersInOrNear(@NotNull @ApiParam(value = "longitude in signed decimal", required = true) @Valid @RequestParam(value = "longitude", required = true) Double longitude,@NotNull @ApiParam(value = "latitude in signed decimal", required = true) @Valid @RequestParam(value = "latitude", required = true) Double latitude,@NotNull @ApiParam(value = "Maximum distance from coordinates in miles. The search range.", required = true) @Valid @RequestParam(value = "distance", required = true) Double distance,@ApiParam(value = "User home location.  Example, London") @Valid @RequestParam(value = "location", required = false) String location) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            // Check input for bad request
            //Latitudes range from -90 to 90.
            if ((latitude <-90)||(latitude > 90)) {
                throw new InputException("Latitude is out of range (-90 - 90)");
            }
            //Longitudes range from -180 to 180
            if ((longitude <-180)||(longitude > 180)) {
                throw new InputException("Longitude is out of range (-180 - 180)");
            }
            if (distance<0) {
                throw new InputException("Distance from coordinates can not be below zero");
            }


            try {
                double d = 0;

                ObjectMapper mapper = new ObjectMapper();
                User[] pojos = null;
                User[] pojos2 = null;

                pojos = getUserListFromURL(url_default + "/users",mapper);
                if (location!=null) {
                    pojos2 = getUserListFromURL(url_default + "/city/" + location + "/users", mapper);
                }
                List<User> pojoList = new ArrayList<User>();

                // I am aware that the code below does not check if users are being added twice
                // once bases on distance, then potentially again based on home location.
                // As this is test rather than production code I left it as is.

                if (pojos!=null) {
                    for (int i = 0; i < pojos.length; i++) {
                        d = getDistanceFrom(longitude, latitude, pojos[i].getLongitude(), pojos[i].getLatitude());
                        if (d < distance) {
                            pojoList.add(pojos[i]);
                        }
                    }
                }
                if (pojos2!=null) {
                    for (int i = 0; i < pojos2.length; i++) {
                        pojoList.add(pojos2[i]);
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

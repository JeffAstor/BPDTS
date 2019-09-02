using System;
using System.Threading.Tasks;
using System.Collections.Generic;
using Newtonsoft.Json;

/* 
Quick and dirty application to test getDistanceFrom function with data obtained from web.
*/
namespace DWP_CodingTest
{
    class GeoLocation
    {
        public GeoLocation(string Location_name, double Long, double Lat)
        {
            setInfo(Location_name, Long, Lat);
        }

        public void setInfo(string Location_name, double Long, double Lat)
        {
            location = Location_name;
            longitude = Long;
            latitude = Lat;
            isSet = true;
        }

        public double getDistanceFrom(double Long, double Lat)
        {
            // Using haversine formula
            // Assuming input is Signed degree format
            // Latitudes range from - 90 to 90.
            // Longitudes range from - 180 to 180.

            if (!IsSet)
                return -1.0;        // error (throw!)

            double R = 6371e3; // metres (magic number)
            double radlat1 = Math.PI * Lat / 180.0;
            double radlat2 = Math.PI * this.Latitude / 180.0;

            double delta_lat = radlat2 - radlat1;
            double delta_long = ((this.Longitude - Long) / 180.0) * Math.PI;

            var a = Math.Sin(delta_lat / 2) * Math.Sin(delta_lat / 2) + Math.Cos(radlat1) * Math.Cos(radlat2) * Math.Sin(delta_long / 2) * Math.Sin(delta_long / 2);
            var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));

            var d = R * c;
            return d;
        }

        public double getDistanceFrom(GeoLocation place)
        {
            // lat1 is place
            if (!this.IsSet)
                return -1.0;        // error (throw!)
            if (!place.IsSet)
                return -1.0;        // error (Throw!)

            double d = getDistanceFrom(place.Longitude, place.Latitude);

            return d;
        }
        // Properties
        public string Location
        {
            get { return location; }
        }
        public double Longitude
        {
            get { return longitude; }
        }
        public double Latitude { get { return latitude; } }            // North to South
        public bool IsSet { get { return isSet; } }

        // Private members
        private string location = "not set";
        private double longitude = 0.0;           // East to West in Degrees
        private double latitude = 0.0;            // North to South in Degrees
        private bool isSet = false;

    }

    // Copy of class returned from server, for JSON deserialize
    public class User
    {
        public int Id { get; set; }
        public string First_name { get; set; }
        public string Last_name { get; set; }
        public string Email { get; set; }
        public string Ip_address { get; set; }
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public string City { get; set; }

        public void DisplayInConsole()
        {
            System.Console.WriteLine("User {");
            System.Console.WriteLine("\tID:{0}", Id);
            System.Console.WriteLine("\tFirst_name:{0}", First_name);
            System.Console.WriteLine("\tLast_name:{0}", Last_name);
            System.Console.WriteLine("\tEmail:{0}", Email);
            System.Console.WriteLine("\tIp_address:{0}", Ip_address);
            System.Console.WriteLine("\tLatitude:{0}", Latitude);
            System.Console.WriteLine("\tLongitude:{0}", Longitude);
            System.Console.WriteLine("}");
        }
    }

    class Program
    {
        static readonly System.Net.Http.HttpClient client = new System.Net.Http.HttpClient();

        static private async Task<string> GetHTTPBody(string url)
        {
            var response = await client.GetAsync(url);

            response.EnsureSuccessStatusCode();
            string responseBody = await response.Content.ReadAsStringAsync();
            return responseBody;
        }

        static private double meterToMile(double distanceInMetres)
        {
            return distanceInMetres / 1609;
        }

        static private double mileToMeter(double distanceInMiles)
        {
            return distanceInMiles * 1609;
        }

        static async Task Main(string[] args)
        {
            GeoLocation Place = new GeoLocation("London", -0.118092, 51.5074);
#if _DEBUG
            GeoLocation Stratford = new GeoLocation("Stratford", -0.0308128, 51.545213);
            GeoLocation ShepardsBush = new GeoLocation("Sheppards Bush", -0.2431452, 51.5101494);
            GeoLocation Manchester = new GeoLocation("Manchester", -2.2936736, 53.4722249);

            

            Console.WriteLine("Distance from {0} to {1} is {2} meters, {3} miles", Place.Location, Stratford.Location, Place.getDistanceFrom(Stratford), meterToMile(Place.getDistanceFrom(Stratford)));
            Console.WriteLine("Distance from {0} to {1} is {2} meters, {3} miles", Place.Location, ShepardsBush.Location, Place.getDistanceFrom(ShepardsBush), meterToMile(Place.getDistanceFrom(ShepardsBush)));
            Console.WriteLine("Distance from {0} to {1} is {2} meters, {3} miles", Place.Location, Manchester.Location, Place.getDistanceFrom(Manchester), meterToMile(Place.getDistanceFrom(Manchester)));
#endif

            double MaxDistanceMile = 50.0;
            double MaxDistanceMeter = mileToMeter(MaxDistanceMile);
            string url_default = "http://bpdts-test-app.herokuapp.com";
            Console.WriteLine("Jeff Astor DWP Test");

            if (args.Length > 0)
            {
                //  Console.WriteLine("Hello World!");
            }
            else
            {
                Console.WriteLine("No parameters passed, using default data. Location {0}, Long {1}, Lat {2} ", Place.Location, Place.Longitude, Place.Latitude);
            }
            Console.WriteLine("Requesting user data from server, please wait");
            var taskGetLondonUsers = GetHTTPBody(url_default + "/city/" + Place.Location + "/users");
            var taskGetUsers = GetHTTPBody(url_default + "/users");

            string LocationUsersJSON = await taskGetLondonUsers;
            string UserListJSON = await taskGetUsers;

            Console.WriteLine("Obtained user data, building list.");
            List<User> UsersInLocation = JsonConvert.DeserializeObject<List<User>>(LocationUsersJSON);
            List<User> UsersAll = JsonConvert.DeserializeObject<List<User>>(UserListJSON);

            List<User> UsersWithin = new List<User>();
            Console.WriteLine("Users listed as living in {0}:", Place.Location);
            foreach (User usr in UsersInLocation)
            {
                UsersWithin.Add(usr);
                usr.DisplayInConsole();
            }
            Console.WriteLine("Users currently within {0} Miles of {1}", MaxDistanceMile, Place.Location);
            double dist;

            foreach (User usr in UsersAll)
            {
                dist = Place.getDistanceFrom(usr.Longitude, usr.Latitude);
                if (dist <= MaxDistanceMeter)
                {
                    UsersWithin.Add(usr);
                    usr.DisplayInConsole();
                    Console.WriteLine("Distance: {0}", meterToMile(dist));
                }
            }
        }
    }
}
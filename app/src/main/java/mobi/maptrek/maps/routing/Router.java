package mobi.maptrek.maps.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;

import java.util.Locale;

import mobi.maptrek.MapTrek;

public class Router {
    public ResponsePath compute(String osmFilePath, double fromLat, double fromLon, double toLat, double toLon) {
        GraphHopper hopper = createGraphHopperInstance(osmFilePath);
        ResponsePath result = routing(hopper, fromLat, fromLon, toLat, toLon);
//        speedModeVersusFlexibleMode(hopper);
//        headingAndAlternativeRoute(hopper);
//        customizableRouting(relDir + "core/files/andorra.osm.pbf");
//
//        // release resources to properly shutdown or start a new instance
        hopper.close();
        return result;
    }

    static GraphHopper createGraphHopperInstance(String ghLoc) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(ghLoc);
        // specify where to store graphhopper files
        String graphHopperPathDataPath = MapTrek.getApplication().getApplicationContext().getCacheDir().getAbsolutePath();
        hopper.setGraphHopperLocation(graphHopperPathDataPath);

        // see docs/core/profiles.md to learn more about profiles
        Profile routingProfile = new Profile(("hike"));
        routingProfile.setVehicle("hike");
        routingProfile.setWeighting("short_fastest");
        hopper.setProfiles(routingProfile);

        // now this can take minutes if it imports or a few seconds for loading of course this is dependent on the area you import
        hopper.importOrLoad();
        return hopper;
    }

    public static ResponsePath routing(GraphHopper hopper, double fromLat, double fromLon, double toLat, double toLon) {
        // simple configuration of the request object
        GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                // note that we have to specify which profile we are using even when there is only one like here
                        setProfile("hike").
                // define the language for the turn instructions
                        setLocale(Locale.US);
        GHResponse rsp = hopper.route(req);

        // handle errors
        if (rsp.hasErrors())
            throw new RuntimeException(rsp.getErrors().toString());

        // use the best path, see the GHResponse class for more possibilities.
        return rsp.getBest();
    }
}

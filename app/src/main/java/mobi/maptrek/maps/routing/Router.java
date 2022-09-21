package mobi.maptrek.maps.routing;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.LMProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.weighting.custom.CustomProfile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;

import java.util.Arrays;
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
        String graphHopperPathDataPath = MapTrek.getApplication().getExternalDir("graphhopper").getPath();
        hopper.setGraphHopperLocation(graphHopperPathDataPath);

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(new Profile("hike"));

        // this enables speed mode for the profile we called car
//        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));

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

    public static void speedModeVersusFlexibleMode(GraphHopper hopper) {
        GHRequest req = new GHRequest(42.508552, 1.532936, 42.507508, 1.528773).
                setProfile("car").setAlgorithm(Parameters.Algorithms.ASTAR_BI).putHint(Parameters.CH.DISABLE, true);
        GHResponse res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());
        assert Helper.round(res.getBest().getDistance(), -2) == 900;
    }

    public static void headingAndAlternativeRoute(GraphHopper hopper) {
        // define a heading (direction) at start and destination
        GHRequest req = new GHRequest().setProfile("car").
                addPoint(new GHPoint(42.508774, 1.535414)).addPoint(new GHPoint(42.506595, 1.528795)).
                setHeadings(Arrays.asList(180d, 90d)).
                // use flexible mode (i.e. disable contraction hierarchies) to make heading and pass_through working
                        putHint(Parameters.CH.DISABLE, true);
        // if you have via points you can avoid U-turns there with
        // req.getHints().putObject(Parameters.Routing.PASS_THROUGH, true);
        GHResponse res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());
        assert res.getAll().size() == 1;
        assert Helper.round(res.getBest().getDistance(), -2) == 800;

        // calculate alternative routes between two points (supported with and without CH)
        req = new GHRequest().setProfile("car").
                addPoint(new GHPoint(42.502904, 1.514714)).addPoint(new GHPoint(42.511953, 1.535914)).
                setAlgorithm(Parameters.Algorithms.ALT_ROUTE);
        req.getHints().putObject(Parameters.Algorithms.AltRoute.MAX_PATHS, 3);
        res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());
        assert res.getAll().size() == 2;
        assert Helper.round(res.getBest().getDistance(), -2) == 2300;
    }

    /**
     * To customize profiles in the config.yml file you can use a json or yml file or embed it directly. See this list:
     * web/src/test/resources/com/graphhopper/application/resources and https://www.graphhopper.com/?s=customizable+routing
     */
    public static void customizableRouting(String ghLoc) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(ghLoc);
        hopper.setGraphHopperLocation("target/routing-custom-graph-cache");
        hopper.setProfiles(new CustomProfile("car_custom").setCustomModel(new CustomModel()).setVehicle("car"));

        // The hybrid mode uses the "landmark algorithm" and is up to 15x faster than the flexible mode (Dijkstra).
        // Still it is slower than the speed mode ("contraction hierarchies algorithm") ...
        hopper.getLMPreparationHandler().setLMProfiles(new LMProfile("car_custom"));
        hopper.importOrLoad();

        // ... but for the hybrid mode we can customize the route calculation even at request time:
        // 1. a request with default preferences
        GHRequest req = new GHRequest().setProfile("car_custom").
                addPoint(new GHPoint(42.506472, 1.522475)).addPoint(new GHPoint(42.513108, 1.536005));

        GHResponse res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());

        assert Math.round(res.getBest().getTime() / 1000d) == 96;

        // 2. now avoid primary roads and reduce maximum speed, see docs/core/custom-models.md for an in-depth explanation
        // and also the blog posts https://www.graphhopper.com/?s=customizable+routing
        CustomModel model = new CustomModel();
        model.addToPriority(If("road_class == PRIMARY", MULTIPLY, Double.parseDouble("0.5")));

        // unconditional limit to 100km/h
        model.addToPriority(If("true", LIMIT, Double.parseDouble("100")));

        req.setCustomModel(model);
        res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());

        assert Math.round(res.getBest().getTime() / 1000d) == 165;
    }
}
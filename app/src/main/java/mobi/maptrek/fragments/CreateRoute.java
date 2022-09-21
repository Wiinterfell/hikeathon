/*
 * Copyright 2022 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.fragments;

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Double.parseDouble;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.shapes.GHPoint3D;

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.map.Map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import mobi.maptrek.MainActivity;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.maps.routing.Router;
import mobi.maptrek.util.StringFormatter;

public class CreateRoute extends Fragment implements Map.UpdateListener {
    private static String OSM_FILE_NAME = "bretagne.pbf";
    private static String GH_CONFIG_FILE = "config.yml";
    private Router mRouter;
    private MapHolder mMapHolder;
    TextInputEditText mFromField;
    TextInputEditText mToField;

    public CreateRoute() {
        super(R.layout.fragment_create_route);
        this.mRouter = new Router();
    }

    @MainThread
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.compute_route).setOnClickListener(v -> {
            double fromLat = NaN;
            double fromLon = NaN;
            double toLat = NaN;
            double toLon = NaN;

            if (mFromField.getText().length() > 0 && mToField.getText().length() > 0) {
                String[] parsedFrom = mFromField.getText().toString().split(" ");
                String[] parsedTo = mToField.getText().toString().split(" ");
                fromLat = parseDouble(parsedFrom[0]);
                fromLon = parseDouble(parsedFrom[1]);
                toLat = parseDouble(parsedTo[0]);
                toLon = parseDouble(parsedTo[1]);
            }

            if (isNaN(fromLat) || isNaN(fromLon) || isNaN(toLat) || isNaN(toLon)) {
                Toast.makeText(getContext(), "Fill From and To fields first.", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                // First load profiles in cache folder for GraphHopper
                getFileFromAssets(getContext(), GH_CONFIG_FILE);
                ResponsePath route = mRouter.compute(getFileFromAssets(getContext(), OSM_FILE_NAME), fromLat, fromLon, toLat, toLon);
                Log.d("Router", "Route distance: " + route.getDistance() + ", time: " + route.getTime());
                ((MainActivity) getActivity()).showRoute(routeResponseToFileDataSource(route));
            } catch (Exception e) {
                Toast.makeText(getContext(), "Could not load route: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        mFromField = view.findViewById(R.id.route_from);
        mToField = view.findViewById(R.id.route_to);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mMapHolder = (MapHolder) context;
            mMapHolder.getMap().events.bind(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement MapHolder");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapHolder.getMap().events.unbind(this);
    }

    private void updateLocation(double latitude, double longitude) {
        if (mFromField.isFocused()) {
            mFromField.setText(StringFormatter.coordinates(0, " ", latitude, longitude));
        } else if (mToField.isFocused()) {
            mToField.setText(StringFormatter.coordinates(0, " ", latitude, longitude));
        }
    }

    private String getFileFromAssets(Context context, String fileName) throws IOException {
        Path outFileName = Paths.get(context.getCacheDir().getAbsolutePath(), fileName);
        Files.copy(getResources().getAssets().open(OSM_FILE_NAME), outFileName, StandardCopyOption.REPLACE_EXISTING);
        return outFileName.toAbsolutePath().toString();
    }

    private FileDataSource routeResponseToFileDataSource(ResponsePath route) {
        FileDataSource dataSource = new FileDataSource();
        dataSource.name = getString(R.string.defaultCustomRouteName);
        Track track = new Track();
        dataSource.name = track.name;
        dataSource.tracks.add(track);
        track.source = dataSource;
        dataSource.propertiesOffset = 0L;

        for (GHPoint3D point : route.getPoints()) {
            track.addPoint(/* continuous */ true, (int) (point.lat * 1E6), (int) (point.lon * 1E6), (float) point.ele, Float.NaN, Float.NaN, Float.NaN, 0L);
        }

        return dataSource;
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.POSITION_EVENT) {
            updateLocation(mapPosition.getLatitude(), mapPosition.getLongitude());
        }
    }
}
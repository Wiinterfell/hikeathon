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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.graphhopper.ResponsePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import mobi.maptrek.R;
import mobi.maptrek.maps.routing.Router;

public class CreateRoute extends Fragment {
    private static String OSM_FILE_NAME = "bretagne.pbf";
    private Router mRouter;

    public CreateRoute() {
        super(R.layout.fragment_create_route);
        this.mRouter = new Router();
    }

    @MainThread
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.compute_route).setOnClickListener(v -> {
            try {
                double fromLat = 47.74064;
                double fromLon = -3.38867;
                double toLat = 47.74529;
                double toLon = -3.35381;
                ResponsePath route = mRouter.compute(getFileFromAssets(getContext(), OSM_FILE_NAME), fromLat, fromLon, toLat, toLon);
                Log.d("Router", "Route distance: " + route.getDistance() + ", time: " + route.getTime());
            } catch (IOException e) {
                Toast.makeText(getContext(), "Could not load map file.", Toast.LENGTH_LONG);
                e.printStackTrace();
            }
        });
    }

    private String getFileFromAssets(Context context, String fileName) throws IOException {
        Path outFileName = Paths.get(context.getCacheDir().getAbsolutePath(), fileName);
        Files.copy(getResources().getAssets().open(OSM_FILE_NAME), outFileName, StandardCopyOption.REPLACE_EXISTING);
        return outFileName.toAbsolutePath().toString();
    }
}
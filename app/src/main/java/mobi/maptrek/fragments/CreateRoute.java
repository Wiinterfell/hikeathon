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

import android.os.Bundle;
import android.view.View;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.maps.routing.Router;

public class CreateRoute extends Fragment {
    private Router mRouter;
    private MapIndex mMapIndex;

    public CreateRoute() {
        super(R.layout.fragment_create_route);
        this.mMapIndex = MapTrek.getApplication().getExtraMapIndex();
        this.mRouter = new Router();
    }

    @MainThread
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.compute_route).setOnClickListener(v -> {
            mRouter.compute(mMapIndex);
        });
    }
}
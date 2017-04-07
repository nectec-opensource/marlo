/*
 * Copyright (c) 2016 NECTEC
 *   National Electronics and Computer Technology Center, Thailand
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package th.or.nectec.marlo;

import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Stack;

import th.or.nectec.marlo.model.Coordinate;
import th.or.nectec.marlo.model.Polygon;
import th.or.nectec.marlo.model.PolygonData;
import th.or.nectec.marlo.option.DefaultPolygonMarkerOptionFactory;
import th.or.nectec.marlo.option.DefaultPolygonOptionFactory;
import th.or.nectec.marlo.option.MarkerOptionFactory;
import th.or.nectec.marlo.option.PolygonOptionFactory;

public class PolygonMarloFragment extends MarloFragment {

    private MarkerOptionFactory passiveMarkOptFactory;
    private PolygonOptionFactory polyOptFactory;
    private PolygonController controller = new PolygonController();

    public PolygonMarloFragment() {
        super();
        markOptFactory = new DefaultPolygonMarkerOptionFactory();
        polyOptFactory = new DefaultPolygonOptionFactory();
    }

    public static PolygonMarloFragment newInstance() {
        PolygonMarloFragment fragment = new PolygonMarloFragment();
        return fragment;
    }

    public void setPolygonOptionFactory(PolygonOptionFactory polygonOptionFactory) {
        this.polyOptFactory = polygonOptionFactory;
    }

    @Override
    public void setMarkerOptionFactory(MarkerOptionFactory markerOptionFactory) {
        super.setMarkerOptionFactory(markerOptionFactory);
    }

    public void setPassiveMakerOptionFactory(MarkerOptionFactory passiveMarkerOptFactory){
        this.passiveMarkOptFactory = passiveMarkerOptFactory;
    }

    protected PolygonController getController() {
        return controller;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        activeMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
//        passiveMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
    }

    private Polygon tempRestoreData;

    public void setRestoreData(Polygon restoreData){
        if (googleMap != null) {
            controller.retore(restoreData);
            tempRestoreData = null;
            return;
        }
        tempRestoreData = restoreData;
    }

    public void useDefaultToolsMenu(){
        ViewUtils.addPolygonToolsMenu(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.marlo_hole) {
            try { controller.newHole(); }
            catch (IllegalStateException expected){ onNotReadyToNewHole(); }
        } else if (view.getId() == R.id.marlo_boundary) {
            try { controller.startNewPolygon(); }
            catch (IllegalStateException expected) { onNotReadyToNewPolygon(); }
        } else if (view.getId() == R.id.marlo_undo) {
            undo();
        } else {
            super.onClick(view);
        }
    }

    protected void onNotReadyToNewHole() {
        //For subclass to implement
    }

    protected void onNotReadyToNewPolygon() {
        //For subclass to implement
    }

    protected void onPolygonChanged(List<Polygon> polygons, Coordinate focusCoordinate) {
        //For subclass to implement
    }

    protected void onMarkInvalidHole(List<Polygon> polygons, LatLng markPoint) {
        //For subclass to implement
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        controller.setPresenter(new GoogleMapPresenter(googleMap));

        if (tempRestoreData != null) {
            setRestoreData(tempRestoreData);
        }
    }

    @Override
    public void mark(LatLng markPoint) {
        try {
            controller.mark(new Coordinate(markPoint));
            SoundUtility.play(getContext(), R.raw.thumpsoundeffect);
            onPolygonChanged(controller.getPolygons(), controller.getFocusPolygon().getLastCoordinate());
        } catch (HoleInvalidException expected){
            onMarkInvalidHole(controller.getPolygons(), markPoint);
        }
    }

    @Override
    public boolean undo() {
        boolean undo = controller.undo();
        if (undo){
            onPolygonChanged(controller.getPolygons(), controller.getFocusPolygon().getLastCoordinate());
        }
        return undo;
    }

    public List<Polygon> getPolygons() {
        return controller.getPolygons();
    }

    private class GoogleMapPresenter implements PolygonController.Presenter {

        private final Stack<PolygonData> multiPolygon = new Stack<>();
        private final GoogleMap googleMap;

        public GoogleMapPresenter(GoogleMap googleMap) {
            this.googleMap = googleMap;
            multiPolygon.push(new PolygonData());
        }

        @Override
        public void markHole(Coordinate coordinate) {
            updateIconToLastMarker(passiveMarkOptFactory);

            LatLng markPoint = coordinate.toLatLng();
            Marker marker = googleMap.addMarker(markOptFactory.build(PolygonMarloFragment.this, markPoint));
            PolygonData activePolygon = getActivePolygonData();
            activePolygon.addMarker(marker);
            PolygonDrawUtils.draw(googleMap, activePolygon, polyOptFactory.build(PolygonMarloFragment.this));
        }

        @Override
        public void markBoundary(Coordinate coordinate) {
            updateIconToLastMarker(passiveMarkOptFactory);

            LatLng markPoint = coordinate.toLatLng();
            Marker marker = googleMap.addMarker(markOptFactory.build(PolygonMarloFragment.this, markPoint));
            PolygonData activePolygon = getActivePolygonData();
            activePolygon.addMarker(marker);
            PolygonDrawUtils.draw(googleMap, activePolygon, polyOptFactory.build(PolygonMarloFragment.this));
        }

        @Override
        public void prepareForNewPolygon() {
            multiPolygon.push(new PolygonData());
            getActivePolygonData().setCurrentState(PolygonData.State.BOUNDARY);
        }

        @Override
        public void prepareForNewHole() {
            getActivePolygonData().newHole();
            getActivePolygonData().setCurrentState(PolygonData.State.HOLE);
        }

        @Override
        public void removeLastMarker() {
            PolygonData polygonData = getActivePolygonData();
            if (polygonData.isEmpty()) {
                return;
            }
            boolean removed = polygonData.removeLastMarker();
            updateIconToLastMarker(markOptFactory);

            if (removed && polygonData.isEmpty() && multiPolygon.size() > 1) {
                multiPolygon.pop();
            }

            PolygonDrawUtils.draw(googleMap, polygonData,
                    polyOptFactory.build(PolygonMarloFragment.this));
        }

        private void updateIconToLastMarker(MarkerOptionFactory optionFactory) {
            MarkerOptions option = optionFactory.build(PolygonMarloFragment.this, new LatLng(1, 1));
            Marker lastMarker = getActivePolygonData().getLastMarker();
            if (lastMarker != null) {
                updateByOption(lastMarker, option);
            } else if (multiPolygon.size() > 1) {
                PolygonData topEmptyPolygon = multiPolygon.pop();
                updateByOption(multiPolygon.peek().getLastMarker(), option);
                multiPolygon.push(topEmptyPolygon);
            }
        }

        private PolygonData getActivePolygonData() {
            return multiPolygon.peek();
        }

        private void updateByOption(Marker marker, MarkerOptions options){
            marker.setAlpha(options.getAlpha());
            marker.setAnchor(options.getAnchorU(), options.getAnchorV());
            marker.setIcon(options.getIcon());
            marker.setFlat(options.isFlat());
        }
    }
}

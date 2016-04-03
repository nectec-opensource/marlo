package th.or.nectec.marlo;

import android.os.Bundle;
import android.view.View;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.Stack;

public class MultiMarkerMarloFragment extends MarloFragment {

    private final Stack<Marker> markers = new Stack<>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewUtils.addPolygonToolsMenu(this);
        findViewBy(R.id.marlo_hole).setVisibility(View.GONE);
        findViewBy(R.id.marlo_boundary).setVisibility(View.GONE);
    }

    @Override
    protected void onViewfinderClick(LatLng viewfinderTarget) {
        SoundUtility.play(getContext(), R.raw.thumpsoundeffect);
        Marker marker = getGoogleMap().addMarker(markerFactory.build(this, viewfinderTarget));
        markers.push(marker);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.marlo_undo) {
            undo();
        } else {
            super.onClick(view);
        }
    }

    private void undo() {
        if (!markers.empty())
            markers.pop().remove();
    }
}

package com.example.attendance2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class VenueAdapter extends RecyclerView.Adapter<VenueAdapter.VenueViewHolder> {

    private final List<VenueGps> venueList;

    public VenueAdapter(List<VenueGps> venueList) {
        this.venueList = venueList;
    }

    @NonNull
    @Override
    public VenueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_venue_card, parent, false);
        return new VenueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VenueViewHolder holder, int position) {
        VenueGps venue = venueList.get(position);

        // Bind the text views to the specific data elements from your schema
        holder.itemVenueNameTextView.setText(venue.venueName);
        holder.itemVenueCodeTextView.setText(venue.venueCode);

        // Safely format primitives into strings matching your telemetry standards
        holder.itemRadiusTextView.setText(String.format(Locale.getDefault(), "%dm Radius", venue.radius));
        holder.itemLatitudeTextView.setText(String.format(Locale.getDefault(), "%.2f", venue.latitude));
        holder.itemLongitudeTextView.setText(String.format(Locale.getDefault(), "%.2f", venue.longitude));
    }

    @Override
    public int getItemCount() {
        return venueList.size();
    }

    public static class VenueViewHolder extends RecyclerView.ViewHolder {
        TextView itemVenueNameTextView, itemVenueCodeTextView, itemRadiusTextView, itemLatitudeTextView, itemLongitudeTextView;

        public VenueViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link directly to the IDs defined inside your item_venue_card.xml
            itemVenueNameTextView = itemView.findViewById(R.id.itemVenueNameTextView);
            itemVenueCodeTextView = itemView.findViewById(R.id.itemVenueCodeTextView);
            itemRadiusTextView = itemView.findViewById(R.id.itemRadiusTextView);
            itemLatitudeTextView = itemView.findViewById(R.id.itemLatitudeTextView);
            itemLongitudeTextView = itemView.findViewById(R.id.itemLongitudeTextView);
        }
    }
}
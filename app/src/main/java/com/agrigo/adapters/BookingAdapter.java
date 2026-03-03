package com.agrigo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.agrigo.R;
import com.agrigo.models.Booking;
import java.util.List;

/**
 * RecyclerView adapter for bookings
 */
public class BookingAdapter extends RecyclerView.Adapter<BookingViewHolder> {
    
    private List<Booking> bookingList;
    private Context context;
    private OnBookingSelectListener listener;

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @Override
    public BookingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BookingViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_booking, parent, false));
    }

    @Override
    public void onBindViewHolder(BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.bind(booking, context);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookingSelected(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookingList = bookings;
        notifyDataSetChanged();
    }

    public void setOnBookingSelectListener(OnBookingSelectListener listener) {
        this.listener = listener;
    }

    public interface OnBookingSelectListener {
        void onBookingSelected(Booking booking);
    }
}

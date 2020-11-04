package com.example.opencv_mobile.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opencv_mobile.R;
import com.example.opencv_mobile.pojo.OptionsData;
import com.example.opencv_mobile.utils.MEventsManager;

import java.util.ArrayList;

public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.OptionsVH> {
    private ArrayList<OptionsData> data;
    private LayoutInflater inflater;
    private Context context;
    private static final String TAG = "OptionsAdapter";

    public OptionsAdapter(Context context) {
        Log.e(TAG, "OptionsAdapter: ");
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @NonNull
    @Override
    public OptionsAdapter.OptionsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.options_card_layout, parent, false);
        return new OptionsVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionsAdapter.OptionsVH holder, int position) {
        Log.e(TAG, "onBindViewHolder: ");
        holder.optionsIcon.setImageResource(data.get(position).getImgID());
        holder.optionText.setText(data.get(position).getOptionsName());
    }

    public void setData(ArrayList<OptionsData> list) {
        this.data = list;
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    class OptionsVH extends RecyclerView.ViewHolder {
        ImageView optionsIcon;
        TextView optionText;

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MEventsManager.getInstance().inject(MEventsManager.SELECTED_OPTION_TYPE, data.get(getAdapterPosition()).getOptionsName());
            }
        };

        public OptionsVH(@NonNull View itemView) {
            super(itemView);
            optionsIcon = itemView.findViewById(R.id.options_image);
            optionText = itemView.findViewById(R.id.option_text);
            itemView.setOnClickListener(clickListener);
        }
    }
}

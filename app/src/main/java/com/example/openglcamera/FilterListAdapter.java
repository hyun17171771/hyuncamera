package com.example.openglcamera;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;

import java.util.ArrayList;

public class FilterListAdapter extends RecyclerView.Adapter<FilterListAdapter.ViewHolder> {

    private static final String TAG = FilterListAdapter.class.getSimpleName();
    private Context mContext;
    private RequestManager mGlideManager;
    private ArrayList<FilterVO> mList;

    public interface OnItemClickListener {
        void onClick(View view, int position);
    }

    private static OnItemClickListener mListener;


    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView mIvFilterImage;
        public TextView mTvName;

        public ViewHolder(View v) {
            super(v);

            v.findViewById(R.id.cl_item).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "## 아이템 클릭");
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        if (mListener != null) {
                            mListener.onClick(view, position);
                        }
                    }
                }
            });

            //mIvFilterImage = v.findViewById(R.id.iv_channel);

            mIvFilterImage = v.findViewById(R.id.iv_sample_img);
            mTvName = v.findViewById(R.id.tv_filer_name);

        }
    }

    public FilterListAdapter(Context context, RequestManager glideManager, ArrayList<FilterVO> list) {
        this.mContext = context;
        this.mGlideManager = glideManager;
        this.mList = list;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_filter, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        mGlideManager.load(mList.get(position).getSampleImage()).into(holder.mIvFilterImage);
        holder.mTvName.setText(mList.get(position).getType().getFilterName());
    }

    @Override
    public int getItemCount() {
        return (mList == null) ? 0 : mList.size();
    }


}

package com.fiixed.photogallery;


import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by abell on 12/7/13.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    ThumbnailDownloader<ImageView> mThumbnailThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);  //retains the fragment state when parent Activity is destroyed
        new FetchItemsTask().execute();  //starts the AsyncTask and runs doInBackground()

        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());  //creates a new thread and passes a handler to it
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if(isVisible()) {  //checks to see if the fragment is visible first
                    imageView.setImageBitmap(thumbnail);  //returns bitmaps from background thread
                }
            }
        });
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);  //inflates the layout
        mGridView = (GridView)v.findViewById(R.id.gridView);

        setupAdapter();
        return v;
    }

    void setupAdapter() {
        if(getActivity() == null || mGridView == null) {  //fragments can exist unattached from the activity
            return;
        }
        if(mItems != null) {
            mGridView.setAdapter(new GalleryItemAdapter(mItems));
        } else {
            mGridView.setAdapter(null);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    /*
    creates background thread
     */
    private class FetchItemsTask extends AsyncTask<Void,Void,ArrayList<GalleryItem>> {
        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetcher().fetchItems();

        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {  //run in the main UI thread, not the background
            mItems = items;
            setupAdapter(); //Cannot update the UI from a background thread
        }
    }

    /*
    custom adapter to display photos in an ImageView
     */
    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

        public GalleryItemAdapter(ArrayList<GalleryItem> items) {
            super(getActivity(), 0 , items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
            }

            ImageView imageView = (ImageView)convertView.findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(R.drawable.me);
            GalleryItem item = getItem(position);  //gets the correct item for the position
            mThumbnailThread.queueThumbnail(imageView, item.getUrl());  //triggers the image downloading
            return convertView;
        }

    }
}

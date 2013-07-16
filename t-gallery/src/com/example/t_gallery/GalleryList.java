package com.example.t_gallery;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Images.Thumbnails;
import android.app.Activity;
import android.app.ExpandableListActivity;
import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

class GalleryListAdapter extends BaseExpandableListAdapter{

	
	GalleryListAdapter(Context aContext){
		context = aContext;
	}
	
	static class ViewHolder{
		ImageView icons[] = new ImageView[4];
		BitmapWorkerTask task[] = new BitmapWorkerTask[4];
	}
	
	class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap>{

		private final WeakReference<ImageView> iconReference;
		private int index;
		
		public BitmapWorkerTask(ImageView icon){
			iconReference = new WeakReference<ImageView>(icon);
		}
		
		@Override
		protected  Bitmap doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			GalleryList app = (GalleryList)context;
			long id = 0;
			index = params[0];
		    BitmapFactory.Options options = new BitmapFactory.Options();	

			
			synchronized (app.mImageList){
			app.mImageList.moveToPosition(params[0]);
			id = app.mImageList.getLong(app.mImageList.getColumnIndex(Media._ID));
			}
			
			Bitmap thumb = Thumbnails.getThumbnail(app.getContentResolver(), id, Thumbnails.MINI_KIND, options);
			int height = thumb.getHeight();
			int width = thumb.getWidth();
			
			if (height > width){
				thumb = Bitmap.createBitmap(thumb, 0, (height-width)/2, width, width);
			}
			else{
				thumb = Bitmap.createBitmap(thumb, (width-height)/2, 0, height, height);
			}
			
	        return Bitmap.createScaledBitmap(thumb,170,170,false);
	
			/*BitmapFactory.Options option = new BitmapFactory.Options();
			option.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(app.cameraFiles.get(params[0]), option);
			
			float xRatio = (float)option.outWidth / (float)270;
			float yRatio = (float)option.outHeight / (float)270;
			
			float scaleRatio = (xRatio<yRatio)?xRatio:yRatio;
			
			option.inJustDecodeBounds = false;
			option.inSampleSize = Math.round(scaleRatio);
			index = params[0];
			return BitmapFactory.decodeFile(app.cameraFiles.get(params[0]), option);*/
		}
		
		public void onPostExecute(Bitmap bitmap){
			GalleryList app = (GalleryList)context;
			
			if (iconReference != null && bitmap != null){
				final ImageView iconView = iconReference.get();
				
				if (iconView != null){
					app.addBitmapToRamCache(index, bitmap);
					iconView.setImageBitmap(bitmap);
				}
			}
		}
		
	}
	
	private Context context;

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return groupPosition*100000 + childPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return groupPosition*100000 + childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		LinearLayout line;
		GalleryList app = (GalleryList)context;
		
		Log.e("t-Gallery","Fetch child view of "+groupPosition+"/"+childPosition);
		
		ViewHolder holder;
		
		if (convertView == null){
			line = new LinearLayout(context);
			line.setOrientation(LinearLayout.HORIZONTAL);
			
            holder = new ViewHolder();
            
            for (int i=0; i<4; i++){
            	holder.icons[i] = new ImageView(context);
            	holder.icons[i].setAdjustViewBounds(true);
				holder.icons[i].setScaleType(ImageView.ScaleType.CENTER_CROP);	
    			line.addView(holder.icons[i], new LinearLayout.LayoutParams(270,270));
            }
			
			line.setTag(holder);
		}
		else {
			line = (LinearLayout)convertView;
			holder = (ViewHolder)(line.getTag()); 
			
			for (int i=0; i<4; i++){
				if (holder.task[i] != null){
					holder.task[i].cancel(true);
					holder.task[i] = null;
				}
			}
		}
		
		if (groupPosition == 0){
			for (int i=0; i<4; i++){
				Bitmap bmp = app.getBitmapFromRamCace(childPosition*4 + i);
				
				if (bmp != null){
				    holder.icons[i].setImageBitmap(bmp);	
				}
				else{
				    BitmapWorkerTask task = new BitmapWorkerTask(holder.icons[i]);
				    task.execute(childPosition*4 + i);
				    holder.task[i] = task;
				    holder.icons[i].setImageResource(R.drawable.empty_photo);
				}			
			}
		}
		else {
		    int offset = childPosition%6;

		    for (int i=0; i<4; i++){
			    holder.icons[i].setImageResource(R.drawable.photo01 + offset*4 + i);
		    }
		}
		return line;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		// TODO Auto-generated method stub
		if (groupPosition == 0){
			GalleryList app = (GalleryList)context;
			return (app.mImageList.getCount()/4);
		}
		else if (groupPosition == 1){
			return 4;
		}
		else 
		{
		    return 10000;
		}
	}

	@Override
	public Object getGroup(int groupPosition) {
		// TODO Auto-generated method stub
		return groupPosition;
	}

	@Override
	public int getGroupCount() {
		// TODO Auto-generated method stub
		return 10;
	}

	@Override
	public long getGroupId(int groupPosition) {
		// TODO Auto-generated method stub
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		TextView text;
		
		if (convertView == null){
			text = new TextView(context);
		}
		else{
			text = (TextView)convertView;
		}
		
		if (groupPosition == 0){
			text.setText(new String("    Camera"));
		}
		else if (groupPosition == 1){
			text.setText(new String("    Small Gallery"));
		}
		else {
		    text.setText(new String("    Big Gallery "));
		}
		
		text.setTextSize(30);
		
		return text;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return false;
	}
	

}

public class GalleryList extends ExpandableListActivity 
	implements OnScrollListener{
	
	public ArrayList<String> cameraFiles = new ArrayList<String>();
	
	private LruCache<Integer, Bitmap> mRamCache;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_gallery_list);
				
		mImageList = getContentResolver().query(Media.EXTERNAL_CONTENT_URI, new String[]{Media._ID}, null, null, null);
		
		final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 4;
		
		mRamCache = new LruCache<Integer, Bitmap>(cacheSize){
			protected int sizeOf(Integer key, Bitmap bmp){
				return (bmp.getByteCount()/1024);
			}
		};
		
		/*
		File cameraDir = new File(new String("/sdcard/DCIM/Camera"));
		String fileNames[] = cameraDir.list();
		String suffix = new String("jpg");
		
		for(int i=0; i<fileNames.length; i++){
			if (fileNames[i].endsWith(suffix)){
				String prefix = new String("/sdcard/DCIM/Camera/");
				cameraFiles.add(prefix.concat(fileNames[i]));
			}
		}
		 */
		
	
		setListAdapter(new GalleryListAdapter(this));
		getExpandableListView().setOnScrollListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_gallery_list, menu);
		return true;
	}
		
	public synchronized Bitmap getBitmapFromRamCace(int key){
		return mRamCache.get(key);	
	}
	
	public synchronized void addBitmapToRamCache(int key, Bitmap bmp){
		if (null == getBitmapFromRamCace(key)){
			mRamCache.put(key, bmp);
		}
	}
	
	public Cursor mImageList;


	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		
		/*ExpandableListView list = getExpandableListView();
		long index = list.getExpandableListPosition(firstVisibleItem);
		int groupIndex = list.getPackedPositionGroup(index);
		int childIndex = list.getPackedPositionChild(index);onChild(index);
		Log.i("t-Gallery", "on scrolling "+index+childIndex+groupIndex);*/
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		
	}

}

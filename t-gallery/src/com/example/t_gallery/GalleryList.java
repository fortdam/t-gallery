package com.example.t_gallery;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Images.Thumbnails;
import android.animation.ObjectAnimator;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;



public class GalleryList extends ExpandableListActivity 
	implements OnScrollListener{
	
	
	static public class Config{
		/*Application configuration*/
		static public final int MAX_ALBUM = 1000;
		static public final int MAX_PICTURE_IN_ALBUM = 10000;
		
		static public final int THUMBNAILS_PER_LINE = 4;
		static public final int THUMBNAIL_WIDTH = 270;
		static public final int THUMBNAIL_HEIGHT = 270;
		
		/*RAM cache*/
		static public final int RAM_CACHE_SIZE_KB = (int)(Runtime.getRuntime().maxMemory()/4096);
		
		/*Content query*/
		static public final Uri MEDIA_URI = Media.EXTERNAL_CONTENT_URI;
		static public final String ALBUM_PROJECTION[] = {Media.BUCKET_ID, Media.BUCKET_DISPLAY_NAME};
		static public final String ALBUM_WHERE_CLAUSE = "1) GROUP BY (1"; /*This is a trick to use GROUP BY */
		static public final String IMAGE_PROJECTION[] = {Media._ID};
		static public final String IMAGE_WHERE_CLAUSE = Media.BUCKET_ID + " = ?";
	}
	
	private void fetchGalleryList(){
		
		mGalleryList = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.ALBUM_PROJECTION, 
				Config.ALBUM_WHERE_CLAUSE, 
				null, null);
	
		mImageLists = new Cursor[mGalleryList.getCount()];
		
		for (int i=0; i<mGalleryList.getCount(); i++){
			mGalleryList.moveToPosition(i);
			String galleryId = mGalleryList.getString(mGalleryList.getColumnIndex(Media.BUCKET_ID));
			mImageLists[i] = getContentResolver().query(
					Config.MEDIA_URI, 
					Config.IMAGE_PROJECTION, 
					Config.IMAGE_WHERE_CLAUSE, 
					new String[]{mGalleryList.getString(mGalleryList.getColumnIndex(Media.BUCKET_ID))}, 
					null);
		}
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gallery_list);
		
		fetchGalleryList();						
		
		mRamCache = new LruCache<Long, Bitmap>(Config.RAM_CACHE_SIZE_KB){
			protected int sizeOf(Long key, Bitmap bmp){
				return (bmp.getByteCount()/1024);
			}
		};
	
		setListAdapter(new GalleryListAdapter());
		getExpandableListView().setOnScrollListener(this);
	}
	
	private TextView shortcut;
	
	@Override
	protected void onResume(){
		super.onResume();
		shortcut = (TextView)findViewById(R.id.collapse_shortcut);		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_gallery_list, menu);
		return true;
	}
		
	private synchronized Bitmap getBitmapFromRamCace(long key){
		return mRamCache.get(key);	
	}
	
	private synchronized void addBitmapToRamCache(long key, Bitmap bmp){
		if (null == getBitmapFromRamCace(key)){
			mRamCache.put(key, bmp);
		}
	}
	
	private Cursor mGalleryList;
	private Cursor mImageLists[];
	private LruCache<Long, Bitmap> mRamCache;
	
	private ObjectAnimator ongoingAnim;
	private boolean shortcutDisp = true;
	
	private void ShowShortcut(){
		
		if (shortcutDisp){
			return;
		}
		
		int height = shortcut.getHeight();
		ongoingAnim.cancel();
		
		ObjectAnimator anim = ObjectAnimator.ofFloat(shortcut, "Y", 0).setDuration(500);
		anim.start();
		ongoingAnim = anim;
		shortcutDisp = true;
		
		
	}
	
	private void HideShortcut(boolean immediate){
		
		if(!shortcutDisp){
			return;
		}
		
		int height = shortcut.getHeight();
		
		if (ongoingAnim != null)
		ongoingAnim.cancel();
		
		if (immediate){
			shortcut.setY(0 - height);
		}
		else{
			ObjectAnimator anim = ObjectAnimator.ofFloat(shortcut, "Y", 0-height).setDuration(500);
			anim.start();
			ongoingAnim = anim;
		}
		shortcutDisp = false;
	}
	
	private int scrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

	private int prevFirstItem = -1;
	private int prevVisibleItemCount = -1;
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
			if (prevFirstItem == -1){
				prevFirstItem = firstVisibleItem;
				prevVisibleItemCount = visibleItemCount;
			}
			else {
				if (prevFirstItem >  firstVisibleItem){
					ShowShortcut();
				}
				else if (prevFirstItem < firstVisibleItem){
					HideShortcut(false);
				}
			}
		}
		else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING){
			prevFirstItem = -1;
			ExpandableListView list = getExpandableListView();
			long index = list.getExpandableListPosition(firstVisibleItem);
			int groupIndex = list.getPackedPositionGroup(index);
			int childIndex = list.getPackedPositionChild(index);
			
			if (childIndex == -1){
				HideShortcut(false);
			}
		}
	}

	private Timer timerToHide;
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
		if (this.scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING 
				&& scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE){
			if (shortcutDisp){
				timerToHide = new Timer();
				timerToHide.schedule(new TimerTask(){
					
					Handler msgHandler = new Handler(){
						public void handleMessage(Message msg){
							if (shortcutDisp){
								HideShortcut(false);
							}
						}
					};

					@Override
					public void run() {
						msgHandler.obtainMessage().sendToTarget();
					}
					
				}, 1500);
			}
		}
		
		this.scrollState = scrollState;
	}

	
	class GalleryListAdapter extends BaseExpandableListAdapter{
		
		class BitmapWorkerTask extends AsyncTask<Long, Void, Bitmap>{

			private final WeakReference<ImageView> iconReference;
			private long id;
			
			public BitmapWorkerTask(ImageView icon){
				iconReference = new WeakReference<ImageView>(icon);
			}
			
			@Override
			protected  Bitmap doInBackground(Long... params) {
			    BitmapFactory.Options options = new BitmapFactory.Options();
			    
			    id = params[0];
				
				Bitmap thumb = Thumbnails.getThumbnail(getContentResolver(), id, Thumbnails.MINI_KIND, options);
				int height = thumb.getHeight();
				int width = thumb.getWidth();
				
				if (height > width){
					thumb = Bitmap.createBitmap(thumb, 0, (height-width)/2, width, width);
				}
				else{
					thumb = Bitmap.createBitmap(thumb, (width-height)/2, 0, height, height);
				}
				
		        return Bitmap.createScaledBitmap(thumb,Config.THUMBNAIL_WIDTH,Config.THUMBNAIL_HEIGHT,false);
		
			}
			
			public void onPostExecute(Bitmap bitmap){
				
				if (iconReference != null && bitmap != null){
					final ImageView iconView = iconReference.get();
					
					if (iconView != null){
						addBitmapToRamCache(id, bitmap);
						iconView.setImageBitmap(bitmap);
					}
				}
			}
			
		}
		
	    class ViewHolder{
			ImageView icons[] = new ImageView[Config.THUMBNAILS_PER_LINE];
			BitmapWorkerTask task[] = new BitmapWorkerTask[Config.THUMBNAILS_PER_LINE];
		}
	    
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return groupPosition*Config.MAX_PICTURE_IN_ALBUM + childPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return groupPosition*Config.MAX_PICTURE_IN_ALBUM + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			LinearLayout line;			
			ViewHolder holder;
			
			/*Prepare the view (no content yet)*/
			if (convertView == null){
				line = new LinearLayout(getApplicationContext());
				line.setOrientation(LinearLayout.HORIZONTAL);
				
	            holder = new ViewHolder();
	            
	            for (int i=0; i<Config.THUMBNAILS_PER_LINE; i++){
	            	holder.icons[i] = new ImageView(getApplicationContext());
	            	holder.icons[i].setAdjustViewBounds(true);
					holder.icons[i].setScaleType(ImageView.ScaleType.CENTER_CROP);	
	    			line.addView(holder.icons[i], new LinearLayout.LayoutParams(Config.THUMBNAIL_WIDTH,Config.THUMBNAIL_HEIGHT));
	            }
				
				line.setTag(holder);
			}
			else {
				line = (LinearLayout)convertView;
				holder = (ViewHolder)(line.getTag()); 
				
				for (int i=0; i<Config.THUMBNAILS_PER_LINE; i++){
					if (holder.task[i] != null){
						holder.task[i].cancel(true);
						holder.task[i] = null;
					}
				}
			}
			
			/*Fill in the content*/
	        for (int i=0; i<Config.THUMBNAILS_PER_LINE; i++){
	        	
	        	Cursor imageList = mImageLists[groupPosition];
	        	
	        	if (imageList.getCount() <= (childPosition*Config.THUMBNAILS_PER_LINE+i)){
	        		holder.icons[i].setImageResource(R.drawable.black);
	        	}
	        	else {
	        		imageList.moveToPosition(childPosition*Config.THUMBNAILS_PER_LINE+i);
	        		long id = imageList.getLong (imageList.getColumnIndex(Media._ID));
	        		Bitmap bmp = getBitmapFromRamCace(id);
	        		
	        		if (bmp != null){
	        			holder.icons[i].setImageBitmap(bmp);
	        		}
	        		else {
	        			BitmapWorkerTask task = new BitmapWorkerTask(holder.icons[i]);
					    task.execute(id);
					    holder.task[i] = task;
					    holder.icons[i].setImageResource(R.drawable.empty_photo);
	        		}
	        	}	
			}

			return line;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return (mImageLists[groupPosition].getCount() + Config.THUMBNAILS_PER_LINE - 1)/Config.THUMBNAILS_PER_LINE;
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groupPosition;
		}

		@Override
		public int getGroupCount() {
			return mImageLists.length;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			
			TextView text;
			
			if (convertView == null){
				text = new TextView(getApplicationContext());
			}
			else {
				text = (TextView)convertView;
			}
			
			mGalleryList.moveToPosition(groupPosition);
			int count = mImageLists[groupPosition].getCount();
			text.setText("     "+mGalleryList.getString(mGalleryList.getColumnIndex(Media.BUCKET_DISPLAY_NAME))+" ("+count+")");
			text.setTextSize(30);
			
			return text;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}
	}
}

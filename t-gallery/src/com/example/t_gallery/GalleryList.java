package com.example.t_gallery;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
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
		
		/*UI Effect*/
		static public final int LIST_ANIM_DURATION = 300; /*count in ms*/
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
		//getExpandableListView().setOnScrollListener(this);
	}
	
	private TextView shortcut;
	
	class Position {
		Position(long id, int top, int bottom, View view){
			mId = id;
			mTop = top;
			mBottom = bottom;
			mView = view;
		}
		public long mId;
		public int mTop;
		public int mBottom;
		public View mView;
	}
	
	
	class AnimHandler {
		protected PositionArray mPositionArray = new PositionArray();
		protected ExpandableListView listView;
		protected ViewGroup container;
		
		public boolean inAnimation = false;
		
		class ClearAnimState implements Runnable{

			@Override
			public void run() {
				inAnimation = false;
			}
			
		}
		
		class ViewDetacher implements Runnable{
			ViewGroup mParent;
			View mChild;
			
			public ViewDetacher(ViewGroup parent, View child){
				mParent = parent;
				mChild = child;
			}
			
			public void run(){
				mParent.removeView(mChild);
				inAnimation = false;
			}
		}
		
		class ViewVisible implements Runnable {
			private View item;
			private int value;
			
			public ViewVisible(View aItem, int aValue){
				item = aItem;
				value = aValue;
			}
			
			public void run(){
				item.setVisibility(value);
				inAnimation = false;
			}
		}
		
		class PositionArray extends ArrayList<Position> {
			
			public Position findById(long Id){
				for (int i=0; i<this.size(); i++){
					if (this.get(i).mId == Id){
						return this.get(i);
					}
				}
				return null;
			}
			
			public void clear(){
				for (int i=0; i<this.size(); i++){
					if (this.get(i).mView != null){
					    this.get(i).mView.setTag(R.id.recyclable, true);
					}
				}
				super.clear();
			}
		}
		
		public AnimHandler(ExpandableListView aListView, ViewGroup aContainer){
			listView = aListView;
			container = aContainer;
		}
		
		protected View findById(long Id){
			for (int i=0; i<listView.getChildCount(); i++){
				if (listView.getItemIdAtPosition(listView.getFirstVisiblePosition()+i) == Id){
					return listView.getChildAt(i);
				}
			}
			return null;
		}
		
		protected ViewPropertyAnimator animateTranslationY(View item, boolean inList, float start, float end){
			inAnimation = true;
			
			if (inList){
				item.setTranslationY(start);
				return item.animate().translationY(end).setDuration(Config.LIST_ANIM_DURATION).withEndAction(new ClearAnimState());
			}
			else {
				ImageView bmpCache = new ImageView(item.getContext());
				item.buildDrawingCache();
				bmpCache.setImageBitmap(item.getDrawingCache());
				
				container.addView(bmpCache, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				bmpCache.setTranslationY(start);
				return bmpCache.animate().translationY(end).setDuration(Config.LIST_ANIM_DURATION).withEndAction(new ViewDetacher(container, bmpCache));
			}
		}
		
		protected ViewPropertyAnimator animateTranslationY2(View item, boolean inList, float start, float end){
			inAnimation = true;
			if (inList){
				item.setTranslationY(start);
				return item.animate().translationY(end).setDuration(Config.LIST_ANIM_DURATION).withEndAction(new ClearAnimState());
			}
			else {
				ImageView bmpCache = new ImageView(item.getContext());
				item.buildDrawingCache();
				bmpCache.setImageBitmap(item.getDrawingCache());
				
				item.setVisibility(View.INVISIBLE);
				container.addView(bmpCache, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				bmpCache.setTranslationY(start);
				
				final ViewDetacher run1= new ViewDetacher(container, bmpCache);
				final ViewVisible run2 = new ViewVisible(item, View.VISIBLE);
				
				return bmpCache.animate().translationY(end).setDuration(Config.LIST_ANIM_DURATION).withEndAction(new Runnable(){

					@Override
					public void run() {
						run1.run();
						run2.run();
					}
					
				});
			}
		}
		
		protected void animateMask(float start, float end){
			/*int height = listView.getBottom() - (int)start + 2;
			
			TextView mask = new TextView(container.getContext());
			mask.setText(" aaa  ");
			mask.setTextSize(300);
			mask.setHeight(height);
			mask.setBackgroundColor(0);
			mask.setVisibility(View.VISIBLE);
			
			container.addView(mask, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			mask.setTranslationY(start);
			mask.animate().translationY(end).setDuration(Config.LIST_ANIM_DURATION).withEndAction(new ViewDetacher(container, mask));
			*/
			inAnimation = true;
			shortcut.setVisibility(View.VISIBLE);
			shortcut.setText(" ");
			shortcut.setY(start);
			shortcut.animate().translationY(end).setDuration(Config.LIST_ANIM_DURATION).withEndAction(new Runnable(){

				public void run() {
					// TODO Auto-generated method stub
					shortcut.setVisibility(View.INVISIBLE);
					inAnimation = false;
				}
			});
		}
		
		protected void buildPositionMap(boolean setTranientState){
			mPositionArray.clear();

			for (int i=0; i<listView.getChildCount(); i++) {
				long itemId = listView.getItemIdAtPosition(listView.getFirstVisiblePosition() + i);
				View listItem = listView.getChildAt(i);
				if (setTranientState){
					listItem.setTag(R.id.recyclable, false);
				}
				mPositionArray.add(new Position(itemId, listItem.getTop(), listItem.getBottom(), listItem));
			}
		}
	}
	
	class ExpandAnimHandler extends AnimHandler
		implements ExpandableListView.OnGroupClickListener{

		private int expandGroupPosition = -1;
		
		public ExpandAnimHandler(ExpandableListView aListView, ViewGroup aContainer){
			super(aListView, aContainer);
			
			//listView.setOnGroupClickListener(this);
		}
		

		@Override
		public boolean onGroupClick(ExpandableListView parent, View v,
				int groupPosition, long id) {		
			
			if (parent.isGroupExpanded(groupPosition)){
				return false;
			}
			
			buildPositionMap(true);
			
			
			for (int i=0; i<mPositionArray.size(); i++){
				if (mPositionArray.get(i).mView == v){
					expandGroupPosition = i;
					break;
				}
			}
			
			final ViewTreeObserver observer = listView.getViewTreeObserver();
			
			observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				
				@Override
				public boolean onPreDraw() {
					observer.removeOnPreDrawListener(this);
							
					int upBound = 0;
					
					for (int i=expandGroupPosition; i>=0; i--){
						Position oldPos = mPositionArray.get(i);
						View currentView = findById(oldPos.mId);
						
						if (currentView != null){
							animateTranslationY(currentView, true, oldPos.mTop-currentView.getTop(), 0);
							upBound = currentView.getTop();
						}
						else {
							animateTranslationY(oldPos.mView, false, oldPos.mTop, upBound-oldPos.mBottom+oldPos.mTop);
							upBound -= (oldPos.mBottom - oldPos.mTop);
						}
					}
					
					int downBound = listView.getBottom();
					for (int i=expandGroupPosition+1; i<mPositionArray.size(); i++){
						Position oldPos = mPositionArray.get(i);
						View currentView = findById(oldPos.mId);
						
						if (currentView != null){
							animateTranslationY(currentView, true, oldPos.mTop-currentView.getTop(), 0);
							downBound = currentView.getBottom();
						}
						else {
							animateTranslationY(oldPos.mView, false, oldPos.mTop, downBound);
							downBound += (oldPos.mBottom - oldPos.mTop );
						}
					}
					
					if (mPositionArray.get(mPositionArray.size()-1).mBottom < listView.getBottom()){
						animateMask(mPositionArray.get(mPositionArray.size()-1).mBottom, listView.getChildAt(listView.getChildCount()-1).getBottom());
					}
                    
					
					mPositionArray.clear();
					return false;
				}
			});
			
			return false;
		}
		
	}
	
	
	class CollapseAnimHandler extends AnimHandler
		implements ExpandableListView.OnGroupClickListener{

		private long collapseGroupID = -1;
		
		private Drawable backupListBG = null;
		private Bitmap cachedBG = null;
		
		public CollapseAnimHandler(ExpandableListView aListView, ViewGroup aContainer){
			super(aListView, aContainer);
			
			//listView.setOnGroupClickListener(this);
		}
	

	@Override
		public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {		
		
		if (!parent.isGroupExpanded(groupPosition)){
			return false;
		}
		
		buildPositionMap(true);
		
		
		
		for (int i=0; i<mPositionArray.size(); i++){
			if (mPositionArray.get(i).mView == v){
				collapseGroupID = mPositionArray.get(i).mId;
			}
		}
		
		/*Snapshot the current list*/
		listView.buildDrawingCache();
		cachedBG = listView.getDrawingCache();	
		
		final ViewTreeObserver observer = listView.getViewTreeObserver();
		
		observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			
			@Override
			public boolean onPreDraw() {
				observer.removeOnPreDrawListener(this);
				
				backupListBG = listView.getBackground();
				listView.setBackground(new BitmapDrawable(cachedBG));
				
				int collapseGroupPosition = -1;
				
				for (int i=0; i<listView.getChildCount(); i++){
					if (listView.getItemIdAtPosition(listView.getFirstVisiblePosition()+i) == collapseGroupID){
						collapseGroupPosition = i;
					}
				}
						
				int upBound = 0;
				
				for (int i=collapseGroupPosition; i>=0; i--){
					Position oldPos = mPositionArray.findById(listView.getItemIdAtPosition(listView.getFirstVisiblePosition()+i));
					View currentView = listView.getChildAt(i);
					
					if (oldPos != null){
						animateTranslationY(currentView, true, oldPos.mTop-currentView.getTop(), 0);
						upBound = oldPos.mTop;
					}
					else {
						animateTranslationY(currentView, false, upBound-currentView.getHeight(), currentView.getTop());
						upBound -= currentView.getHeight();
					}
				}
				
				int downBound = listView.getBottom();
				if (downBound > mPositionArray.get(mPositionArray.size()-1).mBottom){
					downBound = mPositionArray.get(mPositionArray.size()-1).mBottom;
				}
				for (int i=collapseGroupPosition+1; i<listView.getChildCount(); i++){
					Position oldPos = mPositionArray.findById(listView.getItemIdAtPosition(listView.getFirstVisiblePosition()+i));
					View currentView = listView.getChildAt(i);
					
					if (oldPos != null){
						//animateTranslationY(currentView, true, oldPos.mTop-currentView.getTop(), 0);
						/*Make the original one invisible during the animation*/
						animateTranslationY2(currentView, false, oldPos.mTop, currentView.getTop());

						downBound = oldPos.mBottom;
					}
					else {
						animateTranslationY2(currentView, false, downBound, currentView.getTop());
						downBound += currentView.getHeight();
					}
				}
				
				if (listView.getChildAt(listView.getChildCount()-1).getBottom() < listView.getBottom()){
					animateMask(downBound, listView.getChildAt(listView.getChildCount()-1).getBottom());
				}
                
				listView.getChildAt(0).animate().setDuration(Config.LIST_ANIM_DURATION).withEndAction(new Runnable(){
					@Override
					public void run() {
						listView.setBackground(backupListBG);
					}
				});
				
				mPositionArray.clear();
				return false;
			}
		});
		
		return false;
	}
	
}
	
	class GroupClickHandler implements  ExpandableListView.OnGroupClickListener {
		private ExpandableListView myList;
		private ExpandAnimHandler expand;
		private CollapseAnimHandler collapse;
		
		GroupClickHandler(ExpandableListView aList, ViewGroup container){
			myList = aList;
			expand = new ExpandAnimHandler(aList, container);
			collapse = new CollapseAnimHandler(aList, container);
			
			myList.setOnGroupClickListener(this);
		}

		@Override
		public boolean onGroupClick(ExpandableListView parent, View v,
				int groupPosition, long id) {
			// TODO Auto-generated method stub
			
			if (collapse.inAnimation || expand.inAnimation){
				return true; /*Ignore if the animatin is ongoing*/
			}
			
			if (myList.isGroupExpanded(groupPosition)){
				return collapse.onGroupClick(parent, v, groupPosition, id);
			}
			else {
				return expand.onGroupClick(parent, v, groupPosition, id);
			}
			
		}
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		shortcut = (TextView)findViewById(R.id.collapse_shortcut);
		shortcut.setVisibility(View.INVISIBLE);
		
		new GroupClickHandler(getExpandableListView(), (ViewGroup)findViewById(R.id.root_container));
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
			boolean inTransient = false;
		}
	    
	    @Override
	    public int getChildTypeCount(){
	    	return 1;
	    }
	    
	    @Override
	    public int getChildType(int groupPosition, int childPosition){
	    	return 0;
	    }
	    
	    @Override
	    public int getGroupTypeCount(){
	    	return 1;
	    }
	    
	    @Override
	    public int getGroupType(int groupPosition){
	    	return 0;
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
			if (convertView == null || !getRecyclable(convertView)){
				line = new LinearLayout(getApplicationContext());
				line.setOrientation(LinearLayout.HORIZONTAL);
				
	            holder = new ViewHolder();
	            
	            for (int i=0; i<Config.THUMBNAILS_PER_LINE; i++){
	            	holder.icons[i] = new ImageView(getApplicationContext());
	            	holder.icons[i].setAdjustViewBounds(true);
					holder.icons[i].setScaleType(ImageView.ScaleType.CENTER_CROP);	
	    			line.addView(holder.icons[i], new LinearLayout.LayoutParams(Config.THUMBNAIL_WIDTH,Config.THUMBNAIL_HEIGHT));
	            }
				
				line.setTag(R.id.data_holder, holder);
				line.setTag(R.id.recyclable, true);
			}
			else {
				line = (LinearLayout)convertView;
				holder = (ViewHolder)(line.getTag(R.id.data_holder)); 
				
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
			
			LinearLayout item;
			
			if (convertView == null || !getRecyclable(convertView)){
				LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService
					      (Context.LAYOUT_INFLATER_SERVICE);
				item = (LinearLayout)inflater.inflate(R.layout.gallery_item, null);
				item.setTag(R.id.recyclable, true);
			}
			else {
				item = (LinearLayout)convertView;
			}
			
			mGalleryList.moveToPosition(groupPosition);
			int count = mImageLists[groupPosition].getCount();
			TextView text = (TextView)item.getChildAt(0);
			text.setText(mGalleryList.getString(mGalleryList.getColumnIndex(Media.BUCKET_DISPLAY_NAME))+" ("+count+")");
			
			return item;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}
		
		public void setRecyclable(boolean flag, View item){
			item.setTag(R.id.recyclable,flag);
		}
		
		public boolean getRecyclable(View item){
			return (Boolean)item.getTag(R.id.recyclable);
		}
	}	 
}

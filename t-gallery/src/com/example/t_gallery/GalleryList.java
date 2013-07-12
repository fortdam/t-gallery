package com.example.t_gallery;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.ExpandableListActivity;
import android.app.ListActivity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

class GalleryListAdapter extends BaseExpandableListAdapter{

	
	GalleryListAdapter(Context aContext){
		context = aContext;
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
		Log.e("t-Gallery","Fetch child view of "+groupPosition+"/"+childPosition);
		
		if (convertView == null){
			line = new LinearLayout(context);
			line.setOrientation(LinearLayout.HORIZONTAL);
			

		}
		else {
			line = (LinearLayout)convertView;
		}
		
		if (groupPosition == 0){
			GalleryList app = (GalleryList)context;
			
			for (int i=0; i<4; i++){
				BitmapFactory.Options option = new BitmapFactory.Options();
				option.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(app.cameraFiles.get(childPosition*4 + i), option);
				
				float xRatio = (float)option.outWidth / (float)270;
				float yRatio = (float)option.outHeight / (float)270;
				
				float scaleRatio = (xRatio<yRatio)?xRatio:yRatio;
				
				option.inJustDecodeBounds = false;
				option.inSampleSize = Math.round(scaleRatio);
				Bitmap bmp = BitmapFactory.decodeFile(app.cameraFiles.get(childPosition*4 + i), option);
				
				ImageView icon = new ImageView(context);
				icon.setImageBitmap(bmp);
				icon.setAdjustViewBounds(true);
				icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
				
				line.addView(icon, new LinearLayout.LayoutParams(270,270));
			}
		}
		else {
		    for (int i=0; i<4; i++){
		    	ImageView icon = new ImageView(context);
			    int offset = childPosition%6;
			    icon.setImageResource(R.drawable.photo01 + offset*4 + i);
			    icon.setAdjustViewBounds(true);
			    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);

		    	line.addView(icon, new LinearLayout.LayoutParams(
					270,
					270));
		    }
		}
		return line;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		// TODO Auto-generated method stub
		if (groupPosition == 0){
			GalleryList app = (GalleryList)context;
			return (app.cameraFiles.size()/4);
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

public class GalleryList extends ExpandableListActivity {
	
	public ArrayList<String> cameraFiles = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_gallery_list);
		File cameraDir = new File(new String("/sdcard/DCIM/Camera"));
		String fileNames[] = cameraDir.list();
		String suffix = new String("jpg");
		
		for(int i=0; i<fileNames.length; i++){
			if (fileNames[i].endsWith(suffix)){
				String prefix = new String("/sdcard/DCIM/Camera/");
				cameraFiles.add(prefix.concat(fileNames[i]));
			}
		}
	
		setListAdapter(new GalleryListAdapter(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_gallery_list, menu);
		return true;
	}

}

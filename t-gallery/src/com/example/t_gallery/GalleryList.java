package com.example.t_gallery;

import android.os.Bundle;
import android.app.Activity;
import android.app.ExpandableListActivity;
import android.app.ListActivity;
import android.content.Context;
import android.database.DataSetObserver;
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
			
			for (int i=0; i<4; i++){
				ImageView icon = new ImageView(context);
				icon.setImageResource(R.drawable.photo01);
				icon.setAdjustViewBounds(true);
				icon.setVisibility(1);
				icon.setMinimumWidth(270);
				icon.setMinimumHeight(270);
				icon.setMaxWidth(270);
				icon.setMaxHeight(270);
				icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
				line.addView(icon, new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT));
			}
		}
		else {
			line = (LinearLayout)convertView;
		}
		
		
		return line;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		// TODO Auto-generated method stub
		return 10000;
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
		
		text.setText(new String("    Gallery "+(groupPosition+1)));
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
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_gallery_list);
	
		setListAdapter(new GalleryListAdapter(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_gallery_list, menu);
		return true;
	}

}

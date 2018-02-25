package hackillinois.text_direction;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomList extends ArrayAdapter<String>{

  private final Activity context;
  private final ArrayList<String> directions;
  private final ArrayList<String> distances;
  private final ArrayList<Integer> imageID;

  // Requires direction list, distance list, and image resource ID list
  public CustomList(Activity context, ArrayList<String> directions, ArrayList<String> distances,
                    ArrayList<Integer> imageID) {
    super(context, R.layout.list_single, directions);
    this.context = context;
    this.directions = directions;
    this.distances = distances;
    this.imageID = imageID;
  }

  // Populate the listView
  @Override
  public View getView(int position, View view, ViewGroup parent) {
    LayoutInflater inflater = context.getLayoutInflater();
    View rowView= inflater.inflate(R.layout.list_single, null, true);

    TextView directionText = rowView.findViewById(R.id.direction_text);
    TextView distanceText = rowView.findViewById(R.id.distance_text);
    ImageView imageView = rowView.findViewById(R.id.direction_img);

    directionText.setText((String)(directions.toArray())[position]);
    distanceText.setText((String)(distances.toArray())[position]);
    imageView.setImageResource((Integer)(imageID.toArray())[position]);

    return rowView;
  }
}